package com.example.core.media

import androidx.compose.ui.graphics.Color

/**
 * Customization models for Audio Mastering Waveform, Spectrum & Peak Meter Visualizations.
 * Provides color palettes and rendering styles optimized for accessibility and user preference.
 */
enum class VisualizerTheme(
    val id: String,
    val displayName: String,
    val bassColor: Color,
    val midColor: Color,
    val trebleColor: Color,
    val waveformColor: Color,
    val peakColor: Color,
    val meterSafeColor: Color,
    val meterWarningColor: Color,
    val meterClipColor: Color
) {
    CYAN_PINK(
        id = "CYAN_PINK",
        displayName = "Studio Cyber",
        bassColor = Color(0xFF06B6D4),      // Cyan
        midColor = Color(0xFFA855F7),       // Violet
        trebleColor = Color(0xFFEC4899),    // Pink
        waveformColor = Color(0xFF38BDF8),  // Sky Blue
        peakColor = Color.White,
        meterSafeColor = Color(0xFF10B981), // Emerald Green
        meterWarningColor = Color(0xFFF59E0B), // Amber
        meterClipColor = Color(0xFFEF4444)  // Bright Red
    ),
    HIGH_CONTRAST(
        id = "HIGH_CONTRAST",
        displayName = "High Contrast (Accessibility)",
        bassColor = Color(0xFF2563EB),      // Pure Blue
        midColor = Color(0xFFFACC15),       // High Visibility Yellow
        trebleColor = Color(0xFFFFFFFF),    // Bright White
        waveformColor = Color(0xFFFACC15),  // Yellow
        peakColor = Color(0xFF38BDF8),      // Cyan
        meterSafeColor = Color(0xFF3B82F6), // Blue
        meterWarningColor = Color(0xFFF59E0B), // Orange
        meterClipColor = Color(0xFFDC2626)  // Red
    ),
    NEON_LIME(
        id = "NEON_LIME",
        displayName = "Neon Matrix",
        bassColor = Color(0xFF10B981),      // Emerald
        midColor = Color(0xFF84CC16),       // Electric Lime
        trebleColor = Color(0xFF06B6D4),    // Cyan
        waveformColor = Color(0xFF84CC16),  // Neon Lime
        peakColor = Color(0xFFF43F5E),      // Bright Rose
        meterSafeColor = Color(0xFF84CC16),
        meterWarningColor = Color(0xFFEAB308),
        meterClipColor = Color(0xFFF43F5E)
    ),
    SUNSET_AMBER(
        id = "SUNSET_AMBER",
        displayName = "Warm Analog",
        bassColor = Color(0xFFD97706),      // Deep Amber
        midColor = Color(0xFFEA580C),       // Sunset Orange
        trebleColor = Color(0xFFDC2626),    // Crimson Red
        waveformColor = Color(0xFFF59E0B),  // Amber Gold
        peakColor = Color.White,
        meterSafeColor = Color(0xFFF59E0B),
        meterWarningColor = Color(0xFFEA580C),
        meterClipColor = Color(0xFFEF4444)
    ),
    MONOCHROME(
        id = "MONOCHROME",
        displayName = "Monochrome Pro",
        bassColor = Color(0xFF94A3B8),      // Slate 400
        midColor = Color(0xFFCBD5E1),       // Slate 300
        trebleColor = Color(0xFFF8FAFC),    // Crisp White
        waveformColor = Color(0xFFE2E8F0),  // Slate 200
        peakColor = Color(0xFF38BDF8),
        meterSafeColor = Color(0xFFCBD5E1),
        meterWarningColor = Color(0xFF94A3B8),
        meterClipColor = Color(0xFFEF4444)
    )
}

enum class VisualizerBarMode(val id: String, val displayName: String) {
    BARS("BARS", "Solid Bars"),
    SMOOTH_CURVE("SMOOTH_CURVE", "Smooth Envelope"),
    DOTS("DOTS", "Dot Matrix")
}

enum class PeakMeterStyle(val id: String, val displayName: String) {
    SEGMENTED_LED("SEGMENTED_LED", "LED Segments"),
    SOLID_GRADIENT("SOLID_GRADIENT", "Solid Bar"),
    THIN_NEON("THIN_NEON", "Neon Line")
}
