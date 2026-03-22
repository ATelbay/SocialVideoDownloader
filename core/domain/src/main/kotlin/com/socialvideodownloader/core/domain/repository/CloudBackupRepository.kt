package com.socialvideodownloader.core.domain.repository

import com.socialvideodownloader.core.domain.model.DownloadRecord

interface CloudBackupRepository {
    /** Upload a single record to cloud. Returns true on success. */
    suspend fun uploadRecord(record: DownloadRecord): Boolean

    /** Delete a single record from cloud by its sourceUrlHash. */
    suspend fun deleteRecord(sourceUrlHash: String): Boolean

    /** Fetch all cloud records, decrypted. For restore flow. */
    suspend fun fetchAllRecords(): List<DownloadRecord>

    /** Get current cloud record count. */
    suspend fun getCloudRecordCount(): Int

    /** Get the user's current tier limit. */
    suspend fun getTierLimit(): Int

    /** Update tier limit in cloud metadata. */
    suspend fun updateTierLimit(limit: Int)

    /** Evict oldest N records from cloud. */
    suspend fun evictOldestRecords(count: Int)

    /** Set the recorded count in the counters document to [count]. Used for reconciliation. */
    suspend fun setRecordCount(count: Int)
}
