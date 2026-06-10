package com.socialvideodownloader.shared.data.di

import com.socialvideodownloader.shared.data.platform.AndroidClipboard
import com.socialvideodownloader.shared.data.platform.AndroidDownloadManager
import com.socialvideodownloader.shared.data.platform.AndroidFileStorage
import com.socialvideodownloader.shared.data.platform.AndroidStringProvider
import com.socialvideodownloader.shared.data.platform.PlatformClipboard
import com.socialvideodownloader.shared.data.platform.PlatformDownloadManager
import com.socialvideodownloader.shared.data.platform.PlatformFileStorage
import com.socialvideodownloader.shared.data.platform.PlatformStringProvider
import com.socialvideodownloader.shared.data.platform.androidContext
import kotlinx.coroutines.Dispatchers
import org.koin.dsl.module

/**
 * Android-specific Koin module providing platform implementations.
 *
 * Requires androidContext to be set before this module is loaded.
 * This is handled by KoinInitializer in the app module.
 */
val androidDataModule =
    module {
        single<PlatformDownloadManager> {
            AndroidDownloadManager(context = androidContext)
        }

        single<PlatformFileStorage> {
            // Dispatchers.IO is supplied here at the DI composition root, not hardcoded
            // inside the adapter, so the storage class stays testable.
            AndroidFileStorage(context = androidContext, ioDispatcher = Dispatchers.IO)
        }

        single<PlatformClipboard> {
            AndroidClipboard(context = androidContext)
        }

        single<PlatformStringProvider> {
            AndroidStringProvider(context = androidContext)
        }
    }
