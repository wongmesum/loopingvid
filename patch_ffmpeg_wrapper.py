import re

with open('app/src/main/java/com/example/core/ffmpeg/FFmpegWrapper.kt', 'r') as f:
    content = f.read()

get_args = """    private fun getVideoExportArgs(resolution: String, frameRate: String, bitrate: String, aspectRatio: String = "Asli"): List<String> {
        val args = mutableListOf<String>()
        val fps = frameRate.replace("fps", "")
        if (fps.isNotEmpty()) {
            args.addAll(listOf("-r", fps))
        }

        val maxDim = when (resolution) {
            "4K" -> "3840"
            "1080p" -> "1920"
            "720p" -> "1280"
            "480p" -> "854"
            else -> "1920"
        }
        
        val scaleFilter = when (aspectRatio) {
            "16:9" -> "crop=min(iw\\,ih*16/9):min(ih\\,iw*9/16),scale=w=$maxDim:h=-2"
            "9:16" -> "crop=min(iw\\,ih*9/16):min(ih\\,iw*16/9),scale=w=-2:h=$maxDim"
            "1:1" -> "crop=min(iw\\,ih):min(iw\\,ih),scale=w=$maxDim:h=$maxDim"
            "4:5" -> "crop=min(iw\\,ih*4/5):min(ih\\,iw*5/4),scale=w=-2:h=$maxDim"
            else -> "scale=w=$maxDim:h=$maxDim:force_original_aspect_ratio=decrease"
        }
        
        args.addAll(listOf("-vf", scaleFilter))
        
        val bv = when (bitrate) {
            "Low" -> "1M"
            "Medium" -> "4M"
            "High" -> "8M"
            else -> "4M"
        }
        args.addAll(listOf("-b:v", bv, "-maxrate", bv, "-bufsize", "${bv.replace("M", "")}M"))
        
        return args
    }"""

content = re.sub(
    r'    private fun getVideoExportArgs.*?return args\n    \}',
    get_args,
    content,
    flags=re.DOTALL
)

# update buildNormalLoopCommand
content = content.replace(
    '        bitrate: String = "Medium"\n    ): List<String> {',
    '        bitrate: String = "Medium",\n        aspectRatio: String = "Asli"\n    ): List<String> {'
)
content = content.replace(
    'args.addAll(getVideoExportArgs(resolution, frameRate, bitrate))',
    'args.addAll(getVideoExportArgs(resolution, frameRate, bitrate, aspectRatio))'
)

# update buildCrossfadeLoopCommand
content = content.replace(
    '        bitrate: String = "Medium"\n    ): List<String> {',
    '        bitrate: String = "Medium",\n        aspectRatio: String = "Asli"\n    ): List<String> {'
)

# inside buildCrossfadeLoopCommand it does:
# val filterGraph = "xfade=transition=fade:duration=$crossfadeDurationSec:offset=${(durationSec - crossfadeDurationSec).coerceAtLeast(0.1)},scale=w=$maxDim:h=$maxDim:force_original_aspect_ratio=decrease"
# let's update it to use aspectRatio
crossfade = """        val maxDim = when (resolution) {
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
        
        val filterGraph = "xfade=transition=fade:duration=$crossfadeDurationSec:offset=${(durationSec - crossfadeDurationSec).coerceAtLeast(0.1)},$scaleFilter"
"""

content = re.sub(
    r'        val maxDim = when \(resolution\).*?force_original_aspect_ratio=decrease"',
    crossfade.strip('\n'),
    content,
    flags=re.DOTALL
)

# update buildTrimmedVideoAudioMasteringCommand
content = content.replace(
    '        frameRate: String = "30fps",\n        bitrate: String = "Medium"\n    ): List<String> {',
    '        frameRate: String = "30fps",\n        bitrate: String = "Medium",\n        aspectRatio: String = "Asli"\n    ): List<String> {'
)
content = content.replace(
    'args.addAll(getVideoExportArgs(resolution, frameRate, bitrate))',
    'args.addAll(getVideoExportArgs(resolution, frameRate, bitrate, aspectRatio))'
)

with open('app/src/main/java/com/example/core/ffmpeg/FFmpegWrapper.kt', 'w') as f:
    f.write(content)
