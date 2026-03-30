package com.socialvideodownloader.core.domain.usecase

import com.socialvideodownloader.core.domain.model.VideoMetadata
import com.socialvideodownloader.core.domain.repository.VideoExtractorRepository

class ExtractVideoInfoUseCase(
    private val repository: VideoExtractorRepository,
) {
    suspend operator fun invoke(url: String): Result<VideoMetadata> =
        runCatching {
            if (!url.startsWith("http://") && !url.startsWith("https://")) {
                throw IllegalArgumentException("Only HTTP and HTTPS URLs are supported")
            }
            repository.extractInfo(url)
        }
}
