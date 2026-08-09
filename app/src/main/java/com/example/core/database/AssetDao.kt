package com.example.core.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for the real-media asset cache (recent files, favorites, pinned items).
 */
@Dao
interface AssetDao {
    @Query("SELECT * FROM assets ORDER BY isPinned DESC, lastAccessedAt DESC LIMIT :limit")
    fun observeRecent(limit: Int): Flow<List<AssetEntity>>

    @Query("SELECT * FROM assets WHERE isFavorite = 1 ORDER BY lastAccessedAt DESC")
    fun observeFavorites(): Flow<List<AssetEntity>>

    @Query("SELECT * FROM assets WHERE uriString = :uriString LIMIT 1")
    suspend fun getByUri(uriString: String): AssetEntity?

    @Query("SELECT * FROM assets WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): AssetEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(asset: AssetEntity): Long

    @Update
    suspend fun update(asset: AssetEntity)

    @Query("DELETE FROM assets WHERE id = :id")
    suspend fun deleteById(id: Long)
}
