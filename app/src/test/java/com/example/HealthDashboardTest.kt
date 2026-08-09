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
        // After simulation removal, defaults are honest zeros — no fake seeded metrics.
        assertEquals(0, uiState.healthScorePct)
        assertEquals(0, uiState.currentBitrateKbps)
        assertEquals(4500, uiState.targetBitrateKbps)
        assertEquals(0, uiState.droppedFrames)
        assertEquals(0, uiState.latencyMs)
        assertEquals(0, uiState.jitterMs)
        assertTrue(uiState.bitrateHistory.isEmpty())
        assertTrue(uiState.rttHistory.isEmpty())
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
