package com.socialvideodownloader.core.domain.usecase

import com.socialvideodownloader.core.domain.model.DownloadRecord
import com.socialvideodownloader.core.domain.repository.DownloadRepository
import javax.inject.Inject

class SaveDownloadRecordUseCase
    @Inject
    constructor(
        private val repository: DownloadRepository,
    ) {
        suspend operator fun invoke(record: DownloadRecord): Long = repository.insert(record)
    }
