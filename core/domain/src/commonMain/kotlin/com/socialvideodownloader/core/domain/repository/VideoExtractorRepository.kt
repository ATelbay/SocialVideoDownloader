package com.socialvideodownloader.core.domain.repository

import com.socialvideodownloader.core.domain.model.DownloadRequest
import com.socialvideodownloader.core.domain.model.VideoMetadata

interface VideoExtractorRepository {
    suspend fun extractInfo(url: String): VideoMetadata

    suspend fun download(
        request: DownloadRequest,
        callback: (Float, Long, String) -> Unit,
    ): String

    fun cancelDownload(processId: String)
}
