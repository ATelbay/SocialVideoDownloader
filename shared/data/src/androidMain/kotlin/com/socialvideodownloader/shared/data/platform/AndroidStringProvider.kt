package com.socialvideodownloader.shared.data.platform

import android.content.Context

/**
 * Android implementation of [PlatformStringProvider].
 *
 * Maps [StringKey] enum values to Android string resource IDs and resolves
 * them via Context.getString(). This keeps the shared ViewModels decoupled
 * from Android's R.string.* references.
 *
 * Note: The actual R.string.* resource IDs are referenced by name convention.
 * The mapping uses a when-expression that will produce a compile error if
 * a new StringKey is added without a corresponding mapping.
 */
class AndroidStringProvider(
    private val context: Context,
) : PlatformStringProvider {
    override fun getString(key: StringKey): String {
        val resId = getStringResId(key)
        return if (resId != 0) {
            context.getString(resId)
        } else {
            // Fallback: use the enum name as a readable string
            key.name.lowercase().replace('_', ' ')
        }
    }

    private fun getStringResId(key: StringKey): Int {
        val resName =
            when (key) {
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
        return context.resources.getIdentifier(resName, "string", context.packageName)
    }
}
