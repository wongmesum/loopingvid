# Manual Loop Duration — Execution Plan

## One-liner

Make the already-existing manual duration UI actually control output length, by deriving loop
repetition from real source duration and capping output with an explicit FFmpeg `-t`.

## Audit finding (verified, read-only)

The UI is not the gap. `LoopScreen.kt:539-598` already provides manual input, +/-10s steps,
and presets 15s..3600s. The break is downstream:

| Layer | File:line | Defect |
|---|---|---|
| Loop count | `MediaProcessor.kt:149` | `(targetDurationSec / 10).toInt()` assumes every source is 10s |
| Normal command | `FFmpegWrapper.kt:112-134` | no `-t`; output length = source length x repeats |
| Crossfade command | `FFmpegWrapper.kt:136-183` | no `-t`; target only shifts xfade offset |
| Validation | `LoopViewModel.kt:136-146` | accepts 0, negative, NaN, Infinity |
| Mute | `MediaProcessor.kt:59` | `muteAudio` accepted, never reaches the command |
| Progress | `FFmpegWrapperImpl.kt:168-174` | reads `-t`; absent, so loop progress is wrong |

Net effect: requesting 137s yields `-stream_loop 13` and an output whose length depends on the
source, never 137s.

## Strategy

Correctness comes from `-t`, not from the repeat count. `-t` truncates output at the exact
target, so the repeat count only has to be *at least* enough. That makes the feature correct
even when duration probing fails.

- repeats needed = `ceil(target / segment)`; `-stream_loop` takes N *extra* passes, so emit `N-1`.
- `target <= segment` gives `-stream_loop 0` plus `-t target`, which trims shorter-than-source.
- probe failure falls back to a generous repeat count derived from a minimum assumed segment;
  `-t` still guarantees exact length.

## Acceptance criteria

1. `planLoop(segment=10.0, target=137.0)` returns `streamLoopCount=13`, `outputDurationSec=137.0`.
2. `planLoop(segment=60.0, target=15.0)` returns `streamLoopCount=0`, `outputDurationSec=15.0`.
3. `buildNormalLoopCommand` emits `-t` `137.000` and retains `-stream_loop`.
4. `buildCrossfadeLoopCommand` emits `-t` `137.000`.
5. `-t` is formatted with `Locale.US`, so a comma-decimal host locale cannot corrupt it.
6. `muteAudio = true` emits `-an` and no `-c:a`.
7. `setTargetDuration` rejects `0.0`, negatives, `NaN`, `Infinity`, and clamps to 1..3600.
8. `testDebugUnitTest`, `lintDebug`, and `assembleDebug` pass.
9. Release APK still packages `arm64-v8a` only.

## Phases

### Phase 1 — failing tests (RED)

Goal: encode the contract before implementation.
Files: `app/src/test/java/com/example/core/ffmpeg/LoopDurationPlannerTest.kt` (new),
`app/src/test/java/com/example/core/ffmpeg/FFmpegCommandBuilderLoopTest.kt` (new).
Done: both fail for the intended reason, not import/setup noise.

### Phase 2 — pure planner (GREEN)

Goal: `LoopDurationPlanner` with validation constants and `planLoop`.
Files: `app/src/main/java/com/example/core/ffmpeg/LoopDurationPlanner.kt` (new, < 90 LOC).
Done: `LoopDurationPlannerTest` green. No Android dependency, so it runs on JVM.

### Phase 3 — command builders

Goal: thread an explicit output duration and mute flag into both loop commands.
Files: `app/src/main/java/com/example/core/ffmpeg/FFmpegWrapper.kt` (modify).
Done: `FFmpegCommandBuilderLoopTest` green.

### Phase 4 — processor wiring

Goal: probe real segment duration via `MediaMetadataRetriever`, plan, pass `-t` + mute.
Files: `app/src/main/java/com/example/core/ffmpeg/MediaProcessor.kt` (modify).
Handoff: depends on Phase 2 and 3 being committed-green first.
Done: full unit suite green.

### Phase 5 — validation

Goal: reject non-finite and out-of-range input at the state boundary.
Files: `app/src/main/java/com/example/feature/loop/LoopViewModel.kt` (modify).
Done: validation test green.

### Phase 6 — verify

Goal: `testDebugUnitTest`, `lintDebug`, `assembleDebug`; re-inspect release ABI.
Done: all pass, evidence recorded.

## Edge cases

- Probe returns 0 or fails: fall back to a generous repeat count; `-t` still caps exactly.
- Trim active: the trimmed temp file becomes the segment, so it is probed instead of the source.
- Trim silently failing already falls back to the original input; unchanged, reported as a risk.
- Target below one frame: floor at 1s via the validator.
- Comma-decimal locale: `-t` uses `Locale.US`, matching the existing `buildPreciseTrimCommand` fix.

## Out of scope

- Repairing the structurally broken `xfade` graph, which supplies one input to a two-input
  filter (`FFmpegWrapper.kt:162-174`). It gets `-t` but stays otherwise untouched.
- Implementing `audioFadeInSec` / `audioFadeOutSec`, still ignored.
- `PING_PONG` is a forward repeat, not a real ping-pong; unchanged.
- Unchecked FFmpeg exit code in `executeLoopJob`; unchanged.
- No push, no tag, no merge.

## Dependencies

`MediaMetadataRetriever` (platform), MockK and `kotlinx-coroutines-test` (already configured
in `app/build.gradle.kts:63,128-145`).

## Risks

Native FFmpeg still cannot be executed on this x86_64 host, so `-t` semantics are proven by
command-shape assertions, not by a rendered file. That limit is reported, not hidden.
