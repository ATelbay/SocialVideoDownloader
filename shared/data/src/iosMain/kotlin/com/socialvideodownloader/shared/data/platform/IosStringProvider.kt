package com.socialvideodownloader.shared.data.platform

import platform.Foundation.NSBundle
import platform.Foundation.NSLocalizedString

/**
 * iOS implementation of [PlatformStringProvider].
 *
 * Maps [StringKey] values to NSLocalizedString keys defined in Localizable.strings.
 * Falls back to hardcoded English strings if the key is missing from the bundle.
 */
class IosStringProvider : PlatformStringProvider {
    override fun getString(key: StringKey): String {
        val locKey = key.toLocalizableKey()
        val resolved = NSLocalizedString(locKey, bundle = NSBundle.mainBundle, value = "", comment = "")
        // If NSLocalizedString returns the key itself, the string is missing — use fallback.
        return if (resolved == locKey) key.defaultEnglish() else resolved
    }

    private fun StringKey.toLocalizableKey(): String =
        when (this) {
            StringKey.ERROR_NETWORK -> "error_network"
            StringKey.ERROR_SERVER_UNAVAILABLE -> "error_server_unavailable"
            StringKey.ERROR_EXTRACTION_FAILED -> "error_extraction_failed"
            StringKey.ERROR_UNSUPPORTED_URL -> "error_unsupported_url"
            StringKey.ERROR_STORAGE_FULL -> "error_storage_full"
            StringKey.ERROR_DOWNLOAD_FAILED -> "error_download_failed"
            StringKey.ERROR_UNKNOWN -> "error_unknown"
            StringKey.HISTORY_DELETED -> "history_deleted"
            StringKey.HISTORY_ALL_DELETED -> "history_all_deleted"
            StringKey.HISTORY_RESTORED -> "history_restored"
            StringKey.LIBRARY_OPEN_ERROR -> "library_open_error"
            StringKey.COPY_SUCCESS -> "copy_success"
        }

    private fun StringKey.defaultEnglish(): String =
        when (this) {
            StringKey.ERROR_NETWORK -> "Network error. Check your connection and try again."
            StringKey.ERROR_SERVER_UNAVAILABLE -> "Download server is unavailable. Try again later."
            StringKey.ERROR_EXTRACTION_FAILED -> "Could not extract video info. The video may be private or unavailable."
            StringKey.ERROR_UNSUPPORTED_URL -> "This URL is not supported."
            StringKey.ERROR_STORAGE_FULL -> "Not enough storage space to save the download."
            StringKey.ERROR_DOWNLOAD_FAILED -> "Download failed. Please try again."
            StringKey.ERROR_UNKNOWN -> "Something went wrong. Please try again."
            StringKey.HISTORY_DELETED -> "Download deleted."
            StringKey.HISTORY_ALL_DELETED -> "All downloads deleted."
            StringKey.HISTORY_RESTORED -> "Download restored."
            StringKey.LIBRARY_OPEN_ERROR -> "Could not open file."
            StringKey.COPY_SUCCESS -> "URL copied to clipboard."
        }
}
