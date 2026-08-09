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
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ProjectSnapshotRepositoryTest {
    private val testDispatcher = StandardTestDispatcher()
    private lateinit var database: AppDatabase
    private lateinit var repository: ProjectSnapshotRepository

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .setQueryExecutor(testDispatcher.asExecutor())
            .setTransactionExecutor(testDispatcher.asExecutor())
            .build()
        repository = ProjectSnapshotRepository(
            snapshotDao = database.projectSnapshotDao(),
            projectDao = database.projectDao()
        )
    }

    @After
    fun tearDown() {
        database.close()
        Dispatchers.resetMain()
    }

    private suspend fun insertProject(
        name: String = "Test Project",
        configJson: String = """{"speed":1}"""
    ): Long {
        return database.projectDao().insertProject(
            ProjectEntity(
                name = name,
                type = "loop",
                configJson = configJson,
                status = "draft",
                sourceMediaUri = "content://video/1",
                sourceAudioUri = "content://audio/1",
                createdAt = 1000L,
                updatedAt = 1000L
            )
        )
    }

    @Test
    fun `createSnapshot captures current project config`() = runTest(testDispatcher) {
        val projectId = insertProject(configJson = """{"speed":2}""")

        val snapshotId = repository.createSnapshot(projectId, "Versi awal", now = 5000L)
        assertTrue("snapshot insert must return a positive id", snapshotId > 0)

        val snapshots = repository.getSnapshotsForProject(projectId).first()
        assertEquals(1, snapshots.size)
        assertEquals("Versi awal", snapshots[0].label)
        assertEquals("""{"speed":2}""", snapshots[0].configJson)
        assertEquals("content://video/1", snapshots[0].sourceMediaUri)
        assertEquals("content://audio/1", snapshots[0].sourceAudioUri)
        assertEquals(5000L, snapshots[0].createdAt)
    }

    @Test
    fun `createSnapshot with blank label defaults to Snapshot`() = runTest(testDispatcher) {
        val projectId = insertProject()

        repository.createSnapshot(projectId, "   ", now = 1000L)

        val snapshots = repository.getSnapshotsForProject(projectId).first()
        assertEquals("Snapshot", snapshots[0].label)
    }

    @Test
    fun `createSnapshot for nonexistent project returns -1`() = runTest(testDispatcher) {
        val result = repository.createSnapshot(999L, "Fail", now = 1000L)
        assertEquals(-1L, result)
    }

    @Test
    fun `trimToLimit keeps only the 10 most recent snapshots`() = runTest(testDispatcher) {
        val projectId = insertProject()

        // Create 12 snapshots with distinct timestamps
        for (i in 1..12) {
            repository.createSnapshot(projectId, "Snap $i", now = i * 1000L)
        }

        val snapshots = repository.getSnapshotsForProject(projectId).first()
        assertEquals(
            "only MAX_SNAPSHOTS_PER_PROJECT should survive",
            ProjectSnapshotRepository.MAX_SNAPSHOTS_PER_PROJECT,
            snapshots.size
        )

        // Oldest two (Snap 1, Snap 2) should have been trimmed
        val labels = snapshots.map { it.label }
        assertTrue("Snap 1" !in labels)
        assertTrue("Snap 2" !in labels)
        assertTrue("Snap 12" in labels)
        assertTrue("Snap 3" in labels)
    }

    @Test
    fun `restoreSnapshot overwrites project configJson and URIs`() = runTest(testDispatcher) {
        val projectId = insertProject(configJson = """{"speed":1}""")

        // Snapshot the initial state
        val snapshotId = repository.createSnapshot(projectId, "Before change", now = 1000L)

        // Simulate user modifying the project
        val modified = database.projectDao().getProjectById(projectId)!!
        database.projectDao().updateProject(
            modified.copy(
                configJson = """{"speed":3}""",
                sourceMediaUri = "content://video/new",
                updatedAt = 2000L
            )
        )

        // Verify current state changed
        val before = database.projectDao().getProjectById(projectId)!!
        assertEquals("""{"speed":3}""", before.configJson)

        // Restore the snapshot
        val restored = repository.restoreSnapshot(snapshotId)
        assertTrue("restore must succeed", restored)

        // Verify project returned to snapshot state
        val after = database.projectDao().getProjectById(projectId)!!
        assertEquals("""{"speed":1}""", after.configJson)
        assertEquals("content://video/1", after.sourceMediaUri)
        assertEquals("content://audio/1", after.sourceAudioUri)
    }

    @Test
    fun `restoreSnapshot with invalid snapshotId returns false`() = runTest(testDispatcher) {
        val result = repository.restoreSnapshot(999L)
        assertEquals(false, result)
    }

    @Test
    fun `deleteSnapshot removes snapshot by id`() = runTest(testDispatcher) {
        val projectId = insertProject()
        val snapshotId = repository.createSnapshot(projectId, "To delete", now = 1000L)

        repository.deleteSnapshot(snapshotId)

        val snapshots = repository.getSnapshotsForProject(projectId).first()
        assertTrue(snapshots.isEmpty())
    }

    @Test
    fun `cascade delete removes snapshots when project is deleted`() = runTest(testDispatcher) {
        val projectId = insertProject()
        repository.createSnapshot(projectId, "Snap A", now = 1000L)
        repository.createSnapshot(projectId, "Snap B", now = 2000L)

        // Confirm snapshots exist
        assertEquals(2, repository.getSnapshotsForProject(projectId).first().size)

        // Delete the project — FK CASCADE should wipe snapshots
        database.projectDao().deleteProjectById(projectId)

        val remaining = repository.getSnapshotsForProject(projectId).first()
        assertTrue("cascade delete must remove all child snapshots", remaining.isEmpty())
    }

    @Test
    fun `snapshots are isolated between projects`() = runTest(testDispatcher) {
        val projectA = insertProject(name = "Project A")
        val projectB = insertProject(name = "Project B")

        repository.createSnapshot(projectA, "A snap", now = 1000L)
        repository.createSnapshot(projectB, "B snap", now = 2000L)

        assertEquals(1, repository.getSnapshotsForProject(projectA).first().size)
        assertEquals(1, repository.getSnapshotsForProject(projectB).first().size)
    }
}
