package com.example.feature.project

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.database.LoopingVidRepository
import com.example.core.database.ProjectEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ProjectManagerUiState(
    val projects: List<ProjectEntity> = emptyList(),
    val isLoading: Boolean = false
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
        viewModelScope.launch {
            repository.saveProject(
                ProjectEntity(
                    name = name,
                    type = type.value,
                    status = "draft"
                )
            )
        }
    }

    fun renameProject(project: ProjectEntity, newName: String) {
        viewModelScope.launch {
            repository.updateProject(project.copy(name = newName))
        }
    }

    fun deleteProject(id: Long) {
        viewModelScope.launch {
            repository.deleteProjectById(id)
        }
    }
}
