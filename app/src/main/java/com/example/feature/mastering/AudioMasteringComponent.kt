package com.example.feature.mastering

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Pause
import com.example.core.media.VisualizerTheme
import com.example.core.media.VisualizerBarMode
import com.example.core.media.PeakMeterStyle
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.animateFloat

import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.media.AudioAnalysisData
import com.example.core.media.AudioMasteringEngine
import com.example.core.media.EqBandConfig
import com.example.core.media.MasteringPreset
import kotlinx.coroutines.isActive
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Modern, high-fidelity Audio Mastering Composable Component.
 * Incorporates real-time animated frequency spectrum visualization, EQ response curve overlay,
 * gain adjustments, EQ preset selection, 5-band fine-tuning, and peak meters.
 */
@Composable
fun AudioMasteringComponent(
    analysisData: AudioAnalysisData?,
    selectedPreset: MasteringPreset,
    customPresets: List<MasteringPreset> = emptyList(),
    eqConfig: EqBandConfig,
    inputGainDb: Float,
    outputGainDb: Float,
    targetLufs: Double,
    calculatedOutputLufs: Double,
    visualizerTheme: VisualizerTheme = VisualizerTheme.CYAN_PINK,
    visualizerBarMode: VisualizerBarMode = VisualizerBarMode.BARS,
    peakMeterStyle: PeakMeterStyle = PeakMeterStyle.SEGMENTED_LED,
    onPresetSelected: (MasteringPreset) -> Unit,
    onSavePreset: (String) -> Unit = {},
    onDeletePreset: (MasteringPreset) -> Unit = {},
    onInputGainChanged: (Float) -> Unit,
    onOutputGainChanged: (Float) -> Unit,
    onEqLowChanged: (Float) -> Unit,
    onEqMidLowChanged: (Float) -> Unit,
    onEqMidChanged: (Float) -> Unit,
    onEqMidHighChanged: (Float) -> Unit,
    onEqHighChanged: (Float) -> Unit,
    onThemeChanged: (VisualizerTheme) -> Unit = {},
    onBarModeChanged: (VisualizerBarMode) -> Unit = {},
    onPeakMeterStyleChanged: (PeakMeterStyle) -> Unit = {},
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Audio Waveform & Real-Time Spectrum Canvas Component
        AudioWaveformCanvasCard(
            analysisData = analysisData,
            eqConfig = eqConfig,
            inputGainDb = inputGainDb,
            outputGainDb = outputGainDb,
            targetLufs = targetLufs,
            calculatedOutputLufs = calculatedOutputLufs,
            visualizerTheme = visualizerTheme,
            visualizerBarMode = visualizerBarMode,
            peakMeterStyle = peakMeterStyle,
            onThemeChanged = onThemeChanged,
            onBarModeChanged = onBarModeChanged,
            onPeakMeterStyleChanged = onPeakMeterStyleChanged
        )

        // 2. Audio Gain Controls Component
        AudioGainControlCard(
            inputGainDb = inputGainDb,
            outputGainDb = outputGainDb,
            onInputGainChanged = onInputGainChanged,
            onOutputGainChanged = onOutputGainChanged
        )

        // 3. Audio Mastering Presets Selector Component ('Bass Boost', 'Voice Clarity', 'Noise Reduction', etc.)
        AudioMasteringPresetSelector(
            selectedPreset = selectedPreset,
            customPresets = customPresets,
            onPresetSelected = onPresetSelected,
            onSaveCustomPreset = onSavePreset,
            onDeleteCustomPreset = onDeletePreset
        )

        // 4. 5-Band Equalizer Fine-Tuning Component
        Audio5BandEqControlCard(
            eqConfig = eqConfig,
            onEqLowChanged = onEqLowChanged,
            onEqMidLowChanged = onEqMidLowChanged,
            onEqMidChanged = onEqMidChanged,
            onEqMidHighChanged = onEqMidHighChanged,
            onEqHighChanged = onEqHighChanged
        )
    }
}

/**
 * Custom Canvas Implementation for Real-Time Frequency Spectrum Visualization,
 * Dynamic EQ Curve Overlay, and Dual-Stereo VU Peak Metering.
 */
