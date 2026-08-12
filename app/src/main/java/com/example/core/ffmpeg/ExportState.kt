package com.example.core.ffmpeg

import android.net.Uri

sealed interface ExportState {
    data object Idle : ExportState

    /** User is picking a destination folder/URI. Never entered until processing has succeeded. */
    data object ChoosingDestination : ExportState

    data object Exporting : ExportState
    data class Success(val uri: Uri) : ExportState
    data class Failed(val message: String) : ExportState
}
