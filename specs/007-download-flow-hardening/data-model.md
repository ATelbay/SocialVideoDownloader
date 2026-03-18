# Data Model: Core Download Flow Hardening

**Feature**: 007-download-flow-hardening | **Date**: 2026-03-18

This feature does not introduce new entities. It modifies existing entity usage and adds event types.

## Modified Entities

### DownloadProgress (existing — `core/domain/model/DownloadProgress.kt`)

No schema change. The `downloadedBytes` field already exists as `Long` but is always set to `0L` in `DownloadService`. After this feature, it will be calculated from `progressPercent * totalBytes`.

| Field | Type | Before | After |
|-------|------|--------|-------|
| downloadedBytes | Long | Always 0L | Calculated: `(progressPercent / 100f * totalBytes).toLong()` when totalBytes > 0 |

### DownloadRecord (existing — `core/domain/model/DownloadRecord.kt`)

No schema change. The `fileSizeBytes` field already exists as `Long?` but is always `null` (default) when constructed in `DownloadService`. After this feature, it will be populated with the actual file size.

| Field | Type | Before | After |
|-------|------|--------|-------|
| fileSizeBytes | Long? | Always null (default, never set) | Actual file size from MediaStore query or File.length() |

### DownloadRequest (existing — `feature/download/service/DownloadRequest.kt`)

New field added to carry total bytes from ViewModel to Service for downloadedBytes calculation.

| Field | Type | Change |
|-------|------|--------|
| totalBytes | Long? | **Added** — nullable, from `selectedFormat.fileSizeBytes` |

## New Types

### DownloadEvent (modified — `feature/download/ui/DownloadEvent.kt`)

New event variants for notification permission and snackbar feedback.

| Variant | Data | Purpose |
|---------|------|---------|
| RequestNotificationPermission | none | Triggers permission launcher in composable |
| ShowSnackbar(message: String) | message text | Transient feedback (queued confirmation, permission rationale) |

**Existing variants** (unchanged): `OpenFile(uri, mimeType)`, `ShareFile(uri, mimeType)`.

## State Transitions

### Notification Permission Flow (new)

```
DownloadClicked intent received
  → [API < 33 OR permission already granted] → proceed to download
  → [API 33+ AND permission not granted]
      → emit RequestNotificationPermission event
      → composable launches system permission dialog
      → [User grants] → proceed to download
      → [User denies] → emit ShowSnackbar(rationale) → proceed to download
```

### Queued State Handling (modified)

```
Before: DownloadServiceState.Queued → ignored (no-op in ViewModel)
After:  DownloadServiceState.Queued → emit ShowSnackbar("Download queued")
```

### Cancel Cleanup (modified)

```
Before: ACTION_CANCEL_DOWNLOAD → destroyProcessById → emit Cancelled → stopIfQueueEmpty
After:  ACTION_CANCEL_DOWNLOAD → destroyProcessById → delete cacheDir/ytdl_downloads/* → emit Cancelled → stopIfQueueEmpty
```

## No Database Migration Required

All changes modify runtime behavior and event flow. The Room schema (`DownloadEntity`) is unchanged — `fileSizeBytes` column already exists in schema version 4. No migration needed.
