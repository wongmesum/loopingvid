package com.example.core.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "projects")
data class ProjectEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val type: String, // "loop" | "slideshow" | "visualizer"
    val thumbnailUri: String? = null,
    val sourceMediaUri: String? = null,
    val sourceAudioUri: String? = null,
    val configJson: String = "{}",
    val status: String = "draft", // "draft" | "rendering" | "completed" | "failed"
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
