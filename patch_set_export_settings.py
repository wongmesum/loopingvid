import re

with open('app/src/main/java/com/example/core/ui/ExportViewModel.kt', 'r') as f:
    content = f.read()

replacement = """    fun setExportSettings(resolution: String, frameRate: String, bitrate: String, aspectRatio: String = "Asli") {
        _uiState.value = _uiState.value.copy(
            selectedResolution = resolution,
            selectedFrameRate = frameRate,
            selectedBitrate = bitrate,
            selectedAspectRatio = aspectRatio
        )
    }"""

content = re.sub(
    r'    fun setExportSettings\(resolution: String, frameRate: String, bitrate: String\) \{.*?\}',
    replacement,
    content,
    flags=re.DOTALL
)

with open('app/src/main/java/com/example/core/ui/ExportViewModel.kt', 'w') as f:
    f.write(content)

