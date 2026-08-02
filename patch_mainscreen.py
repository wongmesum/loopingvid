import re

with open('app/src/main/java/com/example/ui/navigation/MainScreen.kt', 'r') as f:
    content = f.read()

content = content.replace(
    'onConfirm = { fileName, format, destination, resolution, frameRate, bitrate ->',
    'onConfirm = { fileName, format, destination, resolution, frameRate, bitrate, aspectRatio ->'
)
content = content.replace(
    'onEnqueue = { fileName, format, destination, resolution, frameRate, bitrate ->',
    'onEnqueue = { fileName, format, destination, resolution, frameRate, bitrate, aspectRatio ->'
)
content = content.replace(
    'exportViewModel.setExportSettings(resolution, frameRate, bitrate)',
    'exportViewModel.setExportSettings(resolution, frameRate, bitrate, aspectRatio)'
)

with open('app/src/main/java/com/example/ui/navigation/MainScreen.kt', 'w') as f:
    f.write(content)

