package com.example.feature.slideshow

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.ffmpeg.JobProgressState
import com.example.core.ffmpeg.SlideshowProcessor
import com.example.core.ffmpeg.SlideshowRenderRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

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
    val jobProgress: JobProgressState = JobProgressState(),
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
    private val slideshowProcessor: SlideshowProcessor
) : ViewModel() {

    private val _uiState = MutableStateFlow(SlideshowUiState())
    val uiState: StateFlow<SlideshowUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            slideshowProcessor.progressState.collect { progress ->
                _uiState.value = _uiState.value.copy(jobProgress = progress)
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

    fun renderSlideshow() {
        val state = _uiState.value
        if (state.images.isEmpty()) {
            _uiState.value = state.copy(validationMessage = "Pilih minimal satu gambar terlebih dahulu.")
            return
        }
        if (state.jobProgress.isProcessing) return

        viewModelScope.launch(Dispatchers.IO) {
            try {
                slideshowProcessor.renderSlideshow(
                    SlideshowRenderRequest(
                        imageUris = state.images.map { it.uri },
                        audioUri = state.audioUri,
                        perImageDurationSec = state.perImageDurationSec,
                        transition = state.transition,
                        transitionDurationSec = state.transitionDurationSec,
                        resolution = state.resolution,
                        aspectRatio = state.aspectRatio,
                        outputName = state.outputName
                    )
                )
            } catch (error: Exception) {
                // Failure is surfaced through slideshowProcessor.progressState.
            }
        }
    }

    fun cancelRender() {
        slideshowProcessor.cancel()
    }

    fun dismissValidationMessage() {
        _uiState.value = _uiState.value.copy(validationMessage = null)
    }

    companion object {
        val TRANSITIONS = listOf("fade", "wipeleft", "slideright", "circleopen", "none")
        val ASPECT_RATIOS = listOf("16:9", "9:16", "1:1", "4:5")
        val RESOLUTIONS = listOf("480p", "720p", "1080p", "4K")
    }
}
