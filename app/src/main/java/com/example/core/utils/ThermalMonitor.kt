package com.example.core.utils

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import android.os.PowerManager
import java.util.Locale

/**
 * Thermal Status Levels representing device thermal state during video encoding.
 */
enum class ThermalStatusLevel(
    val label: String,
    val isWarning: Boolean,
    val maxRecommendedFps: Int,
    val bitrateScaleFactor: Float
) {
    NORMAL("Normal", false, 60, 1.0f),
    WARM("Warm", false, 60, 0.9f),
    MODERATE("Moderate Throttling", true, 30, 0.75f),
    SEVERE("Severe Overheating", true, 30, 0.50f),
    CRITICAL("Critical Thermal Warning", true, 15, 0.30f)
}

/**
 * Detailed thermal telemetry state object.
 */
data class ThermalInfo(
    val level: ThermalStatusLevel = ThermalStatusLevel.NORMAL,
    val temperatureCelsius: Float = 36.5f,
    val isThrottling: Boolean = false,
    val warningMessage: String? = null,
    val mitigationSuggestion: String? = null
)

/**
 * Hardware Thermal Monitoring Utility
 * Detects device thermal state via system PowerManager.getCurrentThermalStatus() and Battery thermal sensors.
 */
object ThermalMonitor {

    /**
     * Obtains current system thermal status using PowerManager.getCurrentThermalStatus() API (Android 10+ / API 29+).
     * Returns an integer constant corresponding to PowerManager.THERMAL_STATUS_*
     */
    fun getCurrentThermalStatus(context: Context): Int {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
                powerManager?.currentThermalStatus ?: PowerManager.THERMAL_STATUS_NONE
            } catch (e: Throwable) {
                PowerManager.THERMAL_STATUS_NONE
            }
        } else {
            0
        }
    }

    /**
     * Reads current battery temperature in Celsius.
     */
    fun getDeviceTemperatureCelsius(context: Context): Float {
        return try {
            val intentFilter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
            val batteryStatus: Intent? = context.registerReceiver(null, intentFilter)
            val tempRaw = batteryStatus?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) ?: 0
            if (tempRaw > 0) tempRaw / 10.0f else 36.5f
        } catch (e: Exception) {
            36.5f
        }
    }

    /**
     * Evaluates thermal status based on device temperature and system PowerManager thermal status code.
     */
    fun evaluateThermalStatus(tempCelsius: Float, systemStatusCode: Int = 0): ThermalInfo {
        val level = when {
            tempCelsius >= 48f || (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && systemStatusCode >= PowerManager.THERMAL_STATUS_CRITICAL) -> ThermalStatusLevel.CRITICAL
            tempCelsius >= 44f || (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && systemStatusCode == PowerManager.THERMAL_STATUS_SEVERE) -> ThermalStatusLevel.SEVERE
            tempCelsius >= 40f || (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && systemStatusCode == PowerManager.THERMAL_STATUS_MODERATE) -> ThermalStatusLevel.MODERATE
            tempCelsius >= 37f || (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && systemStatusCode == PowerManager.THERMAL_STATUS_LIGHT) -> ThermalStatusLevel.WARM
            else -> ThermalStatusLevel.NORMAL
        }

        val warning = when (level) {
            ThermalStatusLevel.CRITICAL -> "CRITICAL OVERHEATING! Video encoder thermal limit reached. Frame drops and stream stalling likely."
            ThermalStatusLevel.SEVERE -> "Severe device heating detected during stream encoding. Hardware thermal throttling active."
            ThermalStatusLevel.MODERATE -> "Device temperature elevated (${String.format(Locale.US, "%.1f", tempCelsius)}°C). Performance may degrade."
            ThermalStatusLevel.WARM -> null
            ThermalStatusLevel.NORMAL -> null
        }

        val mitigation = when (level) {
            ThermalStatusLevel.CRITICAL -> "Auto-reducing encoding bitrate by 50% and capping FPS to 15 to prevent shutdown."
            ThermalStatusLevel.SEVERE -> "Recommend reducing stream bitrate or resolution to prevent frame drops."
            ThermalStatusLevel.MODERATE -> "Close background apps or lower spectrum overlay complexity."
            ThermalStatusLevel.WARM -> null
            ThermalStatusLevel.NORMAL -> null
        }

        return ThermalInfo(
            level = level,
            temperatureCelsius = tempCelsius,
            isThrottling = level.isWarning,
            warningMessage = warning,
            mitigationSuggestion = mitigation
        )
    }

    /**
     * Obtains comprehensive current thermal telemetry info using PowerManager.getCurrentThermalStatus().
     */
    fun getThermalInfo(context: Context): ThermalInfo {
        val statusCode = getCurrentThermalStatus(context)
        val tempCelsius = getDeviceTemperatureCelsius(context)
        return evaluateThermalStatus(tempCelsius, statusCode)
    }

    /**
     * Registers a listener for system PowerManager thermal status changes (Android 10+).
     */
    fun registerThermalListener(context: Context, onThermalChanged: (ThermalInfo) -> Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
                val executor = androidx.core.content.ContextCompat.getMainExecutor(context)
                powerManager?.addThermalStatusListener(executor) { status ->
                    val temp = getDeviceTemperatureCelsius(context)
                    val info = evaluateThermalStatus(temp, status)
                    onThermalChanged(info)
                }
            } catch (e: Throwable) {
                // Safely handle thermal listener registration failure
            }
        }
    }
}

