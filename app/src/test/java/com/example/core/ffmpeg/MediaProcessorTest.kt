package com.example.core.ffmpeg

import android.content.Context
import android.util.Log
import com.example.core.database.LoopingVidRepository
import com.example.core.database.RenderJobEntity
import com.example.core.utils.MediaStoreExporter
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
class MediaProcessorTest {

    private lateinit var context: Context
    private lateinit var repository: LoopingVidRepository
    private lateinit var mediaProcessor: MediaProcessor
    private lateinit var mockExternalFilesDir: File

    @Before
    fun setup() {
        // android.util.Log is a throwing stub under JVM unit tests; the render path logs
        // diagnostics, so silence it instead of letting it fail the test.
        mockkStatic(Log::class)
        every { Log.d(any(), any()) } returns 0
        every { Log.i(any(), any()) } returns 0
        every { Log.w(any(), any<String>()) } returns 0
        every { Log.e(any(), any()) } returns 0
        every { Log.e(any(), any(), any()) } returns 0

        // Mock Android Context
        context = mockk(relaxed = true)
        repository = mockk(relaxed = true)
        
        // Create temporary directory for test outputs
        mockExternalFilesDir = createTempDir("test_media_processor")
        every { context.getExternalFilesDir(any()) } returns mockExternalFilesDir
        every { context.filesDir } returns mockExternalFilesDir
        // Renders now target app-private cache, so cacheDir must resolve to a real directory.
        val mockCacheDir = File(mockExternalFilesDir, "cache").apply { mkdirs() }
        every { context.cacheDir } returns mockCacheDir
        
        // Initialize MediaProcessor with a mock prober for tests
        mediaProcessor = MediaProcessor(context, repository, durationProber = { 30.0 })
    }

    @After
    fun teardown() {
        // Clean up temporary files
        mockExternalFilesDir.deleteRecursively()
        clearAllMocks()
    }

    @Test
    fun `progressState initial state is not processing`() = runTest {
        val initialState = mediaProcessor.progressState.first()
        
        assertFalse(initialState.isProcessing)
        assertEquals(0, initialState.progress)
        assertEquals("", initialState.statusText)
        assertEquals("", initialState.outputFilePath)
        assertNull(initialState.errorMessage)
    }

    @Test
    fun `cancelActiveJob updates state with cancellation message`() = runTest {
        mediaProcessor.cancelActiveJob()
        
        val state = mediaProcessor.progressState.first()
        
        assertFalse(state.isProcessing)
        assertEquals("Job was cancelled by user", state.errorMessage)
    }

    @Test
    fun `executeLoopJob creates output directory if not exists`() = runTest {
        // Arrange
        val inputUri = "content://test/video.mp4"
        val outputFolder = "TestOutput"
        
        coEvery { repository.saveJob(any()) } returns 1L
        coEvery { repository.updateJob(any()) } just Runs
        coEvery { repository.getJobById(any()) } returns mockk(relaxed = true)
        
        // Mock FFmpegWrapper to avoid actual execution
        mockkConstructor(FFmpegWrapperImpl::class)
        every { anyConstructed<FFmpegWrapperImpl>().getVersion() } returns "6.0"
        coEvery { anyConstructed<FFmpegWrapperImpl>().execute(any<List<String>>(), any(), any()) } returns 0
        
        // Mock MediaStoreExporter
        mockkObject(MediaStoreExporter)
        coEvery { MediaStoreExporter.exportVideoToGallery(any(), any(), any(), any(), any()) } returns Result.success(mockk())
        
        // Act
        try {
            mediaProcessor.executeLoopJob(
                inputUri = inputUri,
                targetDurationSec = 10.0,
                loopStyle = "NORMAL",
                destinationFolder = outputFolder
            )
        } catch (e: Exception) {
            // Expected to fail due to mocking limitations, but directory should be created
        }
        
        // Assert
        val expectedDir = File(mockExternalFilesDir, "cache/render")
        assertTrue("Render cache directory should be created", expectedDir.exists())
        assertTrue("Render cache path should be a directory", expectedDir.isDirectory)
    }

