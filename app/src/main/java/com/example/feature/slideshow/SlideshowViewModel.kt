package com.example.feature.slideshow

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.ffmpeg.JobProgressState
import com.example.core.ffmpeg.SlideshowProcessor
import com.example.core.ffmpeg.SlideshowRenderRequest
import com.example.core.work.BatchExportRequest
import com.example.core.work.ExportQueueViewModel
import com.example.core.work.QueueStatus
import com.example.core.work.RenderRequestSerializer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

data class SlideshowImage(
    val uri: String,
    val displayName: String
)

data class SlideshowUiState(
    val images: List<SlideshowImage> = emptyList(),
    val audioUri: String? = null,
    val audioName: String = "",
    val perImageDurationSec: Double = 3.0,
    val transition: String = "fade",
    val transitionDurationSec: Double = 1.0,
    val aspectRatio: String = "16:9",
    val resolution: String = "1080p",
    val outputName: String = "",
    val kenBurnsEnabled: Boolean = false,
    val overlayText: String = "",
    val jobProgress: JobProgressState = JobProgressState(),
    /** Work id of the render this screen enqueued, used to track and cancel it. */
    val exportJobId: UUID? = null,
    val validationMessage: String? = null
) {
    /** Real playback length after transition overlap is accounted for. */
    val estimatedDurationSec: Double
        get() {
            if (images.isEmpty()) return 0.0
            val overlap = if (transition == "none") 0.0 else transitionDurationSec
            val total = images.size * perImageDurationSec - (images.size - 1) * overlap
            return total.coerceAtLeast(perImageDurationSec)
        }

    val canRender: Boolean
        get() = images.isNotEmpty() && !jobProgress.isProcessing
}

