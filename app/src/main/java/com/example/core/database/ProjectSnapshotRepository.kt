package com.example.core.database

import kotlinx.coroutines.flow.Flow
import org.json.JSONException
import org.json.JSONObject

/**
 * Manages project snapshot revisions, abstracting the DAO and enforcing the
 * 10-snapshot cap per project.
 */
class ProjectSnapshotRepository(
    private val snapshotDao: ProjectSnapshotDao? = null,
    private val projectDao: ProjectDao? = null
) {
    companion object {
        const val MAX_SNAPSHOTS_PER_PROJECT = 10
    }

    /** Returns all snapshots for a given project, ordered newest first. */
    fun getSnapshotsForProject(projectId: Long): Flow<List<ProjectSnapshotEntity>> =
        snapshotDao?.getSnapshotsForProject(projectId) ?: kotlinx.coroutines.flow.flowOf(emptyList())

    /**
     * Creates a new snapshot from the project's current state.
     * Enforces [MAX_SNAPSHOTS_PER_PROJECT] by deleting the oldest if necessary.
     */
    suspend fun createSnapshot(
        projectId: Long,
        label: String,
        now: Long = System.currentTimeMillis()
    ): Long {
        if (snapshotDao == null || projectDao == null) return -1L

        val project = projectDao.getProjectById(projectId) ?: return -1L

        // Guard against invalid JSON out of caution, though the project should be safe
        if (!isValidConfigJson(project.configJson)) return -1L

        val snapshot = ProjectSnapshotEntity(
            projectId = projectId,
            label = label.trim().takeIf { it.isNotEmpty() } ?: "Snapshot",
            configJson = project.configJson,
            sourceMediaUri = project.sourceMediaUri,
            sourceAudioUri = project.sourceAudioUri,
            createdAt = now
        )

        val id = snapshotDao.insertSnapshot(snapshot)
        snapshotDao.trimToLimit(projectId, MAX_SNAPSHOTS_PER_PROJECT)
        return id
    }

    /**
     * Restores a snapshot by overwriting the parent project's current state
     * with the snapshot's state. Does NOT delete the snapshot.
     */
    suspend fun restoreSnapshot(snapshotId: Long): Boolean {
        if (snapshotDao == null || projectDao == null) return false

        val snapshot = snapshotDao.getSnapshotById(snapshotId) ?: return false
        val project = projectDao.getProjectById(snapshot.projectId) ?: return false

        val restored = project.copy(
            configJson = snapshot.configJson,
            sourceMediaUri = snapshot.sourceMediaUri,
            sourceAudioUri = snapshot.sourceAudioUri,
            updatedAt = System.currentTimeMillis()
        )

        projectDao.updateProject(restored)
        return true
    }

    suspend fun deleteSnapshot(snapshotId: Long) {
        snapshotDao?.deleteSnapshot(snapshotId)
    }

    private fun isValidConfigJson(configJson: String): Boolean =
        try { JSONObject(configJson); true } catch (_: JSONException) { false }
}
