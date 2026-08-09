package com.example.core.media

import com.example.core.audio.AudioAnalysisResult

/**
 * UI-facing view of an analysis pass.
 *
 * Field names [peakLufs] and [currentRmsLufs] predate the real loudness engine and are kept
 * so existing mastering/trimmer Composables need no rewrite. [peakLufs] actually carries a
 * peak level in dBFS, not a true LUFS figure; [currentRmsLufs] does carry integrated LUFS
 * when the analyzer produced it.
 */
data class AudioAnalysisData(
    val waveformPoints: List<Float>, // Normalized 0.0 to 1.0
    val beatMarkersMs: List<Long>,
    val durationMs: Long,
    val peakLufs: Double,
    val currentRmsLufs: Double
)

/** Below this confidence the detected tempo is not trustworthy enough to draw a beat grid. */
private const val MIN_BPM_CONFIDENCE = 0.5f

/** Guards against a nonsense tempo estimate producing an unusable grid. */
private const val MIN_PLAUSIBLE_BPM = 20.0
private const val MAX_PLAUSIBLE_BPM = 300.0

/** Keeps a long track from allocating an unbounded marker list. */
private const val MAX_BEAT_MARKERS = 10_000

/**
 * Adapts a real [AudioAnalysisResult] to the shape the UI already renders.
 *
 * Every value here comes from the analyzer. When the analyzer did not produce a section, the
 * corresponding field is empty or zero — this never substitutes a generated stand-in.
 */
fun AudioAnalysisResult.toAudioAnalysisData(): AudioAnalysisData = AudioAnalysisData(
    waveformPoints = waveform?.points.orEmpty(),
    beatMarkersMs = beatGridFrom(bpm?.bpm, bpm?.confidence, durationMs),
    durationMs = durationMs,
    peakLufs = loudness?.peakDb ?: peakDb,
    currentRmsLufs = loudness?.integratedLufs ?: rmsDb
)

/**
 * Builds an evenly spaced beat grid from the *measured* tempo. The analyzer reports a single
 * BPM value rather than individual onset timestamps, so the grid is derived arithmetically
 * from that real measurement — it is not a fabricated beat pattern. An untrustworthy or
 * missing tempo yields no markers at all.
 */
private fun beatGridFrom(bpm: Double?, confidence: Float?, durationMs: Long): List<Long> {
    if (bpm == null || confidence == null) return emptyList()
    if (!bpm.isFinite() || !confidence.isFinite()) return emptyList()
    if (confidence < MIN_BPM_CONFIDENCE) return emptyList()
    if (bpm < MIN_PLAUSIBLE_BPM || bpm > MAX_PLAUSIBLE_BPM) return emptyList()
    if (durationMs <= 0L) return emptyList()

    val beatIntervalMs = 60_000.0 / bpm
    if (beatIntervalMs <= 0.0) return emptyList()

    val markers = ArrayList<Long>()
    var beatIndex = 0
    while (markers.size < MAX_BEAT_MARKERS) {
        val positionMs = (beatIndex * beatIntervalMs).toLong()
        if (positionMs >= durationMs) break
        markers.add(positionMs)
        beatIndex++
    }
    return markers
}
