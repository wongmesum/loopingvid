package com.example.core.ffmpeg

/** Measured loudnorm values emitted by FFmpeg pass one. */
data class LoudnormMeasurement(
    val inputIntegratedLufs: Double,
    val inputTruePeakDb: Double,
    val inputLoudnessRange: Double,
    val inputThreshold: Double
)

object LoudnormAnalysisParser {
    private val fieldPattern = Regex("\\\"(input_i|input_tp|input_lra|input_thresh)\\\"\\s*:\\s*\\\"?([-+]?\\d+(?:\\.\\d+)?)\\\"?")

    fun parse(output: String): LoudnormMeasurement? {
        val values = fieldPattern.findAll(output).associate { match ->
            match.groupValues[1] to match.groupValues[2].toDoubleOrNull()
        }
        val inputIntegratedLufs = values["input_i"] ?: return null
        val inputTruePeakDb = values["input_tp"] ?: return null
        val inputLoudnessRange = values["input_lra"] ?: return null
        val inputThreshold = values["input_thresh"] ?: return null
        if (!listOf(inputIntegratedLufs, inputTruePeakDb, inputLoudnessRange, inputThreshold)
                .all(Double::isFinite)
        ) {
            return null
        }
        return LoudnormMeasurement(
            inputIntegratedLufs = inputIntegratedLufs,
            inputTruePeakDb = inputTruePeakDb,
            inputLoudnessRange = inputLoudnessRange,
            inputThreshold = inputThreshold
        )
    }
}
