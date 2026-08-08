package com.example.core.audio

import android.content.Context
import android.net.Uri
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import timber.log.Timber

private val Context.audioAnalysisDataStore: DataStore<Preferences> by
    preferencesDataStore(name = "audio_analysis_cache")

/**
 * Caches [AudioAnalysisResult] per audio source so repeated opens of the same
 * track skip the decode pass. Analysis itself is delegated to an [AudioAnalyzer],
 * and the [DataStore] is injectable so tests can run against a temp file.
 */
class AudioAnalysisRepository(
    private val dataStore: DataStore<Preferences>,
    private val analyzer: AudioAnalyzer
) {

    constructor(context: Context) : this(
        context.audioAnalysisDataStore,
        AudioAnalysisService(context)
    )

    private val resultAdapter = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()
        .adapter(AudioAnalysisResult::class.java)

    /**
     * Returns a cached result when one exists for this [uri] and [options],
     * otherwise runs the analyzer and stores the outcome. Cache read/write
     * failures never fail the call — analysis still returns its own result.
     */
    suspend fun getOrAnalyze(
        uri: Uri,
        options: AnalysisOptions = AnalysisOptions()
    ): Result<AudioAnalysisResult> {
        val cacheKey = cacheKeyFor(uri, options)

        readCached(cacheKey)?.let { return Result.success(it) }

        return analyzer.analyze(uri, options).onSuccess { result ->
            writeCached(cacheKey, result)
        }
    }

    suspend fun getCached(uri: Uri, options: AnalysisOptions = AnalysisOptions()): AudioAnalysisResult? =
        readCached(cacheKeyFor(uri, options))

    suspend fun clearCache() {
        runCatching { dataStore.edit { it.clear() } }
            .onFailure { Timber.e(it, "Failed to clear audio analysis cache") }
    }

    private suspend fun readCached(cacheKey: String): AudioAnalysisResult? = try {
        val json = dataStore.data.first()[stringPreferencesKey(cacheKey)]
        json?.let { resultAdapter.fromJson(it) }
    } catch (e: CancellationException) {
        // Must propagate: swallowing it would let a cancelled caller continue.
        throw e
    } catch (e: Exception) {
        Timber.e(e, "Failed to read cached analysis")
        null
    }

    private suspend fun writeCached(cacheKey: String, result: AudioAnalysisResult) {
        try {
            val json = resultAdapter.toJson(result)
            dataStore.edit { it[stringPreferencesKey(cacheKey)] = json }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Timber.e(e, "Failed to persist cached analysis")
        }
    }

    private fun cacheKeyFor(uri: Uri, options: AnalysisOptions): String =
        "analysis_v${ANALYSIS_ALGORITHM_VERSION}_${uri}_${options.hashCode()}"

    companion object {
        /**
         * Bump this whenever waveform/spectrum/BPM/loudness math changes: entries
         * written by an older algorithm would otherwise be served forever.
         */
        const val ANALYSIS_ALGORITHM_VERSION = 1
    }
}
