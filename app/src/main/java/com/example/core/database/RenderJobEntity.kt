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
    // The MediaStore content:// Uri of the copy pushed to the public Gallery/Music library
    // (Movies/Music folder), captured when the job completes. Null if the export to MediaStore
    // failed or hasn't happened. Used so metadata edits can also update the public copy, not
    // just the app-private outputUri file.
    val galleryUri: String? = null,
    // Whether THIS specific job has been successfully uploaded to Firestore. Previously the
    // History screen's per-job "Synced to Firestore" badge read the app-wide
    // FirestoreSyncInfo.syncState instead of anything job-specific, so every job in the list
    // showed the same badge regardless of whether it individually made it to the cloud. Set by
    // LoopingVidRepository.markJobSyncedToCloud() after a real Firestore upload succeeds.
    val isSyncedToCloud: Boolean = false
)
