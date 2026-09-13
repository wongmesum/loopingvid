package com.example.feature.live

import android.util.Log
import com.pedro.common.ConnectChecker
import com.pedro.library.rtmp.RtmpCamera2
import com.pedro.library.view.OpenGlView

/**
 * Real RTMP broadcast engine built on RootEncoder's [RtmpCamera2].
 *
 * This manager owns the [RtmpCamera2] instance bound to an [OpenGlView] surface, forwards
 * real connection/bitrate callbacks to a [Listener], and exposes simple control methods
 * (start/stop/switch camera/torch/mute/bitrate) used by the live streaming UI and ViewModel.
 *
 * All engine calls are wrapped in try/catch so a device/codec/native failure degrades to a
 * reported error instead of crashing the app.
 */
class RtmpStreamManager : ConnectChecker {

    companion object {
        private const val TAG = "RtmpStreamManager"

        // Default encoding parameters. These are conservative, widely-supported values.
        private const val DEFAULT_AUDIO_BITRATE = 128 * 1024 // 128 Kbps
        private const val DEFAULT_SAMPLE_RATE = 44100
        private const val DEFAULT_STEREO = true
    }

    /**
     * Callbacks describing the real state of the RTMP connection. All methods are invoked on
     * RootEncoder's internal threads; implementers must marshal to the main thread if needed.
     */
    interface Listener {
        fun onConnectionStarted(url: String)
        fun onConnectionSuccess()
        fun onConnectionFailed(reason: String)
        fun onDisconnect()
        fun onAuthError()
        fun onAuthSuccess()
        /** Real upload bitrate in bits per second, reported roughly once per second. */
        fun onNewBitrate(bitrateBps: Long)
    }

    private var rtmpCamera: RtmpCamera2? = null
    private var listener: Listener? = null

    val isStreaming: Boolean
        get() = try { rtmpCamera?.isStreaming == true } catch (t: Throwable) { false }

    val isOnPreview: Boolean
        get() = try { rtmpCamera?.isOnPreview == true } catch (t: Throwable) { false }

    fun setListener(listener: Listener?) {
        this.listener = listener
    }

    /**
     * Binds the engine to the given [OpenGlView]. Must be called before [startPreview]/[startStream].
     * Safe to call again with a new view (re-creates the engine).
     */
    fun bind(openGlView: OpenGlView) {
        try {
            if (rtmpCamera == null) {
                rtmpCamera = RtmpCamera2(openGlView, this)
            }
        } catch (t: Throwable) {
            Log.e(TAG, "Failed to create RtmpCamera2", t)
        }
    }

    fun startPreview() {
        val camera = rtmpCamera ?: return
        try {
            if (!camera.isOnPreview && !camera.isStreaming) {
                camera.startPreview()
            }
        } catch (t: Throwable) {
            Log.e(TAG, "startPreview failed", t)
        }
    }

    fun stopPreview() {
        val camera = rtmpCamera ?: return
        try {
            if (camera.isOnPreview) camera.stopPreview()
        } catch (t: Throwable) {
            Log.e(TAG, "stopPreview failed", t)
        }
    }

    /**
     * Prepares the encoders and starts pushing to the given RTMP endpoint.
     * @return true if the stream started, false if encoder preparation failed.
     */
    fun startStream(
        fullRtmpUrl: String,
        width: Int,
        height: Int,
        fps: Int,
        videoBitrateBps: Int
    ): Boolean {
        val camera = rtmpCamera
        if (camera == null) {
            listener?.onConnectionFailed("Streaming engine not initialized")
            return false
        }
        if (camera.isStreaming) return true

        return try {
            val audioReady = camera.prepareAudio(DEFAULT_AUDIO_BITRATE, DEFAULT_SAMPLE_RATE, DEFAULT_STEREO)
            val videoReady = camera.prepareVideo(width, height, fps, videoBitrateBps, 0)
            if (audioReady && videoReady) {
                camera.startStream(fullRtmpUrl)
                true
            } else {
                listener?.onConnectionFailed("Encoder preparation failed (unsupported resolution/codec)")
                false
            }
        } catch (t: Throwable) {
            Log.e(TAG, "startStream failed", t)
            listener?.onConnectionFailed(t.localizedMessage ?: "Unknown streaming error")
            false
        }
    }

    fun stopStream() {
        val camera = rtmpCamera ?: return
        try {
            if (camera.isStreaming) camera.stopStream()
        } catch (t: Throwable) {
            Log.e(TAG, "stopStream failed", t)
        }
    }

    fun switchCamera() {
        try {
            rtmpCamera?.switchCamera()
        } catch (t: Throwable) {
            Log.e(TAG, "switchCamera failed", t)
        }
    }

    fun setTorch(enabled: Boolean) {
        val camera = rtmpCamera ?: return
        try {
            if (enabled) camera.enableLantern() else camera.disableLantern()
        } catch (t: Throwable) {
            Log.e(TAG, "setTorch failed", t)
        }
    }

    fun setAudioMuted(muted: Boolean) {
        val camera = rtmpCamera ?: return
        try {
            if (muted) camera.disableAudio() else camera.enableAudio()
        } catch (t: Throwable) {
            Log.e(TAG, "setAudioMuted failed", t)
        }
    }

    /** Adjusts the video bitrate (bits per second) while streaming, if supported. */
    fun setVideoBitrateOnFly(bitrateBps: Int) {
        try {
            rtmpCamera?.setVideoBitrateOnFly(bitrateBps)
        } catch (t: Throwable) {
            Log.e(TAG, "setVideoBitrateOnFly failed", t)
        }
    }

    /**
     * Fully releases the engine. Stops streaming/preview and drops the reference so the
     * bound surface can be garbage collected.
     */
    fun release() {
        try {
            stopStream()
            stopPreview()
        } catch (t: Throwable) {
            Log.e(TAG, "release failed", t)
        } finally {
            rtmpCamera = null
            listener = null
        }
    }

    // ---- ConnectChecker (RootEncoder callbacks) ----

    override fun onConnectionStarted(url: String) {
        listener?.onConnectionStarted(url)
    }

    override fun onConnectionSuccess() {
        listener?.onConnectionSuccess()
    }

    override fun onConnectionFailed(reason: String) {
        listener?.onConnectionFailed(reason)
    }

    override fun onDisconnect() {
        listener?.onDisconnect()
    }

    override fun onAuthError() {
        listener?.onAuthError()
    }

    override fun onAuthSuccess() {
        listener?.onAuthSuccess()
    }

    override fun onNewBitrate(bitrate: Long) {
        listener?.onNewBitrate(bitrate)
    }
}
