package com.example.feature.editor

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.SurroundSound
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Waves
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.cos
import kotlin.math.sin

enum class VisualizerMode(val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    FFT_BARS("FFT Bars", Icons.Default.BarChart),
    WAVEFORM("Waveform", Icons.Default.Waves),
    CIRCULAR("Circular Pulse", Icons.Default.SurroundSound),
    LED_GRID("LED Peak Grid", Icons.Default.GridOn)
}

data class SpectrumPalette(
    val name: String,
    val primary: Color,
    val secondary: Color
)

val SPECTRUM_PALETTES = listOf(
    SpectrumPalette("Cyberpunk", Color(0xFFA855F7), Color(0xFF06B6D4)),
    SpectrumPalette("Sunset Blaze", Color(0xFFF97316), Color(0xFFEF4444)),
    SpectrumPalette("Matrix Green", Color(0xFF10B981), Color(0xFF84CC16)),
    SpectrumPalette("Gold Glow", Color(0xFFEAB308), Color(0xFFF59E0B)),
    SpectrumPalette("Electric Pink", Color(0xFFEC4899), Color(0xFF3B82F6))
)

/**
 * Interactive Audio Spectrum Visualizer Card for EditorPage.
 * Uses real-time frequency analysis and audio processing data from Media3.
 */
@Composable
fun InteractiveAudioSpectrumCard(
    spectrumData: FloatArray,
    isPlaying: Boolean,
    peakDb: Float,
    rmsEnergy: Float,
    dominantFreqHz: Int,
    sensitivityGain: Float,
    selectedMode: VisualizerMode,
    selectedPalette: SpectrumPalette,
    bandCount: Int,
    onModeSelected: (VisualizerMode) -> Unit,
    onPaletteSelected: (SpectrumPalette) -> Unit,
    onSensitivityChange: (Float) -> Unit,
    onBandCountChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var highlightedBandIndex by remember { mutableIntStateOf(-1) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("interactive_audio_spectrum_card"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.GraphicEq,
                        contentDescription = "Spectrum Visualizer",
                        tint = selectedPalette.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "Media3 Audio Spectrum Visualizer",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Interactive Real-Time Frequency Analyzer",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Live Activity Pill
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (isPlaying) selectedPalette.primary.copy(alpha = 0.2f)
                            else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                        )
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(if (isPlaying) selectedPalette.primary else Color.Gray)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isPlaying) "LIVE FFT" else "PAUSED",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = if (isPlaying) selectedPalette.primary else Color.Gray
                        )
                    }
                }
            }

            // Mode Selection Chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                VisualizerMode.entries.forEach { mode ->
                    val isSelected = selectedMode == mode
                    FilterChip(
                        selected = isSelected,
                        onClick = { onModeSelected(mode) },
                        leadingIcon = {
                            Icon(
                                imageVector = mode.icon,
                                contentDescription = mode.label,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        label = { Text(mode.label, fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = selectedPalette.primary,
                            selectedLabelColor = Color.White,
                            selectedLeadingIconColor = Color.White
                        ),
                        modifier = Modifier.testTag("visualizer_mode_chip_${mode.name}")
                    )
                }
            }

            // Interactive Spectrum Canvas
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF0F172A)) // Sleek dark slate canvas
                    .border(1.dp, selectedPalette.primary.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
            ) {
                InteractiveSpectrumCanvas(
                    data = spectrumData,
                    isPlaying = isPlaying,
                    mode = selectedMode,
                    palette = selectedPalette,
                    highlightedIndex = highlightedBandIndex,
                    onBandTapped = { bandIdx -> highlightedBandIndex = bandIdx },
                    modifier = Modifier.matchParentSize()
                )

                // Interactive Overlay Notice
                if (highlightedBandIndex >= 0) {
                    val bandFreq = calculateBandFrequency(highlightedBandIndex, spectrumData.size)
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                            .background(Color.Black.copy(alpha = 0.75f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "Selected Band: ${bandFreq}Hz (${(spectrumData.getOrElse(highlightedBandIndex) { 0f } * 100).toInt()}%)",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = selectedPalette.secondary
                        )
                    }
                }
            }

            // Real-Time Audio Telemetry Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(10.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TelemetryMetricTile(
                    label = "Peak dB",
                    value = "%.1f dB".format(peakDb),
                    color = if (peakDb > -3f) Color.Red else selectedPalette.primary
                )
                TelemetryMetricTile(
                    label = "Dominant",
                    value = "${dominantFreqHz} Hz",
                    color = selectedPalette.secondary
                )
                TelemetryMetricTile(
                    label = "RMS Power",
                    value = "%.2f".format(rmsEnergy),
                    color = MaterialTheme.colorScheme.onSurface
                )
                TelemetryMetricTile(
                    label = "Bands",
                    value = "${spectrumData.size}",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Controls: Sensitivity Gain & Palette Picker
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                // Color Palette Selector
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Palette, contentDescription = "Palette", modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Color Theme", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        SPECTRUM_PALETTES.forEach { pal ->
                            val isSelected = selectedPalette.name == pal.name
                            Box(
                                modifier = Modifier
                                    .size(22.dp)
                                    .clip(CircleShape)
                                    .background(Brush.linearGradient(listOf(pal.primary, pal.secondary)))
                                    .border(
                                        width = if (isSelected) 2.dp else 0.dp,
                                        color = if (isSelected) Color.White else Color.Transparent,
                                        shape = CircleShape
                                    )
                                    .clickable { onPaletteSelected(pal) }
                                    .testTag("palette_chip_${pal.name.replace(" ", "_")}")
                            )
                        }
                    }
                }

                // Band Count Density Selector
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("FFT Resolution Density", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(16, 32, 64).forEach { count ->
                            val isSelected = bandCount == count
                            FilterChip(
                                selected = isSelected,
                                onClick = { onBandCountChange(count) },
                                label = { Text("${count} BARS", fontSize = 10.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = selectedPalette.primary,
                                    selectedLabelColor = Color.White
                                ),
                                modifier = Modifier.testTag("band_count_chip_$count")
                            )
                        }
                    }
                }

                // Gain Sensitivity Control
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Speed, contentDescription = "Sensitivity", modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("FFT Gain Sensitivity", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Text("%.1fx".format(sensitivityGain), style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = selectedPalette.primary)
                }

                Slider(
                    value = sensitivityGain,
                    onValueChange = onSensitivityChange,
                    valueRange = 0.5f..3.0f,
                    colors = SliderDefaults.colors(
                        thumbColor = selectedPalette.primary,
                        activeTrackColor = selectedPalette.primary
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("spectrum_sensitivity_slider")
                )
            }
        }
    }
}

