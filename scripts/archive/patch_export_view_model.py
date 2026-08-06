import re

with open('app/src/main/java/com/example/core/ui/ExportViewModel.kt', 'r') as f:
    content = f.read()

# Update ExportUiState
# val selectedBitrate: String = "Medium",
# val selectedAspectRatio: String = "Asli",
content = re.sub(
    r'val selectedBitrate: String = "Medium",',
    'val selectedBitrate: String = "Medium",\n    val selectedAspectRatio: String = "Asli",',
    content
)

# Update _uiState.update functions for resolution, framerate, bitrate
content = content.replace(
    'fun updateResolution(res: String) {',
    'fun updateAspectRatio(ar: String) {\n        _uiState.update { it.copy(selectedAspectRatio = ar) }\n    }\n\n    fun updateResolution(res: String) {'
)

with open('app/src/main/java/com/example/core/ui/ExportViewModel.kt', 'w') as f:
    f.write(content)
