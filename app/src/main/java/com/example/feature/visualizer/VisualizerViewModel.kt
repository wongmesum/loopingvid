package com.example.feature.visualizer

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.audio.AnalysisOptions
import com.example.core.audio.AudioAnalysisRepository
import com.example.core.audio.AudioAnalysisResult
import com.example.core.ffmpeg.JobProgressState
import com.example.core.ffmpeg.VisualizerProcessor
import com.example.core.ffmpeg.VisualizerRenderRequest
import com.example.core.work.BatchExportRequest
import com.example.core.work.ExportQueueViewModel
import com.example.core.work.QueueStatus
import com.example.core.work.RenderRequestSerializer
import com.example.core.media.Media3SpectrumAudioProcessor
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import com.example.feature.visualizer.beat.BeatDetectionConfig
import com.example.feature.visualizer.beat.BeatDetectionEngine
import com.example.feature.visualizer.beat.BeatEffect
import com.example.feature.visualizer.beat.BeatGridDivision
import com.example.feature.visualizer.beat.BeatGridSnapper
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
import java.util.UUID

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
    val outputName: String = "",
    val jobProgress: JobProgressState = JobProgressState(),
    /** Work id of the render this screen enqueued, used to track and cancel it. */
    val exportJobId: UUID? = null,
    val validationMessage: String? = null
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
            outputName == other.outputName &&
            jobProgress == other.jobProgress &&
            exportJobId == other.exportJobId &&
            validationMessage == other.validationMessage
    }

    override fun hashCode(): Int {
        var result = audioUri?.hashCode() ?: 0
        result = 31 * result + magnitudes.contentHashCode()
        result = 31 * result + config.hashCode()
        result = 31 * result + isPlaying.hashCode()
        return result
    }

    val canExport: Boolean
        get() = audioUri != null && !jobProgress.isProcessing
}

/**
 * ViewModel for Visualizer Studio. Drives real-time FFT preview using the
 * same [Media3SpectrumAudioProcessor] as the Editor module, but with its own
 * [VisualizerRenderConfig] state tree.
 */
