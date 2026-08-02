package com.example.core.database

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room Entity for storing video rendering and audio mastering task logs and history.
 */
@Entity(tableName = "job_history")
data class JobHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val taskType: String, // "VIDEO_RENDERING", "AUDIO_MASTERING", "FFMPEG_PROCESS", "EDITOR_EXPORT"
    val title: String,
    val status: String, // "SUCCESS", "FAILED", "IN_PROGRESS", "CANCELLED"
    val inputUri: String,
    val outputUri: String,
    val executionLogs: String = "", // Detailed task logs (e.g. Timber/FFmpeg stdout/stderr)
    val errorMessage: String? = null,
    val durationMs: Long = 0L,
    val fileSizeBytes: Long = 0L,
    val timestamp: Long = System.currentTimeMillis()
)

typealias JobHistory = JobHistoryEntity
