package com.socialvideodownloader.shared.data

import androidx.sqlite.SQLiteConnection
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.sqlite.execSQL
import com.socialvideodownloader.shared.data.local.MIGRATION_5_6
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Verifies [MIGRATION_5_6] normalizes the `sync_queue` unique index name.
 *
 * Older :core:data builds created the index as `idx_sync_queue_download_op`, which
 * diverges from the entity-derived schema (`index_sync_queue_downloadId_operation`)
 * and would fail Room's schema validation when the Room KMP database opens such a
 * database. The migration must rename the legacy index to the canonical name while
 * being a no-op when the canonical name is already present.
 *
 * The migration SQL is exercised directly against an in-memory SQLite database via the
 * bundled driver. This lives in `iosTest` (not `commonTest`) because the bundled SQLite
 * native library is only loadable on iOS targets and on Android instrumentation — it
 * cannot be loaded by the host-JVM `testDebugUnitTest` task.
 */
class SyncQueueIndexMigrationTest {
    private val legacyIndex = "idx_sync_queue_download_op"
    private val canonicalIndex = "index_sync_queue_downloadId_operation"

    private lateinit var connection: SQLiteConnection

    @BeforeTest
    fun setUp() {
        connection = BundledSQLiteDriver().open(":memory:")
        connection.execSQL(
            "CREATE TABLE sync_queue (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "downloadId INTEGER NOT NULL, " +
                "operation TEXT NOT NULL, " +
                "createdAt INTEGER NOT NULL, " +
                "retryCount INTEGER NOT NULL DEFAULT 0, " +
                "lastError TEXT)",
        )
    }

    @AfterTest
    fun tearDown() {
        connection.close()
    }

    @Test
    fun renamesLegacyIndexToCanonical() {
        // Simulate a database migrated by an older :core:data 4->5 migration.
        connection.execSQL("CREATE UNIQUE INDEX $legacyIndex ON sync_queue (downloadId, operation)")

        MIGRATION_5_6.migrate(connection)

        val indexes = userIndexNames()
        assertTrue(canonicalIndex in indexes, "canonical index missing after migration: $indexes")
        assertFalse(legacyIndex in indexes, "legacy index not dropped: $indexes")
        assertTrue(uniqueConstraintEnforced(), "unique (downloadId, operation) constraint should survive")
    }

    @Test
    fun isNoOpWhenCanonicalIndexAlreadyPresent() {
        // Simulate a fresh install / database created by the Room KMP schema directly.
        connection.execSQL("CREATE UNIQUE INDEX $canonicalIndex ON sync_queue (downloadId, operation)")

        MIGRATION_5_6.migrate(connection)

        val indexes = userIndexNames()
        assertTrue(canonicalIndex in indexes, "canonical index missing: $indexes")
        assertFalse(legacyIndex in indexes, "legacy index unexpectedly present: $indexes")
        assertTrue(uniqueConstraintEnforced(), "unique (downloadId, operation) constraint should survive")
    }

    /** Index names declared in code (excludes SQLite's auto-generated `sqlite_autoindex_*`). */
    private fun userIndexNames(): List<String> {
        val names = mutableListOf<String>()
        val statement =
            connection.prepare(
                "SELECT name FROM sqlite_master WHERE type = 'index' AND tbl_name = 'sync_queue'",
            )
        try {
            while (statement.step()) {
                val name = statement.getText(0)
                if (!name.startsWith("sqlite_autoindex")) names += name
            }
        } finally {
            statement.close()
        }
        return names
    }

    private fun uniqueConstraintEnforced(): Boolean {
        connection.execSQL("DELETE FROM sync_queue")
        connection.execSQL("INSERT INTO sync_queue (downloadId, operation, createdAt) VALUES (1, 'UPLOAD', 0)")
        return try {
            connection.execSQL("INSERT INTO sync_queue (downloadId, operation, createdAt) VALUES (1, 'UPLOAD', 0)")
            false
        } catch (_: Throwable) {
            true
        }
    }
}
