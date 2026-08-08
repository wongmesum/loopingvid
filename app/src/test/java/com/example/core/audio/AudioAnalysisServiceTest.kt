package com.example.core.audio

import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.math.abs

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class AudioAnalysisServiceTest {

    private lateinit var service: AudioAnalysisService

    @Before
    fun setup() {
        service = AudioAnalysisService(ApplicationProvider.getApplicationContext())
    }

    @Test
    fun `generateWaveform downsamples correctly`() {
        // Create 100 samples with clear peak patterns
        val samples = MutableList(100) { 0.1f }
        samples[25] = 0.8f // Peak in second quartile
        samples[75] = -0.9f // Peak in fourth quartile

        val data = service.generateWaveform(samples, points = 4)

        assertEquals(4, data.points.size)
        assertTrue("Q1 max should be 0.1", data.points[0] in 0.09f..0.11f)
        assertTrue("Q2 max should be 0.8", data.points[1] in 0.79f..0.81f)
        assertTrue("Q3 max should be 0.1", data.points[2] in 0.09f..0.11f)
        assertTrue("Q4 max should be |-0.9|", data.points[3] in 0.89f..0.91f)
    }

    @Test
    fun `generateWaveform handles empty input`() {
        val data = service.generateWaveform(emptyList(), points = 10)
        assertTrue(data.points.isEmpty())
    }

    @Test
    fun `generateSpectrum generates expected band magnitudes`() {
        // Generate a synthetic low frequency block and high frequency block
        val samples = mutableListOf<Float>()
        for (i in 0 until 1000) samples.add(0.5f) // loud LF
        for (i in 0 until 1000) samples.add(0.1f) // quiet HF

        val data = service.generateSpectrum(samples, bands = 2)

        assertEquals(2, data.magnitudes.size)
        // Magnitude includes frequency weighting (b.toFloat()/bands)
        val band0 = data.magnitudes[0] // Avg 0.5 * weight ~1.0 = ~0.5
        val band1 = data.magnitudes[1] // Avg 0.1 * weight ~1.4 = ~0.14

        assertTrue("Band 0 expected ~0.5, was $band0", band0 > 0.4f)
        assertTrue("Band 1 expected ~0.14, was $band1", band1 < 0.2f)
    }

    @Test
    fun `detectBpm identifies simple synthetic pulse`() {
        val sampleRate = 44100
        val samples = mutableListOf<Float>()

        // 120 BPM = 2 beats per second = 1 beat every 0.5 seconds
        val intervalSamples = sampleRate / 2

        // Create 5 seconds of silence with a pulse every 0.5s
        for (i in 0 until (sampleRate * 5)) {
            if (i % intervalSamples < 100) { // 100 sample pulse
                samples.add(1.0f)
            } else {
                samples.add(0.0f)
            }
        }

        val data = service.detectBpm(samples, sampleRate)

        assertTrue("BPM should be near 120, was ${data.bpm}", abs(data.bpm - 120.0) < 5.0)
        assertTrue("Confidence should be high, was ${data.confidence}", data.confidence > 0.8f)
    }

    @Test
    fun `detectBpm handles silence safely`() {
        val samples = MutableList(44100 * 2) { 0f }
        val data = service.detectBpm(samples, 44100)
        assertEquals(0.0, data.bpm, 0.01)
    }

    @Test
    fun `analyzeLoudness computes reasonable LUFS approximation`() {
        // Sine wave approximation at -6 dBFS peak (0.5 amplitude)
        val samples = mutableListOf<Float>()
        for (i in 0 until 44100) {
            val phase = (i * 2.0 * Math.PI * 440.0) / 44100.0
            samples.add((0.5 * Math.sin(phase)).toFloat())
        }

        val data = service.analyzeLoudness(samples)

        // RMS of sine wave with peak 0.5 is 0.5/sqrt(2) ≈ 0.353
        // 20*log10(0.353) ≈ -9 dB
        // LUFS ~ RMS - 0.691 ≈ -9.7
        assertTrue("RMS dB expected around -9.0, was ${data.rmsDb}", abs(data.rmsDb - (-9.0)) < 1.0)
        assertTrue("Peak expected near -6.0, was ${data.peakDb}", abs(data.peakDb - (-6.0)) < 1.0)
        assertTrue("LUFS expected near -9.7, was ${data.integratedLufs}", abs(data.integratedLufs - (-9.7)) < 1.0)
    }
}
