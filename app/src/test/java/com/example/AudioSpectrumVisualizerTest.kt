package com.example

import com.example.core.media.Media3SpectrumAudioProcessor
import org.junit.Assert.assertEquals
import org.junit.Test

class AudioSpectrumVisualizerTest {

    @Test
    fun media3SpectrumAudioProcessor_configuresBandCountAndGain() {
        val processor = Media3SpectrumAudioProcessor()
        processor.bandCount = 64
        processor.sensitivityGain = 2.0f

        assertEquals(64, processor.bandCount)
        assertEquals(2.0f, processor.sensitivityGain, 0.01f)
    }

    @Test
    fun media3SpectrumAudioProcessor_clampsGainsAndBands() {
        val processor = Media3SpectrumAudioProcessor()
        processor.bandCount = 128 // Should clamp to max 64
        processor.sensitivityGain = 10.0f // Should clamp to max 4.0

        assertEquals(64, processor.bandCount)
        assertEquals(4.0f, processor.sensitivityGain, 0.01f)
    }
}
