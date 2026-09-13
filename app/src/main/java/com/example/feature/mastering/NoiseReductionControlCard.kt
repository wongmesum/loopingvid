package com.example.feature.mastering

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.example.core.media.VisualizerTheme
import com.example.core.media.PeakMeterStyle
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.media.NoiseReductionConfig
import com.example.core.media.NoiseReductionEngine
import com.example.ui.theme.ElegantGoldDim
import com.example.ui.theme.StudioLiveRed
import com.example.ui.theme.StudioSuccessGreen
import kotlinx.coroutines.isActive

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun NoiseReductionControlCard(
    config: NoiseReductionConfig,
    onToggleEnabled: (Boolean) -> Unit,
    onReductionDbChanged: (Float) -> Unit,
    onNoiseFloorDbChanged: (Float) -> Unit,
    onFftSizeChanged: (Int) -> Unit,
    theme: VisualizerTheme = VisualizerTheme.CYAN_PINK,
    meterStyle: PeakMeterStyle = PeakMeterStyle.SEGMENTED_LED,
    modifier: Modifier = Modifier
) {
    var timeMs by remember { mutableLongStateOf(0L) }

    LaunchedEffect(config.isEnabled) {
        var lastNanos = System.nanoTime()
        while (isActive) {
            withFrameNanos { frameNanos ->
                val deltaMs = ((frameNanos - lastNanos) / 1_000_000L).coerceIn(1L, 50L)
                lastNanos = frameNanos
                timeMs += deltaMs
            }
        }
    }

    val analysis = remember(config, timeMs) {
        NoiseReductionEngine.analyzeSpectralHiss(config, timeMs = timeMs)
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("noise_reduction_card"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header Row: Title & Toggle Switch
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.GraphicEq,
                        contentDescription = "FFT Noise Reduction",
                        tint = if (config.isEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Real-Time FFT Noise Reduction",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "FFT spectral subtraction to remove video hiss & static",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Switch(
                    checked = config.isEnabled,
                    onCheckedChange = onToggleEnabled,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                        checkedTrackColor = MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier.testTag("noise_reduction_switch")
                )
            }

            // Live Spectral Hiss Visualizer Canvas
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp)
                ) {
                    // Spectral Header Metrics
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Column {
                                Text("ESTIMATED HISS", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    text = "%.1f dB".format(analysis.detectedHissLevelDb),
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = if (analysis.detectedHissLevelDb > -40f) MaterialTheme.colorScheme.error else ElegantGoldDim
                                )
                            }
                            Column {
                                Text("FFT SUPPRESSION", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    text = if (config.isEnabled) "-%.1f dB".format(config.reductionDb) else "OFF",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = if (config.isEnabled) StudioSuccessGreen else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Column {
                                Text("WINDOW", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    text = "${config.fftSize} pts",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        Surface(
                            color = if (config.isEnabled) StudioSuccessGreen.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = if (config.isEnabled) "ACTIVE FILTER" else "BYPASS",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = if (config.isEnabled) StudioSuccessGreen else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Spectrum Canvas
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(110.dp)
                            .testTag("spectral_hiss_canvas")
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val w = size.width
                            val h = size.height

                            val bins = analysis.rawSpectrum.size
                            val barW = w / bins

                            // Draw Noise Floor Threshold Line
                            val safeThresholdDb = if (analysis.noiseFloorThresholdDb.isNaN() || analysis.noiseFloorThresholdDb.isInfinite()) -45f else analysis.noiseFloorThresholdDb
                            val floorRatio = ((safeThresholdDb + 70f) / 50f).coerceIn(0.1f, 0.9f)
                            val floorY = h * (1.0f - floorRatio)

                            drawLine(
                                color = Color(0xFFF59E0B).copy(alpha = 0.8f),
                                start = Offset(0f, floorY),
                                end = Offset(w, floorY),
                                strokeWidth = 1.5f,
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
                            )

                            // Render Raw vs Filtered Frequency Spectrum
                            val rawPath = Path()
                            val filteredPath = Path()

                            analysis.rawSpectrum.forEachIndexed { i, rawMag ->
                                val x = i * barW + barW / 2f
                                val filteredMag = analysis.filteredSpectrum[i]

                                val maxBound = h.coerceAtLeast(4f)
                                val rawY = h - (rawMag * h * 0.85f).coerceIn(4f, maxBound)
                                val filteredY = h - (filteredMag * h * 0.85f).coerceIn(4f, maxBound)

                                if (i == 0) {
                                    rawPath.moveTo(x, rawY)
                                    filteredPath.moveTo(x, filteredY)
                                } else {
                                    rawPath.lineTo(x, rawY)
                                    filteredPath.lineTo(x, filteredY)
                                }

                                // High-frequency Hiss highlight bars (bins > 30%)
                                if (i > (bins * 0.35f)) {
                                    val barH = h - rawY
                                    val isSuppressed = config.isEnabled && (rawMag - filteredMag) > 0.05f

                                    val barColor = if (isSuppressed) {
                                        Color(0xFFEF4444).copy(alpha = 0.25f) // Red noise subtracted
                                    } else {
                                        Color(0xFFF59E0B).copy(alpha = 0.15f)
                                    }

                                    drawRect(
                                        color = barColor,
                                        topLeft = Offset(i * barW, rawY),
                                        size = Size(barW * 0.85f, barH)
                                    )
                                }
                            }

                            // Raw Audio Hiss Spectrum Curve (Orange/Red)
                            drawPath(
                                path = rawPath,
                                color = if (config.isEnabled) Color(0xFFF87171).copy(alpha = 0.6f) else Color(0xFF38BDF8),
                                style = Stroke(width = 2f)
                            )

                            // Clean Filtered FFT Spectrum Curve (Emerald Green)
                            if (config.isEnabled) {
                                drawPath(
                                    path = filteredPath,
                                    color = Color(0xFF34D399),
                                    style = Stroke(width = 3f)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Legend
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(StudioSuccessGreen))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Clean Audio", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(StudioLiveRed))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Raw Hiss Noise", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(ElegantGoldDim))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Noise Floor", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }

                        Text(
                            text = "FFT Spectral Subtraction",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Real-Time Peak Level Meter Visualization (Gain Staging Pre vs Post NR)
                    RealtimeGainStagingPeakMeter(
                        analysis = analysis,
                        isEnabled = config.isEnabled,
                        theme = theme,
                        meterStyle = meterStyle
                    )
                }
            }

            AnimatedVisibility(visible = config.isEnabled) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    // Preset Quick Action Buttons. FlowRow so the 3 longer labels ("Medium
                    // Static", "High Fan/AC") wrap onto a second line on narrow screens.
                    androidx.compose.foundation.layout.FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(
                            "Mild Hiss" to (8f to -50f),
                            "Medium Static" to (12f to -45f),
                            "High Fan/AC" to (18f to -40f)
                        ).forEach { (label, params) ->
                            val (redDb, floorDb) = params
                            val isSelected = config.reductionDb == redDb && config.noiseFloorDb == floorDb
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    onReductionDbChanged(redDb)
                                    onNoiseFloorDbChanged(floorDb)
                                },
                                label = {
                                    Text(
                                        label,
                                        style = MaterialTheme.typography.labelMedium,
                                        maxLines = 1,
                                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                ),
                                modifier = Modifier.testTag("preset_noise_${label.lowercase().replace(" ", "_")}")
                            )
                        }
                    }

                    // Noise Suppression Amount Slider
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Noise Suppression Strength",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "%.0f dB".format(config.reductionDb),
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Slider(
                            value = config.reductionDb,
                            onValueChange = onReductionDbChanged,
                            valueRange = 0f..24f,
                            steps = 23,
                            colors = SliderDefaults.colors(
                                thumbColor = MaterialTheme.colorScheme.primary,
                                activeTrackColor = MaterialTheme.colorScheme.primary
                            ),
                            modifier = Modifier.testTag("noise_reduction_strength_slider")
                        )
                    }

                    // Noise Floor Threshold Slider
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Noise Floor Detection Threshold",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "%.0f dBFS".format(config.noiseFloorDb),
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                        Slider(
                            value = config.noiseFloorDb,
                            onValueChange = onNoiseFloorDbChanged,
                            valueRange = -70f..-20f,
                            steps = 49,
                            colors = SliderDefaults.colors(
                                thumbColor = MaterialTheme.colorScheme.secondary,
                                activeTrackColor = MaterialTheme.colorScheme.secondary
                            ),
                            modifier = Modifier.testTag("noise_floor_threshold_slider")
                        )
                    }

                    // FFT Window Size Selection. Stacked (label above chips, not side-by-side)
                    // so the 4 size chips have the full card width to wrap into on narrow phone
                    // screens instead of being squeezed next to the label.
                    //
                    // IMPORTANT: this only changes the resolution of the live preview visualizer's
                    // spectral analysis (NoiseReductionEngine.analyzeSpectralHiss). FFmpeg's actual
                    // export-time noise filter (`afftdn`) has no window-size parameter to receive
                    // this value, so it intentionally does NOT affect the exported file's noise
                    // reduction - the label below makes that explicit instead of silently implying
                    // it changes export quality.
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "FFT Window Size (Preview Only)",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Changes the live hiss visualizer's analysis resolution. Does not affect the exported audio.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        androidx.compose.foundation.layout.FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf(256, 512, 1024, 2048).forEach { size ->
                                FilterChip(
                                    selected = config.fftSize == size,
                                    onClick = { onFftSizeChanged(size) },
                                    label = { Text("$size", style = MaterialTheme.typography.labelMedium) },
                                    modifier = Modifier.testTag("fft_size_$size")
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RealtimeGainStagingPeakMeter(
    analysis: NoiseReductionEngine.SpectralHissAnalysis,
    isEnabled: Boolean,
    theme: VisualizerTheme = VisualizerTheme.CYAN_PINK,
    meterStyle: PeakMeterStyle = PeakMeterStyle.SEGMENTED_LED,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("realtime_peak_meter"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background),
        shape = RoundedCornerShape(10.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Speed,
                        contentDescription = "Gain Staging Peak Meter",
                        tint = theme.meterSafeColor,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Gain Staging Peak Meter",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                val statusColor = when {
                    analysis.postPeakDb > -1f -> theme.meterClipColor
                    analysis.gainReductionDb > 4f -> theme.meterSafeColor
                    else -> theme.midColor
                }
                val statusText = when {
                    analysis.postPeakDb > -1f -> "CLIP RISK"
                    analysis.gainReductionDb > 0.5f && isEnabled -> "NOISE SUPPRESSED"
                    else -> "HEADROOM SAFE"
                }

                Surface(
                    color = statusColor.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(6.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, statusColor.copy(alpha = 0.5f))
                ) {
                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = statusColor,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            // Dual Meter Bars: Pre-NR and Post-NR
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                // 1. PRE-NR (Raw Signal Level)
                PeakMeterBar(
                    label = "PRE-NR (RAW)",
                    peakDb = analysis.prePeakDb,
                    theme = theme,
                    meterStyle = meterStyle,
                    barColorPrimary = theme.meterWarningColor,
                    barColorPeak = theme.peakColor
                )

                // 2. POST-NR (Clean Signal Level)
                PeakMeterBar(
                    label = "POST-NR (CLEAN)",
                    peakDb = if (isEnabled) analysis.postPeakDb else analysis.prePeakDb,
                    theme = theme,
                    meterStyle = meterStyle,
                    barColorPrimary = theme.meterSafeColor,
                    barColorPeak = theme.peakColor
                )
            }

            // Delta Readout & dB Scale Legend
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "DELTA: -%.1f dB".format(if (isEnabled) analysis.gainReductionDb else 0.0f),
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = if (isEnabled && analysis.gainReductionDb > 0.1f) theme.meterSafeColor else MaterialTheme.colorScheme.onSurfaceVariant
                )

                // dB Tick Marks
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("-60", "-30", "-12", "-6", "0 dB").forEach { tick ->
                        Text(
                            text = tick,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PeakMeterBar(
    label: String,
    peakDb: Float,
    theme: VisualizerTheme,
    meterStyle: PeakMeterStyle,
    barColorPrimary: Color,
    barColorPeak: Color
) {
    val safePeakDb = if (peakDb.isNaN() || peakDb.isInfinite()) -60f else peakDb
    val normFraction = ((safePeakDb + 60f) / 60f).coerceIn(0f, 1f)

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(88.dp)
        )

        Box(
            modifier = Modifier
                .weight(1f)
                .height(16.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height

                when (meterStyle) {
                    PeakMeterStyle.SEGMENTED_LED -> {
                        val segmentCount = 28
                        val segmentGap = 2f
                        val segmentWidth = (w - (segmentCount * segmentGap)) / segmentCount
                        val filledSegments = (normFraction * segmentCount).toInt()

                        for (i in 0 until segmentCount) {
                            val segRatio = i.toFloat() / segmentCount
                            val color = when {
                                segRatio > 0.90f -> theme.meterClipColor
                                segRatio > 0.75f -> theme.meterWarningColor
                                else -> barColorPrimary
                            }

                            val alpha = if (i < filledSegments) 1.0f else 0.15f

                            drawRect(
                                color = color.copy(alpha = alpha),
                                topLeft = Offset(i * (segmentWidth + segmentGap), 0f),
                                size = Size(segmentWidth, h)
                            )
                        }

                        if (filledSegments > 0) {
                            val peakX = filledSegments * (segmentWidth + segmentGap)
                            drawLine(
                                color = barColorPeak,
                                start = Offset(peakX, 0f),
                                end = Offset(peakX, h),
                                strokeWidth = 3f
                            )
                        }
                    }
                    PeakMeterStyle.SOLID_GRADIENT -> {
                        val fillWidth = w * normFraction
                        if (fillWidth > 0f) {
                            val gradientColors = listOf(
                                barColorPrimary,
                                if (normFraction > 0.75f) theme.meterWarningColor else barColorPrimary,
                                if (normFraction > 0.90f) theme.meterClipColor else theme.meterWarningColor
                            )
                            drawRoundRect(
                                brush = Brush.horizontalGradient(gradientColors, startX = 0f, endX = w),
                                topLeft = Offset(0f, 0f),
                                size = Size(fillWidth, h),
                                cornerRadius = CornerRadius(2f, 2f)
                            )
                            drawLine(
                                color = barColorPeak,
                                start = Offset(fillWidth.coerceAtMost(w - 2f), 0f),
                                end = Offset(fillWidth.coerceAtMost(w - 2f), h),
                                strokeWidth = 3f
                            )
                        }
                    }
                    PeakMeterStyle.THIN_NEON -> {
                        val fillWidth = w * normFraction
                        val neonY = h / 2f
                        drawLine(
                            color = Color(0xFF334155),
                            start = Offset(0f, neonY),
                            end = Offset(w, neonY),
                            strokeWidth = 3f
                        )
                        if (fillWidth > 0f) {
                            val neonColor = when {
                                normFraction > 0.90f -> theme.meterClipColor
                                normFraction > 0.75f -> theme.meterWarningColor
                                else -> barColorPrimary
                            }
                            drawLine(
                                color = neonColor,
                                start = Offset(0f, neonY),
                                end = Offset(fillWidth, neonY),
                                strokeWidth = 6f
                            )
                            drawCircle(
                                color = barColorPeak,
                                radius = 4f,
                                center = Offset(fillWidth, neonY)
                            )
                        }
                    }
                }
            }
        }

        Text(
            text = "%.1f".format(peakDb),
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            color = if (peakDb > -3f) theme.meterClipColor else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.width(38.dp)
        )
    }
}
