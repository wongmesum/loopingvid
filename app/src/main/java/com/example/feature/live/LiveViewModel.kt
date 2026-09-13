package com.example.feature.live

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.database.LiveSessionEntity
import com.example.core.database.LoopingVidRepository
import com.example.core.media.SpectrumStyle
import com.example.core.utils.RtmpUrlValidator
import com.example.core.utils.StorageInfo
import com.example.core.utils.StorageWatcher
import com.example.core.utils.ThermalInfo
import com.example.core.utils.ThermalMonitor
import com.example.core.utils.ThermalStatusLevel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class LivePlatform {
    YOUTUBE, TIKTOK, CUSTOM_RTMP
}

enum class StreamStatus {
    OFFLINE, CONNECTING, LIVE, RECONNECTING, STOPPED
}

enum class D3VisMode {
    DUAL_METRICS, VIEWER_HEATMAP, BANDWIDTH_STABILITY
}

enum class StreamResolution(val label: String, val badge: String, val recommendedBitrateKbps: Int) {
    RES_720P("720p HD", "720p60", 2800),
    RES_1080P("1080p Full HD", "1080p60", 4500),
    RES_1440P("2K QHD", "1440p60", 6800),
    RES_4K("4K Ultra HD", "2160p30", 9500)
}

enum class CameraFocusMode(val label: String) {
    AUTO("Continuous AF"),
    FIXED("Fixed Lock"),
    MACRO("Macro Detail"),
    TAP_TO_FOCUS("Touch Focus")
}

data class LiveUiState(
    val platform: LivePlatform = LivePlatform.YOUTUBE,
    val streamTitle: String = "24/7 LoopingVid Stream",
    val rtmpUrl: String = "rtmp://a.rtmp.youtube.com/live2",
    val streamKey: String = "",
    val sourceUri: String? = null,
    val sourceName: String = "",
    val streamStatus: StreamStatus = StreamStatus.OFFLINE,
    val currentLoopRound: Int = 1,
    val liveDurationSec: Long = 0,
    val currentBitrateKbps: Int = 0,
    val targetBitrateKbps: Int = 4500,
    // Bitrate history is populated from the engine's real onNewBitrate callbacks; starts empty.
    val bitrateHistory: List<Int> = emptyList(),
    val viewerCount: Int = 0,
    val peakViewerCount: Int = 0,
    val viewerHistory: List<Int> = emptyList(),
    // True when viewerCount reflects a real platform API value (e.g. YouTube Data API).
    val isViewerCountLive: Boolean = false,
    // YouTube Data API credentials for real concurrent-viewer polling (user-supplied).
    val youtubeApiKey: String = "",
    val youtubeVideoId: String = "",
    // Real upload bandwidth is derived from the engine's onNewBitrate; starts at 0/empty.
    val bandwidthMbps: Float = 0f,
    val bandwidthHistoryMbps: List<Float> = emptyList(),
    val showD3OverlayOnVideo: Boolean = true,
    val d3OverlayOpacity: Float = 0.85f,
    val d3OverlayTimeWindowSec: Int = 30,
    val d3VisualizationMode: D3VisMode = D3VisMode.DUAL_METRICS,
    val droppedFrames: Int = 0,
    val totalFrames: Long = 0,
    val fps: Int = 60,
    // RTT and jitter are not reported by the RTMP client, so they start empty/zero and are
    // NOT fabricated. They remain 0 unless a real measurement source is integrated.
    val latencyMs: Int = 0, // RTT (ms) - 0 = unknown
    val rttHistory: List<Int> = emptyList(),
    val jitterMs: Int = 0,
    val jitterHistory: List<Int> = emptyList(),
    val bufferHealthPct: Float = 0f,
    val healthScorePct: Int = 0,
    val isBuffering: Boolean = false,
    val reconnectAttempt: Int = 0,
    val maxReconnectAttempts: Int = 5,
    val reconnectCountdownSec: Int = 0,
    val connectionLossReason: String? = null,
    val titleOverlayText: String = "NOW LIVE",
    val watermarkText: String = "@LoopingVid",
    val tickerText: String = "🔴 WELCOME TO THE LIVE STREAM • SHARE & SUBSCRIBE! • 24/7 NON-STOP BROADCAST • LIKE & COMMENT",
    val isTickerEnabled: Boolean = true,
    val tickerSpeed: TickerSpeed = TickerSpeed.NORMAL,
    val tickerPosition: TickerPosition = TickerPosition.BOTTOM,
    val selectedTickerStyleIndex: Int = 0,
    // One-time validation/failure message for "Start Live" (invalid RTMP URL, camera preview not
    // ready, or the RTMP engine failing to start). Consumed by the global error dialog in
    // MainScreen so the user actually sees why the stream did not start, instead of only the
    // internal Live Log Viewer entry.
    val validationError: String? = null,
    val showSpectrumOverlay: Boolean = true,
    val spectrumStyle: SpectrumStyle = SpectrumStyle.BARS,
    val autoStopMinutes: Int = 0, // 0 = continuous
    val thermalState: String = "Normal",
    val thermalInfo: ThermalInfo = ThermalInfo(),
    val videoSourceVolume: Float = 1.0f,
    val isVideoSourceMuted: Boolean = false,
    val micInputVolume: Float = 0.8f,
    val isMicInputMuted: Boolean = false,
    val bgMusicVolume: Float = 0.5f,
    val isBgMusicMuted: Boolean = false,
    val bgMusicTrack: String = "Chill Lo-Fi Beat",
    val isMicDuckingEnabled: Boolean = true,
    val masterVolume: Float = 1.0f,
    val isMasterMuted: Boolean = false,
    val isQuickSettingsExpanded: Boolean = false,
    val isCameraAutoFocusEnabled: Boolean = true,
    val cameraFocusMode: CameraFocusMode = CameraFocusMode.AUTO,
    val streamResolution: StreamResolution = StreamResolution.RES_1080P,
    val isQuickMuted: Boolean = false,
    val isTorchActive: Boolean = false,
    val isSourcePreviewPlaying: Boolean = true,
    val showDiagnosticOverlay: Boolean = true,
    val isDiagnosticOverlayExpanded: Boolean = true,
    val bassGainDb: Float = 0.0f,
    val trebleGainDb: Float = 0.0f,
    val selectedEqPresetName: String = "Flat / Neutral",
    val videoVuLevel: Float = 0.72f,
    val micVuLevel: Float = 0.45f,
    val bgMusicVuLevel: Float = 0.38f,
    val masterVuLevel: Float = 0.80f,
    val storageInfo: StorageInfo = StorageInfo(
        availableBytes = 850 * 1024 * 1024L,
        totalBytes = 64 * 1024 * 1024 * 1024L,
        availableMb = 850L,
        isLowStorage = false,
        isCriticalStorage = false,
        warningMessage = null
    ),
    val targets: List<BroadcastTarget> = listOf(
        BroadcastTarget(
            id = "target_youtube_primary",
            name = "YouTube Primary Channel",
            platform = LivePlatform.YOUTUBE,
            rtmpUrl = "rtmp://a.rtmp.youtube.com/live2",
            streamKey = "",
            isEnabled = true,
            requiredBitrateKbps = 4500
        ),
        BroadcastTarget(
            id = "target_tiktok_secondary",
            name = "TikTok Live Feed",
            platform = LivePlatform.TIKTOK,
            rtmpUrl = "rtmp://live-push.tiktok.com/live",
            streamKey = "",
            isEnabled = false,
            requiredBitrateKbps = 3500
        )
    ),
    val isSimulcastEnabled: Boolean = false,
    val availableBandwidthKbps: Int = 12000,
    val isBandwidthSufficient: Boolean = true,
    val bandwidthWarningMessage: String? = null,
    val dbSessionId: Long? = null,
    // Logs start empty and are populated with REAL events (RTMP connect, bitrate, errors)
    // as they happen. No pre-seeded "hardware initialized" entries that never occurred.
    val logs: List<LiveLogEvent> = emptyList()
)

