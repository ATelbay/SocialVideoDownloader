package com.socialvideodownloader.shared.data.local

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor

@Database(
    entities = [DownloadEntity::class, SyncQueueEntity::class],
    version = 5,
    exportSchema = true,
)
@ConstructedBy(AppDatabaseConstructor::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun downloadDao(): DownloadDao

    abstract fun syncQueueDao(): SyncQueueDao
}

// Room compiler auto-generates the actual implementations for each platform.
// No manual actual declaration is needed.
@Suppress("KotlinNoActualForExpect")
expect object AppDatabaseConstructor : RoomDatabaseConstructor<AppDatabase>
