package com.example.feature.live

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Loop
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PauseCircle
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.SignalCellular4Bar
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.ui.BatteryStatusCard
import com.example.core.ui.PermissionManagerCard
import com.example.core.ui.VideoPlayer
import com.example.core.utils.RtmpUrlValidator

enum class LiveInputSource {
    CAMERAX, FILE_LOOP, MEDIA3_CAMERA
}

enum class LiveToolCategory(
    val title: String,
    val icon: ImageVector,
    val description: String
) {
    INPUT_SOURCE("Source & Overlay", Icons.Default.Videocam, "Camera Feed, Video File & Ticker Overlay"),
    RTMP_SERVER("RTMP & Server", Icons.Default.Radio, "Server URL, Stream Key & Quick Settings"),
    AUDIO_MIXER("Audio Mixer", Icons.Default.Equalizer, "Master Volume, Bass & Equalizer"),
    TELEMETRY("Health & D3", Icons.Default.Speed, "Health Dashboard, Diagnostics & D3 Overlay"),
    SYSTEM_STATUS("System & Logs", Icons.Default.Info, "Thermal, Storage, Battery & Event Logs"),
    ENGAGEMENT("Engagement", Icons.AutoMirrored.Filled.TrendingUp, "Viewer count & Real-time Metrics"),
    ALL_TOOLS("All Tools", Icons.Default.FolderOpen, "View All Live Studio Panels")
}

/**
 * LiveStreamingPage provides a complete live streaming control dashboard:
 * Organized in clean CapCut-Style Tool Categories with a pinned preview canvas.
 */
