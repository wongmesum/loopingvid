package com.example.core.work

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.ServiceInfo
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.example.core.database.AppDatabase
import com.example.core.database.LoopingVidRepository
import com.example.core.database.RenderJobEntity
import com.example.core.ffmpeg.MediaProcessor
import com.example.core.utils.MediaStoreExporter
import kotlinx.coroutines.launch
import java.io.File

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
        const val KEY_PLAYBACK_SPEED = "KEY_PLAYBACK_SPEED"
        const val KEY_TRIM_START_SEC = "KEY_TRIM_START_SEC"
        const val KEY_TRIM_END_SEC = "KEY_TRIM_END_SEC"

        const val KEY_PROGRESS = "KEY_PROGRESS"
        const val KEY_STATUS = "KEY_STATUS"
        const val KEY_GALLERY_URI = "KEY_GALLERY_URI"
        const val KEY_ERROR = "KEY_ERROR"

        private const val EXPORT_CHANNEL_ID = "video_export_channel"
        private const val EXPORT_NOTIFICATION_ID = 2001
    }

    /**
     * Provides the foreground notification so long FFmpeg exports run as expedited/foreground
     * work and are not killed by the OS while the app is backgrounded.
     */
    override suspend fun getForegroundInfo(): ForegroundInfo {
        return createForegroundInfo("Preparing export...")
    }

    private fun createForegroundInfo(status: String): ForegroundInfo {
        val context = applicationContext
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val channel = NotificationChannel(
                EXPORT_CHANNEL_ID,
                "Video Export",
                NotificationManager.IMPORTANCE_LOW
            ).apply { description = "Background video/audio export progress" }
            manager.createNotificationChannel(channel)
        }

        val notification: Notification = NotificationCompat.Builder(context, EXPORT_CHANNEL_ID)
            .setContentTitle("LoopingVid Export")
            .setContentText(status)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setOngoing(true)
            .build()

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(EXPORT_NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            ForegroundInfo(EXPORT_NOTIFICATION_ID, notification)
        }
    }

    override suspend fun doWork(): Result {
        val context = applicationContext

        // Promote to a foreground service so long exports survive app backgrounding.
        try {
            setForeground(createForegroundInfo("Exporting video..."))
        } catch (_: Throwable) {
            // If the OS declines foreground promotion (e.g. background start limits), continue anyway.
        }

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
        val playbackSpeedVal = inputData.getFloat(KEY_PLAYBACK_SPEED, 1.0f)
        val trimStartVal = inputData.getDouble(KEY_TRIM_START_SEC, 0.0)
        val trimEndVal = inputData.getDouble(KEY_TRIM_END_SEC, 0.0)

        val repository = LoopingVidRepository(
            AppDatabase.getDatabase(context).renderJobDao(),
            AppDatabase.getDatabase(context).liveSessionDao(),
            AppDatabase.getDatabase(context).appSettingDao()
        )
        val mediaProcessor = MediaProcessor(context, repository)

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
                        aspectRatio = aspectRatioStr
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
                        fadeOutSec = inputData.getFloat(KEY_FADE_OUT_SEC, 0f)
                    )
                }
                // Editor-family jobs. BATCH_COLOR_GRADING / TRANSITION_RENDER / TEMPLATE_EXPORT /
                // SPEED_RETIME all encode an edited video where the specific operation is expressed
                // through the FFmpeg filter string (built by their respective control cards), so they
                // are correctly handled by the editor processor. They are listed explicitly so the
                // routing is intentional rather than an implicit fall-through.
                "EDITOR", "BATCH_COLOR_GRADING", "TRANSITION_RENDER", "TEMPLATE_EXPORT", "SPEED_RETIME" -> {
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
                        playbackSpeed = playbackSpeedVal,
                        trimStartSec = trimStartVal,
                        trimEndSec = trimEndVal
                    )
                }
                else -> {
                    // Unknown job type: default to the editor pipeline so nothing silently drops.
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
                        playbackSpeed = playbackSpeedVal,
                        trimStartSec = trimStartVal,
                        trimEndSec = trimEndVal
                    )
                }
            }

            progressCollectorJob.cancel()
            val galleryUri = Uri.parse(resultJobEntity.outputUri ?: "")

            return Result.success(
                workDataOf(
                    KEY_PROGRESS to 100,
                    KEY_STATUS to "Export complete",
                    KEY_GALLERY_URI to galleryUri.toString(),
                    KEY_PROJECT_TITLE to title
                )
            )
        } catch (e: Exception) {
            progressCollectorJob.cancel()
            this@VideoExportWorker.setProgress(workDataOf(KEY_PROGRESS to 0, KEY_STATUS to "Export failed: ${e.localizedMessage}"))
            return Result.failure(workDataOf(KEY_ERROR to (e.localizedMessage ?: "Unknown export error")))
        }
    }
}
