package com.example.core.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.ffmpeg.MediaProcessor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ExportUiState(
    val showDialog: Boolean = false,
    val fileName: String = "",
    val availableFormats: List<String> = emptyList(),
    val selectedFormat: String = "",
    val destinations: List<String> = listOf("Downloads", "Movies", "Music", "Documents"),
    val selectedDestination: String = "Downloads",
    val jobType: String = "", // "LOOP", "MASTERING", "EDITOR"
    val isFastPreview: Boolean = false,
    val selectedResolution: String = "1080p",
    val selectedFrameRate: String = "30fps",
    val selectedBitrate: String = "Medium",
    val selectedAspectRatio: String = "Asli",
    val completedExportSummary: ExportSummaryData? = null,
    // ID3/container metadata (title/artist/album/genre/year/comment/cover art etc.) collected
    // directly in the export dialog, since it only matters at export time. Applied onto
    // currentJobConfig right before dispatching to MediaProcessor in confirmExport().
    val metadata: com.example.core.media.AudioMetadata = com.example.core.media.AudioMetadata()
)

sealed class ExportJobConfig {
    data class LoopJob(
        val inputUri: String,
        val targetDurationSec: Double,
        val loopStyle: String,
        val crossfadeDurationSec: Double = 1.0,
        val trimStartSec: Double = 0.0,
        val trimEndSec: Double = 0.0,
        val muteAudio: Boolean = false,
        val audioFadeInSec: Double = 0.0,
        val audioFadeOutSec: Double = 0.0,
        val presetQuality: String = "1080p",
        val metadata: com.example.core.media.AudioMetadata? = null
    ) : ExportJobConfig()
    
    data class MasteringJob(
        val inputUri: String,
        val presetName: String,
        val targetLufs: Double,
        val isNoiseReductionEnabled: Boolean = false,
        val noiseReductionDb: Float = 12f,
        val noiseFloorDb: Float = -45f,
        val isAutoLevelingEnabled: Boolean = false,
        val autoLevelingTargetLufs: Float = -14.0f,
        val fadeInSec: Float = 0f,
        val fadeOutSec: Float = 0f,
        val audioMetadata: com.example.core.media.AudioMetadata? = null,
        val eqConfig: com.example.core.media.EqBandConfig? = null,
        val compressorConfig: com.example.core.media.CompressorConfig? = null,
        val inputGainDb: Float = 0f,
        val outputGainDb: Float = 0f
    ) : ExportJobConfig()
    
    data class TwoPassAudioNormalizationJob(
        val inputUri: String,
        val targetLufs: Double
    ) : ExportJobConfig()
    
    data class TrimmedVideoMasteringJob(
        val inputUri: String,
        val trimStartSec: Double,
        val trimEndSec: Double,
        val presetName: String = "Voice Clarity",
        val targetLufs: Double = -14.0,
        val presetQuality: String = "1080p",
        val ffmpegFilterString: String? = null,
        val metadata: com.example.core.media.AudioMetadata? = null
    ) : ExportJobConfig()

    data class EditorJob(
        val mediaUri: String,
        val audioUri: String?,
        val titleText: String,
        val watermarkText: String,
        val spectrumStyle: String,
        val presetQuality: String = "1080p",
        val ffmpegFilterString: String? = null,
        val audioMasteringPreset: com.example.core.media.MasteringPreset? = null,
        val overlayUri: String? = null,
        val overlayPosition: String? = null,
        val playbackSpeed: Float = 1.0f,
        val trimStartSec: Double = 0.0,
        val trimEndSec: Double = 0.0,
        val subtitleSrt: String? = null,
        val metadata: com.example.core.media.AudioMetadata? = null
    ) : ExportJobConfig()
}

