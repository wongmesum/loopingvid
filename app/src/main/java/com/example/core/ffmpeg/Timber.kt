package com.example.core.ffmpeg

import android.util.Log

/**
 * Timber logger facade for FFmpeg execution logging.
 * Delegates to android.util.Log for native Android system logging while providing Timber-compatible API.
 */
object Timber {
    fun tag(tag: String): Timber = this

    fun d(message: String, vararg args: Any?) {
        Log.d("FFmpegWrapper", format(message, *args))
    }

    fun i(message: String, vararg args: Any?) {
        Log.i("FFmpegWrapper", format(message, *args))
    }

    fun w(message: String, vararg args: Any?) {
        Log.w("FFmpegWrapper", format(message, *args))
    }

    fun e(message: String, t: Throwable? = null) {
        Log.e("FFmpegWrapper", message, t)
    }

    private fun format(message: String, vararg args: Any?): String {
        return if (args.isNotEmpty()) {
            try {
                String.format(message, *args)
            } catch (e: Exception) {
                message
            }
        } else {
            message
        }
    }
}
