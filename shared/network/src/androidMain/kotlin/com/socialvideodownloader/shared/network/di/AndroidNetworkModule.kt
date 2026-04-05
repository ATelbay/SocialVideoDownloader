package com.socialvideodownloader.shared.network.di

import com.socialvideodownloader.shared.network.auth.CookieStore
import com.socialvideodownloader.shared.network.auth.SecureCookieStore
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

/**
 * Android-specific Koin module for network layer.
 *
 * Provides [SecureCookieStore] bound as [CookieStore] using Android EncryptedSharedPreferences.
 * Relies on Koin's [androidContext] which is initialized via startKoin { androidContext(...) }
 * in the app's Application.onCreate().
 */
val androidNetworkModule =
    module {
        single<CookieStore> { SecureCookieStore(context = androidContext()) }
    }
