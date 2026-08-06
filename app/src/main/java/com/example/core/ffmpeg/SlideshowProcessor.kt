package com.example.core.ffmpeg

import android.content.Context
import com.example.core.database.LoopingVidRepository
import com.example.core.database.RenderJobEntity
import com.example.core.utils.MediaStoreExporter
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Renders image sequences without increasing the responsibilities of MediaProcessor. */
class SlideshowProcessor(
    private val context: Context,
    private val repository: LoopingVidRepository,
    private val ffmpegWrapper: FFmpegWrapper = FFmpegWrapperImpl(context)
) {
    private val _progressState = MutableStateFlow(JobProgressState())
    val progressState: StateFlow<JobProgressState> = _progressState.asStateFlow()

    suspend fun renderSlideshow(request: SlideshowRenderRequest): RenderJobEntity =
        withContext(Dispatchers.IO) {
            require(request.imageUris.isNotEmpty()) { "Pilih minimal satu gambar" }
            val outputFile = createOutputFile(request.outputName)
            val initialJob = createInitialJob(request, outputFile)
            val jobId = repository.saveJob(initialJob)

            _progressState.value = JobProgressState(
                jobId = jobId,
                isProcessing = true,
                statusText = "Menyiapkan ${request.imageUris.size} gambar..."
            )

            try {
                executeRender(request, outputFile, initialJob, jobId)
                completeRender(request, outputFile, initialJob, jobId)
            } catch (error: CancellationException) {
                repository.updateJob(initialJob.copy(id = jobId, status = "CANCELLED"))
                throw error
            } catch (error: Exception) {
                failRender(initialJob, jobId, error)
                throw error
            }
        }

    fun cancel() {
        ffmpegWrapper.cancel()
        _progressState.value = JobProgressState(
            isProcessing = false,
            errorMessage = "Render slideshow dibatalkan"
        )
    }

    private suspend fun executeRender(
        request: SlideshowRenderRequest,
        outputFile: File,
        initialJob: RenderJobEntity,
        jobId: Long
    ) {
        val command = FFmpegCommandBuilder.buildSlideshowCommand(
            imagePaths = request.imageUris,
            outputPath = outputFile.absolutePath,
            perImageDurationSec = request.perImageDurationSec,
            transition = request.transition,
            transitionDurationSec = request.transitionDurationSec,
            audioPath = request.audioUri,
            resolution = request.resolution,
            aspectRatio = request.aspectRatio,
            frameRate = request.frameRate
        )
        val exitCode = ffmpegWrapper.execute(command) { progress ->
            updateProgress(initialJob, jobId, progress)
        }
        check(exitCode == 0 && outputFile.exists()) {
            "FFmpeg gagal merender slideshow (kode $exitCode)"
        }
    }

    private suspend fun updateProgress(initialJob: RenderJobEntity, jobId: Long, progress: Int) {
        val status = when {
            progress < 25 -> "Membaca gambar ($progress%)"
            progress < 70 -> "Menerapkan transisi ($progress%)"
            else -> "Mengenkode video ($progress%)"
        }
        _progressState.value = JobProgressState(jobId, true, progress, status)
        if (progress % 10 == 0) {
            repository.updateJob(initialJob.copy(id = jobId, progress = progress))
        }
    }

    private suspend fun completeRender(
        request: SlideshowRenderRequest,
        outputFile: File,
        initialJob: RenderJobEntity,
        jobId: Long
    ): RenderJobEntity {
        MediaStoreExporter.exportVideoToGallery(
            context = context,
            videoFile = outputFile,
            title = outputFile.nameWithoutExtension,
            mimeType = "video/mp4",
            relativeFolder = "Movies/LoopingVid/Slideshow"
        )
        val completed = initialJob.copy(
            id = jobId,
            status = "COMPLETED",
            progress = 100,
            fileSizeMb = outputFile.length().toDouble() / (1024 * 1024)
        )
        repository.updateJob(completed)
        _progressState.value = JobProgressState(
            jobId = jobId,
            progress = 100,
            statusText = "Slideshow tersimpan di galeri",
            outputFilePath = outputFile.absolutePath
        )
        return completed
    }

    private suspend fun failRender(initialJob: RenderJobEntity, jobId: Long, error: Exception) {
        repository.updateJob(initialJob.copy(id = jobId, status = "FAILED"))
        _progressState.value = JobProgressState(
            jobId = jobId,
            errorMessage = error.localizedMessage ?: "Render slideshow gagal"
        )
    }

    private fun createOutputFile(customName: String): File {
        val outputDir = File(context.getExternalFilesDir(null) ?: context.filesDir, "SlideshowOutput")
        outputDir.mkdirs()
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val safeName = customName.trim().takeIf { it.isNotEmpty() } ?: "Slideshow_$timestamp"
        return File(outputDir, "$safeName.mp4")
    }

    private fun createInitialJob(request: SlideshowRenderRequest, outputFile: File): RenderJobEntity {
        val duration = request.imageUris.size * request.perImageDurationSec -
            (request.imageUris.size - 1).coerceAtLeast(0) * request.transitionDurationSec
        return RenderJobEntity(
            jobType = "SLIDESHOW",
            title = outputFile.nameWithoutExtension,
            inputUri = request.imageUris.joinToString("|"),
            outputUri = outputFile.absolutePath,
            style = request.transition.uppercase(),
            status = "PROCESSING",
            progress = 0,
            durationSec = duration.coerceAtLeast(request.perImageDurationSec),
            fileSizeMb = 0.0,
            paramsSummary = "${request.imageUris.size} gambar, ${request.aspectRatio}, ${request.resolution}"
        )
    }
}

data class SlideshowRenderRequest(
    val imageUris: List<String>,
    val audioUri: String? = null,
    val perImageDurationSec: Double = 3.0,
    val transition: String = "fade",
    val transitionDurationSec: Double = 1.0,
    val resolution: String = "1080p",
    val aspectRatio: String = "16:9",
    val frameRate: String = "30fps",
    val outputName: String = ""
)
