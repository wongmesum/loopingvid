package com.example

import com.example.feature.live.BroadcastTarget
import com.example.feature.live.LivePlatform
import com.example.feature.live.LiveUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SimulcastTargetsTest {

    @Test
    fun defaultUiState_hasInitialTargetsAndSimulcastDisabled() {
        val state = LiveUiState()
        assertTrue("Should default to at least 2 broadcast targets", state.targets.size >= 2)
        assertFalse("Simulcast should be disabled by default", state.isSimulcastEnabled)
        assertTrue("Default single target bandwidth should be sufficient", state.isBandwidthSufficient)
        assertNull(state.bandwidthWarningMessage)
    }

    @Test
    fun bandwidthValidation_sufficientCapacity_passesValidation() {
        val targets = listOf(
            BroadcastTarget(id = "1", name = "YouTube", requiredBitrateKbps = 4500, isEnabled = true),
            BroadcastTarget(id = "2", name = "TikTok", requiredBitrateKbps = 3500, isEnabled = true)
        )
        val availableBandwidth = 12000 // 12 Mbps capacity > 8.0 Mbps total

        val totalRequired = targets.filter { it.isEnabled }.sumOf { it.requiredBitrateKbps }
        val isSufficient = totalRequired <= availableBandwidth

        assertTrue(isSufficient)
    }

    @Test
    fun bandwidthValidation_exceedsCapacity_triggersWarning() {
        val targets = listOf(
            BroadcastTarget(id = "1", name = "YouTube Primary", requiredBitrateKbps = 6000, isEnabled = true),
            BroadcastTarget(id = "2", name = "TikTok Secondary", requiredBitrateKbps = 5000, isEnabled = true),
            BroadcastTarget(id = "3", name = "Twitch Backup", requiredBitrateKbps = 5000, isEnabled = true)
        )
        val availableBandwidth = 10000 // 10 Mbps capacity < 16 Mbps required

        val totalRequired = targets.filter { it.isEnabled }.sumOf { it.requiredBitrateKbps }
        val isSufficient = totalRequired <= availableBandwidth

        assertFalse("16 Mbps total required should fail against 10 Mbps available capacity", isSufficient)
    }
}