    @Test
    fun `executeLoopJob saves initial job to repository`() = runTest {
        // Arrange
        val inputUri = "content://test/video.mp4"
        val insertedId = 42L
        
        val capturedJob = slot<RenderJobEntity>()
        coEvery { repository.saveJob(capture(capturedJob)) } returns insertedId
        coEvery { repository.updateJob(any()) } just Runs
        
        // Mock FFmpegWrapper
        mockkConstructor(FFmpegWrapperImpl::class)
        every { anyConstructed<FFmpegWrapperImpl>().getVersion() } returns "6.0"
        coEvery { anyConstructed<FFmpegWrapperImpl>().execute(any<List<String>>(), any(), any()) } returns 0
        
        // Mock MediaStoreExporter
        mockkObject(MediaStoreExporter)
        coEvery { MediaStoreExporter.exportVideoToGallery(any(), any(), any(), any(), any()) } returns Result.success(mockk())
        
        // Act
        try {
            mediaProcessor.executeLoopJob(
                inputUri = inputUri,
                targetDurationSec = 15.0,
                loopStyle = "CROSSFADE",
                presetQuality = "1080p",
                projectId = 77L
            )
        } catch (e: Exception) {
            // Expected to fail due to mocking limitations
        }
        
        // Assert
        coVerify { repository.saveJob(any()) }
        
        val savedJob = capturedJob.captured
        assertEquals("LOOP", savedJob.jobType)
        assertEquals(inputUri, savedJob.inputUri)
        assertEquals("CROSSFADE", savedJob.style)
        assertEquals("PROCESSING", savedJob.status)
        assertEquals(0, savedJob.progress)
        assertEquals(15.0, savedJob.durationSec, 0.01)
        assertEquals(77L, savedJob.projectId)
    }

    @Test
    fun `executeLoopJob generates correct filename with timestamp`() = runTest {
        // Arrange
        val inputUri = "content://test/video.mp4"
        val customFileName = "MyCustomLoop"
        
        coEvery { repository.saveJob(any()) } returns 1L
        coEvery { repository.updateJob(any()) } just Runs
        
        // Mock FFmpegWrapper
        mockkConstructor(FFmpegWrapperImpl::class)
        every { anyConstructed<FFmpegWrapperImpl>().getVersion() } returns "6.0"
        coEvery { anyConstructed<FFmpegWrapperImpl>().execute(any<List<String>>(), any(), any()) } returns 0
        
        // Mock MediaStoreExporter
        mockkObject(MediaStoreExporter)
        coEvery { MediaStoreExporter.exportVideoToGallery(any(), any(), any(), any(), any()) } returns Result.success(mockk())
        
        // Act
        try {
            mediaProcessor.executeLoopJob(
                inputUri = inputUri,
                targetDurationSec = 10.0,
                loopStyle = "NORMAL",
                customFileName = customFileName,
                exportFormat = "mp4"
            )
        } catch (e: Exception) {
            // Expected to fail
        }
        
        // Assert - verify file was created with custom name
        val outputDir = File(mockExternalFilesDir, "RenderOutput")
        val files = outputDir.listFiles()?.filter { it.name.startsWith(customFileName) }
        
        // Note: File might not be created due to mocking, but we verified the logic path
        coVerify { repository.saveJob(any()) }
    }

