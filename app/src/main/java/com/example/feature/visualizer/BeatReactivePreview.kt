package com.example.feature.visualizer

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.example.feature.visualizer.beat.BeatEffect
import com.example.feature.visualizer.beat.BeatPulse
import kotlin.math.sin

@Composable
fun BeatReactiveVisualizer(
    magnitudes: FloatArray,
    config: VisualizerRenderConfig,
    effect: BeatEffect,
    pulse: BeatPulse,
    modifier: Modifier = Modifier
) {
    val intensity = pulse.intensity.coerceIn(0f, 1f)
    val reactiveConfig = createReactiveConfig(config, effect, intensity)
    val transform = createBeatTransform(effect, intensity)

    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = transform.scale
                scaleY = transform.scale
                translationX = transform.translationX
                translationY = transform.translationY
                rotationZ = transform.rotation
            }
    ) {
        VisualizerCanvas(
            magnitudes = magnitudes,
            config = reactiveConfig,
            modifier = Modifier.fillMaxSize(),
            testTag = "visualizer_live_preview"
        )

        if (effect == BeatEffect.BEAT_FLASH && intensity > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.White.copy(alpha = intensity * 0.45f))
            )
        }

        if (effect == BeatEffect.TEXT_BOUNCE && intensity > 0f) {
            Text(
                text = "BEAT",
                style = MaterialTheme.typography.headlineLarge,
                color = reactiveConfig.secondaryColor,
                modifier = Modifier
                    .align(Alignment.Center)
                    .graphicsLayer { translationY = -36f * intensity; scaleX = 1f + intensity * 0.25f; scaleY = scaleX }
                    .alpha(intensity)
                    .padding(8.dp)
            )
        }
    }
}

private data class BeatTransform(
    val scale: Float = 1f,
    val translationX: Float = 0f,
    val translationY: Float = 0f,
    val rotation: Float = 0f
)

private fun createBeatTransform(effect: BeatEffect, intensity: Float): BeatTransform = when (effect) {
    BeatEffect.BASS_PULSE -> BeatTransform(scale = 1f + intensity * 0.08f)
    BeatEffect.KICK_ZOOM -> BeatTransform(scale = 1f + intensity * 0.18f)
    BeatEffect.SNARE_SHAKE -> BeatTransform(
        translationX = sin(intensity * 24f) * 14f,
        translationY = sin(intensity * 31f) * 8f,
        rotation = sin(intensity * 18f) * 1.5f
    )
    else -> BeatTransform()
}

private fun createReactiveConfig(
    config: VisualizerRenderConfig,
    effect: BeatEffect,
    intensity: Float
): VisualizerRenderConfig = when (effect) {
    BeatEffect.COLOR_SHIFT -> config.copy(
        primaryColor = lerpBeatColor(config.primaryColor, config.secondaryColor, intensity),
        secondaryColor = lerpBeatColor(config.secondaryColor, config.primaryColor, intensity)
    )
    BeatEffect.PARTICLE_BURST -> config.copy(
        sizeScale = config.sizeScale * (1f + intensity * 0.35f),
        glowRadius = config.glowRadius + intensity * 18f
    )
    BeatEffect.BACKGROUND_PULSE -> config.copy(
        background = VisualizerBackground.Gradient(
            topColor = lerpBeatColor(Color(0xFF0B0D12), config.primaryColor, intensity * 0.45f),
            bottomColor = lerpBeatColor(Color(0xFF131720), config.secondaryColor, intensity * 0.3f)
        )
    )
    else -> config
}

private fun lerpBeatColor(start: Color, end: Color, fraction: Float): Color {
    val safeFraction = fraction.coerceIn(0f, 1f)
    return Color(
        red = start.red + (end.red - start.red) * safeFraction,
        green = start.green + (end.green - start.green) * safeFraction,
        blue = start.blue + (end.blue - start.blue) * safeFraction,
        alpha = start.alpha + (end.alpha - start.alpha) * safeFraction
    )
}
