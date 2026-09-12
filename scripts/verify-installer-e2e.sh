#!/usr/bin/env bash
# Installer end-to-end smoke verification (ATW-72ld).
#
# Builds the production app-image and asserts the four failure modes that
# unit tests cannot catch (ATW-mcwe):
#   1. the bundled runtime still carries bin/java (jLink must not strip it)
#   2. the packaged launcher JAR contains every compiled Launcher*.class
#   3. the packaged JAR (built with -Pproduction) has productionMode:true
#   4. the bundled launcher downloads a JAR through an HTTP 302 and starts
#      it with the bundled runtime (mock GitHub release server)
#
# Usage: scripts/verify-installer-e2e.sh
# Exits 0 on success, 1 on the first failed assertion.
set -euo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$REPO_ROOT"

if [ -z "${JAVA_HOME:-}" ]; then
  echo "JAVA_HOME must point to a JDK 25 with jpackage (e.g. Temurin 25)" >&2
  exit 1
fi

fail() {
  echo "FAIL: $1" >&2
  exit 1
}

echo "== Building production app-image (this takes a few minutes) =="
rm -rf "target/dist/All Time Wrestling.app" target/dist/all-time-wrestling/ 2>/dev/null || true
mvn -B -q package -Pproduction,desktop -DskipTests -Dsurefire.skip=true -Djpackage.type=APP_IMAGE

# ── Locate the app bundle (macOS .app vs linux directory) ────────────────────
if [ -d "target/dist/All Time Wrestling.app" ]; then
  APP="target/dist/All Time Wrestling.app"
  RUNTIME_HOME="$APP/Contents/runtime/Contents/Home"
  APP_DIR="$APP/Contents/app"
else
  # Linux app-image: a directory named after --name ("All Time Wrestling"), which
  # jpackage may keep verbatim — do not assume a fixed name here.
  APP="$(find target/dist -mindepth 1 -maxdepth 1 -type d | head -1)"
  if [ -z "$APP" ] || [ ! -d "$APP/lib/runtime" ]; then
    fail "no Linux app-image with lib/runtime under target/dist"
  fi
  RUNTIME_HOME="$APP/lib/runtime"
  APP_DIR="$APP/lib/app"
fi

echo "== Assertion 1: bundled runtime has bin/java and it runs =="
if [ ! -x "$RUNTIME_HOME/bin/java" ]; then
  fail "bundled runtime is missing bin/java — jLink stripNativeCommands must be false (ATW-mcwe)"
fi
JAVA_VERSION_LINE="$("$RUNTIME_HOME/bin/java" -version 2>&1 | head -1)"
echo "  bundled: $JAVA_VERSION_LINE"
case "$JAVA_VERSION_LINE" in
  *"25."*) : ;;
  *) fail "bundled runtime is not Java 25: $JAVA_VERSION_LINE" ;;
esac

echo "== Assertion 2: launcher JAR carries every compiled Launcher*.class =="
LAUNCHER_JAR="$(find "$APP_DIR" -name 'atw-launcher-*.jar' | head -1)"
if [ -z "$LAUNCHER_JAR" ]; then
  fail "no atw-launcher-*.jar in the app image"
fi
# Nested classes compile to separate files (Launcher$ReleaseInfo.class) — each
# compiled class must be packaged or the launcher dies with NoClassDefFoundError.
EXPECTED_CLASSES="$(
  find "$REPO_ROOT/target/classes/com/github/javydreamercsw" -maxdepth 1 \
    -name 'Launcher*.class' -exec basename {} \; | sort
)"
if [ -z "$EXPECTED_CLASSES" ]; then
  fail "no compiled Launcher*.class under target/classes — build ran?"
fi
PACKAGED_CLASSES="$(
  unzip -l "$LAUNCHER_JAR" 'com/github/javydreamercsw/Launcher*.class' \
    | awk '/Launcher.*\.class/ {print $NF}' | sed 's|.*/||' | sort
)"
if [ "$EXPECTED_CLASSES" != "$PACKAGED_CLASSES" ]; then
  echo "  compiled: $EXPECTED_CLASSES" >&2
  echo "  packaged: $PACKAGED_CLASSES" >&2
  fail "packaged launcher JAR is missing compiled Launcher classes (ATW-mcwe)"
fi
echo "  OK: $(echo "$EXPECTED_CLASSES" | wc -l | tr -d ' ') Launcher classes present"

echo "== Assertion 3: packaged JAR is production mode =="
MAIN_JAR="$(find target -maxdepth 1 -name 'all-time-wrestling-rpg-*.jar' ! -name '*-launcher.jar' | head -1)"
if [ -z "$MAIN_JAR" ]; then
  fail "no packaged app JAR found"
