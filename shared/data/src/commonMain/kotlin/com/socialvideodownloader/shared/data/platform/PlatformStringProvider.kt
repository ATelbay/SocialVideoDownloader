package com.socialvideodownloader.shared.data.platform

/**
 * Platform abstraction for localized string resolution.
 *
 * Replaces the @StringRes Int pattern used in Android ViewModels.
 * Shared ViewModels emit StringKey values in effects; platform UI layers
 * resolve them to localized strings.
 *
 * Android: Maps StringKey to R.string.* resource IDs.
 * iOS: Maps StringKey to NSLocalizedString keys.
 */
interface PlatformStringProvider {
    /** Resolve a platform-localized string by typed key. */
    fun getString(key: StringKey): String
}

/** Typed string keys for platform-independent string references. */
enum class StringKey {
    ERROR_NETWORK,
    ERROR_SERVER_UNAVAILABLE,
    ERROR_EXTRACTION_FAILED,
    ERROR_UNSUPPORTED_URL,
    ERROR_STORAGE_FULL,
    ERROR_DOWNLOAD_FAILED,
    ERROR_UNKNOWN,
    HISTORY_DELETED,
    HISTORY_ALL_DELETED,
    HISTORY_RESTORED,
    LIBRARY_OPEN_ERROR,
    COPY_SUCCESS,
}
