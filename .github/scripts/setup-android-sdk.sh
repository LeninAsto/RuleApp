#!/usr/bin/env bash
set -euo pipefail

# GitHub's Ubuntu runner already includes the SDK and command-line tools.
# The retired "tools" package must not be requested from sdkmanager.
sdk_root="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-}}"
if [[ -z "$sdk_root" ]]; then
  echo 'Android SDK location is missing (ANDROID_HOME/ANDROID_SDK_ROOT).' >&2
  exit 1
fi

sdk_manager="$sdk_root/cmdline-tools/latest/bin/sdkmanager"
if [[ ! -x "$sdk_manager" ]]; then
  sdk_manager="$(command -v sdkmanager || true)"
fi
if [[ -z "$sdk_manager" || ! -x "$sdk_manager" ]]; then
  echo 'sdkmanager is missing from the runner SDK and PATH.' >&2
  exit 1
fi

export ANDROID_HOME="$sdk_root"
export ANDROID_SDK_ROOT="$sdk_root"
# Process substitution avoids a SIGPIPE from yes masking sdkmanager's status.
"$sdk_manager" --sdk_root="$sdk_root" --licenses < <(yes)
"$sdk_manager" --sdk_root="$sdk_root" 'platform-tools' 'platforms;android-36' 'build-tools;36.0.0'

if [[ -n "${GITHUB_ENV:-}" ]]; then
  printf 'ANDROID_HOME=%s\nANDROID_SDK_ROOT=%s\n' "$sdk_root" "$sdk_root" >> "$GITHUB_ENV"
fi
