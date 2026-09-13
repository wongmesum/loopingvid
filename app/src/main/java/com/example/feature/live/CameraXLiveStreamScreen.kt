package com.example.feature.live

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.RadioButtonChecked
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SignalCellular4Bar
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.core.utils.RtmpUrlValidator

/**
 * CameraXLiveStreamScreen integrates CameraX library for live camera feed capture,
 * featuring real-time preview, camera control options (front/back lens switch, torch),
 * and full RTMP live streaming ingest configuration.
 */
@Composable
fun CameraXLiveStreamScreen(
    viewModel: LiveViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        hasCameraPermission = (permissions[Manifest.permission.CAMERA] == true) &&
                              (permissions[Manifest.permission.RECORD_AUDIO] == true)
    }

    LaunchedEffect(Unit) {
        viewModel.initializeStreamModule(context)
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
            .testTag("camerax_live_screen"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Top Title Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "CameraX Live Feed & RTMP Broadcast",
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Seamless video capture with RTMP stream configuration",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Platform indicator badge
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = RoundedCornerShape(20.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Videocam,
                        contentDescription = "CameraX Active",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "CameraX",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }

        if (!hasCameraPermission) {
            // Permission Card Gate
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("camera_permission_card"),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Videocam,
                        contentDescription = "Camera Permission Required",
                        tint = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.size(48.dp)
                    )
                    Text(
                        text = "Camera & Audio Permission Required",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                    Text(
                        text = "CameraX requires camera and microphone permissions to capture live video for RTMP streaming.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                    Button(
                        onClick = {
                            permissionLauncher.launch(
                                arrayOf(
                                    Manifest.permission.CAMERA,
                                    Manifest.permission.RECORD_AUDIO
                                )
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        modifier = Modifier.testTag("grant_camera_permission_button")
                    ) {
                        Text("Grant Camera & Mic Access")
                    }
                }
            }
        } else {
            // Live RTMP camera preview component (RootEncoder OpenGlView)
            CameraXPreviewBox(
                viewModel = viewModel,
                uiState = uiState,
                onMicMuteToggled = { viewModel.toggleMicInputMute() }
            )
        }

        // Diagnostic Telemetry Overlay Component (Latency, Dropped Frames, Bitrates)
        StreamDiagnosticOverlayCard(
            uiState = uiState,
            onToggleExpanded = { viewModel.toggleDiagnosticOverlayExpanded() },
            onCloseOverlay = { viewModel.toggleDiagnosticOverlay() }
        )

        // Quick Expandable Stream Settings Panel
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

        // Buffering & Network Reconnection State Indicator
        BufferingStateIndicator(
            uiState = uiState,
            onRetryNow = { viewModel.handleNetworkLoss("Manual user retry handshake", context) },
            onCancel = { viewModel.cancelReconnection() }
        )

        // Live Stream Operational Metrics (when active)
        if (uiState.streamStatus == StreamStatus.LIVE || uiState.streamStatus == StreamStatus.CONNECTING) {
            LiveStreamMetricsBar(uiState = uiState)
        }

        // D3.js Real-time Data Overlay (Viewer Count & Bandwidth Fluctuations)
        D3DataOverlayCard(
            uiState = uiState,
            onToggleOverlay = { viewModel.toggleD3OverlayOnVideo() },
            onSetOpacity = { viewModel.setD3OverlayOpacity(it) },
            onSetTimeWindow = { viewModel.setD3OverlayTimeWindow(it) },
            onSetVisMode = { viewModel.setD3VisualizationMode(it) }
        )

        // RTMP Stream Configuration Panel
        RtmpStreamConfigCard(
            uiState = uiState,
            viewModel = viewModel
        )

        // Broadcast Action Button
        StreamActionButton(
            uiState = uiState,
            onStartStream = { viewModel.startLiveStream(context) },
            onStopStream = { viewModel.stopLiveStream(context) }
        )
    }
}

/**
 * CameraX Preview Viewport with floating overlay controls (Lens flip, torch, status).
 */
