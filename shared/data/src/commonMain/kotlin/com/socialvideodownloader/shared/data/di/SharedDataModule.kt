package com.socialvideodownloader.shared.data.di

import com.socialvideodownloader.core.domain.repository.DownloadRepository
import com.socialvideodownloader.shared.data.local.ALL_MIGRATIONS
import com.socialvideodownloader.shared.data.local.AppDatabase
import com.socialvideodownloader.shared.data.local.DownloadDao
import com.socialvideodownloader.shared.data.local.SyncQueueDao
import com.socialvideodownloader.shared.data.platform.createDatabaseBuilder
import com.socialvideodownloader.shared.data.repository.DownloadRepositoryImpl
import org.koin.dsl.module

/**
 * Shared Koin module providing the Room KMP database, DAOs, and shared
 * repository implementations.
 *
 * Platform-specific dependencies (database builder, platform abstractions)
 * are provided by [AndroidDataModule] or [IosDataModule].
 */
val sharedDataModule = module {

    single<AppDatabase> {
        createDatabaseBuilder()
            .addMigrations(*ALL_MIGRATIONS)
            .build()
    }

    single<DownloadDao> { get<AppDatabase>().downloadDao() }

    single<SyncQueueDao> { get<AppDatabase>().syncQueueDao() }

    single<DownloadRepositoryImpl> { DownloadRepositoryImpl(get()) }

    // Bind the shared DownloadRepositoryImpl as the DownloadRepository interface.
    // On Android, the Hilt bridge may override this with the sync-aware wrapper
    // from core/data if cloud backup is needed.
    single<DownloadRepository> { get<DownloadRepositoryImpl>() }
}
