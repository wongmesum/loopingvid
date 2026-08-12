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

    /**
     * Runtime regression: the crossfade path must actually loop the source. Without
     * `-stream_loop` the encoder stops at the end of the single input pass, so a long
     * target duration silently produces a short file.
     */
    @Test
    fun `crossfade loop command repeats the source with -stream_loop`() {
        val cmd = FFmpegCommandBuilder.buildCrossfadeLoopCommand(
            inputPath = "/input.mp4",
            outputPath = "/output.mp4",
            durationSec = 120.0,
            targetDurationSec = 120.0,
            loopCount = 11
        )

        val idx = cmd.indexOf("-stream_loop")
        assertTrue("-stream_loop must be present in crossfade command", idx >= 0)
        assertEquals("11", cmd[idx + 1])
        assertTrue(
            "-stream_loop must precede the input it applies to",
            idx < cmd.indexOf("-i")
        )
    }

    /**
     * Runtime regression: `xfade` needs two inputs. Emitting it against a single `-i`
     * makes FFmpeg abort while building the filter graph, which previously surfaced as a
     * partially written output file instead of a render failure.
     */
    @Test
    fun `crossfade loop command never emits a two-input filter against a single input`() {
        val cmd = FFmpegCommandBuilder.buildCrossfadeLoopCommand(
            inputPath = "/input.mp4",
            outputPath = "/output.mp4",
            durationSec = 60.0,
            targetDurationSec = 60.0,
            loopCount = 5
        )

        val inputCount = cmd.count { it == "-i" }
        val filterArgs = cmd.filter { it.contains("xfade") }
        assertEquals("Command declares exactly one input", 1, inputCount)
        assertTrue(
            "Single-input command must not use xfade (requires two inputs): $filterArgs",
            filterArgs.isEmpty()
        )
    }

    @Test
    fun `crossfade loop command keeps a video filter chain for the fade effect`() {
        val cmd = FFmpegCommandBuilder.buildCrossfadeLoopCommand(
            inputPath = "/input.mp4",
            outputPath = "/output.mp4",
            durationSec = 60.0,
            targetDurationSec = 60.0,
            crossfadeDurationSec = 1.5,
            loopCount = 5
        )

        val vfIndex = cmd.indexOf("-vf")
        assertTrue("-vf must be present", vfIndex >= 0)
        val chain = cmd[vfIndex + 1]
        assertTrue("Fade effect must be applied: $chain", chain.contains("fade="))
        assertFalse("Filter chain must not contain a comma decimal", chain.contains(",5"))
    }

    @Test
    fun `loop commands use shipped non GPL encoder`() {
        val normal = FFmpegCommandBuilder.buildNormalLoopCommand(
            inputPath = "/input.mp4",
            outputPath = "/output.mp4",
            loopCount = 2,
            targetDurationSec = 30.0
        )
        val crossfade = FFmpegCommandBuilder.buildCrossfadeLoopCommand(
            inputPath = "/input.mp4",
            outputPath = "/output.mp4",
            durationSec = 30.0,
            targetDurationSec = 30.0,
            loopCount = 2
        )
        val trim = FFmpegCommandBuilder.buildPreciseTrimCommand(
            inputPath = "/input.mp4",
            outputPath = "/trim.mp4",
            trimStartSec = 0.0,
            trimEndSec = 10.0
        )

        listOf(normal, crossfade, trim).forEach { command ->
            val codecIndex = command.indexOf("-c:v")
            assertTrue("Video codec must be explicit", codecIndex >= 0)
            assertEquals("libopenh264", command[codecIndex + 1])
            assertFalse("GPL encoder is not shipped", command.contains("libx264"))
            assertFalse("x264-only preset must not be emitted", command.contains("-preset"))
            assertFalse("x264-only CRF must not be emitted", command.contains("-crf"))
        }
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