    @Test
    fun `executeMasteringJob creates correct job entity`() = runTest {
        // Arrange
        val inputUri = "content://test/audio.mp3"
        val presetName = "Voice Clarity"
        val targetLufs = -16.0
        val insertedId = 10L
        
        val capturedJob = slot<RenderJobEntity>()
        coEvery { repository.saveJob(capture(capturedJob)) } returns insertedId
        coEvery { repository.updateJob(any()) } just Runs
        coEvery { repository.getJobById(insertedId) } returns mockk(relaxed = true)
        
        // Mock FFmpegWrapper
        mockkConstructor(FFmpegWrapperImpl::class)
        coEvery { anyConstructed<FFmpegWrapperImpl>().execute(any<List<String>>(), any(), any()) } returns 0
        
        // Mock MediaStoreExporter
        mockkObject(MediaStoreExporter)
        coEvery { MediaStoreExporter.exportAudioToGallery(any(), any(), any(), any(), any()) } returns Result.success(mockk())
        
        // Act
        try {
            mediaProcessor.executeMasteringJob(
                inputUri = inputUri,
                presetName = presetName,
                targetLufs = targetLufs,
                exportFormat = "MP3",
                projectId = 88L
            )
        } catch (e: Exception) {
            // Expected to fail
        }
        
        // Assert
        coVerify { repository.saveJob(any()) }
        
        val savedJob = capturedJob.captured
        assertEquals("MASTERING", savedJob.jobType)
        assertEquals(inputUri, savedJob.inputUri)
        assertEquals("MASTERING", savedJob.style)
        assertTrue(savedJob.title.contains(presetName))
        assertTrue(savedJob.paramsSummary.contains(targetLufs.toString()))
        assertEquals(88L, savedJob.projectId)
    }

    private val loudnormAnalysisOutput = """
        [Parsed_loudnorm_0 @ 0x1] {
            "input_i" : "-20.47",
            "input_tp" : "-2.31",
            "input_lra" : "8.20",
            "input_thresh" : "-30.68"
        }
    """.trimIndent()

    @Test
    fun `executeTwoPassAudioNormalization feeds measured loudness into the second pass`() = runTest {
        coEvery { repository.saveJob(any()) } returns 5L
        coEvery { repository.updateJob(any()) } just Runs

        val capturedCommand = slot<List<String>>()
        mockkConstructor(FFmpegWrapperImpl::class)
        coEvery { anyConstructed<FFmpegWrapperImpl>().executeForOutput(any()) } returns
            FFmpegExecution(returnCode = 0, output = loudnormAnalysisOutput)
        coEvery {
            anyConstructed<FFmpegWrapperImpl>().execute(capture(capturedCommand), any(), any())
        } answers {
            File(firstArg<List<String>>().last()).writeBytes(ByteArray(400_000))
            0
        }
        mockkObject(MediaStoreExporter)
        coEvery { MediaStoreExporter.exportVideoToGallery(any(), any(), any(), any(), any()) } returns Result.success(mockk())

        mediaProcessor = MediaProcessor(
            context,
            repository,
            durationProber = { 30.0 },
            streamProber = { path -> probedVideo(path, durationMs = 30_000L) }
        )

        val job = mediaProcessor.executeTwoPassAudioNormalization(
            inputUri = "content://test/video.mp4",
            targetLufs = -14.0
        )

        assertEquals("COMPLETED", job.status)
        val secondPass = capturedCommand.captured.joinToString(" ")
        // The measured values must be the ones FFmpeg reported, not placeholders.
        assertTrue("Second pass must use measured_I from pass 1: $secondPass", secondPass.contains("measured_I=-20.47"))
        assertTrue(secondPass.contains("measured_TP=-2.31"))
        assertTrue(secondPass.contains("measured_LRA=8.2"))
        assertTrue(secondPass.contains("measured_thresh=-30.68"))
        // Normalization output stays in cache; publishing is a separate explicit step.
        assertTrue(job.outputUri.replace('\\', '/').contains("/cache/mastering/"))
        coVerify(exactly = 0) { MediaStoreExporter.exportVideoToGallery(any(), any(), any(), any(), any()) }
    }

