package com.example.core.database

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.io.IOException

@RunWith(AndroidJUnit4::class)
@Config(sdk = [35])
class AppDatabaseMigrationTest {

    private val TEST_DB = "migration-test"

    @get:Rule
    val helper: MigrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AppDatabase::class.java.canonicalName,
        FrameworkSQLiteOpenHelperFactory()
    )

    @Test
    @Throws(IOException::class)
    fun migrate3To4() {
        // Create the database with version 3
        var db = helper.createDatabase(TEST_DB, 3)

        // Insert some data into the old schema
        db.execSQL(
            """
            INSERT INTO render_jobs (id, jobType, title, inputUri, outputUri, style, status, progress, durationSec, fileSizeMb, createdAt, paramsSummary)
            VALUES (1, 'LOOP', 'Test Render', 'content://1', '/out.mp4', 'NORMAL', 'COMPLETED', 100, 10.0, 1.5, 1234567890, 'summary')
            """.trimIndent()
        )
        db.close()

        // Run the migration to version 4
        db = helper.runMigrationsAndValidate(TEST_DB, 4, true, MIGRATION_3_4)

        // Verify the old data is preserved and the new column is nullable (null by default)
        val cursor = db.query("SELECT * FROM render_jobs WHERE id = 1")
        assertEquals(true, cursor.moveToFirst())
        assertEquals("Test Render", cursor.getString(cursor.getColumnIndexOrThrow("title")))
        assertEquals(true, cursor.isNull(cursor.getColumnIndexOrThrow("projectId")))
        cursor.close()

        // Verify the new table exists
        val projectsCursor = db.query("SELECT * FROM projects")
        assertEquals(false, projectsCursor.moveToFirst()) // Table should be empty
        projectsCursor.close()
    }
}
