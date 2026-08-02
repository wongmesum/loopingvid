package com.example.core.media

import androidx.compose.ui.graphics.ColorMatrix

enum class ColorFilterPreset(
    val id: String,
    val displayName: String,
    val description: String,
    val ffmpegFilterSnippet: String
) {
    NONE(
        id = "none",
        displayName = "Original",
        description = "Natural video color without filters",
        ffmpegFilterSnippet = ""
    ),
    GRAYSCALE(
        id = "grayscale",
        displayName = "Noir B&W",
        description = "Monochrome high-contrast black & white",
        ffmpegFilterSnippet = "hue=s=0"
    ),
    SEPIA(
        id = "sepia",
        displayName = "Vintage Sepia",
        description = "Nostalgic warm golden-amber sepia tone",
        ffmpegFilterSnippet = "colorchannelmixer=.393:.769:.189:0:.349:.686:.168:0:.272:.534:.131"
    ),
    VIVID(
        id = "vivid",
        displayName = "Vivid Pop",
        description = "Enhanced color saturation & crisp contrast",
        ffmpegFilterSnippet = "eq=saturation=1.75:contrast=1.2"
    ),
    CYBERPUNK(
        id = "cyberpunk",
        displayName = "Neon Cyber",
        description = "Teal & magenta futuristic color shift",
        ffmpegFilterSnippet = "hue=h=160:s=2.0,eq=contrast=1.35"
    ),
    CINEMATIC(
        id = "cinematic",
        displayName = "Cinematic Cool",
        description = "Hollywood teal & orange blockbuster grade",
        ffmpegFilterSnippet = "eq=contrast=1.2:brightness=-0.02,colorbalance=rs=-0.1:gs=-0.05:bs=0.2"
    ),
    WARM_SUNSET(
        id = "warm_sunset",
        displayName = "Warm Sunset",
        description = "Golden hour warmth with rich highlights",
        ffmpegFilterSnippet = "colorbalance=rs=0.25:gs=0.1:bs=-0.15,eq=saturation=1.25"
    ),
    RETRO_FILM(
        id = "retro_film",
        displayName = "Retro Film",
        description = "Classic muted analogue film stock",
        ffmpegFilterSnippet = "eq=contrast=1.1:brightness=-0.03:saturation=0.85,colorbalance=rs=0.1:gs=0.05:bs=-0.1"
    )
}

data class CustomColorGradingPreset(
    val id: String,
    val name: String,
    val config: ColorGradingConfig
)

data class ColorGradingConfig(
    val preset: ColorFilterPreset = ColorFilterPreset.NONE,
    val brightness: Float = 0f, // -0.5f to 0.5f
    val contrast: Float = 1.0f,   // 0.5f to 2.0f
    val saturation: Float = 1.0f, // 0.0f to 2.5f
    val hue: Float = 0f           // -180f to 180f
) {
    /**
     * Builds the complete FFmpeg video filter (-vf) argument string based on preset and sliders.
     */
    fun buildFfmpegFilterString(): String {
        val parts = mutableListOf<String>()

        if (preset.ffmpegFilterSnippet.isNotBlank()) {
            parts.add(preset.ffmpegFilterSnippet)
        }

        // Add fine-tuning eq parameters if modified
        val eqParams = mutableListOf<String>()
        if (brightness != 0f) {
            eqParams.add("brightness=%.2f".format(brightness))
        }
        if (contrast != 1.0f) {
            eqParams.add("contrast=%.2f".format(contrast))
        }
        if (saturation != 1.0f && preset != ColorFilterPreset.GRAYSCALE) {
            eqParams.add("saturation=%.2f".format(saturation))
        }

        if (eqParams.isNotEmpty()) {
            parts.add("eq=${eqParams.joinToString(":")}")
        }

        if (hue != 0f && preset != ColorFilterPreset.CYBERPUNK) {
            parts.add("hue=h=%.1f".format(hue))
        }

        return parts.joinToString(",")
    }

    /**
     * Constructs Compose ColorMatrix for real-time visual canvas rendering.
     */
    fun toComposeColorMatrix(): ColorMatrix {
        val matrix = when (preset) {
            ColorFilterPreset.GRAYSCALE -> ColorMatrix().apply { setToSaturation(0f) }
            ColorFilterPreset.SEPIA -> ColorMatrix(
                floatArrayOf(
                    0.393f, 0.769f, 0.189f, 0f, 0f,
                    0.349f, 0.686f, 0.168f, 0f, 0f,
                    0.272f, 0.534f, 0.131f, 0f, 0f,
                    0f,     0f,     0f,     1f, 0f
                )
            )
            ColorFilterPreset.VIVID -> ColorMatrix().apply { setToSaturation(1.8f * saturation) }
            ColorFilterPreset.CYBERPUNK -> ColorMatrix(
                floatArrayOf(
                    0.2f, 0.8f, 0.5f, 0f, 20f,
                    0.8f, 0.1f, 0.6f, 0f, 0f,
                    0.3f, 0.7f, 1.4f, 0f, 30f,
                    0f,   0f,   0f,   1f, 0f
                )
            )
            ColorFilterPreset.CINEMATIC -> ColorMatrix(
                floatArrayOf(
                    0.9f, 0.1f, 0.0f, 0f, 5f,
                    0.0f, 0.95f,0.05f,0f, 0f,
                    0.1f, 0.2f, 1.25f,0f, 15f,
                    0f,   0f,   0f,   1f, 0f
                )
            )
            ColorFilterPreset.WARM_SUNSET -> ColorMatrix(
                floatArrayOf(
                    1.2f, 0.1f, 0.0f, 0f, 15f,
                    0.1f, 1.05f,0.0f, 0f, 5f,
                    0.0f, 0.1f, 0.8f, 0f, -10f,
                    0f,   0f,   0f,   1f, 0f
                )
            )
            ColorFilterPreset.RETRO_FILM -> ColorMatrix(
                floatArrayOf(
                    0.85f, 0.1f, 0.05f, 0f, 10f,
                    0.05f, 0.85f, 0.1f, 0f, 10f,
                    0.1f,  0.1f,  0.75f, 0f, 5f,
                    0f,    0f,    0f,    1f, 0f
                )
            )
            ColorFilterPreset.NONE -> ColorMatrix().apply {
                if (saturation != 1.0f) {
                    setToSaturation(saturation)
                }
            }
        }

        // Apply contrast & brightness adjustments to matrix if modified
        if (contrast != 1.0f || brightness != 0f) {
            val scale = contrast
            val translate = (brightness * 255f) + (128f * (1f - scale))
            val array = matrix.values
            for (i in 0..14) {
                if (i % 5 != 4) {
                    array[i] *= scale
                } else {
                    array[i] += translate
                }
            }
        }

        return matrix
    }
}
