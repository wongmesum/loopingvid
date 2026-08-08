package com.example.core.presets

/**
 * Unified export preset system for all LoopingVid tools.
 * Provides platform-optimized and quality-based presets for video/audio exports.
 */
data class ExportPreset(
    val id: String,
    val name: String,
    val description: String,
    val category: PresetCategory,
    val resolution: String,
    val frameRate: String,
    val bitrate: String,
    val aspectRatio: String,
    val format: String,
    val targetLufs: Double? = null, // For audio mastering
    val isBuiltIn: Boolean = true,
    val createdAtTimestamp: Long = System.currentTimeMillis()
)

enum class PresetCategory {
    PLATFORM,      // TikTok, YouTube, Instagram, etc.
    QUALITY,       // High, Medium, Low
    CUSTOM         // User-created
}

object ExportPresets {

    /** Formats that carry audio only, used to split audio presets from video presets. */
    val AUDIO_FORMATS = listOf("m4a", "mp3", "wav")

    // Platform-optimized presets
    val TIKTOK = ExportPreset(
        id = "preset_tiktok",
        name = "TikTok / Reels",
        description = "Vertical 9:16, 1080x1920, 30fps, optimized for mobile",
        category = PresetCategory.PLATFORM,
        resolution = "1080p",
        frameRate = "30fps",
        bitrate = "High",
        aspectRatio = "9:16",
        format = "mp4",
        targetLufs = -14.0
    )

    val YOUTUBE = ExportPreset(
        id = "preset_youtube",
        name = "YouTube",
        description = "16:9, 1080p, 60fps, high bitrate for desktop viewing",
        category = PresetCategory.PLATFORM,
        resolution = "1080p",
        frameRate = "60fps",
        bitrate = "High",
        aspectRatio = "16:9",
        format = "mp4",
        targetLufs = -14.0
    )

    val YOUTUBE_SHORTS = ExportPreset(
        id = "preset_youtube_shorts",
        name = "YouTube Shorts",
        description = "Vertical 9:16, 1080x1920, 30fps, mobile-first",
        category = PresetCategory.PLATFORM,
        resolution = "1080p",
        frameRate = "30fps",
        bitrate = "High",
        aspectRatio = "9:16",
        format = "mp4",
        targetLufs = -14.0
    )

    val INSTAGRAM_FEED = ExportPreset(
        id = "preset_instagram_feed",
        name = "Instagram Feed",
        description = "Square 1:1, 1080x1080, 30fps, perfect for grid posts",
        category = PresetCategory.PLATFORM,
        resolution = "1080p",
        frameRate = "30fps",
        bitrate = "Medium",
        aspectRatio = "1:1",
        format = "mp4",
        targetLufs = -14.0
    )

    val INSTAGRAM_PORTRAIT = ExportPreset(
        id = "preset_instagram_portrait",
        name = "Instagram Portrait",
        description = "Portrait 4:5, 1080x1350, 30fps, maximum feed height",
        category = PresetCategory.PLATFORM,
        resolution = "1080p",
        frameRate = "30fps",
        bitrate = "Medium",
        aspectRatio = "4:5",
        format = "mp4",
        targetLufs = -14.0
    )

    val INSTAGRAM_STORY = ExportPreset(
        id = "preset_instagram_story",
        name = "Instagram Story",
        description = "Vertical 9:16, 1080x1920, 30fps, 15-second optimized",
        category = PresetCategory.PLATFORM,
        resolution = "1080p",
        frameRate = "30fps",
        bitrate = "Medium",
        aspectRatio = "9:16",
        format = "mp4",
        targetLufs = -14.0
    )

    val TWITTER = ExportPreset(
        id = "preset_twitter",
        name = "Twitter / X",
        description = "16:9, 720p, 30fps, optimized for timeline playback",
        category = PresetCategory.PLATFORM,
        resolution = "720p",
        frameRate = "30fps",
        bitrate = "Medium",
        aspectRatio = "16:9",
        format = "mp4",
        targetLufs = -14.0
    )

