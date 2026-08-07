package com.example.feature.visualizer.beat

import kotlin.math.abs
import kotlin.math.roundToLong

/** Which frequency range to analyze for onsets. */
enum class FrequencyBand {
    BASS,   // ~20-250 Hz
    MID,    // ~250-4000 Hz
    HIGH,   // ~4000-16000 Hz
    FULL    // Entire spectrum
}

/** Tuning knobs for beat detection. */
data class BeatDetectionConfig(
    val band: FrequencyBand = FrequencyBand.FULL,
    val sensitivity: Float = 1.0f,
    val threshold: Float = 0.35f,
    val smoothing: Float = 0.6f,
    val attackMs: Long = 5L,
    val releaseMs: Long = 50L,
    val offsetMs: Long = 0L,
    val minIntervalMs: Long = 200L,
    val effectStrength: Float = 1.0f
)

/** Result of beat analysis on a PCM buffer. */
data class BeatAnalysisResult(
    val bpm: Double,
    val markersMs: List<Long>,
    val confidence: Float
)

/**
 * Onset-energy beat detection operating on normalised mono PCM floats (-1..1).
 *
 * Algorithm:
 * 1. Optionally bandpass-filter the signal by [FrequencyBand].
 * 2. Compute short-time energy in overlapping windows (hop = 512 samples).
 * 3. Compute a rolling average energy (the "local mean").
 * 4. Mark an onset where instantaneous energy exceeds localMean * threshold factor.
 * 5. Enforce minimum interval between consecutive onsets.
 * 6. Estimate BPM from median inter-onset interval.
 * 7. Apply user offset to all markers.
 */
object BeatDetectionEngine {

    private const val HOP_SIZE = 512
    private const val WINDOW_SIZE = 1024
    private const val HISTORY_FRAMES = 43 // ~1 second of history at 44100/512
    private const val MIN_BPM = 20.0
    private const val MAX_BPM = 300.0

    fun analyze(
        pcm: FloatArray,
        sampleRate: Int,
        config: BeatDetectionConfig
    ): BeatAnalysisResult {
        if (pcm.isEmpty()) return BeatAnalysisResult(0.0, emptyList(), 0f)

        val filtered = applyBandFilter(pcm, sampleRate, config.band)
        val energyFrames = computeFrameEnergies(filtered)

        if (energyFrames.isEmpty()) return BeatAnalysisResult(0.0, emptyList(), 0f)

        val onsetFrames = detectOnsets(energyFrames, config)
        val markersMs = framesToMs(onsetFrames, sampleRate)
        val intervalFiltered = enforceMinInterval(markersMs, config.minIntervalMs)
        // A negative offset can push early markers before the start of the track.
        // Drop those instead of clamping, which would stack them all at 0 ms.
        val shifted = intervalFiltered
            .map { it + config.offsetMs }
            .filter { it >= 0L }
            .distinct()
            .sorted()

        val bpm = estimateBpm(shifted)
        val confidence = if (shifted.size >= 4) 0.8f else if (shifted.size >= 2) 0.5f else 0f

        return BeatAnalysisResult(bpm, shifted, confidence)
    }

    /** Calculate BPM from a list of tap timestamps (ms). */
    fun bpmFromTaps(taps: List<Long>): Double {
        if (taps.size < 2) return 0.0
        val intervals = taps.zipWithNext { a, b -> b - a }.filter { it > 0 }
        if (intervals.isEmpty()) return 0.0
        val avgIntervalMs = intervals.average()
        return 60_000.0 / avgIntervalMs
    }

    /**
     * Generate an evenly-spaced beat grid from a known BPM.
     *
     * Rejects values that cannot describe a real tempo — NaN, infinities, and
     * anything outside [MIN_BPM]..[MAX_BPM] — so a bad manual entry or a Tap BPM
     * double-tap cannot flood the timeline with markers. A negative offset is
     * allowed (it nudges the grid earlier), but markers landing before 0 ms are
     * dropped rather than clamped, which would otherwise stack them all at 0.
     */
    fun gridFromBpm(bpm: Double, durationMs: Long, offsetMs: Long = 0L): List<Long> {
        if (!bpm.isFinite() || bpm < MIN_BPM || bpm > MAX_BPM) return emptyList()
        if (durationMs <= 0L) return emptyList()

        val intervalMs = (60_000.0 / bpm).roundToLong()
        if (intervalMs <= 0L) return emptyList()

        val markers = mutableListOf<Long>()
        var current = offsetMs
        while (current < durationMs) {
            if (current >= 0L) markers.add(current)
            current += intervalMs
        }
        return markers
    }

