package com.example.core.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for persisting and querying video rendering and audio mastering job history and logs.
 */
@Dao
interface JobHistoryDao {
    @Query("SELECT * FROM job_history ORDER BY timestamp DESC")
    fun getAllHistory(): Flow<List<JobHistoryEntity>>

    @Query("SELECT * FROM job_history WHERE taskType = :taskType ORDER BY timestamp DESC")
    fun getHistoryByTaskType(taskType: String): Flow<List<JobHistoryEntity>>

    @Query("SELECT * FROM job_history WHERE status = :status ORDER BY timestamp DESC")
    fun getHistoryByStatus(status: String): Flow<List<JobHistoryEntity>>

    @Query("SELECT * FROM job_history WHERE id = :id")
    suspend fun getHistoryById(id: Long): JobHistoryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistory(jobHistory: JobHistoryEntity): Long

    @Update
    suspend fun updateHistory(jobHistory: JobHistoryEntity)

    @Query("DELETE FROM job_history WHERE id = :id")
    suspend fun deleteHistoryById(id: Long)

    @Query("DELETE FROM job_history")
    suspend fun clearAllHistory()
}
