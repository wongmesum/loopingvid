package com.example.core.media

import org.junit.Assert.assertEquals
import org.junit.Test

class ColorGradingConfigTest {

    @Test
    fun `buildFfmpegFilterString returns empty for NONE with defaults`() {
        val config = ColorGradingConfig(preset = ColorFilterPreset.NONE)
        assertEquals("", config.buildFfmpegFilterString())
    }

    @Test
    fun `buildFfmpegFilterString prepends preset snippet`() {
        val config = ColorGradingConfig(preset = ColorFilterPreset.CINEMATIC)
        assertEquals(ColorFilterPreset.CINEMATIC.ffmpegFilterSnippet, config.buildFfmpegFilterString())
    }

    @Test
    fun `buildFfmpegFilterString adds brightness and contrast`() {
        val config = ColorGradingConfig(
            preset = ColorFilterPreset.NONE,
            brightness = 0.25f,
            contrast = 1.5f
        )
        assertEquals("eq=brightness=0.25:contrast=1.50", config.buildFfmpegFilterString())
    }

    @Test
    fun `buildFfmpegFilterString ignores saturation for GRAYSCALE`() {
        val config = ColorGradingConfig(
            preset = ColorFilterPreset.GRAYSCALE,
            saturation = 2.0f
        )
        // preset snippet is hue=s=0, the eq=saturation=2.00 should NOT be appended
        assertEquals("hue=s=0", config.buildFfmpegFilterString())
    }

    @Test
    fun `buildFfmpegFilterString ignores hue for CYBERPUNK`() {
        val config = ColorGradingConfig(
            preset = ColorFilterPreset.CYBERPUNK,
            hue = 90f
        )
        // preset snippet is hue=h=160:s=2.0,eq=contrast=1.35, hue=h=90.0 should NOT be appended
        assertEquals("hue=h=160:s=2.0,eq=contrast=1.35", config.buildFfmpegFilterString())
    }

    @Test
    fun `buildFfmpegFilterString combines preset with adjustments`() {
        val config = ColorGradingConfig(
            preset = ColorFilterPreset.VIVID, // eq=saturation=1.75:contrast=1.2
            brightness = -0.1f,
            saturation = 1.5f,
            hue = 45f
        )
        val expected = "eq=saturation=1.75:contrast=1.2,eq=brightness=-0.10:saturation=1.50,hue=h=45.0"
        assertEquals(expected, config.buildFfmpegFilterString())
    }
}
