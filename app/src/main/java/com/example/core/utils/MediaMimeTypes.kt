package com.example.core.utils

import java.io.File
import java.util.Locale

/**
 * Maps output container extensions to the MIME types MediaStore should advertise.
 *
 * The gallery trusts whatever MIME type is written into the MediaStore row, so a wrong
 * label (for example a WAV published as "audio/mpeg") makes some players refuse the file.
 */
object MediaMimeTypes {

    const val DEFAULT_AUDIO_MIME_TYPE = "audio/mpeg"
    const val DEFAULT_VIDEO_MIME_TYPE = "video/mp4"

    private val AUDIO_MIME_TYPES = mapOf(
        "mp3" to "audio/mpeg",
        "wav" to "audio/wav",
        "m4a" to "audio/mp4",
        "aac" to "audio/aac",
        "flac" to "audio/flac",
        "ogg" to "audio/ogg",
        "opus" to "audio/opus"
    )

    private val VIDEO_MIME_TYPES = mapOf(
        "mp4" to "video/mp4",
        "mkv" to "video/x-matroska",
        "mov" to "video/quicktime",
        "webm" to "video/webm",
        "3gp" to "video/3gpp"
    )

    fun resolveAudioMimeType(extension: String): String =
        AUDIO_MIME_TYPES[normalize(extension)] ?: DEFAULT_AUDIO_MIME_TYPE

    fun resolveVideoMimeType(extension: String): String =
        VIDEO_MIME_TYPES[normalize(extension)] ?: DEFAULT_VIDEO_MIME_TYPE

    fun resolveAudioMimeTypeForFile(fileName: String): String =
        resolveAudioMimeType(File(fileName).extension)

    fun resolveVideoMimeTypeForFile(fileName: String): String =
        resolveVideoMimeType(File(fileName).extension)

    private fun normalize(extension: String): String =
        extension.trim().removePrefix(".").lowercase(Locale.US)
}
