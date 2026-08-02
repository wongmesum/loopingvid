package com.example.core.media

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.util.Base64
import com.example.core.ffmpeg.FFmpegWrapperImpl
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query
import timber.log.Timber
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.concurrent.TimeUnit

@JsonClass(generateAdapter = true)
data class GenerateContentRequest(
    val contents: List<Content>,
    val generationConfig: GenerationConfig? = null,
    val systemInstruction: Content? = null
)

@JsonClass(generateAdapter = true)
data class Content(
    val parts: List<Part>
)

@JsonClass(generateAdapter = true)
data class Part(
    val text: String? = null,
    val inlineData: InlineData? = null
)

@JsonClass(generateAdapter = true)
data class InlineData(
    val mimeType: String,
    val data: String
)

@JsonClass(generateAdapter = true)
data class GenerationConfig(
    val temperature: Float? = null,
    val topP: Float? = null,
    val topK: Int? = null,
    val responseModalities: List<String>? = null
)

@JsonClass(generateAdapter = true)
data class GenerateContentResponse(
    val candidates: List<Candidate>? = null,
    val error: ErrorResponse? = null
)

@JsonClass(generateAdapter = true)
data class ErrorResponse(
    val message: String? = null
)

@JsonClass(generateAdapter = true)
data class Candidate(
    val content: Content
)

interface GeminiApiService {
    @POST("v1beta/models/gemini-2.5-flash-native-audio-preview-12-2025:generateContent")
    suspend fun generateContent(
        @Query("key") apiKey: String,
        @Body request: GenerateContentRequest
    ): GenerateContentResponse
}

object RetrofitClient {
    private const val BASE_URL = "https://generativelanguage.googleapis.com/"

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(120, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(120, TimeUnit.SECONDS)
        .build()

    val service: GeminiApiService by lazy {
        val moshi = Moshi.Builder()
            .add(KotlinJsonAdapterFactory())
            .build()
        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
        retrofit.create(GeminiApiService::class.java)
    }
}

class GeminiCaptionGenerator(private val context: Context) {
    suspend fun generateCaptionsForVideo(videoUri: String, apiKey: String): String = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) {
            return@withContext "Error: Gemini API Key is missing. Please configure it in Settings."
        }
        
        var tempAudioFile: File? = null
        try {
            // Extract audio using FFmpeg
            tempAudioFile = File(context.cacheDir, "temp_audio_${System.currentTimeMillis()}.mp3")
            val cmd = "-y -i $videoUri -vn -acodec libmp3lame -ar 16000 -ac 1 -b:a 32k ${tempAudioFile.absolutePath}"
            
            Timber.d("Extracting audio for Gemini: $cmd")
            val wrapper = FFmpegWrapperImpl(context)
            val result = wrapper.execute(cmd)
            
            if (!tempAudioFile.exists()) {
                return@withContext "Error: Failed to extract audio from video for transcription."
            }
            
            val audioBytes = tempAudioFile.readBytes()
            val base64Audio = Base64.encodeToString(audioBytes, Base64.NO_WRAP)
            
            val prompt = """
                Please analyze this audio track and generate synchronized text captions (subtitles).
                Provide the output strictly in standard SRT format. Ensure timestamps are accurate.
                Do not include markdown blocks like ```srt or any conversational text, just the raw SRT content.
            """.trimIndent()
            
            val request = GenerateContentRequest(
                contents = listOf(
                    Content(
                        parts = listOf(
                            Part(text = prompt),
                            Part(inlineData = InlineData(mimeType = "audio/mp3", data = base64Audio))
                        )
                    )
                ),
                generationConfig = GenerationConfig(temperature = 0.2f)
            )
            
            Timber.d("Sending audio to Gemini API (size: ${audioBytes.size} bytes)...")
            val response = RetrofitClient.service.generateContent(apiKey, request)
            
            if (response.error != null) {
                 return@withContext "API Error: ${response.error.message}"
            }
            
            val text = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
            return@withContext text?.trim() ?: "No captions generated."
            
        } catch (e: Exception) {
            Timber.e(e, "Error generating captions with Gemini")
            return@withContext "Error: ${e.message}"
        } finally {
            tempAudioFile?.delete()
        }
    }
}
