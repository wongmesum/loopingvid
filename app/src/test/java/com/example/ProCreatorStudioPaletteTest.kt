package com.example

import androidx.compose.ui.graphics.toArgb
import com.example.ui.theme.ProBackground
import com.example.ui.theme.ProLive
import com.example.ui.theme.ProPrimary
import com.example.ui.theme.ProSecondary
import com.example.ui.theme.ProSuccess
import com.example.ui.theme.ProSurface
import com.example.ui.theme.ProSurfaceElevated
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Regression guard for the "Professional Creator Studio" premium dark palette.
 * Locks the exact hex values requested by the design spec so a future edit
 * to Color.kt cannot silently drift the brand palette.
 */
class ProCreatorStudioPaletteTest {

    @Test
    fun `background matches spec hex 0B0D12`() {
        assertEquals(0xFF0B0D12.toInt(), ProBackground.toArgb())
    }

    @Test
    fun `surface matches spec hex 131720`() {
        assertEquals(0xFF131720.toInt(), ProSurface.toArgb())
    }

    @Test
    fun `elevated surface matches spec hex 1A202C`() {
        assertEquals(0xFF1A202C.toInt(), ProSurfaceElevated.toArgb())
    }

    @Test
    fun `primary matches spec hex 7C5CFF`() {
        assertEquals(0xFF7C5CFF.toInt(), ProPrimary.toArgb())
    }

    @Test
    fun `secondary matches spec hex 22D3EE`() {
        assertEquals(0xFF22D3EE.toInt(), ProSecondary.toArgb())
    }

    @Test
    fun `success matches spec hex 34D399`() {
        assertEquals(0xFF34D399.toInt(), ProSuccess.toArgb())
    }

    @Test
    fun `live error matches spec hex FF4D67`() {
        assertEquals(0xFFFF4D67.toInt(), ProLive.toArgb())
    }
}
