package com.example.core.media

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.East
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.RadioButtonChecked
import androidx.compose.material.icons.filled.SwipeLeft
import androidx.compose.material.icons.filled.SwipeRight
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.West
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.ui.graphics.vector.ImageVector
import java.util.Locale

enum class TransitionEffect(
    val displayName: String,
    val xfadeName: String,
    val description: String,
    val category: String
) {
    CROSSFADE("Crossfade", "fade", "Smooth linear opacity blend", "Fade"),
    DISSOLVE("Dissolve", "dissolve", "Dithered noise dissolve transition", "Fade"),
    WIPE_LEFT("Wipe Left", "wipeleft", "Directional wipe clearing left", "Wipe"),
    WIPE_RIGHT("Wipe Right", "wiperight", "Directional wipe clearing right", "Wipe"),
    SLIDE_LEFT("Slide Left", "slideleft", "Push slide incoming segment left", "Slide"),
    SLIDE_RIGHT("Slide Right", "slideright", "Push slide incoming segment right", "Slide"),
    CIRCLE_CROP("Circle Crop", "circlecrop", "Expanding iris circular mask", "Iris"),
    ZOOM_IN("Zoom In", "zoomin", "Camera zoom scale cross-transition", "Zoom"),
    PIXELIZE("Pixelize", "pixelize", "Mosaic pixel block fade transition", "Glitch"),
    RADIAL("Radial Wipe", "radial", "Clockwise sweep radial wipe transition", "Wipe");

    fun getIcon(): ImageVector {
        return when (this) {
            CROSSFADE -> Icons.Default.Tune
            DISSOLVE -> Icons.Default.AutoFixHigh
            WIPE_LEFT -> Icons.Default.West
            WIPE_RIGHT -> Icons.Default.East
            SLIDE_LEFT -> Icons.Default.SwipeLeft
            SLIDE_RIGHT -> Icons.Default.SwipeRight
            CIRCLE_CROP -> Icons.Default.RadioButtonChecked
            ZOOM_IN -> Icons.Default.ZoomIn
            PIXELIZE -> Icons.Default.GridOn
            RADIAL -> Icons.Default.Autorenew
        }
    }
}

data class LoopedSegmentConfig(
    val id: String,
    val segmentName: String,
    val durationSec: Double = 5.0,
    val loopRepeatCount: Int = 2,
    val transitionToNext: TransitionEffect = TransitionEffect.CROSSFADE,
    val transitionDurationSec: Double = 1.0
) {
    val totalEffectiveDurationSec: Double
        get() = durationSec * loopRepeatCount
}

data class SegmentTransitionConfig(
    val segments: List<LoopedSegmentConfig> = listOf(
        LoopedSegmentConfig("seg_1", "Intro Loop", durationSec = 4.0, loopRepeatCount = 2, transitionToNext = TransitionEffect.CROSSFADE, transitionDurationSec = 1.0),
        LoopedSegmentConfig("seg_2", "Chorus Hook", durationSec = 6.0, loopRepeatCount = 2, transitionToNext = TransitionEffect.WIPE_LEFT, transitionDurationSec = 1.0),
        LoopedSegmentConfig("seg_3", "Outro Breakdown", durationSec = 5.0, loopRepeatCount = 1, transitionToNext = TransitionEffect.DISSOLVE, transitionDurationSec = 1.0)
    ),
    val globalTransitionEffect: TransitionEffect = TransitionEffect.CROSSFADE,
    val globalTransitionDurationSec: Double = 1.0
) {
    /**
     * Total video duration across all chained segments considering xfade overlaps.
     */
    fun calculateTotalDurationSec(): Double {
        if (segments.isEmpty()) return 0.0
        var total = segments[0].totalEffectiveDurationSec
        for (i in 1 until segments.size) {
            val transDur = segments[i - 1].transitionDurationSec
            total += segments[i].totalEffectiveDurationSec - transDur
        }
        return total.coerceAtLeast(1.0)
    }

    /**
     * Constructs valid FFmpeg complex filter graph (`-filter_complex`) for segment xfade transitions.
     */
    fun buildFfmpegXfadeFilterGraph(): String {
        if (segments.size <= 1) return ""
        val filterParts = mutableListOf<String>()
        var cumulativeOffset = segments[0].totalEffectiveDurationSec

        for (i in 0 until segments.size - 1) {
            val currSeg = segments[i]
            val nextSeg = segments[i + 1]
            val transEffect = currSeg.transitionToNext
            val transDur = currSeg.transitionDurationSec.coerceAtMost(currSeg.totalEffectiveDurationSec / 2.0)

            val offset = (cumulativeOffset - transDur).coerceAtLeast(0.1)
            val inTag = if (i == 0) "[0:v]" else "[v_xfade_$i]"
            val nextTag = "[${i + 1}:v]"
            val outTag = if (i == segments.size - 2) "[v_out]" else "[v_xfade_${i + 1}]"

            filterParts.add("${inTag}${nextTag}xfade=transition=${transEffect.xfadeName}:duration=${String.format(Locale.US, "%.1f", transDur)}:offset=${String.format(Locale.US, "%.1f", offset)}${outTag}")

            cumulativeOffset = offset + nextSeg.totalEffectiveDurationSec
        }

        return filterParts.joinToString("; ")
    }
}
