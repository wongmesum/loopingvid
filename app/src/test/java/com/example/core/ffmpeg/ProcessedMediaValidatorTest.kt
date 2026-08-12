package com.example.core.ffmpeg

import com.example.core.media.ProcessedMedia
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ProcessedMediaValidatorTest {

    @Test
    fun `valid video output passes validation`() {
        val result = ProcessedMediaValidator.validate(
            media = videoMedia(),
            exists = true,
            expectedDurationMs = 20_000,
            requireVideo = true,
            requireAudio = false
        )

        assertEquals(RenderValidation.Valid, result)
    }

    @Test
    fun `small but valid output is accepted`() {
        val result = ProcessedMediaValidator.validate(
            media = videoMedia(sizeBytes = 128),
            exists = true,
            expectedDurationMs = 20_000,
            requireVideo = true,
            requireAudio = false
        )

        assertEquals(RenderValidation.Valid, result)
    }

    @Test
    fun `missing required video stream fails validation`() {
        val result = ProcessedMediaValidator.validate(
            media = videoMedia(width = null, height = null, videoCodec = null),
            exists = true,
            expectedDurationMs = 20_000,
            requireVideo = true,
            requireAudio = false
        )

        assertInvalidReason(result, "video stream")
    }

    @Test
    fun `invalid video resolution fails validation`() {
        val result = ProcessedMediaValidator.validate(
            media = videoMedia(width = 0),
            exists = true,
            expectedDurationMs = 20_000,
            requireVideo = true,
            requireAudio = false
        )

        assertInvalidReason(result, "resolution")
    }

    @Test
    fun `valid mastering audio passes validation`() {
        val result = ProcessedMediaValidator.validate(
            media = audioMedia(),
            exists = true,
            expectedDurationMs = 60_000,
            requireVideo = false,
            requireAudio = true
        )

        assertEquals(RenderValidation.Valid, result)
    }

    @Test
    fun `missing required audio stream fails validation`() {
        val result = ProcessedMediaValidator.validate(
            media = audioMedia(audioCodec = null),
            exists = true,
            expectedDurationMs = 60_000,
            requireVideo = false,
            requireAudio = true
        )

        assertInvalidReason(result, "audio stream")
    }

    @Test
    fun `invalid sample rate fails mastering validation`() {
        val result = ProcessedMediaValidator.validate(
            media = audioMedia(sampleRateHz = 0),
            exists = true,
            expectedDurationMs = 60_000,
            requireVideo = false,
            requireAudio = true
        )

        assertInvalidReason(result, "sample rate")
    }

    @Test
    fun `invalid channel count fails mastering validation`() {
        val result = ProcessedMediaValidator.validate(
            media = audioMedia(channels = 0),
            exists = true,
            expectedDurationMs = 60_000,
            requireVideo = false,
            requireAudio = true
        )

        assertInvalidReason(result, "channel")
    }

    @Test
    fun `duration mismatch fails validation`() {
        val result = ProcessedMediaValidator.validate(
            media = videoMedia(durationMs = 30_000),
            exists = true,
            expectedDurationMs = 20_000,
            requireVideo = true,
            requireAudio = false
        )

        assertInvalidReason(result, "duration")
    }

    @Test
    fun `unknown expected duration still validates positive actual duration`() {
        val result = ProcessedMediaValidator.validate(
            media = videoMedia(durationMs = 20_000),
            exists = true,
            expectedDurationMs = null,
            requireVideo = true,
            requireAudio = false
        )

        assertEquals(RenderValidation.Valid, result)
    }

    private fun videoMedia(
        durationMs: Long = 20_000,
        sizeBytes: Long = 1_024,
        width: Int? = 1920,
        height: Int? = 1080,
        videoCodec: String? = "video/avc"
    ) = ProcessedMedia(
        path = "/cache/editor/edit_session.mp4",
        durationMs = durationMs,
        sizeBytes = sizeBytes,
        width = width,
        height = height,
        videoCodec = videoCodec
    )

    private fun audioMedia(
        audioCodec: String? = "audio/mp4a-latm",
        sampleRateHz: Int? = 48_000,
        channels: Int? = 2
    ) = ProcessedMedia(
        path = "/cache/mastering/master_session.m4a",
        durationMs = 60_000,
        sizeBytes = 1_024,
        audioCodec = audioCodec,
        sampleRateHz = sampleRateHz,
        channels = channels,
        audioBitrateBps = 320_000
    )

    private fun assertInvalidReason(result: RenderValidation, expectedText: String) {
        assertTrue(result is RenderValidation.Invalid)
        val reason = (result as RenderValidation.Invalid).reason.lowercase()
        assertTrue("Expected '$expectedText' in '$reason'", reason.contains(expectedText))
    }
}
