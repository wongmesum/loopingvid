package com.example.core.media

data class AudioMetadata(
    val title: String = "",
    val artist: String = "",
    val genre: String = "",
    val album: String = "",
    val year: String = "",
    val comment: String = "",
    val trackNumber: String = "",
    val albumArtist: String = "",
    // content:// URI (or local path) of a user-picked image to embed as cover/album art. Applied
    // as a separate lossless remux pass after the main render/edit succeeds, so it never touches
    // the encode pipeline. Not supported for WAV outputs (no attached-picture convention).
    val coverArtUri: String? = null
)
