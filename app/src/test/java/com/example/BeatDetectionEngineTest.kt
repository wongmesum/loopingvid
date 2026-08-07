package com.example

import com.example.feature.visualizer.beat.BeatDetectionConfig
import com.example.feature.visualizer.beat.BeatDetectionEngine
import com.example.feature.visualizer.beat.FrequencyBand
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI
import kotlin.math.sin

/**
 * Behaviour guards for beat detection. The engine must find real onsets from
 * PCM data, not emit a fixed grid, so these tests feed synthetic audio with a
 * known BPM and assert the detected tempo and marker spacing.
 */
class BeatDetectionEngineTest {

    private val sampleRate = 44100

    /**
     * Builds mono 16-bit-normalised PCM containing a short percussive burst on
     * every beat at [bpm], with near-silence between the hits.
     */
    private fun buildPulsedPcm(
        bpm: Int,
        durationSec: Double,
        toneHz: Double = 60.0,
        burstSec: Double = 0.06
    ): FloatArray {
        val totalSamples = (sampleRate * durationSec).toInt()
        val samplesPerBeat = (sampleRate * 60.0 / bpm).toInt()
        val burstSamples = (sampleRate * burstSec).toInt()
        val pcm = FloatArray(totalSamples)

        var beatStart = 0
        while (beatStart < totalSamples) {
            val end = minOf(beatStart + burstSamples, totalSamples)
            for (i in beatStart until end) {
                val progress = (i - beatStart).toDouble() / burstSamples
                val envelope = (1.0 - progress).coerceAtLeast(0.0)
                pcm[i] = (sin(2 * PI * toneHz * i / sampleRate) * envelope * 0.9).toFloat()
            }
            beatStart += samplesPerBeat
        }
        return pcm
    }

    @Test
    fun `detects 120 bpm from pulsed audio within tolerance`() {
        val pcm = buildPulsedPcm(bpm = 120, durationSec = 8.0)

        val result = BeatDetectionEngine.analyze(pcm, sampleRate, BeatDetectionConfig())

        assertEquals(120.0, result.bpm, 6.0)
    }

    @Test
    fun `detects 90 bpm from pulsed audio within tolerance`() {
        val pcm = buildPulsedPcm(bpm = 90, durationSec = 8.0)

        val result = BeatDetectionEngine.analyze(pcm, sampleRate, BeatDetectionConfig())

        assertEquals(90.0, result.bpm, 6.0)
    }

    @Test
    fun `marker count roughly matches the number of pulses`() {
        val bpm = 120
        val durationSec = 8.0
        val pcm = buildPulsedPcm(bpm = bpm, durationSec = durationSec)
        val expectedBeats = (durationSec / (60.0 / bpm)).toInt()

        val result = BeatDetectionEngine.analyze(pcm, sampleRate, BeatDetectionConfig())

        assertTrue(
            "Expected roughly $expectedBeats markers, got ${result.markersMs.size}",
            result.markersMs.size in (expectedBeats - 2)..(expectedBeats + 2)
        )
    }

    @Test
    fun `markers are strictly increasing`() {
        val pcm = buildPulsedPcm(bpm = 128, durationSec = 6.0)

        val markers = BeatDetectionEngine.analyze(pcm, sampleRate, BeatDetectionConfig()).markersMs

        markers.zipWithNext { current, next ->
            assertTrue("Markers must increase: $current -> $next", next > current)
        }
    }

    @Test
    fun `offset shifts every marker by the configured amount`() {
        val pcm = buildPulsedPcm(bpm = 120, durationSec = 6.0)
        val base = BeatDetectionEngine.analyze(pcm, sampleRate, BeatDetectionConfig())

        val shifted = BeatDetectionEngine.analyze(
            pcm,
            sampleRate,
            BeatDetectionConfig(offsetMs = 120L)
        )

        assertEquals(base.markersMs.size, shifted.markersMs.size)
        base.markersMs.zip(shifted.markersMs).forEach { (original, moved) ->
            assertEquals(original + 120L, moved)
        }
    }

