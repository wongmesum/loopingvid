package com.example.feature.visualizer.beat

import android.os.SystemClock
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.ui.theme.ProLive
import com.example.ui.theme.ProPrimary
import com.example.ui.theme.ProSuccess

@Composable
fun BeatSyncControlPanel(
    beatSync: BeatSyncState,
    onReanalyze: () -> Unit,
    onTapBpm: (Long) -> Unit,
    onManualBpm: (Double) -> Unit,
    onBandChange: (FrequencyBand) -> Unit,
    onSensitivityChange: (Float) -> Unit,
    onThresholdChange: (Float) -> Unit,
    onSmoothingChange: (Float) -> Unit,
    onAttackChange: (Long) -> Unit,
    onReleaseChange: (Long) -> Unit,
    onOffsetChange: (Long) -> Unit,
    onMinIntervalChange: (Long) -> Unit,
    onStrengthChange: (Float) -> Unit,
    onEffectChange: (BeatEffect) -> Unit,
    onGridDivisionChange: (BeatGridDivision) -> Unit,
    onQuantizeMarkers: () -> Unit,
    modifier: Modifier = Modifier
) {
    var manualBpmText by remember(beatSync.bpm) {
        mutableStateOf(if (beatSync.bpm > 0.0) "%.1f".format(beatSync.bpm) else "")
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            BeatHeader(beatSync)
            BeatActions(
                manualBpmText = manualBpmText,
                onManualTextChange = { manualBpmText = it.filter { character -> character.isDigit() || character == '.' } },
                onApplyManualBpm = { manualBpmText.toDoubleOrNull()?.let(onManualBpm) },
                onTapBpm = { onTapBpm(SystemClock.elapsedRealtime()) },
                onReanalyze = onReanalyze
            )
            FrequencyBandSelector(beatSync.config.band, onBandChange)
            EffectSelector(beatSync.selectedEffect, onEffectChange)
            GridDivisionSelector(
                selected = beatSync.gridDivision,
                canQuantize = BeatGridSnapper.gridStepMs(beatSync.bpm, beatSync.gridDivision) != null &&
                    beatSync.markersMs.isNotEmpty(),
                onSelect = onGridDivisionChange,
                onQuantize = onQuantizeMarkers
            )

            ControlSlider("Sensitivitas", beatSync.config.sensitivity, 0.1f..4f, onSensitivityChange)
            ControlSlider("Threshold", beatSync.config.threshold, 0.05f..1f, onThresholdChange)
            ControlSlider("Smoothing", beatSync.config.smoothing, 0f..1f, onSmoothingChange)
            ControlSlider("Attack: ${beatSync.config.attackMs} ms", beatSync.config.attackMs.toFloat(), 1f..500f) {
                onAttackChange(it.toLong())
            }
            ControlSlider("Release: ${beatSync.config.releaseMs} ms", beatSync.config.releaseMs.toFloat(), 10f..2000f) {
                onReleaseChange(it.toLong())
            }
            ControlSlider("Offset: ${beatSync.config.offsetMs} ms", beatSync.config.offsetMs.toFloat(), -1000f..1000f) {
                onOffsetChange(it.toLong())
            }
            ControlSlider("Interval minimum: ${beatSync.config.minIntervalMs} ms", beatSync.config.minIntervalMs.toFloat(), 50f..1000f) {
                onMinIntervalChange(it.toLong())
            }
            ControlSlider("Kekuatan efek", beatSync.config.effectStrength, 0f..2f, onStrengthChange)
        }
    }
}

@Composable
private fun BeatHeader(beatSync: BeatSyncState) {
    Column {
        Text("Spectrum Beat Sync", style = MaterialTheme.typography.titleMedium)
        when {
            beatSync.isAnalyzing -> Text("Menganalisis audio...", color = ProPrimary)
            beatSync.hasAnalysis -> Text(
                "%.1f BPM • ${beatSync.markersMs.size} marker".format(beatSync.bpm),
                style = MaterialTheme.typography.bodySmall,
                color = ProSuccess
            )
            else -> Text("Belum ada hasil analisis", style = MaterialTheme.typography.bodySmall)
        }
        beatSync.analysisError?.let { error ->
            Text(error, style = MaterialTheme.typography.bodySmall, color = ProLive)
        }
    }
}

@Composable
private fun BeatActions(
    manualBpmText: String,
    onManualTextChange: (String) -> Unit,
    onApplyManualBpm: () -> Unit,
    onTapBpm: () -> Unit,
    onReanalyze: () -> Unit
) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            value = manualBpmText,
            onValueChange = onManualTextChange,
            label = { Text("BPM manual") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true,
            modifier = Modifier.weight(1f).testTag("beat_manual_bpm")
        )
        Button(onClick = onApplyManualBpm, modifier = Modifier.testTag("beat_apply_bpm")) {
            Text("Terapkan")
        }
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(onClick = onTapBpm, modifier = Modifier.testTag("beat_tap_bpm")) {
            Icon(Icons.Rounded.TouchApp, contentDescription = null)
            Text("Tap BPM")
        }
        OutlinedButton(onClick = onReanalyze, modifier = Modifier.testTag("beat_reanalyze")) {
            Icon(Icons.Rounded.Refresh, contentDescription = null)
            Text("Analisis ulang")
        }
    }
}

@Composable
private fun FrequencyBandSelector(selected: FrequencyBand, onSelect: (FrequencyBand) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("Frekuensi", style = MaterialTheme.typography.labelMedium)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(FrequencyBand.entries) { band ->
                FilterChip(
                    selected = selected == band,
                    onClick = { onSelect(band) },
                    label = { Text(band.name) },
                    modifier = Modifier.testTag("beat_band_${band.name.lowercase()}")
                )
            }
        }
    }
}

@Composable
private fun EffectSelector(selected: BeatEffect, onSelect: (BeatEffect) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("Efek", style = MaterialTheme.typography.labelMedium)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(BeatEffect.entries) { effect ->
                FilterChip(
                    selected = selected == effect,
                    onClick = { onSelect(effect) },
                    label = { Text(effect.label) },
                    modifier = Modifier.testTag("beat_effect_${effect.name.lowercase()}")
                )
            }
        }
    }
}

@Composable
private fun GridDivisionSelector(
    selected: BeatGridDivision,
    canQuantize: Boolean,
    onSelect: (BeatGridDivision) -> Unit,
    onQuantize: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("Grid snap", style = MaterialTheme.typography.labelMedium)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(BeatGridDivision.entries) { division ->
                FilterChip(
                    selected = selected == division,
                    onClick = { onSelect(division) },
                    label = { Text(division.label) },
                    modifier = Modifier.testTag("beat_grid_${division.name.lowercase()}")
                )
            }
        }
        OutlinedButton(
            onClick = onQuantize,
            enabled = canQuantize,
            modifier = Modifier.testTag("beat_quantize")
        ) {
            Text("Quantize markers")
        }
    }
}

@Composable
private fun ControlSlider(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onChange: (Float) -> Unit
) {
    Column {
        Text(label, style = MaterialTheme.typography.labelMedium)
        Slider(value = value.coerceIn(range.start, range.endInclusive), onValueChange = onChange, valueRange = range)
    }
}
