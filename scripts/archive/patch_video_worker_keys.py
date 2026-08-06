import re

with open('app/src/main/java/com/example/core/work/VideoExportWorker.kt', 'r') as f:
    content = f.read()

content = content.replace(
    '        const val KEY_OVERLAY_POSITION = "KEY_OVERLAY_POSITION"',
    '        const val KEY_OVERLAY_POSITION = "KEY_OVERLAY_POSITION"\n        const val KEY_RESOLUTION = "KEY_RESOLUTION"\n        const val KEY_FRAME_RATE = "KEY_FRAME_RATE"\n        const val KEY_BITRATE = "KEY_BITRATE"\n        const val KEY_ASPECT_RATIO = "KEY_ASPECT_RATIO"'
)

with open('app/src/main/java/com/example/core/work/VideoExportWorker.kt', 'w') as f:
    f.write(content)
