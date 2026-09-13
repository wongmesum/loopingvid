package com.example.feature.mastering

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.core.media.AudioAnalysisData
import com.example.core.media.VisualizerTheme

/**
 * Lightweight amplitude-envelope waveform renderer. Draws the real [AudioAnalysisData.waveformPoints]
 * (normalized 0..1) as mirrored vertical bars around a center line. When no analysis data is present
 * it renders a flat baseline rather than fabricating a signal.
 */
@Composable
fun RealtimeWaveformVisualizer(
    analysisData: AudioAnalysisData?,
    modifier: Modifier = Modifier,
    theme: VisualizerTheme = VisualizerTheme.CYAN_PINK
) {
    val points = analysisData?.waveformPoints ?: emptyList()
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(64.dp)
    ) {
        val w = size.width
        val h = size.height
        val centerY = h / 2f
        val baselineColor = theme.waveformColor.copy(alpha = 0.35f)

        // Baseline
        drawLine(
            color = baselineColor,
            start = Offset(0f, centerY),
            end = Offset(w, centerY),
            strokeWidth = 1.5f
        )

        if (points.isEmpty()) return@Canvas

        val barSpacing = w / points.size
        val barWidth = (barSpacing * 0.6f).coerceAtLeast(1f)
        points.forEachIndexed { index, raw ->
            val amp = raw.coerceIn(0f, 1f)
            val barHeight = amp * (h * 0.9f)
            val x = index * barSpacing + (barSpacing - barWidth) / 2f
            // Color by position: bass (left) -> mid -> treble (right)
            val frac = index.toFloat() / points.size
            val color: Color = when {
                frac < 0.33f -> theme.bassColor
                frac < 0.66f -> theme.midColor
                else -> theme.trebleColor
            }
            drawLine(
                color = color,
                start = Offset(x + barWidth / 2f, centerY - barHeight / 2f),
                end = Offset(x + barWidth / 2f, centerY + barHeight / 2f),
                strokeWidth = barWidth
            )
        }
    }
}
