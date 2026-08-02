package com.example.core.media

data class EqBandConfig(
    val lowGainDb: Float = 0f,      // 60 Hz (Bass)
    val midLowGainDb: Float = 0f,   // 250 Hz (Warmth)
    val midGainDb: Float = 0f,      // 1 kHz (Presence)
    val midHighGainDb: Float = 0f,  // 4 kHz (Clarity)
    val highGainDb: Float = 0f      // 12 kHz (Air/Treble)
)

data class CompressorConfig(
    val thresholdDb: Float = -18f,
    val ratio: Float = 3.0f,
    val attackMs: Float = 15f,
    val releaseMs: Float = 100f,
    val makeupGainDb: Float = 3.0f
)

data class AutoLevelingConfig(
    val isEnabled: Boolean = false,
    val targetLoudnessLufs: Float = -14.0f,
    val maxBoostDb: Float = 6.0f,
    val maxReductionDb: Float = -6.0f
)

data class MasteringPreset(
    val name: String,
    val eqBandConfig: EqBandConfig,
    val compressorConfig: CompressorConfig,
    val noiseReductionConfig: NoiseReductionConfig = NoiseReductionConfig(),
    val autoLevelingConfig: AutoLevelingConfig = AutoLevelingConfig(),
    val targetLufs: Double = -14.0,
    val inputGainDb: Float = 0f,
    val outputGainDb: Float = 0f,
    val fadeInSec: Float = 0f,
    val fadeOutSec: Float = 0f,
    val isCustom: Boolean = false,
    val id: String = name,
    val description: String = "Balanced audio mastering profile for video production",
    val category: String = "General"
)

object AudioMasteringEngine {

    val PRESETS = listOf(
        MasteringPreset(
            name = "Bass Boost",
            eqBandConfig = EqBandConfig(lowGainDb = 6.0f, midLowGainDb = 3.0f, midGainDb = -1.0f, midHighGainDb = 0.5f, highGainDb = 1.0f),
            compressorConfig = CompressorConfig(thresholdDb = -20f, ratio = 4.0f, makeupGainDb = 4.5f),
            noiseReductionConfig = NoiseReductionConfig(isEnabled = false),
            targetLufs = -13.5,
            description = "Enhances low-end punch and sub-bass resonance for music and intense video scenes.",
            category = "Bass & Beats"
        ),
        MasteringPreset(
            name = "Voice Clarity",
            eqBandConfig = EqBandConfig(lowGainDb = -3.0f, midLowGainDb = -1.0f, midGainDb = 3.5f, midHighGainDb = 4.0f, highGainDb = 2.0f),
            compressorConfig = CompressorConfig(thresholdDb = -18f, ratio = 3.2f, makeupGainDb = 3.8f),
            noiseReductionConfig = NoiseReductionConfig(isEnabled = true, reductionDb = 8f, noiseFloorDb = -50f),
            targetLufs = -14.0,
            description = "Cuts low-end rumble and boosts 1kHz-4kHz speech frequencies for crisp dialogue.",
            category = "Vocals & Dialogue"
        ),
        MasteringPreset(
            name = "Noise Reduction",
            eqBandConfig = EqBandConfig(lowGainDb = -4.5f, midLowGainDb = -1.5f, midGainDb = 0.0f, midHighGainDb = -1.0f, highGainDb = -3.5f),
            compressorConfig = CompressorConfig(thresholdDb = -22f, ratio = 2.8f, makeupGainDb = 2.0f),
            noiseReductionConfig = NoiseReductionConfig(isEnabled = true, reductionDb = 14f, noiseFloorDb = -48f),
            targetLufs = -15.0,
            description = "Attenuates harsh high-frequency hiss, fan noise, and background HVAC hum.",
            category = "Restoration"
        ),
        MasteringPreset(
            name = "Loudness Maximizer",
            eqBandConfig = EqBandConfig(lowGainDb = 1.5f, midLowGainDb = 0.0f, midGainDb = 1.0f, midHighGainDb = 2.0f, highGainDb = 2.5f),
            compressorConfig = CompressorConfig(thresholdDb = -24f, ratio = 5.0f, makeupGainDb = 6.0f),
            targetLufs = -11.0,
            description = "Pushes overall perceived volume to streaming ceiling without digital clipping.",
            category = "Streaming & Social"
        ),
        MasteringPreset(
            name = "Clear & Punchy",
            eqBandConfig = EqBandConfig(lowGainDb = 2.5f, midLowGainDb = -1.0f, midGainDb = 0.5f, midHighGainDb = 2.0f, highGainDb = 3.0f),
            compressorConfig = CompressorConfig(thresholdDb = -16f, ratio = 3.5f, makeupGainDb = 4.0f),
            targetLufs = -14.0,
            description = "Balanced dynamic range with enhanced presence and high-end air.",
            category = "General"
        ),
        MasteringPreset(
            name = "Cinematic Warmth",
            eqBandConfig = EqBandConfig(lowGainDb = 3.5f, midLowGainDb = 2.5f, midGainDb = 0.0f, midHighGainDb = -0.5f, highGainDb = -1.5f),
            compressorConfig = CompressorConfig(thresholdDb = -16f, ratio = 2.5f, makeupGainDb = 2.5f),
            targetLufs = -15.5,
            description = "Rich analog warmth and smooth top end suited for movie trailers and ambient tracks.",
            category = "Film & Trailer"
        ),
        MasteringPreset(
            name = "Transparent Neutral",
            eqBandConfig = EqBandConfig(lowGainDb = 0.0f, midLowGainDb = 0.0f, midGainDb = 0.0f, midHighGainDb = 0.0f, highGainDb = 0.0f),
            compressorConfig = CompressorConfig(thresholdDb = -12f, ratio = 2.0f, makeupGainDb = 1.5f),
            targetLufs = -14.0,
            description = "Flattest response with gentle limiting for faithful audio reproduction.",
            category = "General"
        )
    )

