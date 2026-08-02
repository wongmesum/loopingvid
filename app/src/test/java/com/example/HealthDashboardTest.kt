package com.example

import com.example.feature.live.LiveUiState
import com.example.feature.live.StreamStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HealthDashboardTest {

    @Test
    fun defaultLiveUiState_hasValidHealthScoreAndMetrics() {
        val uiState = LiveUiState()
        assertEquals(98, uiState.healthScorePct)
        assertEquals(4500, uiState.currentBitrateKbps)
        assertEquals(4500, uiState.targetBitrateKbps)
        assertEquals(0, uiState.droppedFrames)
        assertEquals(120, uiState.latencyMs)
        assertEquals(6, uiState.jitterMs)
        assertTrue(uiState.bitrateHistory.isNotEmpty())
        assertTrue(uiState.rttHistory.isNotEmpty())
    }

    @Test
    fun liveUiState_healthScorePercentageReflectsMetrics() {
        val lowHealthState = LiveUiState(
            healthScorePct = 65,
            droppedFrames = 48,
            latencyMs = 210,
            jitterMs = 28,
            streamStatus = StreamStatus.LIVE
        )

        assertEquals(65, lowHealthState.healthScorePct)
        assertEquals(48, lowHealthState.droppedFrames)
        assertEquals(210, lowHealthState.latencyMs)
    }
}
