package com.example.core.ffmpeg

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Regression guard for manual loop duration planning.
 *
 * The output length must follow the user's requested target, not a hardcoded assumption about
 * the source being 10 seconds long.
 */
class LoopDurationPlannerTest {

    @Test
    fun `plan repeats a short source enough times to cover a manual target`() {
        val plan = LoopDurationPlanner.planLoop(segmentDurationSec = 10.0, targetDurationSec = 137.0)

        // -stream_loop counts EXTRA passes, so 14 total passes of a 10s clip cover 137s.
        assertEquals(13, plan.streamLoopCount)
        assertEquals(137.0, plan.outputDurationSec, 0.001)
    }

    @Test
    fun `plan trims without repeating when the target is shorter than the source`() {
        val plan = LoopDurationPlanner.planLoop(segmentDurationSec = 60.0, targetDurationSec = 15.0)

        assertEquals(0, plan.streamLoopCount)
        assertEquals(15.0, plan.outputDurationSec, 0.001)
    }

    @Test
    fun `plan does not repeat when target exactly matches source duration`() {
        val plan = LoopDurationPlanner.planLoop(segmentDurationSec = 30.0, targetDurationSec = 30.0)

        assertEquals(0, plan.streamLoopCount)
        assertEquals(30.0, plan.outputDurationSec, 0.001)
    }

    @Test
    fun `plan still guarantees target length when source duration is unknown`() {
        val plan = LoopDurationPlanner.planLoop(segmentDurationSec = 0.0, targetDurationSec = 120.0)

        // A failed probe must not silently shorten the render. The repeat count has to be
        // generous, because the exact length is enforced by the output duration instead.
        assertTrue(
            "Unknown source duration should still cover the target",
            plan.streamLoopCount >= 120
        )
        assertEquals(120.0, plan.outputDurationSec, 0.001)
    }

    @Test
    fun `plan rejects a non-positive target by falling back to the minimum`() {
        val plan = LoopDurationPlanner.planLoop(segmentDurationSec = 10.0, targetDurationSec = 0.0)

        assertEquals(LoopDurationPlanner.MIN_DURATION_SEC, plan.outputDurationSec, 0.001)
    }

    @Test
    fun `sanitize clamps out of range and non-finite input`() {
        assertEquals(
            LoopDurationPlanner.MIN_DURATION_SEC,
            LoopDurationPlanner.sanitizeTargetDuration(-5.0),
            0.001
        )
        assertEquals(
            LoopDurationPlanner.MAX_DURATION_SEC,
            LoopDurationPlanner.sanitizeTargetDuration(99_999.0),
            0.001
        )
        assertEquals(
            LoopDurationPlanner.MIN_DURATION_SEC,
            LoopDurationPlanner.sanitizeTargetDuration(Double.NaN),
            0.001
        )
        assertEquals(
            LoopDurationPlanner.MAX_DURATION_SEC,
            LoopDurationPlanner.sanitizeTargetDuration(Double.POSITIVE_INFINITY),
            0.001
        )
        assertEquals(137.0, LoopDurationPlanner.sanitizeTargetDuration(137.0), 0.001)
    }
}
