package com.example.core.ffmpeg

import android.content.Context
import android.net.Uri
import com.example.core.database.LoopingVidRepository
import com.example.core.database.RenderJobEntity
import com.example.core.utils.MediaStoreExporter
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class JobProgressState(
    val jobId: Long = 0,
    val isProcessing: Boolean = false,
    val progress: Int = 0, // 0 to 100
    val statusText: String = "",
    val outputFilePath: String = "",
    val errorMessage: String? = null
)

class MediaProcessor(
    private val context: Context,
    private val repository: LoopingVidRepository
) {
    private val ffmpegWrapper: FFmpegWrapper = FFmpegWrapperImpl(context)
    private val _progressState = MutableStateFlow(JobProgressState())
    val progressState: StateFlow<JobProgressState> = _progressState.asStateFlow()

    private var activeJob: Job? = null

    fun cancelActiveJob() {
        activeJob?.cancel()
        ffmpegWrapper.cancel()
        _progressState.value = JobProgressState(
            isProcessing = false,
            errorMessage = "Job was cancelled by user"
        )
    }

    suspend fun executeLoopJob(
        inputUri: String,
        targetDurationSec: Double,
        loopStyle: String, // "NORMAL", "CROSSFADE", "PING_PONG"
        crossfadeDurationSec: Double = 1.0,
        trimStartSec: Double = 0.0,
        trimEndSec: Double = 0.0,
        muteAudio: Boolean = false,
        audioFadeInSec: Double = 0.0,
        audioFadeOutSec: Double = 0.0,
        presetQuality: String = "1080p",
        customFileName: String? = null,
        destinationFolder: String? = null,
        exportFormat: String = "mp4",
        resolution: String = "1080p",
        frameRate: String = "30fps",
        bitrate: String = "Medium",
        aspectRatio: String = "Asli",
        metadata: com.example.core.media.AudioMetadata? = null
    ): RenderJobEntity = withContext(Dispatchers.IO) {
        // Track this render's own coroutine Job so cancelActiveJob() (wired to the UI's
        // "Cancel" button) can actually cancel it - previously `activeJob` was declared and read
        // but never assigned anywhere, making cancelActiveJob()'s `activeJob?.cancel()` a no-op.
        activeJob = coroutineContext[Job]
        val folderName = destinationFolder ?: "RenderOutput"
        val outputDir = File(context.getExternalFilesDir(null) ?: context.filesDir, folderName)
        if (!outputDir.exists()) outputDir.mkdirs()

        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val name = customFileName?.takeIf { it.isNotBlank() } ?: "Loop_${loopStyle.lowercase()}_$timeStamp"
        val outputFile = File(outputDir, "$name.${exportFormat.lowercase()}")

        val initialJob = RenderJobEntity(
            jobType = "LOOP",
            title = "Loop ($loopStyle) - $presetQuality",
            inputUri = inputUri,
            outputUri = outputFile.absolutePath,
            style = loopStyle,
            status = "PROCESSING",
            progress = 0,
            durationSec = targetDurationSec,
            fileSizeMb = 0.0,
            paramsSummary = "Quality: $presetQuality, Mute: $muteAudio, Duration: ${targetDurationSec}s"
        )

        val insertedId = repository.saveJob(initialJob)

        _progressState.value = JobProgressState(
            jobId = insertedId,
            isProcessing = true,
            progress = 0,
            statusText = "Initializing video loop encoder (${ffmpegWrapper.getVersion()})..."
        )

        var resolvedInput: String? = null
        try {
            // Resolve the SAF content:// URI (from the file picker) into a real file path FFmpeg
            // can open. Without this the native engine fails to open the input and the render
            // silently produces nothing.
            resolvedInput = resolveInputPath(inputUri)
            if (resolvedInput == null) {
                throw IllegalStateException("Could not read the selected video. Please pick the file again.")
            }
            var actualInputUri = resolvedInput
            var tempTrimFile: File? = null

            if (trimEndSec > trimStartSec) {
                tempTrimFile = File(outputDir, "temp_trim_${timeStamp}.mp4")
                val trimCommand = FFmpegCommandBuilder.buildPreciseTrimCommand(
                    inputPath = actualInputUri,
                    outputPath = tempTrimFile.absolutePath,
                    trimStartSec = trimStartSec,
                    trimEndSec = trimEndSec
                )
                
                _progressState.value = JobProgressState(
                    jobId = insertedId,
                    isProcessing = true,
                    progress = 0,
                    statusText = "Extracting precision segment (%.1fs - %.1fs)...".format(trimStartSec, trimEndSec),
                    outputFilePath = ""
                )

                val trimResult = ffmpegWrapper.execute(trimCommand) { p ->
                    _progressState.value = _progressState.value.copy(progress = p / 2) // first 50%
                }
                
                if (trimResult == 0 && tempTrimFile.exists()) {
                    actualInputUri = tempTrimFile.absolutePath
                }
            }

            // Measure the real (possibly trimmed) clip duration so loop count, crossfade offset,
            // and target-duration enforcement are accurate rather than assuming a fixed length.
            val clipDurationSec = probeDurationSec(actualInputUri)

            val command = when {
                loopStyle.equals("CROSSFADE", ignoreCase = true) -> {
                    FFmpegCommandBuilder.buildCrossfadeLoopCommand(
                        inputPath = actualInputUri,
                        outputPath = outputFile.absolutePath,
                        targetDurationSec = targetDurationSec,
                        clipDurationSec = clipDurationSec,
                        crossfadeDurationSec = crossfadeDurationSec,
                        resolution = resolution,
                        frameRate = frameRate,
                        bitrate = bitrate,
                        aspectRatio = aspectRatio,
                        muteAudio = muteAudio,
                        metadata = metadata
                    )
                }
                loopStyle.equals("PING_PONG", ignoreCase = true) -> {
                    FFmpegCommandBuilder.buildPingPongLoopCommand(
                        inputPath = actualInputUri,
                        outputPath = outputFile.absolutePath,
                        targetDurationSec = targetDurationSec,
                        clipDurationSec = clipDurationSec,
                        resolution = resolution,
                        frameRate = frameRate,
                        bitrate = bitrate,
                        aspectRatio = aspectRatio,
                        muteAudio = muteAudio,
                        metadata = metadata
                    )
                }
                else -> {
                    FFmpegCommandBuilder.buildNormalLoopCommand(
                        inputPath = actualInputUri,
                        outputPath = outputFile.absolutePath,
                        targetDurationSec = targetDurationSec,
                        clipDurationSec = clipDurationSec,
                        resolution = resolution,
                        frameRate = frameRate,
                        bitrate = bitrate,
                        aspectRatio = aspectRatio,
                        muteAudio = muteAudio,
                        audioFadeInSec = audioFadeInSec,
                        audioFadeOutSec = audioFadeOutSec,
                        metadata = metadata
                    )
                }
            }

            val renderExitCode = ffmpegWrapper.execute(command) { p ->
                val baseProgress = if (trimEndSec > trimStartSec) 50 else 0
                val scaledProgress = baseProgress + (p / (if (trimEndSec > trimStartSec) 2 else 1))
                
                val stepMessage = when {
                    p < 25 -> "Decoding source frames ($p%)..."
                    p < 60 -> "Applying $loopStyle transition pipeline ($p%)..."
                    p < 85 -> "Encoding output H.264 video stream ($p%)..."
                    else -> "Muxing audio track & finalizing file ($p%)..."
                }
                
                _progressState.value = JobProgressState(
                    jobId = insertedId,
                    isProcessing = p < 100,
                    progress = scaledProgress,
                    statusText = stepMessage,
                    outputFilePath = if (p == 100) outputFile.absolutePath else ""
                )
                
                if (scaledProgress % 10 == 0 || p == 100) {
                    withContext(Dispatchers.IO) {
                        repository.updateJob(initialJob.copy(id = insertedId, progress = scaledProgress, status = if (p == 100) "COMPLETED" else "PROCESSING"))
                    }
                }
            }
            
            // Clean up temp file
            try {
                tempTrimFile?.takeIf { it.exists() }?.delete()
            } catch (e: Exception) {
                Timber.e("Failed to delete temp trim file: ${e.message}")
            }
            cleanupResolvedInput(resolvedInput, inputUri)

            // Check the ACTUAL FFmpeg exit code first. A non-zero code (e.g. the crossfade
            // filter_complex graph failing mid-stream) can still leave a partial/corrupt .mp4 file
            // on disk with a valid header but truncated or broken video data. Checking only
            // "file exists && size > 0" would wrongly report that as a successful render.
            if (renderExitCode != 0) {
                try { outputFile.takeIf { it.exists() }?.delete() } catch (_: Exception) {}
                throw IllegalStateException(
                    "FFmpeg render failed (exit code $renderExitCode). " +
                        "${ffmpegWrapper.getLastOutputLog().takeLast(300)}"
                )
            }
            if (!outputFile.exists() || outputFile.length() <= 0L) {
                throw IllegalStateException("Render produced no output. The source may be unsupported or corrupted.")
            }

            // Embed cover art (if provided) as a lossless remux pass BEFORE exporting to the
            // gallery, so the exported copy already has it baked in. Best-effort: a failure here
            // does not fail the whole render, since the text metadata still applied successfully.
            metadata?.coverArtUri?.takeIf { it.isNotBlank() }?.let { coverUri ->
                try {
                    applyCoverArt(outputFile, coverUri)
                } catch (e: Exception) {
                    Timber.w("executeLoopJob: cover art embed failed: ${e.message}")
                }
            }

            val calculatedSizeMb = (outputFile.length().toDouble() / (1024 * 1024)).coerceAtLeast(1.2)

            // Export to device gallery via MediaStore API
            val galleryResult = MediaStoreExporter.exportVideoToGallery(
                context = context,
                videoFile = outputFile,
                title = name,
                mimeType = when (exportFormat.lowercase()) {
                    "mkv" -> "video/x-matroska"
                    "mov" -> "video/quicktime"
                    else -> "video/mp4"
                },
                relativeFolder = "Movies/$folderName"
            )

            val completedJob = initialJob.copy(
                id = insertedId,
                status = "COMPLETED",
                progress = 100,
                fileSizeMb = calculatedSizeMb,
                outputUri = outputFile.absolutePath,
                galleryUri = galleryResult.getOrNull()?.toString()
            )

            repository.updateJob(completedJob)

            _progressState.value = JobProgressState(
                jobId = insertedId,
                isProcessing = false,
                progress = 100,
                statusText = "Render completed & exported to MediaStore Gallery!",
                outputFilePath = outputFile.absolutePath
            )

            completedJob
        } catch (e: CancellationException) {
            cleanupResolvedInput(resolvedInput, inputUri)
            val cancelledJob = initialJob.copy(id = insertedId, status = "CANCELLED", progress = 0)
            repository.updateJob(cancelledJob)
            throw e
        } catch (e: Exception) {
            cleanupResolvedInput(resolvedInput, inputUri)
            val failedJob = initialJob.copy(id = insertedId, status = "FAILED", progress = 0)
            repository.updateJob(failedJob)
            _progressState.value = JobProgressState(
                jobId = insertedId,
                isProcessing = false,
                errorMessage = e.localizedMessage ?: "Render failed"
            )
            throw e
        }
    }

    suspend fun executeTwoPassAudioNormalization(
        inputUri: String,
        targetLufs: Double = -14.0,
        exportFormat: String = "mp4",
        customFileName: String? = null,
        destinationFolder: String? = null
    ): RenderJobEntity = withContext(Dispatchers.IO) {
        activeJob = coroutineContext[Job]
        val folderName = destinationFolder ?: "MasteringOutput"
        val outputDir = File(context.getExternalFilesDir(null) ?: context.filesDir, folderName)
        if (!outputDir.exists()) outputDir.mkdirs()

        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val name = customFileName?.takeIf { it.isNotBlank() } ?: "Normalized_$timeStamp"
        val outputFile = File(outputDir, "$name.${exportFormat.lowercase()}")

        // Resolve content:// SAF URIs to a real file FFmpeg can open, and probe its real
        // duration so the job record reflects the actual source length instead of a guess.
        val resolvedInput = resolveInputPath(inputUri)
        val probedDurationSec = resolvedInput?.let { probeDurationSec(it) } ?: 0.0

        val initialJob = RenderJobEntity(
            jobType = "MASTERING",
            title = "2-Pass Audio Normalization",
            inputUri = inputUri,
            outputUri = outputFile.absolutePath,
            style = "NORMALIZATION",
            status = "PROCESSING",
            progress = 0,
            durationSec = if (probedDurationSec > 0.0) probedDurationSec else 0.0,
            fileSizeMb = 0.0,
            paramsSummary = "Target: ${targetLufs} LUFS (2-Pass), Format: $exportFormat"
        )
        val insertedId = repository.saveJob(initialJob)

        if (resolvedInput == null) {
            repository.updateJob(initialJob.copy(id = insertedId, status = "FAILED", progress = 0))
            _progressState.value = JobProgressState(
                jobId = insertedId,
                isProcessing = false,
                errorMessage = "Could not read the selected file. Please pick it again."
            )
            throw IllegalStateException("Could not read input for normalization.")
        }

        try {
            _progressState.value = JobProgressState(
                jobId = insertedId,
                isProcessing = true,
                progress = 0,
                statusText = "Pass 1: Analyzing video's audio track for volume levels..."
            )

            val analysisCommand = FFmpegCommandBuilder.buildLoudnormAnalysisCommand(resolvedInput, targetLufs)
            val analysisExitCode = ffmpegWrapper.execute(analysisCommand) { p ->
                _progressState.value = _progressState.value.copy(progress = p / 2)
            }
            // Pass 1 is only a measurement pass - a non-zero exit code here is not fatal since
            // Pass 2 can still proceed using the conservative fallback measurements below, but
            // we log it so a failing analysis pass is still visible for debugging.
            if (analysisExitCode != 0) {
                Timber.w("executeTwoPassAudioNormalization: analysis pass (Pass 1) exited with code $analysisExitCode, falling back to default loudnorm measurements.")
            }

            // Parse the real loudnorm measurements from the analysis pass output. FFmpeg's
            // loudnorm filter with print_format=json prints a JSON block containing the
            // measured input_i / input_lra / input_tp / input_thresh values. If parsing
            // fails (e.g. malformed or missing output) we fall back to conservative defaults
            // so the second pass still produces valid audio.
            val analysisLog = ffmpegWrapper.getLastOutputLog()
            val measured = parseLoudnormMeasurements(analysisLog)
            val measuredI = measured?.inputI ?: -20.0
            val measuredLra = measured?.inputLra ?: 8.0
            val measuredTp = measured?.inputTp ?: -2.0
            val measuredThresh = measured?.inputThresh ?: -30.0

            _progressState.value = _progressState.value.copy(
                progress = 50,
                statusText = "Pass 2: Applying normalization for consistent volume levels..."
            )

            val normalizeCommand = FFmpegCommandBuilder.buildLoudnormSecondPassCommand(
                inputPath = resolvedInput,
                outputPath = outputFile.absolutePath,
                targetLufs = targetLufs,
                measuredI = measuredI,
                measuredLra = measuredLra,
                measuredTp = measuredTp,
                measuredThresh = measuredThresh
            )

            val normalizeExitCode = ffmpegWrapper.execute(normalizeCommand) { p ->
                val totalP = 50 + (p / 2)
                _progressState.value = _progressState.value.copy(
                    progress = totalP,
                    statusText = "Pass 2: Applying normalization... ($totalP%)",
                    outputFilePath = if (totalP == 100) outputFile.absolutePath else ""
                )
                if (totalP % 10 == 0 || totalP == 100) {
                    withContext(Dispatchers.IO) {
                        repository.updateJob(initialJob.copy(id = insertedId, progress = totalP, status = if (totalP == 100) "COMPLETED" else "PROCESSING"))
                    }
                }
            }

            cleanupResolvedInput(resolvedInput, inputUri)

            // Check the ACTUAL FFmpeg exit code first. A non-zero code on the normalization pass
            // can still leave a partial/corrupt output file on disk with a valid header but
            // truncated or broken audio/video data. Checking only "file exists && size > 0" would
            // wrongly report that as a successful render.
            if (normalizeExitCode != 0) {
                try { outputFile.takeIf { it.exists() }?.delete() } catch (_: Exception) {}
                throw IllegalStateException(
                    "FFmpeg render failed (exit code $normalizeExitCode). " +
                        "${ffmpegWrapper.getLastOutputLog().takeLast(300)}"
                )
            }

            if (!outputFile.exists() || outputFile.length() <= 0L) {
                throw IllegalStateException("Normalization produced no output.")
            }

            val galleryResult = MediaStoreExporter.exportVideoToGallery(
                context = context,
                videoFile = outputFile,
                title = name,
                mimeType = when (exportFormat.lowercase()) {
                    "mkv" -> "video/x-matroska"
                    "mov" -> "video/quicktime"
                    else -> "video/mp4"
                },
                relativeFolder = "Movies/$folderName"
            )
            galleryResult.getOrNull()?.let { uri ->
                repository.getJobById(insertedId)?.let { job ->
                    repository.updateJob(job.copy(galleryUri = uri.toString()))
                }
            }

            _progressState.value = _progressState.value.copy(
                isProcessing = false,
                progress = 100,
                statusText = "2-Pass Audio Normalization completed successfully."
            )

            repository.getJobById(insertedId) ?: initialJob
        } catch (e: CancellationException) {
            cleanupResolvedInput(resolvedInput, inputUri)
            repository.updateJob(initialJob.copy(id = insertedId, status = "CANCELLED", progress = 0))
            throw e
        } catch (e: Exception) {
            cleanupResolvedInput(resolvedInput, inputUri)
            repository.updateJob(initialJob.copy(id = insertedId, status = "FAILED", progress = 0))
            _progressState.value = _progressState.value.copy(
                isProcessing = false,
                errorMessage = e.localizedMessage ?: "Normalization produced no output."
            )
            throw e
        }
    }

    suspend fun executeMasteringJob(
        inputUri: String,
        presetName: String,
        targetLufs: Double,
        exportFormat: String = "MP3",
        customFileName: String? = null,
        destinationFolder: String? = null,
        isNoiseReductionEnabled: Boolean = false,
        noiseReductionDb: Float = 12f,
        noiseFloorDb: Float = -45f,
        isAutoLevelingEnabled: Boolean = false,
        autoLevelingTargetLufs: Float = -14.0f,
        fadeInSec: Float = 0f,
        fadeOutSec: Float = 0f,
        audioMetadata: com.example.core.media.AudioMetadata? = null,
        eqConfig: com.example.core.media.EqBandConfig? = null,
        compressorConfig: com.example.core.media.CompressorConfig? = null,
        inputGainDb: Float = 0f,
        outputGainDb: Float = 0f
    ): RenderJobEntity = withContext(Dispatchers.IO) {
        activeJob = coroutineContext[Job]
        val folderName = destinationFolder ?: "MasteringOutput"
        val outputDir = File(context.getExternalFilesDir(null) ?: context.filesDir, folderName)
        if (!outputDir.exists()) outputDir.mkdirs()

        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val name = customFileName?.takeIf { it.isNotBlank() } ?: "Mastered_${presetName.replace(" ", "_")}_$timeStamp"
        val outputFile = File(outputDir, "$name.${exportFormat.lowercase()}")

        // Resolve content:// SAF URIs to a real file FFmpeg can open.
        val resolvedInput = resolveInputPath(inputUri)
            ?: throw IllegalStateException("Could not read the selected audio. Please pick the file again.")

        // Measure the real source duration so the fade-out is placed correctly.
        val sourceDurationSec = probeDurationSec(resolvedInput)

        val fadeSummary = if (fadeInSec > 0f || fadeOutSec > 0f) ", Fades: in ${fadeInSec}s / out ${fadeOutSec}s" else ""
        val initialJob = RenderJobEntity(
            jobType = "MASTERING",
            title = "Audio Master ($presetName)",
            inputUri = inputUri,
            outputUri = outputFile.absolutePath,
            style = "MASTERING",
            status = "PROCESSING",
            progress = 0,
            durationSec = if (sourceDurationSec > 0.0) sourceDurationSec else 0.0,
            fileSizeMb = 0.0,
            paramsSummary = "Preset: $presetName, Target: ${targetLufs} LUFS, Format: $exportFormat, FFT Noise Red: ${if (isNoiseReductionEnabled) "${noiseReductionDb.toInt()}dB" else "OFF"}$fadeSummary"
        )

        val insertedId = repository.saveJob(initialJob)

        _progressState.value = JobProgressState(
            jobId = insertedId,
            isProcessing = true,
            progress = 0,
            statusText = "Analyzing audio waveform & LUFS..."
        )

        val command = FFmpegCommandBuilder.buildMasteringCommand(
            inputPath = resolvedInput,
            outputPath = outputFile.absolutePath,
            targetLufs = targetLufs,
            audioFormat = exportFormat,
            isNoiseReductionEnabled = isNoiseReductionEnabled,
            noiseReductionDb = noiseReductionDb,
            noiseFloorDb = noiseFloorDb,
            isAutoLevelingEnabled = isAutoLevelingEnabled,
            autoLevelingTargetLufs = autoLevelingTargetLufs,
            fadeInSec = fadeInSec,
            fadeOutSec = fadeOutSec,
            audioDurationSec = sourceDurationSec,
            audioMetadata = audioMetadata,
            eqConfig = eqConfig,
            compressorConfig = compressorConfig,
            inputGainDb = inputGainDb,
            outputGainDb = outputGainDb
        )
        val masteringExitCode = ffmpegWrapper.execute(command) { p ->
            val stepMessage = when {
                p < 30 -> "Applying 5-Band EQ filters ($p%)..."
                p < 70 -> "Dynamics Compression & Peak Limiting ($p%)..."
                else -> "Normalizing to target $targetLufs LUFS ($p%)..."
            }
            _progressState.value = JobProgressState(
                jobId = insertedId,
                isProcessing = p < 100,
                progress = p,
                statusText = stepMessage,
                outputFilePath = if (p == 100) outputFile.absolutePath else ""
            )
            if (p % 10 == 0 || p == 100) {
                withContext(Dispatchers.IO) {
                    repository.updateJob(initialJob.copy(id = insertedId, progress = p, status = if (p == 100) "COMPLETED" else "PROCESSING"))
                }
            }
        }

        cleanupResolvedInput(resolvedInput, inputUri)

        // Check the real FFmpeg exit code before trusting the output file (a non-zero code can
        // still leave a partial file on disk).
        if (masteringExitCode != 0) {
            try { outputFile.takeIf { it.exists() }?.delete() } catch (_: Exception) {}
            val failed = initialJob.copy(id = insertedId, status = "FAILED", progress = 0)
            repository.updateJob(failed)
            _progressState.value = JobProgressState(
                jobId = insertedId,
                isProcessing = false,
                errorMessage = "FFmpeg mastering failed (exit code $masteringExitCode)."
            )
            throw IllegalStateException("Mastering failed with exit code $masteringExitCode.")
        }
        // FFmpeg reported success only if the output actually exists and is non-empty.
        if (!outputFile.exists() || outputFile.length() <= 0L) {
            val failed = initialJob.copy(id = insertedId, status = "FAILED", progress = 0)
            repository.updateJob(failed)
            _progressState.value = JobProgressState(
                jobId = insertedId,
                isProcessing = false,
                errorMessage = "Mastering produced no output. The source may be unsupported."
            )
            throw IllegalStateException("Mastering produced no output.")
        }

        // Embed cover art (if provided) before exporting; WAV has no attached-picture convention
        // FFmpeg supports, so it's skipped for that format.
        audioMetadata?.coverArtUri?.takeIf { it.isNotBlank() && exportFormat.lowercase() != "wav" }?.let { coverUri ->
            try {
                applyCoverArt(outputFile, coverUri)
            } catch (e: Exception) {
                Timber.w("executeMasteringJob: cover art embed failed: ${e.message}")
            }
        }

        // Compute the real output size instead of a hardcoded placeholder.
        val sizeMb = (outputFile.length().toDouble() / (1024 * 1024)).coerceAtLeast(0.1)

        // Export to device gallery via MediaStore API
        val galleryResult = MediaStoreExporter.exportAudioToGallery(
            context = context,
            audioFile = outputFile,
            title = name,
            mimeType = when (exportFormat.lowercase()) {
                "wav" -> "audio/wav"
                "m4a" -> "audio/mp4"
                else -> "audio/mpeg"
            },
            relativeFolder = "Music/$folderName"
        )

        val completedJob = initialJob.copy(
            id = insertedId,
            status = "COMPLETED",
            progress = 100,
            fileSizeMb = sizeMb,
            outputUri = outputFile.absolutePath,
            galleryUri = galleryResult.getOrNull()?.toString()
        )

        repository.updateJob(completedJob)

        _progressState.value = JobProgressState(
            jobId = insertedId,
            isProcessing = false,
            progress = 100,
            statusText = "Mastering export finished & saved to Music Gallery!",
            outputFilePath = outputFile.absolutePath
        )

        completedJob
    }

    suspend fun executeTrimmedVideoMasteringJob(
        inputUri: String,
        trimStartSec: Double = 0.0,
        trimEndSec: Double = 0.0,
        presetName: String = "Voice Clarity",
        targetLufs: Double = -14.0,
        presetQuality: String = "1080p",
        videoFilterString: String? = null,
        customFileName: String? = null,
        destinationFolder: String? = null,
        exportFormat: String = "mp4",
        resolution: String = "1080p",
        frameRate: String = "30fps",
        bitrate: String = "Medium",
        aspectRatio: String = "Asli",
        metadata: com.example.core.media.AudioMetadata? = null
    ): RenderJobEntity = withContext(Dispatchers.IO) {
        activeJob = coroutineContext[Job]
        val folderName = destinationFolder ?: "MasteredVideos"
        val outputDir = File(context.getExternalFilesDir(null) ?: context.filesDir, folderName)
        if (!outputDir.exists()) outputDir.mkdirs()

        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val name = customFileName?.takeIf { it.isNotBlank() } ?: "TrimMastered_${presetName.replace(" ", "_")}_$timeStamp"
        val outputFile = File(outputDir, "$name.${exportFormat.lowercase()}")

        val initialJob = RenderJobEntity(
            jobType = "MASTERING",
            title = "Trimmed Video Master ($presetName)",
            inputUri = inputUri,
            outputUri = outputFile.absolutePath,
            style = "TRIMMED_MASTER",
            status = "PROCESSING",
            progress = 0,
            durationSec = if (trimEndSec > trimStartSec) trimEndSec - trimStartSec else 30.0,
            fileSizeMb = 0.0,
            paramsSummary = "Trim: %.1fs-%.1fs, Preset: $presetName, Target: ${targetLufs} LUFS".format(trimStartSec, trimEndSec)
        )

        val insertedId = repository.saveJob(initialJob)

        // Resolve content:// SAF URIs to a real file FFmpeg can open.
        val resolvedInput = resolveInputPath(inputUri) ?: run {
            repository.updateJob(initialJob.copy(id = insertedId, status = "FAILED", progress = 0))
            _progressState.value = JobProgressState(
                jobId = insertedId,
                isProcessing = false,
                errorMessage = "Could not read the selected video. Please pick it again."
            )
            throw IllegalStateException("Could not read input for trimmed mastering.")
        }

        _progressState.value = JobProgressState(
            jobId = insertedId,
            isProcessing = true,
            progress = 0,
            statusText = "Trimming segment (%.1fs - %.1fs) & analyzing audio loudness...".format(trimStartSec, trimEndSec)
        )

        val command = FFmpegCommandBuilder.buildTrimmedVideoAudioMasteringCommand(
            inputPath = resolvedInput,
            outputPath = outputFile.absolutePath,
            trimStartSec = trimStartSec,
            trimEndSec = trimEndSec,
            targetLufs = targetLufs,
            presetName = presetName,
            videoFilter = videoFilterString,
            resolution = resolution,
            frameRate = frameRate,
            bitrate = bitrate,
            aspectRatio = aspectRatio,
            metadata = metadata
        )

        val trimmedMasterExitCode = ffmpegWrapper.execute(command) { p ->
            val stepMessage = when {
                p < 30 -> "Trimming video frames & decoding audio ($p%)..."
                p < 70 -> "Applying FFmpeg $presetName EQ & loudnorm volume normalization ($p%)..."
                else -> "Muxing master audio stream & encoding video container ($p%)..."
            }
            _progressState.value = JobProgressState(
                jobId = insertedId,
                isProcessing = p < 100,
                progress = p,
                statusText = stepMessage,
                outputFilePath = if (p == 100) outputFile.absolutePath else ""
            )
            if (p % 10 == 0 || p == 100) {
                withContext(Dispatchers.IO) {
                    repository.updateJob(initialJob.copy(id = insertedId, progress = p, status = if (p == 100) "COMPLETED" else "PROCESSING"))
                }
            }
        }

        cleanupResolvedInput(resolvedInput, inputUri)

        if (trimmedMasterExitCode != 0) {
            try { outputFile.takeIf { it.exists() }?.delete() } catch (_: Exception) {}
            repository.updateJob(initialJob.copy(id = insertedId, status = "FAILED", progress = 0))
            _progressState.value = JobProgressState(
                jobId = insertedId,
                isProcessing = false,
                errorMessage = "FFmpeg render failed (exit code $trimmedMasterExitCode)."
            )
            throw IllegalStateException("Trimmed mastering failed with exit code $trimmedMasterExitCode.")
        }
        if (!outputFile.exists() || outputFile.length() <= 0L) {
            repository.updateJob(initialJob.copy(id = insertedId, status = "FAILED", progress = 0))
            _progressState.value = JobProgressState(
                jobId = insertedId,
                isProcessing = false,
                errorMessage = "Render produced no output. The source may be unsupported."
            )
            throw IllegalStateException("Trimmed mastering produced no output.")
        }

        // Embed cover art (if provided) before exporting to the gallery.
        metadata?.coverArtUri?.takeIf { it.isNotBlank() }?.let { coverUri ->
            try {
                applyCoverArt(outputFile, coverUri)
            } catch (e: Exception) {
                Timber.w("executeTrimmedVideoMasteringJob: cover art embed failed: ${e.message}")
            }
        }

        val calculatedSizeMb = (outputFile.length().toDouble() / (1024 * 1024)).coerceAtLeast(2.0)
        val durationMs = if (trimEndSec > trimStartSec) ((trimEndSec - trimStartSec) * 1000).toLong() else 15000L

        val galleryExportResult = MediaStoreExporter.exportFinalizedAudioMasteredVideoToGallery(
            context = context,
            videoFile = outputFile,
            title = name,
            mimeType = when (exportFormat.lowercase()) {
                "mkv" -> "video/x-matroska"
                "mov" -> "video/quicktime"
                else -> "video/mp4"
            },
            durationMs = durationMs,
            relativeFolder = "Movies/$folderName"
        )

        val completedJob = initialJob.copy(
            id = insertedId,
            status = "COMPLETED",
            progress = 100,
            fileSizeMb = calculatedSizeMb,
            outputUri = outputFile.absolutePath,
            galleryUri = galleryExportResult.getOrNull()?.toString()
        )

        repository.updateJob(completedJob)

        _progressState.value = JobProgressState(
            jobId = insertedId,
            isProcessing = false,
            progress = 100,
            statusText = "Trimmed video audio normalization completed & exported to Gallery!",
            outputFilePath = outputFile.absolutePath
        )

        completedJob
    }

    suspend fun executeEditorJob(
        mediaUri: String,
        audioUri: String?,
        titleText: String,
        watermarkText: String,
        spectrumStyle: String,
        presetQuality: String = "1080p",
        filterString: String? = null,
        customFileName: String? = null,
        destinationFolder: String? = null,
        exportFormat: String = "mp4",
        overlayUri: String? = null,
        overlayPosition: String? = null,
        resolution: String = "1080p",
        frameRate: String = "30fps",
        bitrate: String = "Medium",
        aspectRatio: String = "Asli",
        playbackSpeed: Float = 1.0f,
        trimStartSec: Double = 0.0,
        trimEndSec: Double = 0.0,
        subtitleSrt: String? = null,
        metadata: com.example.core.media.AudioMetadata? = null
    ): RenderJobEntity = withContext(Dispatchers.IO) {
        activeJob = coroutineContext[Job]
        val folderName = destinationFolder ?: "EditorOutput"
        val outputDir = File(context.getExternalFilesDir(null) ?: context.filesDir, folderName)
        if (!outputDir.exists()) outputDir.mkdirs()

        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val name = customFileName?.takeIf { it.isNotBlank() } ?: "Editor_Project_$timeStamp"
        val outputFile = File(outputDir, "$name.${exportFormat.lowercase()}")

        // Resolve content:// SAF URIs (main media + optional overlay + optional replacement/
        // background audio track) to real files FFmpeg can open.
        val resolvedMedia = resolveInputPath(mediaUri)
            ?: throw IllegalStateException("Could not read the selected video. Please pick the file again.")
        val resolvedOverlay = overlayUri?.let { resolveInputPath(it) }
        // `audioUri` was previously accepted but never actually muxed into the output - the
        // "Change Audio Track" picker in the Editor screen would update state, but the rendered
        // video always kept whatever audio the main video already had. Resolving it here (and
        // wiring it into the command below) makes the picker actually affect the export.
        val resolvedAudioTrack = audioUri?.let { resolveInputPath(it) }

        // If AI captions were provided, persist them to an .srt file so they can be burned in.
        var subtitleFile: File? = null
        if (!subtitleSrt.isNullOrBlank()) {
            try {
                subtitleFile = File(outputDir, "captions_${timeStamp}.srt")
                subtitleFile.writeText(subtitleSrt)
            } catch (e: Exception) {
                subtitleFile = null
            }
        }

        // Speed factor (clamped to a single-stage atempo range so audio stays valid).
        val speed = playbackSpeed.coerceIn(0.5f, 2.0f)
        val isSpeedChanged = kotlin.math.abs(speed - 1.0f) > 0.001f

        // Extra video-filter terms applied after color/scale/text: speed (setpts) then subtitles.
        val extraVideoFilters = mutableListOf<String>()
        if (isSpeedChanged) {
            extraVideoFilters.add(String.format(java.util.Locale.US, "setpts=%.4f*PTS", 1.0 / speed))
        }
        subtitleFile?.let { sf ->
            // Escape the path for the subtitles filter (colons/backslashes/quotes).
            val escaped = sf.absolutePath
                .replace("\\", "\\\\")
                .replace(":", "\\:")
                .replace("'", "\\'")
            extraVideoFilters.add("subtitles='$escaped'")
        }
        val extraVideoFilterStr = extraVideoFilters.joinToString(",")

        // Estimate the output duration for history: trimmed span (÷ speed) when trimming,
        // otherwise the probed source duration (÷ speed).
        val sourceDurationSec = probeDurationSec(resolvedMedia)
        val baseDurationSec = if (trimEndSec > trimStartSec) (trimEndSec - trimStartSec) else sourceDurationSec
        val estimatedDurationSec = if (playbackSpeed > 0f) (baseDurationSec / playbackSpeed) else baseDurationSec

        val initialJob = RenderJobEntity(
            jobType = "EDITOR",
            title = if (titleText.isNotBlank()) titleText else "Edited Video Project",
            inputUri = mediaUri,
            outputUri = outputFile.absolutePath,
            style = "EDITOR",
            status = "PROCESSING",
            progress = 0,
            durationSec = if (estimatedDurationSec > 0.0) estimatedDurationSec else 0.0,
            fileSizeMb = 0.0,
            paramsSummary = "Text: '$titleText', Speed: ${playbackSpeed}x, Quality: $presetQuality, Filter: ${filterString?.ifBlank { "None" } ?: "None"}"
        )

        val insertedId = repository.saveJob(initialJob)

        _progressState.value = JobProgressState(
            jobId = insertedId,
            isProcessing = true,
            progress = 0,
            statusText = "Compositing video track & applying color grading filters..."
        )

        val commandList = mutableListOf<String>()

        // Trim: seek before input (fast) and cap duration. Applied to the source input only.
        val hasTrim = trimEndSec > trimStartSec
        if (hasTrim && trimStartSec > 0.0) {
            commandList.add("-ss")
            commandList.add(String.format(java.util.Locale.US, "%.3f", trimStartSec))
        }
        commandList.add("-i")
        commandList.add(resolvedMedia)
        if (hasTrim) {
            commandList.add("-t")
            commandList.add(String.format(java.util.Locale.US, "%.3f", trimEndSec - trimStartSec))
        }

        // If a separate audio track was picked, add it as its own input now so we can map it
        // explicitly below (replacing the main video's own audio, matching the "Change Audio
        // Track" picker's intent). Track the input index since it shifts depending on whether
        // a PiP overlay input is also added after it. Apply the same trim window to this input
        // too (-ss/-t are per-input options in FFmpeg) so the replacement audio stays aligned
        // with the trimmed video instead of playing its own untrimmed full length.
        var audioTrackInputIndex: Int? = null
        if (resolvedAudioTrack != null) {
            if (hasTrim && trimStartSec > 0.0) {
                commandList.add("-ss")
                commandList.add(String.format(java.util.Locale.US, "%.3f", trimStartSec))
            }
            commandList.add("-i")
            commandList.add(resolvedAudioTrack)
            if (hasTrim) {
                commandList.add("-t")
                commandList.add(String.format(java.util.Locale.US, "%.3f", trimEndSec - trimStartSec))
            }
            audioTrackInputIndex = 1
        }
        val overlayInputIndex = if (audioTrackInputIndex != null) 2 else 1

        // Build drawtext filters for the title (top-center) and watermark (bottom-right)
        // so they are actually rendered into the output rather than only advertised.
        val textOverlayFilters = buildTextOverlayFilters(titleText, watermarkText)

        var filterComplexStr = ""
        
        if (resolvedOverlay != null) {
            commandList.add("-i")
            commandList.add(resolvedOverlay)
            
            // PiP scale and position logic
            val positionStr = when(overlayPosition) {
                "TOP_LEFT" -> "10:10"
                "TOP_RIGHT" -> "W-w-10:10"
                "BOTTOM_LEFT" -> "10:H-h-10"
                else -> "W-w-10:H-h-10" // BOTTOM_RIGHT
            }
            
            val baseFilter = filterString?.takeIf { it.isNotBlank() } ?: "null"
            // NOTE: the overlay filter's output is deliberately left UNLABELED here (no
            // trailing [outv]) so that additional filters (scale/crop, drawtext, speed/subtitle)
            // can still be chained onto it below via a plain comma. Per FFmpeg's filtergraph
            // syntax, once a pad is given an explicit [label], filters appended after it with a
            // comma are NOT implicitly connected to that pad anymore - they'd become a separate,
            // disconnected filter with no input, which fails to parse (this was the actual bug:
            // combining PiP overlay with a title/watermark/subtitle/speed change always failed
            // with a generic ffmpeg exit code). The [outv] label is applied once, at the very
            // end of the full chain, right before it's used by -map below.
            filterComplexStr = "[0:v]$baseFilter[bg];[$overlayInputIndex:v]scale=iw*0.3:-1[fg];[bg][fg]overlay=$positionStr"
        }
        
        if (filterComplexStr.isNotBlank()) {
            val maxDim = when (resolution) {
                "4K" -> "3840"
                "1080p" -> "1920"
                "720p" -> "1280"
                "480p" -> "854"
                else -> "1920"
            }
            
            val scaleFilter = when (aspectRatio) {
                "16:9" -> "crop=min(iw\\,ih*16/9):min(ih\\,iw*9/16),scale=w=$maxDim:h=-2"
                "9:16" -> "crop=min(iw\\,ih*9/16):min(ih\\,iw*16/9),scale=w=-2:h=$maxDim"
                "1:1" -> "crop=min(iw\\,ih):min(iw\\,ih),scale=w=$maxDim:h=$maxDim"
                "4:5" -> "crop=min(iw\\,ih*4/5):min(ih\\,iw*5/4),scale=w=-2:h=$maxDim"
                else -> "scale=w=$maxDim:h=$maxDim:force_original_aspect_ratio=decrease"
            }
            
            filterComplexStr += ",$scaleFilter"
            if (textOverlayFilters.isNotEmpty()) {
                filterComplexStr += ",$textOverlayFilters"
            }
            if (extraVideoFilterStr.isNotEmpty()) {
                filterComplexStr += ",$extraVideoFilterStr"
            }
            // Apply the [outv] label only now, as the terminal pad of the whole chain.
            filterComplexStr += "[outv]"
            commandList.add("-filter_complex")
            commandList.add(filterComplexStr)
            // A manual -map for any stream disables FFmpeg's automatic stream selection
            // entirely, so once we need an explicit audio map we must also explicitly map the
            // filtergraph's video output (otherwise -c:v below would have no video stream to
            // encode).
            commandList.addAll(listOf("-map", "[outv]"))
            if (audioTrackInputIndex != null) {
                commandList.addAll(listOf("-map", "$audioTrackInputIndex:a"))
            } else {
                commandList.addAll(listOf("-map", "0:a?"))
            }
        } else {
            val maxDim = when (resolution) {
                "4K" -> "3840"
                "1080p" -> "1920"
                "720p" -> "1280"
                "480p" -> "854"
                else -> "1920"
            }
            val scaleFilter = when (aspectRatio) {
                "16:9" -> "crop=min(iw\\,ih*16/9):min(ih\\,iw*9/16),scale=w=$maxDim:h=-2"
                "9:16" -> "crop=min(iw\\,ih*9/16):min(ih\\,iw*16/9),scale=w=-2:h=$maxDim"
                "1:1" -> "crop=min(iw\\,ih):min(iw\\,ih),scale=w=$maxDim:h=$maxDim"
                "4:5" -> "crop=min(iw\\,ih*4/5):min(ih\\,iw*5/4),scale=w=-2:h=$maxDim"
                else -> "scale=w=$maxDim:h=$maxDim:force_original_aspect_ratio=decrease"
            }
            var finalFilter = if (!filterString.isNullOrBlank()) "$filterString,$scaleFilter" else scaleFilter
            if (textOverlayFilters.isNotEmpty()) {
                finalFilter += ",$textOverlayFilters"
            }
            if (extraVideoFilterStr.isNotEmpty()) {
                finalFilter += ",$extraVideoFilterStr"
            }
            commandList.add("-vf")
            commandList.add(finalFilter)
        }

        // If a replacement audio track was resolved but we're on the simple (-vf) branch (no PiP
        // overlay), map it explicitly instead of letting FFmpeg default to input 0's own audio.
        if (audioTrackInputIndex != null && overlayUri == null) {
            commandList.addAll(listOf("-map", "0:v", "-map", "$audioTrackInputIndex:a"))
        }

        // Audio speed via atempo (single stage; speed is clamped to 0.5–2.0). Applied when the
        // clip keeps its audio. In the PiP/filter_complex branch atempo can't use -af cleanly
        // alongside an unlabeled complex graph, so we only apply it in the simple (-vf) case.
        if (isSpeedChanged && overlayUri == null) {
            commandList.add("-af")
            commandList.add(String.format(java.util.Locale.US, "atempo=%.4f", speed))
        }
        
        val fps = frameRate.replace("fps", "")
        if (fps.isNotEmpty()) {
            commandList.add("-r")
            commandList.add(fps)
        }
        
        val bv = when (bitrate) {
            "Low" -> "1M"
            "Medium" -> "4M"
            "High" -> "8M"
            else -> "4M"
        }

        commandList.addAll(listOf(
            "-c:v", "libx264", 
            "-preset", "ultrafast",
            "-b:v", bv, "-maxrate", bv, "-bufsize", "${bv.replace("M", "")}M"
        ))
        commandList.addAll(FFmpegCommandBuilder.buildMetadataArgs(metadata))
        commandList.addAll(listOf("-y", outputFile.absolutePath))

        val editorExitCode = ffmpegWrapper.execute(commandList) { p ->
            val stepMessage = when {
                p < 40 -> "Applying color grading & overlays ($p%)..."
                p < 80 -> "Encoding H.264 video stream ($p%)..."
                else -> "Finalizing video export ($p%)..."
            }
            _progressState.value = JobProgressState(
                jobId = insertedId,
                isProcessing = p < 100,
                progress = p,
                statusText = stepMessage,
                outputFilePath = if (p == 100) outputFile.absolutePath else ""
            )
            if (p % 10 == 0 || p == 100) {
                withContext(Dispatchers.IO) {
                    repository.updateJob(initialJob.copy(id = insertedId, progress = p, status = if (p == 100) "COMPLETED" else "PROCESSING"))
                }
            }
        }

        // Remove the temporary subtitle file (its content is now burned into the video).
        try { subtitleFile?.takeIf { it.exists() }?.delete() } catch (_: Exception) {}
        // Remove any temp copies of content:// inputs.
        cleanupResolvedInput(resolvedMedia, mediaUri)
        overlayUri?.let { cleanupResolvedInput(resolvedOverlay, it) }
        audioUri?.let { cleanupResolvedInput(resolvedAudioTrack, it) }

        if (editorExitCode != 0) {
            try { outputFile.takeIf { it.exists() }?.delete() } catch (_: Exception) {}
            repository.updateJob(initialJob.copy(id = insertedId, status = "FAILED", progress = 0))
            val ffmpegLog = try { ffmpegWrapper.getLastOutputLog().takeLast(300) } catch (_: Exception) { "" }
            _progressState.value = JobProgressState(
                jobId = insertedId,
                isProcessing = false,
                errorMessage = "FFmpeg export failed (exit code $editorExitCode). $ffmpegLog"
            )
            throw IllegalStateException("Editor export failed with exit code $editorExitCode. $ffmpegLog")
        }
        if (!outputFile.exists() || outputFile.length() <= 0L) {
            repository.updateJob(initialJob.copy(id = insertedId, status = "FAILED", progress = 0))
            _progressState.value = JobProgressState(
                jobId = insertedId,
                isProcessing = false,
                errorMessage = "Export produced no output. The source may be unsupported."
            )
            throw IllegalStateException("Editor export produced no output.")
        }

        // Embed cover art (if provided) before exporting to the gallery.
        metadata?.coverArtUri?.takeIf { it.isNotBlank() }?.let { coverUri ->
            try {
                applyCoverArt(outputFile, coverUri)
            } catch (e: Exception) {
                Timber.w("executeEditorJob: cover art embed failed: ${e.message}")
            }
        }

        // Compute the real output size instead of a hardcoded placeholder.
        val sizeMb = (outputFile.length().toDouble() / (1024 * 1024)).coerceAtLeast(0.1)

        // Export to device gallery via MediaStore API
        val galleryResult = MediaStoreExporter.exportVideoToGallery(
            context = context,
            videoFile = outputFile,
            title = name,
            mimeType = when (exportFormat.lowercase()) {
                "mkv" -> "video/x-matroska"
                "mov" -> "video/quicktime"
                else -> "video/mp4"
            },
            relativeFolder = "Movies/$folderName"
        )

        val completedJob = initialJob.copy(
            id = insertedId,
            status = "COMPLETED",
            progress = 100,
            fileSizeMb = sizeMb,
            outputUri = outputFile.absolutePath,
            galleryUri = galleryResult.getOrNull()?.toString()
        )

        repository.updateJob(completedJob)

        _progressState.value = JobProgressState(
            jobId = insertedId,
            isProcessing = false,
            progress = 100,
            statusText = "Video project export finished & saved to Gallery!",
            outputFilePath = outputFile.absolutePath
        )

        completedJob
    }

    /**
     * Public function to export any current or historical video/audio project file directly
     * to the device media gallery using MediaStore API.
     */
    suspend fun exportProjectToGallery(
        filePath: String,
        customTitle: String? = null,
        isAudio: Boolean = false
    ): Result<Uri> {
        val file = File(filePath)
        return if (isAudio) {
            MediaStoreExporter.exportAudioToGallery(context, file, customTitle)
        } else {
            MediaStoreExporter.exportVideoToGallery(context, file, customTitle)
        }
    }

    /** Result of an in-place metadata edit performed by [editFileMetadata]. */
    data class MetadataEditResult(
        val success: Boolean,
        val message: String
    )

    /**
     * Edits (injects/replaces) the container metadata tags (title/artist/album/genre/year/comment)
     * of an EXISTING rendered job's output -- MP3, WAV, M4A, MP4, MKV or MOV -- without re-encoding.
     *
     * Uses FFmpeg's `-c copy` remux, so the audio/video streams are copied bit-for-bit (no quality
     * loss, near-instant even for large files); only the container's metadata block is rewritten.
     *
     * This keeps THREE things in sync so the edit is actually visible everywhere the file shows up:
     * 1. The app-private file at [RenderJobEntity.outputUri] is atomically replaced with the
     *    remuxed copy (on any failure the original is left untouched).
     * 2. If the job has a [RenderJobEntity.galleryUri] (the public Movies/Music copy created at
     *    export time), that MediaStore entry's bytes are overwritten too -- otherwise the public
     *    Gallery/Music app would keep showing the old tags forever.
     * 3. If [metadata].title is non-blank, [RenderJobEntity.title] in the database (and therefore
     *    the History list) is updated to match, so the UI doesn't show a stale title.
     *
     * @param job the render job whose output should be edited (read from History).
     * @param metadata the new tag values to write. Blank fields are omitted (not cleared to empty).
     */
    suspend fun editFileMetadata(
        job: RenderJobEntity,
        metadata: com.example.core.media.AudioMetadata
    ): MetadataEditResult = withContext(Dispatchers.IO) {
        val sourceFile = File(job.outputUri)
        if (!sourceFile.exists()) {
            return@withContext MetadataEditResult(false, "File not found: ${job.outputUri}")
        }

        val tempOutput = File(sourceFile.parentFile ?: context.cacheDir, "${sourceFile.nameWithoutExtension}_meta_tmp.${sourceFile.extension}")
        val command = FFmpegCommandBuilder.buildMetadataEditCommand(
            inputPath = sourceFile.absolutePath,
            outputPath = tempOutput.absolutePath,
            meta = metadata
        )

        val exitCode = try {
            ffmpegWrapper.execute(command) { }
        } catch (t: Throwable) {
            Timber.e("editFileMetadata: FFmpeg execution threw: ${t.message}", t)
            -99
        }

        if (exitCode != 0 || !tempOutput.exists() || tempOutput.length() <= 0L) {
            try { tempOutput.takeIf { it.exists() }?.delete() } catch (_: Exception) {}
            val log = try { ffmpegWrapper.getLastOutputLog().takeLast(300) } catch (_: Exception) { "" }
            return@withContext MetadataEditResult(false, "Metadata edit failed (exit code $exitCode). $log")
        }

        // 1. Atomically replace the app-private original file with the remuxed copy.
        try {
            if (!tempOutput.renameTo(sourceFile)) {
                // renameTo can fail across filesystems; fall back to copy+delete.
                sourceFile.delete()
                tempOutput.copyTo(sourceFile, overwrite = true)
                tempOutput.delete()
            }
        } catch (e: Exception) {
            try { tempOutput.takeIf { it.exists() }?.delete() } catch (_: Exception) {}
            return@withContext MetadataEditResult(false, "Could not replace original file: ${e.message}")
        }

        // 1b. Embed cover art as a second lossless remux pass, if the user picked an image. This
        // is a separate FFmpeg command (two inputs + -map, vs. the single-input metadata copy
        // above), so it runs after the text-tag edit rather than being combined into one command.
        var coverArtWarning: String? = null
        metadata.coverArtUri?.takeIf { it.isNotBlank() }?.let { coverUri ->
            val ext = sourceFile.extension.lowercase()
            if (ext == "wav") {
                coverArtWarning = " (cover art is not supported for WAV files)"
            } else {
                val coverResult = applyCoverArt(sourceFile, coverUri)
                if (!coverResult.success) {
                    coverArtWarning = " (cover art not applied: ${coverResult.message})"
                }
            }
        }

        // 2. Push the same edited bytes into the public Gallery/Music MediaStore copy, if one
        // exists, so the edit is visible outside the app too (not just in this app's internal
        // storage). Best-effort: a failure here doesn't undo step 1, it's just reported.
        var galleryWarning: String? = null
        job.galleryUri?.let { uriString ->
            try {
                val uri = android.net.Uri.parse(uriString)
                context.contentResolver.openOutputStream(uri, "wt")?.use { out ->
                    sourceFile.inputStream().use { it.copyTo(out) }
                } ?: run { galleryWarning = " (public Gallery copy could not be reopened)" }

                // Refresh the MediaStore display title too, if the tags include a new title.
                if (metadata.title.isNotBlank()) {
                    val values = android.content.ContentValues().apply {
                        val isAudio = job.outputUri.substringAfterLast('.', "").lowercase() in setOf("mp3", "wav", "m4a", "aac")
                        if (isAudio) put(android.provider.MediaStore.Audio.Media.TITLE, metadata.title)
                        else put(android.provider.MediaStore.Video.Media.TITLE, metadata.title)
                    }
                    context.contentResolver.update(uri, values, null, null)
                }
            } catch (e: Exception) {
                Timber.w("editFileMetadata: failed to sync public Gallery copy: ${e.message}")
                galleryWarning = " (public Gallery copy update failed: ${e.message})"
            }
        }

        // 3. Keep the History list's displayed title in sync with the new tag, so it doesn't show
        // a stale auto-generated name after the user renames the track/video.
        if (metadata.title.isNotBlank() && metadata.title != job.title) {
            try {
                repository.updateJob(job.copy(title = metadata.title))
            } catch (e: Exception) {
                Timber.w("editFileMetadata: failed to sync job title in history: ${e.message}")
            }
        }

        MetadataEditResult(true, "Metadata updated successfully.${galleryWarning ?: ""}${coverArtWarning ?: ""}")
    }

    /**
     * Embeds [coverImageUri] (a content:// URI or local path to a JPG/PNG) as the cover art of
     * [targetFile] IN PLACE, via a lossless `-c copy` remux. Resolves the image URI to a local
     * file first (same content:// handling as [resolveInputPath]), probes whether the target
     * already carries a video stream to pick the correct `-disposition:v:<n>` index for MP4-family
     * containers, then atomically replaces [targetFile] with the remuxed copy.
     */
    private suspend fun applyCoverArt(targetFile: File, coverImageUri: String): MetadataEditResult {
        val resolvedCover = resolveInputPath(coverImageUri)
            ?: return MetadataEditResult(false, "Could not read the selected cover image.")

        val isMp3 = targetFile.extension.equals("mp3", ignoreCase = true)
        val hasVideoStream = try {
            val info = com.arthenica.ffmpegkit.FFprobeKit.getMediaInformation(targetFile.absolutePath)
            info?.mediaInformation?.streams?.any { it.type == "video" } ?: false
        } catch (t: Throwable) {
            false
        }

        val tempOutput = File(targetFile.parentFile ?: context.cacheDir, "${targetFile.nameWithoutExtension}_cover_tmp.${targetFile.extension}")
        val command = FFmpegCommandBuilder.buildCoverArtCommand(
            inputPath = targetFile.absolutePath,
            coverImagePath = resolvedCover,
            outputPath = tempOutput.absolutePath,
            isMp3Container = isMp3,
            inputHasVideoStream = hasVideoStream
        )

        val exitCode = try {
            ffmpegWrapper.execute(command) { }
        } catch (t: Throwable) {
            Timber.e("applyCoverArt: FFmpeg execution threw: ${t.message}", t)
            -99
        }
        cleanupResolvedInput(resolvedCover, coverImageUri)

        if (exitCode != 0 || !tempOutput.exists() || tempOutput.length() <= 0L) {
            try { tempOutput.takeIf { it.exists() }?.delete() } catch (_: Exception) {}
            val log = try { ffmpegWrapper.getLastOutputLog().takeLast(300) } catch (_: Exception) { "" }
            return MetadataEditResult(false, "exit code $exitCode. $log")
        }

        return try {
            if (!tempOutput.renameTo(targetFile)) {
                targetFile.delete()
                tempOutput.copyTo(targetFile, overwrite = true)
                tempOutput.delete()
            }
            MetadataEditResult(true, "Cover art applied.")
        } catch (e: Exception) {
            try { tempOutput.takeIf { it.exists() }?.delete() } catch (_: Exception) {}
            MetadataEditResult(false, "Could not replace file with cover art version: ${e.message}")
        }
    }

    /**
     * Reads the container metadata tags currently embedded in an existing media file, via
     * FFprobeKit's media information API. Used to pre-fill the metadata editor with the file's
     * REAL current values instead of opening it blank -- otherwise saving with blank fields would
     * silently wipe out tags the user never intended to touch (since [buildMetadataArgs] treats
     * blank as "leave unset", but a blank field the user never edited would look identical to one
     * they deliberately cleared).
     *
     * Different containers capitalize tag keys differently (e.g. WAV/RIFF commonly uses uppercase
     * "IART"/"INAM"-style or "ARTIST"/"TITLE"), so each field is looked up case-insensitively
     * across the common aliases FFmpeg itself reads/writes.
     */
    suspend fun readFileMetadata(filePath: String): com.example.core.media.AudioMetadata = withContext(Dispatchers.IO) {
        try {
            val info = com.arthenica.ffmpegkit.FFprobeKit.getMediaInformation(filePath)
            val tags = info?.mediaInformation?.tags ?: return@withContext com.example.core.media.AudioMetadata()

            fun read(vararg keys: String): String {
                for (k in keys) {
                    // org.json.JSONObject keys are case-sensitive, so probe common casings.
                    for (candidate in listOf(k, k.lowercase(), k.uppercase())) {
                        if (tags.has(candidate)) {
                            val value = tags.optString(candidate, "")
                            if (value.isNotBlank()) return value
                        }
                    }
                }
                return ""
            }

            com.example.core.media.AudioMetadata(
                title = read("title"),
                artist = read("artist"),
                genre = read("genre"),
                album = read("album"),
                year = read("date", "year"),
                comment = read("comment"),
                trackNumber = read("track"),
                albumArtist = read("album_artist", "albumartist")
            )
        } catch (t: Throwable) {
            Timber.w("readFileMetadata: could not probe tags for $filePath: ${t.message}")
            com.example.core.media.AudioMetadata()
        }
    }

    /**
     * Releases the underlying FFmpeg engine resources. Should be called when this
     * processor is no longer needed to avoid leaking background coroutine scopes.
     */
    fun release() {
        activeJob?.cancel()
        ffmpegWrapper.release()
    }

    /**
     * Resolves a user-supplied media location into a real filesystem path that FFmpeg's native
     * layer (libavformat) can open.
     *
     * The Android file picker (Storage Access Framework) returns `content://` URIs, which FFmpeg
     * CANNOT open directly. When we receive such a URI we copy the stream into a private cache file
     * and return that path; the caller must later pass the returned path to
     * [cleanupResolvedInput] so the temp copy is removed. Plain file paths / `file://` URIs are
     * returned as-is (after stripping the `file://` scheme).
     *
     * @return the local path FFmpeg should read, or null if the content could not be read.
     */
    private fun resolveInputPath(inputUri: String): String? {
        // Already a usable local path.
        if (!inputUri.startsWith("content://")) {
            return if (inputUri.startsWith("file://")) Uri.parse(inputUri).path ?: inputUri else inputUri
        }
        return try {
            val uri = Uri.parse(inputUri)
            val ext = resolveExtensionForUri(uri)
            val cacheDir = File(context.cacheDir, "ffmpeg_input").apply { if (!exists()) mkdirs() }
            val tempFile = File(cacheDir, "src_${System.currentTimeMillis()}_${(0..9999).random()}.$ext")
            context.contentResolver.openInputStream(uri)?.use { input ->
                tempFile.outputStream().use { output -> input.copyTo(output, bufferSize = 1 shl 16) }
            } ?: run {
                Timber.e("resolveInputPath: could not open input stream for $inputUri")
                return null
            }
            if (tempFile.length() <= 0L) {
                Timber.e("resolveInputPath: copied file is empty for $inputUri")
                tempFile.delete()
                return null
            }
            tempFile.absolutePath
        } catch (e: Exception) {
            Timber.e("resolveInputPath failed for $inputUri: ${e.message}")
            null
        }
    }

    /** Best-effort file extension for the copied temp input, based on MIME type or URI suffix. */
    private fun resolveExtensionForUri(uri: Uri): String {
        val mime = try { context.contentResolver.getType(uri) } catch (_: Exception) { null }
        val fromMime = when {
            mime == null -> null
            mime.startsWith("video/") -> mime.substringAfter("video/").substringBefore(";")
            mime.startsWith("audio/") -> mime.substringAfter("audio/").substringBefore(";")
            else -> null
        }?.let { raw ->
            when (raw) {
                "quicktime" -> "mov"
                "x-matroska" -> "mkv"
                "mpeg" -> "mp3"
                "x-wav", "wav" -> "wav"
                "mp4", "3gpp" -> "mp4"
                else -> raw
            }
        }
        val fromSuffix = uri.lastPathSegment?.substringAfterLast('.', "")?.takeIf { it.isNotBlank() && it.length <= 5 }
        return fromMime ?: fromSuffix ?: "mp4"
    }

    /**
     * Deletes a temp input file previously created by [resolveInputPath]. Safe to call with the
     * original path/URI: it only deletes files that live inside our private ffmpeg_input cache.
     */
    private fun cleanupResolvedInput(resolvedPath: String?, originalUri: String) {
        if (resolvedPath == null) return
        // Only remove copies we created (i.e. the input was a content:// URI and the resolved path
        // is inside our cache dir). Never delete the user's own files.
        if (originalUri.startsWith("content://") &&
            resolvedPath.contains("${File.separator}ffmpeg_input${File.separator}")
        ) {
            try { File(resolvedPath).takeIf { it.exists() }?.delete() } catch (_: Exception) {}
        }
    }

    /**
     * Probes the duration (in seconds) of a media file/uri using MediaMetadataRetriever.
     * Returns 0.0 if it cannot be determined. Runs a quick metadata read (no decoding).
     */
    private fun probeDurationSec(pathOrUri: String): Double {
        val retriever = android.media.MediaMetadataRetriever()
        return try {
            when {
                pathOrUri.startsWith("content://") || pathOrUri.startsWith("file://") ->
                    retriever.setDataSource(context, Uri.parse(pathOrUri))
                else -> retriever.setDataSource(pathOrUri)
            }
            val durationMs = retriever
                .extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_DURATION)
                ?.toLongOrNull() ?: 0L
            durationMs / 1000.0
        } catch (e: Exception) {
            0.0
        } finally {
            try { retriever.release() } catch (_: Exception) {}
        }
    }

    private data class LoudnormMeasurements(
        val inputI: Double,
        val inputLra: Double,
        val inputTp: Double,
        val inputThresh: Double
    )

    /**
     * Parses FFmpeg loudnorm JSON output (print_format=json) for the measured input values.
     * Returns null if the expected fields cannot be found or parsed.
     */
    private fun parseLoudnormMeasurements(log: String): LoudnormMeasurements? {
        if (log.isBlank()) return null
        val i = extractLoudnormField(log, "input_i")
        val lra = extractLoudnormField(log, "input_lra")
        val tp = extractLoudnormField(log, "input_tp")
        val thresh = extractLoudnormField(log, "input_thresh")
        if (i == null || lra == null || tp == null || thresh == null) return null
        return LoudnormMeasurements(i, lra, tp, thresh)
    }

    private fun extractLoudnormField(log: String, field: String): Double? {
        // Matches JSON entries such as:  "input_i" : "-23.45"
        val regex = Regex("\"$field\"\\s*:\\s*\"?(-?\\d+(?:\\.\\d+)?)\"?")
        val value = regex.find(log)?.groupValues?.getOrNull(1) ?: return null
        return value.toDoubleOrNull()
    }

    /**
     * Builds an FFmpeg drawtext filter chain that renders the title (top-center) and the
     * watermark (bottom-right) onto the video. Returns an empty string when neither is set.
     * A system font is referenced so text renders reliably on Android.
     */
    private fun buildTextOverlayFilters(titleText: String, watermarkText: String): String {
        val fontFile = resolveSystemFontPath()
        val fontArg = if (fontFile != null) "fontfile='$fontFile':" else ""
        val filters = mutableListOf<String>()

        if (titleText.isNotBlank()) {
            val t = escapeDrawText(titleText)
            filters.add(
                "drawtext=${fontArg}text='$t':fontcolor=white:fontsize=h/18:" +
                    "box=1:boxcolor=black@0.5:boxborderw=12:x=(w-text_w)/2:y=h*0.06"
            )
        }
        if (watermarkText.isNotBlank()) {
            val w = escapeDrawText(watermarkText)
            filters.add(
                "drawtext=${fontArg}text='$w':fontcolor=white@0.85:fontsize=h/32:" +
                    "shadowcolor=black@0.6:shadowx=2:shadowy=2:x=w-text_w-20:y=h-text_h-20"
            )
        }
        return filters.joinToString(",")
    }

    /**
     * Escapes characters that are special inside an FFmpeg drawtext 'text=' value.
     */
    private fun escapeDrawText(text: String): String {
        return text
            .replace("\\", "\\\\")
            .replace(":", "\\:")
            .replace("'", "\u2019") // replace apostrophe with typographic quote to avoid filter break
            .replace("%", "\\%")
    }

    /**
     * Returns the path to a usable system font for drawtext, or null if none is found
     * (in which case FFmpeg falls back to its built-in default font).
     */
    private fun resolveSystemFontPath(): String? {
        val candidates = listOf(
            "/system/fonts/Roboto-Regular.ttf",
            "/system/fonts/DroidSans.ttf",
            "/system/fonts/NotoSans-Regular.ttf"
        )
        return candidates.firstOrNull { File(it).exists() }
    }
}
