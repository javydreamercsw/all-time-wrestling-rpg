#!/bin/sh
# Update local knowledge graphs after a commit or branch switch.
# Best effort: graph maintenance must never block Git operations.

set +e

ROOT=$(git rev-parse --show-toplevel 2>/dev/null) || exit 0
cd "$ROOT" || exit 0

LOCK_DIR=$(git rev-parse --git-path knowledge-graph.lock 2>/dev/null)
if [ -z "$LOCK_DIR" ]; then
  exit 0
fi
if ! mkdir "$LOCK_DIR" 2>/dev/null; then
  echo "[knowledge-graph] update already running; skipping" >&2
  exit 0
fi
trap 'rmdir "$LOCK_DIR" 2>/dev/null || true' EXIT HUP INT TERM

LOG_DIR="${HOME:-.}/.cache"
mkdir -p "$LOG_DIR" 2>/dev/null || true
LOG="$LOG_DIR/atw-knowledge-graph-update.log"

run_with_timeout() {
  _seconds=$1
  shift
  if command -v timeout >/dev/null 2>&1; then
    timeout "$_seconds" "$@"
    return $?
  fi
  # macOS has no timeout by default. Python's subprocess timeout is portable.
  _python=$(command -v python3 2>/dev/null || command -v python 2>/dev/null)
  if [ -n "$_python" ]; then
    "$_python" - "$_seconds" "$@" <<'PY'
import subprocess
import sys

seconds = int(sys.argv[1])
try:
    completed = subprocess.run(sys.argv[2:], timeout=seconds)
    raise SystemExit(completed.returncode)
except subprocess.TimeoutExpired:
    print("[knowledge-graph] update timed out", file=sys.stderr)
    raise SystemExit(124)
PY
    return $?
  fi
  "$@"
}

{
  export PYTHONHASHSEED=0
  # Graphify is configured as the online MCP server in .mcp.json. A local
  # graphify CLI is optional and is never invoked by hooks unless explicitly
  # enabled for a developer checkout.
  if [ "${GRAPHIFY_LOCAL_UPDATE:-0}" = "1" ] && command -v graphify >/dev/null 2>&1 && [ -d graphify-out ]; then
    echo "[knowledge-graph] updating optional local graphify graph"
    run_with_timeout 600 graphify update . || echo "[knowledge-graph] local graphify update failed"
  fi
  if command -v code-review-graph >/dev/null 2>&1 && [ -d .code-review-graph ]; then
    echo "[knowledge-graph] updating code-review-graph"
    run_with_timeout 300 code-review-graph update --skip-flows --repo "$ROOT" || \
      echo "[knowledge-graph] code-review-graph update failed"
    if [ "${CODE_REVIEW_GRAPH_EMBED:-0}" = "1" ]; then
      run_with_timeout 300 code-review-graph embed --repo "$ROOT" || \
        echo "[knowledge-graph] code-review-graph embed failed"
    fi
  fi
} >>"$LOG" 2>&1
