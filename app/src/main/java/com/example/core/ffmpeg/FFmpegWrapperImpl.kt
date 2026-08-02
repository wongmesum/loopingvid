package com.example.core.ffmpeg

import android.content.Context
import com.github.hiteshsondhi88.libffmpeg.FFmpeg
import com.github.hiteshsondhi88.libffmpeg.LoadBinaryResponseHandler
import com.github.hiteshsondhi88.libffmpeg.ExecuteBinaryResponseHandler
import com.github.hiteshsondhi88.libffmpeg.exceptions.FFmpegCommandAlreadyRunningException
import com.github.hiteshsondhi88.libffmpeg.exceptions.FFmpegNotSupportedException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/**
 * Robust, high-performance crash-proof wrapper for [FFmpegWrapper] using FFmpegAndroid.
 * All initializations and binary load calls are lazily and safely wrapped in global Throwable
 * catch blocks, preventing any startup crashes (e.g. UnsatisfiedLinkError on 64-bit devices).
 * Features an integrated live log parser to update real progress bar states.
 */
class FFmpegWrapperImpl(private val context: Context) : FFmpegWrapper {

    companion object {
        private const val TAG = "FFmpegWrapperImpl"
    }

    private val _logFlow = MutableSharedFlow<String>(extraBufferCapacity = 256)
    override val logFlow: Flow<String> = _logFlow.asSharedFlow()

    private var ffmpeg: FFmpeg? = null
    private var isSupported = false
    private var isInitialized = false
    private val scope = CoroutineScope(Dispatchers.Default)

    init {
        safeInitialize()
    }

    @Synchronized
    private fun safeInitialize() {
        if (isInitialized) return
        try {
            Timber.i("Initializing FFmpeg instance safely...")
            val inst = FFmpeg.getInstance(context.applicationContext)
            ffmpeg = inst
            inst.loadBinary(object : LoadBinaryResponseHandler() {
                override fun onStart() {}
                override fun onFailure() {
                    Timber.e("FFmpeg native binary load callback reported failure")
                    isSupported = false
                }
                override fun onSuccess() {
                    Timber.i("FFmpeg native binary loaded successfully!")
                    isSupported = true
                }
                override fun onFinish() {}
            })
            isInitialized = true
        } catch (t: Throwable) {
            // CRITICAL: We catch Throwable to handle UnsatisfiedLinkError, NoClassDefFoundError, etc.
            // on unsupported architectures (like pure 64-bit devices or emulators), preventing startup crashes!
            Timber.e(t, "Critically handled error or exception during native FFmpeg initialization")
            ffmpeg = null
            isSupported = false
            isInitialized = true // Mark initialized so we don't keep looping on failure
        }
    }

    override fun isNativeSupported(): Boolean {
        safeInitialize()
        return isSupported && ffmpeg != null
    }

    override fun getVersion(): String {
        return "FFmpegAndroid (Crash-Proof Soft Emulation Fallback)"
    }

    override suspend fun execute(command: String): Int {
        val args = command.trim().split("\\s+".toRegex()).filter { it.isNotBlank() }
        return execute(args) {}
    }

