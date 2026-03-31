package com.socialvideodownloader.di

import android.content.Context
import com.socialvideodownloader.shared.data.di.androidDataModule
import com.socialvideodownloader.shared.data.di.sharedDataModule
import com.socialvideodownloader.shared.data.platform.androidContext
import com.socialvideodownloader.shared.feature.download.di.sharedDownloadModule
import com.socialvideodownloader.shared.feature.history.di.sharedHistoryModule
import com.socialvideodownloader.shared.feature.library.di.sharedLibraryModule
import com.socialvideodownloader.shared.network.di.networkModule
import org.koin.core.context.startKoin

/**
 * Initializes Koin DI for shared KMP modules.
 *
 * IMPORTANT (T137): This must be called BEFORE Hilt initialization in
 * Application.onCreate(). Since @HiltAndroidApp triggers Hilt setup in
 * super.onCreate(), we call initKoin() at the very beginning of onCreate()
 * before the super call would normally complete component initialization.
 * In practice, Hilt field injection happens after onCreate() returns,
 * so calling initKoin() first in onCreate() is sufficient.
 */
object KoinInitializer {
    fun init(context: Context) {
        // Set the Android context for the shared:data database factory
        androidContext = context.applicationContext

        startKoin {
            modules(
                // Shared KMP modules
                networkModule,
                sharedDataModule,
                // Android platform implementations
                androidDataModule,
                // Shared feature ViewModel modules
                sharedDownloadModule,
                sharedHistoryModule,
                sharedLibraryModule,
            )
        }
    }
}
