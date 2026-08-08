package com.example.core.media

import org.junit.Assert.assertEquals
import org.junit.Test

class SegmentTransitionConfigTest {

    @Test
    fun `calculateTotalDurationSec is 0 when empty`() {
        val config = SegmentTransitionConfig(segments = emptyList())
        assertEquals(0.0, config.calculateTotalDurationSec(), 0.01)
    }

    @Test
    fun `calculateTotalDurationSec handles single segment`() {
        val config = SegmentTransitionConfig(
            segments = listOf(
                LoopedSegmentConfig("1", "Seg 1", durationSec = 4.0, loopRepeatCount = 2)
            )
        )
        // 4.0 * 2 = 8.0
        assertEquals(8.0, config.calculateTotalDurationSec(), 0.01)
    }

    @Test
    fun `calculateTotalDurationSec subtracts overlaps for multiple segments`() {
        val config = SegmentTransitionConfig(
            segments = listOf(
                LoopedSegmentConfig("1", "Seg 1", durationSec = 5.0, loopRepeatCount = 1, transitionDurationSec = 1.0),
                LoopedSegmentConfig("2", "Seg 2", durationSec = 4.0, loopRepeatCount = 2, transitionDurationSec = 2.0),
                LoopedSegmentConfig("3", "Seg 3", durationSec = 3.0, loopRepeatCount = 1, transitionDurationSec = 1.0)
            )
        )
        // Seg 1 effective: 5.0
        // Seg 2 effective: 8.0
        // Seg 3 effective: 3.0
        // Overlap 1->2: 1.0
        // Overlap 2->3: 2.0
        // Total: 5.0 + (8.0 - 1.0) + (3.0 - 2.0) = 5.0 + 7.0 + 1.0 = 13.0
        assertEquals(13.0, config.calculateTotalDurationSec(), 0.01)
    }

    @Test
    fun `buildFfmpegXfadeFilterGraph returns empty for 0 or 1 segment`() {
        val emptyConfig = SegmentTransitionConfig(segments = emptyList())
        assertEquals("", emptyConfig.buildFfmpegXfadeFilterGraph())

        val singleConfig = SegmentTransitionConfig(
            segments = listOf(
                LoopedSegmentConfig("1", "Seg 1")
            )
        )
        assertEquals("", singleConfig.buildFfmpegXfadeFilterGraph())
    }

    @Test
    fun `buildFfmpegXfadeFilterGraph creates correct tags and offsets for 2 segments`() {
        val config = SegmentTransitionConfig(
            segments = listOf(
                LoopedSegmentConfig("1", "Seg 1", durationSec = 5.0, loopRepeatCount = 1, transitionToNext = TransitionEffect.WIPE_LEFT, transitionDurationSec = 1.5),
                LoopedSegmentConfig("2", "Seg 2", durationSec = 4.0, loopRepeatCount = 1)
            )
        )

        val graph = config.buildFfmpegXfadeFilterGraph()
        // Offset = 5.0 - 1.5 = 3.5
        assertEquals("[0:v][1:v]xfade=transition=wipeleft:duration=1.5:offset=3.5[v_out]", graph)
    }

    @Test
    fun `buildFfmpegXfadeFilterGraph chains tags correctly for 3 segments`() {
        val config = SegmentTransitionConfig(
            segments = listOf(
                LoopedSegmentConfig("1", "Seg 1", durationSec = 5.0, loopRepeatCount = 1, transitionToNext = TransitionEffect.CROSSFADE, transitionDurationSec = 1.0),
                LoopedSegmentConfig("2", "Seg 2", durationSec = 4.0, loopRepeatCount = 1, transitionToNext = TransitionEffect.DISSOLVE, transitionDurationSec = 0.5),
                LoopedSegmentConfig("3", "Seg 3", durationSec = 3.0, loopRepeatCount = 1)
            )
        )

        val graph = config.buildFfmpegXfadeFilterGraph()

        // Seg 1->2 offset: 5.0 - 1.0 = 4.0
        // Seg 2->3 offset: 4.0 (start of Seg 2) + 4.0 (len) - 0.5 (trans) = 7.5
        val expected = "[0:v][1:v]xfade=transition=fade:duration=1.0:offset=4.0[v_xfade_1]; " +
                       "[v_xfade_1][2:v]xfade=transition=dissolve:duration=0.5:offset=7.5[v_out]"

        assertEquals(expected, graph)
    }

    @Test
    fun `buildFfmpegXfadeFilterGraph caps transition duration to half of effective segment length`() {
        val config = SegmentTransitionConfig(
            segments = listOf(
                // 2.0 sec total, but asks for 1.5 sec trans
                LoopedSegmentConfig("1", "Seg 1", durationSec = 2.0, loopRepeatCount = 1, transitionDurationSec = 1.5),
                LoopedSegmentConfig("2", "Seg 2", durationSec = 4.0, loopRepeatCount = 1)
            )
        )

        val graph = config.buildFfmpegXfadeFilterGraph()
        // Cap is 2.0 / 2.0 = 1.0. Offset = 2.0 - 1.0 = 1.0
        assertEquals("[0:v][1:v]xfade=transition=fade:duration=1.0:offset=1.0[v_out]", graph)
    }
}
