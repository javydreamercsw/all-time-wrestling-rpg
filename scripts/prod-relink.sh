#!/bin/bash
# Re-establish symlinks after a Homebrew upgrade, and restart Tomcat when the
# Homebrew JDK changes. Canonical copy lives in scripts/prod-relink.sh; the
# deployed instance is /opt/homebrew/etc/tomcat/relink.sh (loaded by the
# com.atwrpg.relink-tomcat LaunchAgent, see prod-relink-launchd.plist).
#
# Idempotent: only restart Tomcat when something actually changed. A bare
# launchctl kickstart -k here restarts the server on EVERY WatchPath fire
# (this agent is triggered by WatchPaths on /opt/homebrew/opt/tomcat), which
# killed user sessions whenever anything touched that tree — including
# transient temp files the app writes into java.io.tmpdir. That is also why
# CATALINA_TMPDIR lives outside the watched tree, in /opt/homebrew/etc/tomcat/temp.

setenv_link=/opt/homebrew/opt/tomcat/libexec/bin/setenv.sh
webapps_link=/opt/homebrew/opt/tomcat/libexec/webapps
jdk_link=/opt/homebrew/opt/openjdk
jdk_state="$HOME/.atwrpg/openjdk-target"
changed=0

# Symlink setenv.sh
if [ "$(readlink "$setenv_link" 2>/dev/null)" != "/opt/homebrew/etc/tomcat/setenv.sh" ]; then
    ln -sf /opt/homebrew/etc/tomcat/setenv.sh "$setenv_link"
    changed=1
fi

# Symlink webapps directory
# Remove the default webapps directory first if it's not a symlink
if [ ! -L "$webapps_link" ]; then
    rm -rf "$webapps_link"
fi
if [ "$(readlink "$webapps_link" 2>/dev/null)" != "/opt/homebrew/etc/tomcat/webapps" ]; then
    ln -sfn /opt/homebrew/etc/tomcat/webapps "$webapps_link"
    changed=1
fi

# Detect a Homebrew OpenJDK upgrade. A running JVM keeps its core native
# libraries mapped even after Homebrew deletes their files, but the AWT
# natives load lazily on first use — a Tomcat process running against a
# deleted JDK keeps serving until the first QR-code render, then fails with
# "UnsatisfiedLinkError: no awt in system library path" pointing at the
# vanished directory. Compare the current opt symlink target against the one
# recorded on the previous pass; only a recorded-and-different target counts,
# so the very first run (no state yet) seeds silently instead of bouncing
# Tomcat for nothing.
jdk_target=$(readlink "$jdk_link" 2>/dev/null)
jdk_prev=""
[ -f "$jdk_state" ] && jdk_prev=$(cat "$jdk_state")
if [ -n "$jdk_target" ] && [ -n "$jdk_prev" ] && [ "$jdk_target" != "$jdk_prev" ]; then
    changed=1
fi

# Restart the service only if something actually changed. State is recorded
# after the restart decision: if this script dies between kickstart and the
# write, the next fire still sees the old target and retries (one extra
# bounce, converges).
if [ "$changed" -eq 1 ]; then
    launchctl kickstart -k "gui/$(id -u)/tomcat"
fi
if [ -n "$jdk_target" ]; then
    mkdir -p "$(dirname "$jdk_state")"
    printf '%s\n' "$jdk_target" > "$jdk_state"
fi

# Start Caddy reverse proxy if the config is present (provides HTTPS over LAN for QR code sharing)
# Use absolute paths: launchd runs this without the Homebrew PATH (caddy used to fail with 127).
# Export LAN_IP first: the Caddyfile uses {$LAN_IP} placeholders (site block +
# default_sni), and this agent does not inherit the env setenv.sh gives Tomcat.
LAN_IP=$(ipconfig getifaddr en0 2>/dev/null || ipconfig getifaddr en1 2>/dev/null)
[ -n "$LAN_IP" ] && export LAN_IP

CADDYFILE=/opt/homebrew/etc/tomcat/Caddyfile
if [ -f "$CADDYFILE" ]; then
    /opt/homebrew/bin/caddy stop 2>/dev/null || true
    /opt/homebrew/bin/caddy start --config "$CADDYFILE"
fi
