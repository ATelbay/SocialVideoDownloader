package com.socialvideodownloader.shared.network.auth

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

actual class SecureCookieStore(context: Context) {
    private val prefs: SharedPreferences by lazy {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            FILE_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    }

    actual fun getCookies(platform: SupportedPlatform): String? =
        prefs.getString(platform.storageKey, null)

    actual fun setCookies(platform: SupportedPlatform, cookies: String) {
        prefs.edit().putString(platform.storageKey, cookies).apply()
    }

    actual fun clearCookies(platform: SupportedPlatform) {
        prefs.edit().remove(platform.storageKey).apply()
    }

    actual fun isConnected(platform: SupportedPlatform): Boolean =
        prefs.getString(platform.storageKey, null) != null

    actual fun connectedPlatforms(): List<SupportedPlatform> =
        SupportedPlatform.entries.filter { isConnected(it) }

    companion object {
        private const val FILE_NAME = "svd_platform_cookies"
    }
}

private val SupportedPlatform.storageKey: String
    get() = "cookies_${name.lowercase()}"
