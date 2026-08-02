package com.example.core.media

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import kotlin.math.cos
import kotlin.math.sin

enum class SpectrumStyle {
    BARS, WAVE, CIRCLE
}

@Composable
fun AudioSpectrumVisualizer(
    modifier: Modifier = Modifier,
    style: SpectrumStyle = SpectrumStyle.BARS,
    isPlaying: Boolean = true,
    barCount: Int = 24,
    primaryColor: Color = Color(0xFFA855F7),
    secondaryColor: Color = Color(0xFF06B6D4)
) {
    val infiniteTransition = rememberInfiniteTransition(label = "spectrum")
    val animPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 6.28318f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase"
    )

    Box(modifier = modifier) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height

            if (width <= 0 || height <= 0) return@Canvas

            val activePhase = if (isPlaying) animPhase else 0f

            when (style) {
                SpectrumStyle.BARS -> {
                    val barWidth = (width / barCount) * 0.65f
                    val gap = (width - (barWidth * barCount)) / (barCount + 1)

                    for (i in 0 until barCount) {
                        val factor = sin(activePhase + (i * 0.3f)) * 0.4f + 0.5f
                        val barHeight = (height * 0.8f * factor).coerceAtLeast(12f)
                        val x = gap + i * (barWidth + gap)
                        val y = (height - barHeight) / 2f

                        drawRoundRect(
                            brush = Brush.verticalGradient(
                                colors = listOf(primaryColor, secondaryColor)
                            ),
                            topLeft = Offset(x, y),
                            size = Size(barWidth, barHeight),
                            cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
                        )
                    }
                }
                SpectrumStyle.WAVE -> {
                    val path = Path()
                    val centerY = height / 2f
                    val points = 50
                    val stepX = width / points

                    path.moveTo(0f, centerY)

                    for (i in 0..points) {
                        val x = i * stepX
                        val wave1 = sin(activePhase + (i * 0.2f)) * (height * 0.3f)
                        val wave2 = cos(activePhase * 0.7f + (i * 0.15f)) * (height * 0.15f)
                        val y = centerY + if (isPlaying) (wave1 + wave2) else 0f

                        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                    }

                    drawPath(
                        path = path,
                        brush = Brush.horizontalGradient(
                            colors = listOf(primaryColor, secondaryColor, primaryColor)
                        ),
                        style = Stroke(width = 6f)
                    )
                }
                SpectrumStyle.CIRCLE -> {
                    val centerX = width / 2f
                    val centerY = height / 2f
                    val baseRadius = (minOf(width, height) / 3f).coerceAtLeast(20f)
                    val circleBarCount = 32

                    for (i in 0 until circleBarCount) {
                        val angle = (i.toFloat() / circleBarCount) * 2 * Math.PI
                        val factor = sin(activePhase + (i * 0.4f)) * 0.35f + 0.5f
                        val barLength = (baseRadius * 0.6f * factor).coerceAtLeast(8f)

                        val startX = centerX + (baseRadius * cos(angle)).toFloat()
                        val startY = centerY + (baseRadius * sin(angle)).toFloat()
                        val endX = centerX + ((baseRadius + barLength) * cos(angle)).toFloat()
                        val endY = centerY + ((baseRadius + barLength) * sin(angle)).toFloat()

                        drawLine(
                            brush = Brush.linearGradient(
                                colors = listOf(primaryColor, secondaryColor),
                                start = Offset(startX, startY),
                                end = Offset(endX, endY)
                            ),
                            start = Offset(startX, startY),
                            end = Offset(endX, endY),
                            strokeWidth = 6f
                        )
                    }
                }
            }
        }
    }
}
