package com.example.feature.live

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.CenterFocusWeak
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Hd
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Quick Expandable / Slide-Out Stream Settings Panel.
 * Provides on-the-fly access to camera focus, stream resolution, audio muting, and quick toggles
 * without navigating away from active live streaming view.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun StreamQuickSettingsPanel(
    uiState: LiveUiState,
    onToggleExpanded: () -> Unit,
    onToggleCameraFocus: () -> Unit,
    onSetCameraFocusMode: (CameraFocusMode) -> Unit,
    onToggleAudioMute: () -> Unit,
    onSetResolution: (StreamResolution) -> Unit,
    onToggleTorch: () -> Unit,
    onToggleTicker: () -> Unit,
    onSetMasterVolume: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val isExpanded = uiState.isQuickSettingsExpanded

    Card(
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize(animationSpec = tween(300))
            .border(
                width = 1.dp,
                brush = Brush.horizontalGradient(
                    colors = if (isExpanded) {
                        listOf(Color(0xFF8B5CF6), Color(0xFF06B6D4))
                    } else {
                        listOf(Color(0xFF374151), Color(0xFF1F2937))
                    }
                ),
                shape = RoundedCornerShape(16.dp)
            )
            .testTag("stream_quick_settings_panel"),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF130F26)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            // --- Header Bar ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggleExpanded() }
                    .testTag("toggle_settings_panel_header"),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(Color(0xFF8B5CF6), Color(0xFF6366F1))
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = "Quick Stream Controls",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Column {
                        Text(
                            text = "Quick Stream Controls",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = Color.White
                        )
                        Text(
                            text = if (isExpanded) "Tap to collapse quick panel" else "Camera focus • Audio mute • Stream resolution",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.LightGray
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Active Status Badges when Collapsed
                    if (!isExpanded) {
                        // Resolution Badge
                        Surface(
                            color = Color(0xFF1E1B4B),
                            shape = RoundedCornerShape(6.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF6366F1).copy(alpha = 0.5f))
                        ) {
                            Text(
                                text = uiState.streamResolution.badge,
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = Color(0xFF818CF8),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }

                        // Mute Indicator Badge
                        Surface(
                            color = if (uiState.isQuickMuted || uiState.isMasterMuted) Color(0xFF7F1D1D) else Color(0xFF064E3B),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (uiState.isQuickMuted || uiState.isMasterMuted) Icons.Default.MicOff else Icons.Default.Mic,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = if (uiState.isQuickMuted || uiState.isMasterMuted) "MUTED" else "MIC ON",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = Color.White
                                )
                            }
                        }
                    }

                    val arrowRotation by animateFloatAsState(
                        targetValue = if (isExpanded) 180f else 0f,
                        animationSpec = tween(300),
                        label = "arrowRotate"
                    )

                    IconButton(
                        onClick = onToggleExpanded,
                        modifier = Modifier
                            .size(32.dp)
                            .testTag("toggle_settings_panel_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ExpandMore,
                            contentDescription = "Toggle Settings",
                            tint = Color.White,
                            modifier = Modifier.rotate(arrowRotation)
                        )
                    }
                }
            }

            // --- Expandable Quick Settings Content ---
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically(animationSpec = tween(300)) + fadeIn(),
                exit = shrinkVertically(animationSpec = tween(250)) + fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // --- SECTION 1: Camera Focus Control ---
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF1E1838), RoundedCornerShape(12.dp))
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = if (uiState.isCameraAutoFocusEnabled) Icons.Default.CenterFocusStrong else Icons.Default.CenterFocusWeak,
                                    contentDescription = "Camera Focus",
                                    tint = if (uiState.isCameraAutoFocusEnabled) Color(0xFF10B981) else Color(0xFFF59E0B),
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Camera Focus Mode",
                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                    color = Color.White
                                )
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = if (uiState.isCameraAutoFocusEnabled) "Auto-Focus ON" else "Focus Lock (AF-L)",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (uiState.isCameraAutoFocusEnabled) Color(0xFF34D399) else Color(0xFFFBBF24)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Switch(
                                    checked = uiState.isCameraAutoFocusEnabled,
                                    onCheckedChange = { onToggleCameraFocus() },
                                    modifier = Modifier
                                        .size(36.dp)
                                        .testTag("toggle_camera_focus_switch"),
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color.White,
                                        checkedTrackColor = Color(0xFF10B981)
                                    )
                                )
                            }
                        }

                        // Focus Mode Chips
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            CameraFocusMode.values().forEach { mode ->
                                val isSelected = uiState.cameraFocusMode == mode
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { onSetCameraFocusMode(mode) },
                                    label = {
                                        Text(
                                            text = mode.label,
                                            fontSize = 11.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                        )
                                    },
                                    leadingIcon = if (isSelected) {
                                        {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = null,
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                    } else null,
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = Color(0xFF6366F1),
                                        selectedLabelColor = Color.White,
                                        selectedLeadingIconColor = Color.White,
                                        containerColor = Color(0xFF2A244D),
                                        labelColor = Color.LightGray
                                    ),
                                    modifier = Modifier.testTag("camera_focus_chip_${mode.name.lowercase()}")
                                )
                            }
                        }
                    }

                    // --- SECTION 2: Stream Resolution & Bitrate Target ---
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF1E1838), RoundedCornerShape(12.dp))
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.HighQuality,
                                    contentDescription = "Stream Resolution",
                                    tint = Color(0xFF38BDF8),
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Stream Quality Resolution",
                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                    color = Color.White
                                )
                            }

                            Surface(
                                color = Color(0xFF0284C7),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = "${uiState.targetBitrateKbps} Kbps Target",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        // Resolution Choice Chips
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            StreamResolution.values().forEach { res ->
                                val isSelected = uiState.streamResolution == res
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { onSetResolution(res) },
                                    label = {
                                        Text(
                                            text = res.label,
                                            fontSize = 11.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                        )
                                    },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Default.Hd,
                                            contentDescription = null,
                                            tint = if (isSelected) Color.White else Color(0xFF38BDF8),
                                            modifier = Modifier.size(14.dp)
                                        )
                                    },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = Color(0xFF0284C7),
                                        selectedLabelColor = Color.White,
                                        selectedLeadingIconColor = Color.White,
                                        containerColor = Color(0xFF2A244D),
                                        labelColor = Color.LightGray
                                    ),
                                    modifier = Modifier.testTag("stream_resolution_chip_${res.badge.lowercase()}")
                                )
                            }
                        }
                    }

                    // --- SECTION 3: Audio Mute & Volume Control ---
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF1E1838), RoundedCornerShape(12.dp))
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = if (uiState.isQuickMuted || uiState.isMasterMuted) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                                    contentDescription = "Audio Mute",
                                    tint = if (uiState.isQuickMuted || uiState.isMasterMuted) Color(0xFFEF4444) else Color(0xFF34D399),
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Stream Audio Control",
                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                    color = Color.White
                                )
                            }

                            // Quick Mute Master Toggle Button
                            Surface(
                                modifier = Modifier.clickable { onToggleAudioMute() }.testTag("quick_mute_audio_button"),
                                color = if (uiState.isQuickMuted || uiState.isMasterMuted) Color(0xFFDC2626) else Color(0xFF059669),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = if (uiState.isQuickMuted || uiState.isMasterMuted) Icons.Default.MicOff else Icons.Default.Mic,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = if (uiState.isQuickMuted || uiState.isMasterMuted) "UNMUTE" else "MUTE ALL",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = Color.White
                                    )
                                }
                            }
                        }

                        // Master Volume Quick Slider
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Master Broadcast Output Level",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.LightGray
                                )
                                Text(
                                    text = "${(uiState.masterVolume * 100).toInt()}%",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = Color.White
                                )
                            }

                            Slider(
                                value = uiState.masterVolume,
                                onValueChange = { onSetMasterVolume(it) },
                                valueRange = 0f..1f,
                                colors = SliderDefaults.colors(
                                    thumbColor = Color(0xFF10B981),
                                    activeTrackColor = Color(0xFF059669),
                                    inactiveTrackColor = Color(0xFF374151)
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(24.dp)
                                    .testTag("master_volume_quick_slider")
                            )
                        }
                    }

                    // --- SECTION 4: Studio Quick Toggles Grid ---
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Torch Toggle Card
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { onToggleTorch() }
                                .testTag("quick_toggle_torch_button"),
                            color = if (uiState.isTorchActive) Color(0xFF312E81) else Color(0xFF1E1838),
                            shape = RoundedCornerShape(10.dp),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (uiState.isTorchActive) Color(0xFF818CF8) else Color(0xFF374151)
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = if (uiState.isTorchActive) Icons.Default.FlashOn else Icons.Default.FlashOff,
                                    contentDescription = "Flashlight Torch",
                                    tint = if (uiState.isTorchActive) Color(0xFFFBBF24) else Color.Gray,
                                    modifier = Modifier.size(18.dp)
                                )
                                Column {
                                    Text(
                                        text = "Camera Torch",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = Color.White
                                    )
                                    Text(
                                        text = if (uiState.isTorchActive) "LED ON" else "LED OFF",
                                        fontSize = 10.sp,
                                        color = if (uiState.isTorchActive) Color(0xFFFBBF24) else Color.Gray
                                    )
                                }
                            }
                        }

                        // Scrolling Ticker Toggle Card
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { onToggleTicker() }
                                .testTag("quick_toggle_ticker_button"),
                            color = if (uiState.isTickerEnabled) Color(0xFF064E3B) else Color(0xFF1E1838),
                            shape = RoundedCornerShape(10.dp),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (uiState.isTickerEnabled) Color(0xFF34D399) else Color(0xFF374151)
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Subtitles,
                                    contentDescription = "Live Ticker Overlay",
                                    tint = if (uiState.isTickerEnabled) Color(0xFF34D399) else Color.Gray,
                                    modifier = Modifier.size(18.dp)
                                )
                                Column {
                                    Text(
                                        text = "Ticker Overlay",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = Color.White
                                    )
                                    Text(
                                        text = if (uiState.isTickerEnabled) "ACTIVE" else "HIDDEN",
                                        fontSize = 10.sp,
                                        color = if (uiState.isTickerEnabled) Color(0xFF34D399) else Color.Gray
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
