#!/usr/bin/env bash
# Generates and previews the documentation site locally.
# Mirrors the CI assemble_docs job so screenshots and videos render correctly.
# Serves at http://localhost:4173 when complete.
set -euo pipefail
cd "$(dirname "$0")/.."

# 1. Sync local videos into the VitePress public folder so <video> tags resolve.
#    Mirrors the CI step that downloads the docs-videos artifact to docs/.
if [ -d "docs/videos" ] && compgen -G "docs/videos/*.mp4" > /dev/null 2>&1; then
  mkdir -p docs/site/public/videos
  cp docs/videos/*.mp4 docs/site/public/videos/
  COUNT=$(ls docs/videos/*.mp4 | wc -l | tr -d ' ')
  echo "Synced $COUNT video(s) to docs/site/public/videos/"
else
  echo "No local videos found in docs/videos/ — video sections will be empty."
fi

# 2. Sync challenge content, then generate guide .md files, sidebar, and copy screenshots.
#    GITHUB_PAGES_BASE tells the scripts to emit local asset URLs for the preview server.
echo "Syncing challenge content..."
GITHUB_PAGES_BASE=http://localhost:4173 node scripts/sync-challenge-content.cjs
echo "Running generate-docs.cjs..."
GITHUB_PAGES_BASE=http://localhost:4173 node scripts/generate-docs.cjs

# 3. Rebuild with BASE_URL=/ so all internal links resolve on localhost:4173.
#    This overwrites the embedded-app build from step 2.
echo "Rebuilding VitePress for local preview..."
cd docs/site
BASE_URL=/ npm run docs:build

# 4. Free port 4173 if a previous preview server is still running.
if lsof -ti:4173 > /dev/null 2>&1; then
  echo "Killing existing process on port 4173..."
  lsof -ti:4173 | xargs kill -9
fi

# 5. Launch preview server.
echo ""
echo "Opening docs preview at http://localhost:4173"
npm run docs:preview
