package com.example.core.utils

import android.app.ActivityManager
import android.content.Context
import android.media.MediaCodecInfo
import android.media.MediaCodecList
import android.os.Build
import android.os.Environment
import android.os.StatFs
import com.example.core.ui.BatteryInfo
import java.util.Locale

private const val BYTES_PER_KIB = 1024.0

data class CodecSupport(
    val name: String,
    val mimeType: String,
    val isHardwareAccelerated: Boolean,
    val maxResolution: String
)

data class DeviceCapabilities(
    val deviceName: String,
    val osVersion: String,
    val sdkInt: Int,
    val totalMemoryBytes: Long,
    val availableMemoryBytes: Long,
    val maxRuntimeMemoryBytes: Long,
    val freeRuntimeMemoryBytes: Long,
    val totalStorageBytes: Long,
    val availableStorageBytes: Long,
    val videoEncoders: List<CodecSupport>
)

object DeviceCapabilityDetector {

    fun detectCapabilities(context: Context): DeviceCapabilities {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
        val memoryInfo = ActivityManager.MemoryInfo()
        activityManager?.getMemoryInfo(memoryInfo)

        val runtime = Runtime.getRuntime()
        val storage = StatFs(Environment.getDataDirectory().absolutePath)

        return DeviceCapabilities(
            deviceName = listOf(Build.MANUFACTURER, Build.MODEL)
                .filter { it.isNotBlank() }
                .joinToString(" "),
            osVersion = Build.VERSION.RELEASE ?: "Unknown",
            sdkInt = Build.VERSION.SDK_INT,
            totalMemoryBytes = memoryInfo.totalMem,
            availableMemoryBytes = memoryInfo.availMem,
            maxRuntimeMemoryBytes = runtime.maxMemory(),
            freeRuntimeMemoryBytes = runtime.freeMemory(),
            totalStorageBytes = storage.blockCountLong * storage.blockSizeLong,
            availableStorageBytes = storage.availableBlocksLong * storage.blockSizeLong,
            videoEncoders = queryVideoEncoders()
        )
    }

    fun queryVideoEncoders(): List<CodecSupport> {
        return try {
            MediaCodecList(MediaCodecList.REGULAR_CODECS).codecInfos
                .filter { it.isEncoder }
                .flatMap { codecInfo -> codecInfo.toVideoCodecSupport() }
                .sortedWith(compareBy<CodecSupport> { it.mimeType }.thenBy { it.name })
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun inferHardwareAccelerated(codecName: String): Boolean {
        val normalizedName = codecName.lowercase(Locale.US)
        return !normalizedName.startsWith("omx.google.") &&
            !normalizedName.startsWith("c2.android.") &&
            !normalizedName.contains("software") &&
            !normalizedName.contains("sw")
    }

    fun formatBytes(bytes: Long): String {
        if (bytes <= 0L) return "0 B"

        val units = listOf("B", "KB", "MB", "GB", "TB")
        var value = bytes.toDouble()
        var unitIndex = 0

        while (value >= BYTES_PER_KIB && unitIndex < units.lastIndex) {
            value /= BYTES_PER_KIB
            unitIndex++
        }

        return if (unitIndex == 0) {
            String.format(Locale.US, "%.0f %s", value, units[unitIndex])
        } else {
            String.format(Locale.US, "%.1f %s", value, units[unitIndex])
        }
    }

    fun buildDiagnosticReport(
        capabilities: DeviceCapabilities,
        thermalInfo: ThermalInfo,
        batteryInfo: BatteryInfo
    ): String {
        val encoders = capabilities.videoEncoders.joinToString(separator = "\n") { codec ->
            val acceleration = if (codec.isHardwareAccelerated) "hardware" else "software"
            "- ${codec.mimeType}: ${codec.name} (${acceleration}, ${codec.maxResolution})"
        }.ifBlank { "- No video encoders detected" }

        return """
            LoopingVid Device Diagnostics

            Device
            - Name: ${capabilities.deviceName}
            - Android: ${capabilities.osVersion} (SDK ${capabilities.sdkInt})

            Thermal
            - Level: ${thermalInfo.level.label}
            - Temperature: ${String.format(Locale.US, "%.1f", thermalInfo.temperatureCelsius)}°C
            - Throttling: ${thermalInfo.isThrottling}
            - Recommended FPS: ${thermalInfo.level.maxRecommendedFps}
            - Bitrate scale: ${String.format(Locale.US, "%.2f", thermalInfo.level.bitrateScaleFactor)}

            Battery
            - Level: ${batteryInfo.percentage}%
            - Charging: ${batteryInfo.isCharging}
            - Plug: ${batteryInfo.chargePlug}
            - Health: ${batteryInfo.health}
            - Temperature: ${String.format(Locale.US, "%.1f", batteryInfo.temperatureCelsius)}°C
            - Voltage: ${batteryInfo.voltageMv} mV

            Memory
            - Total device memory: ${formatBytes(capabilities.totalMemoryBytes)}
            - Available device memory: ${formatBytes(capabilities.availableMemoryBytes)}
            - Max app heap: ${formatBytes(capabilities.maxRuntimeMemoryBytes)}
            - Free app heap: ${formatBytes(capabilities.freeRuntimeMemoryBytes)}

            Storage
            - Total internal storage: ${formatBytes(capabilities.totalStorageBytes)}
            - Available internal storage: ${formatBytes(capabilities.availableStorageBytes)}

            Video Encoders
            $encoders
        """.trimIndent()
    }

    private fun MediaCodecInfo.toVideoCodecSupport(): List<CodecSupport> {
        return supportedTypes
            .filter { it.startsWith("video/") }
            .map { mimeType ->
                CodecSupport(
                    name = name,
                    mimeType = mimeType,
                    isHardwareAccelerated = isCodecHardwareAccelerated(this),
                    maxResolution = resolveMaxResolution(this, mimeType)
                )
            }
    }

    private fun isCodecHardwareAccelerated(codecInfo: MediaCodecInfo): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            codecInfo.isHardwareAccelerated
        } else {
            inferHardwareAccelerated(codecInfo.name)
        }
    }

    private fun resolveMaxResolution(codecInfo: MediaCodecInfo, mimeType: String): String {
        return try {
            val capabilities = codecInfo.getCapabilitiesForType(mimeType)
            val videoCapabilities = capabilities.videoCapabilities ?: return "Unknown"
            val maxWidth = videoCapabilities.supportedWidths.upper
            val maxHeight = videoCapabilities.supportedHeights.upper
            "${maxWidth}x${maxHeight}"
        } catch (e: Exception) {
            "Unknown"
        }
    }
}
