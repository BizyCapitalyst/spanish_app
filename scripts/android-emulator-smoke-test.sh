#!/usr/bin/env bash
set -euo pipefail

if [ "$#" -lt 3 ]; then
  echo "Usage: $0 <apk-path> <application-id> <activity-component> [startup-wait-seconds]"
  echo "Example: $0 app-debug.apk com.example.app .MainActivity 7"
  exit 64
fi

APK="$1"
APP_ID="$2"
ACTIVITY="$3"
WAIT_SECONDS="${4:-7}"
LOGFILE="${RUNNER_TEMP:-/tmp}/android-smoke-${APP_ID//./-}.log"
STARTFILE="${RUNNER_TEMP:-/tmp}/android-start-${APP_ID//./-}.txt"

fail_with_log() {
  local message="$1"
  echo "FAIL: $message"
  echo "=== Relevant logcat ==="
  if [ -f "$LOGFILE" ]; then
    grep -Ei -C 15 "FATAL EXCEPTION|AndroidRuntime|Process: ${APP_ID}|${APP_ID}" "$LOGFILE" | tail -300 || true
  fi
  exit 1
}

check_process_and_log() {
  local phase="$1"
  local pid

  pid="$(adb shell pidof "$APP_ID" 2>/dev/null | tr -d '\r' || true)"
  adb logcat -d -v threadtime > "$LOGFILE" || true

  if [ -z "$pid" ]; then
    fail_with_log "$phase: application process is not alive after ${WAIT_SECONDS}s"
  fi

  if grep -Fq "Process: $APP_ID" "$LOGFILE"; then
    fail_with_log "$phase: fatal Android runtime record detected"
  fi

  echo "PASS: $phase process alive; pid=$pid"
}

if [ ! -f "$APK" ]; then
  echo "FAIL: APK not found: $APK"
  exit 1
fi

echo "=== Fresh installation ==="
adb uninstall "$APP_ID" >/dev/null 2>&1 || true
adb install "$APK"

echo "=== Cold launch ==="
adb logcat -c
adb shell am start -W -n "$APP_ID/$ACTIVITY" | tee "$STARTFILE"

if ! grep -Fq "Status: ok" "$STARTFILE"; then
  fail_with_log "cold launch did not report Status: ok"
fi

sleep "$WAIT_SECONDS"
check_process_and_log "cold launch"

echo "=== Force-stop and second launch ==="
adb shell am force-stop "$APP_ID"
adb logcat -c
adb shell am start -W -n "$APP_ID/$ACTIVITY" | tee "$STARTFILE"

if ! grep -Fq "Status: ok" "$STARTFILE"; then
  fail_with_log "second launch did not report Status: ok"
fi

sleep "$WAIT_SECONDS"
check_process_and_log "second launch"

echo "PASS: fresh install, cold launch, process survival, and second launch all succeeded."
