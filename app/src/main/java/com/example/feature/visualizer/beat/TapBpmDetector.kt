package com.example.feature.visualizer.beat

/**
 * Collects timestamps of manual taps and calculates live BPM from the intervals.
 * Keeps a rolling window of up to [maxTaps] entries to stay responsive.
 */
class TapBpmDetector(private val maxTaps: Int = 12) {

    private val taps = mutableListOf<Long>()

    /** Record a tap at the given monotonic timestamp in milliseconds. */
    fun tap(timestampMs: Long) {
        // Reset if it's been more than 3 seconds since the last tap — user probably restarted.
        if (taps.isNotEmpty() && timestampMs - taps.last() > 3000L) {
            taps.clear()
        }
        taps.add(timestampMs)
        if (taps.size > maxTaps) {
            taps.removeAt(0)
        }
    }

    /** Returns the calculated BPM, or 0.0 if fewer than 2 taps have been recorded. */
    fun currentBpm(): Double = BeatDetectionEngine.bpmFromTaps(taps.toList())

    /** Returns the number of taps collected in the current window. */
    fun tapCount(): Int = taps.size

    /** Clear all collected taps and reset state. */
    fun reset() {
        taps.clear()
    }
}
