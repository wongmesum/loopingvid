package com.example.core.audio

import android.content.Context
import android.net.Uri
import com.example.feature.visualizer.beat.PcmDecoder
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import timber.log.Timber
import kotlin.math.abs
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * Analysis entry point used by [AudioAnalysisRepository]. Declared as an interface
 * so the caching layer can be tested without decoding a real media file.
 */
fun interface AudioAnalyzer {
    suspend fun analyze(uri: Uri, options: AnalysisOptions): Result<AudioAnalysisResult>
}

/**
 * Unified audio analysis engine for all LoopingVid tools.
 * Provides waveform, spectrum, BPM, and loudness (LUFS approximation) analysis
 * from a single decode pass over the file.
 *
 * This class holds no cache — [AudioAnalysisRepository] owns caching so analysis
 * results can be persisted across process restarts.
 */
class AudioAnalysisService(private val context: Context) : AudioAnalyzer {

    override suspend fun analyze(uri: Uri, options: AnalysisOptions): Result<AudioAnalysisResult> =
        analyzeAudio(uri, options)

    private val _analysisState = MutableStateFlow<AudioAnalysisState>(AudioAnalysisState.Idle)
    val analysisState: StateFlow<AudioAnalysisState> = _analysisState.asStateFlow()

    /**
     * Analyzes audio from a URI and returns comprehensive analysis results.
     * Runs on [Dispatchers.IO] since it decodes raw sample data from disk.
     */
    suspend fun analyzeAudio(
        uri: Uri,
        options: AnalysisOptions = AnalysisOptions()
    ): Result<AudioAnalysisResult> = withContext(Dispatchers.IO) {
        try {
            _analysisState.value = AudioAnalysisState.Analyzing(0)

            // Decoding goes through PcmDecoder: MediaExtractor alone yields compressed
            // packets, so reading them as PCM16 would produce meaningless analysis.
            val decoded = PcmDecoder.decode(context, uri.toString()).getOrElse { error ->
                _analysisState.value = AudioAnalysisState.Error(error.message ?: "Decode failed")
                return@withContext Result.failure(error)
            }

            val samples = decoded.samples.asList()
            val sampleRate = decoded.sampleRate
            val durationMs = decoded.durationMs
            // PcmDecoder downmixes to mono, so analysis always runs on a single channel.
            val channelCount = 1

            _analysisState.value = AudioAnalysisState.Analyzing(50)

            val waveform = if (options.generateWaveform) {
                generateWaveform(samples, options.waveformPoints)
            } else null

            val spectrum = if (options.generateSpectrum) {
                generateSpectrum(samples, options.spectrumBands)
            } else null

            val bpm = if (options.detectBpm) {
                detectBpm(samples, sampleRate)
            } else null

            val loudness = if (options.analyzeLoudness) {
                analyzeLoudness(samples)
            } else null

            val result = AudioAnalysisResult(
                durationMs = durationMs,
                sampleRate = sampleRate,
                channelCount = channelCount,
                waveform = waveform,
                spectrum = spectrum,
                bpm = bpm,
                loudness = loudness,
                peakDb = calculatePeakDb(samples),
                rmsDb = calculateRmsDb(samples)
            )

            _analysisState.value = AudioAnalysisState.Complete(result)
            Result.success(result)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Timber.e(e, "Audio analysis failed")
            _analysisState.value = AudioAnalysisState.Error(e.message ?: "Analysis failed")
            Result.failure(e)
        }
    }

    /** Downsamples [samples] to [points] values, each the peak absolute value in its window. */
    internal fun generateWaveform(samples: List<Float>, points: Int): WaveformData {
        if (samples.isEmpty() || points <= 0) return WaveformData(emptyList())

        val samplesPerPoint = max(1, samples.size / points)
        val waveformPoints = ArrayList<Float>(points)

        for (i in 0 until points) {
            val start = i * samplesPerPoint
            if (start >= samples.size) break
            val end = min(samples.size, start + samplesPerPoint)

            var maxAbs = 0f
            for (j in start until end) {
                val absVal = abs(samples[j])
                if (absVal > maxAbs) maxAbs = absVal
            }
            waveformPoints.add(maxAbs)
        }

        return WaveformData(waveformPoints)
    }

    /** Produces a coarse magnitude-per-band spectrum from a leading window of [samples]. */
    internal fun generateSpectrum(samples: List<Float>, bands: Int): SpectrumData {
        if (samples.isEmpty() || bands <= 0) return SpectrumData(emptyList())

        val windowSize = min(2048, samples.size)
        val window = samples.subList(0, windowSize)

        val magnitudes = FloatArray(bands)
        val samplesPerBand = max(1, windowSize / bands)

        for (b in 0 until bands) {
            val start = b * samplesPerBand
            val end = min(windowSize, start + samplesPerBand)

            var sum = 0f
            for (i in start until end) {
                sum += abs(window[i])
            }

            val avg = if (end > start) sum / (end - start) else 0f
            val freqWeight = 1.0f + (b.toFloat() / bands) * 0.8f
            magnitudes[b] = (avg * freqWeight).coerceIn(0f, 1f)
        }

        return SpectrumData(magnitudes.toList())
    }

