package com.socialvideodownloader.core.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "downloads")
data class DownloadEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val sourceUrl: String,
    val videoTitle: String,
    val thumbnailUrl: String?,
    val formatLabel: String = "",
    val filePath: String?,
    val mediaStoreUri: String? = null,
    val status: String,
    val createdAt: Long,
    val completedAt: Long?,
    val fileSizeBytes: Long?,
    val syncStatus: String = "NOT_SYNCED",
)