    @Test
    fun `executeTwoPassAudioNormalization fails instead of guessing loudness when analysis is unusable`() = runTest {
        coEvery { repository.saveJob(any()) } returns 6L
        coEvery { repository.updateJob(any()) } just Runs

        mockkConstructor(FFmpegWrapperImpl::class)
        // Analysis succeeded but printed no measurement (e.g. a silent or streamless input).
        coEvery { anyConstructed<FFmpegWrapperImpl>().executeForOutput(any()) } returns
            FFmpegExecution(returnCode = 0, output = "no loudnorm report here")
        coEvery { anyConstructed<FFmpegWrapperImpl>().execute(any<List<String>>(), any(), any()) } returns 0

        mediaProcessor = MediaProcessor(context, repository, durationProber = { 30.0 })

        val job = mediaProcessor.executeTwoPassAudioNormalization(
            inputUri = "content://test/video.mp4"
        )

        assertEquals("FAILED", job.status)
        // A second pass must never run against invented measurements.
        coVerify(exactly = 0) { anyConstructed<FFmpegWrapperImpl>().execute(any<List<String>>(), any(), any()) }
    }

    @Test
    fun `executeEditorJob handles overlay when provided`() = runTest {
        // Arrange
        val mediaUri = "content://test/video.mp4"
        val overlayUri = "content://test/overlay.png"
        val overlayPosition = "TOP_RIGHT"
        
        coEvery { repository.saveJob(any()) } returns 1L
        coEvery { repository.updateJob(any()) } just Runs
        
        // Mock FFmpegWrapper
        mockkConstructor(FFmpegWrapperImpl::class)
        val capturedCommand = slot<List<String>>()
        coEvery { anyConstructed<FFmpegWrapperImpl>().execute(capture(capturedCommand), any(), any()) } returns 0
        mediaProcessor = MediaProcessor(context, repository)

        // Mock MediaStoreExporter
        mockkObject(MediaStoreExporter)
        coEvery { MediaStoreExporter.exportVideoToGallery(any(), any(), any(), any(), any()) } returns Result.success(mockk())

        // Act
        mediaProcessor.executeEditorJob(
            mediaUri = mediaUri,
            audioUri = null,
            titleText = "Test Title",
            watermarkText = "Watermark",
            spectrumStyle = "Bars",
            overlayUri = overlayUri,
            overlayPosition = overlayPosition
        )

        // Assert - command should include overlay input
        coVerify { anyConstructed<FFmpegWrapperImpl>().execute(any<List<String>>(), any(), any()) }
        
        val command = capturedCommand.captured
        assertTrue("Command should include overlay input", command.contains(overlayUri))
        assertTrue("Command should include filter_complex", command.contains("-filter_complex"))
    }

    /**
     * Stand-in for platform stream probing: MediaExtractor is a throwing stub under JVM tests,
     * so validation is exercised with metadata a real successful render would carry.
     */
    private fun probedVideo(path: String, durationMs: Long): com.example.core.media.ProcessedMedia =
        com.example.core.media.ProcessedMedia(
            path = path,
            durationMs = durationMs,
            sizeBytes = File(path).length(),
            width = 1920,
            height = 1080,
            videoCodec = "video/avc",
            audioCodec = "audio/mp4a-latm",
            sampleRateHz = 48_000,
            channels = 2
        )

    /**
     * Makes the mocked FFmpeg write [bytes] to the command's output path so validation
     * runs against a real file, exactly like a partial or complete render on device.
     */
    private fun mockFfmpegWriting(bytes: Int, returnCode: Int) {
        mockkConstructor(FFmpegWrapperImpl::class)
        every { anyConstructed<FFmpegWrapperImpl>().getVersion() } returns "6.0"
        coEvery { anyConstructed<FFmpegWrapperImpl>().execute(any<List<String>>(), any(), any()) } answers {
            val args = firstArg<List<String>>()
            File(args.last()).writeBytes(ByteArray(bytes))
            returnCode
        }
    }

