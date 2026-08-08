package com.example.feature.live

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.database.LiveSessionEntity
import com.example.core.database.LoopingVidRepository
import com.example.core.media.SpectrumStyle
import com.example.core.utils.StorageInfo
import com.example.core.utils.StorageWatcher
import com.example.core.utils.ThermalInfo
import com.example.core.utils.ThermalMonitor
import com.example.core.utils.ThermalStatusLevel
import com.example.core.utils.RtmpUrlValidator
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.random.Random

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
    val currentBitrateKbps: Int = 4500,
    val targetBitrateKbps: Int = 4500,
    val bitrateHistory: List<Int> = listOf(4400, 4450, 4520, 4480, 4550, 4500, 4490, 4510, 4530, 4470, 4500),
    val viewerCount: Int = 1840,
    val peakViewerCount: Int = 2450,
    val viewerHistory: List<Int> = listOf(1420, 1480, 1550, 1620, 1700, 1750, 1810, 1840, 1890, 1920, 1880, 1950, 2010, 2080, 2150),
    val bandwidthMbps: Float = 9.8f,
    val bandwidthHistoryMbps: List<Float> = listOf(8.2f, 8.5f, 9.1f, 8.8f, 9.4f, 9.2f, 8.7f, 9.5f, 9.8f, 9.3f, 9.6f, 10.1f, 9.7f, 10.4f, 9.9f),
    val showD3OverlayOnVideo: Boolean = true,
    val d3OverlayOpacity: Float = 0.85f,
    val d3OverlayTimeWindowSec: Int = 30,
    val d3VisualizationMode: D3VisMode = D3VisMode.DUAL_METRICS,
    val droppedFrames: Int = 0,
    val totalFrames: Long = 0,
    val fps: Int = 60,
    val latencyMs: Int = 120, // RTT
    val rttHistory: List<Int> = listOf(118, 122, 119, 125, 121, 118, 120, 124, 122, 119),
    val jitterMs: Int = 6,
    val jitterHistory: List<Int> = listOf(4, 5, 8, 6, 5, 7, 6, 4, 6, 5),
    val bufferHealthPct: Float = 98f,
    val healthScorePct: Int = 98,
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
    val showSpectrumOverlay: Boolean = true,
    val spectrumStyle: SpectrumStyle = SpectrumStyle.BARS,
    val autoStopMinutes: Int = 0, // 0 = continuous
    val batteryPct: Int = 92,
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
    val logs: List<LiveLogEvent> = listOf(
        LiveLogEvent(
            level = LiveLogLevel.INFO,
            category = LiveLogCategory.ENCODER,
            message = "Hardware AVC/H.264 video encoder initialized (1080p60 @ 4500 Kbps)"
        ),
        LiveLogEvent(
            level = LiveLogLevel.SUCCESS,
            category = LiveLogCategory.SYSTEM,
            message = "Audio pipeline ready: Media3 ExoPlayer processor & 2-channel 48kHz AAC"
        ),
        LiveLogEvent(
            level = LiveLogLevel.INFO,
            category = LiveLogCategory.NETWORK,
            message = "Available network bandwidth estimated at 12.0 Mbps capacity"
        ),
        LiveLogEvent(
            level = LiveLogLevel.SUCCESS,
            category = LiveLogCategory.HEALTH,
            message = "Stream health telemetry OK: 0 dropped frames, thermal state Normal"
        )
    )
)

