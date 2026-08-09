package com.example.core.ui

import android.net.Uri
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import java.util.Locale
import com.example.core.audio.AudioAnalysisRepository
import com.example.core.media.AudioAnalysisData
import com.example.core.media.toAudioAnalysisData
import androidx.compose.material3.Switch
import timber.log.Timber

/**
 * Modern Media3-backed UI Component for precise video segment trimming.
 * Allows setting frame-exact start (In-point) and end (Out-point) timestamps
 * with millisecond nudge controls, interactive player preview, and segment range sliders.
 */
@OptIn(UnstableApi::class)
@Composable
fun Media3SegmentTrimmer(
    mediaUri: String?,
    modifier: Modifier = Modifier,
    initialStartMs: Long = 0L,
    initialEndMs: Long = 0L,
    onTrimChange: ((startMs: Long, endMs: Long) -> Unit)? = null,
    onApplyTrimmedSegment: ((startMs: Long, endMs: Long) -> Unit)? = null,
    audioAnalysisRepository: AudioAnalysisRepository? = null
) {
    val context = LocalContext.current

    var startMs by remember(initialStartMs) { mutableLongStateOf(initialStartMs) }
    var endMs by remember(initialEndMs) { mutableLongStateOf(initialEndMs) }
    var totalDurationMs by remember { mutableLongStateOf(0L) }
    var currentPositionMs by remember { mutableLongStateOf(0L) }
    var isPlaying by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Touch Gesture Controls (Pinch-to-zoom frame selection & Drag scrubbing)
    var zoomScale by remember { mutableFloatStateOf(1.0f) }
    var panOffset by remember { mutableStateOf(Offset.Zero) }
    var isScrubbingActive by remember { mutableStateOf(false) }
    var scrubTimeDisplayMs by remember { mutableLongStateOf(0L) }

    var audioAnalysisData by remember { mutableStateOf<AudioAnalysisData?>(null) }
    var snapToBeat by remember { mutableStateOf(false) }

    LaunchedEffect(mediaUri, audioAnalysisRepository) {
        audioAnalysisData = null
        if (mediaUri.isNullOrEmpty() || audioAnalysisRepository == null) return@LaunchedEffect

        val uri = try {
            Uri.parse(mediaUri)
        } catch (error: Exception) {
            Timber.e(error, "Invalid media URI for audio analysis")
            return@LaunchedEffect
        }

        audioAnalysisRepository.getOrAnalyze(uri)
            .onSuccess { result ->
                audioAnalysisData = result.toAudioAnalysisData()
            }
            .onFailure { error ->
                Timber.e(error, "Audio analysis failed for trimmer media")
            }
    }

    fun snapToNearestBeat(timeMs: Long): Long {
        if (!snapToBeat || audioAnalysisData == null) return timeMs
        val beats = audioAnalysisData!!.beatMarkersMs
        if (beats.isEmpty()) return timeMs
        var nearest = beats[0]
        var minDiff = kotlin.math.abs(beats[0] - timeMs)
        for (beat in beats) {
            val diff = kotlin.math.abs(beat - timeMs)
            if (diff < minDiff) {
                minDiff = diff
                nearest = beat
            }
        }
        return nearest
    }

    // Media3 ExoPlayer Instance
    val exoPlayer = remember(context) {
        val renderersFactory = androidx.media3.exoplayer.DefaultRenderersFactory(context)
            .setExtensionRendererMode(androidx.media3.exoplayer.DefaultRenderersFactory.EXTENSION_RENDERER_MODE_PREFER)
            .setEnableDecoderFallback(true)

        ExoPlayer.Builder(context, renderersFactory).build().apply {
            repeatMode = Player.REPEAT_MODE_OFF
            playWhenReady = true
        }
    }

    // Load media into ExoPlayer
    LaunchedEffect(mediaUri) {
        if (mediaUri.isNullOrEmpty()) return@LaunchedEffect
        try {
            errorMessage = null
            val item = MediaItem.fromUri(Uri.parse(mediaUri))
            exoPlayer.setMediaItem(item)
            exoPlayer.prepare()
            exoPlayer.playWhenReady = isPlaying
        } catch (e: Exception) {
            errorMessage = e.localizedMessage ?: "Error loading media in Media3 Trimmer"
        }
    }
    
    // Monitor position and duration, lock playback between startMs and endMs
    LaunchedEffect(exoPlayer, startMs, endMs, isPlaying) {
        while (isActive) {
            if (exoPlayer.isPlaying) {
                val pos = exoPlayer.currentPosition.coerceAtLeast(0L)
                currentPositionMs = pos

                val dur = exoPlayer.duration
                if (dur > 0L && totalDurationMs != dur) {
                    totalDurationMs = dur
                    if (endMs == 0L || endMs > dur) {
                        endMs = dur
                    }
                }
                
                // If playback exceeds Out-point (endMs), loop back to In-point (startMs)
                if (endMs > startMs && pos >= endMs) {
                    exoPlayer.seekTo(startMs)
                }
            }
            delay(100)
        }
    }

    // Control play state
    LaunchedEffect(isPlaying) {
        exoPlayer.playWhenReady = isPlaying
    }

    // Clean up player
    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onPlayerError(error: PlaybackException) {
                errorMessage = error.localizedMessage ?: "Playback error in Media3 Trimmer"
            }
        }
        exoPlayer.addListener(listener)
        onDispose {
            exoPlayer.removeListener(listener)
            exoPlayer.release()
        }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("media3_segment_trimmer_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCut,
                            contentDescription = "Media3 Trimmer",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Media3 Precision Segment Trimmer",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Frame-accurate start & end point clipping with Media3",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    val selectedDurMs = (endMs - startMs).coerceAtLeast(0L)
                    Text(
                        text = "Duration: ${formatTimecode(selectedDurMs)}",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }

            // Media3 ExoPlayer Screen with Pinch-to-Zoom & Timeline Scrubbing Gestures
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(240.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.Black)
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onDoubleTap = {
                                zoomScale = if (zoomScale > 1.2f) 1.0f else 2.2f
                                panOffset = Offset.Zero
                            }
                        )
                    }
                    .pointerInput(Unit) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            val newZoom = (zoomScale * zoom).coerceIn(1.0f, 4.0f)
                            zoomScale = newZoom
                            if (newZoom > 1.0f) {
                                panOffset = Offset(
                                    x = (panOffset.x + pan.x).coerceIn(-400f * (newZoom - 1f), 400f * (newZoom - 1f)),
                                    y = (panOffset.y + pan.y).coerceIn(-250f * (newZoom - 1f), 250f * (newZoom - 1f))
                                )
                            } else {
                                panOffset = Offset.Zero
                            }
                        }
                    }
                    .pointerInput(totalDurationMs) {
                        detectHorizontalDragGestures(
                            onDragStart = {
                                isScrubbingActive = true
                            },
                            onHorizontalDrag = { _, dragAmount ->
                                if (totalDurationMs > 0L) {
                                    val deltaMs = (dragAmount * 25f).toLong()
                                    val newPos = (currentPositionMs + deltaMs).coerceIn(0L, totalDurationMs)
                                    currentPositionMs = newPos
                                    scrubTimeDisplayMs = newPos
                                    exoPlayer.seekTo(newPos)
                                }
                            },
                            onDragEnd = { isScrubbingActive = false },
                            onDragCancel = { isScrubbingActive = false }
                        )
                    }
                    .testTag("media3_trimmer_viewport"),
                contentAlignment = Alignment.Center
            ) {
                if (mediaUri.isNullOrEmpty()) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Movie,
                            contentDescription = null,
                            tint = Color.Gray,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Select a video to begin Media3 segment trimming",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.Gray
                        )
                    }
                } else if (errorMessage != null) {
                    Text(
                        text = errorMessage ?: "Media3 error",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(16.dp)
                    )
                } else {
                    Box(modifier = Modifier.fillMaxSize()) {
                        AndroidView(
                            factory = { ctx ->
                                PlayerView(ctx).apply {
                                    player = exoPlayer
                                    useController = false
                                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                                    setShowBuffering(PlayerView.SHOW_BUFFERING_WHEN_PLAYING)
                                    layoutParams = FrameLayout.LayoutParams(
                                        ViewGroup.LayoutParams.MATCH_PARENT,
                                        ViewGroup.LayoutParams.MATCH_PARENT
                                    )
                                }
                            },
                            update = { playerView ->
                                playerView.player = exoPlayer
                            },
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer(
                                    scaleX = zoomScale,
                                    scaleY = zoomScale,
                                    translationX = panOffset.x,
                                    translationY = panOffset.y
                                )
                        )

                        // Top Overlay Badges (In / Out Points Indicator)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                color = Color(0xFF16A34A).copy(alpha = 0.85f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(imageVector = Icons.Default.Flag, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "In [A]: ${formatTimecode(startMs)}",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.sp),
                                        color = Color.White
                                    )
                                }
                            }

                            // Active Zoom Scale Badge
                            if (zoomScale > 1.05f) {
                                Surface(
                                    color = MaterialTheme.colorScheme.primary,
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.clickable {
                                        zoomScale = 1.0f
                                        panOffset = Offset.Zero
                                    }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(imageVector = Icons.Default.ZoomIn, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "${String.format(Locale.US, "%.1fx", zoomScale)} Zoom (Reset)",
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.sp),
                                            color = Color.White
                                        )
                                    }
                                }
                            }

                            Surface(
                                color = Color(0xFFDC2626).copy(alpha = 0.85f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(imageVector = Icons.Default.Flag, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Out [B]: ${formatTimecode(if (endMs > 0) endMs else totalDurationMs)}",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.sp),
                                        color = Color.White
                                    )
                                }
                            }
                        }

                        // Real-time Drag Scrubbing HUD Overlay
                        if (isScrubbingActive) {
                            Box(
                                modifier = Modifier.align(Alignment.Center)
                            ) {
                                Surface(
                                    color = Color.Black.copy(alpha = 0.85f),
                                    shape = RoundedCornerShape(12.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary),
                                    shadowElevation = 8.dp
                                ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.TouchApp,
                                        contentDescription = "Scrubbing",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Column {
                                        Text(
                                            text = "Scrubbing Frame Timeline",
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Text(
                                            text = formatTimecode(scrubTimeDisplayMs),
                                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                            color = Color.White
                                        )
                                    }
                                }
                            }
                            }
                        }

                        // Bottom Floating Transport Control Bar
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .align(Alignment.BottomCenter)
                                .background(Color.Black.copy(alpha = 0.65f))
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(
                                        onClick = { isPlaying = !isPlaying },
                                        modifier = Modifier.testTag("media3_trimmer_toggle_play")
                                    ) {
                                        Icon(
                                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                            contentDescription = if (isPlaying) "Pause" else "Play",
                                            tint = Color.White
                                        )
                                    }

                                    IconButton(
                                        onClick = {
                                            exoPlayer.seekTo(startMs)
                                            currentPositionMs = startMs
                                        },
                                        modifier = Modifier.testTag("media3_trimmer_seek_in_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Replay,
                                            contentDescription = "Seek to In-Point",
                                            tint = Color.White
                                        )
                                    }

                                    Text(
                                        text = "${formatTimecode(currentPositionMs)} / ${formatTimecode(totalDurationMs)}",
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                        color = Color.White
                                    )
                                }

                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Button(
                                        onClick = {
                                            startMs = currentPositionMs
                                            if (endMs <= startMs) endMs = totalDurationMs
                                            onTrimChange?.invoke(startMs, endMs)
                                        },
                                        contentPadding = ButtonDefaults.ContentPadding,
                                        shape = RoundedCornerShape(8.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A)),
                                        modifier = Modifier.testTag("mark_in_button")
                                    ) {
                                        Text("Mark In [A]", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.sp))
                                    }

                                    Button(
                                        onClick = {
                                            endMs = currentPositionMs
                                            if (startMs >= endMs) startMs = 0L
                                            onTrimChange?.invoke(startMs, endMs)
                                        },
                                        contentPadding = ButtonDefaults.ContentPadding,
                                        shape = RoundedCornerShape(8.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                                        modifier = Modifier.testTag("mark_out_button")
                                    ) {
                                        Text("Mark Out [B]", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.sp))
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Interactive Timeline Range Slider
            if (totalDurationMs > 0L) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    val sliderStart = startMs.toFloat().coerceIn(0f, totalDurationMs.toFloat())
                    val sliderEnd = (if (endMs > 0) endMs else totalDurationMs).toFloat().coerceIn(sliderStart, totalDurationMs.toFloat())

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "Timeline", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("Snap to Beats", style = MaterialTheme.typography.labelSmall)
                            Switch(checked = snapToBeat, onCheckedChange = { snapToBeat = it }, modifier = Modifier.size(width = 36.dp, height = 20.dp))
                        }
                    }

                    Box(modifier = Modifier.fillMaxWidth().height(60.dp)) {
                        // Waveform Visualizer
                        audioAnalysisData?.let { analysis ->
                            Canvas(modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp)) {
                                val width = size.width
                                val height = size.height
                                val points = analysis.waveformPoints

                                val primaryColor = Color.Cyan.copy(alpha = 0.5f)

                                if (points.isNotEmpty()) {
                                    val barWidth = width / points.size
                                    points.forEachIndexed { index, value ->
                                        val x = index * barWidth
                                        val yOffset = (value * height / 2f)
                                        drawLine(
                                            color = primaryColor,
                                            start = Offset(x, height / 2f - yOffset),
                                            end = Offset(x, height / 2f + yOffset),
                                            strokeWidth = barWidth * 0.8f
                                        )
                                    }
                                }

                                // Draw beat markers
                                val beatColor = Color.White.copy(alpha = 0.6f)
                                if (analysis.durationMs > 0L) {
                                    analysis.beatMarkersMs.forEach { beatMs ->
                                        val x = (beatMs.toFloat() / analysis.durationMs.toFloat()) * width
                                        drawLine(
                                            color = beatColor,
                                            start = Offset(x, 0f),
                                            end = Offset(x, height),
                                            strokeWidth = 2f
                                        )
                                    }
                                }
                            }
                        }

                        RangeSlider(
                            value = sliderStart..sliderEnd,
                            onValueChange = { range ->
                                startMs = snapToNearestBeat(range.start.toLong())
                                endMs = snapToNearestBeat(range.endInclusive.toLong())
                                exoPlayer.seekTo(startMs)
                                onTrimChange?.invoke(startMs, endMs)
                            },
                            valueRange = 0f..totalDurationMs.toFloat(),
                            colors = SliderDefaults.colors(
                                thumbColor = MaterialTheme.colorScheme.primary,
                                activeTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                                inactiveTrackColor = Color.Transparent
                            ),
                            modifier = Modifier
                                .fillMaxSize()
                                .testTag("media3_timeline_range_slider")
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "00:00.0", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(text = "Playhead: ${formatTimecode(currentPositionMs)}", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.primary)
                        Text(text = formatTimecode(totalDurationMs), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            // Touch Gestures & Precision Frame Zoom Bar
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.TouchApp,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Touch Controls: Drag to scrub • Pinch to zoom",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        if (zoomScale > 1.05f) {
                            OutlinedButton(
                                onClick = {
                                    zoomScale = 1.0f
                                    panOffset = Offset.Zero
                                },
                                contentPadding = ButtonDefaults.ContentPadding,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.testTag("reset_zoom_button")
                            ) {
                                Icon(imageVector = Icons.Default.RestartAlt, contentDescription = "Reset", modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Reset Zoom", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }

                    // Precision Zoom Presets Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Frame Zoom:",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        listOf(1.0f, 1.5f, 2.0f, 3.0f, 4.0f).forEach { scalePreset ->
                            val isSelected = kotlin.math.abs(zoomScale - scalePreset) < 0.1f
                            Surface(
                                selected = isSelected,
                                onClick = {
                                    zoomScale = scalePreset
                                    if (scalePreset == 1.0f) panOffset = Offset.Zero
                                },
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                                ),
                                modifier = Modifier.testTag("zoom_preset_${scalePreset}x")
                            ) {
                                Text(
                                    text = "${scalePreset}x",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    ),
                                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }
            }

            Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            // Precision Millisecond Nudge Controls for In (A) and Out (B)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // In-Point Fine Tuning Box
                NudgeControlBox(
                    title = "In-Point (Start)",
                    timestampMs = startMs,
                    accentColor = Color(0xFF16A34A),
                    modifier = Modifier.weight(1f),
                    onNudge = { deltaMs ->
                        val newStart = (startMs + deltaMs).coerceIn(0L, maxOf(0L, endMs - 100L))
                        startMs = newStart
                        exoPlayer.seekTo(startMs)
                        onTrimChange?.invoke(startMs, endMs)
                    },
                    testTagPrefix = "in_point"
                )

                // Out-Point Fine Tuning Box
                NudgeControlBox(
                    title = "Out-Point (End)",
                    timestampMs = if (endMs > 0) endMs else totalDurationMs,
                    accentColor = Color(0xFFDC2626),
                    modifier = Modifier.weight(1f),
                    onNudge = { deltaMs ->
                        val maxLimit = if (totalDurationMs > 0) totalDurationMs else 600000L
                        val newEnd = (endMs + deltaMs).coerceIn(startMs + 100L, maxLimit)
                        endMs = newEnd
                        onTrimChange?.invoke(startMs, endMs)
                    },
                    testTagPrefix = "out_point"
                )
            }

            // Quick Preset Trimming Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        startMs = 0L
                        endMs = totalDurationMs
                        onTrimChange?.invoke(startMs, endMs)
                    },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f).testTag("preset_full_clip")
                ) {
                    Text("Full Video", style = MaterialTheme.typography.labelSmall)
                }

                OutlinedButton(
                    onClick = {
                        if (totalDurationMs > 0) {
                            startMs = 0L
                            endMs = (10000L).coerceAtMost(totalDurationMs)
                            onTrimChange?.invoke(startMs, endMs)
                        }
                    },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f).testTag("preset_first_10s")
                ) {
                    Text("First 10s", style = MaterialTheme.typography.labelSmall)
                }

                OutlinedButton(
                    onClick = {
                        if (totalDurationMs > 0) {
                            startMs = (totalDurationMs * 0.25f).toLong()
                            endMs = (totalDurationMs * 0.75f).toLong()
                            onTrimChange?.invoke(startMs, endMs)
                        }
                    },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f).testTag("preset_middle_50")
                ) {
                    Text("Mid 50%", style = MaterialTheme.typography.labelSmall)
                }
            }

            // Apply Trim Action Button
            Button(
                onClick = {
                    val finalEnd = if (endMs > 0) endMs else totalDurationMs
                    onApplyTrimmedSegment?.invoke(startMs, finalEnd)
                },
                enabled = !mediaUri.isNullOrEmpty() && (endMs > startMs),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("apply_media3_trim_button")
            ) {
                Icon(imageVector = Icons.Default.Check, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Confirm & Apply Media3 Trim Segment",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                )
            }
        }
    }
}

