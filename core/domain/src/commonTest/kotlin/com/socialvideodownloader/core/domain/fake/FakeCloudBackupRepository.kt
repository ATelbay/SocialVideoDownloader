package com.socialvideodownloader.core.domain.fake

import com.socialvideodownloader.core.domain.model.DownloadRecord
import com.socialvideodownloader.core.domain.repository.CloudBackupRepository

class FakeCloudBackupRepository : CloudBackupRepository {
    var records = mutableListOf<DownloadRecord>()
    var cloudRecordCount = 0
    var tierLimit = 1000
    var fetchException: Exception? = null

    override suspend fun uploadRecord(record: DownloadRecord): Boolean {
        records.add(record)
        cloudRecordCount++
        return true
    }

    override suspend fun deleteRecord(sourceUrlHash: String): Boolean {
        return true
    }

    override suspend fun fetchAllRecords(): List<DownloadRecord> {
        fetchException?.let { throw it }
        return records.toList()
    }

    override suspend fun getCloudRecordCount(): Int = cloudRecordCount

    override suspend fun getTierLimit(): Int = tierLimit

    override suspend fun updateTierLimit(limit: Int) {
        tierLimit = limit
    }

    override suspend fun evictOldestRecords(count: Int) {}

    override suspend fun setRecordCount(count: Int) {
        cloudRecordCount = count
    }
}
