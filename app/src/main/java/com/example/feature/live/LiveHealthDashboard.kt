package com.example.feature.live

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.SignalCellular4Bar
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Live Health Dashboard Component:
 * Real-time network telemetry visualizer for live streams.
 * Monitors bitrate trends, dropped frame rates, RTT latency, network jitter,
 * buffer health, and overall stream stability.
 */
@Composable
fun LiveHealthDashboard(
    uiState: LiveUiState,
    onSimulateNetworkLoss: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val healthColor by animateColorAsState(
        targetValue = when {
            uiState.healthScorePct >= 90 -> Color(0xFF10B981) // Green
            uiState.healthScorePct >= 75 -> Color(0xFFF59E0B) // Amber
            else -> Color(0xFFEF4444) // Red
        },
        label = "healthColor"
    )

    val animatedHealthScore by animateFloatAsState(
        targetValue = uiState.healthScorePct.toFloat(),
        animationSpec = tween(500),
        label = "animatedHealthScore"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("live_health_dashboard"),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF120E24)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Dashboard Header & Health Score Ring
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.NetworkCheck,
                        contentDescription = "Live Network Health",
                        tint = healthColor,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "Stream Health Dashboard",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color.White
                        )
                        Text(
                            text = "Real-time RTMP Network Telemetry",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.Gray
                        )
                    }
                }

                // Health Score Badge
                Box(
                    modifier = Modifier
                        .background(healthColor.copy(alpha = 0.2f), RoundedCornerShape(20.dp))
                        .border(1.dp, healthColor, RoundedCornerShape(20.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                        .testTag("health_score_badge"),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(healthColor, CircleShape)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "${animatedHealthScore.toInt()}% HEALTH",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = healthColor
                        )
                    }
                }
            }

            // 1. Bitrate Trend Real-time Graph
            BitrateTrendCard(
                currentBitrate = uiState.currentBitrateKbps,
                targetBitrate = uiState.targetBitrateKbps,
                bitrateHistory = uiState.bitrateHistory
            )

            // 2. Metrics Row: Dropped Frames & RTT / Jitter
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Dropped Frames Card
                DroppedFramesCard(
                    modifier = Modifier.weight(1f),
                    droppedFrames = uiState.droppedFrames,
                    totalFrames = uiState.totalFrames,
                    fps = uiState.fps
                )

                // RTT & Jitter Card
                RttJitterCard(
                    modifier = Modifier.weight(1f),
                    rttMs = uiState.latencyMs,
                    jitterMs = uiState.jitterMs,
                    rttHistory = uiState.rttHistory
                )
            }

            // 3. Buffer Health & Diagnostic Insight Banner
            BufferHealthBanner(
                bufferPct = uiState.bufferHealthPct,
                streamStatus = uiState.streamStatus,
                droppedFrames = uiState.droppedFrames,
                rttMs = uiState.latencyMs,
                onSimulateNetworkLoss = onSimulateNetworkLoss
            )
        }
    }
}

/**
 * Real-time Bitrate Trend Graph using Compose Canvas
 */
@Composable
private fun BitrateTrendCard(
    currentBitrate: Int,
    targetBitrate: Int,
    bitrateHistory: List<Int>,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("bitrate_chart_card"),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1B1536)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Speed,
                        contentDescription = "Bitrate Trend",
                        tint = Color(0xFF10B981),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Bitrate Trend",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = Color.White
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "$currentBitrate kbps",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold),
                        color = Color(0xFF10B981)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "(Target $targetBitrate)",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Gray
                    )
                }
            }

            // Canvas Bitrate Line & Gradient Area Graph
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(90.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF120E24))
                    .padding(8.dp)
                    .testTag("bitrate_canvas_graph")
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    if (bitrateHistory.size < 2) return@Canvas

                    val minVal = (bitrateHistory.minOrNull() ?: 4000).coerceAtMost(3800).toFloat()
                    val maxVal = (bitrateHistory.maxOrNull() ?: 5000).coerceAtLeast(5200).toFloat()
                    val range = (maxVal - minVal).coerceAtLeast(100f)

                    val width = size.width
                    val height = size.height
                    val stepX = width / (bitrateHistory.size - 1)

                    val strokePath = Path()
                    val fillPath = Path()

                    bitrateHistory.forEachIndexed { index, value ->
                        val x = index * stepX
                        val normalizedY = (value - minVal) / range
                        val y = height - (normalizedY * height)

                        if (index == 0) {
                            strokePath.moveTo(x, y)
                            fillPath.moveTo(x, height)
                            fillPath.lineTo(x, y)
                        } else {
                            val prevX = (index - 1) * stepX
                            val prevVal = bitrateHistory[index - 1]
                            val prevNormalizedY = (prevVal - minVal) / range
                            val prevY = height - (prevNormalizedY * height)

                            // Smooth cubic Bezier control points
                            val cx1 = prevX + stepX / 2f
                            val cy1 = prevY
                            val cx2 = prevX + stepX / 2f
                            val cy2 = y

                            strokePath.cubicTo(cx1, cy1, cx2, cy2, x, y)
                            fillPath.cubicTo(cx1, cy1, cx2, cy2, x, y)
                        }

                        if (index == bitrateHistory.size - 1) {
                            fillPath.lineTo(x, height)
                            fillPath.close()
                        }
                    }

                    // Draw Gradient Fill
                    drawPath(
                        path = fillPath,
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFF10B981).copy(alpha = 0.4f),
                                Color(0xFF10B981).copy(alpha = 0.0f)
                            )
                        )
                    )

                    // Draw Smooth Line
                    drawPath(
                        path = strokePath,
                        color = Color(0xFF10B981),
                        style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                    )

                    // Draw Target Reference Line
                    val targetNormalizedY = (targetBitrate - minVal) / range
                    val targetY = height - (targetNormalizedY * height)
                    drawLine(
                        color = Color.White.copy(alpha = 0.3f),
                        start = Offset(0f, targetY),
                        end = Offset(width, targetY),
                        strokeWidth = 1.dp.toPx()
                    )
                }
            }
        }
    }
}

