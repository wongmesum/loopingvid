package com.example.feature.slideshow

import com.example.core.ffmpeg.JobProgressState
import com.example.core.ffmpeg.SlideshowProcessor
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SlideshowViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var progressState: MutableStateFlow<JobProgressState>
    private lateinit var processor: SlideshowProcessor
    private lateinit var viewModel: SlideshowViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        progressState = MutableStateFlow(JobProgressState())
        processor = mockk(relaxed = true)
        every { processor.progressState } returns progressState
        viewModel = SlideshowViewModel(processor)
    }

    @After
    fun teardown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `addImages deduplicates by URI and clears validation message`() {
        viewModel.renderSlideshow()
        assertNotNull(viewModel.uiState.value.validationMessage)

        viewModel.addImages(listOf(
            SlideshowImage("uri1", "Image 1"),
            SlideshowImage("uri2", "Image 2")
        ))

        val state1 = viewModel.uiState.value
        assertEquals(2, state1.images.size)
        assertNull(state1.validationMessage)

        viewModel.addImages(listOf(
            SlideshowImage("uri2", "Image 2 Duplicate"),
            SlideshowImage("uri3", "Image 3")
        ))

        val state2 = viewModel.uiState.value
        assertEquals(3, state2.images.size)
        assertEquals("uri3", state2.images[2].uri)
    }

    @Test
    fun `removeImage removes correct item`() {
        viewModel.addImages(listOf(
            SlideshowImage("uri1", "1"),
            SlideshowImage("uri2", "2"),
            SlideshowImage("uri3", "3")
        ))

        viewModel.removeImage("uri2")

        val state = viewModel.uiState.value
        assertEquals(2, state.images.size)
        assertEquals("uri1", state.images[0].uri)
        assertEquals("uri3", state.images[1].uri)
    }

    @Test
    fun `moveImage reorders list`() {
        viewModel.addImages(listOf(
            SlideshowImage("uri1", "1"),
            SlideshowImage("uri2", "2"),
            SlideshowImage("uri3", "3")
        ))

        viewModel.moveImage(0, 2)

        val state = viewModel.uiState.value
        assertEquals("uri2", state.images[0].uri)
        assertEquals("uri3", state.images[1].uri)
        assertEquals("uri1", state.images[2].uri)
    }

    @Test
    fun `moveImage ignores out of bounds`() {
        viewModel.addImages(listOf(
            SlideshowImage("uri1", "1"),
            SlideshowImage("uri2", "2")
        ))

        viewModel.moveImage(0, 5)

        assertEquals("uri1", viewModel.uiState.value.images[0].uri)
    }

    @Test
    fun `setPerImageDuration clamps between 1 and 15 seconds`() {
        viewModel.setPerImageDuration(0.5)
        assertEquals(1.0, viewModel.uiState.value.perImageDurationSec, 0.01)

        viewModel.setPerImageDuration(20.0)
        assertEquals(15.0, viewModel.uiState.value.perImageDurationSec, 0.01)

        viewModel.setPerImageDuration(5.0)
        assertEquals(5.0, viewModel.uiState.value.perImageDurationSec, 0.01)
    }

    @Test
    fun `setPerImageDuration lowers transitionDurationSec if too long`() {
        viewModel.setPerImageDuration(5.0)
        viewModel.setTransitionDuration(3.0)

        viewModel.setPerImageDuration(2.0)

        // max allowed is perImageDurationSec - 0.2
        assertEquals(1.8, viewModel.uiState.value.transitionDurationSec, 0.01)
    }

    @Test
    fun `setTransitionDuration clamps between 0_1 and ceiling`() {
        viewModel.setPerImageDuration(4.0)

        viewModel.setTransitionDuration(0.05)
        assertEquals(0.1, viewModel.uiState.value.transitionDurationSec, 0.01)

        viewModel.setTransitionDuration(5.0)
        assertEquals(3.8, viewModel.uiState.value.transitionDurationSec, 0.01)
    }

    @Test
    fun `estimatedDurationSec is 0 when empty`() {
        assertEquals(0.0, viewModel.uiState.value.estimatedDurationSec, 0.01)
    }

    @Test
    fun `estimatedDurationSec ignores overlap when transition is none`() {
        viewModel.addImages(listOf(
            SlideshowImage("1", "1"),
            SlideshowImage("2", "2"),
            SlideshowImage("3", "3")
        ))
        viewModel.setPerImageDuration(3.0)
        viewModel.setTransition("none")

        // 3 images * 3 sec = 9.0 sec
        assertEquals(9.0, viewModel.uiState.value.estimatedDurationSec, 0.01)
    }

    @Test
    fun `estimatedDurationSec subtracts overlaps when transition exists`() {
        viewModel.addImages(listOf(
            SlideshowImage("1", "1"),
            SlideshowImage("2", "2"),
            SlideshowImage("3", "3")
        ))
        viewModel.setPerImageDuration(3.0)
        viewModel.setTransition("fade")
        viewModel.setTransitionDuration(1.0)

        // 3 images * 3 sec = 9.0 sec
        // 2 overlaps * 1 sec = 2.0 sec subtracted
        // Total = 7.0 sec
        assertEquals(7.0, viewModel.uiState.value.estimatedDurationSec, 0.01)
    }

    @Test
    fun `canRender is false when images empty`() {
        assertFalse(viewModel.uiState.value.canRender)

        viewModel.addImages(listOf(SlideshowImage("1", "1")))

        assertTrue(viewModel.uiState.value.canRender)
    }

    @Test
    fun `setOverlayText truncates at 100 chars`() {
        val longString = "A".repeat(150)
        viewModel.setOverlayText(longString)

        assertEquals(100, viewModel.uiState.value.overlayText.length)
        assertEquals("A".repeat(100), viewModel.uiState.value.overlayText)
    }

    @Test
    fun `renderSlideshow sets validation message when images empty`() {
        viewModel.renderSlideshow()

        assertEquals("Please select at least one image first.", viewModel.uiState.value.validationMessage)
    }
}
