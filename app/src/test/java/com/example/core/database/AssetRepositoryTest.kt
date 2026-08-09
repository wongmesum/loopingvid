package com.example.core.database

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.asExecutor
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class AssetRepositoryTest {
    private val testDispatcher = StandardTestDispatcher()
    private lateinit var database: AppDatabase
    private lateinit var repository: AssetRepository

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .setQueryExecutor(testDispatcher.asExecutor())
            .setTransactionExecutor(testDispatcher.asExecutor())
            .build()
        repository = AssetRepository(database.assetDao())
    }

    @After
    fun tearDown() {
        database.close()
        Dispatchers.resetMain()
    }

    @Test
    fun `first recordAccess inserts with usageCount 1`() = runTest(testDispatcher) {
        repository.recordAccess(
            uriString = "content://media/1",
            fileName = "clip.mp4",
            fileSize = 2048,
            mimeType = "video/mp4",
            mediaType = "VIDEO",
            permissionPersisted = true,
            now = 1000L
        )

        val assets = repository.recentAssets.first()
        assertEquals(1, assets.size)
        assertEquals(1, assets[0].usageCount)
        assertEquals("clip.mp4", assets[0].fileName)
        assertEquals(1000L, assets[0].createdAt)
        assertTrue(assets[0].permissionPersisted)
    }

    @Test
    fun `second recordAccess on same URI increments usageCount without duplicating`() = runTest(testDispatcher) {
        repository.recordAccess(
            uriString = "content://media/1",
            fileName = "clip.mp4",
            fileSize = 2048,
            mimeType = "video/mp4",
            mediaType = "VIDEO",
            permissionPersisted = false,
            now = 1000L
        )
        repository.recordAccess(
            uriString = "content://media/1",
            fileName = "clip_renamed.mp4",
            fileSize = 4096,
            mimeType = "video/mp4",
            mediaType = "VIDEO",
            permissionPersisted = false,
            now = 2000L
        )

        val assets = repository.recentAssets.first()
        assertEquals(1, assets.size)
        assertEquals(2, assets[0].usageCount)
        assertEquals("clip_renamed.mp4", assets[0].fileName)
        assertEquals(4096, assets[0].fileSize)
        assertEquals(2000L, assets[0].lastAccessedAt)
        // createdAt stays at the original value
        assertEquals(1000L, assets[0].createdAt)
    }

    @Test
    fun `permissionPersisted never downgrades once true`() = runTest(testDispatcher) {
        repository.recordAccess(
            uriString = "content://media/1",
            fileName = "clip.mp4",
            fileSize = 2048,
            mimeType = "video/mp4",
            mediaType = "VIDEO",
            permissionPersisted = true,
            now = 1000L
        )
        // Second pick via a non-persistable picker
        repository.recordAccess(
            uriString = "content://media/1",
            fileName = "clip.mp4",
            fileSize = 2048,
            mimeType = "video/mp4",
            mediaType = "VIDEO",
            permissionPersisted = false,
            now = 2000L
        )

        val asset = repository.recentAssets.first().single()
        assertTrue(asset.permissionPersisted)
    }

    @Test
    fun `toggleFavorite flips isFavorite`() = runTest(testDispatcher) {
        val id = repository.recordAccess(
            uriString = "content://media/2",
            fileName = "song.mp3",
            fileSize = 512,
            mimeType = "audio/mpeg",
            mediaType = "AUDIO",
            permissionPersisted = false,
            now = 1000L
        )

        repository.toggleFavorite(id)
        var asset = repository.recentAssets.first().single()
        assertTrue(asset.isFavorite)

        repository.toggleFavorite(id)
        asset = repository.recentAssets.first().single()
        assertFalse(asset.isFavorite)
    }

    @Test
    fun `togglePinned flips isPinned`() = runTest(testDispatcher) {
        val id = repository.recordAccess(
            uriString = "content://media/3",
            fileName = "track.wav",
            fileSize = 1024,
            mimeType = "audio/wav",
            mediaType = "AUDIO",
            permissionPersisted = false,
            now = 1000L
        )

        repository.togglePinned(id)
        var asset = repository.recentAssets.first().single()
        assertTrue(asset.isPinned)

        repository.togglePinned(id)
        asset = repository.recentAssets.first().single()
        assertFalse(asset.isPinned)
    }

    @Test
    fun `favoriteAssets only returns favorites`() = runTest(testDispatcher) {
        val id1 = repository.recordAccess(
            uriString = "content://media/1",
            fileName = "a.mp4",
            fileSize = 100,
            mimeType = "video/mp4",
            mediaType = "VIDEO",
            permissionPersisted = false,
            now = 1000L
        )
        repository.recordAccess(
            uriString = "content://media/2",
            fileName = "b.mp4",
            fileSize = 200,
            mimeType = "video/mp4",
            mediaType = "VIDEO",
            permissionPersisted = false,
            now = 2000L
        )

        repository.toggleFavorite(id1)

        val favorites = repository.favoriteAssets.first()
        assertEquals(1, favorites.size)
        assertEquals("a.mp4", favorites[0].fileName)
    }

    @Test
    fun `deleteAsset removes the row`() = runTest(testDispatcher) {
        val id = repository.recordAccess(
            uriString = "content://media/1",
            fileName = "clip.mp4",
            fileSize = 2048,
            mimeType = "video/mp4",
            mediaType = "VIDEO",
            permissionPersisted = false,
            now = 1000L
        )

        repository.deleteAsset(id)

        val assets = repository.recentAssets.first()
        assertTrue(assets.isEmpty())
    }
}
