package com.example

import com.example.feature.live.LiveUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AudioMixerTest {

    @Test
    fun defaultAudioMixerState_hasExpectedDefaults() {
        val uiState = LiveUiState()

        assertEquals(1.0f, uiState.videoSourceVolume, 0.01f)
        assertFalse(uiState.isVideoSourceMuted)

        assertEquals(0.8f, uiState.micInputVolume, 0.01f)
        assertFalse(uiState.isMicInputMuted)

        assertEquals(0.5f, uiState.bgMusicVolume, 0.01f)
        assertFalse(uiState.isBgMusicMuted)

        assertEquals("Chill Lo-Fi Beat", uiState.bgMusicTrack)
        assertTrue(uiState.isMicDuckingEnabled)

        assertEquals(1.0f, uiState.masterVolume, 0.01f)
        assertFalse(uiState.isMasterMuted)
    }

    @Test
    fun volumeCoercion_clampsValuesBetweenZeroAndOne() {
        val volOverflow = 1.5f.coerceIn(0f, 1f)
        val volUnderflow = (-0.2f).coerceIn(0f, 1f)

        assertEquals(1.0f, volOverflow, 0.01f)
        assertEquals(0.0f, volUnderflow, 0.01f)
    }
}
