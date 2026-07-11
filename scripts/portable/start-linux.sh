#!/bin/bash
set -euo pipefail

REPO="javydreamercsw/all-time-wrestling-rpg"
API_URL="https://api.github.com/repos/${REPO}/releases/latest"

jar_version() {
  echo "$1" | sed 's/all-time-wrestling-rpg-\([^.][^.]*\.[^.]*\.[^.]*\)\.jar/\1/'
}

is_newer() {
  [ "$(printf '%s\n' "$1" "$2" | sort -V | tail -1)" = "$1" ] && [ "$1" != "$2" ]
}

check_and_update() {
  CURRENT_JAR=$(ls all-time-wrestling-rpg-*.jar 2>/dev/null | head -n 1)
  CURRENT_VER=""
  if [ -n "$CURRENT_JAR" ]; then
    CURRENT_VER=$(jar_version "$CURRENT_JAR")
  fi

  echo "Checking for updates..."
  LATEST_JSON=$(curl -sf --max-time 15 "$API_URL" || true)
  if [ -z "$LATEST_JSON" ]; then
    echo "Could not reach GitHub — launching existing version."
    return
  fi

  LATEST_TAG=$(echo "$LATEST_JSON" | grep '"tag_name"' | sed 's/.*"v\([^"]*\)".*/\1/')
  ASSET_URL=$(echo "$LATEST_JSON" | grep '"browser_download_url"' | grep '\.jar"' | grep -v '\.war"' | head -n 1 | sed 's/.*"\(https[^"]*\)".*/\1/')

  if [ -z "$LATEST_TAG" ] || [ -z "$ASSET_URL" ]; then
    echo "Could not parse release info — launching existing version."
    return
  fi

  if [ -z "$CURRENT_JAR" ] || is_newer "$LATEST_TAG" "$CURRENT_VER"; then
    if [ -n "$CURRENT_JAR" ]; then
      read -r -p "Update v${LATEST_TAG} available. Apply now? [y/N] " REPLY
      if [[ ! "$REPLY" =~ ^[Yy]$ ]]; then
        return
      fi
    else
      echo "No application JAR found. Downloading v${LATEST_TAG}..."
    fi

    TMP_JAR="all-time-wrestling-rpg-${LATEST_TAG}.jar.tmp"
    NEW_JAR="all-time-wrestling-rpg-${LATEST_TAG}.jar"

    echo "Downloading v${LATEST_TAG}..."
    if curl -L --max-time 600 -o "$TMP_JAR" "$ASSET_URL"; then
      if unzip -t "$TMP_JAR" >/dev/null 2>&1; then
        [ -n "$CURRENT_JAR" ] && mv "$CURRENT_JAR" "${CURRENT_JAR}.old"
        mv "$TMP_JAR" "$NEW_JAR"
        CURRENT_JAR="$NEW_JAR"
        echo "Update applied: v${LATEST_TAG}"
      else
        echo "Downloaded file is corrupt — keeping existing version."
        rm -f "$TMP_JAR"
      fi
    else
      echo "Download failed — launching existing version."
      rm -f "$TMP_JAR"
    fi
  else
    echo "Already up to date (v${CURRENT_VER})."
  fi
}

find . -maxdepth 1 -name "*.jar.tmp" -mmin +60 -delete 2>/dev/null || true

check_and_update

JAR_FILE=$(ls all-time-wrestling-rpg-*.jar 2>/dev/null | head -n 1)
if [ -z "$JAR_FILE" ]; then
  echo "No application JAR found. Exiting."
  exit 1
fi

echo "Starting All Time Wrestling RPG (${JAR_FILE})..."
java -jar "$JAR_FILE" --spring.profiles.active=prod,h2
EXIT_CODE=$?

if [ $EXIT_CODE -ne 0 ]; then
  BACKUP=$(ls ./*.jar.old 2>/dev/null | head -n 1)
  if [ -n "$BACKUP" ]; then
    echo "Application crashed (exit $EXIT_CODE). Restoring previous version..."
    RESTORED="${BACKUP%.old}"
    mv "$BACKUP" "$RESTORED"
    rm -f "$JAR_FILE"
    echo "Restored. Please try launching again."
  fi
  read -n 1 -s -r -p "Press any key to exit."
  echo
fi

exit $EXIT_CODE