    @Test
    fun `silence produces no markers and zero bpm`() {
        val silence = FloatArray(sampleRate * 4)

        val result = BeatDetectionEngine.analyze(silence, sampleRate, BeatDetectionConfig())

        assertTrue("Silence must not yield beats", result.markersMs.isEmpty())
        assertEquals(0.0, result.bpm, 0.001)
    }

    @Test
    fun `higher threshold detects no more beats than a lower one`() {
        val pcm = buildPulsedPcm(bpm = 120, durationSec = 8.0)

        val sensitive = BeatDetectionEngine.analyze(pcm, sampleRate, BeatDetectionConfig(threshold = 0.15f))
        val strict = BeatDetectionEngine.analyze(pcm, sampleRate, BeatDetectionConfig(threshold = 0.85f))

        assertTrue(
            "Strict threshold (${strict.markersMs.size}) must not exceed sensitive (${sensitive.markersMs.size})",
            strict.markersMs.size <= sensitive.markersMs.size
        )
    }

    @Test
    fun `bass band still tracks a low frequency pulse`() {
        val pcm = buildPulsedPcm(bpm = 120, durationSec = 8.0, toneHz = 55.0)

        val result = BeatDetectionEngine.analyze(
            pcm,
            sampleRate,
            BeatDetectionConfig(band = FrequencyBand.BASS)
        )

        assertEquals(120.0, result.bpm, 8.0)
    }

    @Test
    fun `minimum interval suppresses double triggers`() {
        val pcm = buildPulsedPcm(bpm = 200, durationSec = 6.0)

        val result = BeatDetectionEngine.analyze(
            pcm,
            sampleRate,
            BeatDetectionConfig(minIntervalMs = 500L)
        )

        result.markersMs.zipWithNext { current, next ->
            assertTrue("Interval must respect 500ms floor", next - current >= 500L)
        }
    }

    @Test
    fun `tap bpm averages the tap intervals`() {
        // Taps 500ms apart => 120 BPM
        val taps = listOf(0L, 500L, 1000L, 1500L, 2000L)

        val bpm = BeatDetectionEngine.bpmFromTaps(taps)

        assertEquals(120.0, bpm, 0.5)
    }

    @Test
    fun `tap bpm needs at least two taps`() {
        assertEquals(0.0, BeatDetectionEngine.bpmFromTaps(listOf(1000L)), 0.001)
        assertEquals(0.0, BeatDetectionEngine.bpmFromTaps(emptyList()), 0.001)
    }

    @Test
    fun `manual bpm generates an evenly spaced grid`() {
        val markers = BeatDetectionEngine.gridFromBpm(bpm = 120.0, durationMs = 4000L, offsetMs = 0L)

        assertEquals(listOf(0L, 500L, 1000L, 1500L, 2000L, 2500L, 3000L, 3500L), markers)
    }

    @Test
    fun `manual bpm grid honours offset`() {
        val markers = BeatDetectionEngine.gridFromBpm(bpm = 60.0, durationMs = 3000L, offsetMs = 250L)

        assertEquals(listOf(250L, 1250L, 2250L), markers)
    }

    @Test
    fun `manual bpm ignores NaN and Infinity`() {
        assertTrue(BeatDetectionEngine.gridFromBpm(Double.NaN, 4000L, 0L).isEmpty())
        assertTrue(BeatDetectionEngine.gridFromBpm(Double.POSITIVE_INFINITY, 4000L, 0L).isEmpty())
        assertTrue(BeatDetectionEngine.gridFromBpm(Double.NEGATIVE_INFINITY, 4000L, 0L).isEmpty())
        assertTrue(BeatDetectionEngine.gridFromBpm(-120.0, 4000L, 0L).isEmpty())
        assertTrue(BeatDetectionEngine.gridFromBpm(500.0, 4000L, 0L).isEmpty()) // Clamp unrealistic fast
    }

    @Test
    fun `manual bpm with negative offset drops markers before zero`() {
        val markers = BeatDetectionEngine.gridFromBpm(bpm = 120.0, durationMs = 2000L, offsetMs = -200L)
        // 120BPM = 500ms intervals. Offset -200ms means markers at:
        // -200 (dropped), 300, 800, 1300, 1800.
        assertEquals(listOf(300L, 800L, 1300L, 1800L), markers)
    }
}
