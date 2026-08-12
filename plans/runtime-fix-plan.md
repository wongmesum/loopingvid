# LoopingVid Runtime Fix Plan

## Root Cause Summary

The ~1.6 MB uniform output bug has **THREE concurrent causes**:

1. **CROSSFADE is the default loop style**, and `buildCrossfadeLoopCommand` generates an **invalid FFmpeg filter graph** — it uses `xfade` (requires 2 inputs) with only 1 `-i`. FFmpeg fails immediately after writing partial output.
2. **Return code from `ffmpegWrapper.execute()` is IGNORED** at `MediaProcessor.kt:170`. The code proceeds to mark the job COMPLETED and export regardless.
3. **MediaStore export is unconditional** — partial/corrupt files get published to the gallery.

Additional issues: `.coerceAtLeast(1.2)` falsifies reported file size; "Start Render" button opens an Export settings dialog instead of starting FFmpeg directly; no output duration validation; no unique session temp file.

---

## Fix Phases (TDD, sequential)

### Phase 1: Fix CROSSFADE command (THE root cause of 1.6 MB)

**Problem:** `buildCrossfadeLoopCommand` uses `xfade` with 1 input. `xfade` needs 2 separate streams.

**Fix:** Replace the invalid xfade approach with a valid single-input loop strategy that achieves a crossfade effect:
- Use `-stream_loop N` + `-t` (same as NORMAL) for the looping
- Add a crossfade effect using `split` + `xfade` on the same stream (offset at loop boundary), OR
- Simpler: use `-stream_loop` with a blend filter at loop points

**Simplest correct approach:** Since `-stream_loop` already creates seamless concatenation, the crossfade "smoothness" between loops requires:
```
-stream_loop N -i input -t target -vf "fade=t=in:st=0:d=CF,fade=t=out:st=TARGET-CF:d=CF" -c:v libx264 ...
```
This applies fade-in at start and fade-out at end, giving the perception of smooth looping. NOT a true crossfade between segments, but avoids the invalid 2-input problem.

**Better correct approach:** Use concat demuxer or dual-input with same file to do real xfade between last-N-seconds and first-N-seconds. But this is complex and out of scope for a bugfix. The NORMAL path (stream_loop + truncate) is correct and produces valid output.

**Decision:** For this bugfix, make CROSSFADE use the same `-stream_loop N -t target` approach as NORMAL, but add fade-in/fade-out filters at loop points. If the user wants a true crossfade between loop iterations, that's a feature enhancement, not a bugfix.

**Files:** `FFmpegWrapper.kt` (the `FFmpegCommandBuilder` object)

**Tests (RED first):**
- `buildCrossfadeLoopCommand` produces args containing `-stream_loop`
- `buildCrossfadeLoopCommand` produces args containing `-t` with target duration
- `buildCrossfadeLoopCommand` does NOT produce an `xfade` filter (since single-input)
- Command args have exactly 1 `-i` occurrence followed by a valid input path

---

### Phase 2: Capture & handle FFmpeg return code

**Problem:** `MediaProcessor.kt:170` discards the return value of `execute()`.

**Fix:**
```kotlin
val returnCode = ffmpegWrapper.execute(command) { p -> ... }
if (returnCode != 0) {
    // Delete partial output
    outputFile.takeIf { it.exists() }?.delete()
    // Update job status
    val failedJob = initialJob.copy(id = insertedId, status = "FAILED", progress = 0)
    repository.updateJob(failedJob)
    _progressState.value = JobProgressState(
        jobId = insertedId,
        isProcessing = false,
        errorMessage = "FFmpeg render failed (exit code $returnCode)"
    )
    return@withContext failedJob
}
```

**Also handle trim return code** at line 125-131 — already partially handled (checks `trimResult == 0`) but doesn't abort if trim fails. Should abort with FAILED status.

**Files:** `MediaProcessor.kt`

**Tests (RED first):**
- When `ffmpegWrapper.execute()` returns non-zero → job status = FAILED
- When execute returns non-zero → partial output deleted
- When execute returns non-zero → MediaStore export NOT called
- When execute returns 0 → job status = COMPLETED, output exists

---

### Phase 3: Post-render output validation

**Problem:** No validation of output file after FFmpeg reports success.

**Fix:** After return code == 0, validate:
1. `outputFile.exists()` — must be true
2. `outputFile.length() > 0` — must be true
3. Probe output duration via `probeDurationSec(outputFile.absolutePath)`
4. Compare `abs(actualDuration - targetDuration) <= 0.5` (500ms tolerance)
5. If validation fails → mark FAILED, delete output, do NOT export

**Remove `.coerceAtLeast(1.2)`** — report actual file size.

**Files:** `MediaProcessor.kt`

**Tests (RED first):**
- Output file missing after "success" → FAILED
- Output file 0 bytes → FAILED
- Output duration 30.2s for target 30s → PASS (within 500ms)
- Output duration 10s for target 30s → FAILED
- `fileSizeMb` reflects actual file size (no forced minimum)

---

### Phase 4: Render State sealed interface

**Problem:** Only `isProcessing: Boolean` + progress int. No distinction between Idle/Preparing/Rendering/Success/Failed/Cancelled.

