package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.core.database.AppDatabase
import com.example.core.database.LoopingVidRepository
import com.example.core.database.RenderJobEntity
import com.example.core.media.AudioMasteringEngine
import com.example.core.media.CompressorConfig
import com.example.core.media.EqBandConfig
import com.example.core.media.WaveformAnalyzer
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class LoopingVidTest {

    private lateinit var db: AppDatabase
    private lateinit var repository: LoopingVidRepository

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = LoopingVidRepository(db.renderJobDao(), db.liveSessionDao(), db.appSettingDao())
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun `test AudioMasteringEngine LUFS calculation`() {
        val inputLufs = -22.0
        val eq = EqBandConfig(lowGainDb = 2.0f, midGainDb = 1.0f, highGainDb = 3.0f)
        val comp = CompressorConfig(thresholdDb = -18f, ratio = 3.0f, makeupGainDb = 4.0f)

        val outputLufs = AudioMasteringEngine.calculateOutputLufs(
            inputRmsLufs = inputLufs,
            eqConfig = eq,
            compConfig = comp,
            autoLevelConfig = com.example.core.media.AutoLevelingConfig(),
            targetLufs = -14.0
        )

        assertTrue("Output LUFS should be boosted from -22.0", outputLufs > inputLufs)
    }

    @Test
    fun `test WaveformAnalyzer generates points and beat markers`() {
        val analysis = WaveformAnalyzer.generateSimulatedWaveform(durationMs = 60000L, pointCount = 50)
        assertEquals(50, analysis.waveformPoints.size)
        assertTrue(analysis.beatMarkersMs.isNotEmpty())
    }

    @Test
    fun `test Repository saves and retrieves render job`() = runBlocking {
        val job = RenderJobEntity(
            jobType = "LOOP",
            title = "Test Loop",
            inputUri = "content://media/1",
            outputUri = "/storage/test.mp4",
            style = "CROSSFADE",
            status = "COMPLETED",
            progress = 100,
            durationSec = 60.0,
            fileSizeMb = 5.0
        )

        val id = repository.saveJob(job)
        val fetched = repository.getJobById(id)

        assertNotNull(fetched)
        assertEquals("Test Loop", fetched?.title)
        assertEquals("CROSSFADE", fetched?.style)

        val allJobs = repository.allJobs.first()
        assertEquals(1, allJobs.size)
    }
}
