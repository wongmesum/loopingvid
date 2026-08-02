package com.example

import com.example.core.media.ExoPlayerAudioProcessor
import com.example.feature.live.EQ_PRESETS
import com.example.feature.live.LiveUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AudioAdjustmentPanelTest {

    @Test
    fun defaultUiState_hasInitialAudioAdjustments() {
        val state = LiveUiState()
        assertEquals(1.0f, state.masterVolume, 0.01f)
        assertFalse(state.isMasterMuted)
        assertEquals(0.0f, state.bassGainDb, 0.01f)
        assertEquals(0.0f, state.trebleGainDb, 0.01f)
        assertEquals("Flat / Neutral", state.selectedEqPresetName)
    }

    @Test
    fun eqPresets_hasExpectedRanges() {
        val bassBoostPreset = EQ_PRESETS.first { it.name == "Bass Boost" }
        assertEquals(8f, bassBoostPreset.bassDb, 0.01f)
        assertEquals(0f, bassBoostPreset.trebleDb, 0.01f)

        val vocalClarityPreset = EQ_PRESETS.first { it.name == "Vocal & Clarity" }
        assertEquals(-2f, vocalClarityPreset.bassDb, 0.01f)
        assertEquals(6f, vocalClarityPreset.trebleDb, 0.01f)
    }

    @Test
    fun exoPlayerAudioProcessor_configuresVolumeBassAndTreble() {
        val processor = ExoPlayerAudioProcessor()
        processor.volume = 0.85f
        processor.bassGainDb = 6.0f
        processor.trebleGainDb = 3.5f
        processor.isMuted = false

        assertEquals(0.85f, processor.volume, 0.01f)
        assertEquals(6.0f, processor.bassGainDb, 0.01f)
        assertEquals(3.5f, processor.trebleGainDb, 0.01f)
        assertFalse(processor.isMuted)
    }

    @Test
    fun exoPlayerAudioProcessor_clampsGainsWithinSafeBounds() {
        val processor = ExoPlayerAudioProcessor()
        processor.bassGainDb = 25.0f // Should clamp to 12 dB max
        processor.trebleGainDb = -20.0f // Should clamp to -12 dB min

        assertEquals(12.0f, processor.bassGainDb, 0.01f)
        assertEquals(-12.0f, processor.trebleGainDb, 0.01f)
    }
}
