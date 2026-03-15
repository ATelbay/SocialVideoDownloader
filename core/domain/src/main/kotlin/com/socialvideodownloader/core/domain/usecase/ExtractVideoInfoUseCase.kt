package com.socialvideodownloader.core.domain.usecase

import com.socialvideodownloader.core.domain.model.VideoMetadata
import com.socialvideodownloader.core.domain.repository.VideoExtractorRepository
import javax.inject.Inject

class ExtractVideoInfoUseCase
    @Inject
    constructor(
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
