package com.example.feature.live

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.WifiProtectedSetup
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Visual Buffering & Reconnection Indicator Card.
 * Displays when network connection drops or live stream ingest buffers/retries.
 * Prevents application crashes and communicates real-time auto-reconnection progress to the user.
 */
@Composable
fun BufferingStateIndicator(
    uiState: LiveUiState,
    onRetryNow: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = uiState.isBuffering || uiState.streamStatus == StreamStatus.RECONNECTING,
        enter = fadeIn(tween(300)),
        exit = fadeOut(tween(300))
    ) {
        val infiniteTransition = rememberInfiniteTransition(label = "pulse_border")
        val alphaGlow by infiniteTransition.animateFloat(
            initialValue = 0.5f,
            targetValue = 1.0f,
            animationSpec = infiniteRepeatable(
                animation = tween(1000),
                repeatMode = RepeatMode.Reverse
            ),
            label = "alphaGlow"
        )

        Card(
            modifier = modifier
                .fillMaxWidth()
                .border(
                    width = 2.dp,
                    color = Color(0xFFF97316).copy(alpha = alphaGlow),
                    shape = RoundedCornerShape(16.dp)
                )
                .testTag("buffering_state_indicator"),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E132B)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(36.dp),
                            color = Color(0xFFF97316),
                            strokeWidth = 3.dp
                        )
                        Icon(
                            imageVector = Icons.Default.WifiProtectedSetup,
                            contentDescription = "Reconnecting",
                            tint = Color(0xFFF97316),
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Network Buffering & Reconnecting...",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color.White
                        )
                        Text(
                            text = "Connection lost during live stream transmission",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.LightGray
                        )
                    }

                    Surface(
                        color = Color(0xFFEA580C),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "Attempt ${uiState.reconnectAttempt}/${uiState.maxReconnectAttempts}",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                // Countdown and Reason Banner
                Surface(
                    color = Color(0xFF2D1B3F),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CloudOff,
                                contentDescription = null,
                                tint = Color(0xFFFCA5A5),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = uiState.connectionLossReason ?: "Network socket packet drop / timeout",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.9f)
                            )
                        }

                        if (uiState.reconnectCountdownSec > 0) {
                            Text(
                                text = "Retry in ${uiState.reconnectCountdownSec}s",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = Color(0xFF38BDF8)
                            )
                        }
                    }
                }

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onCancel,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("cancel_reconnection_button"),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.LightGray)
                    ) {
                        Text("Stop Stream")
                    }

                    Button(
                        onClick = onRetryNow,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("retry_reconnection_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF97316))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Retry Handshake")
                    }
                }
            }
        }
    }
}

/**
 * Full Overlay over Video Stream View (CameraX / Video File Preview)
 * displayed when stream network connection drops.
 */
@Composable
fun VideoBufferingOverlay(
    isBuffering: Boolean,
    streamStatus: StreamStatus,
    reconnectAttempt: Int,
    maxReconnectAttempts: Int,
    countdownSec: Int,
    modifier: Modifier = Modifier
) {
    if (isBuffering || streamStatus == StreamStatus.RECONNECTING) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.75f)),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.padding(16.dp)
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(48.dp),
                    color = Color(0xFFF97316),
                    strokeWidth = 4.dp
                )

                Text(
                    text = "BUFFERING & RECONNECTING",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color.White,
                    letterSpacing = 1.sp
                )

                Text(
                    text = "Re-establishing RTMP socket connection...\nAttempt $reconnectAttempt of $maxReconnectAttempts",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.LightGray,
                    textAlign = TextAlign.Center
                )

                if (countdownSec > 0) {
                    Surface(
                        color = Color(0xFFEA580C),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "Auto-retry in ${countdownSec}s",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }
    }
}
