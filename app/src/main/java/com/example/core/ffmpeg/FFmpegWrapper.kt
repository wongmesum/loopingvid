package com.example.core.ffmpeg

import kotlinx.coroutines.flow.Flow

/**
 * FFmpeg Wrapper Interface for media processing commands in LoopingVid Studio.
 */
interface FFmpegWrapper {
    /**
     * Shared flow emitting live execution logs from FFmpeg.
     */
    val logFlow: Flow<String>

    /**
     * Returns true if native FFmpeg library is loaded via JNI/NDK.
     */
    fun isNativeSupported(): Boolean

    /**
     * Returns the version string of the FFmpeg engine.
     */
    fun getVersion(): String

    /**
     * Executes an FFmpeg command string asynchronously.
     * @param command Single FFmpeg command string, e.g. "-i input.mp4 -c:v libx264 output.mp4"
     * @return Exit code (0 for success)
     */
    suspend fun execute(command: String): Int

    /**
     * Executes an FFmpeg command sequence asynchronously.
     * @param commandArgs List of arguments, e.g., listOf("-i", "input.mp4", "-c:v", "h264_mediacodec", "output.mp4")
     * @param onProgress Callback receiving progress percent (0..100)
     * @return Exit code (0 for success)
     */
    suspend fun execute(
        commandArgs: List<String>,
        onProgress: suspend (Int) -> Unit = {}
    ): Int

    /**
     * Cancels any active FFmpeg execution session.
     */
    fun cancel()
}

/**
 * FFmpeg Command Builder Utility for video looping, audio mastering, and editor processing.
 */
object FFmpegCommandBuilder {

    private const val KEN_BURNS_ZOOM_STEP = "0.0015"
    private const val KEN_BURNS_MAX_ZOOM = "1.5"

    fun buildPreciseTrimCommand(
        inputPath: String,
        outputPath: String,
        trimStartSec: Double,
        trimEndSec: Double
    ): List<String> {
        val duration = trimEndSec - trimStartSec
        return listOf(
            "-ss", String.format(java.util.Locale.US, "%.3f", trimStartSec),
            "-i", inputPath,
            "-t", String.format(java.util.Locale.US, "%.3f", duration),
            "-c:v", "libx264",
            "-preset", "ultrafast",
            "-crf", "18",
            "-c:a", "aac",
            "-b:a", "192k",
            "-y", outputPath
        )
    }

