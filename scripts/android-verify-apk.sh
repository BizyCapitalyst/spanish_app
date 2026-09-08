#!/usr/bin/env bash
set -euo pipefail

if [ "$#" -lt 3 ]; then
  echo "Usage: $0 <apk-path> <application-id> <launchable-activity> [build-tools-version]"
  exit 64
fi

APK="$1"
APP_ID="$2"
ACTIVITY="$3"
BUILD_TOOLS_VERSION="${4:-35.0.0}"

: "${ANDROID_HOME:?ANDROID_HOME must be set}"

APKSIGNER="$ANDROID_HOME/build-tools/$BUILD_TOOLS_VERSION/apksigner"
AAPT2="$ANDROID_HOME/build-tools/$BUILD_TOOLS_VERSION/aapt2"

if [ ! -f "$APK" ]; then
  echo "FAIL: APK not found: $APK"
  exit 1
fi

if [ ! -x "$APKSIGNER" ]; then
  echo "FAIL: apksigner not found: $APKSIGNER"
  exit 1
fi

if [ ! -x "$AAPT2" ]; then
  echo "FAIL: aapt2 not found: $AAPT2"
  exit 1
fi

echo "=== APK signature verification ==="
"$APKSIGNER" verify --verbose "$APK"

echo "=== APK badging ==="
BADGING="$($AAPT2 dump badging "$APK")"
printf '%s\n' "$BADGING" | head -30

if ! printf '%s\n' "$BADGING" | grep -Fq "package: name='$APP_ID'"; then
  echo "FAIL: expected application ID not found: $APP_ID"
  exit 1
fi

if ! printf '%s\n' "$BADGING" | grep -Fq "launchable-activity: name='$ACTIVITY'"; then
  echo "FAIL: expected launchable Activity not found: $ACTIVITY"
  exit 1
fi

echo "PASS: APK signature, application ID, and launcher metadata verified."
