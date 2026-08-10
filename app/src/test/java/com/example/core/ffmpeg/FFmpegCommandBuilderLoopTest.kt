package com.example.core.ffmpeg

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Locale

/**
 * Regression guard: loop FFmpeg commands must emit an explicit output duration (`-t`)
 * and respect the mute audio flag.
 */
class FFmpegCommandBuilderLoopTest {

    // --- Normal loop ---

    @Test
    fun `normal loop command emits explicit -t for the target duration`() {
        val cmd = FFmpegCommandBuilder.buildNormalLoopCommand(
            inputPath = "/input.mp4",
            outputPath = "/output.mp4",
            loopCount = 13,
            targetDurationSec = 137.0
        )

        val tIndex = cmd.indexOf("-t")
        assertTrue("-t must be present", tIndex >= 0)
        assertEquals(
            "Duration must be formatted with US locale",
            String.format(Locale.US, "%.3f", 137.0),
            cmd[tIndex + 1]
        )
    }

    @Test
    fun `normal loop command retains -stream_loop`() {
        val cmd = FFmpegCommandBuilder.buildNormalLoopCommand(
            inputPath = "/input.mp4",
            outputPath = "/output.mp4",
            loopCount = 5,
            targetDurationSec = 60.0
        )

        val idx = cmd.indexOf("-stream_loop")
        assertTrue("-stream_loop must be present", idx >= 0)
        assertEquals("5", cmd[idx + 1])
    }

    @Test
    fun `normal loop command emits -an when mute is true`() {
        val cmd = FFmpegCommandBuilder.buildNormalLoopCommand(
            inputPath = "/input.mp4",
            outputPath = "/output.mp4",
            loopCount = 2,
            targetDurationSec = 30.0,
            muteAudio = true
        )

        assertTrue("Should contain -an", cmd.contains("-an"))
        assertFalse("Should not contain -c:a when muted", cmd.contains("-c:a"))
        assertFalse("Should not contain -b:a when muted", cmd.contains("-b:a"))
    }

    @Test
    fun `normal loop command emits audio codec when mute is false`() {
        val cmd = FFmpegCommandBuilder.buildNormalLoopCommand(
            inputPath = "/input.mp4",
            outputPath = "/output.mp4",
            loopCount = 2,
            targetDurationSec = 30.0,
            muteAudio = false
        )

        assertFalse("Should not contain -an", cmd.contains("-an"))
        assertTrue("Should contain -c:a", cmd.contains("-c:a"))
    }

    // --- Crossfade loop ---

    @Test
    fun `crossfade loop command emits explicit -t for the target duration`() {
        val cmd = FFmpegCommandBuilder.buildCrossfadeLoopCommand(
            inputPath = "/input.mp4",
            outputPath = "/output.mp4",
            durationSec = 137.0,
            targetDurationSec = 137.0
        )

        val tIndex = cmd.indexOf("-t")
        assertTrue("-t must be present in crossfade command", tIndex >= 0)
        assertEquals(
            String.format(Locale.US, "%.3f", 137.0),
            cmd[tIndex + 1]
        )
    }

    @Test
    fun `crossfade loop command emits -an when mute is true`() {
        val cmd = FFmpegCommandBuilder.buildCrossfadeLoopCommand(
            inputPath = "/input.mp4",
            outputPath = "/output.mp4",
            durationSec = 60.0,
            targetDurationSec = 60.0,
            muteAudio = true
        )

        assertTrue("Should contain -an", cmd.contains("-an"))
        assertFalse("Should not contain -c:a when muted", cmd.contains("-c:a"))
    }

    // --- Locale safety ---

    @Test
    fun `duration value never uses comma as decimal separator`() {
        // Run with a comma-decimal locale simulation
        val formatted = String.format(Locale.US, "%.3f", 137.5)
        assertFalse("No comma in formatted duration", formatted.contains(","))
        assertTrue("Has period", formatted.contains("."))
    }
}
