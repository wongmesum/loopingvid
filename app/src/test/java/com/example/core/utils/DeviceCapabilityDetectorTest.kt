package com.example.core.utils

import com.example.core.ui.BatteryInfo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Locale

class DeviceCapabilityDetectorTest {

    private fun sampleCapabilities(
        videoEncoders: List<CodecSupport> = listOf(
            CodecSupport(
                name = "c2.android.avc.encoder",
                mimeType = "video/avc",
                isHardwareAccelerated = false,
                maxResolution = "1920x1080"
            )
        )
    ) = DeviceCapabilities(
        deviceName = "Acme Pixelish",
        osVersion = "14",
        sdkInt = 34,
        totalMemoryBytes = 8L * 1024 * 1024 * 1024,
        availableMemoryBytes = 2L * 1024 * 1024 * 1024,
        maxRuntimeMemoryBytes = 512L * 1024 * 1024,
        freeRuntimeMemoryBytes = 128L * 1024 * 1024,
        totalStorageBytes = 128L * 1024 * 1024 * 1024,
        availableStorageBytes = 32L * 1024 * 1024 * 1024,
        videoEncoders = videoEncoders
    )

    // --- formatBytes ---

    @Test
    fun `formatBytes returns zero for non positive input`() {
        assertEquals("0 B", DeviceCapabilityDetector.formatBytes(0L))
        assertEquals("0 B", DeviceCapabilityDetector.formatBytes(-1L))
    }

    @Test
    fun `formatBytes keeps raw bytes without decimals below one KB`() {
        assertEquals("512 B", DeviceCapabilityDetector.formatBytes(512L))
        assertEquals("1023 B", DeviceCapabilityDetector.formatBytes(1023L))
    }

    @Test
    fun `formatBytes promotes to next unit at each 1024 boundary`() {
        assertEquals("1.0 KB", DeviceCapabilityDetector.formatBytes(1024L))
        assertEquals("1.0 MB", DeviceCapabilityDetector.formatBytes(1024L * 1024))
        assertEquals("1.0 GB", DeviceCapabilityDetector.formatBytes(1024L * 1024 * 1024))
        assertEquals("1.0 TB", DeviceCapabilityDetector.formatBytes(1024L * 1024 * 1024 * 1024))
    }

    @Test
    fun `formatBytes caps at terabytes instead of overflowing the unit list`() {
        assertEquals("2048.0 TB", DeviceCapabilityDetector.formatBytes(2048L * 1024 * 1024 * 1024 * 1024))
    }

    @Test
    fun `formatBytes uses a dot decimal separator under comma locales`() {
        val originalLocale = Locale.getDefault()
        try {
            Locale.setDefault(Locale.forLanguageTag("id-ID"))
            assertEquals("1.5 GB", DeviceCapabilityDetector.formatBytes(1536L * 1024 * 1024))
        } finally {
            Locale.setDefault(originalLocale)
        }
    }

    // --- inferHardwareAccelerated ---

    @Test
    fun `inferHardwareAccelerated treats google and android reference codecs as software`() {
        assertFalse(DeviceCapabilityDetector.inferHardwareAccelerated("OMX.google.h264.encoder"))
        assertFalse(DeviceCapabilityDetector.inferHardwareAccelerated("c2.android.avc.encoder"))
    }

    @Test
    fun `inferHardwareAccelerated treats vendor codecs as hardware`() {
        assertTrue(DeviceCapabilityDetector.inferHardwareAccelerated("OMX.qcom.video.encoder.avc"))
        assertTrue(DeviceCapabilityDetector.inferHardwareAccelerated("c2.exynos.h264.encoder"))
    }

    @Test
    fun `inferHardwareAccelerated flags explicit software names regardless of vendor`() {
        assertFalse(DeviceCapabilityDetector.inferHardwareAccelerated("OMX.vendor.software.avc"))
    }

    // --- buildDiagnosticReport ---

    @Test
    fun `buildDiagnosticReport includes every section header`() {
        val report = DeviceCapabilityDetector.buildDiagnosticReport(
            capabilities = sampleCapabilities(),
            thermalInfo = ThermalInfo(),
            batteryInfo = BatteryInfo()
        )

        listOf("Device", "Thermal", "Battery", "Memory", "Storage", "Video Encoders").forEach { header ->
            assertTrue("Missing section: $header", report.contains(header))
        }
    }

    @Test
    fun `buildDiagnosticReport reflects thermal and battery values`() {
        val thermalInfo = ThermalMonitor.evaluateThermalStatus(45.0f, 0)
        val batteryInfo = BatteryInfo(
            percentage = 42,
            isCharging = true,
            chargePlug = "AC Power",
            health = "Good",
            temperatureCelsius = 33.25f,
            voltageMv = 4050
        )

        val report = DeviceCapabilityDetector.buildDiagnosticReport(
            capabilities = sampleCapabilities(),
            thermalInfo = thermalInfo,
            batteryInfo = batteryInfo
        )

        assertTrue(report.contains(ThermalStatusLevel.SEVERE.label))
        assertTrue(report.contains("45.0°C"))
        assertTrue(report.contains("Throttling: true"))
        assertTrue(report.contains("Level: 42%"))
        assertTrue(report.contains("Plug: AC Power"))
        assertTrue(report.contains("33.3°C"))
        assertTrue(report.contains("4050 mV"))
    }

    @Test
    fun `buildDiagnosticReport formats memory and storage in human units`() {
        val report = DeviceCapabilityDetector.buildDiagnosticReport(
            capabilities = sampleCapabilities(),
            thermalInfo = ThermalInfo(),
            batteryInfo = BatteryInfo()
        )

        assertTrue(report.contains("Total device memory: 8.0 GB"))
        assertTrue(report.contains("Max app heap: 512.0 MB"))
        assertTrue(report.contains("Available internal storage: 32.0 GB"))
    }

    @Test
    fun `buildDiagnosticReport labels encoder acceleration mode`() {
        val report = DeviceCapabilityDetector.buildDiagnosticReport(
            capabilities = sampleCapabilities(
                videoEncoders = listOf(
                    CodecSupport("c2.android.avc.encoder", "video/avc", false, "1920x1080"),
                    CodecSupport("OMX.qcom.video.encoder.hevc", "video/hevc", true, "3840x2160")
                )
            ),
            thermalInfo = ThermalInfo(),
            batteryInfo = BatteryInfo()
        )

        assertTrue(report.contains("- video/avc: c2.android.avc.encoder (software, 1920x1080)"))
        assertTrue(report.contains("- video/hevc: OMX.qcom.video.encoder.hevc (hardware, 3840x2160)"))
    }

    @Test
    fun `buildDiagnosticReport states when no encoder was detected`() {
        val report = DeviceCapabilityDetector.buildDiagnosticReport(
            capabilities = sampleCapabilities(videoEncoders = emptyList()),
            thermalInfo = ThermalInfo(),
            batteryInfo = BatteryInfo()
        )

        assertTrue(report.contains("No video encoders detected"))
    }
}
