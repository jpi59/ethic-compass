# Test record

Last reviewed: 2026-09-10

## Local release checks

The following checks passed for version `1.0.1` / code `2`:

- `scripts/verify-privacy.sh`
- `./gradlew clean assembleRelease lintRelease`, using Android SDK platform 36
- APK manifest inspection with Android `aapt`

The inspected release APK declares package `org.jpi59.ethiccompass`, minimum SDK
24, target SDK 36, and only `android.permission.VIBRATE`. It is deliberately
unsigned: it is a local source-build artifact, not a developer release or an
APK submitted to F-Droid.

## Physical-device test

Date: 2026-09-10

The connected device identified itself through ADB as `moto g34 5G` (`fogos`),
Android API 35. It was not a Moto G35, despite the initial expectation.

- A debug-only APK was verified with APK Signature Scheme v2, installed, and
  launched successfully from a cold start.
- Android reported the expected package, version, and only the `VIBRATE`
  permission.
- The device exposes a rotation-vector sensor, accelerometer, and magnetometer.
  While the activity was visible, `dumpsys sensorservice` reported an active
  rotation-vector subscription for the app. After moving the activity to the
  background, there were no active direct connections; reopening it reactivated
  the subscription.
- A visual inspection found two layout defects on the first implementation:
  content was cut off at the bottom, then the title appeared beneath the status
  bar. Both were corrected and a final screenshot showed all content within the
  system bars.

## Version 1.0.1 interface update

The Moto G34 5G was updated in place with a debug build of version `1.0.1` /
code `2`. It launched successfully from a cold start. A visual inspection
confirmed an original canvas-drawn compass dial with a large central reading,
a five-degree visual scale, an explicit magnetic-north label, and a long brown
orientation pointer. This is a display scale, not a claim that every device
sensor has five-degree physical accuracy.

Both the light and dark themes were visually checked on the device. The dark
theme changes the system bars, dial, labels, pointer, and custom checkbox while
retaining visible contrast. Switching the explicit `Usar modo oscuro` control
persisted only the local `dark_mode` preference; the preference was restored to
its prior light-theme value after testing. The bright theme also showed the
optional haptic control and the dark-theme control without clipping.

The same device was then forced temporarily into landscape orientation for a
reversible test; its original autorotation settings were restored immediately
after the capture. Landscape uses a dedicated two-panel layout: heading,
offline status, haptic control, dark-theme control, and details are in the left
panel, while the complete compass remains visible in the right panel. No
clipping was observed.

## Limits and pending checks

- No physical confirmation of the haptic sensation was obtained. The checkbox
  and its local persistence were visible, but hardware feedback needs a person
  holding the device to verify it.
- TalkBack or another screen reader was not enabled, so the spoken behaviour of
  the cardinal live region is not certified.
- This is not a calibrated-heading or safety-navigation validation. Magnetic
  interference and sensor calibration can affect readings.
- The no-compatible-sensor screen has not been exercised on a device lacking
  both the rotation vector and the accelerometer/magnetometer fallback.
