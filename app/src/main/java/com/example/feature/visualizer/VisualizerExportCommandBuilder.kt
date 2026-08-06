package com.example.feature.visualizer

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import java.util.Locale

/**
 * Builds the FFmpeg command that renders a visualizer video.
 *
 * Both this builder and the on-screen Compose preview read the same
 * [VisualizerRenderConfig], so mode, colours, band count, ratio, opacity and
 * rotation stay in sync between what the user sees and what is exported.
 *
 * Mode mapping (Compose renderer -> FFmpeg source filter):
 * - BARS / MIRROR_BARS -> `showfreqs` in bar mode, resampled to the configured
 *   band count so the exported bar count matches the preview.
 * - WAVE / LINE        -> `showwaves`
 * - CIRCLE / PARTICLES -> `showcqt` (radial spectrum)
 *
 * Beat effects are applied through timeline-enabled `eq` pulses at the detected
 * marker positions. This is a brightness/contrast pulse rather than a
 * geometric zoom: per-frame geometry changes require frame-by-frame rendering,
 * which this single-pass command deliberately does not attempt.
 */
object VisualizerExportCommandBuilder {

    private const val FRAME_RATE = 30
    private const val BEAT_PULSE_SECONDS = 0.12

    fun build(
        audioPath: String,
        outputPath: String,
        config: VisualizerRenderConfig,
        beatMarkersMs: List<Long> = emptyList(),
        beatEffectExpression: String? = null
    ): List<String> {
        require(audioPath.isNotBlank()) { "Audio sumber wajib dipilih untuk ekspor visualizer" }

        val canvas = canvasSize(config.aspectRatio)
        val background = config.background
        val args = mutableListOf("-i", audioPath)
        if (background is VisualizerBackground.Image) {
            args.add("-i")
            args.add(background.uri)
        }

        args.add("-filter_complex")
        args.add(buildFilterGraph(config, canvas, beatMarkersMs, beatEffectExpression))
        args.addAll(listOf("-map", "[outv]", "-map", "0:a"))
        args.addAll(listOf("-c:v", "libx264", "-preset", "ultrafast", "-pix_fmt", "yuv420p"))
        args.addAll(listOf("-c:a", "aac", "-b:a", "192k", "-shortest"))
        args.add("-y")
        args.add(outputPath)
        return args
    }

    private data class Canvas(val width: Int, val height: Int) {
        val size: String get() = "${width}x$height"
    }

    private fun canvasSize(aspectRatio: String): Canvas = when (aspectRatio) {
        "9:16" -> Canvas(1080, 1920)
        "1:1" -> Canvas(1080, 1080)
        "4:5" -> Canvas(864, 1080)
        else -> Canvas(1920, 1080)
    }

    private fun buildFilterGraph(
        config: VisualizerRenderConfig,
        canvas: Canvas,
        beatMarkersMs: List<Long>,
        beatEffectExpression: String?
    ): String {
        val parts = mutableListOf<String>()
        parts.add(backgroundChain(config.background, canvas))
        parts.add(visualizerChain(config, canvas, beatMarkersMs, beatEffectExpression))

        val overlay = StringBuilder("[bg][vis]overlay=(W-w)/2:(H-h)/2")
        if (config.rotationDegrees != 0f) {
            val radians = String.format(Locale.US, "%.4f", Math.toRadians(config.rotationDegrees.toDouble()))
            overlay.append(",rotate=$radians:c=none")
        }
        overlay.append(",format=yuv420p[outv]")
        parts.add(overlay.toString())

        return parts.joinToString(";")
    }

