import re

with open('app/src/main/java/com/example/core/ffmpeg/FFmpegWrapper.kt', 'r') as f:
    content = f.read()

# Removing the erroneous filterGraph from buildTrimmedVideoAudioMasteringCommand
replacement = """        val scaleFilter = when (aspectRatio) {
            "16:9" -> "crop=min(iw\\\\,ih*16/9):min(ih\\\\,iw*9/16),scale=w=$maxDim:h=-2"
            "9:16" -> "crop=min(iw\\\\,ih*9/16):min(ih\\\\,iw*16/9),scale=w=-2:h=$maxDim"
            "1:1" -> "crop=min(iw\\\\,ih):min(iw\\\\,ih),scale=w=$maxDim:h=$maxDim"
            "4:5" -> "crop=min(iw\\\\,ih*4/5):min(ih\\\\,iw*5/4),scale=w=-2:h=$maxDim"
            else -> "scale=w=$maxDim:h=$maxDim:force_original_aspect_ratio=decrease"
        }
        val finalVideoFilter = if (!videoFilter.isNullOrBlank()) {"""

content = re.sub(
    r'        val scaleFilter = when \(aspectRatio\) \{.*?\} \n        val filterGraph = "xfade.*?coerceAtLeast\(0\.1\)\},\$scaleFilter"\n        val finalVideoFilter = if \(\!videoFilter\.isNullOrBlank\(\)\) \{',
    replacement,
    content,
    flags=re.DOTALL
)

with open('app/src/main/java/com/example/core/ffmpeg/FFmpegWrapper.kt', 'w') as f:
    f.write(content)

