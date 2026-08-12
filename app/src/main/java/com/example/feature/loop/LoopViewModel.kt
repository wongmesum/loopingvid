package com.example.feature.loop

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.command.Command
import com.example.core.command.UndoRedoManager
import com.example.core.command.UndoRedoState
import com.example.core.ffmpeg.JobProgressState
import com.example.core.ffmpeg.LoopDurationPlanner
import com.example.core.ffmpeg.MediaProcessor
import com.example.core.ffmpeg.RenderState
import com.example.core.ffmpeg.ExportState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LoopUiState(
    val selectedMediaUri: String? = null,
    val selectedMediaName: String = "",
    val targetDurationSec: Double = 60.0, // Default 60s
    val loopStyle: String = "CROSSFADE", // "NORMAL", "CROSSFADE", "PING_PONG"
    val crossfadeDurationSec: Double = 1.0, // Default 1.0s crossfade transition duration
    val trimStartSec: Double = 0.0,
    val trimEndSec: Double = 0.0,
    val muteAudio: Boolean = false,
    val audioFadeInSec: Double = 1.0,
    val audioFadeOutSec: Double = 1.0,
    val presetQuality: String = "1080p", // "1080p", "720p", "480p"
    val isPreviewPlaying: Boolean = true,
    val isSeamlessLoopEnabled: Boolean = true,
    val playbackSpeed: Float = 1.0f,
    val currentPositionMs: Long = 0L,
    val durationMs: Long = 0L,
    val loopCount: Int = 0,
    val jobProgress: JobProgressState = JobProgressState(),
    val renderState: RenderState = RenderState.Idle,
    val exportState: ExportState = ExportState.Idle,
    val lastRenderedOutputUri: String? = null
)

