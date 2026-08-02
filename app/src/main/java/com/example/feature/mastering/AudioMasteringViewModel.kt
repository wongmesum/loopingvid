package com.example.feature.mastering

import android.content.Context
import androidx.annotation.OptIn
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.audio.SonicAudioProcessor
import androidx.media3.common.util.UnstableApi
import com.example.core.media.AudioAnalysisData
import com.example.core.media.AudioMasteringEngine
import com.example.core.media.AutoLevelingConfig
import com.example.core.media.CompressorConfig
import com.example.core.media.EqBandConfig
import com.example.core.media.ExoPlayerAudioProcessor
import com.example.core.media.Media3SpectrumAudioProcessor
import com.example.core.media.NoiseReductionConfig
import com.example.core.media.WaveformAnalyzer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@OptIn(UnstableApi::class)
data class AudioMasteringState(
    val audioUri: String? = null,
    val audioTitle: String = "Untitled Track",
    val isPlaying: Boolean = false,
    val playbackSpeed: Float = 1.0f,
    val playbackPitch: Float = 1.0f,
    val inputGainDb: Float = 0.0f,
    val outputGainDb: Float = 0.0f,
    val targetLufs: Double = -14.0f.toDouble(),
    val isAutoLevelingEnabled: Boolean = false,
    val autoLevelingConfig: AutoLevelingConfig = AutoLevelingConfig(),
    val eqConfig: EqBandConfig = EqBandConfig(),
    val compressorConfig: CompressorConfig = CompressorConfig(),
    val noiseReductionConfig: NoiseReductionConfig = NoiseReductionConfig(),
    val analysisData: AudioAnalysisData? = null,
    val calculatedOutputLufs: Double = -14.0,
    val peakDb: Float = -60.0f,
    val rmsEnergy: Float = 0.0f,
    val spectrumMagnitudes: FloatArray = FloatArray(32),
    val errorMessage: String? = null
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as AudioMasteringState

        if (audioUri != other.audioUri) return false
        if (isPlaying != other.isPlaying) return false
        if (playbackSpeed != other.playbackSpeed) return false
        if (playbackPitch != other.playbackPitch) return false
        if (inputGainDb != other.inputGainDb) return false
        if (outputGainDb != other.outputGainDb) return false
        if (targetLufs != other.targetLufs) return false
        if (isAutoLevelingEnabled != other.isAutoLevelingEnabled) return false
        if (autoLevelingConfig != other.autoLevelingConfig) return false
        if (eqConfig != other.eqConfig) return false
        if (compressorConfig != other.compressorConfig) return false
        if (noiseReductionConfig != other.noiseReductionConfig) return false
        if (peakDb != other.peakDb) return false
        if (rmsEnergy != other.rmsEnergy) return false
        if (!spectrumMagnitudes.contentEquals(other.spectrumMagnitudes)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = audioUri?.hashCode() ?: 0
        result = 31 * result + isPlaying.hashCode()
        result = 31 * result + playbackSpeed.hashCode()
        result = 31 * result + playbackPitch.hashCode()
        result = 31 * result + inputGainDb.hashCode()
        result = 31 * result + outputGainDb.hashCode()
        result = 31 * result + targetLufs.hashCode()
        result = 31 * result + isAutoLevelingEnabled.hashCode()
        result = 31 * result + autoLevelingConfig.hashCode()
        result = 31 * result + eqConfig.hashCode()
        result = 31 * result + compressorConfig.hashCode()
        result = 31 * result + noiseReductionConfig.hashCode()
        result = 31 * result + peakDb.hashCode()
        result = 31 * result + rmsEnergy.hashCode()
        result = 31 * result + spectrumMagnitudes.contentHashCode()
        return result
    }
}

/**
 * ViewModel for Audio Mastering utilizing Media3's SonicAudioProcessor, ExoPlayerAudioProcessor,
 * and Media3SpectrumAudioProcessor for real-time gain, normalization, pitch/speed, and equalization.
 */
