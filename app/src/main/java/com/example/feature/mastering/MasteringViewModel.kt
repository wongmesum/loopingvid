package com.example.feature.mastering

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.command.Command
import com.example.core.command.UndoRedoManager
import com.example.core.command.UndoRedoState
import com.example.core.ffmpeg.JobProgressState
import com.example.core.ffmpeg.MediaProcessor
import com.example.core.media.AudioAnalysisData
import com.example.core.media.AudioMasteringEngine
import com.example.core.media.CompressorConfig
import com.example.core.media.EqBandConfig
import com.example.core.media.MasteringPreset
import com.example.core.media.AutoLevelingConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

import com.example.core.media.NoiseReductionConfig
import com.example.core.media.VisualizerTheme
import com.example.core.media.VisualizerBarMode
import com.example.core.media.PeakMeterStyle

data class MasteringUiState(
    val selectedAudioUri: String? = null,
    val selectedAudioName: String = "",
    val analysisData: AudioAnalysisData? = null,
    val selectedPreset: MasteringPreset = AudioMasteringEngine.PRESETS.first(),
    val customPresets: List<MasteringPreset> = emptyList(),
    val eqConfig: EqBandConfig = EqBandConfig(),
    val compressorConfig: CompressorConfig = CompressorConfig(),
    val noiseReductionConfig: NoiseReductionConfig = NoiseReductionConfig(),
    val autoLevelingConfig: AutoLevelingConfig = AutoLevelingConfig(),
    val visualizerTheme: VisualizerTheme = VisualizerTheme.CYAN_PINK,
    val visualizerBarMode: VisualizerBarMode = VisualizerBarMode.BARS,
    val peakMeterStyle: PeakMeterStyle = PeakMeterStyle.SEGMENTED_LED,
    val inputGainDb: Float = 0f,
    val outputGainDb: Float = 0f,
    val targetLufs: Double = -14.0,
    val fadeInSec: Float = 0f,
    val fadeOutSec: Float = 0f,
    val exportFormat: String = "MP3", // "WAV", "MP3", "M4A"
    val calculatedOutputLufs: Double = -14.0,
    val audioMetadata: com.example.core.media.AudioMetadata = com.example.core.media.AudioMetadata(),
    val jobProgress: JobProgressState = JobProgressState(),
    val lastMasteredOutputUri: String? = null
)

