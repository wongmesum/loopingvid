package com.example.core.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [RenderJobEntity::class, LiveSessionEntity::class, AppSettingEntity::class, JobHistoryEntity::class, EditorAutoSaveEntity::class],
    version = 5,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun renderJobDao(): RenderJobDao
    abstract fun liveSessionDao(): LiveSessionDao
    abstract fun appSettingDao(): AppSettingDao
    abstract fun jobHistoryDao(): JobHistoryDao
    abstract fun editorAutoSaveDao(): EditorAutoSaveDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "loopingvid_db"
                ).fallbackToDestructiveMigration(dropAllTables = true).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