    fun calculateOutputLufs(
        inputRmsLufs: Double,
        eqConfig: EqBandConfig,
        compConfig: CompressorConfig,
        autoLevelConfig: AutoLevelingConfig,
        targetLufs: Double
    ): Double {
        val safeInputRmsLufs = if (inputRmsLufs.isNaN() || inputRmsLufs.isInfinite()) -22.0 else inputRmsLufs
        val totalEqBoost = (eqConfig.lowGainDb + eqConfig.midLowGainDb + eqConfig.midGainDb + eqConfig.midHighGainDb + eqConfig.highGainDb) / 5.0
        val compRatio = if (compConfig.ratio <= 0.1f || compConfig.ratio.isNaN() || compConfig.ratio.isInfinite()) 1.0f else compConfig.ratio
        val compThreshold = compConfig.thresholdDb.toDouble()
        val compReduction = if (safeInputRmsLufs > compThreshold) {
            (safeInputRmsLufs - compThreshold) * (1.0 - (1.0 / compRatio))
        } else {
            0.0
        }

        var estimatedOutputLufs = safeInputRmsLufs + totalEqBoost - compReduction + compConfig.makeupGainDb
        
        if (estimatedOutputLufs.isNaN() || estimatedOutputLufs.isInfinite()) {
            estimatedOutputLufs = -14.0
        }

        if (autoLevelConfig.isEnabled) {
            val lufsDifference = autoLevelConfig.targetLoudnessLufs - estimatedOutputLufs
            val appliedGain = if (lufsDifference.isNaN() || lufsDifference.isInfinite()) {
                0.0
            } else {
                lufsDifference.coerceIn(autoLevelConfig.maxReductionDb.toDouble(), autoLevelConfig.maxBoostDb.toDouble())
            }
            estimatedOutputLufs += appliedGain
        }
        
        return if (estimatedOutputLufs.isNaN() || estimatedOutputLufs.isInfinite()) -14.0 else estimatedOutputLufs.coerceIn(-30.0, -6.0)
    }
}
