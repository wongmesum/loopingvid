import re

with open('app/src/main/java/com/example/core/ui/ExportViewModel.kt', 'r') as f:
    content = f.read()

# Add aspectRatio to TwoPassAudioNormalizationJob
content = content.replace(
    '                frameRate = state.selectedFrameRate,\n                bitrate = state.selectedBitrate\n            )',
    '                frameRate = state.selectedFrameRate,\n                bitrate = state.selectedBitrate,\n                aspectRatio = state.selectedAspectRatio\n            )'
)

# Add aspectRatio to TrimmedVideoMasteringJob
content = content.replace(
    '                frameRate = state.selectedFrameRate,\n                bitrate = state.selectedBitrate\n            )',
    '                frameRate = state.selectedFrameRate,\n                bitrate = state.selectedBitrate,\n                aspectRatio = state.selectedAspectRatio\n            )'
)

# In confirmExport
content = content.replace(
    '                            frameRate = state.selectedFrameRate,\n                            bitrate = state.selectedBitrate\n                        )',
    '                            frameRate = state.selectedFrameRate,\n                            bitrate = state.selectedBitrate,\n                            aspectRatio = state.selectedAspectRatio\n                        )'
)

with open('app/src/main/java/com/example/core/ui/ExportViewModel.kt', 'w') as f:
    f.write(content)
