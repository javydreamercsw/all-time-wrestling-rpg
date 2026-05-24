#!/usr/bin/env bash
# Generates .checksums manifest files for released Flyway migration scripts.
# Called by the release workflow after .released is updated.
# Output: db/migration/{h2,mysql}/.checksums  — one line per file: "sha256  filename"
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

generate() {
  local dir="$ROOT/src/main/resources/db/migration/$1"
  local released_file="$dir/.released"
  local output="$dir/.checksums"

  if [[ ! -f "$released_file" ]]; then
    echo "ERROR: $released_file not found" >&2
    exit 1
  fi

  local released
  released=$(cat "$released_file" | tr -d '[:space:]')
  local released_num
  released_num=$(echo "$released" | grep -o '[0-9]*')

  echo "Generating checksums for $1 (released: $released)..."

  # Clear output
  > "$output"

  for f in "$dir"/V*__*.sql; do
    [[ -f "$f" ]] || continue
    local vnum
    vnum=$(basename "$f" | grep -o 'V[0-9]*' | grep -o '[0-9]*')
    if [[ "$vnum" -le "$released_num" ]]; then
      # Format: sha256hex  relative-filename (relative to migration dir)
      sha256sum "$f" | awk -v base="$(basename "$f")" '{print $1 "  " base}'
    fi
  done | sort -t'V' -k2 -V >> "$output"

  local count
  count=$(wc -l < "$output" | tr -d ' ')
  echo "  Written $count checksums to $output"
}

generate "h2"
generate "mysql"
