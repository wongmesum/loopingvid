package com.example.feature.editor

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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PictureInPicture
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.media.AudioSpectrumVisualizer
import com.example.core.media.SpectrumStyle
import com.example.core.ui.UndoRedoBar
import com.example.core.ui.VideoPlayer
import com.example.core.ui.Media3SegmentTrimmer
import com.example.core.ui.ExportDialog
import com.example.core.ui.ExportQueueCard
import com.example.core.ui.FfmpegCircularProgressIndicator
import com.example.core.work.ExportQueueViewModel

import kotlinx.coroutines.delay
import androidx.compose.runtime.LaunchedEffect

enum class EditorToolCategory(val title: String, val icon: ImageVector, val description: String) {
    MEDIA("Media", Icons.Default.Movie, "Video, Audio & PiP Overlays"),
    TRIM_SPEED("Trim & Speed", Icons.Default.ContentCut, "Trimmer, Retime & Transitions"),
    COLOR("Color & Filters", Icons.Default.Tune, "Color Grading & Presets"),
    AUDIO_SPECTRUM("Audio & Spectrum", Icons.Default.GraphicEq, "Spectrum & Volume Mastering"),
    TEXT_CAPTIONS("Text & Captions", Icons.Default.Edit, "Title, Watermark & AI SRT"),
    TEMPLATES("Templates & Queue", Icons.Default.AutoFixHigh, "Presets, Auto-Save & Queue"),
    ALL_TOOLS("All Tools", Icons.Default.FolderOpen, "View All Editing Panels")
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun EditorScreen(
    exportViewModel: com.example.core.ui.ExportViewModel,
    viewModel: EditorViewModel,
    exportQueueViewModel: ExportQueueViewModel? = null,
    audioAnalysisRepository: com.example.core.audio.AudioAnalysisRepository? = null,
    onNavigateToGoLive: (sourceUri: String) -> Unit,
    assetManagerViewModel: com.example.feature.assets.AssetManagerViewModel? = null
) {
    var isLoading by remember { mutableStateOf(true) }
    
    LaunchedEffect(Unit) {
        delay(150)
        isLoading = false
    }

    if (isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                androidx.compose.material3.CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(16.dp))
                Text("Memuat Editor CapCut Style...", color = MaterialTheme.colorScheme.onBackground)
            }
        }
        return
    }

    val uiState by viewModel.uiState.collectAsState()
    val undoRedoState by viewModel.undoRedoState.collectAsState()
    val scrollState = rememberScrollState()

    var activeCategory by remember { mutableStateOf(EditorToolCategory.MEDIA) }
    var isDropdownMenuExpanded by remember { mutableStateOf(false) }

    // Collapsible Panel Expansion States
    var isMediaPanelExpanded by remember { mutableStateOf(true) }
    var isTrimPanelExpanded by remember { mutableStateOf(true) }
    var isSpeedPanelExpanded by remember { mutableStateOf(true) }
    var isTransitionPanelExpanded by remember { mutableStateOf(false) }
    var isColorPanelExpanded by remember { mutableStateOf(true) }
    var isBatchColorPanelExpanded by remember { mutableStateOf(false) }
    var isAudioSpectrumPanelExpanded by remember { mutableStateOf(true) }
    var isAudioMasteringPanelExpanded by remember { mutableStateOf(false) }
    var isTextCaptionsPanelExpanded by remember { mutableStateOf(true) }
    var isTemplatesPanelExpanded by remember { mutableStateOf(true) }
    var isAutoSavePanelExpanded by remember { mutableStateOf(false) }
    var isMuteMainTrack by remember { mutableStateOf(false) }
    var isGridSnapEnabled by remember { mutableStateOf(true) }

    fun expandAllPanels(expand: Boolean) {
        isMediaPanelExpanded = expand
        isTrimPanelExpanded = expand
        isSpeedPanelExpanded = expand
        isTransitionPanelExpanded = expand
        isColorPanelExpanded = expand
        isBatchColorPanelExpanded = expand
        isAudioSpectrumPanelExpanded = expand
        isAudioMasteringPanelExpanded = expand
        isTextCaptionsPanelExpanded = expand
        isTemplatesPanelExpanded = expand
        isAutoSavePanelExpanded = expand
    }

    val recordAsset = com.example.feature.assets.rememberAssetRecorder(assetManagerViewModel)

    val videoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            recordAsset(it)
            viewModel.onMediaSelected(it.toString(), it.lastPathSegment ?: "video.mp4")
        }
    }

    val audioPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            recordAsset(it)
            viewModel.onAudioSelected(it.toString(), it.lastPathSegment ?: "audio.mp3")
        }
    }

    val overlayPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            recordAsset(it)
            viewModel.onOverlaySelected(it.toString(), it.lastPathSegment ?: "overlay.mp4")
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
        // Undo / Redo Command Bar & Export Quick Action
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
            
            // Top Quick Export Button
            Button(
                onClick = { 
                    if (uiState.selectedMediaUri != null) {
                        exportViewModel.showDialogForEditor("EditorProject", com.example.core.ui.ExportJobConfig.EditorJob(
                            mediaUri = uiState.selectedMediaUri!!,
                            audioUri = uiState.selectedAudioUri,
                            titleText = uiState.titleText,
                            watermarkText = uiState.watermarkText,
                            spectrumStyle = uiState.spectrumStyle.name,
                            presetQuality = uiState.presetQuality,
                            ffmpegFilterString = uiState.colorGradingConfig.buildFfmpegFilterString(),
                            audioMasteringPreset = uiState.audioMasteringPreset,
                            overlayUri = uiState.selectedOverlayUri,
                            overlayPosition = uiState.pipPosition.name
                        ))
                    }
                },
                enabled = uiState.selectedMediaUri != null && !uiState.jobProgress.isProcessing,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                modifier = Modifier.height(42.dp)
            ) {
                Icon(imageVector = Icons.Default.Edit, contentDescription = "Export", modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Export", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold))
            }
        }

        // Live CapCut Preview Canvas
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "CapCut Studio Preview",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = if (uiState.selectedMediaUri != null) "Ready" else "No Media",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (uiState.selectedMediaUri != null) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(210.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFF0F0D1A)),
                    contentAlignment = Alignment.Center
                ) {
                    if (uiState.selectedMediaUri != null) {
                        VideoPlayer(
                            mediaUri = uiState.selectedMediaUri,
                            modifier = Modifier.fillMaxSize(),
                            isPlaying = uiState.isPreviewPlaying,
                            useController = false,
                            playbackSpeed = uiState.playbackSpeed,
                            colorMatrix = remember(uiState.colorGradingConfig) { uiState.colorGradingConfig.toComposeColorMatrix() },
                            testTag = "editor_video_player"
                        )
                    }

                    // Audio Spectrum Overlay inside Canvas
                    AudioSpectrumVisualizer(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(20.dp),
                        style = uiState.spectrumStyle,
                        isPlaying = uiState.isPreviewPlaying,
                        primaryColor = MaterialTheme.colorScheme.primary,
                        secondaryColor = MaterialTheme.colorScheme.tertiary
                    )

                    // Text Overlays
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Title Overlay
                        if (uiState.titleText.isNotBlank()) {
                            Text(
                                text = uiState.titleText,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = Color.White,
                                modifier = Modifier
                                    .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        } else {
                            Spacer(modifier = Modifier.height(1.dp))
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Bottom
                        ) {
                            // Watermark
                            if (uiState.watermarkText.isNotBlank()) {
                                Text(
                                    text = uiState.watermarkText,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier
                                        .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(6.dp))
                                        .padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            } else {
                                Spacer(modifier = Modifier.width(1.dp))
                            }

                            // Play/Pause Floating Controller
                            IconButton(
                                onClick = { viewModel.togglePreviewPlayback() },
                                modifier = Modifier
                                    .background(MaterialTheme.colorScheme.primary, CircleShape)
                                    .size(40.dp)
                            ) {
                                Icon(
                                    imageVector = if (uiState.isPreviewPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = "Play/Pause Preview",
                                    tint = Color.White
                                )
                            }
                        }
                    }
                }
            }
        }

        // CapCut-Style Quick Icon Tool Bar & Category Jump Dropdown
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "CapCut Video Tools",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Expand/Collapse All Quick Toggle Button
                        Surface(
                            onClick = {
                                val allExpanded = isMediaPanelExpanded && isTrimPanelExpanded && isSpeedPanelExpanded
                                expandAllPanels(!allExpanded)
                            },
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surface,
                            tonalElevation = 1.dp
                        ) {
                            Text(
                                text = if (isMediaPanelExpanded && isTrimPanelExpanded) "Collapse All" else "Expand All",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                            )
                        }

                        // Category Dropdown Button
                        Box {
                            Surface(
                                onClick = { isDropdownMenuExpanded = true },
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.surface,
                                tonalElevation = 1.dp
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = activeCategory.icon,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = activeCategory.title,
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.width(2.dp))
                                    Icon(
                                        imageVector = Icons.Default.ArrowDropDown,
                                        contentDescription = "Dropdown",
                                        tint = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }

                            DropdownMenu(
                                expanded = isDropdownMenuExpanded,
                                onDismissRequest = { isDropdownMenuExpanded = false }
                            ) {
                                EditorToolCategory.values().forEach { category ->
                                    DropdownMenuItem(
                                        text = {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    imageVector = category.icon,
                                                    contentDescription = null,
                                                    tint = if (category == activeCategory) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    text = category.title,
                                                    style = MaterialTheme.typography.bodyMedium.copy(
                                                        fontWeight = if (category == activeCategory) FontWeight.Bold else FontWeight.Normal
                                                    )
                                                )
                                            }
                                        },
                                        onClick = {
                                            activeCategory = category
                                            isDropdownMenuExpanded = false
                                            expandAllPanels(true)
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                QuickIconToolDock(
                    activeCategory = activeCategory,
                    onCategorySelect = { category ->
                        activeCategory = category
                        expandAllPanels(true)
                    }
                )
            }
        }

        // Animated Active Category Panel View
        AnimatedContent(
            targetState = activeCategory,
            transitionSpec = {
                fadeIn(animationSpec = tween(durationMillis = 260, easing = FastOutSlowInEasing)) togetherWith
                        fadeOut(animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing))
            },
            label = "editor_category_crossfade"
        ) { category ->
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                when (category) {
                    EditorToolCategory.MEDIA -> {
                        // Media Pickers Card (Collapsible)
                        CollapsibleToolPanel(
                            title = "Media Assets",
                            icon = Icons.Default.Movie,
                            isExpanded = isMediaPanelExpanded,
                            onToggleExpand = { isMediaPanelExpanded = !isMediaPanelExpanded },
                            statusBadgeText = uiState.selectedMediaName.ifBlank { "No Video" }
                        ) {
                            // Video/Image Pick Button
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(imageVector = Icons.Default.Movie, contentDescription = "Video", tint = MaterialTheme.colorScheme.tertiary)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = uiState.selectedMediaName.ifBlank { "No Video/Image Selected" },
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                OutlinedButton(
                                    onClick = { videoPickerLauncher.launch("video/*") },
                                    modifier = Modifier.testTag("pick_video_editor_button")
                                ) {
                                    Text("Select")
                                }
                            }

                            // Audio Pick Button
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(imageVector = Icons.Default.Audiotrack, contentDescription = "Audio", tint = MaterialTheme.colorScheme.secondary)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = uiState.selectedAudioName.ifBlank { "Default Audio Track" },
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                OutlinedButton(
                                    onClick = { audioPickerLauncher.launch("audio/*") },
                                    modifier = Modifier.testTag("pick_audio_editor_button")
                                ) {
                                    Text("Select")
                                }
                            }
                            
                            // PiP Overlay Video Pick Button
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(imageVector = Icons.Default.PictureInPicture, contentDescription = "Overlay Video", tint = MaterialTheme.colorScheme.tertiary)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = uiState.selectedOverlayName.ifBlank { "No PiP Overlay Selected" },
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                OutlinedButton(
                                    onClick = { overlayPickerLauncher.launch("video/*") }
                                ) {
                                    Text("Select PiP")
                                }
                            }
                            
                            // PiP Position Selection if overlay is present
                            if (uiState.selectedOverlayUri != null) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("PiP Position:", style = MaterialTheme.typography.labelMedium)
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        com.example.core.media.PipPosition.values().forEach { pos ->
                                            val isSelected = uiState.pipPosition == pos
                                            Surface(
                                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                                shape = RoundedCornerShape(8.dp),
                                                modifier = Modifier.clickable { viewModel.setPipPosition(pos) }
                                            ) {
                                                Text(
                                                    text = pos.displayName,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    EditorToolCategory.TRIM_SPEED -> {
                        // Collapsible Video Segment Timeline & Trimmer
                        CollapsibleToolPanel(
                            title = "Video Timeline & Trimmer",
                            icon = Icons.Default.ContentCut,
                            isExpanded = isTrimPanelExpanded,
                            onToggleExpand = { isTrimPanelExpanded = !isTrimPanelExpanded },
                            statusBadgeText = "%.1fs - %.1fs".format(uiState.trimStartSec, uiState.trimEndSec)
                        ) {
                            // Multi-Track Timeline Header Bar with Track Actions
                            MultiTrackTimelineHeader(
                                timecodeText = "%.1fs / %.1fs".format(uiState.trimStartSec, uiState.trimEndSec),
                                isMuted = isMuteMainTrack,
                                isGridSnapEnabled = isGridSnapEnabled,
                                onToggleMute = { isMuteMainTrack = !isMuteMainTrack },
                                onToggleGridSnap = { isGridSnapEnabled = !isGridSnapEnabled },
                                onSplitSegment = {
                                    val mid = (uiState.trimStartSec + uiState.trimEndSec) / 2.0
                                    viewModel.setTrim(uiState.trimStartSec, mid)
                                },
                                onDuplicateSegment = { viewModel.addLoopedSegment() },
                                onDeleteSegment = { viewModel.setTrim(0.0, 10.0) },
                                onResetTrim = { viewModel.setTrim(0.0, 30.0) }
                            )

                            // Precision Video Segment Trimmer
                            Media3SegmentTrimmer(
                                mediaUri = uiState.selectedMediaUri,
                                initialStartMs = (uiState.trimStartSec * 1000).toLong(),
                                initialEndMs = (uiState.trimEndSec * 1000).toLong(),
                                onTrimChange = { startMs, endMs ->
                                    viewModel.updateTrimPreview(startMs / 1000.0, endMs / 1000.0)
                                },
                                onApplyTrimmedSegment = { startMs, endMs ->
                                    viewModel.commitTrimChange(startMs / 1000.0, endMs / 1000.0)
                                },
                                audioAnalysisRepository = audioAnalysisRepository
                            )
                        }

                        // Collapsible Playback Speed Control
                        CollapsibleToolPanel(
                            title = "Playback Speed",
                            icon = Icons.Default.FastForward,
                            isExpanded = isSpeedPanelExpanded,
                            onToggleExpand = { isSpeedPanelExpanded = !isSpeedPanelExpanded },
                            statusBadgeText = "%.2fx Speed".format(uiState.playbackSpeed)
                        ) {
                            PlaybackSpeedControlCard(
                                currentSpeed = uiState.playbackSpeed,
                                onSpeedChange = { viewModel.setPlaybackSpeed(it) },
                                onResetSpeed = { viewModel.resetPlaybackSpeed() },
                                baseDurationSec = uiState.transitionConfig.calculateTotalDurationSec(),
                                queueViewModel = exportQueueViewModel
                            )
                        }

                        // Collapsible Segment Transitions
                        CollapsibleToolPanel(
                            title = "Segment Transitions",
                            icon = Icons.Default.AutoFixHigh,
                            isExpanded = isTransitionPanelExpanded,
                            onToggleExpand = { isTransitionPanelExpanded = !isTransitionPanelExpanded },
                            statusBadgeText = "${uiState.transitionConfig.segments.size} Segments"
                        ) {
                            SegmentTransitionControlCard(
                                transitionConfig = uiState.transitionConfig,
                                onUpdateSegmentTransition = { segId, effect -> viewModel.updateSegmentTransition(segId, effect) },
                                onUpdateSegmentTransitionDuration = { segId, dur -> viewModel.updateSegmentTransitionDuration(segId, dur) },
                                onUpdateSegmentLoopCount = { segId, count -> viewModel.updateSegmentLoopCount(segId, count) },
                                onAddSegment = { viewModel.addLoopedSegment() },
                                onRemoveSegment = { segId -> viewModel.removeLoopedSegment(segId) },
                                onApplyGlobalEffect = { effect -> viewModel.applyGlobalTransitionEffect(effect) },
                                queueViewModel = exportQueueViewModel
                            )
                        }
                    }

                    EditorToolCategory.COLOR -> {
                        // Collapsible Color Grading
                        CollapsibleToolPanel(
                            title = "Color Grading & Filters",
                            icon = Icons.Default.Tune,
                            isExpanded = isColorPanelExpanded,
                            onToggleExpand = { isColorPanelExpanded = !isColorPanelExpanded },
                            statusBadgeText = uiState.colorGradingConfig.preset.displayName
                        ) {
                            ColorGradingControlCard(
                                config = uiState.colorGradingConfig,
                                onPresetSelected = { viewModel.setColorFilterPreset(it) },
                                onBrightnessChange = { viewModel.updateFilterBrightness(it) },
                                onContrastChange = { viewModel.updateFilterContrast(it) },
                                onSaturationChange = { viewModel.updateFilterSaturation(it) },
                                onHueChange = { viewModel.updateFilterHue(it) },
                                onReset = { viewModel.resetColorGrading() },
                                onGradingChangeFinished = { viewModel.onColorGradingSliderFinished(it) },
                                customColorPresets = uiState.customColorPresets,
                                onSaveCustomPreset = { viewModel.saveCurrentColorConfigAsPreset(it) },
                                onApplyCustomPreset = { viewModel.applyCustomColorPreset(it) },
                                onDeleteCustomPreset = { viewModel.deleteCustomColorPreset(it) }
                            )
                        }

                        // Collapsible Batch Color Grading
                        CollapsibleToolPanel(
                            title = "Batch Color Filters",
                            icon = Icons.Default.Tune,
                            isExpanded = isBatchColorPanelExpanded,
                            onToggleExpand = { isBatchColorPanelExpanded = !isBatchColorPanelExpanded },
                            statusBadgeText = "Batch Mode"
                        ) {
                            BatchColorGradingCard(
                                currentConfig = uiState.colorGradingConfig,
                                onPresetSelect = { viewModel.setColorFilterPreset(it) },
                                queueViewModel = exportQueueViewModel
                            )
                        }
                    }

                    EditorToolCategory.AUDIO_SPECTRUM -> {
                        // Collapsible Audio Spectrum Visualizer
                        CollapsibleToolPanel(
                            title = "Audio Spectrum Visualizer",
                            icon = Icons.Default.GraphicEq,
                            isExpanded = isAudioSpectrumPanelExpanded,
                            onToggleExpand = { isAudioSpectrumPanelExpanded = !isAudioSpectrumPanelExpanded },
                            statusBadgeText = uiState.visualizerMode.name
                        ) {
                            InteractiveAudioSpectrumCard(
                                spectrumData = uiState.spectrumMagnitudes,
                                isPlaying = uiState.isPreviewPlaying,
                                peakDb = uiState.spectrumPeakDb,
                                rmsEnergy = uiState.spectrumRmsEnergy,
                                dominantFreqHz = uiState.spectrumDominantFreqHz,
                                sensitivityGain = uiState.spectrumSensitivityGain,
                                selectedMode = uiState.visualizerMode,
                                selectedPalette = uiState.selectedPalette,
                                bandCount = uiState.spectrumBandCount,
                                onModeSelected = { viewModel.setVisualizerMode(it) },
                                onPaletteSelected = { viewModel.setSpectrumPalette(it) },
                                onSensitivityChange = { viewModel.setSpectrumSensitivity(it) },
                                onBandCountChange = { viewModel.setSpectrumBandCount(it) }
                            )
                        }

                        // Collapsible Audio Mastering
                        CollapsibleToolPanel(
                            title = "Audio Mastering & EQ",
                            icon = Icons.Default.Audiotrack,
                            isExpanded = isAudioMasteringPanelExpanded,
                            onToggleExpand = { isAudioMasteringPanelExpanded = !isAudioMasteringPanelExpanded },
                            statusBadgeText = uiState.audioMasteringPreset.name
                        ) {
                            TrimmedVideoAudioMasteringCard(
                                mediaUri = uiState.selectedMediaUri,
                                trimStartSec = uiState.trimStartSec,
                                trimEndSec = uiState.trimEndSec,
                                exportViewModel = exportViewModel,
                                ffmpegFilterString = uiState.colorGradingConfig.buildFfmpegFilterString()
                            )
                        }
                    }

                    EditorToolCategory.TEXT_CAPTIONS -> {
                        // Collapsible Text & Captions Overlays
                        CollapsibleToolPanel(
                            title = "Text & Captions Overlays",
                            icon = Icons.Default.Edit,
                            isExpanded = isTextCaptionsPanelExpanded,
                            onToggleExpand = { isTextCaptionsPanelExpanded = !isTextCaptionsPanelExpanded },
                            statusBadgeText = if (uiState.titleText.isNotBlank()) "Title Set" else "No Title"
                        ) {
                            OutlinedTextField(
                                value = uiState.titleText,
                                onValueChange = { viewModel.updateTitleText(it) },
                                label = { Text("Title Overlay Text") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("title_input"),
                                singleLine = true
                            )

                            OutlinedTextField(
                                value = uiState.watermarkText,
                                onValueChange = { viewModel.updateWatermarkText(it) },
                                label = { Text("Watermark Text") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("watermark_input"),
                                singleLine = true
                            )

                            // AI Captions Generation
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("AI Auto Captions (Gemini)", style = MaterialTheme.typography.titleSmall)
                                    Text("Automatically transcribe audio track to SRT subtitles.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Button(
                                    onClick = { viewModel.generateAutoCaptions() },
                                    enabled = uiState.selectedMediaUri != null,
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                                ) {
                                    Text("Generate Captions")
                                }
                            }
                            
                            AnimatedVisibility(visible = uiState.aiGeneratedCaptions.isNotBlank()) {
                                OutlinedTextField(
                                    value = uiState.aiGeneratedCaptions,
                                    onValueChange = { },
                                    readOnly = true,
                                    label = { Text("Generated SRT Captions") },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(120.dp),
                                    maxLines = 5
                                )
                            }

                            // Spectrum Style Selector
                            Text("Spectrum Visualizer Style", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                SpectrumStyle.entries.forEach { style ->
                                    FilterChip(
                                        selected = uiState.spectrumStyle == style,
                                        onClick = { viewModel.setSpectrumStyle(style) },
                                        label = { Text(style.name) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                                            selectedLabelColor = Color.White
                                        ),
                                        modifier = Modifier.testTag("spectrum_chip_${style.name}")
                                    )
                                }
                            }
                        }
                    }

                    EditorToolCategory.TEMPLATES -> {
                        // Collapsible Project Templates
                        CollapsibleToolPanel(
                            title = "Project Templates",
                            icon = Icons.Default.AutoFixHigh,
                            isExpanded = isTemplatesPanelExpanded,
                            onToggleExpand = { isTemplatesPanelExpanded = !isTemplatesPanelExpanded },
                            statusBadgeText = if (uiState.selectedTemplateId != null) "Active" else "Presets"
                        ) {
                            ProjectTemplateControlCard(
                                templates = uiState.projectTemplates,
                                selectedTemplateId = uiState.selectedTemplateId,
                                onApplyTemplate = { viewModel.applyProjectTemplate(it) },
                                onSaveCurrentAsTemplate = { name, desc, cat -> viewModel.saveCurrentAsTemplate(name, desc, cat) },
                                onDeleteCustomTemplate = { viewModel.deleteCustomTemplate(it) },
                                queueViewModel = exportQueueViewModel
                            )
                        }

                        // Collapsible Auto-Save Control
                        CollapsibleToolPanel(
                            title = "Auto-Save Session",
                            icon = Icons.Default.FolderOpen,
                            isExpanded = isAutoSavePanelExpanded,
                            onToggleExpand = { isAutoSavePanelExpanded = !isAutoSavePanelExpanded },
                            statusBadgeText = if (uiState.autoSaveEnabled) "ON" else "OFF"
                        ) {
                            AutoSaveControlCard(
                                autoSaveEnabled = uiState.autoSaveEnabled,
                                autoSaveIntervalSec = uiState.autoSaveIntervalSec,
                                autoSaveInfo = uiState.autoSaveSessionInfo,
                                isSavingInProgress = uiState.isSavingInProgress,
                                lastSavedMessage = uiState.lastSavedMessage,
                                showRecoveryBanner = uiState.showRecoveryBanner,
                                onToggleAutoSave = { viewModel.toggleAutoSave(it) },
                                onSelectIntervalSec = { viewModel.setAutoSaveIntervalSec(it) },
                                onManualSaveNow = { viewModel.triggerManualSaveNow() },
                                onRestoreSession = { viewModel.restoreAutoSavedSession() },
                                onDiscardSession = { viewModel.discardAutoSavedSession() },
                                onDismissRecoveryBanner = { viewModel.dismissRecoveryBanner() }
                            )
                        }

                        // Export Queue Card
                        exportQueueViewModel?.let { queueVm ->
                            ExportQueueCard(
                                queueViewModel = queueVm,
                                onViewSummary = { item ->
                                    exportViewModel.showSummaryForJob(
                                        title = item.title,
                                        filePath = item.galleryUri ?: "/storage/emulated/0/Movies/${item.title}.${item.format}",
                                        format = item.format,
                                        jobType = item.jobType
                                    )
                                }
                            )
                        }
                    }

                    EditorToolCategory.ALL_TOOLS -> {
                        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            CollapsibleToolPanel(
                                title = "Media Assets",
                                icon = Icons.Default.Movie,
                                isExpanded = isMediaPanelExpanded,
                                onToggleExpand = { isMediaPanelExpanded = !isMediaPanelExpanded },
                                statusBadgeText = uiState.selectedMediaName.ifBlank { "No Video" }
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(imageVector = Icons.Default.Movie, contentDescription = "Video", tint = MaterialTheme.colorScheme.tertiary)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = uiState.selectedMediaName.ifBlank { "No Video/Image Selected" },
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                    OutlinedButton(
                                        onClick = { videoPickerLauncher.launch("video/*") }
                                    ) {
                                        Text("Select")
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(imageVector = Icons.Default.Audiotrack, contentDescription = "Audio", tint = MaterialTheme.colorScheme.secondary)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = uiState.selectedAudioName.ifBlank { "Default Audio Track" },
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                    OutlinedButton(
                                        onClick = { audioPickerLauncher.launch("audio/*") }
                                    ) {
                                        Text("Select")
                                    }
                                }
                            }

                            CollapsibleToolPanel(
                                title = "Video Timeline & Trimmer",
                                icon = Icons.Default.ContentCut,
                                isExpanded = isTrimPanelExpanded,
                                onToggleExpand = { isTrimPanelExpanded = !isTrimPanelExpanded },
                                statusBadgeText = "%.1fs - %.1fs".format(uiState.trimStartSec, uiState.trimEndSec)
                            ) {
                                MultiTrackTimelineHeader(
                                    timecodeText = "%.1fs / %.1fs".format(uiState.trimStartSec, uiState.trimEndSec),
                                    isMuted = isMuteMainTrack,
                                    isGridSnapEnabled = isGridSnapEnabled,
                                    onToggleMute = { isMuteMainTrack = !isMuteMainTrack },
                                    onToggleGridSnap = { isGridSnapEnabled = !isGridSnapEnabled },
                                    onSplitSegment = {
                                        val mid = (uiState.trimStartSec + uiState.trimEndSec) / 2.0
                                        viewModel.setTrim(uiState.trimStartSec, mid)
                                    },
                                    onDuplicateSegment = { viewModel.addLoopedSegment() },
                                    onDeleteSegment = { viewModel.setTrim(0.0, 10.0) },
                                    onResetTrim = { viewModel.setTrim(0.0, 30.0) }
                                )

                                Media3SegmentTrimmer(
                                    mediaUri = uiState.selectedMediaUri,
                                    initialStartMs = (uiState.trimStartSec * 1000).toLong(),
                                    initialEndMs = (uiState.trimEndSec * 1000).toLong(),
                                    onTrimChange = { startMs, endMs -> viewModel.updateTrimPreview(startMs / 1000.0, endMs / 1000.0) },
                                    onApplyTrimmedSegment = { startMs, endMs -> viewModel.commitTrimChange(startMs / 1000.0, endMs / 1000.0) },
                                    audioAnalysisRepository = audioAnalysisRepository
                                )
                            }

                            CollapsibleToolPanel(
                                title = "Color Grading & Filters",
                                icon = Icons.Default.Tune,
                                isExpanded = isColorPanelExpanded,
                                onToggleExpand = { isColorPanelExpanded = !isColorPanelExpanded },
                                statusBadgeText = uiState.colorGradingConfig.preset.displayName
                            ) {
                                ColorGradingControlCard(
                                    config = uiState.colorGradingConfig,
                                    onPresetSelected = { viewModel.setColorFilterPreset(it) },
                                    onBrightnessChange = { viewModel.updateFilterBrightness(it) },
                                    onContrastChange = { viewModel.updateFilterContrast(it) },
                                    onSaturationChange = { viewModel.updateFilterSaturation(it) },
                                    onHueChange = { viewModel.updateFilterHue(it) },
                                    onReset = { viewModel.resetColorGrading() },
                                    onGradingChangeFinished = { viewModel.onColorGradingSliderFinished(it) },
                                    customColorPresets = uiState.customColorPresets,
                                    onSaveCustomPreset = { viewModel.saveCurrentColorConfigAsPreset(it) },
                                    onApplyCustomPreset = { viewModel.applyCustomColorPreset(it) },
                                    onDeleteCustomPreset = { viewModel.deleteCustomColorPreset(it) }
                                )
                            }

                            CollapsibleToolPanel(
                                title = "Playback Speed",
                                icon = Icons.Default.FastForward,
                                isExpanded = isSpeedPanelExpanded,
                                onToggleExpand = { isSpeedPanelExpanded = !isSpeedPanelExpanded },
                                statusBadgeText = "%.2fx Speed".format(uiState.playbackSpeed)
                            ) {
                                PlaybackSpeedControlCard(
                                    currentSpeed = uiState.playbackSpeed,
                                    onSpeedChange = { viewModel.setPlaybackSpeed(it) },
                                    onResetSpeed = { viewModel.resetPlaybackSpeed() },
                                    baseDurationSec = uiState.transitionConfig.calculateTotalDurationSec(),
                                    queueViewModel = exportQueueViewModel
                                )
                            }

                            CollapsibleToolPanel(
                                title = "Audio Spectrum Visualizer",
                                icon = Icons.Default.GraphicEq,
                                isExpanded = isAudioSpectrumPanelExpanded,
                                onToggleExpand = { isAudioSpectrumPanelExpanded = !isAudioSpectrumPanelExpanded },
                                statusBadgeText = uiState.visualizerMode.name
                            ) {
                                InteractiveAudioSpectrumCard(
                                    spectrumData = uiState.spectrumMagnitudes,
                                    isPlaying = uiState.isPreviewPlaying,
                                    peakDb = uiState.spectrumPeakDb,
                                    rmsEnergy = uiState.spectrumRmsEnergy,
                                    dominantFreqHz = uiState.spectrumDominantFreqHz,
                                    sensitivityGain = uiState.spectrumSensitivityGain,
                                    selectedMode = uiState.visualizerMode,
                                    selectedPalette = uiState.selectedPalette,
                                    bandCount = uiState.spectrumBandCount,
                                    onModeSelected = { viewModel.setVisualizerMode(it) },
                                    onPaletteSelected = { viewModel.setSpectrumPalette(it) },
                                    onSensitivityChange = { viewModel.setSpectrumSensitivity(it) },
                                    onBandCountChange = { viewModel.setSpectrumBandCount(it) }
                                )
                            }
                        }
                    }
                }
            }
        }

        // Persistent Export Action & Go Live Card
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
                        onClick = { exportViewModel.showDialogForEditor("EditorProject", com.example.core.ui.ExportJobConfig.EditorJob(
                                mediaUri = uiState.selectedMediaUri!!,
                                audioUri = uiState.selectedAudioUri,
                                titleText = uiState.titleText,
                                watermarkText = uiState.watermarkText,
                                spectrumStyle = uiState.spectrumStyle.name,
                                presetQuality = uiState.presetQuality,
                                ffmpegFilterString = uiState.colorGradingConfig.buildFfmpegFilterString(),
                                audioMasteringPreset = uiState.audioMasteringPreset,
                                overlayUri = uiState.selectedOverlayUri,
                                overlayPosition = uiState.pipPosition.name
                            )) },
                        enabled = uiState.selectedMediaUri != null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("export_editor_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(imageVector = Icons.Default.Edit, contentDescription = "Export Video", tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Export Video Composition", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                    }
                } else {
                    FfmpegCircularProgressIndicator(
                        progress = uiState.jobProgress.progress,
                        statusText = uiState.jobProgress.statusText,
                        title = "FFmpeg Video Composition Export",
                        accentColor = MaterialTheme.colorScheme.primary
                    )
                }

                AnimatedVisibility(visible = uiState.lastExportedOutputUri != null) {
                    uiState.lastExportedOutputUri?.let { outputPath ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(imageVector = Icons.Default.CheckCircle, contentDescription = "Exported", tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Video Composition Exported!", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.primary)
                            }
                            Button(
                                onClick = { onNavigateToGoLive(outputPath) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("go_live_from_editor_button"),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                            ) {
                                Icon(imageVector = Icons.Default.Radio, contentDescription = "Go Live")
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Go Live with this Video Composition", color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

