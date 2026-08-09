package com.example.core.database

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * One saved revision of a project's configuration.
 *
 * Snapshots are explicit user checkpoints, distinct from [EditorAutoSaveEntity]:
 * that one is a single-slot crash-recovery draft (`id = 1`, overwritten every
 * interval), while this table keeps a linear history the user can restore from.
 *
 * Deleting a project drops its snapshots via [ForeignKey.CASCADE] so no orphan
 * revisions survive the parent row.
 */
@Entity(
    tableName = "project_snapshots",
    foreignKeys = [
        ForeignKey(
            entity = ProjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["projectId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["projectId"])]
)
data class ProjectSnapshotEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val projectId: Long,
    val label: String,
    val configJson: String,
    val sourceMediaUri: String? = null,
    val sourceAudioUri: String? = null,
    val createdAt: Long
)
