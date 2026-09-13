package com.example.feature.live

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.FormatColorFill
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

enum class TickerSpeed(val label: String, val durationMs: Int) {
    SLOW("Slow", 16000),
    NORMAL("Normal", 10000),
    FAST("Fast", 5000)
}

enum class TickerPosition(val label: String) {
    TOP("Top"),
    BOTTOM("Bottom")
}

data class TickerPresetStyle(
    val name: String,
    val bgColor: Color,
    val textColor: Color
)

val TICKER_PRESET_STYLES = listOf(
    TickerPresetStyle("News Red", Color(0xCCB91C1C), Color(0xFFFEF08A)), // Red bg, Yellow text
    TickerPresetStyle("Dark Gold", Color(0xDD111827), Color(0xFFFBBF24)), // Dark bg, Gold text
    TickerPresetStyle("Neon Cyan", Color(0xDD0F172A), Color(0xFF22D3EE)), // Slate bg, Cyan text
    TickerPresetStyle("Emerald", Color(0xCC047857), Color.White) // Emerald bg, White text
)

/**
 * Scrolling Ticker Overlay Composable
 * Animates customized user running text continuously across the live video preview screen.
 */
@Composable
fun ScrollingTickerOverlay(
    text: String,
    enabled: Boolean,
    speed: TickerSpeed,
    bgColor: Color,
    textColor: Color,
    modifier: Modifier = Modifier
) {
    if (!enabled || text.isBlank()) return

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .background(bgColor)
            .padding(vertical = 4.dp, horizontal = 8.dp)
            .clipToBounds()
            .testTag("scrolling_ticker_overlay")
    ) {
        val containerWidthPx = with(LocalDensity.current) { maxWidth.toPx() }
        val transition = rememberInfiniteTransition(label = "tickerTransition")

        val offsetFraction by transition.animateFloat(
            initialValue = 1.1f,
            targetValue = -1.1f,
            animationSpec = infiniteRepeatable(
                animation = tween(
                    durationMillis = speed.durationMs,
                    easing = LinearEasing
                ),
                repeatMode = RepeatMode.Restart
            ),
            label = "tickerOffset"
        )

        val displayText = "$text   •   $text"

        Text(
            text = displayText,
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.ExtraBold,
                fontSize = 12.sp,
                letterSpacing = 0.6.sp
            ),
            color = textColor,
            maxLines = 1,
            softWrap = false,
            modifier = Modifier
                .offset { IntOffset(x = (offsetFraction * containerWidthPx).toInt(), y = 0) }
                .testTag("ticker_animated_text")
        )
    }
}

/**
 * Ticker Configuration & Customization Control Panel
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ScrollingTickerControlCard(
    tickerText: String,
    isTickerEnabled: Boolean,
    tickerSpeed: TickerSpeed,
    tickerPosition: TickerPosition,
    selectedStyleIndex: Int,
    onTickerTextChange: (String) -> Unit,
    onTickerEnabledChange: (Boolean) -> Unit,
    onTickerSpeedChange: (TickerSpeed) -> Unit,
    onTickerPositionChange: (TickerPosition) -> Unit,
    onStyleSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("scrolling_ticker_control_card"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header Row with Toggle Switch
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Subtitles,
                        contentDescription = "Ticker Overlay Icon",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "Live Ticker Overlay",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (isTickerEnabled) "Running text overlay active" else "Overlay disabled",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Switch(
                    checked = isTickerEnabled,
                    onCheckedChange = onTickerEnabledChange,
                    modifier = Modifier.testTag("ticker_enable_switch"),
                    colors = SwitchDefaults.colors(checkedThumbColor = Color.White)
                )
            }

            AnimatedVisibility(
                visible = isTickerEnabled,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Custom Text Input Field
                    OutlinedTextField(
                        value = tickerText,
                        onValueChange = onTickerTextChange,
                        label = { Text("Ticker Running Text Input") },
                        placeholder = { Text("Enter live announcements, social handles, or news text...") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Campaign,
                                contentDescription = "Announcement",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("ticker_text_input"),
                        singleLine = true
                    )

                    // Speed Selector
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Speed,
                                contentDescription = "Ticker Speed",
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Scroll Speed",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            TickerSpeed.entries.forEach { speed ->
                                FilterChip(
                                    selected = tickerSpeed == speed,
                                    onClick = { onTickerSpeedChange(speed) },
                                    label = { Text(speed.label) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                                        selectedLabelColor = Color.White
                                    ),
                                    modifier = Modifier.testTag("ticker_speed_chip_${speed.name}")
                                )
                            }
                        }
                    }

                    // Position Selector
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "Overlay Position",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            TickerPosition.entries.forEach { pos ->
                                FilterChip(
                                    selected = tickerPosition == pos,
                                    onClick = { onTickerPositionChange(pos) },
                                    label = { Text(pos.label) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.secondary,
                                        selectedLabelColor = Color.White
                                    ),
                                    modifier = Modifier.testTag("ticker_position_chip_${pos.name}")
                                )
                            }
                        }
                    }

                    // Theme Style Presets
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.FormatColorFill,
                                contentDescription = "Style Preset",
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Ticker Visual Style",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            TICKER_PRESET_STYLES.forEachIndexed { index, style ->
                                val isSelected = selectedStyleIndex == index
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(style.bgColor)
                                        .border(
                                            width = if (isSelected) 2.dp else 1.dp,
                                            color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Gray.copy(alpha = 0.5f),
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                        .clickable { onStyleSelect(index) }
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                        .testTag("ticker_style_preset_$index"),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(8.dp)
                                                .background(style.textColor, CircleShape)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = style.name,
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            color = style.textColor
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
