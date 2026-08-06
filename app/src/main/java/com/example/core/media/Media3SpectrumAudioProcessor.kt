package com.example.core.media

import androidx.annotation.OptIn
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.BaseAudioProcessor
import androidx.media3.common.util.UnstableApi
import java.nio.ByteBuffer
import kotlin.math.abs
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/**
 * Real-time Media3 ExoPlayer AudioProcessor that extracts audio frequency spectrum bands (FFT)
 * and audio energy levels from PCM 16-bit audio stream buffers.
 */
@OptIn(UnstableApi::class)
class Media3SpectrumAudioProcessor : BaseAudioProcessor() {

    var bandCount: Int = 32
        set(value) {
            field = value.coerceIn(8, 64)
            currentMagnitudes = FloatArray(field)
        }

    var sensitivityGain: Float = 1.0f
        set(value) {
            field = value.coerceIn(0.2f, 4.0f)
        }

    /**
     * Weight given to the previous frame when blending new magnitudes.
     * 0f reacts instantly, higher values damp the movement. The default matches
     * the blend this processor shipped with before the value was configurable.
     */
    var smoothing: Float = 0.4f
        set(value) {
            field = value.coerceIn(0f, 0.95f)
        }

    private var currentMagnitudes = FloatArray(32)
    var peakDb: Float = -60f
        private set
    var rmsEnergy: Float = 0f
        private set
    var dominantFrequencyHz: Int = 440
        private set

    var onSpectrumDataListener: ((magnitudes: FloatArray, peakDb: Float, rms: Float) -> Unit)? = null

    override fun onConfigure(inputAudioFormat: AudioProcessor.AudioFormat): AudioProcessor.AudioFormat {
        if (inputAudioFormat.encoding != androidx.media3.common.C.ENCODING_PCM_16BIT) {
            return AudioProcessor.AudioFormat.NOT_SET
        }
        return inputAudioFormat
    }

    override fun queueInput(inputBuffer: ByteBuffer) {
        val position = inputBuffer.position()
        val limit = inputBuffer.limit()
        val remaining = limit - position

        val outputBuffer = replaceOutputBuffer(remaining)
        if (remaining <= 0) return

        var sumSquares = 0.0
        var maxSampleValue = 0
        val sampleCount = remaining / 2
        val samples = FloatArray(min(sampleCount, 512))

        var sampleIdx = 0
        var i = position
        while (i < limit - 1) {
            val sampleLow = inputBuffer.get(i).toInt() and 0xFF
            val sampleHigh = inputBuffer.get(i + 1).toInt()
            val rawSample = (sampleHigh shl 8) or sampleLow
            
            val absVal = abs(rawSample)
            if (absVal > maxSampleValue) {
                maxSampleValue = absVal
            }

            val normalized = rawSample / 32768.0f
            sumSquares += (normalized * normalized)

            if (sampleIdx < samples.size) {
                samples[sampleIdx] = normalized
                sampleIdx++
            }

            // Copy PCM buffer straight through
            outputBuffer.put((rawSample and 0xFF).toByte())
            outputBuffer.put(((rawSample ushr 8) and 0xFF).toByte())
            i += 2
        }

        // Calculate RMS & Peak dB
        val meanSquare = if (sampleCount > 0) sumSquares / sampleCount else 0.0
        rmsEnergy = sqrt(meanSquare).toFloat()
        val normalizedPeak = maxSampleValue / 32768.0f
        peakDb = if (normalizedPeak > 0.0001f) 20f * log10(normalizedPeak) else -60f

        // Process Spectrum Bands
        computeSpectrumBands(samples, sampleIdx)

        inputBuffer.position(limit)
        outputBuffer.flip()

        onSpectrumDataListener?.invoke(currentMagnitudes, peakDb, rmsEnergy)
    }

    private fun computeSpectrumBands(samples: FloatArray, count: Int) {
        if (count == 0) return
        val bands = FloatArray(bandCount)
        val chunkSize = max(1, count / bandCount)

        var highestEnergy = 0f
        var dominantIndex = 0

        for (b in 0 until bandCount) {
            val start = b * chunkSize
            val end = min(count, start + chunkSize)
            var bandSum = 0f

            for (s in start until end) {
                bandSum += abs(samples[s])
            }

            val avgBandEnergy = if (end > start) (bandSum / (end - start)) else 0f
            // Apply frequency weighting (bass bump + logarithmic treble curve)
            val freqWeight = 1.0f + (b.toFloat() / bandCount) * 0.8f
            val calculatedMagnitude = (avgBandEnergy * freqWeight * sensitivityGain * 2.5f).coerceIn(0.05f, 1.0f)

            bands[b] = calculatedMagnitude

            if (calculatedMagnitude > highestEnergy) {
                highestEnergy = calculatedMagnitude
                dominantIndex = b
            }
        }

        // Smooth transition with previous magnitudes
        for (b in 0 until bandCount) {
            val prev = if (b < currentMagnitudes.size) currentMagnitudes[b] else 0f
            currentMagnitudes[b] = prev * smoothing + bands[b] * (1f - smoothing)
        }

        // Estimate dominant frequency Hz (logarithmic spread across 20Hz - 16kHz)
        val minFreq = 20.0
        val maxFreq = 16000.0
        val ratio = dominantIndex.toDouble() / max(1, bandCount - 1)
        dominantFrequencyHz = (minFreq * Math.pow(maxFreq / minFreq, ratio)).toInt()
    }
}
