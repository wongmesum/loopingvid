package com.example.core.work

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.asFlow
import androidx.lifecycle.viewModelScope
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.example.core.ui.ExportJobConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.UUID

data class QueueItemUiState(
    val id: UUID,
    val title: String,
    val jobType: String,
    val format: String,
    val destinationFolder: String,
    val status: QueueStatus,
    val progress: Int,
    val statusText: String,
    val galleryUri: String? = null,
    val error: String? = null,
    val rawData: Data = Data.EMPTY
)

enum class QueueStatus {
    QUEUED, RUNNING, SUCCEEDED, FAILED, CANCELLED
}

data class ExportQueueUiState(
    val items: List<QueueItemUiState> = emptyList(),
    val totalQueued: Int = 0,
    val totalRunning: Int = 0,
    val totalCompleted: Int = 0,
    val totalFailed: Int = 0,
    val isSequentialProcessingActive: Boolean = false
)

data class BatchExportRequest(
    val title: String,
    val jobType: String, // "EDITOR", "LOOP", "MASTERING", "BATCH_COLOR_GRADING"
    val format: String = "mp4",
    val destinationFolder: String = "Movies",
    val inputUri: String = "",
    val audioUri: String? = null,
    val titleText: String? = null,
    val watermarkText: String? = null,
    val spectrumStyle: String? = null,
    val presetQuality: String = "1080p",
    val resolution: String = "1080p",
    val frameRate: String = "30fps",
    val bitrate: String = "Medium",
    val aspectRatio: String = "Asli",
    val targetDurationSec: Double = 15.0,
    val loopStyle: String = "STANDARD",
    val targetLufs: Double = -14.0,
    val presetName: String = "PUNCHY",
    val ffmpegFilterString: String? = null,
    val overlayUri: String? = null,
    val overlayPosition: String? = null,
    val audioMetadata: com.example.core.media.AudioMetadata? = null,
    val isNoiseReductionEnabled: Boolean = false,
    val noiseReductionDb: Float = 12f,
    val noiseFloorDb: Float = -45f,
    val isAutoLevelingEnabled: Boolean = false,
    val autoLevelingTargetLufs: Float = -14.0f,
    val fadeInSec: Float = 0f,
    val fadeOutSec: Float = 0f
)

