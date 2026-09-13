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
     * Returns the captured native-initialization error message (e.g. the UnsatisfiedLinkError
     * detail) when the engine failed to load, or null when initialization succeeded. Used for
     * on-screen diagnostics.
     */
    fun getInitError(): String?

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
     * Returns the full stdout/stderr text captured during the most recent [execute] call.
     * Useful for parsing structured FFmpeg output such as loudnorm JSON measurements.
     */
    fun getLastOutputLog(): String

    /**
     * Cancels any active FFmpeg execution session.
     */
    fun cancel()

    /**
     * Releases internal resources (coroutine scopes, native handles). Call when the owning
     * component is being destroyed to avoid leaking background scopes.
     */
    fun release()
}

/**
 * FFmpeg Command Builder Utility for video looping, audio mastering, and editor processing.
 */
object FFmpegCommandBuilder {

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

    /**
     * Builds a standard loop command. The output is repeated to reach [targetDurationSec] and then
     * hard-capped with `-t`, so the exported length matches the request regardless of clip length.
     *
     * @param clipDurationSec measured duration of the (possibly trimmed) source clip. If <= 0 a
     *   conservative fallback repeat count is used.
     * @param muteAudio drops the audio track entirely (`-an`).
     * @param audioFadeInSec / audioFadeOutSec apply an audio fade at the very start/end of the loop.
     */
    fun buildNormalLoopCommand(
        inputPath: String,
        outputPath: String,
        targetDurationSec: Double,
        clipDurationSec: Double,
        resolution: String = "1080p",
        frameRate: String = "30fps",
        bitrate: String = "Medium",
        aspectRatio: String = "Asli",
        muteAudio: Boolean = false,
        audioFadeInSec: Double = 0.0,
        audioFadeOutSec: Double = 0.0,
        metadata: com.example.core.media.AudioMetadata? = null
    ): List<String> {
        // Number of EXTRA input repeats needed to cover the target. -stream_loop N plays the
        // input N+1 times total. If clip length is unknown, fall back to a safe repeat count.
        val streamLoop = if (clipDurationSec > 0.1) {
            (kotlin.math.ceil(targetDurationSec / clipDurationSec).toInt() - 1).coerceIn(0, 5000)
        } else {
            (targetDurationSec / 10).toInt().coerceAtLeast(1)
        }

        val args = mutableListOf(
            "-stream_loop", streamLoop.toString(),
            "-i", inputPath,
            "-t", String.format(java.util.Locale.US, "%.3f", targetDurationSec.coerceAtLeast(0.1)),
            "-c:v", "libx264"
        )
        args.addAll(getVideoExportArgs(resolution, frameRate, bitrate, aspectRatio))
        args.addAll(buildLoopAudioArgs(muteAudio, audioFadeInSec, audioFadeOutSec, targetDurationSec))
        args.addAll(buildMetadataArgs(metadata))
        args.addAll(listOf("-y", outputPath))
        return args
    }

    /**
     * Shared audio-encoding args for loop outputs. Emits `-an` when muted, otherwise AAC with an
     * optional afade in/out anchored to the requested [targetDurationSec].
     */
    private fun buildLoopAudioArgs(
        muteAudio: Boolean,
        audioFadeInSec: Double,
        audioFadeOutSec: Double,
        targetDurationSec: Double
    ): List<String> {
        if (muteAudio) return listOf("-an")

        val fades = mutableListOf<String>()
        if (audioFadeInSec > 0.0) {
            fades.add(String.format(java.util.Locale.US, "afade=t=in:st=0:d=%.2f", audioFadeInSec))
        }
        if (audioFadeOutSec > 0.0) {
            val start = (targetDurationSec - audioFadeOutSec).coerceAtLeast(0.0)
            fades.add(String.format(java.util.Locale.US, "afade=t=out:st=%.2f:d=%.2f", start, audioFadeOutSec))
        }
        val args = mutableListOf<String>()
        if (fades.isNotEmpty()) {
            args.add("-af")
            args.add(fades.joinToString(","))
        }
        args.addAll(listOf("-c:a", "aac", "-b:a", "192k"))
        return args
    }

