package com.socialvideodownloader.feature.history.ui

import androidx.annotation.StringRes

sealed interface HistoryEffect {
    data class OpenContent(val contentUri: String) : HistoryEffect
    data class ShareContent(val contentUri: String) : HistoryEffect
    data class ShowMessage(@StringRes val messageResId: Int) : HistoryEffect
    data class RetryDownload(val sourceUrl: String) : HistoryEffect
}
