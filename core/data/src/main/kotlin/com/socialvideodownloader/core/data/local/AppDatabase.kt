package com.socialvideodownloader.core.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [DownloadEntity::class, SyncQueueEntity::class],
    version = 5,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun downloadDao(): DownloadDao

    abstract fun syncQueueDao(): SyncQueueDao

    companion object {
        val MIGRATION_1_2 =
            object : Migration(1, 2) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    db.execSQL("ALTER TABLE downloads ADD COLUMN formatLabel TEXT NOT NULL DEFAULT ''")
                }
            }

        val MIGRATION_2_3 =
            object : Migration(2, 3) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    db.execSQL("ALTER TABLE downloads ADD COLUMN mediaStoreUri TEXT")
                }
            }

        val MIGRATION_3_4 =
            object : Migration(3, 4) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    db.execSQL(
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
                override fun migrate(db: SupportSQLiteDatabase) {
                    db.execSQL("ALTER TABLE downloads ADD COLUMN syncStatus TEXT NOT NULL DEFAULT 'NOT_SYNCED'")
                    db.execSQL(
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
                    db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS idx_sync_queue_download_op ON sync_queue (downloadId, operation)")
                }
            }
    }
}
