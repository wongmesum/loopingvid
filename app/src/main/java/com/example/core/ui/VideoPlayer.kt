package com.example.core.ui

import android.net.Uri
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PlayCircleOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.compose.ui.graphics.graphicsLayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

/**
 * Reusable Media3-backed Video Player component for Compose.
 * Utilized across Loop, Editor, Mastering, and Live modules.
 */
@OptIn(UnstableApi::class)
@Composable
fun VideoPlayer(
    mediaUri: String?,
    modifier: Modifier = Modifier,
    isPlaying: Boolean = true,
    isLooping: Boolean = true,
    isMuted: Boolean = false,
    useController: Boolean = true,
    playbackSpeed: Float = 1.0f,
    startMs: Long = 0L,
    endMs: Long = 0L,
    colorMatrix: androidx.compose.ui.graphics.ColorMatrix? = null,
    resizeMode: Int = AspectRatioFrameLayout.RESIZE_MODE_FIT,
    onPlaybackStateChanged: ((isPlaying: Boolean) -> Unit)? = null,
    onProgressUpdate: ((positionMs: Long, durationMs: Long) -> Unit)? = null,
    onError: ((String) -> Unit)? = null,
    testTag: String = "video_player_component"
) {
    val context = LocalContext.current
    var errorMessage by remember { mutableStateOf<String?>(null) }

    if (mediaUri.isNull_Or_Empty()) {
        VideoPlayerPlaceholder(
            modifier = modifier.testTag(testTag),
            message = "No Video Selected"
        )
        return
    }

    val exoPlayer = remember(context) {
        val renderersFactory = androidx.media3.exoplayer.DefaultRenderersFactory(context)
            .setExtensionRendererMode(androidx.media3.exoplayer.DefaultRenderersFactory.EXTENSION_RENDERER_MODE_PREFER)
            .setEnableDecoderFallback(true)
            
        ExoPlayer.Builder(context, renderersFactory).build().apply {
            repeatMode = if (isLooping) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF
            playWhenReady = isPlaying
            volume = if (isMuted) 0f else 1f
        }
    }

    // Handle Media Source updates & segment clipping
    LaunchedEffect(mediaUri, startMs, endMs) {
        try {
            errorMessage = null
            val uri = Uri.parse(mediaUri)
            val clipBuilder = MediaItem.ClippingConfiguration.Builder()
            if (endMs > startMs && endMs > 0L) {
                clipBuilder.setStartPositionMs(startMs)
                clipBuilder.setEndPositionMs(endMs)
                clipBuilder.setStartsAtKeyFrame(true)
            } else if (startMs > 0L) {
                clipBuilder.setStartPositionMs(startMs)
            }
            val mediaItem = MediaItem.Builder()
                .setUri(uri)
                .setClippingConfiguration(clipBuilder.build())
                .build()
            exoPlayer.setMediaItem(mediaItem)
            exoPlayer.prepare()
        } catch (e: Exception) {
            val message = e.localizedMessage ?: "Failed to load media"
            errorMessage = message
            onError?.invoke(message)
        }
    }

    // Sync Playback state
    LaunchedEffect(isPlaying) {
        exoPlayer.playWhenReady = isPlaying
    }

    // Sync Looping
    LaunchedEffect(isLooping) {
        exoPlayer.repeatMode = if (isLooping) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF
    }

    // Sync Mute
    LaunchedEffect(isMuted) {
        exoPlayer.volume = if (isMuted) 0f else 1f
    }

    // Sync Playback Speed
    LaunchedEffect(playbackSpeed) {
        exoPlayer.playbackParameters = androidx.media3.common.PlaybackParameters(playbackSpeed.coerceIn(0.25f, 4.0f))
    }

    // Progress updates
    LaunchedEffect(isPlaying, exoPlayer) {
        while (isActive) {
            if (exoPlayer.isPlaying) {
                val current = exoPlayer.currentPosition.coerceAtLeast(0L)
                val duration = exoPlayer.duration.coerceAtLeast(0L)
                onProgressUpdate?.invoke(current, duration)
            }
            delay(250)
        }
    }

    // Player Event Listener
    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                onPlaybackStateChanged?.invoke(playing)
            }

            override fun onPlayerError(error: PlaybackException) {
                val message = error.localizedMessage ?: "Playback error"
                errorMessage = message
                onError?.invoke(message)
            }
        }
        exoPlayer.addListener(listener)

        onDispose {
            exoPlayer.removeListener(listener)
            exoPlayer.release()
        }
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color.Black)
            .testTag(testTag),
        contentAlignment = Alignment.Center
    ) {
        if (errorMessage != null) {
            VideoPlayerPlaceholder(
                modifier = Modifier.fillMaxSize(),
                message = errorMessage ?: "Playback Error"
            )
        } else {
            Box(modifier = Modifier.fillMaxSize()) {
                AndroidView(
                    factory = { ctx ->
                        val view = android.view.LayoutInflater.from(ctx).inflate(com.example.R.layout.texture_player_view, null) as PlayerView
                        view.apply {
                            player = exoPlayer
                            this.useController = useController
                            this.resizeMode = resizeMode
                            setShowBuffering(PlayerView.SHOW_BUFFERING_WHEN_PLAYING)
                            layoutParams = FrameLayout.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                            )
                        }
                    },
                    update = { playerView ->
                        playerView.useController = useController
                        playerView.resizeMode = resizeMode
                    },
                    modifier = Modifier.fillMaxSize()
                )

                if (colorMatrix != null) {
                    androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                        drawRect(
                            color = androidx.compose.ui.graphics.Color.White,
                            colorFilter = androidx.compose.ui.graphics.ColorFilter.colorMatrix(colorMatrix),
                            blendMode = androidx.compose.ui.graphics.BlendMode.Color
                        )
                    }
                }
            }
        }
    }
}