@Composable
private fun CameraXPreviewBox(
    viewModel: LiveViewModel,
    uiState: LiveUiState,
    onMicMuteToggled: () -> Unit
) {
    var isTorchEnabled by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(280.dp)
            .testTag("camerax_preview_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Black)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Real RTMP camera surface (RootEncoder OpenGlView). Displays the live camera feed
            // and is the source that gets encoded and pushed when broadcasting.
            RtmpCameraPreview(
                viewModel = viewModel,
                modifier = Modifier.fillMaxSize()
            )

            // Top Status Badge Overlay
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
                    .align(Alignment.TopStart),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Live status badge with pulsing dot
                Surface(
                    color = when (uiState.streamStatus) {
                        StreamStatus.LIVE -> Color(0xFFDC2626) // Red
                        StreamStatus.CONNECTING -> Color(0xFFD97706) // Amber
                        else -> Color.Black.copy(alpha = 0.65f)
                    },
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (uiState.streamStatus == StreamStatus.LIVE) {
                            val infiniteTransition = rememberInfiniteTransition()
                            val alpha by infiniteTransition.animateFloat(
                                initialValue = 0.3f,
                                targetValue = 1.0f,
                                animationSpec = infiniteRepeatable(
                                    animation = tween(800),
                                    repeatMode = RepeatMode.Reverse
                                )
                            )
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .alpha(alpha)
                                    .background(Color.White, CircleShape)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "LIVE RTMP",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = Color.White
                            )
                        } else {
                            Text(
                                text = "CAM PREVIEW",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = Color.White
                            )
                        }
                    }
                }

                // Resolution & FPS badge
                Surface(
                    color = Color.Black.copy(alpha = 0.65f),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(
                        text = "1080p @ 60 FPS",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            // Floating D3.js Telemetry Overlay over Camera View
            if (uiState.showD3OverlayOnVideo) {
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .padding(start = 12.dp, top = 20.dp)
                        .fillMaxWidth(0.6f)
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

            // Bottom Controls Bar Overlay (Flip Camera, Torch, Mic Toggle)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.5f))
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .align(Alignment.BottomCenter),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { viewModel.switchStreamCamera() },
                    modifier = Modifier.testTag("switch_camera_lens_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Cameraswitch,
                        contentDescription = "Switch Camera Lens",
                        tint = Color.White
                    )
                }

                IconButton(
                    onClick = {
                        isTorchEnabled = !isTorchEnabled
                        viewModel.applyTorchToEngine(isTorchEnabled)
                    },
                    modifier = Modifier.testTag("toggle_torch_button")
                ) {
                    Icon(
                        imageVector = if (isTorchEnabled) Icons.Default.FlashOn else Icons.Default.FlashOff,
                        contentDescription = "Toggle Torch",
                        tint = if (isTorchEnabled) Color(0xFFFBBF24) else Color.White
                    )
                }

                IconButton(
                    onClick = onMicMuteToggled,
                    modifier = Modifier.testTag("toggle_mic_button")
                ) {
                    Icon(
                        imageVector = if (uiState.isMicInputMuted) Icons.Default.MicOff else Icons.Default.Mic,
                        contentDescription = "Toggle Microphone",
                        tint = if (uiState.isMicInputMuted) Color(0xFFEF4444) else Color.White
                    )
                }
            }

            // Video Buffering & Reconnection Full Screen Overlay
            VideoBufferingOverlay(
                isBuffering = uiState.isBuffering,
                streamStatus = uiState.streamStatus,
                reconnectAttempt = uiState.reconnectAttempt,
                maxReconnectAttempts = uiState.maxReconnectAttempts,
                countdownSec = uiState.reconnectCountdownSec,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

/**
 * Metrics display during live stream (Bitrate, Latency, Up-time, Dropped Frames).
 */
@Composable
private fun LiveStreamMetricsBar(uiState: LiveUiState) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("stream_metrics_card"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            MetricColumn(
                label = "UPTIME",
                value = formatUptime(uiState.liveDurationSec),
                valueColor = MaterialTheme.colorScheme.primary
            )
            MetricColumn(
                label = "BITRATE",
                value = "${uiState.currentBitrateKbps} Kbps",
                valueColor = Color(0xFF10B981)
            )
            MetricColumn(
                label = "LATENCY",
                value = "${uiState.latencyMs} ms",
                valueColor = MaterialTheme.colorScheme.onSurface
            )
            MetricColumn(
                label = "DROPPED",
                value = "${uiState.droppedFrames}",
                valueColor = if (uiState.droppedFrames > 10) Color(0xFFEF4444) else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun MetricColumn(label: String, value: String, valueColor: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
            color = valueColor
        )
    }
}

/**
 * RTMP Live Streaming Configuration Input Panel.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RtmpStreamConfigCard(
    uiState: LiveUiState,
    viewModel: LiveViewModel
) {
    var isKeyVisible by remember { mutableStateOf(false) }

    val isValidRtmp = remember(uiState.rtmpUrl) {
        RtmpUrlValidator.validateRtmpUrl(uiState.rtmpUrl).isValid
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("rtmp_config_card"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
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
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Radio,
                        contentDescription = "RTMP Config",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "RTMP Broadcast Configuration",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                }

                if (isValidRtmp) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        color = Color(0xFF10B981).copy(alpha = 0.15f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "Valid RTMP URL",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = Color(0xFF10B981),
                            maxLines = 1,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            // Quick Platform Presets
            Text(
                text = "Target Platform Preset",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(
                    LivePlatform.YOUTUBE to "YouTube Live",
                    LivePlatform.TIKTOK to "TikTok Live",
                    LivePlatform.CUSTOM_RTMP to "Custom RTMP"
                ).forEach { (platform, label) ->
                    val isSelected = uiState.platform == platform
                    FilterChip(
                        selected = isSelected,
                        onClick = { viewModel.setPlatform(platform) },
                        label = { Text(label) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        modifier = Modifier.testTag("preset_chip_${platform.name}")
                    )
                }
            }

            // Stream Title Field
            OutlinedTextField(
                value = uiState.streamTitle,
                onValueChange = { viewModel.updateStreamTitle(it) },
                label = { Text("Stream Title") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("stream_title_input")
            )

            // RTMP Ingest Server URL
            OutlinedTextField(
                value = uiState.rtmpUrl,
                onValueChange = { viewModel.updateRtmpUrl(it) },
                label = { Text("RTMP Server Ingest URL") },
                placeholder = { Text("rtmp://a.rtmp.youtube.com/live2") },
                singleLine = true,
                trailingIcon = {
                    if (isValidRtmp) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Valid RTMP",
                            tint = Color(0xFF10B981)
                        )
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("rtmp_url_input")
            )

            // Stream Key
            OutlinedTextField(
                value = uiState.streamKey,
                onValueChange = { viewModel.updateStreamKey(it) },
                label = { Text("Stream Key") },
                placeholder = { Text("xxxx-xxxx-xxxx-xxxx") },
                singleLine = true,
                visualTransformation = if (isKeyVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { isKeyVisible = !isKeyVisible }) {
                        Icon(
                            imageVector = if (isKeyVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = if (isKeyVisible) "Hide Key" else "Show Key"
                        )
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("stream_key_input")
            )

            // Real viewer-count configuration. Only YouTube exposes a public concurrent-viewer
            // API (via the YouTube Data API v3). For TikTok / custom RTMP the count stays hidden.
            if (uiState.platform == LivePlatform.YOUTUBE) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Real Viewer Count (YouTube Data API)",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Optional. Enter a YouTube Data API v3 key and the live video ID to show real concurrent viewers.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = uiState.youtubeApiKey,
                    onValueChange = { viewModel.updateYoutubeApiKey(it) },
                    label = { Text("YouTube Data API Key") },
                    placeholder = { Text("AIza...") },
                    singleLine = true,
                    visualTransformation = if (isKeyVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("youtube_api_key_input")
                )

                OutlinedTextField(
                    value = uiState.youtubeVideoId,
                    onValueChange = { viewModel.updateYoutubeVideoId(it) },
                    label = { Text("Live Video ID") },
                    placeholder = { Text("e.g. dQw4w9WgXcQ") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("youtube_video_id_input")
                )

                if (uiState.streamStatus == StreamStatus.LIVE) {
                    val viewerStatus = if (uiState.isViewerCountLive) {
                        "Live viewers: ${uiState.viewerCount} (peak ${uiState.peakViewerCount})"
                    } else {
                        "Viewer count unavailable (check API key / video ID)"
                    }
                    Text(
                        text = viewerStatus,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (uiState.isViewerCountLive) Color(0xFF10B981) else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

/**
 * Big "START LIVE STREAM" / "STOP STREAM" action button.
 */
@Composable
private fun StreamActionButton(
    uiState: LiveUiState,
    onStartStream: () -> Unit,
    onStopStream: () -> Unit
) {
    val isStreaming = uiState.streamStatus == StreamStatus.LIVE

    if (isStreaming) {
        Button(
            onClick = onStopStream,
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .testTag("stop_live_stream_button"),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
            shape = RoundedCornerShape(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Stop,
                contentDescription = "Stop Stream",
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "END LIVE BROADCAST",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
        }
    } else {
        Button(
            onClick = onStartStream,
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .testTag("start_live_stream_button"),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
            shape = RoundedCornerShape(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.RadioButtonChecked,
                contentDescription = "Go Live",
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "GO LIVE WITH CAMERAX (RTMP)",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = Color.White
            )
        }
    }
}

private fun formatUptime(seconds: Long): String {
    val mins = seconds / 60
    val secs = seconds % 60
    return "%02d:%02d".format(mins, secs)
}
