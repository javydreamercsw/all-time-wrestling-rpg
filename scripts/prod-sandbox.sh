#!/usr/bin/env bash
#
# prod-sandbox.sh — safe pre-release smoke testing against a COPY of production data.
#
# Workflow:
#   preflight  (optional) validate pending Flyway migrations against a prod dump in Docker
#   snapshot   dump the prod schema and load it into a sandbox schema
#   start      run the -Pproduction candidate JAR on :8081 against the SANDBOX schema
#   stop       stop the candidate
#   discard    stop + drop the sandbox schema (revert path — prod untouched)
#   promote    make the tested sandbox data the new prod data (guarded, with rollback dump)
#   rollback   restore the pre-promote dump and archived WAR (post-promote escape hatch)
#   status     show schemas, dumps, and candidate state
#
# Configuration (environment, or ~/.atwrpg/sandbox.env which is sourced if present):
#   PROD_DB        (required) production schema name
#   SANDBOX_DB     sandbox schema name       (default: ${PROD_DB}_sandbox)
#   BACKUP_DIR     dump/WAR archive dir      (default: ~/.atwrpg/sandbox-backups)
#   CANDIDATE_PORT candidate HTTP port       (default: 8081)
#   MYSQL_HOST     MySQL host                (default: 127.0.0.1)
#   MYSQL_USER / MYSQL_PWD  credentials; omit to rely on ~/.my.cnf
#   TOMCAT_WEBAPPS Tomcat webapps dir (promote/rollback WAR archiving; same var Cargo uses)
#   TOMCAT_SERVICE brew service name         (default: tomcat)
#
# Freeze rule: do NOT use production between `snapshot` and `promote`/`discard` —
# prod changes made after the snapshot are lost on promote.
#
# Promote guards: `promote` refuses unless the sandbox was provably created by
# `snapshot` (provenance file) AND production is byte-identical to the snapshot
# (freeze-rule check). Set FORCE_PROMOTE=1 to override the freeze check ONLY when
# you accept losing the prod changes made since the snapshot.

set -euo pipefail

CONFIG_FILE="${HOME}/.atwrpg/sandbox.env"
# shellcheck disable=SC1090
[ -f "$CONFIG_FILE" ] && source "$CONFIG_FILE"

: "${PROD_DB:?PROD_DB is required (set it in the environment or ${CONFIG_FILE})}"
SANDBOX_DB="${SANDBOX_DB:-${PROD_DB}_sandbox}"
BACKUP_DIR="${BACKUP_DIR:-${HOME}/.atwrpg/sandbox-backups}"
CANDIDATE_PORT="${CANDIDATE_PORT:-8081}"
MYSQL_HOST="${MYSQL_HOST:-127.0.0.1}"
TOMCAT_SERVICE="${TOMCAT_SERVICE:-tomcat}"
RUN_DIR="${HOME}/.atwrpg/sandbox"
PID_FILE="${RUN_DIR}/candidate.pid"
LOG_FILE="${RUN_DIR}/candidate.log"
# Provenance record proving the sandbox was seeded from a prod snapshot.
STATE_FILE="${RUN_DIR}/sandbox.meta"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"

if [ "$SANDBOX_DB" = "$PROD_DB" ]; then
  echo "FATAL: SANDBOX_DB must differ from PROD_DB" >&2
  exit 1
fi

mkdir -p "$BACKUP_DIR" "$RUN_DIR"

timestamp() { date +%Y%m%d_%H%M%S; }

mysql_args() {
  local args=(--host="$MYSQL_HOST")
  [ -n "${MYSQL_USER:-}" ] && args+=(--user="$MYSQL_USER")
  # MYSQL_PWD is honored by the mysql client itself when exported.
  echo "${args[@]}"
}

run_mysql() {
  # shellcheck disable=SC2046
  mysql $(mysql_args) "$@"
}

run_mysqldump() {
  # --skip-dump-date keeps dumps deterministic so identical data hashes identically
  # (the freeze-rule check in `promote` depends on this).
  # shellcheck disable=SC2046
  mysqldump $(mysql_args) --single-transaction --routines --triggers --skip-dump-date "$@"
}

dump_sha() { shasum -a 256 "$1" | awk '{print $1}'; }

meta_get() { sed -n "s/^${1}=//p" "$STATE_FILE" 2>/dev/null | head -1; }

# The sandbox is only trustworthy if OUR `snapshot` created it from the current
# PROD_DB. Anything else (hand-made schema, stale schema from an abandoned cycle,
# schema built by Flyway from scratch) must never be promoted over production.
meta_valid() {
  [ -f "$STATE_FILE" ] \
    && [ "$(meta_get PROD_DB)" = "$PROD_DB" ] \
    && [ "$(meta_get SANDBOX_DB)" = "$SANDBOX_DB" ] \
    && [ -n "$(meta_get SNAPSHOT_SHA256)" ]
}

