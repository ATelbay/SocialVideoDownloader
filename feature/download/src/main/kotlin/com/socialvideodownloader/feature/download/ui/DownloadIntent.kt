package com.socialvideodownloader.feature.download.ui

sealed interface DownloadIntent {
    data class UrlChanged(val url: String) : DownloadIntent
    data object ExtractClicked : DownloadIntent
    data class FormatSelected(val formatId: String) : DownloadIntent
    data object DownloadClicked : DownloadIntent
    data object CancelDownloadClicked : DownloadIntent
    data object RetryClicked : DownloadIntent
    data object OpenFileClicked : DownloadIntent
    data object ShareFileClicked : DownloadIntent
    data object NewDownloadClicked : DownloadIntent
    data class ClipboardUrlDetected(val url: String) : DownloadIntent
    data class PrefillUrl(val url: String) : DownloadIntent
}