@Composable
internal fun AudioWaveformCanvasCard(
    analysisData: AudioAnalysisData?,
    eqConfig: EqBandConfig,
    inputGainDb: Float,
    outputGainDb: Float,
    targetLufs: Double,
    calculatedOutputLufs: Double,
    visualizerTheme: VisualizerTheme = VisualizerTheme.CYAN_PINK,
    visualizerBarMode: VisualizerBarMode = VisualizerBarMode.BARS,
    peakMeterStyle: PeakMeterStyle = PeakMeterStyle.SEGMENTED_LED,
    onThemeChanged: (VisualizerTheme) -> Unit = {},
    onBarModeChanged: (VisualizerBarMode) -> Unit = {},
    onPeakMeterStyleChanged: (PeakMeterStyle) -> Unit = {}
) {
    var isPlaying by remember { mutableStateOf(true) }
    var playheadRatio by remember { mutableFloatStateOf(0.35f) }
    var timeMillis by remember { mutableStateOf(0L) }
    var selectedVisualMode by remember { mutableStateOf("FULL") } // "FULL", "SPECTRUM", "EQ"
    var isStyleCustomizerExpanded by remember { mutableStateOf(false) }
    var peakThreshold by remember { mutableFloatStateOf(0.70f) }
    var quietThreshold by remember { mutableFloatStateOf(0.25f) }

    // Smoothly animate all gain parameters for fluid, professional real-time visual feedback
    val animLowGain by animateFloatAsState(targetValue = eqConfig.lowGainDb, animationSpec = tween(120), label = "low")
    val animMidLowGain by animateFloatAsState(targetValue = eqConfig.midLowGainDb, animationSpec = tween(120), label = "midLow")
    val animMidGain by animateFloatAsState(targetValue = eqConfig.midGainDb, animationSpec = tween(120), label = "mid")
    val animMidHighGain by animateFloatAsState(targetValue = eqConfig.midHighGainDb, animationSpec = tween(120), label = "midHigh")
    val animHighGain by animateFloatAsState(targetValue = eqConfig.highGainDb, animationSpec = tween(120), label = "high")
    val animInputGain by animateFloatAsState(targetValue = inputGainDb, animationSpec = tween(120), label = "inGain")
    val animOutputGain by animateFloatAsState(targetValue = outputGainDb, animationSpec = tween(120), label = "outGain")

    // Continuous real-time frame clock for 60fps audio frequency animation
    LaunchedEffect(isPlaying) {
        var lastNanos = System.nanoTime()
        while (isActive && isPlaying) {
            withFrameNanos { frameNanos ->
                val deltaMs = ((frameNanos - lastNanos) / 1_000_000L).coerceIn(1L, 50L)
                lastNanos = frameNanos
                timeMillis += deltaMs
                playheadRatio = (playheadRatio + (deltaMs / 22000f)) % 1.0f
            }
        }
    }

    val isClipping = calculatedOutputLufs > -0.5 || (inputGainDb + outputGainDb) > 14f

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("audio_waveform_card"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { isPlaying = !isPlaying },
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer)
                            .testTag("play_pause_canvas_button")
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isPlaying) "Pause Visualizer" else "Play Visualizer",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "Realtime Audio Canvas",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (isPlaying) "Live 60fps Spectrum & EQ Response" else "Paused Canvas Preview",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    IconButton(
                        onClick = { isStyleCustomizerExpanded = !isStyleCustomizerExpanded },
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(if (isStyleCustomizerExpanded) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                            .testTag("customize_visualizer_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Palette,
                            contentDescription = "Customize Colors and Styles",
                            tint = if (isStyleCustomizerExpanded) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    if (isClipping) {
                        Surface(
                            color = Color(0xFFEF4444).copy(alpha = 0.2f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = "Clipping",
                                    tint = Color(0xFFEF4444),
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "CLIP",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = Color(0xFFEF4444)
                                )
                            }
                        }
                    }

                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Text(
                            text = "%.1f LUFS".format(calculatedOutputLufs),
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            AnimatedVisibility(visible = isStyleCustomizerExpanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                        .background(Color(0xFF0F172A), RoundedCornerShape(12.dp))
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Visualizer Theme & Accessibility",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color.White
                    )

                    // Palette Theme Selector
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("COLOR PALETTE", style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp), color = Color.Gray)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            VisualizerTheme.entries.forEach { theme ->
                                val isSelected = theme == visualizerTheme
                                Surface(
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else Color(0xFF1E293B),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { onThemeChanged(theme) }
                                        .testTag("theme_chip_${theme.id}")
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center,
                                        modifier = Modifier.padding(vertical = 6.dp, horizontal = 2.dp)
                                    ) {
                                        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                            Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(theme.bassColor))
                                            Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(theme.midColor))
                                            Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(theme.trebleColor))
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = theme.displayName.split(" ").first(),
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                fontSize = 9.sp
                                            ),
                                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else Color.LightGray
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Bar Mode & Peak Meter Style Row
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("WAVEFORM STYLE", style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp), color = Color.Gray)
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                VisualizerBarMode.entries.forEach { mode ->
                                    val isSelected = mode == visualizerBarMode
                                    Surface(
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else Color(0xFF1E293B),
                                        shape = RoundedCornerShape(6.dp),
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable { onBarModeChanged(mode) }
                                            .testTag("barmode_chip_${mode.id}")
                                    ) {
                                        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(vertical = 6.dp)) {
                                            Text(
                                                text = mode.displayName.split(" ").first(),
                                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal),
                                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else Color.LightGray
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("METER STYLE", style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp), color = Color.Gray)
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                PeakMeterStyle.entries.forEach { style ->
                                    val isSelected = style == peakMeterStyle
                                    Surface(
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else Color(0xFF1E293B),
                                        shape = RoundedCornerShape(6.dp),
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable { onPeakMeterStyleChanged(style) }
                                            .testTag("meterstyle_chip_${style.id}")
                                    ) {
                                        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(vertical = 6.dp)) {
                                            Text(
                                                text = style.displayName.split(" ").first(),
                                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal),
                                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else Color.LightGray
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Visual Mode Tabs
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                listOf(
                    "FULL" to "Full Spectrum",
                    "WAVEFORM" to "Waveform Peaks",
                    "SPECTRUM" to "Spectrum Bars",
                    "EQ" to "EQ Curve"
                ).forEach { (mode, label) ->
                    val isSelected = selectedVisualMode == mode
                    Surface(
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .weight(1f)
                            .clickable { selectedVisualMode = mode }
                            .testTag("canvas_mode_$mode")
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(vertical = 5.dp)) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 10.sp
                                ),
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Realtime Volume Levels & Peak Metric Banner
            val safeOutputLufs = if (calculatedOutputLufs.isNaN() || calculatedOutputLufs.isInfinite()) -14.0 else calculatedOutputLufs
            val rawPeakDbVal = safeOutputLufs + 12.8 + (inputGainDb + outputGainDb) * 0.5
            val peakDbVal = if (rawPeakDbVal.isNaN() || rawPeakDbVal.isInfinite()) -1.0 else rawPeakDbVal.coerceIn(-36.0, 3.0)
            val rawRmsDbVal = safeOutputLufs + 4.2
            val rmsDbVal = if (rawRmsDbVal.isNaN() || rawRmsDbVal.isInfinite()) -10.0 else rawRmsDbVal.coerceIn(-48.0, -2.0)
            val headroomDb = (-0.1 - peakDbVal).coerceAtLeast(0.0)

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF0F172A), RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Column {
                        Text("PEAK LEVEL", style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold), color = Color.Gray)
                        Text(
                            text = if (peakDbVal > 0.0) "+%.1f dB CLIP!".format(peakDbVal) else "%.1f dBFS".format(peakDbVal),
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = if (peakDbVal > 0.0) Color(0xFFEF4444) else if (peakDbVal > -2.0) Color(0xFFF59E0B) else Color(0xFF10B981)
                        )
                    }
                    Column {
                        Text("RMS LEVEL", style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold), color = Color.Gray)
                        Text(
                            text = "%.1f dB".format(rmsDbVal),
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color(0xFF38BDF8)
                        )
                    }
                    Column {
                        Text("HEADROOM", style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold), color = Color.Gray)
                        Text(
                            text = "%.1f dB".format(headroomDb),
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = if (headroomDb < 0.5) Color(0xFFF59E0B) else Color(0xFFA855F7)
                        )
                    }
                }

                Surface(
                    color = Color(0xFF1E293B),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = "LUFS: %.1f".format(calculatedOutputLufs),
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            // Custom Realtime Canvas Audio Visualizer
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
                    .clip(RoundedCornerShape(bottomStart = 12.dp, bottomEnd = 12.dp))
                    .background(Color(0xFF0F172A)) // Dark studio canvas theme
                    .pointerInput(Unit) {
                        detectTapGestures { offset ->
                            playheadRatio = (offset.x / size.width).coerceIn(0f, 1f)
                        }
                    }
                    .testTag("waveform_canvas")
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height

                    val meterWidth = 50f
                    val availableWidth = w - meterWidth - 12f
                    val centerY = h / 2f

                    // 1. Draw dB Reference Grid Lines & Band Labels
                    val dbLines = listOf(
                        0.20f to "+12 dB",
                        0.35f to "+6 dB",
                        0.50f to "0 dB",
                        0.65f to "-6 dB",
                        0.80f to "-12 dB"
                    )

                    dbLines.forEach { (ratio, label) ->
                        val lineY = h * ratio
                        drawLine(
                            color = Color(0xFF334155).copy(alpha = if (ratio == 0.5f) 0.6f else 0.3f),
                            start = Offset(0f, lineY),
                            end = Offset(availableWidth, lineY),
                            strokeWidth = if (ratio == 0.5f) 1.5f else 1f,
                            pathEffect = if (ratio != 0.5f) PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f) else null
                        )
                    }

                    // Frequency Band Vertical Guides (60Hz, 250Hz, 1kHz, 4kHz, 12kHz)
                    val bandFreqPositions = listOf(
                        0.10f to "60Hz",
                        0.30f to "250Hz",
                        0.50f to "1kHz",
                        0.70f to "4kHz",
                        0.90f to "12kHz"
                    )

                    bandFreqPositions.forEach { (xRatio, freqLabel) ->
                        val posX = availableWidth * xRatio
                        drawLine(
                            color = Color(0xFF334155).copy(alpha = 0.25f),
                            start = Offset(posX, 0f),
                            end = Offset(posX, h),
                            strokeWidth = 1f,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 4f), 0f)
                        )
                    }

                    // Waveform and spectrum need real analysis output. When it is absent we skip only
                    // those blocks instead of returning from the Canvas, so the EQ curve, playhead and
                    // VU meters -- which derive from actual settings -- stay on screen.
                    val realWaveformPoints = analysisData?.waveformPoints ?: emptyList()

                    // 2. Pure Time-Domain Oscillating Audio Waveform (WAVEFORM mode)
                    if (selectedVisualMode == "WAVEFORM" && realWaveformPoints.isNotEmpty()) {
                        val points = realWaveformPoints
                        val pointCount = points.size
                        val barWidth = availableWidth / pointCount.toFloat()
                        val gainScale = 10.0.pow((animInputGain + animOutputGain) / 20.0).toFloat()

                        val topEnvelopePath = Path()
                        val bottomEnvelopePath = Path()

                        points.forEachIndexed { i, valNorm ->
                            val xNorm = i.toFloat() / pointCount.toFloat()
                            val eqGain = calculateEqGainNormalized(
                                xNorm = xNorm,
                                low = animLowGain,
                                midLow = animMidLowGain,
                                mid = animMidGain,
                                midHigh = animMidHighGain,
                                high = animHighGain
                            )
                            val totalGain = gainScale * 10.0.pow(eqGain / 20.0).toFloat()

                            val livePulse = if (isPlaying) {
                                1.0f + 0.20f * sin(timeMillis / 90f + i * 0.28f).toFloat()
                            } else 1.0f

                            val scaledAmp = (valNorm * totalGain * livePulse).coerceIn(0.06f, 0.96f)
                            val barHeight = (h * 0.80f) * scaledAmp
                            val x = i * barWidth + (barWidth * 0.1f)
                            val topY = centerY - (barHeight / 2f)
                            val bottomY = centerY + (barHeight / 2f)

                            val isPastPlayhead = (x / availableWidth) <= playheadRatio

                            if (i == 0) {
                                topEnvelopePath.moveTo(x, topY)
                                bottomEnvelopePath.moveTo(x, bottomY)
                            } else {
                                topEnvelopePath.lineTo(x, topY)
                                bottomEnvelopePath.lineTo(x, bottomY)
                            }

                            // Render Symmetrical Waveform Bar according to theme & bar mode
                            val barColor = if (isPastPlayhead) {
                                when {
                                    scaledAmp > peakThreshold -> Color(0xFFF43F5E) // Bright Coral Peak Alert
                                    scaledAmp < quietThreshold -> Color(0xFF38BDF8).copy(alpha = 0.5f) // Ice Blue Quiet
                                    else -> visualizerTheme.waveformColor
                                }
                            } else {
                                Color(0xFF334155).copy(alpha = 0.4f)
                            }

                            when (visualizerBarMode) {
                                VisualizerBarMode.BARS, VisualizerBarMode.SMOOTH_CURVE -> {
                                    drawRoundRect(
                                        color = barColor,
                                        topLeft = Offset(x, topY),
                                        size = Size((barWidth * 0.8f).coerceAtLeast(2f), barHeight),
                                        cornerRadius = CornerRadius(2f, 2f)
                                    )
                                }
                                VisualizerBarMode.DOTS -> {
                                    val dotCount = (barHeight / 8f).toInt().coerceIn(2, 8)
                                    val dotSpacing = barHeight / dotCount
                                    for (d in 0 until dotCount) {
                                        val dotY = topY + d * dotSpacing
                                        drawCircle(
                                            color = barColor,
                                            radius = (barWidth * 0.35f).coerceIn(1.5f, 4f),
                                            center = Offset(x + barWidth * 0.4f, dotY)
                                        )
                                    }
                                }
                            }

                            // Peak Hold Indicators
                            drawRect(
                                color = if (isPastPlayhead) visualizerTheme.peakColor else Color.Gray,
                                topLeft = Offset(x, topY - 2f),
                                size = Size(barWidth * 0.8f, 2f)
                            )
                            drawRect(
                                color = if (isPastPlayhead) visualizerTheme.peakColor else Color.Gray,
                                topLeft = Offset(x, bottomY),
                                size = Size(barWidth * 0.8f, 2f)
                            )
                        }

                        // Draw Waveform Peak Envelope Line
                        drawPath(
                            path = topEnvelopePath,
                            color = visualizerTheme.waveformColor.copy(alpha = 0.85f),
                            style = Stroke(width = if (visualizerBarMode == VisualizerBarMode.SMOOTH_CURVE) 3.5f else 1.5f)
                        )
                        drawPath(
                            path = bottomEnvelopePath,
                            color = visualizerTheme.waveformColor.copy(alpha = 0.85f),
                            style = Stroke(width = if (visualizerBarMode == VisualizerBarMode.SMOOTH_CURVE) 3.5f else 1.5f)
                        )

                        // Draw Symmetrical Peak Threshold lines on waveform
                        val peakYOffset = (h * 0.80f) * peakThreshold / 2f
                        val peakLineTop = centerY - peakYOffset
                        val peakLineBottom = centerY + peakYOffset

                        drawLine(
                            color = Color(0xFFF43F5E).copy(alpha = 0.7f),
                            start = Offset(0f, peakLineTop),
                            end = Offset(availableWidth, peakLineTop),
                            strokeWidth = 1.5f,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 6f), 0f)
                        )
                        drawLine(
                            color = Color(0xFFF43F5E).copy(alpha = 0.7f),
                            start = Offset(0f, peakLineBottom),
                            end = Offset(availableWidth, peakLineBottom),
                            strokeWidth = 1.5f,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 6f), 0f)
                        )

                        // Draw Symmetrical Quiet Threshold lines on waveform
                        val quietYOffset = (h * 0.80f) * quietThreshold / 2f
                        val quietLineTop = centerY - quietYOffset
                        val quietLineBottom = centerY + quietYOffset

                        drawLine(
                            color = Color(0xFF38BDF8).copy(alpha = 0.6f),
                            start = Offset(0f, quietLineTop),
                            end = Offset(availableWidth, quietLineTop),
                            strokeWidth = 1.5f,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f), 0f)
                        )
                        drawLine(
                            color = Color(0xFF38BDF8).copy(alpha = 0.6f),
                            start = Offset(0f, quietLineBottom),
                            end = Offset(availableWidth, quietLineBottom),
                            strokeWidth = 1.5f,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f), 0f)
                        )
                    }

                    // 3. Real-Time Audio Frequency Spectrum Bars (when FULL or SPECTRUM mode)
                    if ((selectedVisualMode == "FULL" || selectedVisualMode == "SPECTRUM") &&
                        realWaveformPoints.isNotEmpty()
                    ) {
                        val points = realWaveformPoints
                        val barCount = points.size
                        val barWidth = (availableWidth / barCount).coerceAtLeast(2.5f)

                        val gainScale = 10.0.pow((animInputGain + animOutputGain) / 20.0).toFloat()

                        points.forEachIndexed { i, valNorm ->
                            val xNorm = i.toFloat() / barCount.toFloat()

                            // Compute EQ response for this bar's frequency
                            val eqGainForBar = calculateEqGainNormalized(
                                xNorm = xNorm,
                                low = animLowGain,
                                midLow = animMidLowGain,
                                mid = animMidGain,
                                midHigh = animMidHighGain,
                                high = animHighGain
                            )

                            val totalBandGainScale = gainScale * 10.0.pow(eqGainForBar / 20.0).toFloat()

                            // Real-time oscillation pulse for dynamic audio frequency movement
                            val livePulse = if (isPlaying) {
                                1.0f + 0.25f * sin(timeMillis / 120f + i * 0.32f).toFloat() +
                                       0.12f * cos(timeMillis / 65f - i * 0.55f).toFloat()
                            } else {
                                1.0f
                            }

                            val scaledAmp = (valNorm * totalBandGainScale * livePulse).coerceIn(0.04f, 0.96f)
                            val barHeight = (h * 0.82f) * scaledAmp
                            val x = i * barWidth
                            val topY = centerY - (barHeight / 2f)

                            val isPastPlayhead = (x / availableWidth) <= playheadRatio

                            // Frequency-based color mapping using theme colors
                            val baseColors = when {
                                xNorm < 0.25f -> listOf(visualizerTheme.bassColor, visualizerTheme.bassColor.copy(alpha = 0.7f))
                                xNorm < 0.65f -> listOf(visualizerTheme.midColor, visualizerTheme.midColor.copy(alpha = 0.7f))
                                else -> listOf(visualizerTheme.trebleColor, visualizerTheme.trebleColor.copy(alpha = 0.7f))
                            }

                            val drawColors = if (isPastPlayhead) {
                                baseColors
                            } else {
                                baseColors.map { it.copy(alpha = 0.35f) }
                            }

                            when (visualizerBarMode) {
                                VisualizerBarMode.BARS, VisualizerBarMode.SMOOTH_CURVE -> {
                                    drawRoundRect(
                                        brush = Brush.verticalGradient(
                                            colors = drawColors,
                                            startY = topY,
                                            endY = topY + barHeight
                                        ),
                                        topLeft = Offset(x, topY),
                                        size = Size(barWidth * 0.75f, barHeight),
                                        cornerRadius = CornerRadius(2f, 2f)
                                    )
                                }
                                VisualizerBarMode.DOTS -> {
                                    val dotCount = (barHeight / 7f).toInt().coerceIn(2, 10)
                                    val dotSpacing = barHeight / dotCount
                                    for (d in 0 until dotCount) {
                                        val dotY = topY + d * dotSpacing
                                        drawCircle(
                                            color = drawColors.first(),
                                            radius = (barWidth * 0.35f).coerceIn(1.5f, 4f),
                                            center = Offset(x + barWidth * 0.37f, dotY)
                                        )
                                    }
                                }
                            }

                            // Peak Hold Tick with theme peak color
                            val peakTickY = (topY - 3f).coerceAtLeast(2f)
                            drawRect(
                                color = if (isPastPlayhead) visualizerTheme.peakColor else Color.Gray.copy(alpha = 0.5f),
                                topLeft = Offset(x, peakTickY),
                                size = Size(barWidth * 0.75f, 2f)
                            )
                        }
                    }

                    // 4. Dynamic 5-Band EQ Response Curve Overlay (when FULL or EQ mode)
                    if (selectedVisualMode == "FULL" || selectedVisualMode == "EQ") {
                        val curvePath = Path()
                        val fillPath = Path()

                        val steps = 80
                        fillPath.moveTo(0f, centerY)

                        for (step in 0..steps) {
                            val xNorm = step.toFloat() / steps.toFloat()
                            val posX = availableWidth * xNorm

                            val eqGain = calculateEqGainNormalized(
                                xNorm = xNorm,
                                low = animLowGain,
                                midLow = animMidLowGain,
                                mid = animMidGain,
                                midHigh = animMidHighGain,
                                high = animHighGain
                            )

                            // Map -12dB..+12dB gain to Y coordinates relative to centerY
                            val maxBound = (h - 10f).coerceAtLeast(10f)
                            val posY = (centerY - (eqGain / 18f) * (h * 0.4f)).coerceIn(10f, maxBound)

                            if (step == 0) {
                                curvePath.moveTo(posX, posY)
                                fillPath.lineTo(posX, posY)
                            } else {
                                curvePath.lineTo(posX, posY)
                                fillPath.lineTo(posX, posY)
                            }
                        }

                        fillPath.lineTo(availableWidth, centerY)
                        fillPath.lineTo(0f, centerY)
                        fillPath.close()

                        // Translucent gain area fill
                        drawPath(
                            path = fillPath,
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    Color(0xFF10B981).copy(alpha = 0.25f), // Green boost fill
                                    Color(0xFF3B82F6).copy(alpha = 0.05f),
                                    Color(0xFFEF4444).copy(alpha = 0.20f)  // Red cut fill
                                ),
                                startY = 10f,
                                endY = h - 10f
                            )
                        )

                        // Neon stroke line
                        drawPath(
                            path = curvePath,
                            color = Color(0xFF34D399), // Emerald Neon
                            style = Stroke(width = 3.5f, cap = StrokeCap.Round)
                        )

                        // Draw 5 EQ Frequency Band Nodes
                        bandFreqPositions.forEachIndexed { idx, (xRatio, label) ->
                            val posX = availableWidth * xRatio
                            val bandGain = when (idx) {
                                0 -> animLowGain
                                1 -> animMidLowGain
                                2 -> animMidGain
                                3 -> animMidHighGain
                                else -> animHighGain
                            }

                            val maxNodeBound = (h - 10f).coerceAtLeast(10f)
                            val posY = (centerY - (bandGain / 18f) * (h * 0.4f)).coerceIn(10f, maxNodeBound)

                            // Outer node glow
                            drawCircle(
                                color = Color(0xFF34D399).copy(alpha = 0.35f),
                                radius = 9f,
                                center = Offset(posX, posY)
                            )
                            // Inner node point
                            drawCircle(
                                color = Color.White,
                                radius = 4f,
                                center = Offset(posX, posY)
                            )
                        }
                    }

                    // 5. Playhead Line & Handle
                    val playheadX = availableWidth * playheadRatio
                    drawLine(
                        color = Color.White.copy(alpha = 0.95f),
                        start = Offset(playheadX, 0f),
                        end = Offset(playheadX, h),
                        strokeWidth = 2f
                    )
                    drawCircle(
                        color = Color(0xFF38BDF8),
                        radius = 5f,
                        center = Offset(playheadX, 6f)
                    )

                    // 6. Dual Stereo VU Meter Bars (Left & Right Channels)
                    val meterX = w - meterWidth + 4f
                    val meterHeight = h * 0.88f
                    val meterTopY = (h - meterHeight) / 2f
                    val singleMeterW = 18f

                    // Base signal level normalized (0.0 to 1.0)
                    val safeLufsVal = if (calculatedOutputLufs.isNaN() || calculatedOutputLufs.isInfinite()) -14.0 else calculatedOutputLufs
                    val baseLevelNorm = ((safeLufsVal + 32.0) / 26.0).coerceIn(0.08, 1.0).toFloat()

                    // Left/Right dynamic oscillation when playing
                    val leftOsc = if (isPlaying) 1.0f + 0.12f * sin(timeMillis / 95f).toFloat() else 1.0f
                    val rightOsc = if (isPlaying) 1.0f + 0.12f * cos(timeMillis / 110f).toFloat() else 1.0f

                    val safeBaseLevelNorm = if (baseLevelNorm.isNaN() || baseLevelNorm.isInfinite()) 0.08f else baseLevelNorm
                    val leftNorm = (safeBaseLevelNorm * leftOsc).coerceIn(0.05f, 1.0f)
                    val rightNorm = (safeBaseLevelNorm * rightOsc).coerceIn(0.05f, 1.0f)

                    // Render Channel 1 (Left) & Channel 2 (Right)
                    listOf(meterX to leftNorm, (meterX + singleMeterW + 4f) to rightNorm).forEachIndexed { chIdx, (posX, levelNorm) ->
                        // Background slot
                        drawRoundRect(
                            color = Color(0xFF1E293B),
                            topLeft = Offset(posX, meterTopY),
                            size = Size(singleMeterW, meterHeight),
                            cornerRadius = CornerRadius(3f, 3f)
                        )

                        // Segmented LED meter fill (12 segments)
                        val segmentCount = 12
                        val activeSegments = (levelNorm * segmentCount).roundToInt()
                        val segHeight = (meterHeight / segmentCount) - 1.5f

                        for (seg in 0 until segmentCount) {
                            val segY = meterTopY + meterHeight - ((seg + 1) * (segHeight + 1.5f))
                            val isActive = seg < activeSegments

                            val segColor = when {
                                seg >= 10 -> if (isActive) Color(0xFFEF4444) else Color(0xFF7F1D1D).copy(alpha = 0.3f) // Red peak
                                seg >= 8 -> if (isActive) Color(0xFFF59E0B) else Color(0xFF78350F).copy(alpha = 0.3f)  // Yellow warning
                                else -> if (isActive) Color(0xFF10B981) else Color(0xFF064E3B).copy(alpha = 0.3f)      // Green normal
                            }

                            drawRoundRect(
                                color = segColor,
                                topLeft = Offset(posX + 1f, segY),
                                size = Size(singleMeterW - 2f, segHeight),
                                cornerRadius = CornerRadius(1.5f, 1.5f)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Canvas Legend & Real-Time Gain Summary Readout
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color(0xFF34D399)))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("EQ Curve", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color(0xFF38BDF8)))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Waveform", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color(0xFF06B6D4)))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Bass", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color(0xFFEC4899)))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Treble", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                Text(
                    text = "Total Gain Delta: %+.1f dB".format(inputGainDb + outputGainDb),
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = if ((inputGainDb + outputGainDb) > 0f) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (selectedVisualMode == "WAVEFORM") {
                Spacer(modifier = Modifier.height(16.dp))
                
                // Horizontal divider
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f))
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Text(
                    text = "Waveform Smart Analysis & Diagnostics",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Adjust thresholds below to scan and diagnose quiet parts or loud peaks in the audio track.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // Real-time calculation of peak and quiet zones
                // Return empty instead of fake flat array
                val pts = analysisData?.waveformPoints ?: emptyList()
                if (pts.isEmpty()) {
                    Text("Analisis audio belum tersedia.", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                } else {
                    val ptCount = pts.size
                    var peakCount = 0
                    var quietCount = 0

                    pts.forEachIndexed { idx, valNorm ->
                    val xNorm = idx.toFloat() / ptCount.toFloat()
                    val eqGain = calculateEqGainNormalized(
                        xNorm = xNorm,
                        low = animLowGain,
                        midLow = animMidLowGain,
                        mid = animMidGain,
                        midHigh = animMidHighGain,
                        high = animHighGain
                    )
                    val gainScale = 10.0.pow((animInputGain + animOutputGain) / 20.0).toFloat()
                    val livePulse = if (isPlaying) {
                        1.0f + 0.20f * sin(timeMillis / 90f + idx * 0.28f).toFloat()
                    } else 1.0f
                    val rawAmp = valNorm * gainScale * 10.0.pow(eqGain / 20.0).toFloat() * livePulse
                    val amp = if (rawAmp.isNaN()) 0.06f else rawAmp.coerceIn(0.06f, 0.96f)
                    
                    if (amp > peakThreshold) {
                        peakCount++
                    } else if (amp < quietThreshold) {
                        quietCount++
                    }
                }
                
                // Symmetrical layout of statistics
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // PEAKS DIAGNOSTIC CARD
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (peakCount > 0) Color(0xFF7F1D1D).copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .border(
                                1.dp, 
                                if (peakCount > 0) Color(0xFFEF4444).copy(alpha = 0.4f) else Color.Transparent,
                                RoundedCornerShape(12.dp)
                            ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(20.dp)
                                        .clip(CircleShape)
                                        .background(if (peakCount > 0) Color(0xFFEF4444) else Color.Gray),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = peakCount.toString(),
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.sp),
                                        color = Color.White
                                    )
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Peaks Detected",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = if (peakCount > 0) Color(0xFFFCA5A5) else MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = if (peakCount > 0) 
                                    "Reduce input gain or enable compressor to clip loud transients and protect headroom." 
                                else "No peak levels exceeding threshold. Transient headroom is safe.",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, lineHeight = 14.sp),
                                color = if (peakCount > 0) Color(0xFFFDBA74) else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    
                    // QUIET SECTIONS DIAGNOSTIC CARD
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (quietCount > 0) Color(0xFF0F172A).copy(alpha = 0.6f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .border(
                                1.dp, 
                                if (quietCount > 0) Color(0xFF38BDF8).copy(alpha = 0.4f) else Color.Transparent,
                                RoundedCornerShape(12.dp)
                            ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(20.dp)
                                        .clip(CircleShape)
                                        .background(if (quietCount > 0) Color(0xFF38BDF8) else Color.Gray),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = quietCount.toString(),
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.sp),
                                        color = Color.White
                                    )
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Quiet Sections",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = if (quietCount > 0) Color(0xFF7DD3FC) else MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = if (quietCount > 0) 
                                    "Quiet areas detected. Enable FFT Noise Reduction or expander to prevent background hiss." 
                                else "No significant quiet sections detected. Consistent signal floor.",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, lineHeight = 14.sp),
                                color = if (quietCount > 0) Color(0xFFBAE6FD) else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // SLIDERS PANEL
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF0F172A), RoundedCornerShape(12.dp))
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Peak Slider
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Peak Limit Threshold",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = Color(0xFFFCA5A5)
                            )
                            Text(
                                text = "%.0f%% Height".format(peakThreshold * 100f),
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                color = Color(0xFFFCA5A5)
                            )
                        }
                        Slider(
                            value = peakThreshold,
                            onValueChange = { peakThreshold = it },
                            valueRange = 0.40f..0.95f,
                            colors = SliderDefaults.colors(
                                thumbColor = Color(0xFFF43F5E),
                                activeTrackColor = Color(0xFFF43F5E),
                                inactiveTrackColor = Color(0xFF334155)
                            ),
                            modifier = Modifier.height(28.dp).testTag("waveform_peak_slider")
                        )
                    }
                    
                    // Quiet Slider
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Quiet Level Threshold",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = Color(0xFF7DD3FC)
                            )
                            Text(
                                text = "%.0f%% Height".format(quietThreshold * 100f),
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                color = Color(0xFF7DD3FC)
                            )
                        }
                        Slider(
                            value = quietThreshold,
                            onValueChange = { quietThreshold = it },
                            valueRange = 0.05f..0.45f,
                            colors = SliderDefaults.colors(
                                thumbColor = Color(0xFF38BDF8),
                                activeTrackColor = Color(0xFF38BDF8),
                                inactiveTrackColor = Color(0xFF334155)
                            ),
                            modifier = Modifier.height(28.dp).testTag("waveform_quiet_slider")
                        )
                    }
                }
                } // end of "analysis data available" branch
            }
        }
    }
}

