package com.example.feature.visualizer

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.testTag
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Real-time visualizer canvas. Draws the six visualizer modes from
 * [VisualizerRenderConfig] using live FFT magnitudes supplied by the caller.
 *
 * The drawing math lives in [drawVisualizer] so the preview and any offline
 * frame renderer stay pixel-consistent.
 */
@Composable
fun VisualizerCanvas(
    magnitudes: FloatArray,
    config: VisualizerRenderConfig,
    modifier: Modifier = Modifier,
    testTag: String = "visualizer_canvas"
) {
    Canvas(modifier = modifier.testTag(testTag)) {
        drawVisualizerBackground(config)
        if (magnitudes.isEmpty() || size.minDimension <= 0f) return@Canvas

        val centerOffset = Offset(
            x = size.width / 2f + config.offsetX * size.width / 2f,
            y = size.height / 2f + config.offsetY * size.height / 2f
        )

        rotate(degrees = config.rotationDegrees, pivot = centerOffset) {
            drawVisualizer(magnitudes, config, centerOffset)
        }
    }
}

/** Paints the configured background layer. Image backgrounds are drawn by the caller behind the canvas. */
private fun DrawScope.drawVisualizerBackground(config: VisualizerRenderConfig) {
    when (val background = config.background) {
        is VisualizerBackground.SolidColor -> drawRect(color = background.color)
        is VisualizerBackground.Gradient -> drawRect(
            brush = Brush.verticalGradient(listOf(background.topColor, background.bottomColor))
        )
        // Transparent and Image backgrounds intentionally paint nothing here.
        VisualizerBackground.Transparent -> Unit
        is VisualizerBackground.Image -> Unit
    }
}

/** Dispatches to the per-mode renderer. Shared by preview and offline rendering. */
fun DrawScope.drawVisualizer(
    magnitudes: FloatArray,
    config: VisualizerRenderConfig,
    center: Offset
) {
    val brush = Brush.verticalGradient(listOf(config.primaryColor, config.secondaryColor))
    val alpha = config.opacity.coerceIn(0f, 1f)

    when (config.mode) {
        VisualizerMode.BARS -> drawBars(magnitudes, config, brush, alpha, mirrored = false)
        VisualizerMode.MIRROR_BARS -> drawBars(magnitudes, config, brush, alpha, mirrored = true)
        VisualizerMode.WAVE -> drawWaveOrLine(magnitudes, config, brush, alpha, smooth = true)
        VisualizerMode.LINE -> drawWaveOrLine(magnitudes, config, brush, alpha, smooth = false)
        VisualizerMode.CIRCLE -> drawRadialSpokes(magnitudes, config, brush, alpha, center)
        VisualizerMode.PARTICLES -> drawParticles(magnitudes, config, alpha, center)
    }
}

private fun DrawScope.drawBars(
    magnitudes: FloatArray,
    config: VisualizerRenderConfig,
    brush: Brush,
    alpha: Float,
    mirrored: Boolean
) {
    val count = magnitudes.size
    val slotWidth = size.width / count
    val barWidth = (slotWidth * 0.72f * config.sizeScale).coerceAtLeast(1f)
    val gap = (slotWidth - barWidth).coerceAtLeast(0f) * config.gapScale
    val maxHeight = if (mirrored) size.height / 2f else size.height
    val baseline = if (mirrored) size.height / 2f else size.height

    for (index in 0 until count) {
        val magnitude = magnitudes[index].coerceIn(0f, 1f)
        val barHeight = (maxHeight * 0.9f * magnitude).coerceAtLeast(2f)
        val x = index * slotWidth + gap / 2f

        if (config.glowRadius > 0f) {
            drawRoundRect(
                color = config.primaryColor.copy(alpha = alpha * 0.25f),
                topLeft = Offset(x - config.glowRadius, baseline - barHeight - config.glowRadius),
                size = Size(barWidth + config.glowRadius * 2f, barHeight + config.glowRadius),
                cornerRadius = CornerRadius(config.cornerRadius, config.cornerRadius)
            )
        }

        drawRoundRect(
            brush = brush,
            topLeft = Offset(x, baseline - barHeight),
            size = Size(barWidth, barHeight),
            cornerRadius = CornerRadius(config.cornerRadius, config.cornerRadius),
            alpha = alpha
        )

        if (mirrored) {
            drawRoundRect(
                brush = brush,
                topLeft = Offset(x, baseline),
                size = Size(barWidth, barHeight),
                cornerRadius = CornerRadius(config.cornerRadius, config.cornerRadius),
                alpha = alpha * 0.55f
            )
        }
    }
}

