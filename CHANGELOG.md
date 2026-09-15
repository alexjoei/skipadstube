# Changelog

## Unreleased  

### Project naming

- Standardize current source, Java package directories, Android namespace/application ID and preference keys on skipadstube.
- Android requires a new build and installs as a separate app; historical APK contents remain unchanged.

### Desktop 0.1.6

- User confirmed browser-level skipping works in Chrome and requested it as the default.
- Remove the experimental mode switch and ineffective synthetic input path. The skipAds setting alone controls skipping; old local mode preferences are ignored.
- Retain active YouTube tab checks, fresh button coordinates and debugger cleanup.

### Desktop 0.1.5 (experimental)

- Declare YouTube-only host access so the browser-input guard can read the tab URL.
- Distinguish missing site access, inactive tabs and navigation outside YouTube.
- The previous tests supplied tab URLs unconditionally and missed Chrome withholding this property.

### Desktop 0.1.4 (experimental)

- Add an off-by-default browser-input mode for skip buttons that ignore scripted events.
- Requires the powerful Chrome debugger permission at extension load time; Chrome does not support requesting it as an optional permission.
- Restrict requests to top-level YouTube content scripts and active YouTube tabs, recheck button coordinates, throttle requests and detach after each attempt.
- Report browser-input errors in the popup. Live YouTube success remains unverified.

### Desktop 0.1.3

- Send pointer/mouse press and release events before clicking the visible skip-button child at its center.
- Avoid clicks through overlays and stop when the control disappears during the sequence.
- No new permissions. These remain synthetic events; this does not establish that YouTube accepts them.
- Regression coverage includes event order, overlay blocking and child targets. Live YouTube verification is pending.

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

Android source: skipadstube-Android-0.2.0-source.zip
Desktop source: skipadstube-Desktop-0.1.0.zip
Historical APK: skipadstube-0.2.0.apk
APK SHA-256: 7d2e04719b115a88cf9e28529b9f087b087c197192dcf31a8bb429629d250ba4

The historical APK has not been rebuilt or device-tested in this workspace.
