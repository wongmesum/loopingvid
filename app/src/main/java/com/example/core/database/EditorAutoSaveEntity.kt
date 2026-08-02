package com.example.core.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "editor_autosave_session")
data class EditorAutoSaveEntity(
    @PrimaryKey val id: Int = 1,
    val lastSavedTimestamp: Long = System.currentTimeMillis(),
    val selectedMediaUri: String? = null,
    val selectedMediaName: String = "",
    val selectedAudioUri: String? = null,
    val selectedAudioName: String = "",
    val titleText: String = "",
    val watermarkText: String = "",
    val showTimerOverlay: Boolean = true,
    val spectrumStyle: String = "BARS",
    val presetQuality: String = "1080p",
    val visualizerMode: String = "FFT_BARS",
    val playbackSpeed: Float = 1.0f,
    val selectedTemplateId: String? = null,
    val trimStartSec: Double = 0.0,
    val trimEndSec: Double = 0.0,
    val colorGradingJson: String = "",
    val transitionConfigJson: String = "",
    val version: Int = 1,
    val wasCleanExit: Boolean = false
)
