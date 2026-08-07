package com.example

import androidx.compose.ui.graphics.Color
import com.example.feature.visualizer.VisualizerBackground
import com.example.feature.visualizer.VisualizerExportCommandBuilder
import com.example.feature.visualizer.VisualizerMode
import com.example.feature.visualizer.VisualizerRenderConfig
import com.example.feature.visualizer.beat.BeatEffect
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Guards that the exported FFmpeg command is derived from the same
 * [VisualizerRenderConfig] that drives the on-screen preview, so what the user
 * sees is what gets rendered.
 */
class VisualizerExportCommandTest {

    private val audio = "/audio/track.mp3"
    private val output = "/out/visualizer.mp4"

    @Test
    fun `audio input is always present and mapped`() {
        val args = VisualizerExportCommandBuilder.build(audio, output, VisualizerRenderConfig())

        assertTrue(args.contains(audio))
        assertTrue(args.contains("-y"))
        assertEquals(output, args.last())
    }

    @Test
    fun `aspect ratio 9 by 16 produces a portrait canvas`() {
        val args = VisualizerExportCommandBuilder.build(
            audio,
            output,
            VisualizerRenderConfig(aspectRatio = "9:16")
        )

        val filter = args[args.indexOf("-filter_complex") + 1]
        assertTrue("Portrait export must target 1080x1920", filter.contains("1080x1920"))
    }

    @Test
    fun `aspect ratio 1 by 1 produces a square canvas`() {
        val args = VisualizerExportCommandBuilder.build(
            audio,
            output,
            VisualizerRenderConfig(aspectRatio = "1:1")
        )

        val filter = args[args.indexOf("-filter_complex") + 1]
        assertTrue(filter.contains("1080x1080"))
    }

    @Test
    fun `bars mode uses showspectrum style filter`() {
        val args = VisualizerExportCommandBuilder.build(
            audio,
            output,
            VisualizerRenderConfig(mode = VisualizerMode.BARS)
        )

        val filter = args[args.indexOf("-filter_complex") + 1]
        assertTrue("Bars must render via showfreqs bars", filter.contains("showfreqs"))
        assertTrue(filter.contains("mode=bar"))
    }

    @Test
    fun `wave and line modes use showwaves`() {
        listOf(VisualizerMode.WAVE, VisualizerMode.LINE).forEach { mode ->
            val args = VisualizerExportCommandBuilder.build(
                audio,
                output,
                VisualizerRenderConfig(mode = mode)
            )
            val filter = args[args.indexOf("-filter_complex") + 1]
            assertTrue("$mode must render via showwaves", filter.contains("showwaves"))
        }
    }

    @Test
    fun `circle and particles modes use showcqt radial rendering`() {
        listOf(VisualizerMode.CIRCLE, VisualizerMode.PARTICLES).forEach { mode ->
            val args = VisualizerExportCommandBuilder.build(
                audio,
                output,
                VisualizerRenderConfig(mode = mode)
            )
            val filter = args[args.indexOf("-filter_complex") + 1]
            assertTrue("$mode must render via showcqt", filter.contains("showcqt"))
        }
    }

    @Test
    fun `band count from config drives the frequency resolution`() {
        val args = VisualizerExportCommandBuilder.build(
            audio,
            output,
            VisualizerRenderConfig(mode = VisualizerMode.BARS, bandCount = 48)
        )

        val filter = args[args.indexOf("-filter_complex") + 1]
        assertTrue("Band count must reach the filter graph", filter.contains("48"))
    }

    @Test
    fun `primary color is emitted as an ffmpeg hex color`() {
        val args = VisualizerExportCommandBuilder.build(
            audio,
            output,
            VisualizerRenderConfig(mode = VisualizerMode.WAVE, primaryColor = Color(0xFF7C5CFF))
        )

        val filter = args[args.indexOf("-filter_complex") + 1]
        assertTrue("Expected 0x7C5CFF in filter, got: $filter", filter.contains("0x7C5CFF", ignoreCase = true))
    }

