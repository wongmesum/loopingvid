package com.example

import android.os.PowerManager
import com.example.core.utils.ThermalMonitor
import com.example.core.utils.ThermalStatusLevel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ThermalMonitorTest {

    @Test
    fun evaluateThermalStatus_evaluatesNormalStatus() {
        val info = ThermalMonitor.evaluateThermalStatus(35.0f, 0)
        assertEquals(ThermalStatusLevel.NORMAL, info.level)
        assertFalse(info.isThrottling)
    }

    @Test
    fun evaluateThermalStatus_evaluatesModerateThrottling() {
        val info = ThermalMonitor.evaluateThermalStatus(41.0f, 2)
        assertEquals(ThermalStatusLevel.MODERATE, info.level)
        assertTrue(info.isThrottling)
        assertNotNull(info.warningMessage)
        assertNotNull(info.mitigationSuggestion)
    }

    @Test
    fun evaluateThermalStatus_evaluatesCriticalOverheating() {
        val info = ThermalMonitor.evaluateThermalStatus(49.0f, 4)
        assertEquals(ThermalStatusLevel.CRITICAL, info.level)
        assertTrue(info.isThrottling)
        assertTrue(info.warningMessage!!.contains("CRITICAL OVERHEATING"))
    }

    @Test
    fun thermalStatusLevel_propertiesSetCorrectly() {
        assertEquals(60, ThermalStatusLevel.NORMAL.maxRecommendedFps)
        assertEquals(30, ThermalStatusLevel.MODERATE.maxRecommendedFps)
        assertEquals(15, ThermalStatusLevel.CRITICAL.maxRecommendedFps)

        assertEquals(1.0f, ThermalStatusLevel.NORMAL.bitrateScaleFactor, 0.01f)
        assertEquals(0.75f, ThermalStatusLevel.MODERATE.bitrateScaleFactor, 0.01f)
        assertEquals(0.30f, ThermalStatusLevel.CRITICAL.bitrateScaleFactor, 0.01f)
    }
}
