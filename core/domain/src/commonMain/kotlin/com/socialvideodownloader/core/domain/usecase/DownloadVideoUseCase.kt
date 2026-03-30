package com.socialvideodownloader.core.domain.usecase

import com.socialvideodownloader.core.domain.model.DownloadRequest
import com.socialvideodownloader.core.domain.repository.VideoExtractorRepository

class DownloadVideoUseCase(
    private val repository: VideoExtractorRepository,
) {
    suspend operator fun invoke(
        request: DownloadRequest,
        callback: (Float, Long, String) -> Unit,
    ): String = repository.download(request, callback)
}