private fun DrawScope.drawWaveOrLine(
    magnitudes: FloatArray,
    config: VisualizerRenderConfig,
    brush: Brush,
    alpha: Float,
    smooth: Boolean
) {
    val count = magnitudes.size
    if (count < 2) return

    val centerY = size.height / 2f
    val stepX = size.width / (count - 1)
    val amplitude = size.height * 0.42f * config.sizeScale
    val path = Path()

    for (index in 0 until count) {
        val magnitude = magnitudes[index].coerceIn(0f, 1f)
        val x = index * stepX
        val y = centerY - (magnitude - 0.5f) * 2f * amplitude

        when {
            index == 0 -> path.moveTo(x, y)
            smooth -> {
                val previousX = (index - 1) * stepX
                val midX = (previousX + x) / 2f
                path.quadraticBezierTo(previousX, y, midX, y)
                path.lineTo(x, y)
            }
            else -> path.lineTo(x, y)
        }
    }

    if (config.shadowRadius > 0f) {
        drawPath(
            path = path,
            color = Color.Black.copy(alpha = alpha * 0.35f),
            style = Stroke(width = config.thickness + config.shadowRadius)
        )
    }

    drawPath(
        path = path,
        brush = brush,
        style = Stroke(width = config.thickness.coerceAtLeast(1f)),
        alpha = alpha
    )
}

private fun DrawScope.drawRadialSpokes(
    magnitudes: FloatArray,
    config: VisualizerRenderConfig,
    brush: Brush,
    alpha: Float,
    center: Offset
) {
    val count = magnitudes.size
    val baseRadius = size.minDimension * 0.22f * config.sizeScale

    drawCircle(
        color = config.primaryColor.copy(alpha = alpha * 0.3f),
        radius = baseRadius,
        center = center,
        style = Stroke(width = config.thickness / 2f)
    )

    for (index in 0 until count) {
        val magnitude = magnitudes[index].coerceIn(0f, 1f)
        val angle = (index.toFloat() / count) * 2f * PI.toFloat()
        val spokeLength = baseRadius * magnitude * 1.1f

        val start = Offset(
            x = center.x + baseRadius * cos(angle),
            y = center.y + baseRadius * sin(angle)
        )
        val end = Offset(
            x = center.x + (baseRadius + spokeLength) * cos(angle),
            y = center.y + (baseRadius + spokeLength) * sin(angle)
        )

        drawLine(
            brush = brush,
            start = start,
            end = end,
            strokeWidth = config.thickness.coerceAtLeast(1f),
            alpha = alpha
        )
    }
}

private fun DrawScope.drawParticles(
    magnitudes: FloatArray,
    config: VisualizerRenderConfig,
    alpha: Float,
    center: Offset
) {
    val count = magnitudes.size
    val maxRadius = size.minDimension * 0.45f * config.sizeScale

    for (index in 0 until count) {
        val magnitude = magnitudes[index].coerceIn(0f, 1f)
        // Golden-angle placement keeps particles evenly distributed as band count changes.
        val angle = index * 2.39996f
        val distance = maxRadius * magnitude
        val particleRadius = (config.thickness * 0.6f * (0.4f + magnitude)).coerceAtLeast(1f)

        val position = Offset(
            x = center.x + distance * cos(angle),
            y = center.y + distance * sin(angle)
        )

        if (config.glowRadius > 0f) {
            drawCircle(
                color = config.secondaryColor.copy(alpha = alpha * 0.25f),
                radius = particleRadius + config.glowRadius,
                center = position
            )
        }

        drawCircle(
            color = lerpColor(config.primaryColor, config.secondaryColor, magnitude),
            radius = particleRadius,
            center = position,
            alpha = alpha
        )
    }
}

private fun lerpColor(start: Color, end: Color, fraction: Float): Color {
    val clamped = fraction.coerceIn(0f, 1f)
    return Color(
        red = start.red + (end.red - start.red) * clamped,
        green = start.green + (end.green - start.green) * clamped,
        blue = start.blue + (end.blue - start.blue) * clamped,
        alpha = start.alpha + (end.alpha - start.alpha) * clamped
    )
}
