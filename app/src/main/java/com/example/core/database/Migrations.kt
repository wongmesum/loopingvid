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

/**
 * Non-destructive migration from schema v4 to v5.
 * Adds the `assets` table for caching user-selected media metadata.
 */
val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `assets` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `uriString` TEXT NOT NULL,
                `fileName` TEXT NOT NULL,
                `fileSize` INTEGER NOT NULL,
                `mimeType` TEXT NOT NULL,
                `mediaType` TEXT NOT NULL,
                `isFavorite` INTEGER NOT NULL,
                `isPinned` INTEGER NOT NULL,
                `usageCount` INTEGER NOT NULL,
                `lastAccessedAt` INTEGER NOT NULL,
                `createdAt` INTEGER NOT NULL,
                `permissionPersisted` INTEGER NOT NULL,
                `isMissing` INTEGER NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_assets_uriString` ON `assets` (`uriString`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_assets_lastAccessedAt` ON `assets` (`lastAccessedAt`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_assets_isFavorite` ON `assets` (`isFavorite`)")
    }
}
