# Phase 7: Unified Audio Analysis — Implementation Plan

## One-liner
Add a centralized audio analysis service with persistent caching so waveform/BPM/loudness is computed once per file and shared across modules.

## Context
- Recovered commit `fba8461` has good logic in `AudioAnalysisService` but `AudioAnalysisRepository` is broken (Gson absent, infinite `Flow.collect`, dead cache code)
- `VisualizerViewModel` currently does full PCM decode via `PcmDecoder` every time a track is loaded — no caching
- `MasteringViewModel.onAudioSelected()` calls `WaveformAnalyzer.generateSimulatedWaveform()` — always fake data
- Project has no Gson; uses Moshi. DataStore pattern established in Phase 6 (`PresetRepository`)

## Success Criteria (from stabilization plan)
- Audio analysis runs once per file
- Results cached and reused
- Faster module switching (no redundant decode)

## File-by-File Implementation Order

### 1. `app/src/main/java/com/example/core/audio/AudioAnalysisService.kt` (~280 LOC)
**Responsibility:** Pure analysis engine — waveform, spectrum, BPM, loudness from a URI.
**Port from:** `fba8461` verbatim (no bugs found in service itself).
**Changes from recovered:** None — service is clean. Remove the internal `cache` map (caching moves to repository).

### 2. `app/src/main/java/com/example/core/audio/AudioAnalysisRepository.kt` (~100 LOC)
**Responsibility:** DataStore-backed cache layer around `AudioAnalysisService`.
**Fixes vs recovered commit:**
- Gson → Moshi (with `KotlinJsonAdapterFactory`)
- Constructor-inject `DataStore<Preferences>` (same pattern as `PresetRepository`)
- `getCachedResult()`: use `dataStore.data.first()[key]` instead of broken `Flow.collect`
- Secondary constructor `(context: Context)` for production use

### 3. `app/src/test/java/com/example/core/audio/AudioAnalysisServiceTest.kt` (~80 LOC)
**Responsibility:** Unit tests for pure analysis functions (waveform generation, BPM detection, loudness).
**Approach:** Extract testable pure functions; feed synthetic PCM arrays directly. No Robolectric needed for pure math — use plain JUnit + `@Config(sdk = [35])` only if Context is required.

### 4. `app/src/test/java/com/example/core/audio/AudioAnalysisRepositoryTest.kt` (~70 LOC)
**Responsibility:** Cache round-trip tests — store result, retrieve from cache, verify no re-analysis.
**Pattern:** Same as `PresetRepositoryTest` — isolated `PreferenceDataStoreFactory.create()` per test with unique filenames.

### 5. `app/src/main/java/com/example/feature/visualizer/VisualizerViewModel.kt` (MODIFY ~20 lines)
**Change:** Add optional `audioAnalysisRepository: AudioAnalysisRepository? = null` constructor param. In `analyzeBeats()`, try repository first for BPM; fall back to existing `PcmDecoder` path if null or low confidence. Backward-compatible — existing call site passes null until wired.

### 6. `app/src/main/java/com/example/MainActivity.kt` (MODIFY ~5 lines)
**Change:** Instantiate `AudioAnalysisRepository(applicationContext)` and pass to `VisualizerViewModel`.

## Dependency Graph
```
(1) AudioAnalysisService  ← no deps on project code except Timber
(2) AudioAnalysisRepository  ← depends on (1), DataStore, Moshi
(3) AudioAnalysisServiceTest  ← depends on (1)
(4) AudioAnalysisRepositoryTest  ← depends on (2)
(5) VisualizerViewModel mod  ← depends on (2)
(6) MainActivity mod  ← depends on (2), (5)
```

## What is NOT in scope
- Room entity for analysis cache (DataStore is sufficient for MVP; Room migration adds risk)
- `MasteringViewModel` integration (can be Phase 7b — it currently uses simulated data which is fine for mastering preview)
- Modifying `WaveformAnalyzer` existing API (it's used by `MasteringViewModel`)
- `ExportPresetSelector.kt` (555 LOC dead code from recovered commit — excluded)

## Tech Decisions
- **DataStore over Room** for cache: analysis results are keyed by URI string, serialized as JSON — same shape as preset cache. Avoids a Room migration on an existing 6-entity database.
- **Moshi over Gson**: Gson is not in `libs.versions.toml`; Moshi already used for Retrofit and Phase 6 presets.
- **Optional constructor param** over mandatory DI: project has no Hilt; nullable param keeps backward compat and makes tests trivial.
- **Keep PcmDecoder fallback**: `AudioAnalysisService` uses raw `MediaExtractor.readSampleData()` which gives encoded bytes on some codecs. `PcmDecoder` uses proper `MediaCodec` decode pipeline. The service's BPM detection is a fast-path for common formats; `PcmDecoder` remains the robust fallback.

## Estimated LOC
- New: ~530 (service 280 + repo 100 + tests 150)
- Modified: ~25 (ViewModel + MainActivity)
- Total: ~555 — within single-session budget, no split needed
