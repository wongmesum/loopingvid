package com.example.feature.visualizer

import androidx.compose.ui.graphics.Color
import com.example.core.media.SpectrumStyle

/** Render mode for the visualizer. Mirrors the existing spectrum styles but extends them. */
enum class VisualizerMode {
    BARS,
    WAVE,
    CIRCLE,
    MIRROR_BARS,
    LINE,
    PARTICLES
}

/** Config for the visualizer's background layer. */
sealed class VisualizerBackground {
    object Transparent : VisualizerBackground()
    data class SolidColor(val color: Color) : VisualizerBackground()
    data class Gradient(val topColor: Color, val bottomColor: Color) : VisualizerBackground()
    data class Image(val uri: String, val blurRadius: Float = 0f) : VisualizerBackground()
}

/**
 * Single source of truth for visualizer rendering.
 * Used by both the real-time Compose Canvas and the FFmpeg export pipeline.
 */
data class VisualizerRenderConfig(
    val mode: VisualizerMode = VisualizerMode.BARS,

    // Geometry & Layout
    val bandCount: Int = 32,
    val sizeScale: Float = 1.0f,
    val gapScale: Float = 1.0f,
    val thickness: Float = 6f,
    val cornerRadius: Float = 4f,

    // Audio Reactivity
    val sensitivityGain: Float = 1.0f,
    val smoothing: Float = 0.6f,

    // Positioning
    val offsetX: Float = 0f, // -1.0 to 1.0 (relative to center)
    val offsetY: Float = 0f, // -1.0 to 1.0
    val rotationDegrees: Float = 0f,

    // Styling & FX
    val primaryColor: Color = Color(0xFF7C5CFF),
    val secondaryColor: Color = Color(0xFF22D3EE),
    val opacity: Float = 1.0f,
    val glowRadius: Float = 0f,
    val shadowRadius: Float = 0f,
    val trailFade: Float = 0f, // 0 = no trail, 1 = infinite hold

    // Canvas
    val background: VisualizerBackground = VisualizerBackground.SolidColor(Color(0xFF0B0D12)),
    val aspectRatio: String = "16:9", // "16:9", "9:16", "1:1", "4:5"
    val safeAreaOverlay: String? = null // "none", "tiktok", "reels", "youtube"
) {
    /**
     * Converts the new VisualizerMode to the legacy SpectrumStyle if possible.
     * Used for backward compatibility with older processor flows.
     */
    fun toLegacyStyle(): SpectrumStyle {
        return when (mode) {
            VisualizerMode.BARS, VisualizerMode.MIRROR_BARS -> SpectrumStyle.BARS
            VisualizerMode.WAVE, VisualizerMode.LINE -> SpectrumStyle.WAVE
            VisualizerMode.CIRCLE, VisualizerMode.PARTICLES -> SpectrumStyle.CIRCLE
        }
    }
}
