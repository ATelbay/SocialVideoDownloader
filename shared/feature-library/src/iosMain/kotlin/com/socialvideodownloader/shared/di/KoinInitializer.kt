package com.socialvideodownloader.shared.di

import com.socialvideodownloader.shared.data.di.iosDataModule
import com.socialvideodownloader.shared.data.di.sharedDataModule
import com.socialvideodownloader.shared.feature.download.di.sharedDownloadModule
import com.socialvideodownloader.shared.feature.history.di.sharedHistoryModule
import com.socialvideodownloader.shared.feature.library.di.sharedLibraryModule
import com.socialvideodownloader.shared.network.di.networkModule
import org.koin.core.context.startKoin

/**
 * Initializes Koin with all shared and iOS-platform modules.
 *
 * Called from `SocialVideoDownloaderApp.init()` in Swift via the generated
 * Kotlin/Native framework.
 *
 * ```swift
 * KoinInitializerKt.doInitKoin()
 * ```
 *
 * TODO: Tech debt — this file lives in :shared:feature-library but aggregates all feature
 *   modules. It should be moved to a dedicated :shared:di or :shared:app-entry module
 *   alongside KoinHelper.kt. See KoinHelper.kt for full context.
 */
fun initKoin() {
    startKoin {
        modules(
            networkModule,
            sharedDataModule,
            iosDataModule,
            sharedDownloadModule,
            sharedHistoryModule,
            sharedLibraryModule,
        )
    }
}
