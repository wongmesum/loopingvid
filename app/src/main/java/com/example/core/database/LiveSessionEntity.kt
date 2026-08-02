package com.example.core.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "live_sessions")
data class LiveSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val platform: String, // "YOUTUBE", "TIKTOK", "CUSTOM_RTMP"
    val streamTitle: String,
    val rtmpUrl: String,
    val sourceUri: String,
    val durationSec: Long,
    val totalLoops: Int,
    val status: String, // "ACTIVE", "STOPPED", "ERROR"
    val avgBitrateKbps: Int,
    val droppedFrames: Int,
    val startedAt: Long = System.currentTimeMillis(),
    val endedAt: Long? = null
)
