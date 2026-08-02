package com.example.core.ui

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext

data class BatteryInfo(
    val percentage: Int = 100,
    val isCharging: Boolean = false,
    val chargePlug: String = "Discharging",
    val health: String = "Good",
    val temperatureCelsius: Float = 28.0f,
    val voltageMv: Int = 3800,
    val isLowBattery: Boolean = false,
    val isCriticalBattery: Boolean = false,
    val warningMessage: String? = null
)

object BatteryStatusMonitor {

    fun getBatteryInfo(context: Context): BatteryInfo {
        return try {
            val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
            val batteryIntent = context.registerReceiver(null, filter) ?: return BatteryInfo()
            parseBatteryIntent(batteryIntent)
        } catch (e: Exception) {
            BatteryInfo()
        }
    }

    fun evaluateBatteryInfo(
        level: Int,
        scale: Int,
        status: Int,
        chargePlugCode: Int,
        healthCode: Int,
        tempCelsius: Float,
        voltageMv: Int
    ): BatteryInfo {
        val percentage = if (level >= 0 && scale > 0) {
            ((level.toFloat() / scale.toFloat()) * 100f).toInt()
        } else {
            100
        }

        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                status == BatteryManager.BATTERY_STATUS_FULL

        val chargePlug = when (chargePlugCode) {
            BatteryManager.BATTERY_PLUGGED_AC -> "AC Power"
            BatteryManager.BATTERY_PLUGGED_USB -> "USB Port"
            BatteryManager.BATTERY_PLUGGED_WIRELESS -> "Wireless Dock"
            else -> if (isCharging) "Power In" else "Discharging"
        }

        val health = when (healthCode) {
            BatteryManager.BATTERY_HEALTH_GOOD -> "Good"
            BatteryManager.BATTERY_HEALTH_OVERHEAT -> "Overheat Warning"
            BatteryManager.BATTERY_HEALTH_DEAD -> "Dead / Damaged"
            BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE -> "Over Voltage"
            BatteryManager.BATTERY_HEALTH_COLD -> "Cold"
            else -> "Normal"
        }

        val isCritical = percentage <= 10 && !isCharging
        val isLow = (percentage <= 20 || isCritical) && !isCharging

        val warningMessage = when {
            isCritical -> "CRITICAL BATTERY LEVEL (${percentage}%): Stream encoding active. Connect AC power charger immediately to prevent shut off."
            isLow -> "LOW BATTERY ALERT (${percentage}%): Battery is discharging rapidly during live encoding. Connect power supply."
            healthCode == BatteryManager.BATTERY_HEALTH_OVERHEAT -> "BATTERY OVERHEAT WARNING: Battery temperature is high (${tempCelsius}°C)."
            else -> null
        }

        return BatteryInfo(
            percentage = percentage,
            isCharging = isCharging,
            chargePlug = chargePlug,
            health = health,
            temperatureCelsius = tempCelsius,
            voltageMv = voltageMv,
            isLowBattery = isLow,
            isCriticalBattery = isCritical,
            warningMessage = warningMessage
        )
    }

    fun parseBatteryIntent(intent: Intent): BatteryInfo {
        val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
        val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
        val chargePlugCode = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1)
        val healthCode = intent.getIntExtra(BatteryManager.EXTRA_HEALTH, -1)
        val tempTenths = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0)
        val tempCelsius = tempTenths / 10.0f
        val voltageMv = intent.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0)

        return evaluateBatteryInfo(
            level = level,
            scale = scale,
            status = status,
            chargePlugCode = chargePlugCode,
            healthCode = healthCode,
            tempCelsius = tempCelsius,
            voltageMv = voltageMv
        )
    }
}

@Composable
fun rememberBatteryStatus(): BatteryInfo {
    val context = LocalContext.current
    var batteryInfo by remember { mutableStateOf(BatteryStatusMonitor.getBatteryInfo(context)) }

    DisposableEffect(context) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                if (intent?.action == Intent.ACTION_BATTERY_CHANGED) {
                    intent.let {
                        batteryInfo = BatteryStatusMonitor.parseBatteryIntent(it)
                    }
                }
            }
        }
        val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        try {
            androidx.core.content.ContextCompat.registerReceiver(
                context,
                receiver,
                filter,
                androidx.core.content.ContextCompat.RECEIVER_EXPORTED
            )
        } catch (e: Exception) {
            try {
                context.registerReceiver(receiver, filter)
            } catch (_: Exception) {
                // Ignore receiver registration failures on restricted runtimes
            }
        }

        onDispose {
            try {
                context.unregisterReceiver(receiver)
            } catch (e: Exception) {
                // Ignore
            }
        }
    }

    return batteryInfo
}
