package com.socialvideodownloader.di

import com.socialvideodownloader.shared.data.platform.PlatformClipboard
import com.socialvideodownloader.shared.data.platform.PlatformDownloadManager
import com.socialvideodownloader.shared.data.platform.PlatformFileStorage
import com.socialvideodownloader.shared.data.platform.PlatformStringProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import org.koin.mp.KoinPlatform
import javax.inject.Singleton

/**
 * Hilt module that bridges Koin-managed shared KMP platform abstractions
 * into the Hilt dependency graph.
 *
 * Database-related bindings (AppDatabase, DownloadDao, SyncQueueDao) are
 * provided by [com.socialvideodownloader.core.data.di.DatabaseModule] to
 * avoid duplicate Hilt bindings.
 *
 * Each @Provides method calls KoinPlatform.getKoin().get<T>() to retrieve
 * the singleton instance managed by Koin.
 */
@Module
@InstallIn(SingletonComponent::class)
object KoinBridgeModule {

    @Provides
    @Singleton
    fun providePlatformDownloadManager(): PlatformDownloadManager =
        KoinPlatform.getKoin().get()

    @Provides
    @Singleton
    fun providePlatformFileStorage(): PlatformFileStorage =
        KoinPlatform.getKoin().get()

    @Provides
    @Singleton
    fun providePlatformClipboard(): PlatformClipboard =
        KoinPlatform.getKoin().get()

    @Provides
    @Singleton
    fun providePlatformStringProvider(): PlatformStringProvider =
        KoinPlatform.getKoin().get()
}
