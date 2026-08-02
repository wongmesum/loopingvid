package com.example.core.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext

/**
 * State holder managing file picker Activity Result API requests for video and audio files.
 */
@Stable
class MediaPickerHelper(
    private val onSingleFileSelected: (SelectedMediaFile) -> Unit,
    private val onMultipleFilesSelected: (List<SelectedMediaFile>) -> Unit,
    private val onError: (String) -> Unit,
    private val launchSingleContract: (String) -> Unit,
    private val launchMultipleContract: (Array<String>) -> Unit,
    private val launchDocumentContract: (Array<String>) -> Unit
) {
    var isPicking by mutableStateOf(false)
        private set

    var lastSelectedFile by mutableStateOf<SelectedMediaFile?>(null)
        private set

    var selectedFilesList by mutableStateOf<List<SelectedMediaFile>>(emptyList())
        private set

    /**
     * Opens system picker to select a video file.
     */
    fun pickVideo() {
        isPicking = true
        try {
            launchSingleContract("video/*")
        } catch (e: Exception) {
            isPicking = false
            onError("Failed to launch video picker: ${e.localizedMessage}")
        }
    }

    /**
     * Opens system picker to select an audio file.
     */
    fun pickAudio() {
        isPicking = true
        try {
            launchSingleContract("audio/*")
        } catch (e: Exception) {
            isPicking = false
            onError("Failed to launch audio picker: ${e.localizedMessage}")
        }
    }

    /**
     * Opens system picker to select multiple video files.
     */
    fun pickMultipleVideos() {
        isPicking = true
        try {
            launchMultipleContract(arrayOf("video/*"))
        } catch (e: Exception) {
            isPicking = false
            onError("Failed to launch multiple video picker: ${e.localizedMessage}")
        }
    }

    /**
     * Opens system picker to select multiple audio files.
     */
    fun pickMultipleAudio() {
        isPicking = true
        try {
            launchMultipleContract(arrayOf("audio/*"))
        } catch (e: Exception) {
            isPicking = false
            onError("Failed to launch multiple audio picker: ${e.localizedMessage}")
        }
    }

    /**
     * Opens Storage Access Framework document picker for media files.
     */
    fun pickMediaDocument(mimeTypes: Array<String> = arrayOf("video/*", "audio/*")) {
        isPicking = true
        try {
            launchDocumentContract(mimeTypes)
        } catch (e: Exception) {
            isPicking = false
            onError("Failed to launch document picker: ${e.localizedMessage}")
        }
    }

    internal fun handleSingleResult(mediaFile: SelectedMediaFile?) {
        isPicking = false
        if (mediaFile != null) {
            lastSelectedFile = mediaFile
            selectedFilesList = listOf(mediaFile)
            onSingleFileSelected(mediaFile)
        }
    }

    internal fun handleMultipleResult(mediaFiles: List<SelectedMediaFile>) {
        isPicking = false
        if (mediaFiles.isNotEmpty()) {
            selectedFilesList = mediaFiles
            lastSelectedFile = mediaFiles.lastOrNull()
            onMultipleFilesSelected(mediaFiles)
        }
    }
}

/**
 * Creates and remembers a [MediaPickerHelper] using Jetpack Compose Activity Result API contracts.
 *
 * @param onSingleFileSelected Callback invoked when a single video/audio file is selected.
 * @param onMultipleFilesSelected Callback invoked when multiple media files are selected.
 * @param onError Callback invoked if file picker launch fails.
 */
@Composable
fun rememberMediaPickerHelper(
    onSingleFileSelected: (SelectedMediaFile) -> Unit = {},
    onMultipleFilesSelected: (List<SelectedMediaFile>) -> Unit = {},
    onError: (String) -> Unit = {}
): MediaPickerHelper {
    val context = LocalContext.current
    val currentOnSingleFileSelected by rememberUpdatedState(onSingleFileSelected)
    val currentOnMultipleFilesSelected by rememberUpdatedState(onMultipleFilesSelected)
    val currentOnError by rememberUpdatedState(onError)

    var helperInstance: MediaPickerHelper? = null

    val singleContentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            val file = SelectedMediaFile.fromUri(context, uri)
            file.takePersistablePermission(context)
            helperInstance?.handleSingleResult(file)
        } else {
            helperInstance?.handleSingleResult(null)
        }
    }

    val multipleContentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris: List<Uri> ->
        val files = uris.map { uri ->
            val file = SelectedMediaFile.fromUri(context, uri)
            file.takePersistablePermission(context)
            file
        }
        helperInstance?.handleMultipleResult(files)
    }

    val openDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            val file = SelectedMediaFile.fromUri(context, uri)
            file.takePersistablePermission(context)
            helperInstance?.handleSingleResult(file)
        } else {
            helperInstance?.handleSingleResult(null)
        }
    }

    val helper = remember {
        MediaPickerHelper(
            onSingleFileSelected = { currentOnSingleFileSelected(it) },
            onMultipleFilesSelected = { currentOnMultipleFilesSelected(it) },
            onError = { currentOnError(it) },
            launchSingleContract = { mimeType -> singleContentLauncher.launch(mimeType) },
            launchMultipleContract = { mimeTypes -> multipleContentLauncher.launch(mimeTypes.firstOrNull() ?: "*/*") },
            launchDocumentContract = { mimeTypes -> openDocumentLauncher.launch(mimeTypes) }
        )
    }

    helperInstance = helper
    return helper
}
