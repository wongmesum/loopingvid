package com.example.feature.live

import android.view.SurfaceHolder
import android.view.ViewGroup
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.pedro.library.view.OpenGlView

/**
 * Compose wrapper hosting RootEncoder's [OpenGlView] and binding it to the [LiveViewModel]'s
 * [RtmpStreamManager]. This is the real camera surface used for RTMP broadcasting.
 *
 * The camera preview starts once the surface is created and stops when the composable leaves
 * the composition (e.g. navigating away), unless a broadcast is active.
 */
@Composable
fun RtmpCameraPreview(
    viewModel: LiveViewModel,
    modifier: Modifier = Modifier
) {
    val manager = viewModel.getRtmpManager()

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            OpenGlView(ctx).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                manager.bind(this)
                holder.addCallback(object : SurfaceHolder.Callback {
                    override fun surfaceCreated(holder: SurfaceHolder) {
                        manager.startPreview()
                        viewModel.onStreamViewBound()
                    }

                    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
                        // No-op: RootEncoder handles surface size internally.
                    }

                    override fun surfaceDestroyed(holder: SurfaceHolder) {
                        viewModel.onStreamViewUnbound()
                        // Keep the stream running if broadcasting; only stop the local preview.
                        if (!manager.isStreaming) {
                            manager.stopPreview()
                        }
                    }
                })
            }
        }
    )

    DisposableEffect(Unit) {
        onDispose {
            viewModel.onStreamViewUnbound()
            if (!manager.isStreaming) {
                manager.stopPreview()
            }
        }
    }
}
