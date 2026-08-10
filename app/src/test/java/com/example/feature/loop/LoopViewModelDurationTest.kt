package com.example.feature.loop

import com.example.core.ffmpeg.LoopDurationPlanner
import com.example.core.ffmpeg.MediaProcessor
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Regression guard: manual duration input must be sanitized at the state boundary, so the
 * value shown in the UI is the same value that reaches the FFmpeg command.
 */
class LoopViewModelDurationTest {

    private fun createViewModel(): LoopViewModel {
        val processor = mockk<MediaProcessor>(relaxed = true)
        every { processor.progressState } returns MutableStateFlow(mockk(relaxed = true))
        return LoopViewModel(processor)
    }

    @Test
    fun `setTargetDuration keeps a valid manual value`() {
        val viewModel = createViewModel()

        viewModel.setTargetDuration(137.0)

        assertEquals(137.0, viewModel.uiState.value.targetDurationSec, 0.001)
    }

    @Test
    fun `setTargetDuration rejects zero and negative input`() {
        val viewModel = createViewModel()

        viewModel.setTargetDuration(0.0)
        assertEquals(
            LoopDurationPlanner.MIN_DURATION_SEC,
            viewModel.uiState.value.targetDurationSec,
            0.001
        )

        viewModel.setTargetDuration(-42.0)
        assertEquals(
            LoopDurationPlanner.MIN_DURATION_SEC,
            viewModel.uiState.value.targetDurationSec,
            0.001
        )
    }

    @Test
    fun `setTargetDuration rejects non-finite input`() {
        val viewModel = createViewModel()

        viewModel.setTargetDuration(Double.NaN)
        assertEquals(
            LoopDurationPlanner.MIN_DURATION_SEC,
            viewModel.uiState.value.targetDurationSec,
            0.001
        )
    }

    @Test
    fun `setTargetDuration clamps above the supported maximum`() {
        val viewModel = createViewModel()

        viewModel.setTargetDuration(99_999.0)

        assertEquals(
            LoopDurationPlanner.MAX_DURATION_SEC,
            viewModel.uiState.value.targetDurationSec,
            0.001
        )
    }
}
