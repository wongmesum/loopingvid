package com.example.feature.project

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.core.database.AppDatabase
import com.example.core.database.LoopingVidRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ProjectManagerViewModelTest {
    private val testDispatcher = StandardTestDispatcher()
    private lateinit var database: AppDatabase
    private lateinit var viewModel: ProjectManagerViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val repository = LoopingVidRepository(
            database.renderJobDao(),
            database.liveSessionDao(),
            database.appSettingDao(),
            projectDao = database.projectDao()
        )
        viewModel = ProjectManagerViewModel(repository)
    }

    @After
    fun tearDown() {
        database.close()
        Dispatchers.resetMain()
    }

    @Test
    fun `create rename and delete project updates state`() = runTest(testDispatcher) {
        viewModel.createProject("My Loop", ProjectType.LOOP)
        advanceUntilIdle()

        val created = viewModel.uiState.value.projects.single()
        assertEquals("My Loop", created.name)
        assertEquals(ProjectType.LOOP, created.projectType)

        viewModel.renameProject(created, "Renamed Loop")
        advanceUntilIdle()
        assertEquals("Renamed Loop", viewModel.uiState.value.projects.single().name)

        viewModel.deleteProject(created.id)
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.projects.isEmpty())
    }
}
