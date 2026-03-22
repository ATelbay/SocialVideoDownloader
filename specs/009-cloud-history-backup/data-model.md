# Data Model: Cloud History Backup

**Feature**: 009-cloud-history-backup
**Date**: 2026-03-21

## Entity Relationship Overview

```
DownloadRecord (existing, extended)
    ├── 1:1 → SyncState (sync tracking)
    └── 1:1 → CloudHistoryRecord (encrypted cloud copy)

CloudTier (user's capacity)
    └── determines eviction threshold

SyncQueue (pending operations)
    └── references DownloadRecord.id
```

## Entities

### 1. DownloadEntity (MODIFIED — existing table)

**Table**: `downloads`
**Change**: Add `syncStatus` column with Room migration.

| Field          | Type     | Nullable | Default      | Notes                              |
|----------------|----------|----------|--------------|------------------------------------|
| id             | Long     | No       | autoGenerate | PK                                 |
| sourceUrl      | String   | No       |              | Original video URL                 |
| videoTitle     | String   | No       |              |                                    |
| thumbnailUrl   | String?  | Yes      | null         |                                    |
| formatLabel    | String   | No       | ""           |                                    |
| filePath       | String?  | Yes      | null         | Legacy file path                   |
| mediaStoreUri  | String?  | Yes      | null         | MediaStore content URI             |
| status         | String   | No       |              | DownloadStatus enum name           |
| createdAt      | Long     | No       |              | Unix epoch millis                  |
| completedAt    | Long?    | Yes      | null         |                                    |
| fileSizeBytes  | Long?    | Yes      | null         |                                    |
| **syncStatus** | **String** | **No** | **"NOT_SYNCED"** | **NEW: NOT_SYNCED, SYNCED, PENDING_DELETE** |

**Migration**: `ALTER TABLE downloads ADD COLUMN syncStatus TEXT NOT NULL DEFAULT 'NOT_SYNCED'`

**State transitions**:
```
NOT_SYNCED → SYNCED        (after successful upload)
SYNCED → PENDING_DELETE    (when user deletes locally, cloud delete queued)
PENDING_DELETE → (deleted) (after cloud delete confirmed, row removed)
NOT_SYNCED → NOT_SYNCED    (cloud backup disabled, stays unsynced)
```

### 2. SyncQueueEntity (NEW)

**Table**: `sync_queue`
**Purpose**: Queue of pending cloud operations (upload, delete) with retry tracking.

| Field       | Type    | Nullable | Default      | Notes                       |
|-------------|---------|----------|--------------|-----------------------------|
| id          | Long    | No       | autoGenerate | PK                          |
| downloadId  | Long    | No       |              | FK → downloads.id           |
| operation   | String  | No       |              | UPLOAD or DELETE             |
| createdAt   | Long    | No       |              | When operation was queued    |
| retryCount  | Int     | No       | 0            | Number of failed attempts    |
| lastError   | String? | Yes      | null         | Last error message for debug |

**Uniqueness**: One pending operation per downloadId+operation pair.

**Lifecycle**:
- Created when a download completes (UPLOAD) or user deletes a synced record (DELETE)
- Processed by sync worker when online
- Deleted after successful cloud operation
- Retry with exponential backoff (max 5 retries, then marked as failed)

### 3. CloudHistoryRecord (Firestore document — NOT a Room entity)

**Collection**: `users/{uid}/history/{documentId}`
**Purpose**: Encrypted backup of a download record in Firestore.

| Field            | Type      | Notes                                          |
|------------------|-----------|-------------------------------------------------|
| encryptedPayload | Blob      | AES-256-GCM encrypted JSON (Android Keystore) of DownloadRecord |
| createdAt        | Timestamp | Plaintext — needed for LRU eviction ordering    |
| sourceUrlHash    | String    | SHA-256(sourceUrl + createdAt) — dedup key       |

**What's inside `encryptedPayload`** (JSON before encryption):
```json
{
  "sourceUrl": "https://...",
  "videoTitle": "...",
  "thumbnailUrl": "...",
  "formatLabel": "1080p",
  "status": "COMPLETED",
  "createdAt": 1711036800000,
  "completedAt": 1711036900000,
  "fileSizeBytes": 52428800
}
```

**Fields excluded from encrypted payload**:
- `id` — local autoGenerate, not meaningful across devices
- `filePath` — local filesystem path, device-specific
- `mediaStoreUri` — local MediaStore URI, device-specific
- `syncStatus` — local tracking only

### 4. CloudCounters (Firestore document)

**Document**: `users/{uid}/meta/counters`
**Purpose**: Track record count without querying all documents.

| Field       | Type | Notes                                    |
|-------------|------|------------------------------------------|
| recordCount | Int  | Current number of history docs           |
| tierLimit   | Int  | 1000 (free) or 10000 (paid)             |

**Operations**:
- Increment `recordCount` on upload (use `FieldValue.increment(1)`)
- Decrement on delete/eviction (use `FieldValue.increment(-1)`)
- Update `tierLimit` on purchase verification

### 5. CloudTier (Domain model — Kotlin only)

**Purpose**: Represents the user's cloud capacity tier.

```kotlin
enum class CloudTier(val maxRecords: Int) {
    FREE(maxRecords = 1000),
    PAID(maxRecords = 10000),
}
```

**Persistence**: Derived from Google Play Billing purchase state — not stored in Room.
On app launch, `queryPurchasesAsync()` determines the tier.

### 6. CloudBackupPreferences (DataStore)

**Purpose**: User preferences for cloud backup feature.

| Key               | Type    | Default | Notes                        |
|-------------------|---------|---------|------------------------------|
| isBackupEnabled   | Boolean | false   | Master toggle                |
| lastSyncTimestamp  | Long    | 0       | Last successful sync (millis) |
| hasEverEnabled    | Boolean | false   | Gates Firebase initialization |

**Storage**: AndroidX DataStore Preferences (already in project as dependency).

## Validation Rules

- `sourceUrlHash` uniqueness enforced during restore (skip if exists)
- `recordCount` must be reconciled on app launch if `recordCount != actual document count`
- `syncStatus` must never be `SYNCED` if `hasEverEnabled` is false
- `retryCount` capped at 5 — operations with 5+ retries are dropped from queue

## Room Migration

**Migration N → N+1**:
1. Add `syncStatus TEXT NOT NULL DEFAULT 'NOT_SYNCED'` to `downloads` table
2. Create `sync_queue` table

```kotlin
val MIGRATION_N_N1 = object : Migration(N, N + 1) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "ALTER TABLE downloads ADD COLUMN syncStatus TEXT NOT NULL DEFAULT 'NOT_SYNCED'"
        )
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS sync_queue (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                downloadId INTEGER NOT NULL,
                operation TEXT NOT NULL,
                createdAt INTEGER NOT NULL,
                retryCount INTEGER NOT NULL DEFAULT 0,
                lastError TEXT
            )
        """)
        db.execSQL(
            "CREATE UNIQUE INDEX IF NOT EXISTS idx_sync_queue_download_op ON sync_queue (downloadId, operation)"
        )
    }
}
```
