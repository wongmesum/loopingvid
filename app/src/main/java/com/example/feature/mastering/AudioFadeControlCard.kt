package com.example.feature.mastering

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material.icons.automirrored.filled.VolumeDown
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt

@Composable
fun AudioFadeControlCard(
    fadeInSec: Float,
    fadeOutSec: Float,
    onFadeInChanged: (Float) -> Unit,
    onFadeInChangeFinished: (Float) -> Unit = {},
    onFadeOutChanged: (Float) -> Unit,
    onFadeOutChangeFinished: (Float) -> Unit = {},
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("audio_fade_control_card"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
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
                            .size(36.dp)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Timeline,
                            contentDescription = "Track Fades",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Track Fade In & Fade Out",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Define smooth volume ramping for track intros & outros",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Interactive Fade Volume Envelope Canvas
            FadeEnvelopeCanvas(
                fadeInSec = fadeInSec,
                fadeOutSec = fadeOutSec,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(90.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF12141C))
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
            )

            // Fade-In Section
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp))
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                            contentDescription = "Fade In",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Fade-In Duration",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Text(
                            text = if (fadeInSec <= 0f) "OFF (0.0s)" else "%.1fs".format(fadeInSec),
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }

                // Preset Chips for Fade-In
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val presets = listOf(0.0f, 0.5f, 1.0f, 1.5f, 2.0f, 3.0f, 5.0f)
                    items(presets) { presetVal ->
                        val isSelected = kotlin.math.abs(fadeInSec - presetVal) < 0.05f
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                onFadeInChanged(presetVal)
                                onFadeInChangeFinished(presetVal)
                            },
                            label = {
                                Text(
                                    text = if (presetVal == 0f) "OFF" else "${presetVal}s",
                                    fontSize = 11.sp
                                )
                            },
                            modifier = Modifier.testTag("preset_fade_in_${(presetVal * 10).toInt()}"),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                            )
                        )
                    }
                }

                // Slider
                Slider(
                    value = fadeInSec,
                    onValueChange = onFadeInChanged,
                    onValueChangeFinished = { onFadeInChangeFinished(fadeInSec) },
                    valueRange = 0.0f..10.0f,
                    steps = 99, // 0.1s increments
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("fade_in_slider"),
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.primary,
                        activeTrackColor = MaterialTheme.colorScheme.primary
                    )
                )
            }

            // Fade-Out Section
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp))
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.VolumeDown,
                            contentDescription = "Fade Out",
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Fade-Out Duration",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer
                    ) {
                        Text(
                            text = if (fadeOutSec <= 0f) "OFF (0.0s)" else "%.1fs".format(fadeOutSec),
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }

                // Preset Chips for Fade-Out
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val presets = listOf(0.0f, 0.5f, 1.0f, 2.0f, 3.0f, 5.0f, 8.0f)
                    items(presets) { presetVal ->
                        val isSelected = kotlin.math.abs(fadeOutSec - presetVal) < 0.05f
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                onFadeOutChanged(presetVal)
                                onFadeOutChangeFinished(presetVal)
                            },
                            label = {
                                Text(
                                    text = if (presetVal == 0f) "OFF" else "${presetVal}s",
                                    fontSize = 11.sp
                                )
                            },
                            modifier = Modifier.testTag("preset_fade_out_${(presetVal * 10).toInt()}"),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.secondary,
                                selectedLabelColor = MaterialTheme.colorScheme.onSecondary
                            )
                        )
                    }
                }

                // Slider
                Slider(
                    value = fadeOutSec,
                    onValueChange = onFadeOutChanged,
                    onValueChangeFinished = { onFadeOutChangeFinished(fadeOutSec) },
                    valueRange = 0.0f..10.0f,
                    steps = 99, // 0.1s increments
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("fade_out_slider"),
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.secondary,
                        activeTrackColor = MaterialTheme.colorScheme.secondary
                    )
                )
            }

            // Pro Tip Note
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f), RoundedCornerShape(8.dp))
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Tip: 0.5s–1.5s fade-in avoids audio start pops; 2.0s–3.0s fade-out provides broadcast-ready audio endings.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun FadeEnvelopeCanvas(
    fadeInSec: Float,
    fadeOutSec: Float,
    modifier: Modifier = Modifier
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary
    val surfaceVariantColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val paddingX = 16.dp.toPx()
        val paddingY = 12.dp.toPx()
        val graphW = w - paddingX * 2
        val graphH = h - paddingY * 2

        val topY = paddingY
        val bottomY = h - paddingY

        // Represent total timeline as e.g. 30 seconds
        val totalSpanSec = 30f
        val fadeInRatio = (fadeInSec / totalSpanSec).coerceIn(0f, 0.45f)
        val fadeOutRatio = (fadeOutSec / totalSpanSec).coerceIn(0f, 0.45f)

        val x0 = paddingX
        val x1 = paddingX + graphW * fadeInRatio
        val x2 = paddingX + graphW * (1f - fadeOutRatio)
        val x3 = paddingX + graphW

        // Background grid line
        drawLine(
            color = surfaceVariantColor,
            start = Offset(paddingX, bottomY),
            end = Offset(paddingX + graphW, bottomY),
            strokeWidth = 1.dp.toPx(),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f))
        )

        // Shaded Envelope Area
        val fillPath = Path().apply {
            moveTo(x0, bottomY)
            lineTo(x1, topY)
            lineTo(x2, topY)
            lineTo(x3, bottomY)
            close()
        }

        val fillGradient = Brush.verticalGradient(
            colors = listOf(
                primaryColor.copy(alpha = 0.35f),
                secondaryColor.copy(alpha = 0.05f)
            ),
            startY = topY,
            endY = bottomY
        )
        drawPath(fillPath, brush = fillGradient)

        // Envelope Outline Curve
        val strokePath = Path().apply {
            moveTo(x0, bottomY)
            lineTo(x1, topY)
            lineTo(x2, topY)
            lineTo(x3, bottomY)
        }

        drawPath(
            strokePath,
            brush = Brush.horizontalGradient(
                colors = listOf(primaryColor, primaryColor, secondaryColor)
            ),
            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
        )

        // Anchor points
        if (fadeInSec > 0f) {
            drawCircle(
                color = primaryColor,
                radius = 5.dp.toPx(),
                center = Offset(x1, topY)
            )
        }
        if (fadeOutSec > 0f) {
            drawCircle(
                color = secondaryColor,
                radius = 5.dp.toPx(),
                center = Offset(x2, topY)
            )
        }
    }
}
