#!/usr/bin/env bash
# Tomcat sources this file before startup (place in Tomcat's bin/ directory).
# Detects the current LAN IP, starts Caddy in the background, and exports
# QR_BASE_URL so the app always generates scannable QR codes.

LAN_IP=$(ipconfig getifaddr en0 2>/dev/null \
      || ipconfig getifaddr en1 2>/dev/null \
      || hostname -I 2>/dev/null | awk '{print $1}')

if [[ -n "$LAN_IP" ]]; then
  export LAN_IP
  export QR_BASE_URL="https://${LAN_IP}"

  CADDYFILE="$(dirname "$0")/../conf/Caddyfile"
  if [[ -f "$CADDYFILE" ]] && command -v caddy &>/dev/null; then
    caddy start --config "$CADDYFILE" --pidfile "$(dirname "$0")/../logs/caddy.pid" 2>/dev/null &
    echo "Caddy started with LAN_IP=${LAN_IP}"
  else
    echo "WARNING: Caddyfile not found at ${CADDYFILE} or caddy not on PATH — skipping Caddy start"
  fi
else
  echo "WARNING: Could not detect LAN IP — QR codes will fall back to localhost"
fi
