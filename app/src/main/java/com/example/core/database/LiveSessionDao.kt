package com.example.core.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface LiveSessionDao {
    @Query("SELECT * FROM live_sessions ORDER BY startedAt DESC")
    fun getAllSessions(): Flow<List<LiveSessionEntity>>

    @Query("SELECT * FROM live_sessions WHERE id = :id")
    suspend fun getSessionById(id: Long): LiveSessionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: LiveSessionEntity): Long

    @Update
    suspend fun updateSession(session: LiveSessionEntity)

    @Query("DELETE FROM live_sessions WHERE id = :id")
    suspend fun deleteSessionById(id: Long)
}
