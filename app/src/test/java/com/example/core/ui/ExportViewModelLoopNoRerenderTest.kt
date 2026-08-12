package com.example.core.ui

import com.example.core.ffmpeg.MediaProcessor
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withContext
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

/**
 * Regression guard for the runtime double-encode bug: the loop output is rendered exactly once
 * into app-private cache by LoopViewModel and then published by copying that validated file.
 * The legacy export dialog path must never re-run FFmpeg for a LoopJob.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ExportViewModelLoopNoRerenderTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var processor: MediaProcessor
    private lateinit var viewModel: ExportViewModel

    private val loopConfig = ExportJobConfig.LoopJob(
        inputUri = "content://media/external/video/1",
        targetDurationSec = 60.0,
        loopStyle = "NORMAL"
    )

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

    /** Real-dispatcher grace period: confirmExport() launches on Dispatchers.IO. */
    private suspend fun awaitBackgroundWork() {
        withContext(Dispatchers.Default) { delay(300) }
    }

    @Test
    fun `confirmExport never re-runs FFmpeg for a loop job`() = runTest(testDispatcher) {
        viewModel.showDialogForLoop("MyLoop", loopConfig)

        viewModel.confirmExport()
        advanceUntilIdle()
        awaitBackgroundWork()

        coVerify(exactly = 0) {
            processor.executeLoopJob(
                any(), any(), any(), any(), any(), any(), any(), any(), any(),
                any(), any(), any(), any(), any(), any(), any(), any(), any()
            )
        }
    }

    @Test
    fun `confirmExport publishes no summary for a loop job`() = runTest(testDispatcher) {
        viewModel.showDialogForLoop("MyLoop", loopConfig)

        viewModel.confirmExport()
        advanceUntilIdle()
        awaitBackgroundWork()

        // A summary here would mean an unvalidated output was surfaced as a finished export.
        assertNull(viewModel.uiState.value.completedExportSummary)
        assertFalse(viewModel.uiState.value.showDialog)
    }

    @Test
    fun `confirmExport without a pending config does nothing`() = runTest(testDispatcher) {
        viewModel.confirmExport()
        advanceUntilIdle()
        awaitBackgroundWork()

        coVerify(exactly = 0) {
            processor.executeLoopJob(
                any(), any(), any(), any(), any(), any(), any(), any(), any(),
                any(), any(), any(), any(), any(), any(), any(), any(), any()
            )
        }
        assertNull(viewModel.uiState.value.completedExportSummary)
    }
}
