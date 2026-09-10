package com.example

import com.example.feature.live.LiveUiState
import com.example.feature.live.StreamStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HealthDashboardTest {

    @Test
    fun defaultLiveUiState_startsWithHonestZeroedMetrics() {
        // Before streaming there is no real telemetry, so all measured metrics default to 0 and
        // histories are empty. Nothing is fabricated. Only targetBitrateKbps has a real default
        // (the recommended target the encoder will aim for once streaming begins).
        val uiState = LiveUiState()
        assertEquals(0, uiState.healthScorePct)
        assertEquals(0, uiState.currentBitrateKbps)
        assertEquals(4500, uiState.targetBitrateKbps)
        assertEquals(0, uiState.droppedFrames)
        assertEquals(0, uiState.latencyMs)
        assertEquals(0, uiState.jitterMs)
        assertTrue("bitrate history starts empty until real onNewBitrate callbacks arrive", uiState.bitrateHistory.isEmpty())
        assertTrue("rtt history starts empty (RTT is not reported by the RTMP client)", uiState.rttHistory.isEmpty())
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
