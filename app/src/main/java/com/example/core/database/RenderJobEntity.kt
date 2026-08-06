package com.example.core.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "render_jobs")
data class RenderJobEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val jobType: String, // "LOOP", "MASTERING", "EDITOR"
    val title: String,
    val inputUri: String,
    val outputUri: String,
    val style: String, // "NORMAL", "CROSSFADE", "PING_PONG"
    val status: String, // "PROCESSING", "COMPLETED", "FAILED", "CANCELLED"
    val progress: Int, // 0 to 100
    val durationSec: Double,
    val fileSizeMb: Double,
    val createdAt: Long = System.currentTimeMillis(),
    val paramsSummary: String = "",
    val projectId: Long? = null
)
