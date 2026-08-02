package com.example.core.media

data class ProjectTemplate(
    val id: String,
    val name: String,
    val description: String,
    val category: String,
    val playbackSpeed: Float = 1.0f,
    val colorGradingConfig: ColorGradingConfig = ColorGradingConfig(),
    val transitionConfig: SegmentTransitionConfig = SegmentTransitionConfig(),
    val masteringPreset: MasteringPreset = AudioMasteringEngine.PRESETS.first { it.name == "Transparent Neutral" },
    val isBuiltIn: Boolean = false,
    val createdAtTimestamp: Long = System.currentTimeMillis()
)

val BUILTIN_PROJECT_TEMPLATES = listOf(
    ProjectTemplate(
        id = "tpl_social_reel",
        name = "TikTok / Reel High Energy",
        description = "1.25x brisk speed with punchy vibrant colors & directional wipe transitions.",
        category = "Social Reel",
        playbackSpeed = 1.25f,
        colorGradingConfig = ColorGradingConfig(
            preset = ColorFilterPreset.VIVID,
            brightness = 0.05f,
            contrast = 1.2f,
            saturation = 1.3f
        ),
        transitionConfig = SegmentTransitionConfig(
            globalTransitionEffect = TransitionEffect.WIPE_LEFT,
            globalTransitionDurationSec = 0.8
        ),
        masteringPreset = AudioMasteringEngine.PRESETS.first { it.name == "Loudness Maximizer" },
        isBuiltIn = true
    ),
    ProjectTemplate(
        id = "tpl_cinematic_drama",
        name = "Cinematic Slow-Mo Drama",
        description = "0.75x dramatic slow-mo with teal & orange color grading & 1.5s crossfades.",
        category = "Cinematic",
        playbackSpeed = 0.75f,
        colorGradingConfig = ColorGradingConfig(
            preset = ColorFilterPreset.CINEMATIC,
            contrast = 1.25f,
            saturation = 1.15f
        ),
        transitionConfig = SegmentTransitionConfig(
            globalTransitionEffect = TransitionEffect.CROSSFADE,
            globalTransitionDurationSec = 1.5
        ),
        masteringPreset = AudioMasteringEngine.PRESETS.first { it.name == "Cinematic Warmth" },
        isBuiltIn = true
    ),
    ProjectTemplate(
        id = "tpl_lofi_beats",
        name = "Lo-Fi Chill Beats Loop",
        description = "Normal speed with analogue retro film tone, noise dissolves & 3x loop repeat.",
        category = "Lo-Fi Music",
        playbackSpeed = 1.0f,
        colorGradingConfig = ColorGradingConfig(
            preset = ColorFilterPreset.RETRO_FILM,
            contrast = 1.05f,
            saturation = 0.85f
        ),
        transitionConfig = SegmentTransitionConfig(
            segments = listOf(
                LoopedSegmentConfig("seg_1", "Intro Chill Loop", durationSec = 5.0, loopRepeatCount = 3, transitionToNext = TransitionEffect.DISSOLVE, transitionDurationSec = 1.2),
                LoopedSegmentConfig("seg_2", "Ambient Melody", durationSec = 6.0, loopRepeatCount = 3, transitionToNext = TransitionEffect.DISSOLVE, transitionDurationSec = 1.2)
            ),
            globalTransitionEffect = TransitionEffect.DISSOLVE,
            globalTransitionDurationSec = 1.2
        ),
        masteringPreset = AudioMasteringEngine.PRESETS.first { it.name == "Bass Boost" },
        isBuiltIn = true
    ),
    ProjectTemplate(
        id = "tpl_timelapse_action",
        name = "Timelapse Hyper-Drive",
        description = "2.0x maximum speed timelapse with vivid contrast and rapid slide transitions.",
        category = "Action",
        playbackSpeed = 2.0f,
        colorGradingConfig = ColorGradingConfig(
            preset = ColorFilterPreset.CYBERPUNK,
            contrast = 1.3f,
            saturation = 1.4f
        ),
        transitionConfig = SegmentTransitionConfig(
            globalTransitionEffect = TransitionEffect.SLIDE_LEFT,
            globalTransitionDurationSec = 0.5
        ),
        masteringPreset = AudioMasteringEngine.PRESETS.first { it.name == "Clear & Punchy" },
        isBuiltIn = true
    ),
    ProjectTemplate(
        id = "tpl_vintage_noir",
        name = "Vintage B&W Noir Stutter",
        description = "0.85x retro pace with moody monochrome black-and-white and pixelize transitions.",
        category = "Retro",
        playbackSpeed = 0.85f,
        colorGradingConfig = ColorGradingConfig(
            preset = ColorFilterPreset.GRAYSCALE,
            contrast = 1.35f
        ),
        transitionConfig = SegmentTransitionConfig(
            globalTransitionEffect = TransitionEffect.PIXELIZE,
            globalTransitionDurationSec = 1.0
        ),
        masteringPreset = AudioMasteringEngine.PRESETS.first { it.name == "Noise Reduction" },
        isBuiltIn = true
    )
)
