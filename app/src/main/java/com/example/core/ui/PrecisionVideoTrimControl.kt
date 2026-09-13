package com.example.core.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale
import kotlin.math.sin

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PrecisionVideoTrimControl(
    trimStartSec: Double,
    trimEndSec: Double,
    mediaDurationMs: Long,
    currentPositionMs: Long,
    onTrimChange: (startSec: Double, endSec: Double) -> Unit,
    onTrimChangeFinished: (startSec: Double, endSec: Double) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier
) {
    val maxDurationSec = if (mediaDurationMs > 0) mediaDurationMs / 1000.0 else 60.0
    val effectiveEnd = if (trimEndSec <= 0.0 || trimEndSec > maxDurationSec) maxDurationSec else trimEndSec
    val effectiveStart = trimStartSec.coerceIn(0.0, maxOf(0.0, effectiveEnd - 0.2))
    val loopDurationSec = (effectiveEnd - effectiveStart).coerceAtLeast(0.0)

    val currentPosSec = (currentPositionMs / 1000.0).coerceIn(0.0, maxDurationSec)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("video_trim_control_card"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    listOf(
                                        MaterialTheme.colorScheme.secondary,
                                        MaterialTheme.colorScheme.primary
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCut,
                            contentDescription = "Trim Control",
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Visual Segment Loop Selector",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Drag handles to define A-B loop segment points",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Active Loop Duration Pill
                Surface(
                    color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f))
                ) {
                    Text(
                        text = "Loop Segment: %.1fs".format(Locale.US, loopDurationSec),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }

            // Visual Segment Selector with Waveform Track & Integrated Draggable RangeSlider
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                // Interactive Timeline Box
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(72.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.background)
                        .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                        .testTag("visual_segment_selector_canvas")
                ) {
                    val startFrac = (effectiveStart / maxDurationSec).toFloat().coerceIn(0f, 1f)
                    val endFrac = (effectiveEnd / maxDurationSec).toFloat().coerceIn(0f, 1f)
                    val playheadFrac = (currentPosSec / maxDurationSec).toFloat().coerceIn(0f, 1f)

                    // Waveform & Loop Segment Canvas. Uses theme tokens for the A/B markers
                    // (secondary=gold for A, primary=purple for B) and the tertiary gold accent
                    // for the playhead, so this stays on-theme instead of the old cyan/violet/red
                    // palette that had no relation to the app's dark+gold scheme.
                    val markerAColor = MaterialTheme.colorScheme.secondary
                    val markerBColor = MaterialTheme.colorScheme.primary
                    val playheadColor = MaterialTheme.colorScheme.tertiary
                    val cutAreaColor = MaterialTheme.colorScheme.error
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val w = size.width
                        val h = size.height

                        // 1. Draw Waveform Bars Background
                        val numBars = 48
                        val barWidth = w / numBars
                        for (i in 0 until numBars) {
                            val barX = i * barWidth + barWidth / 2f
                            val barFrac = i.toFloat() / numBars
                            val isInsideActive = barFrac >= startFrac && barFrac <= endFrac

                            val waveVal = (sin(i * 0.45) * 0.35 + sin(i * 0.9) * 0.25 + 0.4).coerceIn(0.1, 0.95).toFloat()
                            val barHeight = h * 0.6f * waveVal

                            val barColor = if (isInsideActive) {
                                markerAColor.copy(alpha = 0.85f)
                            } else {
                                markerBColor.copy(alpha = 0.25f)
                            }

                            drawRoundRect(
                                color = barColor,
                                topLeft = Offset(barX - barWidth * 0.35f, (h - barHeight) / 2f),
                                size = Size(barWidth * 0.7f, barHeight),
                                cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
                            )
                        }

                        // 2. Unselected Cut Areas
                        if (startFrac > 0f) {
                            drawRect(
                                color = cutAreaColor.copy(alpha = 0.2f),
                                topLeft = Offset(0f, 0f),
                                size = Size(w * startFrac, h)
                            )
                        }
                        if (endFrac < 1f) {
                            drawRect(
                                color = cutAreaColor.copy(alpha = 0.2f),
                                topLeft = Offset(w * endFrac, 0f),
                                size = Size(w * (1f - endFrac), h)
                            )
                        }

                        // 3. Highlighted Loop Segment Active Range (Gradient Fill)
                        val activeWidth = w * (endFrac - startFrac)
                        if (activeWidth > 0f) {
                            drawRect(
                                brush = Brush.horizontalGradient(
                                    colors = listOf(
                                        markerAColor.copy(alpha = 0.25f),
                                        markerBColor.copy(alpha = 0.35f)
                                    ),
                                    startX = w * startFrac,
                                    endX = w * endFrac
                                ),
                                topLeft = Offset(w * startFrac, 0f),
                                size = Size(activeWidth, h)
                            )
                        }

                        // 4. Start Handle Line A
                        drawLine(
                            color = markerAColor,
                            start = Offset(w * startFrac, 0f),
                            end = Offset(w * startFrac, h),
                            strokeWidth = 3.dp.toPx()
                        )

                        // 5. End Handle Line B
                        drawLine(
                            color = markerBColor,
                            start = Offset(w * endFrac, 0f),
                            end = Offset(w * endFrac, h),
                            strokeWidth = 3.dp.toPx()
                        )

                        // 6. Playhead Indicator Line
                        val playheadX = w * playheadFrac
                        drawLine(
                            color = playheadColor,
                            start = Offset(playheadX, 0f),
                            end = Offset(playheadX, h),
                            strokeWidth = 2.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 5f), 0f)
                        )
                        drawCircle(
                            color = playheadColor,
                            radius = 5.dp.toPx(),
                            center = Offset(playheadX, 6.dp.toPx())
                        )
                    }

                    // Integrated Draggable RangeSlider Overlaid on Canvas
                    RangeSlider(
                        value = effectiveStart.toFloat()..effectiveEnd.toFloat(),
                        onValueChange = { range ->
                            val minGap = 0.2f
                            val newStart = range.start.coerceIn(0f, maxOf(0f, maxDurationSec.toFloat() - minGap))
                            val newEnd = range.endInclusive.coerceIn(newStart + minGap, maxDurationSec.toFloat())
                            onTrimChange(newStart.toDouble(), newEnd.toDouble())
                        },
                        onValueChangeFinished = {
                            onTrimChangeFinished(effectiveStart, effectiveEnd)
                        },
                        valueRange = 0f..maxDurationSec.toFloat(),
                        colors = SliderDefaults.colors(
                            thumbColor = MaterialTheme.colorScheme.secondary,
                            activeTrackColor = Color.Transparent,
                            inactiveTrackColor = Color.Transparent
                        ),
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 0.dp)
                            .testTag("draggable_loop_range_slider")
                    )
                }

                // Time Labels & Playhead Status
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "A: ${formatSeconds(effectiveStart)}",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.secondary
                    )

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.size(6.dp)
                        ) {}
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Playhead: ${formatSeconds(currentPosSec)}",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.tertiary
                        )
                    }

                    Text(
                        text = "B: ${formatSeconds(effectiveEnd)}",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Start Cut Point Fine Control Section
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Start Cut Point (A):",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = formatSeconds(effectiveStart),
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.testTag("trim_start_value_text")
                        )
                    }

                    // Set Start at Playhead Button
                    OutlinedButton(
                        onClick = {
                            val newStart = currentPosSec.coerceIn(0.0, maxOf(0.0, effectiveEnd - 0.2))
                            onTrimChange(newStart, effectiveEnd)
                        },
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("set_start_at_playhead_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Start @ Playhead", style = MaterialTheme.typography.labelMedium)
                    }
                }

                // Precision Stepper Row for Start Point
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    TrimStepperButton("-1.0s", Modifier.testTag("trim_start_minus_1")) {
                        onTrimChange((effectiveStart - 1.0).coerceAtLeast(0.0), effectiveEnd)
                    }
                    TrimStepperButton("-0.1s", Modifier.testTag("trim_start_minus_01")) {
                        onTrimChange((effectiveStart - 0.1).coerceAtLeast(0.0), effectiveEnd)
                    }
                    TrimStepperButton("+0.1s", Modifier.testTag("trim_start_plus_01")) {
                        onTrimChange((effectiveStart + 0.1).coerceAtMost(effectiveEnd - 0.2), effectiveEnd)
                    }
                    TrimStepperButton("+1.0s", Modifier.testTag("trim_start_plus_1")) {
                        onTrimChange((effectiveStart + 1.0).coerceAtMost(effectiveEnd - 0.2), effectiveEnd)
                    }
                }
            }

            // End Cut Point Fine Control Section
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "End Cut Point (B):",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = formatSeconds(effectiveEnd),
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.testTag("trim_end_value_text")
                        )
                    }

                    // Set End at Playhead Button
                    OutlinedButton(
                        onClick = {
                            val newEnd = currentPosSec.coerceIn(effectiveStart + 0.2, maxDurationSec)
                            onTrimChange(effectiveStart, newEnd)
                        },
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("set_end_at_playhead_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("End @ Playhead", style = MaterialTheme.typography.labelMedium)
                    }
                }

                // Precision Stepper Row for End Point
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    TrimStepperButton("-1.0s", Modifier.testTag("trim_end_minus_1")) {
                        onTrimChange(effectiveStart, (effectiveEnd - 1.0).coerceAtLeast(effectiveStart + 0.2))
                    }
                    TrimStepperButton("-0.1s", Modifier.testTag("trim_end_minus_01")) {
                        onTrimChange(effectiveStart, (effectiveEnd - 0.1).coerceAtLeast(effectiveStart + 0.2))
                    }
                    TrimStepperButton("+0.1s", Modifier.testTag("trim_end_plus_01")) {
                        onTrimChange(effectiveStart, (effectiveEnd + 0.1).coerceAtMost(maxDurationSec))
                    }
                    TrimStepperButton("+1.0s", Modifier.testTag("trim_end_plus_1")) {
                        onTrimChange(effectiveStart, (effectiveEnd + 1.0).coerceAtMost(maxDurationSec))
                    }
                }
            }

            // Quick Segment Presets
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Quick Loop Segment Presets",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilterChip(
                        selected = (effectiveStart == 0.0 && effectiveEnd == maxDurationSec),
                        onClick = { onTrimChange(0.0, maxDurationSec) },
                        label = { Text("Full Video", style = MaterialTheme.typography.labelMedium) },
                        modifier = Modifier.testTag("trim_preset_full")
                    )
                    FilterChip(
                        selected = (effectiveStart == 0.0 && effectiveEnd == minOf(5.0, maxDurationSec)),
                        onClick = { onTrimChange(0.0, minOf(5.0, maxDurationSec)) },
                        label = { Text("First 5s", style = MaterialTheme.typography.labelMedium) },
                        modifier = Modifier.testTag("trim_preset_5s")
                    )
                    FilterChip(
                        selected = (effectiveStart == 0.0 && effectiveEnd == minOf(10.0, maxDurationSec)),
                        onClick = { onTrimChange(0.0, minOf(10.0, maxDurationSec)) },
                        label = { Text("First 10s", style = MaterialTheme.typography.labelMedium) },
                        modifier = Modifier.testTag("trim_preset_10s")
                    )
                    FilterChip(
                        selected = false,
                        onClick = {
                            val mid = maxDurationSec / 2.0
                            val s = maxOf(0.0, mid - 5.0)
                            val e = minOf(maxDurationSec, mid + 5.0)
                            onTrimChange(s, e)
                        },
                        label = { Text("Middle 10s", style = MaterialTheme.typography.labelMedium) },
                        modifier = Modifier.testTag("trim_preset_middle")
                    )
                }
            }
        }
    }
}

@Composable
private fun TrimStepperButton(
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(6.dp),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

private fun formatSeconds(sec: Double): String {
    val totalMs = (sec * 1000).toLong()
    val minutes = (totalMs / 60000)
    val seconds = (totalMs % 60000) / 1000
    val millis = (totalMs % 1000) / 100
    return String.format(Locale.US, "%02d:%02d.%d", minutes, seconds, millis)
}