/**
 * Calculates 5-Band Gaussian EQ Gain curve response across normalized frequency xNorm (0.0 to 1.0)
 */
private fun calculateEqGainNormalized(
    xNorm: Float,
    low: Float,
    midLow: Float,
    mid: Float,
    midHigh: Float,
    high: Float
): Float {
    val gLow = exp(-((xNorm - 0.10f) / 0.16f).pow(2)) * low
    val gMidLow = exp(-((xNorm - 0.30f) / 0.16f).pow(2)) * midLow
    val gMid = exp(-((xNorm - 0.50f) / 0.16f).pow(2)) * mid
    val gMidHigh = exp(-((xNorm - 0.70f) / 0.16f).pow(2)) * midHigh
    val gHigh = exp(-((xNorm - 0.90f) / 0.16f).pow(2)) * high
    return gLow + gMidLow + gMid + gMidHigh + gHigh
}

/**
 * Component for Input Gain and Output Gain Controls
 */
@Composable
internal fun AudioGainControlCard(
    inputGainDb: Float,
    outputGainDb: Float,
    onInputGainChanged: (Float) -> Unit,
    onOutputGainChanged: (Float) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("audio_gain_card"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.VolumeUp,
                        contentDescription = "Gain Controls",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Master Audio Gain",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                if (inputGainDb != 0f || outputGainDb != 0f) {
                    IconButton(
                        onClick = {
                            onInputGainChanged(0f)
                            onOutputGainChanged(0f)
                        },
                        modifier = Modifier
                            .size(32.dp)
                            .testTag("reset_gain_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Reset Gain",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Input Gain Slider
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Input Pre-Gain",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "%+.1f dB".format(inputGainDb),
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                        color = if (inputGainDb > 0f) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurface
                    )
                }
                Slider(
                    value = inputGainDb,
                    onValueChange = onInputGainChanged,
                    valueRange = -18f..18f,
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.secondary,
                        activeTrackColor = MaterialTheme.colorScheme.secondary
                    ),
                    modifier = Modifier.testTag("input_gain_slider")
                )
            }

            // Output / Makeup Gain Slider
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Master Output Makeup Gain",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "%+.1f dB".format(outputGainDb),
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                        color = if (outputGainDb > 0f) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                    )
                }
                Slider(
                    value = outputGainDb,
                    onValueChange = onOutputGainChanged,
                    valueRange = 0f..12f,
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.primary,
                        activeTrackColor = MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier.testTag("output_gain_slider")
                )
            }
        }
    }
}