class ExportQueueViewModel(
    private val application: Application
) : AndroidViewModel(application) {

    private val workManager = WorkManager.getInstance(application)

    private val _uiState = MutableStateFlow(ExportQueueUiState())
    val uiState: StateFlow<ExportQueueUiState> = _uiState.asStateFlow()

    companion object {
        const val UNIQUE_QUEUE_NAME = "sequential_video_export_queue"
        const val QUEUE_TAG = "video_export_queue"
    }

    init {
        observeQueueWork()
    }

    private fun observeQueueWork() {
        viewModelScope.launch {
            workManager.getWorkInfosByTagLiveData(QUEUE_TAG)
                .asFlow()
                .collectLatest { workInfos ->
                    val queueItems = workInfos.map { info ->
                        val progressData = info.progress
                        val outputData = info.outputData

                        val title = progressData.getString(VideoExportWorker.KEY_PROJECT_TITLE)
                            ?: outputData.getString(VideoExportWorker.KEY_PROJECT_TITLE)
                            ?: "Video Project ${info.id.toString().take(6)}"
                        val jobType = progressData.getString(VideoExportWorker.KEY_JOB_TYPE)
                            ?: outputData.getString(VideoExportWorker.KEY_JOB_TYPE)
                            ?: "EDITOR"
                        val format = progressData.getString(VideoExportWorker.KEY_EXPORT_FORMAT)
                            ?: outputData.getString(VideoExportWorker.KEY_EXPORT_FORMAT)
                            ?: "mp4"
                        val folder = progressData.getString(VideoExportWorker.KEY_DESTINATION_FOLDER)
                            ?: outputData.getString(VideoExportWorker.KEY_DESTINATION_FOLDER)
                            ?: "Movies"

                        val status = when (info.state) {
                            WorkInfo.State.ENQUEUED -> QueueStatus.QUEUED
                            WorkInfo.State.RUNNING -> QueueStatus.RUNNING
                            WorkInfo.State.SUCCEEDED -> QueueStatus.SUCCEEDED
                            WorkInfo.State.FAILED -> QueueStatus.FAILED
                            WorkInfo.State.CANCELLED, WorkInfo.State.BLOCKED -> QueueStatus.CANCELLED
                        }

                        val progress = when (info.state) {
                            WorkInfo.State.SUCCEEDED -> 100
                            WorkInfo.State.RUNNING -> progressData.getInt(VideoExportWorker.KEY_PROGRESS, 15)
                            else -> 0
                        }

                        val statusText = when (info.state) {
                            WorkInfo.State.ENQUEUED -> "Queued for export..."
                            WorkInfo.State.RUNNING -> progressData.getString(VideoExportWorker.KEY_STATUS) ?: "Rendering & exporting..."
                            WorkInfo.State.SUCCEEDED -> "Exported to Gallery!"
                            WorkInfo.State.FAILED -> outputData.getString(VideoExportWorker.KEY_ERROR) ?: "Export failed"
                            WorkInfo.State.CANCELLED -> "Export cancelled"
                            WorkInfo.State.BLOCKED -> "Waiting in queue..."
                        }

                        val galleryUri = outputData.getString(VideoExportWorker.KEY_GALLERY_URI)
                        val error = outputData.getString(VideoExportWorker.KEY_ERROR)

                        QueueItemUiState(
                            id = info.id,
                            title = title,
                            jobType = jobType,
                            format = format,
                            destinationFolder = folder,
                            status = status,
                            progress = progress,
                            statusText = statusText,
                            galleryUri = galleryUri,
                            error = error,
                            rawData = progressData
                        )
                    }

                    _uiState.value = ExportQueueUiState(
                        items = queueItems,
                        totalQueued = queueItems.count { it.status == QueueStatus.QUEUED },
                        totalRunning = queueItems.count { it.status == QueueStatus.RUNNING },
                        totalCompleted = queueItems.count { it.status == QueueStatus.SUCCEEDED },
                        totalFailed = queueItems.count { it.status == QueueStatus.FAILED },
                        isSequentialProcessingActive = queueItems.any { it.status == QueueStatus.RUNNING || it.status == QueueStatus.QUEUED }
                    )
                }
        }
    }

    /**
     * Enqueues a single edited video project export into the sequential WorkManager queue.
     */
    fun enqueueProjectExport(request: BatchExportRequest) {
        val inputData = buildDataFromRequest(request)

        val workRequest = OneTimeWorkRequestBuilder<VideoExportWorker>()
            .setInputData(inputData)
            .addTag(QUEUE_TAG)
            .build()

        workManager.beginUniqueWork(
            UNIQUE_QUEUE_NAME,
            ExistingWorkPolicy.APPEND_OR_REPLACE,
            workRequest
        ).enqueue()
    }

    /**
     * Enqueues a list of edited video projects sequentially for batch export to the device gallery.
     */
    fun enqueueBatchProjects(requests: List<BatchExportRequest>) {
        if (requests.isEmpty()) return

        var continuation = workManager.beginUniqueWork(
            UNIQUE_QUEUE_NAME,
            ExistingWorkPolicy.APPEND_OR_REPLACE,
            OneTimeWorkRequestBuilder<VideoExportWorker>()
                .setInputData(buildDataFromRequest(requests.first()))
                .addTag(QUEUE_TAG)
                .build()
        )

        for (i in 1 until requests.size) {
            val workRequest = OneTimeWorkRequestBuilder<VideoExportWorker>()
                .setInputData(buildDataFromRequest(requests[i]))
                .addTag(QUEUE_TAG)
                .build()
            continuation = continuation.then(workRequest)
        }

        continuation.enqueue()
    }

    private fun buildDataFromRequest(request: BatchExportRequest): Data {
        return workDataOf(
            VideoExportWorker.KEY_PROJECT_TITLE to request.title,
            VideoExportWorker.KEY_JOB_TYPE to request.jobType,
            VideoExportWorker.KEY_EXPORT_FORMAT to request.format,
            VideoExportWorker.KEY_DESTINATION_FOLDER to request.destinationFolder,
            VideoExportWorker.KEY_INPUT_URI to request.inputUri,
            VideoExportWorker.KEY_AUDIO_URI to request.audioUri,
            VideoExportWorker.KEY_TITLE_TEXT to request.titleText,
            VideoExportWorker.KEY_WATERMARK_TEXT to request.watermarkText,
            VideoExportWorker.KEY_SPECTRUM_STYLE to request.spectrumStyle,
            VideoExportWorker.KEY_PRESET_QUALITY to request.presetQuality,
            VideoExportWorker.KEY_TARGET_DURATION_SEC to request.targetDurationSec,
            VideoExportWorker.KEY_LOOP_STYLE to request.loopStyle,
            VideoExportWorker.KEY_TARGET_LUFS to request.targetLufs,
            VideoExportWorker.KEY_PRESET_NAME to request.presetName,
            VideoExportWorker.KEY_FFMPEG_FILTER to request.ffmpegFilterString,
            VideoExportWorker.KEY_OVERLAY_URI to request.overlayUri,
            VideoExportWorker.KEY_OVERLAY_POSITION to request.overlayPosition,
            VideoExportWorker.KEY_RESOLUTION to request.resolution,
            VideoExportWorker.KEY_FRAME_RATE to request.frameRate,
            VideoExportWorker.KEY_BITRATE to request.bitrate,
            VideoExportWorker.KEY_ASPECT_RATIO to request.aspectRatio,
            VideoExportWorker.KEY_NOISE_REDUCTION_ENABLED to request.isNoiseReductionEnabled,
            VideoExportWorker.KEY_NOISE_REDUCTION_DB to request.noiseReductionDb,
            VideoExportWorker.KEY_NOISE_FLOOR_DB to request.noiseFloorDb,
            VideoExportWorker.KEY_AUTO_LEVELING_ENABLED to request.isAutoLevelingEnabled,
            VideoExportWorker.KEY_AUTO_LEVELING_TARGET_LUFS to request.autoLevelingTargetLufs,
            VideoExportWorker.KEY_FADE_IN_SEC to request.fadeInSec,
            VideoExportWorker.KEY_FADE_OUT_SEC to request.fadeOutSec
        )
    }

    /**
     * Cancels a specific work item by ID.
     */
    fun cancelExportJob(workId: UUID) {
        workManager.cancelWorkById(workId)
    }

    /**
     * Cancels all enqueued or running export jobs in WorkManager.
     */
    fun cancelAllExports() {
        workManager.cancelAllWorkByTag(QUEUE_TAG)
    }

    /**
     * Retries a failed export job by re-enqueueing it.
     */
    fun retryFailedExport(item: QueueItemUiState) {
        val workRequest = OneTimeWorkRequestBuilder<VideoExportWorker>()
            .setInputData(item.rawData)
            .addTag(QUEUE_TAG)
            .build()

        workManager.beginUniqueWork(
            UNIQUE_QUEUE_NAME,
            ExistingWorkPolicy.APPEND_OR_REPLACE,
            workRequest
        ).enqueue()
    }

    /**
     * Prunes finished work requests from WorkManager.
     */
    fun clearCompletedJobs() {
        workManager.pruneWork()
    }
}
