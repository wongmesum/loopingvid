package com.example.feature.visualizer

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.core.ui.VideoPlayer
import com.example.feature.visualizer.beat.BeatSyncControlPanel
import com.example.feature.visualizer.beat.BeatTimeline
import com.example.ui.theme.ProPrimary

@Composable
fun VisualizerStudioScreen(
    viewModel: VisualizerViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    var currentPositionMs by remember { mutableLongStateOf(0L) }
    val context = LocalContext.current
    val audioPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        uri?.let {
            try {
                context.contentResolver.takePersistableUriPermission(
                    it,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (e: SecurityException) {
                // Ignore if permission isn't persistable (e.g. some third party providers)
            }
            // Resolving the display name could be done here via DocumentFile or ContentResolver query,
            // but lastPathSegment is retained as a lightweight fallback consistent with the original.
            viewModel.setAudioSource(it.toString(), it.lastPathSegment ?: "audio")
        }
    }

    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        VisualizerHeader()
        AudioSourceCard(uiState.audioName, onPickAudio = { audioPicker.launch(arrayOf("audio/*")) })
        VisualizerPreviewCard(
            uiState = uiState,
            viewModel = viewModel,
            onPlaybackPosition = { currentPositionMs = it }
        )
        VisualizerModeCard(uiState.config.mode, viewModel::setMode)
        VisualizerControlCard(uiState.config, viewModel)

        if (uiState.audioUri != null) {
            BeatTimeline(
                markersMs = uiState.beatSync.markersMs,
                durationMs = uiState.beatSync.durationMs,
                currentPositionMs = currentPositionMs,
                bpm = uiState.beatSync.bpm,
                gridDivision = uiState.beatSync.gridDivision,
                offsetMs = uiState.beatSync.config.offsetMs,
                onAddMarker = viewModel::addBeatMarker,
                onMoveMarker = viewModel::moveBeatMarker,
                onRemoveMarker = viewModel::removeBeatMarker
            )
            BeatSyncControlPanel(
                beatSync = uiState.beatSync,
                onReanalyze = viewModel::reanalyzeBeats,
                onTapBpm = viewModel::tapBpm,
                onManualBpm = viewModel::setManualBpm,
                onBandChange = viewModel::setBeatBand,
                onSensitivityChange = viewModel::setBeatSensitivity,
                onThresholdChange = viewModel::setBeatThreshold,
                onSmoothingChange = viewModel::setBeatSmoothing,
                onAttackChange = viewModel::setBeatAttack,
                onReleaseChange = viewModel::setBeatRelease,
                onOffsetChange = viewModel::setBeatOffset,
                onMinIntervalChange = viewModel::setBeatMinInterval,
                onStrengthChange = viewModel::setBeatEffectStrength,
                onEffectChange = viewModel::setSelectedBeatEffect,
                onGridDivisionChange = viewModel::setBeatGridDivision,
                onQuantizeMarkers = viewModel::quantizeBeatMarkers
            )
        }

        VisualizerExportCard(
            outputName = uiState.outputName,
            canExport = uiState.canExport,
            jobProgress = uiState.jobProgress,
            validationMessage = uiState.validationMessage,
            onOutputNameChange = viewModel::setOutputName,
            onExport = viewModel::exportVisualizer,
            onCancel = viewModel::cancelExport,
            onDismissValidation = viewModel::dismissValidationMessage
        )
    }
}

