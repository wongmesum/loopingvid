package com.example.core.database

import android.content.Context
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Guards the v3 -> v4 -> v5 upgrade path against data loss.
 *
 * This drives [MIGRATION_3_4] against a real SQLite database seeded with the v3
 * schema instead of going through Room's MigrationTestHelper: the helper
 * validates against the exported schema JSON, and the v4 JSON is a KSP build
 * output rather than a committed file, so it is not reliably present in the
 * test assets. Exercising the migration SQL directly keeps the guard running in
 * CI, which only executes unit tests.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class AppDatabaseMigrationTest {

    private companion object {
        const val TEST_DB = "migration-test.db"

        /** `render_jobs` exactly as schema version 3 shipped it. */
        const val CREATE_RENDER_JOBS_V3 = """
            CREATE TABLE IF NOT EXISTS `render_jobs` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `jobType` TEXT NOT NULL,
                `title` TEXT NOT NULL,
                `inputUri` TEXT NOT NULL,
                `outputUri` TEXT NOT NULL,
                `style` TEXT NOT NULL,
                `status` TEXT NOT NULL,
                `progress` INTEGER NOT NULL,
                `durationSec` REAL NOT NULL,
                `fileSizeMb` REAL NOT NULL,
                `createdAt` INTEGER NOT NULL,
                `paramsSummary` TEXT NOT NULL
            )
        """
    }

    private lateinit var openHelper: SupportSQLiteOpenHelper

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        context.deleteDatabase(TEST_DB)

        val configuration = SupportSQLiteOpenHelper.Configuration.builder(context)
            .name(TEST_DB)
            .callback(object : SupportSQLiteOpenHelper.Callback(3) {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    db.execSQL(CREATE_RENDER_JOBS_V3)
                }

                override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {
                    // The test drives the migration explicitly.
                }
            })
            .build()

        openHelper = FrameworkSQLiteOpenHelperFactory().create(configuration)
    }

    @After
    fun tearDown() {
        openHelper.close()
        ApplicationProvider.getApplicationContext<Context>().deleteDatabase(TEST_DB)
    }

    @Test
    fun `migration 3 to 4 preserves existing render jobs`() {
        val db = openHelper.writableDatabase
        db.execSQL(
            """
            INSERT INTO render_jobs
                (id, jobType, title, inputUri, outputUri, style, status, progress, durationSec, fileSizeMb, createdAt, paramsSummary)
            VALUES
                (1, 'LOOP', 'Test Render', 'content://1', '/out.mp4', 'NORMAL', 'COMPLETED', 100, 10.0, 1.5, 1234567890, 'summary')
            """.trimIndent()
        )

        MIGRATION_3_4.migrate(db)

        db.query("SELECT title, projectId FROM render_jobs WHERE id = 1").use { cursor ->
            assertTrue("row inserted before the migration must survive it", cursor.moveToFirst())
            assertEquals("Test Render", cursor.getString(cursor.getColumnIndexOrThrow("title")))
            assertTrue(
                "projectId must default to null for pre-existing rows",
                cursor.isNull(cursor.getColumnIndexOrThrow("projectId"))
            )
        }
    }

    @Test
    fun `migration 3 to 4 creates an empty projects table`() {
        val db = openHelper.writableDatabase

        MIGRATION_3_4.migrate(db)

        db.query("SELECT COUNT(*) FROM projects").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals(0, cursor.getInt(0))
        }
    }

    @Test
    fun `projects table accepts a row with the columns the entity declares`() {
        val db = openHelper.writableDatabase
        MIGRATION_3_4.migrate(db)

        db.execSQL(
            """
            INSERT INTO projects
                (name, type, thumbnailUri, sourceMediaUri, sourceAudioUri, configJson, status, createdAt, updatedAt)
            VALUES
                ('My Loop', 'loop', NULL, NULL, NULL, '{}', 'draft', 100, 200)
            """.trimIndent()
        )

        db.query("SELECT name, type, status FROM projects").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("My Loop", cursor.getString(cursor.getColumnIndexOrThrow("name")))
            assertEquals("loop", cursor.getString(cursor.getColumnIndexOrThrow("type")))
            assertEquals("draft", cursor.getString(cursor.getColumnIndexOrThrow("status")))
        }
    }

    @Test
    fun `migration 4 to 5 creates an empty assets table`() {
        val db = openHelper.writableDatabase

        MIGRATION_3_4.migrate(db)
        MIGRATION_4_5.migrate(db)

        db.query("SELECT COUNT(*) FROM assets").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals(0, cursor.getInt(0))
        }
    }

    @Test
    fun `assets table accepts a row with the columns the entity declares`() {
        val db = openHelper.writableDatabase

        MIGRATION_3_4.migrate(db)
        MIGRATION_4_5.migrate(db)

        db.execSQL(
            """
            INSERT INTO assets
                (uriString, fileName, fileSize, mimeType, mediaType, isFavorite, isPinned, usageCount, lastAccessedAt, createdAt, permissionPersisted, isMissing)
            VALUES
                ('content://media/1', 'vid.mp4', 1024, 'video/mp4', 'VIDEO', 0, 1, 5, 200, 100, 1, 0)
            """.trimIndent()
        )

        db.query("SELECT uriString, fileName, isPinned FROM assets").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("content://media/1", cursor.getString(cursor.getColumnIndexOrThrow("uriString")))
            assertEquals("vid.mp4", cursor.getString(cursor.getColumnIndexOrThrow("fileName")))
            assertEquals(1, cursor.getInt(cursor.getColumnIndexOrThrow("isPinned")))
        }
    }

    @Test
    fun `migration 5 to 6 creates an empty project snapshots table`() {
        val db = openHelper.writableDatabase

        MIGRATION_3_4.migrate(db)
        MIGRATION_4_5.migrate(db)
        MIGRATION_5_6.migrate(db)

        db.query("SELECT COUNT(*) FROM project_snapshots").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals(0, cursor.getInt(0))
        }
    }

    @Test
    fun `project snapshots accept entity columns and preserve the project`() {
        val db = openHelper.writableDatabase

        MIGRATION_3_4.migrate(db)
        MIGRATION_4_5.migrate(db)
        db.execSQL(
            """
            INSERT INTO projects
                (id, name, type, thumbnailUri, sourceMediaUri, sourceAudioUri, configJson, status, createdAt, updatedAt)
            VALUES
                (7, 'Saved Project', 'loop', NULL, 'content://video/1', 'content://audio/1', '{"speed":2}', 'draft', 100, 200)
            """.trimIndent()
        )

        MIGRATION_5_6.migrate(db)
        db.execSQL(
            """
            INSERT INTO project_snapshots
                (projectId, label, configJson, sourceMediaUri, sourceAudioUri, createdAt)
            VALUES
                (7, 'Versi awal', '{"speed":1}', 'content://video/1', 'content://audio/1', 300)
            """.trimIndent()
        )

        db.query(
            """
            SELECT p.name, s.label, s.configJson
            FROM projects p
            JOIN project_snapshots s ON s.projectId = p.id
            WHERE p.id = 7
            """.trimIndent()
        ).use { cursor ->
            assertTrue("the existing project and its snapshot must be readable", cursor.moveToFirst())
            assertEquals("Saved Project", cursor.getString(cursor.getColumnIndexOrThrow("name")))
            assertEquals("Versi awal", cursor.getString(cursor.getColumnIndexOrThrow("label")))
            assertEquals("{\"speed\":1}", cursor.getString(cursor.getColumnIndexOrThrow("configJson")))
        }
    }
}
