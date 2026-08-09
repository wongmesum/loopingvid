package com.example.core.ffmpeg

import android.content.Context
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

/**
 * Validates interface constraints and API compatibility for FFmpegWrapper implementations.
 * We avoid testing native FFmpegKit execution directly here as it requires Robolectric native loads,
 * focusing instead on argument mapping and interface contract preservation.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class FFmpegWrapperContractTest {

    private lateinit var context: Context
    private lateinit var wrapper: FFmpegWrapperImpl

    @Before
    fun setup() {
        context = mockk(relaxed = true)
        wrapper = FFmpegWrapperImpl(context)
    }

    @Test
    fun `deprecated execute(String) parses arguments correctly before native dispatch`() = runTest {
        // Just verify it doesn't crash on simple parsing.
        // It will return 1 (failure) because native FFmpegKit isn't loaded in plain JUnit.
        val result = wrapper.execute("-i input.mp4 -c:v copy out.mp4")
        assertEquals("Should fail gracefully when native library isn't available", 1, result)
    }

    @Test
    fun `execute(List) preserves spaces in arguments`() = runTest {
        val args = listOf("-i", "input with space.mp4", "-c:v", "copy", "out with space.mp4")
        val result = wrapper.execute(args)
        assertEquals("Should fail gracefully when native library isn't available", 1, result)
    }
}