package com.socialvideodownloader.core.domain.model

data class DownloadRecord(
    val id: Long = 0,
    val sourceUrl: String,
    val videoTitle: String,
    val thumbnailUrl: String? = null,
    val formatLabel: String = "",
    val filePath: String? = null,
    val mediaStoreUri: String? = null,
    val status: DownloadStatus,
    val createdAt: Long,
    val completedAt: Long? = null,
    val fileSizeBytes: Long? = null,
    // TODO: Tech debt — syncStatus should be a sealed class or enum (e.g. SyncStatus.NotSynced,
    //   Synced, Pending) rather than a raw String. Requires updating DownloadEntity,
    //   DownloadMapper, and all ViewModels that read/write this field.
    val syncStatus: String = "NOT_SYNCED",
)
