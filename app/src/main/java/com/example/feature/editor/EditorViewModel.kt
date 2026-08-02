package com.example.feature.editor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.content.Context
import com.example.core.command.Command
import com.example.core.command.UndoRedoManager
import com.example.core.command.UndoRedoState
import com.example.core.ffmpeg.JobProgressState
import com.example.core.ffmpeg.MediaProcessor
import com.example.core.media.AutoSaveSessionInfo
import com.example.core.media.BUILTIN_PROJECT_TEMPLATES
import com.example.core.media.ColorFilterPreset
import com.example.core.media.ColorGradingConfig
import com.example.core.media.EditorAutoSaveManager
import com.example.core.media.LoopedSegmentConfig
import com.example.core.media.Media3SpectrumAudioProcessor
import com.example.core.media.ProjectTemplate
import com.example.core.media.SegmentTransitionConfig
import com.example.core.media.SpectrumStyle
import com.example.core.media.TransitionEffect
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.sin

data class EditorUiState(
    val selectedMediaUri: String? = null,
    val selectedMediaName: String = "",
    val selectedAudioUri: String? = null,
    val selectedAudioName: String = "",
    val selectedOverlayUri: String? = null,
    val selectedOverlayName: String = "",
    val pipPosition: com.example.core.media.PipPosition = com.example.core.media.PipPosition.BOTTOM_RIGHT,
    val titleText: String = "LoopingVid Live Studio",
    val watermarkText: String = "@LoopingVid",
    val showTimerOverlay: Boolean = true,
    val spectrumStyle: SpectrumStyle = SpectrumStyle.BARS,
    val presetQuality: String = "1080p",
    val isPreviewPlaying: Boolean = true,
    val visualizerMode: VisualizerMode = VisualizerMode.FFT_BARS,
    val selectedPalette: SpectrumPalette = SPECTRUM_PALETTES[0],
    val spectrumSensitivityGain: Float = 1.2f,
    val spectrumBandCount: Int = 32,
    val spectrumMagnitudes: FloatArray = FloatArray(32) { (sin(it.toDouble() * 0.4) * 0.4 + 0.5).toFloat() },
    val spectrumPeakDb: Float = -8.5f,
    val spectrumRmsEnergy: Float = 0.38f,
    val spectrumDominantFreqHz: Int = 420,
    val colorGradingConfig: ColorGradingConfig = ColorGradingConfig(),
    val customColorPresets: List<com.example.core.media.CustomColorGradingPreset> = emptyList(),
    val transitionConfig: SegmentTransitionConfig = SegmentTransitionConfig(),
    val audioMasteringPreset: com.example.core.media.MasteringPreset = com.example.core.media.AudioMasteringEngine.PRESETS.first(),
    val playbackSpeed: Float = 1.0f,
    val projectTemplates: List<ProjectTemplate> = BUILTIN_PROJECT_TEMPLATES,
    val selectedTemplateId: String? = null,
    val jobProgress: JobProgressState = JobProgressState(),
    val lastExportedOutputUri: String? = null,
    val autoSaveEnabled: Boolean = true,
    val autoSaveIntervalSec: Int = 10,
    val autoSaveSessionInfo: AutoSaveSessionInfo = AutoSaveSessionInfo(),
    val isSavingInProgress: Boolean = false,
    val lastSavedMessage: String = "Auto-save active (10s)",
    val showRecoveryBanner: Boolean = false,
    val trimStartSec: Double = 0.0,
    val trimEndSec: Double = 0.0,
    val aiGeneratedCaptions: String = ""
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as EditorUiState

        if (selectedMediaUri != other.selectedMediaUri) return false
        if (selectedMediaName != other.selectedMediaName) return false
        if (selectedAudioUri != other.selectedAudioUri) return false
        if (selectedAudioName != other.selectedAudioName) return false
        if (selectedOverlayUri != other.selectedOverlayUri) return false
        if (selectedOverlayName != other.selectedOverlayName) return false
        if (pipPosition != other.pipPosition) return false
        if (titleText != other.titleText) return false
        if (watermarkText != other.watermarkText) return false
        if (showTimerOverlay != other.showTimerOverlay) return false
        if (spectrumStyle != other.spectrumStyle) return false
        if (presetQuality != other.presetQuality) return false
        if (isPreviewPlaying != other.isPreviewPlaying) return false
        if (visualizerMode != other.visualizerMode) return false
        if (selectedPalette != other.selectedPalette) return false
        if (spectrumSensitivityGain != other.spectrumSensitivityGain) return false
        if (spectrumBandCount != other.spectrumBandCount) return false
        if (!spectrumMagnitudes.contentEquals(other.spectrumMagnitudes)) return false
        if (spectrumPeakDb != other.spectrumPeakDb) return false
        if (spectrumRmsEnergy != other.spectrumRmsEnergy) return false
        if (spectrumDominantFreqHz != other.spectrumDominantFreqHz) return false
        if (colorGradingConfig != other.colorGradingConfig) return false
        if (customColorPresets != other.customColorPresets) return false
        if (transitionConfig != other.transitionConfig) return false
        if (audioMasteringPreset != other.audioMasteringPreset) return false
        if (playbackSpeed != other.playbackSpeed) return false
        if (projectTemplates != other.projectTemplates) return false
        if (selectedTemplateId != other.selectedTemplateId) return false
        if (jobProgress != other.jobProgress) return false
        if (lastExportedOutputUri != other.lastExportedOutputUri) return false
        if (autoSaveEnabled != other.autoSaveEnabled) return false
        if (autoSaveIntervalSec != other.autoSaveIntervalSec) return false
        if (autoSaveSessionInfo != other.autoSaveSessionInfo) return false
        if (isSavingInProgress != other.isSavingInProgress) return false
        if (lastSavedMessage != other.lastSavedMessage) return false
        if (showRecoveryBanner != other.showRecoveryBanner) return false
        if (trimStartSec != other.trimStartSec) return false
        if (trimEndSec != other.trimEndSec) return false
        if (aiGeneratedCaptions != other.aiGeneratedCaptions) return false

        return true
    }

    override fun hashCode(): Int {
        var result = selectedMediaUri?.hashCode() ?: 0
        result = 31 * result + selectedMediaName.hashCode()
        result = 31 * result + (selectedAudioUri?.hashCode() ?: 0)
        result = 31 * result + selectedAudioName.hashCode()
        result = 31 * result + (selectedOverlayUri?.hashCode() ?: 0)
        result = 31 * result + selectedOverlayName.hashCode()
        result = 31 * result + pipPosition.hashCode()
        result = 31 * result + titleText.hashCode()
        result = 31 * result + watermarkText.hashCode()
        result = 31 * result + showTimerOverlay.hashCode()
        result = 31 * result + spectrumStyle.hashCode()
        result = 31 * result + presetQuality.hashCode()
        result = 31 * result + isPreviewPlaying.hashCode()
        result = 31 * result + visualizerMode.hashCode()
        result = 31 * result + selectedPalette.hashCode()
        result = 31 * result + spectrumSensitivityGain.hashCode()
        result = 31 * result + spectrumBandCount.hashCode()
        result = 31 * result + spectrumMagnitudes.contentHashCode()
        result = 31 * result + spectrumPeakDb.hashCode()
        result = 31 * result + spectrumRmsEnergy.hashCode()
        result = 31 * result + spectrumDominantFreqHz.hashCode()
        result = 31 * result + colorGradingConfig.hashCode()
        result = 31 * result + customColorPresets.hashCode()
        result = 31 * result + transitionConfig.hashCode()
        result = 31 * result + audioMasteringPreset.hashCode()
        result = 31 * result + playbackSpeed.hashCode()
        result = 31 * result + projectTemplates.hashCode()
        result = 31 * result + (selectedTemplateId?.hashCode() ?: 0)
        result = 31 * result + jobProgress.hashCode()
        result = 31 * result + (lastExportedOutputUri?.hashCode() ?: 0)
        result = 31 * result + autoSaveEnabled.hashCode()
        result = 31 * result + autoSaveIntervalSec.hashCode()
        result = 31 * result + autoSaveSessionInfo.hashCode()
        result = 31 * result + isSavingInProgress.hashCode()
        result = 31 * result + lastSavedMessage.hashCode()
        result = 31 * result + showRecoveryBanner.hashCode()
        result = 31 * result + trimStartSec.hashCode()
        result = 31 * result + trimEndSec.hashCode()
        result = 31 * result + aiGeneratedCaptions.hashCode()
        return result
    }
}

