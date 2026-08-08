package com.example

import com.example.core.ffmpeg.FFmpegCommandBuilder
import com.example.feature.slideshow.SlideshowViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Behaviour guards for the Slideshow FFmpeg command builder.
 *
 * The slideshow renders N still images into a real video file. These tests lock
 * the contract that matters for correctness: every image becomes an input, the
 * per-image duration is honoured, transitions are only emitted when there is an
 * actual boundary to cross, and the audio track is optional.
 */
class SlideshowCommandBuilderTest {

    private val images = listOf("/img/a.jpg", "/img/b.jpg", "/img/c.jpg")

    @Test
    fun `every image is passed as its own looped input`() {
        val args = FFmpegCommandBuilder.buildSlideshowCommand(
            imagePaths = images,
            outputPath = "/out/slideshow.mp4",
            perImageDurationSec = 3.0
        )

        images.forEach { path ->
            assertTrue("Expected image input $path in command", args.contains(path))
        }
        assertEquals(
            "Each still image needs its own -loop input",
            images.size,
            args.count { it == "-loop" }
        )
    }

    @Test
    fun `per image duration drives the input duration flag`() {
        val args = FFmpegCommandBuilder.buildSlideshowCommand(
            imagePaths = images,
            outputPath = "/out/slideshow.mp4",
            perImageDurationSec = 4.5
        )

        assertEquals(images.size, args.count { it == "-t" })
        assertTrue("Duration 4.5s should appear in the command", args.contains("4.500"))
    }

    @Test
    fun `fade transition emits one xfade per image boundary`() {
        val args = FFmpegCommandBuilder.buildSlideshowCommand(
            imagePaths = images,
            outputPath = "/out/slideshow.mp4",
            perImageDurationSec = 3.0,
            transition = "fade",
            transitionDurationSec = 1.0
        )

        val filterGraph = args[args.indexOf("-filter_complex") + 1]
        // 3 images => 2 boundaries => 2 xfade nodes
        assertEquals(2, Regex("xfade").findAll(filterGraph).count())
        assertTrue(filterGraph.contains("transition=fade"))
    }

    @Test
    fun `single image needs no transition node`() {
        val args = FFmpegCommandBuilder.buildSlideshowCommand(
            imagePaths = listOf("/img/only.jpg"),
            outputPath = "/out/slideshow.mp4",
            perImageDurationSec = 3.0,
            transition = "fade",
            transitionDurationSec = 1.0
        )

        val filterGraph = args[args.indexOf("-filter_complex") + 1]
        assertTrue("A single image has no boundary to cross", !filterGraph.contains("xfade"))
    }

    @Test
    fun `audio track is muxed and trimmed to the video length when provided`() {
        val args = FFmpegCommandBuilder.buildSlideshowCommand(
            imagePaths = images,
            outputPath = "/out/slideshow.mp4",
            perImageDurationSec = 3.0,
            audioPath = "/audio/bgm.mp3"
        )

        assertTrue(args.contains("/audio/bgm.mp3"))
        assertTrue("Audio must be encoded", args.contains("aac"))
        assertTrue("Output must end when the images end", args.contains("-shortest"))
    }

    @Test
    fun `no audio input means no audio codec flags`() {
        val args = FFmpegCommandBuilder.buildSlideshowCommand(
            imagePaths = images,
            outputPath = "/out/slideshow.mp4",
            perImageDurationSec = 3.0,
            audioPath = null
        )

        assertTrue(!args.contains("-shortest"))
        assertTrue(args.contains("-an"))
    }

    @Test
    fun `aspect ratio preset is applied to the scaling filter`() {
        val args = FFmpegCommandBuilder.buildSlideshowCommand(
            imagePaths = images,
            outputPath = "/out/slideshow.mp4",
            perImageDurationSec = 3.0,
            aspectRatio = "9:16",
            resolution = "1080p"
        )

        val filterGraph = args[args.indexOf("-filter_complex") + 1]
        assertTrue("9:16 export should target a 1080 wide canvas", filterGraph.contains("1080"))
        assertTrue("9:16 export should target a 1920 tall canvas", filterGraph.contains("1920"))
    }

    @Test
    fun `at least ten transition effects are available`() {
        val effects = SlideshowViewModel.TRANSITIONS.filterNot { it == "none" }

        assertTrue("Phase 9 requires at least 10 transition effects", effects.size >= 10)
    }

    @Test
    fun `new transition is passed to xfade`() {
        val args = FFmpegCommandBuilder.buildSlideshowCommand(
            imagePaths = images,
            outputPath = "/out/slideshow.mp4",
            perImageDurationSec = 3.0,
            transition = "wiperight"
        )

        val filterGraph = args[args.indexOf("-filter_complex") + 1]
        assertTrue(filterGraph.contains("transition=wiperight"))
    }

    @Test
    fun `ken burns enabled emits zoompan filter`() {
        val args = FFmpegCommandBuilder.buildSlideshowCommand(
            imagePaths = images,
            outputPath = "/out/slideshow.mp4",
            perImageDurationSec = 3.0,
            kenBurnsEnabled = true
        )

        val filterGraph = args[args.indexOf("-filter_complex") + 1]
        assertTrue(filterGraph.contains("zoompan="))
    }

    @Test
    fun `ken burns disabled keeps static image filter`() {
        val args = FFmpegCommandBuilder.buildSlideshowCommand(
            imagePaths = images,
            outputPath = "/out/slideshow.mp4",
            perImageDurationSec = 3.0,
            kenBurnsEnabled = false
        )

        val filterGraph = args[args.indexOf("-filter_complex") + 1]
        assertTrue(!filterGraph.contains("zoompan="))
    }

    @Test
    fun `overlay text emits drawtext filter`() {
        val args = FFmpegCommandBuilder.buildSlideshowCommand(
            imagePaths = images,
            outputPath = "/out/slideshow.mp4",
            perImageDurationSec = 3.0,
            overlayText = "Summer memories"
        )

        val filterGraph = args[args.indexOf("-filter_complex") + 1]
        assertTrue(filterGraph.contains("drawtext=text='Summer memories'"))
        assertTrue(args.contains("[textout]"))
    }

    @Test
    fun `empty overlay text skips drawtext filter`() {
        val args = FFmpegCommandBuilder.buildSlideshowCommand(
            imagePaths = images,
            outputPath = "/out/slideshow.mp4",
            perImageDurationSec = 3.0,
            overlayText = ""
        )

        val filterGraph = args[args.indexOf("-filter_complex") + 1]
        assertTrue(!filterGraph.contains("drawtext="))
    }

    @Test
    fun `overlay text escapes ffmpeg special characters`() {
        val args = FFmpegCommandBuilder.buildSlideshowCommand(
            imagePaths = images,
            outputPath = "/out/slideshow.mp4",
            perImageDurationSec = 3.0,
            overlayText = "It's 10:30"
        )

        val filterGraph = args[args.indexOf("-filter_complex") + 1]
        assertTrue(filterGraph.contains("It\\'s 10\\:30"))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `empty image list is rejected`() {
        FFmpegCommandBuilder.buildSlideshowCommand(
            imagePaths = emptyList(),
            outputPath = "/out/slideshow.mp4",
            perImageDurationSec = 3.0
        )
    }
}
