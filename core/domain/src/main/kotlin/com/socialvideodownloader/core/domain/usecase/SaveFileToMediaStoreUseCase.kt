package com.socialvideodownloader.core.domain.usecase

import com.socialvideodownloader.core.domain.repository.MediaStoreRepository
import javax.inject.Inject

class SaveFileToMediaStoreUseCase
    @Inject
    constructor(
        private val repository: MediaStoreRepository,
    ) {
        suspend operator fun invoke(
            tempFilePath: String,
            title: String,
            mimeType: String,
        ): String = repository.saveToDownloads(tempFilePath, title, mimeType)
    }