class ExportViewModel(
    private val mediaProcessor: MediaProcessor,
    private val repository: com.example.core.database.LoopingVidRepository? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(ExportUiState())
    val uiState: StateFlow<ExportUiState> = _uiState.asStateFlow()

    private var currentJobConfig: ExportJobConfig? = null

    // Default export destination loaded from user settings; falls back to the state default.
    private var defaultDestination: String = _uiState.value.selectedDestination

    init {
        repository?.let { repo ->
            // Keep the default export destination in sync with the user's Settings choice.
            viewModelScope.launch {
                repo.observeSettingValue("output_dir").collect { saved ->
                    if (!saved.isNullOrBlank() && saved in _uiState.value.destinations) {
                        defaultDestination = saved
                        // If the dialog isn't currently open, reflect the new default immediately.
                        if (!_uiState.value.showDialog) {
                            _uiState.value = _uiState.value.copy(selectedDestination = saved)
                        }
                    }
                }
            }
        }
    }

    fun showDialogForLoop(defaultFileName: String, config: ExportJobConfig.LoopJob) {
        currentJobConfig = config
        val formats = listOf("mp4", "mkv", "mov")
        _uiState.value = _uiState.value.copy(
            showDialog = true,
            fileName = defaultFileName,
            availableFormats = formats,
            selectedFormat = formats.firstOrNull() ?: "",
            selectedDestination = defaultDestination,
            jobType = "LOOP",
            metadata = config.metadata ?: com.example.core.media.AudioMetadata()
        )
    }

    fun showDialogForMastering(defaultFileName: String, config: ExportJobConfig.MasteringJob) {
        currentJobConfig = config
        val formats = listOf("mp3", "wav", "m4a")
        _uiState.value = _uiState.value.copy(
            showDialog = true,
            fileName = defaultFileName,
            availableFormats = formats,
            selectedFormat = formats.firstOrNull() ?: "",
            selectedDestination = defaultDestination,
            jobType = "MASTERING",
            metadata = config.audioMetadata ?: com.example.core.media.AudioMetadata()
        )
    }

    fun showDialogForTwoPassNormalization(defaultFileName: String, config: ExportJobConfig.TwoPassAudioNormalizationJob) {
        currentJobConfig = config
        val formats = listOf("mp4", "mkv", "mov") // Outputting video with normalized audio
        _uiState.value = _uiState.value.copy(
            showDialog = true,
            fileName = defaultFileName,
            availableFormats = formats,
            selectedFormat = formats.firstOrNull() ?: "",
            selectedDestination = defaultDestination,
            jobType = "MASTERING",
            metadata = com.example.core.media.AudioMetadata()
        )
    }

    fun showDialogForTrimmedVideoMastering(defaultFileName: String, config: ExportJobConfig.TrimmedVideoMasteringJob) {
        currentJobConfig = config
        val formats = listOf("mp4", "mkv", "mov")
        _uiState.value = _uiState.value.copy(
            showDialog = true,
            fileName = defaultFileName,
            availableFormats = formats,
            selectedFormat = formats.firstOrNull() ?: "",
            selectedDestination = defaultDestination,
            jobType = "MASTERING",
            metadata = config.metadata ?: com.example.core.media.AudioMetadata()
        )
    }

    fun showDialogForEditor(defaultFileName: String, config: ExportJobConfig.EditorJob) {
        currentJobConfig = config
        val formats = listOf("mp4", "mkv", "mov")
        _uiState.value = _uiState.value.copy(
            showDialog = true,
            fileName = defaultFileName,
            availableFormats = formats,
            selectedFormat = formats.firstOrNull() ?: "",
            selectedDestination = defaultDestination,
            jobType = "EDITOR",
            metadata = config.metadata ?: com.example.core.media.AudioMetadata()
        )
    }

    fun dismissDialog() {
        _uiState.value = _uiState.value.copy(showDialog = false, metadata = com.example.core.media.AudioMetadata())
        currentJobConfig = null
    }

    /** Updates the ID3/container metadata collected inside the export dialog. */
    fun updateMetadata(metadata: com.example.core.media.AudioMetadata) {
        _uiState.value = _uiState.value.copy(metadata = metadata)
    }

    fun updateFileName(name: String) {
        _uiState.value = _uiState.value.copy(fileName = name)
    }

    fun selectFormat(format: String) {
        _uiState.value = _uiState.value.copy(selectedFormat = format)
    }

    fun selectDestination(dest: String) {
        _uiState.value = _uiState.value.copy(selectedDestination = dest)
    }
    
    fun setExportSettings(resolution: String, frameRate: String, bitrate: String, aspectRatio: String = "Asli") {
        _uiState.value = _uiState.value.copy(
            selectedResolution = resolution,
            selectedFrameRate = frameRate,
            selectedBitrate = bitrate,
            selectedAspectRatio = aspectRatio
        )
    }

    fun setFastPreview(isFast: Boolean) {
        _uiState.value = _uiState.value.copy(isFastPreview = isFast)
    }

    fun getCurrentBatchRequest(fileName: String, format: String, destination: String): com.example.core.work.BatchExportRequest? {
        val config = currentJobConfig ?: return null
        val state = _uiState.value
        return when (config) {
            is ExportJobConfig.LoopJob -> com.example.core.work.BatchExportRequest(
                title = fileName,
                jobType = "LOOP",
                format = format,
                destinationFolder = destination,
                inputUri = config.inputUri,
                presetQuality = config.presetQuality,
                resolution = state.selectedResolution,
                frameRate = state.selectedFrameRate,
                bitrate = state.selectedBitrate,
                aspectRatio = state.selectedAspectRatio,
                targetDurationSec = config.targetDurationSec,
                loopStyle = config.loopStyle,
                audioMetadata = state.metadata
            )
            is ExportJobConfig.MasteringJob -> com.example.core.work.BatchExportRequest(
                title = fileName,
                jobType = "MASTERING",
                format = format,
                destinationFolder = destination,
                inputUri = config.inputUri,
                presetName = config.presetName,
                targetLufs = config.targetLufs,
                resolution = state.selectedResolution,
                frameRate = state.selectedFrameRate,
                bitrate = state.selectedBitrate,
                aspectRatio = state.selectedAspectRatio,
                audioMetadata = state.metadata,
                isNoiseReductionEnabled = config.isNoiseReductionEnabled,
                noiseReductionDb = config.noiseReductionDb,
                noiseFloorDb = config.noiseFloorDb,
                isAutoLevelingEnabled = config.isAutoLevelingEnabled,
                autoLevelingTargetLufs = config.autoLevelingTargetLufs,
                fadeInSec = config.fadeInSec,
                fadeOutSec = config.fadeOutSec
            )
            is ExportJobConfig.TwoPassAudioNormalizationJob -> com.example.core.work.BatchExportRequest(
                title = fileName,
                jobType = "NORMALIZATION",
                format = format,
                destinationFolder = destination,
                inputUri = config.inputUri,
                presetName = "2-Pass Normalization",
                targetLufs = config.targetLufs,
                resolution = state.selectedResolution,
                frameRate = state.selectedFrameRate,
                bitrate = state.selectedBitrate,
                aspectRatio = state.selectedAspectRatio
            )
            is ExportJobConfig.TrimmedVideoMasteringJob -> com.example.core.work.BatchExportRequest(
                title = fileName,
                jobType = "MASTERING",
                format = format,
                destinationFolder = destination,
                inputUri = config.inputUri,
                presetName = config.presetName,
                targetLufs = config.targetLufs,
                resolution = state.selectedResolution,
                frameRate = state.selectedFrameRate,
                bitrate = state.selectedBitrate,
                aspectRatio = state.selectedAspectRatio,
                audioMetadata = state.metadata
            )
            is ExportJobConfig.EditorJob -> com.example.core.work.BatchExportRequest(
                title = fileName,
                jobType = "EDITOR",
                format = format,
                destinationFolder = destination,
                inputUri = config.mediaUri,
                audioUri = config.audioUri,
                titleText = config.titleText,
                watermarkText = config.watermarkText,
                spectrumStyle = config.spectrumStyle,
                presetQuality = config.presetQuality,
                resolution = state.selectedResolution,
                frameRate = state.selectedFrameRate,
                bitrate = state.selectedBitrate,
                aspectRatio = state.selectedAspectRatio,
                overlayUri = config.overlayUri,
                overlayPosition = config.overlayPosition,
                audioMetadata = state.metadata
            )
        }
    }

    fun dismissSummary() {
        _uiState.value = _uiState.value.copy(completedExportSummary = null)
    }

    fun showSummaryForJob(
        title: String,
        filePath: String,
        fileSizeMb: Double = 12.5,
        durationSec: Double = 30.0,
        format: String = "mp4",
        jobType: String = "Video Export",
        presetQuality: String = "1080p"
    ) {
        val summary = ExportSummaryData(
            fileName = title,
            filePath = filePath,
            fileSizeMb = fileSizeMb,
            resolution = if (presetQuality == "480p") "480p (854x480)" else "1080p (1920x1080)",
            durationSec = durationSec,
            format = format,
            jobType = jobType,
            isVideo = format != "mp3" && format != "wav",
            presetQuality = presetQuality,
            aspectRatio = "9:16 Vertical (Shorts/Reels)"
        )
        _uiState.value = _uiState.value.copy(completedExportSummary = summary)
    }

    fun confirmExport() {
        val state = _uiState.value
        // Apply the metadata collected in the export dialog onto the frozen job config right
        // before dispatch. The metadata field is named differently across variants
        // (MasteringJob uses "audioMetadata", the others use "metadata"), and
        // TwoPassAudioNormalizationJob has no metadata field at all (normalization doesn't retag).
        val config = (currentJobConfig ?: return).let { cfg ->
            when (cfg) {
                is ExportJobConfig.LoopJob -> cfg.copy(metadata = state.metadata)
                is ExportJobConfig.MasteringJob -> cfg.copy(audioMetadata = state.metadata)
                is ExportJobConfig.TrimmedVideoMasteringJob -> cfg.copy(metadata = state.metadata)
                is ExportJobConfig.EditorJob -> cfg.copy(metadata = state.metadata)
                is ExportJobConfig.TwoPassAudioNormalizationJob -> cfg
            }
        }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val completedJob = when (config) {
                    is ExportJobConfig.LoopJob -> {
                        val quality = config.presetQuality
                        mediaProcessor.executeLoopJob(
                            inputUri = config.inputUri,
                            targetDurationSec = config.targetDurationSec,
                            loopStyle = config.loopStyle,
                            crossfadeDurationSec = config.crossfadeDurationSec,
                            trimStartSec = config.trimStartSec,
                            trimEndSec = config.trimEndSec,
                            muteAudio = config.muteAudio,
                            audioFadeInSec = config.audioFadeInSec,
                            audioFadeOutSec = config.audioFadeOutSec,
                            presetQuality = quality,
                            customFileName = state.fileName,
                            destinationFolder = state.selectedDestination,
                            exportFormat = state.selectedFormat,
                            resolution = state.selectedResolution,
                            frameRate = state.selectedFrameRate,
                            bitrate = state.selectedBitrate,
                            aspectRatio = state.selectedAspectRatio,
                            metadata = config.metadata
                        )
                    }
                    is ExportJobConfig.MasteringJob -> {
                        mediaProcessor.executeMasteringJob(
                            inputUri = config.inputUri,
                            presetName = config.presetName,
                            targetLufs = config.targetLufs,
                            exportFormat = state.selectedFormat,
                            customFileName = state.fileName,
                            destinationFolder = state.selectedDestination,
                            isNoiseReductionEnabled = config.isNoiseReductionEnabled,
                            noiseReductionDb = config.noiseReductionDb,
                            noiseFloorDb = config.noiseFloorDb,
                            isAutoLevelingEnabled = config.isAutoLevelingEnabled,
                            autoLevelingTargetLufs = config.autoLevelingTargetLufs,
                            fadeInSec = config.fadeInSec,
                            fadeOutSec = config.fadeOutSec,
                            audioMetadata = config.audioMetadata,
                            eqConfig = config.eqConfig,
                            compressorConfig = config.compressorConfig,
                            inputGainDb = config.inputGainDb,
                            outputGainDb = config.outputGainDb
                        )
                    }
                    is ExportJobConfig.TwoPassAudioNormalizationJob -> {
                        mediaProcessor.executeTwoPassAudioNormalization(
                            inputUri = config.inputUri,
                            targetLufs = config.targetLufs,
                            exportFormat = state.selectedFormat,
                            customFileName = state.fileName,
                            destinationFolder = state.selectedDestination
                        )
                    }
                    is ExportJobConfig.TrimmedVideoMasteringJob -> {
                        val quality = config.presetQuality
                        mediaProcessor.executeTrimmedVideoMasteringJob(
                            inputUri = config.inputUri,
                            trimStartSec = config.trimStartSec,
                            trimEndSec = config.trimEndSec,
                            presetName = config.presetName,
                            targetLufs = config.targetLufs,
                            presetQuality = quality,
                            videoFilterString = config.ffmpegFilterString,
                            customFileName = state.fileName,
                            destinationFolder = state.selectedDestination,
                            exportFormat = state.selectedFormat,
                            resolution = state.selectedResolution,
                            frameRate = state.selectedFrameRate,
                            bitrate = state.selectedBitrate,
                            aspectRatio = state.selectedAspectRatio,
                            metadata = config.metadata
                        )
                    }
                    is ExportJobConfig.EditorJob -> {
                        val quality = config.presetQuality
                        mediaProcessor.executeEditorJob(
                            mediaUri = config.mediaUri,
                            audioUri = config.audioUri,
                            titleText = config.titleText,
                            watermarkText = config.watermarkText,
                            spectrumStyle = config.spectrumStyle,
                            presetQuality = quality,
                            filterString = config.ffmpegFilterString,
                            customFileName = state.fileName,
                            destinationFolder = state.selectedDestination,
                            exportFormat = state.selectedFormat,
                            overlayUri = config.overlayUri,
                            overlayPosition = config.overlayPosition,
                            resolution = state.selectedResolution,
                            frameRate = state.selectedFrameRate,
                            bitrate = state.selectedBitrate,
                            aspectRatio = state.selectedAspectRatio,
                            playbackSpeed = config.playbackSpeed,
                            trimStartSec = config.trimStartSec,
                            trimEndSec = config.trimEndSec,
                            subtitleSrt = config.subtitleSrt,
                            metadata = config.metadata
                        )
                    }
                }

                val summary = ExportSummaryData(
                    fileName = state.fileName.ifBlank { completedJob.title },
                    filePath = completedJob.outputUri,
                    fileSizeMb = completedJob.fileSizeMb,
                    resolution = state.selectedResolution,
                    durationSec = completedJob.durationSec,
                    format = state.selectedFormat,
                    jobType = completedJob.jobType,
                    isVideo = completedJob.jobType != "MASTERING" || state.selectedFormat in listOf("mp4", "mkv", "mov"),
                    presetQuality = state.selectedResolution,
                    aspectRatio = "9:16 Vertical (Shorts/Reels)"
                )

                _uiState.value = _uiState.value.copy(completedExportSummary = summary)
            } catch (e: Exception) {
                // Handled in progress state
            }
        }
        dismissDialog()
    }
}