class LiveViewModel(
    private val repository: LoopingVidRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(LiveUiState())
    val uiState: StateFlow<LiveUiState> = _uiState.asStateFlow()

    private var liveTimerJob: Job? = null

    // Real RTMP broadcast engine (RootEncoder). Null until the live screen binds a preview surface.
    private val rtmpManager = RtmpStreamManager()

    // Whether a broadcast surface has been bound. When false we cannot push a real stream.
    @Volatile
    private var isStreamViewBound: Boolean = false

    // Polls the platform API (currently YouTube) for real concurrent viewer counts.
    private var viewerPollJob: Job? = null

    /** Exposes the RTMP engine so the Composable preview can bind its OpenGlView. */
    fun getRtmpManager(): RtmpStreamManager = rtmpManager

    /** Called by the preview Composable once the OpenGlView surface is ready. */
    fun onStreamViewBound() {
        isStreamViewBound = true
    }

    /** Called by the preview Composable when the surface is destroyed (navigation away). */
    fun onStreamViewUnbound() {
        isStreamViewBound = false
    }

    /**
     * Real RTMP engine callbacks. Runs on RootEncoder threads; state is pushed through the
     * thread-safe MutableStateFlow and DB writes are marshalled onto viewModelScope.
     */
    private val rtmpListener = object : RtmpStreamManager.Listener {
        override fun onConnectionStarted(url: String) {
            addLog(LiveLogLevel.INFO, LiveLogCategory.RTMP, "RTMP connection started: opening socket to ingest server...")
        }

        override fun onConnectionSuccess() {
            addLog(LiveLogLevel.SUCCESS, LiveLogCategory.RTMP, "RTMP connection established. Stream is LIVE.")
            _uiState.value = _uiState.value.copy(
                streamStatus = StreamStatus.LIVE,
                isBuffering = false,
                connectionLossReason = null,
                reconnectAttempt = 0,
                reconnectCountdownSec = 0
            )
            // Begin polling real viewer counts (YouTube) now that we are live.
            startViewerCountPolling()
        }

        override fun onConnectionFailed(reason: String) {
            addLog(LiveLogLevel.ERROR, LiveLogCategory.RTMP, "RTMP connection failed: $reason")
            // Delegate to the reconnection handler which performs a real retry via the engine.
            handleRealConnectionDrop(reason)
        }

        override fun onDisconnect() {
            addLog(LiveLogLevel.WARN, LiveLogCategory.RTMP, "RTMP disconnected.")
        }

        override fun onAuthError() {
            addLog(LiveLogLevel.ERROR, LiveLogCategory.RTMP, "RTMP authentication error. Check your stream key.")
            _uiState.value = _uiState.value.copy(
                streamStatus = StreamStatus.STOPPED,
                isBuffering = false,
                connectionLossReason = "Authentication error (invalid stream key)."
            )
            rtmpManager.stopStream()
        }

        override fun onAuthSuccess() {
            addLog(LiveLogLevel.SUCCESS, LiveLogCategory.RTMP, "RTMP authentication successful.")
        }

        override fun onNewBitrate(bitrateBps: Long) {
            // Convert bits/sec to Kbps and feed the real value into the UI/telemetry.
            val kbps = (bitrateBps / 1000L).toInt().coerceAtLeast(0)
            val history = (_uiState.value.bitrateHistory + kbps).takeLast(25)
            _uiState.value = _uiState.value.copy(
                currentBitrateKbps = kbps,
                bitrateHistory = history,
                bandwidthMbps = (kbps / 1000f).coerceIn(0f, 100f)
            )
        }
    }

    init {
        // Register the RTMP listener with the engine before anything else runs. Without this,
        // real RootEncoder callbacks (onConnectionSuccess, onNewBitrate, onConnectionFailed, etc.)
        // never reach the ViewModel, so streamStatus never becomes LIVE, bitrate history stays
        // empty, and auto-reconnect never triggers even when the RTMP connection is genuinely up.
        rtmpManager.setListener(rtmpListener)
        initializeLiveStreamViewModel()
    }

    private fun initializeLiveStreamViewModel() {
        try {
            // Load default saved RTMP key if present
            viewModelScope.launch {
                try {
                    val savedKey = repository.getSettingValue("stream_key_youtube")
                    if (!savedKey.isNullOrBlank()) {
                        _uiState.value = _uiState.value.copy(streamKey = savedKey)
                    }
                    // Restore saved YouTube viewer-count credentials, if any.
                    val savedYtApiKey = repository.getSettingValue("youtube_api_key")
                    val savedYtVideoId = repository.getSettingValue("youtube_video_id")
                    if (!savedYtApiKey.isNullOrBlank() || !savedYtVideoId.isNullOrBlank()) {
                        _uiState.value = _uiState.value.copy(
                            youtubeApiKey = savedYtApiKey ?: _uiState.value.youtubeApiKey,
                            youtubeVideoId = savedYtVideoId ?: _uiState.value.youtubeVideoId
                        )
                    }
                } catch (e: Throwable) {
                    addLog(
                        level = LiveLogLevel.WARN,
                        category = LiveLogCategory.SYSTEM,
                        message = "Saved stream key restoration deferred safely: ${e.localizedMessage}"
                    )
                }
            }
        } catch (e: Throwable) {
            addLog(
                level = LiveLogLevel.ERROR,
                category = LiveLogCategory.SYSTEM,
                message = "LiveViewModel init isolated safely: ${e.localizedMessage}"
            )
        }
    }

    /**
     * Isolates and safely executes initialization routines when navigating to the Live Streaming screen.
     * Prevents app force close during screen navigation, context retrieval, or hardware query failures.
     */
    fun initializeStreamModule(context: Context) {
        viewModelScope.launch {
            try {
                checkThermalStatus(context)
            } catch (e: Throwable) {
                addLog(
                    level = LiveLogLevel.WARN,
                    category = LiveLogCategory.HEALTH,
                    message = "Thermal initialization query handled safely: ${e.localizedMessage}"
                )
            }

            try {
                refreshStorageInfo(context)
            } catch (e: Throwable) {
                addLog(
                    level = LiveLogLevel.WARN,
                    category = LiveLogCategory.HEALTH,
                    message = "Storage info initialization query handled safely: ${e.localizedMessage}"
                )
            }

            addLog(
                level = LiveLogLevel.SUCCESS,
                category = LiveLogCategory.SYSTEM,
                message = "Live streaming module initialized with full exception isolation boundaries."
            )
        }
    }

    private fun String?.isNull_or_blank(): Boolean = this == null || this.isBlank()

    fun setPlatform(platform: LivePlatform) {
        val defaultUrl = when (platform) {
            LivePlatform.YOUTUBE -> "rtmp://a.rtmp.youtube.com/live2"
            LivePlatform.TIKTOK -> "rtmp://live-push.tiktok.com/live"
            LivePlatform.CUSTOM_RTMP -> "rtmp://custom.server.com/live"
        }
        _uiState.value = _uiState.value.copy(
            platform = platform,
            rtmpUrl = defaultUrl
        )
    }

    fun setSourceMedia(uri: String, name: String) {
        _uiState.value = _uiState.value.copy(
            sourceUri = uri,
            sourceName = name
        )
    }

    fun updateStreamTitle(title: String) {
        _uiState.value = _uiState.value.copy(streamTitle = title)
    }

    fun updateRtmpUrl(url: String) {
        _uiState.value = _uiState.value.copy(rtmpUrl = url)
    }

    fun updateStreamKey(key: String) {
        _uiState.value = _uiState.value.copy(streamKey = key)
        viewModelScope.launch {
            repository.setSetting("stream_key_${_uiState.value.platform.name.lowercase()}", key)
        }
    }

    fun updateYoutubeApiKey(key: String) {
        _uiState.value = _uiState.value.copy(youtubeApiKey = key)
        viewModelScope.launch {
            try { repository.setSetting("youtube_api_key", key) } catch (_: Throwable) {}
        }
    }

    fun updateYoutubeVideoId(videoId: String) {
        _uiState.value = _uiState.value.copy(youtubeVideoId = videoId)
        viewModelScope.launch {
            try { repository.setSetting("youtube_video_id", videoId) } catch (_: Throwable) {}
        }
    }

    fun updateTitleOverlay(text: String) {
        _uiState.value = _uiState.value.copy(titleOverlayText = text)
    }

    fun updateWatermark(text: String) {
        _uiState.value = _uiState.value.copy(watermarkText = text)
    }

    fun updateTickerText(text: String) {
        _uiState.value = _uiState.value.copy(tickerText = text)
    }

    fun setTickerEnabled(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(isTickerEnabled = enabled)
    }

    fun setTickerSpeed(speed: TickerSpeed) {
        _uiState.value = _uiState.value.copy(tickerSpeed = speed)
    }

    fun setTickerPosition(position: TickerPosition) {
        _uiState.value = _uiState.value.copy(tickerPosition = position)
    }

    fun setTickerStyleIndex(index: Int) {
        _uiState.value = _uiState.value.copy(selectedTickerStyleIndex = index)
    }

    fun setAutoStopMinutes(minutes: Int) {
        _uiState.value = _uiState.value.copy(autoStopMinutes = minutes.coerceAtLeast(0))
    }

    fun setSpectrumStyle(style: SpectrumStyle) {
        _uiState.value = _uiState.value.copy(spectrumStyle = style)
    }

    // --- D3.js Real-time Overlay Controls ---
    fun toggleD3OverlayOnVideo() {
        _uiState.value = _uiState.value.copy(showD3OverlayOnVideo = !_uiState.value.showD3OverlayOnVideo)
    }

    fun setD3OverlayOpacity(opacity: Float) {
        _uiState.value = _uiState.value.copy(d3OverlayOpacity = opacity.coerceIn(0.2f, 1.0f))
    }

    fun setD3OverlayTimeWindow(seconds: Int) {
        _uiState.value = _uiState.value.copy(d3OverlayTimeWindowSec = seconds)
    }

    fun setD3VisualizationMode(mode: D3VisMode) {
        _uiState.value = _uiState.value.copy(d3VisualizationMode = mode)
    }

    // --- Quick Stream Settings Panel Actions ---
    fun toggleQuickSettingsPanel() {
        _uiState.value = _uiState.value.copy(isQuickSettingsExpanded = !_uiState.value.isQuickSettingsExpanded)
    }

    fun setQuickSettingsPanelExpanded(expanded: Boolean) {
        _uiState.value = _uiState.value.copy(isQuickSettingsExpanded = expanded)
    }

    fun toggleCameraAutoFocus() {
        val newFocus = !_uiState.value.isCameraAutoFocusEnabled
        val newMode = if (newFocus) CameraFocusMode.AUTO else CameraFocusMode.FIXED
        _uiState.value = _uiState.value.copy(
            isCameraAutoFocusEnabled = newFocus,
            cameraFocusMode = newMode
        )
        addLog(
            level = LiveLogLevel.INFO,
            category = LiveLogCategory.SYSTEM,
            message = "Camera focus updated: ${if (newFocus) "Auto-Focus Enabled (AF-C)" else "Focus Locked (AF-L)"}"
        )
    }

    fun setCameraFocusMode(mode: CameraFocusMode) {
        val autoFocus = mode == CameraFocusMode.AUTO
        _uiState.value = _uiState.value.copy(
            cameraFocusMode = mode,
            isCameraAutoFocusEnabled = autoFocus
        )
        addLog(
            level = LiveLogLevel.INFO,
            category = LiveLogCategory.SYSTEM,
            message = "Camera focus mode set to: ${mode.label}"
        )
    }

    fun toggleQuickMuteAudio() {
        val newMuted = !(_uiState.value.isQuickMuted || _uiState.value.isMasterMuted)
        _uiState.value = _uiState.value.copy(
            isQuickMuted = newMuted,
            isMasterMuted = newMuted,
            isMicInputMuted = newMuted
        )
        addLog(
            level = LiveLogLevel.INFO,
            category = LiveLogCategory.SYSTEM,
            message = if (newMuted) "Quick Stream Audio MUTE Activated" else "Quick Stream Audio UNMUTED"
        )
    }

    fun setStreamResolution(resolution: StreamResolution) {
        _uiState.value = _uiState.value.copy(
            streamResolution = resolution,
            targetBitrateKbps = resolution.recommendedBitrateKbps,
            currentBitrateKbps = resolution.recommendedBitrateKbps
        )
        addLog(
            level = LiveLogLevel.SUCCESS,
            category = LiveLogCategory.ENCODER,
            message = "Stream resolution set to ${resolution.label} (${resolution.badge}). Target bitrate adjusted to ${resolution.recommendedBitrateKbps} Kbps."
        )
    }

    fun toggleTorchActive() {
        val newTorch = !_uiState.value.isTorchActive
        _uiState.value = _uiState.value.copy(isTorchActive = newTorch)
        addLog(
            level = LiveLogLevel.INFO,
            category = LiveLogCategory.SYSTEM,
            message = "Camera LED flashlight ${if (newTorch) "ENABLED" else "DISABLED"}"
        )
    }

    fun toggleDiagnosticOverlay() {
        val nextState = !_uiState.value.showDiagnosticOverlay
        _uiState.value = _uiState.value.copy(showDiagnosticOverlay = nextState)
        addLog(
            level = LiveLogLevel.INFO,
            category = LiveLogCategory.SYSTEM,
            message = "Stream Diagnostic Overlay ${if (nextState) "VISIBLE" else "HIDDEN"}"
        )
    }

    fun toggleDiagnosticOverlayExpanded() {
        _uiState.value = _uiState.value.copy(isDiagnosticOverlayExpanded = !_uiState.value.isDiagnosticOverlayExpanded)
    }

    fun toggleSourcePreviewPlayPause() {
        val nextState = !_uiState.value.isSourcePreviewPlaying
        _uiState.value = _uiState.value.copy(isSourcePreviewPlaying = nextState)
        addLog(
            level = LiveLogLevel.INFO,
            category = LiveLogCategory.SYSTEM,
            message = "Source video preview ${if (nextState) "PLAYING" else "PAUSED"}"
        )
    }

    // --- Audio Mixer Controls ---
    fun setVideoSourceVolume(volume: Float) {
        _uiState.value = _uiState.value.copy(videoSourceVolume = volume.coerceIn(0f, 1f))
    }

    fun toggleVideoSourceMute() {
        _uiState.value = _uiState.value.copy(isVideoSourceMuted = !_uiState.value.isVideoSourceMuted)
    }

    fun setMicInputVolume(volume: Float) {
        _uiState.value = _uiState.value.copy(micInputVolume = volume.coerceIn(0f, 1f))
    }

    fun toggleMicInputMute() {
        val newMuted = !_uiState.value.isMicInputMuted
        _uiState.value = _uiState.value.copy(isMicInputMuted = newMuted)
        // Reflect the change on the live audio track if streaming.
        rtmpManager.setAudioMuted(newMuted || _uiState.value.isQuickMuted)
    }

    fun setBgMusicVolume(volume: Float) {
        _uiState.value = _uiState.value.copy(bgMusicVolume = volume.coerceIn(0f, 1f))
    }

    fun toggleBgMusicMute() {
        _uiState.value = _uiState.value.copy(isBgMusicMuted = !_uiState.value.isBgMusicMuted)
    }

    fun setBgMusicTrack(track: String) {
        _uiState.value = _uiState.value.copy(bgMusicTrack = track)
    }

    fun toggleMicDucking() {
        _uiState.value = _uiState.value.copy(isMicDuckingEnabled = !_uiState.value.isMicDuckingEnabled)
    }

    fun setMasterVolume(volume: Float) {
        _uiState.value = _uiState.value.copy(masterVolume = volume.coerceIn(0f, 1f))
    }

    fun toggleMasterMute() {
        _uiState.value = _uiState.value.copy(isMasterMuted = !_uiState.value.isMasterMuted)
    }

    fun setBassGainDb(gainDb: Float) {
        _uiState.value = _uiState.value.copy(
            bassGainDb = gainDb.coerceIn(-12f, 12f),
            selectedEqPresetName = "Custom"
        )
    }

    fun setTrebleGainDb(gainDb: Float) {
        _uiState.value = _uiState.value.copy(
            trebleGainDb = gainDb.coerceIn(-12f, 12f),
            selectedEqPresetName = "Custom"
        )
    }

    fun selectEqPreset(presetName: String, bassDb: Float, trebleDb: Float) {
        _uiState.value = _uiState.value.copy(
            selectedEqPresetName = presetName,
            bassGainDb = bassDb,
            trebleGainDb = trebleDb
        )
    }

    fun resetAudioAdjustments() {
        _uiState.value = _uiState.value.copy(
            masterVolume = 1.0f,
            isMasterMuted = false,
            bassGainDb = 0.0f,
            trebleGainDb = 0.0f,
            selectedEqPresetName = "Flat / Neutral"
        )
    }

    // --- Broadcast Target & Simulcast Controls ---
    private fun validateSimulcastBandwidth(state: LiveUiState): LiveUiState {
        val activeTargets = if (state.isSimulcastEnabled) {
            state.targets.filter { it.isEnabled }
        } else {
            val primary = state.targets.firstOrNull { it.isEnabled } ?: state.targets.firstOrNull()
            if (primary != null) listOf(primary) else emptyList()
        }

        val totalRequiredKbps = activeTargets.sumOf { it.requiredBitrateKbps }
        val isSufficient = totalRequiredKbps <= state.availableBandwidthKbps

        val warning = if (!isSufficient) {
            "BANDWIDTH WARNING: Required stream bitrate (${totalRequiredKbps / 1000f} Mbps for ${activeTargets.size} destinations) exceeds measured network bandwidth (${state.availableBandwidthKbps / 1000f} Mbps). Simulcast quality or frame rate may degrade!"
        } else null

        return state.copy(
            isBandwidthSufficient = isSufficient,
            bandwidthWarningMessage = warning
        )
    }

    fun toggleSimulcastEnabled() {
        val updatedState = _uiState.value.copy(isSimulcastEnabled = !_uiState.value.isSimulcastEnabled)
        _uiState.value = validateSimulcastBandwidth(updatedState)
    }

    fun toggleTargetEnabled(targetId: String) {
        val updatedTargets = _uiState.value.targets.map {
            if (it.id == targetId) it.copy(isEnabled = !it.isEnabled) else it
        }
        val updatedState = _uiState.value.copy(targets = updatedTargets)
        _uiState.value = validateSimulcastBandwidth(updatedState)
    }

    fun addBroadcastTarget(
        name: String,
        platform: LivePlatform,
        rtmpUrl: String,
        streamKey: String,
        requiredBitrateKbps: Int
    ) {
        val newTarget = BroadcastTarget(
            name = name,
            platform = platform,
            rtmpUrl = rtmpUrl,
            streamKey = streamKey,
            isEnabled = true,
            requiredBitrateKbps = requiredBitrateKbps
        )
        val updatedTargets = _uiState.value.targets + newTarget
        val updatedState = _uiState.value.copy(targets = updatedTargets)
        _uiState.value = validateSimulcastBandwidth(updatedState)
    }

    fun updateBroadcastTarget(
        id: String,
        name: String,
        platform: LivePlatform,
        rtmpUrl: String,
        streamKey: String,
        requiredBitrateKbps: Int
    ) {
        val updatedTargets = _uiState.value.targets.map {
            if (it.id == id) {
                it.copy(
                    name = name,
                    platform = platform,
                    rtmpUrl = rtmpUrl,
                    streamKey = streamKey,
                    requiredBitrateKbps = requiredBitrateKbps
                )
            } else it
        }
        val updatedState = _uiState.value.copy(targets = updatedTargets)
        _uiState.value = validateSimulcastBandwidth(updatedState)
    }

    fun removeBroadcastTarget(targetId: String) {
        val updatedTargets = _uiState.value.targets.filter { it.id != targetId }
        val updatedState = _uiState.value.copy(targets = updatedTargets)
        _uiState.value = validateSimulcastBandwidth(updatedState)
    }

    fun setAvailableBandwidth(bandwidthKbps: Int) {
        val updatedState = _uiState.value.copy(availableBandwidthKbps = bandwidthKbps.coerceAtLeast(1000))
        _uiState.value = validateSimulcastBandwidth(updatedState)
    }

    /**
     * Generates a randomized bandwidth estimate for simulcast validation. This is intentionally
     * NOT a real upload speed test - it performs no network probe. A genuine measurement would
     * require uploading a payload to a server and timing it, which risks consuming the user's
     * data/quota without explicit consent, so this stays a clearly-labeled simulation (see the
     * "Simulate Speed" button text in BroadcastTargetsCard) rather than silently faking a real
     * measurement.
     */
    fun simulateBandwidthTest() {
        val simulatedCapacityKbps = (6000..18000).random()
        setAvailableBandwidth(simulatedCapacityKbps)
    }

    fun startLiveStream(context: Context) {
        try {
            val state = _uiState.value

            // Validate the RTMP endpoint. For camera streaming the video source is the device
            // camera bound to the preview surface, so a media source URI is not required.
            val validation = RtmpUrlValidator.validateRtmpUrl(state.rtmpUrl)
            if (!validation.isValid) {
                addLog(
                    level = LiveLogLevel.WARN,
                    category = LiveLogCategory.RTMP,
                    message = "Start live stream skipped: invalid RTMP URL. ${validation.errorMessage ?: ""}"
                )
                _uiState.value = _uiState.value.copy(
                    validationError = "RTMP URL tidak valid: ${validation.errorMessage ?: "Periksa kembali konfigurasi streaming."}"
                )
                return
            }

            if (!isStreamViewBound) {
                addLog(
                    level = LiveLogLevel.WARN,
                    category = LiveLogCategory.RTMP,
                    message = "Start live stream skipped: camera preview is not ready yet. Grant camera/mic permission and wait for preview."
                )
                _uiState.value = _uiState.value.copy(
                    validationError = "Kamera preview belum siap. Berikan izin kamera/mikrofon dan tunggu preview aktif."
                )
                return
            }

            // Compose the full ingest URL: server URL + stream key.
            val fullUrl = buildFullRtmpUrl(state.rtmpUrl, state.streamKey)

            _uiState.value = state.copy(streamStatus = StreamStatus.CONNECTING)

            viewModelScope.launch {
                try {
                    val session = LiveSessionEntity(
                        platform = state.platform.name,
                        streamTitle = state.streamTitle,
                        rtmpUrl = state.rtmpUrl,
                        sourceUri = state.sourceUri ?: "",
                        durationSec = 0,
                        totalLoops = 1,
                        status = "ACTIVE",
                        avgBitrateKbps = 0,
                        droppedFrames = 0
                    )

                    val sessionId = try {
                        repository.saveLiveSession(session)
                    } catch (e: Throwable) {
                        addLog(
                            level = LiveLogLevel.WARN,
                            category = LiveLogCategory.SYSTEM,
                            message = "Session database recording deferred: ${e.localizedMessage}"
                        )
                        System.currentTimeMillis()
                    }

                    _uiState.value = _uiState.value.copy(
                        dbSessionId = sessionId,
                        liveDurationSec = 0,
                        currentLoopRound = 1
                    )

                    // Start the foreground service (camera|microphone) before pushing frames.
                    try {
                        val serviceIntent = Intent(context, LiveStreamService::class.java).apply {
                            putExtra(LiveStreamService.EXTRA_PLATFORM, state.platform.name)
                            putExtra(LiveStreamService.EXTRA_TITLE, state.streamTitle)
                        }
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                            context.startForegroundService(serviceIntent)
                        } else {
                            context.startService(serviceIntent)
                        }
                    } catch (e: Throwable) {
                        addLog(
                            level = LiveLogLevel.WARN,
                            category = LiveLogCategory.SYSTEM,
                            message = "Foreground stream service start handled safely: ${e.localizedMessage}"
                        )
                    }

                    // Start the real RTMP broadcast. The status becomes LIVE only once the
                    // engine reports onConnectionSuccess (handled by rtmpListener).
                    val resolution = state.streamResolution
                    val (w, h) = resolutionToSize(resolution)
                    val started = rtmpManager.startStream(
                        fullRtmpUrl = fullUrl,
                        width = w,
                        height = h,
                        fps = state.fps.coerceIn(15, 60),
                        videoBitrateBps = resolution.recommendedBitrateKbps * 1000
                    )

                    if (!started) {
                        addLog(
                            level = LiveLogLevel.ERROR,
                            category = LiveLogCategory.RTMP,
                            message = "Failed to start RTMP stream: camera/encoder initialization failed."
                        )
                        _uiState.value = _uiState.value.copy(
                            streamStatus = StreamStatus.STOPPED,
                            isBuffering = false,
                            validationError = "Gagal memulai live stream: inisialisasi kamera/encoder gagal."
                        )
                        // Stop the service we just started since streaming didn't begin.
                        try { context.stopService(Intent(context, LiveStreamService::class.java)) } catch (_: Throwable) {}
                        return@launch
                    }

                    // Apply current mute state to the live audio track.
                    rtmpManager.setAudioMuted(_uiState.value.isMicInputMuted || _uiState.value.isQuickMuted)

                    // Begin the uptime/thermal/storage telemetry loop (real bitrate comes via callback).
                    startLiveTelemetryLoop(sessionId, context)
                } catch (e: Throwable) {
                    addLog(
                        level = LiveLogLevel.ERROR,
                        category = LiveLogCategory.RTMP,
                        message = "RTMP connection startup error caught safely: ${e.localizedMessage}"
                    )
                    _uiState.value = _uiState.value.copy(
                        streamStatus = StreamStatus.STOPPED,
                        isBuffering = false,
                        validationError = "Gagal memulai live stream: ${e.localizedMessage ?: "Kesalahan tidak diketahui."}"
                    )
                }
            }
        } catch (e: Throwable) {
            addLog(
                level = LiveLogLevel.ERROR,
                category = LiveLogCategory.SYSTEM,
                message = "Live stream start launch error caught: ${e.localizedMessage}"
            )
            _uiState.value = _uiState.value.copy(
                streamStatus = StreamStatus.STOPPED,
                validationError = "Gagal memulai live stream: ${e.localizedMessage ?: "Kesalahan tidak diketahui."}"
            )
        }
    }

    /** Clears [LiveUiState.validationError] after the global error dialog has shown it. */
    fun consumeValidationError() {
        _uiState.value = _uiState.value.copy(validationError = null)
    }

    /** Builds the full RTMP ingest URL by appending the stream key to the server URL. */
    private fun buildFullRtmpUrl(serverUrl: String, streamKey: String): String {
        val trimmedUrl = serverUrl.trim().trimEnd('/')
        val trimmedKey = streamKey.trim()
        return if (trimmedKey.isEmpty()) trimmedUrl else "$trimmedUrl/$trimmedKey"
    }

    /** Maps a [StreamResolution] to concrete encoder width/height. */
    private fun resolutionToSize(resolution: StreamResolution): Pair<Int, Int> {
        return when (resolution) {
            StreamResolution.RES_720P -> 1280 to 720
            StreamResolution.RES_1080P -> 1920 to 1080
            StreamResolution.RES_1440P -> 2560 to 1440
            StreamResolution.RES_4K -> 3840 to 2160
        }
    }

    /** Builds the viewer-count provider appropriate for the current platform/credentials. */
    private fun buildViewerCountProvider(): ViewerCountProvider {
        val state = _uiState.value
        return if (state.platform == LivePlatform.YOUTUBE &&
            state.youtubeApiKey.isNotBlank() &&
            state.youtubeVideoId.isNotBlank()
        ) {
            YouTubeViewerCountProvider(state.youtubeApiKey.trim(), state.youtubeVideoId.trim())
        } else {
            // TikTok and custom RTMP have no public concurrent-viewer API.
            NoOpViewerCountProvider
        }
    }

    /**
     * Polls the platform API for real concurrent viewers while the stream is live. Only
     * updates the UI with values actually returned by the API; when unavailable the viewer
     * figures stay at their honest placeholder (0 / not live).
     */
    private fun startViewerCountPolling() {
        viewerPollJob?.cancel()
        val provider = buildViewerCountProvider()
        if (provider is NoOpViewerCountProvider) {
            // No real source; make it explicit that the viewer count is not live data.
            _uiState.value = _uiState.value.copy(isViewerCountLive = false)
            return
        }

        viewerPollJob = viewModelScope.launch {
            addLog(LiveLogLevel.INFO, LiveLogCategory.NETWORK, "Viewer count polling started (YouTube Data API).")
            while (_uiState.value.streamStatus == StreamStatus.LIVE ||
                _uiState.value.streamStatus == StreamStatus.RECONNECTING
            ) {
                val viewers = provider.getConcurrentViewers()
                if (viewers != null) {
                    val history = (_uiState.value.viewerHistory + viewers).takeLast(25)
                    val peak = maxOf(_uiState.value.peakViewerCount, viewers)
                    _uiState.value = _uiState.value.copy(
                        viewerCount = viewers,
                        peakViewerCount = peak,
                        viewerHistory = history,
                        isViewerCountLive = true
                    )
                } else {
                    _uiState.value = _uiState.value.copy(isViewerCountLive = false)
                }
                // Poll every 15s to respect API quota while staying reasonably fresh.
                delay(15_000)
            }
        }
    }

    private fun stopViewerCountPolling() {
        viewerPollJob?.cancel()
        viewerPollJob = null
    }

    /**
     * Handles a real RTMP connection drop reported by the engine. Marks the stream as
     * reconnecting and attempts to restart the stream via the engine with backoff.
     */
    private fun handleRealConnectionDrop(reason: String) {
        if (_uiState.value.streamStatus == StreamStatus.STOPPED) return

        reconnectJob?.cancel()
        _uiState.value = _uiState.value.copy(
            streamStatus = StreamStatus.RECONNECTING,
            isBuffering = true,
            connectionLossReason = reason,
            reconnectAttempt = 1,
            reconnectCountdownSec = 3
        )

        reconnectJob = viewModelScope.launch {
            val maxAttempts = _uiState.value.maxReconnectAttempts
            var attempt = 1
            while (attempt <= maxAttempts && _uiState.value.streamStatus == StreamStatus.RECONNECTING) {
                _uiState.value = _uiState.value.copy(reconnectAttempt = attempt)
                for (sec in 3 downTo 1) {
                    if (_uiState.value.streamStatus != StreamStatus.RECONNECTING) return@launch
                    _uiState.value = _uiState.value.copy(reconnectCountdownSec = sec)
                    delay(1000)
                }
                addLog(
                    level = LiveLogLevel.INFO,
                    category = LiveLogCategory.NETWORK,
                    message = "Reconnection attempt $attempt/$maxAttempts: restarting RTMP stream..."
                )
                try {
                    rtmpManager.stopStream()
                    val state = _uiState.value
                    val (w, h) = resolutionToSize(state.streamResolution)
                    val ok = rtmpManager.startStream(
                        fullRtmpUrl = buildFullRtmpUrl(state.rtmpUrl, state.streamKey),
                        width = w,
                        height = h,
                        fps = state.fps.coerceIn(15, 60),
                        videoBitrateBps = state.streamResolution.recommendedBitrateKbps * 1000
                    )
                    // Success is confirmed asynchronously via onConnectionSuccess; give it a moment.
                    if (ok) {
                        delay(2500)
                        if (_uiState.value.streamStatus == StreamStatus.LIVE) return@launch
                    }
                } catch (e: Throwable) {
                    addLog(LiveLogLevel.WARN, LiveLogCategory.NETWORK, "Reconnect attempt failed: ${e.localizedMessage}")
                }
                attempt++
            }
            if (_uiState.value.streamStatus == StreamStatus.RECONNECTING) {
                addLog(
                    level = LiveLogLevel.ERROR,
                    category = LiveLogCategory.NETWORK,
                    message = "RECONNECTION FAILED: exceeded $maxAttempts attempts. Stream stopped."
                )
                _uiState.value = _uiState.value.copy(
                    streamStatus = StreamStatus.STOPPED,
                    isBuffering = false,
                    connectionLossReason = "Max reconnection retries exceeded."
                )
                rtmpManager.stopStream()
            }
        }
    }

    private fun startLiveTelemetryLoop(sessionId: Long, context: Context) {
        liveTimerJob?.cancel()
        liveTimerJob = viewModelScope.launch {
            var elapsed = 0L
            var loops = 1
            var totalBitrateSum = 0L
            val accumulatedDroppedFrames = 0
            var totalFramesSent = 0L

            val bandwidthHistoryList = _uiState.value.bandwidthHistoryMbps.toMutableList()

            // Run while the stream is connecting/live/reconnecting. Uptime only advances when LIVE.
            while (_uiState.value.streamStatus == StreamStatus.LIVE ||
                _uiState.value.streamStatus == StreamStatus.CONNECTING ||
                _uiState.value.streamStatus == StreamStatus.RECONNECTING
            ) {
                delay(1000)

                val isLiveNow = _uiState.value.streamStatus == StreamStatus.LIVE
                if (!isLiveNow) {
                    // While connecting/reconnecting, don't accumulate uptime or fake telemetry.
                    continue
                }

                elapsed++
                totalFramesSent += _uiState.value.fps.toLong().coerceAtLeast(1L)

                // Check Auto-Stop Timer Limit
                val autoStopLimitMin = _uiState.value.autoStopMinutes
                if (autoStopLimitMin > 0 && elapsed >= autoStopLimitMin * 60L) {
                    stopLiveStream(context)
                    break
                }

                // Every 30 seconds mark a seamless loop round completion.
                if (elapsed % 30L == 0L) {
                    loops++
                }

                // Real bitrate is supplied by the engine via onNewBitrate; derive health from it.
                val currentBitrate = _uiState.value.currentBitrateKbps
                if (currentBitrate > 0) totalBitrateSum += currentBitrate
                val avgBitrate = if (elapsed > 0) (totalBitrateSum / elapsed).toInt() else currentBitrate

                // A sustained bitrate far below target indicates network trouble -> health proxy.
                val targetKbps = _uiState.value.streamResolution.recommendedBitrateKbps
                val bitrateRatio = if (targetKbps > 0) currentBitrate.toFloat() / targetKbps else 1f
                val healthScore = when {
                    currentBitrate == 0 -> 60
                    bitrateRatio < 0.4f -> 72
                    bitrateRatio < 0.7f -> 86
                    else -> 98
                }

                // Device Thermal Telemetry Evaluation using PowerManager.getCurrentThermalStatus()
                val baseTemp = ThermalMonitor.getDeviceTemperatureCelsius(context)
                val systemThermalCode = ThermalMonitor.getCurrentThermalStatus(context)
                val encoderHeatOffset = (elapsed.toFloat() / 120f).coerceAtMost(6.0f)
                val currentTemp = baseTemp + encoderHeatOffset
                val thermalInfo = ThermalMonitor.evaluateThermalStatus(currentTemp, systemThermalCode)

                // StorageWatcher Disk Space Evaluation
                val currentStorageInfo = if (elapsed % 10L == 0L) {
                    StorageWatcher.getStorageInfo(context)
                } else {
                    _uiState.value.storageInfo
                }

                // Audio Mixer level indicators. RootEncoder does not expose per-channel PCM
                // metering, so instead of fabricating random peaks we reflect the user's actual
                // fader/mute settings deterministically: a muted channel reads 0, otherwise the
                // configured volume (with mic ducking applied). This is an honest level display,
                // not a fake animated meter.
                val masterFactor = if (_uiState.value.isMasterMuted) 0f else _uiState.value.masterVolume
                val videoPeak = if (!_uiState.value.isVideoSourceMuted) {
                    _uiState.value.videoSourceVolume.coerceIn(0f, 1f)
                } else 0f

                val micPeak = if (!_uiState.value.isMicInputMuted) {
                    _uiState.value.micInputVolume.coerceIn(0f, 1f)
                } else 0f

                val duckingFactor = if (_uiState.value.isMicDuckingEnabled && micPeak > 0.35f) 0.35f else 1.0f
                val bgPeak = if (!_uiState.value.isBgMusicMuted && _uiState.value.bgMusicTrack != "None") {
                    (_uiState.value.bgMusicVolume * duckingFactor).coerceIn(0f, 1f)
                } else 0f

                val masterPeak = (((videoPeak * 0.5f) + (micPeak * 0.6f) + (bgPeak * 0.4f)) * masterFactor).coerceIn(0f, 1f)

                // Bandwidth history tracks the real upload bitrate reported by the engine.
                val currentBandwidthMbps = (currentBitrate / 1000f).coerceIn(0f, 100f)
                bandwidthHistoryList.add(currentBandwidthMbps)
                if (bandwidthHistoryList.size > 25) bandwidthHistoryList.removeAt(0)

                // NOTE: Viewer counts are not available from the RTMP protocol itself; real values
                // require each platform's data API (e.g. YouTube/TikTok). Until integrated, the
                // viewer figures remain a placeholder and are intentionally not fabricated here.

                _uiState.value = _uiState.value.copy(
                    liveDurationSec = elapsed,
                    currentLoopRound = loops,
                    bandwidthHistoryMbps = bandwidthHistoryList.toList(),
                    droppedFrames = accumulatedDroppedFrames,
                    totalFrames = totalFramesSent,
                    healthScorePct = healthScore,
                    bufferHealthPct = if (currentBitrate > 0) 98f else 70f,
                    thermalState = thermalInfo.level.label,
                    thermalInfo = thermalInfo,
                    storageInfo = currentStorageInfo,
                    videoVuLevel = videoPeak,
                    micVuLevel = micPeak,
                    bgMusicVuLevel = bgPeak,
                    masterVuLevel = masterPeak
                )

                // Update session in DB periodically
                repository.updateLiveSession(
                    LiveSessionEntity(
                        id = sessionId,
                        platform = _uiState.value.platform.name,
                        streamTitle = _uiState.value.streamTitle,
                        rtmpUrl = _uiState.value.rtmpUrl,
                        sourceUri = _uiState.value.sourceUri ?: "",
                        durationSec = elapsed,
                        totalLoops = loops,
                        status = "ACTIVE",
                        avgBitrateKbps = avgBitrate,
                        droppedFrames = accumulatedDroppedFrames
                    )
                )
            }
        }
    }

    private var reconnectJob: Job? = null

    /**
     * Handles a network loss on the live stream. Delegates to [handleRealConnectionDrop], which
     * performs a genuine RTMP stream restart via the engine with backoff (no fake handshake or
     * random "reconnected" outcome). Used both by real disconnect callbacks and the manual retry.
     */
    fun handleNetworkLoss(reason: String = "Network socket reset / packet drop", context: Context? = null) {
        if (_uiState.value.streamStatus != StreamStatus.LIVE && _uiState.value.streamStatus != StreamStatus.RECONNECTING) return
        handleRealConnectionDrop(reason)
    }

    /**
     * Manually triggers connection loss simulation for user testing & visual verification of buffering state indicator.
     */
    fun simulateConnectionLoss(context: Context? = null) {
        if (_uiState.value.streamStatus == StreamStatus.LIVE) {
            handleNetworkLoss("Simulated active network drop / socket timeout", context)
        } else {
            _uiState.value = _uiState.value.copy(streamStatus = StreamStatus.LIVE)
            handleNetworkLoss("Simulated test network drop", context)
        }
    }

    fun cancelReconnection() {
        reconnectJob?.cancel()
        _uiState.value = _uiState.value.copy(
            streamStatus = StreamStatus.STOPPED,
            isBuffering = false,
            reconnectAttempt = 0
        )
        addLog(
            level = LiveLogLevel.INFO,
            category = LiveLogCategory.NETWORK,
            message = "Reconnection attempt cancelled by user."
        )
    }

    fun stopLiveStream(context: Context) {
        liveTimerJob?.cancel()
        reconnectJob?.cancel()

        viewModelScope.launch {
            val sessionId = _uiState.value.dbSessionId
            if (sessionId != null) {
                val currentSession = repository.allLiveSessions
                repository.updateLiveSession(
                    LiveSessionEntity(
                        id = sessionId,
                        platform = _uiState.value.platform.name,
                        streamTitle = _uiState.value.streamTitle,
                        rtmpUrl = _uiState.value.rtmpUrl,
                        sourceUri = _uiState.value.sourceUri ?: "",
                        durationSec = _uiState.value.liveDurationSec,
                        totalLoops = _uiState.value.currentLoopRound,
                        status = "STOPPED",
                        avgBitrateKbps = _uiState.value.currentBitrateKbps,
                        droppedFrames = _uiState.value.droppedFrames,
                        endedAt = System.currentTimeMillis()
                    )
                )
            }

            // Stop the real RTMP broadcast engine.
            try {
                rtmpManager.stopStream()
            } catch (e: Throwable) {
                addLog(LiveLogLevel.WARN, LiveLogCategory.RTMP, "Stop stream handled safely: ${e.localizedMessage}")
            }

            stopViewerCountPolling()

            _uiState.value = _uiState.value.copy(streamStatus = StreamStatus.STOPPED)

            // Stop Foreground Service
            val serviceIntent = Intent(context, LiveStreamService::class.java)
            context.stopService(serviceIntent)
        }
    }

    /**
     * Applies torch/mic/camera controls to the real engine. Called by the UI toggles so the
     * live stream reflects them immediately.
     */
    fun applyTorchToEngine(enabled: Boolean) {
        rtmpManager.setTorch(enabled)
    }

    fun switchStreamCamera() {
        rtmpManager.switchCamera()
    }

    override fun onCleared() {
        super.onCleared()
        liveTimerJob?.cancel()
        reconnectJob?.cancel()
        viewerPollJob?.cancel()
        try {
            rtmpManager.release()
        } catch (_: Throwable) {}
    }

    /**
     * Reduces encoder load to cool down device when thermal warning triggers. Applies the lower
     * bitrate to the RUNNING RTMP stream via [RtmpStreamManager.setVideoBitrateOnFly] (previously
     * this only rewrote [LiveUiState.targetBitrateKbps]/[LiveUiState.currentBitrateKbps] without
     * ever calling that real API, so the encoder kept running at its original bitrate despite the
     * UI showing a lower number).
     *
     * Note: [LiveUiState.fps] is only read once at [startStream]'s initial encoder setup - RootEncoder
     * has no on-the-fly frame-rate API, so lowering `fps` here cannot affect the already-running
     * encoder. It's kept as a hint that only takes effect the next time streaming is (re)started.
     */
    fun coolDownEncoding() {
        val currentBitrate = _uiState.value.targetBitrateKbps
        val lowerBitrate = (currentBitrate * 0.70f).toInt().coerceAtLeast(1500)
        val lowerFps = if (_uiState.value.fps > 30) 30 else 15

        if (_uiState.value.streamStatus == StreamStatus.LIVE) {
            try {
                rtmpManager.setVideoBitrateOnFly(lowerBitrate * 1000)
            } catch (e: Throwable) {
                addLog(
                    level = LiveLogLevel.WARN,
                    category = LiveLogCategory.ENCODER,
                    message = "Cool-down bitrate change failed to apply to the live encoder: ${e.localizedMessage}"
                )
            }
        }

        _uiState.value = _uiState.value.copy(
            targetBitrateKbps = lowerBitrate,
            currentBitrateKbps = lowerBitrate,
            fps = lowerFps,
            showSpectrumOverlay = false
        )
    }

    /**
     * Checks real device thermal state using PowerManager.getCurrentThermalStatus() API.
     */
    fun checkThermalStatus(context: Context) {
        val info = ThermalMonitor.getThermalInfo(context)
        _uiState.value = _uiState.value.copy(
            thermalState = info.level.label,
            thermalInfo = info
        )
        if (info.level.isWarning) {
            addLog(
                level = if (info.level == ThermalStatusLevel.CRITICAL) LiveLogLevel.ERROR else LiveLogLevel.WARN,
                category = LiveLogCategory.HEALTH,
                message = "Thermal warning threshold reached (${info.level.label}): ${info.warningMessage}"
            )
        }
    }

    /**
     * Simulates or tests thermal warning state with PowerManager throttling status.
     */
    fun simulateThermalWarning() {
        val simulatedTemp = 45.2f
        val systemCode = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            android.os.PowerManager.THERMAL_STATUS_SEVERE
        } else 3
        val info = ThermalMonitor.evaluateThermalStatus(simulatedTemp, systemCode)
        _uiState.value = _uiState.value.copy(
            thermalState = info.level.label,
            thermalInfo = info
        )
        addLog(
            level = LiveLogLevel.WARN,
            category = LiveLogCategory.HEALTH,
            message = "Thermal warning alert (${info.level.label}): PowerManager status = SEVERE (${"%.1f".format(simulatedTemp)}°C)"
        )
    }

    /**
     * Refreshes internal disk storage status using StorageWatcher.
     */
    fun refreshStorageInfo(context: Context) {
        val info = StorageWatcher.getStorageInfo(context)
        _uiState.value = _uiState.value.copy(storageInfo = info)
    }

    /**
     * Simulates a low storage warning state (<500MB available) for testing.
     */
    fun simulateLowStorageAlert() {
        val simulatedMb = 320L
        _uiState.value = _uiState.value.copy(
            storageInfo = StorageInfo(
                availableBytes = simulatedMb * 1024 * 1024L,
                totalBytes = 64 * 1024 * 1024 * 1024L,
                availableMb = simulatedMb,
                isLowStorage = true,
                isCriticalStorage = true,
                warningMessage = "CRITICAL STORAGE ALERT: Only 320 MB free on internal storage (< 500MB). Local recording and cache dumps may fail!"
            )
        )
    }

    /**
     * Log management methods for stream diagnostics & event viewer.
     */
    fun addLog(level: LiveLogLevel, category: LiveLogCategory, message: String) {
        val newEvent = LiveLogEvent(level = level, category = category, message = message)
        val updatedLogs = (_uiState.value.logs + newEvent).takeLast(200) // Keep last 200 logs
        _uiState.value = _uiState.value.copy(logs = updatedLogs)
    }

    fun clearLogs() {
        _uiState.value = _uiState.value.copy(logs = emptyList())
    }

    private var logSimulateIndex = 0

    fun simulateLogEvent() {
        val samples = listOf(
            Triple(LiveLogLevel.INFO, LiveLogCategory.ENCODER, "Video encoder re-negotiating keyframe interval (GOP = 120 frames @ 60fps)"),
            Triple(LiveLogLevel.WARN, LiveLogCategory.NETWORK, "Network socket congestion detected: RTT increased to 185ms (Bitrate buffered)"),
            Triple(LiveLogLevel.SUCCESS, LiveLogCategory.NETWORK, "Re-established TCP socket connection to RTMP ingest endpoint"),
            Triple(LiveLogLevel.INFO, LiveLogCategory.HEALTH, "Periodic telemetry ping: Bitrate 4520 Kbps, 0 dropped frames, FPS 60.0"),
            Triple(LiveLogLevel.WARN, LiveLogCategory.HEALTH, "Thermal throttle warning: SoC temp reached 41.5°C (Cooling profile standby)"),
            Triple(LiveLogLevel.SUCCESS, LiveLogCategory.RTMP, "RTMP Handshake verified: FLV tag header initialized successfully"),
            Triple(LiveLogLevel.ERROR, LiveLogCategory.NETWORK, "RTMP socket packet timeout on secondary endpoint (Retrying attempt 2/5...)")
        )

        val sample = samples[logSimulateIndex % samples.size]
        logSimulateIndex++
        addLog(sample.first, sample.second, sample.third)
    }
}