@OptIn(UnstableApi::class)
class AudioMasteringViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(AudioMasteringState())
    val uiState: StateFlow<AudioMasteringState> = _uiState.asStateFlow()

    // Media3 Audio Processors
    val sonicAudioProcessor = SonicAudioProcessor()
    val exoPlayerAudioProcessor = ExoPlayerAudioProcessor()
    val spectrumAudioProcessor = Media3SpectrumAudioProcessor().apply {
        onSpectrumDataListener = { mags, peak, rms ->
            _uiState.update {
                it.copy(
                    spectrumMagnitudes = mags,
                    peakDb = peak,
                    rmsEnergy = rms
                )
            }
        }
    }

    init {
        // Initialize processors with defaults
        sonicAudioProcessor.setSpeed(1.0f)
        sonicAudioProcessor.setPitch(1.0f)
    }

    fun loadAudioTrack(uri: String, title: String) {
        val analysis = WaveformAnalyzer.generateSimulatedWaveform()
        _uiState.update {
            it.copy(
                audioUri = uri,
                audioTitle = title,
                analysisData = analysis
            )
        }
        recalculateMasteringMetrics()
    }

    fun setPlaybackSpeed(speed: Float) {
        val clampedSpeed = speed.coerceIn(0.25f, 3.0f)
        sonicAudioProcessor.setSpeed(clampedSpeed)
        _uiState.update { it.copy(playbackSpeed = clampedSpeed) }
    }

    fun setPlaybackPitch(pitch: Float) {
        val clampedPitch = pitch.coerceIn(0.5f, 2.0f)
        sonicAudioProcessor.setPitch(clampedPitch)
        _uiState.update { it.copy(playbackPitch = clampedPitch) }
    }

    fun setInputGain(gainDb: Float) {
        val clamped = gainDb.coerceIn(-24f, 24f)
        exoPlayerAudioProcessor.volume = Math.pow(10.0, (clamped / 20.0).toDouble()).toFloat().coerceIn(0f, 2f)
        _uiState.update { it.copy(inputGainDb = clamped) }
        recalculateMasteringMetrics()
    }

    fun setOutputGain(gainDb: Float) {
        val clamped = gainDb.coerceIn(-24f, 24f)
        _uiState.update { it.copy(outputGainDb = clamped) }
        recalculateMasteringMetrics()
    }

    fun setTargetLufs(lufs: Double) {
        _uiState.update { it.copy(targetLufs = lufs) }
        recalculateMasteringMetrics()
    }

    fun setAutoLevelingEnabled(enabled: Boolean) {
        _uiState.update {
            it.copy(
                isAutoLevelingEnabled = enabled,
                autoLevelingConfig = it.autoLevelingConfig.copy(isEnabled = enabled)
            )
        }
        recalculateMasteringMetrics()
    }

    fun updateEqBand(bandIndex: Int, gainDb: Float) {
        val currentEq = _uiState.value.eqConfig
        val newEq = when (bandIndex) {
            0 -> currentEq.copy(lowGainDb = gainDb)
            1 -> currentEq.copy(midLowGainDb = gainDb)
            2 -> currentEq.copy(midGainDb = gainDb)
            3 -> currentEq.copy(midHighGainDb = gainDb)
            else -> currentEq.copy(highGainDb = gainDb)
        }

        // Apply to ExoPlayer audio processor bass/treble approximate shelf mapping
        if (bandIndex == 0) {
            exoPlayerAudioProcessor.bassGainDb = gainDb
        } else if (bandIndex >= 3) {
            exoPlayerAudioProcessor.trebleGainDb = gainDb
        }

        _uiState.update { it.copy(eqConfig = newEq) }
        recalculateMasteringMetrics()
    }

    fun setCompressorSettings(threshold: Float, ratio: Float, attack: Float, release: Float) {
        _uiState.update {
            it.copy(
                compressorConfig = CompressorConfig(
                    thresholdDb = threshold,
                    ratio = ratio,
                    attackMs = attack,
                    releaseMs = release
                )
            )
        }
        recalculateMasteringMetrics()
    }

    fun toggleNoiseReduction(enabled: Boolean, reductionDb: Float = 14f) {
        _uiState.update {
            it.copy(
                noiseReductionConfig = it.noiseReductionConfig.copy(
                    isEnabled = enabled,
                    reductionDb = reductionDb
                )
            )
        }
    }

    fun setPlaying(playing: Boolean) {
        _uiState.update { it.copy(isPlaying = playing) }
    }

    private fun recalculateMasteringMetrics() {
        val state = _uiState.value
        val inputLufs = (state.analysisData?.currentRmsLufs ?: -22.0) + state.inputGainDb
        val baseOutput = AudioMasteringEngine.calculateOutputLufs(
            inputRmsLufs = inputLufs,
            eqConfig = state.eqConfig,
            compConfig = state.compressorConfig,
            autoLevelConfig = state.autoLevelingConfig,
            targetLufs = state.targetLufs
        )
        val finalOutput = baseOutput + state.outputGainDb
        _uiState.update { it.copy(calculatedOutputLufs = finalOutput) }
    }
}
