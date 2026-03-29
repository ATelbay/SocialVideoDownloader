package com.socialvideodownloader.shared.data.platform

import androidx.room.RoomDatabase
import com.socialvideodownloader.shared.data.local.AppDatabase

/**
 * Platform-specific Room database builder factory.
 *
 * Android: Uses Room.databaseBuilder with application Context.
 * iOS: Uses Room.databaseBuilder with NSDocumentDirectory file path.
 */
expect fun createDatabaseBuilder(): RoomDatabase.Builder<AppDatabase>