**Fix:** Create `RenderState` sealed interface:
```kotlin
sealed interface RenderState {
    data object Idle : RenderState
    data object Preparing : RenderState
    data class Rendering(val progress: Int, val processedMs: Long, val targetMs: Long) : RenderState
    data class Validating(val outputPath: String) : RenderState
    data class Success(val outputPath: String, val durationMs: Long, val fileSizeBytes: Long, val resolution: String) : RenderState
    data class Failed(val message: String, val returnCode: Int? = null) : RenderState
    data object Cancelled : RenderState
}
```

Wire into `MediaProcessor.progressState` (replace or supplement `JobProgressState`). 

**Backward compatibility:** Keep `JobProgressState` for DB/existing consumers but add `renderState: StateFlow<RenderState>` alongside.

**Files:** New `RenderState.kt`, modify `MediaProcessor.kt`

**Tests (RED first):**
- State transitions: Idle → Preparing → Rendering → Success
- State transitions: Idle → Preparing → Rendering → Failed
- State transitions: Rendering → Cancelled
- Progress calculation: 50s processed / 100s target = 50%
- Progress: negative → 0; over target → 100; target zero → safe (no divide-by-zero)

---

### Phase 5: Fix "Start Render" flow (render first, export after)

**Problem:** `LoopScreen.kt:687` — button calls `exportViewModel.showDialogForLoop()` which opens dialog. FFmpeg only starts after user confirms dialog settings.

**Required flow:** Start Render → FFmpeg to app-private temp → Progress → Validation → Result Screen → user chooses Save/Export.

**Fix:**
1. Change button onClick from `exportViewModel.showDialogForLoop(...)` to `viewModel.startRenderJob()`
2. `MediaProcessor.executeLoopJob` renders to `context.cacheDir/render/loop_render_<sessionId>.mp4` (unique temp path)
3. Remove the unconditional `MediaStoreExporter.exportVideoToGallery()` call from `executeLoopJob`
4. After Success state, show result card with Putar/Simpan/Bagikan
5. Only when user presses "Simpan" → call MediaStore export (copy temp → gallery)

**Files:** `LoopScreen.kt`, `MediaProcessor.kt`, `LoopViewModel.kt`

**Tests (RED first):**
- `startRenderJob()` does NOT call `showDialogForLoop`
- Output path is in app cache/render directory
- Output filename contains session ID (unique)
- `MediaStoreExporter` is NOT called during render
- Export only triggered by explicit user action after Success

---

### Phase 6: Export State separation

**Problem:** Render and export are conflated into one job.

**Fix:** Add `ExportState` sealed interface:
```kotlin
sealed interface ExportState {
    data object Idle : ExportState
    data object ChoosingDestination : ExportState
    data class Exporting(val progress: Int) : ExportState
    data class Success(val destinationUri: String) : ExportState
    data class Failed(val message: String) : ExportState
}
```

Add export function in LoopViewModel:
```kotlin
fun exportRenderedOutput(destinationFolder: String? = null) {
    // Copy from temp render file → MediaStore
}
```

**Files:** New `ExportState.kt`, modify `LoopViewModel.kt`, modify `LoopScreen.kt`

**Tests (RED first):**
- Export from Idle → error (nothing to export)
- Export from Success → copies temp file to MediaStore
- Cancel destination picker → keeps render result
- New render replaces old temp file

---

### Phase 7: Diagnostic logging

**Problem:** No way to see actual FFmpeg command at runtime.

**Fix:** Add DEBUG-level logging in `MediaProcessor.executeLoopJob`:
```kotlin
Timber.d("RENDER_DIAG sessionId=%s inputUri=%s targetDuration=%.3f loopCount=%d style=%s outputPath=%s",
    sessionId, inputUri, targetDurationSec, loopPlan.streamLoopCount, loopStyle, outputFile.absolutePath)
Timber.d("RENDER_DIAG command=%s", command.joinToString(" "))
```

After render:
```kotlin
Timber.d("RENDER_RESULT sessionId=%s returnCode=%d actualDuration=%.3f targetDuration=%.3f fileBytes=%d",
    sessionId, returnCode, actualDuration, targetDurationSec, outputFile.length())
```

**No credentials, no sensitive data.**

**Files:** `MediaProcessor.kt`

---

### Phase 8: Cancel support & cleanup

**Problem:** Cancel partially works but doesn't clean up temp output.

**Fix:**
- On cancel: delete partial temp output file
- State → Cancelled
- Never export partial output
- Button shows "Batalkan" during render
- After cancel, UI returns to ready state (can start new render)

**Files:** `MediaProcessor.kt`, `LoopViewModel.kt`

**Tests (RED first):**
- Cancel during render → state Cancelled
- Cancel → partial file deleted
- Cancel → no export triggered
- New render after cancel → new session ID

---

## Build Gates (after all phases)

1. `:app:testDebugUnitTest` — ALL tests pass (existing 300 + new)
2. `:app:lintDebug` — no new errors
3. `:app:assembleDebug` — BUILD SUCCESSFUL
4. APK stays arm64-v8a only with FFmpeg native libs

## Constraints

- Do NOT change codec from current (no libx264→other, no GPL addition)
- Do NOT change ABI (arm64-v8a only)
- Do NOT change signing config
- Do NOT tag v1.1.0 or any new tag
- Do NOT force push
- Commit only after root cause fix has tests
- All 300 existing tests must keep passing

## Execution Order

Phase 1 → Phase 2 → Phase 3 → Phase 4 → Phase 5 → Phase 6 → Phase 7 → Phase 8

Phase 1 is the critical path (fixes the 1.6 MB root cause). Phase 2 is the second-most-critical (prevents silent failures). Both must have regression tests before commit.
