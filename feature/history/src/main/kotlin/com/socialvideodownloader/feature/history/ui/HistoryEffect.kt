package com.socialvideodownloader.feature.history.ui

import androidx.annotation.StringRes

sealed interface HistoryEffect {
    data class OpenContent(val contentUri: String) : HistoryEffect
    data class ShareContent(val contentUri: String) : HistoryEffect
    data class ShowMessage(@StringRes val messageResId: Int) : HistoryEffect
    data class RetryDownload(val sourceUrl: String, val existingRecordId: Long) : HistoryEffect
    // US3: Billing — signal screen to show upgrade dialog (activity context needed for billing flow)
    data object LaunchUpgradeFlow : HistoryEffect
    // Google Sign-In — signal screen to launch Credential Manager
    data object LaunchGoogleSignIn : HistoryEffect
}
