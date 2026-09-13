package com.example.feature.live

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeDown
import androidx.compose.material.icons.automirrored.filled.VolumeMute
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class AudioEqPreset(
    val name: String,
    val bassDb: Float,
    val trebleDb: Float
)

val EQ_PRESETS = listOf(
    AudioEqPreset("Flat / Neutral", 0f, 0f),
    AudioEqPreset("Bass Boost", 8f, 0f),
    AudioEqPreset("Vocal & Clarity", -2f, 6f),
    AudioEqPreset("Treble Sparkle", 0f, 8f),
    AudioEqPreset("Deep & Punchy", 6f, 4f)
)

/**
 * Audio Adjustment Panel with Sliders for Master Volume, Bass, and Treble Controls.
 * Integrates directly with Media3 ExoPlayer audio processing capabilities.
 */
@Composable
fun AudioAdjustmentCard(
    volume: Float,
    isMuted: Boolean,
    bassGainDb: Float,
    trebleGainDb: Float,
    selectedPresetName: String,
    onVolumeChange: (Float) -> Unit,
    onMuteToggle: () -> Unit,
    onBassChange: (Float) -> Unit,
    onTrebleChange: (Float) -> Unit,
    onSelectPreset: (AudioEqPreset) -> Unit,
    onResetAdjustments: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("audio_adjustment_card"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = "Audio Equalizer Panel",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "Audio Preview Adjustments",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            // Honest label: RootEncoder (the real RTMP broadcast engine) has no
                            // on-the-fly PCM/EQ hook, so Bass/Treble here cannot reach the actual
                            // broadcast audio - only local monitoring/preview volume is real.
                            text = "Local monitoring only - does not change the broadcast audio",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Reset Button
                    IconButton(
                        onClick = onResetAdjustments,
                        modifier = Modifier.testTag("reset_audio_adjustments_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Reset Sound Adjustments",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Mute Button
                    IconButton(
                        onClick = onMuteToggle,
                        modifier = Modifier.testTag("audio_panel_mute_button")
                    ) {
                        Icon(
                            imageVector = if (isMuted || volume == 0f) Icons.AutoMirrored.Filled.VolumeMute else Icons.AutoMirrored.Filled.VolumeUp,
                            contentDescription = "Mute Toggle",
                            tint = if (isMuted) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }

            // Real-Time EQ Frequency Response Curve Visualizer
            EqFrequencyCurveCanvas(
                bassDb = if (isMuted) -12f else bassGainDb,
                trebleDb = if (isMuted) -12f else trebleGainDb,
                volume = if (isMuted) 0f else volume,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(90.dp)
                    .testTag("eq_frequency_curve_canvas")
            )

            // EQ Presets Selector Row
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "EQ Sound Presets:",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    EQ_PRESETS.forEach { preset ->
                        val isSelected = selectedPresetName == preset.name
                        FilterChip(
                            selected = isSelected,
                            onClick = { onSelectPreset(preset) },
                            label = {
                                Text(
                                    text = preset.name,
                                    fontSize = 11.sp
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = Color.White
                            ),
                            modifier = Modifier.testTag("eq_preset_chip_${preset.name.replace(" ", "_")}")
                        )
                    }
                }
            }

            // 1. Master Volume Control Slider
            AudioControlSliderTile(
                title = "Master Output Volume",
                valueText = if (isMuted) "MUTED" else "${(volume * 100).toInt()}%",
                value = if (isMuted) 0f else volume,
                valueRange = 0f..1f,
                icon = if (isMuted) Icons.AutoMirrored.Filled.VolumeMute else Icons.AutoMirrored.Filled.VolumeDown,
                activeColor = MaterialTheme.colorScheme.primary,
                testTag = "master_volume_adjustment_slider",
                onValueChange = onVolumeChange
            )

            // 2. Bass Control Slider (-12 dB to +12 dB)
            AudioControlSliderTile(
                title = "Bass Boost (Low Frequency Gain)",
                valueText = formatDb(bassGainDb),
                value = bassGainDb,
                valueRange = -12f..12f,
                icon = Icons.Default.GraphicEq,
                activeColor = MaterialTheme.colorScheme.secondary,
                testTag = "bass_gain_adjustment_slider",
                onValueChange = onBassChange
            )

            // 3. Treble Control Slider (-12 dB to +12 dB)
            AudioControlSliderTile(
                title = "Treble Clarity (High Frequency Gain)",
                valueText = formatDb(trebleGainDb),
                value = trebleGainDb,
                valueRange = -12f..12f,
                icon = Icons.Default.MusicNote,
                activeColor = Color(0xFF10B981), // Emerald Green
                testTag = "treble_gain_adjustment_slider",
                onValueChange = onTrebleChange
            )
        }
    }
}

/**
 * Individual Audio Slider Control Tile
 */
@Composable
private fun AudioControlSliderTile(
    title: String,
    valueText: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    activeColor: Color,
    testTag: String,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = activeColor,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Text(
                text = valueText,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.ExtraBold),
                color = activeColor
            )
        }

        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            colors = SliderDefaults.colors(
                thumbColor = activeColor,
                activeTrackColor = activeColor
            ),
            modifier = Modifier
                .fillMaxWidth()
                .testTag(testTag)
        )
    }
}

