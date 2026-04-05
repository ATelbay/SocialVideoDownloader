package com.socialvideodownloader.shared.network.auth

/**
 * Per-platform encrypted storage for Netscape-format cookie strings.
 * Platform implementations:
 * - Android: EncryptedSharedPreferences (AES-256, Keystore-backed)
 * - iOS: Keychain (kSecClassGenericPassword)
 */
expect class SecureCookieStore {
    /** Returns stored Netscape cookie string for the platform, or null if not connected. */
    fun getCookies(platform: SupportedPlatform): String?

    /** Stores a Netscape cookie string for the platform. */
    fun setCookies(platform: SupportedPlatform, cookies: String)

    /** Removes stored cookies for the platform. */
    fun clearCookies(platform: SupportedPlatform)

    /** Returns true if cookies exist for the platform. */
    fun isConnected(platform: SupportedPlatform): Boolean

    /** Returns all platforms that have stored cookies. */
    fun connectedPlatforms(): List<SupportedPlatform>
}
