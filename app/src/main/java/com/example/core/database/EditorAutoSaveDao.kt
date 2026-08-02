package com.example.core.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface EditorAutoSaveDao {

    @Query("SELECT * FROM editor_autosave_session WHERE id = 1 LIMIT 1")
    fun observeAutoSaveSession(): Flow<EditorAutoSaveEntity?>

    @Query("SELECT * FROM editor_autosave_session WHERE id = 1 LIMIT 1")
    suspend fun getAutoSaveSession(): EditorAutoSaveEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveAutoSaveSession(session: EditorAutoSaveEntity)

    @Query("DELETE FROM editor_autosave_session WHERE id = 1")
    suspend fun clearAutoSaveSession()
}
