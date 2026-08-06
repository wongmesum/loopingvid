import re

with open('app/src/main/java/com/example/core/ffmpeg/MediaProcessor.kt', 'r') as f:
    content = f.read()

content = content.replace(
    'min(iw\\,ih*16/9):min(ih\\,iw*9/16)',
    'min(iw\\\\,ih*16/9):min(ih\\\\,iw*9/16)'
)
content = content.replace(
    'min(iw\\,ih*9/16):min(ih\\,iw*16/9)',
    'min(iw\\\\,ih*9/16):min(ih\\\\,iw*16/9)'
)
content = content.replace(
    'min(iw\\,ih):min(iw\\,ih)',
    'min(iw\\\\,ih):min(iw\\\\,ih)'
)
content = content.replace(
    'min(iw\\,ih*4/5):min(ih\\,iw*5/4)',
    'min(iw\\\\,ih*4/5):min(ih\\\\,iw*5/4)'
)

with open('app/src/main/java/com/example/core/ffmpeg/MediaProcessor.kt', 'w') as f:
    f.write(content)

