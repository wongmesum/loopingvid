package com.example.core.ffmpeg

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.core.database.AppDatabase
import com.example.core.database.LoopingVidRepository
import com.example.feature.visualizer.VisualizerMode
import com.example.feature.visualizer.VisualizerRenderConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

open class DummyFFmpegWrapper : FFmpegWrapper {
    override val logFlow = MutableStateFlow("")
    var lastCommandArgs = emptyList<String>()
    var exitCodeToReturn = 0

    override fun isNativeSupported() = true
    override fun getVersion() = "test"

    override suspend fun execute(command: String): Int = exitCodeToReturn

    override suspend fun execute(commandArgs: List<String>, onProgress: suspend (Int) -> Unit): Int {
        lastCommandArgs = commandArgs
        onProgress(50)
        onProgress(100)
        return exitCodeToReturn
    }

    override fun cancel() {}
}

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class VisualizerProcessorTest {
    private lateinit var db: AppDatabase
    private lateinit var repository: LoopingVidRepository
    private lateinit var ffmpegWrapper: DummyFFmpegWrapper
    private lateinit var processor: VisualizerProcessor

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = LoopingVidRepository(db.renderJobDao(), db.liveSessionDao(), db.appSettingDao())
        ffmpegWrapper = DummyFFmpegWrapper()
        processor = VisualizerProcessor(context, repository, ffmpegWrapper)
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test(expected = IllegalArgumentException::class)
    fun `renderVisualizer rejects empty audio uri`() {
        runBlocking {
            processor.renderVisualizer(
                VisualizerRenderRequest(
                    audioUri = "",
                    config = VisualizerRenderConfig()
                )
            )
        }
    }

    @Test
    fun `renderVisualizer creates a job, executes ffmpeg, and marks as completed`() {
        runBlocking {
            val context = ApplicationProvider.getApplicationContext<Context>()

            // Stand in for a successful FFmpeg run by writing the file the
            // processor checks for before it marks the job complete.
            ffmpegWrapper = object : DummyFFmpegWrapper() {
                override suspend fun execute(
                    commandArgs: List<String>,
                    onProgress: suspend (Int) -> Unit
                ): Int {
                    lastCommandArgs = commandArgs
                    onProgress(100)
                    val outputPath = commandArgs.last()
                    File(outputPath).parentFile?.mkdirs()
                    File(outputPath).writeText("dummy")
                    return 0
                }
            }
            processor = VisualizerProcessor(context, repository, ffmpegWrapper)

            val job = processor.renderVisualizer(
                VisualizerRenderRequest(
                    audioUri = "/audio/track.mp3",
                    config = VisualizerRenderConfig(mode = VisualizerMode.WAVE),
                    outputName = "TestExport"
                )
            )

            assertEquals("COMPLETED", job.status)
            assertEquals("VISUALIZER", job.jobType)
            assertEquals(100, job.progress)
            assertTrue("Output URI should carry the custom name", job.outputUri.contains("TestExport"))
            assertTrue(
                "FFmpeg must receive the audio track",
                ffmpegWrapper.lastCommandArgs.contains("/audio/track.mp3")
            )

            val finalState = processor.progressState.first()
            assertEquals(100, finalState.progress)
            assertTrue(finalState.outputFilePath.isNotBlank())
        }
    }
}
