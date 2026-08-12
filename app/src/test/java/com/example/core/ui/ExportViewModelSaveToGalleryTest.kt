package com.example.core.ui

import android.net.Uri
import com.example.core.ffmpeg.ExportState
import com.example.core.ffmpeg.MediaProcessor
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
 * Export is a separate, explicit step that publishes the already-validated cache output.
 * These guards pin that behaviour: no FFmpeg re-run, and nothing published before a
 * successful render produced a summary.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ExportViewModelSaveToGalleryTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var processor: MediaProcessor
    private lateinit var viewModel: ExportViewModel

    private val cachePath = "/data/user/0/com.example/cache/mastering/master_ab12cd34.mp3"

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        processor = mockk(relaxed = true)
        viewModel = ExportViewModel(processor)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun audioSummary(path: String = cachePath) = ExportSummaryData(
        fileName = "MasteredAudio",
        filePath = path,
        fileSizeMb = 4.2,
        durationSec = 30.0,
        format = "mp3",
        jobType = "MASTERING",
        isVideo = false
    )

    @Test
    fun `saveCompletedExportToGallery publishes the validated cache output without re-rendering`() =
        runTest(testDispatcher) {
            val galleryUri = mockk<Uri>()
            coEvery { processor.exportProjectToGallery(cachePath, any(), true) } returns
                Result.success(galleryUri)
            viewModel.showSummaryForJob(
                title = "MasteredAudio",
                filePath = cachePath,
                format = "mp3",
                jobType = "MASTERING"
            )

            viewModel.saveCompletedExportToGallery()
            advanceUntilIdle()

            coVerify(exactly = 1) { processor.exportProjectToGallery(cachePath, any(), true) }
            assertEquals(
                ExportState.Success(galleryUri),
                viewModel.uiState.value.galleryExportState
            )
        }

    @Test
    fun `saveCompletedExportToGallery routes video output to the video exporter`() =
        runTest(testDispatcher) {
            val videoPath = "/data/user/0/com.example/cache/editor/editor_ff00.mp4"
            coEvery { processor.exportProjectToGallery(videoPath, any(), false) } returns
                Result.success(mockk())
            viewModel.showSummaryForJob(
                title = "EditorProject",
                filePath = videoPath,
                format = "mp4",
                jobType = "EDITOR"
            )

            viewModel.saveCompletedExportToGallery()
            advanceUntilIdle()

            coVerify(exactly = 1) { processor.exportProjectToGallery(videoPath, any(), false) }
        }

    @Test
    fun `saveCompletedExportToGallery does nothing when no validated output exists`() =
        runTest(testDispatcher) {
            viewModel.saveCompletedExportToGallery()
            advanceUntilIdle()

            coVerify(exactly = 0) { processor.exportProjectToGallery(any(), any(), any()) }
            assertEquals(ExportState.Idle, viewModel.uiState.value.galleryExportState)
        }

    @Test
    fun `saveCompletedExportToGallery surfaces a failure instead of claiming success`() =
        runTest(testDispatcher) {
            coEvery { processor.exportProjectToGallery(any(), any(), any()) } returns
                Result.failure(IllegalStateException("MediaStore insert rejected"))
            viewModel.showSummaryForJob(
                title = "MasteredAudio",
                filePath = cachePath,
                format = "mp3",
                jobType = "MASTERING"
            )

            viewModel.saveCompletedExportToGallery()
            advanceUntilIdle()

            val state = viewModel.uiState.value.galleryExportState
            assertTrue("Expected Failed but was $state", state is ExportState.Failed)
            assertTrue((state as ExportState.Failed).message.isNotBlank())
        }

    @Test
    fun `dismissSummary clears any previous gallery export result`() = runTest(testDispatcher) {
        coEvery { processor.exportProjectToGallery(any(), any(), any()) } returns
            Result.success(mockk())
        viewModel.showSummaryForJob(
            title = "MasteredAudio",
            filePath = cachePath,
            format = "mp3",
            jobType = "MASTERING"
        )
        viewModel.saveCompletedExportToGallery()
        advanceUntilIdle()

        viewModel.dismissSummary()

        assertEquals(ExportState.Idle, viewModel.uiState.value.galleryExportState)
    }
}
