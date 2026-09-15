# Changelog

## Unreleased

### Desktop 0.1.2

- Recognize exact short labels Skip, Omitir and Saltar within the player while an ad is detected.
- Show content-script version, ad detection, skip availability, setting and click attempts in the popup.
- Limit click retries to once per second; attempts do not imply successful skips.
- Ten regression tests pass. Live Chrome ad skipping still requires verification.

### Desktop 0.1.1

- Detect available skip controls without requiring the player ad-state class.
- Check all matching controls, including when a hidden duplicate comes first.
- Restrict fallback button matching to the player and reject disabled or hidden controls.
- Document reloading both the extension and the YouTube tab after updates.

- Rename Android and desktop display branding to skipadstube.
- Import desktop extension 0.1.0 alongside Android 0.2.0, preserving Android Git history.
- Preserve Android package identity and stored settings.

## Supplied baseline

Android source: TubeQuiet-Android-0.2.0-source.zip
Desktop source: TubeQuiet-Desktop-0.1.0.zip
Historical APK: TubeQuiet-0.2.0.apk
APK SHA-256: 7d2e04719b115a88cf9e28529b9f087b087c197192dcf31a8bb429629d250ba4

The historical APK has not been rebuilt or device-tested in this workspace.
