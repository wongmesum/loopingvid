import re

with open('app/src/main/java/com/example/core/ffmpeg/FFmpegWrapper.kt', 'r') as f:
    content = f.read()

metadata_logic = """
        val metadataArgs = mutableListOf<String>()
        audioMetadata?.let { meta ->
            if (meta.title.isNotBlank()) {
                metadataArgs.add("-metadata")
                metadataArgs.add("title=${meta.title}")
            }
            if (meta.artist.isNotBlank()) {
                metadataArgs.add("-metadata")
                metadataArgs.add("artist=${meta.artist}")
            }
            if (meta.album.isNotBlank()) {
                metadataArgs.add("-metadata")
                metadataArgs.add("album=${meta.album}")
            }
            if (meta.genre.isNotBlank()) {
                metadataArgs.add("-metadata")
                metadataArgs.add("genre=${meta.genre}")
            }
            if (meta.year.isNotBlank()) {
                metadataArgs.add("-metadata")
                metadataArgs.add("date=${meta.year}")
            }
            if (meta.comment.isNotBlank()) {
                metadataArgs.add("-metadata")
                metadataArgs.add("comment=${meta.comment}")
            }
        }

        val baseCommand = mutableListOf(
            "-i", inputPath,
            "-af", loudnormFilter,
            "-c:a", if (audioFormat.equals("wav", ignoreCase = true)) "pcm_s16le" else "libmp3lame",
            "-b:a", "320k"
        )
        baseCommand.addAll(metadataArgs)
        baseCommand.add("-y")
        baseCommand.add(outputPath)
        
        return baseCommand
"""

pattern = r'return listOf\(\s*"-i", inputPath,\s*"-af", loudnormFilter,\s*"-c:a", if \(audioFormat\.equals\("wav", ignoreCase = true\)\) "pcm_s16le" else "libmp3lame",\s*"-b:a", "320k",\s*"-y", outputPath\s*\)'

content = re.sub(pattern, metadata_logic.strip(), content)

with open('app/src/main/java/com/example/core/ffmpeg/FFmpegWrapper.kt', 'w') as f:
    f.write(content)

