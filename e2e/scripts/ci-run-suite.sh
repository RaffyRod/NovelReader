#!/usr/bin/env bash
# Invoked as a single command by android-emulator-runner (it runs each script: line in a fresh shell).
# Suites: smoke | general | gates | features | … — "gates" = smoke then general on one AVD boot.
set -euo pipefail

SUITE="${1:?suite name required (smoke|general|gates|features|features-plugins|features-flows|reader|providers)}"
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

echo "Waiting for emulator boot..."
BOOT=""
for _ in $(seq 1 120); do
  BOOT="$(adb shell getprop sys.boot_completed 2>/dev/null | tr -d '\r' || true)"
  if [ "$BOOT" = "1" ]; then
    echo "boot_completed=1"
    break
  fi
  sleep 2
done
if [ "${BOOT:-}" != "1" ]; then
  echo "Emulator did not report boot_completed=1"
  adb shell getprop 2>/dev/null | grep -E 'boot|init' || true
  exit 1
fi

# Wait until package manager answers (avoids racing a half-dead system after snapshot load).
echo "Waiting for package manager..."
PM_OK=0
for _ in $(seq 1 60); do
  if adb shell pm path android >/dev/null 2>&1; then
    PM_OK=1
    echo "pm path android OK"
    break
  fi
  sleep 2
done
if [ "$PM_OK" != "1" ]; then
  echo "Package manager never became ready"
  adb devices -l || true
  exit 1
fi

# Unlock + settle — never fail the suite on input inject flakes (DeadSystemException).
adb shell input keyevent 82 >/dev/null 2>&1 || true
adb shell input keyevent 3 >/dev/null 2>&1 || true
adb shell settings put global window_animation_scale 0 >/dev/null 2>&1 || true
adb shell settings put global transition_animation_scale 0 >/dev/null 2>&1 || true
adb shell settings put global animator_duration_scale 0 >/dev/null 2>&1 || true
sleep 3

test -f "$APK"
test -f "$E2E_DIR/package.json"
ls -lh "$APK"

# Pre-install so session create is not the first APK push (still may reinstall with noReset:false).
echo "Pre-installing APK..."
adb uninstall com.lagradost.quicknovel.ultimate >/dev/null 2>&1 || true
adb uninstall com.lagradost.quicknovel.ultimate.debug >/dev/null 2>&1 || true
INSTALL_OK=0
for attempt in 1 2 3; do
  if adb install -r -g "$APK"; then
    INSTALL_OK=1
    break
  fi
  echo "adb install attempt $attempt failed; retrying..."
  sleep 5
done
if [ "$INSTALL_OK" != "1" ]; then
  echo "adb install failed after retries"
  exit 1
fi
adb shell pm path com.lagradost.quicknovel.ultimate

cd "$E2E_DIR"

# External Appium avoids WDIO appium-service + worker IPC races on GH runners
npx appium --address "$APPIUM_HOST" --port "$APPIUM_PORT" --relaxed-security \
  --allow-insecure adb_shell --log-level info >"$APPIUM_LOG" 2>&1 &
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
# WDIO v9 wraps workers in xvfb-run when DISPLAY is empty; on Ubuntu 26.04
# that closes the Node IPC fd → write EINVAL (webdriverio/webdriverio#15685).
# Appium/Android does not need X11 — any non-empty DISPLAY forces plain fork.
export DISPLAY="${DISPLAY:-:99}"

run_wdio() {
  local name="$1"
  echo "===== WDIO suite: ${name} ====="
  set +e
  ./node_modules/.bin/wdio run ./wdio.conf.js --suite "$name"
  local rc=$?
  set -e
  if [ "$rc" -ne 0 ]; then
    echo "===== appium-ci.log (tail) ====="
    tail -n 120 "$APPIUM_LOG" || true
    echo "===== adb devices ====="
    adb devices -l || true
  fi
  return "$rc"
}

# Stash Allure files so a second suite does not wipe the first shard.
stash_allure() {
  local shard="$1"
  mkdir -p "allure-shards/${shard}"
  if [ -d allure-results ] && [ "$(ls -A allure-results 2>/dev/null || true)" ]; then
    cp -a allure-results/. "allure-shards/${shard}/"
  fi
  rm -rf allure-results
  mkdir -p allure-results
}

merge_allure_shards() {
  mkdir -p allure-results
  if [ -d allure-shards ]; then
    find allure-shards -type f -exec cp {} allure-results/ \;
  fi
}

FINAL_EXIT=0
if [ "$SUITE" = "gates" ]; then
  # One AVD + Appium: smoke gate, then general (saves a full cold-boot job).
  if ! run_wdio smoke; then
    FINAL_EXIT=1
  else
    stash_allure smoke
    if ! run_wdio general; then
      FINAL_EXIT=1
    fi
    stash_allure general
    merge_allure_shards
  fi
else
  if ! run_wdio "$SUITE"; then
    FINAL_EXIT=1
  fi
fi

exit "$FINAL_EXIT"
