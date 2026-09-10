package com.example.core.ffmpeg

import android.content.Context
import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.FFmpegKitConfig
import com.arthenica.ffmpegkit.Level
import com.arthenica.ffmpegkit.ReturnCode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import timber.log.Timber
import kotlin.coroutines.resume

/**
 * FFmpeg engine wrapper backed by FFmpegKit (io.github.maitrungduc1410 fork).
 *
 * Unlike the previous 32-bit-only engine, FFmpegKit ships native libraries for
 * arm64-v8a/armeabi-v7a/x86/x86_64, so real encoding works on modern arm64 devices.
 *
 * All engine calls are wrapped in Throwable catch blocks so an unexpected native/link
 * error degrades to a non-zero exit code instead of crashing the app.
 */
class FFmpegWrapperImpl(private val context: Context) : FFmpegWrapper {

    companion object {
        private const val TAG = "FFmpegWrapperImpl"
    }

    private val _logFlow = MutableSharedFlow<String>(extraBufferCapacity = 256)
    override val logFlow: Flow<String> = _logFlow.asSharedFlow()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    // Accumulates the raw output of the most recent execution for structured parsing
    // (e.g. loudnorm JSON). Guarded via synchronized(outputLogBuffer).
    private val outputLogBuffer = StringBuilder()

    @Volatile
    private var isSupported: Boolean = false

    @Volatile
    private var initialized: Boolean = false

    init {
        safeInitialize()
    }

    @Volatile
    private var initErrorMessage: String? = null

    @Synchronized
    private fun safeInitialize() {
        if (initialized) return
        try {
            // Touching FFmpegKitConfig forces the native library to load. If the library is
            // missing for this ABI it throws here and we mark the engine unsupported.
            FFmpegKitConfig.setLogLevel(Level.AV_LOG_INFO)
            isSupported = true
            initErrorMessage = null
            Timber.i("FFmpegKit engine initialized successfully")
        } catch (t: Throwable) {
            // Handles UnsatisfiedLinkError / NoClassDefFoundError on unsupported setups.
            Timber.e(t, "FFmpegKit native initialization failed; engine unsupported")
            isSupported = false
            // Capture the real cause (e.g. the missing symbol in an UnsatisfiedLinkError) so it can
            // be surfaced in the on-screen diagnostics instead of a bare "not supported".
            initErrorMessage = "${t.javaClass.simpleName}: ${t.message ?: "no message"}" +
                (t.cause?.let { " | cause: ${it.javaClass.simpleName}: ${it.message}" } ?: "")
        } finally {
            initialized = true
        }
    }

    override fun getInitError(): String? {
        safeInitialize()
        return initErrorMessage
    }

    override fun isNativeSupported(): Boolean {
        safeInitialize()
        return isSupported
    }

