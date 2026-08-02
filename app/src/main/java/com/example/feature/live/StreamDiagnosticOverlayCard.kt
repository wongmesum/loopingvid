package com.example.feature.live

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Real-time Stream Diagnostic Overlay Component.
 * Displays network latency, dropped frames, bitrate stability, and connection quality metrics
 * with live sparklines and status badges.
 */
@Composable
fun StreamDiagnosticOverlayCard(
    uiState: LiveUiState,
    onToggleExpanded: () -> Unit,
    onCloseOverlay: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (!uiState.showDiagnosticOverlay) return

    val isExpanded = uiState.isDiagnosticOverlayExpanded

    // Network Health Color Scheme
    val latencyColor = when {
        uiState.latencyMs < 150 -> Color(0xFF10B981) // Green
        uiState.latencyMs < 300 -> Color(0xFFF59E0B) // Amber
        else -> Color(0xFFEF4444) // Red
    }

    val frameLossPct = if (uiState.totalFrames > 0) {
        (uiState.droppedFrames.toFloat() / uiState.totalFrames.toFloat()) * 100f
    } else 0f

    val frameLossColor = when {
        frameLossPct < 1.0f -> Color(0xFF10B981)
        frameLossPct < 5.0f -> Color(0xFFF59E0B)
        else -> Color(0xFFEF4444)
    }

    val bitrateRatio = if (uiState.targetBitrateKbps > 0) {
        (uiState.currentBitrateKbps.toFloat() / uiState.targetBitrateKbps.toFloat()).coerceIn(0f, 1.2f)
    } else 1.0f

    val bitrateColor = when {
        bitrateRatio >= 0.9f -> Color(0xFF38BDF8) // Cyan/Blue
        bitrateRatio >= 0.7f -> Color(0xFFF59E0B)
        else -> Color(0xFFEF4444)
    }

    val qualityLabel = when {
        uiState.healthScorePct >= 90 -> "EXCELLENT"
        uiState.healthScorePct >= 75 -> "GOOD"
        uiState.healthScorePct >= 50 -> "FAIR"
        else -> "POOR"
    }

    val qualityBadgeBg = when {
        uiState.healthScorePct >= 90 -> Color(0xFF065F46)
        uiState.healthScorePct >= 75 -> Color(0xFF1E3A8A)
        uiState.healthScorePct >= 50 -> Color(0xFF92400E)
        else -> Color(0xFF991B1B)
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize(animationSpec = tween(300))
            .border(
                width = 1.dp,
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        latencyColor.copy(alpha = 0.8f),
                        bitrateColor.copy(alpha = 0.8f)
                    )
                ),
                shape = RoundedCornerShape(16.dp)
            )
            .testTag("stream_diagnostic_overlay"),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF0B0F19).copy(alpha = 0.95f)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            // --- Header Row ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggleExpanded() }
                    .testTag("diagnostic_overlay_header"),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(Color(0xFF0EA5E9), Color(0xFF6366F1))
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Analytics,
                            contentDescription = "Stream Diagnostics",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Stream Diagnostic Telemetry",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                color = qualityBadgeBg,
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = qualityLabel,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            text = if (isExpanded) "Real-time latency, frame loss & throughput" else "RTT: ${uiState.latencyMs}ms • ${uiState.currentBitrateKbps} Kbps • ${uiState.droppedFrames} Drops",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.Gray
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val arrowRotation by animateFloatAsState(
                        targetValue = if (isExpanded) 180f else 0f,
                        animationSpec = tween(300),
                        label = "arrowRotation"
                    )

                    IconButton(
                        onClick = onToggleExpanded,
                        modifier = Modifier
                            .size(30.dp)
                            .testTag("diagnostic_overlay_expand_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ExpandMore,
                            contentDescription = "Expand Diagnostics",
                            tint = Color.White,
                            modifier = Modifier.rotate(arrowRotation)
                        )
                    }

                    IconButton(
                        onClick = onCloseOverlay,
                        modifier = Modifier
                            .size(30.dp)
                            .testTag("diagnostic_overlay_close_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close Diagnostics Overlay",
                            tint = Color.LightGray
                        )
                    }
                }
            }

            // --- Collapsed Quick Metrics Strip ---
            if (!isExpanded) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    QuickMetricPill(
                        label = "Latency (RTT)",
                        value = "${uiState.latencyMs} ms",
                        color = latencyColor,
                        testTag = "metric_latency_value"
                    )
                    QuickMetricPill(
                        label = "Bitrate",
                        value = "${uiState.currentBitrateKbps} Kbps",
                        color = bitrateColor,
                        testTag = "metric_bitrate_value"
                    )
                    QuickMetricPill(
                        label = "Dropped",
                        value = "${uiState.droppedFrames} frames",
                        color = frameLossColor,
                        testTag = "metric_dropped_frames_value"
                    )
                }
            }

            // --- Expanded Diagnostic Panel ---
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically(animationSpec = tween(300)) + fadeIn(),
                exit = shrinkVertically(animationSpec = tween(250)) + fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // --- SECTION 1: Latency & RTT Stability ---
                    Surface(
                        color = Color(0xFF131B2E),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E293B))
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.NetworkCheck,
                                        contentDescription = null,
                                        tint = latencyColor,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Network Latency & Jitter",
                                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                        color = Color.White
                                    )
                                }

                                Text(
                                    text = "${uiState.latencyMs} ms RTT",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                                    color = latencyColor,
                                    modifier = Modifier.testTag("metric_latency_value")
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Network Jitter: ${uiState.jitterMs} ms",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.LightGray
                                )
                                Text(
                                    text = "Buffer Health: ${uiState.bufferHealthPct.toInt()}%",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (uiState.bufferHealthPct > 80) Color(0xFF10B981) else Color(0xFFF59E0B)
                                )
                            }

                            // RTT Sparkline Graph
                            SparklineGraph(
                                dataPoints = uiState.rttHistory.map { it.toFloat() },
                                lineColor = latencyColor,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(36.dp)
                            )
                        }
                    }

                    // --- SECTION 2: Frame Rate & Dropped Frames ---
                    Surface(
                        color = Color(0xFF131B2E),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E293B))
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Videocam,
                                        contentDescription = null,
                                        tint = frameLossColor,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Video Encoding & Dropped Frames",
                                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                        color = Color.White
                                    )
                                }

                                Text(
                                    text = "${uiState.droppedFrames} Drops (${String.format("%.1f", frameLossPct)}%)",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                                    color = frameLossColor,
                                    modifier = Modifier.testTag("metric_dropped_frames_value")
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Target FPS: ${uiState.fps} fps",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.LightGray
                                )
                                Text(
                                    text = "Total Streamed: ${uiState.totalFrames} frames",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.LightGray
                                )
                            }

                            LinearProgressIndicator(
                                progress = { (1.0f - (frameLossPct / 100f)).coerceIn(0f, 1f) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp)),
                                color = frameLossColor,
                                trackColor = Color(0xFF1E293B)
                            )
                        }
                    }

                    // --- SECTION 3: Bitrate Stability & Bandwidth ---
                    Surface(
                        color = Color(0xFF131B2E),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E293B))
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Speed,
                                        contentDescription = null,
                                        tint = bitrateColor,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "RTMP Stream Bitrate",
                                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                        color = Color.White
                                    )
                                }

                                Text(
                                    text = "${uiState.currentBitrateKbps} / ${uiState.targetBitrateKbps} Kbps",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                                    color = bitrateColor,
                                    modifier = Modifier.testTag("metric_bitrate_value")
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Uplink Bandwidth: ${uiState.bandwidthMbps} Mbps",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.LightGray
                                )
                                Text(
                                    text = "Resolution: ${uiState.streamResolution.badge}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF38BDF8)
                                )
                            }

                            // Bitrate History Sparkline Graph
                            SparklineGraph(
                                dataPoints = uiState.bitrateHistory.map { it.toFloat() },
                                lineColor = bitrateColor,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(36.dp)
                            )
                        }
                    }

                    // Diagnostic Status Summary Pill
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF0F172A), RoundedCornerShape(8.dp))
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = if (uiState.healthScorePct >= 75) Icons.Default.CheckCircle else Icons.Default.Warning,
                            contentDescription = null,
                            tint = if (uiState.healthScorePct >= 75) Color(0xFF10B981) else Color(0xFFF59E0B),
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = if (uiState.healthScorePct >= 75) {
                                "Connection status optimal. Bitrate matches encoder target with minimal jitter."
                            } else {
                                "Slight connection fluctuation detected. Adaptive buffer active."
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.LightGray
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickMetricPill(
    label: String,
    value: String,
    color: Color,
    testTag: String
) {
    Surface(
        color = Color(0xFF131B2E),
        shape = RoundedCornerShape(8.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.4f))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label,
                fontSize = 10.sp,
                color = Color.Gray
            )
            Text(
                text = value,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = color,
                modifier = Modifier.testTag(testTag)
            )
        }
    }
}

