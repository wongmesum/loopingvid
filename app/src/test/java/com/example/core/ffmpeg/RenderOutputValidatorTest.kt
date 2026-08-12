package com.example.core.ffmpeg

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Runtime regression: a render is only "done" when the output has been proven to match
 * the request. Previously the pipeline reported success whenever it reached the end of the
 * coroutine, regardless of FFmpeg's exit code or what was actually on disk.
 */
class RenderOutputValidatorTest {

    private fun validate(
        exists: Boolean = true,
        sizeBytes: Long = 2_000_000L,
        actualDurationSec: Double = 30.0,
        requestedDurationSec: Double = 30.0,
        returnCode: Int = 0
    ) = RenderOutputValidator.validate(
        exists = exists,
        sizeBytes = sizeBytes,
        actualDurationSec = actualDurationSec,
        requestedDurationSec = requestedDurationSec,
        returnCode = returnCode
    )

    @Test
    fun `exact duration match is valid`() {
        assertEquals(RenderValidation.Valid, validate())
    }

    @Test
    fun `duration slightly over target stays within tolerance`() {
        assertEquals(RenderValidation.Valid, validate(actualDurationSec = 30.2))
    }

    @Test
    fun `duration slightly under target stays within tolerance`() {
        assertEquals(RenderValidation.Valid, validate(actualDurationSec = 29.8))
    }

    @Test
    fun `duration far below target is invalid`() {
        val result = validate(actualDurationSec = 10.0)
        assertTrue(result is RenderValidation.Invalid)
        assertTrue(
            "Reason should name both durations: $result",
            (result as RenderValidation.Invalid).reason.contains("10.000")
        )
    }

    @Test
    fun `zero byte output is invalid even when ffmpeg reports success`() {
        val result = validate(sizeBytes = 0L)
        assertTrue(result is RenderValidation.Invalid)
        assertEquals("Output file is empty", (result as RenderValidation.Invalid).reason)
    }

    @Test
    fun `missing output is invalid`() {
        val result = validate(exists = false)
        assertTrue(result is RenderValidation.Invalid)
        assertEquals("Output file was not created", (result as RenderValidation.Invalid).reason)
    }

    /**
     * The reported bug: FFmpeg failed, but a partial file on disk was treated as a
     * finished render.
     */
    @Test
    fun `ffmpeg failure with a partial file present is invalid`() {
        val result = validate(returnCode = 1, sizeBytes = 1_600_000L, actualDurationSec = 4.0)
        assertTrue(result is RenderValidation.Invalid)
        assertTrue(
            "Failure must be attributed to the exit code",
            (result as RenderValidation.Invalid).reason.contains("code 1")
        )
    }

    @Test
    fun `unknown output duration is invalid`() {
        val result = validate(actualDurationSec = 0.0)
        assertTrue(result is RenderValidation.Invalid)
        assertEquals(
            "Output duration could not be determined",
            (result as RenderValidation.Invalid).reason
        )
    }

    /** A small file is not proof of failure; only an empty one is. */
    @Test
    fun `small but correctly timed output is valid`() {
        assertEquals(RenderValidation.Valid, validate(sizeBytes = 40_000L))
    }

    // --- Progress math ---

    @Test
    fun `progress is the processed fraction of the target`() {
        assertEquals(50, RenderProgress.percentOf(processedMs = 50_000, targetMs = 100_000))
    }

    @Test
    fun `progress clamps a negative processed time to zero`() {
        assertEquals(0, RenderProgress.percentOf(processedMs = -5_000, targetMs = 100_000))
    }

    @Test
    fun `progress clamps beyond the target to one hundred`() {
        assertEquals(100, RenderProgress.percentOf(processedMs = 150_000, targetMs = 100_000))
    }

    @Test
    fun `progress is zero when the target is unknown`() {
        assertEquals(0, RenderProgress.percentOf(processedMs = 10_000, targetMs = 0))
    }
}
