package com.example.feature.live

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.SignalCellular4Bar
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.utils.StorageInfo
import com.example.core.utils.ThermalInfo

/**
 * Health Status Level Enum
 */
enum class HealthDotState(val label: String, val color: Color) {
    EXCELLENT("Excellent", Color(0xFF10B981)), // Green
    FAIR("Fair", Color(0xFFF59E0B)),          // Amber / Yellow
    POOR("Critical", Color(0xFFEF4444)),       // Red
    OFFLINE("Offline", Color(0xFF6B7280))      // Gray
}

/**
 * Calculates network quality status based on bitrate, latency, and dropped frames.
 */
fun getNetworkQualityDotState(
    streamStatus: StreamStatus,
    bitrateKbps: Int,
    latencyMs: Int,
    droppedFrames: Int
): HealthDotState {
    if (streamStatus != StreamStatus.LIVE) return HealthDotState.OFFLINE
    return when {
        droppedFrames > 15 || latencyMs > 250 || bitrateKbps < 1500 -> HealthDotState.POOR
        droppedFrames > 3 || latencyMs > 140 || bitrateKbps < 3500 -> HealthDotState.FAIR
        else -> HealthDotState.EXCELLENT
    }
}

/**
 * Calculates storage health dot state.
 */
fun getStorageDotState(storageInfo: StorageInfo): HealthDotState {
    return when {
        storageInfo.isCriticalStorage -> HealthDotState.POOR
        storageInfo.isLowStorage -> HealthDotState.FAIR
        else -> HealthDotState.EXCELLENT
    }
}

/**
 * Calculates thermal/encoder health dot state.
 */
fun getThermalDotState(thermalInfo: ThermalInfo): HealthDotState {
    return when {
        thermalInfo.level.name == "EMERGENCY" || thermalInfo.level.name == "SERIOUS" -> HealthDotState.POOR
        thermalInfo.level.name == "MODERATE" -> HealthDotState.FAIR
        else -> HealthDotState.EXCELLENT
    }
}

/**
 * Stream Health Status Icons Bar
 * Visually displays color-coded dots (Green/Yellow/Red) representing Connection Status, Network Quality, Hardware Temp, and Disk Storage.
 */
@Composable
fun StreamHealthStatusCard(
    uiState: LiveUiState,
    modifier: Modifier = Modifier
) {
    val networkState = getNetworkQualityDotState(
        streamStatus = uiState.streamStatus,
        bitrateKbps = uiState.currentBitrateKbps,
        latencyMs = uiState.latencyMs,
        droppedFrames = uiState.droppedFrames
    )

    val storageState = getStorageDotState(uiState.storageInfo)
    val thermalState = getThermalDotState(uiState.thermalInfo)

    val connectionDotColor = when (uiState.streamStatus) {
        StreamStatus.LIVE -> Color(0xFF10B981)
        StreamStatus.CONNECTING -> Color(0xFFF59E0B)
        StreamStatus.RECONNECTING -> Color(0xFFEF4444)
        StreamStatus.OFFLINE, StreamStatus.STOPPED -> Color(0xFF6B7280)
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("stream_health_status_card"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "System Health & Network Status",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = if (uiState.streamStatus == StreamStatus.LIVE) "MONITORING LIVE" else "READY",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = if (uiState.streamStatus == StreamStatus.LIVE) Color(0xFF10B981) else Color.Gray
                )
            }

            // Grid of 4 Status Indicators with Green/Yellow/Red Dots
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // 1. Connection Dot
                SingleStatusDotTile(
                    label = "Connection",
                    value = uiState.streamStatus.name,
                    dotColor = connectionDotColor,
                    isPulsing = uiState.streamStatus == StreamStatus.LIVE || uiState.streamStatus == StreamStatus.CONNECTING,
                    testTag = "status_dot_connection"
                )

                // 2. Network Quality Dot
                SingleStatusDotTile(
                    label = "Network",
                    value = networkState.label,
                    dotColor = networkState.color,
                    isPulsing = false,
                    testTag = "status_dot_network"
                )

                // 3. Encoder / Thermal Dot
                SingleStatusDotTile(
                    label = "Thermal",
                    value = thermalState.label,
                    dotColor = thermalState.color,
                    isPulsing = false,
                    testTag = "status_dot_thermal"
                )

                // 4. Storage Space Dot
                SingleStatusDotTile(
                    label = "Storage",
                    value = storageState.label,
                    dotColor = storageState.color,
                    isPulsing = false,
                    testTag = "status_dot_storage"
                )
            }
        }
    }
}

/**
 * Single Status Indicator Tile with Dot Icon and Label
 */
@Composable
private fun SingleStatusDotTile(
    label: String,
    value: String,
    dotColor: Color,
    isPulsing: Boolean,
    testTag: String,
    modifier: Modifier = Modifier
) {
    val transition = rememberInfiniteTransition(label = "dotPulse")
    val alphaPulse by transition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dotPulseAlpha"
    )

    Column(
        modifier = modifier
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(10.dp))
            .padding(horizontal = 10.dp, vertical = 8.dp)
            .testTag(testTag),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .alpha(if (isPulsing) alphaPulse else 1f)
                    .background(dotColor, CircleShape)
                    .border(1.dp, dotColor.copy(alpha = 0.5f), CircleShape)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Text(
            text = value,
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, fontSize = 11.sp),
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

/**
 * Compact Video Preview Overlay Badge showing status dots
 */
@Composable
fun VideoOverlayStatusBadge(
    uiState: LiveUiState,
    modifier: Modifier = Modifier
) {
    val networkState = getNetworkQualityDotState(
        streamStatus = uiState.streamStatus,
        bitrateKbps = uiState.currentBitrateKbps,
        latencyMs = uiState.latencyMs,
        droppedFrames = uiState.droppedFrames
    )
    val storageState = getStorageDotState(uiState.storageInfo)

    val connColor = when (uiState.streamStatus) {
        StreamStatus.LIVE -> Color(0xFF10B981)
        StreamStatus.CONNECTING -> Color(0xFFF59E0B)
        else -> Color(0xFF6B7280)
    }

    Row(
        modifier = modifier
            .background(Color.Black.copy(alpha = 0.65f), RoundedCornerShape(20.dp))
            .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(20.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp)
            .testTag("video_overlay_status_badge"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // Connection Dot
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(connColor, CircleShape)
                .testTag("preview_dot_connection")
        )

        // Network Quality Dot
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(networkState.color, CircleShape)
                .testTag("preview_dot_network")
        )

        // Storage Dot
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(storageState.color, CircleShape)
                .testTag("preview_dot_storage")
        )

        Spacer(modifier = Modifier.width(2.dp))

        Text(
            text = if (uiState.streamStatus == StreamStatus.LIVE) "LIVE" else "OFFLINE",
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.sp),
            color = Color.White
        )
    }
}
