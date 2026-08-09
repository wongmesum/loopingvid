package com.example.feature.loop

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Loop
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import com.example.core.ui.FfmpegCircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.media.AudioSpectrumVisualizer
import com.example.core.media.SpectrumStyle
import com.example.core.ui.UndoRedoBar
import com.example.core.ui.VideoPlayer
import com.example.core.ui.ExportDialog
import com.example.core.ui.PrecisionVideoTrimControl

@Composable
fun LoopScreen(
    exportViewModel: com.example.core.ui.ExportViewModel,
    viewModel: LoopViewModel,
    onNavigateToGoLive: (sourceUri: String) -> Unit,
    assetManagerViewModel: com.example.feature.assets.AssetManagerViewModel? = null
) {
    val uiState by viewModel.uiState.collectAsState()
    val undoRedoState by viewModel.undoRedoState.collectAsState()
    val scrollState = rememberScrollState()

    var isManualDurationMode by remember { mutableStateOf(false) }
    var manualDurationText by remember(uiState.targetDurationSec) {
        mutableStateOf(uiState.targetDurationSec.toInt().toString())
    }

    val recordAsset = com.example.feature.assets.rememberAssetRecorder(assetManagerViewModel)

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            recordAsset(it)
            viewModel.onMediaSelected(it.toString(), it.lastPathSegment ?: "selected_media.mp4")
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Undo / Redo Command Bar
        UndoRedoBar(
            state = undoRedoState,
            onUndo = { viewModel.undo() },
            onRedo = { viewModel.redo() },
            onClearHistory = { viewModel.clearHistory() }
        )

        // Top Header
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Loop,
                        contentDescription = "Loop Tool",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Video Loop Studio",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Create seamless looping video outputs with crossfade & ping-pong",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Input Selector
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = "1. Select Source Media",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(8.dp))

                if (uiState.selectedMediaUri == null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(110.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                            .clickable { filePickerLauncher.launch("video/*") }
                            .testTag("select_video_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.FolderOpen,
                                contentDescription = "Pick Media",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Tap to choose video file",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                } else {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        VideoPlayer(
                            mediaUri = uiState.selectedMediaUri,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(220.dp),
                            isPlaying = uiState.isPreviewPlaying,
                            isLooping = uiState.isSeamlessLoopEnabled,
                            isMuted = uiState.muteAudio,
                            startMs = (uiState.trimStartSec * 1000).toLong(),
                            endMs = (uiState.trimEndSec * 1000).toLong(),
                            onProgressUpdate = { pos, dur ->
                                viewModel.updateProgress(pos, dur)
                            },
                            onPlaybackStateChanged = { playing ->
                                viewModel.onPlaybackStateChanged(playing)
                            },
                            testTag = "loop_video_player"
                        )

                        // Media3 ExoPlayer Seamless Loop Player Overlay Controls
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp))
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                IconButton(
                                    onClick = { viewModel.togglePlayPause() },
                                    modifier = Modifier.testTag("preview_play_pause_button")
                                ) {
                                    Icon(
                                        imageVector = if (uiState.isPreviewPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                        contentDescription = if (uiState.isPreviewPlaying) "Pause Preview" else "Play Preview",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Icon(
                                    imageVector = Icons.Default.RepeatOne,
                                    contentDescription = "Seamless Loop Active",
                                    tint = if (uiState.isSeamlessLoopEnabled) MaterialTheme.colorScheme.secondary else Color.Gray,
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clickable { viewModel.setSeamlessLoopEnabled(!uiState.isSeamlessLoopEnabled) }
                                )
                                Text(
                                    text = "${formatTimeMs(uiState.currentPositionMs)} / ${formatTimeMs(uiState.durationMs)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Surface(
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text(
                                        text = "Loop #${uiState.loopCount}",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp))
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Movie,
                                contentDescription = "Selected Media",
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = uiState.selectedMediaName,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = if (uiState.isSeamlessLoopEnabled) "ExoPlayer Media3 Seamless Repeat Active" else "Standard Playback",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            }
                            OutlinedButton(
                                onClick = { filePickerLauncher.launch("video/*") },
                                modifier = Modifier.testTag("change_video_button")
                            ) {
                                Text("Change")
                            }
                        }
                    }
                }
            }
        }

        // Precision Video Loop Start & End Trim Control
        AnimatedVisibility(visible = uiState.selectedMediaUri != null) {
            PrecisionVideoTrimControl(
                trimStartSec = uiState.trimStartSec,
                trimEndSec = uiState.trimEndSec,
                mediaDurationMs = uiState.durationMs,
                currentPositionMs = uiState.currentPositionMs,
                onTrimChange = { startSec, endSec ->
                    viewModel.updateTrimPreview(startSec, endSec)
                },
                onTrimChangeFinished = { startSec, endSec ->
                    viewModel.commitTrimChange(startSec, endSec)
                }
            )

            VideoLoopingPlayer(
                mediaUri = uiState.selectedMediaUri,
                initialStartMs = (uiState.trimStartSec * 1000).toLong(),
                initialEndMs = (uiState.trimEndSec * 1000).toLong(),
                isSeamlessLoopEnabled = uiState.isSeamlessLoopEnabled,
                onSegmentChanged = { startMs, endMs ->
                    viewModel.setTrim(startMs / 1000.0, endMs / 1000.0)
                },
                onExportSegmentRequested = { _, _, _ ->
                    viewModel.startRenderJob()
                }
            )
        }

        // Loop Controls
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "2. Looping Settings",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface
                )

                // Style Chips
                Text("Loop Style Transition", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("NORMAL", "CROSSFADE", "PING_PONG").forEach { style ->
                        FilterChip(
                            selected = uiState.loopStyle == style,
                            onClick = { viewModel.setLoopStyle(style) },
                            label = { Text(if (style == "CROSSFADE") "CROSSFADE (Smooth)" else style) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            modifier = Modifier.testTag("style_chip_$style")
                        )
                    }
                }

                // Crossfade Transition Settings Panel
                AnimatedVisibility(visible = uiState.loopStyle == "CROSSFADE") {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Repeat,
                                        contentDescription = "Crossfade Seamless Loop",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Crossfade Duration",
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                Text(
                                    text = "%.1fs".format(java.util.Locale.US, uiState.crossfadeDurationSec),
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }

                            Text(
                                text = "Blends the end of the video with its start to eliminate jumpy frames when looping back to the beginning.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                listOf(0.5, 1.0, 1.5, 2.0, 3.0).forEach { preset ->
                                    FilterChip(
                                        selected = Math.abs(uiState.crossfadeDurationSec - preset) < 0.05,
                                        onClick = { viewModel.setCrossfadeDuration(preset) },
                                        label = { Text("${preset}s", style = MaterialTheme.typography.labelSmall) },
                                        modifier = Modifier.testTag("crossfade_preset_${preset.toString().replace('.', '_')}")
                                    )
                                }
                            }

                            Slider(
                                value = uiState.crossfadeDurationSec.toFloat(),
                                onValueChange = { viewModel.updateCrossfadeDurationPreview(it.toDouble()) },
                                onValueChangeFinished = { viewModel.commitCrossfadeDurationChange(uiState.crossfadeDurationSec) },
                                valueRange = 0.2f..5.0f,
                                colors = SliderDefaults.colors(
                                    thumbColor = MaterialTheme.colorScheme.primary,
                                    activeTrackColor = MaterialTheme.colorScheme.primary
                                ),
                                modifier = Modifier.testTag("crossfade_duration_slider")
                            )
                        }
                    }
                }

                // Target Duration Section (Slider + Manual Input & Quick Presets)
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Target Loop Duration",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            FilterChip(
                                selected = isManualDurationMode,
                                onClick = { isManualDurationMode = !isManualDurationMode },
                                label = {
                                    Text(
                                        text = if (isManualDurationMode) "Isi Manual ✏️" else "Slider 🎚️",
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                },
                                modifier = Modifier.testTag("toggle_manual_duration")
                            )
                        }

                        val durationInt = uiState.targetDurationSec.toInt()
                        val minutes = durationInt / 60
                        val seconds = durationInt % 60
                        val formattedDuration = if (minutes > 0) "${minutes}m ${seconds}s" else "${durationInt}s"

                        Text(
                            text = formattedDuration,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    // Quick Duration Preset Chips
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        val presets = listOf(15, 30, 60, 180, 300, 600, 1800, 3600)
                        items(presets) { presetSec ->
                            val isSelected = uiState.targetDurationSec.toInt() == presetSec
                            val labelText = when {
                                presetSec < 60 -> "${presetSec}s"
                                presetSec % 60 == 0 -> "${presetSec / 60}m"
                                else -> "${presetSec / 60}m ${presetSec % 60}s"
                            }
                            FilterChip(
                                selected = isSelected,
                                onClick = { viewModel.setTargetDuration(presetSec.toDouble()) },
                                label = { Text(labelText, style = MaterialTheme.typography.labelSmall) },
                                modifier = Modifier.testTag("duration_preset_$presetSec")
                            )
                        }
                    }

                    if (isManualDurationMode) {
                        // Manual Text Input + Quick Step Buttons (-10s / +10s)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedIconButton(
                                onClick = {
                                    val newDur = (uiState.targetDurationSec - 10.0).coerceAtLeast(1.0)
                                    viewModel.setTargetDuration(newDur)
                                },
                                modifier = Modifier.testTag("decrease_duration_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Remove,
                                    contentDescription = "Kurangi 10 detik"
                                )
                            }

                            OutlinedTextField(
                                value = manualDurationText,
                                onValueChange = { input ->
                                    val digitsOnly = input.filter { it.isDigit() }
                                    manualDurationText = digitsOnly
                                    val parsed = digitsOnly.toDoubleOrNull()
                                    if (parsed != null && parsed > 0) {
                                        viewModel.setTargetDuration(parsed)
                                    }
                                },
                                label = { Text("Durasi Manual (Detik)") },
                                placeholder = { Text("misal: 60, 300, 1800") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                trailingIcon = {
                                    Text(
                                        text = "detik",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(end = 8.dp)
                                    )
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("manual_duration_input")
                            )

                            OutlinedIconButton(
                                onClick = {
                                    val newDur = uiState.targetDurationSec + 10.0
                                    viewModel.setTargetDuration(newDur)
                                },
                                modifier = Modifier.testTag("increase_duration_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "Tambah 10 detik"
                                )
                            }
                        }
                    } else {
                        // Interactive Slider
                        Slider(
                            value = uiState.targetDurationSec.toFloat().coerceIn(5f, 600f),
                            onValueChange = { viewModel.setTargetDuration(it.toDouble()) },
                            valueRange = 5f..600f,
                            colors = SliderDefaults.colors(
                                thumbColor = MaterialTheme.colorScheme.primary,
                                activeTrackColor = MaterialTheme.colorScheme.primary
                            ),
                            modifier = Modifier.testTag("duration_slider")
                        )
                    }
                }

                // Preset Quality Chips
                Text("Preset Output Quality", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("1080p", "720p", "480p").forEach { quality ->
                        FilterChip(
                            selected = uiState.presetQuality == quality,
                            onClick = { viewModel.setPresetQuality(quality) },
                            label = { Text(quality) },
                            modifier = Modifier.testTag("quality_chip_$quality")
                        )
                    }
                }

                // Mute Audio Toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Mute Original Audio", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                    Switch(
                        checked = uiState.muteAudio,
                        onCheckedChange = { viewModel.setMuteAudio(it) },
                        colors = SwitchDefaults.colors(checkedThumbColor = MaterialTheme.colorScheme.primary),
                        modifier = Modifier.testTag("mute_audio_switch")
                    )
                }
            }
        }

        // Live Audio Spectrum Visualizer Preview
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = "Live Audio Dynamics Preview",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.secondary
                )
                Spacer(modifier = Modifier.height(8.dp))
                AudioSpectrumVisualizer(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp),
                    style = SpectrumStyle.BARS,
                    isPlaying = true
                )
            }
        }

        // Render Action Card & Progress
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (!uiState.jobProgress.isProcessing) {
                    Button(
                        onClick = { exportViewModel.showDialogForLoop("LoopVideo", com.example.core.ui.ExportJobConfig.LoopJob(
                                inputUri = uiState.selectedMediaUri!!,
                                targetDurationSec = uiState.targetDurationSec,
                                loopStyle = uiState.loopStyle,
                                crossfadeDurationSec = uiState.crossfadeDurationSec,
                                trimStartSec = uiState.trimStartSec,
                                trimEndSec = uiState.trimEndSec,
                                muteAudio = uiState.muteAudio,
                                audioFadeInSec = uiState.audioFadeInSec,
                                audioFadeOutSec = uiState.audioFadeOutSec,
                                presetQuality = uiState.presetQuality
                            )) },
                        enabled = uiState.selectedMediaUri != null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("start_render_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(imageVector = Icons.Default.PlayArrow, contentDescription = "Start Render")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Start Render Loop Video",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                } else {
                    FfmpegCircularProgressIndicator(
                        progress = uiState.jobProgress.progress,
                        statusText = uiState.jobProgress.statusText,
                        title = "FFmpeg Seamless Video Loop Processing",
                        accentColor = MaterialTheme.colorScheme.primary,
                        onCancel = { viewModel.cancelRenderJob() }
                    )
                }

                // Go Live Direct Pipeline
                AnimatedVisibility(visible = uiState.lastRenderedOutputUri != null) {
                    uiState.lastRenderedOutputUri?.let { renderedPath ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "Success",
                                    tint = MaterialTheme.colorScheme.secondary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Render Ready for Live Streaming!",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            }
                            Button(
                                onClick = { onNavigateToGoLive(renderedPath) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("go_live_with_this_button"),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                            ) {
                                Icon(imageVector = Icons.Default.Radio, contentDescription = "Go Live")
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Go Live with This Video", color = Color.Black, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

}
}

private fun formatTimeMs(timeMs: Long): String {
    val totalSeconds = (timeMs / 1000).toInt().coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%02d:%02d", minutes, seconds)
}