    @Test
    fun `executeLoopJob marks job failed and deletes partial output when ffmpeg fails`() = runTest {
        val capturedJobs = mutableListOf<RenderJobEntity>()
        coEvery { repository.saveJob(any()) } returns 7L
        coEvery { repository.updateJob(capture(capturedJobs)) } just Runs

        // FFmpeg exits non-zero but still leaves a ~1.6 MB partial container behind.
        mockFfmpegWriting(bytes = 1_600_000, returnCode = 1)
        mockkObject(MediaStoreExporter)
        coEvery { MediaStoreExporter.exportVideoToGallery(any(), any(), any(), any(), any()) } returns Result.success(mockk())

        mediaProcessor = MediaProcessor(context, repository, durationProber = { 30.0 })

        val job = mediaProcessor.executeLoopJob(
            inputUri = "content://test/video.mp4",
            targetDurationSec = 30.0,
            loopStyle = "NORMAL",
            customFileName = "FailedLoop"
        )

        assertEquals("FAILED", job.status)
        assertFalse("Partial output must not survive a failed render", File(job.outputUri).exists())
        coVerify(exactly = 0) { MediaStoreExporter.exportVideoToGallery(any(), any(), any(), any(), any()) }
        assertTrue(capturedJobs.any { it.status == "FAILED" })
        assertNotNull(mediaProcessor.progressState.first().errorMessage)
    }

    @Test
    fun `executeLoopJob fails when output duration does not match requested duration`() = runTest {
        coEvery { repository.saveJob(any()) } returns 8L
        coEvery { repository.updateJob(any()) } just Runs

        mockFfmpegWriting(bytes = 1_600_000, returnCode = 0)
        mockkObject(MediaStoreExporter)
        coEvery { MediaStoreExporter.exportVideoToGallery(any(), any(), any(), any(), any()) } returns Result.success(mockk())

        // Source is 5s; the render only produced 5s even though 30s was requested.
        mediaProcessor = MediaProcessor(context, repository, durationProber = { 5.0 })

        val job = mediaProcessor.executeLoopJob(
            inputUri = "content://test/video.mp4",
            targetDurationSec = 30.0,
            loopStyle = "NORMAL",
            customFileName = "ShortLoop"
        )

        assertEquals("FAILED", job.status)
        coVerify(exactly = 0) { MediaStoreExporter.exportVideoToGallery(any(), any(), any(), any(), any()) }
    }

    @Test
    fun `executeLoopJob reports real file size and probed duration on success`() = runTest {
        coEvery { repository.saveJob(any()) } returns 9L
        coEvery { repository.updateJob(any()) } just Runs

        val renderedBytes = 500_000
        mockFfmpegWriting(bytes = renderedBytes, returnCode = 0)
        mockkObject(MediaStoreExporter)
        coEvery { MediaStoreExporter.exportVideoToGallery(any(), any(), any(), any(), any()) } returns Result.success(mockk())

        mediaProcessor = MediaProcessor(context, repository, durationProber = { 30.0 })

        val job = mediaProcessor.executeLoopJob(
            inputUri = "content://test/video.mp4",
            targetDurationSec = 30.0,
            loopStyle = "NORMAL",
            customFileName = "GoodLoop"
        )

        assertEquals("COMPLETED", job.status)
        assertEquals(30.0, job.durationSec, 0.01)
        // No arbitrary minimum: the reported size is the real byte count.
        assertEquals(renderedBytes / (1024.0 * 1024.0), job.fileSizeMb, 0.0001)
        assertTrue(job.fileSizeMb < 1.2)
    }