/**
 * Dropped Frames Counter & Loss Rate Card
 */
@Composable
private fun DroppedFramesCard(
    droppedFrames: Int,
    totalFrames: Long,
    fps: Int,
    modifier: Modifier = Modifier
) {
    val dropPct = if (totalFrames > 0) (droppedFrames.toFloat() / totalFrames) * 100f else 0f

    val badgeColor = when {
        dropPct > 2.0f -> Color(0xFFEF4444)
        dropPct > 0.5f -> Color(0xFFF59E0B)
        else -> Color(0xFF10B981)
    }

    Card(
        modifier = modifier.testTag("dropped_frames_card"),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1B1536)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Dropped Frames",
                        tint = badgeColor,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Frame Loss",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color.White
                    )
                }
                Text(
                    text = "$fps FPS",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.LightGray
                )
            }

            Text(
                text = "$droppedFrames frames",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = Color.White
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Loss Rate",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Gray
                )
                Text(
                    text = "%.2f%%".format(dropPct),
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = badgeColor
                )
            }
        }
    }
}

/**
 * RTT (Round Trip Time) Latency & Jitter Card
 */
@Composable
private fun RttJitterCard(
    rttMs: Int,
    jitterMs: Int,
    rttHistory: List<Int>,
    modifier: Modifier = Modifier
) {
    val rttColor = when {
        rttMs > 200 -> Color(0xFFEF4444)
        rttMs > 150 -> Color(0xFFF59E0B)
        else -> Color(0xFF3B82F6) // Blue
    }

    Card(
        modifier = modifier.testTag("rtt_jitter_card"),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1B1536)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.SignalCellular4Bar,
                        contentDescription = "RTT & Jitter",
                        tint = rttColor,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "RTT / Jitter",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color.White
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    Text(
                        text = "$rttMs ms RTT",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color.White
                    )
                    Text(
                        text = "Jitter: ±$jitterMs ms",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.LightGray
                    )
                }

                // Mini RTT Canvas Bar Indicator
                Box(
                    modifier = Modifier
                        .width(48.dp)
                        .height(24.dp)
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        if (rttHistory.isEmpty()) return@Canvas
                        val maxRtt = (rttHistory.maxOrNull() ?: 200).toFloat()
                        val stepX = size.width / (rttHistory.size.coerceAtLeast(2) - 1)

                        rttHistory.forEachIndexed { idx, valMs ->
                            val x = idx * stepX
                            val barHeight = (valMs / maxRtt) * size.height
                            drawLine(
                                color = rttColor,
                                start = Offset(x, size.height),
                                end = Offset(x, size.height - barHeight),
                                strokeWidth = 2.dp.toPx()
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Buffer Health Bar & Diagnostic Insight Banner
 */
@Composable
private fun BufferHealthBanner(
    bufferPct: Float,
    streamStatus: StreamStatus,
    droppedFrames: Int,
    rttMs: Int,
    onSimulateNetworkLoss: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val insightMessage = when {
        streamStatus == StreamStatus.RECONNECTING -> "Network connection interrupted! Auto-reconnection handler actively recovering RTMP session..."
        streamStatus != StreamStatus.LIVE -> "Stream is idle. Start stream to initialize RTMP network monitoring."
        droppedFrames > 10 -> "Minor dropped frames detected. Check local Wi-Fi or reduce video bitrate."
        rttMs > 180 -> "High RTT latency. RTMP server ingest node may be far or congested."
        else -> "Network connection is excellent. Video buffer is healthy with 0 congestion."
    }

    val insightIcon = when {
        streamStatus == StreamStatus.RECONNECTING || droppedFrames > 10 || rttMs > 180 -> Icons.Default.Warning
        streamStatus == StreamStatus.LIVE -> Icons.Default.CheckCircle
        else -> Icons.Default.Info
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("buffer_health_banner"),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Buffer Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Ingest Buffer Fill Level",
                style = MaterialTheme.typography.labelSmall,
                color = Color.Gray
            )
            Text(
                text = "%.1f%%".format(bufferPct),
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = Color.White
            )
        }

        LinearProgressIndicator(
            progress = { (bufferPct / 100f).coerceIn(0f, 1f) },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = if (streamStatus == StreamStatus.RECONNECTING) Color(0xFFF97316) else Color(0xFF10B981),
            trackColor = Color(0xFF1B1536)
        )

        // Diagnostic Note & Test Trigger
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF1B1536), RoundedCornerShape(8.dp))
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = insightIcon,
                    contentDescription = "Diagnostic Insight",
                    tint = if (streamStatus == StreamStatus.RECONNECTING || droppedFrames > 10 || rttMs > 180) Color(0xFFF59E0B) else Color(0xFF10B981),
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = insightMessage,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.9f)
                )
            }

            if (onSimulateNetworkLoss != null && streamStatus == StreamStatus.LIVE) {
                Spacer(modifier = Modifier.width(8.dp))
                androidx.compose.material3.OutlinedButton(
                    onClick = onSimulateNetworkLoss,
                    modifier = Modifier.testTag("simulate_network_loss_button"),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = "Simulate Drop",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFFF97316)
                    )
                }
            }
        }
    }
}
