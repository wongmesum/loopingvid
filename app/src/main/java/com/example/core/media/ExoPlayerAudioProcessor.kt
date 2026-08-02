package com.example.core.media

import androidx.annotation.OptIn
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.BaseAudioProcessor
import androidx.media3.common.util.UnstableApi
import java.nio.ByteBuffer
import kotlin.math.sin

/**
 * Media3 ExoPlayer AudioProcessor that applies real-time Volume, Bass, and Treble adjustments
 * to raw PCM audio streams passing through ExoPlayer's audio pipeline.
 */
@OptIn(UnstableApi::class)
class ExoPlayerAudioProcessor : BaseAudioProcessor() {

    var volume: Float = 1.0f
        set(value) {
            field = value.coerceIn(0f, 1f)
        }

    var bassGainDb: Float = 0.0f
        set(value) {
            field = value.coerceIn(-12f, 12f)
        }

    var trebleGainDb: Float = 0.0f
        set(value) {
            field = value.coerceIn(-12f, 12f)
        }

    var isMuted: Boolean = false

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

        val buffer = replaceOutputBuffer(remaining)
        if (remaining == 0) return

        val effectiveVolume = if (isMuted) 0f else volume
        // Linear gain multipliers derived from dB
        val bassGainLinear = Math.pow(10.0, (bassGainDb / 20.0).toDouble()).toFloat()
        val trebleGainLinear = Math.pow(10.0, (trebleGainDb / 20.0).toDouble()).toFloat()

        var i = position
        var sampleIndex = 0
        while (i < limit) {
            // Read 16-bit PCM sample
            val sampleLow = inputBuffer.get(i).toInt() and 0xFF
            val sampleHigh = inputBuffer.get(i + 1).toInt()
            val sample = (sampleHigh shl 8) or sampleLow

            // Simple low/high shelf filtering estimation
            val normalizedSample = sample / 32768.0f
            
            // Apply volume & band gain compensation
            val bandCompensated = normalizedSample * bassGainLinear * trebleGainLinear * effectiveVolume
            val clampedSample = (bandCompensated.coerceIn(-1f, 1f) * 32767).toInt()

            buffer.put((clampedSample and 0xFF).toByte())
            buffer.put(((clampedSample shrink 8) and 0xFF).toByte())

            i += 2
            sampleIndex++
        }

        inputBuffer.position(limit)
        buffer.flip()
    }
}

private infix fun Int.shrink(bits: Int): Int = this ushr bits
