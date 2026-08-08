package com.example.feature.project

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.core.database.AppDatabase
import com.example.core.database.LoopingVidRepository
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
        // Room runs queries on its own executors by default, which advanceUntilIdle()
        // cannot wait for — the Flow would not have emitted yet when we assert.
        // Pinning both executors to the test dispatcher makes emissions deterministic.
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .setQueryExecutor(testDispatcher.asExecutor())
            .setTransactionExecutor(testDispatcher.asExecutor())
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

    @Test
    fun `create and rename rejects blank names`() = runTest(testDispatcher) {
        viewModel.createProject("   ", ProjectType.LOOP)
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.projects.isEmpty())
        assertEquals("Project name cannot be empty", viewModel.uiState.value.errorMessage)

        viewModel.dismissError()
        viewModel.createProject("Valid", ProjectType.LOOP)
        advanceUntilIdle()
        val created = viewModel.uiState.value.projects.single()

        viewModel.renameProject(created, "")
        advanceUntilIdle()
        assertEquals("Valid", viewModel.uiState.value.projects.single().name)
        assertEquals("Project name cannot be empty", viewModel.uiState.value.errorMessage)
    }

    @Test
    fun `update config rejects invalid json`() = runTest(testDispatcher) {
        viewModel.createProject("My Source", ProjectType.LOOP)
        advanceUntilIdle()
        val original = viewModel.uiState.value.projects.single()

        viewModel.updateProjectConfig(original, "invalid json {")
        advanceUntilIdle()
        assertEquals("{}", viewModel.uiState.value.projects.single().configJson)
        assertEquals("Invalid project configuration", viewModel.uiState.value.errorMessage)

        viewModel.dismissError()
        viewModel.updateProjectConfig(original, """{"key": "value"}""")
        advanceUntilIdle()
        assertEquals("""{"key": "value"}""", viewModel.uiState.value.projects.single().configJson)
    }

    @Test
    fun `duplicate and archive project`() = runTest(testDispatcher) {
        viewModel.createProject("My Source", ProjectType.LOOP)
        advanceUntilIdle()

        val original = viewModel.uiState.value.projects.single()

        viewModel.duplicateProject(original)
        advanceUntilIdle()

        val projects = viewModel.uiState.value.projects
        assertEquals(2, projects.size)
        assertTrue(projects.any { it.name == "My Source" })
        assertTrue(projects.any { it.name == "My Source (Copy)" })

        viewModel.archiveProject(original.id)
        advanceUntilIdle()

        val archived = viewModel.uiState.value.projects.find { it.id == original.id }
        assertEquals("archived", archived?.status)
    }
}