/**
 * Canvas drawing the frequency response curve for Bass, Mid, and Treble settings.
 */
@Composable
private fun EqFrequencyCurveCanvas(
    bassDb: Float,
    trebleDb: Float,
    volume: Float,
    modifier: Modifier = Modifier
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary
    val gridColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f)

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFF0F172A)) // Dark slate background
            .padding(8.dp)
    ) {
        Canvas(modifier = Modifier.fillMaxWidth().height(80.dp)) {
            val width = size.width
            val height = size.height
            val centerY = height / 2f

            // Draw Grid Lines (+12dB, 0dB, -12dB)
            drawLine(
                color = gridColor,
                start = Offset(0f, 0f),
                end = Offset(width, 0f),
                strokeWidth = 1f
            )
            drawLine(
                color = gridColor,
                start = Offset(0f, centerY),
                end = Offset(width, centerY),
                strokeWidth = 1.5f
            )
            drawLine(
                color = gridColor,
                start = Offset(0f, height),
                end = Offset(width, height),
                strokeWidth = 1f
            )

            // Frequency vertical grid lines (100Hz, 1kHz, 10kHz)
            val gridXs = listOf(width * 0.2f, width * 0.5f, width * 0.8f)
            gridXs.forEach { x ->
                drawLine(
                    color = gridColor,
                    start = Offset(x, 0f),
                    end = Offset(x, height),
                    strokeWidth = 1f
                )
            }

            // Calculate curve points across 5 frequency bands
            // Bass (20Hz - 250Hz), Mid (1kHz), Treble (4kHz - 20kHz)
            val bassY = centerY - (bassDb / 12f) * (centerY * 0.85f) * volume
            val midY = centerY
            val trebleY = centerY - (trebleDb / 12f) * (centerY * 0.85f) * volume

            val path = Path().apply {
                moveTo(0f, bassY)
                cubicTo(
                    width * 0.25f, bassY,
                    width * 0.35f, midY,
                    width * 0.5f, midY
                )
                cubicTo(
                    width * 0.65f, midY,
                    width * 0.75f, trebleY,
                    width, trebleY
                )
            }

            // Fill under curve
            val fillPath = Path().apply {
                addPath(path)
                lineTo(width, height)
                lineTo(0f, height)
                close()
            }

            drawPath(
                path = fillPath,
                brush = Brush.verticalGradient(
                    colors = listOf(primaryColor.copy(alpha = 0.35f), Color.Transparent)
                )
            )

            // Draw EQ Curve stroke
            drawPath(
                path = path,
                brush = Brush.horizontalGradient(
                    colors = listOf(secondaryColor, primaryColor, Color(0xFF10B981))
                ),
                style = Stroke(width = 3.dp.toPx())
            )
        }

        // Frequency Labels Footer
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("20Hz", fontSize = 8.sp, color = Color.Gray)
            Text("250Hz", fontSize = 8.sp, color = Color.Gray)
            Text("1kHz", fontSize = 8.sp, color = Color.Gray)
            Text("4kHz", fontSize = 8.sp, color = Color.Gray)
            Text("20kHz", fontSize = 8.sp, color = Color.Gray)
        }
    }
}

private fun formatDb(db: Float): String {
    return when {
        db > 0f -> "+%.1f dB".format(db)
        db < 0f -> "%.1f dB".format(db)
        else -> "0.0 dB (Flat)"
    }
}
