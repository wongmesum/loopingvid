import re

with open('app/src/main/java/com/example/core/ffmpeg/MediaProcessor.kt', 'r') as f:
    content = f.read()

# Add aspectRatio to executeLoopJob
content = content.replace(
    '        bitrate: String = "Medium"\n    ): RenderJobEntity',
    '        bitrate: String = "Medium",\n        aspectRatio: String = "Asli"\n    ): RenderJobEntity'
)

# Pass it to buildLoopCommand
content = content.replace(
    '                    bitrate = bitrate\n                )',
    '                    bitrate = bitrate,\n                    aspectRatio = aspectRatio\n                )'
)
content = content.replace(
    '                    bitrate = bitrate\n                )',
    '                    bitrate = bitrate,\n                    aspectRatio = aspectRatio\n                )'
) # This might miss some if indentation is different. Let's do it safely.

with open('app/src/main/java/com/example/core/ffmpeg/MediaProcessor.kt', 'w') as f:
    f.write(content)
