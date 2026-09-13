package com.example.core.media

import android.content.Context
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.nio.ByteBuffer
import kotlin.math.log10
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

data class AudioAnalysisData(
    val waveformPoints: List<Float>, // Normalized 0.0 to 1.0
    val beatMarkersMs: List<Long>,
    val durationMs: Long,
    val peakLufs: Double,
    val currentRmsLufs: Double
)

object WaveformAnalyzer {

    /**
     * Decodes the given audio/video track and produces a real amplitude envelope, duration,
     * and rough loudness estimate. Falls back to [generateSimulatedWaveform] if decoding fails
     * (unsupported codec, protected content, missing track, etc.) so callers always get usable data.
     *
     * This runs on [Dispatchers.IO] as it performs blocking codec work.
     */
    suspend fun analyzeAudio(
        context: Context,
        uri: String,
        pointCount: Int = 100
    ): AudioAnalysisData = withContext(Dispatchers.IO) {
        try {
            decodeAmplitudeEnvelope(context, uri, pointCount)
        } catch (t: Throwable) {
            // Any failure (codec, IO, DRM) degrades gracefully to the synthetic waveform.
            generateSimulatedWaveform(pointCount = pointCount)
        }
    }

    private fun decodeAmplitudeEnvelope(
        context: Context,
        uri: String,
        pointCount: Int
    ): AudioAnalysisData {
        val extractor = MediaExtractor()
        var codec: MediaCodec? = null
        try {
            when {
                uri.startsWith("content://") || uri.startsWith("file://") ->
                    extractor.setDataSource(context, Uri.parse(uri), null)
                else -> extractor.setDataSource(uri)
            }

            var audioTrackIndex = -1
            var format: MediaFormat? = null
            for (i in 0 until extractor.trackCount) {
                val f = extractor.getTrackFormat(i)
                val mime = f.getString(MediaFormat.KEY_MIME) ?: continue
                if (mime.startsWith("audio/")) {
                    audioTrackIndex = i
                    format = f
                    break
                }
            }
            if (audioTrackIndex < 0 || format == null) {
                return generateSimulatedWaveform(pointCount = pointCount)
            }

            extractor.selectTrack(audioTrackIndex)
            val durationUs = if (format.containsKey(MediaFormat.KEY_DURATION)) {
                format.getLong(MediaFormat.KEY_DURATION)
            } else 0L
            val durationMs = (durationUs / 1000L).coerceAtLeast(1L)
            val mime = format.getString(MediaFormat.KEY_MIME)!!

            codec = MediaCodec.createDecoderByType(mime)
            codec.configure(format, null, null, 0)
            codec.start()

            // Accumulate RMS amplitude into evenly spaced buckets across the timeline.
            val buckets = FloatArray(pointCount)
            val bucketSamples = IntArray(pointCount)
            val bufferInfo = MediaCodec.BufferInfo()
            var sawInputEof = false
            var sawOutputEof = false
            var globalPeak = 0f

            while (!sawOutputEof) {
                if (!sawInputEof) {
                    val inIndex = codec.dequeueInputBuffer(10_000)
                    if (inIndex >= 0) {
                        val inBuf = codec.getInputBuffer(inIndex)
                        val sampleSize = if (inBuf != null) extractor.readSampleData(inBuf, 0) else -1
                        if (sampleSize < 0) {
                            codec.queueInputBuffer(inIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                            sawInputEof = true
                        } else {
                            val presentationUs = extractor.sampleTime
                            codec.queueInputBuffer(inIndex, 0, sampleSize, presentationUs, 0)
                            extractor.advance()
                        }
                    }
                }

                val outIndex = codec.dequeueOutputBuffer(bufferInfo, 10_000)
                if (outIndex >= 0) {
                    val outBuf: ByteBuffer? = codec.getOutputBuffer(outIndex)
                    if (outBuf != null && bufferInfo.size > 0) {
                        val bucketIndex = if (durationUs > 0) {
                            ((bufferInfo.presentationTimeUs.toDouble() / durationUs) * pointCount)
                                .toInt().coerceIn(0, pointCount - 1)
                        } else 0

                        outBuf.position(bufferInfo.offset)
                        val limit = bufferInfo.offset + bufferInfo.size
                        var sumSquares = 0.0
                        var count = 0
                        // Interpret as 16-bit PCM little-endian.
                        while (outBuf.position() + 1 < limit) {
                            val low = outBuf.get().toInt() and 0xFF
                            val high = outBuf.get().toInt()
                            val sample = (high shl 8) or low
                            val norm = sample / 32768f
                            sumSquares += (norm * norm).toDouble()
                            count++
                        }
                        if (count > 0) {
                            val rms = sqrt(sumSquares / count).toFloat()
                            buckets[bucketIndex] += rms
                            bucketSamples[bucketIndex] += 1
                            if (rms > globalPeak) globalPeak = rms
                        }
                    }
                    codec.releaseOutputBuffer(outIndex, false)
                    if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
                        sawOutputEof = true
                    }
                }
            }

            val points = ArrayList<Float>(pointCount)
            var lastValue = 0.05f
            var totalRms = 0.0
            var nonEmpty = 0
            for (i in 0 until pointCount) {
                val v = if (bucketSamples[i] > 0) (buckets[i] / bucketSamples[i]) else lastValue
                if (bucketSamples[i] > 0) {
                    totalRms += v
                    nonEmpty++
                    lastValue = v
                }
                points.add(v.coerceIn(0.02f, 1.0f))
            }

            // Rough loudness estimate: convert average RMS to dBFS (not true LUFS, but real-signal based).
            val avgRms = if (nonEmpty > 0) (totalRms / nonEmpty) else 0.0
            val rmsDb = if (avgRms > 0) 20.0 * log10(avgRms) else -60.0
            val peakDb = if (globalPeak > 0) 20.0 * log10(globalPeak.toDouble()) else -40.0

            val beatMarkers = estimateBeatMarkers(points, durationMs)

            return AudioAnalysisData(
                waveformPoints = points,
                beatMarkersMs = beatMarkers,
                durationMs = durationMs,
                peakLufs = peakDb,
                currentRmsLufs = rmsDb
            )
        } finally {
            try { codec?.stop() } catch (_: Throwable) {}
            try { codec?.release() } catch (_: Throwable) {}
            try { extractor.release() } catch (_: Throwable) {}
        }
    }

