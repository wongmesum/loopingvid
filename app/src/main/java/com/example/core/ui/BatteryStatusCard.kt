package com.example.core.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun BatteryStatusCard(
    modifier: Modifier = Modifier,
    onBatteryAlertTriggered: (BatteryInfo) -> Unit = {}
) {
    val realBatteryInfo = rememberBatteryStatus()
    var simulatedLowBattery by remember { mutableStateOf(false) }

    val currentInfo = if (simulatedLowBattery) {
        BatteryInfo(
            percentage = 8,
            isCharging = false,
            chargePlug = "Discharging",
            health = "Good",
            temperatureCelsius = 39.5f,
            voltageMv = 3520,
            isLowBattery = true,
            isCriticalBattery = true,
            warningMessage = "CRITICAL BATTERY LEVEL (8%): Stream encoding active. Connect AC power charger immediately to prevent shut off."
        )
    } else {
        realBatteryInfo
    }

    LaunchedEffect(currentInfo.isLowBattery, currentInfo.isCriticalBattery) {
        if (currentInfo.isLowBattery || currentInfo.isCriticalBattery) {
            onBatteryAlertTriggered(currentInfo)
        }
    }

    val batteryColor = when {
        currentInfo.isCriticalBattery -> Color(0xFFEF4444)
        currentInfo.isLowBattery -> Color(0xFFF59E0B)
        currentInfo.isCharging -> Color(0xFF10B981)
        else -> MaterialTheme.colorScheme.primary
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("battery_status_card"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (currentInfo.isCharging) Icons.Default.BatteryChargingFull
                        else if (currentInfo.isLowBattery) Icons.Default.BatteryAlert
                        else Icons.Default.BatteryFull,
                        contentDescription = "Battery Status",
                        tint = batteryColor,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "Battery & Power Monitor",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Hardware power supply state & charge metrics",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Simulate Toggle Button
                OutlinedButton(
                    onClick = { simulatedLowBattery = !simulatedLowBattery },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.testTag("simulate_low_battery_button")
                ) {
                    Text(
                        text = if (simulatedLowBattery) "Reset Real" else "Simulate <10%",
                        fontSize = 10.sp
                    )
                }
            }

            // Warning Alert Banner (Visible when low or critical)
            AnimatedVisibility(
                visible = currentInfo.isLowBattery || currentInfo.isCriticalBattery,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (currentInfo.isCriticalBattery) Color(0xFFEF4444).copy(alpha = 0.15f)
                            else Color(0xFFF59E0B).copy(alpha = 0.15f)
                        )
                        .border(
                            width = 1.dp,
                            color = if (currentInfo.isCriticalBattery) Color(0xFFEF4444) else Color(0xFFF59E0B),
                            shape = RoundedCornerShape(12.dp)
                        )
                        .padding(12.dp)
                        .testTag("battery_warning_banner")
                ) {
                    Row(verticalAlignment = Alignment.Top) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Low Battery Alert",
                            tint = if (currentInfo.isCriticalBattery) Color(0xFFEF4444) else Color(0xFFF59E0B),
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = if (currentInfo.isCriticalBattery) "CRITICAL BATTERY WARNING" else "LOW BATTERY ALERT",
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                color = if (currentInfo.isCriticalBattery) Color(0xFFEF4444) else Color(0xFFF59E0B)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = currentInfo.warningMessage ?: "Device battery level is low. Connect power source.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            // Battery Level Bar & Percentage Display
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "${currentInfo.percentage}%",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = batteryColor
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        if (currentInfo.isCharging) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0xFF10B981).copy(alpha = 0.2f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Bolt,
                                        contentDescription = "Charging",
                                        tint = Color(0xFF10B981),
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(modifier = Modifier.width(2.dp))
                                    Text(
                                        text = currentInfo.chargePlug,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF10B981)
                                    )
                                }
                            }
                        } else {
                            Text(
                                text = "Discharging",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Text(
                        text = "${"%.1f".format(currentInfo.temperatureCelsius)}°C • ${currentInfo.voltageMv} mV",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Progress Bar
                LinearProgressIndicator(
                    progress = { currentInfo.percentage / 100f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = batteryColor,
                    trackColor = MaterialTheme.colorScheme.surface
                )
            }
        }
    }
}
