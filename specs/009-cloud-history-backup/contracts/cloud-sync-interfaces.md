# Cloud Sync Interface Contracts

**Feature**: 009-cloud-history-backup
**Date**: 2026-03-21

## Domain Layer Interfaces (`:core:domain`)

### CloudBackupRepository

```kotlin
package com.socialvideodownloader.core.domain.repository

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
}
```

### SyncManager

```kotlin
package com.socialvideodownloader.core.domain.sync

interface SyncManager {
    /** Process all pending sync operations (uploads + deletes). */
    suspend fun processPendingOperations()

    /** Sync a single newly completed download. */
    suspend fun syncNewRecord(record: DownloadRecord)

    /** Queue a cloud deletion for a locally deleted record. */
    suspend fun queueDeletion(record: DownloadRecord)

    /** Observe sync status for UI indicator. */
    fun observeSyncStatus(): Flow<SyncStatus>
}

sealed interface SyncStatus {
    data object Idle : SyncStatus
    data object Syncing : SyncStatus
    data class Synced(val lastSyncTimestamp: Long) : SyncStatus
    data class Paused(val reason: String) : SyncStatus
    data class Error(val message: String) : SyncStatus
}
```

### CloudAuthService

```kotlin
package com.socialvideodownloader.core.domain.sync

interface CloudAuthService {
    /** Sign in anonymously. Returns UID on success. */
    suspend fun signInAnonymously(): String

    /** Get current UID, or null if not authenticated. */
    fun getCurrentUid(): String?

    /** Check if user is authenticated. */
    fun isAuthenticated(): Boolean
}
```

### EncryptionService

```kotlin
package com.socialvideodownloader.core.domain.sync

interface EncryptionService {
    /** Encrypt a DownloadRecord to a byte array. */
    fun encrypt(record: DownloadRecord): ByteArray

    /** Decrypt a byte array back to a DownloadRecord. */
    fun decrypt(data: ByteArray): DownloadRecord

    /** Check if the encryption key is valid and usable. */
    fun isKeyValid(): Boolean

    /** Regenerate the encryption key (invalidates old encrypted data). */
    fun regenerateKey()
}
```

### BillingRepository

```kotlin
package com.socialvideodownloader.core.domain.repository

interface BillingRepository {
    /** Observe current tier (reactive, updates on purchase/refund). */
    fun observeTier(): Flow<CloudTier>

    /** Check and restore purchases on app launch. */
    suspend fun restorePurchases(): CloudTier

    /** Initiate purchase flow. Requires Activity context. */
    suspend fun launchPurchaseFlow(activity: Activity): BillingResult
}

enum class CloudTier(val maxRecords: Int) {
    FREE(maxRecords = 1000),
    PAID(maxRecords = 10000),
}

sealed interface BillingResult {
    data object Success : BillingResult
    data object Cancelled : BillingResult
    data class Error(val message: String) : BillingResult
}
```

### BackupPreferences

```kotlin
package com.socialvideodownloader.core.domain.sync

interface BackupPreferences {
    /** Observe whether cloud backup is currently enabled. */
    fun observeIsBackupEnabled(): Flow<Boolean>

    /** Observe the last successful sync timestamp (epoch millis, 0 if never). */
    fun observeLastSyncTimestamp(): Flow<Long>

    /** Whether cloud backup has ever been enabled (gates Firebase init). */
    suspend fun hasEverEnabled(): Boolean

    suspend fun setBackupEnabled(enabled: Boolean)
    suspend fun setLastSyncTimestamp(timestamp: Long)
    suspend fun setHasEverEnabled(hasEver: Boolean)
}
```

## Use Cases (`:core:domain`)

### EnableCloudBackupUseCase
```kotlin
/** Toggle cloud backup on. Authenticates if needed, starts initial sync. */
class EnableCloudBackupUseCase @Inject constructor(
    private val authService: CloudAuthService,
    private val preferences: BackupPreferences,
    private val syncManager: SyncManager,
)
```

### DisableCloudBackupUseCase
```kotlin
/** Toggle cloud backup off. Stops sync, retains cloud records. */
class DisableCloudBackupUseCase @Inject constructor(
    private val preferences: BackupPreferences,
)
```

### RestoreFromCloudUseCase
```kotlin
/** Fetch all cloud records, decrypt, merge into local DB. */
class RestoreFromCloudUseCase @Inject constructor(
    private val cloudBackupRepository: CloudBackupRepository,
    private val downloadRepository: DownloadRepository,
)
```

### ObserveCloudCapacityUseCase
```kotlin
/** Observe current/max record count for capacity banner. */
class ObserveCloudCapacityUseCase @Inject constructor(
    private val cloudBackupRepository: CloudBackupRepository,
    private val billingRepository: BillingRepository,
)
```

## Firestore Security Rules

```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    // All user data scoped to their anonymous UID
    match /users/{userId}/{document=**} {
      allow read, write: if request.auth != null
                         && request.auth.uid == userId;
    }

    // Deny all other access
    match /{document=**} {
      allow read, write: if false;
    }
  }
}
```