    @Test
    fun `gradient background emits two color stops`() {
        val args = VisualizerExportCommandBuilder.build(
            audio,
            output,
            VisualizerRenderConfig(
                background = VisualizerBackground.Gradient(Color(0xFF0B0D12), Color(0xFF131720))
            )
        )

        val filter = args[args.indexOf("-filter_complex") + 1]
        assertTrue(filter.contains("0x0B0D12", ignoreCase = true))
        assertTrue(filter.contains("0x131720", ignoreCase = true))
    }

    @Test
    fun `image background is added as a second input`() {
        val args = VisualizerExportCommandBuilder.build(
            audio,
            output,
            VisualizerRenderConfig(background = VisualizerBackground.Image("/img/bg.jpg"))
        )

        assertTrue(args.contains("/img/bg.jpg"))
        assertEquals("Audio plus background image means two inputs", 2, args.count { it == "-i" })
    }

    @Test
    fun `no image background means a single input`() {
        val args = VisualizerExportCommandBuilder.build(audio, output, VisualizerRenderConfig())

        assertEquals(1, args.count { it == "-i" })
    }

    @Test
    fun `opacity below one emits a blend alpha`() {
        val args = VisualizerExportCommandBuilder.build(
            audio,
            output,
            VisualizerRenderConfig(opacity = 0.5f)
        )

        val filter = args[args.indexOf("-filter_complex") + 1]
        assertTrue("Expected alpha term for 50% opacity", filter.contains("0.50"))
    }

    @Test
    fun `rotation emits a rotate filter`() {
        val args = VisualizerExportCommandBuilder.build(
            audio,
            output,
            VisualizerRenderConfig(rotationDegrees = 90f)
        )

        val filter = args[args.indexOf("-filter_complex") + 1]
        assertTrue(filter.contains("rotate"))
    }

    @Test
    fun `zero rotation omits the rotate filter`() {
        val args = VisualizerExportCommandBuilder.build(
            audio,
            output,
            VisualizerRenderConfig(rotationDegrees = 0f)
        )

        val filter = args[args.indexOf("-filter_complex") + 1]
        assertTrue(!filter.contains("rotate"))
    }

    @Test
    fun `beat markers become a timeline filter when effects are active`() {
        val args = VisualizerExportCommandBuilder.build(
            audioPath = audio,
            outputPath = output,
            config = VisualizerRenderConfig(),
            beatMarkersMs = listOf(0L, 500L, 1000L),
            beatEffectExpression = BeatEffect.BASS_PULSE.name
        )

        val filter = args[args.indexOf("-filter_complex") + 1]
        assertTrue("Beat markers must appear as timestamps", filter.contains("0.500"))
        assertTrue(filter.contains("brightness=0.08"))
    }

    @Test
    fun `each beat effect maps to a distinct export filter`() {
        val filters = BeatEffect.entries.map { effect ->
            val args = VisualizerExportCommandBuilder.build(
                audioPath = audio,
                outputPath = output,
                config = VisualizerRenderConfig(),
                beatMarkersMs = listOf(500L),
                beatEffectExpression = effect.name
            )
            args[args.indexOf("-filter_complex") + 1]
        }

        assertEquals(BeatEffect.entries.size, filters.distinct().size)
        assertTrue(filters.all { it.contains("0.500") })
    }

    @Test
    fun `transparent background falls back to opaque black for h264`() {
        val args = VisualizerExportCommandBuilder.build(
            audio,
            output,
            VisualizerRenderConfig(background = VisualizerBackground.Transparent)
        )

        val filter = args[args.indexOf("-filter_complex") + 1]
        assertTrue(filter.contains("color=c=black:"))
        assertTrue(!filter.contains("black@0.0"))
        assertTrue(args.contains("yuv420p"))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `blank audio path is rejected`() {
        VisualizerExportCommandBuilder.build("", output, VisualizerRenderConfig())
    }
}
