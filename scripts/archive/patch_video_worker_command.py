import re

with open('app/src/main/java/com/example/core/work/VideoExportWorker.kt', 'r') as f:
    content = f.read()

replacement = """                    val command = FFmpegCommandBuilder.buildTrimmedVideoAudioMasteringCommand(
                        inputPath = inputUriStr,
                        outputPath = outputFile.absolutePath,
                        presetQuality = inputData.getString(KEY_PRESET_QUALITY) ?: "1080p",
                        presetName = inputData.getString(KEY_PRESET_NAME) ?: "PUNCHY",
                        targetLufs = inputData.getDouble(KEY_TARGET_LUFS, -14.0),
                        videoFilter = ffmpegFilterStr,
                        resolution = resolution,
                        frameRate = frameRate,
                        bitrate = bitrate,
                        aspectRatio = aspectRatio
                    )"""

content = re.sub(
    r'                    val command = FFmpegCommandBuilder\.buildTrimmedVideoAudioMasteringCommand\([^)]+\)',
    replacement.strip('\n'),
    content
)

with open('app/src/main/java/com/example/core/work/VideoExportWorker.kt', 'w') as f:
    f.write(content)
