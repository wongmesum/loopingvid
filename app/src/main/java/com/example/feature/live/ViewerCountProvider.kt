package com.example.feature.live

import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import timber.log.Timber
import java.util.concurrent.TimeUnit

/**
 * Supplies the current concurrent viewer count for a live broadcast.
 *
 * Viewer counts are NOT part of the RTMP protocol; they can only be obtained from each
 * streaming platform's own data API. Implementations wrap a specific platform API. When a
 * platform does not expose viewer data (e.g. TikTok has no public API, or a custom RTMP
 * server), the provider returns null so the UI can hide/placeholder the value honestly.
 */
interface ViewerCountProvider {
    /**
     * @return the current concurrent viewer count, or null if unavailable/unsupported.
     */
    suspend fun getConcurrentViewers(): Int?
}

/**
 * A provider used when viewer telemetry is not available for the selected platform
 * (TikTok, custom RTMP, or when no credentials are configured). Always returns null.
 */
object NoOpViewerCountProvider : ViewerCountProvider {
    override suspend fun getConcurrentViewers(): Int? = null
}

// ---- YouTube Data API v3 models (only the fields we need) ----

@JsonClass(generateAdapter = true)
data class YouTubeVideoListResponse(
    val items: List<YouTubeVideoItem>? = null,
    val error: YouTubeErrorResponse? = null
)

@JsonClass(generateAdapter = true)
data class YouTubeVideoItem(
    val id: String? = null,
    val liveStreamingDetails: YouTubeLiveStreamingDetails? = null
)

@JsonClass(generateAdapter = true)
data class YouTubeLiveStreamingDetails(
    // YouTube returns this as a string; null when the broadcast is not live.
    val concurrentViewers: String? = null
)

@JsonClass(generateAdapter = true)
data class YouTubeErrorResponse(
    val code: Int? = null,
    val message: String? = null
)

interface YouTubeDataApiService {
    /**
     * Fetches liveStreamingDetails for a video id. For a live broadcast, the response
     * contains liveStreamingDetails.concurrentViewers.
     */
    @GET("youtube/v3/videos")
    suspend fun getVideoLiveDetails(
        @Query("part") part: String = "liveStreamingDetails",
        @Query("id") videoId: String,
        @Query("key") apiKey: String
    ): YouTubeVideoListResponse
}

/**
 * Real viewer count provider backed by the YouTube Data API v3. Uses a simple API key
 * (sufficient for reading public live broadcast details) rather than full OAuth.
 *
 * Requirements supplied by the user:
 *  - [apiKey]: a YouTube Data API v3 key from a Google Cloud project.
 *  - [videoId]: the id of the active live broadcast video.
 */
class YouTubeViewerCountProvider(
    private val apiKey: String,
    private val videoId: String
) : ViewerCountProvider {

    override suspend fun getConcurrentViewers(): Int? = withContext(Dispatchers.IO) {
        if (apiKey.isBlank() || videoId.isBlank()) return@withContext null
        try {
            val response = service.getVideoLiveDetails(videoId = videoId, apiKey = apiKey)
            if (response.error != null) {
                Timber.w("YouTube API error ${response.error.code}: ${response.error.message}")
                return@withContext null
            }
            val viewers = response.items
                ?.firstOrNull()
                ?.liveStreamingDetails
                ?.concurrentViewers
                ?.toIntOrNull()
            viewers
        } catch (t: Throwable) {
            Timber.w(t, "Failed to fetch YouTube concurrent viewers")
            null
        }
    }

    companion object {
        private const val BASE_URL = "https://www.googleapis.com/"

        private val okHttpClient by lazy {
            OkHttpClient.Builder()
                .connectTimeout(20, TimeUnit.SECONDS)
                .readTimeout(20, TimeUnit.SECONDS)
                .build()
        }

        private val service: YouTubeDataApiService by lazy {
            val moshi = Moshi.Builder()
                .add(KotlinJsonAdapterFactory())
                .build()
            Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(okHttpClient)
                .addConverterFactory(MoshiConverterFactory.create(moshi))
                .build()
                .create(YouTubeDataApiService::class.java)
        }
    }
}
