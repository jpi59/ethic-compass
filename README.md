# Ethic Compass

Ethic Compass is an independently implemented, offline magnetic compass for
Android. It is designed for readable, high-contrast orientation and optional,
local haptic feedback when the heading reaches a cardinal direction.

## Privacy and permissions

- It has no `INTERNET`, location, microphone, camera, storage or advertising
  permission.
- It reads the device's orientation sensors only while the activity is visible.
- The optional haptic feedback setting is off by default and is stored only in
  Android's local app preferences.
- The optional dark theme is off by default and is stored only in Android's
  local app preferences.
- It uses magnetic north. It does not claim to provide true north.

## Accessibility

The current heading is rendered in large text, announced when the cardinal
direction changes, and paired with a high-contrast compass in either light or
dark theme. The drawing is created entirely in code from simple geometric
primitives; no third-party images, fonts, trackers or SDKs are included.

## Build

Build with a Java 17-compatible JDK and Android SDK platform 36:

```sh
./gradlew clean assembleRelease lintVitalRelease
```

The release build does not contain VCS or dependency metadata. The Gradle
wrapper pins both its distribution version and SHA-256 checksum.

## F-Droid status

Version `1.0.2` has a [production-signed APK and SHA-256 checksum on its GitHub
release](https://github.com/jpi59/ethic-compass/releases/tag/v1.0.2). The
certificate fingerprint and verification command are recorded there.

The metadata recipe is proposed in [F-Droid merge request
!48463](https://gitlab.com/fdroid/fdroiddata/-/merge_requests/48463). The
source tag and local checks are documented here, but F-Droid acceptance,
publication and independent reproducibility remain decisions for its build and
review process. F-Droid will build and sign an independent APK if it accepts the
recipe; it is not an update-compatible replacement for the direct APK.

## License

Copyright © 2026 jpi59. Ethic Compass is licensed under GPL-3.0-or-later; see
[LICENSE](LICENSE). No source code or visual asset from another compass project
was copied into this repository.
