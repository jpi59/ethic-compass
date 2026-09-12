# Privacy statement

Last reviewed: 2026-09-12, against version 1.0.2 (`versionCode 3`).

Ethic Compass is designed to operate locally on the device.

- It does not request network access and has no code path that transmits data.
- Its manifest declares `VIBRATE` solely for optional local haptic feedback;
  it is not a network, location or data-access permission.
- It does not request location, microphone, camera, contacts, storage or
  notification access.
- It reads orientation sensor events only while its screen is active in order
  to calculate a magnetic heading.
- If enabled by the person using the app, local preferences record the choices
  to provide haptic direction feedback and to use dark mode. These preferences
  remain on the device and are not backed up by the app.

The app uses magnetic north, which can be affected by local interference and
device calibration. It is not a navigation or safety instrument and must not be
relied upon where an inaccurate heading could cause harm.
