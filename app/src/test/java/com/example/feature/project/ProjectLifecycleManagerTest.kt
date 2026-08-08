package com.example.feature.project

import com.example.core.database.LoopingVidRepository
import com.example.core.database.ProjectEntity
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.just
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.Assert.*

class ProjectLifecycleManagerTest {

    @Test
    fun `when projectId is null, update is skipped`() = runTest {
        val repository = mockk<LoopingVidRepository>()
        val manager = ProjectLifecycleManager(repository)

        manager.markCompleted(null)

        coVerify(exactly = 0) { repository.getProjectById(any()) }
        coVerify(exactly = 0) { repository.updateProject(any()) }
    }

    @Test
    fun `when projectId is invalid, update is skipped`() = runTest {
        val repository = mockk<LoopingVidRepository>()
        val manager = ProjectLifecycleManager(repository)

        manager.markRendering(-1L)

        coVerify(exactly = 0) { repository.getProjectById(any()) }
        coVerify(exactly = 0) { repository.updateProject(any()) }
    }

    @Test
    fun `when project does not exist, update is skipped`() = runTest {
        val repository = mockk<LoopingVidRepository>()
        coEvery { repository.getProjectById(123L) } returns null
        val manager = ProjectLifecycleManager(repository)

        manager.markFailed(123L)

        coVerify(exactly = 1) { repository.getProjectById(123L) }
        coVerify(exactly = 0) { repository.updateProject(any()) }
    }

    @Test
    fun `when project exists, status is updated correctly`() = runTest {
        val repository = mockk<LoopingVidRepository>()
        val project = ProjectEntity(id = 123L, name = "Test", type = "loop", status = "draft")
        coEvery { repository.getProjectById(123L) } returns project
        coEvery { repository.updateProject(any()) } just Runs
        val manager = ProjectLifecycleManager(repository)

        manager.markRendering(123L)
        manager.markCompleted(123L)
        manager.markFailed(123L)

        coVerify(exactly = 3) { repository.getProjectById(123L) }
        coVerify { repository.updateProject(match { it.status == "rendering" }) }
        coVerify { repository.updateProject(match { it.status == "completed" }) }
        coVerify { repository.updateProject(match { it.status == "failed" }) }
    }
}
