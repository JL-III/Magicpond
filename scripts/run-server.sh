#!/usr/bin/env bash
#
# Build Magicpond and launch a local Paper test server with the plugin installed.
#
# Usage:
#   bash scripts/run-server.sh          # build the plugin, then start the server
#   mvn -Prun verify                    # same thing, driven from Maven / your IDE
#
# Everything lives in a gitignored ./run directory, so it's safe to delete to
# get a clean server. Each Paper build is cached there and only downloaded once.
#
# Configurable via environment variables:
#   PAPER_VERSION   Paper/Minecraft version           (default: 26.1.2)
#                   May also be given as "<version>-<build>", e.g. 26.1.2-69.
#   PAPER_BUILD     Exact build number to pin          (default: 69; empty = latest)
#   PAPER_API       Downloads API base URL            (default: https://fill.papermc.io/v3)
#   RUN_DIR         Working directory for the server   (default: run)
#   SKIP_BUILD      Set to "true" to skip `mvn package` (default: false)
#   JAVA_OPTS       Extra JVM flags                    (default: -Xms1G -Xmx2G)
#   MVN             Maven executable                   (default: mvn)
#
set -euo pipefail

PAPER_VERSION="${PAPER_VERSION:-26.1.2}"
PAPER_BUILD="${PAPER_BUILD:-69}"
PAPER_API="${PAPER_API:-https://fill.papermc.io/v3}"
RUN_DIR="${RUN_DIR:-run}"
SKIP_BUILD="${SKIP_BUILD:-false}"
JAVA_OPTS="${JAVA_OPTS:--Xms1G -Xmx2G}"
MVN="${MVN:-mvn}"

# Accept a combined "<version>-<build>" form (e.g. PAPER_VERSION=26.1.2-69).
if [ -z "$PAPER_BUILD" ] && [[ "$PAPER_VERSION" =~ ^(.+)-([0-9]+)$ ]]; then
  PAPER_VERSION="${BASH_REMATCH[1]}"
  PAPER_BUILD="${BASH_REMATCH[2]}"
fi

# Resolve the project root (this script lives in <root>/scripts).
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
cd "$PROJECT_DIR"

command -v curl >/dev/null 2>&1 || { echo "[run] ERROR: curl is required to download Paper." >&2; exit 1; }

# 1. Build the plugin (unless the caller already did).
if [ "$SKIP_BUILD" != "true" ]; then
  echo "[run] Building plugin with $MVN ..."
  "$MVN" -q -DskipTests package
fi

# Locate the built plugin jar (skip the shade plugin's original-/sources/javadoc jars).
PLUGIN_JAR="$(ls -t target/*.jar 2>/dev/null | grep -Ev 'original-|-sources|-javadoc' | head -n1 || true)"
if [ -z "$PLUGIN_JAR" ]; then
  echo "[run] ERROR: no plugin jar found in target/. Did the build succeed?" >&2
  exit 1
fi

mkdir -p "$RUN_DIR/plugins"

# 2. Download the requested Paper build (PaperMC Fill v3 API) if not already cached.
#    In v3 the download URL is read from the API response, not constructed by hand.
BUILDS_JSON=""
fetch_builds() {
  [ -n "$BUILDS_JSON" ] && return 0
  BUILDS_JSON="$(curl -fsSL -H 'User-Agent: magicpond-run-script' \
    "$PAPER_API/projects/paper/versions/$PAPER_VERSION/builds")" \
    || { echo "[run] ERROR: failed to query $PAPER_API/projects/paper/versions/$PAPER_VERSION/builds" >&2; return 1; }
}

# The v3 builds endpoint may return a bare array or a {"builds":[...]} object;
# normalize to the array either way.
JQ_NORM='if type=="array" then . else .builds end'

latest_build_from_json() {
  if command -v jq >/dev/null 2>&1; then
    printf '%s' "$BUILDS_JSON" | jq -r "$JQ_NORM | [.[].id] | max"
  else
    printf '%s' "$BUILDS_JSON" | grep -oE '"id"[[:space:]]*:[[:space:]]*[0-9]+' \
      | grep -oE '[0-9]+' | sort -n | tail -n1
  fi
}

download_url_from_json() {
  if command -v jq >/dev/null 2>&1; then
    printf '%s' "$BUILDS_JSON" \
      | jq -r --argjson b "$PAPER_BUILD" "$JQ_NORM | .[] | select(.id == \$b) | .downloads.\"server:default\".url"
  else
    # No jq: pull the URL straight out of the JSON by its jar name (don't construct it).
    printf '%s' "$BUILDS_JSON" \
      | grep -oE 'https://[^"]+/paper-'"$PAPER_VERSION"'-'"$PAPER_BUILD"'\.jar' | head -n1
  fi
}

# If a build is pinned and already cached, skip the network entirely.
PAPER_JAR=""
if [ -n "$PAPER_BUILD" ]; then
  PAPER_JAR="$RUN_DIR/paper-$PAPER_VERSION-$PAPER_BUILD.jar"
fi

if [ -z "$PAPER_JAR" ] || [ ! -f "$PAPER_JAR" ]; then
  fetch_builds || exit 1

  if [ -z "$PAPER_BUILD" ]; then
    PAPER_BUILD="$(latest_build_from_json || true)"
    if [ -z "$PAPER_BUILD" ] || [ "$PAPER_BUILD" = "null" ]; then
      echo "[run] ERROR: no builds found for Paper $PAPER_VERSION." >&2
      echo "[run] Browse versions/builds at $PAPER_API/projects/paper" >&2
      exit 1
    fi
    PAPER_JAR="$RUN_DIR/paper-$PAPER_VERSION-$PAPER_BUILD.jar"
  fi

  if [ ! -f "$PAPER_JAR" ]; then
    DL_URL="$(download_url_from_json || true)"
    if [ -z "$DL_URL" ] || [ "$DL_URL" = "null" ]; then
      echo "[run] ERROR: Paper $PAPER_VERSION build $PAPER_BUILD not found." >&2
      echo "[run] Browse versions/builds at $PAPER_API/projects/paper" >&2
      exit 1
    fi
    echo "[run] Downloading Paper $PAPER_VERSION build $PAPER_BUILD ..."
    curl -fsSL -H 'User-Agent: magicpond-run-script' -o "$PAPER_JAR" "$DL_URL"
  fi
fi
PAPER_JAR_NAME="$(basename "$PAPER_JAR")"

# 3. Install the freshly built plugin (replacing any older copy) and accept the EULA.
echo "[run] Installing $(basename "$PLUGIN_JAR") into $RUN_DIR/plugins/"
rm -f "$RUN_DIR"/plugins/[Mm]agicpond-*.jar
cp "$PLUGIN_JAR" "$RUN_DIR/plugins/"
echo "eula=true" > "$RUN_DIR/eula.txt"

# Frictionless defaults for local testing (only written once; edit freely afterward).
if [ ! -f "$RUN_DIR/server.properties" ]; then
  cat > "$RUN_DIR/server.properties" <<EOF
# Generated by scripts/run-server.sh for local plugin testing.
online-mode=false
motd=Magicpond dev server
max-players=5
spawn-protection=0
EOF
fi

# 4. Launch (interactive console: type 'stop' or press Ctrl+C to quit).
echo "[run] Starting $PAPER_JAR_NAME ..."
cd "$RUN_DIR"
exec java $JAVA_OPTS -jar "$PAPER_JAR_NAME" nogui
