# Changelog

## Unreleased  

### Android 1.1.0 / Desktop 1.1.0 — 2026-10-07

- New: **volume leveler** so loud ads (iVoox) stop jumping above the content. Android attaches a
  `DynamicsProcessing` compressor + limiter to the audio sessions of the apps chosen from a list
  (`VolumeLevelingService`, foreground service; default app `com.ivoox.app`), with Suave / Medio /
  Fuerte strengths (`LevelingProfile`). It only changes those apps' audio, not the phone volume.
- Sessions are discovered two ways: the standard `OPEN_AUDIO_EFFECT_CONTROL_SESSION` broadcast, and
  `dumpsys audio` parsed by `AudioSessionParser`, which needs the DUMP permission granted once:
  `adb shell pm grant com.skipadstube.app android.permission.DUMP` (the app shows this command when it is missing).
- Desktop: `leveler.js` runs a Web Audio compressor + limiter on `<audio>/<video>` at ivoox.com, toggled
  by "Nivelar volumen (iVoox)" in the popup. It deliberately skips cross-origin media without CORS,
  because attaching to those would silence playback, so it may not engage on every iVoox player.
- Verified: Android build and unit tests (new `AudioSessionParserTest`, `LevelingProfileTest`) and the 3 new
  desktop tests pass. **Not verified on a real device or in a browser yet**: whether Realme UI /
  Android 15 lets the effect attach to iVoox's session, the real `dumpsys audio` line format on that
  phone (the parser is tolerant but unconfirmed), and the compressor values (tune by ear).
- APK SHA-256: `ede4a71bc14b3b214982b5d35817ef0ed8838d1ee6a74a063b52a3cb609f268b`.
- Desktop zip SHA-256: `dd0ffef001f31cdb22bc4a10d1b3160a79b22da199ccc6f398b7a85ede1075e6`.

### Android 1.0.0 / Desktop 1.0.0 — 2026-10-03

- Add `content_ms_before_ad`: time spent watching since the previous ad ended (0 for the first
  ad of a session), so "an ad every X minutes" and "ads per hour" are directly computable from
  the average of this column — this is what the user asked for after the previous release.
- Desktop measures this precisely: only counts time the `<video>` element is actually unpaused
  (`!media.paused`), reset to not counting on every poll where it's paused. Android cannot read
  play/pause state reliably via Accessibility (no confirmed resource id for that control), so it
  counts "YouTube foreground with the player on screen and no ad" instead — this still excludes
  backgrounded-app time, but a paused video left on screen is still counted as watched, unlike
  on desktop. Documented this platform difference explicitly in both READMEs rather than
  pretending the two columns mean exactly the same thing.
- `AdStatsRecorder.update()` (Android) gained a `playerVisible` parameter so the three
  early-return/flush call sites can tell the recorder "we don't know, don't count this gap" vs.
  the normal poll path's "player is visible right now."
- Android: `content_ms_before_ad` inserted into `ad_stats.csv` right after `pod_position`. Added
  4 new `AdStatsRecorderTest` cases (now 15 total). All 43 unit tests pass. Generated
  `dist/skipadstube-1.0.0.apk` (version code 13); metadata and signature verified.
- Desktop: same column added to the CSV export and the `chrome.storage.local` row shape. Added 2
  new `content-stats.test.cjs` cases (now 18 desktop tests total, all passing). Packaged
  `dist/skipadstube-desktop-1.0.0.zip`.
- Bumped both platforms to 1.0.0 together (no behavior reason beyond the version number — this
  is still a debug-signed test build, not a Play Store / Web Store release).
- Not yet exercised on a real device/browser for this change: `content_ms_before_ad` is verified
  only by the unit tests above, with real wall-clock timing for the desktop "unpaused" tests and
  a fake clock for Android's.
- APK SHA-256: `9010d5ff4d710f537352e76222fa8f346db86954c04373b625ac78f6d3d5ad86`.
- Desktop zip SHA-256: `20fc2c5ca6a9a744b7c529baa9074edf6587fc18dd6912d11b6fd749a822ecb7`.

### Android 0.2.10 / Desktop 0.1.10 — 2026-10-03

- Add an in-app, easy-access way to get `ad_stats.csv` off the device: a "Compartir
  estadísticas de anuncios (CSV)" button in `MainActivity` launches `Intent.ACTION_SEND`
  through a new minimal `AdStatsFileProvider` (a hand-rolled `ContentProvider`, not the
  androidx FileProvider, to avoid adding a new Gradle dependency this build environment may
  not be able to download). Shows a toast instead of a broken share sheet when no stats
  have been recorded yet.
