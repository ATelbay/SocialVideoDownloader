package com.socialvideodownloader.shared.data.di

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