fi
FLOW_INFO="$(unzip -p "$MAIN_JAR" META-INF/VAADIN/config/flow-build-info.json 2>/dev/null || echo '')"
echo "$FLOW_INFO" | grep -q '"productionMode" *: *true' \
  || fail "packaged JAR has productionMode != true — release artifacts must build with -Pproduction (ATW-mcwe)"
echo "  productionMode: true"

echo "== Assertion 4: launcher downloads via 302 and starts the app =="
# Mock a GitHub release: the API points at a redirector host (302, mirroring
# github.com -> release-assets.githubusercontent.com) which forwards to the
# asset host serving a tiny runnable JAR. Its main() prints a marker and exits
# 0, so the launcher detects a healthy child, exits 0 itself, and the script
# survives set -e after proving the full download -> swap -> launch path.
SMOKE_HOME="$(mktemp -d)"
export ATW_LAUNCHER_TEST_HOME="$SMOKE_HOME"
PORT_FILE="$SMOKE_HOME/ports"
MARKER_JAR="$SMOKE_HOME/smoke-asset.jar"

# Compile the marker JAR the launcher will download and run.
cat > "$SMOKE_HOME/SmokeApp.java" <<'JAVA'
public class SmokeApp {
  public static void main(String[] args) {
    System.out.println("SMOKE_APP_STARTED marker=" + String.join(",", args));
  }
}
JAVA
mkdir -p "$SMOKE_HOME/classes"
"$JAVA_HOME/bin/javac" -d "$SMOKE_HOME/classes" "$SMOKE_HOME/SmokeApp.java"
"$JAVA_HOME/bin/jar" cfe "$MARKER_JAR" SmokeApp -C "$SMOKE_HOME/classes" SmokeApp.class

node - "$PORT_FILE" "$MARKER_JAR" <<'NODE' &
const http = require('http');
const fs = require('fs');
const portFile = process.argv[2];
const markerJar = fs.readFileSync(process.argv[3]);
let redirectorPort = 0;
let secondPort = 0;
const api = http.createServer((req, res) => {
  if (req.url.startsWith('/api/releases')) {
    res.writeHead(200, {'Content-Type': 'application/json'});
    res.end(JSON.stringify({
      tag_name: 'v99.0.0-smoke',
      assets: [{browser_download_url: 'http://127.0.0.1:' + redirectorPort + '/asset/app.jar'}]
    }));
  } else { res.writeHead(404); res.end(); }
});
const redirector = http.createServer((req, res) => {
  // 302 to a second host, mirroring github.com -> release-assets.githubusercontent.com
  res.writeHead(302, {Location: 'http://127.0.0.1:' + secondPort + '/real/app.jar'});
  res.end();
});
const second = http.createServer((req, res) => {
  res.writeHead(200, {'Content-Type': 'application/java-archive'});
  res.end(markerJar);
});
api.listen(0, '127.0.0.1', () => {
  redirector.listen(0, '127.0.0.1', () => {
    second.listen(0, '127.0.0.1', () => {
      redirectorPort = redirector.address().port;
      secondPort = second.address().port;
      fs.writeFileSync(portFile, String(api.address().port));
    });
  });
});
NODE
for _ in $(seq 1 50); do
  [ -s "$PORT_FILE" ] && break
  sleep 0.2
done
[ -s "$PORT_FILE" ] || fail "mock release server failed to start"
API_PORT="$(cat "$PORT_FILE")"
echo "  mock release API on 127.0.0.1:$API_PORT"

LOG="$SMOKE_HOME/launcher.log"
# The launcher resolves its data dir from user.home — point it at the smoke home.
# No timeout here: a healthy run finishes in seconds because the marker app
# exits immediately (CI wraps the step in timeout-minutes instead; `timeout`
# is not reliably on PATH on macOS runners).
if ! "$JAVA_HOME/bin/java" \
  -Duser.home="$SMOKE_HOME" \
  "-Datw.launcher.releases-api=http://127.0.0.1:$API_PORT/api/releases/latest" \
  -cp "$LAUNCHER_JAR" com.github.javydreamercsw.Launcher > "$LOG" 2>&1; then
  echo "  launcher exited non-zero:"
  sed 's/^/    /' "$LOG" | tail -20
  fail "launcher process failed (see $LOG)"
fi

echo "  launcher output:"
sed 's/^/    /' "$LOG" | tail -8

grep -q "Download complete" "$LOG" \
  || fail "launcher did not complete the redirecting download (see $LOG)"
grep -q "Starting all-time-wrestling-rpg-99.0.0-smoke.jar" "$LOG" \
  || fail "launcher did not start the downloaded JAR"
grep -q "SMOKE_APP_STARTED" "$LOG" \
  || fail "downloaded JAR was not executed by the resolved java executable"
grep -q "NoClassDefFoundError" "$LOG" \
  && fail "launcher crashed with NoClassDefFoundError (ATW-mcwe)"
rm -rf "$SMOKE_HOME"

echo "PASS: all installer E2E assertions succeeded"
