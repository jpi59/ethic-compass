# F-Droid readiness record

Last reviewed: 2026-09-10

## Scope

Ethic Compass is a new, independently implemented application with package ID
`org.jpi59.ethiccompass`. It is not a fork or rebrand of the already listed
F-Droid package `com.bobek.compass`.

Its stated distinct value is accessible orientation: a high-contrast visual
display, large heading text, a polite live region for cardinal-direction
changes, and opt-in haptic feedback. Reviewers remain responsible for deciding
whether that value meets F-Droid's inclusion policy.

## Checked in source

- GPL-3.0-or-later license and original-work provenance are documented.
- The only declared Android permission is `android.permission.VIBRATE`; it is
  used only after the user enables the setting.
- `android.permission.INTERNET` and cleartext traffic are absent.
- The app has no external Gradle dependencies.
- The build uses a pinned Gradle distribution checksum and an Android Gradle
  Plugin from Google Maven. F-Droid must independently validate its supported
  tooling when the release is proposed.

## Submission state

The metadata recipe is proposed in [F-Droid merge request
!48463](https://gitlab.com/fdroid/fdroiddata/-/merge_requests/48463). It is
open for automated checks and maintainer review; this does not certify
acceptance or publication.

## Completed preparation

1. `NOTICE.md`, the application name and package ID document the known rights
   limitation; they do not certify trademark clearance.
2. The release was built, linted and manifest-inspected, and a compatible
   sensor device test is recorded in `TESTING.md`. Graceful behaviour on a
   device without compatible sensors remains pending.
3. The public, immutable annotated tag `v1.0.1` identifies the reviewed source
   revision. No developer APK was published for F-Droid to consume.
4. The separate, reviewable `fdroiddata` merge request is open.

No CI pass, metadata file or local build guarantees acceptance or publication.