    override fun getVersion(): String {
        return try {
            "FFmpegKit ${FFmpegKitConfig.getVersion()}"
        } catch (t: Throwable) {
            "FFmpegKit (version unavailable)"
        }
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
        if (!isSupported) {
            Timber.e("FFmpegKit engine is not supported on this device")
            _logFlow.emit("Error: Native FFmpeg engine is not supported on this device's architecture.")
            return@withContext 1
        }

        // Reset captured output so getLastOutputLog() reflects only this execution.
        synchronized(outputLogBuffer) { outputLogBuffer.setLength(0) }

        val cmdString = commandArgs.joinToString(" ")
        Timber.i("Executing FFmpeg command: ffmpeg $cmdString")
        _logFlow.emit("Executing: ffmpeg $cmdString")

        // Resolve the total media duration (in ms) so statistics time can be mapped to a percent.
        val totalDurationMs = resolveTargetDurationMs(commandArgs)

        try {
            // Run FFmpeg ASYNCHRONOUSLY with a real-time StatisticsCallback, instead of the
            // blocking executeWithArguments(). The blocking call only exposes session.statistics
            // AFTER the whole command finishes, so a short render (e.g. a 3-6s loop) reports just
            // one or two statistics entries at completion time -- the UI sees progress jump
            // straight from 0% to ~50%/100% instead of advancing smoothly. Async + statisticsCallback
            // delivers progress updates while FFmpeg is actually running.
            val returnCode = suspendCancellableCoroutine<ReturnCode?> { cont ->
                val session = FFmpegKit.executeWithArgumentsAsync(
                    commandArgs.toTypedArray(),
                    { completedSession ->
                        // Capture the full console output for structured parsing (e.g. loudnorm JSON).
                        try {
                            val logs = completedSession?.output
                            if (!logs.isNullOrBlank()) {
                                synchronized(outputLogBuffer) { outputLogBuffer.append(logs) }
                            }
                        } catch (t: Throwable) {
                            Timber.w(t, "Failed to capture FFmpegKit session output")
                        }
                        if (cont.isActive) cont.resume(completedSession?.returnCode)
                    },
                    { /* per-line log callback: already captured via session.output above */ },
                    { stats ->
                        if (totalDurationMs > 0 && stats != null) {
                            val processedMs = stats.time
                            if (processedMs > 0) {
                                val percent = ((processedMs / totalDurationMs.toDouble()) * 100.0)
                                    .toInt().coerceIn(0, 99) // reserve 100 for actual completion
                                scope.launch { onProgress(percent) }
                            }
                        }
                    }
                )
                cont.invokeOnCancellation {
                    try { FFmpegKit.cancel(session.sessionId) } catch (_: Throwable) {}
                }
            }

            return@withContext if (ReturnCode.isSuccess(returnCode)) {
                scope.launch { onProgress(100) }
                Timber.i("FFmpeg command finished successfully.")
                _logFlow.tryEmit("FFmpeg [success]: Command completed with exit code 0.")
                0
            } else if (ReturnCode.isCancel(returnCode)) {
                Timber.w("FFmpeg command was cancelled.")
                _logFlow.tryEmit("FFmpeg [cancelled]: Command was cancelled.")
                1
            } else {
                val code = returnCode?.value ?: -1
                Timber.e("FFmpeg execution failed with return code %d", code)
                _logFlow.tryEmit("FFmpeg [error]: Command failed with return code $code.")
                1
            }
        } catch (t: Throwable) {
            Timber.e(t, "Uncaught error during FFmpeg execution")
            _logFlow.tryEmit("FFmpeg [error]: Execution failed due to system/native error.")
            return@withContext 1
        }
    }

    /**
     * Determines the target duration in milliseconds used for progress reporting.
     * Prefers an explicit "-t <seconds>" argument, then falls back to probing the input's
     * duration via FFmpegKit's media information API.
     */
    private fun resolveTargetDurationMs(commandArgs: List<String>): Long {
        val tIndex = commandArgs.indexOf("-t")
        if (tIndex != -1 && tIndex + 1 < commandArgs.size) {
            val seconds = commandArgs[tIndex + 1].toDoubleOrNull()
            if (seconds != null && seconds > 0) return (seconds * 1000).toLong()
        }

        val iIndex = commandArgs.indexOf("-i")
        if (iIndex != -1 && iIndex + 1 < commandArgs.size) {
            val inputPath = commandArgs[iIndex + 1]
            try {
                val info = com.arthenica.ffmpegkit.FFprobeKit.getMediaInformation(inputPath)
                val durationStr = info?.mediaInformation?.duration
                val durationSec = durationStr?.toDoubleOrNull()
                if (durationSec != null && durationSec > 0) return (durationSec * 1000).toLong()
            } catch (t: Throwable) {
                Timber.w(t, "Could not probe media duration for progress reporting")
            }
        }
        return 0L
    }

    override fun cancel() {
        Timber.w("FFmpeg cancel requested")
        try {
            FFmpegKit.cancel()
        } catch (t: Throwable) {
            Timber.e(t, "Failed to cancel FFmpegKit sessions safely")
        }
    }

    override fun getLastOutputLog(): String {
        return synchronized(outputLogBuffer) { outputLogBuffer.toString() }
    }

    override fun release() {
        cancel()
        try {
            scope.cancel()
        } catch (t: Throwable) {
            Timber.e(t, "Failed to cancel FFmpeg wrapper scope")
        }
    }
}
