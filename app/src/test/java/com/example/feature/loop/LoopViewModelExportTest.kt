package com.example.feature.loop

import android.net.Uri
import com.example.core.ffmpeg.ExportState
import com.example.core.ffmpeg.JobProgressState
import com.example.core.ffmpeg.MediaProcessor
import com.example.core.ffmpeg.RenderState
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Regression guards for loop export (save-to-gallery) flow:
 * - Save uses the already-rendered temp path from RenderState.Success.
 * - Save never re-invokes FFmpeg / executeLoopJob.
 * - ExportState lifecycle is independent of RenderState.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class LoopViewModelExportTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var processor: MediaProcessor
    private lateinit var renderStateFlow: MutableStateFlow<RenderState>
    private lateinit var progressStateFlow: MutableStateFlow<JobProgressState>

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        renderStateFlow = MutableStateFlow(RenderState.Idle)
        progressStateFlow = MutableStateFlow(JobProgressState())
        processor = mockk(relaxed = true)
        every { processor.renderState } returns renderStateFlow
        every { processor.progressState } returns progressStateFlow
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(): LoopViewModel {
        return LoopViewModel(processor)
    }

    @Test
    fun `saveRenderToGallery calls exportProjectToGallery with success output path`() = runTest(testDispatcher) {
        val viewModel = createViewModel()

        // Simulate a completed render
        val outputPath = "/data/data/com.example/cache/render/loop_render_abc12345.mp4"
        renderStateFlow.value = RenderState.Success(
            outputPath = outputPath,
            durationMs = 30_000L,
            fileSizeBytes = 5_000_000L,
            resolution = "1080p"
        )
        advanceUntilIdle()

        val fakeUri = mockk<Uri>()
        coEvery { processor.exportProjectToGallery(outputPath, null, false) } answers {
            Result.success(fakeUri)
        }

        viewModel.saveRenderToGallery()
        advanceUntilIdle()

        coVerify(exactly = 1) { processor.exportProjectToGallery(outputPath, null, false) }
        val state = viewModel.uiState.value.exportState
        assertTrue("Expected ExportState.Success but was $state", state is ExportState.Success)
        assertEquals(fakeUri, (state as ExportState.Success).uri)
    }

    @Test
    fun `saveRenderToGallery does not invoke executeLoopJob`() = runTest(testDispatcher) {
        val viewModel = createViewModel()

        val outputPath = "/data/data/com.example/cache/render/loop_render_xyz99999.mp4"
        renderStateFlow.value = RenderState.Success(
            outputPath = outputPath,
            durationMs = 60_000L,
            fileSizeBytes = 10_000_000L,
            resolution = "1080p"
        )
        advanceUntilIdle()

        coEvery { processor.exportProjectToGallery(any(), any(), any()) } answers { Result.success(mockk()) }

        viewModel.saveRenderToGallery()
        advanceUntilIdle()

        coVerify(exactly = 0) {
            processor.executeLoopJob(
                inputUri = any(),
                targetDurationSec = any(),
                loopStyle = any(),
                crossfadeDurationSec = any(),
                trimStartSec = any(),
                trimEndSec = any(),
                muteAudio = any(),
                audioFadeInSec = any(),
                audioFadeOutSec = any(),
                presetQuality = any(),
                customFileName = any(),
                destinationFolder = any(),
                exportFormat = any(),
                resolution = any(),
                frameRate = any(),
                bitrate = any(),
                aspectRatio = any(),
                projectId = any()
            )
        }
    }

    @Test
    fun `saveRenderToGallery does nothing when render state is not Success`() = runTest(testDispatcher) {
        val viewModel = createViewModel()

        // State is Idle (no render completed)
        viewModel.saveRenderToGallery()
        advanceUntilIdle()

        coVerify(exactly = 0) { processor.exportProjectToGallery(any(), any(), any()) }
        assertEquals(ExportState.Idle, viewModel.uiState.value.exportState)
    }

    @Test
    fun `saveRenderToGallery sets Failed state on export error`() = runTest(testDispatcher) {
        val viewModel = createViewModel()

        val outputPath = "/data/data/com.example/cache/render/loop_render_err00001.mp4"
        renderStateFlow.value = RenderState.Success(
            outputPath = outputPath,
            durationMs = 30_000L,
            fileSizeBytes = 5_000_000L,
            resolution = "1080p"
        )
        advanceUntilIdle()

        coEvery { processor.exportProjectToGallery(outputPath, null, false) } answers {
            Result.failure(RuntimeException("MediaStore write denied"))
        }

        viewModel.saveRenderToGallery()
        advanceUntilIdle()

        val state = viewModel.uiState.value.exportState
        assertTrue("Expected ExportState.Failed but was $state", state is ExportState.Failed)
        assertTrue(
            "Expected error message to contain 'MediaStore write denied'",
            (state as ExportState.Failed).message.contains("MediaStore write denied")
        )
    }

    @Test
    fun `dismissExportState resets to Idle`() = runTest(testDispatcher) {
        val viewModel = createViewModel()

        val outputPath = "/data/data/com.example/cache/render/loop_render_dismiss1.mp4"
        renderStateFlow.value = RenderState.Success(
            outputPath = outputPath,
            durationMs = 30_000L,
            fileSizeBytes = 5_000_000L,
            resolution = "1080p"
        )
        advanceUntilIdle()

        coEvery { processor.exportProjectToGallery(any(), any(), any()) } answers { Result.success(mockk()) }
        viewModel.saveRenderToGallery()
        advanceUntilIdle()

        // Verify not idle before dismissing
        assertTrue(viewModel.uiState.value.exportState is ExportState.Success)

        viewModel.dismissExportState()

        assertEquals(ExportState.Idle, viewModel.uiState.value.exportState)
    }
}
