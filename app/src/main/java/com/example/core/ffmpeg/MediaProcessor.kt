package com.example.core.ffmpeg

import android.content.Context
import android.net.Uri
import com.example.core.database.LoopingVidRepository
import com.example.core.database.RenderJobEntity
import com.example.core.media.ProcessedMedia
import com.example.core.utils.MediaMimeTypes
import com.example.core.utils.MediaStoreExporter
import com.example.core.utils.SanitizationUtil
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.currentCoroutineContext
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
    private val repository: LoopingVidRepository,
    /**
     * Injectable so render validation can be verified off-device. On a real device this
     * delegates to MediaMetadataRetriever, which is a stub under JVM unit tests.
     */
    private val durationProber: ((String) -> Double)? = null,
    /** Injectable prober for full media validation during JVM tests. */
    private val streamProber: ((String) -> ProcessedMedia)? = null
) {
    private val ffmpegWrapper: FFmpegWrapper = FFmpegWrapperImpl(context)
    private val _progressState = MutableStateFlow(JobProgressState())
    val progressState: StateFlow<JobProgressState> = _progressState.asStateFlow()
    private val _renderState = MutableStateFlow<RenderState>(RenderState.Idle)
    val renderState: StateFlow<RenderState> = _renderState.asStateFlow()

    private var activeJob: Job? = null
    private var activeOutputFile: File? = null
    private var activeSessionId: String? = null

    fun cancelActiveJob() {
        activeJob?.cancel()
        ffmpegWrapper.cancel()
        // A cancelled encode leaves a truncated file behind; it must never be offered as a result.
        try {
            activeOutputFile?.takeIf { it.exists() }?.delete()
        } catch (e: Exception) {
            Timber.e("Failed to delete cancelled render output: ${e.message}")
        }
        activeOutputFile = null
        activeSessionId = null
        _renderState.value = RenderState.Cancelled
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
        projectId: Long? = null
    ): RenderJobEntity = withContext(Dispatchers.IO) {
        // Renders go to an app-private cache temp file, isolated per run.
        val renderCacheDir = File(context.cacheDir, "render").apply { mkdirs() }
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val sessionId = java.util.UUID.randomUUID().toString().substring(0, 8)
        val defaultName = "Loop_${loopStyle.lowercase()}_$timeStamp"
        val name = SanitizationUtil.sanitizeFileName(customFileName?.takeIf { it.isNotBlank() } ?: defaultName, defaultName)

        val outputFile = File(renderCacheDir, "loop_render_${sessionId}.mp4")

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
            paramsSummary = "Quality: $presetQuality, Mute: $muteAudio, Duration: ${targetDurationSec}s",
            projectId = projectId
        )

        val insertedId = repository.saveJob(initialJob)

        // Session identity lets a late callback from an older render be discarded instead of
        // overwriting the state of the render the user is currently watching.
        activeSessionId = sessionId
        activeOutputFile = outputFile
        activeJob = currentCoroutineContext()[Job]

        _renderState.value = RenderState.Preparing
        _progressState.value = JobProgressState(
            jobId = insertedId,
            isProcessing = true,
            progress = 0,
            statusText = "Initializing video loop encoder (${ffmpegWrapper.getVersion()})..."
        )

        try {
            var actualInputUri = inputUri
            var tempTrimFile: File? = null

            if (trimEndSec > trimStartSec) {
                tempTrimFile = File(renderCacheDir, "temp_trim_${sessionId}.mp4")
                val trimCommand = FFmpegCommandBuilder.buildPreciseTrimCommand(
                    inputPath = inputUri,
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

                // A failed trim must not silently fall back to the untrimmed source: the render
                // would then loop the wrong segment while still reporting success.
                if (trimResult != 0) {
                    return@withContext failRender(
                        initialJob = initialJob,
                        insertedId = insertedId,
                        outputFile = outputFile,
                        tempTrimFile = tempTrimFile,
                        reason = "Segment extraction failed (FFmpeg exit code $trimResult)",
                        returnCode = trimResult
                    )
                }
                if (!tempTrimFile.exists()) {
                    return@withContext failRender(
                        initialJob = initialJob,
                        insertedId = insertedId,
                        outputFile = outputFile,
                        tempTrimFile = tempTrimFile,
                        reason = "Segment extraction produced no file",
                        returnCode = trimResult
                    )
                }
                actualInputUri = tempTrimFile.absolutePath
            }

            // Repetition is derived from the real segment length, not a fixed assumption. The
            // exact output length is enforced by -t, so a failed probe cannot shorten the render.
            val segmentDurationSec = probeDurationSec(actualInputUri)
            val loopPlan = LoopDurationPlanner.planLoop(
                segmentDurationSec = segmentDurationSec,
                targetDurationSec = targetDurationSec
            )

            val command = if (loopStyle.equals("CROSSFADE", ignoreCase = true)) {
                FFmpegCommandBuilder.buildCrossfadeLoopCommand(
                    inputPath = actualInputUri,
                    outputPath = outputFile.absolutePath,
                    durationSec = loopPlan.outputDurationSec,
                    targetDurationSec = loopPlan.outputDurationSec,
                    muteAudio = muteAudio,
                    crossfadeDurationSec = crossfadeDurationSec,
                    loopCount = loopPlan.streamLoopCount,
                    resolution = presetQuality,
                    frameRate = frameRate,
                    bitrate = bitrate,
                    aspectRatio = aspectRatio
                )
            } else {
                FFmpegCommandBuilder.buildNormalLoopCommand(
                    inputPath = actualInputUri,
                    outputPath = outputFile.absolutePath,
                    loopCount = loopPlan.streamLoopCount,
                    targetDurationSec = loopPlan.outputDurationSec,
                    muteAudio = muteAudio,
                    presetQuality = presetQuality,
                    resolution = resolution,
                    frameRate = frameRate,
                    bitrate = bitrate,
                    aspectRatio = aspectRatio
                )
            }

            Timber.d(
                "RENDER_DIAG jobId=%d style=%s segmentDurationSec=%.3f targetDurationSec=%.3f streamLoopCount=%d outputPath=%s",
                insertedId, loopStyle, segmentDurationSec, loopPlan.outputDurationSec, loopPlan.streamLoopCount, outputFile.absolutePath
            )
            Timber.d("RENDER_DIAG jobId=%d command=%s", insertedId, command.joinToString(" "))

            _renderState.value = RenderState.Rendering(progress = 0, processedMs = 0, targetMs = (targetDurationSec * 1000).toLong())

            val renderReturnCode = ffmpegWrapper.execute(command, onProgress = { p ->
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
                    outputFilePath = ""
                )

                if (scaledProgress % 10 == 0) {
                    withContext(Dispatchers.IO) {
                        repository.updateJob(initialJob.copy(id = insertedId, progress = scaledProgress, status = "PROCESSING"))
                    }
                }
            }, onStatistics = { stats ->
                // Guard: ignore statistics arriving after the session was replaced or cancelled
                if (activeSessionId == sessionId) {
                    _renderState.value = RenderState.Rendering(
                        progress = stats.percent,
                        processedMs = stats.processedMs,
                        targetMs = stats.targetMs
                    )
                }
            })

            // Clean up temp file regardless of outcome; it is never the render's final output.
            try {
                tempTrimFile?.takeIf { it.exists() }?.delete()
            } catch (e: Exception) {
                Timber.e("Failed to delete temp trim file: ${e.message}")
            }

            // The FFmpeg exit code is the primary signal. A file existing on disk is not
            // proof of success: FFmpeg can write a container header and partial frames
            // before failing (e.g. an unsupported filter graph or missing encoder).

            if (renderReturnCode == FFmpegWrapperImpl.RETURN_CODE_CANCELLED) {
                // Cancellation is explicitly not a failure; it was handled by cancelActiveJob
                // and the output was already deleted. We just return here.
                return@withContext initialJob.copy(id = insertedId, status = "CANCELLED", progress = 0)
            }

            _renderState.value = RenderState.Validating(outputPath = outputFile.absolutePath)

            val outputExists = outputFile.exists()
            val outputSizeBytes = if (outputExists) outputFile.length() else 0L
            val actualDurationSec = if (outputExists) probeDurationSec(outputFile.absolutePath) else 0.0

            val validation = RenderOutputValidator.validate(
                exists = outputExists,
                sizeBytes = outputSizeBytes,
                actualDurationSec = actualDurationSec,
                requestedDurationSec = targetDurationSec,
                returnCode = renderReturnCode
            )

            Timber.d(
                "RENDER_RESULT jobId=%d returnCode=%d actualDurationSec=%.3f targetDurationSec=%.3f fileBytes=%d valid=%s",
                insertedId, renderReturnCode, actualDurationSec, targetDurationSec, outputSizeBytes, validation is RenderValidation.Valid
            )

            if (validation is RenderValidation.Invalid) {
                return@withContext failRender(
                    initialJob = initialJob,
                    insertedId = insertedId,
                    outputFile = outputFile,
                    tempTrimFile = null, // Already deleted in finally-style block
                    reason = validation.reason,
                    returnCode = renderReturnCode
                )
            }

            val calculatedSizeMb = outputSizeBytes.toDouble() / (1024 * 1024)

            val completedJob = initialJob.copy(
                id = insertedId,
                status = "COMPLETED",
                progress = 100,
                fileSizeMb = calculatedSizeMb,
                durationSec = actualDurationSec,
                outputUri = outputFile.absolutePath
            )

            repository.updateJob(completedJob)

            _renderState.value = RenderState.Success(
                outputPath = outputFile.absolutePath,
                durationMs = (actualDurationSec * 1000).toLong(),
                fileSizeBytes = outputSizeBytes,
                resolution = presetQuality // A future probe could read actual video dimensions
            )

            _progressState.value = JobProgressState(
                jobId = insertedId,
                isProcessing = false,
                progress = 100,
                statusText = "Render ready",
                outputFilePath = outputFile.absolutePath
            )

            completedJob
        } catch (e: CancellationException) {
            outputFile.takeIf { it.exists() }?.delete()
            val cancelledJob = initialJob.copy(id = insertedId, status = "CANCELLED", progress = 0)
            repository.updateJob(cancelledJob)
            if (activeSessionId == sessionId) {
                _renderState.value = RenderState.Cancelled
                _progressState.value = JobProgressState(
                    jobId = insertedId,
                    isProcessing = false,
                    errorMessage = "Job was cancelled by user"
                )
            }
            throw e
        } catch (e: Exception) {
            outputFile.takeIf { it.exists() }?.delete()
            val message = e.localizedMessage ?: "Render failed"
            val failedJob = initialJob.copy(id = insertedId, status = "FAILED", progress = 0)
            repository.updateJob(failedJob)
            if (activeSessionId == sessionId) {
                _renderState.value = RenderState.Failed(message)
                _progressState.value = JobProgressState(
                    jobId = insertedId,
                    isProcessing = false,
                    errorMessage = message
                )
            }
            throw e
        } finally {
            if (activeSessionId == sessionId) {
                activeJob = null
                activeOutputFile = null
                activeSessionId = null
            }
        }
    }

    private suspend fun failRender(
        initialJob: RenderJobEntity,
        insertedId: Long,
        outputFile: File,
        tempTrimFile: File?,
        reason: String,
        returnCode: Int
    ): RenderJobEntity {
        try {
            tempTrimFile?.takeIf { it.exists() }?.delete()
            outputFile.takeIf { it.exists() }?.delete()
        } catch (e: Exception) {
            Timber.e("Failed to delete invalid render output: ${e.message}")
        }

        val failedJob = initialJob.copy(id = insertedId, status = "FAILED", progress = 0)
        repository.updateJob(failedJob)
        _renderState.value = RenderState.Failed(reason, returnCode)
        _progressState.value = JobProgressState(
            jobId = insertedId,
            isProcessing = false,
            progress = 0,
            errorMessage = reason
        )
        return failedJob
    }

    /**
     * Reads the real duration of a local/content media path. Returns 0.0 when it cannot be
     * determined, so callers must treat 0.0 as "unknown" rather than "zero-length".
     */
    private fun probeDurationSec(path: String): Double {
        durationProber?.let { return it(path) }

        val retriever = android.media.MediaMetadataRetriever()
        return try {
            if (path.startsWith("content://")) {
                retriever.setDataSource(context, android.net.Uri.parse(path))
            } else {
                retriever.setDataSource(path)
            }
            val durationMs = retriever
                .extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_DURATION)
                ?.toLongOrNull()
            (durationMs ?: 0L) / 1000.0
        } catch (e: Exception) {
            Timber.w("Failed to probe media duration for %s: %s", path, e.message)
            0.0
        } finally {
            try {
                retriever.release()
            } catch (e: Exception) {
                Timber.w("Failed to release MediaMetadataRetriever: %s", e.message)
            }
        }
    }

    /**
     * Probes output streams with platform media metadata. Unknown fields stay null so validation
     * cannot turn an unprobed or partial file into Success.
     */
    private fun probeProcessedMedia(file: File): ProcessedMedia {
        streamProber?.let { return it(file.absolutePath) }

        var durationMs = 0L
        var width: Int? = null
        var height: Int? = null
        var videoMime: String? = null
        var audioMime: String? = null
        var sampleRateHz: Int? = null
        var channels: Int? = null
        val extractor = android.media.MediaExtractor()

        try {
            extractor.setDataSource(file.absolutePath)
            for (trackIndex in 0 until extractor.trackCount) {
                val format = extractor.getTrackFormat(trackIndex)
                val mime = format.getString(android.media.MediaFormat.KEY_MIME) ?: continue
                if (format.containsKey(android.media.MediaFormat.KEY_DURATION)) {
                    durationMs = maxOf(durationMs, format.getLong(android.media.MediaFormat.KEY_DURATION) / 1000L)
                }
                if (mime.startsWith("video/")) {
                    videoMime = mime
                    width = format.getIntegerOrNull(android.media.MediaFormat.KEY_WIDTH)
                    height = format.getIntegerOrNull(android.media.MediaFormat.KEY_HEIGHT)
                }
                if (mime.startsWith("audio/")) {
                    audioMime = mime
                    sampleRateHz = format.getIntegerOrNull(android.media.MediaFormat.KEY_SAMPLE_RATE)
                    channels = format.getIntegerOrNull(android.media.MediaFormat.KEY_CHANNEL_COUNT)
                }
            }
        } catch (e: Exception) {
            Timber.w("Failed to probe output streams for %s: %s", file.absolutePath, e.message)
        } finally {
            try {
                extractor.release()
            } catch (e: Exception) {
                Timber.w("Failed to release MediaExtractor: %s", e.message)
            }
        }

        if (durationMs <= 0L) {
            durationMs = (probeDurationSec(file.absolutePath) * 1000).toLong()
        }
        return ProcessedMedia(
            path = file.absolutePath,
            durationMs = durationMs,
            sizeBytes = if (file.exists()) file.length() else 0L,
            width = width,
            height = height,
            videoCodec = videoMime,
            audioCodec = audioMime,
            sampleRateHz = sampleRateHz,
            channels = channels
        )
    }

    private fun android.media.MediaFormat.getIntegerOrNull(key: String): Int? {
        return if (containsKey(key)) getInteger(key) else null
    }

    suspend fun executeTwoPassAudioNormalization(
        inputUri: String,
        targetLufs: Double = -14.0,
        exportFormat: String = "mp4",
        customFileName: String? = null,
        @Suppress("UNUSED_PARAMETER") destinationFolder: String? = null,
        projectId: Long? = null
    ): RenderJobEntity = withContext(Dispatchers.IO) {
        val sessionId = java.util.UUID.randomUUID().toString().substring(0, 8)
        val masteringCacheDir = File(context.cacheDir, "mastering").apply { mkdirs() }
        val outputFile = File(masteringCacheDir, "normalized_${sessionId}.${exportFormat.lowercase()}")
        val expectedDurationSec = probeDurationSec(inputUri).takeIf { it > 0.0 }

        val initialJob = RenderJobEntity(
            jobType = "MASTERING",
            title = "2-Pass Audio Normalization",
            inputUri = inputUri,
            outputUri = outputFile.absolutePath,
            style = "NORMALIZATION",
            status = "PROCESSING",
            progress = 0,
            durationSec = expectedDurationSec ?: 0.0,
            fileSizeMb = 0.0,
            paramsSummary = "Target: $targetLufs LUFS (2-Pass), Format: $exportFormat",
            projectId = projectId
        )
        val insertedId = repository.saveJob(initialJob)

        activeSessionId = sessionId
        activeOutputFile = outputFile
        activeJob = currentCoroutineContext()[Job]
        _renderState.value = RenderState.Preparing
        _progressState.value = JobProgressState(
            jobId = insertedId,
            isProcessing = true,
            progress = 0,
            statusText = "Pass 1: measuring source loudness..."
        )

        try {
            // Pass 1 only measures; its numbers must come from FFmpeg, never from assumptions.
            val measurement = measureLoudness(inputUri, targetLufs) { percent ->
                _progressState.value = _progressState.value.copy(progress = percent / 2)
            } ?: return@withContext failRender(
                initialJob,
                insertedId,
                outputFile,
                null,
                "Loudness analysis did not report measurable levels",
                FFmpegWrapperImpl.RETURN_CODE_FAILED
            )

            _progressState.value = _progressState.value.copy(
                progress = 50,
                statusText = "Pass 2: applying measured normalization..."
            )
            val normalizeCommand = FFmpegCommandBuilder.buildLoudnormSecondPassCommand(
                inputPath = inputUri,
                outputPath = outputFile.absolutePath,
                targetLufs = targetLufs,
                measuredI = measurement.inputIntegratedLufs,
                measuredLra = measurement.inputLoudnessRange,
                measuredTp = measurement.inputTruePeakDb,
                measuredThresh = measurement.inputThreshold
            ).toMutableList()
            if (expectedDurationSec != null && "-t" !in normalizeCommand) {
                normalizeCommand.add(normalizeCommand.lastIndex, "-t")
                normalizeCommand.add(
                    normalizeCommand.lastIndex,
                    String.format(Locale.US, "%.3f", expectedDurationSec)
                )
            }

            _renderState.value = RenderState.Rendering(
                progress = 0,
                processedMs = 0L,
                targetMs = ((expectedDurationSec ?: 0.0) * 1000).toLong()
            )
            val returnCode = ffmpegWrapper.execute(
                commandArgs = normalizeCommand,
                onStatistics = { statistics ->
                    if (activeSessionId == sessionId) {
                        _renderState.value = RenderState.Rendering(
                            statistics.percent,
                            statistics.processedMs,
                            statistics.targetMs
                        )
                    }
                },
                onProgress = { percent ->
                    val totalPercent = 50 + (percent / 2)
                    _progressState.value = _progressState.value.copy(
                        isProcessing = totalPercent < 100,
                        progress = totalPercent,
                        statusText = "Pass 2: applying measured normalization ($totalPercent%)"
                    )
                }
            )

            if (returnCode == FFmpegWrapperImpl.RETURN_CODE_CANCELLED) {
                outputFile.takeIf { it.exists() }?.delete()
                val cancelledJob = initialJob.copy(id = insertedId, status = "CANCELLED")
                repository.updateJob(cancelledJob)
                _renderState.value = RenderState.Cancelled
                return@withContext cancelledJob
            }

            _renderState.value = RenderState.Validating(outputFile.absolutePath)
            val media = probeProcessedMedia(outputFile)
            val validation = if (returnCode != 0) {
                RenderValidation.Invalid("FFmpeg exited with code $returnCode")
            } else {
                ProcessedMediaValidator.validate(
                    media = media,
                    exists = outputFile.exists(),
                    expectedDurationMs = expectedDurationSec?.let { (it * 1000).toLong() },
                    requireVideo = exportFormat.lowercase() in VIDEO_CONTAINERS,
                    requireAudio = true
                )
            }
            if (validation is RenderValidation.Invalid) {
                return@withContext failRender(
                    initialJob,
                    insertedId,
                    outputFile,
                    null,
                    validation.reason,
                    returnCode
                )
            }

            val completedJob = initialJob.copy(
                id = insertedId,
                status = "COMPLETED",
                progress = 100,
                durationSec = media.durationMs / 1000.0,
                fileSizeMb = media.sizeBytes.toDouble() / (1024 * 1024),
                outputUri = outputFile.absolutePath,
                paramsSummary = initialJob.paramsSummary +
                    String.format(
                        Locale.US,
                        ", Measured: %.2f LUFS / TP %.2f dB",
                        measurement.inputIntegratedLufs,
                        measurement.inputTruePeakDb
                    )
            )
            repository.updateJob(completedJob)
            _renderState.value = RenderState.Success(
                outputPath = outputFile.absolutePath,
                durationMs = media.durationMs,
                fileSizeBytes = media.sizeBytes
            )
            _progressState.value = JobProgressState(
                jobId = insertedId,
                isProcessing = false,
                progress = 100,
                statusText = "Normalization completed and validated",
                outputFilePath = outputFile.absolutePath
            )
            completedJob
        } catch (e: CancellationException) {
            outputFile.takeIf { it.exists() }?.delete()
            repository.updateJob(initialJob.copy(id = insertedId, status = "CANCELLED"))
            _renderState.value = RenderState.Cancelled
            throw e
        } catch (e: Exception) {
            outputFile.takeIf { it.exists() }?.delete()
            val message = e.localizedMessage ?: "Normalization failed"
            repository.updateJob(initialJob.copy(id = insertedId, status = "FAILED"))
            _renderState.value = RenderState.Failed(message)
            _progressState.value = JobProgressState(jobId = insertedId, errorMessage = message)
            throw e
        } finally {
            if (activeSessionId == sessionId) {
                activeJob = null
                activeOutputFile = null
                activeSessionId = null
            }
        }
    }

    /**
     * Runs the analysis-only loudnorm pass and returns what FFmpeg actually measured.
     * Returns null when the pass fails or prints no usable measurement, so the caller
     * fails instead of normalizing against invented levels.
     */
    private suspend fun measureLoudness(
        inputUri: String,
        targetLufs: Double,
        onProgress: suspend (Int) -> Unit
    ): LoudnormMeasurement? {
        onProgress(0)
        val analysis = ffmpegWrapper.executeForOutput(
            FFmpegCommandBuilder.buildLoudnormAnalysisCommand(inputUri, targetLufs)
        )
        onProgress(100)
        if (analysis.returnCode != 0) {
            Timber.w("Loudness analysis failed with exit code %d", analysis.returnCode)
            return null
        }
        return LoudnormAnalysisParser.parse(analysis.output)
    }

    private companion object {
        /** Containers that must carry a video stream after normalization. */
        val VIDEO_CONTAINERS = setOf("mp4", "mkv", "mov", "webm")

        /** Used only when the input duration cannot be probed, so fade-out still lands somewhere sane. */
        const val FALLBACK_AUDIO_DURATION_SEC = 180.0
    }

    suspend fun executeMasteringJob(
        inputUri: String,
        presetName: String,
        targetLufs: Double,
        exportFormat: String = "MP3",
        customFileName: String? = null,
        @Suppress("UNUSED_PARAMETER") destinationFolder: String? = null,
        isNoiseReductionEnabled: Boolean = false,
        noiseReductionDb: Float = 12f,
        noiseFloorDb: Float = -45f,
        isAutoLevelingEnabled: Boolean = false,
        autoLevelingTargetLufs: Float = -14.0f,
        fadeInSec: Float = 0f,
        fadeOutSec: Float = 0f,
        audioMetadata: com.example.core.media.AudioMetadata? = null,
        projectId: Long? = null
    ): RenderJobEntity = withContext(Dispatchers.IO) {
        val sessionId = java.util.UUID.randomUUID().toString().substring(0, 8)
        val masteringCacheDir = File(context.cacheDir, "mastering").apply { mkdirs() }
        val outputFile = File(masteringCacheDir, "master_${sessionId}.${exportFormat.lowercase()}")
        val expectedDurationSec = probeDurationSec(inputUri).takeIf { it > 0.0 }

        val fadeSummary = if (fadeInSec > 0f || fadeOutSec > 0f) ", Fades: in ${fadeInSec}s / out ${fadeOutSec}s" else ""
        val initialJob = RenderJobEntity(
            jobType = "MASTERING",
            title = "Audio Master ($presetName)",
            inputUri = inputUri,
            outputUri = outputFile.absolutePath,
            style = "MASTERING",
            status = "PROCESSING",
            progress = 0,
            durationSec = expectedDurationSec ?: 0.0,
            fileSizeMb = 0.0,
            paramsSummary = "Preset: $presetName, Target: ${targetLufs} LUFS, Format: $exportFormat, FFT Noise Red: ${if (isNoiseReductionEnabled) "${noiseReductionDb.toInt()}dB" else "OFF"}$fadeSummary",
            projectId = projectId
        )

        val insertedId = repository.saveJob(initialJob)

        activeSessionId = sessionId
        activeOutputFile = outputFile
        activeJob = currentCoroutineContext()[Job]
        _renderState.value = RenderState.Preparing
        _progressState.value = JobProgressState(
            jobId = insertedId,
            isProcessing = true,
            progress = 0,
            statusText = "Preparing audio mastering pipeline..."
        )

        try {
            val command = FFmpegCommandBuilder.buildMasteringCommand(
                inputPath = inputUri,
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
                // Fade-out is positioned from the end of the track, so it needs the real
                // input length instead of the builder's placeholder default.
                audioDurationSec = expectedDurationSec ?: FALLBACK_AUDIO_DURATION_SEC,
                audioMetadata = audioMetadata
            ).toMutableList()
            if (expectedDurationSec != null && "-t" !in command) {
                command.add(command.lastIndex, "-t")
                command.add(command.lastIndex, String.format(Locale.US, "%.3f", expectedDurationSec))
            }

            _renderState.value = RenderState.Rendering(0, 0L, ((expectedDurationSec ?: 0.0) * 1000).toLong())
            val returnCode = ffmpegWrapper.execute(
                commandArgs = command,
                onStatistics = { statistics ->
                    if (activeSessionId == sessionId) {
                        _renderState.value = RenderState.Rendering(
                            statistics.percent,
                            statistics.processedMs,
                            statistics.targetMs
                        )
                    }
                },
                onProgress = { percent ->
                    val stepMessage = when {
                        percent < 30 -> "Applying 5-Band EQ filters ($percent%)..."
                        percent < 70 -> "Dynamics Compression & Peak Limiting ($percent%)..."
                        else -> "Normalizing to target $targetLufs LUFS ($percent%)..."
                    }
                    _progressState.value = _progressState.value.copy(
                        isProcessing = percent < 100,
                        progress = percent,
                        statusText = stepMessage
                    )
                }
            )

            if (returnCode == FFmpegWrapperImpl.RETURN_CODE_CANCELLED) {
                outputFile.delete()
                val cancelledJob = initialJob.copy(id = insertedId, status = "CANCELLED")
                repository.updateJob(cancelledJob)
                _renderState.value = RenderState.Cancelled
                return@withContext cancelledJob
            }

            _renderState.value = RenderState.Validating(outputFile.absolutePath)
            val media = probeProcessedMedia(outputFile)
            val validation = if (returnCode != 0) {
                RenderValidation.Invalid("FFmpeg exited with code $returnCode")
            } else {
                ProcessedMediaValidator.validate(
                    media = media,
                    exists = outputFile.exists(),
                    expectedDurationMs = expectedDurationSec?.let { (it * 1000).toLong() },
                    requireVideo = false,
                    requireAudio = true
                )
            }
            if (validation is RenderValidation.Invalid) {
                return@withContext failRender(
                    initialJob,
                    insertedId,
                    outputFile,
                    null,
                    validation.reason,
                    returnCode
                )
            }

            val completedJob = initialJob.copy(
                id = insertedId,
                status = "COMPLETED",
                progress = 100,
                durationSec = media.durationMs / 1000.0,
                fileSizeMb = media.sizeBytes.toDouble() / (1024 * 1024),
                outputUri = outputFile.absolutePath
            )
            repository.updateJob(completedJob)
            _renderState.value = RenderState.Success(
                outputPath = outputFile.absolutePath,
                durationMs = media.durationMs,
                fileSizeBytes = media.sizeBytes
            )
            _progressState.value = JobProgressState(
                jobId = insertedId,
                isProcessing = false,
                progress = 100,
                statusText = "Mastering completed and validated",
                outputFilePath = outputFile.absolutePath
            )
            completedJob
        } catch (e: CancellationException) {
            outputFile.delete()
            repository.updateJob(initialJob.copy(id = insertedId, status = "CANCELLED"))
            _renderState.value = RenderState.Cancelled
            throw e
        } catch (e: Exception) {
            outputFile.delete()
            val message = e.localizedMessage ?: "Audio mastering failed"
            repository.updateJob(initialJob.copy(id = insertedId, status = "FAILED"))
            _renderState.value = RenderState.Failed(message)
            _progressState.value = JobProgressState(jobId = insertedId, errorMessage = message)
            throw e
        } finally {
            if (activeSessionId == sessionId) {
                activeJob = null
                activeOutputFile = null
                activeSessionId = null
            }
        }
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
        @Suppress("UNUSED_PARAMETER") destinationFolder: String? = null,
        exportFormat: String = "mp4",
        resolution: String = "1080p",
        frameRate: String = "30fps",
        bitrate: String = "Medium",
        aspectRatio: String = "Asli"
    ): RenderJobEntity = withContext(Dispatchers.IO) {
        val sessionId = java.util.UUID.randomUUID().toString().substring(0, 8)
        val masteringCacheDir = File(context.cacheDir, "mastering").apply { mkdirs() }
        val outputFile = File(masteringCacheDir, "trim_master_${sessionId}.${exportFormat.lowercase()}")
        val trimmedDurationSec = (trimEndSec - trimStartSec).takeIf { it > 0.0 }
        val expectedDurationSec = trimmedDurationSec
            ?: probeDurationSec(inputUri).takeIf { it > 0.0 }

        val initialJob = RenderJobEntity(
            jobType = "MASTERING",
            title = "Trimmed Video Master ($presetName)",
            inputUri = inputUri,
            outputUri = outputFile.absolutePath,
            style = "TRIMMED_MASTER",
            status = "PROCESSING",
            progress = 0,
            durationSec = expectedDurationSec ?: 0.0,
            fileSizeMb = 0.0,
            paramsSummary = "Trim: %.1fs-%.1fs, Preset: $presetName, Target: ${targetLufs} LUFS".format(trimStartSec, trimEndSec)
        )
        val insertedId = repository.saveJob(initialJob)

        activeSessionId = sessionId
        activeOutputFile = outputFile
        activeJob = currentCoroutineContext()[Job]
        _renderState.value = RenderState.Preparing
        _progressState.value = JobProgressState(
            jobId = insertedId,
            isProcessing = true,
            statusText = "Preparing trimmed mastering pipeline..."
        )

        try {
            val command = FFmpegCommandBuilder.buildTrimmedVideoAudioMasteringCommand(
                inputPath = inputUri,
                outputPath = outputFile.absolutePath,
                trimStartSec = trimStartSec,
                trimEndSec = trimEndSec,
                targetLufs = targetLufs,
                presetName = presetName,
                presetQuality = presetQuality,
                videoFilter = videoFilterString,
                resolution = resolution,
                frameRate = frameRate,
                bitrate = bitrate,
                aspectRatio = aspectRatio
            ).toMutableList()
            if (expectedDurationSec != null && "-t" !in command) {
                command.add(command.lastIndex, "-t")
                command.add(command.lastIndex, String.format(Locale.US, "%.3f", expectedDurationSec))
            }

            _renderState.value = RenderState.Rendering(
                progress = 0,
                processedMs = 0L,
                targetMs = ((expectedDurationSec ?: 0.0) * 1000).toLong()
            )
            val returnCode = ffmpegWrapper.execute(
                commandArgs = command,
                onStatistics = { statistics ->
                    if (activeSessionId == sessionId) {
                        _renderState.value = RenderState.Rendering(
                            statistics.percent,
                            statistics.processedMs,
                            statistics.targetMs
                        )
                    }
                },
                onProgress = { percent ->
                    val status = when {
                        percent < 30 -> "Trimming video and decoding audio ($percent%)..."
                        percent < 70 -> "Applying mastering filters ($percent%)..."
                        else -> "Encoding mastered video ($percent%)..."
                    }
                    _progressState.value = _progressState.value.copy(
                        isProcessing = percent < 100,
                        progress = percent,
                        statusText = status
                    )
                }
            )

            if (returnCode == FFmpegWrapperImpl.RETURN_CODE_CANCELLED) {
                outputFile.delete()
                val cancelledJob = initialJob.copy(id = insertedId, status = "CANCELLED")
                repository.updateJob(cancelledJob)
                _renderState.value = RenderState.Cancelled
                return@withContext cancelledJob
            }

            _renderState.value = RenderState.Validating(outputFile.absolutePath)
            val media = probeProcessedMedia(outputFile)
            val validation = if (returnCode != 0) {
                RenderValidation.Invalid("FFmpeg exited with code $returnCode")
            } else {
                ProcessedMediaValidator.validate(
                    media = media,
                    exists = outputFile.exists(),
                    expectedDurationMs = expectedDurationSec?.let { (it * 1000).toLong() },
                    requireVideo = true,
                    requireAudio = true
                )
            }
            if (validation is RenderValidation.Invalid) {
                return@withContext failRender(
                    initialJob,
                    insertedId,
                    outputFile,
                    null,
                    validation.reason,
                    returnCode
                )
            }

            val completedJob = initialJob.copy(
                id = insertedId,
                status = "COMPLETED",
                progress = 100,
                durationSec = media.durationMs / 1000.0,
                fileSizeMb = media.sizeBytes.toDouble() / (1024 * 1024),
                outputUri = outputFile.absolutePath
            )
            repository.updateJob(completedJob)
            _renderState.value = RenderState.Success(
                outputPath = outputFile.absolutePath,
                durationMs = media.durationMs,
                fileSizeBytes = media.sizeBytes,
                resolution = "${media.width}x${media.height}"
            )
            _progressState.value = JobProgressState(
                jobId = insertedId,
                isProcessing = false,
                progress = 100,
                statusText = "Trimmed mastering completed and validated",
                outputFilePath = outputFile.absolutePath
            )
            completedJob
        } catch (e: CancellationException) {
            outputFile.delete()
            repository.updateJob(initialJob.copy(id = insertedId, status = "CANCELLED"))
            _renderState.value = RenderState.Cancelled
            throw e
        } catch (e: Exception) {
            outputFile.delete()
            val message = e.localizedMessage ?: "Trimmed mastering failed"
            repository.updateJob(initialJob.copy(id = insertedId, status = "FAILED"))
            _renderState.value = RenderState.Failed(message)
            _progressState.value = JobProgressState(jobId = insertedId, errorMessage = message)
            throw e
        } finally {
            if (activeSessionId == sessionId) {
                activeJob = null
                activeOutputFile = null
                activeSessionId = null
            }
        }
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
        @Suppress("UNUSED_PARAMETER") destinationFolder: String? = null,
        exportFormat: String = "mp4",
        overlayUri: String? = null,
        overlayPosition: String? = null,
        resolution: String = "1080p",
        frameRate: String = "30fps",
        bitrate: String = "Medium",
        aspectRatio: String = "Asli",
        projectId: Long? = null
    ): RenderJobEntity = withContext(Dispatchers.IO) {
        val sessionId = java.util.UUID.randomUUID().toString().substring(0, 8)
        val editorCacheDir = File(context.cacheDir, "editor").apply { mkdirs() }
        val outputFile = File(editorCacheDir, "edit_${sessionId}.${exportFormat.lowercase()}")
        val expectedDurationSec = probeDurationSec(mediaUri).takeIf { it > 0.0 }
        val initialJob = RenderJobEntity(
            jobType = "EDITOR",
            title = if (titleText.isNotBlank()) titleText else "Edited Video Project",
            inputUri = mediaUri,
            outputUri = outputFile.absolutePath,
            style = "EDITOR",
            status = "PROCESSING",
            progress = 0,
            durationSec = expectedDurationSec ?: 0.0,
            fileSizeMb = 0.0,
            paramsSummary = "Text: '$titleText', Spectrum: $spectrumStyle, Quality: $presetQuality, Filter: ${filterString?.ifBlank { "None" } ?: "None"}",
            projectId = projectId
        )
        val insertedId = repository.saveJob(initialJob)
        activeSessionId = sessionId
        activeOutputFile = outputFile
        activeJob = currentCoroutineContext()[Job]
        _renderState.value = RenderState.Preparing
        _progressState.value = JobProgressState(
            jobId = insertedId,
            isProcessing = true,
            statusText = "Preparing editor render..."
        )

        try {
            val commandList = mutableListOf("-i", mediaUri)
            var filterComplexStr = ""
            if (overlayUri != null) {
                commandList.addAll(listOf("-i", overlayUri))
                val position = when (overlayPosition) {
                    "TOP_LEFT" -> "10:10"
                    "TOP_RIGHT" -> "W-w-10:10"
                    "BOTTOM_LEFT" -> "10:H-h-10"
                    else -> "W-w-10:H-h-10"
                }
                val baseFilter = filterString?.takeIf { it.isNotBlank() } ?: "null"
                filterComplexStr = "[0:v]$baseFilter[bg];[1:v]scale=iw*0.3:-1[fg];[bg][fg]overlay=$position"
            }
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
            if (filterComplexStr.isNotBlank()) {
                commandList.addAll(listOf("-filter_complex", "$filterComplexStr,$scaleFilter"))
            } else {
                val finalFilter = if (!filterString.isNullOrBlank()) "$filterString,$scaleFilter" else scaleFilter
                commandList.addAll(listOf("-vf", finalFilter))
            }
            val fps = frameRate.replace("fps", "")
            if (fps.isNotEmpty()) commandList.addAll(listOf("-r", fps))
            val videoBitrate = when (bitrate) {
                "Low" -> "1M"
                "Medium" -> "4M"
                "High" -> "8M"
                else -> "4M"
            }
            if (expectedDurationSec != null) {
                commandList.addAll(listOf("-t", String.format(Locale.US, "%.3f", expectedDurationSec)))
            }
            commandList.addAll(listOf(
                "-c:v", "libopenh264",
                "-b:v", videoBitrate,
                "-maxrate", videoBitrate,
                "-bufsize", "${videoBitrate.replace("M", "")}M",
                "-y", outputFile.absolutePath
            ))

            _renderState.value = RenderState.Rendering(0, 0L, ((expectedDurationSec ?: 0.0) * 1000).toLong())
            val returnCode = ffmpegWrapper.execute(
                commandList,
                onStatistics = { statistics ->
                    if (activeSessionId == sessionId) {
                        _renderState.value = RenderState.Rendering(
                            statistics.percent,
                            statistics.processedMs,
                            statistics.targetMs
                        )
                    }
                },
                onProgress = { percent ->
                    _progressState.value = _progressState.value.copy(
                        isProcessing = percent < 100,
                        progress = percent,
                        statusText = "Rendering editor output ($percent%)"
                    )
                }
            )
            if (returnCode == FFmpegWrapperImpl.RETURN_CODE_CANCELLED) {
                outputFile.delete()
                repository.updateJob(initialJob.copy(id = insertedId, status = "CANCELLED"))
                _renderState.value = RenderState.Cancelled
                return@withContext initialJob.copy(id = insertedId, status = "CANCELLED")
            }

            _renderState.value = RenderState.Validating(outputFile.absolutePath)
            val media = probeProcessedMedia(outputFile)
            val validation = ProcessedMediaValidator.validate(
                media = media,
                exists = outputFile.exists(),
                expectedDurationMs = expectedDurationSec?.let { (it * 1000).toLong() },
                requireVideo = true,
                requireAudio = false
            ).let { result ->
                if (returnCode == 0) result else RenderValidation.Invalid("FFmpeg exited with code $returnCode")
            }
            if (validation is RenderValidation.Invalid) {
                return@withContext failRender(initialJob, insertedId, outputFile, null, validation.reason, returnCode)
            }

            val actualDurationSec = media.durationMs / 1000.0
            val completedJob = initialJob.copy(
                id = insertedId,
                status = "COMPLETED",
                progress = 100,
                durationSec = actualDurationSec,
                fileSizeMb = media.sizeBytes.toDouble() / (1024 * 1024),
                outputUri = outputFile.absolutePath
            )
            repository.updateJob(completedJob)
            _renderState.value = RenderState.Success(
                outputPath = outputFile.absolutePath,
                durationMs = media.durationMs,
                fileSizeBytes = media.sizeBytes,
                resolution = "${media.width ?: 0}x${media.height ?: 0}"
            )
            _progressState.value = JobProgressState(
                jobId = insertedId,
                isProcessing = false,
                progress = 100,
                statusText = "Editor render ready",
                outputFilePath = outputFile.absolutePath
            )
            completedJob
        } catch (e: CancellationException) {
            outputFile.delete()
            repository.updateJob(initialJob.copy(id = insertedId, status = "CANCELLED"))
            _renderState.value = RenderState.Cancelled
            throw e
        } catch (e: Exception) {
            outputFile.delete()
            val message = e.localizedMessage ?: "Editor render failed"
            repository.updateJob(initialJob.copy(id = insertedId, status = "FAILED"))
            _renderState.value = RenderState.Failed(message)
            _progressState.value = JobProgressState(jobId = insertedId, errorMessage = message)
            throw e
        } finally {
            if (activeSessionId == sessionId) {
                activeJob = null
                activeOutputFile = null
                activeSessionId = null
            }
        }
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
            val mimeType = MediaMimeTypes.resolveAudioMimeTypeForFile(filePath)
            MediaStoreExporter.exportAudioToGallery(context, file, customTitle, mimeType)
        } else {
            val mimeType = MediaMimeTypes.resolveVideoMimeTypeForFile(filePath)
            MediaStoreExporter.exportVideoToGallery(context, file, customTitle, mimeType)
        }
    }
}
