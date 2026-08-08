package com.example

import com.example.feature.visualizer.beat.BeatGridDivision
import com.example.feature.visualizer.beat.BeatGridSnapper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Snapping is what makes the beat grid usable: a marker dropped by finger drag
 * lands a few dozen ms off, and these guards pin down where it should end up.
 */
class BeatGridSnapperTest {

    // 120 BPM => 500ms per beat.
    private val bpm = 120.0
    private val durationMs = 10_000L

    @Test
    fun `division off returns the marker untouched`() {
        val snapped = BeatGridSnapper.snap(
            markerMs = 617L,
            bpm = bpm,
            division = BeatGridDivision.OFF,
            offsetMs = 0L,
            durationMs = durationMs
        )

        assertEquals(617L, snapped)
    }

    @Test
    fun `beat division rounds to the nearest whole beat`() {
        // 617ms is closer to 500 than to 1000.
        assertEquals(
            500L,
            BeatGridSnapper.snap(617L, bpm, BeatGridDivision.BEAT, 0L, durationMs)
        )
        // 780ms is closer to 1000.
        assertEquals(
            1000L,
            BeatGridSnapper.snap(780L, bpm, BeatGridDivision.BEAT, 0L, durationMs)
        )
    }

    @Test
    fun `half beat division snaps to 250ms steps at 120 bpm`() {
        assertEquals(
            750L,
            BeatGridSnapper.snap(700L, bpm, BeatGridDivision.HALF_BEAT, 0L, durationMs)
        )
    }

    @Test
    fun `quarter beat division snaps to 125ms steps at 120 bpm`() {
        assertEquals(
            625L,
            BeatGridSnapper.snap(617L, bpm, BeatGridDivision.QUARTER_BEAT, 0L, durationMs)
        )
    }

    @Test
    fun `offset shifts the whole grid`() {
        // Grid lines become 100, 600, 1100... so 617 snaps to 600, not 500.
        assertEquals(
            600L,
            BeatGridSnapper.snap(617L, bpm, BeatGridDivision.BEAT, 100L, durationMs)
        )
    }

    @Test
    fun `snapping never lands outside the track`() {
        val past = BeatGridSnapper.snap(9_950L, bpm, BeatGridDivision.BEAT, 0L, durationMs)
        assertTrue("Snapped marker must stay within duration, got $past", past <= durationMs)

        val negative = BeatGridSnapper.snap(20L, bpm, BeatGridDivision.BEAT, -400L, durationMs)
        assertTrue("Snapped marker must not be negative, got $negative", negative >= 0L)
    }

    @Test
    fun `unusable bpm leaves the marker alone rather than collapsing it to zero`() {
        listOf(0.0, -120.0, Double.NaN, Double.POSITIVE_INFINITY).forEach { badBpm ->
            assertEquals(
                "bpm=$badBpm must be treated as no grid",
                617L,
                BeatGridSnapper.snap(617L, badBpm, BeatGridDivision.BEAT, 0L, durationMs)
            )
        }
    }

    @Test
    fun `quantize snaps every marker and keeps the list sorted and distinct`() {
        val markers = listOf(1010L, 480L, 505L, 1490L)

        val quantized = BeatGridSnapper.quantize(
            markers = markers,
            bpm = bpm,
            division = BeatGridDivision.BEAT,
            offsetMs = 0L,
            durationMs = durationMs
        )

        // 480 and 505 both land on 500 and must collapse to one marker.
        assertEquals(listOf(500L, 1000L, 1500L), quantized)
    }

    @Test
    fun `quantize with division off is a no-op`() {
        val markers = listOf(480L, 1010L)

        val quantized = BeatGridSnapper.quantize(markers, bpm, BeatGridDivision.OFF, 0L, durationMs)

        assertEquals(markers, quantized)
    }
}