    private fun backgroundChain(background: VisualizerBackground, canvas: Canvas): String =
        when (background) {
            is VisualizerBackground.SolidColor ->
                "color=c=${background.color.toFfmpegHex()}:s=${canvas.size}:r=$FRAME_RATE[bg]"

            is VisualizerBackground.Gradient ->
                "gradients=s=${canvas.size}:r=$FRAME_RATE" +
                    ":c0=${background.topColor.toFfmpegHex()}" +
                    ":c1=${background.bottomColor.toFfmpegHex()}[bg]"

            is VisualizerBackground.Image -> {
                val scale = "scale=${canvas.width}:${canvas.height}:force_original_aspect_ratio=increase" +
                    ",crop=${canvas.width}:${canvas.height}"
                val blur = if (background.blurRadius > 0f) {
                    ",gblur=sigma=${String.format(Locale.US, "%.1f", background.blurRadius)}"
                } else ""
                "[1:v]$scale$blur,fps=$FRAME_RATE[bg]"
            }

            VisualizerBackground.Transparent ->
                "color=c=black@0.0:s=${canvas.size}:r=$FRAME_RATE[bg]"
        }

    private fun visualizerChain(
        config: VisualizerRenderConfig,
        canvas: Canvas,
        beatMarkersMs: List<Long>,
        beatEffectExpression: String?
    ): String {
        val chain = StringBuilder("[0:a]")
        chain.append(sourceFilter(config, canvas))

        if (config.mode == VisualizerMode.BARS || config.mode == VisualizerMode.MIRROR_BARS) {
            // Collapse to exactly bandCount columns, then stretch back with nearest
            // neighbour so the exported bar count matches the preview's bandCount.
            chain.append(",scale=${config.bandCount}:${canvas.height}:flags=neighbor")
            chain.append(",scale=${canvas.width}:${canvas.height}:flags=neighbor")
        }

        if (config.mode == VisualizerMode.MIRROR_BARS) {
            chain.append(",split[top][bottom];[bottom]vflip[flipped];[top][flipped]vstack")
            chain.append(",scale=${canvas.width}:${canvas.height}")
        }

        beatPulseFilter(beatMarkersMs, beatEffectExpression)?.let { chain.append(",$it") }

        if (config.opacity < 1f) {
            val alpha = String.format(Locale.US, "%.2f", config.opacity.coerceIn(0f, 1f))
            chain.append(",format=rgba,colorchannelmixer=aa=$alpha")
        }

        chain.append("[vis]")
        return chain.toString()
    }

    private fun sourceFilter(config: VisualizerRenderConfig, canvas: Canvas): String {
        val colors = "${config.primaryColor.toFfmpegHex()}|${config.secondaryColor.toFfmpegHex()}"
        return when (config.mode) {
            VisualizerMode.BARS, VisualizerMode.MIRROR_BARS ->
                "showfreqs=mode=bar:ascale=log:fscale=log:win_size=1024" +
                    ":colors=$colors:s=${canvas.size}"

            VisualizerMode.WAVE, VisualizerMode.LINE -> {
                val mode = if (config.mode == VisualizerMode.WAVE) "cline" else "line"
                "showwaves=mode=$mode:rate=$FRAME_RATE:colors=$colors:s=${canvas.size}"
            }

            VisualizerMode.CIRCLE, VisualizerMode.PARTICLES -> {
                val bar = if (config.mode == VisualizerMode.PARTICLES) 0.4 else 0.9
                "showcqt=fps=$FRAME_RATE:size=${canvas.size}" +
                    ":bar_g=${String.format(Locale.US, "%.1f", bar)}:count=${config.bandCount}"
            }
        }
    }

    /**
     * Emits a timeline-enabled brightness/contrast pulse at each beat marker.
     * Returns null when there is nothing to sync to.
     */
    private fun beatPulseFilter(beatMarkersMs: List<Long>, effectExpression: String?): String? {
        if (beatMarkersMs.isEmpty() || effectExpression.isNullOrBlank()) return null

        val windows = beatMarkersMs.joinToString("+") { markerMs ->
            val start = markerMs / 1000.0
            val end = start + BEAT_PULSE_SECONDS
            "between(t,${format(start)},${format(end)})"
        }
        return "eq=brightness=0.10:contrast=1.15:enable='$windows'"
    }

    private fun format(seconds: Double): String = String.format(Locale.US, "%.3f", seconds)

    /** Converts a Compose colour to the `0xRRGGBB` literal FFmpeg expects. */
    private fun Color.toFfmpegHex(): String {
        val rgb = toArgb() and 0xFFFFFF
        return "0x%06X".format(rgb)
    }
}