    /**
     * Builds a crossfade (seamless) loop. The clip is supplied as two inputs and the tail of the
     * first copy is blended into the head of the second via `xfade` (and `acrossfade` for audio),
     * producing a seam-free loop unit. The output is capped to [targetDurationSec] with `-t`.
     *
     * The crossfade offset is derived from the REAL [clipDurationSec] (not the target), so the
     * transition triggers at the correct point regardless of the requested loop length.
     */
    fun buildCrossfadeLoopCommand(
        inputPath: String,
        outputPath: String,
        targetDurationSec: Double,
        clipDurationSec: Double,
        crossfadeDurationSec: Double = 1.0,
        resolution: String = "1080p",
        frameRate: String = "30fps",
        bitrate: String = "Medium",
        aspectRatio: String = "Asli",
        muteAudio: Boolean = false,
        metadata: com.example.core.media.AudioMetadata? = null
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

        // xfade offset = point in the (concatenated) timeline where the blend starts. For two
        // copies of a clip of length L with crossfade d, the seam is at (L - d).
        val safeClip = if (clipDurationSec > 0.1) clipDurationSec else (targetDurationSec.coerceAtLeast(2.0))
        val xfade = crossfadeDurationSec.coerceIn(0.1, (safeClip / 2).coerceAtLeast(0.1))
        val offset = (safeClip - xfade).coerceAtLeast(0.1)

        // Video: blend the two inputs, then scale/crop to the requested aspect/resolution.
        val videoFilter = "[0:v][1:v]xfade=transition=fade:duration=%.3f:offset=%.3f[vx];[vx]%s[v]"
            .format(java.util.Locale.US, xfade, offset, scaleFilter)

        val fps = frameRate.replace("fps", "")
        val bv = when (bitrate) {
            "Low" -> "1M"
            "Medium" -> "4M"
            "High" -> "8M"
            else -> "4M"
        }

        val args = mutableListOf(
            "-i", inputPath,
            "-i", inputPath
        )

        if (muteAudio) {
            args.addAll(listOf("-filter_complex", videoFilter, "-map", "[v]", "-an"))
        } else {
            // Audio: acrossfade the two copies for a click-free seam.
            val audioFilter = ";[0:a][1:a]acrossfade=d=%.3f[a]".format(java.util.Locale.US, xfade)
            args.addAll(listOf("-filter_complex", videoFilter + audioFilter, "-map", "[v]", "-map", "[a]"))
        }

        args.addAll(listOf(
            "-t", String.format(java.util.Locale.US, "%.3f", targetDurationSec.coerceAtLeast(0.1)),
            "-c:v", "libx264",
            "-preset", "ultrafast",
            "-r", fps,
            "-b:v", bv, "-maxrate", bv, "-bufsize", "${bv.replace("M", "")}M"
        ))
        if (!muteAudio) {
            args.addAll(listOf("-c:a", "aac", "-b:a", "192k"))
        }
        args.addAll(buildMetadataArgs(metadata))
        args.addAll(listOf("-y", outputPath))
        return args
    }

    /**
     * Builds a ping-pong (boomerang) loop: the clip plays forward then reversed, and that
     * forward+reverse unit is repeated to reach [targetDurationSec] (capped with `-t`).
     * Uses `reverse`/`areverse` + `concat`. Note: `reverse` buffers frames in memory, so this
     * is best for short clips.
     */
    fun buildPingPongLoopCommand(
        inputPath: String,
        outputPath: String,
        targetDurationSec: Double,
        clipDurationSec: Double,
        resolution: String = "1080p",
        frameRate: String = "30fps",
        bitrate: String = "Medium",
        aspectRatio: String = "Asli",
        muteAudio: Boolean = false,
        metadata: com.example.core.media.AudioMetadata? = null
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

        // Build forward+reverse unit. Video always; audio only when not muted.
        val filter = if (muteAudio) {
            "[0:v]split[v1][v2];[v2]reverse[vr];[v1][vr]concat=n=2:v=1:a=0[vc];[vc]$scaleFilter[v]"
        } else {
            "[0:v]split[v1][v2];[v2]reverse[vr];[v1][vr]concat=n=2:v=1:a=0[vc];[vc]$scaleFilter[v];" +
                "[0:a]asplit[a1][a2];[a2]areverse[ar];[a1][ar]concat=n=2:v=0:a=1[a]"
        }

        val fps = frameRate.replace("fps", "")
        val bv = when (bitrate) {
            "Low" -> "1M"
            "Medium" -> "4M"
            "High" -> "8M"
            else -> "4M"
        }

        // Produce a single forward+reverse boomerang unit (length ~ 2*clip). `reverse` buffers
        // the whole clip in memory, so we intentionally do NOT stream_loop here (that would be
        // memory-explosive). `-t` caps the output if the target is shorter than one unit.
        val args = mutableListOf(
            "-i", inputPath,
            "-filter_complex", filter,
            "-map", "[v]"
        )
        if (muteAudio) {
            args.add("-an")
        } else {
            args.addAll(listOf("-map", "[a]"))
        }
        args.addAll(listOf(
            "-t", String.format(java.util.Locale.US, "%.3f", targetDurationSec.coerceAtLeast(0.1)),
            "-c:v", "libx264",
            "-preset", "ultrafast",
            "-r", fps,
            "-b:v", bv, "-maxrate", bv, "-bufsize", "${bv.replace("M", "")}M"
        ))
        if (!muteAudio) {
            args.addAll(listOf("-c:a", "aac", "-b:a", "192k"))
        }
        args.addAll(buildMetadataArgs(metadata))
        args.addAll(listOf("-y", outputPath))
        return args
    }

