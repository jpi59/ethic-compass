#!/bin/sh
set -eu

manifest='app/src/main/AndroidManifest.xml'
if grep -q 'android.permission.INTERNET' "$manifest"; then
    echo 'Unexpected Internet permission' >&2
    exit 1
fi
if grep -q 'android.permission.ACCESS_' "$manifest"; then
    echo 'Unexpected location permission' >&2
    exit 1
fi
if ! grep -q 'android.permission.VIBRATE' "$manifest"; then
    echo 'Expected opt-in haptic permission is missing' >&2
    exit 1
fi
if ! grep -q 'android:usesCleartextTraffic="false"' "$manifest"; then
    echo 'Cleartext traffic must be disabled' >&2
    exit 1
fi
echo 'Manifest privacy checks passed.'
