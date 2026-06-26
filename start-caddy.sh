#!/usr/bin/env bash
# Detects the current LAN IP and starts Caddy with it injected into the Caddyfile.
# Usage: ./start-caddy.sh [extra caddy args]

set -euo pipefail

LAN_IP=$(ipconfig getifaddr en0 2>/dev/null \
      || ipconfig getifaddr en1 2>/dev/null \
      || hostname -I 2>/dev/null | awk '{print $1}')

if [[ -z "$LAN_IP" ]]; then
  echo "ERROR: Could not detect LAN IP address" >&2
  exit 1
fi

echo "Starting Caddy with LAN_IP=$LAN_IP"
export LAN_IP
exec caddy run --config "$(dirname "$0")/Caddyfile" "$@"
