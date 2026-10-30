#!/usr/bin/env bash
# Invoked as a single command by android-emulator-runner (it runs each script: line in a fresh shell).
set -euo pipefail

SUITE="${1:?suite name required (smoke|features|reader|providers)}"
ROOT="${GITHUB_WORKSPACE:-$(cd "$(dirname "$0")/../.." && pwd)}"
E2E_DIR="$ROOT/e2e"
APK="$E2E_DIR/apps/QuickNovel-Ultimate.apk"
APPIUM_HOST="${APPIUM_HOST:-127.0.0.1}"
APPIUM_PORT="${APPIUM_PORT:-4723}"
APPIUM_LOG="$E2E_DIR/appium-ci.log"
APPIUM_PID=""

cleanup() {
  if [ -n "${APPIUM_PID}" ] && kill -0 "${APPIUM_PID}" 2>/dev/null; then
    kill "${APPIUM_PID}" 2>/dev/null || true
    wait "${APPIUM_PID}" 2>/dev/null || true
  fi
}
trap cleanup EXIT

adb wait-for-device
adb shell settings put global window_animation_scale 0
adb shell settings put global transition_animation_scale 0
adb shell settings put global animator_duration_scale 0

test -f "$APK"
test -f "$E2E_DIR/package.json"
test -f "$APK" && ls -lh "$APK"

cd "$E2E_DIR"

# External Appium avoids WDIO appium-service + worker IPC races on GH runners
npx appium --address "$APPIUM_HOST" --port "$APPIUM_PORT" --relaxed-security \
  --allow-insecure adb_shell --log-level warn >"$APPIUM_LOG" 2>&1 &
APPIUM_PID=$!

echo "Waiting for Appium on ${APPIUM_HOST}:${APPIUM_PORT} (pid ${APPIUM_PID})..."
for _ in $(seq 1 60); do
  if curl -sf "http://${APPIUM_HOST}:${APPIUM_PORT}/status" >/dev/null; then
    echo "Appium is up"
    break
  fi
  if ! kill -0 "${APPIUM_PID}" 2>/dev/null; then
    echo "Appium exited early; log:"
    cat "$APPIUM_LOG" || true
    exit 1
  fi
  sleep 1
done
curl -sf "http://${APPIUM_HOST}:${APPIUM_PORT}/status" >/dev/null

export APP_PATH="$APK"
export CI=true
export WDIO_LOG_LEVEL="${WDIO_LOG_LEVEL:-warn}"
export APPIUM_EXTERNAL=1
export APPIUM_HOST
export APPIUM_PORT

# One worker for the whole suite (see nested arrays in wdio.conf suites)
./node_modules/.bin/wdio run ./wdio.conf.js --suite "$SUITE"
