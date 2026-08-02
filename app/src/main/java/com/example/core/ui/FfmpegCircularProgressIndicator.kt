package com.example.core.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProgressIndicatorDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Reusable Circular Progress Indicator Card for FFmpeg video/audio processing tasks.
 * Provides real-time visual feedback with an animated circular progress ring, percentage readout,
 * and current processing status message.
 */
@Composable
fun FfmpegCircularProgressIndicator(
    progress: Int,
    statusText: String,
    modifier: Modifier = Modifier,
    title: String = "FFmpeg Processing in Progress",
    accentColor: Color = MaterialTheme.colorScheme.primary,
    indicatorSize: Dp = 64.dp,
    strokeWidth: Dp = 6.dp,
    onCancel: (() -> Unit)? = null
) {
    val animatedProgress by animateFloatAsState(
        targetValue = (progress.coerceIn(0, 100) / 100f),
        animationSpec = ProgressIndicatorDefaults.ProgressAnimationSpec,
        label = "ffmpeg_progress_anim"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("ffmpeg_circular_progress_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
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
                            .size(32.dp)
                            .background(accentColor.copy(alpha = 0.2f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Memory,
                            contentDescription = "FFmpeg Engine",
                            tint = accentColor,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                if (onCancel != null) {
                    IconButton(
                        onClick = onCancel,
                        modifier = Modifier
                            .size(28.dp)
                            .testTag("cancel_ffmpeg_task_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Cancel Operation",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // Circular Progress Indicator & Status Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Circular Progress Ring with Center Percentage Text
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(indicatorSize)
                ) {
                    // Outer background track
                    CircularProgressIndicator(
                        progress = { 1f },
                        modifier = Modifier.size(indicatorSize),
                        color = accentColor.copy(alpha = 0.2f),
                        strokeWidth = strokeWidth
                    )

                    // Active animated progress ring
                    if (progress > 0) {
                        CircularProgressIndicator(
                            progress = { animatedProgress },
                            modifier = Modifier
                                .size(indicatorSize)
                                .testTag("ffmpeg_circular_progress_indicator"),
                            color = accentColor,
                            strokeWidth = strokeWidth
                        )
                    } else {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .size(indicatorSize)
                                .testTag("ffmpeg_circular_progress_indicator"),
                            color = accentColor,
                            strokeWidth = strokeWidth
                        )
                    }

                    // Percentage Readout
                    Text(
                        text = "${progress.coerceIn(0, 100)}%",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                // Status Message Column
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Surface(
                        color = accentColor.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = if (progress >= 100) "Completed" else "Executing FFmpeg Task...",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = accentColor,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }

                    Text(
                        text = statusText.ifBlank { "Processing video & audio streams..." },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 3
                    )
                }
            }
        }
    }
}
