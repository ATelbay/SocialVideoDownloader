package com.socialvideodownloader.core.domain.usecase

import com.socialvideodownloader.core.domain.model.DownloadRequest
import com.socialvideodownloader.core.domain.repository.VideoExtractorRepository
import javax.inject.Inject

class DownloadVideoUseCase
    @Inject
    constructor(
        private val repository: VideoExtractorRepository,
    ) {
        suspend operator fun invoke(
            request: DownloadRequest,
            callback: (Float, Long, String) -> Unit,
        ): String = repository.download(request, callback)
    }