- Expand `ad_stats.csv` / the desktop CSV export with five new columns, same schema on both
  platforms: `declared_seconds` (best-effort full ad length parsed from the existing ad
  countdown/label, so "how long would this have lasted if we hadn't skipped it" is
  answerable), `time_to_skip_ms` (time from ad start to the skip control first becoming
  available; empty if it never did), `skippable` (whether a skip control ever appeared,
  independent of whether we actually clicked it — separates non-skippable ads from skippable
  ones we didn't reach in time), and `pod_position` (1-based position within a back-to-back
  run of ads — resets to 1 after a gap of more than 3s since the previous ad ended — answers
  "did only one ad play, or several in a row").
- Add `advertiser_guess`: an experimental, broader text scan of the whole player while an ad
  is confirmed active (gated on the existing ad signal, so it never touches an ordinary
  video's title or controls), filtering out known player-chrome labels and the countdown
  text. Added because `ad_label` alone was coming back empty in the user's real-world testing.
  Still **not confirmed to ever surface the advertiser's name** — may keep coming back empty,
  or pick up unrelated visible text. If it ever does surface something useful (or someone
  finds the right accessibility/DOM node via a real-ad inspection/dump), report it back so
  `DetectionRules`/the selector list can be tightened.
- New `AdStatsRecorder.parseDeclaredSeconds` (Android, pure) / `parseDeclaredSeconds` (desktop)
  parse "mm:ss" and plain-seconds patterns out of the ad label.
- Android: 12 tests in `AdStatsRecorderTest` (was 8) and 3 new `DetectionRulesTest.isAdNoise`
  tests. All 40 unit tests pass. Generated `dist/skipadstube-0.2.10.apk` (version code 12);
  package metadata and debug signature verified with aapt/apksigner.
- Desktop: 5 tests in `content-stats.test.cjs` (was 3) covering `skippable`, `pod_position`
  and `advertiser_guess`. All 16 desktop tests pass. Packaged
  `dist/skipadstube-desktop-0.1.10.zip`.
- Fixed two more stale hardcoded version strings found while touching this code: Android's
  diagnostics screen said 0.2.9 while shipping 0.2.10-era behavior, and the desktop status
  reply said 0.1.9 for the same reason.
- Not yet exercised on a real device/browser for this change: the share button, the new CSV
  columns, and `advertiser_guess` are verified only by the unit tests above.
- APK SHA-256: `f50a4e6c34521413693cc01a53e01dfb47ef8a2292108589f3f7a0a5eb9a5028`.
- Desktop zip SHA-256: `7144126d22134913f9d1976dfebfc86c4935a111884bc420a6bcdfb68856d71`.

### Android 0.2.9 / Desktop 0.1.9 — 2026-10-03

- Add local ad statistics: `YouTubeAutomationService` now records each ad occurrence (start, end,
  duration, whether our skip click fired, and the first non-empty label/description already
  classified as an ad signal) as one CSV row in `ad_stats.csv` under app-specific storage
  (`getExternalFilesDir`, falling back to `getFilesDir`). No new permission; the app still has no
  Internet access and nothing leaves the device. New `AdStatsRecorder` (pure state machine) and
  `AdStatsFile` (file sink); wired into `YouTubeAutomationService.inspect()`/`onDestroy()` so a
  pending ad is flushed if the service is destroyed or the user leaves YouTube mid-ad.
- Show a running count and the file path in the app's diagnostics (`RuntimeStatus.stats`), and
  disclose the local-only log in the main screen's description text and README.
- The `ad_label` column is best-effort: it reuses whatever text/description the existing
  `DetectionRules` ad-signal match already read (today mostly the ad countdown, e.g. "Anuncio ·
  15"). Whether YouTube ever exposes the advertiser name itself (e.g. Repsol) through that same
  node has not been confirmed on a real device; treat the column as unverified for that purpose
  until checked against a live ad.
- Add 8 unit tests for `AdStatsRecorder` (CSV row construction, label capture precedence, CSV
  quote escaping, skip-click flag, consecutive ads). All 33 unit tests pass (25 previous + 8 new).
- Generated `dist/skipadstube-0.2.9.apk` (version code 11); package metadata (`com.skipadstube.app`,
  versionName 0.2.9, only `MODIFY_AUDIO_SETTINGS`) and the debug signature were verified with aapt
  and apksigner. Not yet installed or exercised on a real device, so the CSV file has not been
  observed being written from an actual ad.
- APK SHA-256: `7d5cc490efa93362222fcf49a3cc8f683b11472939ad886824930877fc419903`.
- Fix the Android diagnostics screen showing a stale "Versión 0.2.7" string regardless of the
  actual build (unrelated pre-existing bug, touched while adding the stats line).

### Desktop 0.1.9 — 2026-10-03

- Mirror the Android ad-statistics feature: `content.js` now times each ad from `beginAd` to
  `endAd`, captures whether our own browser-input skip click was confirmed sent during that ad,
  and a best-effort label from `.ytp-ad-text` / `.ytp-ad-simple-ad-badge` / `.ytp-ad-preview-text`
  (same caveat as Android: not confirmed to ever carry the advertiser's name). Sends one row per
  finished ad to the background service worker.
- New `recordAdStat` in `background.js` appends rows to `chrome.storage.local` (key `adStats`),
  capped at the 5000 most recent rows; no new permission, nothing leaves the browser.
- Add a "Descargar estadísticas (CSV)" button to the popup that reads `chrome.storage.local` and
  downloads `skipadstube-ad-stats.csv` with the same columns as the Android file
  (`start,end,duration_ms,skipped,ad_label`), via a Blob URL — no `downloads` permission needed.
- Fix the popup status line reporting a stale `version: '0.1.7'` regardless of the actual build
  (unrelated pre-existing bug, touched while adding the new message type).
- Add 5 regression tests (`content-stats.test.cjs`, plus two in `background.test.cjs`) covering a
  finished ad's row shape, an ad that is never clicked, an ad that never ends, `recordAdStat`
  appending, and the 5000-row cap. All 14 desktop tests pass (`node --test desktop/tests/*.test.cjs`).
- Not yet loaded in a real Chrome/Edge profile for this change; the download button and storage
  writes are exercised only by the new unit tests.
- Packaged `dist/skipadstube-desktop-0.1.9.zip`.
- Desktop zip SHA-256: `49600f70efaa2eaa9426c6b55f6d793506decb0a59738890caa26e28a672e7a5`.

### Android 0.2.8 / Desktop 0.1.8 — 2026-09-16

- Replace the app and browser-extension icon with the new "AD skip" artwork (assets/logo.png), regenerated at all Android launcher densities (mdpi–xxxhdpi, standard and foreground) and desktop extension sizes (16/32/48/128px) via `scripts/generate-icons.ps1`. No behavior changes.
- All 25 unit tests pass (unaffected by this change). Generated `dist/skipadstube-0.2.8.apk` (version code 10); package metadata and APK contents verified to include the updated launcher icons.
- APK SHA-256: `fa848df9746d35bd189a02551cc4dc651c6f77425c3ec1afaa0e89e2b112d4a2`.

### Android 0.2.7 — 2026-09-15

- Stop treating generic advertising labels, informational controls and text mentioning skip actions as sufficient evidence of a playing ad. Require exact skip labels, playback-related IDs or structured ad counters/timers.
- Scan the refreshed YouTube player every 500 ms; ignore sponsored recommendation cards outside the player. Recognize the real device's `ad_progress_text` marker and remove the 1.4-second mute hold after ad signals disappear.
- Realme's audio framework rejected background absolute-volume and mute calls. Fall back to public volume-step adjustments; retain the original volume until restoration succeeds instead of discarding it after a rejected call.
- On a Realme RMX3851 running Android 15, observed two consecutive ads at media volume 0 and subsequent content restored to its saved volume (2/16). Ordinary video playback with sponsored recommendations remained unmuted. This verifies system audio state, not an external recording; other devices and ad layouts remain unverified.
- Add regression tests for rejected audio writes, retrying restoration and ordinary content detection. Soft chimes remain inaudible according to user feedback and are not claimed fixed.
- All 25 unit tests pass. Generated and installed `dist/skipadstube-0.2.7.apk` (version code 9); package metadata and APK signature verified. Automatic skipping re-enabled after the full-ad test.
- APK SHA-256: `5c19c9f3bdedd02aebb7a89f683fb2c86830202f2fe93aaa1b7992f0ed2a5497`.

### Android 0.2.6 — 2026-09-15

- Fix the chime initialization regression introduced in 0.2.4: static AudioTrack starts in STATE_NO_STATIC_DATA, so load PCM before requiring STATE_INITIALIZED. The old pre-write check threw IllegalStateException and prevented playback.
- Include the error message in sound diagnostics to distinguish creation, data loading and playback failures.
- APK build and signature verified; 16 existing unit tests pass. These tests do not exercise Android audio hardware; audible playback still requires phone verification. Ad-muting behavior remains unchanged from 0.2.5.

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
