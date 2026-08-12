package com.example.core.ffmpeg

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LoudnormAnalysisParserTest {

    @Test
    fun `parse returns measured values from ffmpeg loudnorm json`() {
        val output = """
            [Parsed_loudnorm_0 @ 0x123] {
                "input_i" : "-20.47",
                "input_tp" : "-2.31",
                "input_lra" : "8.20",
                "input_thresh" : "-30.68",
                "output_i" : "-13.95"
            }
        """.trimIndent()

        val measurement = LoudnormAnalysisParser.parse(output)

        requireNotNull(measurement)
        assertEquals(-20.47, measurement.inputIntegratedLufs, 0.001)
        assertEquals(-2.31, measurement.inputTruePeakDb, 0.001)
        assertEquals(8.20, measurement.inputLoudnessRange, 0.001)
        assertEquals(-30.68, measurement.inputThreshold, 0.001)
    }

    @Test
    fun `parse returns null when analysis is incomplete`() {
        val output = """
            {
                "input_i" : "-20.47",
                "input_tp" : "-2.31"
            }
        """.trimIndent()

        assertNull(LoudnormAnalysisParser.parse(output))
    }

    @Test
    fun `parse returns null for non finite measurements`() {
        val output = """
            {
                "input_i" : "-inf",
                "input_tp" : "-2.31",
                "input_lra" : "8.20",
                "input_thresh" : "-30.68"
            }
        """.trimIndent()

        assertNull(LoudnormAnalysisParser.parse(output))
    }
}
