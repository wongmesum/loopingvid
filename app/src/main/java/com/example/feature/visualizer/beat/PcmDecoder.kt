package com.example.feature.visualizer.beat

import android.content.Context
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.nio.ByteBuffer
import java.nio.ByteOrder

/** Decoded mono PCM plus the metadata beat detection needs. */
data class DecodedPcm(
    val samples: FloatArray,
    val sampleRate: Int,
    val durationMs: Long
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is DecodedPcm) return false
        return sampleRate == other.sampleRate &&
            durationMs == other.durationMs &&
            samples.contentEquals(other.samples)
    }

    override fun hashCode(): Int {
        var result = samples.contentHashCode()
        result = 31 * result + sampleRate
        result = 31 * result + durationMs.hashCode()
        return result
    }
}

/**
 * Decodes an audio track to normalised mono PCM floats using MediaExtractor +
 * MediaCodec. Beat detection needs the whole waveform up front, which the
 * real-time Media3 processor cannot provide, so this runs once per source.
 *
 * Samples are downmixed to mono and decimated to [TARGET_SAMPLE_RATE] to keep
 * memory bounded on long tracks — onset detection does not need full fidelity.
 */
object PcmDecoder {

    const val TARGET_SAMPLE_RATE = 22050
    private const val DEQUEUE_TIMEOUT_US = 10_000L
    private const val MAX_DURATION_MS = 10 * 60 * 1000L // Guard against pathological inputs

    suspend fun decode(context: Context, uri: String): Result<DecodedPcm> =
        withContext(Dispatchers.IO) {
            val extractor = MediaExtractor()
            var codec: MediaCodec? = null
            try {
                extractor.setDataSource(context, Uri.parse(uri), null)
                val trackIndex = findAudioTrack(extractor)
                    ?: return@withContext Result.failure(
                        IllegalArgumentException("Tidak ada trek audio pada berkas ini")
                    )

                extractor.selectTrack(trackIndex)
                val format = extractor.getTrackFormat(trackIndex)
                val mime = format.getString(MediaFormat.KEY_MIME)
                    ?: return@withContext Result.failure(IllegalStateException("Format audio tidak dikenali"))

                val sourceSampleRate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE)
                val channelCount = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT).coerceAtLeast(1)
                val durationMs = runCatching { format.getLong(MediaFormat.KEY_DURATION) / 1000 }
                    .getOrDefault(0L)
                    .coerceAtMost(MAX_DURATION_MS)

                codec = MediaCodec.createDecoderByType(mime).apply {
                    configure(format, null, null, 0)
                    start()
                }

                val samples = drainDecoder(extractor, codec, channelCount, sourceSampleRate)
                Result.success(
                    DecodedPcm(
                        samples = samples,
                        sampleRate = TARGET_SAMPLE_RATE,
                        durationMs = if (durationMs > 0) durationMs else estimateDurationMs(samples.size)
                    )
                )
            } catch (error: Exception) {
                Result.failure(error)
            } finally {
                runCatching { codec?.stop() }
                runCatching { codec?.release() }
                runCatching { extractor.release() }
            }
        }

    private fun findAudioTrack(extractor: MediaExtractor): Int? {
        for (index in 0 until extractor.trackCount) {
            val mime = extractor.getTrackFormat(index).getString(MediaFormat.KEY_MIME)
            if (mime?.startsWith("audio/") == true) return index
        }
        return null
    }

    private fun drainDecoder(
        extractor: MediaExtractor,
        codec: MediaCodec,
        channelCount: Int,
        sourceSampleRate: Int
    ): FloatArray {
        val output = ArrayList<Float>(INITIAL_CAPACITY)
        val bufferInfo = MediaCodec.BufferInfo()
        // Keep 1 of every `stride` mono frames so the result lands near TARGET_SAMPLE_RATE.
        val stride = (sourceSampleRate.toDouble() / TARGET_SAMPLE_RATE).toInt().coerceAtLeast(1)
        var monoFrameIndex = 0
        var inputDone = false
        var outputDone = false

        while (!outputDone) {
            if (!inputDone) {
                inputDone = feedInput(extractor, codec)
            }

            when (val outputIndex = codec.dequeueOutputBuffer(bufferInfo, DEQUEUE_TIMEOUT_US)) {
                MediaCodec.INFO_TRY_AGAIN_LATER, MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> Unit
                else -> {
                    if (outputIndex >= 0) {
                        val buffer = codec.getOutputBuffer(outputIndex)
                        if (buffer != null && bufferInfo.size > 0) {
                            monoFrameIndex = appendSamples(
                                buffer = buffer,
                                info = bufferInfo,
                                channelCount = channelCount,
                                stride = stride,
                                startFrameIndex = monoFrameIndex,
                                target = output
                            )
                        }
                        codec.releaseOutputBuffer(outputIndex, false)
                    }
                    if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
                        outputDone = true
                    }
                }
            }
        }

        return FloatArray(output.size) { output[it] }
    }

    /** Returns true once the end of the source stream has been signalled. */
    private fun feedInput(extractor: MediaExtractor, codec: MediaCodec): Boolean {
        val inputIndex = codec.dequeueInputBuffer(DEQUEUE_TIMEOUT_US)
        if (inputIndex < 0) return false

        val inputBuffer = codec.getInputBuffer(inputIndex) ?: return false
        val sampleSize = extractor.readSampleData(inputBuffer, 0)

        return if (sampleSize < 0) {
            codec.queueInputBuffer(inputIndex, 0, 0, 0L, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
            true
        } else {
            codec.queueInputBuffer(inputIndex, 0, sampleSize, extractor.sampleTime, 0)
            extractor.advance()
            false
        }
    }

    /** Downmixes to mono, normalises to -1..1, and decimates by [stride]. */
    private fun appendSamples(
        buffer: ByteBuffer,
        info: MediaCodec.BufferInfo,
        channelCount: Int,
        stride: Int,
        startFrameIndex: Int,
        target: MutableList<Float>
    ): Int {
        val shorts = buffer.order(ByteOrder.nativeOrder()).asShortBuffer()
        val totalShorts = info.size / 2
        var frameIndex = startFrameIndex
        var offset = 0

        while (offset + channelCount <= totalShorts) {
            var sum = 0f
            for (channel in 0 until channelCount) {
                sum += shorts.get(offset + channel) / 32768f
            }
            if (frameIndex % stride == 0) {
                target.add(sum / channelCount)
            }
            frameIndex++
            offset += channelCount
        }
        return frameIndex
    }

    private fun estimateDurationMs(sampleCount: Int): Long =
        (sampleCount.toLong() * 1000L) / TARGET_SAMPLE_RATE

    private const val INITIAL_CAPACITY = 1 shl 16
}
