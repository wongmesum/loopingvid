package com.example.feature.project

import com.example.core.database.LoopingVidRepository

class ProjectLifecycleManager(
    private val repository: LoopingVidRepository
) {
    suspend fun markRendering(projectId: Long?) = updateStatus(projectId, "rendering")
    suspend fun markCompleted(projectId: Long?) = updateStatus(projectId, "completed")
    suspend fun markFailed(projectId: Long?) = updateStatus(projectId, "failed")

    private suspend fun updateStatus(projectId: Long?, status: String) {
        if (projectId == null || projectId <= 0L) return
        val project = repository.getProjectById(projectId) ?: return
        repository.updateProject(project.copy(status = status, updatedAt = System.currentTimeMillis()))
    }
}
