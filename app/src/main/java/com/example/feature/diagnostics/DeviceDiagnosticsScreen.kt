package com.example.feature.diagnostics

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Sd
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.VideoSettings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.core.ui.rememberBatteryStatus
import com.example.core.utils.DeviceCapabilities
import com.example.core.utils.DeviceCapabilityDetector
import com.example.core.utils.ThermalInfo
import com.example.core.utils.ThermalMonitor
import com.example.core.utils.ThermalStatusLevel
import kotlinx.coroutines.delay
import java.util.Locale

// Polling interval for thermal refresh. registerThermalListener() is API 29+ only and has
// no unregister hook, so a plain poll works uniformly down to minSdk 24.
private const val THERMAL_POLL_INTERVAL_MS = 2000L

private val ColorHealthy = Color(0xFF10B981)
private val ColorWarning = Color(0xFFF59E0B)
private val ColorCritical = Color(0xFFEF4444)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceDiagnosticsScreen(
    onNavigateBack: () -> Unit = {}
) {
    val context = LocalContext.current
    var capabilities by remember { mutableStateOf(DeviceCapabilityDetector.detectCapabilities(context)) }
    var thermalInfo by remember { mutableStateOf(ThermalMonitor.getThermalInfo(context)) }
    val batteryInfo = rememberBatteryStatus()
    val scrollState = rememberScrollState()

    LaunchedEffect(Unit) {
        while (true) {
            delay(THERMAL_POLL_INTERVAL_MS)
            thermalInfo = ThermalMonitor.getThermalInfo(context)
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Diagnostik Perangkat") },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("diagnostics_back_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali")
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            capabilities = DeviceCapabilityDetector.detectCapabilities(context)
                            thermalInfo = ThermalMonitor.getThermalInfo(context)
                        },
                        modifier = Modifier.testTag("diagnostics_refresh_button")
                    ) {
                        Icon(Icons.Filled.Refresh, contentDescription = "Segarkan")
                    }
                    IconButton(
                        onClick = {
                            val report = DeviceCapabilityDetector.buildDiagnosticReport(capabilities, thermalInfo, batteryInfo)
                            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, report)
                            }
                            context.startActivity(Intent.createChooser(sendIntent, "Bagikan Laporan Diagnostik"))
                        },
                        modifier = Modifier.testTag("diagnostics_export_button")
                    ) {
                        Icon(Icons.Filled.Share, contentDescription = "Ekspor")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            DeviceSection(capabilities)
            ThermalSection(thermalInfo)
            BatterySection(batteryInfo = batteryInfo)
            MemorySection(capabilities)
            StorageSection(capabilities)
            EncodersSection(capabilities)
        }
    }
}

@Composable
private fun DiagnosticCard(
    title: String,
    icon: ImageVector,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().testTag("diagnostics_section_${title.lowercase(Locale.US)}"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(imageVector = icon, contentDescription = title, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
            }
            content()
        }
    }
}

@Composable
private fun MetricRow(label: String, value: String, valueColor: Color? = null) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            color = valueColor ?: MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun DeviceSection(capabilities: DeviceCapabilities) {
    DiagnosticCard(title = "Device", icon = Icons.Filled.PhoneAndroid) {
        MetricRow("Perangkat", capabilities.deviceName)
        MetricRow("Android", "${capabilities.osVersion} (SDK ${capabilities.sdkInt})")
    }
}

@Composable
private fun ThermalSection(thermalInfo: ThermalInfo) {
    val statusColor = when (thermalInfo.level) {
        ThermalStatusLevel.NORMAL, ThermalStatusLevel.WARM -> ColorHealthy
        ThermalStatusLevel.MODERATE -> ColorWarning
        ThermalStatusLevel.SEVERE, ThermalStatusLevel.CRITICAL -> ColorCritical
    }

    DiagnosticCard(title = "Thermal", icon = Icons.Filled.Thermostat) {
        MetricRow("Status", thermalInfo.level.label, valueColor = statusColor)
        MetricRow("Suhu", String.format(Locale.US, "%.1f°C", thermalInfo.temperatureCelsius))
        MetricRow("FPS Rekomendasi", "${thermalInfo.level.maxRecommendedFps}")
        thermalInfo.warningMessage?.let { message ->
            Text(message, style = MaterialTheme.typography.bodySmall, color = statusColor)
        }
    }
}

@Composable
private fun BatterySection(batteryInfo: com.example.core.ui.BatteryInfo) {
    val statusColor = when {
        batteryInfo.isCriticalBattery -> ColorCritical
        batteryInfo.isLowBattery -> ColorWarning
        else -> ColorHealthy
    }

    DiagnosticCard(title = "Battery", icon = Icons.Filled.BatteryFull) {
        MetricRow("Level", "${batteryInfo.percentage}%", valueColor = statusColor)
        MetricRow("Status", if (batteryInfo.isCharging) batteryInfo.chargePlug else "Discharging")
        MetricRow("Kesehatan", batteryInfo.health)
        MetricRow("Suhu", String.format(Locale.US, "%.1f°C", batteryInfo.temperatureCelsius))
        batteryInfo.warningMessage?.let { message ->
            Text(message, style = MaterialTheme.typography.bodySmall, color = statusColor)
        }
    }
}

@Composable
private fun MemorySection(capabilities: DeviceCapabilities) {
    DiagnosticCard(title = "Memory", icon = Icons.Filled.Memory) {
        MetricRow("Total RAM", DeviceCapabilityDetector.formatBytes(capabilities.totalMemoryBytes))
        MetricRow("RAM Tersedia", DeviceCapabilityDetector.formatBytes(capabilities.availableMemoryBytes))
        MetricRow("Max Heap Aplikasi", DeviceCapabilityDetector.formatBytes(capabilities.maxRuntimeMemoryBytes))
        MetricRow("Heap Bebas", DeviceCapabilityDetector.formatBytes(capabilities.freeRuntimeMemoryBytes))
    }
}

@Composable
private fun StorageSection(capabilities: DeviceCapabilities) {
    DiagnosticCard(title = "Storage", icon = Icons.Filled.Sd) {
        MetricRow("Total Penyimpanan", DeviceCapabilityDetector.formatBytes(capabilities.totalStorageBytes))
        MetricRow("Tersedia", DeviceCapabilityDetector.formatBytes(capabilities.availableStorageBytes))
    }
}

@Composable
private fun EncodersSection(capabilities: DeviceCapabilities) {
    DiagnosticCard(title = "Video Encoders", icon = Icons.Filled.VideoSettings) {
        if (capabilities.videoEncoders.isEmpty()) {
            Text(
                "Tidak ada encoder video terdeteksi.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            capabilities.videoEncoders.forEach { codec ->
                val accelerationLabel = if (codec.isHardwareAccelerated) "hardware" else "software"
                MetricRow(
                    label = "${codec.mimeType} · ${codec.name}",
                    value = "$accelerationLabel · ${codec.maxResolution}",
                    valueColor = if (codec.isHardwareAccelerated) ColorHealthy else MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}
