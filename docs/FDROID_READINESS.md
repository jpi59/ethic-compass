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

## Required before a merge request

1. Review `NOTICE.md`, the final application name and package ID for trademark
   and rights conflicts. This record does not certify trademark clearance.
2. Build, lint, inspect the release manifest and test on a physical device. A
   compatible-sensor test is recorded in `TESTING.md`; graceful behaviour on a
   device without compatible sensors remains pending.
3. Create and push an immutable annotated Git tag that identifies the exact source
   revision. Do not publish a binary merely to request F-Droid inclusion.
4. Submit a separate, reviewable `fdroiddata` merge request.

No CI pass, metadata file or local build guarantees acceptance or publication.
