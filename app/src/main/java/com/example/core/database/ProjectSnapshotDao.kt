package com.example.core.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ProjectSnapshotDao {

    @Query("SELECT * FROM project_snapshots WHERE projectId = :projectId ORDER BY createdAt DESC, id DESC")
    fun getSnapshotsForProject(projectId: Long): Flow<List<ProjectSnapshotEntity>>

    @Query("SELECT * FROM project_snapshots WHERE id = :snapshotId")
    suspend fun getSnapshotById(snapshotId: Long): ProjectSnapshotEntity?

    @Insert
    suspend fun insertSnapshot(snapshot: ProjectSnapshotEntity): Long

    @Query("DELETE FROM project_snapshots WHERE id = :snapshotId")
    suspend fun deleteSnapshot(snapshotId: Long)

    @Query("SELECT COUNT(*) FROM project_snapshots WHERE projectId = :projectId")
    suspend fun getSnapshotCount(projectId: Long): Int

    /**
     * Remove the oldest snapshot(s) when the cap is exceeded.
     * Keeps only the [limit] most recent snapshots for a project.
     */
    @Query(
        """
        DELETE FROM project_snapshots
        WHERE projectId = :projectId
        AND id NOT IN (
            SELECT id FROM project_snapshots
            WHERE projectId = :projectId
            ORDER BY createdAt DESC, id DESC
            LIMIT :limit
        )
        """
    )
    suspend fun trimToLimit(projectId: Long, limit: Int)
}