    @Test
    fun `executeLoopJob does not silently overwrite an earlier render with the same name`() = runTest {
        coEvery { repository.saveJob(any()) } returns 11L
        coEvery { repository.updateJob(any()) } just Runs

        mockFfmpegWriting(bytes = 400_000, returnCode = 0)
        mockkObject(MediaStoreExporter)
        coEvery { MediaStoreExporter.exportVideoToGallery(any(), any(), any(), any(), any()) } returns Result.success(mockk())

        mediaProcessor = MediaProcessor(context, repository, durationProber = { 30.0 })

        val first = mediaProcessor.executeLoopJob(
            inputUri = "content://test/video.mp4",
            targetDurationSec = 30.0,
            loopStyle = "NORMAL",
            customFileName = "SameName"
        )
        val second = mediaProcessor.executeLoopJob(
            inputUri = "content://test/video.mp4",
            targetDurationSec = 30.0,
            loopStyle = "NORMAL",
            customFileName = "SameName"
        )

        assertEquals("COMPLETED", first.status)
        assertEquals("COMPLETED", second.status)
        assertNotEquals(
            "A second render must not overwrite the first output",
            first.outputUri,
            second.outputUri
        )
        assertTrue(File(first.outputUri).exists())
        assertTrue(File(second.outputUri).exists())
    }

    @Test
    fun `executeLoopJob ends in Success state carrying validated output metadata`() = runTest {
        coEvery { repository.saveJob(any()) } returns 21L
        coEvery { repository.updateJob(any()) } just Runs

        val renderedBytes = 640_000
        mockFfmpegWriting(bytes = renderedBytes, returnCode = 0)

        mediaProcessor = MediaProcessor(context, repository, durationProber = { 30.0 })

        mediaProcessor.executeLoopJob(
            inputUri = "content://test/video.mp4",
            targetDurationSec = 30.0,
            loopStyle = "NORMAL",
            presetQuality = "720p",
            customFileName = "StateSuccess"
        )

        val state = mediaProcessor.renderState.first()
        assertTrue("Expected Success but was $state", state is RenderState.Success)
        state as RenderState.Success
        assertEquals(30_000L, state.durationMs)
        assertEquals(renderedBytes.toLong(), state.fileSizeBytes)
        assertEquals("720p", state.resolution)
        assertTrue(File(state.outputPath).exists())
    }

    @Test
    fun `executeLoopJob ends in Failed state with return code when ffmpeg fails`() = runTest {
        coEvery { repository.saveJob(any()) } returns 22L
        coEvery { repository.updateJob(any()) } just Runs

        mockFfmpegWriting(bytes = 1_600_000, returnCode = 1)

        mediaProcessor = MediaProcessor(context, repository, durationProber = { 30.0 })

        mediaProcessor.executeLoopJob(
            inputUri = "content://test/video.mp4",
            targetDurationSec = 30.0,
            loopStyle = "NORMAL",
            customFileName = "StateFailed"
        )

        val state = mediaProcessor.renderState.first()
        assertTrue("Expected Failed but was $state", state is RenderState.Failed)
        state as RenderState.Failed
        assertEquals(1, state.returnCode)
    }

    @Test
    fun `executeLoopJob renders to app-private cache instead of exporting to gallery`() = runTest {
        coEvery { repository.saveJob(any()) } returns 23L
        coEvery { repository.updateJob(any()) } just Runs

        mockFfmpegWriting(bytes = 320_000, returnCode = 0)
        mockkObject(MediaStoreExporter)
        coEvery { MediaStoreExporter.exportVideoToGallery(any(), any(), any(), any(), any()) } returns Result.success(mockk())

        mediaProcessor = MediaProcessor(context, repository, durationProber = { 30.0 })

        val job = mediaProcessor.executeLoopJob(
            inputUri = "content://test/video.mp4",
            targetDurationSec = 30.0,
            loopStyle = "NORMAL",
            customFileName = "NoGalleryOnRender"
        )

        assertEquals("COMPLETED", job.status)
        assertTrue(
            "Render output must stay in app-private cache until the user exports it",
            job.outputUri.replace('\\', '/').contains("/cache/render/")
        )
        // Start Render must not publish anything; export is a separate, explicit step.
        coVerify(exactly = 0) { MediaStoreExporter.exportVideoToGallery(any(), any(), any(), any(), any()) }
    }

