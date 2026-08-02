import re

with open('app/src/main/java/com/example/core/ffmpeg/MediaProcessor.kt', 'r') as f:
    content = f.read()

replacement = """        if (filterComplexStr.isNotBlank()) {
            val maxDim = when (resolution) {
                "4K" -> "3840"
                "1080p" -> "1920"
                "720p" -> "1280"
                "480p" -> "854"
                else -> "1920"
            }
            
            val scaleFilter = when (aspectRatio) {
                "16:9" -> "crop=min(iw\\\\,ih*16/9):min(ih\\\\,iw*9/16),scale=w=$maxDim:h=-2"
                "9:16" -> "crop=min(iw\\\\,ih*9/16):min(ih\\\\,iw*16/9),scale=w=-2:h=$maxDim"
                "1:1" -> "crop=min(iw\\\\,ih):min(iw\\\\,ih),scale=w=$maxDim:h=$maxDim"
                "4:5" -> "crop=min(iw\\\\,ih*4/5):min(ih\\\\,iw*5/4),scale=w=-2:h=$maxDim"
                else -> "scale=w=$maxDim:h=$maxDim:force_original_aspect_ratio=decrease"
            }
            
            filterComplexStr += ",$scaleFilter"
            commandList.add("-filter_complex")
            commandList.add(filterComplexStr)
        } else {
            val maxDim = when (resolution) {
                "4K" -> "3840"
                "1080p" -> "1920"
                "720p" -> "1280"
                "480p" -> "854"
                else -> "1920"
            }
            val scaleFilter = when (aspectRatio) {
                "16:9" -> "crop=min(iw\\\\,ih*16/9):min(ih\\\\,iw*9/16),scale=w=$maxDim:h=-2"
                "9:16" -> "crop=min(iw\\\\,ih*9/16):min(ih\\\\,iw*16/9),scale=w=-2:h=$maxDim"
                "1:1" -> "crop=min(iw\\\\,ih):min(iw\\\\,ih),scale=w=$maxDim:h=$maxDim"
                "4:5" -> "crop=min(iw\\\\,ih*4/5):min(ih\\\\,iw*5/4),scale=w=-2:h=$maxDim"
                else -> "scale=w=$maxDim:h=$maxDim:force_original_aspect_ratio=decrease"
            }
            val finalFilter = if (!filterString.isNullOrBlank()) "$filterString,$scaleFilter" else scaleFilter
            commandList.add("-vf")
            commandList.add(finalFilter)
        }"""

content = re.sub(
    r'        if \(filterComplexStr.isNotBlank\(\)\) \{.*?commandList.add\(finalFilter\)\n        \}',
    replacement,
    content,
    flags=re.DOTALL
)

with open('app/src/main/java/com/example/core/ffmpeg/MediaProcessor.kt', 'w') as f:
    f.write(content)

