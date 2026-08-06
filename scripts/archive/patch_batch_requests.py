import re

with open('app/src/main/java/com/example/core/ui/ExportViewModel.kt', 'r') as f:
    content = f.read()

content = content.replace(
    'bitrate = state.selectedBitrate,',
    'bitrate = state.selectedBitrate,\n                aspectRatio = state.selectedAspectRatio,'
)

with open('app/src/main/java/com/example/core/ui/ExportViewModel.kt', 'w') as f:
    f.write(content)