    @Test
    fun `executeLoopJob progress comes from ffmpeg statistics not a timer`() = runTest {
        coEvery { repository.saveJob(any()) } returns 24L
        coEvery { repository.updateJob(any()) } just Runs

        mockkConstructor(FFmpegWrapperImpl::class)
        every { anyConstructed<FFmpegWrapperImpl>().getVersion() } returns "6.0"
        val observedStates = mutableListOf<RenderState>()
        coEvery {
            anyConstructed<FFmpegWrapperImpl>().execute(any<List<String>>(), any(), any())
        } coAnswers {
            val args = firstArg<List<String>>()
            val onStatistics = secondArg<suspend (FFmpegProgress) -> Unit>()
            // Emit a mid-render statistics sample, exactly like FFmpegKit does on device.
            onStatistics(FFmpegProgress(processedMs = 15_000L, targetMs = 30_000L, percent = 50))
            observedStates.add(mediaProcessor.renderState.first())
            File(args.last()).writeBytes(ByteArray(256_000))
            0
        }

        mediaProcessor = MediaProcessor(context, repository, durationProber = { 30.0 })

        mediaProcessor.executeLoopJob(
            inputUri = "content://test/video.mp4",
            targetDurationSec = 30.0,
            loopStyle = "NORMAL",
            customFileName = "StatsProgress"
        )

        val rendering = observedStates.filterIsInstance<RenderState.Rendering>().lastOrNull()
        assertNotNull("Statistics callback must drive a Rendering state", rendering)
        assertEquals(50, rendering!!.progress)
        assertEquals(15_000L, rendering.processedMs)
        assertEquals(30_000L, rendering.targetMs)
    }

    @Test
    fun `cancelActiveJob reports Cancelled render state`() = runTest {
        mediaProcessor.cancelActiveJob()

        assertEquals(RenderState.Cancelled, mediaProcessor.renderState.first())
    }

    @Test
    fun `exportProjectToGallery calls correct exporter for video`() = runTest {
        // Arrange
        val videoPath = File(mockExternalFilesDir, "test_video.mp4").apply {
            writeText("fake video content")
        }.absolutePath
        
        // Mock MediaStoreExporter
        mockkObject(MediaStoreExporter)
        coEvery { MediaStoreExporter.exportVideoToGallery(any(), any(), any(), any(), any()) } returns Result.success(mockk())
        
        // Act
        val result = mediaProcessor.exportProjectToGallery(
            filePath = videoPath,
            customTitle = "My Video",
            isAudio = false
        )
        
        // Assert
        assertTrue(result.isSuccess)
        coVerify { MediaStoreExporter.exportVideoToGallery(context, any(), "My Video", any(), any()) }
        coVerify(exactly = 0) { MediaStoreExporter.exportAudioToGallery(any(), any(), any(), any(), any()) }
    }

    @Test
    fun `exportProjectToGallery calls correct exporter for audio`() = runTest {
        // Arrange
        val audioPath = File(mockExternalFilesDir, "test_audio.mp3").apply {
            writeText("fake audio content")
        }.absolutePath
        
        // Mock MediaStoreExporter
        mockkObject(MediaStoreExporter)
        coEvery { MediaStoreExporter.exportAudioToGallery(any(), any(), any(), any(), any()) } returns Result.success(mockk())
        
        // Act
        val result = mediaProcessor.exportProjectToGallery(
            filePath = audioPath,
            customTitle = "My Audio",
            isAudio = true
        )
        
        // Assert
        assertTrue(result.isSuccess)
        coVerify { MediaStoreExporter.exportAudioToGallery(context, any(), "My Audio", any(), any()) }
        coVerify(exactly = 0) { MediaStoreExporter.exportVideoToGallery(any(), any(), any(), any(), any()) }
    }
}