class VisualizerViewModel(
    private val appContext: Context? = null,
    private val visualizerProcessor: VisualizerProcessor? = null,
    private val audioAnalysisRepository: AudioAnalysisRepository? = null,
    private val exportQueueViewModel: ExportQueueViewModel? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(VisualizerUiState())
    val uiState: StateFlow<VisualizerUiState> = _uiState.asStateFlow()

    private val _fullAnalysis = MutableStateFlow<AudioAnalysisResult?>(null)
    /** Waveform/spectrum/BPM/loudness data, populated once [audioAnalysisRepository] finishes. */
    val fullAnalysis: StateFlow<AudioAnalysisResult?> = _fullAnalysis.asStateFlow()

    val spectrumProcessor = Media3SpectrumAudioProcessor()
    private val tapBpmDetector = TapBpmDetector()
    private var decodedDurationMs: Long = 0L
    private var analysisJob: Job? = null
    private var fullAnalysisJob: Job? = null

    init {
        // Fallback: observe processor progress directly when no queue is injected.
        if (exportQueueViewModel == null) {
            visualizerProcessor?.let { processor ->
                viewModelScope.launch {
                    processor.progressState.collect { progress ->
                        _uiState.update { it.copy(jobProgress = progress) }
                    }
                }
            }
        }

        // Primary: observe queue state and map back to JobProgressState for this job.
        exportQueueViewModel?.let { queue ->
            viewModelScope.launch {
                queue.uiState.collect { queueState ->
                    val currentJobId = _uiState.value.exportJobId ?: return@collect
                    val myItem = queueState.items.find { it.id == currentJobId } ?: return@collect
                    val isProcessing = myItem.status == QueueStatus.RUNNING || myItem.status == QueueStatus.QUEUED
                    _uiState.update {
                        it.copy(
                            jobProgress = JobProgressState(
                                jobId = currentJobId.mostSignificantBits,
                                isProcessing = isProcessing,
                                progress = myItem.progress,
                                statusText = myItem.statusText,
                                outputFilePath = myItem.galleryUri ?: "",
                                errorMessage = myItem.error
                            )
                        )
                    }
                }
            }
        }

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
        // Full analysis is not started here: beat detection already decodes this
        // file, and a second concurrent decode would cost seconds for data no
        // screen consumes yet. Callers ask for it via requestFullAnalysis().
        fullAnalysisJob?.cancel()
        _fullAnalysis.value = null
        analyzeBeats(uri)
    }

    fun setPlaying(playing: Boolean) {
        _uiState.update { it.copy(isPlaying = playing) }
    }

    /**
     * Requests a full waveform/spectrum/BPM/loudness analysis through the
     * [AudioAnalysisRepository] cache. Safe to call multiple times — the result
     * is cached, and a second call for the same track is a no-op.
     */
    fun requestFullAnalysis() {
        val uri = _uiState.value.audioUri ?: return
        val repository = audioAnalysisRepository ?: return
        // Already in flight or completed for this URI
        if (_fullAnalysis.value != null || fullAnalysisJob?.isActive == true) return

        fullAnalysisJob = viewModelScope.launch {
            repository.getOrAnalyze(
                uri = android.net.Uri.parse(uri),
                options = AnalysisOptions()
            ).fold(
                onSuccess = { result ->
                    if (_uiState.value.audioUri == uri) {
                        _fullAnalysis.value = result
                    }
                },
                onFailure = { error ->
                    if (error is CancellationException) throw error
                    // Beat detection still uses PcmDecoder, so full analysis failure
                    // must not prevent the existing Visualizer workflow.
                }
            )
        }
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

        // Switching tracks must abandon the in-flight decode: it can take seconds
        // on a long file, and its result would otherwise land on the new track.
        analysisJob?.cancel()
        _uiState.update { it.copy(beatSync = it.beatSync.copy(isAnalyzing = true, analysisError = null)) }

        analysisJob = viewModelScope.launch {
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
                                    "No beats detected. Try lowering threshold or use Tap BPM."
                                } else null
                            )
                        )
                    }
                },
                onFailure = { error ->
                    if (error is CancellationException) {
                        // The coroutine was cancelled (e.g. by another track pick),
                        // don't overwrite the new track's state with a cancellation error.
                        throw error
                    }
                    _uiState.update {
                        it.copy(
                            beatSync = it.beatSync.copy(
                                isAnalyzing = false,
                                analysisError = error.localizedMessage ?: "Beat analysis failed"
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

    fun setBeatGridDivision(division: BeatGridDivision) {
        _uiState.update { it.copy(beatSync = it.beatSync.copy(gridDivision = division)) }
    }

    fun quantizeBeatMarkers() {
        _uiState.update { state ->
            val sync = state.beatSync
            state.copy(
                beatSync = sync.copy(
                    markersMs = BeatGridSnapper.quantize(
                        markers = sync.markersMs,
                        bpm = sync.bpm,
                        division = sync.gridDivision,
                        offsetMs = sync.config.offsetMs,
                        durationMs = sync.durationMs
                    )
                )
            )
        }
    }

    fun addBeatMarker(markerMs: Long) {
        _uiState.update { state ->
            val sync = state.beatSync
            val targetMs = BeatGridSnapper.snap(
                markerMs = markerMs,
                bpm = sync.bpm,
                division = sync.gridDivision,
                offsetMs = sync.config.offsetMs,
                durationMs = sync.durationMs
            )
            state.copy(
                beatSync = sync.copy(
                    markersMs = BeatMarkerEditor.add(sync.markersMs, targetMs, sync.durationMs)
                )
            )
        }
    }

    fun moveBeatMarker(index: Int, markerMs: Long) {
        _uiState.update { state ->
            val sync = state.beatSync
            val targetMs = BeatGridSnapper.snap(
                markerMs = markerMs,
                bpm = sync.bpm,
                division = sync.gridDivision,
                offsetMs = sync.config.offsetMs,
                durationMs = sync.durationMs
            )
            state.copy(
                beatSync = sync.copy(
                    markersMs = BeatMarkerEditor.move(sync.markersMs, index, targetMs, sync.durationMs)
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
        val safe = value.coerceIn(0f, 1f)
        spectrumProcessor.smoothing = safe.coerceAtMost(0.95f)
        updateConfig { it.copy(smoothing = safe) }
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

    // --- Export ---

    fun setOutputName(name: String) {
        _uiState.update { it.copy(outputName = name, validationMessage = null) }
    }

    fun dismissValidationMessage() {
        _uiState.update { it.copy(validationMessage = null) }
    }

    /**
     * Renders the current config to an MP4. Beat markers are forwarded so the
     * exported video pulses on the same beats the preview does.
     */
    fun exportVisualizer() {
        val state = _uiState.value
        val audioUri = state.audioUri
        if (audioUri.isNullOrBlank()) {
            _uiState.update { it.copy(validationMessage = "Please select an audio source first.") }
            return
        }
        val queue = exportQueueViewModel
        if (queue == null) {
            _uiState.update { it.copy(validationMessage = "Export queue not ready.") }
            return
        }
        if (state.jobProgress.isProcessing) return

        viewModelScope.launch {
            try {
                val request = VisualizerRenderRequest(
                    audioUri = audioUri,
                    config = state.config,
                    beatMarkersMs = state.beatSync.markersMs,
                    beatEffectExpression = state.beatSync.selectedEffect.name.takeIf {
                        state.beatSync.markersMs.isNotEmpty()
                    },
                    durationMs = state.beatSync.durationMs,
                    outputName = state.outputName
                )
                val jobId = queue.enqueueProjectExport(
                    BatchExportRequest(
                        title = state.outputName.ifBlank { "Visualizer" },
                        jobType = "VISUALIZER",
                        visualizerConfigJson = RenderRequestSerializer.serializeVisualizer(request)
                    )
                )
                _uiState.update {
                    it.copy(
                        exportJobId = jobId,
                        jobProgress = JobProgressState(
                            jobId = jobId.mostSignificantBits,
                            isProcessing = true,
                            statusText = "Menunggu dalam antrean..."
                        )
                    )
                }
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                // Failure is surfaced through queue item state.
            }
        }
    }

    fun cancelExport() {
        _uiState.value.exportJobId?.let { jobId ->
            exportQueueViewModel?.cancelExportJob(jobId)
        }
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