class SlideshowViewModel(
    private val slideshowProcessor: SlideshowProcessor,
    private val exportQueueViewModel: ExportQueueViewModel? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(SlideshowUiState())
    val uiState: StateFlow<SlideshowUiState> = _uiState.asStateFlow()

    init {
        // Fallback for tests and callers that do not inject the unified queue.
        if (exportQueueViewModel == null) {
            viewModelScope.launch {
                slideshowProcessor.progressState.collect { progress ->
                    _uiState.value = _uiState.value.copy(jobProgress = progress)
                }
            }
        }

        exportQueueViewModel?.let { queue ->
            viewModelScope.launch {
                queue.uiState.collect { queueState ->
                    val currentJobId = _uiState.value.exportJobId ?: return@collect
                    val item = queueState.items.find { it.id == currentJobId } ?: return@collect
                    val isProcessing = item.status == QueueStatus.RUNNING || item.status == QueueStatus.QUEUED
                    _uiState.value = _uiState.value.copy(
                        jobProgress = JobProgressState(
                            jobId = currentJobId.mostSignificantBits,
                            isProcessing = isProcessing,
                            progress = item.progress,
                            statusText = item.statusText,
                            outputFilePath = item.galleryUri ?: "",
                            errorMessage = item.error
                        )
                    )
                }
            }
        }
    }

    fun addImages(images: List<SlideshowImage>) {
        if (images.isEmpty()) return
        val existing = _uiState.value.images
        val merged = existing + images.filterNot { candidate -> existing.any { it.uri == candidate.uri } }
        _uiState.value = _uiState.value.copy(images = merged, validationMessage = null)
    }

    fun removeImage(uri: String) {
        _uiState.value = _uiState.value.copy(images = _uiState.value.images.filterNot { it.uri == uri })
    }

    fun moveImage(fromIndex: Int, toIndex: Int) {
        val images = _uiState.value.images
        if (fromIndex !in images.indices || toIndex !in images.indices) return
        val reordered = images.toMutableList()
        reordered.add(toIndex, reordered.removeAt(fromIndex))
        _uiState.value = _uiState.value.copy(images = reordered)
    }

    fun setAudio(uri: String?, name: String) {
        _uiState.value = _uiState.value.copy(audioUri = uri, audioName = name)
    }

    fun setPerImageDuration(seconds: Double) {
        val safeDuration = seconds.coerceIn(1.0, 15.0)
        _uiState.value = _uiState.value.copy(
            perImageDurationSec = safeDuration,
            // A transition can never outlast the image it fades from.
            transitionDurationSec = _uiState.value.transitionDurationSec.coerceAtMost(safeDuration - 0.2)
        )
    }

    fun setTransition(transition: String) {
        _uiState.value = _uiState.value.copy(transition = transition)
    }

    fun setTransitionDuration(seconds: Double) {
        val ceiling = (_uiState.value.perImageDurationSec - 0.2).coerceAtLeast(0.1)
        _uiState.value = _uiState.value.copy(transitionDurationSec = seconds.coerceIn(0.1, ceiling))
    }

    fun setAspectRatio(aspectRatio: String) {
        _uiState.value = _uiState.value.copy(aspectRatio = aspectRatio)
    }

    fun setResolution(resolution: String) {
        _uiState.value = _uiState.value.copy(resolution = resolution)
    }

    fun setOutputName(name: String) {
        _uiState.value = _uiState.value.copy(outputName = name)
    }

    fun setKenBurns(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(kenBurnsEnabled = enabled)
    }

    fun setOverlayText(text: String) {
        _uiState.value = _uiState.value.copy(overlayText = text.take(MAX_OVERLAY_TEXT_LENGTH))
    }

    fun renderSlideshow() {
        val state = _uiState.value
        if (state.images.isEmpty()) {
            _uiState.value = state.copy(validationMessage = "Please select at least one image first.")
            return
        }
        if (state.jobProgress.isProcessing) return

        val request = SlideshowRenderRequest(
            imageUris = state.images.map { it.uri },
            audioUri = state.audioUri,
            perImageDurationSec = state.perImageDurationSec,
            transition = state.transition,
            transitionDurationSec = state.transitionDurationSec,
            resolution = state.resolution,
            aspectRatio = state.aspectRatio,
            outputName = state.outputName,
            kenBurnsEnabled = state.kenBurnsEnabled,
            overlayText = state.overlayText
        )

        val queue = exportQueueViewModel
        if (queue != null) {
            val jobId = queue.enqueueProjectExport(
                BatchExportRequest(
                    title = state.outputName.ifBlank { "Slideshow" },
                    jobType = "SLIDESHOW",
                    slideshowConfigJson = RenderRequestSerializer.serializeSlideshow(request)
                )
            )
            _uiState.value = _uiState.value.copy(
                exportJobId = jobId,
                jobProgress = JobProgressState(
                    jobId = jobId.mostSignificantBits,
                    isProcessing = true,
                    statusText = "Menunggu dalam antrean..."
                )
            )
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                slideshowProcessor.renderSlideshow(request)
            } catch (error: Exception) {
                // Failure is surfaced through slideshowProcessor.progressState.
            }
        }
    }

    fun cancelRender() {
        val jobId = _uiState.value.exportJobId
        if (exportQueueViewModel != null && jobId != null) {
            exportQueueViewModel.cancelExportJob(jobId)
            return
        }
        slideshowProcessor.cancel()
    }

    fun dismissValidationMessage() {
        _uiState.value = _uiState.value.copy(validationMessage = null)
    }

    companion object {
        private const val MAX_OVERLAY_TEXT_LENGTH = 100

        val TRANSITIONS = listOf(
            "fade", "wipeleft", "slideright", "circleopen",
            "wiperight", "wipeup", "wipedown", "slideleft",
            "slideup", "slidedown", "dissolve",
            "none"
        )
        val ASPECT_RATIOS = listOf("16:9", "9:16", "1:1", "4:5")
        val RESOLUTIONS = listOf("480p", "720p", "1080p", "4K")
    }
}
