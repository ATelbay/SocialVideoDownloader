package com.socialvideodownloader.core.domain.repository

interface MediaStoreRepository {
    suspend fun saveToDownloads(
        tempFilePath: String,
        title: String,
        mimeType: String,
    ): String
}