    /**
     * Builds the full audio-mastering filter chain and encode args. Applies (in order):
     * input gain → noise reduction → 5-band EQ → compressor → fades → loudness normalization
     * → output gain, then encodes with the codec matching [audioFormat] and writes metadata.
     *
     * @param audioDurationSec REAL measured duration of the source, used to place the fade-out
     *   correctly (never assume a fixed length).
     */
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
        audioDurationSec: Double = 0.0,
        audioMetadata: com.example.core.media.AudioMetadata? = null,
        eqConfig: com.example.core.media.EqBandConfig? = null,
        compressorConfig: com.example.core.media.CompressorConfig? = null,
        inputGainDb: Float = 0f,
        outputGainDb: Float = 0f
    ): List<String> {
        // Ordered filter chain. Each entry is a single FFmpeg -af term.
        val chain = mutableListOf<String>()

        // 1. Input gain (pre-processing trim/boost).
        if (kotlin.math.abs(inputGainDb) > 0.01f) {
            chain.add(String.format(java.util.Locale.US, "volume=%.2fdB", inputGainDb))
        }

        // 2. Noise reduction (FFT denoise + low-cut).
        if (isNoiseReductionEnabled) {
            val nr = noiseReductionDb.toInt().coerceIn(1, 40)
            val nf = noiseFloorDb.toInt().coerceIn(-80, -10)
            chain.add("afftdn=nr=$nr:nf=$nf:tn=1")
            chain.add("highpass=f=80")
        }

        // 3. Five-band EQ. Maps each band gain to a peaking `equalizer` term at its centre freq.
        eqConfig?.let { eq ->
            fun band(freq: Int, width: Int, gain: Float) {
                if (kotlin.math.abs(gain) > 0.01f) {
                    chain.add(String.format(java.util.Locale.US, "equalizer=f=%d:t=h:w=%d:g=%.2f", freq, width, gain))
                }
            }
            band(60, 50, eq.lowGainDb)
            band(250, 150, eq.midLowGainDb)
            band(1000, 400, eq.midGainDb)
            band(4000, 1500, eq.midHighGainDb)
            band(12000, 3000, eq.highGainDb)
        }

        // 4. Compressor (dynamics). acompressor gains are linear ratios; makeup applied via gain.
        compressorConfig?.let { comp ->
            val threshold = comp.thresholdDb.coerceIn(-60f, 0f)
            val ratio = comp.ratio.coerceIn(1f, 20f)
            val attack = comp.attackMs.coerceIn(0.01f, 2000f)
            val release = comp.releaseMs.coerceIn(0.01f, 9000f)
            val makeup = comp.makeupGainDb.coerceIn(0f, 24f)
            chain.add(
                String.format(
                    java.util.Locale.US,
                    "acompressor=threshold=%.4f:ratio=%.2f:attack=%.2f:release=%.2f:makeup=%.2f",
                    dbToLinear(threshold), ratio, attack, release, dbToLinear(makeup)
                )
            )
        }

        // 5. Fades (fade-out anchored to the REAL duration when known).
        if (fadeInSec > 0f) {
            chain.add(String.format(java.util.Locale.US, "afade=t=in:st=0:d=%.2f", fadeInSec))
        }
        if (fadeOutSec > 0f && audioDurationSec > 0.0) {
            val fadeOutStart = (audioDurationSec - fadeOutSec).coerceAtLeast(0.0)
            chain.add(String.format(java.util.Locale.US, "afade=t=out:st=%.2f:d=%.2f", fadeOutStart, fadeOutSec))
        }

        // 6. Loudness normalization. dynaudnorm (dynamic) when auto-level on, else single loudnorm.
        val effectiveTargetLufs = if (isAutoLevelingEnabled) autoLevelingTargetLufs.toDouble() else targetLufs
        if (isAutoLevelingEnabled) {
            chain.add("dynaudnorm=f=150:g=15")
        }
        chain.add("loudnorm=I=$effectiveTargetLufs:LRA=11:TP=-1.5")

        // 7. Output gain (final trim after normalization).
        if (kotlin.math.abs(outputGainDb) > 0.01f) {
            chain.add(String.format(java.util.Locale.US, "volume=%.2fdB", outputGainDb))
        }

        val filterChain = chain.joinToString(",")

        val metadataArgs = buildMetadataArgs(audioMetadata)

        // Codec + bitrate per requested container. WAV -> PCM (no bitrate), M4A -> AAC, else MP3.
        val (codec, bitrateArgs) = when (audioFormat.lowercase()) {
            "wav" -> "pcm_s16le" to emptyList()
            "m4a", "aac", "mp4" -> "aac" to listOf("-b:a", "256k")
            else -> "libmp3lame" to listOf("-b:a", "320k")
        }

        val baseCommand = mutableListOf(
            "-i", inputPath,
            "-af", filterChain,
            "-c:a", codec
        )
        baseCommand.addAll(bitrateArgs)
        baseCommand.addAll(metadataArgs)
        baseCommand.add("-y")
        baseCommand.add(outputPath)

        return baseCommand
    }

    /** Converts a dB value to a linear amplitude ratio (for acompressor threshold/makeup). */
    private fun dbToLinear(db: Float): Double = Math.pow(10.0, db / 20.0)

    /**
     * Builds `-metadata key=value` args from [meta], escaping problematic characters.
     * Works for both audio (MP3/WAV/M4A -> ID3/APE/MP4 tags) and video (MP4/MKV/MOV container
     * tags) outputs; FFmpeg maps the same `-metadata` keys to the right tag format per container.
     */
    fun buildMetadataArgs(meta: com.example.core.media.AudioMetadata?): List<String> {
        if (meta == null) return emptyList()
        val args = mutableListOf<String>()
        fun add(key: String, value: String) {
            if (value.isNotBlank()) {
                // Neutralize characters that could break the key=value token.
                val safe = value.replace("\n", " ").replace("\r", " ")
                args.add("-metadata")
                args.add("$key=$safe")
            }
        }
        add("title", meta.title)
        add("artist", meta.artist)
        add("album", meta.album)
        add("genre", meta.genre)
        add("date", meta.year)
        add("comment", meta.comment)
        add("track", meta.trackNumber)
        add("album_artist", meta.albumArtist)
        return args
    }

    /**
     * Builds a fast, lossless metadata-only edit command: remuxes [inputPath] into [outputPath]
     * with `-c copy` (no re-encoding, so quality/duration are untouched) while replacing the
     * container's metadata tags with [meta]. Works for both audio (MP3/WAV/M4A) and video
     * (MP4/MKV/MOV) files, letting existing renders/exports have their title/artist/album/genre/
     * year/comment tags added or changed after the fact.
     *
     * `-map_metadata -1` first clears any pre-existing tags so stale values from the source don't
     * linger alongside the new ones.
     */
    fun buildMetadataEditCommand(
        inputPath: String,
        outputPath: String,
        meta: com.example.core.media.AudioMetadata
    ): List<String> {
        val args = mutableListOf(
            "-i", inputPath,
            "-map_metadata", "-1",
            "-c", "copy"
        )
        args.addAll(buildMetadataArgs(meta))
        args.addAll(listOf("-y", outputPath))
        return args
    }

    /**
     * Builds a lossless remux command that embeds [coverImagePath] (a JPG/PNG) as the cover/album
     * art of [inputPath], writing the result to [outputPath]. The exact syntax differs by
     * container, per FFmpeg's own conventions:
     * - MP3 (ID3 APIC frame): `-id3v2_version 3` plus `title`/`comment` stream tags, which ID3
     *   readers map to the APIC description/picture-type fields. No `-disposition` flag exists
     *   for MP3.
     * - MP4/M4A/MOV (attached picture stream): the image is added as an extra video stream with
     *   `-disposition:v:<n> attached_pic`, where `<n>` is that stream's 0-based index among the
     *   OUTPUT's video streams -- 0 if [inputHasVideoStream] is false (pure audio, e.g. Mastering's
     *   M4A), 1 if true (Loop/Editor's video already occupies v:0).
     *
     * WAV and MKV are intentionally not handled here: WAV has no attached-picture convention, and
     * MKV cover art uses a different mechanism (`-attach`, a real file attachment rather than a
     * stream) that callers should not assume behaves the same way.
     *
     * @param isMp3Container true for an MP3 output, false for MP4/M4A/MOV.
     * @param inputHasVideoStream true when [inputPath] already carries a video track (Loop/Editor
     *   renders); false for pure-audio inputs (Mastering's M4A/MP3 output).
     */
    fun buildCoverArtCommand(
        inputPath: String,
        coverImagePath: String,
        outputPath: String,
        isMp3Container: Boolean,
        inputHasVideoStream: Boolean
    ): List<String> {
        val args = mutableListOf(
            "-i", inputPath,
            "-i", coverImagePath,
            "-map", "0",
            "-map", "1",
            "-c", "copy"
        )
        if (isMp3Container) {
            args.addAll(
                listOf(
                    "-id3v2_version", "3",
                    "-metadata:s:v", "title=Album cover",
                    "-metadata:s:v", "comment=Cover (front)"
                )
            )
        } else {
            val newImageStreamIndex = if (inputHasVideoStream) 1 else 0
            args.addAll(listOf("-disposition:v:$newImageStreamIndex", "attached_pic"))
        }
        args.addAll(listOf("-y", outputPath))
        return args
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
        videoFilter: String? = null,
        resolution: String = "1080p",
        frameRate: String = "30fps",
        bitrate: String = "Medium",
        aspectRatio: String = "Asli",
        metadata: com.example.core.media.AudioMetadata? = null
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

        // Each of the 5 presets exposed by TrimmedVideoAudioMasteringCard resolves to a distinct
        // EQ chain here. Previously "Music Master", "Podcast Clean", and "Balanced Loudness" fell
        // through to the empty `else` branch (no keyword matched "Bass"/"Voice"/"Vocal"/"Noise"),
        // so 3 of the 5 preset names had zero audible difference from each other - just plain
        // loudnorm. Order matters: more specific keywords are checked before the generic ones.
        val eqPresetFilter = when {
            presetName.contains("Bass", ignoreCase = true) -> "equalizer=f=80:g=6:w=1.0,"
            presetName.contains("Voice", ignoreCase = true) || presetName.contains("Vocal", ignoreCase = true) -> "equalizer=f=3000:g=4:w=1.5,highpass=f=80,"
            presetName.contains("Podcast", ignoreCase = true) ->
                // Podcast Clean: cut boomy low-mids, add speech presence, tame harsh sibilance.
                "highpass=f=90,equalizer=f=250:g=-3:w=1.0,equalizer=f=4000:g=3:w=1.5,equalizer=f=8000:g=-2:w=1.0,"
            presetName.contains("Music", ignoreCase = true) ->
                // Music Master: gentle "smile curve" - a touch of bass and treble lift for a fuller mix.
                "equalizer=f=60:g=3.5:w=1.0,equalizer=f=10000:g=3:w=1.0,"
            presetName.contains("Balanced", ignoreCase = true) || presetName.contains("Loudness", ignoreCase = true) ->
                // Balanced Loudness: minimal tonal shaping, just enough to correct thin/dull sources.
                "equalizer=f=100:g=1:w=1.0,equalizer=f=6000:g=1:w=1.0,"
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
                "-b:a", "192k"
            )
        )
        commands.addAll(buildMetadataArgs(metadata))
        commands.addAll(listOf("-y", outputPath))
        return commands
    }
}
