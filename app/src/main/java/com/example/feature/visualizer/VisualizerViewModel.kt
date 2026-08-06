package com.example.feature.visualizer

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.media.Media3SpectrumAudioProcessor
import com.example.feature.visualizer.beat.BeatDetectionConfig
import com.example.feature.visualizer.beat.BeatDetectionEngine
import com.example.feature.visualizer.beat.BeatEffect
import com.example.feature.visualizer.beat.BeatMarkerEditor
import com.example.feature.visualizer.beat.BeatPulseCalculator
import com.example.feature.visualizer.beat.BeatSyncState
import com.example.feature.visualizer.beat.FrequencyBand
import com.example.feature.visualizer.beat.PcmDecoder
import com.example.feature.visualizer.beat.TapBpmDetector
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class VisualizerUiState(
    val audioUri: String? = null,
    val audioName: String = "",
    val isPlaying: Boolean = false,
    val config: VisualizerRenderConfig = VisualizerRenderConfig(),
    val magnitudes: FloatArray = FloatArray(32) { 0f },
    val peakDb: Float = -60f,
    val rmsEnergy: Float = 0f,
    val dominantFreqHz: Int = 0,
    val beatSync: BeatSyncState = BeatSyncState(),
    val isExporting: Boolean = false,
    val exportProgress: Int = 0,
    val exportStatus: String = ""
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is VisualizerUiState) return false
        return audioUri == other.audioUri &&
            audioName == other.audioName &&
            isPlaying == other.isPlaying &&
            config == other.config &&
            magnitudes.contentEquals(other.magnitudes) &&
            peakDb == other.peakDb &&
            rmsEnergy == other.rmsEnergy &&
            dominantFreqHz == other.dominantFreqHz &&
            beatSync == other.beatSync &&
            isExporting == other.isExporting &&
            exportProgress == other.exportProgress &&
            exportStatus == other.exportStatus
    }

    override fun hashCode(): Int {
        var result = audioUri?.hashCode() ?: 0
        result = 31 * result + magnitudes.contentHashCode()
        result = 31 * result + config.hashCode()
        result = 31 * result + isPlaying.hashCode()
        return result
    }
}

/**
 * ViewModel for Visualizer Studio. Drives real-time FFT preview using the
 * same [Media3SpectrumAudioProcessor] as the Editor module, but with its own
 * [VisualizerRenderConfig] state tree.
 */
