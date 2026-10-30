#!/usr/bin/env bash
# Invoked as a single command by android-emulator-runner (it runs each script: line in a fresh shell).
set -euo pipefail

SUITE_SCRIPT="${1:?npm script name required (e.g. test:smoke)}"
ROOT="${GITHUB_WORKSPACE:-$(cd "$(dirname "$0")/../.." && pwd)}"
E2E_DIR="$ROOT/e2e"
APK="$E2E_DIR/apps/QuickNovel-Ultimate.apk"

adb wait-for-device
adb shell settings put global window_animation_scale 0
adb shell settings put global transition_animation_scale 0
adb shell settings put global animator_duration_scale 0

test -f "$APK"
test -f "$E2E_DIR/package.json"

export APP_PATH="$APK"
export CI=true
export WDIO_LOG_LEVEL="${WDIO_LOG_LEVEL:-warn}"

cd "$E2E_DIR"
npm run "$SUITE_SCRIPT"
