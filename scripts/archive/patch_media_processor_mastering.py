import re

with open('app/src/main/java/com/example/core/ffmpeg/MediaProcessor.kt', 'r') as f:
    content = f.read()

content = content.replace(
    '        exportFormat: String = "mp4"\n    ): RenderJobEntity',
    '        exportFormat: String = "mp4",\n        aspectRatio: String = "Asli"\n    ): RenderJobEntity'
)

content = content.replace(
    '            bitrate = bitrate\n        )',
    '            bitrate = bitrate,\n            aspectRatio = aspectRatio\n        )'
)

# And executeEditorJob
content = content.replace(
    '        bitrate: String = "Medium"\n    ): RenderJobEntity',
    '        bitrate: String = "Medium",\n        aspectRatio: String = "Asli"\n    ): RenderJobEntity'
)

with open('app/src/main/java/com/example/core/ffmpeg/MediaProcessor.kt', 'w') as f:
    f.write(content)