class VisualizerViewModel(
    private val appContext: Context? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(VisualizerUiState())
    val uiState: StateFlow<VisualizerUiState> = _uiState.asStateFlow()

    val spectrumProcessor = Media3SpectrumAudioProcessor()
    private val tapBpmDetector = TapBpmDetector()
    private var decodedDurationMs: Long = 0L

    init {
        spectrumProcessor.onSpectrumDataListener = { magnitudes, peakDb, rms ->
            _uiState.update {
                it.copy(
                    magnitudes = magnitudes.copyOf(),
                    peakDb = peakDb,
                    rmsEnergy = rms,
                    dominantFreqHz = spectrumProcessor.dominantFrequencyHz
                )
            }
        }
    }

    // --- Audio Source ---

    fun setAudioSource(uri: String, name: String) {
        _uiState.update {
            it.copy(
                audioUri = uri,
                audioName = name,
                isPlaying = true,
                beatSync = BeatSyncState() // Reset any previous track's analysis.
            )
        }
        analyzeBeats(uri)
    }

    fun setPlaying(playing: Boolean) {
        _uiState.update { it.copy(isPlaying = playing) }
    }

    /** Updates the beat pulse envelope for the given playback position. Call every preview frame. */
    fun updatePlaybackPosition(positionMs: Long) {
        val beatSync = _uiState.value.beatSync
        if (!beatSync.hasAnalysis) return
        val pulse = BeatPulseCalculator.pulseAt(
            positionMs = positionMs,
            markersMs = beatSync.markersMs,
            attackMs = beatSync.config.attackMs,
            releaseMs = beatSync.config.releaseMs,
            strength = beatSync.config.effectStrength
        )
        _uiState.update { it.copy(beatSync = it.beatSync.copy(currentPulse = pulse)) }
    }

    // --- Beat Sync ---

    private fun analyzeBeats(uri: String) {
        val context = appContext ?: return
        _uiState.update { it.copy(beatSync = it.beatSync.copy(isAnalyzing = true, analysisError = null)) }

        viewModelScope.launch {
            val decoded = PcmDecoder.decode(context, uri)
            decoded.fold(
                onSuccess = { pcm ->
                    decodedDurationMs = pcm.durationMs
                    val config = _uiState.value.beatSync.config
                    val result = BeatDetectionEngine.analyze(pcm.samples, pcm.sampleRate, config)
                    _uiState.update {
                        it.copy(
                            beatSync = it.beatSync.copy(
                                isAnalyzing = false,
                                bpm = result.bpm,
                                durationMs = pcm.durationMs,
                                markersMs = result.markersMs,
                                analysisError = if (result.markersMs.isEmpty()) {
                                    "Tidak ada beat terdeteksi. Coba turunkan threshold atau gunakan Tap BPM."
                                } else null
                            )
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            beatSync = it.beatSync.copy(
                                isAnalyzing = false,
                                analysisError = error.localizedMessage ?: "Analisis beat gagal"
                            )
                        )
                    }
                }
            )
        }
    }

    fun reanalyzeBeats() {
        _uiState.value.audioUri?.let { analyzeBeats(it) }
    }

    fun setBeatBand(band: FrequencyBand) {
        updateBeatConfig { it.copy(band = band) }
    }

    fun setBeatSensitivity(value: Float) {
        updateBeatConfig { it.copy(sensitivity = value.coerceIn(0.1f, 4.0f)) }
    }

    fun setBeatThreshold(value: Float) {
        updateBeatConfig { it.copy(threshold = value.coerceIn(0.05f, 1.0f)) }
    }

    fun setBeatSmoothing(value: Float) {
        updateBeatConfig { it.copy(smoothing = value.coerceIn(0f, 1f)) }
    }

    fun setBeatAttack(ms: Long) {
        updateBeatConfig { it.copy(attackMs = ms.coerceIn(1L, 500L)) }
    }

    fun setBeatRelease(ms: Long) {
        updateBeatConfig { it.copy(releaseMs = ms.coerceIn(10L, 2000L)) }
    }

    fun setBeatOffset(ms: Long) {
        val previous = _uiState.value.beatSync.config.offsetMs
        val delta = ms - previous
        updateBeatConfig { it.copy(offsetMs = ms) }
        _uiState.update {
            it.copy(
                beatSync = it.beatSync.copy(
                    markersMs = BeatMarkerEditor.applyOffset(it.beatSync.markersMs, delta, it.beatSync.durationMs)
                )
            )
        }
    }

    fun setBeatMinInterval(ms: Long) {
        updateBeatConfig { it.copy(minIntervalMs = ms.coerceIn(50L, 2000L)) }
    }

    fun setBeatEffectStrength(value: Float) {
        updateBeatConfig { it.copy(effectStrength = value.coerceIn(0f, 2f)) }
    }

    fun setSelectedBeatEffect(effect: BeatEffect) {
        _uiState.update { it.copy(beatSync = it.beatSync.copy(selectedEffect = effect)) }
    }

    fun addBeatMarker(markerMs: Long) {
        _uiState.update { state ->
            state.copy(
                beatSync = state.beatSync.copy(
                    markersMs = BeatMarkerEditor.add(state.beatSync.markersMs, markerMs, state.beatSync.durationMs)
                )
            )
        }
    }

    fun moveBeatMarker(index: Int, markerMs: Long) {
        _uiState.update { state ->
            state.copy(
                beatSync = state.beatSync.copy(
                    markersMs = BeatMarkerEditor.move(state.beatSync.markersMs, index, markerMs, state.beatSync.durationMs)
                )
            )
        }
    }

    fun removeBeatMarker(index: Int) {
        _uiState.update { state ->
            state.copy(beatSync = state.beatSync.copy(markersMs = BeatMarkerEditor.remove(state.beatSync.markersMs, index)))
        }
    }

    /** Records a manual tap and, once at least two taps exist, regenerates the marker grid from the live BPM. */
    fun tapBpm(timestampMs: Long) {
        tapBpmDetector.tap(timestampMs)
        val bpm = tapBpmDetector.currentBpm()
        if (bpm <= 0.0) return

        val duration = decodedDurationMs.takeIf { it > 0 } ?: _uiState.value.beatSync.durationMs
        applyManualBpm(bpm, duration)
    }

    fun resetTapBpm() {
        tapBpmDetector.reset()
    }

    /** Applies a manually typed BPM value, replacing detected markers with an even grid. */
    fun setManualBpm(bpm: Double) {
        val duration = decodedDurationMs.takeIf { it > 0 } ?: _uiState.value.beatSync.durationMs
        applyManualBpm(bpm, duration)
    }

    private fun applyManualBpm(bpm: Double, durationMs: Long) {
        val offset = _uiState.value.beatSync.config.offsetMs
        val grid = BeatDetectionEngine.gridFromBpm(bpm, durationMs.coerceAtLeast(1000L), offset)
        _uiState.update {
            it.copy(
                beatSync = it.beatSync.copy(
                    bpm = bpm,
                    durationMs = if (durationMs > 0) durationMs else it.beatSync.durationMs,
                    markersMs = grid,
                    analysisError = null
                )
            )
        }
    }

    private inline fun updateBeatConfig(transform: (BeatDetectionConfig) -> BeatDetectionConfig) {
        _uiState.update { it.copy(beatSync = it.beatSync.copy(config = transform(it.beatSync.config))) }
    }

    // --- Config Mutations ---

    fun setMode(mode: VisualizerMode) {
        updateConfig { it.copy(mode = mode) }
    }

    fun setBandCount(count: Int) {
        val safe = count.coerceIn(8, 64)
        spectrumProcessor.bandCount = safe
        updateConfig { it.copy(bandCount = safe) }
    }

    fun setSensitivity(gain: Float) {
        val safe = gain.coerceIn(0.2f, 4.0f)
        spectrumProcessor.sensitivityGain = safe
        updateConfig { it.copy(sensitivityGain = safe) }
    }

    fun setSmoothing(value: Float) {
        updateConfig { it.copy(smoothing = value.coerceIn(0f, 1f)) }
    }

    fun setSizeScale(scale: Float) {
        updateConfig { it.copy(sizeScale = scale.coerceIn(0.3f, 2.0f)) }
    }

    fun setGapScale(scale: Float) {
        updateConfig { it.copy(gapScale = scale.coerceIn(0.0f, 3.0f)) }
    }

    fun setThickness(value: Float) {
        updateConfig { it.copy(thickness = value.coerceIn(1f, 20f)) }
    }

    fun setCornerRadius(value: Float) {
        updateConfig { it.copy(cornerRadius = value.coerceIn(0f, 20f)) }
    }

    fun setOffset(x: Float, y: Float) {
        updateConfig { it.copy(offsetX = x.coerceIn(-1f, 1f), offsetY = y.coerceIn(-1f, 1f)) }
    }

    fun setRotation(degrees: Float) {
        updateConfig { it.copy(rotationDegrees = degrees % 360f) }
    }

    fun setOpacity(value: Float) {
        updateConfig { it.copy(opacity = value.coerceIn(0f, 1f)) }
    }

    fun setGlow(radius: Float) {
        updateConfig { it.copy(glowRadius = radius.coerceIn(0f, 24f)) }
    }

    fun setShadow(radius: Float) {
        updateConfig { it.copy(shadowRadius = radius.coerceIn(0f, 16f)) }
    }

    fun setTrailFade(value: Float) {
        updateConfig { it.copy(trailFade = value.coerceIn(0f, 1f)) }
    }

    fun setPrimaryColor(color: Color) {
        updateConfig { it.copy(primaryColor = color) }
    }

    fun setSecondaryColor(color: Color) {
        updateConfig { it.copy(secondaryColor = color) }
    }

    fun setBackground(background: VisualizerBackground) {
        updateConfig { it.copy(background = background) }
    }

    fun setAspectRatio(ratio: String) {
        updateConfig { it.copy(aspectRatio = ratio) }
    }

    fun setSafeAreaOverlay(overlay: String?) {
        updateConfig { it.copy(safeAreaOverlay = overlay) }
    }

    // --- Helpers ---

    private inline fun updateConfig(transform: (VisualizerRenderConfig) -> VisualizerRenderConfig) {
        _uiState.update { state -> state.copy(config = transform(state.config)) }
    }

    companion object {
        val ASPECT_RATIOS = listOf("9:16", "16:9", "1:1", "4:5")
        val SAFE_AREAS = listOf("none", "tiktok", "reels", "shorts", "youtube")
    }
}
