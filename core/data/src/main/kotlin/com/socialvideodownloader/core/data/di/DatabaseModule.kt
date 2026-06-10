package com.socialvideodownloader.core.data.di

import android.content.Context
import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.socialvideodownloader.shared.data.local.AppDatabase
import com.socialvideodownloader.shared.data.local.DownloadDao
import com.socialvideodownloader.shared.data.local.MIGRATION_1_2
import com.socialvideodownloader.shared.data.local.MIGRATION_2_3
import com.socialvideodownloader.shared.data.local.MIGRATION_3_4
import com.socialvideodownloader.shared.data.local.MIGRATION_4_5
import com.socialvideodownloader.shared.data.local.MIGRATION_5_6
import com.socialvideodownloader.shared.data.local.SyncQueueDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * On Android the single physical Room database is owned by the Hilt graph here, but the
 * schema/DAOs are the shared Room KMP [AppDatabase] in :shared:data. The builder mirrors
 * [com.socialvideodownloader.shared.data.platform.createDatabaseBuilder] exactly (same file
 * name + BundledSQLiteDriver) so existing users keep their history. The injected
 * [ApplicationContext] is used instead of the global `androidContext` lateinit so this does
 * not depend on Koin start-up ordering.
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideAppDatabase(
        @ApplicationContext context: Context,
    ): AppDatabase =
        Room.databaseBuilder<AppDatabase>(
            context = context,
            name = context.getDatabasePath("social-video-downloader-database").absolutePath,
        ).setDriver(BundledSQLiteDriver())
            .addMigrations(
                MIGRATION_1_2,
                MIGRATION_2_3,
                MIGRATION_3_4,
                MIGRATION_4_5,
                MIGRATION_5_6,
            ).build()

    @Provides
    fun provideDownloadDao(database: AppDatabase): DownloadDao = database.downloadDao()

    @Provides
    fun provideSyncQueueDao(database: AppDatabase): SyncQueueDao = database.syncQueueDao()
}