require_snapshot_provenance() {
  if ! meta_valid; then
    echo "REFUSING: sandbox '${SANDBOX_DB}' was not created by '$0 snapshot' for '${PROD_DB}'" >&2
    echo "(missing or mismatched ${STATE_FILE})." >&2
    echo "Promoting such a sandbox would REPLACE production with data that never" >&2
    echo "started as a copy of production. Run '$0 snapshot' first." >&2
    exit 1
  fi
}

# Read a key from the [client] section of ~/.my.cnf (what the mysql CLI uses),
# so the candidate JAR can authenticate the same way without extra configuration.
my_cnf_client() {
  [ -f "${HOME}/.my.cnf" ] || return 0
  sed -n '/^\[client\]/,/^\[/p' "${HOME}/.my.cnf" \
    | sed -n "s/^${1}[[:space:]]*=[[:space:]]*//p" | head -1 \
    | sed -e 's/^"\(.*\)"$/\1/' -e "s/^'\(.*\)'\$/\1/"
}

schema_exists() {
  run_mysql -N -e "SELECT COUNT(*) FROM information_schema.schemata WHERE schema_name='$1'" | grep -q '^1$'
}

confirm_prod() {
  echo "!! This operation modifies the PRODUCTION schema '${PROD_DB}'."
  read -r -p "Type the production schema name to continue: " answer
  if [ "$answer" != "$PROD_DB" ]; then
    echo "Aborted."
    exit 1
  fi
}

latest_dump() {
  # $1 = prefix (pre-test | pre-promote)
  ls -1t "${BACKUP_DIR}/${1}-"*.sql 2>/dev/null | head -1 || true
}

candidate_pid() {
  [ -f "$PID_FILE" ] && cat "$PID_FILE" || true
}

candidate_running() {
  local pid
  pid=$(candidate_pid)
  [ -n "$pid" ] && kill -0 "$pid" 2>/dev/null
}

cmd_preflight() {
  local dump
  dump="${1:-$(latest_dump pre-test)}"
  if [ -z "$dump" ]; then
    echo "No dump found. Run 'snapshot' first or pass a dump path." >&2
    exit 1
  fi
  echo "Validating pending migrations against: $dump (requires Docker)"
  (cd "$REPO_ROOT" && mvn -Pintegration-test verify \
      -Dit.test=FlywayMigrationIT \
      -Dsurefire.skip=true \
      -Dprod.dump.file="$dump")
  echo "Preflight OK — pending migrations apply cleanly to production data."
}

cmd_snapshot() {
  local dump="${BACKUP_DIR}/pre-test-$(timestamp).sql"
  echo "Dumping '${PROD_DB}' -> ${dump}"
  run_mysqldump "$PROD_DB" > "$dump"
  echo "Rebuilding sandbox schema '${SANDBOX_DB}'"
  run_mysql -e "DROP DATABASE IF EXISTS \`${SANDBOX_DB}\`; CREATE DATABASE \`${SANDBOX_DB}\`"
  run_mysql "$SANDBOX_DB" < "$dump"
  # Record provenance: promote/start refuse without this, and promote uses the
  # dump hash to detect prod changes made after the snapshot (freeze-rule check).
  cat > "$STATE_FILE" <<EOF
PROD_DB=${PROD_DB}
SANDBOX_DB=${SANDBOX_DB}
PRE_TEST_DUMP=${dump}
SNAPSHOT_SHA256=$(dump_sha "$dump")
CREATED=$(timestamp)
EOF
  echo
  echo "Sandbox ready: ${SANDBOX_DB} (copy of ${PROD_DB})"
  echo ">>> FREEZE production use now. Changes to '${PROD_DB}' after this point are LOST on promote. <<<"
}

