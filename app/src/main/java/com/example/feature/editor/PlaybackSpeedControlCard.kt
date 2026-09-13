package com.example.feature.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.QueuePlayNext
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SlowMotionVideo
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.work.BatchExportRequest
import com.example.core.work.ExportQueueViewModel
import kotlin.math.roundToInt

data class SpeedPreset(
    val speed: Float,
    val label: String,
    val category: String
)

val SPEED_PRESETS = listOf(
    SpeedPreset(0.50f, "0.5x", "Slow-Mo"),
    SpeedPreset(0.75f, "0.75x", "Relaxed"),
    SpeedPreset(1.00f, "1.0x", "Normal"),
    SpeedPreset(1.25f, "1.25x", "Brisk"),
    SpeedPreset(1.50f, "1.5x", "Fast"),
    SpeedPreset(1.75f, "1.75x", "Rapid"),
    SpeedPreset(2.00f, "2.0x", "Timelapse")
)

@Composable
fun PlaybackSpeedControlCard(
    currentSpeed: Float,
    onSpeedChange: (Float) -> Unit,
    onResetSpeed: () -> Unit,
    baseDurationSec: Double = 15.0,
    queueViewModel: ExportQueueViewModel?,
    selectedMediaUri: String? = null,
    modifier: Modifier = Modifier
) {
    var pitchCorrectionEnabled by remember { mutableStateOf(true) }

    val adjustedDuration = baseDurationSec / currentSpeed
    val videoPtsScale = 1.0f / currentSpeed
    val vfCommand = "setpts=%.4f*PTS".format(videoPtsScale)
    val afCommand = "atempo=%.2f".format(currentSpeed)

    val speedBadgeColor = when {
        currentSpeed < 0.95f -> Color(0xFF3B82F6) // Blue for Slow
        currentSpeed > 1.05f -> Color(0xFFE11D48) // Red for Fast
        else -> MaterialTheme.colorScheme.primary // Primary for Normal
    }

    val speedCategoryText = when {
        currentSpeed <= 0.6f -> "0.5x SLOW MOTION"
        currentSpeed <= 0.85f -> "0.75x RELAXED"
        currentSpeed in 0.95f..1.05f -> "1.0x NORMAL SPEED"
        currentSpeed <= 1.35f -> "1.25x BRISK"
        currentSpeed <= 1.65f -> "1.5x FAST FORWARD"
        else -> "2.0x TIMELAPSE"
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("playback_speed_control_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(speedBadgeColor.copy(alpha = 0.15f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (currentSpeed < 1.0f) Icons.Default.SlowMotionVideo else Icons.Default.FastForward,
                            contentDescription = "FFmpeg speed control",
                            tint = speedBadgeColor,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Playback Speed (FFmpeg)",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "0.5x slow-mo to 2.0x timelapse speed processing",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Surface(
                    color = speedBadgeColor.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "%.2fx".format(currentSpeed),
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold),
                        color = speedBadgeColor,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            // 1. Quick Speed Presets
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Quick Presets",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    if (currentSpeed != 1.0f) {
                        OutlinedButton(
                            onClick = onResetSpeed,
                            modifier = Modifier.height(30.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Reset speed",
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(text = "Reset 1.0x", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(SPEED_PRESETS) { preset ->
                        val isSelected = (currentSpeed - preset.speed).let { kotlin.math.abs(it) < 0.03f }
                        val bg = if (isSelected) speedBadgeColor else MaterialTheme.colorScheme.surfaceVariant
                        val fg = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = bg,
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { onSpeedChange(preset.speed) }
                                .testTag("speed_preset_${preset.label.replace(".", "_")}")
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = preset.label,
                                    style = MaterialTheme.typography.labelLarge.copy(
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                    ),
                                    color = fg
                                )
                                Text(
                                    text = preset.category,
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                    color = fg.copy(alpha = 0.85f)
                                )
                            }
                        }
                    }
                }
            }

            // 2. Fine-Tuning Speed Slider
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Speed,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Speed Fine-Tuning Slider",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Text(
                        text = speedCategoryText,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = speedBadgeColor
                    )
                }

                Slider(
                    value = currentSpeed,
                    onValueChange = { newVal ->
                        val rounded = (newVal * 20f).roundToInt() / 20f
                        onSpeedChange(rounded)
                    },
                    valueRange = 0.5f..2.0f,
                    steps = 29, // 0.05 increments between 0.5 and 2.0
                    colors = SliderDefaults.colors(
                        thumbColor = speedBadgeColor,
                        activeTrackColor = speedBadgeColor,
                        inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("playback_speed_slider")
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "0.5x (Slow)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(text = "1.0x (Normal)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(text = "2.0x (Fast)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            // 3. Duration Impact & Pitch Toggle Stats Panel
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Timer,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Adjusted Output Duration",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Text(
                            text = "%.1fs → %.1fs".format(baseDurationSec, adjustedDuration),
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            ),
                            color = speedBadgeColor
                        )
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.MusicNote,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Column {
                                Text(
                                    text = "Audio Tempo Pitch Lock (atempo)",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Preserves natural vocal pitch while retiming audio",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Switch(
                            checked = pitchCorrectionEnabled,
                            onCheckedChange = { pitchCorrectionEnabled = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = MaterialTheme.colorScheme.primary)
                        )
                    }
                }
            }

            // 4. FFmpeg Command Technical Code Box
            Surface(
                color = Color(0xFF1E1E2E),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Code,
                                contentDescription = null,
                                tint = Color(0xFF89B4FA),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "FFmpeg Speed Filter Expressions",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = Color(0xFF89B4FA)
                            )
                        }

                        Surface(
                            color = Color(0xFF313244),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "VIDEO + AUDIO",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                                color = Color(0xFFCBA6F7),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Text(
                        text = "-vf \"$vfCommand\" ${if (pitchCorrectionEnabled) "-af \"$afCommand\"" else ""}",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            color = Color(0xFFA6ADC8)
                        )
                    )
                }
            }

            // 5. Submit Speed Processing to WorkManager Queue
            Button(
                onClick = {
                    if (queueViewModel != null && !selectedMediaUri.isNullOrBlank()) {
                        // Speed is applied by the real playbackSpeed pipeline (setpts + atempo),
                        // not a raw -vf filter string, so it is correct and audio stays in sync.
                        val request = BatchExportRequest(
                            title = "Speed Retime (%.2fx)".format(currentSpeed),
                            jobType = "SPEED_RETIME",
                            format = "mp4",
                            destinationFolder = "Movies/Retime",
                            inputUri = selectedMediaUri,
                            playbackSpeed = currentSpeed
                        )
                        queueViewModel.enqueueProjectExport(request)
                    }
                },
                enabled = queueViewModel != null && !selectedMediaUri.isNullOrBlank(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("submit_speed_retime_button"),
                colors = ButtonDefaults.buttonColors(containerColor = speedBadgeColor),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.QueuePlayNext,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Enqueue Speed Retime Job (%.2fx)".format(currentSpeed),
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                )
            }
        }
    }
}
