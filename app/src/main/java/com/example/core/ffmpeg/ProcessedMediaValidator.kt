package com.example.core.ffmpeg

import com.example.core.media.ProcessedMedia
import kotlin.math.abs

/** Shared post-probe validation for video and audio processing outputs. */
object ProcessedMediaValidator {

    private const val DURATION_TOLERANCE_MS = 500L

    fun validate(
        media: ProcessedMedia,
        exists: Boolean,
        expectedDurationMs: Long?,
        requireVideo: Boolean,
        requireAudio: Boolean
    ): RenderValidation {
        if (!exists) return RenderValidation.Invalid("Output file was not created")
        if (media.sizeBytes <= 0L) return RenderValidation.Invalid("Output file is empty")
        if (media.durationMs <= 0L) return RenderValidation.Invalid("Output duration is invalid")

        if (expectedDurationMs != null && expectedDurationMs > 0L) {
            val durationDrift = abs(media.durationMs - expectedDurationMs)
            if (durationDrift > DURATION_TOLERANCE_MS) {
                return RenderValidation.Invalid(
                    "Output duration ${media.durationMs}ms does not match expected ${expectedDurationMs}ms"
                )
            }
        }

        if (requireVideo) {
            if (media.videoCodec.isNullOrBlank()) {
                return RenderValidation.Invalid("Required video stream is missing")
            }
            if ((media.width ?: 0) <= 0 || (media.height ?: 0) <= 0) {
                return RenderValidation.Invalid("Output video resolution is invalid")
            }
        }

        if (requireAudio) {
            if (media.audioCodec.isNullOrBlank()) {
                return RenderValidation.Invalid("Required audio stream is missing")
            }
            if ((media.sampleRateHz ?: 0) <= 0) {
                return RenderValidation.Invalid("Output audio sample rate is invalid")
            }
            if ((media.channels ?: 0) <= 0) {
                return RenderValidation.Invalid("Output audio channel count is invalid")
            }
        }

        return RenderValidation.Valid
    }
}
