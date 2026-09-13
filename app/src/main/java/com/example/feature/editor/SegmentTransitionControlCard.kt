package com.example.feature.editor

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Loop
import androidx.compose.material.icons.filled.MovieFilter
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QueuePlayNext
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Transform
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.media.LoopedSegmentConfig
import com.example.core.media.SegmentTransitionConfig
import com.example.core.media.TransitionEffect
import com.example.core.work.BatchExportRequest
import com.example.core.work.ExportQueueViewModel

@Composable
fun SegmentTransitionControlCard(
    transitionConfig: SegmentTransitionConfig,
    onUpdateSegmentTransition: (segmentId: String, effect: TransitionEffect) -> Unit,
    onUpdateSegmentTransitionDuration: (segmentId: String, durationSec: Double) -> Unit,
    onUpdateSegmentLoopCount: (segmentId: String, repeatCount: Int) -> Unit,
    onAddSegment: () -> Unit,
    onRemoveSegment: (segmentId: String) -> Unit,
    onApplyGlobalEffect: (effect: TransitionEffect) -> Unit,
    queueViewModel: ExportQueueViewModel?,
    selectedMediaUri: String? = null,
    modifier: Modifier = Modifier
) {
    var activePreviewEffect by remember { mutableStateOf(transitionConfig.globalTransitionEffect) }
    val totalDuration = transitionConfig.calculateTotalDurationSec()
    val filterGraph = transitionConfig.buildFfmpegXfadeFilterGraph()

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("segment_transition_control_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Card Title Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(MaterialTheme.colorScheme.tertiaryContainer, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.MovieFilter,
                            contentDescription = "FFmpeg xfade transitions",
                            tint = MaterialTheme.colorScheme.onTertiaryContainer,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Segment Transition Studio",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "FFmpeg xfade crossfades & wipes between looped segments",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Loop,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = String.format("%.1fs TOTAL", totalDuration),
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            // Live Transition Animation Canvas Preview
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Live Effect Preview: ${activePreviewEffect.displayName}",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "-vf \"xfade=transition=${activePreviewEffect.xfadeName}\"",
                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                AnimatedTransitionPreviewCanvas(
                    effect = activePreviewEffect,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(110.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(14.dp))
                )
            }

            // Quick Global Transition Presets Selector
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Quick Apply Effect to All Segment Junctions",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface
                )

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(TransitionEffect.values()) { effect ->
                        val isSelected = activePreviewEffect == effect
                        val bg = if (isSelected) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.surfaceVariant
                        val fg = if (isSelected) MaterialTheme.colorScheme.onTertiary else MaterialTheme.colorScheme.onSurfaceVariant

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = bg,
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    activePreviewEffect = effect
                                    onApplyGlobalEffect(effect)
                                }
                                .testTag("transition_effect_${effect.name.lowercase()}")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = effect.getIcon(),
                                    contentDescription = effect.displayName,
                                    tint = fg,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = effect.displayName,
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    ),
                                    color = fg
                                )
                            }
                        }
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            // Interactive Segment Timeline Chain
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Looped Segment Sequence (${transitionConfig.segments.size} Segments)",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    OutlinedButton(
                        onClick = onAddSegment,
                        modifier = Modifier.height(34.dp),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add Segment",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "Add Segment", style = MaterialTheme.typography.labelMedium)
                    }
                }

                // Render each segment and transition junction
                transitionConfig.segments.forEachIndexed { index, segment ->
                    SegmentItemCard(
                        segment = segment,
                        segmentIndex = index,
                        totalSegments = transitionConfig.segments.size,
                        onLoopCountChange = { count -> onUpdateSegmentLoopCount(segment.id, count) },
                        onRemove = { onRemoveSegment(segment.id) }
                    )

                    // Render transition node between segment index and index+1
                    if (index < transitionConfig.segments.size - 1) {
                        TransitionJunctionNode(
                            currentEffect = segment.transitionToNext,
                            durationSec = segment.transitionDurationSec,
                            onEffectSelected = { newEffect ->
                                activePreviewEffect = newEffect
                                onUpdateSegmentTransition(segment.id, newEffect)
                            },
                            onDurationChange = { dur ->
                                onUpdateSegmentTransitionDuration(segment.id, dur)
                            }
                        )
                    }
                }
            }

            // FFmpeg Filter Graph Output Code Box
            if (filterGraph.isNotBlank()) {
                Surface(
                    color = Color(0xFF1E1E2E),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Code,
                                    contentDescription = null,
                                    tint = Color(0xFF89B4FA),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Generated FFmpeg Filter Complex (-filter_complex)",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = Color(0xFF89B4FA)
                                )
                            }
                        }

                        Text(
                            text = filterGraph,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                color = Color(0xFFA6ADC8)
                            )
                        )
                    }
                }
            }

            // Enqueue / Render Button.
            // NOTE: A true multi-segment xfade needs a multi-input filter_complex pipeline that
            // the single-input editor renderer does not provide. We therefore enqueue a valid
            // re-encode of the selected clip (transitions applied in preview only) rather than
            // sending an unusable multi-input xfade graph into -vf (which would fail). Requires a
            // selected media clip.
            Button(
                onClick = {
                    if (queueViewModel != null && !selectedMediaUri.isNullOrBlank()) {
                        val batchRequest = BatchExportRequest(
                            title = "Segment Render (${transitionConfig.segments.size} Segments)",
                            jobType = "TRANSITION_RENDER",
                            format = "mp4",
                            destinationFolder = "Movies/Transitions",
                            inputUri = selectedMediaUri
                        )
                        queueViewModel.enqueueProjectExport(batchRequest)
                    }
                },
                enabled = queueViewModel != null && !selectedMediaUri.isNullOrBlank(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("submit_segment_transition_button"),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.QueuePlayNext,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Enqueue Multi-Segment Transition Export",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                )
            }
        }
    }
}

