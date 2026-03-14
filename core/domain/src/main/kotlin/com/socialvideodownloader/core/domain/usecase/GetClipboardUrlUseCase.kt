package com.socialvideodownloader.core.domain.usecase

import com.socialvideodownloader.core.domain.repository.ClipboardRepository
import javax.inject.Inject

class GetClipboardUrlUseCase
    @Inject
    constructor(
        private val repository: ClipboardRepository,
    ) {
        operator fun invoke(): String? = repository.getVideoUrl()
    }
