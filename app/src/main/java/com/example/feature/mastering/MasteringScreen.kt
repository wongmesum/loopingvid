package com.example.feature.mastering
import androidx.compose.material.icons.filled.Info

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import com.example.core.ui.FfmpegCircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.media.AudioMasteringEngine
import com.example.core.ui.UndoRedoBar
import com.example.core.ui.VideoPlayer
import com.example.core.ui.ExportDialog

enum class MasteringToolCategory(val title: String, val icon: ImageVector, val description: String) {
    AUDIO_INPUT("Audio Track", Icons.Default.Audiotrack, "Load Audio & Media Player Preview"),
    PRESETS("Presets", Icons.Default.Bookmark, "Voice Clarity, Bass Boost, Podcast, EDM"),
    NOISE_REDUCTION("FFT Noise Filter", Icons.Default.GraphicEq, "Real-Time FFT Hiss & Static Reduction"),
    AUTO_LEVELING("Auto Leveling", Icons.Default.Autorenew, "Dynamic Normalization & LUFS Matching"),
    EQUALIZER("5-Band EQ", Icons.Default.Equalizer, "Low, Mid-Low, Mid, Mid-High & High"),
    GAIN_DYNAMICS("Gain & LUFS", Icons.Default.VolumeUp, "Input Gain, Output Gain & LUFS Target"),
    AUDIO_FADES("Track Fades", Icons.Default.Timeline, "Fade-In & Fade-Out Envelope"),
    AUDIO_METADATA("Metadata", Icons.Default.Info, "ID3 Tags: Title, Artist, Genre"),
    EXPORT("Export", Icons.Default.Download, "Export Format & Mastering Action"),
    ALL_TOOLS("All Tools", Icons.Default.FolderOpen, "View All Mastering Controls")
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun MasteringScreen(
    exportViewModel: com.example.core.ui.ExportViewModel,
    viewModel: MasteringViewModel,
    onNavigateToGoLive: (sourceUri: String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val undoRedoState by viewModel.undoRedoState.collectAsState()
    val scrollState = rememberScrollState()

    var activeCategory by remember { mutableStateOf(MasteringToolCategory.AUDIO_INPUT) }
    var isDropdownMenuExpanded by remember { mutableStateOf(false) }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            viewModel.onAudioSelected(it.toString(), it.lastPathSegment ?: "audio_track.mp3")
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
        // Undo / Redo Command Bar & Export Quick Button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.weight(1f)) {
                UndoRedoBar(
                    state = undoRedoState,
                    onUndo = { viewModel.undo() },
                    onRedo = { viewModel.redo() },
                    onClearHistory = { viewModel.clearHistory() }
                )
            }
            
            Spacer(modifier = Modifier.width(8.dp))
            
            Button(
                onClick = { 
                    if (uiState.selectedAudioUri != null) {
                        exportViewModel.showDialogForMastering("MasteredAudio", com.example.core.ui.ExportJobConfig.MasteringJob(
                            inputUri = uiState.selectedAudioUri!!,
                            presetName = uiState.selectedPreset.name,
                            targetLufs = uiState.targetLufs,
                            fadeInSec = uiState.fadeInSec,
                            fadeOutSec = uiState.fadeOutSec
                        ))
                    }
                },
                enabled = uiState.selectedAudioUri != null && !uiState.jobProgress.isProcessing,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                modifier = Modifier.height(42.dp)
            ) {
                Icon(imageVector = Icons.Default.Download, contentDescription = "Export", tint = Color.Black, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Export", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold), color = Color.Black)
            }
        }

        // Real-Time Waveform & Spectrum Canvas Card (Pinned Header)
        AudioWaveformCanvasCard(
            analysisData = uiState.analysisData,
            eqConfig = uiState.eqConfig,
            inputGainDb = uiState.inputGainDb,
            outputGainDb = uiState.outputGainDb,
            targetLufs = uiState.targetLufs,
            calculatedOutputLufs = uiState.calculatedOutputLufs,
            visualizerTheme = uiState.visualizerTheme,
            visualizerBarMode = uiState.visualizerBarMode,
            peakMeterStyle = uiState.peakMeterStyle,
            onThemeChanged = { viewModel.setVisualizerTheme(it) },
            onBarModeChanged = { viewModel.setVisualizerBarMode(it) },
            onPeakMeterStyleChanged = { viewModel.setPeakMeterStyle(it) }
        )

        // CapCut-Style Modern Tool Category Bar (Dropdown Menu + Category Chips)
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp, horizontal = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Mastering Tools",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Quick Jump Dropdown
                    Box {
                        Surface(
                            onClick = { isDropdownMenuExpanded = true },
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surface,
                            tonalElevation = 2.dp
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = activeCategory.icon,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = activeCategory.title,
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    imageVector = Icons.Default.ArrowDropDown,
                                    contentDescription = "Select Category",
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                        DropdownMenu(
                            expanded = isDropdownMenuExpanded,
                            onDismissRequest = { isDropdownMenuExpanded = false }
                        ) {
                            MasteringToolCategory.values().forEach { category ->
                                DropdownMenuItem(
                                    text = {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = category.icon,
                                                contentDescription = null,
                                                tint = if (category == activeCategory) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurface,
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Column {
                                                Text(
                                                    text = category.title,
                                                    style = MaterialTheme.typography.bodyMedium.copy(
                                                        fontWeight = if (category == activeCategory) FontWeight.Bold else FontWeight.Normal
                                                    )
                                                )
                                                Text(
                                                    text = category.description,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    },
                                    onClick = {
                                        activeCategory = category
                                        isDropdownMenuExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Scrollable Category Icons Bar
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(MasteringToolCategory.values()) { category ->
                        val isSelected = category == activeCategory
                        Surface(
                            onClick = { activeCategory = category },
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.surface,
                            modifier = Modifier.testTag("mastering_chip_${category.name}")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = category.icon,
                                    contentDescription = category.title,
                                    tint = if (isSelected) Color.Black else MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = category.title,
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                    ),
                                    color = if (isSelected) Color.Black else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }
        }

        // Active Category Content Area
        AnimatedContent(
            targetState = activeCategory,
            transitionSpec = {
                fadeIn(animationSpec = tween(durationMillis = 260, easing = FastOutSlowInEasing)) togetherWith
                        fadeOut(animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing))
            },
            label = "mastering_category_crossfade"
        ) { category ->
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                when (category) {
                    MasteringToolCategory.AUDIO_INPUT -> {
                        // Input Audio Selector Card
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
                                    text = "Select Input Audio Track",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(8.dp))

                                if (uiState.selectedAudioUri == null) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(110.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .border(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                                            .clickable { filePickerLauncher.launch("audio/*") }
                                            .testTag("select_audio_button"),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Icon(
                                                imageVector = Icons.Default.FolderOpen,
                                                contentDescription = "Pick Audio",
                                                tint = MaterialTheme.colorScheme.secondary,
                                                modifier = Modifier.size(32.dp)
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = "Tap to load audio file (MP3, WAV, M4A)",
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
                                        if (uiState.selectedAudioUri?.contains(".mp4", ignoreCase = true) == true ||
                                            uiState.selectedAudioUri?.contains(".mkv", ignoreCase = true) == true ||
                                            uiState.selectedAudioUri?.contains(".webm", ignoreCase = true) == true) {
                                            VideoPlayer(
                                                mediaUri = uiState.selectedAudioUri,
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(180.dp),
                                                testTag = "mastering_video_player"
                                            )
                                        }
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp))
                                                .padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Audiotrack,
                                                contentDescription = "Selected Audio",
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(28.dp)
                                            )
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = uiState.selectedAudioName,
                                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                                Text(
                                                    text = "Analyzed RMS: %.1f LUFS".format(uiState.analysisData?.currentRmsLufs ?: -22.0),
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.secondary
                                                )
                                            }
                                            OutlinedButton(
                                                onClick = { filePickerLauncher.launch("audio/*") },
                                                modifier = Modifier.testTag("change_audio_button")
                                            ) {
                                                Text("Change")
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "Live Audio Normalization Waveform",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        RealtimeWaveformVisualizer(
                                            analysisData = uiState.analysisData,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(64.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    MasteringToolCategory.PRESETS -> {
                        AudioEqPresetSelectorCard(
                            selectedPreset = uiState.selectedPreset,
                            customPresets = uiState.customPresets,
                            eqConfig = uiState.eqConfig,
                            inputGainDb = uiState.inputGainDb,
                            outputGainDb = uiState.outputGainDb,
                            targetLufs = uiState.targetLufs,
                            onPresetSelected = { viewModel.applyPreset(it) },
                            onSavePreset = { viewModel.saveCurrentPreset(it) },
                            onDeletePreset = { viewModel.deleteCustomPreset(it) }
                        )
                    }

                    MasteringToolCategory.NOISE_REDUCTION -> {
                        NoiseReductionControlCard(
                            config = uiState.noiseReductionConfig,
                            theme = uiState.visualizerTheme,
                            meterStyle = uiState.peakMeterStyle,
                            onToggleEnabled = { viewModel.toggleNoiseReduction(it) },
                            onReductionDbChanged = { viewModel.updateNoiseReductionDb(it) },
                            onNoiseFloorDbChanged = { viewModel.updateNoiseFloorDb(it) },
                            onFftSizeChanged = { viewModel.updateFftSize(it) }
                        )
                    }

                    MasteringToolCategory.AUTO_LEVELING -> {
                        AutoLevelingControlCard(
                            config = uiState.autoLevelingConfig,
                            onToggleEnabled = { viewModel.toggleAutoLeveling(it) },
                            onTargetLoudnessChanged = { viewModel.updateAutoLevelingTarget(it) }
                        )
                    }

                    MasteringToolCategory.EQUALIZER -> {
                        Audio5BandEqControlCard(
                            eqConfig = uiState.eqConfig,
                            onEqLowChanged = { viewModel.updateEqLow(it) },
                            onEqMidLowChanged = { viewModel.updateEqMidLow(it) },
                            onEqMidChanged = { viewModel.updateEqMid(it) },
                            onEqMidHighChanged = { viewModel.updateEqMidHigh(it) },
                            onEqHighChanged = { viewModel.updateEqHigh(it) }
                        )
                    }

                    MasteringToolCategory.GAIN_DYNAMICS -> {
                        AudioGainControlCard(
                            inputGainDb = uiState.inputGainDb,
                            outputGainDb = uiState.outputGainDb,
                            onInputGainChanged = { viewModel.updateInputGain(it) },
                            onOutputGainChanged = { viewModel.updateOutputGain(it) }
                        )
                    }

                                        MasteringToolCategory.AUDIO_METADATA -> {
                        AudioMetadataEditorCard(
                            metadata = uiState.audioMetadata,
                            onMetadataChanged = { viewModel.updateAudioMetadata(it) }
                        )
                    }
                    MasteringToolCategory.AUDIO_FADES -> {

                                                AudioMetadataEditorCard(
                            metadata = uiState.audioMetadata,
                            onMetadataChanged = { viewModel.updateAudioMetadata(it) }
                        )
                        AudioFadeControlCard(

                            fadeInSec = uiState.fadeInSec,
                            fadeOutSec = uiState.fadeOutSec,
                            onFadeInChanged = { viewModel.updateFadeInPreview(it) },
                            onFadeInChangeFinished = { viewModel.commitFadeInChange(it) },
                            onFadeOutChanged = { viewModel.updateFadeOutPreview(it) },
                            onFadeOutChangeFinished = { viewModel.commitFadeOutChange(it) }
                        )
                    }

                    MasteringToolCategory.EXPORT -> {
                        // Export Format Selector Card
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Export Audio Format", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold), color = MaterialTheme.colorScheme.onSurface)
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    listOf("MP3", "WAV", "M4A").forEach { fmt ->
                                        FilterChip(
                                            selected = uiState.exportFormat == fmt,
                                            onClick = { viewModel.setExportFormat(fmt) },
                                            label = { Text(fmt) },
                                            modifier = Modifier.testTag("format_chip_$fmt")
                                        )
                                    }
                                }
                            }
                        }

                        // Export Action & Progress
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
                                        onClick = { 
                                            if (uiState.selectedAudioUri != null) {
                                                exportViewModel.showDialogForMastering("MasteredAudio", com.example.core.ui.ExportJobConfig.MasteringJob(
                                                    inputUri = uiState.selectedAudioUri!!,
                                                    presetName = uiState.selectedPreset.name,
                                                    targetLufs = uiState.targetLufs,
                                                    fadeInSec = uiState.fadeInSec,
                                                    fadeOutSec = uiState.fadeOutSec
                                                ))
                                            }
                                        },
                                        enabled = uiState.selectedAudioUri != null,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(52.dp)
                                            .testTag("start_mastering_button"),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                                    ) {
                                        Icon(imageVector = Icons.Default.Equalizer, contentDescription = "Export Master", tint = Color.Black)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "Export Mastered Audio",
                                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                            color = Color.Black
                                        )
                                    }
                                    
                                    OutlinedButton(
                                        onClick = {
                                            if (uiState.selectedAudioUri != null) {
                                                exportViewModel.showDialogForTwoPassNormalization("NormalizedAudio", com.example.core.ui.ExportJobConfig.TwoPassAudioNormalizationJob(
                                                    inputUri = uiState.selectedAudioUri!!,
                                                    targetLufs = uiState.targetLufs
                                                ))
                                            }
                                        },
                                        enabled = uiState.selectedAudioUri != null,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(52.dp)
                                            .testTag("start_2pass_normalization_button"),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Icon(imageVector = Icons.Default.GraphicEq, contentDescription = "2-Pass Normalization", tint = MaterialTheme.colorScheme.secondary)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "Export 2-Pass Normalization",
                                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.secondary
                                        )
                                    }
                                } else {
                                    FfmpegCircularProgressIndicator(
                                        progress = uiState.jobProgress.progress,
                                        statusText = uiState.jobProgress.statusText,
                                        title = "FFmpeg Audio Mastering & LUFS Normalization",
                                        accentColor = MaterialTheme.colorScheme.secondary
                                    )
                                }

                                AnimatedVisibility(visible = uiState.lastMasteredOutputUri != null) {
                                    uiState.lastMasteredOutputUri?.let { outputPath ->
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                                                .padding(12.dp),
                                            verticalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(imageVector = Icons.Default.CheckCircle, contentDescription = "Mastered", tint = MaterialTheme.colorScheme.primary)
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text("Mastered Audio Exported!", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.primary)
                                            }
                                            Button(
                                                onClick = { onNavigateToGoLive(outputPath) },
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .testTag("use_in_live_button"),
                                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                                            ) {
                                                Icon(imageVector = Icons.Default.Radio, contentDescription = "Go Live")
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text("Use in Live Stream / Editor", color = Color.White, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    MasteringToolCategory.ALL_TOOLS -> {
                        AudioMetadataEditorCard(
                            metadata = uiState.audioMetadata,
                            onMetadataChanged = { viewModel.updateAudioMetadata(it) }
                        )
                        AudioFadeControlCard(
                            fadeInSec = uiState.fadeInSec,
                            fadeOutSec = uiState.fadeOutSec,
                            onFadeInChanged = { viewModel.updateFadeInPreview(it) },
                            onFadeInChangeFinished = { viewModel.commitFadeInChange(it) },
                            onFadeOutChanged = { viewModel.updateFadeOutPreview(it) },
                            onFadeOutChangeFinished = { viewModel.commitFadeOutChange(it) }
                        )

                        NoiseReductionControlCard(
                            config = uiState.noiseReductionConfig,
                            theme = uiState.visualizerTheme,
                            meterStyle = uiState.peakMeterStyle,
                            onToggleEnabled = { viewModel.toggleNoiseReduction(it) },
                            onReductionDbChanged = { viewModel.updateNoiseReductionDb(it) },
                            onNoiseFloorDbChanged = { viewModel.updateNoiseFloorDb(it) },
                            onFftSizeChanged = { viewModel.updateFftSize(it) }
                        )

                        AutoLevelingControlCard(
                            config = uiState.autoLevelingConfig,
                            onToggleEnabled = { viewModel.toggleAutoLeveling(it) },
                            onTargetLoudnessChanged = { viewModel.updateAutoLevelingTarget(it) }
                        )

                        AudioGainControlCard(
                            inputGainDb = uiState.inputGainDb,
                            outputGainDb = uiState.outputGainDb,
                            onInputGainChanged = { viewModel.updateInputGain(it) },
                            onOutputGainChanged = { viewModel.updateOutputGain(it) }
                        )

                        AudioEqPresetSelectorCard(
                            selectedPreset = uiState.selectedPreset,
                            customPresets = uiState.customPresets,
                            eqConfig = uiState.eqConfig,
                            inputGainDb = uiState.inputGainDb,
                            outputGainDb = uiState.outputGainDb,
                            targetLufs = uiState.targetLufs,
                            onPresetSelected = { viewModel.applyPreset(it) },
                            onSavePreset = { viewModel.saveCurrentPreset(it) },
                            onDeletePreset = { viewModel.deleteCustomPreset(it) }
                        )

                        Audio5BandEqControlCard(
                            eqConfig = uiState.eqConfig,
                            onEqLowChanged = { viewModel.updateEqLow(it) },
                            onEqMidLowChanged = { viewModel.updateEqMidLow(it) },
                            onEqMidChanged = { viewModel.updateEqMid(it) },
                            onEqMidHighChanged = { viewModel.updateEqMidHigh(it) },
                            onEqHighChanged = { viewModel.updateEqHigh(it) }
                        )
                    }
                }
            }
        }
    }
}

