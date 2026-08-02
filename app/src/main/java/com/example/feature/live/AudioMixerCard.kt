package com.example.feature.live

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.MusicOff
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VideocamOff
import androidx.compose.material.icons.automirrored.filled.VolumeDown
import androidx.compose.material.icons.automirrored.filled.VolumeMute
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

val BG_MUSIC_TRACKS = listOf(
    "Chill Lo-Fi Beat",
    "Upbeat Energy",
    "Acoustic Stream",
    "Ambient Pad",
    "None"
)

/**
 * Real-Time Audio Mixer Card
 * Allows independent volume adjustment & muting for Video Source, Microphone, and Background Music.
 */
@Composable
fun AudioMixerCard(
    videoVolume: Float,
    isVideoMuted: Boolean,
    micVolume: Float,
    isMicMuted: Boolean,
    bgMusicVolume: Float,
    isBgMusicMuted: Boolean,
    bgMusicTrack: String,
    isMicDuckingEnabled: Boolean,
    masterVolume: Float,
    isMasterMuted: Boolean,
    videoVuLevel: Float,
    micVuLevel: Float,
    bgMusicVuLevel: Float,
    masterVuLevel: Float,
    onVideoVolumeChange: (Float) -> Unit,
    onVideoMuteToggle: () -> Unit,
    onMicVolumeChange: (Float) -> Unit,
    onMicMuteToggle: () -> Unit,
    onBgMusicVolumeChange: (Float) -> Unit,
    onBgMusicMuteToggle: () -> Unit,
    onBgMusicTrackChange: (String) -> Unit,
    onMicDuckingToggle: () -> Unit,
    onMasterVolumeChange: (Float) -> Unit,
    onMasterMuteToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("audio_mixer_card"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.GraphicEq,
                        contentDescription = "Audio Mixer Console",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "Real-Time Audio Mixer",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "3-Channel live input leveling & background track balance",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Master Mute Quick Button
                IconButton(
                    onClick = onMasterMuteToggle,
                    modifier = Modifier.testTag("master_mute_button")
                ) {
                    Icon(
                        imageVector = if (isMasterMuted || masterVolume == 0f) Icons.AutoMirrored.Filled.VolumeMute else Icons.AutoMirrored.Filled.VolumeUp,
                        contentDescription = "Master Mute Toggle",
                        tint = if (isMasterMuted) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Master Output Level Strip
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp))
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "MASTER OUTPUT MIX",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold),
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isMasterMuted) "(MUTED)" else "${(masterVolume * 100).toInt()}%",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = if (isMasterMuted) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Live Output VU Peak Bar Indicator
                    VuPeakBar(
                        level = if (isMasterMuted) 0f else masterVuLevel,
                        modifier = Modifier
                            .width(100.dp)
                            .testTag("master_vu_meter")
                    )
                }

                Slider(
                    value = if (isMasterMuted) 0f else masterVolume,
                    onValueChange = onMasterVolumeChange,
                    valueRange = 0f..1f,
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.primary,
                        activeTrackColor = MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("master_volume_slider")
                )
            }

            // Channel 1: Video Source Audio
            AudioChannelStrip(
                channelTitle = "Main Video Source",
                icon = if (isVideoMuted) Icons.Default.VideocamOff else Icons.Default.Videocam,
                volume = videoVolume,
                isMuted = isVideoMuted,
                vuLevel = videoVuLevel,
                onVolumeChange = onVideoVolumeChange,
                onMuteToggle = onVideoMuteToggle,
                sliderTag = "video_volume_slider",
                muteButtonTag = "video_mute_button"
            )

            // Channel 2: Microphone Input
            AudioChannelStrip(
                channelTitle = "Microphone Input",
                icon = if (isMicMuted) Icons.Default.MicOff else Icons.Default.Mic,
                volume = micVolume,
                isMuted = isMicMuted,
                vuLevel = micVuLevel,
                onVolumeChange = onMicVolumeChange,
                onMuteToggle = onMicMuteToggle,
                sliderTag = "mic_volume_slider",
                muteButtonTag = "mic_mute_button"
            ) {
                // Mic Ducking Toggle Switch
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.RecordVoiceOver,
                            contentDescription = "Mic Ducking",
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Auto-Duck Music when speaking",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Switch(
                        checked = isMicDuckingEnabled,
                        onCheckedChange = { onMicDuckingToggle() },
                        modifier = Modifier.testTag("mic_ducking_switch"),
                        colors = SwitchDefaults.colors(checkedThumbColor = Color.White)
                    )
                }
            }

            // Channel 3: Background Music
            AudioChannelStrip(
                channelTitle = "Background Music",
                icon = if (isBgMusicMuted || bgMusicTrack == "None") Icons.Default.MusicOff else Icons.Default.MusicNote,
                volume = bgMusicVolume,
                isMuted = isBgMusicMuted || bgMusicTrack == "None",
                vuLevel = bgMusicVuLevel,
                onVolumeChange = onBgMusicVolumeChange,
                onMuteToggle = onBgMusicMuteToggle,
                sliderTag = "bg_music_volume_slider",
                muteButtonTag = "bg_music_mute_button"
            ) {
                // Background Track Selection Chips
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Select Audio Track:",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        BG_MUSIC_TRACKS.forEach { track ->
                            val isSelected = bgMusicTrack == track
                            FilterChip(
                                selected = isSelected,
                                onClick = { onBgMusicTrackChange(track) },
                                label = {
                                    Text(
                                        text = track,
                                        fontSize = 11.sp
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.secondary,
                                    selectedLabelColor = Color.White
                                ),
                                modifier = Modifier.testTag("bg_track_chip_$track")
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Individual Audio Channel Control Strip
 */
@Composable
private fun AudioChannelStrip(
    channelTitle: String,
    icon: ImageVector,
    volume: Float,
    isMuted: Boolean,
    vuLevel: Float,
    onVolumeChange: (Float) -> Unit,
    onMuteToggle: () -> Unit,
    sliderTag: String,
    muteButtonTag: String,
    extraControls: (@Composable () -> Unit)? = null
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = channelTitle,
                    tint = if (isMuted) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = channelTitle,
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (isMuted) "(Muted)" else "${(volume * 100).toInt()}%",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isMuted) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                // Live Peak Level Bar
                VuPeakBar(
                    level = if (isMuted) 0f else vuLevel,
                    modifier = Modifier.width(70.dp)
                )

                Spacer(modifier = Modifier.width(8.dp))

                IconButton(
                    onClick = onMuteToggle,
                    modifier = Modifier
                        .size(32.dp)
                        .testTag(muteButtonTag)
                ) {
                    Icon(
                        imageVector = if (isMuted) Icons.AutoMirrored.Filled.VolumeMute else Icons.AutoMirrored.Filled.VolumeDown,
                        contentDescription = "Mute $channelTitle",
                        tint = if (isMuted) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        Slider(
            value = if (isMuted) 0f else volume,
            onValueChange = onVolumeChange,
            valueRange = 0f..1f,
            colors = SliderDefaults.colors(
                thumbColor = if (isMuted) Color.Gray else MaterialTheme.colorScheme.secondary,
                activeTrackColor = if (isMuted) Color.Gray else MaterialTheme.colorScheme.secondary
            ),
            modifier = Modifier
                .fillMaxWidth()
                .testTag(sliderTag)
        )

        extraControls?.invoke()
    }
}

/**
 * Animated VU Level Bar (Green -> Amber -> Red peak indicator)
 */
@Composable
private fun VuPeakBar(
    level: Float,
    modifier: Modifier = Modifier
) {
    val barColor by animateColorAsState(
        targetValue = when {
            level > 0.85f -> Color(0xFFEF4444) // Red Peak
            level > 0.65f -> Color(0xFFF59E0B) // Amber
            else -> Color(0xFF10B981)          // Green
        },
        label = "vuColor"
    )

    LinearProgressIndicator(
        progress = { level.coerceIn(0f, 1f) },
        modifier = modifier
            .height(6.dp)
            .clip(RoundedCornerShape(3.dp)),
        color = barColor,
        trackColor = MaterialTheme.colorScheme.surfaceVariant
    )
}