    // --- Internal ---

    private fun applyBandFilter(pcm: FloatArray, sampleRate: Int, band: FrequencyBand): FloatArray {
        if (band == FrequencyBand.FULL) return pcm

        // Simple single-pole IIR low/high pass for band isolation.
        // Not audiophile-grade, but sufficient for energy-onset detection.
        return when (band) {
            FrequencyBand.BASS -> lowPass(pcm, sampleRate, cutoffHz = 250.0)
            FrequencyBand.HIGH -> highPass(pcm, sampleRate, cutoffHz = 4000.0)
            FrequencyBand.MID -> {
                val low = highPass(pcm, sampleRate, cutoffHz = 250.0)
                lowPass(low, sampleRate, cutoffHz = 4000.0)
            }
            FrequencyBand.FULL -> pcm
        }
    }

    private fun lowPass(pcm: FloatArray, sampleRate: Int, cutoffHz: Double): FloatArray {
        val rc = 1.0 / (2.0 * Math.PI * cutoffHz)
        val dt = 1.0 / sampleRate
        val alpha = (dt / (rc + dt)).toFloat()
        val output = FloatArray(pcm.size)
        output[0] = pcm[0]
        for (i in 1 until pcm.size) {
            output[i] = output[i - 1] + alpha * (pcm[i] - output[i - 1])
        }
        return output
    }

    private fun highPass(pcm: FloatArray, sampleRate: Int, cutoffHz: Double): FloatArray {
        val rc = 1.0 / (2.0 * Math.PI * cutoffHz)
        val dt = 1.0 / sampleRate
        val alpha = (rc / (rc + dt)).toFloat()
        val output = FloatArray(pcm.size)
        output[0] = pcm[0]
        for (i in 1 until pcm.size) {
            output[i] = alpha * (output[i - 1] + pcm[i] - pcm[i - 1])
        }
        return output
    }

    private fun computeFrameEnergies(pcm: FloatArray): FloatArray {
        val frameCount = (pcm.size - WINDOW_SIZE) / HOP_SIZE + 1
        if (frameCount <= 0) return FloatArray(0)
        val energies = FloatArray(frameCount)
        for (frame in 0 until frameCount) {
            val start = frame * HOP_SIZE
            var sum = 0.0f
            val end = minOf(start + WINDOW_SIZE, pcm.size)
            for (i in start until end) {
                sum += pcm[i] * pcm[i]
            }
            energies[frame] = sum / WINDOW_SIZE
        }
        return energies
    }

    private fun detectOnsets(energies: FloatArray, config: BeatDetectionConfig): List<Int> {
        val onsets = mutableListOf<Int>()
        val historySize = HISTORY_FRAMES
        val thresholdFactor = 1.0f + config.threshold * 3.0f // Higher threshold => higher multiplier

        for (frame in historySize until energies.size) {
            val localMean = energies.slice((frame - historySize) until frame).average().toFloat()
            val current = energies[frame] * config.sensitivity

            if (localMean > 0f && current > localMean * thresholdFactor) {
                onsets.add(frame)
            }
        }
        return onsets
    }

    private fun framesToMs(frames: List<Int>, sampleRate: Int): List<Long> {
        val msPerFrame = (HOP_SIZE.toDouble() / sampleRate) * 1000.0
        return frames.map { (it * msPerFrame).roundToLong() }
    }

    private fun enforceMinInterval(markers: List<Long>, minMs: Long): List<Long> {
        if (markers.isEmpty()) return markers
        val result = mutableListOf(markers[0])
        for (i in 1 until markers.size) {
            if (markers[i] - result.last() >= minMs) {
                result.add(markers[i])
            }
        }
        return result
    }

    private fun estimateBpm(markers: List<Long>): Double {
        if (markers.size < 2) return 0.0
        val intervals = markers.zipWithNext { a, b -> b - a }.filter { it > 0 }
        if (intervals.isEmpty()) return 0.0
        val sorted = intervals.sorted()
        val median = sorted[sorted.size / 2]
        return if (median > 0) 60_000.0 / median else 0.0
    }
}