    private fun getVideoExportArgs(resolution: String, frameRate: String, bitrate: String, aspectRatio: String = "Asli"): List<String> {
        val args = mutableListOf<String>()
        val fps = frameRate.replace("fps", "")
        if (fps.isNotEmpty()) {
            args.addAll(listOf("-r", fps))
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
        
        args.addAll(listOf("-vf", scaleFilter))
        
        val bv = when (bitrate) {
            "Low" -> "1M"
            "Medium" -> "4M"
            "High" -> "8M"
            else -> "4M"
        }
        args.addAll(listOf("-b:v", bv, "-maxrate", bv, "-bufsize", "${bv.replace("M", "")}M", "-preset", "ultrafast"))
        
        return args
    }

    fun buildNormalLoopCommand(
        inputPath: String,
        outputPath: String,
        loopCount: Int,
        targetDurationSec: Double,
        muteAudio: Boolean = false,
        presetQuality: String = "1080p",
        resolution: String = "1080p",
        frameRate: String = "30fps",
        bitrate: String = "Medium",
        aspectRatio: String = "Asli"
    ): List<String> {
        val args = mutableListOf(
            "-stream_loop", loopCount.toString(),
            "-i", inputPath,
            "-t", formatDuration(targetDurationSec),
            "-c:v", "libx264"
        )
        args.addAll(getVideoExportArgs(resolution, frameRate, bitrate, aspectRatio))
        appendAudioArgs(args, muteAudio)
        args.addAll(listOf("-y", outputPath))
        return args
    }

    fun buildCrossfadeLoopCommand(
        inputPath: String,
        outputPath: String,
        durationSec: Double,
        targetDurationSec: Double = durationSec,
        muteAudio: Boolean = false,
        crossfadeDurationSec: Double = 1.0,
        resolution: String = "1080p",
        frameRate: String = "30fps",
        bitrate: String = "Medium",
        aspectRatio: String = "Asli"
    ): List<String> {
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
        
        val filterGraph = "xfade=transition=fade:duration=$crossfadeDurationSec:offset=${(durationSec - crossfadeDurationSec).coerceAtLeast(0.1)},$scaleFilter"
        
        val fps = frameRate.replace("fps", "")
        val bv = when (bitrate) {
            "Low" -> "1M"
            "Medium" -> "4M"
            "High" -> "8M"
            else -> "4M"
        }

        val args = mutableListOf(
            "-i", inputPath,
            "-filter_complex", filterGraph,
            "-t", formatDuration(targetDurationSec),
            "-c:v", "libx264",
            "-preset", "ultrafast",
            "-r", fps,
            "-b:v", bv, "-maxrate", bv, "-bufsize", "${bv.replace("M", "")}M"
        )
        appendAudioArgs(args, muteAudio)
        args.addAll(listOf("-y", outputPath))
        return args
    }

    private fun appendAudioArgs(args: MutableList<String>, muteAudio: Boolean) {
        if (muteAudio) {
            args.add("-an")
            return
        }
        args.addAll(listOf("-c:a", "aac", "-b:a", "192k"))
    }

    private fun formatDuration(durationSec: Double): String =
        String.format(java.util.Locale.US, "%.3f", durationSec)

    fun buildMasteringCommand(
        inputPath: String,
        outputPath: String,
        targetLufs: Double = -14.0,
        audioFormat: String = "mp3",
        isNoiseReductionEnabled: Boolean = false,
        noiseReductionDb: Float = 12f,
        noiseFloorDb: Float = -45f,
        isAutoLevelingEnabled: Boolean = false,
        autoLevelingTargetLufs: Float = -14.0f,
        fadeInSec: Float = 0f,
        fadeOutSec: Float = 0f,
        audioDurationSec: Double = 180.0,
        audioMetadata: com.example.core.media.AudioMetadata? = null
    ): List<String> {
        val noiseFilter = if (isNoiseReductionEnabled) {
            val nr = noiseReductionDb.toInt().coerceIn(1, 40)
            val nf = noiseFloorDb.toInt().coerceIn(-80, -10)
            "afftdn=nr=$nr:nf=$nf:tn=1,highpass=f=80,"
        } else ""

        val fadeFilters = mutableListOf<String>()
        if (fadeInSec > 0f) {
            fadeFilters.add(String.format(java.util.Locale.US, "afade=t=in:ss=0:d=%.2f", fadeInSec))
        }
        if (fadeOutSec > 0f) {
            val fadeOutStart = (audioDurationSec - fadeOutSec).coerceAtLeast(0.0)
            fadeFilters.add(String.format(java.util.Locale.US, "afade=t=out:st=%.2f:d=%.2f", fadeOutStart, fadeOutSec))
        }
        val fadeFilterStr = if (fadeFilters.isNotEmpty()) fadeFilters.joinToString(",") + "," else ""

        val effectiveTargetLufs = if (isAutoLevelingEnabled) autoLevelingTargetLufs.toDouble() else targetLufs
        
        // Use dynaudnorm for dynamic auto leveling if enabled, otherwise loudnorm
        val dynamicLevelingFilter = if (isAutoLevelingEnabled) {
            "dynaudnorm=f=150:g=15,"
        } else ""

        val loudnormFilter = "${noiseFilter}${fadeFilterStr}${dynamicLevelingFilter}loudnorm=I=$effectiveTargetLufs:LRA=11:TP=-1.5"
        val metadataArgs = mutableListOf<String>()
        audioMetadata?.let { meta ->
            if (meta.title.isNotBlank()) {
                metadataArgs.add("-metadata")
                metadataArgs.add("title=${meta.title}")
            }
            if (meta.artist.isNotBlank()) {
                metadataArgs.add("-metadata")
                metadataArgs.add("artist=${meta.artist}")
            }
            if (meta.album.isNotBlank()) {
                metadataArgs.add("-metadata")
                metadataArgs.add("album=${meta.album}")
            }
            if (meta.genre.isNotBlank()) {
                metadataArgs.add("-metadata")
                metadataArgs.add("genre=${meta.genre}")
            }
            if (meta.year.isNotBlank()) {
                metadataArgs.add("-metadata")
                metadataArgs.add("date=${meta.year}")
            }
            if (meta.comment.isNotBlank()) {
                metadataArgs.add("-metadata")
                metadataArgs.add("comment=${meta.comment}")
            }
        }

        val baseCommand = mutableListOf(
            "-i", inputPath,
            "-af", loudnormFilter,
            "-c:a", if (audioFormat.equals("wav", ignoreCase = true)) "pcm_s16le" else "libmp3lame",
            "-b:a", "320k"
        )
        baseCommand.addAll(metadataArgs)
        baseCommand.add("-y")
        baseCommand.add(outputPath)
        
        return baseCommand
    }

    fun buildLoudnormAnalysisCommand(inputPath: String, targetLufs: Double = -14.0): List<String> {
        return listOf(
            "-i", inputPath,
            "-af", "loudnorm=I=$targetLufs:LRA=11:TP=-1.5:print_format=json",
            "-f", "null", "-"
        )
    }

    fun buildLoudnormSecondPassCommand(
        inputPath: String,
        outputPath: String,
        targetLufs: Double = -14.0,
        measuredI: Double,
        measuredLra: Double,
        measuredTp: Double,
        measuredThresh: Double
    ): List<String> {
        val filter = "loudnorm=I=$targetLufs:LRA=11:TP=-1.5:measured_I=$measuredI:measured_LRA=$measuredLra:measured_TP=$measuredTp:measured_thresh=$measuredThresh:linear=true:print_format=summary"
        return listOf(
            "-i", inputPath,
            "-af", filter,
            "-c:v", "copy",
            "-c:a", "aac",
            "-b:a", "192k",
            "-y", outputPath
        )
    }

    fun buildTrimmedVideoAudioMasteringCommand(
        inputPath: String,
        outputPath: String,
        trimStartSec: Double = 0.0,
        trimEndSec: Double = 0.0,
        targetLufs: Double = -14.0,
        presetName: String = "Voice Clarity",
        presetQuality: String = "1080p",
        videoFilter: String? = null,
        resolution: String = "1080p",
        frameRate: String = "30fps",
        bitrate: String = "Medium",
        aspectRatio: String = "Asli"
    ): List<String> {
        val commands = mutableListOf<String>()

        if (trimStartSec > 0.0) {
            commands.add("-ss")
            commands.add(String.format(java.util.Locale.US, "%.3f", trimStartSec))
        }

        commands.add("-i")
        commands.add(inputPath)

        if (trimEndSec > trimStartSec) {
            val duration = trimEndSec - trimStartSec
            commands.add("-t")
            commands.add(String.format(java.util.Locale.US, "%.3f", duration))
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
        

        val finalVideoFilter = if (!videoFilter.isNullOrBlank()) {
            "$videoFilter,$scaleFilter"
        } else {
            scaleFilter
        }
        commands.add("-vf")
        commands.add(finalVideoFilter)

        val eqPresetFilter = when {
            presetName.contains("Bass", ignoreCase = true) -> "equalizer=f=80:g=6:w=1.0,"
            presetName.contains("Voice", ignoreCase = true) || presetName.contains("Vocal", ignoreCase = true) -> "equalizer=f=3000:g=4:w=1.5,highpass=f=80,"
            presetName.contains("Noise", ignoreCase = true) -> "afftdn,highpass=f=100,lowpass=f=12000,"
            else -> ""
        }
        val loudnormFilter = "${eqPresetFilter}loudnorm=I=$targetLufs:LRA=11:TP=-1.5"

        val fps = frameRate.replace("fps", "")
        if (fps.isNotEmpty()) {
            commands.addAll(listOf("-r", fps))
        }
        
        val bv = when (bitrate) {
            "Low" -> "1M"
            "Medium" -> "4M"
            "High" -> "8M"
            else -> "4M"
        }

        commands.addAll(
            listOf(
                "-af", loudnormFilter,
                "-c:v", "libx264",
                "-preset", "ultrafast",
                "-b:v", bv, "-maxrate", bv, "-bufsize", "${bv.replace("M", "")}M",
                "-c:a", "aac",
                "-b:a", "192k",
                "-y", outputPath
            )
        )
        return commands
    }

    /**
     * Builds a real FFmpeg command that renders a sequence of still images into a
     * video file (Slideshow). Each image becomes a looped input held for
     * [perImageDurationSec]; consecutive images are joined either with an `xfade`
     * transition or, when [transition] is "none", a hard-cut `concat`. An optional
     * background audio track is muxed in and trimmed to the video length.
     */
    fun buildSlideshowCommand(
        imagePaths: List<String>,
        outputPath: String,
        perImageDurationSec: Double,
        transition: String = "fade",
        transitionDurationSec: Double = 1.0,
        audioPath: String? = null,
        resolution: String = "1080p",
        aspectRatio: String = "16:9",
        frameRate: String = "30fps",
        kenBurnsEnabled: Boolean = false,
        overlayText: String = ""
    ): List<String> {
        require(imagePaths.isNotEmpty()) { "Slideshow requires at least one image" }

        val longSide = when (resolution) {
            "4K" -> 3840
            "1080p" -> 1920
            "720p" -> 1280
            "480p" -> 854
            else -> 1920
        }
        val (canvasWidth, canvasHeight) = when (aspectRatio) {
            "16:9" -> longSide to (longSide * 9 / 16)
            "9:16" -> (longSide * 9 / 16) to longSide
            "1:1" -> longSide to longSide
            "4:5" -> (longSide * 4 / 5) to longSide
            else -> longSide to (longSide * 9 / 16)
        }

        val args = mutableListOf<String>()
        val durationStr = String.format(java.util.Locale.US, "%.3f", perImageDurationSec)

        imagePaths.forEach { path ->
            args.add("-loop"); args.add("1")
            args.add("-t"); args.add(durationStr)
            args.add("-i"); args.add(path)
        }

        val hasAudio = !audioPath.isNullOrBlank()
        if (hasAudio) {
            args.add("-i")
            args.add(audioPath!!)
        }

        val fps = frameRate.replace("fps", "").ifBlank { "30" }
        val fpsValue = fps.toDoubleOrNull() ?: 30.0

        // Fit the still into the canvas first, then let Ken Burns zoom the already-padded frame so
        // the motion never changes the output aspect ratio.
        val fitFilter = "scale=w=$canvasWidth:h=$canvasHeight:force_original_aspect_ratio=decrease," +
            "pad=$canvasWidth:$canvasHeight:(ow-iw)/2:(oh-ih)/2,setsar=1"
        val scaleFilter = if (kenBurnsEnabled) {
            val frames = (perImageDurationSec * fpsValue).toInt().coerceAtLeast(1)
            "$fitFilter,zoompan=z='min(zoom+$KEN_BURNS_ZOOM_STEP,$KEN_BURNS_MAX_ZOOM)':d=$frames:" +
                "x='iw/2-(iw/zoom/2)':y='ih/2-(ih/zoom/2)':s=${canvasWidth}x$canvasHeight:fps=$fps"
        } else {
            "$fitFilter,fps=$fps"
        }

        val scaledLabels = imagePaths.indices.map { i -> "v$i" }
        val scaleChain = imagePaths.indices.joinToString(";") { i -> "[$i:v]$scaleFilter[${scaledLabels[i]}]" }

        val noTransition = imagePaths.size == 1 || transition.isBlank() || transition.equals("none", ignoreCase = true)
        val (transitionGraph, transitionOutputLabel) = if (noTransition) {
            buildConcatGraph(scaleChain, scaledLabels)
        } else {
            buildXfadeGraph(scaleChain, scaledLabels, perImageDurationSec, transition, transitionDurationSec)
        }
        val (filterGraph, finalVideoLabel) = appendTextOverlay(
            filterGraph = transitionGraph,
            inputLabel = transitionOutputLabel,
            overlayText = overlayText
        )

        args.add("-filter_complex")
        args.add(filterGraph)
        args.add("-map")
        args.add("[$finalVideoLabel]")

        if (hasAudio) {
            args.add("-map")
            args.add("${imagePaths.size}:a")
            args.add("-c:a")
            args.add("aac")
            args.add("-b:a")
            args.add("192k")
            args.add("-shortest")
        } else {
            args.add("-an")
        }

        args.addAll(listOf("-c:v", "libx264", "-preset", "ultrafast", "-pix_fmt", "yuv420p"))
        args.add("-y")
        args.add(outputPath)

        return args
    }

    private fun buildConcatGraph(scaleChain: String, scaledLabels: List<String>): Pair<String, String> {
        if (scaledLabels.size == 1) return scaleChain to scaledLabels[0]
        val concatInputs = scaledLabels.joinToString("") { "[$it]" }
        val graph = "$scaleChain;${concatInputs}concat=n=${scaledLabels.size}:v=1:a=0[outv]"
        return graph to "outv"
    }

    private fun buildXfadeGraph(
        scaleChain: String,
        scaledLabels: List<String>,
        perImageDurationSec: Double,
        transition: String,
        transitionDurationSec: Double
    ): Pair<String, String> {
        val xfadeChain = StringBuilder()
        var prevLabel = scaledLabels[0]
        var cumulativeOffset = perImageDurationSec - transitionDurationSec
        for (i in 1 until scaledLabels.size) {
            val nextLabel = scaledLabels[i]
            val outLabel = "x$i"
            val offsetStr = String.format(java.util.Locale.US, "%.3f", cumulativeOffset.coerceAtLeast(0.0))
            val durationStr = String.format(java.util.Locale.US, "%.3f", transitionDurationSec)
            xfadeChain.append(
                ";[$prevLabel][$nextLabel]xfade=transition=$transition:duration=$durationStr:offset=$offsetStr[$outLabel]"
            )
            prevLabel = outLabel
            cumulativeOffset += perImageDurationSec - transitionDurationSec
        }
        return "$scaleChain$xfadeChain" to prevLabel
    }

    private fun appendTextOverlay(
        filterGraph: String,
        inputLabel: String,
        overlayText: String
    ): Pair<String, String> {
        if (overlayText.isBlank()) return filterGraph to inputLabel

        val escapedText = escapeDrawText(overlayText)
        val overlayFilter = "drawtext=text='$escapedText':fontsize=48:fontcolor=white:" +
            "borderw=2:bordercolor=black:x=(w-text_w)/2:y=h-text_h-40"
        return "$filterGraph;[$inputLabel]$overlayFilter[textout]" to "textout"
    }

    private fun escapeDrawText(text: String): String = text
        .replace("\\", "\\\\")
        .replace("'", "\\'")
        .replace(":", "\\:")
        .replace("%", "\\%")
}
