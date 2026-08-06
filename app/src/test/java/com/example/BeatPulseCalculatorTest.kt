package com.example

import com.example.feature.visualizer.beat.BeatPulseCalculator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BeatPulseCalculatorTest {

    private val markers = listOf(1000L, 2000L, 3000L)

    @Test
    fun `before any marker yields idle pulse`() {
        val pulse = BeatPulseCalculator.pulseAt(500L, markers, attackMs = 20L, releaseMs = 100L, strength = 1f)

        assertEquals(0f, pulse.intensity, 0.001f)
        assertTrue(!pulse.isOnBeat)
    }

    @Test
    fun `exactly on a marker after full attack reaches peak intensity`() {
        val pulse = BeatPulseCalculator.pulseAt(1020L, markers, attackMs = 20L, releaseMs = 100L, strength = 1f)

        assertEquals(1f, pulse.intensity, 0.05f)
        assertTrue(pulse.isOnBeat)
    }

    @Test
    fun `intensity decays to zero after attack plus release`() {
        val pulse = BeatPulseCalculator.pulseAt(1200L, markers, attackMs = 20L, releaseMs = 100L, strength = 1f)

        assertEquals(0f, pulse.intensity, 0.001f)
    }

    @Test
    fun `strength scales the peak`() {
        val pulse = BeatPulseCalculator.pulseAt(1020L, markers, attackMs = 20L, releaseMs = 100L, strength = 0.5f)

        assertEquals(0.5f, pulse.intensity, 0.05f)
    }

    @Test
    fun `empty markers always yields idle`() {
        val pulse = BeatPulseCalculator.pulseAt(1000L, emptyList(), attackMs = 20L, releaseMs = 100L, strength = 1f)

        assertEquals(0f, pulse.intensity, 0.001f)
    }
}