    val FACEBOOK = ExportPreset(
        id = "preset_facebook",
        name = "Facebook",
        description = "16:9, 1080p, 30fps, balanced quality and file size",
        category = PresetCategory.PLATFORM,
        resolution = "1080p",
        frameRate = "30fps",
        bitrate = "Medium",
        aspectRatio = "16:9",
        format = "mp4",
        targetLufs = -14.0
    )

    // Quality-based presets
    val HIGH_QUALITY = ExportPreset(
        id = "preset_high_quality",
        name = "High Quality",
        description = "1080p, 60fps, maximum bitrate, best for archival",
        category = PresetCategory.QUALITY,
        resolution = "1080p",
        frameRate = "60fps",
        bitrate = "High",
        aspectRatio = "Asli",
        format = "mp4"
    )

    val MEDIUM_QUALITY = ExportPreset(
        id = "preset_medium_quality",
        name = "Medium Quality",
        description = "720p, 30fps, balanced quality and file size",
        category = PresetCategory.QUALITY,
        resolution = "720p",
        frameRate = "30fps",
        bitrate = "Medium",
        aspectRatio = "Asli",
        format = "mp4"
    )

    val LOW_QUALITY = ExportPreset(
        id = "preset_low_quality",
        name = "Low Quality / Fast Export",
        description = "480p, 30fps, small file size, quick processing",
        category = PresetCategory.QUALITY,
        resolution = "480p",
        frameRate = "30fps",
        bitrate = "Low",
        aspectRatio = "Asli",
        format = "mp4"
    )

    val ULTRA_HD = ExportPreset(
        id = "preset_4k",
        name = "4K Ultra HD",
        description = "2160p, 60fps, maximum quality for professional use",
        category = PresetCategory.QUALITY,
        resolution = "4K",
        frameRate = "60fps",
        bitrate = "High",
        aspectRatio = "Asli",
        format = "mp4"
    )

    // Audio-only presets
    val AUDIO_HIGH = ExportPreset(
        id = "preset_audio_high",
        name = "High Quality Audio",
        description = "320kbps, 48kHz, lossless compression",
        category = PresetCategory.QUALITY,
        resolution = "Audio",
        frameRate = "N/A",
        bitrate = "High",
        aspectRatio = "N/A",
        format = "m4a",
        targetLufs = -14.0
    )

    val AUDIO_PODCAST = ExportPreset(
        id = "preset_audio_podcast",
        name = "Podcast / Voice",
        description = "128kbps, 44.1kHz, optimized for speech",
        category = PresetCategory.QUALITY,
        resolution = "Audio",
        frameRate = "N/A",
        bitrate = "Medium",
        aspectRatio = "N/A",
        format = "m4a",
        targetLufs = -16.0
    )

    val ALL_PRESETS = listOf(
        // Platform presets
        TIKTOK,
        YOUTUBE,
        YOUTUBE_SHORTS,
        INSTAGRAM_FEED,
        INSTAGRAM_PORTRAIT,
        INSTAGRAM_STORY,
        TWITTER,
        FACEBOOK,
        // Quality presets
        HIGH_QUALITY,
        MEDIUM_QUALITY,
        LOW_QUALITY,
        ULTRA_HD,
        // Audio presets
        AUDIO_HIGH,
        AUDIO_PODCAST
    )

    fun getPlatformPresets() = ALL_PRESETS.filter { it.category == PresetCategory.PLATFORM }
    fun getQualityPresets() = ALL_PRESETS.filter { it.category == PresetCategory.QUALITY }
    fun getAudioPresets() = ALL_PRESETS.filter { it.format in AUDIO_FORMATS }
    fun getVideoPresets() = ALL_PRESETS.filter { it.format !in AUDIO_FORMATS }

    fun findById(id: String) = ALL_PRESETS.firstOrNull { it.id == id }
    fun findByName(name: String) = ALL_PRESETS.firstOrNull { it.name.equals(name, ignoreCase = true) }
}
