package com.socialvideodownloader.shared.data.platform

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.socialvideodownloader.shared.data.local.AppDatabase

/**
 * Android database builder using Room.databaseBuilder with application Context.
 *
 * The database name matches the existing Android Room database so that
 * existing users retain their download history after the KMP migration.
 */
lateinit var androidContext: Context

actual fun createDatabaseBuilder(): RoomDatabase.Builder<AppDatabase> {
    return Room.databaseBuilder<AppDatabase>(
        context = androidContext,
        name = androidContext.getDatabasePath("social-video-downloader-database").absolutePath,
    ).setDriver(BundledSQLiteDriver())
}