@Composable
private fun TelemetryMetricTile(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = label, fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = value, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = color)
    }
}

/**
 * Custom Canvas performing high-performance rendering of real-time audio FFT data.
 */
@Composable
private fun InteractiveSpectrumCanvas(
    data: FloatArray,
    isPlaying: Boolean,
    mode: VisualizerMode,
    palette: SpectrumPalette,
    highlightedIndex: Int,
    onBandTapped: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "canvas_anim")
    val animPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 6.28318f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "animPhase"
    )

    Canvas(
        modifier = modifier
            .pointerInput(data.size) {
                detectTapGestures { offset ->
                    if (data.isNotEmpty()) {
                        val bandWidth = size.width / data.size
                        if (bandWidth > 0) {
                            val ratio = offset.x / bandWidth
                            val safeRatio = if (ratio.isNaN() || ratio.isInfinite()) 0f else ratio
                            val tappedIdx = safeRatio.toInt().coerceIn(0, data.size - 1)
                            onBandTapped(tappedIdx)
                        }
                    }
                }
            }
    ) {
        val width = size.width
        val height = size.height
        if (width <= 0 || height <= 0 || data.isEmpty()) return@Canvas

        val activePhase = if (isPlaying) animPhase else 0f
        val count = data.size

        when (mode) {
            VisualizerMode.FFT_BARS -> {
                val barWidth = (width / count) * 0.72f
                val gap = (width - (barWidth * count)) / (count + 1)

                for (i in 0 until count) {
                    val sampleMag = if (isPlaying) data[i] else (data[i] * 0.2f)
                    val isHighlighted = i == highlightedIndex
                    val animatedFactor = if (isPlaying) (sin(activePhase + i * 0.2f) * 0.15f + 0.85f) else 1.0f
                    val barHeight = (height * 0.85f * sampleMag * animatedFactor).coerceAtLeast(8f)

                    val x = gap + i * (barWidth + gap)
                    val y = height - barHeight

                    val barBrush = if (isHighlighted) {
                        Brush.verticalGradient(listOf(Color.White, palette.primary))
                    } else {
                        Brush.verticalGradient(listOf(palette.primary, palette.secondary))
                    }

                    drawRoundRect(
                        brush = barBrush,
                        topLeft = Offset(x, y),
                        size = Size(barWidth, barHeight),
                        cornerRadius = CornerRadius(4f, 4f)
                    )

                    // Draw peak dot cap above bar
                    val capY = (y - 6f).coerceAtLeast(2f)
                    drawCircle(
                        color = if (isHighlighted) Color.White else palette.secondary,
                        radius = barWidth / 3f,
                        center = Offset(x + barWidth / 2f, capY)
                    )
                }
            }

            VisualizerMode.WAVEFORM -> {
                val path = Path()
                val centerY = height / 2f
                val stepX = width / (count - 1).coerceAtLeast(1)

                for (i in 0 until count) {
                    val x = i * stepX
                    val amplitude = (data[i] * height * 0.45f) * if (isPlaying) sin(activePhase + i * 0.25f) else 0.1f
                    val y = centerY + amplitude

                    if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                }

                drawPath(
                    path = path,
                    brush = Brush.horizontalGradient(listOf(palette.primary, palette.secondary, palette.primary)),
                    style = Stroke(width = 6f)
                )

                // Render baseline
                drawLine(
                    color = palette.secondary.copy(alpha = 0.3f),
                    start = Offset(0f, centerY),
                    end = Offset(width, centerY),
                    strokeWidth = 2f
                )
            }

            VisualizerMode.CIRCULAR -> {
                val centerX = width / 2f
                val centerY = height / 2f
                val baseRadius = (minOf(width, height) * 0.28f).coerceAtLeast(20f)

                for (i in 0 until count) {
                    val angle = (i.toFloat() / count) * 2 * Math.PI
                    val mag = data[i]
                    val isHighlighted = i == highlightedIndex
                    val barLength = (baseRadius * 0.9f * mag).coerceAtLeast(6f)

                    val startX = centerX + (baseRadius * cos(angle)).toFloat()
                    val startY = centerY + (baseRadius * sin(angle)).toFloat()
                    val endX = centerX + ((baseRadius + barLength) * cos(angle)).toFloat()
                    val endY = centerY + ((baseRadius + barLength) * sin(angle)).toFloat()

                    drawLine(
                        brush = Brush.linearGradient(
                            colors = if (isHighlighted) listOf(Color.White, palette.primary) else listOf(palette.primary, palette.secondary),
                            start = Offset(startX, startY),
                            end = Offset(endX, endY)
                        ),
                        start = Offset(startX, startY),
                        end = Offset(endX, endY),
                        strokeWidth = if (isHighlighted) 8f else 5f
                    )
                }

                // Pulse inner ring
                drawCircle(
                    color = palette.primary.copy(alpha = 0.25f),
                    radius = baseRadius,
                    center = Offset(centerX, centerY),
                    style = Stroke(width = 3f)
                )
            }

            VisualizerMode.LED_GRID -> {
                val rows = 8
                val cols = count.coerceAtMost(32)
                val cellWidth = (width / cols) * 0.75f
                val cellHeight = (height / rows) * 0.75f
                val gapX = (width - (cellWidth * cols)) / (cols + 1)
                val gapY = (height - (cellHeight * rows)) / (rows + 1)

                for (c in 0 until cols) {
                    val mag = data[c]
                    val safeMag = if (mag.isNaN() || mag.isInfinite()) 0f else mag
                    val litRows = (safeMag * rows).toInt().coerceIn(1, rows)

                    for (r in 0 until rows) {
                        val isLit = (rows - 1 - r) < litRows
                        val x = gapX + c * (cellWidth + gapX)
                        val y = gapY + r * (cellHeight + gapY)

                        val ledColor = when {
                            !isLit -> Color.DarkGray.copy(alpha = 0.25f)
                            r < 2 -> Color.Red // Peak clipping LED
                            r < 4 -> Color.Yellow // Caution zone LED
                            else -> palette.primary // Normal zone LED
                        }

                        drawRoundRect(
                            color = ledColor,
                            topLeft = Offset(x, y),
                            size = Size(cellWidth, cellHeight),
                            cornerRadius = CornerRadius(2f, 2f)
                        )
                    }
                }
            }
        }
    }
}

private fun calculateBandFrequency(index: Int, totalBands: Int): Int {
    val minFreq = 20.0
    val maxFreq = 16000.0
    val ratio = index.toDouble() / (totalBands - 1).coerceAtLeast(1)
    return (minFreq * Math.pow(maxFreq / minFreq, ratio)).toInt()
}