/**
 * Overload for VideoPlayer receiving Uri object directly.
 */
@Composable
fun VideoPlayer(
    mediaUri: Uri?,
    modifier: Modifier = Modifier,
    isPlaying: Boolean = true,
    isLooping: Boolean = true,
    isMuted: Boolean = false,
    useController: Boolean = true,
    startMs: Long = 0L,
    endMs: Long = 0L,
    colorMatrix: androidx.compose.ui.graphics.ColorMatrix? = null,
    resizeMode: Int = AspectRatioFrameLayout.RESIZE_MODE_FIT,
    onPlaybackStateChanged: ((isPlaying: Boolean) -> Unit)? = null,
    onProgressUpdate: ((positionMs: Long, durationMs: Long) -> Unit)? = null,
    onError: ((String) -> Unit)? = null,
    testTag: String = "video_player_component"
) {
    VideoPlayer(
        mediaUri = mediaUri?.toString(),
        modifier = modifier,
        isPlaying = isPlaying,
        isLooping = isLooping,
        isMuted = isMuted,
        useController = useController,
        startMs = startMs,
        endMs = endMs,
        colorMatrix = colorMatrix,
        resizeMode = resizeMode,
        onPlaybackStateChanged = onPlaybackStateChanged,
        onProgressUpdate = onProgressUpdate,
        onError = onError,
        testTag = testTag
    )
}

/**
 * Overload for VideoPlayer receiving an externally managed ExoPlayer instance.
 */
@OptIn(UnstableApi::class)
@Composable
fun VideoPlayer(
    player: ExoPlayer?,
    modifier: Modifier = Modifier,
    useController: Boolean = true,
    resizeMode: Int = AspectRatioFrameLayout.RESIZE_MODE_FIT,
    testTag: String = "video_player_external"
) {
    if (player == null) {
        VideoPlayerPlaceholder(
            modifier = modifier.testTag(testTag),
            message = "Player Initializing..."
        )
        return
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color.Black)
            .testTag(testTag),
        contentAlignment = Alignment.Center
    ) {
        AndroidView(
            factory = { ctx ->
                val view = android.view.LayoutInflater.from(ctx).inflate(com.example.R.layout.texture_player_view, null) as PlayerView
                view.apply {
                    this.player = player
                    this.useController = useController
                    this.resizeMode = resizeMode
                    setShowBuffering(PlayerView.SHOW_BUFFERING_WHEN_PLAYING)
                    layoutParams = FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                }
            },
            update = { playerView ->
                playerView.player = player
                playerView.useController = useController
                playerView.resizeMode = resizeMode
            },
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Composable
fun VideoPlayerPlaceholder(
    modifier: Modifier = Modifier,
    message: String
) {
    Box(
        modifier = modifier
            .background(Color(0xFF130F26), RoundedCornerShape(12.dp))
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Movie,
                contentDescription = "Video Placeholder",
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                color = Color.White.copy(alpha = 0.8f)
            )
        }
    }
}

private fun String?.isNull_Or_Empty(): Boolean {
    return this == null || this.trim().isEmpty()
}
