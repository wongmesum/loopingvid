package com.example.feature.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.core.ui.ExportJobConfig
import com.example.core.ui.ExportViewModel

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun TrimmedVideoAudioMasteringCard(
    mediaUri: String?,
    trimStartSec: Double,
    trimEndSec: Double,
    exportViewModel: ExportViewModel,
    ffmpegFilterString: String? = null,
    modifier: Modifier = Modifier
) {
    var isNormalizationEnabled by remember { mutableStateOf(true) }
    var selectedPreset by remember { mutableStateOf("Voice Clarity") }
    var targetLufs by remember { mutableDoubleStateOf(-14.0) }

    val presets = listOf("Voice Clarity", "Music Master", "Bass Boost", "Podcast Clean", "Balanced Loudness")
    val lufsPresets = listOf(-14.0, -16.0, -12.0, -10.0)

    val effectiveEnd = if (trimEndSec > trimStartSec) trimEndSec else (trimStartSec + 15.0)
    val durationSec = (effectiveEnd - trimStartSec).coerceAtLeast(0.1)

    // No outer Card here: this composable's only call site (EditorScreen's AUDIO_SPECTRUM tab)
    // already wraps it in a CollapsibleToolPanel, which renders its own surfaceVariant Card.
    // Adding a second identically-colored Card on top produced a visible "card inside card"
    // double border with no added visual hierarchy.
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.GraphicEq,
                        contentDescription = "FFmpeg Audio Mastering",
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "FFmpeg Audio Mastering & Volume Normalization",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Segment: %.1fs - %.1fs (%.1fs total)".format(trimStartSec, effectiveEnd, durationSec),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
                Switch(
                    checked = isNormalizationEnabled,
                    onCheckedChange = { isNormalizationEnabled = it },
                    colors = SwitchDefaults.colors(checkedThumbColor = MaterialTheme.colorScheme.secondary),
                    modifier = Modifier.testTag("toggle_trimmed_audio_normalization")
                )
            }

            if (isNormalizationEnabled) {
                // Preset selector chips
                Text(
                    text = "Mastering EQ Preset",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                androidx.compose.foundation.layout.FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    presets.forEach { preset ->
                        FilterChip(
                            selected = selectedPreset == preset,
                            onClick = { selectedPreset = preset },
                            label = {
                                Text(
                                    preset,
                                    style = MaterialTheme.typography.labelSmall,
                                    maxLines = 1,
                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.secondary,
                                selectedLabelColor = MaterialTheme.colorScheme.onSecondary
                            ),
                            modifier = Modifier.testTag("preset_chip_${preset.replace(" ", "_")}")
                        )
                    }
                }

                // LUFS Target Slider & Presets
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Target Loudness (LUFS)",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "%.1f LUFS".format(targetLufs),
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.secondary
                    )
                }

                androidx.compose.foundation.layout.FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    lufsPresets.forEach { lufs ->
                        FilterChip(
                            selected = targetLufs == lufs,
                            onClick = { targetLufs = lufs },
                            label = { Text("${lufs.toInt()} LUFS") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.tertiary,
                                selectedLabelColor = MaterialTheme.colorScheme.onTertiary
                            ),
                            modifier = Modifier.testTag("lufs_chip_${lufs.toInt()}")
                        )
                    }
                }

                Slider(
                    value = targetLufs.toFloat(),
                    onValueChange = { targetLufs = it.toDouble() },
                    valueRange = -24f..-6f,
                    colors = SliderDefaults.colors(thumbColor = MaterialTheme.colorScheme.secondary),
                    modifier = Modifier.testTag("target_lufs_slider")
                )
            }

            // Export Action Button
            Button(
                onClick = {
                    mediaUri?.let { uri ->
                        exportViewModel.showDialogForTrimmedVideoMastering(
                            defaultFileName = "MasteredTrim_${(durationSec).toInt()}s",
                            config = ExportJobConfig.TrimmedVideoMasteringJob(
                                inputUri = uri,
                                trimStartSec = trimStartSec,
                                trimEndSec = effectiveEnd,
                                presetName = selectedPreset,
                                targetLufs = targetLufs,
                                ffmpegFilterString = ffmpegFilterString
                            )
                        )
                    }
                },
                enabled = mediaUri != null,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("export_trimmed_master_button"),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
            ) {
                Icon(
                    imageVector = Icons.Default.Equalizer,
                    contentDescription = "Export Trimmed Master",
                    tint = MaterialTheme.colorScheme.onSecondary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Export Mastered & Normalized Trimmed Video",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSecondary,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
            }
    }
}
