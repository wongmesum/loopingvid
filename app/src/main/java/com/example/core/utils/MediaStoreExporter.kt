package com.example.core.utils

import android.content.ContentValues
import android.content.Context
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * MediaStoreExporter provides functions to export edited video and audio projects
 * directly to the device media gallery using Android's MediaStore API.
 */
object MediaStoreExporter {

    /**
     * Specifically exports finalized, audio-mastered, and trimmed video segments
     * directly to the user's local device gallery (Movies/MasteredVideos) with full MediaStore metadata.
     *
     * @param context Application context
     * @param videoFile File object of the finalized, trimmed & audio-mastered video
     * @param title Title or display name for the exported video
     * @param mimeType Video MIME type (e.g. "video/mp4", "video/x-matroska", "video/quicktime")
     * @param durationMs Optional duration in milliseconds of the trimmed segment
     * @param relativeFolder Relative path in public storage (defaults to "Movies/MasteredVideos")
     * @return Result containing the content Uri of the saved video in the device gallery
     */
    suspend fun exportFinalizedAudioMasteredVideoToGallery(
        context: Context,
        videoFile: File,
        title: String? = null,
        mimeType: String = "video/mp4",
        durationMs: Long? = null,
        relativeFolder: String = "Movies/MasteredVideos"
    ): Result<Uri> = withContext(Dispatchers.IO) {
        try {
            if (!videoFile.exists() || videoFile.length() <= 0) {
                return@withContext Result.failure(IllegalArgumentException("Source video file does not exist or is empty."))
            }

            val displayName = title?.takeIf { it.isNotBlank() } ?: videoFile.nameWithoutExtension
            val extension = videoFile.extension.ifBlank { "mp4" }
            val fullFileName = if (displayName.endsWith(".$extension", ignoreCase = true)) {
                displayName
            } else {
                "$displayName.$extension"
            }

            val contentResolver = context.contentResolver

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val contentValues = ContentValues().apply {
                    put(MediaStore.Video.Media.DISPLAY_NAME, fullFileName)
                    put(MediaStore.Video.Media.MIME_TYPE, mimeType)
                    put(MediaStore.Video.Media.TITLE, displayName)
                    put(MediaStore.Video.Media.DATE_ADDED, System.currentTimeMillis() / 1000)
                    put(MediaStore.Video.Media.DATE_TAKEN, System.currentTimeMillis())
                    put(MediaStore.Video.Media.RELATIVE_PATH, relativeFolder)
                    if (durationMs != null && durationMs > 0) {
                        put(MediaStore.Video.Media.DURATION, durationMs)
                    }
                    put(MediaStore.Video.Media.IS_PENDING, 1)
                }

                val collection = MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
                val itemUri = contentResolver.insert(collection, contentValues)
                    ?: return@withContext Result.failure(IllegalStateException("Failed to create MediaStore entry."))

                try {
                    contentResolver.openOutputStream(itemUri)?.use { outputStream ->
                        videoFile.inputStream().use { inputStream ->
                            inputStream.copyTo(outputStream)
                        }
                    } ?: return@withContext Result.failure(IllegalStateException("Failed to open MediaStore output stream."))

                    contentValues.clear()
                    contentValues.put(MediaStore.Video.Media.IS_PENDING, 0)
                    contentResolver.update(itemUri, contentValues, null, null)

                    Result.success(itemUri)
                } catch (e: Exception) {
                    contentResolver.delete(itemUri, null, null)
                    Result.failure(e)
                }
            } else {
                val publicMoviesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES)
                val targetDir = File(publicMoviesDir, relativeFolder.removePrefix("Movies/"))
                if (!targetDir.exists()) {
                    targetDir.mkdirs()
                }

                val targetFile = File(targetDir, fullFileName)
                videoFile.copyTo(targetFile, overwrite = true)

                var scannedUri: Uri? = null
                MediaScannerConnection.scanFile(
                    context,
                    arrayOf(targetFile.absolutePath),
                    arrayOf(mimeType)
                ) { _, uri ->
                    scannedUri = uri
                }

                Result.success(scannedUri ?: Uri.fromFile(targetFile))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Exports a video project file to the device public media gallery (Movies folder) via MediaStore API.
     *
     * @param context Application context
     * @param videoFile File object of the rendered video project
     * @param title Title or display name for the exported video
     * @param mimeType Video MIME type (e.g. "video/mp4", "video/x-matroska", "video/quicktime")
     * @param relativeFolder Relative path in public storage (defaults to "Movies/LoopingVid")
     * @return Result containing the content Uri of the exported video in MediaStore
     */
    suspend fun exportVideoToGallery(
        context: Context,
        videoFile: File,
        title: String? = null,
        mimeType: String = "video/mp4",
        relativeFolder: String = "Movies/LoopingVid"
    ): Result<Uri> = withContext(Dispatchers.IO) {
        try {
            if (!videoFile.exists() || videoFile.length() <= 0) {
                return@withContext Result.failure(IllegalArgumentException("Source video file does not exist or is empty."))
            }

            val displayName = title?.takeIf { it.isNotBlank() } ?: videoFile.nameWithoutExtension
            val extension = videoFile.extension.ifBlank { "mp4" }
            val fullFileName = if (displayName.endsWith(".$extension", ignoreCase = true)) {
                displayName
            } else {
                "$displayName.$extension"
            }

            val contentResolver = context.contentResolver

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val contentValues = ContentValues().apply {
                    put(MediaStore.Video.Media.DISPLAY_NAME, fullFileName)
                    put(MediaStore.Video.Media.MIME_TYPE, mimeType)
                    put(MediaStore.Video.Media.TITLE, displayName)
                    put(MediaStore.Video.Media.DATE_ADDED, System.currentTimeMillis() / 1000)
                    put(MediaStore.Video.Media.DATE_TAKEN, System.currentTimeMillis())
                    put(MediaStore.Video.Media.RELATIVE_PATH, relativeFolder)
                    put(MediaStore.Video.Media.IS_PENDING, 1)
                }

                val collection = MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
                val itemUri = contentResolver.insert(collection, contentValues)
                    ?: return@withContext Result.failure(IllegalStateException("Failed to create MediaStore entry."))

                try {
                    contentResolver.openOutputStream(itemUri)?.use { outputStream ->
                        videoFile.inputStream().use { inputStream ->
                            inputStream.copyTo(outputStream)
                        }
                    } ?: return@withContext Result.failure(IllegalStateException("Failed to open MediaStore output stream."))

                    contentValues.clear()
                    contentValues.put(MediaStore.Video.Media.IS_PENDING, 0)
                    contentResolver.update(itemUri, contentValues, null, null)

                    Result.success(itemUri)
                } catch (e: Exception) {
                    contentResolver.delete(itemUri, null, null)
                    Result.failure(e)
                }
            } else {
                val publicMoviesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES)
                val targetDir = File(publicMoviesDir, relativeFolder.removePrefix("Movies/"))
                if (!targetDir.exists()) {
                    targetDir.mkdirs()
                }

                val targetFile = File(targetDir, fullFileName)
                videoFile.copyTo(targetFile, overwrite = true)

                var scannedUri: Uri? = null
                MediaScannerConnection.scanFile(
                    context,
                    arrayOf(targetFile.absolutePath),
                    arrayOf(mimeType)
                ) { _, uri ->
                    scannedUri = uri
                }

                Result.success(scannedUri ?: Uri.fromFile(targetFile))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Exports an audio project file to the device public music library via MediaStore API.
     *
     * @param context Application context
     * @param audioFile File object of the rendered audio project
     * @param title Title or display name for the exported audio
     * @param mimeType Audio MIME type (e.g. "audio/mpeg", "audio/wav", "audio/mp4")
     * @param relativeFolder Relative path in public storage (defaults to "Music/LoopingVid")
     * @return Result containing the content Uri of the exported audio in MediaStore
     */
    suspend fun exportAudioToGallery(
        context: Context,
        audioFile: File,
        title: String? = null,
        mimeType: String = "audio/mpeg",
        relativeFolder: String = "Music/LoopingVid"
    ): Result<Uri> = withContext(Dispatchers.IO) {
        try {
            if (!audioFile.exists() || audioFile.length() <= 0) {
                return@withContext Result.failure(IllegalArgumentException("Source audio file does not exist or is empty."))
            }

            val displayName = title?.takeIf { it.isNotBlank() } ?: audioFile.nameWithoutExtension
            val extension = audioFile.extension.ifBlank { "mp3" }
            val fullFileName = if (displayName.endsWith(".$extension", ignoreCase = true)) {
                displayName
            } else {
                "$displayName.$extension"
            }

            val contentResolver = context.contentResolver

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val contentValues = ContentValues().apply {
                    put(MediaStore.Audio.Media.DISPLAY_NAME, fullFileName)
                    put(MediaStore.Audio.Media.MIME_TYPE, mimeType)
                    put(MediaStore.Audio.Media.TITLE, displayName)
                    put(MediaStore.Audio.Media.DATE_ADDED, System.currentTimeMillis() / 1000)
                    put(MediaStore.Audio.Media.RELATIVE_PATH, relativeFolder)
                    put(MediaStore.Audio.Media.IS_PENDING, 1)
                }

                val collection = MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
                val itemUri = contentResolver.insert(collection, contentValues)
                    ?: return@withContext Result.failure(IllegalStateException("Failed to create MediaStore entry for audio."))

                try {
                    contentResolver.openOutputStream(itemUri)?.use { outputStream ->
                        audioFile.inputStream().use { inputStream ->
                            inputStream.copyTo(outputStream)
                        }
                    } ?: return@withContext Result.failure(IllegalStateException("Failed to open MediaStore output stream."))

                    contentValues.clear()
                    contentValues.put(MediaStore.Audio.Media.IS_PENDING, 0)
                    contentResolver.update(itemUri, contentValues, null, null)

                    Result.success(itemUri)
                } catch (e: Exception) {
                    contentResolver.delete(itemUri, null, null)
                    Result.failure(e)
                }
            } else {
                val publicMusicDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC)
                val targetDir = File(publicMusicDir, relativeFolder.removePrefix("Music/"))
                if (!targetDir.exists()) targetDir.mkdirs()

                val targetFile = File(targetDir, fullFileName)
                audioFile.copyTo(targetFile, overwrite = true)

                var scannedUri: Uri? = null
                MediaScannerConnection.scanFile(
                    context,
                    arrayOf(targetFile.absolutePath),
                    arrayOf(mimeType)
                ) { _, uri -> scannedUri = uri }

                Result.success(scannedUri ?: Uri.fromFile(targetFile))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
