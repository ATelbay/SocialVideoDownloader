package com.socialvideodownloader.feature.history.ui

sealed interface HistoryEffect {
    data class OpenContent(val contentUri: String) : HistoryEffect
    data class ShareContent(val contentUri: String) : HistoryEffect
    data class ShowMessage(val message: String) : HistoryEffect
}