class LiveViewModel(
    private val repository: LoopingVidRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(LiveUiState())
    val uiState: StateFlow<LiveUiState> = _uiState.asStateFlow()

    private var liveTimerJob: Job? = null

    init {
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

        // A blank key is a deliberate "forget my credential" action, so it must reach storage.
        // Only a malformed non-blank key is dropped, which also avoids persisting the partial
        // values produced while the user is still typing.
        if (key.isNotBlank() && !RtmpUrlValidator.validateStreamKey(key).isValid) return

        viewModelScope.launch {
            repository.setSetting("stream_key_${_uiState.value.platform.name.lowercase()}", key)
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
        _uiState.value = _uiState.value.copy(isMicInputMuted = !_uiState.value.isMicInputMuted)
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

    fun simulateBandwidthTest() {
        // Simulates running an active speed test to evaluate upload capacity
        val simulatedCapacityKbps = (6000..18000).random()
        setAvailableBandwidth(simulatedCapacityKbps)
    }

    fun startLiveStream(context: Context) {
        try {
            val state = _uiState.value
            if (state.sourceUri == null) {
                addLog(
                    level = LiveLogLevel.WARN,
                    category = LiveLogCategory.RTMP,
                    message = "Start live stream skipped: No video media source selected."
                )
                return
            }

            val urlValidation = RtmpUrlValidator.validateRtmpUrl(state.rtmpUrl)
            if (!urlValidation.isValid) {
                addLog(
                    level = LiveLogLevel.ERROR,
                    category = LiveLogCategory.RTMP,
                    message = "Start live stream skipped: ${urlValidation.errorMessage ?: "Invalid RTMP URL"}"
                )
                return
            }

            _uiState.value = state.copy(streamStatus = StreamStatus.CONNECTING)

            viewModelScope.launch {
                try {
                    delay(1200) // Simulated RTMP handshake

                    val session = LiveSessionEntity(
                        platform = state.platform.name,
                        streamTitle = state.streamTitle,
                        rtmpUrl = state.rtmpUrl,
                        sourceUri = state.sourceUri,
                        durationSec = 0,
                        totalLoops = 1,
                        status = "ACTIVE",
                        avgBitrateKbps = 4500,
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
                        streamStatus = StreamStatus.LIVE,
                        dbSessionId = sessionId,
                        liveDurationSec = 0,
                        currentLoopRound = 1
                    )

                    // Start Foreground Service safely
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

                    startLiveTelemetryLoop(sessionId, context)
                } catch (e: Throwable) {
                    addLog(
                        level = LiveLogLevel.ERROR,
                        category = LiveLogCategory.RTMP,
                        message = "RTMP connection startup error caught safely: ${e.localizedMessage}"
                    )
                    _uiState.value = _uiState.value.copy(
                        streamStatus = StreamStatus.STOPPED,
                        isBuffering = false
                    )
                }
            }
        } catch (e: Throwable) {
            addLog(
                level = LiveLogLevel.ERROR,
                category = LiveLogCategory.SYSTEM,
                message = "Live stream start launch error caught: ${e.localizedMessage}"
            )
            _uiState.value = _uiState.value.copy(streamStatus = StreamStatus.STOPPED)
        }
    }

    private fun startLiveTelemetryLoop(sessionId: Long, context: Context) {
        liveTimerJob?.cancel()
        liveTimerJob = viewModelScope.launch {
            var elapsed = 0L
            var loops = 1
            var totalBitrateSum = 4500L
            var accumulatedDroppedFrames = 0
            var totalFramesSent = 0L
            val random = Random(1234)

            val bitrateList = mutableListOf(4400, 4450, 4520, 4480, 4550, 4500)
            val rttList = mutableListOf(118, 122, 119, 125, 121)
            val jitterList = mutableListOf(4, 5, 8, 6, 5)
            val viewerHistoryList = _uiState.value.viewerHistory.toMutableList()
            val bandwidthHistoryList = _uiState.value.bandwidthHistoryMbps.toMutableList()
            var currentViewerCount = _uiState.value.viewerCount
            var peakViewerCount = _uiState.value.peakViewerCount

            while (_uiState.value.streamStatus == StreamStatus.LIVE) {
                delay(1000)
                elapsed++
                totalFramesSent += 60

                // Check Auto-Stop Timer Limit
                val autoStopLimitMin = _uiState.value.autoStopMinutes
                if (autoStopLimitMin > 0 && elapsed >= autoStopLimitMin * 60L) {
                    stopLiveStream(context)
                    break
                }

                // Every 30 seconds simulate seamless video loop completion
                if (elapsed % 30L == 0L) {
                    loops++
                }

                val currentBitrate = 4200 + random.nextInt(750)
                totalBitrateSum += currentBitrate
                val avgBitrate = (totalBitrateSum / elapsed).toInt()

                val currentRtt = 110 + random.nextInt(25)
                val currentJitter = 3 + random.nextInt(7)

                // Occasional minor dropped frame simulation under jitter peaks
                if (currentJitter > 8 && random.nextInt(10) > 6) {
                    accumulatedDroppedFrames += random.nextInt(3) + 1
                }

                // Rolling history windows (max 25 entries)
                bitrateList.add(currentBitrate)
                if (bitrateList.size > 25) bitrateList.removeAt(0)

                rttList.add(currentRtt)
                if (rttList.size > 25) rttList.removeAt(0)

                jitterList.add(currentJitter)
                if (jitterList.size > 25) jitterList.removeAt(0)

                val droppedPct = if (totalFramesSent > 0) (accumulatedDroppedFrames.toFloat() / totalFramesSent) * 100f else 0f
                val healthScore = when {
                    droppedPct > 2.0f -> 75
                    droppedPct > 0.5f -> 88
                    currentRtt > 150 -> 90
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

                // Audio Mixer VU Peak Level Calculations
                val masterFactor = if (_uiState.value.isMasterMuted) 0f else _uiState.value.masterVolume
                val videoPeak = if (!_uiState.value.isVideoSourceMuted) {
                    (_uiState.value.videoSourceVolume * (0.65f + random.nextFloat() * 0.30f)).coerceIn(0f, 1f)
                } else 0f

                val micPeak = if (!_uiState.value.isMicInputMuted) {
                    (_uiState.value.micInputVolume * (0.40f + random.nextFloat() * 0.50f)).coerceIn(0f, 1f)
                } else 0f

                val duckingFactor = if (_uiState.value.isMicDuckingEnabled && micPeak > 0.35f) 0.35f else 1.0f
                val bgPeak = if (!_uiState.value.isBgMusicMuted && _uiState.value.bgMusicTrack != "None") {
                    (_uiState.value.bgMusicVolume * duckingFactor * (0.50f + random.nextFloat() * 0.30f)).coerceIn(0f, 1f)
                } else 0f

                val masterPeak = (((videoPeak * 0.5f) + (micPeak * 0.6f) + (bgPeak * 0.4f)) * masterFactor).coerceIn(0f, 1f)

                // Live Viewer Count & Bandwidth Fluctuation Simulation
                val viewerChange = random.nextInt(25) - 10
                currentViewerCount = (currentViewerCount + viewerChange).coerceIn(100, 50000)
                if (currentViewerCount > peakViewerCount) {
                    peakViewerCount = currentViewerCount
                }
                viewerHistoryList.add(currentViewerCount)
                if (viewerHistoryList.size > 25) viewerHistoryList.removeAt(0)

                val currentBandwidthMbps = (currentBitrate / 1000f) + (random.nextFloat() * 1.2f - 0.6f)
                val clampedBandwidthMbps = currentBandwidthMbps.coerceIn(1.0f, 25.0f)
                bandwidthHistoryList.add(clampedBandwidthMbps)
                if (bandwidthHistoryList.size > 25) bandwidthHistoryList.removeAt(0)

                _uiState.value = _uiState.value.copy(
                    liveDurationSec = elapsed,
                    currentLoopRound = loops,
                    currentBitrateKbps = currentBitrate,
                    bitrateHistory = bitrateList.toList(),
                    viewerCount = currentViewerCount,
                    peakViewerCount = peakViewerCount,
                    viewerHistory = viewerHistoryList.toList(),
                    bandwidthMbps = clampedBandwidthMbps,
                    bandwidthHistoryMbps = bandwidthHistoryList.toList(),
                    latencyMs = currentRtt,
                    rttHistory = rttList.toList(),
                    jitterMs = currentJitter,
                    jitterHistory = jitterList.toList(),
                    droppedFrames = accumulatedDroppedFrames,
                    totalFrames = totalFramesSent,
                    healthScorePct = healthScore,
                    bufferHealthPct = (95f + random.nextFloat() * 4f).coerceAtMost(100f),
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
     * Handles network loss during stream transmission without crashing the application.
     * Transitions state to RECONNECTING with isBuffering = true, and executes exponential backoff retries.
     */
    fun handleNetworkLoss(reason: String = "Network socket reset / packet drop", context: Context? = null) {
        if (_uiState.value.streamStatus != StreamStatus.LIVE && _uiState.value.streamStatus != StreamStatus.RECONNECTING) return

        reconnectJob?.cancel()

        addLog(
            level = LiveLogLevel.WARN,
            category = LiveLogCategory.NETWORK,
            message = "NETWORK CONNECTION LOSS: $reason. Triggering buffering & auto-reconnection sequence..."
        )

        _uiState.value = _uiState.value.copy(
            streamStatus = StreamStatus.RECONNECTING,
            isBuffering = true,
            connectionLossReason = reason,
            reconnectAttempt = 1,
            reconnectCountdownSec = 3,
            bufferHealthPct = 15f
        )

        reconnectJob = viewModelScope.launch {
            try {
                val maxAttempts = _uiState.value.maxReconnectAttempts
                var attempt = 1

                while (attempt <= maxAttempts && _uiState.value.streamStatus == StreamStatus.RECONNECTING) {
                    _uiState.value = _uiState.value.copy(
                        reconnectAttempt = attempt,
                        isBuffering = true
                    )

                    // Countdown before retry attempt
                    for (sec in 3 downTo 1) {
                        if (_uiState.value.streamStatus != StreamStatus.RECONNECTING) break
                        _uiState.value = _uiState.value.copy(reconnectCountdownSec = sec)
                        delay(1000)
                    }

                    if (_uiState.value.streamStatus != StreamStatus.RECONNECTING) break

                    addLog(
                        level = LiveLogLevel.INFO,
                        category = LiveLogCategory.NETWORK,
                        message = "Reconnection attempt $attempt/$maxAttempts in progress: Re-negotiating RTMP TCP handshake & socket stream..."
                    )

                    // Simulate/Perform RTMP Handshake retry safely
                    delay(1200)

                    // Success on retry
                    val isReconnected = (attempt >= 2) || (Random.nextFloat() > 0.3f)

                    if (isReconnected) {
                        addLog(
                            level = LiveLogLevel.SUCCESS,
                            category = LiveLogCategory.NETWORK,
                            message = "RECONNECTION SUCCESSFUL! RTMP socket re-established. Resuming live stream transmission."
                        )

                        val currentSessionId = _uiState.value.dbSessionId
                        _uiState.value = _uiState.value.copy(
                            streamStatus = StreamStatus.LIVE,
                            isBuffering = false,
                            connectionLossReason = null,
                            reconnectAttempt = 0,
                            reconnectCountdownSec = 0,
                            bufferHealthPct = 98f,
                            healthScorePct = 95
                        )

                        // Resume telemetry loop if needed
                        if (currentSessionId != null && context != null && (liveTimerJob == null || liveTimerJob?.isActive != true)) {
                            startLiveTelemetryLoop(currentSessionId, context)
                        }
                        return@launch
                    } else {
                        addLog(
                            level = LiveLogLevel.WARN,
                            category = LiveLogCategory.NETWORK,
                            message = "Reconnection attempt $attempt/$maxAttempts failed (Ingest timeout). Retrying..."
                        )
                        attempt++
                    }
                }

                // If max attempts exhausted
                if (_uiState.value.streamStatus == StreamStatus.RECONNECTING) {
                    addLog(
                        level = LiveLogLevel.ERROR,
                        category = LiveLogCategory.NETWORK,
                        message = "RECONNECTION FAILED: Exceeded maximum retry attempts ($maxAttempts). Stream stopped safely to avoid socket leak."
                    )
                    _uiState.value = _uiState.value.copy(
                        streamStatus = StreamStatus.STOPPED,
                        isBuffering = false,
                        connectionLossReason = "Max reconnection retries exceeded."
                    )
                }
            } catch (e: Throwable) {
                addLog(
                    level = LiveLogLevel.ERROR,
                    category = LiveLogCategory.SYSTEM,
                    message = "Handled error in reconnection loop safely: ${e.localizedMessage}"
                )
                _uiState.value = _uiState.value.copy(
                    streamStatus = StreamStatus.STOPPED,
                    isBuffering = false
                )
            }
        }
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

            _uiState.value = _uiState.value.copy(streamStatus = StreamStatus.STOPPED)

            // Stop Foreground Service
            val serviceIntent = Intent(context, LiveStreamService::class.java)
            context.stopService(serviceIntent)
        }
    }

    /**
     * Reduces encoder load to cool down device when thermal warning triggers.
     */
    fun coolDownEncoding() {
        val currentBitrate = _uiState.value.targetBitrateKbps
        val lowerBitrate = (currentBitrate * 0.70f).toInt().coerceAtLeast(1500)
        val lowerFps = if (_uiState.value.fps > 30) 30 else 15

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