class MasteringViewModel(
    private val mediaProcessor: MediaProcessor,
    private val context: Context? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(MasteringUiState())
    val uiState: StateFlow<MasteringUiState> = _uiState.asStateFlow()

    val undoRedoManager = UndoRedoManager()
    val undoRedoState: StateFlow<UndoRedoState> = undoRedoManager.state

    private class MasteringCommand(
        override val actionName: String,
        private val onExecute: () -> Unit,
        private val onUndo: () -> Unit
    ) : Command {
        override fun execute() = onExecute()
        override fun undo() = onUndo()
    }

    init {
        loadCustomPresetsFromPreferences()
        viewModelScope.launch(Dispatchers.Default) {
            mediaProcessor.progressState.collect { progress ->
                _uiState.update { current ->
                    current.copy(
                        jobProgress = progress,
                        lastMasteredOutputUri = if (progress.progress == 100) progress.outputFilePath else current.lastMasteredOutputUri
                    )
                }
            }
        }
    }

    private fun loadCustomPresetsFromPreferences() {
        context?.let { ctx ->
            try {
                val prefs = ctx.getSharedPreferences("mastering_prefs", Context.MODE_PRIVATE)
                val jsonStr = prefs.getString("custom_presets_json", null) ?: return@let
                val array = JSONArray(jsonStr)
                val loadedList = mutableListOf<MasteringPreset>()
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    val preset = MasteringPreset(
                        name = obj.optString("name", "Custom Preset $i"),
                        eqBandConfig = EqBandConfig(
                            lowGainDb = obj.optDouble("lowGainDb", 0.0).toFloat(),
                            midLowGainDb = obj.optDouble("midLowGainDb", 0.0).toFloat(),
                            midGainDb = obj.optDouble("midGainDb", 0.0).toFloat(),
                            midHighGainDb = obj.optDouble("midHighGainDb", 0.0).toFloat(),
                            highGainDb = obj.optDouble("highGainDb", 0.0).toFloat()
                        ),
                        compressorConfig = CompressorConfig(
                            thresholdDb = obj.optDouble("compThreshold", -18.0).toFloat(),
                            ratio = obj.optDouble("compRatio", 3.0).toFloat(),
                            attackMs = obj.optDouble("compAttack", 15.0).toFloat(),
                            releaseMs = obj.optDouble("compRelease", 100.0).toFloat(),
                            makeupGainDb = obj.optDouble("compMakeup", 3.0).toFloat()
                        ),
                        noiseReductionConfig = NoiseReductionConfig(
                            isEnabled = obj.optBoolean("noiseReduxEnabled", false),
                            reductionDb = obj.optDouble("noiseReduxDb", 14.0).toFloat(),
                            noiseFloorDb = obj.optDouble("noiseFloorDb", -48.0).toFloat()
                        ),
                        autoLevelingConfig = AutoLevelingConfig(
                            isEnabled = obj.optBoolean("autoLevelEnabled", false),
                            targetLoudnessLufs = obj.optDouble("autoLevelTarget", -14.0).toFloat()
                        ),
                        targetLufs = obj.optDouble("targetLufs", -14.0),
                        inputGainDb = obj.optDouble("inputGainDb", 0.0).toFloat(),
                        outputGainDb = obj.optDouble("outputGainDb", 0.0).toFloat(),
                        fadeInSec = obj.optDouble("fadeInSec", 0.0).toFloat(),
                        fadeOutSec = obj.optDouble("fadeOutSec", 0.0).toFloat(),
                        isCustom = true,
                        id = obj.optString("id", "custom_${System.currentTimeMillis()}_$i")
                    )
                    loadedList.add(preset)
                }
                _uiState.value = _uiState.value.copy(customPresets = loadedList)
            } catch (e: Exception) {
                // Ignore corrupt prefs
            }
        }
    }

    private fun saveCustomPresetsToPreferences(presets: List<MasteringPreset>) {
        context?.let { ctx ->
            try {
                val prefs = ctx.getSharedPreferences("mastering_prefs", Context.MODE_PRIVATE)
                val array = JSONArray()
                presets.forEach { preset ->
                    val obj = JSONObject().apply {
                        put("id", preset.id)
                        put("name", preset.name)
                        put("targetLufs", preset.targetLufs)
                        put("inputGainDb", preset.inputGainDb.toDouble())
                        put("outputGainDb", preset.outputGainDb.toDouble())
                        put("fadeInSec", preset.fadeInSec.toDouble())
                        put("fadeOutSec", preset.fadeOutSec.toDouble())
                        put("lowGainDb", preset.eqBandConfig.lowGainDb.toDouble())
                        put("midLowGainDb", preset.eqBandConfig.midLowGainDb.toDouble())
                        put("midGainDb", preset.eqBandConfig.midGainDb.toDouble())
                        put("midHighGainDb", preset.eqBandConfig.midHighGainDb.toDouble())
                        put("highGainDb", preset.eqBandConfig.highGainDb.toDouble())
                        put("compThreshold", preset.compressorConfig.thresholdDb.toDouble())
                        put("compRatio", preset.compressorConfig.ratio.toDouble())
                        put("compAttack", preset.compressorConfig.attackMs.toDouble())
                        put("compRelease", preset.compressorConfig.releaseMs.toDouble())
                        put("compMakeup", preset.compressorConfig.makeupGainDb.toDouble())
                        put("noiseReduxEnabled", preset.noiseReductionConfig.isEnabled)
                        put("noiseReduxDb", preset.noiseReductionConfig.reductionDb.toDouble())
                        put("noiseFloorDb", preset.noiseReductionConfig.noiseFloorDb.toDouble())
                        put("autoLevelEnabled", preset.autoLevelingConfig.isEnabled)
                        put("autoLevelTarget", preset.autoLevelingConfig.targetLoudnessLufs.toDouble())
                    }
                    array.put(obj)
                }
                prefs.edit().putString("custom_presets_json", array.toString()).apply()
            } catch (e: Exception) {
                // Ignore save errors
            }
        }
    }

    fun saveCurrentPreset(presetName: String): MasteringPreset {
        val trimmedName = presetName.trim().ifEmpty { "My Preset ${System.currentTimeMillis() % 1000}" }
        val currentState = _uiState.value
        val newPreset = MasteringPreset(
            name = trimmedName,
            eqBandConfig = currentState.eqConfig,
            compressorConfig = currentState.compressorConfig,
            noiseReductionConfig = currentState.noiseReductionConfig,
            autoLevelingConfig = currentState.autoLevelingConfig,
            targetLufs = currentState.targetLufs,
            inputGainDb = currentState.inputGainDb,
            outputGainDb = currentState.outputGainDb,
            fadeInSec = currentState.fadeInSec,
            fadeOutSec = currentState.fadeOutSec,
            isCustom = true,
            id = "custom_${System.currentTimeMillis()}"
        )

        val updatedCustomList = currentState.customPresets.filter { it.name != trimmedName } + newPreset
        _uiState.value = _uiState.value.copy(
            customPresets = updatedCustomList,
            selectedPreset = newPreset
        )
        saveCustomPresetsToPreferences(updatedCustomList)
        return newPreset
    }

    fun deleteCustomPreset(preset: MasteringPreset) {
        if (!preset.isCustom) return
        val currentState = _uiState.value
        val updatedCustomList = currentState.customPresets.filter { it.id != preset.id && it.name != preset.name }
        val newSelected = if (currentState.selectedPreset.id == preset.id || currentState.selectedPreset.name == preset.name) {
            AudioMasteringEngine.PRESETS.first()
        } else {
            currentState.selectedPreset
        }
        _uiState.value = _uiState.value.copy(
            customPresets = updatedCustomList,
            selectedPreset = newSelected
        )
        saveCustomPresetsToPreferences(updatedCustomList)
    }

    fun setVisualizerTheme(theme: VisualizerTheme) {
        _uiState.update { it.copy(visualizerTheme = theme) }
    }

    fun setVisualizerBarMode(mode: VisualizerBarMode) {
        _uiState.update { it.copy(visualizerBarMode = mode) }
    }

    fun setPeakMeterStyle(style: PeakMeterStyle) {
        _uiState.update { it.copy(peakMeterStyle = style) }
    }

    fun undo() { undoRedoManager.undo() }
    fun redo() { undoRedoManager.redo() }
    fun clearHistory() { undoRedoManager.clear() }

    fun onAudioSelected(uri: String, fileName: String) {
        // Analysis data will be populated by AudioAnalysisRepository integration (Checkpoint 2).
        // Until then, null signals "analysis pending" and UI shows placeholder.
        _uiState.value = _uiState.value.copy(
            selectedAudioUri = uri,
            selectedAudioName = fileName,
            analysisData = null
        )
        recalculateLufs()
        undoRedoManager.clear()
    }

    fun applyPreset(preset: MasteringPreset) {
        val oldPreset = _uiState.value.selectedPreset
        val oldEq = _uiState.value.eqConfig
        val oldComp = _uiState.value.compressorConfig
        val oldNoise = _uiState.value.noiseReductionConfig
        val oldAutoLevel = _uiState.value.autoLevelingConfig
        val oldTarget = _uiState.value.targetLufs
        val oldFadeIn = _uiState.value.fadeInSec
        val oldFadeOut = _uiState.value.fadeOutSec

        if (oldPreset == preset) return

        undoRedoManager.executeCommand(
            MasteringCommand(
                actionName = "Preset (${preset.name})",
                onExecute = {
                    _uiState.value = _uiState.value.copy(
                        selectedPreset = preset,
                        eqConfig = preset.eqBandConfig,
                        compressorConfig = preset.compressorConfig,
                        noiseReductionConfig = preset.noiseReductionConfig,
                        autoLevelingConfig = preset.autoLevelingConfig,
                        targetLufs = preset.targetLufs,
                        fadeInSec = preset.fadeInSec,
                        fadeOutSec = preset.fadeOutSec
                    )
                    recalculateLufs()
                },
                onUndo = {
                    _uiState.value = _uiState.value.copy(
                        selectedPreset = oldPreset,
                        eqConfig = oldEq,
                        compressorConfig = oldComp,
                        noiseReductionConfig = oldNoise,
                        autoLevelingConfig = oldAutoLevel,
                        targetLufs = oldTarget,
                        fadeInSec = oldFadeIn,
                        fadeOutSec = oldFadeOut
                    )
                    recalculateLufs()
                }
            )
        )
    }

    fun toggleNoiseReduction(enabled: Boolean) {
        val oldConfig = _uiState.value.noiseReductionConfig
        if (oldConfig.isEnabled == enabled) return
        val newConfig = oldConfig.copy(isEnabled = enabled)
        undoRedoManager.executeCommand(
            MasteringCommand(
                actionName = if (enabled) "Enable FFT Noise Reduction" else "Disable Noise Reduction",
                onExecute = { _uiState.value = _uiState.value.copy(noiseReductionConfig = newConfig) },
                onUndo = { _uiState.value = _uiState.value.copy(noiseReductionConfig = oldConfig) }
            )
        )
    }

    fun updateNoiseReductionDb(reductionDb: Float) {
        val oldConfig = _uiState.value.noiseReductionConfig
        if (oldConfig.reductionDb == reductionDb) return
        val newConfig = oldConfig.copy(reductionDb = reductionDb)
        undoRedoManager.executeCommand(
            MasteringCommand(
                actionName = "Noise Reduction (${reductionDb.toInt()}dB)",
                onExecute = { _uiState.value = _uiState.value.copy(noiseReductionConfig = newConfig) },
                onUndo = { _uiState.value = _uiState.value.copy(noiseReductionConfig = oldConfig) }
            )
        )
    }

    fun updateNoiseFloorDb(noiseFloorDb: Float) {
        val oldConfig = _uiState.value.noiseReductionConfig
        if (oldConfig.noiseFloorDb == noiseFloorDb) return
        val newConfig = oldConfig.copy(noiseFloorDb = noiseFloorDb)
        undoRedoManager.executeCommand(
            MasteringCommand(
                actionName = "Noise Floor Floor (${noiseFloorDb.toInt()}dB)",
                onExecute = { _uiState.value = _uiState.value.copy(noiseReductionConfig = newConfig) },
                onUndo = { _uiState.value = _uiState.value.copy(noiseReductionConfig = oldConfig) }
            )
        )
    }

    fun updateFftSize(fftSize: Int) {
        val oldConfig = _uiState.value.noiseReductionConfig
        if (oldConfig.fftSize == fftSize) return
        val newConfig = oldConfig.copy(fftSize = fftSize)
        undoRedoManager.executeCommand(
            MasteringCommand(
                actionName = "FFT Window Size ($fftSize)",
                onExecute = { _uiState.value = _uiState.value.copy(noiseReductionConfig = newConfig) },
                onUndo = { _uiState.value = _uiState.value.copy(noiseReductionConfig = oldConfig) }
            )
        )
    }

    fun toggleAutoLeveling(enabled: Boolean) {
        val oldConfig = _uiState.value.autoLevelingConfig
        if (oldConfig.isEnabled == enabled) return
        val newConfig = oldConfig.copy(isEnabled = enabled)
        undoRedoManager.executeCommand(
            MasteringCommand(
                actionName = if (enabled) "Enable Auto Leveling" else "Disable Auto Leveling",
                onExecute = { 
                    _uiState.value = _uiState.value.copy(autoLevelingConfig = newConfig)
                    recalculateLufs()
                },
                onUndo = { 
                    _uiState.value = _uiState.value.copy(autoLevelingConfig = oldConfig)
                    recalculateLufs()
                }
            )
        )
    }

    fun updateAutoLevelingTarget(targetLufs: Float) {
        val oldConfig = _uiState.value.autoLevelingConfig
        if (oldConfig.targetLoudnessLufs == targetLufs) return
        val newConfig = oldConfig.copy(targetLoudnessLufs = targetLufs)
        undoRedoManager.executeCommand(
            MasteringCommand(
                actionName = "Auto Level Target ($targetLufs LUFS)",
                onExecute = { 
                    _uiState.value = _uiState.value.copy(autoLevelingConfig = newConfig)
                    recalculateLufs()
                },
                onUndo = { 
                    _uiState.value = _uiState.value.copy(autoLevelingConfig = oldConfig)
                    recalculateLufs()
                }
            )
        )
    }

    fun updateInputGain(gainDb: Float) {
        val oldGain = _uiState.value.inputGainDb
        if (oldGain == gainDb) return
        undoRedoManager.executeCommand(
            MasteringCommand(
                actionName = "Input Gain (%+.1fdB)".format(gainDb),
                onExecute = {
                    _uiState.value = _uiState.value.copy(inputGainDb = gainDb)
                    recalculateLufs()
                },
                onUndo = {
                    _uiState.value = _uiState.value.copy(inputGainDb = oldGain)
                    recalculateLufs()
                }
            )
        )
    }

    fun updateOutputGain(gainDb: Float) {
        val oldGain = _uiState.value.outputGainDb
        if (oldGain == gainDb) return
        undoRedoManager.executeCommand(
            MasteringCommand(
                actionName = "Output Gain (%+.1fdB)".format(gainDb),
                onExecute = {
                    _uiState.value = _uiState.value.copy(outputGainDb = gainDb)
                    recalculateLufs()
                },
                onUndo = {
                    _uiState.value = _uiState.value.copy(outputGainDb = oldGain)
                    recalculateLufs()
                }
            )
        )
    }

    fun updateEqLow(gainDb: Float) {
        val oldEq = _uiState.value.eqConfig
        if (oldEq.lowGainDb == gainDb) return
        val newEq = oldEq.copy(lowGainDb = gainDb)
        undoRedoManager.executeCommand(
            MasteringCommand(
                actionName = "EQ Low Band (%+.1fdB)".format(gainDb),
                onExecute = {
                    _uiState.value = _uiState.value.copy(eqConfig = newEq)
                    recalculateLufs()
                },
                onUndo = {
                    _uiState.value = _uiState.value.copy(eqConfig = oldEq)
                    recalculateLufs()
                }
            )
        )
    }

    fun updateEqMidLow(gainDb: Float) {
        val oldEq = _uiState.value.eqConfig
        if (oldEq.midLowGainDb == gainDb) return
        val newEq = oldEq.copy(midLowGainDb = gainDb)
        undoRedoManager.executeCommand(
            MasteringCommand(
                actionName = "EQ Mid-Low (%+.1fdB)".format(gainDb),
                onExecute = {
                    _uiState.value = _uiState.value.copy(eqConfig = newEq)
                    recalculateLufs()
                },
                onUndo = {
                    _uiState.value = _uiState.value.copy(eqConfig = oldEq)
                    recalculateLufs()
                }
            )
        )
    }

    fun updateEqMid(gainDb: Float) {
        val oldEq = _uiState.value.eqConfig
        if (oldEq.midGainDb == gainDb) return
        val newEq = oldEq.copy(midGainDb = gainDb)
        undoRedoManager.executeCommand(
            MasteringCommand(
                actionName = "EQ Mid Band (%+.1fdB)".format(gainDb),
                onExecute = {
                    _uiState.value = _uiState.value.copy(eqConfig = newEq)
                    recalculateLufs()
                },
                onUndo = {
                    _uiState.value = _uiState.value.copy(eqConfig = oldEq)
                    recalculateLufs()
                }
            )
        )
    }

    fun updateEqMidHigh(gainDb: Float) {
        val oldEq = _uiState.value.eqConfig
        if (oldEq.midHighGainDb == gainDb) return
        val newEq = oldEq.copy(midHighGainDb = gainDb)
        undoRedoManager.executeCommand(
            MasteringCommand(
                actionName = "EQ Mid-High (%+.1fdB)".format(gainDb),
                onExecute = {
                    _uiState.value = _uiState.value.copy(eqConfig = newEq)
                    recalculateLufs()
                },
                onUndo = {
                    _uiState.value = _uiState.value.copy(eqConfig = oldEq)
                    recalculateLufs()
                }
            )
        )
    }

    fun updateEqHigh(gainDb: Float) {
        val oldEq = _uiState.value.eqConfig
        if (oldEq.highGainDb == gainDb) return
        val newEq = oldEq.copy(highGainDb = gainDb)
        undoRedoManager.executeCommand(
            MasteringCommand(
                actionName = "EQ High Band (%+.1fdB)".format(gainDb),
                onExecute = {
                    _uiState.value = _uiState.value.copy(eqConfig = newEq)
                    recalculateLufs()
                },
                onUndo = {
                    _uiState.value = _uiState.value.copy(eqConfig = oldEq)
                    recalculateLufs()
                }
            )
        )
    }

    fun setTargetLufs(target: Double) {
        val oldTarget = _uiState.value.targetLufs
        if (oldTarget == target) return
        undoRedoManager.executeCommand(
            MasteringCommand(
                actionName = "Target LUFS (%.1f)".format(target),
                onExecute = {
                    _uiState.value = _uiState.value.copy(targetLufs = target)
                    recalculateLufs()
                },
                onUndo = {
                    _uiState.value = _uiState.value.copy(targetLufs = oldTarget)
                    recalculateLufs()
                }
            )
        )
    }

    fun setExportFormat(format: String) {
        _uiState.value = _uiState.value.copy(exportFormat = format)
    }

    private fun recalculateLufs() {
        val state = _uiState.value
        val inputLufs = (state.analysisData?.currentRmsLufs ?: -22.0) + state.inputGainDb
        val baseOutLufs = AudioMasteringEngine.calculateOutputLufs(
            inputRmsLufs = inputLufs,
            eqConfig = state.eqConfig,
            compConfig = state.compressorConfig,
            autoLevelConfig = state.autoLevelingConfig,
            targetLufs = state.targetLufs
        )
        val finalOutLufs = baseOutLufs + state.outputGainDb
        _uiState.value = _uiState.value.copy(calculatedOutputLufs = finalOutLufs)
    }

    private var sliderStartFadeIn: Float? = null
    private var sliderStartFadeOut: Float? = null

    fun updateFadeInPreview(fadeInSec: Float) {
        if (sliderStartFadeIn == null) {
            sliderStartFadeIn = _uiState.value.fadeInSec
        }
        _uiState.value = _uiState.value.copy(fadeInSec = fadeInSec)
    }

    fun commitFadeInChange(fadeInSec: Float) {
        val oldVal = sliderStartFadeIn ?: _uiState.value.fadeInSec
        sliderStartFadeIn = null
        if (oldVal == fadeInSec) return
        undoRedoManager.executeCommand(
            MasteringCommand(
                actionName = "Fade-In (%.1fs)".format(fadeInSec),
                onExecute = { _uiState.value = _uiState.value.copy(fadeInSec = fadeInSec) },
                onUndo = { _uiState.value = _uiState.value.copy(fadeInSec = oldVal) }
            )
        )
    }

    fun updateFadeIn(fadeInSec: Float) {
        updateFadeInPreview(fadeInSec)
        commitFadeInChange(fadeInSec)
    }

    fun updateFadeOutPreview(fadeOutSec: Float) {
        if (sliderStartFadeOut == null) {
            sliderStartFadeOut = _uiState.value.fadeOutSec
        }
        _uiState.value = _uiState.value.copy(fadeOutSec = fadeOutSec)
    }

    fun commitFadeOutChange(fadeOutSec: Float) {
        val oldVal = sliderStartFadeOut ?: _uiState.value.fadeOutSec
        sliderStartFadeOut = null
        if (oldVal == fadeOutSec) return
        undoRedoManager.executeCommand(
            MasteringCommand(
                actionName = "Fade-Out (%.1fs)".format(fadeOutSec),
                onExecute = { _uiState.value = _uiState.value.copy(fadeOutSec = fadeOutSec) },
                onUndo = { _uiState.value = _uiState.value.copy(fadeOutSec = oldVal) }
            )
        )
    }

    fun updateFadeOut(fadeOutSec: Float) {
        updateFadeOutPreview(fadeOutSec)
        commitFadeOutChange(fadeOutSec)
    }

    fun updateAudioMetadata(metadata: com.example.core.media.AudioMetadata) {
        _uiState.update { it.copy(audioMetadata = metadata) }
    }

    fun startMasteringExport(customFileName: String? = null, destinationFolder: String? = null) {
        val state = _uiState.value
        val inputUri = state.selectedAudioUri ?: return

        viewModelScope.launch(Dispatchers.IO) {
            try {
                mediaProcessor.executeMasteringJob(
                    inputUri = inputUri,
                    presetName = state.selectedPreset.name,
                    targetLufs = state.targetLufs,
                    exportFormat = state.exportFormat,
                    customFileName = customFileName,
                    destinationFolder = destinationFolder,
                    isNoiseReductionEnabled = state.noiseReductionConfig.isEnabled,
                    noiseReductionDb = state.noiseReductionConfig.reductionDb,
                    noiseFloorDb = state.noiseReductionConfig.noiseFloorDb,
                    isAutoLevelingEnabled = state.autoLevelingConfig.isEnabled,
                    autoLevelingTargetLufs = state.autoLevelingConfig.targetLoudnessLufs,
                    fadeInSec = state.fadeInSec,
                    fadeOutSec = state.fadeOutSec
                )
            } catch (e: Exception) {
                // Handled in progress state
            }
        }
    }
}

