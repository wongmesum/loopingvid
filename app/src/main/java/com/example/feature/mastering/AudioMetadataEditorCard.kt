package com.example.feature.mastering

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.example.core.media.AudioMetadata

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AudioMetadataEditorCard(
    metadata: AudioMetadata,
    onMetadataChanged: (AudioMetadata) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = "Metadata",
                    tint = MaterialTheme.colorScheme.secondary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "ID3 Metadata Tags",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            }

            OutlinedTextField(
                value = metadata.title,
                onValueChange = { onMetadataChanged(metadata.copy(title = it)) },
                label = { Text("Title") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            
            OutlinedTextField(
                value = metadata.artist,
                onValueChange = { onMetadataChanged(metadata.copy(artist = it)) },
                label = { Text("Artist") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = metadata.albumArtist,
                onValueChange = { onMetadataChanged(metadata.copy(albumArtist = it)) },
                label = { Text("Album Artist") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = metadata.album,
                    onValueChange = { onMetadataChanged(metadata.copy(album = it)) },
                    label = { Text("Album") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )

                val yearError = !isValidYear(metadata.year)
                OutlinedTextField(
                    value = metadata.year,
                    onValueChange = { onMetadataChanged(metadata.copy(year = it.filter { c -> c.isDigit() }.take(4))) },
                    label = { Text("Year") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    isError = yearError,
                    supportingText = if (yearError) { { Text("4-digit year, e.g. 2024") } } else null
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = metadata.genre,
                    onValueChange = { onMetadataChanged(metadata.copy(genre = it)) },
                    label = { Text("Genre") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )

                val trackError = !isValidTrackNumber(metadata.trackNumber)
                OutlinedTextField(
                    value = metadata.trackNumber,
                    onValueChange = { onMetadataChanged(metadata.copy(trackNumber = it.filter { c -> c.isDigit() || c == '/' }.take(7))) },
                    label = { Text("Track #") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    isError = trackError,
                    supportingText = if (trackError) { { Text("Numbers only, e.g. 3 or 3/12") } } else null
                )
            }

            // Cover art (album art / thumbnail). Applied as a separate lossless remux pass; not
            // supported for WAV outputs.
            val coverPickerLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.GetContent()
            ) { uri ->
                if (uri != null) onMetadataChanged(metadata.copy(coverArtUri = uri.toString()))
            }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Cover Art (not supported for WAV)",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (metadata.coverArtUri != null) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surface)
                        ) {
                            Image(
                                painter = rememberAsyncImagePainter(metadata.coverArtUri),
                                contentDescription = "Cover art preview",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surface),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Image,
                                contentDescription = "No cover art",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    OutlinedButton(onClick = { coverPickerLauncher.launch("image/*") }) {
                        Text(if (metadata.coverArtUri != null) "Change" else "Choose Image")
                    }
                    if (metadata.coverArtUri != null) {
                        IconButton(onClick = { onMetadataChanged(metadata.copy(coverArtUri = null)) }) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = "Remove cover art")
                        }
                    }
                }
            }
        }
    }
}

/** Blank is allowed (field is optional); otherwise must be exactly 4 digits. */
private fun isValidYear(year: String): Boolean {
    if (year.isBlank()) return true
    return year.length == 4 && year.all { it.isDigit() }
}

/**
 * Blank is allowed (field is optional); otherwise must be a positive integer, optionally with a
 * "/total" suffix (the common ID3/MP4 "track N of M" convention, e.g. "3/12").
 */
private fun isValidTrackNumber(track: String): Boolean {
    if (track.isBlank()) return true
    val parts = track.split("/")
    if (parts.size > 2) return false
    return parts.all { it.isNotBlank() && it.all { c -> c.isDigit() } }
}
