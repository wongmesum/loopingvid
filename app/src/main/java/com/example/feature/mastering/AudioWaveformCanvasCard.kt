package com.example.feature.mastering

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.core.media.AudioAnalysisData
import com.example.core.media.EqBandConfig
import com.example.core.media.PeakMeterStyle
import com.example.core.media.VisualizerBarMode
import com.example.core.media.VisualizerTheme

/**
 * Pinned header card that renders the real audio waveform envelope plus live loudness readouts
 * (input target vs. calculated output LUFS after EQ + gain), and lets the user pick the visualizer
 * theme, bar mode, and peak-meter style. The waveform is driven by real [AudioAnalysisData]; when
 * absent it shows an empty baseline (no fabricated signal).
 */
@Composable
fun AudioWaveformCanvasCard(
    analysisData: AudioAnalysisData?,
    eqConfig: EqBandConfig,
    inputGainDb: Float,
    outputGainDb: Float,
    targetLufs: Double,
    calculatedOutputLufs: Double,
    visualizerTheme: VisualizerTheme,
    visualizerBarMode: VisualizerBarMode,
    peakMeterStyle: PeakMeterStyle,
    onThemeChanged: (VisualizerTheme) -> Unit,
    onBarModeChanged: (VisualizerBarMode) -> Unit,
    onPeakMeterStyleChanged: (PeakMeterStyle) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.GraphicEq,
                    contentDescription = "Waveform",
                    tint = visualizerTheme.waveformColor,
                    modifier = Modifier.size(22.dp)
                )
                Text(
                    text = "Waveform & Loudness",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            RealtimeWaveformVisualizer(
                analysisData = analysisData,
                theme = visualizerTheme,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(72.dp)
            )

            // Loudness readouts
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                LufsReadout("Target", targetLufs, visualizerTheme.meterSafeColor)
                LufsReadout("Est. Output", calculatedOutputLufs, visualizerTheme.peakColor)
                LufsReadout(
                    "Gain",
                    (inputGainDb + outputGainDb).toDouble(),
                    if (inputGainDb + outputGainDb == 0f) MaterialTheme.colorScheme.onSurfaceVariant else visualizerTheme.midColor,
                    unit = "dB"
                )
            }

            // Visualizer style selectors
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                EnumDropdown(
                    label = "Theme",
                    current = visualizerTheme.displayName,
                    options = VisualizerTheme.entries.map { it to it.displayName },
                    onSelected = onThemeChanged,
                    modifier = Modifier.weight(1f)
                )
                EnumDropdown(
                    label = "Bars",
                    current = visualizerBarMode.displayName,
                    options = VisualizerBarMode.entries.map { it to it.displayName },
                    onSelected = onBarModeChanged,
                    modifier = Modifier.weight(1f)
                )
                EnumDropdown(
                    label = "Meter",
                    current = peakMeterStyle.displayName,
                    options = PeakMeterStyle.entries.map { it to it.displayName },
                    onSelected = onPeakMeterStyleChanged,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun LufsReadout(label: String, value: Double, color: Color, unit: String = "LUFS") {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            text = "%.1f %s".format(value, unit),
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
            color = color
        )
    }
}

@Composable
private fun <T> EnumDropdown(
    label: String,
    current: String,
    options: List<Pair<T, String>>,
    onSelected: (T) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    Column(modifier = modifier) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Box {
            OutlinedButton(
                onClick = { expanded = true },
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = current,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1
                )
                Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface)
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                options.forEach { (value, name) ->
                    DropdownMenuItem(
                        text = { Text(name) },
                        onClick = {
                            onSelected(value)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}
