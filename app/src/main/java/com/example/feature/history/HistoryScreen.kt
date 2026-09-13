package com.example.feature.history

import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.database.CloudSyncState
import com.example.ui.theme.ElegantGoldDim
import com.example.ui.theme.StudioSuccessGreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Custom Composable that retrieves a video/media frame thumbnail
 * using Android's MediaMetadataRetriever asynchronously.
 */
@Composable
fun MediaThumbnailImage(
    uriString: String,
    modifier: Modifier = Modifier,
    placeholderIcon: ImageVector = Icons.Default.Movie
) {
    val context = LocalContext.current
    var bitmap by remember(uriString) { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(uriString) {
        if (uriString.isBlank()) return@LaunchedEffect
        withContext(Dispatchers.IO) {
            var retriever: MediaMetadataRetriever? = null
            try {
                retriever = MediaMetadataRetriever()
                val file = File(uriString)
                if (file.exists()) {
                    retriever.setDataSource(file.absolutePath)
                } else if (uriString.startsWith("content://") || uriString.startsWith("file://") || uriString.startsWith("http")) {
                    retriever.setDataSource(context, Uri.parse(uriString))
                }
                val frame = retriever.getFrameAtTime(1_000_000, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                    ?: retriever.frameAtTime
                bitmap = frame
            } catch (e: Exception) {
                // If thumbnail extraction fails (e.g. invalid format or remote URI), keep placeholder
            } finally {
                try {
                    retriever?.release()
                } catch (_: Exception) {}
            }
        }
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f), RoundedCornerShape(8.dp)),
        contentAlignment = Alignment.Center
    ) {
        val currentBitmap = bitmap
        if (currentBitmap != null) {
            Image(
                bitmap = currentBitmap.asImageBitmap(),
                contentDescription = "Media Thumbnail",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = placeholderIcon,
                    contentDescription = "Media Placeholder",
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun HistoryScreen(
    viewModel: HistoryViewModel,
    onNavigateToGoLive: (sourceUri: String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    val filteredJobs = if (uiState.selectedFilterType == "ALL") {
        uiState.renderJobs
    } else {
        uiState.renderJobs.filter { it.jobType == uiState.selectedFilterType }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Header
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(imageVector = Icons.Default.History, contentDescription = "History", tint = MaterialTheme.colorScheme.primary)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text("Job History & Cloud Sync", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onSurface)
                    Text("Firebase Firestore multi-device editing history sync", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        // Firebase Firestore Cloud Sync Status Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("firestore_sync_card"),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f))
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
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(
                                    Brush.linearGradient(
                                        listOf(
                                            MaterialTheme.colorScheme.secondary,
                                            MaterialTheme.colorScheme.primary
                                        )
                                    ),
                                    CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = when (uiState.syncInfo.syncState) {
                                    CloudSyncState.SYNCED -> Icons.Default.CloudDone
                                    CloudSyncState.SYNCING -> Icons.Default.CloudSync
                                    else -> Icons.Default.Cloud
                                },
                                contentDescription = "Cloud Sync Status",
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Firebase Firestore Sync",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Surface(
                                    color = when (uiState.syncInfo.syncState) {
                                        CloudSyncState.SYNCED -> StudioSuccessGreen.copy(alpha = 0.2f)
                                        CloudSyncState.SYNCING -> MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f)
                                        else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f)
                                    },
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = uiState.syncInfo.syncState.name,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = when (uiState.syncInfo.syncState) {
                                            CloudSyncState.SYNCED -> StudioSuccessGreen
                                            CloudSyncState.SYNCING -> MaterialTheme.colorScheme.secondary
                                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                                        }
                                    )
                                }
                            }
                            Text(
                                text = "Device: ${uiState.syncInfo.currentDeviceId}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Auto Sync Toggle
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = if (uiState.syncInfo.isAutoSyncEnabled) "Auto Sync" else "Off",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (uiState.syncInfo.isAutoSyncEnabled) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(end = 4.dp)
                        )
                        Switch(
                            checked = uiState.syncInfo.isAutoSyncEnabled,
                            onCheckedChange = { viewModel.toggleAutoCloudSync() },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = MaterialTheme.colorScheme.onSecondary,
                                checkedTrackColor = MaterialTheme.colorScheme.secondary
                            ),
                            modifier = Modifier.testTag("toggle_cloud_sync_switch")
                        )
                    }
                }

                // Sync Info Stats Pill Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        modifier = Modifier.weight(1f),
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Synced Records:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("${uiState.syncInfo.totalSyncedCount}", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onSurface)
                        }
                    }

                    Surface(
                        modifier = Modifier.weight(1f),
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Linked Devices:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("${uiState.syncInfo.remoteDeviceCount}", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.secondary)
                        }
                    }
                }

                // Sync Controls
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (uiState.syncInfo.lastSyncedTimeMs > 0)
                            "Last synced: ${formatTimestamp(uiState.syncInfo.lastSyncedTimeMs)}"
                        else
                            "Real-time Firestore listener active",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedButton(
                        onClick = { viewModel.triggerManualCloudSync() },
                        modifier = Modifier.testTag("manual_cloud_sync_button"),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.secondary)
                    ) {
                        Icon(imageVector = Icons.Default.Refresh, contentDescription = "Sync", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Sync Now", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }

        // Tab Selector
        TabRow(
            selectedTabIndex = uiState.selectedTab,
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ) {
            Tab(
                selected = uiState.selectedTab == 0,
                onClick = { viewModel.setTab(0) },
                text = { Text("Render Jobs (${uiState.renderJobs.size})") },
                modifier = Modifier.testTag("tab_render_jobs")
            )
            Tab(
                selected = uiState.selectedTab == 1,
                onClick = { viewModel.setTab(1) },
                text = { Text("Live Sessions (${uiState.liveSessions.size})") },
                modifier = Modifier.testTag("tab_live_sessions")
            )
            Tab(
                selected = uiState.selectedTab == 2,
                onClick = { viewModel.setTab(2) },
                text = { Text("Firestore Logs (${uiState.jobHistoryLogs.size})") },
                modifier = Modifier.testTag("tab_firestore_logs")
            )
        }

        if (uiState.selectedTab == 0) {
            // Filter Chips for Jobs
            androidx.compose.foundation.layout.FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("ALL", "LOOP", "MASTERING", "EDITOR").forEach { type ->
                    FilterChip(
                        selected = uiState.selectedFilterType == type,
                        onClick = { viewModel.setFilterType(type) },
                        label = { Text(type, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis) },
                        modifier = Modifier.testTag("filter_chip_$type")
                    )
                }
            }

            if (filteredJobs.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No render job history found.", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(filteredJobs, key = { it.id }) { job ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        MediaThumbnailImage(
                                            uriString = job.outputUri.ifBlank { job.inputUri },
                                            modifier = Modifier
                                                .size(64.dp)
                                                .testTag("job_thumbnail_${job.id}"),
                                            placeholderIcon = Icons.Default.Movie
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(job.title, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onSurface)
                                            Spacer(modifier = Modifier.height(2.dp))
                                            // Cloud badge reflects THIS job's own isSyncedToCloud flag
                                            // (set only after a real per-job Firestore upload success -
                                            // see LoopingVidRepository.markJobSyncedToCloud). Previously
                                            // this read the app-wide syncInfo.syncState, so every job in
                                            // the list showed the same badge regardless of whether it
                                            // individually reached the cloud.
                                            val isCloudSynced = job.isSyncedToCloud
                                            val badgeColor = if (isCloudSynced) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant
                                            Surface(
                                                color = badgeColor.copy(alpha = 0.15f),
                                                shape = RoundedCornerShape(4.dp)
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Icon(
                                                        imageVector = if (isCloudSynced) Icons.Default.CloudDone else Icons.Default.Cloud,
                                                        contentDescription = null,
                                                        tint = badgeColor,
                                                        modifier = Modifier.size(12.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text(
                                                        if (isCloudSynced) "Synced to Firestore" else "Local only",
                                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                        color = badgeColor
                                                    )
                                                }
                                            }
                                        }
                                    }
                                    IconButton(
                                        onClick = { viewModel.deleteJob(job.id) },
                                        modifier = Modifier.testTag("delete_job_${job.id}")
                                    ) {
                                        Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete Job", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }

                                Text(job.paramsSummary, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Job status chip (colored) so FAILED/PROCESSING/CANCELLED are visible.
                                    val statusColor = when (job.status) {
                                        "COMPLETED" -> StudioSuccessGreen
                                        "PROCESSING" -> MaterialTheme.colorScheme.secondary
                                        "FAILED" -> MaterialTheme.colorScheme.error
                                        "CANCELLED" -> ElegantGoldDim
                                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                                    }
                                    Surface(color = statusColor.copy(alpha = 0.15f), shape = RoundedCornerShape(4.dp)) {
                                        Text(
                                            job.status,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            color = statusColor
                                        )
                                    }
                                    Text("Size: %.1f MB".format(job.fileSizeMb), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                                    Text(formatTimestamp(job.createdAt), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }

                                if (job.status == "COMPLETED") {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        // Share the exported output file.
                                        OutlinedButton(
                                            onClick = { shareMediaFile(context, job.outputUri) },
                                            modifier = Modifier
                                                .weight(1f)
                                                .testTag("share_history_${job.id}")
                                        ) {
                                            Icon(imageVector = Icons.Default.Share, contentDescription = "Share", modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Share", style = MaterialTheme.typography.labelMedium)
                                        }
                                        // Edit the output file's title/artist/album/genre/year/comment
                                        // tags in place (lossless remux, MP3/WAV/M4A/MP4/MKV/MOV).
                                        OutlinedButton(
                                            onClick = { viewModel.openMetadataEditor(job) },
                                            modifier = Modifier
                                                .weight(1f)
                                                .testTag("edit_metadata_${job.id}")
                                        ) {
                                            Icon(imageVector = Icons.Default.Info, contentDescription = "Edit Metadata", modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Metadata", style = MaterialTheme.typography.labelMedium)
                                        }
                                        Button(
                                            onClick = { onNavigateToGoLive(job.outputUri) },
                                            modifier = Modifier
                                                .weight(1f)
                                                .testTag("go_live_history_${job.id}"),
                                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                                        ) {
                                            Icon(imageVector = Icons.Default.Radio, contentDescription = "Go Live", modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                "Go Live",
                                                color = MaterialTheme.colorScheme.onSecondary,
                                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else if (uiState.selectedTab == 1) {
            // Live Sessions List
            if (uiState.liveSessions.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No past live streaming sessions recorded.", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(uiState.liveSessions, key = { it.id }) { session ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        MediaThumbnailImage(
                                            uriString = session.sourceUri,
                                            modifier = Modifier
                                                .size(56.dp)
                                                .testTag("live_session_thumbnail_${session.id}"),
                                            placeholderIcon = Icons.Default.Radio
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(session.streamTitle, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onSurface)
                                            Text("Platform: ${session.platform} | Total Loops: ${session.totalLoops}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }
                                    IconButton(onClick = { viewModel.deleteLiveSession(session.id) }) {
                                        Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete Session", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }

                                Text("Duration: ${session.durationSec}s | Avg Bitrate: ${session.avgBitrateKbps} kbps", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                            }
                        }
                    }
                }
            }
        } else {
            // Firestore Execution Logs Tab
            if (uiState.jobHistoryLogs.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(imageVector = Icons.Default.Terminal, contentDescription = null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("No synced execution logs recorded yet.", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("Perform video renders or audio mastering to generate logs.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(uiState.jobHistoryLogs, key = { it.id }) { history ->
                        var showLogsDetails by remember { mutableStateOf(false) }

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        MediaThumbnailImage(
                                            uriString = history.outputUri.ifBlank { history.inputUri },
                                            modifier = Modifier
                                                .size(52.dp)
                                                .testTag("log_thumbnail_${history.id}"),
                                            placeholderIcon = Icons.Default.Terminal
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(history.title, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onSurface)
                                            Text("Type: ${history.taskType} | Status: ${history.status}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }
                                    IconButton(onClick = { viewModel.deleteJobHistoryLog(history.id) }) {
                                        Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete Log", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Duration: ${history.durationMs}ms", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                                    OutlinedButton(
                                        onClick = { showLogsDetails = !showLogsDetails },
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text(if (showLogsDetails) "Hide Logs" else "View Logs", style = MaterialTheme.typography.labelMedium)
                                    }
                                }

                                AnimatedVisibility(visible = showLogsDetails) {
                                    Surface(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(top = 6.dp),
                                        color = MaterialTheme.colorScheme.background,
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text(
                                            text = history.executionLogs.ifBlank { "Task completed with zero errors." },
                                            modifier = Modifier.padding(10.dp),
                                            style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                                            color = MaterialTheme.colorScheme.secondary
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

    // Metadata editor dialog: lets the user set title/artist/album/genre/year/comment on an
    // already-exported MP3/WAV/M4A/MP4/MKV/MOV file, applied via a lossless FFmpeg remux.
    uiState.metadataEditJob?.let { job ->
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { if (!uiState.isMetadataEditSaving) viewModel.dismissMetadataEditor() },
            title = { Text("Edit Metadata") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = job.title,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (uiState.isMetadataEditLoading) {
                        Text(
                            text = "Reading current tags...",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                    com.example.feature.mastering.AudioMetadataEditorCard(
                        metadata = uiState.metadataEditValues,
                        onMetadataChanged = { viewModel.updateMetadataEditValues(it) }
                    )
                    uiState.metadataEditResultMessage?.let { msg ->
                        Text(
                            text = msg,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.saveMetadataEdit() },
                    enabled = !uiState.isMetadataEditSaving
                ) {
                    Text(if (uiState.isMetadataEditSaving) "Saving..." else "Save")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { viewModel.dismissMetadataEditor() },
                    enabled = !uiState.isMetadataEditSaving
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}

private fun formatTimestamp(timeMs: Long): String {
    val sdf = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())
    return sdf.format(Date(timeMs))
}

/**
 * Shares an exported media file with other apps via a FileProvider content Uri. Falls back
 * gracefully (no crash) if the path is blank/missing or no share target exists.
 */
private fun shareMediaFile(context: android.content.Context, path: String?) {
    if (path.isNullOrBlank()) return
    try {
        val file = java.io.File(path)
        if (!file.exists()) return
        val uri = androidx.core.content.FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val mime = when (file.extension.lowercase()) {
            "mp3", "wav", "m4a" -> "audio/*"
            else -> "video/*"
        }
        val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
            type = mime
            putExtra(android.content.Intent.EXTRA_STREAM, uri)
            addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(
            android.content.Intent.createChooser(intent, "Share media").apply {
                addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        )
    } catch (e: Exception) {
        // Sharing is best-effort; never crash the History screen on failure.
    }
}

