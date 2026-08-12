package com.example.core.ffmpeg

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Regression guard: mastering FFmpeg commands must use the correct audio codec for each
 * output format and never fabricate loudness or metadata values.
 */
class FFmpegCommandBuilderMasteringTest {

    // --- Codec mapping per format ---

    @Test
    fun `mastering m4a format uses aac codec not libmp3lame`() {
        val cmd = FFmpegCommandBuilder.buildMasteringCommand(
            inputPath = "/input.wav",
            outputPath = "/output.m4a",
            audioFormat = "m4a"
        )

        val codecIndex = cmd.indexOf("-c:a")
        assertTrue("-c:a must be present", codecIndex >= 0)
        assertEquals("aac", cmd[codecIndex + 1])
        assertFalse("libmp3lame must not appear for m4a", cmd.contains("libmp3lame"))
    }

    @Test
    fun `mastering aac format uses aac codec`() {
        val cmd = FFmpegCommandBuilder.buildMasteringCommand(
            inputPath = "/input.wav",
            outputPath = "/output.aac",
            audioFormat = "aac"
        )

        val codecIndex = cmd.indexOf("-c:a")
        assertTrue("-c:a must be present", codecIndex >= 0)
        assertEquals("aac", cmd[codecIndex + 1])
    }

    @Test
    fun `mastering mp3 format uses libmp3lame codec`() {
        val cmd = FFmpegCommandBuilder.buildMasteringCommand(
            inputPath = "/input.wav",
            outputPath = "/output.mp3",
            audioFormat = "mp3"
        )

        val codecIndex = cmd.indexOf("-c:a")
        assertTrue("-c:a must be present", codecIndex >= 0)
        assertEquals("libmp3lame", cmd[codecIndex + 1])
    }

    @Test
    fun `mastering wav format uses pcm_s16le codec`() {
        val cmd = FFmpegCommandBuilder.buildMasteringCommand(
            inputPath = "/input.mp3",
            outputPath = "/output.wav",
            audioFormat = "wav"
        )

        val codecIndex = cmd.indexOf("-c:a")
        assertTrue("-c:a must be present", codecIndex >= 0)
        assertEquals("pcm_s16le", cmd[codecIndex + 1])
    }

    // --- loudnorm filter presence ---

    @Test
    fun `mastering command includes loudnorm filter with target LUFS`() {
        val cmd = FFmpegCommandBuilder.buildMasteringCommand(
            inputPath = "/input.wav",
            outputPath = "/output.m4a",
            targetLufs = -16.0,
            audioFormat = "m4a"
        )

        val afIndex = cmd.indexOf("-af")
        assertTrue("-af must be present", afIndex >= 0)
        val filterChain = cmd[afIndex + 1]
        assertTrue("loudnorm must be present", filterChain.contains("loudnorm"))
        assertTrue("Target LUFS must be set", filterChain.contains("I=-16.0"))
    }

    // --- Noise reduction ---

    @Test
    fun `mastering command includes noise reduction when enabled`() {
        val cmd = FFmpegCommandBuilder.buildMasteringCommand(
            inputPath = "/input.wav",
            outputPath = "/output.m4a",
            audioFormat = "m4a",
            isNoiseReductionEnabled = true,
            noiseReductionDb = 20f,
            noiseFloorDb = -50f
        )

        val afIndex = cmd.indexOf("-af")
        assertTrue("-af must be present", afIndex >= 0)
        val filterChain = cmd[afIndex + 1]
        assertTrue("afftdn must be present when NR enabled", filterChain.contains("afftdn"))
        assertTrue("highpass must be present when NR enabled", filterChain.contains("highpass"))
    }

    @Test
    fun `mastering command omits noise reduction when disabled`() {
        val cmd = FFmpegCommandBuilder.buildMasteringCommand(
            inputPath = "/input.wav",
            outputPath = "/output.m4a",
            audioFormat = "m4a",
            isNoiseReductionEnabled = false
        )

        val afIndex = cmd.indexOf("-af")
        assertTrue("-af must be present", afIndex >= 0)
        val filterChain = cmd[afIndex + 1]
        assertFalse("afftdn must not be present when NR disabled", filterChain.contains("afftdn"))
    }

    // --- Fade ---

    @Test
    fun `mastering command includes fade in and out when specified`() {
        val cmd = FFmpegCommandBuilder.buildMasteringCommand(
            inputPath = "/input.wav",
            outputPath = "/output.m4a",
            audioFormat = "m4a",
            fadeInSec = 2.0f,
            fadeOutSec = 3.0f,
            audioDurationSec = 180.0
        )

        val afIndex = cmd.indexOf("-af")
        val filterChain = cmd[afIndex + 1]
        assertTrue("afade=t=in must be present", filterChain.contains("afade=t=in"))
        assertTrue("afade=t=out must be present", filterChain.contains("afade=t=out"))
    }

    // --- Auto leveling ---

    @Test
    fun `mastering command includes dynaudnorm when auto leveling enabled`() {
        val cmd = FFmpegCommandBuilder.buildMasteringCommand(
            inputPath = "/input.wav",
            outputPath = "/output.m4a",
            audioFormat = "m4a",
            isAutoLevelingEnabled = true,
            autoLevelingTargetLufs = -14.0f
        )

        val afIndex = cmd.indexOf("-af")
        val filterChain = cmd[afIndex + 1]
        assertTrue("dynaudnorm must be present when auto-leveling enabled", filterChain.contains("dynaudnorm"))
    }

    // --- Output path and overwrite ---

    @Test
    fun `mastering command ends with -y and output path`() {
        val cmd = FFmpegCommandBuilder.buildMasteringCommand(
            inputPath = "/input.wav",
            outputPath = "/out/mastered.m4a",
            audioFormat = "m4a"
        )

        assertEquals("-y", cmd[cmd.size - 2])
        assertEquals("/out/mastered.m4a", cmd.last())
    }

    // --- Trimmed video mastering codec ---

    @Test
    fun `trimmed video mastering uses libopenh264 not libx264`() {
        val cmd = FFmpegCommandBuilder.buildTrimmedVideoAudioMasteringCommand(
            inputPath = "/input.mp4",
            outputPath = "/output.mp4",
            trimStartSec = 5.0,
            trimEndSec = 25.0,
            targetLufs = -14.0
        )

        val codecIndex = cmd.indexOf("-c:v")
        assertTrue("-c:v must be present", codecIndex >= 0)
        assertEquals("libopenh264", cmd[codecIndex + 1])
        assertFalse("libx264 must not appear (GPL)", cmd.contains("libx264"))
    }

    @Test
    fun `trimmed video mastering includes -t for trim duration`() {
        val cmd = FFmpegCommandBuilder.buildTrimmedVideoAudioMasteringCommand(
            inputPath = "/input.mp4",
            outputPath = "/output.mp4",
            trimStartSec = 10.0,
            trimEndSec = 30.0
        )

        val tIndex = cmd.indexOf("-t")
        assertTrue("-t must be present for trim", tIndex >= 0)
        // duration = 30 - 10 = 20
        assertEquals("20.000", cmd[tIndex + 1])
    }
}
