package com.example.core.ui

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap

/**
 * Represents a media file (video or audio) selected from storage via Activity Result API.
 */
data class SelectedMediaFile(
    val uri: Uri,
    val fileName: String,
    val fileSize: Long,
    val mimeType: String,
    val isVideo: Boolean
) {
    val fileSizeFormatted: String
        get() {
            if (fileSize <= 0) return "0 B"
            val units = arrayOf("B", "KB", "MB", "GB")
            val digitGroups = (Math.log10(fileSize.toDouble()) / Math.log10(1024.0)).toInt()
            val index = digitGroups.coerceIn(0, units.lastIndex)
            return String.format("%.1f %s", fileSize / Math.pow(1024.0, index.toDouble()), units[index])
        }

    companion object {
        /**
         * Resolves metadata from ContentResolver for a given Uri.
         */
        fun fromUri(context: Context, uri: Uri): SelectedMediaFile {
            val contentResolver = context.contentResolver
            var name = "Selected_Media"
            var size = 0L

            contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (cursor.moveToFirst()) {
                    if (nameIndex != -1) {
                        name = cursor.getString(nameIndex) ?: "Selected_Media"
                    }
                    if (sizeIndex != -1) {
                        size = cursor.getLong(sizeIndex)
                    }
                }
            }

            var type = contentResolver.getType(uri) ?: ""
            if (type.isBlank()) {
                val extension = MimeTypeMap.getFileExtensionFromUrl(uri.toString())
                if (extension != null) {
                    type = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension.lowercase()) ?: ""
                }
            }

            val isVideo = type.startsWith("video", ignoreCase = true) || name.endsWith(".mp4", true) || name.endsWith(".mkv", true) || name.endsWith(".webm", true)

            return SelectedMediaFile(
                uri = uri,
                fileName = name,
                fileSize = size,
                mimeType = type.ifBlank { if (isVideo) "video/*" else "audio/*" },
                isVideo = isVideo
            )
        }
    }

    /**
     * Safely attempts to take persistable URI permission for persistent access across app restarts.
     */
    fun takePersistablePermission(context: Context) {
        try {
            val flags = android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
            context.contentResolver.takePersistableUriPermission(uri, flags)
        } catch (_: SecurityException) {
            // Ignored if the URI grant isn't persistable by the source provider
        } catch (_: Exception) {
            // Fallback for other platform variances
        }
    }

    fun hasPersistedReadPermission(context: Context): Boolean =
        context.contentResolver.persistedUriPermissions.any { permission ->
            permission.uri == uri && permission.isReadPermission
        }
}
