package com.socialvideodownloader.core.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface SyncQueueDao {
    @Query("SELECT * FROM sync_queue ORDER BY createdAt ASC")
    suspend fun getAll(): List<SyncQueueEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(entity: SyncQueueEntity)

    @Query("DELETE FROM sync_queue WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("UPDATE sync_queue SET retryCount = :retryCount, lastError = :lastError WHERE id = :id")
    suspend fun updateRetry(id: Long, retryCount: Int, lastError: String?)

    @Query("DELETE FROM sync_queue WHERE retryCount >= :maxRetries")
    suspend fun deleteFailedOperations(maxRetries: Int = 5)
}
