# Changelog

## Unreleased  

### Android 0.2.5 — 2026-09-15

- Request explicit media-stream mute in addition to setting its volume to zero. Restore the preceding volume and mute state after the ad; preserve audio that was already muted.
- Show Android's reported media mute state alongside the last detected ad. Retain independent accessibility-channel chimes and sound preview changes from 0.2.4.
- All 16 unit tests pass, including ignored volume writes with successful explicit mute and preservation of pre-existing mute. APK build, metadata and signature verified; effectiveness on the user's phone remains pending feedback.

### Installation guidance

- Clarify that no floating button or overlay is required; document disabling Android's accessibility shortcut while keeping the service enabled.

### Android 0.2.4 — 2026-09-15

- Move chimes from system sonification to the independent accessibility audio channel when connected; use media audio for standalone previews. Increase the quiet PCM gain and report playback preparation errors.
- Request accessibility volume control and additional accessible views. Add local service, detection, last-ad volume and sound diagnostics without recording screen content.
- Show the relevant sound volume and route the activity volume keys to it. Re-enable the accessibility service after updating to load the new flags.
- Build and signature verified; 14 unit tests pass. The reported real-device muting failure is still under investigation; this build exposes evidence needed to distinguish missing detection from blocked or differently routed audio.

### Android 0.2.3 — 2026-09-15

- Check YouTube every 500 ms instead of repeatedly delaying end checks when accessibility events arrive. Reapply muting while an ad remains detected and restore volume when leaving YouTube.
- Recognize duplicated ad labels in text/description and Spanish ad counters/timers; ignore hidden nodes and disabled skip buttons.
- Apply audio preferences immediately. Play the start chime only after verifying media volume is zero, and the end chime after restoration. Disabling muting restores volume without an end-of-ad chime.
- Add a sound preview button and a scrollable settings screen; preserve the existing mute, skip and soft-chime preferences. Sound-output failures no longer crash the chime thread.
- Build `dist/skipadstube-0.2.3.apk` (version code 5). All 14 unit tests pass; APK metadata and signature verified. No device was connected; real-ad validation remains pending.
- APK SHA-256: `19017429709ec5fa57de5bc1313823d793c3451fb532e9fe93101dab783fe785`.

### Android 0.2.2 / Desktop 0.1.7 — 2026-09-15

- Use the supplied logo for the Android launcher, Chrome extension icons and extension popup.
- Preserve the original artwork in `assets/logo.png`; generate platform icon sizes with `scripts/generate-icons.ps1`.
- Add an adaptive Android icon with padding for launcher masks. Android version code is now 4.
- Rebuild `dist/skipadstube-0.2.2.apk`; Android unit tests and desktop regression checks pass. APK signature and icon metadata verified; device testing remains pending.
- APK SHA-256: `390a514af2f1a5e3d5dab60466c774d7a47efaba8373f5a383227e5c72e16934`.

### Documentation

- Add a short Spanish Android installation guide covering APK installation, Accessibility activation, restricted settings, background operation and the previous app identity.

### Android 0.2.1 — 2026-09-15

- Rebuild the current Android source as `dist/skipadstube-0.2.1.apk`, with display name skipadstube, application ID `com.skipadstube.app`, version 0.2.1 and version code 3.
- Fix resource-ID matching to recognize Android's `:id/` separator for skip buttons and ad indicators.
- Keep namespace, SDK levels and version metadata in Gradle; use the app name string resource for the application label.
- Add the Gradle 8.9 wrapper for repeatable builds.
- Validation: five Android unit tests pass; APK metadata and signature verified. This is a debug-signed test build; device testing is pending.
- APK SHA-256: `35d8add39597c969c5bfa71703bbf0d5b966dc7abe3cb07043df94e8b58fe6ec`.

### Project naming

- Standardize current source, Java package directories, Android namespace/application ID and preference keys on skipadstube.
- Android 0.2.1 includes the renamed identifiers and installs as a separate app; historical APK contents remain unchanged.

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