@Composable
private fun VisualizerHeader() {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Icon(Icons.Rounded.GraphicEq, contentDescription = null, tint = ProPrimary)
        Column {
            Text("Visualizer Studio", style = androidx.compose.material3.MaterialTheme.typography.headlineSmall)
            Text(
                "Preview visual audio dengan enam mode render.",
                style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
                color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun AudioSourceCard(audioName: String, onPickAudio: () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surfaceVariant)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text("Audio sumber", style = androidx.compose.material3.MaterialTheme.typography.titleMedium)
                Text(
                    if (audioName.isBlank()) "Belum ada audio dipilih" else audioName,
                    style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                    color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            OutlinedButton(onClick = onPickAudio, modifier = Modifier.testTag("visualizer_pick_audio")) {
                Icon(Icons.Rounded.MusicNote, contentDescription = null)
                Text("Pilih")
            }
        }
    }
}

@Composable
private fun VisualizerPreviewCard(
    uiState: VisualizerUiState,
    viewModel: VisualizerViewModel,
    onPlaybackPosition: (Long) -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(modifier = Modifier.fillMaxWidth().height(240.dp)) {
                if (uiState.audioUri != null) {
                    VideoPlayer(
                        mediaUri = uiState.audioUri,
                        isPlaying = uiState.isPlaying,
                        audioProcessor = viewModel.spectrumProcessor,
                        onPlaybackStateChanged = { viewModel.setPlaying(it) },
                        onProgressUpdate = { positionMs, _ ->
                            onPlaybackPosition(positionMs)
                            viewModel.updatePlaybackPosition(positionMs)
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }
                BeatReactiveVisualizer(
                    magnitudes = uiState.magnitudes,
                    config = uiState.config,
                    effect = uiState.beatSync.selectedEffect,
                    pulse = uiState.beatSync.currentPulse,
                    modifier = Modifier.matchParentSize()
                )
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Peak %.1f dB".format(uiState.peakDb), style = androidx.compose.material3.MaterialTheme.typography.labelSmall)
                Text("RMS %.2f".format(uiState.rmsEnergy), style = androidx.compose.material3.MaterialTheme.typography.labelSmall)
                Text("${uiState.config.bandCount} band", style = androidx.compose.material3.MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@Composable
private fun VisualizerModeCard(selectedMode: VisualizerMode, onSelect: (VisualizerMode) -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surfaceVariant)) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Mode", style = androidx.compose.material3.MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                VisualizerMode.entries.take(3).forEach { mode ->
                    FilterChip(selected = selectedMode == mode, onClick = { onSelect(mode) }, label = { Text(mode.name) })
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                VisualizerMode.entries.drop(3).forEach { mode ->
                    FilterChip(selected = selectedMode == mode, onClick = { onSelect(mode) }, label = { Text(mode.name) })
                }
            }
        }
    }
}

@Composable
private fun VisualizerControlCard(config: VisualizerRenderConfig, viewModel: VisualizerViewModel) {
    Card(colors = CardDefaults.cardColors(containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surfaceVariant)) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Kontrol Visual", style = androidx.compose.material3.MaterialTheme.typography.titleMedium)
            LabeledSlider("Jumlah bar: ${config.bandCount}", config.bandCount.toFloat(), 8f..64f) { viewModel.setBandCount(it.toInt()) }
            LabeledSlider("Sensitivitas: %.1fx".format(config.sensitivityGain), config.sensitivityGain, 0.2f..4f, viewModel::setSensitivity)
            LabeledSlider("Smoothing: %.0f%%".format(config.smoothing * 100), config.smoothing, 0f..1f, viewModel::setSmoothing)
            LabeledSlider("Ukuran: %.1fx".format(config.sizeScale), config.sizeScale, 0.3f..2f, viewModel::setSizeScale)
            LabeledSlider("Ketebalan: %.0f".format(config.thickness), config.thickness, 1f..20f, viewModel::setThickness)
            LabeledSlider("Opacity: %.0f%%".format(config.opacity * 100), config.opacity, 0.1f..1f, viewModel::setOpacity)
            LabeledSlider("Rotasi: %.0f°".format(config.rotationDegrees), config.rotationDegrees, 0f..360f, viewModel::setRotation)
        }
    }
}

@Composable
private fun LabeledSlider(label: String, value: Float, range: ClosedFloatingPointRange<Float>, onChange: (Float) -> Unit) {
    Column {
        Text(label, style = androidx.compose.material3.MaterialTheme.typography.labelMedium)
        Slider(value = value, onValueChange = onChange, valueRange = range)
    }
}
