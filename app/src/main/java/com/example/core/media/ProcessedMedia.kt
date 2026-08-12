package com.example.core.media

/**
 * Validated details of a successful media processing job (loop, editor, or mastering).
 * Represents a file that has been successfully probed for correctness, not just
 * a completed FFmpeg run.
 */
data class ProcessedMedia(
    /** Absolute path to the validated output file in app-private cache. */
    val path: String,

    /** Actual duration in milliseconds. */
    val durationMs: Long,

    /** File size in bytes. */
    val sizeBytes: Long,

    // Video-specific fields (null for audio-only mastering)
    val width: Int? = null,
    val height: Int? = null,
    val videoCodec: String? = null,

    // Audio-specific fields
    val audioCodec: String? = null,
    val sampleRateHz: Int? = null,
    val channels: Int? = null,
    val audioBitrateBps: Int? = null
)
