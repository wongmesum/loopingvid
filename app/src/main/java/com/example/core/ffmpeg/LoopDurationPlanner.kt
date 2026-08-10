package com.example.core.ffmpeg

import kotlin.math.ceil

/**
 * Pure duration math for manual loop rendering.
 *
 * Correctness relies on the output `-t` cap, not on the repeat count: `-stream_loop` only has
 * to provide *enough* passes, and `-t` truncates to the exact requested length. This keeps the
 * result correct even when the source duration cannot be probed.
 */
object LoopDurationPlanner {

    const val MIN_DURATION_SEC = 1.0
    const val MAX_DURATION_SEC = 3600.0

    /** Fallback segment length assumed when probing the real source duration fails. */
    private const val FALLBACK_SEGMENT_SEC = 1.0

    /**
     * Extra headroom applied only when the source duration is unknown. The real segment may be
     * shorter than [FALLBACK_SEGMENT_SEC], and a short output is a visible defect while surplus
     * passes cost nothing because `-t` stops the encode at the target.
     */
    private const val UNKNOWN_SOURCE_SAFETY_FACTOR = 2

    data class LoopPlan(
        val streamLoopCount: Int,
        val outputDurationSec: Double
    )

    /**
     * @param segmentDurationSec Duration of the actual input FFmpeg will loop (post-trim if
     *   trimming is active). Pass 0.0 or a negative value if the real duration is unknown.
     * @param targetDurationSec User-requested output length in seconds.
     */
    fun planLoop(segmentDurationSec: Double, targetDurationSec: Double): LoopPlan {
        val output = sanitizeTargetDuration(targetDurationSec)
        val segmentKnown = segmentDurationSec.isFinite() && segmentDurationSec > 0.0
        val segment = if (segmentKnown) segmentDurationSec else FALLBACK_SEGMENT_SEC

        val totalPasses = ceil(output / segment).toInt().coerceAtLeast(1)
        // -stream_loop N means N *extra* repeats on top of the first pass.
        val basePasses = (totalPasses - 1).coerceAtLeast(0)
        val streamLoopCount = if (segmentKnown) {
            basePasses
        } else {
            totalPasses * UNKNOWN_SOURCE_SAFETY_FACTOR
        }

        return LoopPlan(streamLoopCount = streamLoopCount, outputDurationSec = output)
    }

    /** Clamps to [MIN_DURATION_SEC, MAX_DURATION_SEC] and rejects non-finite input. */
    fun sanitizeTargetDuration(targetDurationSec: Double): Double {
        if (!targetDurationSec.isFinite()) {
            return if (targetDurationSec == Double.POSITIVE_INFINITY) {
                MAX_DURATION_SEC
            } else {
                MIN_DURATION_SEC
            }
        }
        return targetDurationSec.coerceIn(MIN_DURATION_SEC, MAX_DURATION_SEC)
    }
}