/**
 * Component for Mastering Preset Selection and Management
 */
@Composable
internal fun AudioEqPresetSelectorCard(
    selectedPreset: MasteringPreset,
    customPresets: List<MasteringPreset>,
    eqConfig: EqBandConfig,
    inputGainDb: Float,
    outputGainDb: Float,
    targetLufs: Double,
    onPresetSelected: (MasteringPreset) -> Unit,
    onSavePreset: (String) -> Unit,
    onDeletePreset: (MasteringPreset) -> Unit
) {
    var showSaveDialog by remember { mutableStateOf(false) }
    var presetNameInput by remember { mutableStateOf("") }
    var selectedTab by remember { mutableStateOf("ALL") } // "ALL", "BUILTIN", "CUSTOM"

    val allPresets = remember(customPresets) {
        AudioMasteringEngine.PRESETS + customPresets
    }

    val filteredPresets = when (selectedTab) {
        "BUILTIN" -> AudioMasteringEngine.PRESETS
        "CUSTOM" -> customPresets
        else -> allPresets
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("audio_presets_card"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = "Presets",
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Preset Management",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                // Save Current Preset Button
                OutlinedButton(
                    onClick = {
                        presetNameInput = "My Preset ${customPresets.size + 1}"
                        showSaveDialog = true
                    },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.testTag("save_preset_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.BookmarkAdd,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Save Preset", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }

            // Category Filter Chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = selectedTab == "ALL",
                    onClick = { selectedTab = "ALL" },
                    label = { Text("All (${allPresets.size})", fontSize = 11.sp) },
                    modifier = Modifier.testTag("preset_filter_all")
                )
                FilterChip(
                    selected = selectedTab == "BUILTIN",
                    onClick = { selectedTab = "BUILTIN" },
                    label = { Text("Built-in (${AudioMasteringEngine.PRESETS.size})", fontSize = 11.sp) },
                    modifier = Modifier.testTag("preset_filter_builtin")
                )
                FilterChip(
                    selected = selectedTab == "CUSTOM",
                    onClick = { selectedTab = "CUSTOM" },
                    label = { Text("Custom (${customPresets.size})", fontSize = 11.sp) },
                    modifier = Modifier.testTag("preset_filter_custom")
                )
            }

            // Presets List
            if (filteredPresets.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (selectedTab == "CUSTOM") "No custom presets saved yet. Adjust EQ and gain above, then tap 'Save Preset'!" else "No presets found.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    filteredPresets.forEach { preset ->
                        val isSelected = selectedPreset.name == preset.name || selectedPreset.id == preset.id

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f)
                                    else MaterialTheme.colorScheme.surface
                                )
                                .border(
                                    width = if (isSelected) 1.5.dp else 0.dp,
                                    color = if (isSelected) MaterialTheme.colorScheme.secondary else Color.Transparent,
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .clickable { onPresetSelected(preset) }
                                .padding(horizontal = 14.dp, vertical = 10.dp)
                                .testTag("preset_item_${preset.name}")
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = preset.name,
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        if (preset.isCustom) {
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Surface(
                                                color = MaterialTheme.colorScheme.primaryContainer,
                                                shape = RoundedCornerShape(6.dp)
                                            ) {
                                                Text(
                                                    text = "CUSTOM",
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                                    fontSize = 9.sp
                                                )
                                            }
                                        }
                                    }

                                    // Display EQ & Gain Details
                                    val eqSummary = "EQ: [%+.1f, %+.1f, %+.1f, %+.1f, %+.1f] dB".format(
                                        preset.eqBandConfig.lowGainDb,
                                        preset.eqBandConfig.midLowGainDb,
                                        preset.eqBandConfig.midGainDb,
                                        preset.eqBandConfig.midHighGainDb,
                                        preset.eqBandConfig.highGainDb
                                    )
                                    val gainSummary = "Gains: In %+.1fdB / Out %+.1fdB | Target: %.1f LUFS".format(
                                        preset.inputGainDb,
                                        preset.outputGainDb,
                                        preset.targetLufs
                                    )

                                    Text(
                                        text = "$gainSummary\n$eqSummary",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 11.sp
                                    )
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (preset.isCustom) {
                                        IconButton(
                                            onClick = { onDeletePreset(preset) },
                                            modifier = Modifier.size(32.dp).testTag("delete_preset_${preset.name}")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = "Delete Custom Preset",
                                                tint = Color(0xFFEF4444),
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }

                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Default.Equalizer,
                                            contentDescription = "Selected Preset",
                                            tint = MaterialTheme.colorScheme.secondary,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Save Preset Dialog
    if (showSaveDialog) {
        AlertDialog(
            onDismissRequest = { showSaveDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.BookmarkAdd,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Save Custom Preset")
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Save your current EQ and Gain configuration so you can recall it anytime.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Current Settings Summary Card
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = "Current Settings to Save:",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "• Input Gain: %+.1f dB | Output Gain: %+.1f dB".format(inputGainDb, outputGainDb),
                                style = MaterialTheme.typography.labelSmall
                            )
                            Text(
                                text = "• 5-Band EQ: Low %+.1f | MidLow %+.1f | Mid %+.1f | MidHigh %+.1f | High %+.1f dB".format(
                                    eqConfig.lowGainDb, eqConfig.midLowGainDb, eqConfig.midGainDb, eqConfig.midHighGainDb, eqConfig.highGainDb
                                ),
                                style = MaterialTheme.typography.labelSmall
                            )
                            Text(
                                text = "• Target LUFS: %.1f LUFS".format(targetLufs),
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }

                    OutlinedTextField(
                        value = presetNameInput,
                        onValueChange = { presetNameInput = it },
                        label = { Text("Preset Name") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("save_preset_name_input")
                    )

                    // Quick suggestions
                    Text("Quick Name Suggestions:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("Vocal Punch", "Bass Heavy", "Crisp Master", "Acoustic Warm").forEach { suggestion ->
                            Surface(
                                onClick = { presetNameInput = suggestion },
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                                modifier = Modifier.testTag("preset_suggestion_$suggestion")
                            ) {
                                Text(
                                    text = suggestion,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 10.sp
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (presetNameInput.isNotBlank()) {
                            onSavePreset(presetNameInput.trim())
                            showSaveDialog = false
                        }
                    },
                    modifier = Modifier.testTag("confirm_save_preset_button")
                ) {
                    Icon(imageVector = Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Save & Apply")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showSaveDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

/**
 * Component for 5-Band Equalizer Adjustments
 */
@Composable
internal fun Audio5BandEqControlCard(
    eqConfig: EqBandConfig,
    onEqLowChanged: (Float) -> Unit,
    onEqMidLowChanged: (Float) -> Unit,
    onEqMidChanged: (Float) -> Unit,
    onEqMidHighChanged: (Float) -> Unit,
    onEqHighChanged: (Float) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("audio_5band_eq_card"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Equalizer,
                    contentDescription = "5-Band EQ",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Manual 5-Band Equalizer",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            // Low Band (60 Hz)
            EqBandSliderRow(
                bandLabel = "Low (60 Hz Bass)",
                gainDb = eqConfig.lowGainDb,
                onGainChanged = onEqLowChanged,
                testTag = "eq_slider_low"
            )

            // Mid-Low Band (250 Hz)
            EqBandSliderRow(
                bandLabel = "Mid-Low (250 Hz Warmth)",
                gainDb = eqConfig.midLowGainDb,
                onGainChanged = onEqMidLowChanged,
                testTag = "eq_slider_mid_low"
            )

            // Mid Band (1 kHz)
            EqBandSliderRow(
                bandLabel = "Mid (1 kHz Presence)",
                gainDb = eqConfig.midGainDb,
                onGainChanged = onEqMidChanged,
                testTag = "eq_slider_mid"
            )

            // Mid-High Band (4 kHz)
            EqBandSliderRow(
                bandLabel = "Mid-High (4 kHz Clarity)",
                gainDb = eqConfig.midHighGainDb,
                onGainChanged = onEqMidHighChanged,
                testTag = "eq_slider_mid_high"
            )

            // High Band (12 kHz)
            EqBandSliderRow(
                bandLabel = "High (12 kHz Treble)",
                gainDb = eqConfig.highGainDb,
                onGainChanged = onEqHighChanged,
                testTag = "eq_slider_high"
            )
        }
    }
}

@Composable
private fun EqBandSliderRow(
    bandLabel: String,
    gainDb: Float,
    onGainChanged: (Float) -> Unit,
    testTag: String
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = bandLabel,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "%+.1f dB".format(gainDb),
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                color = when {
                    gainDb > 0f -> MaterialTheme.colorScheme.secondary
                    gainDb < 0f -> Color(0xFFF87171)
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
        }
        Slider(
            value = gainDb,
            onValueChange = onGainChanged,
            valueRange = -12f..12f,
            modifier = Modifier.testTag(testTag)
        )
    }
}


@Composable
fun RealtimeWaveformVisualizer(
    modifier: Modifier = Modifier,
    analysisData: com.example.core.media.AudioAnalysisData?,
    primaryColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.primary,
    secondaryColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.secondary
) {
    if (analysisData == null || analysisData.waveformPoints.isEmpty()) {
        Box(modifier = modifier.background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))) {
            androidx.compose.material3.Text("No waveform data", modifier = Modifier.align(androidx.compose.ui.Alignment.Center), color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }
    
    val infiniteTransition = rememberInfiniteTransition(label = "waveform_anim")
    val animProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "waveform_progress"
    )

    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val points = analysisData.waveformPoints
        val barWidth = width / points.size
        
        val gradientBrush = Brush.verticalGradient(
            colors = listOf(primaryColor, secondaryColor)
        )
        
        for (i in points.indices) {
            val baseAmp = points[i]
            val pulse = (sin(animProgress * Math.PI * 2 + i * 0.1f) * 0.1f + 0.9f).toFloat()
            val amp = (baseAmp * pulse).coerceIn(0f, 1f)
            
            val barHeight = amp * height
            val x = i * barWidth
            val y = (height - barHeight) / 2f
            
            drawRoundRect(
                brush = gradientBrush,
                topLeft = Offset(x, y),
                size = Size(barWidth * 0.8f, barHeight),
                cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
            )
        }
    }
}
