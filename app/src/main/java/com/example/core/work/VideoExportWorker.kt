package com.example.core.work

import android.content.Context
import android.net.Uri
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.example.core.database.AppDatabase
import com.example.core.database.LoopingVidRepository
import com.example.core.ffmpeg.MediaProcessor
import com.example.feature.project.ProjectLifecycleManager
import kotlinx.coroutines.launch

class VideoExportWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        const val KEY_PROJECT_TITLE = "KEY_PROJECT_TITLE"
        const val KEY_JOB_TYPE = "KEY_JOB_TYPE" // "EDITOR", "LOOP", "MASTERING"
        const val KEY_EXPORT_FORMAT = "KEY_EXPORT_FORMAT"
        const val KEY_DESTINATION_FOLDER = "KEY_DESTINATION_FOLDER"
        const val KEY_INPUT_URI = "KEY_INPUT_URI"
        const val KEY_AUDIO_URI = "KEY_AUDIO_URI"
        const val KEY_TITLE_TEXT = "KEY_TITLE_TEXT"
        const val KEY_WATERMARK_TEXT = "KEY_WATERMARK_TEXT"
        const val KEY_SPECTRUM_STYLE = "KEY_SPECTRUM_STYLE"
        const val KEY_PRESET_QUALITY = "KEY_PRESET_QUALITY"
        const val KEY_TARGET_DURATION_SEC = "KEY_TARGET_DURATION_SEC"
        const val KEY_LOOP_STYLE = "KEY_LOOP_STYLE"
        const val KEY_TARGET_LUFS = "KEY_TARGET_LUFS"
        const val KEY_PRESET_NAME = "KEY_PRESET_NAME"
        const val KEY_FFMPEG_FILTER = "KEY_FFMPEG_FILTER"
        const val KEY_OVERLAY_URI = "KEY_OVERLAY_URI"
        const val KEY_OVERLAY_POSITION = "KEY_OVERLAY_POSITION"
        const val KEY_RESOLUTION = "KEY_RESOLUTION"
        const val KEY_FRAME_RATE = "KEY_FRAME_RATE"
        const val KEY_BITRATE = "KEY_BITRATE"
        const val KEY_ASPECT_RATIO = "KEY_ASPECT_RATIO"

        const val KEY_NOISE_REDUCTION_ENABLED = "KEY_NOISE_REDUCTION_ENABLED"
        const val KEY_NOISE_REDUCTION_DB = "KEY_NOISE_REDUCTION_DB"
        const val KEY_NOISE_FLOOR_DB = "KEY_NOISE_FLOOR_DB"
        const val KEY_AUTO_LEVELING_ENABLED = "KEY_AUTO_LEVELING_ENABLED"
        const val KEY_AUTO_LEVELING_TARGET_LUFS = "KEY_AUTO_LEVELING_TARGET_LUFS"
        const val KEY_FADE_IN_SEC = "KEY_FADE_IN_SEC"
        const val KEY_FADE_OUT_SEC = "KEY_FADE_OUT_SEC"

        // WorkManager Data has no nullable Long slot, so absence is encoded as NO_PROJECT_ID.
        const val KEY_PROJECT_ID = "KEY_PROJECT_ID"
        const val NO_PROJECT_ID = -1L

        const val KEY_PROGRESS = "KEY_PROGRESS"
        const val KEY_STATUS = "KEY_STATUS"
        const val KEY_GALLERY_URI = "KEY_GALLERY_URI"
        const val KEY_ERROR = "KEY_ERROR"
    }

    override suspend fun doWork(): Result {
        val context = applicationContext
        val title = inputData.getString(KEY_PROJECT_TITLE) ?: "EditedProject_${System.currentTimeMillis()}"
        val jobType = inputData.getString(KEY_JOB_TYPE) ?: "EDITOR"
        val format = inputData.getString(KEY_EXPORT_FORMAT) ?: "mp4"
        val folder = inputData.getString(KEY_DESTINATION_FOLDER) ?: "Movies"
        val inputUriStr = inputData.getString(KEY_INPUT_URI) ?: ""

        val audioUriStr = inputData.getString(KEY_AUDIO_URI)
        val titleTextStr = inputData.getString(KEY_TITLE_TEXT) ?: ""
        val watermarkTextStr = inputData.getString(KEY_WATERMARK_TEXT) ?: ""
        val spectrumStyleStr = inputData.getString(KEY_SPECTRUM_STYLE) ?: "NONE"
        val presetQualityStr = inputData.getString(KEY_PRESET_QUALITY) ?: "1080p"
        val targetDurationSecVal = inputData.getDouble(KEY_TARGET_DURATION_SEC, 15.0)
        val loopStyleStr = inputData.getString(KEY_LOOP_STYLE) ?: "STANDARD"
        val targetLufsVal = inputData.getDouble(KEY_TARGET_LUFS, -14.0)
        val presetNameStr = inputData.getString(KEY_PRESET_NAME) ?: "PUNCHY"
        val ffmpegFilterStr = inputData.getString(KEY_FFMPEG_FILTER)
        val overlayUriStr = inputData.getString(KEY_OVERLAY_URI)
        val overlayPositionStr = inputData.getString(KEY_OVERLAY_POSITION)
        val resolutionStr = inputData.getString(KEY_RESOLUTION) ?: "1080p"
        val frameRateStr = inputData.getString(KEY_FRAME_RATE) ?: "30fps"
        val bitrateStr = inputData.getString(KEY_BITRATE) ?: "Medium"
        val aspectRatioStr = inputData.getString(KEY_ASPECT_RATIO) ?: "Asli"

        val projectId = inputData.getLong(KEY_PROJECT_ID, NO_PROJECT_ID).takeIf { it > 0L }

        val database = AppDatabase.getDatabase(context)
        val repository = LoopingVidRepository(
            database.renderJobDao(),
            database.liveSessionDao(),
            database.appSettingDao(),
            projectDao = database.projectDao()
        )
        val mediaProcessor = MediaProcessor(context, repository)
        val projectLifecycleManager = ProjectLifecycleManager(repository)

        projectLifecycleManager.markRendering(projectId)

        val progressCollectorJob = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Default).launch {
            mediaProcessor.progressState.collect { progressState ->
                this@VideoExportWorker.setProgress(
                    workDataOf(
                        KEY_PROGRESS to progressState.progress,
                        KEY_STATUS to (progressState.statusText ?: "Processing..."),
                        KEY_PROJECT_TITLE to title,
                        KEY_JOB_TYPE to jobType,
                        KEY_EXPORT_FORMAT to format,
                        KEY_DESTINATION_FOLDER to folder
                    )
                )
            }
        }

        try {
            val resultJobEntity = when (jobType.uppercase()) {
                "LOOP" -> {
                    mediaProcessor.executeLoopJob(
                        inputUri = inputUriStr,
                        targetDurationSec = targetDurationSecVal,
                        loopStyle = loopStyleStr,
                        presetQuality = presetQualityStr,
                        customFileName = title,
                        destinationFolder = folder,
                        exportFormat = format,
                        resolution = resolutionStr,
                        frameRate = frameRateStr,
                        bitrate = bitrateStr,
                        aspectRatio = aspectRatioStr,
                        projectId = projectId
                    )
                }
                "MASTERING" -> {
                    mediaProcessor.executeMasteringJob(
                        inputUri = inputUriStr,
                        presetName = presetNameStr,
                        targetLufs = targetLufsVal,
                        exportFormat = format,
                        customFileName = title,
                        destinationFolder = folder,
                        isNoiseReductionEnabled = inputData.getBoolean(KEY_NOISE_REDUCTION_ENABLED, false),
                        noiseReductionDb = inputData.getFloat(KEY_NOISE_REDUCTION_DB, 12f),
                        noiseFloorDb = inputData.getFloat(KEY_NOISE_FLOOR_DB, -45f),
                        isAutoLevelingEnabled = inputData.getBoolean(KEY_AUTO_LEVELING_ENABLED, false),
                        autoLevelingTargetLufs = inputData.getFloat(KEY_AUTO_LEVELING_TARGET_LUFS, -14.0f),
                        fadeInSec = inputData.getFloat(KEY_FADE_IN_SEC, 0f),
                        fadeOutSec = inputData.getFloat(KEY_FADE_OUT_SEC, 0f),
                        projectId = projectId
                    )
                }
                else -> { // "EDITOR" or others
                    mediaProcessor.executeEditorJob(
                        mediaUri = inputUriStr,
                        audioUri = audioUriStr,
                        titleText = titleTextStr,
                        watermarkText = watermarkTextStr,
                        spectrumStyle = spectrumStyleStr,
                        presetQuality = presetQualityStr,
                        filterString = ffmpegFilterStr,
                        customFileName = title,
                        destinationFolder = folder,
                        exportFormat = format,
                        overlayUri = overlayUriStr,
                        overlayPosition = overlayPositionStr,
                        resolution = resolutionStr,
                        frameRate = frameRateStr,
                        bitrate = bitrateStr,
                        aspectRatio = aspectRatioStr,
                        projectId = projectId
                    )
                }
            }

            progressCollectorJob.cancel()
            projectLifecycleManager.markCompleted(projectId)
            val galleryUri = Uri.parse(resultJobEntity.outputUri ?: "")

            return Result.success(
                workDataOf(
                    KEY_PROGRESS to 100,
                    KEY_STATUS to "Export complete",
                    KEY_GALLERY_URI to galleryUri.toString(),
                    KEY_PROJECT_TITLE to title,
                    KEY_JOB_TYPE to jobType,
                    KEY_EXPORT_FORMAT to format,
                    KEY_DESTINATION_FOLDER to folder
                )
            )
        } catch (e: Exception) {
            progressCollectorJob.cancel()
            projectLifecycleManager.markFailed(projectId)
            this@VideoExportWorker.setProgress(workDataOf(KEY_PROGRESS to 0, KEY_STATUS to "Export failed: ${e.localizedMessage}"))
            return Result.failure(workDataOf(KEY_ERROR to (e.localizedMessage ?: "Unknown export error")))
        }
    }
}
