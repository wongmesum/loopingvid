package com.example.feature.mastering

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
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

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = metadata.album,
                    onValueChange = { onMetadataChanged(metadata.copy(album = it)) },
                    label = { Text("Album") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                
                OutlinedTextField(
                    value = metadata.year,
                    onValueChange = { onMetadataChanged(metadata.copy(year = it)) },
                    label = { Text("Year") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
            }

            OutlinedTextField(
                value = metadata.genre,
                onValueChange = { onMetadataChanged(metadata.copy(genre = it)) },
                label = { Text("Genre") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
        }
    }
}
