package com.example.core.ffmpeg

import kotlin.math.abs

/**
 * Explicit render lifecycle. A single boolean cannot distinguish "still encoding" from
 * "finished but invalid" from "cancelled", which is how a failed FFmpeg run previously
 * reached the UI as a completed render.
 */
sealed interface RenderState {
    data object Idle : RenderState

    data object Preparing : RenderState

    data class Rendering(
        val progress: Int,
        val processedMs: Long,
        val targetMs: Long
    ) : RenderState

    data class Validating(val outputPath: String) : RenderState

    data class Success(
        val outputPath: String,
        val durationMs: Long,
        val fileSizeBytes: Long,
        val resolution: String = ""
    ) : RenderState

    data class Failed(
        val message: String,
        val returnCode: Int? = null
    ) : RenderState

    data object Cancelled : RenderState
}

/**
 * Verdict for a finished render. Existence of an output file is not evidence of success:
 * FFmpeg writes a container header before it can fail, so the file may exist while being
 * empty, truncated, or the wrong length.
 */
sealed interface RenderValidation {
    data object Valid : RenderValidation
    data class Invalid(val reason: String) : RenderValidation
}

object RenderProgress {

    /**
     * Progress derived from FFmpeg statistics rather than a timer, so the bar cannot
     * advance while the encoder is stalled.
     */
    fun percentOf(processedMs: Long, targetMs: Long): Int {
        if (targetMs <= 0L) return 0
        if (processedMs <= 0L) return 0
        val ratio = processedMs.toDouble() / targetMs.toDouble()
        return (ratio * 100.0).toInt().coerceIn(0, 100)
    }
}

object RenderOutputValidator {

    /** Output length may drift by up to half a second from the request. */
    const val DURATION_TOLERANCE_SEC = 0.5

    /**
     * Validates a finished render.
     *
     * @param exists whether the output file is present
     * @param sizeBytes size of the output file on disk
     * @param actualDurationSec probed duration, or a non-positive value when unknown
     * @param requestedDurationSec duration the user asked for
     * @param returnCode FFmpeg exit code
     *
     * No minimum file size is enforced: a short or low-bitrate clip can legitimately be
     * small, so size is only checked for being empty.
     */
    fun validate(
        exists: Boolean,
        sizeBytes: Long,
        actualDurationSec: Double,
        requestedDurationSec: Double,
        returnCode: Int
    ): RenderValidation {
        if (returnCode != 0) {
            return RenderValidation.Invalid("FFmpeg exited with code $returnCode")
        }
        if (!exists) {
            return RenderValidation.Invalid("Output file was not created")
        }
        if (sizeBytes <= 0L) {
            return RenderValidation.Invalid("Output file is empty")
        }
        if (!actualDurationSec.isFinite() || actualDurationSec <= 0.0) {
            return RenderValidation.Invalid("Output duration could not be determined")
        }
        val drift = abs(actualDurationSec - requestedDurationSec)
        if (drift > DURATION_TOLERANCE_SEC) {
            return RenderValidation.Invalid(
                "Output duration %.3fs does not match requested %.3fs".format(
                    actualDurationSec,
                    requestedDurationSec
                )
            )
        }
        return RenderValidation.Valid
    }
}
