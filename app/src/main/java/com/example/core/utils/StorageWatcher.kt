package com.example.core.utils

import android.content.Context
import android.os.Environment
import android.os.StatFs
import java.io.File

/**
 * Detailed storage status information.
 */
data class StorageInfo(
    val availableBytes: Long,
    val totalBytes: Long,
    val availableMb: Long,
    val isLowStorage: Boolean,
    val isCriticalStorage: Boolean,
    val warningMessage: String?
)

/**
 * StorageWatcher Utility
 * Calculates available internal disk space and evaluates low-storage warnings for stream recording & local caching.
 */
object StorageWatcher {

    const val CRITICAL_THRESHOLD_MB = 500L
    const val WARNING_THRESHOLD_MB = 1500L

    /**
     * Inspects available internal storage space.
     */
    fun getStorageInfo(context: Context, thresholdMb: Long = CRITICAL_THRESHOLD_MB): StorageInfo {
        return try {
            val path: File = context.filesDir ?: Environment.getDataDirectory()
            val stat = StatFs(path.path)
            val availableBytes = stat.availableBytes
            val totalBytes = stat.totalBytes
            val availableMb = availableBytes / (1024 * 1024)

            val isCritical = availableMb < thresholdMb
            val isWarning = availableMb < WARNING_THRESHOLD_MB

            val warningMsg = when {
                isCritical -> "CRITICAL STORAGE ALERT: Only ${formatMb(availableMb)} free on internal storage (< ${thresholdMb}MB). Local recording and cache dumps may fail!"
                isWarning -> "Low Storage Warning: ${formatMb(availableMb)} remaining. Consider clearing local clip cache."
                else -> null
            }

            StorageInfo(
                availableBytes = availableBytes,
                totalBytes = totalBytes,
                availableMb = availableMb,
                isLowStorage = isWarning,
                isCriticalStorage = isCritical,
                warningMessage = warningMsg
            )
        } catch (e: Exception) {
            StorageInfo(
                availableBytes = 10 * 1024 * 1024 * 1024L,
                totalBytes = 64 * 1024 * 1024 * 1024L,
                availableMb = 10240L,
                isLowStorage = false,
                isCriticalStorage = false,
                warningMessage = null
            )
        }
    }

    private fun formatMb(mb: Long): String {
        return if (mb >= 1024) {
            "%.2f GB".format(mb / 1024.0)
        } else {
            "$mb MB"
        }
    }
}
