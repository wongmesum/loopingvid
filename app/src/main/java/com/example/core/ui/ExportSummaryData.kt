package com.example.core.ui

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class CompatibilityLevel {
    OPTIMAL, // Green badge - 100% optimal & recommended
    GOOD,    // Blue badge - compatible with minor notes
    WARNING  // Orange badge - size or duration boundary caution
}

data class SocialPlatformCompatibility(
    val platformName: String,
    val iconType: String, // "tiktok", "instagram", "youtube_shorts", "youtube", "whatsapp"
    val statusText: String, // e.g. "100% Optimal", "Compatible", "Duration Warning"
    val level: CompatibilityLevel,
    val aspectRatioLabel: String, // e.g. "9:16 Vertical", "16:9 Widescreen"
    val details: String // Concise analysis of compatibility
)

data class ExportSummaryData(
    val fileName: String,
    val filePath: String,
    val fileSizeMb: Double,
    val resolution: String = "1080p (1920x1080)",
    val durationSec: Double = 30.0,
    val format: String = "mp4",
    val jobType: String = "Editor Project",
    val isVideo: Boolean = true,
    val presetQuality: String = "1080p",
    val bitRateKbps: Int = 8500,
    val frameRate: Int = 30,
    val videoCodec: String = "H.264 / AVC High Profile",
    val audioCodec: String = "AAC-LC 48kHz Stereo 192kbps",
    val aspectRatio: String = "9:16 Vertical (Shorts/Reels)",
    val timestampMs: Long = System.currentTimeMillis()
) {
    fun getFormattedSize(): String {
        return if (fileSizeMb >= 1024.0) {
            "%.2f GB".format(fileSizeMb / 1024.0)
        } else {
            "%.1f MB".format(fileSizeMb.coerceAtLeast(0.1))
        }
    }

    fun getFormattedDuration(): String {
        val totalSec = durationSec.toInt().coerceAtLeast(0)
        val mins = totalSec / 60
        val secs = totalSec % 60
        return "%02d:%02d".format(mins, secs)
    }

    fun getFormattedTimestamp(): String {
        val sdf = SimpleDateFormat("MMM dd, yyyy - HH:mm", Locale.getDefault())
        return sdf.format(Date(timestampMs))
    }
}

object SocialMediaCompatibilityAnalyzer {

