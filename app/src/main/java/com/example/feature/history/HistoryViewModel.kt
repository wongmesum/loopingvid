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
    val syncInfo: FirestoreSyncInfo = FirestoreSyncInfo()
)

class HistoryViewModel(
    private val repository: LoopingVidRepository,
    private val firestoreSyncManager: FirestoreJobHistorySyncManager? = null
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
}

