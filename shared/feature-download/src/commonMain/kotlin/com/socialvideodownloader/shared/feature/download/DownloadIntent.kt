package com.socialvideodownloader.shared.feature.download

import com.socialvideodownloader.shared.network.auth.SupportedPlatform

sealed interface DownloadIntent {
    data class UrlChanged(val url: String) : DownloadIntent

    data object ExtractClicked : DownloadIntent

    data class FormatSelected(val formatId: String) : DownloadIntent

    data object DownloadClicked : DownloadIntent

    data object ShareFormatClicked : DownloadIntent

    data object CancelDownloadClicked : DownloadIntent

    data object RetryClicked : DownloadIntent

    data object OpenFileClicked : DownloadIntent

    data object ShareFileClicked : DownloadIntent

    data object NewDownloadClicked : DownloadIntent

    data class PrefillUrl(val url: String, val existingRecordId: Long? = null) : DownloadIntent

    data object OpenExistingClicked : DownloadIntent

    data object ShareExistingClicked : DownloadIntent

    data object DismissExistingBanner : DownloadIntent

    data object BackToIdleClicked : DownloadIntent

    data class ConnectPlatformClicked(val platform: SupportedPlatform) : DownloadIntent

    data class PlatformLoginResult(val platform: SupportedPlatform, val success: Boolean) : DownloadIntent
}
