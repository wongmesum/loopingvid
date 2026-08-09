package com.example.core.ffmpeg

import android.content.Context
import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.FFmpegKitConfig
import com.arthenica.ffmpegkit.FFmpegSession
import com.arthenica.ffmpegkit.ReturnCode
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import timber.log.Timber
import kotlin.coroutines.resume

/**
 * FFmpegKit-backed implementation of [FFmpegWrapper].
 * Native initialization stays guarded so unsupported devices fail as an execution result.
 */
class FFmpegWrapperImpl(@Suppress("UNUSED_PARAMETER") private val context: Context) : FFmpegWrapper {

    companion object {
        private const val TAG = "FFmpegWrapperImpl"
    }

    private val _logFlow = MutableSharedFlow<String>(extraBufferCapacity = 256)
    override val logFlow: Flow<String> = _logFlow.asSharedFlow()

    private val scope = CoroutineScope(Dispatchers.Default)
    private var isSupported = false
    private var isInitialized = false
    private var activeSession: FFmpegSession? = null
    private val sessionLock = Any()

    init {
        safeInitialize()
    }

    @Synchronized
    private fun safeInitialize() {
        if (isInitialized) return

        try {
            val version = FFmpegKitConfig.getFFmpegVersion()
            isSupported = !version.isNullOrBlank()
            Timber.i("FFmpegKit initialized: %s", version)
        } catch (t: Throwable) {
            Timber.e(t, "Failed to initialize FFmpegKit native engine")
            isSupported = false
        } finally {
            isInitialized = true
        }
    }

    override fun isNativeSupported(): Boolean {
        safeInitialize()
        return isSupported
    }

    override fun getVersion(): String {
        return try {
            "FFmpegKit ${FFmpegKitConfig.getFFmpegVersion()}"
        } catch (t: Throwable) {
            Timber.e(t, "Failed to read FFmpegKit version")
            "FFmpegKit (unavailable)"
        }
    }

    override suspend fun execute(command: String): Int {
        val args = command.trim().split("\\s+".toRegex()).filter(String::isNotBlank)
        return execute(args)
    }

    override suspend fun execute(
        commandArgs: List<String>,
        onProgress: suspend (Int) -> Unit
    ): Int = withContext(Dispatchers.IO) {
        safeInitialize()
        if (!isSupported) {
            return@withContext failExecution("Native FFmpeg engine is not supported on this device")
        }

        // Arguments stay as an array end-to-end: FFmpegKit never re-parses them, so paths
        // containing spaces or quotes cannot split into extra arguments.
        val arguments = commandArgs.toTypedArray()
        Timber.i("Executing FFmpeg command safely: ffmpeg %s", commandArgs.joinToString(" "))
        _logFlow.emit("Executing: ffmpeg ${commandArgs.joinToString(" ")}")

        val targetDurationSec = findTargetDuration(commandArgs)
        suspendCancellableCoroutine { continuation ->
            try {
                val session = FFmpegKit.executeWithArgumentsAsync(
                    arguments,
                    { completedSession ->
                        synchronized(sessionLock) {
                            activeSession = null
                        }
                        val returnCode = completedSession.returnCode
                        if (ReturnCode.isSuccess(returnCode)) {
                            Timber.i("FFmpeg command finished successfully")
                            _logFlow.tryEmit("FFmpeg [success]: Command completed with exit code 0.")
                            if (continuation.isActive) continuation.resume(0)
                        } else {
                            val message = completedSession.failStackTrace ?: "exit code $returnCode"
                            Timber.e("FFmpeg execution failed: %s", message)
                            _logFlow.tryEmit("FFmpeg [error]: Command failed. $message")
                            if (continuation.isActive) continuation.resume(1)
                        }
                    },
                    { log ->
                        log.message?.let { message ->
                            _logFlow.tryEmit(message)
                            parseProgress(message, targetDurationSec)?.let { progress ->
                                scope.launch { onProgress(progress) }
                            }
                        }
                    },
                    { statistics ->
                        if (targetDurationSec > 0.0) {
                            val progress = ((statistics.time / 1000.0 / targetDurationSec) * 100.0)
                                .toInt()
                                .coerceIn(0, 100)
                            scope.launch { onProgress(progress) }
                        }
                    }
                )
                synchronized(sessionLock) {
                    activeSession = session
                }
                continuation.invokeOnCancellation {
                    cancel()
                }
            } catch (t: Throwable) {
                Timber.e(t, "Uncaught error during FFmpeg execution")
                _logFlow.tryEmit("FFmpeg [error]: Execution failed due to system/native error.")
                if (t !is CancellationException && continuation.isActive) {
                    continuation.resume(1)
                }
            }
        }
    }

    override fun cancel() {
        val sessionId = synchronized(sessionLock) { activeSession?.sessionId }
        if (sessionId == null) return

        try {
            Timber.w("Cancelling FFmpeg session: %s", sessionId)
            FFmpegKit.cancel(sessionId)
            synchronized(sessionLock) {
                activeSession = null
            }
        } catch (t: Throwable) {
            Timber.e(t, "Failed to cancel FFmpeg session safely")
        }
    }

    private fun failExecution(message: String): Int {
        Timber.e(message)
        _logFlow.tryEmit("FFmpeg [error]: $message")
        return 1
    }

    private fun findTargetDuration(commandArgs: List<String>): Double {
        val durationIndex = commandArgs.indexOf("-t")
        if (durationIndex >= 0 && durationIndex + 1 < commandArgs.size) {
            return commandArgs[durationIndex + 1].toDoubleOrNull() ?: 0.0
        }
        return 0.0
    }

    private fun parseProgress(message: String, targetDurationSec: Double): Int? {
        if (targetDurationSec <= 0.0) return null
        val timeValue = Regex("time=(\\d{2}):(\\d{2}):(\\d{2}(?:\\.\\d+)?)")
            .find(message)
            ?.groupValues
            ?: return null
        val totalSeconds = timeValue[1].toDouble() * 3600.0 +
            timeValue[2].toDouble() * 60.0 + timeValue[3].toDouble()
        return ((totalSeconds / targetDurationSec) * 100.0).toInt().coerceIn(0, 100)
    }
}
