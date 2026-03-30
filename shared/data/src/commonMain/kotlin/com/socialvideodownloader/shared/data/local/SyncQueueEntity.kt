package com.socialvideodownloader.shared.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "sync_queue",
    indices = [Index(value = ["downloadId", "operation"], unique = true)],
)
data class SyncQueueEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val downloadId: Long,
    // "UPLOAD" or "DELETE"
    val operation: String,
    val createdAt: Long,
    val retryCount: Int = 0,
    val lastError: String? = null,
)
