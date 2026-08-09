package com.example.core.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [RenderJobEntity::class, LiveSessionEntity::class, AppSettingEntity::class, JobHistoryEntity::class, EditorAutoSaveEntity::class, ProjectEntity::class, ProjectSnapshotEntity::class, AssetEntity::class],
    version = 6,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun renderJobDao(): RenderJobDao
    abstract fun liveSessionDao(): LiveSessionDao
    abstract fun appSettingDao(): AppSettingDao
    abstract fun jobHistoryDao(): JobHistoryDao
    abstract fun editorAutoSaveDao(): EditorAutoSaveDao
    abstract fun projectDao(): ProjectDao
    abstract fun projectSnapshotDao(): ProjectSnapshotDao
    abstract fun assetDao(): AssetDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "loopingvid_db"
                )
                    .addMigrations(MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6)
                    // Schemas 1 and 2 shipped before exportSchema was enabled, so
                    // there is no record to migrate them from. Those installs are
                    // still rebuilt from scratch. Every version from 3 onward must
                    // supply an explicit migration instead of silently wiping user
                    // render history and settings.
                    .fallbackToDestructiveMigrationFrom(dropAllTables = true, 1, 2)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