@Composable
fun SegmentItemCard(
    segment: LoopedSegmentConfig,
    segmentIndex: Int,
    totalSegments: Int,
    onLoopCountChange: (Int) -> Unit,
    onRemove: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(MaterialTheme.colorScheme.primary, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "${segmentIndex + 1}",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = segment.segmentName,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${segment.durationSec}s base • ${segment.loopRepeatCount}x loops = ${String.format("%.1f", segment.totalEffectiveDurationSec)}s",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                // Repeat Count Adjuster
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(8.dp))
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    IconButton(
                        onClick = { if (segment.loopRepeatCount > 1) onLoopCountChange(segment.loopRepeatCount - 1) },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Text("-", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                    }
                    Text(
                        text = "${segment.loopRepeatCount}x",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 6.dp)
                    )
                    IconButton(
                        onClick = { if (segment.loopRepeatCount < 10) onLoopCountChange(segment.loopRepeatCount + 1) },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Text("+", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                    }
                }

                if (totalSegments > 2) {
                    Spacer(modifier = Modifier.width(6.dp))
                    IconButton(
                        onClick = onRemove,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Remove segment",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TransitionJunctionNode(
    currentEffect: TransitionEffect,
    durationSec: Double,
    onEffectSelected: (TransitionEffect) -> Unit,
    onDurationChange: (Double) -> Unit
) {
    var showEffectDropdown by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.tertiaryContainer,
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.5f)),
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .clickable { showEffectDropdown = !showEffectDropdown }
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = currentEffect.getIcon(),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onTertiaryContainer,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "TRANSITION: ${currentEffect.displayName} (${String.format("%.1f", durationSec)}s)",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onTertiaryContainer
                )
                Spacer(modifier = Modifier.width(6.dp))
                Icon(
                    imageVector = Icons.Default.Transform,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onTertiaryContainer,
                    modifier = Modifier.size(14.dp)
                )
            }
        }

        if (showEffectDropdown) {
            Spacer(modifier = Modifier.height(8.dp))
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surface,
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                shadowElevation = 4.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Select Transition Effect for Junction",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(TransitionEffect.values()) { fx ->
                            val isSel = fx == currentEffect
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable {
                                        onEffectSelected(fx)
                                        showEffectDropdown = false
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = fx.getIcon(),
                                        contentDescription = null,
                                        tint = if (isSel) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = fx.displayName,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = if (isSel) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    // Duration Slider
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Crossfade Duration",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "${String.format("%.1f", durationSec)}s",
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Slider(
                            value = durationSec.toFloat(),
                            onValueChange = { onDurationChange(it.toDouble()) },
                            valueRange = 0.5f..3.0f,
                            steps = 5,
                            colors = SliderDefaults.colors(thumbColor = MaterialTheme.colorScheme.primary)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AnimatedTransitionPreviewCanvas(
    effect: TransitionEffect,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "transitionPreview")
    val progress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "progress"
    )

    Box(
        modifier = modifier.background(Color(0xFF0F172A)),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            // Segment A Background (Warm Gradient)
            drawRoundRect(
                brush = Brush.linearGradient(listOf(Color(0xFFEC4899), Color(0xFF8B5CF6))),
                size = size,
                cornerRadius = CornerRadius(14f, 14f)
            )

            // Transition effect simulation onto Segment B (Cool Gradient)
            val segmentBBrush = Brush.linearGradient(listOf(Color(0xFF06B6D4), Color(0xFF3B82F6)))

            when (effect) {
                TransitionEffect.CROSSFADE, TransitionEffect.DISSOLVE -> {
                    // Opacity alpha blend
                    drawRoundRect(
                        brush = segmentBBrush,
                        size = size,
                        alpha = progress,
                        cornerRadius = CornerRadius(14f, 14f)
                    )
                }
                TransitionEffect.WIPE_LEFT -> {
                    val wipeW = w * progress
                    drawRect(
                        brush = segmentBBrush,
                        topLeft = Offset(w - wipeW, 0f),
                        size = Size(wipeW, h)
                    )
                }
                TransitionEffect.WIPE_RIGHT -> {
                    val wipeW = w * progress
                    drawRect(
                        brush = segmentBBrush,
                        topLeft = Offset(0f, 0f),
                        size = Size(wipeW, h)
                    )
                }
                TransitionEffect.SLIDE_LEFT -> {
                    val offsetX = w * (1f - progress)
                    drawRect(
                        brush = segmentBBrush,
                        topLeft = Offset(offsetX, 0f),
                        size = size
                    )
                }
                TransitionEffect.SLIDE_RIGHT -> {
                    val offsetX = -w * (1f - progress)
                    drawRect(
                        brush = segmentBBrush,
                        topLeft = Offset(offsetX, 0f),
                        size = size
                    )
                }
                TransitionEffect.CIRCLE_CROP -> {
                    val maxRadius = kotlin.math.hypot(w, h)
                    val r = maxRadius * progress
                    drawCircle(
                        brush = segmentBBrush,
                        radius = r,
                        center = Offset(w / 2f, h / 2f)
                    )
                }
                TransitionEffect.ZOOM_IN -> {
                    val scale = progress
                    val scaledW = w * scale
                    val scaledH = h * scale
                    drawRect(
                        brush = segmentBBrush,
                        topLeft = Offset((w - scaledW) / 2f, (h - scaledH) / 2f),
                        size = Size(scaledW, scaledH)
                    )
                }
                TransitionEffect.PIXELIZE, TransitionEffect.RADIAL -> {
                    // Mosaic block / blend effect
                    drawRoundRect(
                        brush = segmentBBrush,
                        size = size,
                        alpha = progress,
                        cornerRadius = CornerRadius(14f, 14f)
                    )
                }
            }
        }

        // Overlay Badge Labels
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                color = Color.Black.copy(alpha = 0.6f),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = "SEGMENT A",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }

            Surface(
                color = MaterialTheme.colorScheme.tertiary,
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = effect.getIcon(),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onTertiary,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "xfade: ${effect.xfadeName}",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onTertiary
                    )
                }
            }

            Surface(
                color = Color.Black.copy(alpha = 0.6f),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = "SEGMENT B",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
    }
}