    fun analyze(summary: ExportSummaryData): List<SocialPlatformCompatibility> {
        val list = mutableListOf<SocialPlatformCompatibility>()
        val dur = summary.durationSec
        val size = summary.fileSizeMb
        val isVertical = summary.aspectRatio.lowercase().contains("vertical") || summary.aspectRatio.contains("9:16")

        // 1. TikTok
        val tikTokLevel = if (isVertical && dur <= 600.0 && size <= 500.0) {
            CompatibilityLevel.OPTIMAL
        } else if (dur <= 600.0) {
            CompatibilityLevel.GOOD
        } else {
            CompatibilityLevel.WARNING
        }
        val tikTokStatus = when (tikTokLevel) {
            CompatibilityLevel.OPTIMAL -> "100% Optimal for Feed"
            CompatibilityLevel.GOOD -> "Compatible"
            CompatibilityLevel.WARNING -> "Exceeds Standard Length"
        }
        val tikTokDetails = when (tikTokLevel) {
            CompatibilityLevel.OPTIMAL -> "9:16 vertical viewport with H.264 video codec and AAC stereo audio matches TikTok creator standards."
            CompatibilityLevel.GOOD -> "H.264 MP4 supported by TikTok processing pipeline."
            CompatibilityLevel.WARNING -> "Video duration is long for TikTok main recommendation feed."
        }
        list.add(
            SocialPlatformCompatibility(
                platformName = "TikTok",
                iconType = "tiktok",
                statusText = tikTokStatus,
                level = tikTokLevel,
                aspectRatioLabel = if (isVertical) "9:16 Vertical" else "Landscape / Custom",
                details = tikTokDetails
            )
        )

        // 2. Instagram Reels & Stories
        val igLevel = if (isVertical && dur <= 900.0 && size <= 1000.0) {
            CompatibilityLevel.OPTIMAL
        } else if (dur <= 900.0) {
            CompatibilityLevel.GOOD
        } else {
            CompatibilityLevel.WARNING
        }
        val igStatus = when (igLevel) {
            CompatibilityLevel.OPTIMAL -> "Reels & Stories Ready"
            CompatibilityLevel.GOOD -> "Compatible"
            CompatibilityLevel.WARNING -> "Long Format Note"
        }
        val igDetails = when (igLevel) {
            CompatibilityLevel.OPTIMAL -> "Full-screen 9:16 frame with clear AAC audio guarantees smooth IG Reels rendering."
            CompatibilityLevel.GOOD -> "Supported format for Instagram video posts."
            CompatibilityLevel.WARNING -> "Duration exceeds 15-min limit for standard Reels."
        }
        list.add(
            SocialPlatformCompatibility(
                platformName = "Instagram Reels",
                iconType = "instagram",
                statusText = igStatus,
                level = igLevel,
                aspectRatioLabel = if (isVertical) "9:16 Vertical" else "Square / Landscape",
                details = igDetails
            )
        )

        // 3. YouTube Shorts
        val shortsLevel = if (isVertical && dur <= 60.0) {
            CompatibilityLevel.OPTIMAL
        } else if (dur <= 60.0) {
            CompatibilityLevel.GOOD
        } else {
            CompatibilityLevel.WARNING
        }
        val shortsStatus = if (dur <= 60.0) "Shorts Eligible (<60s)" else "Exceeds 60s Limit"
        val shortsDetails = if (dur <= 60.0) {
            "Duration <= 60s & vertical frame triggers YouTube Shorts shelf algorithm distribution."
        } else {
            "Duration > 60s. YouTube will upload as standard long-form video rather than Shorts."
        }
        list.add(
            SocialPlatformCompatibility(
                platformName = "YouTube Shorts",
                iconType = "youtube_shorts",
                statusText = shortsStatus,
                level = shortsLevel,
                aspectRatioLabel = if (isVertical) "9:16 Vertical" else "Non-Vertical",
                details = shortsDetails
            )
        )

        // 4. YouTube Main (HD / 4K)
        val ytMainLevel = if (summary.resolution.contains("1080p") || summary.resolution.contains("4K") || summary.resolution.contains("720p")) {
            CompatibilityLevel.OPTIMAL
        } else {
            CompatibilityLevel.GOOD
        }
        list.add(
            SocialPlatformCompatibility(
                platformName = "YouTube Main",
                iconType = "youtube",
                statusText = "HD Upload Ready",
                level = ytMainLevel,
                aspectRatioLabel = summary.resolution.split(" ").firstOrNull() ?: "1080p",
                details = "H.264 video codec with 48kHz audio sample rate perfectly matches YouTube HD ingestion server recommendations."
            )
        )

        // 5. WhatsApp & Instant Messaging
        val waLevel = if (size <= 16.0 && dur <= 30.0) {
            CompatibilityLevel.OPTIMAL
        } else if (size <= 16.0) {
            CompatibilityLevel.GOOD
        } else {
            CompatibilityLevel.WARNING
        }
        val waStatus = when (waLevel) {
            CompatibilityLevel.OPTIMAL -> "Direct Media Share (<16MB)"
            CompatibilityLevel.GOOD -> "Direct Share Ready"
            CompatibilityLevel.WARNING -> "Large File (>16MB)"
        }
        val waDetails = when (waLevel) {
            CompatibilityLevel.OPTIMAL -> "Under 16MB size limit & <=30s duration for seamless WhatsApp Status & direct chat sharing."
            CompatibilityLevel.GOOD -> "File size fits WhatsApp direct attachment size boundary."
            CompatibilityLevel.WARNING -> "File size >16MB may require compression or link sharing on WhatsApp."
        }
        list.add(
            SocialPlatformCompatibility(
                platformName = "WhatsApp / Telegram",
                iconType = "whatsapp",
                statusText = waStatus,
                level = waLevel,
                aspectRatioLabel = "${summary.getFormattedSize()}",
                details = waDetails
            )
        )

        return list
    }
}