@Composable
private fun SparklineGraph(
    dataPoints: List<Float>,
    lineColor: Color,
    modifier: Modifier = Modifier
) {
    if (dataPoints.isEmpty()) return

    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height

        val minVal = dataPoints.minOrNull() ?: 0f
        val maxVal = (dataPoints.maxOrNull() ?: 1f).coerceAtLeast(minVal + 1f)
        val range = maxVal - minVal

        val points = dataPoints.mapIndexed { index, value ->
            val x = (index.toFloat() / (dataPoints.size - 1).coerceAtLeast(1)) * width
            val normalizedY = (value - minVal) / range
            val y = height - (normalizedY * (height - 8.dp.toPx()) + 4.dp.toPx())
            Offset(x, y)
        }

        // Draw background grid line
        drawLine(
            color = Color(0xFF1E293B),
            start = Offset(0f, height / 2f),
            end = Offset(width, height / 2f),
            strokeWidth = 1.dp.toPx()
        )

        // Draw trend path
        val path = Path().apply {
            moveTo(points.first().x, points.first().y)
            for (i in 1 until points.size) {
                val p1 = points[i - 1]
                val p2 = points[i]
                val cx = (p1.x + p2.x) / 2f
                cubicTo(cx, p1.y, cx, p2.y, p2.x, p2.y)
            }
        }

        drawPath(
            path = path,
            color = lineColor,
            style = Stroke(width = 2.dp.toPx())
        )

        // Draw end dot
        points.lastOrNull()?.let { last ->
            drawCircle(
                color = lineColor,
                radius = 3.dp.toPx(),
                center = last
            )
        }
    }
}
