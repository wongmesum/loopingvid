package com.example.core.audio

import android.net.Uri
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class AudioAnalysisRepositoryTest {

    private val fakeResult = AudioAnalysisResult(
        durationMs = 5000,
        sampleRate = 44100,
        channelCount = 2,
        waveform = WaveformData(listOf(0.1f, 0.5f, 0.3f)),
        spectrum = SpectrumData(listOf(0.2f, 0.8f)),
        bpm = BpmData(120.0, 0.9f),
        loudness = LoudnessData(-14.0, -1.0, -18.0),
        peakDb = -1.0,
        rmsDb = -18.0
    )

    private fun fakeAnalyzer(result: Result<AudioAnalysisResult> = Result.success(fakeResult)): CountingAnalyzer =
        CountingAnalyzer(result)

    private fun newRepository(analyzer: AudioAnalyzer): AudioAnalysisRepository {
        val file = java.io.File.createTempFile("test_audio_cache_${System.nanoTime()}", ".preferences_pb")
        file.delete() // DataStore creates it fresh
        val dataStore = PreferenceDataStoreFactory.create(
            corruptionHandler = null,
            migrations = emptyList(),
            scope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
            produceFile = { file }
        )
        return AudioAnalysisRepository(dataStore, analyzer)
    }

    @Test
    fun `getOrAnalyze returns result from analyzer on cache miss`() = runTest {
        val analyzer = fakeAnalyzer()
        val repo = newRepository(analyzer)
        val uri = Uri.parse("content://media/audio/1")

        val result = repo.getOrAnalyze(uri)

        assertTrue(result.isSuccess)
        assertEquals(fakeResult, result.getOrNull())
        assertEquals(1, analyzer.callCount)
    }

    @Test
    fun `getOrAnalyze returns cached result on second call without re-analyzing`() = runTest {
        val analyzer = fakeAnalyzer()
        val repo = newRepository(analyzer)
        val uri = Uri.parse("content://media/audio/2")

        repo.getOrAnalyze(uri) // first — triggers analyzer
        val second = repo.getOrAnalyze(uri) // second — should hit cache

        assertTrue(second.isSuccess)
        assertEquals(fakeResult, second.getOrNull())
        assertEquals(1, analyzer.callCount) // NOT called again
    }

    @Test
    fun `different URIs are cached independently`() = runTest {
        val analyzer = fakeAnalyzer()
        val repo = newRepository(analyzer)

        repo.getOrAnalyze(Uri.parse("content://media/audio/a"))
        repo.getOrAnalyze(Uri.parse("content://media/audio/b"))

        assertEquals(2, analyzer.callCount)
    }

    @Test
    fun `different options for same URI are cached independently`() = runTest {
        val analyzer = fakeAnalyzer()
        val repo = newRepository(analyzer)
        val uri = Uri.parse("content://media/audio/3")

        repo.getOrAnalyze(uri, AnalysisOptions(detectBpm = true))
        repo.getOrAnalyze(uri, AnalysisOptions(detectBpm = false))

        assertEquals(2, analyzer.callCount)
    }

    @Test
    fun `getCached returns null when nothing cached`() = runTest {
        val repo = newRepository(fakeAnalyzer())

        val cached = repo.getCached(Uri.parse("content://media/audio/4"))
        assertNull(cached)
    }

    @Test
    fun `getCached returns result after getOrAnalyze`() = runTest {
        val repo = newRepository(fakeAnalyzer())
        val uri = Uri.parse("content://media/audio/5")

        repo.getOrAnalyze(uri)
        val cached = repo.getCached(uri)

        assertNotNull(cached)
        assertEquals(fakeResult.durationMs, cached!!.durationMs)
    }

    @Test
    fun `clearCache removes all entries`() = runTest {
        val analyzer = fakeAnalyzer()
        val repo = newRepository(analyzer)
        val uri = Uri.parse("content://media/audio/6")

        repo.getOrAnalyze(uri)
        repo.clearCache()
        repo.getOrAnalyze(uri)

        assertEquals(2, analyzer.callCount) // re-analyzed after clear
    }

    @Test
    fun `analyzer failure is propagated without caching`() = runTest {
        val error = RuntimeException("decode failed")
        val analyzer = fakeAnalyzer(Result.failure(error))
        val repo = newRepository(analyzer)
        val uri = Uri.parse("content://media/audio/7")

        val result = repo.getOrAnalyze(uri)

        assertTrue(result.isFailure)
        assertEquals("decode failed", result.exceptionOrNull()?.message)

        // Verify nothing cached
        assertNull(repo.getCached(uri))
    }

    /** Helper that counts how many times [analyze] was called. */
    private class CountingAnalyzer(private val result: Result<AudioAnalysisResult>) : AudioAnalyzer {
        var callCount = 0
            private set

        override suspend fun analyze(uri: Uri, options: AnalysisOptions): Result<AudioAnalysisResult> {
            callCount++
            return result
        }
    }
}