cmd_start() {
  if candidate_running; then
    echo "Candidate already running (pid $(candidate_pid)). Use 'stop' first." >&2
    exit 1
  fi
  if ! schema_exists "$SANDBOX_DB"; then
    echo "Sandbox schema '${SANDBOX_DB}' does not exist. Run 'snapshot' first." >&2
    exit 1
  fi
  require_snapshot_provenance
  local jar="${1:-}"
  if [ -z "$jar" ]; then
    echo "Building candidate JAR (-Pproduction)..."
    (cd "$REPO_ROOT" && mvn -q -Pproduction package -DskipTests)
    jar=$(ls -1t "${REPO_ROOT}"/target/*.jar 2>/dev/null | grep -v -E 'sources|javadoc|\.original' | head -1)
  fi
  if [ ! -f "$jar" ]; then
    echo "Candidate JAR not found: $jar" >&2
    exit 1
  fi
  echo "Starting candidate on :${CANDIDATE_PORT} against '${SANDBOX_DB}' (log: ${LOG_FILE})"
  # Spring needs explicit creds — it cannot read ~/.my.cnf like the mysql CLI can.
  # Fall back to the CLI's [client] credentials so zero-config setups just work.
  local db_user db_pwd
  db_user="${MYSQL_USER:-$(my_cnf_client user)}"
  db_user="${db_user:-root}"
  db_pwd="${MYSQL_PWD:-$(my_cnf_client password)}"
  local env_vars=(
    SPRING_PROFILES_ACTIVE=prod,mysql
    SPRING_DATASOURCE_URL="jdbc:mysql://${MYSQL_HOST}:3306/${SANDBOX_DB}"
    SPRING_DATASOURCE_USERNAME="$db_user"
    PORT="$CANDIDATE_PORT"
  )
  [ -n "$db_pwd" ] && env_vars+=(SPRING_DATASOURCE_PASSWORD="$db_pwd")
  env "${env_vars[@]}" nohup java -jar "$jar" > "$LOG_FILE" 2>&1 &
  echo $! > "$PID_FILE"
  echo "Waiting for startup (Flyway migrates the SANDBOX schema only)..."
  local i
  for i in $(seq 1 120); do
    if curl -sf "http://localhost:${CANDIDATE_PORT}/" >/dev/null 2>&1; then
      echo "Candidate is up: http://localhost:${CANDIDATE_PORT}/  (pid $(candidate_pid))"
      return 0
    fi
    if ! candidate_running; then
      echo "Candidate died during startup — check ${LOG_FILE}" >&2
      exit 1
    fi
    sleep 2
  done
  echo "Timed out waiting for the candidate; it may still be starting — check ${LOG_FILE}" >&2
  exit 1
}

cmd_stop() {
  if candidate_running; then
    echo "Stopping candidate (pid $(candidate_pid))"
    kill "$(candidate_pid)"
    sleep 2
    candidate_running && kill -9 "$(candidate_pid)" 2>/dev/null
  else
    echo "Candidate not running."
  fi
  rm -f "$PID_FILE"
}

cmd_discard() {
  cmd_stop
  if schema_exists "$SANDBOX_DB"; then
    echo "Dropping sandbox schema '${SANDBOX_DB}' (production untouched)"
    run_mysql -e "DROP DATABASE \`${SANDBOX_DB}\`"
  fi
  rm -f "$STATE_FILE"
  echo "Reverted. Production schema '${PROD_DB}' was never modified."
}

cmd_promote() {
  if ! schema_exists "$SANDBOX_DB"; then
    echo "Sandbox schema '${SANDBOX_DB}' does not exist — nothing to promote." >&2
    exit 1
  fi
  require_snapshot_provenance
  local sandbox_tables
  sandbox_tables=$(run_mysql -N -e \
    "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='${SANDBOX_DB}'")
  if [ "${sandbox_tables:-0}" -eq 0 ]; then
    echo "REFUSING: sandbox '${SANDBOX_DB}' is empty — promoting it would wipe production." >&2
    exit 1
  fi
  confirm_prod
  cmd_stop

  echo "Stopping production Tomcat (${TOMCAT_SERVICE})..."
  brew services stop "$TOMCAT_SERVICE" >/dev/null 2>&1 || \
    echo "  (brew services stop failed or not applicable — ensure Tomcat is stopped before continuing)"

  if [ -n "${TOMCAT_WEBAPPS:-}" ] && ls "${TOMCAT_WEBAPPS}"/*.war >/dev/null 2>&1; then
    local war_archive="${BACKUP_DIR}/war-$(timestamp)"
    mkdir -p "$war_archive"
    cp "${TOMCAT_WEBAPPS}"/*.war "$war_archive/"
    echo "Archived current WAR(s) -> ${war_archive}/"
  else
    echo "NOTE: TOMCAT_WEBAPPS not set or no WAR found — skipping WAR archive."
  fi

  local pre_promote="${BACKUP_DIR}/pre-promote-$(timestamp).sql"
  echo "Rollback point: dumping '${PROD_DB}' -> ${pre_promote}"
  run_mysqldump "$PROD_DB" > "$pre_promote"

  # Freeze-rule check: production must be byte-identical to what `snapshot` copied.
  # If it changed, someone used prod during the test window — promoting now would
  # silently discard those changes.
  if [ "$(dump_sha "$pre_promote")" != "$(meta_get SNAPSHOT_SHA256)" ]; then
    if [ "${FORCE_PROMOTE:-0}" = "1" ]; then
      echo "WARNING: production changed since the snapshot — FORCE_PROMOTE=1 set, continuing." >&2
      echo "         Prod changes made after the snapshot WILL BE LOST (rollback dump: ${pre_promote})." >&2
    else
      echo "REFUSING: production '${PROD_DB}' changed since the snapshot the sandbox was built from." >&2
      echo "Promoting would DISCARD those production changes. Either:" >&2
      echo "  - re-run '$0 snapshot' and repeat the test on fresh data, or" >&2
      echo "  - re-run with FORCE_PROMOTE=1 if losing the post-snapshot prod changes is acceptable." >&2
      exit 1
    fi
  fi

  local sandbox_dump="${BACKUP_DIR}/sandbox-final-$(timestamp).sql"
  echo "Dumping tested sandbox '${SANDBOX_DB}' -> ${sandbox_dump}"
  run_mysqldump "$SANDBOX_DB" > "$sandbox_dump"

  echo "Replacing '${PROD_DB}' with the tested data..."
  run_mysql -e "DROP DATABASE \`${PROD_DB}\`; CREATE DATABASE \`${PROD_DB}\`"
  run_mysql "$PROD_DB" < "$sandbox_dump"
  run_mysql -e "DROP DATABASE \`${SANDBOX_DB}\`"
  rm -f "$STATE_FILE"

  echo
  echo "Promotion complete. '${PROD_DB}' now holds the tested (migrated) data."
  echo "Next steps:"
  echo "  1. Deploy the new WAR via the normal Cargo Deploy run configuration."
  echo "  2. Start Tomcat: brew services start ${TOMCAT_SERVICE}"
  echo "Rollback available via: $0 rollback   (uses ${pre_promote})"
}

cmd_rollback() {
  local dump
  dump=$(latest_dump pre-promote)
  if [ -z "$dump" ]; then
    echo "No pre-promote dump found in ${BACKUP_DIR} — nothing to roll back to." >&2
    exit 1
  fi
  echo "Rolling back '${PROD_DB}' to: $dump"
  confirm_prod
  echo "Stopping production Tomcat (${TOMCAT_SERVICE})..."
  brew services stop "$TOMCAT_SERVICE" >/dev/null 2>&1 || \
    echo "  (brew services stop failed or not applicable — ensure Tomcat is stopped before continuing)"
  run_mysql -e "DROP DATABASE IF EXISTS \`${PROD_DB}\`; CREATE DATABASE \`${PROD_DB}\`"
  run_mysql "$PROD_DB" < "$dump"
  local war_archive
  war_archive=$(ls -1dt "${BACKUP_DIR}"/war-* 2>/dev/null | head -1 || true)
  if [ -n "$war_archive" ] && [ -n "${TOMCAT_WEBAPPS:-}" ]; then
    cp "$war_archive"/*.war "$TOMCAT_WEBAPPS/"
    echo "Restored archived WAR(s) from ${war_archive}/ -> ${TOMCAT_WEBAPPS}/"
  else
    echo "NOTE: no archived WAR restored (missing archive or TOMCAT_WEBAPPS) — redeploy the previous release manually."
  fi
  echo "Start Tomcat when ready: brew services start ${TOMCAT_SERVICE}"
  echo "Rollback complete."
}

cmd_status() {
  echo "Prod schema:     ${PROD_DB}    $(schema_exists "$PROD_DB" && echo '[exists]' || echo '[MISSING]')"
  echo "Sandbox schema:  ${SANDBOX_DB} $(schema_exists "$SANDBOX_DB" && echo '[exists]' || echo '[absent]')"
  if schema_exists "$SANDBOX_DB"; then
    meta_valid \
      && echo "Provenance:      OK (snapshot of ${PROD_DB} taken $(meta_get CREATED))" \
      || echo "Provenance:      INVALID — sandbox not created by 'snapshot'; promote will refuse"
  fi
  if candidate_running; then
    echo "Candidate:       RUNNING (pid $(candidate_pid), http://localhost:${CANDIDATE_PORT}/)"
  else
    echo "Candidate:       stopped"
  fi
  echo "Backups in ${BACKUP_DIR}:"
  ls -1t "${BACKUP_DIR}" 2>/dev/null | head -10 | sed 's/^/  /' || echo "  (none)"
}

usage() {
  sed -n '2,36p' "$0" | sed 's/^# \{0,1\}//'
  exit 1
}

case "${1:-}" in
  preflight) shift; cmd_preflight "$@" ;;
  snapshot)  cmd_snapshot ;;
  start)     shift; cmd_start "$@" ;;
  stop)      cmd_stop ;;
  discard)   cmd_discard ;;
  promote)   cmd_promote ;;
  rollback)  cmd_rollback ;;
  status)    cmd_status ;;
  *)         usage ;;
esac
