#!/usr/bin/env bash
#
# Gated deploy of the production WAR to the local Tomcat.
#
# A red post-merge CI run once sat unnoticed while a broken WAR was built by
# hand and copied into webapps/. This script runs every check that has bitten
# a deploy before, in order, and only touches webapps/ if all of them pass:
#   1. Builds -Pwar,production by default. A -Pwar-only build ships Vaadin
#      dev mode, which hangs a cold Tomcat start (DevModeUsageStatistics).
#   2. ReleasedMigrationChecksumTest — released-script checksums, version
#      gaps, and duplicate versions in the source tree.
#   3. WAR parity — the packaged migration set must exactly match
#      src/main/resources/db/migration. Catches stale copies in target/
#      after renames (resource copy never deletes removed files); auto-purges
#      and rebuilds once when it finds drift.
#   4. Production bundle — the WAR must contain the Vaadin production
#      frontend (VAADIN/webapp/VAADIN/build/*.js).
#   5. CI status — no failed run on the deployed commit (override:
#      ALLOW_RED_CI=1, e.g. for an intentionally cancelled run).
# Then: copy to webapps/, kickstart Tomcat (server.xml has autoDeploy=false
# and the manager app has no user), and watch atwrpg.log for the outcome.
#
# Usage:
#   scripts/pre-deploy-check.sh               # build + check + deploy
#   scripts/pre-deploy-check.sh --skip-build  # deploy existing target WAR
#   scripts/pre-deploy-check.sh --dry-run     # run all checks, deploy nothing

set -euo pipefail

REPO="$(git rev-parse --show-toplevel)"
cd "$REPO"

WAR_NAME="all-time-wrestling-rpg.war"
WAR="target/$WAR_NAME"
WEBAPPS="${WEBAPPS_DIR:-/opt/homebrew/etc/tomcat/webapps}"
LOG="${ATWRPG_LOG:-/opt/homebrew/opt/tomcat/libexec/logs/atwrpg.log}"
STARTUP_TIMEOUT="${STARTUP_TIMEOUT:-300}"

SKIP_BUILD=false
MODE="deploy"
for arg in "$@"; do
  case "$arg" in
    --skip-build) SKIP_BUILD=true ;;
    --dry-run) MODE="dry-run" ;;
    *)
      echo "Unknown argument: $arg" >&2
      echo "Usage: scripts/pre-deploy-check.sh [--skip-build] [--dry-run]" >&2
      exit 2
      ;;
  esac
done

step() { printf '\n==> %s\n' "$*"; }
die() {
  printf '✗ %s\n' "$*" >&2
  exit 1
}

[ -d "$WEBAPPS" ] || die "Tomcat webapps dir not found: $WEBAPPS"
[ -f "$LOG" ] || die "App log not found: $LOG"

# --- 1. Build ---------------------------------------------------------------
if [ "$SKIP_BUILD" = true ]; then
  step "Skipping build (--skip-build)"
  [ -f "$WAR" ] || die "--skip-build set but $WAR does not exist"
else
  step "Building production WAR (-Pwar,production)"
  ./mvnw -q -Pwar,production package -DskipTests
fi
[ -f "$WAR" ] || die "Build did not produce $WAR"

# --- helpers ----------------------------------------------------------------
war_migration_list() {
  unzip -l "$WAR" | awk '{print $4}' \
    | grep -E '^WEB-INF/classes/db/migration/(mysql|h2)/V[0-9]+__.*\.sql$' \
    | sed 's|^WEB-INF/classes/||' | sort
}

src_migration_list() {
  # shellcheck disable=SC2046  # word splitting is the point: relative paths only
  (cd src/main/resources && find db/migration/mysql db/migration/h2 -name 'V[0-9]*__*.sql' | sort)
}

