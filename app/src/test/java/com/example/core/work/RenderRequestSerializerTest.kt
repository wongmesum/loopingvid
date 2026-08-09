package com.example.core.work

import androidx.compose.ui.graphics.Color
import com.example.core.ffmpeg.SlideshowRenderRequest
import com.example.core.ffmpeg.VisualizerRenderRequest
import com.example.feature.visualizer.VisualizerBackground
import com.example.feature.visualizer.VisualizerMode
import com.example.feature.visualizer.VisualizerRenderConfig
import org.junit.Assert.assertEquals
import org.junit.Test

class RenderRequestSerializerTest {

    @Test
    fun serializeSlideshow_roundTrip() {
        val original = SlideshowRenderRequest(
            imageUris = listOf("uri1", "uri2"),
            audioUri = "audio1",
            perImageDurationSec = 4.0,
            transition = "dissolve",
            transitionDurationSec = 1.5,
            resolution = "4k",
            aspectRatio = "9:16",
            frameRate = "60fps",
            outputName = "TestSlide",
            kenBurnsEnabled = true,
            overlayText = "Hello"
        )

        val json = RenderRequestSerializer.serializeSlideshow(original)
        val restored = RenderRequestSerializer.deserializeSlideshow(json)

        assertEquals(original, restored)
    }

    @Test
    fun serializeVisualizer_roundTrip() {
        val original = VisualizerRenderRequest(
            audioUri = "my_audio",
            beatMarkersMs = listOf(100L, 200L, 300L),
            beatEffectExpression = "scale",
            durationMs = 5000L,
            outputName = "TestViz",
            config = VisualizerRenderConfig(
                mode = VisualizerMode.CIRCLE,
                bandCount = 64,
                sizeScale = 1.5f,
                background = VisualizerBackground.Gradient(Color(0xFF000000), Color(0xFFFFFFFF)),
                safeAreaOverlay = "tiktok"
            )
        )

        val json = RenderRequestSerializer.serializeVisualizer(original)
        val restored = RenderRequestSerializer.deserializeVisualizer(json)

        assertEquals(original, restored)
    }
}
