package com.example.feature.visualizer.beat

data class BeatSyncState(
    val isAnalyzing: Boolean = false,
    val bpm: Double = 0.0,
    val durationMs: Long = 0L,
    val markersMs: List<Long> = emptyList(),
    val config: BeatDetectionConfig = BeatDetectionConfig(),
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
