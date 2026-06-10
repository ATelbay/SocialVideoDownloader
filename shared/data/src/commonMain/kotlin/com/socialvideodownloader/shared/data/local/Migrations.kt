package com.socialvideodownloader.shared.data.local

import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

/**
 * Room KMP migrations using SQLiteConnection API.
 *
 * SQL statements are identical to the original Android Room migrations.
 * Only the API surface changed: SupportSQLiteDatabase -> SQLiteConnection.
 *
 * Backwards-compatibility note: Room KMP uses the same underlying SQLite schema
 * format as the Android-only :core:data database, so the migration history in
 * room_master_table is preserved. One divergence had to be reconciled — the
 * `sync_queue` unique index name (see [MIGRATION_5_6]). Until that migration runs,
 * databases migrated by older :core:data builds carry the legacy index name
 * `idx_sync_queue_download_op` and would FAIL this module's schema validation.
 * [MIGRATION_5_6] normalizes it; both modules must ship database version 6.
 */
val MIGRATION_1_2 =
    object : Migration(1, 2) {
        override fun migrate(connection: SQLiteConnection) {
            connection.execSQL("ALTER TABLE downloads ADD COLUMN formatLabel TEXT NOT NULL DEFAULT ''")
        }
    }

val MIGRATION_2_3 =
    object : Migration(2, 3) {
        override fun migrate(connection: SQLiteConnection) {
            connection.execSQL("ALTER TABLE downloads ADD COLUMN mediaStoreUri TEXT")
        }
    }

val MIGRATION_3_4 =
    object : Migration(3, 4) {
        override fun migrate(connection: SQLiteConnection) {
            connection.execSQL(
                """
                UPDATE downloads
                SET mediaStoreUri = filePath, filePath = NULL
                WHERE filePath LIKE 'content://%'
                AND mediaStoreUri IS NULL
                """.trimIndent(),
            )
        }
    }

val MIGRATION_4_5 =
    object : Migration(4, 5) {
        override fun migrate(connection: SQLiteConnection) {
            connection.execSQL("ALTER TABLE downloads ADD COLUMN syncStatus TEXT NOT NULL DEFAULT 'NOT_SYNCED'")
            connection.execSQL(
                """
                CREATE TABLE IF NOT EXISTS sync_queue (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    downloadId INTEGER NOT NULL,
                    operation TEXT NOT NULL,
                    createdAt INTEGER NOT NULL,
                    retryCount INTEGER NOT NULL DEFAULT 0,
                    lastError TEXT
                )
                """.trimIndent(),
            )
            connection.execSQL(
                "CREATE UNIQUE INDEX IF NOT EXISTS index_sync_queue_downloadId_operation ON sync_queue (downloadId, operation)",
            )
        }
    }

/**
 * Normalizes the `sync_queue` unique index name to Room's generated default.
 *
 * The Android-only [com.socialvideodownloader.core.data] module historically created this
 * index as `idx_sync_queue_download_op` in its 4→5 migration, which diverges from the
 * entity-derived schema (`index_sync_queue_downloadId_operation`). When this KMP database
 * opens an existing Android database that went through that path, Room's schema validation
 * fails on the index-name mismatch. This migration drops the legacy index (if present) and
 * recreates it under the canonical name so the shared schema validates cleanly.
 *
 * On a fresh install or a database created by this KMP module the index already has the
 * canonical name, so both statements are no-ops.
 */
val MIGRATION_5_6 =
    object : Migration(5, 6) {
        override fun migrate(connection: SQLiteConnection) {
            connection.execSQL("DROP INDEX IF EXISTS idx_sync_queue_download_op")
            connection.execSQL(
                "CREATE UNIQUE INDEX IF NOT EXISTS index_sync_queue_downloadId_operation " +
                    "ON sync_queue (downloadId, operation)",
            )
        }
    }

val ALL_MIGRATIONS = arrayOf(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6)
