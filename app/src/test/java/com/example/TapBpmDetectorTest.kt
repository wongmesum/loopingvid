package com.example

import com.example.feature.visualizer.beat.TapBpmDetector
import org.junit.Assert.assertEquals
import org.junit.Test

class TapBpmDetectorTest {

    @Test
    fun `two taps 500ms apart yield 120 bpm`() {
        val detector = TapBpmDetector()
        detector.tap(0L)
        detector.tap(500L)

        assertEquals(120.0, detector.currentBpm(), 0.5)
    }

    @Test
    fun `single tap yields zero bpm`() {
        val detector = TapBpmDetector()
        detector.tap(0L)

        assertEquals(0.0, detector.currentBpm(), 0.001)
        assertEquals(1, detector.tapCount())
    }

    @Test
    fun `gap over 3 seconds resets the tap window`() {
        val detector = TapBpmDetector()
        detector.tap(0L)
        detector.tap(500L)
        detector.tap(4000L) // > 3s gap, should reset

        assertEquals(1, detector.tapCount())
    }

    @Test
    fun `reset clears all taps`() {
        val detector = TapBpmDetector()
        detector.tap(0L)
        detector.tap(500L)
        detector.reset()

        assertEquals(0, detector.tapCount())
        assertEquals(0.0, detector.currentBpm(), 0.001)
    }

    @Test
    fun `window is capped at max taps`() {
        val detector = TapBpmDetector(maxTaps = 4)
        // Steady 500ms taps well within the 3s reset gap.
        for (i in 0..9) {
            detector.tap(i * 500L)
        }

        assertEquals(4, detector.tapCount())
    }
}
