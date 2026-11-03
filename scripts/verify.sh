#!/usr/bin/env bash
set -euo pipefail

if [[ -z "${ANDROID_HOME:-}" && -z "${ANDROID_SDK_ROOT:-}" ]]; then
  echo "Android SDK not configured. Set ANDROID_HOME or ANDROID_SDK_ROOT." >&2
  exit 2
fi

# Runs only on a developer machine with a configured Android SDK.
./gradlew --no-daemon :app:testDebugUnitTest :app:lintDebug :app:assembleDebug
echo "Unit tests, Android Lint, and debug APK build completed."
