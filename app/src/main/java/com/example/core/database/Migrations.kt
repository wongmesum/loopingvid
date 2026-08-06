package com.example.core.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Non-destructive migration from schema v3 to v4.
 * Adds the `projects` table and a nullable `projectId` column to `render_jobs`
 * so that render history can be associated with a project.
 */
val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `projects` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `name` TEXT NOT NULL,
                `type` TEXT NOT NULL,
                `thumbnailUri` TEXT,
                `sourceMediaUri` TEXT,
                `sourceAudioUri` TEXT,
                `configJson` TEXT NOT NULL,
                `status` TEXT NOT NULL,
                `createdAt` INTEGER NOT NULL,
                `updatedAt` INTEGER NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL("ALTER TABLE `render_jobs` ADD COLUMN `projectId` INTEGER")
    }
}