purge_stale_target_migrations() {
  local staged f rel
  for staged in target/classes target/*/WEB-INF/classes; do
    [ -d "$staged/db/migration" ] || continue
    for f in "$staged"/db/migration/mysql/*.sql "$staged"/db/migration/h2/*.sql; do
      [ -f "$f" ] || continue
      rel="${f#"$staged"/}"
      if [ ! -f "src/main/resources/$rel" ]; then
        rm "$f"
        echo "  removed stale: $f"
      fi
    done
  done
}

check_war() {
  # Production bundle present? A -Pwar build without -Pproduction ships dev
  # mode and hangs a cold Tomcat in Vaadin's dev-mode initializer.
  # Note: no `grep -q` here — under pipefail it can SIGPIPE the producer and
  # report a false negative on large listings.
  if ! unzip -l "$WAR" 2>/dev/null | grep -E 'VAADIN/webapp/VAADIN/build/[^/]+\.js' > /dev/null; then
    echo "✗ WAR has no Vaadin production frontend bundle (dev-mode WAR)."
    echo "  Rebuild with -Pwar,production."
    return 1
  fi
  # Migration parity: the packaged set must equal the source set exactly.
  local parity
  if ! parity="$(diff <(war_migration_list) <(src_migration_list))"; then
    echo "✗ Packaged migrations differ from src/main/resources/db/migration:"
    printf '%s\n' "$parity" | sed 's/^/    /'
    return 1
  fi
  return 0
}

# --- 2-3. WAR contents ------------------------------------------------------
step "Verifying WAR contents (production bundle + migration parity)"
if ! check_war; then
  step "Drift detected — purging stale target/ migration copies and rebuilding once"
  purge_stale_target_migrations
  rm -f "$WAR"
  ./mvnw -q -Pwar,production package -DskipTests
  check_war || die "WAR still does not match the source migration set after purge+rebuild. Run a full 'mvn clean package -Pwar,production -DskipTests' and retry."
fi
echo "  WAR OK: production bundle present, migrations match source."

# --- 4. Source guard tests ---------------------------------------------------
step "Running migration guard tests (checksums, gaps, duplicate versions)"
./mvnw -q test -Dtest=ReleasedMigrationChecksumTest \
  || die "ReleasedMigrationChecksumTest failed — fix src/main/resources/db/migration before deploying."
echo "  Guard tests passed."

# --- 5. CI status -------------------------------------------------------------
step "Checking CI status for commit $(git rev-parse --short HEAD)"
sha="$(git rev-parse HEAD)"
summary="$(
  gh run list --commit "$sha" --limit 15 --json status,conclusion \
    --jq '{completed: [.[] | select(.status=="completed")] | length,
           red: [.[] | select(.status=="completed") | select(.conclusion!="success" and .conclusion!="skipped" and .conclusion!="neutral")] | length,
           pending: [.[] | select(.status!="completed")] | length}' 2>/dev/null
)" || summary=""
if [ -z "$summary" ]; then
  echo "  WARNING: could not query GitHub Actions (offline or unauthenticated) — continuing without the CI gate."
else
  echo "  $summary"
  case "$summary" in
    *'"red":0'*)
      if [ "${ALLOW_PENDING_CI:-0}" = "1" ]; then
        echo "  ALLOW_PENDING_CI=1 — continuing despite pending CI runs."
      elif echo "$summary" | grep -qE '"pending":[1-9]'; then
        die "CI is still running for commit $(git rev-parse --short HEAD) — a pass is not a verdict yet.
  Wait: gh run watch <run-id>   (or)   Override: ALLOW_PENDING_CI=1 scripts/pre-deploy-check.sh $*"
      else
        echo "  CI has no failed runs for this commit."
      fi
      ;;
    *)
      if [ "${ALLOW_RED_CI:-0}" != "1" ]; then
        die "CI has failed run(s) for commit $(git rev-parse --short HEAD) — deploy refused.
  Inspect: gh run list --commit $sha
  Override (know what you are doing): ALLOW_RED_CI=1 scripts/pre-deploy-check.sh $*"
      fi
      echo "  ALLOW_RED_CI=1 — continuing despite failed CI."
      ;;
  esac
fi

# --- 6. Deploy ----------------------------------------------------------------
if [ "$MODE" = "dry-run" ]; then
  step "Dry run — all checks passed. Commands that a real deploy would run:"
  echo "  cp $WAR $WEBAPPS/atw-rpg.war"
  echo "  launchctl kickstart -k gui/\$(id -u)/tomcat"
  echo "  # then watch $LOG for 'Started Application in' / 'Application run failed'"
  exit 0
fi

step "Deploying $WAR -> $WEBAPPS/atw-rpg.war"
cp "$WAR" "$WEBAPPS/atw-rpg.war"

step "Restarting Tomcat"
log_offset="$(wc -l < "$LOG" | tr -d ' ')"
launchctl kickstart -k "gui/$(id -u)/tomcat"

step "Waiting for startup outcome (timeout ${STARTUP_TIMEOUT}s)"
deadline=$((SECONDS + STARTUP_TIMEOUT))
while [ "$SECONDS" -lt "$deadline" ]; do
  cur_lines="$(wc -l < "$LOG" | tr -d ' ')"
  if [ "$cur_lines" -lt "$log_offset" ]; then log_offset=0; fi # log rotated
  new_log="$(tail -n +"$((log_offset + 1))" "$LOG")"
  if printf '%s\n' "$new_log" | grep "Started Application in" > /dev/null; then
    printf '%s\n' "$new_log" | grep "Started Application in" | tail -1 | sed 's/^/  /'
    echo "✓ Deployed and started — $WEBAPPS/atw-rpg.war is live."
    exit 0
  fi
  if printf '%s\n' "$new_log" | grep "Application run failed" > /dev/null; then
    printf '%s\n' "$new_log" | tail -50 | sed 's/^/  /'
    die "Startup failed — full detail in $LOG"
  fi
  sleep 3
done
die "Timed out after ${STARTUP_TIMEOUT}s waiting for startup — check $LOG"
