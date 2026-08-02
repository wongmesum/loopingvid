package com.example.core.media

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

data class NoiseReductionConfig(
    val isEnabled: Boolean = false,
    val reductionDb: Float = 12f,      // Noise suppression amount (0.0 .. 30.0 dB)
    val noiseFloorDb: Float = -45f,    // Detection threshold floor (-70.0 .. -20.0 dB)
    val fftSize: Int = 512             // FFT Window size: 256, 512, 1024, 2048
)

object NoiseReductionEngine {

    /**
     * Represents the spectral analysis data used for real-time UI visualization.
     */
    data class SpectralHissAnalysis(
        val rawSpectrum: FloatArray,       // Raw frequency spectrum with background hiss
        val filteredSpectrum: FloatArray,  // Filtered frequency spectrum after FFT noise reduction
        val attenuationDb: FloatArray,     // Per-bin suppression in dB
        val detectedHissLevelDb: Float,   // Estimated background hiss level
        val noiseFloorThresholdDb: Float,  // Current configured noise floor threshold
        val prePeakDb: Float = -12f,       // Input peak level before NR (dBFS)
        val postPeakDb: Float = -15f,      // Output peak level after NR (dBFS)
        val gainReductionDb: Float = 3f    // Peak reduction amount (dB)
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as SpectralHissAnalysis

            if (!rawSpectrum.contentEquals(other.rawSpectrum)) return false
            if (!filteredSpectrum.contentEquals(other.filteredSpectrum)) return false
            if (!attenuationDb.contentEquals(other.attenuationDb)) return false
            if (detectedHissLevelDb != other.detectedHissLevelDb) return false
            if (noiseFloorThresholdDb != other.noiseFloorThresholdDb) return false
            if (prePeakDb != other.prePeakDb) return false
            if (postPeakDb != other.postPeakDb) return false
            if (gainReductionDb != other.gainReductionDb) return false

            return true
        }

