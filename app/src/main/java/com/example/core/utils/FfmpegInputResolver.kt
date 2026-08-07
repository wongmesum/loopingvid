package com.example.core.utils

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/** Resolves Android Uri inputs to filesystem paths FFmpeg can read. */
object FfmpegInputResolver {

    suspend fun resolve(context: Context, source: String): ResolvedInput = withContext(Dispatchers.IO) {
        val uri = Uri.parse(source)
        when (uri.scheme) {
            null, "file" -> ResolvedInput(path = uri.path ?: source)
            "content" -> copyContentUriToCache(context, uri)
            else -> ResolvedInput(path = source)
        }
    }

    private fun copyContentUriToCache(context: Context, uri: Uri): ResolvedInput {
        val extension = resolveExtension(context, uri)
        val inputDir = File(context.cacheDir, "ffmpeg-inputs").apply { mkdirs() }
        val target = File.createTempFile("ffmpeg_input_", ".$extension", inputDir)

        context.contentResolver.openInputStream(uri)?.use { input ->
            target.outputStream().use { output -> input.copyTo(output) }
        } ?: error("Cannot open media source")

        return ResolvedInput(path = target.absolutePath, temporaryFile = target)
    }

    private fun resolveExtension(context: Context, uri: Uri): String {
        val displayName = queryDisplayName(context, uri)
        val fromName = displayName?.substringAfterLast('.', missingDelimiterValue = "")
            ?.lowercase()
            ?.takeIf { it.length in 2..5 }
        if (fromName != null) return fromName

        val mimeType = context.contentResolver.getType(uri)
        return when (mimeType) {
            "audio/mpeg" -> "mp3"
            "audio/mp4", "audio/aac" -> "m4a"
            "audio/wav", "audio/x-wav" -> "wav"
            "audio/ogg" -> "ogg"
            "audio/flac" -> "flac"
            "image/jpeg" -> "jpg"
            "image/png" -> "png"
            "image/webp" -> "webp"
            "image/heic", "image/heif" -> "heic"
            "video/mp4" -> "mp4"
            // FFmpeg probes content rather than trusting the extension, so an
            // unknown type still decodes; the suffix only aids demuxer guessing.
            else -> "bin"
        }
    }

    private fun queryDisplayName(context: Context, uri: Uri): String? {
        return context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
            ?.use { cursor ->
                if (!cursor.moveToFirst()) return@use null
                val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (index < 0) null else cursor.getString(index)
            }
    }
}

data class ResolvedInput(
    val path: String,
    val temporaryFile: File? = null
)
