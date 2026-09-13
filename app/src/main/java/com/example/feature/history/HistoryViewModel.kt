package com.example.feature.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.database.FirestoreJobHistorySyncManager
import com.example.core.database.FirestoreSyncInfo
import com.example.core.database.JobHistoryEntity
import com.example.core.database.LiveSessionEntity
import com.example.core.database.LoopingVidRepository
import com.example.core.database.RenderJobEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class HistoryUiState(
    val renderJobs: List<RenderJobEntity> = emptyList(),
    val jobHistoryLogs: List<JobHistoryEntity> = emptyList(),
    val liveSessions: List<LiveSessionEntity> = emptyList(),
    val selectedTab: Int = 0, // 0 = Render Jobs, 1 = Live Sessions, 2 = Execution Logs
    val selectedFilterType: String = "ALL", // "ALL", "LOOP", "MASTERING", "EDITOR"
    val syncInfo: FirestoreSyncInfo = FirestoreSyncInfo(),
    // Metadata editor dialog state: which job (if any) is currently being edited, the working
    // values, and whether an edit is in-flight (FFmpeg remux running).
    val metadataEditJob: RenderJobEntity? = null,
    val metadataEditValues: com.example.core.media.AudioMetadata = com.example.core.media.AudioMetadata(),
    val isMetadataEditSaving: Boolean = false,
    val isMetadataEditLoading: Boolean = false,
    val metadataEditResultMessage: String? = null
)

class HistoryViewModel(
    private val repository: LoopingVidRepository,
    private val firestoreSyncManager: FirestoreJobHistorySyncManager? = null,
    private val mediaProcessor: com.example.core.ffmpeg.MediaProcessor? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(HistoryUiState())
    val uiState: StateFlow<HistoryUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.allJobs.collect { jobs ->
                _uiState.value = _uiState.value.copy(renderJobs = jobs)
            }
        }
        viewModelScope.launch {
            repository.allHistory.collect { historyLogs ->
                _uiState.value = _uiState.value.copy(jobHistoryLogs = historyLogs)
            }
        }
        viewModelScope.launch {
            repository.allLiveSessions.collect { sessions ->
                _uiState.value = _uiState.value.copy(liveSessions = sessions)
            }
        }
        firestoreSyncManager?.let { manager ->
            viewModelScope.launch {
                manager.syncInfo.collect { info ->
                    _uiState.value = _uiState.value.copy(syncInfo = info)
                }
            }
        }
    }

    fun setTab(tabIndex: Int) {
        _uiState.value = _uiState.value.copy(selectedTab = tabIndex)
    }

    fun setFilterType(type: String) {
        _uiState.value = _uiState.value.copy(selectedFilterType = type)
    }

    fun triggerManualCloudSync() {
        firestoreSyncManager?.triggerManualSync(_uiState.value.renderJobs)
    }

    fun toggleAutoCloudSync() {
        firestoreSyncManager?.toggleAutoSync()
    }

    fun deleteJob(id: Long) {
        viewModelScope.launch {
            repository.deleteJobById(id)
        }
    }

    fun deleteLiveSession(id: Long) {
        viewModelScope.launch {
            repository.deleteLiveSessionById(id)
        }
    }

    fun deleteJobHistoryLog(id: Long) {
        viewModelScope.launch {
            repository.deleteJobHistoryById(id)
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            repository.clearAllJobs()
            repository.clearAllHistory()
        }
    }

    /**
     * Opens the metadata editor dialog for a completed render job, pre-filled with the tags
     * already embedded in the file (read via FFprobe) so the user sees the real current values
     * instead of a blank form they might accidentally overwrite.
     */
    fun openMetadataEditor(job: RenderJobEntity) {
        _uiState.value = _uiState.value.copy(
            metadataEditJob = job,
            metadataEditValues = com.example.core.media.AudioMetadata(),
            metadataEditResultMessage = null,
            isMetadataEditLoading = true
        )
        val processor = mediaProcessor ?: run {
            _uiState.value = _uiState.value.copy(isMetadataEditLoading = false)
            return
        }
        viewModelScope.launch {
            val existing = processor.readFileMetadata(job.outputUri)
            // Only apply if the user hasn't since closed/switched the dialog to a different job.
            if (_uiState.value.metadataEditJob?.id == job.id) {
                _uiState.value = _uiState.value.copy(
                    metadataEditValues = existing,
                    isMetadataEditLoading = false
                )
            }
        }
    }

    fun updateMetadataEditValues(metadata: com.example.core.media.AudioMetadata) {
        _uiState.value = _uiState.value.copy(metadataEditValues = metadata)
    }

    fun dismissMetadataEditor() {
        _uiState.value = _uiState.value.copy(metadataEditJob = null, metadataEditResultMessage = null)
    }

    /**
     * Applies [HistoryUiState.metadataEditValues] to the currently-open job's output file via a
     * lossless FFmpeg remux (`-c copy`), for both MP3/WAV/M4A audio and MP4/MKV/MOV video outputs.
     */
    fun saveMetadataEdit() {
        val job = _uiState.value.metadataEditJob ?: return
        val processor = mediaProcessor
        if (processor == null) {
            _uiState.value = _uiState.value.copy(metadataEditResultMessage = "Metadata editing is unavailable.")
            return
        }
        _uiState.value = _uiState.value.copy(isMetadataEditSaving = true, metadataEditResultMessage = null)
        viewModelScope.launch {
            val result = processor.editFileMetadata(job, _uiState.value.metadataEditValues)
            _uiState.value = _uiState.value.copy(
                isMetadataEditSaving = false,
                metadataEditResultMessage = result.message,
                metadataEditJob = if (result.success) null else job
            )
        }
    }
}