@OptIn(ExperimentalAnimationApi::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun LiveStreamingPage(
    viewModel: LiveViewModel,
    modifier: Modifier = Modifier,
    initialSourceUri: String? = null
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    var activeInputSource by remember { mutableStateOf(LiveInputSource.CAMERAX) }
    var activeCategory by remember { mutableStateOf(LiveToolCategory.INPUT_SOURCE) }
    var isDropdownMenuExpanded by remember { mutableStateOf(false) }
    var isKeyVisible by remember { mutableStateOf(false) }

    val urlValidation = remember(uiState.rtmpUrl) {
        RtmpUrlValidator.validateRtmpUrl(uiState.rtmpUrl)
    }
    val keyValidation = remember(uiState.streamKey) {
        RtmpUrlValidator.validateStreamKey(uiState.streamKey)
    }
    val configValidation = remember(uiState.rtmpUrl, uiState.streamKey) {
        RtmpUrlValidator.validateConfiguration(uiState.rtmpUrl, uiState.streamKey)
    }

    // Register PowerManager thermal status listener & safely initialize live streaming module
    androidx.compose.runtime.LaunchedEffect(Unit) {
        viewModel.initializeStreamModule(context)
        try {
            com.example.core.utils.ThermalMonitor.registerThermalListener(context) { _ ->
                viewModel.checkThermalStatus(context)
            }
        } catch (_: Throwable) {
            // Thermal listener registration handled safely
        }
    }

    // Auto set source if passed from another screen
    androidx.compose.runtime.LaunchedEffect(initialSourceUri) {
        initialSourceUri?.let { uri ->
            if (uri.isNotBlank()) {
                viewModel.setSourceMedia(uri, "Rendered_Source.mp4")
            }
        }
    }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            viewModel.setSourceMedia(it.toString(), it.lastPathSegment ?: "live_stream_source.mp4")
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(scrollState)
            .padding(16.dp)
            .testTag("live_streaming_page"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Studio Header Command Bar (Stream Status Pill + Quick Live Toggle)
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            shape = RoundedCornerShape(16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Status Pill
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .background(
                            when (uiState.streamStatus) {
                                StreamStatus.LIVE -> Color(0xFF065F46)
                                StreamStatus.CONNECTING, StreamStatus.RECONNECTING -> Color(0xFF92400E)
                                else -> MaterialTheme.colorScheme.surface
                            },
                            RoundedCornerShape(20.dp)
                        )
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(
                                when (uiState.streamStatus) {
                                    StreamStatus.LIVE -> Color(0xFF34D399)
                                    StreamStatus.CONNECTING, StreamStatus.RECONNECTING -> Color(0xFFFBBF24)
                                    else -> Color(0xFF9CA3AF)
                                },
                                CircleShape
                            )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = when (uiState.streamStatus) {
                            StreamStatus.LIVE -> "LIVE studio (${formatDuration(uiState.liveDurationSec)})"
                            StreamStatus.CONNECTING -> "CONNECTING..."
                            StreamStatus.RECONNECTING -> "RECONNECTING..."
                            else -> "READY TO BROADCAST"
                        },
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = when (uiState.streamStatus) {
                            StreamStatus.LIVE -> Color(0xFFD1FAE5)
                            StreamStatus.CONNECTING, StreamStatus.RECONNECTING -> Color(0xFFFEF3C7)
                            else -> MaterialTheme.colorScheme.onSurface
                        }
                    )
                }

                // Header Action Button
                if (uiState.streamStatus != StreamStatus.LIVE) {
                    Button(
                        onClick = { viewModel.startLiveStream(context) },
                        enabled = (activeInputSource == LiveInputSource.CAMERAX || activeInputSource == LiveInputSource.MEDIA3_CAMERA || uiState.sourceUri != null) && configValidation.isValid,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        modifier = Modifier.height(38.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Radio, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("GO LIVE", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold), color = Color.White)
                    }
                } else {
                    Button(
                        onClick = { viewModel.stopLiveStream(context) },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Gray),
                        modifier = Modifier.height(38.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Stop, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("STOP STREAM", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold), color = Color.White)
                    }
                }
            }
        }

        // 2. Hero Broadcast Preview Canvas Card (Pinned at Top)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("hero_live_preview_card"),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Source Selector Pills
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = activeInputSource == LiveInputSource.CAMERAX,
                        onClick = { activeInputSource = LiveInputSource.CAMERAX },
                        label = { Text("CameraX Feed", fontWeight = FontWeight.SemiBold) },
                        leadingIcon = { Icon(Icons.Default.Videocam, contentDescription = null) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("mode_camerax_chip")
                    )

                    FilterChip(
                        selected = activeInputSource == LiveInputSource.FILE_LOOP,
                        onClick = { activeInputSource = LiveInputSource.FILE_LOOP },
                        label = { Text("File Broadcast", fontWeight = FontWeight.SemiBold) },
                        leadingIcon = { Icon(Icons.Default.Movie, contentDescription = null) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("mode_file_loop_chip")
                    )

                    FilterChip(
                        selected = activeInputSource == LiveInputSource.MEDIA3_CAMERA,
                        onClick = { activeInputSource = LiveInputSource.MEDIA3_CAMERA },
                        label = { Text("Media3 Cam", fontWeight = FontWeight.SemiBold) },
                        leadingIcon = { Icon(Icons.Default.PlayCircle, contentDescription = null) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("mode_media3_cam_chip")
                    )
                }

                // Active Source Display
                if (activeInputSource == LiveInputSource.CAMERAX) {
                    CameraXLiveStreamScreen(
                        viewModel = viewModel
                    )
                } else if (activeInputSource == LiveInputSource.MEDIA3_CAMERA) {
                    Media3LiveStreamScreen(
                        viewModel = viewModel
                    )
                } else {
                    if (uiState.sourceUri == null) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                                .clickable { filePickerLauncher.launch("video/*") }
                                .testTag("select_broadcast_source_button"),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.FolderOpen,
                                    contentDescription = "Select Source Video",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(36.dp)
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Pilih Video untuk Broadcast 24/7",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Mendukung MP4, MKV, WebM",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    } else {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val currentStyle = TICKER_PRESET_STYLES.getOrElse(uiState.selectedTickerStyleIndex) { TICKER_PRESET_STYLES[0] }

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(180.dp)
                                    .clip(RoundedCornerShape(12.dp))
                            ) {
                                VideoPlayer(
                                    mediaUri = uiState.sourceUri,
                                    modifier = Modifier.fillMaxSize(),
                                    isPlaying = uiState.isSourcePreviewPlaying || uiState.streamStatus == StreamStatus.LIVE,
                                    isLooping = true,
                                    testTag = "live_source_video_player"
                                )

                                if (uiState.isTickerEnabled) {
                                    ScrollingTickerOverlay(
                                        text = uiState.tickerText,
                                        enabled = uiState.isTickerEnabled,
                                        speed = uiState.tickerSpeed,
                                        bgColor = currentStyle.bgColor,
                                        textColor = currentStyle.textColor,
                                        modifier = Modifier.align(
                                            if (uiState.tickerPosition == TickerPosition.TOP) Alignment.TopCenter else Alignment.BottomCenter
                                        )
                                    )
                                }

                                // Stream Health Overlay Badge with Status Dots
                                VideoOverlayStatusBadge(
                                    uiState = uiState,
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(8.dp)
                                )

                                // Floating D3.js Real-time Data Telemetry Overlay
                                if (uiState.showD3OverlayOnVideo) {
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.BottomStart)
                                            .padding(8.dp)
                                            .fillMaxWidth(0.65f)
                                            .height(75.dp)
                                            .alpha(uiState.d3OverlayOpacity)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color(0xFF0F0C20).copy(alpha = 0.85f))
                                            .border(1.dp, Color(0xFF00E5FF).copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                                    ) {
                                        D3TelemetryWebView(
                                            viewerHistory = uiState.viewerHistory,
                                            bandwidthHistory = uiState.bandwidthHistoryMbps,
                                            currentViewers = uiState.viewerCount,
                                            currentBandwidth = uiState.bandwidthMbps,
                                            visMode = uiState.d3VisualizationMode,
                                            timeWindowSec = uiState.d3OverlayTimeWindowSec,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }
                                }

                                // Video Buffering & Reconnection Overlay
                                VideoBufferingOverlay(
                                    isBuffering = uiState.isBuffering,
                                    streamStatus = uiState.streamStatus,
                                    reconnectAttempt = uiState.reconnectAttempt,
                                    maxReconnectAttempts = uiState.maxReconnectAttempts,
                                    countdownSec = uiState.reconnectCountdownSec,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp))
                                    .padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Movie,
                                    contentDescription = "Source Video File",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = uiState.sourceName,
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Loop Broadcast Ready",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.secondary
                                    )
                                }
                                IconButton(
                                    onClick = { viewModel.toggleSourcePreviewPlayPause() },
                                    modifier = Modifier.testTag("toggle_preview_play_pause_button")
                                ) {
                                    Icon(
                                        imageVector = if (uiState.isSourcePreviewPlaying) Icons.Default.PauseCircle else Icons.Default.PlayCircle,
                                        contentDescription = "Play/Pause Preview",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(4.dp))
                                OutlinedButton(
                                    onClick = { filePickerLauncher.launch("video/*") },
                                    enabled = uiState.streamStatus != StreamStatus.LIVE,
                                    modifier = Modifier.testTag("change_broadcast_source_button")
                                ) {
                                    Text("Ganti File")
                                }
                            }
                        }
                    }
                }
            }
        }

        // 3. CapCut-Style Modern Tool Category Bar (Dropdown Menu + Category Chips)
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp, horizontal = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Fitur Studio Live",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Quick Jump Dropdown
                    Box {
                        Surface(
                            onClick = { isDropdownMenuExpanded = true },
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surface,
                            modifier = Modifier.border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = activeCategory.icon,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = activeCategory.title,
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    imageVector = Icons.Default.ArrowDropDown,
                                    contentDescription = "Pilih Kategori",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        DropdownMenu(
                            expanded = isDropdownMenuExpanded,
                            onDismissRequest = { isDropdownMenuExpanded = false }
                        ) {
                            LiveToolCategory.entries.forEach { category ->
                                DropdownMenuItem(
                                    text = {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = category.icon,
                                                contentDescription = null,
                                                tint = if (activeCategory == category) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Column {
                                                Text(
                                                    text = category.title,
                                                    style = MaterialTheme.typography.bodyMedium.copy(
                                                        fontWeight = if (activeCategory == category) FontWeight.Bold else FontWeight.Normal
                                                    )
                                                )
                                                Text(
                                                    text = category.description,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    },
                                    onClick = {
                                        activeCategory = category
                                        isDropdownMenuExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Scrollable Category Chips
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(LiveToolCategory.entries) { category ->
                        FilterChip(
                            selected = activeCategory == category,
                            onClick = { activeCategory = category },
                            label = { Text(category.title, fontWeight = FontWeight.SemiBold) },
                            leadingIcon = {
                                Icon(
                                    imageVector = category.icon,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                            )
                        )
                    }
                }
            }
        }

        // 4. Animated Category Content Panels
        AnimatedContent(
            targetState = activeCategory,
            transitionSpec = {
                fadeIn(animationSpec = tween(260, easing = FastOutSlowInEasing)) togetherWith
                        fadeOut(animationSpec = tween(200, easing = FastOutSlowInEasing))
            },
            label = "live_tool_category_transition"
        ) { category ->
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                when (category) {
                    LiveToolCategory.INPUT_SOURCE -> {
                        // Scrolling Ticker Overlay Controls
                        ScrollingTickerControlCard(
                            tickerText = uiState.tickerText,
                            isTickerEnabled = uiState.isTickerEnabled,
                            tickerSpeed = uiState.tickerSpeed,
                            tickerPosition = uiState.tickerPosition,
                            selectedStyleIndex = uiState.selectedTickerStyleIndex,
                            onTickerTextChange = { viewModel.updateTickerText(it) },
                            onTickerEnabledChange = { viewModel.setTickerEnabled(it) },
                            onTickerSpeedChange = { viewModel.setTickerSpeed(it) },
                            onTickerPositionChange = { viewModel.setTickerPosition(it) },
                            onStyleSelect = { viewModel.setTickerStyleIndex(it) }
                        )
                    }

                    LiveToolCategory.RTMP_SERVER -> {
                        // Stream Connection Status Card
                        ConnectionHealthCard(
                            status = uiState.streamStatus,
                            bitrateKbps = uiState.currentBitrateKbps,
                            latencyMs = uiState.latencyMs,
                            durationSec = uiState.liveDurationSec,
                            loopCount = uiState.currentLoopRound
                        )

                        // RTMP Server URL & Stream Key Entry
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("rtmp_config_card"),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text(
                                    text = "Pengaturan Server RTMP",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )

                                androidx.compose.foundation.layout.FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    LivePlatform.entries.forEach { platform ->
                                        FilterChip(
                                            selected = uiState.platform == platform,
                                            onClick = { viewModel.setPlatform(platform) },
                                            label = {
                                                Text(
                                                    platform.name.replace("_", " "),
                                                    maxLines = 1,
                                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                                )
                                            },
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                            ),
                                            modifier = Modifier.testTag("platform_chip_${platform.name}")
                                        )
                                    }
                                }

                                OutlinedTextField(
                                    value = uiState.streamTitle,
                                    onValueChange = { viewModel.updateStreamTitle(it) },
                                    label = { Text("Judul Stream") },
                                    placeholder = { Text("Masukkan judul siaran...") },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("rtmp_stream_title_input"),
                                    singleLine = true,
                                    enabled = uiState.streamStatus != StreamStatus.LIVE
                                )

                                OutlinedTextField(
                                    value = uiState.rtmpUrl,
                                    onValueChange = { viewModel.updateRtmpUrl(it) },
                                    label = { Text("RTMP Server URL") },
                                    placeholder = { Text("rtmp://a.rtmp.youtube.com/live2") },
                                    isError = !urlValidation.isValid && uiState.rtmpUrl.isNotEmpty(),
                                    supportingText = {
                                        if (!urlValidation.isValid && uiState.rtmpUrl.isNotEmpty()) {
                                            Text(
                                                text = urlValidation.errorMessage ?: "URL RTMP Tidak Valid",
                                                color = MaterialTheme.colorScheme.error
                                            )
                                        }
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("rtmp_server_url_input"),
                                    singleLine = true,
                                    enabled = uiState.streamStatus != StreamStatus.LIVE
                                )

                                OutlinedTextField(
                                    value = uiState.streamKey,
                                    onValueChange = { viewModel.updateStreamKey(it) },
                                    label = { Text("Stream Key") },
                                    placeholder = { Text("Masukkan stream key RTMP Anda") },
                                    isError = !keyValidation.isValid && uiState.streamKey.isNotEmpty(),
                                    visualTransformation = if (isKeyVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Default.Key,
                                            contentDescription = "Stream Key Icon",
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    },
                                    trailingIcon = {
                                        IconButton(onClick = { isKeyVisible = !isKeyVisible }) {
                                            Icon(
                                                imageVector = if (isKeyVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                                contentDescription = "Toggle Stream Key Visibility"
                                            )
                                        }
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("rtmp_stream_key_input"),
                                    singleLine = true,
                                    enabled = uiState.streamStatus != StreamStatus.LIVE
                                )
                            }
                        }

                        // Multi-Destination Simulcast Broadcast Targets
                        BroadcastTargetsCard(
                            targets = uiState.targets,
                            isSimulcastEnabled = uiState.isSimulcastEnabled,
                            availableBandwidthKbps = uiState.availableBandwidthKbps,
                            isBandwidthSufficient = uiState.isBandwidthSufficient,
                            bandwidthWarningMessage = uiState.bandwidthWarningMessage,
                            streamStatus = uiState.streamStatus,
                            onToggleSimulcast = { viewModel.toggleSimulcastEnabled() },
                            onToggleTargetEnabled = { viewModel.toggleTargetEnabled(it) },
                            onAddTarget = { name, platform, url, key, bitrate ->
                                viewModel.addBroadcastTarget(name, platform, url, key, bitrate)
                            },
                            onUpdateTarget = { id, name, platform, url, key, bitrate ->
                                viewModel.updateBroadcastTarget(id, name, platform, url, key, bitrate)
                            },
                            onRemoveTarget = { viewModel.removeBroadcastTarget(it) },
                            onRunSpeedTest = { viewModel.simulateBandwidthTest() }
                        )

                        // Quick Stream Settings Panel
                        StreamQuickSettingsPanel(
                            uiState = uiState,
                            onToggleExpanded = { viewModel.toggleQuickSettingsPanel() },
                            onToggleCameraFocus = { viewModel.toggleCameraAutoFocus() },
                            onSetCameraFocusMode = { viewModel.setCameraFocusMode(it) },
                            onToggleAudioMute = { viewModel.toggleQuickMuteAudio() },
                            onSetResolution = { viewModel.setStreamResolution(it) },
                            onToggleTorch = { viewModel.toggleTorchActive() },
                            onToggleTicker = { viewModel.setTickerEnabled(!uiState.isTickerEnabled) },
                            onSetMasterVolume = { viewModel.setMasterVolume(it) }
                        )

                        // Stream Controls Card
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("stream_controls_card"),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp)
                            ) {
                                if (uiState.streamStatus != StreamStatus.LIVE) {
                                    Button(
                                        onClick = { viewModel.startLiveStream(context) },
                                        enabled = (activeInputSource == LiveInputSource.CAMERAX || activeInputSource == LiveInputSource.MEDIA3_CAMERA || uiState.sourceUri != null) && configValidation.isValid,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(52.dp)
                                            .testTag("start_stream_button"),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                                    ) {
                                        Icon(imageVector = Icons.Default.Radio, contentDescription = null, tint = Color.White)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = if (uiState.streamStatus == StreamStatus.CONNECTING) "MENGHUBUNGKAN..." else "MULAI SIARAN LIVE",
                                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                            color = Color.White
                                        )
                                    }
                                } else {
                                    Button(
                                        onClick = { viewModel.stopLiveStream(context) },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(52.dp)
                                            .testTag("stop_stream_button"),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color.Gray)
                                    ) {
                                        Icon(imageVector = Icons.Default.Stop, contentDescription = null, tint = Color.White)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "HENTIKAN SIARAN LIVE",
                                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                            color = Color.White
                                        )
                                    }
                                }
                            }
                        }
                    }

                    LiveToolCategory.AUDIO_MIXER -> {
                        // Real-Time Audio Mixer Controls
                        AudioMixerCard(
                            videoVolume = uiState.videoSourceVolume,
                            isVideoMuted = uiState.isVideoSourceMuted,
                            micVolume = uiState.micInputVolume,
                            isMicMuted = uiState.isMicInputMuted,
                            bgMusicVolume = uiState.bgMusicVolume,
                            isBgMusicMuted = uiState.isBgMusicMuted,
                            bgMusicTrack = uiState.bgMusicTrack,
                            isMicDuckingEnabled = uiState.isMicDuckingEnabled,
                            masterVolume = uiState.masterVolume,
                            isMasterMuted = uiState.isMasterMuted,
                            videoVuLevel = uiState.videoVuLevel,
                            micVuLevel = uiState.micVuLevel,
                            bgMusicVuLevel = uiState.bgMusicVuLevel,
                            masterVuLevel = uiState.masterVuLevel,
                            onVideoVolumeChange = { viewModel.setVideoSourceVolume(it) },
                            onVideoMuteToggle = { viewModel.toggleVideoSourceMute() },
                            onMicVolumeChange = { viewModel.setMicInputVolume(it) },
                            onMicMuteToggle = { viewModel.toggleMicInputMute() },
                            onBgMusicVolumeChange = { viewModel.setBgMusicVolume(it) },
                            onBgMusicMuteToggle = { viewModel.toggleBgMusicMute() },
                            onBgMusicTrackChange = { viewModel.setBgMusicTrack(it) },
                            onMicDuckingToggle = { viewModel.toggleMicDucking() },
                            onMasterVolumeChange = { viewModel.setMasterVolume(it) },
                            onMasterMuteToggle = { viewModel.toggleMasterMute() }
                        )

                        // Media3 Equalizer & Audio Processing
                        AudioAdjustmentCard(
                            volume = uiState.masterVolume,
                            isMuted = uiState.isMasterMuted,
                            bassGainDb = uiState.bassGainDb,
                            trebleGainDb = uiState.trebleGainDb,
                            selectedPresetName = uiState.selectedEqPresetName,
                            onVolumeChange = { viewModel.setMasterVolume(it) },
                            onMuteToggle = { viewModel.toggleMasterMute() },
                            onBassChange = { viewModel.setBassGainDb(it) },
                            onTrebleChange = { viewModel.setTrebleGainDb(it) },
                            onSelectPreset = { preset ->
                                viewModel.selectEqPreset(preset.name, preset.bassDb, preset.trebleDb)
                            },
                            onResetAdjustments = { viewModel.resetAudioAdjustments() }
                        )
                    }

                    LiveToolCategory.TELEMETRY -> {
                        // System & Network Health Status Card
                        StreamHealthStatusCard(
                            uiState = uiState
                        )

                        // Stream Diagnostic Overlay Component
                        StreamDiagnosticOverlayCard(
                            uiState = uiState,
                            onToggleExpanded = { viewModel.toggleDiagnosticOverlayExpanded() },
                            onCloseOverlay = { viewModel.toggleDiagnosticOverlay() }
                        )

                        // Buffering & Network Reconnection State Indicator
                        BufferingStateIndicator(
                            uiState = uiState,
                            onRetryNow = { viewModel.handleNetworkLoss("Manual user retry handshake", context) },
                            onCancel = { viewModel.cancelReconnection() }
                        )

                        // Real-time Network Metrics Health Dashboard
                        LiveHealthDashboard(
                            uiState = uiState,
                            onSimulateNetworkLoss = { viewModel.simulateConnectionLoss(context) }
                        )

                        // D3.js Real-time Data Overlay Card
                        D3DataOverlayCard(
                            uiState = uiState,
                            onToggleOverlay = { viewModel.toggleD3OverlayOnVideo() },
                            onSetOpacity = { viewModel.setD3OverlayOpacity(it) },
                            onSetTimeWindow = { viewModel.setD3OverlayTimeWindow(it) },
                            onSetVisMode = { viewModel.setD3VisualizationMode(it) }
                        )
                    }

                    LiveToolCategory.SYSTEM_STATUS -> {
                        // Thermal Monitoring Component
                        ThermalWarningBanner(
                            thermalInfo = uiState.thermalInfo,
                            onCoolDownClick = { viewModel.coolDownEncoding() },
                            onSimulateTestClick = { viewModel.simulateThermalWarning() }
                        )

                        // Storage Watcher Component
                        StorageWatcherCard(
                            storageInfo = uiState.storageInfo,
                            onRefreshClick = { viewModel.refreshStorageInfo(context) },
                            onSimulateLowStorageClick = { viewModel.simulateLowStorageAlert() }
                        )

                        // Battery Monitor Component
                        BatteryStatusCard(
                            onBatteryAlertTriggered = { batteryInfo ->
                                viewModel.addLog(
                                    level = if (batteryInfo.isCriticalBattery) LiveLogLevel.ERROR else LiveLogLevel.WARN,
                                    category = LiveLogCategory.SYSTEM,
                                    message = "Battery status warning (${batteryInfo.percentage}%): ${batteryInfo.warningMessage}"
                                )
                            }
                        )

                        // Permission Manager
                        PermissionManagerCard()

                        // Auto-Stop Timer Configuration
                        AutoStopTimerCard(
                            autoStopMinutes = uiState.autoStopMinutes,
                            streamStatus = uiState.streamStatus,
                            liveDurationSec = uiState.liveDurationSec,
                            onAutoStopChange = { viewModel.setAutoStopMinutes(it) }
                        )

                        // Real-time Log Viewer
                        LiveLogViewerCard(
                            logs = uiState.logs,
                            onClearLogs = { viewModel.clearLogs() },
                            onSimulateLogEvent = { viewModel.simulateLogEvent() }
                        )
                    }

                    LiveToolCategory.ENGAGEMENT -> {
                        LiveDashboard(
                            viewerCount = uiState.viewerCount,
                            viewerHistory = uiState.viewerHistory,
                            isViewerCountLive = uiState.isViewerCountLive
                        )
                    }
                    LiveToolCategory.ALL_TOOLS -> {
                        // All panels combined under section titles
                        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            Text("1. Teks Overlay & Custom Ticker", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                            ScrollingTickerControlCard(
                                tickerText = uiState.tickerText,
                                isTickerEnabled = uiState.isTickerEnabled,
                                tickerSpeed = uiState.tickerSpeed,
                                tickerPosition = uiState.tickerPosition,
                                selectedStyleIndex = uiState.selectedTickerStyleIndex,
                                onTickerTextChange = { viewModel.updateTickerText(it) },
                                onTickerEnabledChange = { viewModel.setTickerEnabled(it) },
                                onTickerSpeedChange = { viewModel.setTickerSpeed(it) },
                                onTickerPositionChange = { viewModel.setTickerPosition(it) },
                                onStyleSelect = { viewModel.setTickerStyleIndex(it) }
                            )

                            Text("2. Konfigurasi Server RTMP & Target", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                            ConnectionHealthCard(
                                status = uiState.streamStatus,
                                bitrateKbps = uiState.currentBitrateKbps,
                                latencyMs = uiState.latencyMs,
                                durationSec = uiState.liveDurationSec,
                                loopCount = uiState.currentLoopRound
                            )

                            Text("3. Mixer Audio & Equalizer", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                            AudioMixerCard(
                                videoVolume = uiState.videoSourceVolume,
                                isVideoMuted = uiState.isVideoSourceMuted,
                                micVolume = uiState.micInputVolume,
                                isMicMuted = uiState.isMicInputMuted,
                                bgMusicVolume = uiState.bgMusicVolume,
                                isBgMusicMuted = uiState.isBgMusicMuted,
                                bgMusicTrack = uiState.bgMusicTrack,
                                isMicDuckingEnabled = uiState.isMicDuckingEnabled,
                                masterVolume = uiState.masterVolume,
                                isMasterMuted = uiState.isMasterMuted,
                                videoVuLevel = uiState.videoVuLevel,
                                micVuLevel = uiState.micVuLevel,
                                bgMusicVuLevel = uiState.bgMusicVuLevel,
                                masterVuLevel = uiState.masterVuLevel,
                                onVideoVolumeChange = { viewModel.setVideoSourceVolume(it) },
                                onVideoMuteToggle = { viewModel.toggleVideoSourceMute() },
                                onMicVolumeChange = { viewModel.setMicInputVolume(it) },
                                onMicMuteToggle = { viewModel.toggleMicInputMute() },
                                onBgMusicVolumeChange = { viewModel.setBgMusicVolume(it) },
                                onBgMusicMuteToggle = { viewModel.toggleBgMusicMute() },
                                onBgMusicTrackChange = { viewModel.setBgMusicTrack(it) },
                                onMicDuckingToggle = { viewModel.toggleMicDucking() },
                                onMasterVolumeChange = { viewModel.setMasterVolume(it) },
                                onMasterMuteToggle = { viewModel.toggleMasterMute() }
                            )

                            Text("4. Telemetri & D3 Data Overlay", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                            LiveHealthDashboard(
                                uiState = uiState,
                                onSimulateNetworkLoss = { viewModel.simulateConnectionLoss(context) }
                            )

                            Text("5. Perangkat & Log Sistem", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                            PermissionManagerCard()
                            BatteryStatusCard(
                                onBatteryAlertTriggered = { batteryInfo ->
                                    viewModel.addLog(
                                        level = if (batteryInfo.isCriticalBattery) LiveLogLevel.ERROR else LiveLogLevel.WARN,
                                        category = LiveLogCategory.SYSTEM,
                                        message = "Battery status warning (${batteryInfo.percentage}%): ${batteryInfo.warningMessage}"
                                    )
                                }
                            )

                            Text("6. Engagement & Metrics", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                            LiveDashboard(
                                viewerCount = uiState.viewerCount,
                                viewerHistory = uiState.viewerHistory,
                                isViewerCountLive = uiState.isViewerCountLive
                            )
                        }
                    }
                }
            }
        }
    }
}


/**
 * Connection Health and Status Indicator Component
 */
@Composable
fun ConnectionHealthCard(
    status: StreamStatus,
    bitrateKbps: Int,
    latencyMs: Int,
    durationSec: Long,
    loopCount: Int,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val alphaPulse by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alphaPulse"
    )

    val badgeColor by animateColorAsState(
        targetValue = when (status) {
            StreamStatus.LIVE -> Color(0xFF10B981) // Green
            StreamStatus.CONNECTING -> Color(0xFFF59E0B) // Amber
            StreamStatus.RECONNECTING -> Color(0xFFF97316) // Orange
            StreamStatus.OFFLINE, StreamStatus.STOPPED -> Color(0xFF6B7280) // Gray
        },
        label = "badgeColor"
    )

    val statusLabel = when (status) {
        StreamStatus.LIVE -> "LIVE & HEALTHY"
        StreamStatus.CONNECTING -> "CONNECTING..."
        StreamStatus.RECONNECTING -> "RECONNECTING..."
        StreamStatus.OFFLINE -> "OFFLINE / READY"
        StreamStatus.STOPPED -> "STREAM ENDED"
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("connection_health_card"),
        colors = CardDefaults.cardColors(
            containerColor = if (status == StreamStatus.LIVE) Color(0xFF130F26) else MaterialTheme.colorScheme.surface
        ),
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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(14.dp)
                            .alpha(if (status == StreamStatus.LIVE || status == StreamStatus.CONNECTING) alphaPulse else 1f)
                            .background(badgeColor, CircleShape)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = statusLabel,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = if (status == StreamStatus.LIVE) Color.White else MaterialTheme.colorScheme.onSurface
                    )
                }

                if (status == StreamStatus.LIVE) {
                    Box(
                        modifier = Modifier
                            .background(Color(0xFFEF4444), RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = formatStreamTime(durationSec),
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = Color.White
                        )
                    }
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (status == StreamStatus.OFFLINE) Icons.Default.WifiOff else Icons.Default.Wifi,
                            contentDescription = "Connection Status",
                            tint = badgeColor,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (status == StreamStatus.OFFLINE) "Disconnected" else "Active",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Health Metrics Grid
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        if (status == StreamStatus.LIVE) Color(0xFF1E1838) else MaterialTheme.colorScheme.surfaceVariant,
                        RoundedCornerShape(10.dp)
                    )
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Bitrate
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Speed,
                            contentDescription = "Bitrate",
                            tint = Color(0xFF10B981),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Bitrate",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (status == StreamStatus.LIVE) Color.LightGray else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = if (status == StreamStatus.LIVE) "$bitrateKbps kbps" else "--",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = if (status == StreamStatus.LIVE) Color.White else MaterialTheme.colorScheme.onSurface
                    )
                }

                // Latency
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.SignalCellular4Bar,
                            contentDescription = "Latency",
                            tint = Color(0xFF3B82F6),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Latency",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (status == StreamStatus.LIVE) Color.LightGray else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = if (status == StreamStatus.LIVE) "$latencyMs ms" else "--",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = if (status == StreamStatus.LIVE) Color.White else MaterialTheme.colorScheme.onSurface
                    )
                }

                // Health
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Health",
                            tint = badgeColor,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Health",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (status == StreamStatus.LIVE) Color.LightGray else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = when (status) {
                            StreamStatus.LIVE -> "Excellent"
                            StreamStatus.CONNECTING -> "Handshake"
                            StreamStatus.RECONNECTING -> "Retrying"
                            else -> "Idle"
                        },
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = if (status == StreamStatus.LIVE) Color.White else MaterialTheme.colorScheme.onSurface
                    )
                }

                // Loop Round
                if (status == StreamStatus.LIVE) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Loop,
                                contentDescription = "Loop Count",
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Loop",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.LightGray
                            )
                        }
                        Text(
                            text = "#$loopCount",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

private fun formatStreamTime(seconds: Long): String {
    val hrs = seconds / 3600
    val mins = (seconds % 3600) / 60
    val secs = seconds % 60
    return if (hrs > 0) {
        "%02d:%02d:%02d".format(hrs, mins, secs)
    } else {
        "%02d:%02d".format(mins, secs)
    }
}

/**
 * Auto-Stop Stream Timer Configuration Card
 */
@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun AutoStopTimerCard(
    autoStopMinutes: Int,
    streamStatus: StreamStatus,
    liveDurationSec: Long,
    onAutoStopChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var customMinutesInput by remember(autoStopMinutes) {
        mutableStateOf(if (autoStopMinutes > 0 && autoStopMinutes !in listOf(15, 30, 60, 120, 360)) autoStopMinutes.toString() else "")
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("auto_stop_timer_card"),
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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Timer,
                        contentDescription = "Auto-Stop Timer",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "Auto-Stop Stream Timer",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (autoStopMinutes == 0) "Continuous (No auto-stop)" else "Auto-terminates after $autoStopMinutes min",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Preset Chips. FlowRow so all 6 presets (including the longer "Continuous" label)
            // wrap onto a second line on narrow phone screens instead of overflowing.
            androidx.compose.foundation.layout.FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                val presets = listOf(
                    0 to "Continuous",
                    15 to "15m",
                    30 to "30m",
                    60 to "1h",
                    120 to "2h",
                    360 to "6h"
                )

                presets.forEach { (mins, label) ->
                    FilterChip(
                        selected = autoStopMinutes == mins,
                        onClick = { onAutoStopChange(mins) },
                        label = { Text(label, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        modifier = Modifier.testTag("timer_preset_$mins")
                    )
                }
            }

            // Custom minutes input field
            OutlinedTextField(
                value = customMinutesInput,
                onValueChange = { input ->
                    val filtered = input.filter { it.isDigit() }
                    customMinutesInput = filtered
                    val parsed = filtered.toIntOrNull() ?: 0
                    onAutoStopChange(parsed)
                },
                label = { Text("Custom Duration (Minutes)") },
                placeholder = { Text("e.g. 45") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("custom_timer_input")
            )

            // Active Stream Timer Countdown Progress
            if (streamStatus == StreamStatus.LIVE && autoStopMinutes > 0) {
                val targetSec = autoStopMinutes * 60L
                val remainingSec = (targetSec - liveDurationSec).coerceAtLeast(0L)
                val progress = (liveDurationSec.toFloat() / targetSec.toFloat()).coerceIn(0f, 1f)

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
                        Text(
                            text = "Auto-Stop Countdown",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = formatStreamTime(remainingSec) + " remaining",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.ExtraBold),
                            color = MaterialTheme.colorScheme.error
                        )
                    }

                    androidx.compose.material3.LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = MaterialTheme.colorScheme.error,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                }
            }
        }
    }
}
