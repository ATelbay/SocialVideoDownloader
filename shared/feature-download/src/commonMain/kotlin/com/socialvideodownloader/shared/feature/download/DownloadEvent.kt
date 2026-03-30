package com.socialvideodownloader.shared.feature.download

import com.socialvideodownloader.shared.data.platform.DownloadErrorType

/** One-shot side effects emitted by [SharedDownloadViewModel]. */
sealed interface DownloadEvent {
    data class OpenFile(val filePath: String) : DownloadEvent

    data class ShareFile(val filePath: String) : DownloadEvent

    data class ShowError(val errorType: DownloadErrorType, val message: String?) : DownloadEvent

    data object RequestNotificationPermission : DownloadEvent

    data class ShowSnackbarMessage(val message: String) : DownloadEvent
}
