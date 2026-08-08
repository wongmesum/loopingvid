# Changelog

All notable changes to LoopingVid are documented in this file.

## [1.1.0-rc1] - 2026-08-08 (Release Candidate)

Stabilization release. The focus was making existing features real and verifiable
rather than adding new surface area.

### Security

- Output paths derived from user input are now sanitized before reaching the
  filesystem, so a crafted file name or destination folder can no longer escape
  the intended export directory.
- Automatic backup (`adb backup`) is disabled, which closes extraction of the
  local database from a connected device.
- RTMP stream keys and the Gemini API key are masked in Settings, with an
  explicit show/hide toggle.
- RTMP server URLs are validated before a live session starts, and malformed
  stream keys are no longer persisted.
- Corrected the privacy policy, which previously claimed encrypted credential
  storage that did not exist.

### Added

- Device Diagnostics screen reporting real encoder, memory, storage, and thermal
  state, replacing four hardcoded hardware claims in Settings.
- Unified audio analysis service with a versioned cache shared across features.
- Export presets and a preset repository.
- Beat-sync editing: grid snapping, quantization, and an editable beat timeline.
- Slideshow transitions, Ken Burns motion, and text overlay.
- JaCoCo coverage reporting, wired into CI.

### Fixed

- Locale-dependent number formatting in FFmpeg command construction, which
  produced invalid commands on locales using comma decimal separators.
- Audio decode, BPM detection, and cache invalidation defects in the analysis
  path.
- Project lifecycle tracking now flows through the export engine.
- Removed placeholder buttons that only showed a toast, and standardized error
  message language.

### Known Limitations

- The local database is not encrypted at rest. Disabling backup blocks
  extraction over adb, but the data remains readable on a rooted device.
- Code shrinking and obfuscation (R8) remain disabled for release builds.
  ProGuard rules were drafted for Room, Moshi, Retrofit/OkHttp, Firebase, and
  FFmpegKit, but enabling R8 needs an on-device runtime pass before it ships,
  since misconfigured keep rules typically fail at runtime, not compile time.
- `abiFilters` targets `armeabi-v7a` and `x86` only. There is no `arm64-v8a`
  output, which Google Play requires.
- Coverage is low. Much of the logic lives inside composables, which the unit
  test suite cannot reach without an emulator.
- Screen rendering is verified by unit tests and static analysis only. No
  on-device runtime pass has been recorded for this release.
