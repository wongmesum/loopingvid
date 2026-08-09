package com.example.core.database

import com.example.core.ui.SelectedMediaFile
import kotlinx.coroutines.flow.Flow

/**
 * Owns the real-media asset cache.
 *
 * Kept separate from [LoopingVidRepository] on purpose: that class requires three
 * unrelated DAOs, which would force every asset test to fake them. This one wraps
 * [AssetDao] alone so the asset feature stays testable in isolation.
 */
class AssetRepository(private val assetDao: AssetDao) {

    val recentAssets: Flow<List<AssetEntity>> = assetDao.observeRecent(RECENT_LIMIT)

    val favoriteAssets: Flow<List<AssetEntity>> = assetDao.observeFavorites()

    /**
     * Records a real user pick. Re-picking a file already in the cache bumps its
     * usage instead of inserting a duplicate row, and refreshes metadata that can
     * change between picks (a renamed or re-encoded file).
     *
     * @param permissionPersisted must come from a verified read of
     *   `contentResolver.persistedUriPermissions`, never from the return of
     *   [SelectedMediaFile.takePersistablePermission], which swallows failures.
     */
    suspend fun recordAccess(
        uriString: String,
        fileName: String,
        fileSize: Long,
        mimeType: String,
        mediaType: String,
        permissionPersisted: Boolean,
        now: Long = System.currentTimeMillis()
    ): Long {
        val existing = assetDao.getByUri(uriString)
        if (existing == null) {
            return assetDao.insert(
                AssetEntity(
                    uriString = uriString,
                    fileName = fileName,
                    fileSize = fileSize,
                    mimeType = mimeType,
                    mediaType = mediaType,
                    usageCount = 1,
                    lastAccessedAt = now,
                    createdAt = now,
                    permissionPersisted = permissionPersisted
                )
            )
        }

        assetDao.update(
            existing.copy(
                fileName = fileName,
                fileSize = fileSize,
                mimeType = mimeType,
                mediaType = mediaType,
                usageCount = existing.usageCount + 1,
                lastAccessedAt = now,
                // Once a grant is confirmed persisted it stays persisted; a later pick
                // through a non-persistable picker must not downgrade the record.
                permissionPersisted = existing.permissionPersisted || permissionPersisted,
                isMissing = false
            )
        )
        return existing.id
    }

    suspend fun recordAccess(
        file: SelectedMediaFile,
        permissionPersisted: Boolean,
        now: Long = System.currentTimeMillis()
    ): Long = recordAccess(
        uriString = file.uri.toString(),
        fileName = file.fileName,
        fileSize = file.fileSize,
        mimeType = file.mimeType,
        mediaType = if (file.isVideo) MEDIA_TYPE_VIDEO else MEDIA_TYPE_AUDIO,
        permissionPersisted = permissionPersisted,
        now = now
    )

    suspend fun toggleFavorite(id: Long) {
        val asset = assetDao.getById(id) ?: return
        assetDao.update(asset.copy(isFavorite = !asset.isFavorite))
    }

    suspend fun togglePinned(id: Long) {
        val asset = assetDao.getById(id) ?: return
        assetDao.update(asset.copy(isPinned = !asset.isPinned))
    }

    suspend fun markMissing(id: Long, isMissing: Boolean) {
        val asset = assetDao.getById(id) ?: return
        assetDao.update(asset.copy(isMissing = isMissing))
    }

    suspend fun deleteAsset(id: Long) = assetDao.deleteById(id)

    companion object {
        const val RECENT_LIMIT = 50
        const val MEDIA_TYPE_VIDEO = "VIDEO"
        const val MEDIA_TYPE_AUDIO = "AUDIO"
    }
}
