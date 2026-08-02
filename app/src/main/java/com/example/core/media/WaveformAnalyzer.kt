package com.example.core.media

import kotlin.math.sin
import kotlin.random.Random

data class AudioAnalysisData(
    val waveformPoints: List<Float>, // Normalized 0.0 to 1.0
    val beatMarkersMs: List<Long>,
    val durationMs: Long,
    val peakLufs: Double,
    val currentRmsLufs: Double
)

object WaveformAnalyzer {

    fun generateSimulatedWaveform(
        durationMs: Long = 180000L,
        pointCount: Int = 100,
        seed: Long = 42L
    ): AudioAnalysisData {
        val random = Random(seed)
        val points = ArrayList<Float>(pointCount)
        val beatMarkers = ArrayList<Long>()

        val bpm = 120
        val beatIntervalMs = (60000f / bpm).toLong()

        var currentMs = 0L
        while (currentMs < durationMs) {
            beatMarkers.add(currentMs)
            currentMs += beatIntervalMs
        }

        for (i in 0 until pointCount) {
            val progress = i.toFloat() / pointCount
            val envelope = sin(progress * Math.PI).toFloat().coerceIn(0.2f, 1.0f)
            val noise = random.nextFloat() * 0.4f
            val base = sin(i * 0.15f) * 0.3f + 0.5f
            val valNormalized = ((base + noise) * envelope).coerceIn(0.05f, 0.98f)
            points.add(valNormalized)
        }

        val calculatedLufs = -23.5 + (random.nextDouble() * 6.0)

        return AudioAnalysisData(
            waveformPoints = points,
            beatMarkersMs = beatMarkers,
            durationMs = durationMs,
            peakLufs = calculatedLufs + 4.2,
            currentRmsLufs = calculatedLufs
        )
    }
}
