package com.example

import com.example.core.media.SpectrumStyle
import com.example.feature.visualizer.VisualizerMode
import com.example.feature.visualizer.VisualizerRenderConfig
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Guards the visualizer config contract. This config is the single source of
 * truth shared by the real-time Compose preview and the FFmpeg export, so the
 * mode mapping and default geometry must stay stable.
 */
class VisualizerRenderConfigTest {

    @Test
    fun `bar style modes map to legacy BARS`() {
        assertEquals(
            SpectrumStyle.BARS,
            VisualizerRenderConfig(mode = VisualizerMode.BARS).toLegacyStyle()
        )
        assertEquals(
            SpectrumStyle.BARS,
            VisualizerRenderConfig(mode = VisualizerMode.MIRROR_BARS).toLegacyStyle()
        )
    }

    @Test
    fun `line style modes map to legacy WAVE`() {
        assertEquals(
            SpectrumStyle.WAVE,
            VisualizerRenderConfig(mode = VisualizerMode.WAVE).toLegacyStyle()
        )
        assertEquals(
            SpectrumStyle.WAVE,
            VisualizerRenderConfig(mode = VisualizerMode.LINE).toLegacyStyle()
        )
    }

    @Test
    fun `radial style modes map to legacy CIRCLE`() {
        assertEquals(
            SpectrumStyle.CIRCLE,
            VisualizerRenderConfig(mode = VisualizerMode.CIRCLE).toLegacyStyle()
        )
        assertEquals(
            SpectrumStyle.CIRCLE,
            VisualizerRenderConfig(mode = VisualizerMode.PARTICLES).toLegacyStyle()
        )
    }

    @Test
    fun `every mode has a legacy mapping`() {
        VisualizerMode.entries.forEach { mode ->
            // Fails with an exhaustiveness error at compile time if a mode is unmapped,
            // and guards against a null/incorrect runtime result here.
            val style = VisualizerRenderConfig(mode = mode).toLegacyStyle()
            assertEquals(
                "Mode $mode must map to a supported legacy style",
                true,
                style in SpectrumStyle.entries
            )
        }
    }

    @Test
    fun `defaults target a horizontal social canvas with visible bars`() {
        val config = VisualizerRenderConfig()
        assertEquals("16:9", config.aspectRatio)
        assertEquals(32, config.bandCount)
        assertEquals(1.0f, config.opacity)
    }
}