@Composable
private fun NudgeControlBox(
    title: String,
    timestampMs: Long,
    accentColor: Color,
    modifier: Modifier = Modifier,
    onNudge: (deltaMs: Long) -> Unit,
    testTagPrefix: String
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Surface(shape = CircleShape, color = accentColor, modifier = Modifier.size(8.dp)) {}
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Surface(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = formatTimecode(timestampMs),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = accentColor,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }

            // Nudge Step Buttons
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                NudgeButton(label = "-1s", onClick = { onNudge(-1000L) }, testTag = "${testTagPrefix}_minus_1s")
                NudgeButton(label = "-100ms", onClick = { onNudge(-100L) }, testTag = "${testTagPrefix}_minus_100ms")
                NudgeButton(label = "+100ms", onClick = { onNudge(100L) }, testTag = "${testTagPrefix}_plus_100ms")
                NudgeButton(label = "+1s", onClick = { onNudge(1000L) }, testTag = "${testTagPrefix}_plus_1s")
            }
        }
    }
}

@Composable
private fun NudgeButton(
    label: String,
    onClick: () -> Unit,
    testTag: String
) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .clickable { onClick() }
            .testTag(testTag)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
        )
    }
}

private fun formatTimecode(ms: Long): String {
    val totalSeconds = (ms / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    val millis = (ms % 1000) / 100
    return String.format(Locale.US, "%02d:%02d.%d", minutes, seconds, millis)
}
