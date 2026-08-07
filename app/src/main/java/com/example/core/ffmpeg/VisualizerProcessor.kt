package com.example.core.ffmpeg

import android.content.Context
import com.example.core.database.LoopingVidRepository
import com.example.core.database.RenderJobEntity
import com.example.core.utils.FfmpegInputResolver
import com.example.core.utils.MediaStoreExporter
import com.example.core.utils.ResolvedInput
import com.example.feature.visualizer.VisualizerBackground
import com.example.feature.visualizer.VisualizerExportCommandBuilder
import com.example.feature.visualizer.VisualizerRenderConfig
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

/**
 * Renders an audio track into a visualizer video.
 *
 * Kept separate from [MediaProcessor] for the same reason [SlideshowProcessor]
 * is: the visualizer owns its own config tree and output folder, and folding it
 * into MediaProcessor would grow an already large class.
 */
class VisualizerProcessor(
    private val context: Context,
    private val repository: LoopingVidRepository,
    private val ffmpegWrapper: FFmpegWrapper = FFmpegWrapperImpl(context)
) {
    private val _progressState = MutableStateFlow(JobProgressState())
    val progressState: StateFlow<JobProgressState> = _progressState.asStateFlow()

    suspend fun renderVisualizer(request: VisualizerRenderRequest): RenderJobEntity =
        withContext(Dispatchers.IO) {
            require(request.audioUri.isNotBlank()) { "Please select an audio source first" }

            val outputFile = createOutputFile(request.outputName)
            val initialJob = createInitialJob(request, outputFile)
            val jobId = repository.saveJob(initialJob)

            _progressState.value = JobProgressState(
                jobId = jobId,
                isProcessing = true,
                statusText = "Preparing audio source..."
            )

            var resolvedAudio: ResolvedInput? = null
            var resolvedImage: ResolvedInput? = null

            try {
                resolvedAudio = FfmpegInputResolver.resolve(context, request.audioUri)
                resolvedImage = (request.config.background as? VisualizerBackground.Image)?.let { bg ->
                    FfmpegInputResolver.resolve(context, bg.uri)
                }

                val requestWithResolvedFiles = request.copy(
                    audioUri = resolvedAudio.path,
                    config = resolvedImage?.let { img ->
                        request.config.copy(background = VisualizerBackground.Image(img.path, (request.config.background as VisualizerBackground.Image).blurRadius))
                    } ?: request.config
                )

                executeRender(requestWithResolvedFiles, outputFile, initialJob, jobId)
                completeRender(outputFile, initialJob, jobId)
            } catch (error: CancellationException) {
                repository.updateJob(initialJob.copy(id = jobId, status = "CANCELLED"))
                throw error
            } catch (error: Exception) {
                failRender(initialJob, jobId, error)
                throw error
            } finally {
                resolvedAudio?.temporaryFile?.delete()
                resolvedImage?.temporaryFile?.delete()
            }
        }

    fun cancel() {
        ffmpegWrapper.cancel()
        _progressState.value = JobProgressState(
            isProcessing = false,
            errorMessage = "Visualizer render cancelled"
        )
    }

    private suspend fun executeRender(
        request: VisualizerRenderRequest,
        outputFile: File,
        initialJob: RenderJobEntity,
        jobId: Long
    ) {
        val command = VisualizerExportCommandBuilder.build(
            audioPath = request.audioUri,
            outputPath = outputFile.absolutePath,
            config = request.config,
            beatMarkersMs = request.beatMarkersMs,
            beatEffectExpression = request.beatEffectExpression
        )
        val exitCode = ffmpegWrapper.execute(command) { progress ->
            updateProgress(initialJob, jobId, progress)
        }
        check(exitCode == 0 && outputFile.exists()) {
            "FFmpeg failed to render visualizer (code $exitCode)"
        }
    }

    private suspend fun updateProgress(initialJob: RenderJobEntity, jobId: Long, progress: Int) {
        val status = when {
            progress < 20 -> "Analyzing audio ($progress%)"
            progress < 80 -> "Rendering visuals ($progress%)"
            else -> "Encoding video ($progress%)"
        }
        _progressState.value = JobProgressState(jobId, true, progress, status)
        if (progress % 10 == 0) {
            repository.updateJob(initialJob.copy(id = jobId, progress = progress))
        }
    }

    private suspend fun completeRender(
        outputFile: File,
        initialJob: RenderJobEntity,
        jobId: Long
    ): RenderJobEntity {
        MediaStoreExporter.exportVideoToGallery(
            context = context,
            videoFile = outputFile,
            title = outputFile.nameWithoutExtension,
            mimeType = "video/mp4",
            relativeFolder = "Movies/LoopingVid/Visualizer"
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
            statusText = "Visualizer saved to gallery",
            outputFilePath = outputFile.absolutePath
        )
        return completed
    }

    private suspend fun failRender(initialJob: RenderJobEntity, jobId: Long, error: Exception) {
        repository.updateJob(initialJob.copy(id = jobId, status = "FAILED"))
        _progressState.value = JobProgressState(
            jobId = jobId,
            errorMessage = error.localizedMessage ?: "Visualizer render failed"
        )
    }

    private fun createOutputFile(customName: String): File {
        val outputDir = File(context.getExternalFilesDir(null) ?: context.filesDir, "VisualizerOutput")
        outputDir.mkdirs()
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val safeName = customName.trim().takeIf { it.isNotEmpty() } ?: "Visualizer_$timestamp"
        return File(outputDir, "$safeName.mp4")
    }

    private fun createInitialJob(request: VisualizerRenderRequest, outputFile: File): RenderJobEntity =
        RenderJobEntity(
            jobType = "VISUALIZER",
            title = outputFile.nameWithoutExtension,
            inputUri = request.audioUri,
            outputUri = outputFile.absolutePath,
            style = request.config.mode.name,
            status = "PROCESSING",
            progress = 0,
            durationSec = request.durationMs / 1000.0,
            fileSizeMb = 0.0,
            paramsSummary = "${request.config.mode.name}, ${request.config.aspectRatio}, " +
                "${request.config.bandCount} bands, ${request.beatMarkersMs.size} beats"
        )
}

data class VisualizerRenderRequest(
    val audioUri: String,
    val config: VisualizerRenderConfig,
    val beatMarkersMs: List<Long> = emptyList(),
    val beatEffectExpression: String? = null,
    val durationMs: Long = 0L,
    val outputName: String = ""
)
