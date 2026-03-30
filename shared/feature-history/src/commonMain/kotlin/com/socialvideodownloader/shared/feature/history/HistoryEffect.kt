package com.socialvideodownloader.shared.feature.history

/** One-shot side effects emitted by [SharedHistoryViewModel]. */
sealed interface HistoryEffect {
    data class OpenContent(val contentUri: String) : HistoryEffect

    data class ShareContent(val contentUri: String) : HistoryEffect

    /** Replaces @StringRes Int — platform layer resolves [HistoryMessageType] to a localized string. */
    data class ShowMessage(val messageType: HistoryMessageType) : HistoryEffect

    data class RetryDownload(val sourceUrl: String, val existingRecordId: Long) : HistoryEffect

    // US3: Billing — signal screen to show upgrade dialog (activity context needed for billing flow)
    data object LaunchUpgradeFlow : HistoryEffect

    // Google Sign-In — signal screen to launch Credential Manager
    data object LaunchGoogleSignIn : HistoryEffect
}

/** Typed message keys replacing @StringRes Int in history effects. */
enum class HistoryMessageType {
    DELETE_SUCCESS,
    DELETE_ALL_SUCCESS,
    COPY_URL_SUCCESS,
    CLOUD_SYNC_ERROR,
    FILE_UNAVAILABLE,
    DELETE_FILE_FAILED,
}
