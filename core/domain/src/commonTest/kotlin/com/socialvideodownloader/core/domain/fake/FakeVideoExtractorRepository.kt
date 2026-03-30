package com.socialvideodownloader.core.domain.fake

import com.socialvideodownloader.core.domain.model.DownloadRequest
import com.socialvideodownloader.core.domain.model.VideoMetadata
import com.socialvideodownloader.core.domain.repository.VideoExtractorRepository

class FakeVideoExtractorRepository : VideoExtractorRepository {
    var extractInfoResult: Result<VideoMetadata> = Result.failure(NotImplementedError())
    var downloadResult: String = ""
    var lastDownloadRequest: DownloadRequest? = null
    var lastDownloadCallback: ((Float, Long, String) -> Unit)? = null
    var cancelledProcessIds = mutableListOf<String>()

    override suspend fun extractInfo(url: String): VideoMetadata = extractInfoResult.getOrThrow()

    override suspend fun download(
        request: DownloadRequest,
        callback: (Float, Long, String) -> Unit,
    ): String {
        lastDownloadRequest = request
        lastDownloadCallback = callback
        return downloadResult
    }

    override fun cancelDownload(processId: String) {
        cancelledProcessIds.add(processId)
    }
}