    /**
     * Estimates beat positions from the amplitude envelope by detecting local energy peaks.
     * This is a lightweight heuristic, not a full onset-detection algorithm.
     */
    private fun estimateBeatMarkers(points: List<Float>, durationMs: Long): List<Long> {
        if (points.isEmpty() || durationMs <= 0) return emptyList()
        val markers = ArrayList<Long>()
        val avg = points.average().toFloat()
        val threshold = (avg * 1.3f).coerceAtLeast(0.15f)
        val msPerPoint = durationMs.toDouble() / points.size
        for (i in 1 until points.size - 1) {
            if (points[i] > threshold && points[i] >= points[i - 1] && points[i] >= points[i + 1]) {
                markers.add((i * msPerPoint).toLong())
            }
        }
        // Fallback to a steady 120 BPM grid if detection yields too few beats.
        if (markers.size < 4) {
            val beatIntervalMs = (60000f / 120f).toLong()
            var currentMs = 0L
            markers.clear()
            while (currentMs < durationMs) {
                markers.add(currentMs)
                currentMs += beatIntervalMs
            }
        }
        return markers
    }

    fun generateSimulatedWaveform(
        durationMs: Long = 180000L,
        pointCount: Int = 100,
        seed: Long = 42L
    ): AudioAnalysisData {
        val random = Random(seed)
        val points = ArrayList<Float>(pointCount)
        val beatMarkers = ArrayList<Long>()

        val bpm = 120
        val beatIntervalMs = (60000f / bpm).toLong()

        var currentMs = 0L
        while (currentMs < durationMs) {
            beatMarkers.add(currentMs)
            currentMs += beatIntervalMs
        }

        for (i in 0 until pointCount) {
            val progress = i.toFloat() / pointCount
            val envelope = sin(progress * Math.PI).toFloat().coerceIn(0.2f, 1.0f)
            val noise = random.nextFloat() * 0.4f
            val base = sin(i * 0.15f) * 0.3f + 0.5f
            val valNormalized = ((base + noise) * envelope).coerceIn(0.05f, 0.98f)
            points.add(valNormalized)
        }

        val calculatedLufs = -23.5 + (random.nextDouble() * 6.0)

        return AudioAnalysisData(
            waveformPoints = points,
            beatMarkersMs = beatMarkers,
            durationMs = durationMs,
            peakLufs = calculatedLufs + 4.2,
            currentRmsLufs = calculatedLufs
        )
    }
}
