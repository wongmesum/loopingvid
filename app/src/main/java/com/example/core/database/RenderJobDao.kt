package com.example.core.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface RenderJobDao {
    @Query("SELECT * FROM render_jobs ORDER BY createdAt DESC")
    fun getAllJobs(): Flow<List<RenderJobEntity>>

    @Query("SELECT * FROM render_jobs WHERE jobType = :type ORDER BY createdAt DESC")
    fun getJobsByType(type: String): Flow<List<RenderJobEntity>>

    @Query("SELECT * FROM render_jobs WHERE id = :id")
    suspend fun getJobById(id: Long): RenderJobEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertJob(job: RenderJobEntity): Long

    @Update
    suspend fun updateJob(job: RenderJobEntity)

    @Query("DELETE FROM render_jobs WHERE id = :id")
    suspend fun deleteJobById(id: Long)

    @Query("DELETE FROM render_jobs")
    suspend fun clearAllJobs()
}
