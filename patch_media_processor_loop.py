import re

with open('app/src/main/java/com/example/core/ffmpeg/MediaProcessor.kt', 'r') as f:
    content = f.read()

replacement = """                FFmpegCommandBuilder.buildNormalLoopCommand(
                    inputPath = actualInputUri,
                    outputPath = outputFile.absolutePath,
                    loopCount = (targetDurationSec / 10).toInt().coerceAtLeast(1),
                    presetQuality = presetQuality,
                    resolution = resolution,
                    frameRate = frameRate,
                    bitrate = bitrate,
                    aspectRatio = aspectRatio
                )"""

content = re.sub(
    r'                FFmpegCommandBuilder.buildNormalLoopCommand\(actualInputUri, outputFile.absolutePath, \(targetDurationSec / 10\).toInt\(\).coerceAtLeast\(1\), presetQuality\)',
    replacement,
    content
)

with open('app/src/main/java/com/example/core/ffmpeg/MediaProcessor.kt', 'w') as f:
    f.write(content)
