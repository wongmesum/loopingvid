package com.example.feature.project

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.database.LoopingVidRepository
import com.example.core.database.ProjectEntity
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
    val errorMessage: String? = null
)

class ProjectManagerViewModel(
    private val repository: LoopingVidRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(ProjectManagerUiState(isLoading = true))
    val uiState: StateFlow<ProjectManagerUiState> = _uiState.asStateFlow()

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
