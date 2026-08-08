package com.example.feature.visualizer.beat

import kotlin.math.roundToLong

enum class BeatGridDivision(val multiplier: Int, val label: String) {
    OFF(0, "Off"),
    BEAT(1, "Beat"),
    HALF_BEAT(2, "1/2"),
    QUARTER_BEAT(4, "1/4")
}

data class BeatSyncState(
    val isAnalyzing: Boolean = false,
    val bpm: Double = 0.0,
    val durationMs: Long = 0L,
    val markersMs: List<Long> = emptyList(),
    val config: BeatDetectionConfig = BeatDetectionConfig(),
    val gridDivision: BeatGridDivision = BeatGridDivision.OFF,
    val selectedEffect: BeatEffect = BeatEffect.BASS_PULSE,
    val currentPulse: BeatPulse = BeatPulse.Idle,
    val analysisError: String? = null
) {
    val hasAnalysis: Boolean
        get() = bpm > 0.0 && markersMs.isNotEmpty()
}

object BeatMarkerEditor {
    fun add(markers: List<Long>, markerMs: Long, durationMs: Long): List<Long> {
        val safeMarker = markerMs.coerceIn(0L, durationMs.coerceAtLeast(0L))
        return (markers + safeMarker).distinct().sorted()
    }

    fun move(
        markers: List<Long>,
        markerIndex: Int,
        markerMs: Long,
        durationMs: Long
    ): List<Long> {
        if (markerIndex !in markers.indices) return markers
        val updated = markers.toMutableList()
        updated[markerIndex] = markerMs.coerceIn(0L, durationMs.coerceAtLeast(0L))
        return updated.distinct().sorted()
    }

    fun remove(markers: List<Long>, markerIndex: Int): List<Long> {
        if (markerIndex !in markers.indices) return markers
        return markers.filterIndexed { index, _ -> index != markerIndex }
    }

    fun applyOffset(markers: List<Long>, deltaMs: Long, durationMs: Long): List<Long> =
        markers.map { (it + deltaMs).coerceIn(0L, durationMs) }.distinct().sorted()
}

/**
 * Rounds beat markers onto a tempo grid so hand-placed markers line up with the
 * beat instead of sitting a few dozen milliseconds off.
 *
 * Snapping deliberately does nothing when the tempo is unusable (zero, negative,
 * NaN, infinite) or the division is [BeatGridDivision.OFF]: returning the marker
 * unchanged is safer than collapsing every marker onto 0 ms.
 */
object BeatGridSnapper {

    fun snap(
        markerMs: Long,
        bpm: Double,
        division: BeatGridDivision,
        offsetMs: Long,
        durationMs: Long
    ): Long {
        val stepMs = gridStepMs(bpm, division) ?: return markerMs

        // Measure from the grid origin so a non-zero offset shifts every line.
        val relative = markerMs - offsetMs
        val snappedRelative = (relative.toDouble() / stepMs).roundToLong() * stepMs
        val snapped = snappedRelative + offsetMs

        return snapped.coerceIn(0L, durationMs.coerceAtLeast(0L))
    }

    fun quantize(
        markers: List<Long>,
        bpm: Double,
        division: BeatGridDivision,
        offsetMs: Long,
        durationMs: Long
    ): List<Long> {
        if (gridStepMs(bpm, division) == null) return markers
        return markers
            .map { snap(it, bpm, division, offsetMs, durationMs) }
            .distinct()
            .sorted()
    }

    /** Grid line spacing in ms, or null when there is no usable grid. */
    fun gridStepMs(bpm: Double, division: BeatGridDivision): Long? {
        if (division == BeatGridDivision.OFF) return null
        if (!bpm.isFinite() || bpm <= 0.0) return null

        val beatMs = 60_000.0 / bpm
        val stepMs = (beatMs / division.multiplier).roundToLong()
        return stepMs.takeIf { it > 0L }
    }
}
