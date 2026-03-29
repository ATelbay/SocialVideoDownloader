package com.socialvideodownloader.shared.data.repository

import com.socialvideodownloader.core.domain.model.DownloadRequest
import com.socialvideodownloader.core.domain.model.VideoMetadata
import com.socialvideodownloader.core.domain.repository.VideoExtractorRepository
import com.socialvideodownloader.shared.network.ServerVideoExtractorApi

/**
 * iOS implementation of [VideoExtractorRepository].
 *
 * Always uses the yt-dlp API server for extraction and download.
 * There is no local yt-dlp fallback on iOS (Apple prohibits embedded interpreters).
 */
class ServerOnlyVideoExtractorRepository(
    private val serverApi: ServerVideoExtractorApi,
) : VideoExtractorRepository {

    override suspend fun extractInfo(url: String): VideoMetadata {
        return serverApi.extractInfo(url)
    }

    override suspend fun download(
        request: DownloadRequest,
        callback: (Float, Long, String) -> Unit,
    ): String {
        val downloadUrl = request.directDownloadUrl
            ?: throw IllegalStateException(
                "iOS downloads require a directDownloadUrl from the server. " +
                    "Extract info first to obtain format-specific download URLs.",
            )

        return serverApi.downloadFile(
            url = downloadUrl,
            outputPath = request.videoTitle,
            requestId = request.id,
            onProgress = callback,
        )
    }

    override fun cancelDownload(processId: String) {
        // iOS download cancellation is handled by PlatformDownloadManager
        // (URLSession task cancellation), not at the repository level.
    }
}
