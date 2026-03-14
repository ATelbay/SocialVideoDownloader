package com.socialvideodownloader.core.domain.usecase

import com.socialvideodownloader.core.domain.repository.VideoExtractorRepository
import javax.inject.Inject

class CancelDownloadUseCase
    @Inject
    constructor(
        private val repository: VideoExtractorRepository,
    ) {
        operator fun invoke(requestId: String) {
            repository.cancelDownload(requestId)
        }
    }
