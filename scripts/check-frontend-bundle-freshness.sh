#!/bin/bash
# Invalidate a stale Vaadin pre-compiled frontend bundle (ATW-sjhq).
#
# src/main/bundles/prod.bundle is a local-only (gitignored) Vaadin pre-compiled
# frontend bundle. When it exists, Vaadin's production build (TaskPrepareProdBundle)
# treats the frontend as pre-compiled: it skips the full Vite build and copies the
# bundle into the jar as-is. Any view/component added to the codebase AFTER the
# bundle was last regenerated is then missing from the bundle's loadOnDemand
# chunk switch — at runtime the server requests a chunk hash the client does not
# know and the view silently breaks (dead grid, inert upload buttons, console
# spam like "window.Vaadin.setLitRenderer is not a function"). CI never hits
# this because the bundle is gitignored and CI always builds from scratch.
#
# This guard deletes prod.bundle when any hand-written source is newer than it,
# so the production build falls back to the full Vite build (same as CI).
# Vaadin regenerates the bundle afterwards when applicable.

set -u

REPO_ROOT="$(git rev-parse --show-toplevel 2>/dev/null || pwd)"
BUNDLE="$REPO_ROOT/src/main/bundles/prod.bundle"

if [ ! -f "$BUNDLE" ]; then
  exit 0
fi

NEWEST_SRC="$(find "$REPO_ROOT/src/main/java" "$REPO_ROOT/src/main/frontend" \
  \( -type d -name generated \) -prune -o \
  -type f \( -name '*.java' -o -name '*.ts' -o -name '*.tsx' -o -name '*.js' \
             -o -name '*.css' -o -name '*.html' \) -newer "$BUNDLE" -print -quit 2>/dev/null)"

for marker in "$REPO_ROOT/pom.xml" "$REPO_ROOT/package.json" "$REPO_ROOT/package-lock.json"; do
  if [ -z "$NEWEST_SRC" ] && [ -f "$marker" ] && [ "$marker" -nt "$BUNDLE" ]; then
    NEWEST_SRC="$marker"
  fi
done

if [ -n "$NEWEST_SRC" ]; then
  echo "ATW-sjhq: src/main/bundles/prod.bundle is older than ${NEWEST_SRC#$REPO_ROOT/}." >&2
  echo "ATW-sjhq: deleting the stale pre-compiled bundle so the production build" >&2
  echo "ATW-sjhq: regenerates it — a stale bundle silently omits newer views" >&2
  echo "ATW-sjhq: (broken grids, inert upload buttons in production mode)." >&2
  rm -f "$BUNDLE"
fi

exit 0