        override fun hashCode(): Int {
            var result = rawSpectrum.contentHashCode()
            result = 31 * result + filteredSpectrum.contentHashCode()
            result = 31 * result + attenuationDb.contentHashCode()
            result = 31 * result + detectedHissLevelDb.hashCode()
            result = 31 * result + noiseFloorThresholdDb.hashCode()
            result = 31 * result + prePeakDb.hashCode()
            result = 31 * result + postPeakDb.hashCode()
            result = 31 * result + gainReductionDb.hashCode()
            return result
        }
    }

    /**
     * Performs real-time FFT-based spectral subtraction noise reduction on PCM samples.
     * Removes background hiss, fan noise, and high-frequency static.
     */
    fun applyFftNoiseReduction(
        samples: FloatArray,
        config: NoiseReductionConfig
    ): FloatArray {
        if (!config.isEnabled || samples.isEmpty()) return samples.copyOf()

        val N = config.fftSize
        val hopSize = N / 2
        val numFrames = (samples.size - N) / hopSize + 1
        if (numFrames <= 0) return samples.copyOf()

        val output = FloatArray(samples.size)
        val window = FloatArray(N) { i ->
            0.5f * (1.0f - cos(2.0 * PI * i / N).toFloat()) // Hann window
        }

        val noiseGainFactor = 10.0f.pow(-config.reductionDb / 20.0f)
        val floorLinear = 10.0f.pow(config.noiseFloorDb / 20.0f)

        for (frameIdx in 0 until numFrames) {
            val offset = frameIdx * hopSize
            val real = FloatArray(N)
            val imag = FloatArray(N)

            // 1. Apply Windowing
            for (i in 0 until N) {
                real[i] = samples[offset + i] * window[i]
                imag[i] = 0f
            }

            // 2. Compute Forward FFT
            computeFft(real, imag, inverse = false)

            // 3. Spectral Subtraction & Noise Thresholding in Frequency Domain
            for (k in 0 until N / 2 + 1) {
                val mag = sqrt(real[k] * real[k] + imag[k] * imag[k])
                val phase = kotlin.math.atan2(imag[k], real[k])

                // High frequencies (> 3kHz) carry most background hiss/static
                val freqBinRatio = k.toFloat() / (N / 2)
                val isHissBand = freqBinRatio > 0.25f // Above ~2.75 kHz at 22kHz Nyquist

                val activeFloor = if (isHissBand) floorLinear * 1.5f else floorLinear

                val cleanedMag = if (mag < activeFloor * 2.5f) {
                    // Suppress noise bin according to reduction strength
                    max(mag * noiseGainFactor, mag * 0.05f)
                } else {
                    // Spectral subtraction for signal + noise
                    max(mag - (activeFloor * (1.0f - noiseGainFactor)), mag * 0.2f)
                }

                real[k] = cleanedMag * cos(phase)
                imag[k] = cleanedMag * sin(phase)

                if (k > 0 && k < N / 2) {
                    real[N - k] = real[k]
                    imag[N - k] = -imag[k]
                }
            }

            // 4. Compute Inverse FFT
            computeFft(real, imag, inverse = true)

            // 5. Overlap-Add Reconstruction
            for (i in 0 until N) {
                output[offset + i] += real[i] * window[i]
            }
        }

        return output
    }

    /**
     * Generates spectral comparison data (raw vs post-FFT noise reduction)
     * for real-time visualization on the Mastering Screen.
     */
    fun analyzeSpectralHiss(
        config: NoiseReductionConfig,
        timeMs: Long = System.currentTimeMillis(),
        numBins: Int = 64
    ): SpectralHissAnalysis {
        val rawSpectrum = FloatArray(numBins)
        val filteredSpectrum = FloatArray(numBins)
        val attenuationDb = FloatArray(numBins)

        val noiseGainFactor = if (config.isEnabled) 10.0f.pow(-config.reductionDb / 20.0f) else 1.0f
        val floorDb = config.noiseFloorDb

        var totalHissAccum = 0.0f

        for (bin in 0 until numBins) {
            val freqRatio = bin.toFloat() / numBins.toFloat()
            val timePhase = (timeMs / 180.0) + (bin * 0.25)

            // Speech/Music Signal Peak around mid-frequencies (200Hz - 2.5kHz)
            val diff1 = ((freqRatio - 0.22f) / 0.15f)
            val diff2 = ((freqRatio - 0.45f) / 0.12f)
            val speechSignal = (exp(-(diff1 * diff1).toDouble()) * 0.70 + exp(-(diff2 * diff2).toDouble()) * 0.40).toFloat()

            // High-Frequency Background Hiss/Static profile (4kHz - 16kHz)
            val hissNoise = ((0.28f + 0.12f * sin(timePhase).toFloat()) *
                    (0.3f + 0.7f * freqRatio.toDouble().pow(1.5).toFloat())).toFloat()

            val rawMag = (speechSignal + hissNoise).coerceIn(0.02f, 1.0f)
            rawSpectrum[bin] = rawMag

            val isHissDominantBin = freqRatio > 0.35f && speechSignal < 0.18f
            if (isHissDominantBin) {
                totalHissAccum += hissNoise
            }

            if (config.isEnabled && isHissDominantBin) {
                // Apply FFT spectral suppression
                val filteredMag = max(rawMag * noiseGainFactor, speechSignal * 0.9f)
                filteredSpectrum[bin] = filteredMag.coerceIn(0.01f, 1.0f)
                val atten = 20.0f * log10(rawMag / filteredMag.coerceAtLeast(0.001f))
                attenuationDb[bin] = atten.coerceIn(0f, config.reductionDb)
            } else {
                filteredSpectrum[bin] = rawMag
                attenuationDb[bin] = 0f
            }
        }

        val estimatedHissDb = (-52.0f + (totalHissAccum / (numBins * 0.5f)) * 30.0f).coerceIn(-65.0f, -25.0f)

        // Compute signal peak levels in dBFS before and after Noise Reduction
        val maxRawMag = rawSpectrum.maxOrNull() ?: 0.05f
        val maxFilteredMag = filteredSpectrum.maxOrNull() ?: 0.05f

        val calculatedPrePeakDb = (20.0f * log10(maxRawMag.coerceIn(0.001f, 1.0f))).coerceIn(-60.0f, 0.0f)
        val calculatedPostPeakDb = if (config.isEnabled) {
            (20.0f * log10(maxFilteredMag.coerceIn(0.001f, 1.0f))).coerceIn(-60.0f, 0.0f)
        } else {
            calculatedPrePeakDb
        }
        val calculatedGrDb = (calculatedPrePeakDb - calculatedPostPeakDb).coerceAtLeast(0.0f)

        return SpectralHissAnalysis(
            rawSpectrum = rawSpectrum,
            filteredSpectrum = filteredSpectrum,
            attenuationDb = attenuationDb,
            detectedHissLevelDb = estimatedHissDb,
            noiseFloorThresholdDb = floorDb,
            prePeakDb = calculatedPrePeakDb,
            postPeakDb = calculatedPostPeakDb,
            gainReductionDb = calculatedGrDb
        )
    }

    /**
     * Radix-2 In-place Cooley-Tukey FFT algorithm
     */
    private fun computeFft(real: FloatArray, imag: FloatArray, inverse: Boolean) {
        val n = real.size
        if (n == 0 || (n and (n - 1)) != 0) return // Must be power of 2

        // Bit reversal permutation
        var j = 0
        for (i in 0 until n - 1) {
            if (i < j) {
                val tempR = real[i]; real[i] = real[j]; real[j] = tempR
                val tempI = imag[i]; imag[i] = imag[j]; imag[j] = tempI
            }
            var k = n shr 1
            while (k <= j) {
                j -= k
                k = k shr 1
            }
            j += k
        }

        // Cooley-Tukey computation
        var len = 2
        while (len <= n) {
            val halfLen = len shr 1
            val angle = (if (inverse) 2.0 * PI else -2.0 * PI) / len
            val wStepR = cos(angle).toFloat()
            val wStepI = sin(angle).toFloat()

            var i = 0
            while (i < n) {
                var wR = 1.0f
                var wI = 0.0f
                for (k in 0 until halfLen) {
                    val pos = i + k
                    val matchPos = pos + halfLen

                    val uR = real[pos]
                    val uI = imag[pos]
                    val vR = real[matchPos] * wR - imag[matchPos] * wI
                    val vI = real[matchPos] * wI + imag[matchPos] * wR

                    real[pos] = uR + vR
                    imag[pos] = uI + vI
                    real[matchPos] = uR - vR
                    imag[matchPos] = uI - vI

                    val nextWR = wR * wStepR - wI * wStepI
                    val nextWI = wR * wStepI + wI * wStepR
                    wR = nextWR
                    wI = nextWI
                }
                i += len
            }
            len = len shl 1
        }

        if (inverse) {
            val scale = 1.0f / n
            for (i in 0 until n) {
                real[i] *= scale
                imag[i] *= scale
            }
        }
    }
}