class EditorViewModel(
    private val mediaProcessor: MediaProcessor,
    private val context: Context? = null
) : ViewModel() {

    private val autoSaveManager = context?.let { EditorAutoSaveManager(it) }

    private val _uiState = MutableStateFlow(EditorUiState())
    val uiState: StateFlow<EditorUiState> = _uiState.asStateFlow()

    val undoRedoManager = UndoRedoManager()
    val undoRedoState: StateFlow<UndoRedoState> = undoRedoManager.state

    private class EditorCommand(
        override val actionName: String,
        private val onExecute: () -> Unit,
        private val onUndo: () -> Unit
    ) : Command {
        override fun execute() = onExecute()
        override fun undo() = onUndo()
    }

    private val spectrumProcessor = Media3SpectrumAudioProcessor()

    init {
        viewModelScope.launch(Dispatchers.Default) {
            mediaProcessor.progressState.collect { progress ->
                _uiState.update { current ->
                    current.copy(
                        jobProgress = progress,
                        lastExportedOutputUri = if (progress.progress == 100) progress.outputFilePath else current.lastExportedOutputUri
                    )
                }
            }
        }

        spectrumProcessor.onSpectrumDataListener = { magnitudes, peakDb, rms ->
            updateSpectrumData(magnitudes, peakDb, rms)
        }

        startRealtimeSpectrumAnimation()
        checkAutoSaveSessionOnStartup()
        startAutoSaveLoop()
    }

    fun undo() { undoRedoManager.undo() }
    fun redo() { undoRedoManager.redo() }
    fun clearHistory() { undoRedoManager.clear() }

    private fun startRealtimeSpectrumAnimation() {
        viewModelScope.launch(Dispatchers.Default) {
            var phase = 0f
            while (true) {
                val state = _uiState.value
                val isProcessing = state.jobProgress.isProcessing
                delay(if (isProcessing) 500L else 200L)
                if (state.isPreviewPlaying) {
                    phase += 0.15f
                    val count = state.spectrumBandCount
                    val gain = state.spectrumSensitivityGain
                    val simulated = FloatArray(count) { i ->
                        val base = (sin(phase + i * 0.3) * 0.4 + 0.5).toFloat()
                        val noise = (Math.random() * 0.15).toFloat()
                        (base * gain + noise).coerceIn(0.05f, 1.0f)
                    }
                    val currentPeak = (-12f + (sin(phase * 1.2f) * 6f)).coerceIn(-60f, 0f)
                    val currentRms = (0.35f + (sin(phase * 0.8f) * 0.15f)).coerceIn(0f, 1f)
                    val dominantHz = (100 + (sin(phase * 0.5f) * 800 + 800)).toInt()

                    _uiState.update { current ->
                        current.copy(
                            spectrumMagnitudes = simulated,
                            spectrumPeakDb = currentPeak,
                            spectrumRmsEnergy = currentRms,
                            spectrumDominantFreqHz = dominantHz
                        )
                    }
                }
            }
        }
    }

    fun setVisualizerMode(mode: VisualizerMode) {
        val oldVal = _uiState.value.visualizerMode
        if (oldVal == mode) return
        undoRedoManager.executeCommand(
            EditorCommand(
                actionName = "Visualizer Mode (${mode.name})",
                onExecute = { _uiState.value = _uiState.value.copy(visualizerMode = mode) },
                onUndo = { _uiState.value = _uiState.value.copy(visualizerMode = oldVal) }
            )
        )
    }

    fun setSpectrumPalette(palette: SpectrumPalette) {
        val oldVal = _uiState.value.selectedPalette
        if (oldVal == palette) return
        undoRedoManager.executeCommand(
            EditorCommand(
                actionName = "Palette (${palette.name})",
                onExecute = { _uiState.value = _uiState.value.copy(selectedPalette = palette) },
                onUndo = { _uiState.value = _uiState.value.copy(selectedPalette = oldVal) }
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
            EditorCommand(
                actionName = "Trim Segment (%.1fs - %.1fs)".format(startSec, endSec),
                onExecute = { _uiState.value = _uiState.value.copy(trimStartSec = startSec, trimEndSec = endSec) },
                onUndo = { _uiState.value = _uiState.value.copy(trimStartSec = oldStart, trimEndSec = oldEnd) }
            )
        )
    }

    fun setTrim(startSec: Double, endSec: Double) {
        commitTrimChange(startSec, endSec)
    }

    fun setSpectrumSensitivity(gain: Float) {
        val oldVal = _uiState.value.spectrumSensitivityGain
        if (oldVal == gain) return
        undoRedoManager.executeCommand(
            EditorCommand(
                actionName = "Audio Gain (%.1fx)".format(gain),
                onExecute = {
                    spectrumProcessor.sensitivityGain = gain
                    _uiState.value = _uiState.value.copy(spectrumSensitivityGain = gain)
                },
                onUndo = {
                    spectrumProcessor.sensitivityGain = oldVal
                    _uiState.value = _uiState.value.copy(spectrumSensitivityGain = oldVal)
                }
            )
        )
    }

    fun setSpectrumBandCount(count: Int) {
        val oldVal = _uiState.value.spectrumBandCount
        if (oldVal == count) return
        undoRedoManager.executeCommand(
            EditorCommand(
                actionName = "Spectrum Bands ($count)",
                onExecute = {
                    spectrumProcessor.bandCount = count
                    val newMagnitudes = FloatArray(count) { (sin(it.toDouble() * 0.4) * 0.4 + 0.5).toFloat() }
                    _uiState.value = _uiState.value.copy(
                        spectrumBandCount = count,
                        spectrumMagnitudes = newMagnitudes
                    )
                },
                onUndo = {
                    spectrumProcessor.bandCount = oldVal
                    val oldMagnitudes = FloatArray(oldVal) { (sin(it.toDouble() * 0.4) * 0.4 + 0.5).toFloat() }
                    _uiState.value = _uiState.value.copy(
                        spectrumBandCount = oldVal,
                        spectrumMagnitudes = oldMagnitudes
                    )
                }
            )
        )
    }

    fun updateSpectrumData(magnitudes: FloatArray, peakDb: Float, rms: Float) {
        _uiState.value = _uiState.value.copy(
            spectrumMagnitudes = magnitudes,
            spectrumPeakDb = peakDb,
            spectrumRmsEnergy = rms
        )
    }

    fun onMediaSelected(uri: String, name: String) {
        _uiState.value = _uiState.value.copy(
            selectedMediaUri = uri,
            selectedMediaName = name
        )
    }
    
    fun onOverlaySelected(uri: String?, name: String) {
        _uiState.value = _uiState.value.copy(
            selectedOverlayUri = uri,
            selectedOverlayName = name
        )
    }

    fun setPipPosition(position: com.example.core.media.PipPosition) {
        _uiState.value = _uiState.value.copy(
            pipPosition = position
        )
    }

    fun onAudioSelected(uri: String, name: String) {
        val oldUri = _uiState.value.selectedAudioUri
        val oldName = _uiState.value.selectedAudioName
        undoRedoManager.executeCommand(
            EditorCommand(
                actionName = "Audio Track ($name)",
                onExecute = {
                    _uiState.value = _uiState.value.copy(
                        selectedAudioUri = uri,
                        selectedAudioName = name
                    )
                },
                onUndo = {
                    _uiState.value = _uiState.value.copy(
                        selectedAudioUri = oldUri,
                        selectedAudioName = oldName
                    )
                }
            )
        )
    }

    fun updateTitleText(title: String) {
        val oldVal = _uiState.value.titleText
        if (oldVal == title) return
        undoRedoManager.executeCommand(
            EditorCommand(
                actionName = "Title Text",
                onExecute = { _uiState.value = _uiState.value.copy(titleText = title) },
                onUndo = { _uiState.value = _uiState.value.copy(titleText = oldVal) }
            )
        )
    }

    fun updateWatermarkText(watermark: String) {
        val oldVal = _uiState.value.watermarkText
        if (oldVal == watermark) return
        undoRedoManager.executeCommand(
            EditorCommand(
                actionName = "Watermark Text",
                onExecute = { _uiState.value = _uiState.value.copy(watermarkText = watermark) },
                onUndo = { _uiState.value = _uiState.value.copy(watermarkText = oldVal) }
            )
        )
    }

    fun toggleTimerOverlay(show: Boolean) {
        val oldVal = _uiState.value.showTimerOverlay
        if (oldVal == show) return
        undoRedoManager.executeCommand(
            EditorCommand(
                actionName = if (show) "Show Timer" else "Hide Timer",
                onExecute = { _uiState.value = _uiState.value.copy(showTimerOverlay = show) },
                onUndo = { _uiState.value = _uiState.value.copy(showTimerOverlay = oldVal) }
            )
        )
    }

    fun setSpectrumStyle(style: SpectrumStyle) {
        val oldVal = _uiState.value.spectrumStyle
        if (oldVal == style) return
        undoRedoManager.executeCommand(
            EditorCommand(
                actionName = "Spectrum Style (${style.name})",
                onExecute = { _uiState.value = _uiState.value.copy(spectrumStyle = style) },
                onUndo = { _uiState.value = _uiState.value.copy(spectrumStyle = oldVal) }
            )
        )
    }

    fun setColorFilterPreset(preset: ColorFilterPreset) {
        val oldConfig = _uiState.value.colorGradingConfig
        if (oldConfig.preset == preset) return
        val newConfig = oldConfig.copy(preset = preset)
        undoRedoManager.executeCommand(
            EditorCommand(
                actionName = "Color Filter (${preset.displayName})",
                onExecute = { _uiState.value = _uiState.value.copy(colorGradingConfig = newConfig) },
                onUndo = { _uiState.value = _uiState.value.copy(colorGradingConfig = oldConfig) }
            )
        )
    }

    private var sliderStartColorConfig: ColorGradingConfig? = null

    fun onColorGradingSliderStarted() {
        if (sliderStartColorConfig == null) {
            sliderStartColorConfig = _uiState.value.colorGradingConfig
        }
    }

    fun onColorGradingSliderFinished(actionName: String = "Color Adjust") {
        val startConfig = sliderStartColorConfig ?: return
        val currentConfig = _uiState.value.colorGradingConfig
        sliderStartColorConfig = null
        if (startConfig == currentConfig) return

        undoRedoManager.executeCommand(
            EditorCommand(
                actionName = actionName,
                onExecute = { _uiState.value = _uiState.value.copy(colorGradingConfig = currentConfig) },
                onUndo = { _uiState.value = _uiState.value.copy(colorGradingConfig = startConfig) }
            )
        )
    }

    fun updateFilterBrightness(value: Float) {
        onColorGradingSliderStarted()
        val oldConfig = _uiState.value.colorGradingConfig
        _uiState.value = _uiState.value.copy(colorGradingConfig = oldConfig.copy(brightness = value))
    }

    fun updateFilterContrast(value: Float) {
        onColorGradingSliderStarted()
        val oldConfig = _uiState.value.colorGradingConfig
        _uiState.value = _uiState.value.copy(colorGradingConfig = oldConfig.copy(contrast = value))
    }

    fun updateFilterSaturation(value: Float) {
        onColorGradingSliderStarted()
        val oldConfig = _uiState.value.colorGradingConfig
        _uiState.value = _uiState.value.copy(colorGradingConfig = oldConfig.copy(saturation = value))
    }

    fun updateFilterHue(value: Float) {
        onColorGradingSliderStarted()
        val oldConfig = _uiState.value.colorGradingConfig
        _uiState.value = _uiState.value.copy(colorGradingConfig = oldConfig.copy(hue = value))
    }

    fun resetColorGrading() {
        val oldConfig = _uiState.value.colorGradingConfig
        val newConfig = ColorGradingConfig()
        if (oldConfig == newConfig) return
        undoRedoManager.executeCommand(
            EditorCommand(
                actionName = "Reset Color Grading",
                onExecute = { _uiState.value = _uiState.value.copy(colorGradingConfig = newConfig) },
                onUndo = { _uiState.value = _uiState.value.copy(colorGradingConfig = oldConfig) }
            )
        )
    }

    fun saveCurrentColorConfigAsPreset(name: String) {
        val currentState = _uiState.value
        val newPreset = com.example.core.media.CustomColorGradingPreset(
            id = "custom_color_${System.currentTimeMillis()}",
            name = name.ifBlank { "Custom Filter" },
            config = currentState.colorGradingConfig
        )
        _uiState.value = currentState.copy(
            customColorPresets = currentState.customColorPresets + newPreset
        )
    }

    fun applyCustomColorPreset(preset: com.example.core.media.CustomColorGradingPreset) {
        val oldConfig = _uiState.value.colorGradingConfig
        val newConfig = preset.config
        undoRedoManager.executeCommand(
            EditorCommand(
                actionName = "Apply Preset (${preset.name})",
                onExecute = { _uiState.value = _uiState.value.copy(colorGradingConfig = newConfig) },
                onUndo = { _uiState.value = _uiState.value.copy(colorGradingConfig = oldConfig) }
            )
        )
    }

    fun deleteCustomColorPreset(id: String) {
        val currentState = _uiState.value
        _uiState.value = currentState.copy(
            customColorPresets = currentState.customColorPresets.filterNot { it.id == id }
        )
    }

    fun togglePreviewPlayback() {
        _uiState.value = _uiState.value.copy(isPreviewPlaying = !_uiState.value.isPreviewPlaying)
    }

    fun updateSegmentTransition(segmentId: String, effect: TransitionEffect) {
        val currentSegments = _uiState.value.transitionConfig.segments.toMutableList()
        val index = currentSegments.indexOfFirst { it.id == segmentId }
        if (index != -1) {
            currentSegments[index] = currentSegments[index].copy(transitionToNext = effect)
            _uiState.value = _uiState.value.copy(
                transitionConfig = _uiState.value.transitionConfig.copy(segments = currentSegments)
            )
        }
    }

    fun updateSegmentTransitionDuration(segmentId: String, durationSec: Double) {
        val currentSegments = _uiState.value.transitionConfig.segments.toMutableList()
        val index = currentSegments.indexOfFirst { it.id == segmentId }
        if (index != -1) {
            currentSegments[index] = currentSegments[index].copy(transitionDurationSec = durationSec)
            _uiState.value = _uiState.value.copy(
                transitionConfig = _uiState.value.transitionConfig.copy(segments = currentSegments)
            )
        }
    }

    fun updateSegmentLoopCount(segmentId: String, repeatCount: Int) {
        val currentSegments = _uiState.value.transitionConfig.segments.toMutableList()
        val index = currentSegments.indexOfFirst { it.id == segmentId }
        if (index != -1) {
            currentSegments[index] = currentSegments[index].copy(loopRepeatCount = repeatCount)
            _uiState.value = _uiState.value.copy(
                transitionConfig = _uiState.value.transitionConfig.copy(segments = currentSegments)
            )
        }
    }

    fun addLoopedSegment() {
        val currentSegments = _uiState.value.transitionConfig.segments.toMutableList()
        val newId = "seg_${System.currentTimeMillis()}"
        val names = listOf("Bridge Drop", "Solo Breakdown", "Outro Fade", "Climax Loop", "Special Chorus")
        val name = names.getOrElse(currentSegments.size % names.size) { "Segment ${currentSegments.size + 1}" }
        val newSeg = LoopedSegmentConfig(
            id = newId,
            segmentName = name,
            durationSec = 5.0,
            loopRepeatCount = 2,
            transitionToNext = TransitionEffect.CROSSFADE,
            transitionDurationSec = 1.0
        )
        currentSegments.add(newSeg)
        _uiState.value = _uiState.value.copy(
            transitionConfig = _uiState.value.transitionConfig.copy(segments = currentSegments)
        )
    }

    fun removeLoopedSegment(segmentId: String) {
        val currentSegments = _uiState.value.transitionConfig.segments.toMutableList()
        if (currentSegments.size <= 2) return
        currentSegments.removeAll { it.id == segmentId }
        _uiState.value = _uiState.value.copy(
            transitionConfig = _uiState.value.transitionConfig.copy(segments = currentSegments)
        )
    }

    fun applyGlobalTransitionEffect(effect: TransitionEffect) {
        val currentSegments = _uiState.value.transitionConfig.segments.map {
            it.copy(transitionToNext = effect)
        }
        _uiState.value = _uiState.value.copy(
            transitionConfig = _uiState.value.transitionConfig.copy(
                segments = currentSegments,
                globalTransitionEffect = effect
            )
        )
    }

    fun setPlaybackSpeed(speed: Float) {
        val clampedSpeed = speed.coerceIn(0.5f, 2.0f)
        _uiState.value = _uiState.value.copy(playbackSpeed = clampedSpeed)
    }

    fun resetPlaybackSpeed() {
        _uiState.value = _uiState.value.copy(playbackSpeed = 1.0f)
    }

    fun applyProjectTemplate(template: ProjectTemplate) {
        _uiState.value = _uiState.value.copy(
            playbackSpeed = template.playbackSpeed,
            colorGradingConfig = template.colorGradingConfig,
            transitionConfig = template.transitionConfig,
            audioMasteringPreset = template.masteringPreset,
            selectedTemplateId = template.id
        )
    }

    fun saveCurrentAsTemplate(name: String, description: String, category: String) {
        val currentState = _uiState.value
        val newTemplate = ProjectTemplate(
            id = "tpl_custom_${System.currentTimeMillis()}",
            name = name.ifBlank { "Custom Layout Template" },
            description = description.ifBlank { "User saved video editing settings template." },
            category = category.ifBlank { "Custom" },
            playbackSpeed = currentState.playbackSpeed,
            colorGradingConfig = currentState.colorGradingConfig,
            transitionConfig = currentState.transitionConfig,
            masteringPreset = currentState.audioMasteringPreset,
            isBuiltIn = false,
            createdAtTimestamp = System.currentTimeMillis()
        )
        val updatedList = currentState.projectTemplates + newTemplate
        _uiState.value = currentState.copy(
            projectTemplates = updatedList,
            selectedTemplateId = newTemplate.id
        )
    }

    fun deleteCustomTemplate(templateId: String) {
        val currentState = _uiState.value
        val updatedList = currentState.projectTemplates.filterNot { it.id == templateId && !it.isBuiltIn }
        val newSelected = if (currentState.selectedTemplateId == templateId) null else currentState.selectedTemplateId
        _uiState.value = currentState.copy(
            projectTemplates = updatedList,
            selectedTemplateId = newSelected
        )
    }

    private fun checkAutoSaveSessionOnStartup() {
        val manager = autoSaveManager ?: return
        viewModelScope.launch {
            val info = manager.getSessionInfo()
            if (info.exists) {
                _uiState.value = _uiState.value.copy(
                    autoSaveSessionInfo = info,
                    showRecoveryBanner = true,
                    lastSavedMessage = "Recovery session from ${info.formattedTime}"
                )
            }
        }
    }

    private fun startAutoSaveLoop() {
        if (autoSaveManager == null) return
        viewModelScope.launch {
            while (true) {
                val interval = _uiState.value.autoSaveIntervalSec.coerceAtLeast(3)
                delay(interval * 1000L)
                if (_uiState.value.autoSaveEnabled && (_uiState.value.selectedMediaUri != null || _uiState.value.titleText != "LoopingVid Live Studio")) {
                    performAutoSaveInternal()
                }
            }
        }
    }

    fun triggerManualSaveNow() {
        viewModelScope.launch {
            performAutoSaveInternal()
        }
    }

    private suspend fun performAutoSaveInternal() {
        val manager = autoSaveManager ?: return
        _uiState.value = _uiState.value.copy(isSavingInProgress = true)
        val success = manager.saveSession(_uiState.value)
        val info = manager.getSessionInfo()
        _uiState.value = _uiState.value.copy(
            isSavingInProgress = false,
            autoSaveSessionInfo = info,
            lastSavedMessage = if (success) "Auto-saved at ${info.formattedTime}" else "Auto-save failed"
        )
    }

    fun restoreAutoSavedSession() {
        val manager = autoSaveManager ?: return
        viewModelScope.launch {
            val savedState = manager.loadSavedSession()
            if (savedState != null) {
                val info = manager.getSessionInfo()
                _uiState.value = savedState.copy(
                    autoSaveEnabled = _uiState.value.autoSaveEnabled,
                    autoSaveIntervalSec = _uiState.value.autoSaveIntervalSec,
                    autoSaveSessionInfo = info,
                    showRecoveryBanner = false,
                    lastSavedMessage = "Restored session from ${info.formattedTime}"
                )
            }
        }
    }

    fun discardAutoSavedSession() {
        val manager = autoSaveManager ?: return
        viewModelScope.launch {
            manager.discardSession()
            _uiState.value = _uiState.value.copy(
                autoSaveSessionInfo = AutoSaveSessionInfo(exists = false),
                showRecoveryBanner = false,
                lastSavedMessage = "Draft session cleared"
            )
        }
    }

    fun toggleAutoSave(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(
            autoSaveEnabled = enabled,
            lastSavedMessage = if (enabled) "Auto-save active (${_uiState.value.autoSaveIntervalSec}s)" else "Auto-save disabled"
        )
    }

    fun setAutoSaveIntervalSec(intervalSec: Int) {
        _uiState.value = _uiState.value.copy(
            autoSaveIntervalSec = intervalSec,
            lastSavedMessage = "Interval set to ${intervalSec}s"
        )
    }

    fun dismissRecoveryBanner() {
        _uiState.value = _uiState.value.copy(showRecoveryBanner = false)
    }

    fun startEditorExport(customFileName: String? = null, destinationFolder: String? = null, exportFormat: String = "mp4") {
        val state = _uiState.value
        val mediaUri = state.selectedMediaUri ?: return

        viewModelScope.launch {
            try {
                mediaProcessor.executeEditorJob(
                    mediaUri = mediaUri,
                    audioUri = state.selectedAudioUri,
                    titleText = state.titleText,
                    watermarkText = state.watermarkText,
                    spectrumStyle = state.spectrumStyle.name,
                    presetQuality = state.presetQuality,
                    filterString = state.colorGradingConfig.buildFfmpegFilterString(),
                    customFileName = customFileName,
                    destinationFolder = destinationFolder,
                    exportFormat = exportFormat,
                    overlayUri = state.selectedOverlayUri,
                    overlayPosition = state.pipPosition.name
                )
            } catch (e: Exception) {
                // Handled in progress state
            }
        }
    }

    fun generateAutoCaptions() {
        val mediaUri = _uiState.value.selectedMediaUri ?: return
        if (context == null) return
        
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(jobProgress = com.example.core.ffmpeg.JobProgressState(statusText = "Analyzing audio with Gemini 1.5 Pro to generate captions...", progress = 10, isProcessing = true))
            
            val dao = com.example.core.database.AppDatabase.getDatabase(context).appSettingDao()
            val apiKey = dao.getValueByKey("gemini_api_key") ?: ""
            
            val generator = com.example.core.media.GeminiCaptionGenerator(context)
            val srtCaptions = generator.generateCaptionsForVideo(mediaUri, apiKey)
            
            if (srtCaptions.startsWith("Error:")) {
                _uiState.value = _uiState.value.copy(
                    jobProgress = com.example.core.ffmpeg.JobProgressState(statusText = srtCaptions, progress = 0, isProcessing = false)
                )
            } else {
                _uiState.value = _uiState.value.copy(
                    aiGeneratedCaptions = srtCaptions,
                    jobProgress = com.example.core.ffmpeg.JobProgressState(statusText = "Captions Generated Successfully!", progress = 100, isProcessing = false)
                )
            }
        }
    }
}

