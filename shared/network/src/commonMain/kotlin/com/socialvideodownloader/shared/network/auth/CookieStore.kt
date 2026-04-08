package com.socialvideodownloader.shared.network.auth

/**
 * Common interface for per-platform cookie storage.
 * Implemented by [SecureCookieStore] on each platform.
 * Using an interface allows fakes in commonTest without expect/actual test actuals.
 */
interface CookieStore {
    fun getCookies(platform: SupportedPlatform): String?

    fun setCookies(
        platform: SupportedPlatform,
        cookies: String,
    )

    fun clearCookies(platform: SupportedPlatform)

    fun isConnected(platform: SupportedPlatform): Boolean

    fun connectedPlatforms(): List<SupportedPlatform>
}
