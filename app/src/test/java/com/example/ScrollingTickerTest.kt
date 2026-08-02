package com.example

import com.example.feature.live.TICKER_PRESET_STYLES
import com.example.feature.live.TickerPosition
import com.example.feature.live.TickerSpeed
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ScrollingTickerTest {

    @Test
    fun testTickerPresetStylesCountAndColors() {
        assertTrue("Preset styles should contain at least 4 themes", TICKER_PRESET_STYLES.size >= 4)
        assertEquals("News Red", TICKER_PRESET_STYLES[0].name)
        assertEquals("Dark Gold", TICKER_PRESET_STYLES[1].name)
    }

    @Test
    fun testTickerSpeedDurationMapping() {
        assertTrue(TickerSpeed.SLOW.durationMs > TickerSpeed.NORMAL.durationMs)
        assertTrue(TickerSpeed.NORMAL.durationMs > TickerSpeed.FAST.durationMs)
    }

    @Test
    fun testTickerPositionLabels() {
        assertEquals("Top", TickerPosition.TOP.label)
        assertEquals("Bottom", TickerPosition.BOTTOM.label)
    }
}
