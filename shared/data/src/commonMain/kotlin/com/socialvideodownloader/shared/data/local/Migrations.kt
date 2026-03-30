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
 * Backwards-compatibility note (T136): These migrations are compatible with
 * existing Android Room v5 databases. Room KMP uses the same underlying SQLite
 * schema format. An Android device upgrading to the KMP-based build will open
 * the existing database without any additional migration steps. The migration
 * history stored in the room_master_table is preserved.
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
            connection.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_sync_queue_downloadId_operation ON sync_queue (downloadId, operation)")
        }
    }

val ALL_MIGRATIONS = arrayOf(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
