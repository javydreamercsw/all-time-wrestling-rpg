#!/usr/bin/env bash
# Delete the RC releases that belong to a final release (ATW-rvx0).
#
# When a final version like 2.10.0 ships, the RC releases for the same numeric
# version (v2.10.0-RC1, v2.10.0-RC2, ...) linger on the Releases page with
# downloadable stale installers. This script finds every release whose tag
# matches v<BASE>-RC<n> and deletes the release - keeping the git tags, which
# stay historical (javierortiz, 2026-09-13; they also hold the RC build commits
# recorded in ATW-hm95).
#
# Existing RC installs are not stranded: the launcher lists remaining releases,
# and its version ordering (2.10.0 > 2.10.0-RCn) moves RC testers to the final.
#
# Usage: cleanup-rc-releases.sh <base-version>      e.g. cleanup-rc-releases.sh 2.10.0
#        cleanup-rc-releases.sh --emit-matches <base-version> < tags.txt
#          (test seam: print the stdin tags matching the base version, delete nothing)
# Env:
#   GH_CMD  command to use instead of "gh" (tests inject a mock)
#   REPO    owner/name (passed to gh as --repo; defaults to this repository)
#
# Exits 0 when there is nothing to delete or all deletions succeed; 1 on any
# failure or bad input.

set -euo pipefail

# Echo the matching RC tag names for a list of release tags (one per line on
# stdin). The dots in the base version are escaped so 2.10.0 does not match
# v2x1x0-RCn-style strings.
rc_tags_for_base() {
  local base="$1"
  grep -E "^v${base//./\\.}-RC[0-9]+$" || true
}

if [ "${1:-}" = "--emit-matches" ]; then
  shift
  rc_tags_for_base "$1"
  exit 0
fi

if [ $# -ne 1 ]; then
  echo "Usage: $0 <base-version>   e.g. $0 2.10.0" >&2
  exit 1
fi

BASE_VERSION="$1"
# Reject an RC/prefixed version passed by mistake; the base must be bare semver.
if ! echo "$BASE_VERSION" | grep -Eq '^[0-9]+\.[0-9]+\.[0-9]+$'; then
  echo "ERROR: '$BASE_VERSION' is not a bare x.y.z version" >&2
  exit 1
fi

GH="${GH_CMD:-gh}"
REPO="${REPO:-javydreamercsw/all-time-wrestling-rpg}"

tags="$("$GH" api --repo "$REPO" "repos/$REPO/releases?per_page=100" --jq '.[].tag_name' 2>/dev/null || true)"

rc_tags="$(echo "$tags" | rc_tags_for_base "$BASE_VERSION")"

if [ -z "$rc_tags" ]; then
  echo "No RC releases for v$BASE_VERSION - nothing to clean up."
  exit 0
fi

echo "RC releases for v$BASE_VERSION to delete:"
while IFS= read -r rc_tag; do
  [ -z "$rc_tag" ] && continue
  echo "  - $rc_tag"
done <<RC_TAGS
$rc_tags
RC_TAGS

failures=0
while IFS= read -r rc_tag; do
  [ -z "$rc_tag" ] && continue
  echo "Deleting release $rc_tag (git tag is kept)..."
  if ! "$GH" release delete "$rc_tag" --repo "$REPO" --yes; then
    echo "ERROR: failed to delete release $rc_tag" >&2
    failures=$((failures + 1))
  fi
done <<EOF
$rc_tags
EOF

if [ "$failures" -gt 0 ]; then
  echo "ERROR: $failures RC release(s) could not be deleted" >&2
  exit 1
fi
echo "RC cleanup for v$BASE_VERSION complete."
