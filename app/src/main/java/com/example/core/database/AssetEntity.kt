package com.example.core.database

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Caches metadata for a real media file the user has picked at least once, so
 * recent/favorite/pinned lists can be shown without re-resolving the URI every time.
 *
 * `uriString` is unique: re-picking the same file updates the existing row
 * (bumping `usageCount`/`lastAccessedAt`) instead of inserting a duplicate.
 */
@Entity(
    tableName = "assets",
    indices = [
        Index(value = ["uriString"], unique = true),
        Index(value = ["lastAccessedAt"]),
        Index(value = ["isFavorite"])
    ]
)
data class AssetEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val uriString: String,
    val fileName: String,
    val fileSize: Long,
    val mimeType: String,
    val mediaType: String, // "VIDEO" | "AUDIO"
    val isFavorite: Boolean = false,
    val isPinned: Boolean = false,
    val usageCount: Int = 1,
    val lastAccessedAt: Long,
    val createdAt: Long,
    // True only when verified against contentResolver.persistedUriPermissions at record time.
    // takePersistablePermission() swallows SecurityException, so its return alone can't be trusted.
    val permissionPersisted: Boolean = false,
    val isMissing: Boolean = false
)