class LoopViewModel(
    private val mediaProcessor: MediaProcessor
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoopUiState())
    val uiState: StateFlow<LoopUiState> = _uiState.asStateFlow()

    val undoRedoManager = UndoRedoManager()
    val undoRedoState: StateFlow<UndoRedoState> = undoRedoManager.state

    private class LoopCommand(
        override val actionName: String,
        private val onExecute: () -> Unit,
        private val onUndo: () -> Unit
    ) : Command {
        override fun execute() = onExecute()
        override fun undo() = onUndo()
    }

    init {
        viewModelScope.launch(Dispatchers.Default) {
            mediaProcessor.renderState.collect { state ->
                _uiState.update { current ->
                    current.copy(
                        renderState = state,
                        lastRenderedOutputUri = if (state is RenderState.Success) state.outputPath else current.lastRenderedOutputUri
                    )
                }
            }
        }
        viewModelScope.launch(Dispatchers.Default) {
            mediaProcessor.progressState.collect { progress ->
                _uiState.update { current ->
                    current.copy(
                        jobProgress = progress
                    )
                }
            }
        }
    }

    fun undo() { undoRedoManager.undo() }
    fun redo() { undoRedoManager.redo() }
    fun clearHistory() { undoRedoManager.clear() }

    fun onMediaSelected(uri: String, fileName: String) {
        _uiState.value = _uiState.value.copy(
            selectedMediaUri = uri,
            selectedMediaName = fileName,
            currentPositionMs = 0L,
            durationMs = 0L,
            loopCount = 0,
            isPreviewPlaying = true
        )
        undoRedoManager.clear()
    }

    fun togglePlayPause() {
        _uiState.value = _uiState.value.copy(
            isPreviewPlaying = !_uiState.value.isPreviewPlaying
        )
    }

    fun setSeamlessLoopEnabled(enabled: Boolean) {
        val oldVal = _uiState.value.isSeamlessLoopEnabled
        if (oldVal == enabled) return
        undoRedoManager.executeCommand(
            LoopCommand(
                actionName = if (enabled) "Enable Seamless Loop" else "Disable Seamless Loop",
                onExecute = { _uiState.value = _uiState.value.copy(isSeamlessLoopEnabled = enabled) },
                onUndo = { _uiState.value = _uiState.value.copy(isSeamlessLoopEnabled = oldVal) }
            )
        )
    }

    fun setPlaybackSpeed(speed: Float) {
        val oldVal = _uiState.value.playbackSpeed
        if (oldVal == speed) return
        undoRedoManager.executeCommand(
            LoopCommand(
                actionName = "Set Speed to %.1fx".format(speed),
                onExecute = { _uiState.value = _uiState.value.copy(playbackSpeed = speed) },
                onUndo = { _uiState.value = _uiState.value.copy(playbackSpeed = oldVal) }
            )
        )
    }

    fun updateProgress(positionMs: Long, durationMs: Long) {
        val current = _uiState.value.currentPositionMs
        // If current position jumped back to near start while playing, increment loop count
        val isLoopedBack = current > 1000L && positionMs < 500L && _uiState.value.isSeamlessLoopEnabled
        val newLoopCount = if (isLoopedBack) _uiState.value.loopCount + 1 else _uiState.value.loopCount

        _uiState.value = _uiState.value.copy(
            currentPositionMs = positionMs,
            durationMs = durationMs,
            loopCount = newLoopCount
        )
    }

    fun onPlaybackStateChanged(isPlaying: Boolean) {
        _uiState.value = _uiState.value.copy(
            isPreviewPlaying = isPlaying
        )
    }

    fun setTargetDuration(durationSec: Double) {
        // Sanitize at the state boundary so the displayed value, the export request, and the
        // FFmpeg command can never disagree.
        val newVal = LoopDurationPlanner.sanitizeTargetDuration(durationSec)
        val oldVal = _uiState.value.targetDurationSec
        if (oldVal == newVal) return
        undoRedoManager.executeCommand(
            LoopCommand(
                actionName = "Target Duration (%.0fs)".format(newVal),
                onExecute = { _uiState.value = _uiState.value.copy(targetDurationSec = newVal) },
                onUndo = { _uiState.value = _uiState.value.copy(targetDurationSec = oldVal) }
            )
        )
    }

    fun setLoopStyle(style: String) {
        val oldVal = _uiState.value.loopStyle
        if (oldVal == style) return
        undoRedoManager.executeCommand(
            LoopCommand(
                actionName = "Loop Style ($style)",
                onExecute = { _uiState.value = _uiState.value.copy(loopStyle = style) },
                onUndo = { _uiState.value = _uiState.value.copy(loopStyle = oldVal) }
            )
        )
    }

    private var sliderStartTrim: Pair<Double, Double>? = null

    fun updateTrimPreview(startSec: Double, endSec: Double) {
        if (sliderStartTrim == null) {
            sliderStartTrim = Pair(_uiState.value.trimStartSec, _uiState.value.trimEndSec)
        }
        _uiState.value = _uiState.value.copy(trimStartSec = startSec, trimEndSec = endSec)
    }

    fun commitTrimChange(startSec: Double, endSec: Double) {
        val oldTrim = sliderStartTrim ?: Pair(_uiState.value.trimStartSec, _uiState.value.trimEndSec)
        sliderStartTrim = null
        val oldStart = oldTrim.first
        val oldEnd = oldTrim.second
        if (oldStart == startSec && oldEnd == endSec) return

        undoRedoManager.executeCommand(
            LoopCommand(
                actionName = "Trim Points (%.1fs - %.1fs)".format(startSec, endSec),
                onExecute = { _uiState.value = _uiState.value.copy(trimStartSec = startSec, trimEndSec = endSec) },
                onUndo = { _uiState.value = _uiState.value.copy(trimStartSec = oldStart, trimEndSec = oldEnd) }
            )
        )
    }

    fun setTrim(startSec: Double, endSec: Double) {
        commitTrimChange(startSec, endSec)
    }

    fun setMuteAudio(mute: Boolean) {
        val oldVal = _uiState.value.muteAudio
        if (oldVal == mute) return
        undoRedoManager.executeCommand(
            LoopCommand(
                actionName = if (mute) "Mute Audio" else "Unmute Audio",
                onExecute = { _uiState.value = _uiState.value.copy(muteAudio = mute) },
                onUndo = { _uiState.value = _uiState.value.copy(muteAudio = oldVal) }
            )
        )
    }

    private var sliderStartCrossfade: Double? = null

    fun updateCrossfadeDurationPreview(durationSec: Double) {
        if (sliderStartCrossfade == null) {
            sliderStartCrossfade = _uiState.value.crossfadeDurationSec
        }
        _uiState.value = _uiState.value.copy(crossfadeDurationSec = durationSec)
    }

    fun commitCrossfadeDurationChange(durationSec: Double) {
        val oldVal = sliderStartCrossfade ?: _uiState.value.crossfadeDurationSec
        sliderStartCrossfade = null
        if (oldVal == durationSec) return
        undoRedoManager.executeCommand(
            LoopCommand(
                actionName = "Crossfade Duration (%.1fs)".format(durationSec),
                onExecute = { _uiState.value = _uiState.value.copy(crossfadeDurationSec = durationSec) },
                onUndo = { _uiState.value = _uiState.value.copy(crossfadeDurationSec = oldVal) }
            )
        )
    }

    fun setCrossfadeDuration(durationSec: Double) {
        updateCrossfadeDurationPreview(durationSec)
        commitCrossfadeDurationChange(durationSec)
    }

    fun setPresetQuality(quality: String) {
        val oldVal = _uiState.value.presetQuality
        if (oldVal == quality) return
        undoRedoManager.executeCommand(
            LoopCommand(
                actionName = "Quality ($quality)",
                onExecute = { _uiState.value = _uiState.value.copy(presetQuality = quality) },
                onUndo = { _uiState.value = _uiState.value.copy(presetQuality = oldVal) }
            )
        )
    }

    fun startRenderJob(customFileName: String? = null, destinationFolder: String? = null, exportFormat: String = "mp4") {
        val state = _uiState.value
        val inputUri = state.selectedMediaUri ?: return

        viewModelScope.launch(Dispatchers.IO) {
            try {
                mediaProcessor.executeLoopJob(
                    inputUri = inputUri,
                    targetDurationSec = state.targetDurationSec,
                    loopStyle = state.loopStyle,
                    crossfadeDurationSec = state.crossfadeDurationSec,
                    trimStartSec = state.trimStartSec,
                    trimEndSec = state.trimEndSec,
                    muteAudio = state.muteAudio,
                    audioFadeInSec = state.audioFadeInSec,
                    audioFadeOutSec = state.audioFadeOutSec,
                    presetQuality = state.presetQuality,
                    customFileName = customFileName,
                    destinationFolder = destinationFolder,
                    exportFormat = exportFormat
                )
            } catch (e: Exception) {
                // Handled in progress state
            }
        }
    }

    fun cancelRenderJob() {
        mediaProcessor.cancelActiveJob()
    }

    /**
     * Copies the already-validated render output into the device gallery. This never
     * re-invokes FFmpeg: the source of truth for the output path is RenderState.Success,
     * not a re-render.
     */
    fun saveRenderToGallery() {
        // Read directly from processor to bypass state-sync timing in tests
        val renderState = mediaProcessor.renderState.value
        if (renderState !is RenderState.Success) return

        viewModelScope.launch {
            _uiState.update { it.copy(exportState = ExportState.Exporting) }
            // exportProjectToGallery already switches to Dispatchers.IO internally
            val result = mediaProcessor.exportProjectToGallery(
                filePath = renderState.outputPath,
                isAudio = false
            )
            result.onSuccess { uri ->
                _uiState.update { it.copy(exportState = ExportState.Success(uri)) }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(exportState = ExportState.Failed(error.localizedMessage ?: "Export failed"))
                }
            }
        }
    }

    fun dismissExportState() {
        _uiState.update { it.copy(exportState = ExportState.Idle) }
    }
}
