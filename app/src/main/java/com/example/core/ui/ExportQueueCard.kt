package com.example.core.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ClearAll
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Queue
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.StopCircle
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.work.ExportQueueUiState
import com.example.core.work.ExportQueueViewModel
import com.example.core.work.QueueItemUiState
import com.example.core.work.QueueStatus

@Composable
fun ExportQueueCard(
    queueViewModel: ExportQueueViewModel,
    modifier: Modifier = Modifier,
    onViewSummary: ((QueueItemUiState) -> Unit)? = null
) {
    val uiState by queueViewModel.uiState.collectAsState()

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("export_queue_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
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
                    Icon(
                        imageVector = Icons.Default.Queue,
                        contentDescription = "Export Queue",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "Sequential Export Queue",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "WorkManager sequential batch export to device gallery",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                val statusBg = if (uiState.isSequentialProcessingActive) Color(0xFF10B981).copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant
                val statusFg = if (uiState.isSequentialProcessingActive) Color(0xFF10B981) else MaterialTheme.colorScheme.onSurfaceVariant

                Surface(
                    color = statusBg,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = if (uiState.isSequentialProcessingActive) "WORKMANAGER ACTIVE" else "QUEUE IDLE",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = statusFg,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            // Metrics Summary Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                QueueMetric(label = "Queued", count = uiState.totalQueued, color = MaterialTheme.colorScheme.primary)
                QueueMetric(label = "Exporting", count = uiState.totalRunning, color = Color(0xFFD97706))
                QueueMetric(label = "In Gallery", count = uiState.totalCompleted, color = Color(0xFF10B981))
                QueueMetric(label = "Failed", count = uiState.totalFailed, color = Color(0xFFEF4444))
            }

            // Queue List or Empty State
            if (uiState.items.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.VideoLibrary,
                            contentDescription = "Queue Empty",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "No export jobs in queue",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Enqueue edited video projects for automatic background processing.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }
            } else {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    uiState.items.forEach { item ->
                        QueueItemRow(
                            item = item,
                            onCancel = { queueViewModel.cancelExportJob(item.id) },
                            onRetry = { queueViewModel.retryFailedExport(item) },
                            onViewSummary = onViewSummary
                        )
                    }
                }
            }

            // Quick Batch Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (uiState.items.isNotEmpty()) {
                    OutlinedButton(
                        onClick = { queueViewModel.clearCompletedJobs() },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("clear_completed_button")
                    ) {
                        Icon(imageVector = Icons.Default.ClearAll, contentDescription = "Clear Completed")
                    }

                    if (uiState.isSequentialProcessingActive) {
                        OutlinedButton(
                            onClick = { queueViewModel.cancelAllExports() },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.testTag("cancel_all_queue_button")
                        ) {
                            Icon(imageVector = Icons.Default.StopCircle, contentDescription = "Cancel All")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun QueueMetric(label: String, count: Int, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            color = color
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun QueueItemRow(
    item: QueueItemUiState,
    onCancel: () -> Unit,
    onRetry: () -> Unit,
    onViewSummary: ((QueueItemUiState) -> Unit)? = null
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("queue_item_row_${item.id}"),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val statusIcon = when (item.status) {
                        QueueStatus.QUEUED -> Icons.Default.HourglassEmpty
                        QueueStatus.RUNNING -> Icons.Default.PlayArrow
                        QueueStatus.SUCCEEDED -> Icons.Default.CheckCircle
                        QueueStatus.FAILED -> Icons.Default.Error
                        QueueStatus.CANCELLED -> Icons.Default.Close
                    }

                    val iconTint = when (item.status) {
                        QueueStatus.QUEUED -> MaterialTheme.colorScheme.primary
                        QueueStatus.RUNNING -> Color(0xFFD97706)
                        QueueStatus.SUCCEEDED -> Color(0xFF10B981)
                        QueueStatus.FAILED -> Color(0xFFEF4444)
                        QueueStatus.CANCELLED -> MaterialTheme.colorScheme.onSurfaceVariant
                    }

                    Icon(
                        imageVector = statusIcon,
                        contentDescription = item.status.name,
                        tint = iconTint,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (item.jobType == "BATCH_COLOR_GRADING") {
                        Surface(
                            color = Color(0xFF6366F1).copy(alpha = 0.2f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "BATCH FILTER",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = Color(0xFF6366F1),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                    } else if (item.jobType == "SPEED_RETIME") {
                        Surface(
                            color = Color(0xFFE11D48).copy(alpha = 0.2f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "SPEED RETIME",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = Color(0xFFE11D48),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                    } else if (item.jobType == "TRANSITION_RENDER") {
                        Surface(
                            color = Color(0xFF0284C7).copy(alpha = 0.2f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "TRANSITION",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = Color(0xFF0284C7),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                    } else if (item.jobType == "VISUALIZER") {
                        Surface(
                            color = Color(0xFF9333EA).copy(alpha = 0.2f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "VISUALIZER",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = Color(0xFF9333EA),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                    } else if (item.jobType == "SLIDESHOW") {
                        Surface(
                            color = Color(0xFF059669).copy(alpha = 0.2f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "SLIDESHOW",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = Color(0xFF059669),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                    }

                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = item.format.uppercase(),
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    if (item.status == QueueStatus.FAILED) {
                        IconButton(
                            onClick = onRetry,
                            modifier = Modifier
                                .size(28.dp)
                                .testTag("retry_item_${item.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Retry Export",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    if (item.status == QueueStatus.QUEUED || item.status == QueueStatus.RUNNING) {
                        IconButton(
                            onClick = onCancel,
                            modifier = Modifier
                                .size(28.dp)
                                .testTag("cancel_item_${item.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Cancel Export",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            if (item.status == QueueStatus.RUNNING) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.size(36.dp)
                    ) {
                        CircularProgressIndicator(
                            progress = { 1f },
                            modifier = Modifier.size(36.dp),
                            color = Color(0xFF10B981).copy(alpha = 0.2f),
                            strokeWidth = 3.dp
                        )
                        CircularProgressIndicator(
                            progress = { item.progress / 100f },
                            modifier = Modifier
                                .size(36.dp)
                                .testTag("export_queue_circular_progress"),
                            color = Color(0xFF10B981),
                            strokeWidth = 3.dp
                        )
                        Text(
                            text = "${item.progress}%",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Text(
                        text = item.statusText,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFFD97706),
                        modifier = Modifier.weight(1f)
                    )
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = item.statusText,
                        style = MaterialTheme.typography.labelSmall,
                        color = when (item.status) {
                            QueueStatus.SUCCEEDED -> Color(0xFF10B981)
                            QueueStatus.FAILED -> Color(0xFFEF4444)
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )

                    if (item.status == QueueStatus.SUCCEEDED && onViewSummary != null) {
                        OutlinedButton(
                            onClick = { onViewSummary(item) },
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            modifier = Modifier.testTag("view_item_summary_${item.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = "Asset Report",
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Asset Report", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }
    }
}
