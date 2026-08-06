import re

with open('app/src/main/java/com/example/core/ui/ExportDialog.kt', 'r') as f:
    content = f.read()

# Update signature
content = content.replace(
    'onConfirm: (fileName: String, format: String, destination: String, resolution: String, frameRate: String, bitrate: String) -> Unit,',
    'onConfirm: (fileName: String, format: String, destination: String, resolution: String, frameRate: String, bitrate: String, aspectRatio: String) -> Unit,'
)
content = content.replace(
    'onEnqueue: ((fileName: String, format: String, destination: String, resolution: String, frameRate: String, bitrate: String) -> Unit)? = null,',
    'onEnqueue: ((fileName: String, format: String, destination: String, resolution: String, frameRate: String, bitrate: String, aspectRatio: String) -> Unit)? = null,'
)

# Add state variables
state_vars = """        var selectedResolution by remember { mutableStateOf("1080p") }
        var selectedFrameRate by remember { mutableStateOf("30fps") }
        var selectedBitrate by remember { mutableStateOf("Medium") }
        var selectedAspectRatio by remember { mutableStateOf("Asli") }
        var selectedPreset by remember { mutableStateOf("Custom") }
"""
content = re.sub(
    r'        var selectedResolution by remember \{ mutableStateOf\("1080p"\) \}\n        var selectedFrameRate by remember \{ mutableStateOf\("30fps"\) \}\n        var selectedBitrate by remember \{ mutableStateOf\("Medium"\) \}',
    state_vars.strip('\n'),
    content
)

# Lists for dropdowns
lists = """        val destinations = listOf("Downloads", "Movies", "Music", "Documents")
        val resolutions = listOf("480p", "720p", "1080p", "4K")
        val frameRates = listOf("24fps", "30fps", "60fps")
        val bitrates = listOf("Low", "Medium", "High")
        val aspectRatios = listOf("Asli", "16:9", "9:16", "1:1", "4:5")
        val platformPresets = listOf("Custom", "Instagram Reel", "TikTok", "YouTube Short", "YouTube Video")
"""
content = re.sub(
    r'        val destinations = listOf\("Downloads", "Movies", "Music", "Documents"\)\n        val resolutions = listOf\("480p", "720p", "1080p", "4K"\)\n        val frameRates = listOf\("24fps", "30fps", "60fps"\)\n        val bitrates = listOf\("Low", "Medium", "High"\)',
    lists.strip('\n'),
    content
)

preset_ui = """                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
                                modifier = Modifier.menuAnchor().fillMaxWidth()
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

                    Row("""

content = content.replace('                    Row(', preset_ui, 1)

aspect_ratio_ui = """                    Row(
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
                                    modifier = Modifier.menuAnchor().fillMaxWidth()
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
                                    modifier = Modifier.menuAnchor().fillMaxWidth()
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
                    }"""

# Remove old Bitrate column and insert new Row for Bitrate and Aspect Ratio
# Let's find the old Bitrate column
old_bitrate = """                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
                                modifier = Modifier.menuAnchor().fillMaxWidth()
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
                                        }
                                    )
                                }
                            }
                        }
                    }"""

content = content.replace(old_bitrate, aspect_ratio_ui.strip('\n'))

# Also make sure changing resolution and framerate sets preset to Custom
content = content.replace('selectedResolution = res\n                                                expanded = false', 'selectedResolution = res\n                                                expanded = false\n                                                selectedPreset = "Custom"')
content = content.replace('selectedFrameRate = fps\n                                                expanded = false', 'selectedFrameRate = fps\n                                                expanded = false\n                                                selectedPreset = "Custom"')


# Update onEnqueue and onConfirm calls
content = content.replace(
    'onEnqueue(fileName, selectedFormat, selectedDestination, selectedResolution, selectedFrameRate, selectedBitrate)',
    'onEnqueue(fileName, selectedFormat, selectedDestination, selectedResolution, selectedFrameRate, selectedBitrate, selectedAspectRatio)'
)
content = content.replace(
    'onConfirm(fileName, selectedFormat, selectedDestination, selectedResolution, selectedFrameRate, selectedBitrate)',
    'onConfirm(fileName, selectedFormat, selectedDestination, selectedResolution, selectedFrameRate, selectedBitrate, selectedAspectRatio)'
)

with open('app/src/main/java/com/example/core/ui/ExportDialog.kt', 'w') as f:
    f.write(content)
