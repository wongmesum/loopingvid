# Checkpoint 2: Unified Audio Analysis Integration

## Ground truth (verified by reading code directly)

- `core/audio/AudioAnalysisService.kt` + `AudioAnalysisRepository.kt` — real analyzer, already built (Fase 7), already used by `VisualizerViewModel` (wired in `MainActivity.kt:75`). Cache via DataStore, versioned.
- Result model: `AudioAnalysisResult(durationMs, sampleRate, channelCount, waveform: WaveformData?, spectrum: SpectrumData?, bpm: BpmData?, loudness: LoudnessData?, peakDb, rmsDb)`.
  - `BpmData(bpm: Double, confidence: Float)` — **no beat-onset list**, only a single tempo estimate + confidence.
- Legacy model still used by mastering/trimmer UI: `core/media/WaveformAnalyzer.kt` → `AudioAnalysisData(waveformPoints, beatMarkersMs, durationMs, peakLufs, currentRmsLufs)` + `generateSimulatedWaveform()` (fake, still called from `MasteringViewModel`/`AudioMasteringViewModel` per Checkpoint 1 placeholder comments — currently returns `null` instead after Checkpoint 1).
- `VisualizerViewModel.requestFullAnalysis()` has zero callers; `fullAnalysis` StateFlow has zero consumers — dead wiring from Fase 7.
- `Media3SegmentTrimmer` used to self-generate fake analysis; Checkpoint 1 removed the generator call but left `audioAnalysisData` permanently null — beat-snap now silently dead.

## Decision: keep `AudioAnalysisData` shape, source it from the real repository

Rewriting `AudioMasteringComponent.kt` (1900+ lines) to a new model is a large, risky diff for a stabilization branch. Minimal targeted fix: keep the existing `AudioAnalysisData` shape UI code already renders, but produce it from `AudioAnalysisResult` via a mapper — never from a generator. `WaveformAnalyzer.generateSimulatedWaveform()` is deleted; `AudioAnalysisData` data class stays (moved into a new file, since its old file was named after the generator being deleted).

Beat markers: real `BpmData` gives one tempo value, not onset timestamps. Deriving an evenly-spaced grid from **measured** BPM (`60000/bpm` step across `durationMs`) is legitimate — it's built from a real detected value, not fabricated. This is the same approach already used by the Fase 8 beat-sync grid-snap feature. Comment will say so explicitly to keep `SimulationGuardTest` intent honest.

`peakLufs`/`currentRmsLufs` naming predates real loudness metrics. Real `LoudnessData` has `integratedLufs`, `peakDb`, `rmsDb`. Map `currentRmsLufs = loudness.integratedLufs` (true LUFS) and `peakLufs = loudness.peakDb` (documented in the mapper as dBFS peak, not true LUFS — the field name is legacy and not renamed here to keep the diff small; a follow-up phase can rename if desired).

## File-by-file plan (bottom-up)

1. **New: `app/src/main/java/com/example/core/media/AudioAnalysisMapper.kt`**
   - Moves `data class AudioAnalysisData` here (out of `WaveformAnalyzer.kt`).
   - Adds `fun AudioAnalysisResult.toAudioAnalysisData(): AudioAnalysisData`, mapping fields as above. Returns empty waveform list / 0 beat markers when `waveform`/`bpm` is null — no fabrication.

2. **Delete: `app/src/main/java/com/example/core/media/WaveformAnalyzer.kt`**
   - Drop `generateSimulatedWaveform()` entirely. `AudioAnalysisData` already relocated in step 1.

3. **`app/src/main/java/com/example/feature/mastering/MasteringViewModel.kt`**
   - Constructor gains `private val audioAnalysisRepository: AudioAnalysisRepository? = null` (nullable + default so existing call sites without a repository still compile; `MainActivity` passes the real one).
   - `onAudioSelected(uri, fileName)`: keep current immediate state update, then launch `viewModelScope.launch { audioAnalysisRepository?.getOrAnalyze(Uri.parse(uri))?.onSuccess { result -> _uiState.update { it.copy(analysisData = result.toAudioAnalysisData()) } } }`.
   - Update import: drop `WaveformAnalyzer`, add `AudioAnalysisMapper`/`AudioAnalysisRepository`.

4. **`app/src/main/java/com/example/feature/mastering/AudioMasteringViewModel.kt`**
   - Same pattern: nullable repository constructor param, `loadAudioTrack()` launches real analysis, maps to `analysisData`.

5. **`app/src/main/java/com/example/core/ui/Media3SegmentTrimmer.kt`**
   - Add optional param `audioAnalysisRepository: AudioAnalysisRepository? = null`.
   - Replace the now-empty `LaunchedEffect(totalDurationMs)` placeholder with a real fetch keyed on `mediaUri`, guarded so it only runs when a repository is supplied and a URI exists.
   - No change to rendering code — it already reads `audioAnalysisData?.waveformPoints`/`beatMarkersMs`.

6. **`app/src/main/java/com/example/MainActivity.kt`**
   - Pass the existing `audioAnalysisRepository` instance into `MasteringViewModel`, `AudioMasteringViewModel`, and wherever `Media3SegmentTrimmer` is instantiated with a real media URI.

7. **`app/src/main/java/com/example/feature/visualizer/VisualizerViewModel.kt`**
   - Out of scope for this checkpoint to add a caller — no screen currently needs the full analysis panel. Leave as-is; note it as a tracked gap in the plan doc, not a fabricated fix.

8. **Tests**
   - `app/src/test/java/com/example/core/SimulationGuardTest.kt`: replace the "no `generateSimulatedWaveform` call" check (now trivially true) with an explicit assertion that `WaveformAnalyzer.kt` no longer exists, and that `MasteringViewModel`/`AudioMasteringViewModel`/`Media3SegmentTrimmer` reference `AudioAnalysisRepository`/`AudioAnalysisMapper` instead.
   - Update/remove any test in `LoopingVidTest.kt` (or wherever) that calls `generateSimulatedWaveform()` directly.
   - New focused test: `AudioAnalysisMapperTest` — real `AudioAnalysisResult` fixture in → correct `AudioAnalysisData` out, including the null-waveform/null-bpm empty-list case.

## Out of scope for Checkpoint 2
- Renaming `peakLufs`/`currentRmsLufs` to non-LUFS-accurate names.
- Wiring `VisualizerViewModel.requestFullAnalysis()` to a screen.
- Editor's real-time spectrum (`Media3SpectrumAudioProcessor`) — already real, untouched; only its DEBUG-only fake animation fallback (fixed in Checkpoint 1) is related, and stays as-is.
- Any change to `flutter_app/`.

## Verification
1. `:app:compileDebugKotlin`
2. `:app:testDebugUnitTest`
3. `:app:assembleDebug`
4. Commit only after all three are green: `feat(audio): wire mastering and trimmer to real AudioAnalysisRepository`
5. Push `release/1.1.0-stabilization` only after commit.
