package com.example.core.ffmpeg

import android.content.Context
import android.util.Log
import com.example.core.database.LoopingVidRepository
import com.example.core.database.RenderJobEntity
import com.example.core.media.ProcessedMedia
import com.example.core.utils.MediaStoreExporter
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.io.File

/**
 * Regression suite exercising the Mastering and Editor pipelines with an injected
 * [streamProber], so ProcessedMediaValidator actually runs against controlled metadata.
 *
 * Guards against: partial output accepted as success, missing audio stream silently passing,
 * gallery export happening during render, and fake progress.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MediaProcessorPipelineValidationTest {

    private lateinit var context: Context
    private lateinit var repository: LoopingVidRepository
    private lateinit var mockCacheDir: File
    private lateinit var rootDir: File

    @Before
    fun setup() {
        mockkStatic(Log::class)
        every { Log.d(any(), any()) } returns 0
        every { Log.i(any(), any()) } returns 0
        every { Log.w(any(), any<String>()) } returns 0
        every { Log.e(any(), any()) } returns 0
        every { Log.e(any(), any(), any()) } returns 0

        context = mockk(relaxed = true)
        repository = mockk(relaxed = true)

        rootDir = createTempDir("pipeline_validation_test")
        mockCacheDir = File(rootDir, "cache").apply { mkdirs() }
        every { context.getExternalFilesDir(any()) } returns rootDir
        every { context.filesDir } returns rootDir
        every { context.cacheDir } returns mockCacheDir
    }

    @After
    fun teardown() {
        rootDir.deleteRecursively()
        clearAllMocks()
    }

    // ─────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────

    private fun probedAudio(path: String, durationMs: Long, sizeBytes: Long): ProcessedMedia =
        ProcessedMedia(
            path = path,
            durationMs = durationMs,
            sizeBytes = sizeBytes,
            audioCodec = "audio/mp4a-latm",
            sampleRateHz = 44_100,
            channels = 2
        )

    private fun probedAudioMissingStream(path: String, durationMs: Long, sizeBytes: Long): ProcessedMedia =
        ProcessedMedia(
            path = path,
            durationMs = durationMs,
            sizeBytes = sizeBytes,
            audioCodec = null,
            sampleRateHz = 0,
            channels = 0
        )

    private fun probedVideo(path: String, durationMs: Long, sizeBytes: Long): ProcessedMedia =
        ProcessedMedia(
            path = path,
            durationMs = durationMs,
            sizeBytes = sizeBytes,
            width = 1920,
            height = 1080,
            videoCodec = "video/avc",
            audioCodec = "audio/mp4a-latm",
            sampleRateHz = 48_000,
            channels = 2
        )

    private fun probedVideoMissingStream(path: String, durationMs: Long, sizeBytes: Long): ProcessedMedia =
        ProcessedMedia(
            path = path,
            durationMs = durationMs,
            sizeBytes = sizeBytes,
            width = 0,
            height = 0,
            videoCodec = null,
            audioCodec = "audio/mp4a-latm",
            sampleRateHz = 48_000,
            channels = 2
        )

    private fun mockFfmpegWriting(bytes: Int, returnCode: Int) {
        mockkConstructor(FFmpegWrapperImpl::class)
        every { anyConstructed<FFmpegWrapperImpl>().getVersion() } returns "6.0"
        coEvery { anyConstructed<FFmpegWrapperImpl>().execute(any<List<String>>(), any(), any()) } answers {
            val args = firstArg<List<String>>()
            File(args.last()).writeBytes(ByteArray(bytes))
            returnCode
        }
    }

    private fun mockFfmpegWithProgress(bytes: Int, returnCode: Int, progressPercent: Int, processedMs: Long, targetMs: Long) {
        mockkConstructor(FFmpegWrapperImpl::class)
        every { anyConstructed<FFmpegWrapperImpl>().getVersion() } returns "6.0"
        coEvery { anyConstructed<FFmpegWrapperImpl>().execute(any<List<String>>(), any(), any()) } coAnswers {
            val args = firstArg<List<String>>()
            val onStatistics = secondArg<suspend (FFmpegProgress) -> Unit>()
            onStatistics(FFmpegProgress(processedMs = processedMs, targetMs = targetMs, percent = progressPercent))
            File(args.last()).writeBytes(ByteArray(bytes))
            returnCode
        }
    }

    // ─────────────────────────────────────────────────────────
    // Mastering pipeline
    // ─────────────────────────────────────────────────────────

    @Test
    fun `executeMasteringJob fails when ffmpeg returns non-zero`() = runTest {
        coEvery { repository.saveJob(any()) } returns 30L
        coEvery { repository.updateJob(any()) } just Runs

        mockFfmpegWriting(bytes = 200_000, returnCode = 1)

        val processor = MediaProcessor(
            context, repository,
            durationProber = { 60.0 },
            streamProber = { path -> probedAudio(path, 60_000L, File(path).length()) }
        )

        val job = processor.executeMasteringJob(
            inputUri = "content://test/track.mp3",
            presetName = "Voice",
            targetLufs = -16.0,
            exportFormat = "MP3"
        )

        assertEquals("FAILED", job.status)
        val state = processor.renderState.first()
        assertTrue("Expected Failed but was $state", state is RenderState.Failed)
    }

    @Test
    fun `executeMasteringJob fails when output has no audio stream`() = runTest {
        coEvery { repository.saveJob(any()) } returns 31L
        coEvery { repository.updateJob(any()) } just Runs

        mockFfmpegWriting(bytes = 200_000, returnCode = 0)

        val processor = MediaProcessor(
            context, repository,
            durationProber = { 60.0 },
            streamProber = { path -> probedAudioMissingStream(path, 60_000L, File(path).length()) }
        )

        val job = processor.executeMasteringJob(
            inputUri = "content://test/track.mp3",
            presetName = "Voice",
            targetLufs = -16.0,
            exportFormat = "MP3"
        )

        assertEquals("FAILED", job.status)
        val state = processor.renderState.first()
        assertTrue("Expected Failed but was $state", state is RenderState.Failed)
        state as RenderState.Failed
        assertTrue(
            "Failure should cite the missing audio stream, was: ${state.message}",
            state.message.contains("audio", ignoreCase = true)
        )
        assertFalse("Invalid output must be deleted", File(job.outputUri).exists())
    }

    @Test
    fun `executeMasteringJob fails when output duration drifts beyond tolerance`() = runTest {
        coEvery { repository.saveJob(any()) } returns 32L
        coEvery { repository.updateJob(any()) } just Runs

        mockFfmpegWriting(bytes = 200_000, returnCode = 0)

        // The prober reports 30s, but expected was 60s
        val processor = MediaProcessor(
            context, repository,
            durationProber = { 60.0 },
            streamProber = { path -> probedAudio(path, 30_000L, File(path).length()) }
        )

        val job = processor.executeMasteringJob(
            inputUri = "content://test/track.mp3",
            presetName = "Voice",
            targetLufs = -16.0,
            exportFormat = "MP3"
        )

        assertEquals("FAILED", job.status)
    }

    @Test
    fun `executeMasteringJob succeeds with real duration and size from prober`() = runTest {
        coEvery { repository.saveJob(any()) } returns 33L
        coEvery { repository.updateJob(any()) } just Runs

        val renderedBytes = 500_000
        mockFfmpegWriting(bytes = renderedBytes, returnCode = 0)

        val processor = MediaProcessor(
            context, repository,
            durationProber = { 60.0 },
            streamProber = { path -> probedAudio(path, 60_000L, File(path).length()) }
        )

        val job = processor.executeMasteringJob(
            inputUri = "content://test/track.mp3",
            presetName = "Voice",
            targetLufs = -16.0,
            exportFormat = "MP3"
        )

        assertEquals("COMPLETED", job.status)
        assertEquals(60.0, job.durationSec, 0.01)
        assertEquals(renderedBytes / (1024.0 * 1024.0), job.fileSizeMb, 0.0001)

        val state = processor.renderState.first()
        assertTrue("Expected Success but was $state", state is RenderState.Success)
        state as RenderState.Success
        assertEquals(60_000L, state.durationMs)
        assertEquals(renderedBytes.toLong(), state.fileSizeBytes)
    }

    @Test
    fun `executeMasteringJob does not auto-publish to gallery`() = runTest {
        coEvery { repository.saveJob(any()) } returns 34L
        coEvery { repository.updateJob(any()) } just Runs

        mockFfmpegWriting(bytes = 500_000, returnCode = 0)
        mockkObject(MediaStoreExporter)
        coEvery { MediaStoreExporter.exportAudioToGallery(any(), any(), any(), any(), any()) } returns Result.success(mockk())

        val processor = MediaProcessor(
            context, repository,
            durationProber = { 60.0 },
            streamProber = { path -> probedAudio(path, 60_000L, File(path).length()) }
        )

        val job = processor.executeMasteringJob(
            inputUri = "content://test/track.mp3",
            presetName = "Voice",
            targetLufs = -16.0,
            exportFormat = "MP3"
        )

        assertEquals("COMPLETED", job.status)
        assertTrue(
            "Mastering output must stay in app-private cache until explicit export",
            job.outputUri.replace('\\', '/').contains("/cache/mastering/")
        )
        coVerify(exactly = 0) { MediaStoreExporter.exportAudioToGallery(any(), any(), any(), any(), any()) }
    }

    @Test
    fun `executeMasteringJob progress comes from ffmpeg statistics`() = runTest {
        coEvery { repository.saveJob(any()) } returns 35L
        coEvery { repository.updateJob(any()) } just Runs

        mockFfmpegWithProgress(
            bytes = 500_000, returnCode = 0,
            progressPercent = 75, processedMs = 45_000L, targetMs = 60_000L
        )

        val observedRendering = mutableListOf<RenderState.Rendering>()
        // The wrapper is constructed inside MediaProcessor's constructor, so mocking has to be
        // registered first; the processor reference is filled in right after.
        var processorRef: MediaProcessor? = null

        mockkConstructor(FFmpegWrapperImpl::class)
        every { anyConstructed<FFmpegWrapperImpl>().getVersion() } returns "6.0"
        coEvery { anyConstructed<FFmpegWrapperImpl>().execute(any<List<String>>(), any(), any()) } coAnswers {
            val args = firstArg<List<String>>()
            val onStatistics = secondArg<suspend (FFmpegProgress) -> Unit>()
            onStatistics(FFmpegProgress(processedMs = 45_000L, targetMs = 60_000L, percent = 75))
            val current = processorRef!!.renderState.first()
            if (current is RenderState.Rendering) observedRendering.add(current)
            File(args.last()).writeBytes(ByteArray(500_000))
            0
        }

        val processor = MediaProcessor(
            context, repository,
            durationProber = { 60.0 },
            streamProber = { path -> probedAudio(path, 60_000L, File(path).length()) }
        )
        processorRef = processor

        processor.executeMasteringJob(
            inputUri = "content://test/track.mp3",
            presetName = "Voice",
            targetLufs = -16.0,
            exportFormat = "MP3"
        )

        assertTrue("At least one Rendering state must be observed", observedRendering.isNotEmpty())
        val last = observedRendering.last()
        assertEquals(75, last.progress)
        assertEquals(45_000L, last.processedMs)
        assertEquals(60_000L, last.targetMs)
    }

    // ─────────────────────────────────────────────────────────
    // Editor pipeline
    // ─────────────────────────────────────────────────────────

    @Test
    fun `executeEditorJob fails when ffmpeg returns non-zero`() = runTest {
        coEvery { repository.saveJob(any()) } returns 40L
        coEvery { repository.updateJob(any()) } just Runs

        mockFfmpegWriting(bytes = 300_000, returnCode = 1)

        val processor = MediaProcessor(
            context, repository,
            durationProber = { 30.0 },
            streamProber = { path -> probedVideo(path, 30_000L, File(path).length()) }
        )

        val job = processor.executeEditorJob(
            mediaUri = "content://test/video.mp4",
            audioUri = null,
            titleText = "Test",
            watermarkText = "",
            spectrumStyle = "None"
        )

        assertEquals("FAILED", job.status)
        val state = processor.renderState.first()
        assertTrue("Expected Failed but was $state", state is RenderState.Failed)
    }

    @Test
    fun `executeEditorJob fails when video stream is missing in output`() = runTest {
        coEvery { repository.saveJob(any()) } returns 41L
        coEvery { repository.updateJob(any()) } just Runs

        mockFfmpegWriting(bytes = 300_000, returnCode = 0)

        val processor = MediaProcessor(
            context, repository,
            durationProber = { 30.0 },
            streamProber = { path -> probedVideoMissingStream(path, 30_000L, File(path).length()) }
        )

        val job = processor.executeEditorJob(
            mediaUri = "content://test/video.mp4",
            audioUri = null,
            titleText = "Test",
            watermarkText = "",
            spectrumStyle = "None"
        )

        assertEquals("FAILED", job.status)
        val state = processor.renderState.first()
        assertTrue("Expected Failed but was $state", state is RenderState.Failed)
        state as RenderState.Failed
        assertTrue(
            "Failure should mention video stream, was: ${state.message}",
            state.message.contains("video", ignoreCase = true)
        )
    }

    @Test
    fun `executeEditorJob fails when output duration does not match expected`() = runTest {
        coEvery { repository.saveJob(any()) } returns 42L
        coEvery { repository.updateJob(any()) } just Runs

        mockFfmpegWriting(bytes = 300_000, returnCode = 0)

        // Prober reports 10s but input is 30s
        val processor = MediaProcessor(
            context, repository,
            durationProber = { 30.0 },
            streamProber = { path -> probedVideo(path, 10_000L, File(path).length()) }
        )

        val job = processor.executeEditorJob(
            mediaUri = "content://test/video.mp4",
            audioUri = null,
            titleText = "Test",
            watermarkText = "",
            spectrumStyle = "None"
        )

        assertEquals("FAILED", job.status)
    }

    @Test
    fun `executeEditorJob succeeds with real metadata from prober`() = runTest {
        coEvery { repository.saveJob(any()) } returns 43L
        coEvery { repository.updateJob(any()) } just Runs

        val renderedBytes = 600_000
        mockFfmpegWriting(bytes = renderedBytes, returnCode = 0)

        val processor = MediaProcessor(
            context, repository,
            durationProber = { 30.0 },
            streamProber = { path -> probedVideo(path, 30_000L, File(path).length()) }
        )

        val job = processor.executeEditorJob(
            mediaUri = "content://test/video.mp4",
            audioUri = null,
            titleText = "Test",
            watermarkText = "",
            spectrumStyle = "None"
        )

        assertEquals("COMPLETED", job.status)
        assertEquals(30.0, job.durationSec, 0.01)
        assertEquals(renderedBytes / (1024.0 * 1024.0), job.fileSizeMb, 0.0001)

        val state = processor.renderState.first()
        assertTrue("Expected Success but was $state", state is RenderState.Success)
        state as RenderState.Success
        assertEquals(30_000L, state.durationMs)
        assertEquals(renderedBytes.toLong(), state.fileSizeBytes)
    }

    @Test
    fun `executeEditorJob does not auto-publish to gallery`() = runTest {
        coEvery { repository.saveJob(any()) } returns 44L
        coEvery { repository.updateJob(any()) } just Runs

        mockFfmpegWriting(bytes = 600_000, returnCode = 0)
        mockkObject(MediaStoreExporter)
        coEvery { MediaStoreExporter.exportVideoToGallery(any(), any(), any(), any(), any()) } returns Result.success(mockk())

        val processor = MediaProcessor(
            context, repository,
            durationProber = { 30.0 },
            streamProber = { path -> probedVideo(path, 30_000L, File(path).length()) }
        )

        val job = processor.executeEditorJob(
            mediaUri = "content://test/video.mp4",
            audioUri = null,
            titleText = "Test",
            watermarkText = "",
            spectrumStyle = "None"
        )

        assertEquals("COMPLETED", job.status)
        assertTrue(
            "Editor output must stay in app-private cache until explicit export",
            job.outputUri.replace('\\', '/').contains("/cache/editor/")
        )
        coVerify(exactly = 0) { MediaStoreExporter.exportVideoToGallery(any(), any(), any(), any(), any()) }
    }

    @Test
    fun `executeEditorJob progress comes from ffmpeg statistics`() = runTest {
        coEvery { repository.saveJob(any()) } returns 45L
        coEvery { repository.updateJob(any()) } just Runs

        val observedRendering = mutableListOf<RenderState.Rendering>()
        var processorRef: MediaProcessor? = null

        mockkConstructor(FFmpegWrapperImpl::class)
        every { anyConstructed<FFmpegWrapperImpl>().getVersion() } returns "6.0"
        coEvery { anyConstructed<FFmpegWrapperImpl>().execute(any<List<String>>(), any(), any()) } coAnswers {
            val args = firstArg<List<String>>()
            val onStatistics = secondArg<suspend (FFmpegProgress) -> Unit>()
            onStatistics(FFmpegProgress(processedMs = 20_000L, targetMs = 30_000L, percent = 66))
            val current = processorRef!!.renderState.first()
            if (current is RenderState.Rendering) observedRendering.add(current)
            File(args.last()).writeBytes(ByteArray(600_000))
            0
        }

        val processor = MediaProcessor(
            context, repository,
            durationProber = { 30.0 },
            streamProber = { path -> probedVideo(path, 30_000L, File(path).length()) }
        )
        processorRef = processor

        processor.executeEditorJob(
            mediaUri = "content://test/video.mp4",
            audioUri = null,
            titleText = "Test",
            watermarkText = "",
            spectrumStyle = "None"
        )

        assertTrue("At least one Rendering state must be observed", observedRendering.isNotEmpty())
        val last = observedRendering.last()
        assertEquals(66, last.progress)
        assertEquals(20_000L, last.processedMs)
        assertEquals(30_000L, last.targetMs)
    }
}
