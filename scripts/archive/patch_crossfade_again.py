import re

with open('app/src/main/java/com/example/core/ffmpeg/FFmpegWrapper.kt', 'r') as f:
    content = f.read()

# Let's just find and replace the problematic line in buildTrimmedVideoAudioMasteringCommand
import sys
lines = content.split('\n')
for i, line in enumerate(lines):
    if 'val filterGraph = "xfade=' in line and i > 250:
        lines[i] = ""

with open('app/src/main/java/com/example/core/ffmpeg/FFmpegWrapper.kt', 'w') as f:
    f.write('\n'.join(lines))
