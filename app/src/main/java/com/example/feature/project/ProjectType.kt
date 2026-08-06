package com.example.feature.project

import com.example.core.database.ProjectEntity

enum class ProjectType(val value: String) {
    LOOP("loop"),
    EDITOR("editor"),
    MASTERING("mastering"),
    SLIDESHOW("slideshow"),
    VISUALIZER("visualizer");

    companion object {
        fun fromValue(v: String) = values().find { it.value == v } ?: LOOP
    }
}

// Map domain model enum to and from entity raw string
val ProjectEntity.projectType: ProjectType
    get() = ProjectType.fromValue(this.type)
