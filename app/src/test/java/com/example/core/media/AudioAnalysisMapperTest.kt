package com.example.core.media

import com.example.core.audio.AudioAnalysisResult
import com.example.core.audio.BpmData
import com.example.core.audio.LoudnessData
import com.example.core.audio.WaveformData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Guards the adapter between the real analyzer output and the shape the UI renders. The key
 * property under test is that absent analyzer sections stay empty instead of being filled in.
 */
class AudioAnalysisMapperTest {

    private fun resultOf(
        durationMs: Long = 60_000L,
        waveform: WaveformData? = null,
        bpm: BpmData? = null,
        loudness: LoudnessData? = null,
        peakDb: Double = -3.0,
        rmsDb: Double = -18.0
    ) = AudioAnalysisResult(
        durationMs = durationMs,
        sampleRate = 44_100,
        channelCount = 2,
        waveform = waveform,
        spectrum = null,
        bpm = bpm,
        loudness = loudness,
        peakDb = peakDb,
        rmsDb = rmsDb
    )

    @Test
    fun `maps waveform points and loudness from analyzer output`() {
        val data = resultOf(
            durationMs = 12_345L,
            waveform = WaveformData(points = listOf(0.1f, 0.9f, 0.4f)),
            loudness = LoudnessData(integratedLufs = -14.2, peakDb = -1.5, rmsDb = -16.0)
        ).toAudioAnalysisData()

        assertEquals(listOf(0.1f, 0.9f, 0.4f), data.waveformPoints)
        assertEquals(12_345L, data.durationMs)
        assertEquals(-1.5, data.peakLufs, 0.0001)
        assertEquals(-14.2, data.currentRmsLufs, 0.0001)
    }

    @Test
    fun `falls back to top-level peak and rms when loudness section is absent`() {
        val data = resultOf(loudness = null, peakDb = -2.5, rmsDb = -20.5).toAudioAnalysisData()

        assertEquals(-2.5, data.peakLufs, 0.0001)
        assertEquals(-20.5, data.currentRmsLufs, 0.0001)
    }

    @Test
    fun `absent waveform maps to an empty list rather than generated points`() {
        val data = resultOf(waveform = null).toAudioAnalysisData()

        assertTrue(
            "A missing waveform must stay empty so the UI can show a placeholder",
            data.waveformPoints.isEmpty()
        )
    }

    @Test
    fun `absent bpm produces no beat markers`() {
        val data = resultOf(bpm = null).toAudioAnalysisData()

        assertTrue(data.beatMarkersMs.isEmpty())
    }

    @Test
    fun `measured bpm produces a deterministic beat grid across the duration`() {
        // 120 BPM over 10s => a beat every 500ms => 20 markers (0ms..9500ms).
        val data = resultOf(
            durationMs = 10_000L,
            bpm = BpmData(bpm = 120.0, confidence = 0.9f)
        ).toAudioAnalysisData()

        assertEquals(20, data.beatMarkersMs.size)
        assertEquals(0L, data.beatMarkersMs.first())
        assertEquals(500L, data.beatMarkersMs[1])
        assertEquals(9_500L, data.beatMarkersMs.last())
        assertTrue(data.beatMarkersMs.all { it < 10_000L })
    }

    @Test
    fun `low confidence bpm is not turned into a beat grid`() {
        val data = resultOf(
            durationMs = 10_000L,
            bpm = BpmData(bpm = 120.0, confidence = 0.2f)
        ).toAudioAnalysisData()

        assertTrue(
            "An untrustworthy tempo must not draw a grid the user would read as measured",
            data.beatMarkersMs.isEmpty()
        )
    }

    @Test
    fun `implausible bpm is rejected`() {
        val data = resultOf(
            durationMs = 10_000L,
            bpm = BpmData(bpm = 1_000.0, confidence = 0.95f)
        ).toAudioAnalysisData()

        assertTrue(data.beatMarkersMs.isEmpty())
    }

    @Test
    fun `non finite bpm produces no beat markers`() {
        val data = resultOf(
            durationMs = 10_000L,
            bpm = BpmData(bpm = Double.NaN, confidence = 0.9f)
        ).toAudioAnalysisData()

        assertTrue(data.beatMarkersMs.isEmpty())
    }

    @Test
    fun `zero duration produces no beat markers`() {
        val data = resultOf(
            durationMs = 0L,
            bpm = BpmData(bpm = 120.0, confidence = 0.9f)
        ).toAudioAnalysisData()

        assertTrue(data.beatMarkersMs.isEmpty())
    }
}
