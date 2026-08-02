package com.example

import com.example.core.utils.StorageInfo
import com.example.core.utils.ThermalInfo
import com.example.core.utils.ThermalStatusLevel
import com.example.feature.live.HealthDotState
import com.example.feature.live.StreamStatus
import com.example.feature.live.getNetworkQualityDotState
import com.example.feature.live.getStorageDotState
import com.example.feature.live.getThermalDotState
import org.junit.Assert.assertEquals
import org.junit.Test

class StreamHealthStatusIconsTest {

    @Test
    fun getNetworkQualityDotState_offline_returnsOffline() {
        val state = getNetworkQualityDotState(
            streamStatus = StreamStatus.OFFLINE,
            bitrateKbps = 4500,
            latencyMs = 80,
            droppedFrames = 0
        )
        assertEquals(HealthDotState.OFFLINE, state)
    }

    @Test
    fun getNetworkQualityDotState_optimalMetrics_returnsExcellent() {
        val state = getNetworkQualityDotState(
            streamStatus = StreamStatus.LIVE,
            bitrateKbps = 4800,
            latencyMs = 95,
            droppedFrames = 0
        )
        assertEquals(HealthDotState.EXCELLENT, state)
    }

    @Test
    fun getNetworkQualityDotState_highLatencyOrDroppedFrames_returnsPoorOrFair() {
        val fairState = getNetworkQualityDotState(
            streamStatus = StreamStatus.LIVE,
            bitrateKbps = 4000,
            latencyMs = 180,
            droppedFrames = 5
        )
        assertEquals(HealthDotState.FAIR, fairState)

        val poorState = getNetworkQualityDotState(
            streamStatus = StreamStatus.LIVE,
            bitrateKbps = 1200,
            latencyMs = 300,
            droppedFrames = 25
        )
        assertEquals(HealthDotState.POOR, poorState)
    }

    @Test
    fun getStorageDotState_criticalStorage_returnsPoor() {
        val info = StorageInfo(
            availableBytes = 300 * 1024 * 1024L,
            totalBytes = 64 * 1024 * 1024 * 1024L,
            availableMb = 300L,
            isLowStorage = true,
            isCriticalStorage = true,
            warningMessage = "Low storage"
        )
        val state = getStorageDotState(info)
        assertEquals(HealthDotState.POOR, state)
    }

    @Test
    fun getThermalDotState_normal_returnsExcellent() {
        val thermalInfo = ThermalInfo(
            temperatureCelsius = 34.0f,
            level = ThermalStatusLevel.NORMAL
        )
        val state = getThermalDotState(thermalInfo)
        assertEquals(HealthDotState.EXCELLENT, state)
    }
}
