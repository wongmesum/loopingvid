package com.example.feature.live

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
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
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.DeviceThermostat
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.utils.ThermalInfo
import com.example.core.utils.ThermalStatusLevel

/**
 * Thermal Monitoring Component
 * Detects device overheating during stream encoding and displays a warning notification
 * with one-tap encoder cool-down controls.
 */
@Composable
fun ThermalWarningBanner(
    thermalInfo: ThermalInfo,
    onCoolDownClick: () -> Unit,
    onSimulateTestClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val level = thermalInfo.level
    val temp = thermalInfo.temperatureCelsius

    val levelColor by animateColorAsState(
        targetValue = when (level) {
            ThermalStatusLevel.CRITICAL -> Color(0xFFEF4444) // Bright Red
            ThermalStatusLevel.SEVERE -> Color(0xFFDC2626) // Red
            ThermalStatusLevel.MODERATE -> Color(0xFFF59E0B) // Amber
            ThermalStatusLevel.WARM -> Color(0xFFEAB308) // Yellow
            ThermalStatusLevel.NORMAL -> Color(0xFF10B981) // Green
        },
        label = "thermalLevelColor"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("thermal_monitoring_card"),
        colors = CardDefaults.cardColors(
            containerColor = if (level.isWarning) levelColor.copy(alpha = 0.15f) else Color(0xFF161B26)
        ),
        shape = RoundedCornerShape(16.dp),
        border = if (level.isWarning) androidx.compose.foundation.BorderStroke(1.5.dp, levelColor) else null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header Row: Sensor Icon, Status Label, Temperature Gauge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (level.isWarning) Icons.Default.LocalFireDepartment else Icons.Default.DeviceThermostat,
                        contentDescription = "Thermal Sensor",
                        tint = levelColor,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "Encoder Thermal Monitor",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color.White
                        )
                        Text(
                            text = if (level.isWarning) "High heat load detected" else "Device operating at safe thermal limits",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.LightGray
                        )
                    }
                }

                // Temp Badge
                Box(
                    modifier = Modifier
                        .background(levelColor.copy(alpha = 0.2f), RoundedCornerShape(20.dp))
                        .border(1.dp, levelColor, RoundedCornerShape(20.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                        .testTag("thermal_temp_badge"),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(levelColor, CircleShape)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "%.1f°C • ${level.label.uppercase()}".format(temp),
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = levelColor
                        )
                    }
                }
            }

            // Warning Banner Body when Throttling or Warning active
            AnimatedVisibility(
                visible = level.isWarning,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(levelColor.copy(alpha = 0.12f), RoundedCornerShape(12.dp))
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.Top) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Thermal Warning Alert",
                            tint = levelColor,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = thermalInfo.warningMessage ?: "Device thermal threshold exceeded.",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = Color.White
                            )
                            if (thermalInfo.mitigationSuggestion != null) {
                                Text(
                                    text = thermalInfo.mitigationSuggestion,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.LightGray
                                )
                            }
                        }
                    }

                    // Quick Action: Cool Down Encoder
                    Button(
                        onClick = onCoolDownClick,
                        colors = ButtonDefaults.buttonColors(containerColor = levelColor),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .testTag("cool_down_encoder_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.AcUnit,
                            contentDescription = "Cool Down",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Cool Down Encoder (Lower Bitrate & FPS)",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                            color = Color.White
                        )
                    }
                }
            }

            // Normal mode quick test action
            if (!level.isWarning) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = onSimulateTestClick,
                        modifier = Modifier.testTag("simulate_thermal_test_button"),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocalFireDepartment,
                            contentDescription = "Simulate Heat Test",
                            tint = Color.Gray,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Test Thermal Alert",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.LightGray
                        )
                    }
                }
            }
        }
    }
}
