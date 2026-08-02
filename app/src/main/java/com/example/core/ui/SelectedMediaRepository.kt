package com.example.core.ui

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Repository interface and implementation for managing selected video and audio file URIs from device storage.
 */
interface SelectedMediaRepository {
    val selectedVideo: StateFlow<SelectedMediaFile?>
    val selectedAudio: StateFlow<SelectedMediaFile?>
    val selectedMediaList: StateFlow<List<SelectedMediaFile>>

    fun selectVideo(context: Context, uri: Uri): SelectedMediaFile
    fun selectAudio(context: Context, uri: Uri): SelectedMediaFile
    fun setSelectedVideo(mediaFile: SelectedMediaFile)
    fun setSelectedAudio(mediaFile: SelectedMediaFile)
    fun setMediaList(mediaFiles: List<SelectedMediaFile>)
    fun clearSelection()
}

class SelectedMediaRepositoryImpl : SelectedMediaRepository {
    private val _selectedVideo = MutableStateFlow<SelectedMediaFile?>(null)
    override val selectedVideo: StateFlow<SelectedMediaFile?> = _selectedVideo.asStateFlow()

    private val _selectedAudio = MutableStateFlow<SelectedMediaFile?>(null)
    override val selectedAudio: StateFlow<SelectedMediaFile?> = _selectedAudio.asStateFlow()

    private val _selectedMediaList = MutableStateFlow<List<SelectedMediaFile>>(emptyList())
    override val selectedMediaList: StateFlow<List<SelectedMediaFile>> = _selectedMediaList.asStateFlow()

    override fun selectVideo(context: Context, uri: Uri): SelectedMediaFile {
        val mediaFile = SelectedMediaFile.fromUri(context, uri)
        mediaFile.takePersistablePermission(context)
        _selectedVideo.value = mediaFile
        return mediaFile
    }

    override fun selectAudio(context: Context, uri: Uri): SelectedMediaFile {
        val mediaFile = SelectedMediaFile.fromUri(context, uri)
        mediaFile.takePersistablePermission(context)
        _selectedAudio.value = mediaFile
        return mediaFile
    }

    override fun setSelectedVideo(mediaFile: SelectedMediaFile) {
        _selectedVideo.value = mediaFile
    }

    override fun setSelectedAudio(mediaFile: SelectedMediaFile) {
        _selectedAudio.value = mediaFile
    }

    override fun setMediaList(mediaFiles: List<SelectedMediaFile>) {
        _selectedMediaList.value = mediaFiles
    }

    override fun clearSelection() {
        _selectedVideo.value = null
        _selectedAudio.value = null
        _selectedMediaList.value = emptyList()
    }
}
