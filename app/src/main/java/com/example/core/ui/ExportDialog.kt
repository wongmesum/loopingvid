package com.example.core.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.core.media.AudioMetadata
import com.example.feature.mastering.AudioMetadataEditorCard

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ExportDialog(
    showDialog: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (fileName: String, format: String, destination: String, resolution: String, frameRate: String, bitrate: String, aspectRatio: String) -> Unit,
    onEnqueue: ((fileName: String, format: String, destination: String, resolution: String, frameRate: String, bitrate: String, aspectRatio: String) -> Unit)? = null,
    defaultFileName: String,
    availableFormats: List<String>,
    defaultDestination: String = "Downloads",
    // ID3/container metadata (title/artist/album/genre/year/comment/cover art), collected here
    // at export time instead of always-visible on the source screen's main page.
    metadata: AudioMetadata = AudioMetadata(),
    onMetadataChanged: (AudioMetadata) -> Unit = {}
) {
    if (showDialog) {
        var fileName by remember { mutableStateOf(defaultFileName) }
        var selectedFormat by remember { mutableStateOf(if (availableFormats.isNotEmpty()) availableFormats[0] else "") }
        var selectedDestination by remember(defaultDestination) { mutableStateOf(defaultDestination) }
        var selectedResolution by remember { mutableStateOf("1080p") }
        var selectedFrameRate by remember { mutableStateOf("30fps") }
        var selectedBitrate by remember { mutableStateOf("Medium") }
        var selectedAspectRatio by remember { mutableStateOf("Asli") }
        var selectedPreset by remember { mutableStateOf("Custom") }

        LaunchedEffect(defaultFileName, availableFormats) {
            fileName = defaultFileName
            if (availableFormats.isNotEmpty()) {
                selectedFormat = availableFormats[0]
            }
        }
        
        val destinations = listOf("Downloads", "Movies", "Music", "Documents")
        val resolutions = listOf("480p", "720p", "1080p", "4K")
        val frameRates = listOf("24fps", "30fps", "60fps")
        val bitrates = listOf("Low", "Medium", "High")
        val aspectRatios = listOf("Asli", "16:9", "9:16", "1:1", "4:5")
        val platformPresets = listOf("Custom", "Instagram Reel", "TikTok", "YouTube Short", "YouTube Video")

        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Simpan Sebagai / Ekspor") },
            text = {
                Column(
                    modifier = Modifier
                        .heightIn(max = 480.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    OutlinedTextField(
                        value = fileName,
                        onValueChange = { fileName = it },
                        label = { Text("Nama File") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Format", style = MaterialTheme.typography.labelMedium)
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Platform Preset", style = MaterialTheme.typography.labelMedium)
                        var expanded by remember { mutableStateOf(false) }
                        ExposedDropdownMenuBox(
                            expanded = expanded,
                            onExpandedChange = { expanded = !expanded }
                        ) {
                            OutlinedTextField(
                                value = selectedPreset,
                                onValueChange = {},
                                readOnly = true,
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                                modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth()
                            )
                            ExposedDropdownMenu(
                                expanded = expanded,
                                onDismissRequest = { expanded = false }
                            ) {
                                platformPresets.forEach { preset ->
                                    DropdownMenuItem(
                                        text = { Text(preset) },
                                        onClick = {
                                            selectedPreset = preset
                                            expanded = false
                                            when (preset) {
                                                "Instagram Reel", "TikTok", "YouTube Short" -> {
                                                    selectedResolution = "1080p"
                                                    selectedBitrate = "High"
                                                    selectedAspectRatio = "9:16"
                                                }
                                                "YouTube Video" -> {
                                                    selectedResolution = "1080p"
                                                    selectedBitrate = "High"
                                                    selectedAspectRatio = "16:9"
                                                }
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                            availableFormats.forEach { format ->
                                FilterChip(
                                    selected = selectedFormat == format,
                                    onClick = { selectedFormat = format },
                                    label = { Text(format) }
                                )
                            }
                        }
                    }
                    
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Lokasi Penyimpanan", style = MaterialTheme.typography.labelMedium)
                        var expanded by remember { mutableStateOf(false) }
                        ExposedDropdownMenuBox(
                            expanded = expanded,
                            onExpandedChange = { expanded = !expanded }
                        ) {
                            OutlinedTextField(
                                value = selectedDestination,
                                onValueChange = {},
                                readOnly = true,
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                                modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth()
                            )
                            ExposedDropdownMenu(
                                expanded = expanded,
                                onDismissRequest = { expanded = false }
                            ) {
                                destinations.forEach { dest ->
                                    DropdownMenuItem(
                                        text = { Text(dest) },
                                        onClick = {
                                            selectedDestination = dest
                                            expanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Resolusi", style = MaterialTheme.typography.labelMedium)
                            var expanded by remember { mutableStateOf(false) }
                            ExposedDropdownMenuBox(
                                expanded = expanded,
                                onExpandedChange = { expanded = !expanded }
                            ) {
                                OutlinedTextField(
                                    value = selectedResolution,
                                    onValueChange = {},
                                    readOnly = true,
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                                    modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth()
                                )
                                ExposedDropdownMenu(
                                    expanded = expanded,
                                    onDismissRequest = { expanded = false }
                                ) {
                                    resolutions.forEach { res ->
                                        DropdownMenuItem(
                                            text = { Text(res) },
                                            onClick = {
                                                selectedResolution = res
                                                expanded = false
                                                selectedPreset = "Custom"
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Frame Rate", style = MaterialTheme.typography.labelMedium)
                            var expanded by remember { mutableStateOf(false) }
                            ExposedDropdownMenuBox(
                                expanded = expanded,
                                onExpandedChange = { expanded = !expanded }
                            ) {
                                OutlinedTextField(
                                    value = selectedFrameRate,
                                    onValueChange = {},
                                    readOnly = true,
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                                    modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth()
                                )
                                ExposedDropdownMenu(
                                    expanded = expanded,
                                    onDismissRequest = { expanded = false }
                                ) {
                                    frameRates.forEach { fps ->
                                        DropdownMenuItem(
                                            text = { Text(fps) },
                                            onClick = {
                                                selectedFrameRate = fps
                                                expanded = false
                                                selectedPreset = "Custom"
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Bitrate", style = MaterialTheme.typography.labelMedium)
                            var expanded by remember { mutableStateOf(false) }
                            ExposedDropdownMenuBox(
                                expanded = expanded,
                                onExpandedChange = { expanded = !expanded }
                            ) {
                                OutlinedTextField(
                                    value = selectedBitrate,
                                    onValueChange = {},
                                    readOnly = true,
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                                    modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth()
                                )
                                ExposedDropdownMenu(
                                    expanded = expanded,
                                    onDismissRequest = { expanded = false }
                                ) {
                                    bitrates.forEach { br ->
                                        DropdownMenuItem(
                                            text = { Text(br) },
                                            onClick = {
                                                selectedBitrate = br
                                                expanded = false
                                                selectedPreset = "Custom"
                                            }
                                        )
                                    }
                                }
                            }
                        }
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Aspect Ratio", style = MaterialTheme.typography.labelMedium)
                            var expanded by remember { mutableStateOf(false) }
                            ExposedDropdownMenuBox(
                                expanded = expanded,
                                onExpandedChange = { expanded = !expanded }
                            ) {
                                OutlinedTextField(
                                    value = selectedAspectRatio,
                                    onValueChange = {},
                                    readOnly = true,
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                                    modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth()
                                )
                                ExposedDropdownMenu(
                                    expanded = expanded,
                                    onDismissRequest = { expanded = false }
                                ) {
                                    aspectRatios.forEach { ar ->
                                        DropdownMenuItem(
                                            text = { Text(ar) },
                                            onClick = {
                                                selectedAspectRatio = ar
                                                expanded = false
                                                selectedPreset = "Custom"
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // ID3/container metadata (title/artist/album/genre/year/comment/cover art),
                    // collected here at export time. Optional; blank fields are skipped by
                    // FFmpeg's `-metadata`.
                    AudioMetadataEditorCard(
                        metadata = metadata,
                        onMetadataChanged = onMetadataChanged
                    )
                }
            },
            confirmButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (onEnqueue != null) {
                        OutlinedButton(onClick = {
                            // Call directly (synchronously). The handler reads the job config and
                            // dismisses the dialog itself. Do NOT launch async + dismiss here: that
                            // races the dialog's dismiss (which nulls currentJobConfig) ahead of the
                            // handler reading it, causing the export to silently no-op.
                            onEnqueue(fileName, selectedFormat, selectedDestination, selectedResolution, selectedFrameRate, selectedBitrate, selectedAspectRatio)
                        }) {
                            Text("Antrekan")
                        }
                    }
                    Button(onClick = {
                        // Call directly (synchronously); confirmExport() reads currentJobConfig and
                        // dismisses the dialog itself. Launching async and dismissing here caused a
                        // race that nulled the config before it was read -> "nothing happens".
                        onConfirm(fileName, selectedFormat, selectedDestination, selectedResolution, selectedFrameRate, selectedBitrate, selectedAspectRatio)
                    }) {
                        Text("Ekspor Sekarang")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text("Batal")
                }
            },
            shape = RoundedCornerShape(16.dp)
        )
    }
}
