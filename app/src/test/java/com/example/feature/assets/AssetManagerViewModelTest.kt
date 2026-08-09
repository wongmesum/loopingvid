package com.example.feature.assets

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.core.database.AppDatabase
import com.example.core.database.AssetRepository
import com.example.core.ui.SelectedMediaFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.asExecutor
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
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
class AssetManagerViewModelTest {
    private val testDispatcher = StandardTestDispatcher()
    private lateinit var database: AppDatabase
    private lateinit var repository: AssetRepository
    private lateinit var viewModel: AssetManagerViewModel

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
        viewModel = AssetManagerViewModel(repository)
    }

    @After
    fun tearDown() {
        database.close()
        Dispatchers.resetMain()
    }

    @Test
    fun `setTab updates selectedTab`() = runTest(testDispatcher) {
        assertEquals(0, viewModel.uiState.value.selectedTab)
        viewModel.setTab(1)
        assertEquals(1, viewModel.uiState.value.selectedTab)
    }

    @Test
    fun `recordAccess updates recent assets list`() = runTest(testDispatcher) {
        val file = SelectedMediaFile(
            uri = android.net.Uri.parse("content://1"),
            fileName = "clip.mp4",
            fileSize = 100,
            mimeType = "video/mp4",
            isVideo = true
        )
        viewModel.recordAccess(file, permissionPersisted = true)
        advanceUntilIdle()

        val recent = viewModel.uiState.value.recentAssets
        assertEquals(1, recent.size)
        assertEquals("clip.mp4", recent[0].fileName)
    }

    @Test
    fun `toggleFavorite moves asset to favorite list`() = runTest(testDispatcher) {
        val file = SelectedMediaFile(
            uri = android.net.Uri.parse("content://2"),
            fileName = "fav.mp4",
            fileSize = 200,
            mimeType = "video/mp4",
            isVideo = true
        )
        viewModel.recordAccess(file, permissionPersisted = false)
        advanceUntilIdle()

        val id = viewModel.uiState.value.recentAssets.first().id
        viewModel.toggleFavorite(id)
        advanceUntilIdle()

        val favorites = viewModel.uiState.value.favoriteAssets
        assertEquals(1, favorites.size)
        assertEquals("fav.mp4", favorites[0].fileName)
        assertTrue(favorites[0].isFavorite)
    }
}
