# Phase 8: Complete Visualizer & Beat Sync

## State
- **Status:** Implemented, unit tests verified green
- **Branch:** `release/1.1.0-stabilization`
- **TDD:** Regression guards added before implementation where feasible. The RED/GREEN run was blocked by a tool classifier outage at the time of writing; it has since run green (see Verification Status).

## Success Criteria
- At least 10 beat effects available
- Beat grid editor / snapping functional
- Improved beat detection accuracy

## Implemented

### 1. Beat effects expanded to 10
`app/src/main/java/com/example/feature/visualizer/beat/BeatEffect.kt`
- Added `NEON_STROBE("Neon Strobe")`
- Added `WAVE_RIPPLE("Wave Ripple")`

`app/src/main/java/com/example/feature/visualizer/BeatReactivePreview.kt`
- Added Compose preview reactions for new effects.
- `NEON_STROBE`: brighter primary, pink secondary, stronger glow.
- `WAVE_RIPPLE`: subtle scale and ripple-like rotation/trail change.

`app/src/main/java/com/example/feature/visualizer/VisualizerExportCommandBuilder.kt`
- Added distinct FFmpeg filter mapping for every `BeatEffect`.
- Removed stale hard-coded "8 effects" wording.

### 2. Beat grid snapping
`app/src/main/java/com/example/feature/visualizer/beat/BeatSyncState.kt`
- Added `BeatGridDivision` with `OFF`, `BEAT`, `HALF_BEAT`, `QUARTER_BEAT`.
- Added `gridDivision` to `BeatSyncState`.
- Added `BeatGridSnapper.snap`, `BeatGridSnapper.quantize`, and `BeatGridSnapper.gridStepMs`.

`app/src/main/java/com/example/feature/visualizer/VisualizerViewModel.kt`
- Added `setBeatGridDivision`.
- Added `quantizeBeatMarkers`.
- `addBeatMarker` and `moveBeatMarker` now snap through `BeatGridSnapper` when grid is active and BPM usable.

`app/src/main/java/com/example/feature/visualizer/beat/BeatSyncControlPanel.kt`
- Added `GridDivisionSelector` chips.
- Added `Quantize markers` button.
- Button enabled only when grid step and markers exist.

`app/src/main/java/com/example/feature/visualizer/beat/BeatTimeline.kt`
- Added BPM/grid/offset parameters with defaults.
- Draws faint grid tick lines when snap grid is active.
- Uses `MAX_GRID_LINES = 400` to avoid flooding canvas on long tracks.

`app/src/main/java/com/example/feature/visualizer/VisualizerStudioScreen.kt`
- Wired grid state into `BeatTimeline`.
- Wired grid selector and quantize callback into `BeatSyncControlPanel`.

### 3. Beat detection accuracy improved
`app/src/main/java/com/example/feature/visualizer/beat/BeatDetectionEngine.kt`
- `BeatDetectionConfig.smoothing` now affects analysis through EMA over energy envelope.
- Onset detection now requires local peaks above adaptive threshold instead of marking every high-energy frame.
- Confidence now derives from inter-onset interval regularity (`stdDev / mean`) instead of marker count buckets.

## Regression Guards Added
`app/src/test/java/com/example/BeatGridSnapperTest.kt`
- OFF pass-through.
- BEAT, HALF_BEAT, QUARTER_BEAT rounding.
- Offset-aware grid snapping.
- Track-bound clamping.
- Bad BPM no-op.
- Quantize sorted/distinct.

`app/src/test/java/com/example/VisualizerExportCommandTest.kt`
- Asserts `BeatEffect.entries.size >= 10`.
- Asserts every `BeatEffect` emits enabled export filter when markers exist.

`app/src/test/java/com/example/BeatDetectionEngineTest.kt`
- Adds smoothing/noisy pulse guard.
- Existing BPM, marker count, offset, silence, threshold, band, min interval, tap BPM, and manual grid guards remain.

## Verification Status
The focused Gradle suite later completed green in this stabilization run. The earlier classifier blockage is historical context only.

## Next Step
Keep this plan as historical record only; do not treat the old blocker note as current state.