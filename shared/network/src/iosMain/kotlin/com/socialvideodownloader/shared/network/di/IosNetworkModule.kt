package com.socialvideodownloader.shared.network.di

import com.socialvideodownloader.shared.network.auth.CookieStore
import com.socialvideodownloader.shared.network.auth.SecureCookieStore
import org.koin.dsl.module

/**
 * iOS-specific Koin module for network layer.
 *
 * Provides [SecureCookieStore] bound as [CookieStore] using iOS Keychain (kSecClassGenericPassword).
 */
val iosNetworkModule =
    module {
        single { SecureCookieStore() }
        single<CookieStore> { get<SecureCookieStore>() }
    }
