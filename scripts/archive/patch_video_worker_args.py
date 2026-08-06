import re

with open('app/src/main/java/com/example/core/work/VideoExportWorker.kt', 'r') as f:
    content = f.read()

# I need to add the retrieval of the new arguments in VideoExportWorker.kt around doWork.
replacement = """        val ffmpegFilterStr = inputData.getString(KEY_FFMPEG_FILTER)
        val resolution = inputData.getString(KEY_RESOLUTION) ?: "1080p"
        val frameRate = inputData.getString(KEY_FRAME_RATE) ?: "30fps"
        val bitrate = inputData.getString(KEY_BITRATE) ?: "Medium"
        val aspectRatio = inputData.getString(KEY_ASPECT_RATIO) ?: "Asli"
"""

content = re.sub(
    r'        val ffmpegFilterStr = inputData.getString\(KEY_FFMPEG_FILTER\)',
    replacement.strip('\n'),
    content
)

with open('app/src/main/java/com/example/core/work/VideoExportWorker.kt', 'w') as f:
    f.write(content)
