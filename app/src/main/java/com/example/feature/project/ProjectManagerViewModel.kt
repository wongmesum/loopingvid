package com.example.feature.project

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.database.LoopingVidRepository
import com.example.core.database.ProjectEntity
import com.example.core.database.ProjectSnapshotEntity
import com.example.core.database.ProjectSnapshotRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONException
import org.json.JSONObject

private const val STATUS_DRAFT = "draft"
private const val STATUS_ARCHIVED = "archived"
private const val COPY_SUFFIX = " (Copy)"

data class ProjectManagerUiState(
    val projects: List<ProjectEntity> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    /** Project whose snapshot history is open, null when the dialog is closed. */
    val snapshotProject: ProjectEntity? = null,
    val snapshots: List<ProjectSnapshotEntity> = emptyList()
)

class ProjectManagerViewModel(
    private val repository: LoopingVidRepository,
    private val snapshotRepository: ProjectSnapshotRepository? = null
) : ViewModel() {
    private val _uiState = MutableStateFlow(ProjectManagerUiState(isLoading = true))
    val uiState: StateFlow<ProjectManagerUiState> = _uiState.asStateFlow()

    /** Cancelled and replaced whenever a different project's history is opened. */
    private var snapshotObserverJob: Job? = null

    init {
        viewModelScope.launch {
            repository.allProjects.collect { projectsList ->
                _uiState.value = _uiState.value.copy(
                    projects = projectsList,
                    isLoading = false
                )
            }
        }
    }

    fun createProject(name: String, type: ProjectType) {
        val trimmed = name.trim()
        if (trimmed.isBlank()) {
            setError("Project name cannot be empty")
            return
        }
        viewModelScope.launch {
            repository.saveProject(ProjectEntity(name = trimmed, type = type.value, status = STATUS_DRAFT))
        }
    }

    fun renameProject(project: ProjectEntity, newName: String) {
        val trimmed = newName.trim()
        if (trimmed.isBlank()) {
            setError("Project name cannot be empty")
            return
        }
        viewModelScope.launch {
            repository.updateProject(project.copy(name = trimmed))
        }
    }

    fun duplicateProject(project: ProjectEntity) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            repository.saveProject(
                project.copy(
                    id = 0,
                    name = project.name + COPY_SUFFIX,
                    status = STATUS_DRAFT,
                    createdAt = now,
                    updatedAt = now
                )
            )
        }
    }

    fun archiveProject(id: Long) {
        viewModelScope.launch {
            val project = repository.getProjectById(id) ?: return@launch
            repository.updateProject(project.copy(status = STATUS_ARCHIVED))
        }
    }

    fun updateProjectConfig(project: ProjectEntity, configJson: String) {
        if (!isValidConfigJson(configJson)) {
            setError("Invalid project configuration")
            return
        }
        viewModelScope.launch {
            repository.updateProject(project.copy(configJson = configJson))
        }
    }

    fun deleteProject(id: Long) {
        viewModelScope.launch {
            repository.deleteProjectById(id)
        }
    }

    fun dismissError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    // --- Snapshot actions ---

    fun openSnapshotHistory(project: ProjectEntity) {
        if (snapshotRepository == null) return
        _uiState.value = _uiState.value.copy(snapshotProject = project)
        snapshotObserverJob?.cancel()
        snapshotObserverJob = viewModelScope.launch {
            snapshotRepository.getSnapshotsForProject(project.id).collect { list ->
                _uiState.value = _uiState.value.copy(snapshots = list)
            }
        }
    }

    fun closeSnapshotHistory() {
        snapshotObserverJob?.cancel()
        _uiState.value = _uiState.value.copy(snapshotProject = null, snapshots = emptyList())
    }

    fun createSnapshot(projectId: Long, label: String) {
        if (snapshotRepository == null) return
        viewModelScope.launch {
            val id = snapshotRepository.createSnapshot(projectId, label)
            if (id < 0) setError("Gagal menyimpan snapshot")
        }
    }

    fun restoreSnapshot(snapshotId: Long) {
        if (snapshotRepository == null) return
        viewModelScope.launch {
            val ok = snapshotRepository.restoreSnapshot(snapshotId)
            if (!ok) setError("Gagal memulihkan snapshot")
        }
    }

    fun deleteSnapshot(snapshotId: Long) {
        if (snapshotRepository == null) return
        viewModelScope.launch {
            snapshotRepository.deleteSnapshot(snapshotId)
        }
    }

    private fun setError(message: String) {
        _uiState.value = _uiState.value.copy(errorMessage = message)
    }

    private fun isValidConfigJson(configJson: String): Boolean {
        return try {
            JSONObject(configJson)
            true
        } catch (_: JSONException) {
            false
        }
    }
}
