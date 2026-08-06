package com.example

import com.example.core.media.Media3SpectrumAudioProcessor
import com.example.feature.editor.EditorUiState
import com.example.feature.editor.SPECTRUM_PALETTES
import com.example.feature.editor.VisualizerMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AudioSpectrumVisualizerTest {

    @Test
    fun defaultUiState_containsSpectrumVisualizerDefaults() {
        val state = EditorUiState()
        assertEquals(VisualizerMode.FFT_BARS, state.visualizerMode)
        assertEquals(SPECTRUM_PALETTES[0], state.selectedPalette)
        assertEquals(1.2f, state.spectrumSensitivityGain, 0.01f)
        assertEquals(32, state.spectrumBandCount)
        assertEquals(32, state.spectrumMagnitudes.size)
    }

    @Test
    fun spectrumPalettes_hasDiverseOptions() {
        assertTrue("Should have at least 5 color palettes", SPECTRUM_PALETTES.size >= 5)
        val names = SPECTRUM_PALETTES.map { it.name }
        assertTrue(names.contains("Cyberpunk"))
        assertTrue(names.contains("Matrix Green"))
    }

    @Test
    fun media3SpectrumAudioProcessor_configuresBandCountAndGain() {
        val processor = Media3SpectrumAudioProcessor()
        processor.bandCount = 64
        processor.sensitivityGain = 2.0f

        assertEquals(64, processor.bandCount)
        assertEquals(2.0f, processor.sensitivityGain, 0.01f)
    }

    @Test
    fun media3SpectrumAudioProcessor_clampsGainsAndBands() {
        val processor = Media3SpectrumAudioProcessor()
        processor.bandCount = 128 // Should clamp to max 64
        processor.sensitivityGain = 10.0f // Should clamp to max 4.0

        assertEquals(64, processor.bandCount)
        assertEquals(4.0f, processor.sensitivityGain, 0.01f)
    }

    @Test
    fun media3SpectrumAudioProcessor_clampsSmoothing() {
        val processor = Media3SpectrumAudioProcessor()
        processor.smoothing = -1.0f
        assertEquals(0f, processor.smoothing, 0.01f)

        processor.smoothing = 5.0f
        assertEquals(0.95f, processor.smoothing, 0.01f)

        processor.smoothing = 0.7f
        assertEquals(0.7f, processor.smoothing, 0.01f)
    }
}
