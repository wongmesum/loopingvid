package com.example.feature.visualizer.beat

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.ui.theme.ProLive
import com.example.ui.theme.ProPrimary
import com.example.ui.theme.ProSecondary
import kotlin.math.abs

@Composable
fun BeatTimeline(
    markersMs: List<Long>,
    durationMs: Long,
    currentPositionMs: Long,
    onAddMarker: (Long) -> Unit,
    onMoveMarker: (Int, Long) -> Unit,
    onRemoveMarker: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedIndex by remember(markersMs) {
        mutableIntStateOf(if (markersMs.isEmpty()) -1 else 0)
    }
    val safeDuration = durationMs.coerceAtLeast(1L)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp))
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        TimelineHeader(
            markerCount = markersMs.size,
            selectedIndex = selectedIndex,
            currentPositionMs = currentPositionMs,
            onAdd = {
                onAddMarker(currentPositionMs.coerceIn(0L, safeDuration))
                selectedIndex = markersMs.size
            },
            onDelete = {
                if (selectedIndex in markersMs.indices) {
                    onRemoveMarker(selectedIndex)
                    selectedIndex = (selectedIndex - 1).coerceAtLeast(-1)
                }
            }
        )

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(76.dp)
                .testTag("beat_timeline")
                .pointerInput(markersMs, safeDuration) {
                    detectTapGestures { offset ->
                        val targetMs = xToTime(offset.x, size.width.toFloat(), safeDuration)
                        val nearest = nearestMarkerIndex(markersMs, targetMs)
                        val selectionRadiusMs = safeDuration * 0.025
                        if (nearest >= 0 && abs(markersMs[nearest] - targetMs) <= selectionRadiusMs) {
                            selectedIndex = nearest
                        } else {
                            onAddMarker(targetMs)
                            selectedIndex = markersMs.size
                        }
                    }
                }
                .pointerInput(markersMs, selectedIndex, safeDuration) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            val targetMs = xToTime(offset.x, size.width.toFloat(), safeDuration)
                            selectedIndex = nearestMarkerIndex(markersMs, targetMs)
                        },
                        onDrag = { change, _ ->
                            if (selectedIndex in markersMs.indices) {
                                val movedMs = xToTime(change.position.x, size.width.toFloat(), safeDuration)
                                onMoveMarker(selectedIndex, movedMs)
                            }
                        }
                    )
                }
        ) {
            val centerY = size.height / 2f
            drawLine(
                color = MaterialTheme.colorScheme.outline,
                start = Offset(0f, centerY),
                end = Offset(size.width, centerY),
                strokeWidth = 2f
            )

            val playheadX = timeToX(currentPositionMs, safeDuration, size.width)
            drawLine(
                color = ProSecondary,
                start = Offset(playheadX, 0f),
                end = Offset(playheadX, size.height),
                strokeWidth = 3f
            )

            markersMs.forEachIndexed { index, markerMs ->
                val x = timeToX(markerMs, safeDuration, size.width)
                val color = if (index == selectedIndex) ProLive else ProPrimary
                drawLine(
                    color = color,
                    start = Offset(x, 8f),
                    end = Offset(x, size.height - 8f),
                    strokeWidth = if (index == selectedIndex) 5f else 3f
                )
                drawCircle(color = color, radius = 5f, center = Offset(x, centerY))
            }
        }
    }
}

@Composable
private fun TimelineHeader(
    markerCount: Int,
    selectedIndex: Int,
    currentPositionMs: Long,
    onAdd: () -> Unit,
    onDelete: () -> Unit
) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Column {
            Text("Timeline Beat", style = MaterialTheme.typography.titleSmall)
            Text(
                "$markerCount marker • ${formatTimestamp(currentPositionMs)}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Row {
            IconButton(onClick = onAdd, modifier = Modifier.testTag("beat_add_marker")) {
                Icon(Icons.Rounded.Add, contentDescription = "Tambah marker", tint = ProPrimary)
            }
            IconButton(
                onClick = onDelete,
                enabled = selectedIndex >= 0,
                modifier = Modifier.testTag("beat_delete_marker")
            ) {
                Icon(Icons.Rounded.Delete, contentDescription = "Hapus marker", tint = ProLive)
            }
        }
    }
}

private fun nearestMarkerIndex(markers: List<Long>, targetMs: Long): Int =
    markers.indices.minByOrNull { abs(markers[it] - targetMs) } ?: -1

private fun xToTime(x: Float, width: Float, durationMs: Long): Long {
    if (width <= 0f) return 0L
    return ((x / width).coerceIn(0f, 1f) * durationMs).toLong()
}

private fun timeToX(timeMs: Long, durationMs: Long, width: Float): Float =
    (timeMs.toFloat() / durationMs.coerceAtLeast(1L)).coerceIn(0f, 1f) * width

private fun formatTimestamp(timeMs: Long): String {
    val totalSeconds = timeMs.coerceAtLeast(0L) / 1000L
    val minutes = totalSeconds / 60L
    val seconds = totalSeconds % 60L
    return "%02d:%02d".format(minutes, seconds)
}
