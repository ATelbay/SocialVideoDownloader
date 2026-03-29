package com.socialvideodownloader.core.domain.usecase

import com.socialvideodownloader.core.domain.repository.VideoExtractorRepository

class CancelDownloadUseCase(
    private val repository: VideoExtractorRepository,
) {
    operator fun invoke(requestId: String) {
        repository.cancelDownload(requestId)
    }
}