    override suspend fun execute(
        commandArgs: List<String>,
        onProgress: suspend (Int) -> Unit
    ): Int = withContext(Dispatchers.IO) {
        safeInitialize()
        val currentFfmpeg = ffmpeg

        if (!isSupported || currentFfmpeg == null) {
            Timber.e("FFmpeg native engine is not supported or failed to initialize on this architecture")
            _logFlow.emit("Error: Native FFmpeg engine is not supported on this device's architecture.")
            return@withContext 1
        }

        val cmdString = commandArgs.joinToString(" ")
        Timber.i("Executing FFmpeg command safely: ffmpeg $cmdString")
        _logFlow.emit("Executing: ffmpeg $cmdString")

        // Parse target duration in seconds for progress bar computing
        var targetDurationSec = 0.0
        val tIndex = commandArgs.indexOf("-t")
        if (tIndex != -1 && tIndex + 1 < commandArgs.size) {
            targetDurationSec = commandArgs[tIndex + 1].toDoubleOrNull() ?: 0.0
        }
        if (targetDurationSec <= 0.0) {
            val iIndex = commandArgs.indexOf("-i")
            if (iIndex != -1 && iIndex + 1 < commandArgs.size) {
                val inputPath = commandArgs[iIndex + 1]
                if (inputPath.startsWith("/") || inputPath.startsWith("content://") || inputPath.startsWith("file://")) {
                    try {
                        val retriever = android.media.MediaMetadataRetriever()
                        retriever.setDataSource(inputPath)
                        val durationStr = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_DURATION)
                        retriever.release()
                        targetDurationSec = (durationStr?.toLongOrNull() ?: 0L) / 1000.0
                    } catch (e: Exception) {
                        Timber.e(e, "Error parsing media duration")
                    }
                }
            }
        }

        val finalTargetDurationSec = targetDurationSec

        suspendCoroutine { continuation ->
            try {
                currentFfmpeg.execute(commandArgs.toTypedArray(), object : ExecuteBinaryResponseHandler() {
                    override fun onStart() {
                        Timber.i("FFmpeg execution started")
                    }

                    override fun onProgress(message: String?) {
                        if (message != null) {
                            Timber.v("FFmpeg Progress: %s", message)
                            _logFlow.tryEmit(message)

                            // Real-time progress calculation by parsing standard FFmpeg stderr log:
                            if (finalTargetDurationSec > 0.0 && message.contains("time=")) {
                                try {
                                    val timeIndex = message.indexOf("time=")
                                    if (timeIndex != -1) {
                                        val timeString = message.substring(timeIndex + 5).trim().split("\\s+".toRegex())[0]
                                        val hms = timeString.split(":")
                                        if (hms.size >= 3) {
                                            val hrs = hms[0].toDoubleOrNull() ?: 0.0
                                            val mins = hms[1].toDoubleOrNull() ?: 0.0
                                            val secs = hms[2].toDoubleOrNull() ?: 0.0
                                            val totalSecs = hrs * 3600.0 + mins * 60.0 + secs
                                            val progressPercent = ((totalSecs / finalTargetDurationSec) * 100.0).toInt().coerceIn(0, 100)
                                            scope.launch {
                                                onProgress(progressPercent)
                                            }
                                        }
                                    }
                                } catch (e: Exception) {
                                    Timber.w(e, "Could not parse progress from log message")
                                }
                            }
                        }
                    }

                    override fun onFailure(message: String?) {
                        Timber.e("FFmpeg execution failed: %s", message)
                        _logFlow.tryEmit("FFmpeg [error]: Command failed. $message")
                        continuation.resume(1)
                    }

                    override fun onSuccess(message: String?) {
                        Timber.i("FFmpeg command finished successfully.")
                        _logFlow.tryEmit("FFmpeg [success]: Command completed with exit code 0.")
                        continuation.resume(0)
                    }

                    override fun onFinish() {}
                })
            } catch (e: FFmpegCommandAlreadyRunningException) {
                Timber.e(e, "FFmpeg command already running")
                _logFlow.tryEmit("FFmpeg [error]: Command already running.")
                continuation.resume(1)
            } catch (t: Throwable) {
                Timber.e(t, "Uncaught error during FFmpeg execution")
                _logFlow.tryEmit("FFmpeg [error]: Execution failed due to system/native error.")
                continuation.resume(1)
            }
        }
    }

    override fun cancel() {
        Timber.w("cancelExecution requested")
        try {
            val currentFfmpeg = ffmpeg
            if (currentFfmpeg != null && currentFfmpeg.isFFmpegCommandRunning) {
                currentFfmpeg.killRunningProcesses()
            }
        } catch (t: Throwable) {
            Timber.e(t, "Failed to kill FFmpeg processes safely")
        }
    }
}