    /**
     * Detects BPM from energy-onset peaks. This is a fast heuristic path meant
     * for common formats; [com.example.feature.visualizer.beat.BeatDetectionEngine]
     * remains the more thorough fallback used by the Visualizer module.
     */
    internal fun detectBpm(samples: List<Float>, sampleRate: Int): BpmData {
        if (samples.isEmpty() || sampleRate <= 0) return BpmData(0.0, 0f)

        val frameSize = max(1, sampleRate / 100) // 10ms frames keep interval resolution usable
        val frameCount = samples.size / frameSize
        if (frameCount < MIN_ONSET_FRAMES) return BpmData(0.0, 0f)

        val energies = FloatArray(frameCount)
        for (frame in 0 until frameCount) {
            val start = frame * frameSize
            var energy = 0f
            for (i in start until start + frameSize) {
                energy += samples[i] * samples[i]
            }
            energies[frame] = energy
        }

        val meanEnergy = energies.average()
        // Silence must not produce onsets, so require an absolute floor as well
        // as a margin above the running mean.
        if (meanEnergy <= SILENCE_ENERGY_FLOOR) return BpmData(0.0, 0f)
        val threshold = meanEnergy * ONSET_THRESHOLD_FACTOR

        // Onsets are rising edges through the threshold: a plain "above threshold"
        // test would fire repeatedly for every frame of one sustained hit.
        val onsetFrames = mutableListOf<Int>()
        for (frame in energies.indices) {
            val previous = if (frame == 0) 0.0 else energies[frame - 1].toDouble()
            if (energies[frame] > threshold && previous <= threshold) {
                onsetFrames.add(frame)
            }
        }

        if (onsetFrames.size < 2) return BpmData(0.0, 0f)

        val intervals = (1 until onsetFrames.size).map { onsetFrames[it] - onsetFrames[it - 1] }
        val avgInterval = intervals.average()
        if (avgInterval <= 0.0) return BpmData(0.0, 0f)

        val frameDurationMs = frameSize * 1000.0 / sampleRate
        val bpm = foldIntoMusicalRange(60_000.0 / (avgInterval * frameDurationMs))

        val stdDev = sqrt(intervals.map { (it - avgInterval).pow(2) }.average())
        val confidence = (1.0 - stdDev / avgInterval).toFloat().coerceIn(0f, 1f)

        return BpmData(bpm, confidence)
    }

    /** Halves or doubles [bpm] until it lands in the musical range instead of clamping it. */
    private fun foldIntoMusicalRange(bpm: Double): Double {
        if (bpm <= 0.0 || bpm.isNaN()) return 0.0
        var folded = bpm
        while (folded < MIN_BPM && folded > 0.0) folded *= 2
        while (folded > MAX_BPM) folded /= 2
        return folded
    }

    /** Approximates integrated loudness (LUFS) using simplified RMS-based K-weighting. */
    internal fun analyzeLoudness(samples: List<Float>): LoudnessData {
        if (samples.isEmpty()) return LoudnessData(-60.0, -60.0, -60.0)

        val rms = sqrt(samples.map { it * it }.average())
        val rmsDb = if (rms > 0.0001) 20 * log10(rms) else -60.0

        val peak = samples.maxOfOrNull { abs(it) } ?: 0f
        val peakDb = if (peak > 0.0001) 20 * log10(peak.toDouble()) else -60.0

        val lufs = rmsDb - 0.691 // Rough K-weighting approximation

        return LoudnessData(
            integratedLufs = lufs,
            peakDb = peakDb,
            rmsDb = rmsDb
        )
    }

    private fun calculatePeakDb(samples: List<Float>): Double {
        val peak = samples.maxOfOrNull { abs(it) } ?: 0f
        return if (peak > 0.0001) 20 * log10(peak.toDouble()) else -60.0
    }

    private fun calculateRmsDb(samples: List<Float>): Double {
        if (samples.isEmpty()) return -60.0
        val rms = sqrt(samples.map { it * it }.average())
        return if (rms > 0.0001) 20 * log10(rms) else -60.0
    }

    private companion object {
        /** Below ~200ms of frames there are too few onsets for a meaningful interval. */
        const val MIN_ONSET_FRAMES = 20
        const val SILENCE_ENERGY_FLOOR = 1e-7
        const val ONSET_THRESHOLD_FACTOR = 1.5
        const val MIN_BPM = 60.0
        const val MAX_BPM = 200.0
    }
}

data class AnalysisOptions(
    val generateWaveform: Boolean = true,
    val waveformPoints: Int = 200,
    val generateSpectrum: Boolean = true,
    val spectrumBands: Int = 32,
    val detectBpm: Boolean = true,
    val analyzeLoudness: Boolean = true
)

sealed class AudioAnalysisState {
    object Idle : AudioAnalysisState()
    data class Analyzing(val progress: Int) : AudioAnalysisState()
    data class Complete(val result: AudioAnalysisResult) : AudioAnalysisState()
    data class Error(val message: String) : AudioAnalysisState()
}

data class AudioAnalysisResult(
    val durationMs: Long,
    val sampleRate: Int,
    val channelCount: Int,
    val waveform: WaveformData?,
    val spectrum: SpectrumData?,
    val bpm: BpmData?,
    val loudness: LoudnessData?,
    val peakDb: Double,
    val rmsDb: Double
)

data class WaveformData(
    val points: List<Float>
)

data class SpectrumData(
    val magnitudes: List<Float>
)

data class BpmData(
    val bpm: Double,
    val confidence: Float
)

data class LoudnessData(
    val integratedLufs: Double,
    val peakDb: Double,
    val rmsDb: Double
)
