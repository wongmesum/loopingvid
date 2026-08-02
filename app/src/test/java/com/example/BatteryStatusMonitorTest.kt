package com.example

import android.os.BatteryManager
import com.example.core.ui.BatteryInfo
import com.example.core.ui.BatteryStatusMonitor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BatteryStatusMonitorTest {

    @Test
    fun defaultBatteryInfo_hasNormalDefaults() {
        val info = BatteryInfo()
        assertEquals(100, info.percentage)
        assertFalse(info.isCharging)
        assertFalse(info.isLowBattery)
        assertFalse(info.isCriticalBattery)
        assertNull(info.warningMessage)
    }

    @Test
    fun evaluateBatteryInfo_parsesChargingStateAndPlugCorrectly() {
        val info = BatteryStatusMonitor.evaluateBatteryInfo(
            level = 80,
            scale = 100,
            status = BatteryManager.BATTERY_STATUS_CHARGING,
            chargePlugCode = BatteryManager.BATTERY_PLUGGED_AC,
            healthCode = BatteryManager.BATTERY_HEALTH_GOOD,
            tempCelsius = 32.0f,
            voltageMv = 4100
        )

        assertEquals(80, info.percentage)
        assertTrue(info.isCharging)
        assertEquals("AC Power", info.chargePlug)
        assertEquals("Good", info.health)
        assertEquals(32.0f, info.temperatureCelsius, 0.1f)
        assertEquals(4100, info.voltageMv)
        assertFalse(info.isLowBattery)
        assertFalse(info.isCriticalBattery)
    }

    @Test
    fun evaluateBatteryInfo_detectsCriticalLowBatteryWhenDischarging() {
        val info = BatteryStatusMonitor.evaluateBatteryInfo(
            level = 8,
            scale = 100,
            status = BatteryManager.BATTERY_STATUS_DISCHARGING,
            chargePlugCode = 0,
            healthCode = BatteryManager.BATTERY_HEALTH_GOOD,
            tempCelsius = 38.5f,
            voltageMv = 3450
        )

        assertEquals(8, info.percentage)
        assertFalse(info.isCharging)
        assertTrue(info.isLowBattery)
        assertTrue(info.isCriticalBattery)
        assertNotNull(info.warningMessage)
        assertTrue(info.warningMessage!!.contains("CRITICAL BATTERY LEVEL"))
    }
}
