package com.example.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.database.LoopingVidRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SettingsUiState(
    val outputDirectoryPath: String = "/storage/emulated/0/Movies/LoopingVid",
    val highQualityPreview: Boolean = true,
    val youtubeStreamKey: String = "",
    val tiktokStreamKey: String = "",
    val geminiApiKey: String = "",
    val firestoreSyncEnabled: Boolean = true
)

class SettingsViewModel(
    private val repository: LoopingVidRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val dir = repository.getSettingValue("output_dir") ?: _uiState.value.outputDirectoryPath
            val ytKey = repository.getSettingValue("stream_key_youtube") ?: ""
            val ttKey = repository.getSettingValue("stream_key_tiktok") ?: ""
            val geminiKey = repository.getSettingValue("gemini_api_key") ?: ""
            val fsSync = repository.getSettingValue("firestore_sync_enabled")?.toBooleanStrictOrNull() ?: true

            _uiState.value = _uiState.value.copy(
                outputDirectoryPath = dir,
                youtubeStreamKey = ytKey,
                tiktokStreamKey = ttKey,
                geminiApiKey = geminiKey,
                firestoreSyncEnabled = fsSync
            )
        }
    }

    fun updateOutputDirectory(path: String) {
        _uiState.value = _uiState.value.copy(outputDirectoryPath = path)
        viewModelScope.launch {
            repository.setSetting("output_dir", path)
        }
    }

    fun toggleHighQualityPreview(enable: Boolean) {
        _uiState.value = _uiState.value.copy(highQualityPreview = enable)
    }

    fun updateYoutubeKey(key: String) {
        _uiState.value = _uiState.value.copy(youtubeStreamKey = key)
        viewModelScope.launch {
            repository.setSetting("stream_key_youtube", key)
        }
    }

    fun updateTiktokKey(key: String) {
        _uiState.value = _uiState.value.copy(tiktokStreamKey = key)
        viewModelScope.launch {
            repository.setSetting("stream_key_tiktok", key)
        }
    }

    fun updateGeminiKey(key: String) {
        _uiState.value = _uiState.value.copy(geminiApiKey = key)
        viewModelScope.launch {
            repository.setSetting("gemini_api_key", key)
        }
    }

    fun toggleFirestoreSync(enable: Boolean) {
        _uiState.value = _uiState.value.copy(firestoreSyncEnabled = enable)
        viewModelScope.launch {
            repository.setSetting("firestore_sync_enabled", enable.toString())
            repository.firestoreSyncManager?.setAutoSyncEnabled(enable)
        }
    }
}
