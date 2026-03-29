# Data Model: KMP iOS Migration

**Feature**: `011-kmp-ios-migration` | **Date**: 2026-03-30

## Shared Domain Models (commonMain — already exist, move as-is)

### VideoMetadata
| Field | Type | Notes |
|-------|------|-------|
| sourceUrl | String | Original video URL |
| title | String | Video title from extraction |
| thumbnailUrl | String? | Thumbnail image URL |
| durationSeconds | Long? | Video duration |
| author | String? | Video author/channel |
| formats | List\<VideoFormatOption\> | Available download formats |

### VideoFormatOption
| Field | Type | Notes |
|-------|------|-------|
| formatId | String | yt-dlp format identifier |
| label | String | Human-readable label (e.g., "1080p MP4") |
| extension | String | File extension (mp4, webm, etc.) |
| fileSizeBytes | Long? | Estimated file size |
| resolution | String? | e.g., "1920x1080" |
| isVideoOnly | Boolean | Requires muxing with audio |
| codec | String? | Video codec info |

### DownloadRequest
| Field | Type | Notes |
|-------|------|-------|
| id | String | Unique request ID (UUID) |
| sourceUrl | String | Original video URL |
| videoTitle | String | Title for saved file |
| thumbnailUrl | String? | For UI display |
| formatId | String | Selected format |
| formatLabel | String | Human-readable format label |
| isVideoOnly | Boolean | Whether muxing is needed |
| totalBytes | Long? | Expected file size |
| shareOnly | Boolean | Temp download for sharing |
| existingRecordId | Long? | Re-download of existing record |
| directDownloadUrl | String? | Server-provided direct URL (always present on iOS) |

### DownloadRecord
| Field | Type | Notes |
|-------|------|-------|
| id | Long | Auto-generated primary key |
| sourceUrl | String | Original video URL |
| videoTitle | String | Video title |
| thumbnailUrl | String? | Thumbnail URL |
| formatLabel | String | Format description |
| filePath | String | Platform-specific file path |
| mediaStoreUri | String? | Android MediaStore URI (null on iOS) |
| status | DownloadStatus | Current status enum |
| createdAt | Long | Epoch millis |
| completedAt | Long? | Epoch millis |
| fileSizeBytes | Long? | Final file size |
| syncStatus | SyncStatus | Cloud sync state |

### DownloadProgress
| Field | Type | Notes |
|-------|------|-------|
| requestId | String | Matches DownloadRequest.id |
| progressPercent | Float | 0.0 to 1.0 |
| downloadedBytes | Long | Bytes downloaded so far |
| totalBytes | Long? | Total expected bytes |
| speedBytesPerSec | Long? | Current download speed |
| etaSeconds | Long? | Estimated time remaining |
| isMuxing | Boolean | Post-download muxing phase |

### DownloadStatus (enum)
`PENDING` → `QUEUED` → `DOWNLOADING` → `COMPLETED` | `FAILED` | `CANCELLED`

### SyncStatus (enum)
`NOT_SYNCED` | `PENDING_UPLOAD` | `SYNCED` | `PENDING_DELETE`

## Shared Database Entities (commonMain — moved from core/data, adapted for Room KMP)

### DownloadEntity (table: `downloads`)
| Column | Type | Constraints |
|--------|------|-------------|
| id | Long | PK, autoGenerate |
| sourceUrl | String | NOT NULL |
| videoTitle | String | NOT NULL |
| thumbnailUrl | String? | nullable |
| formatLabel | String | NOT NULL |
| filePath | String | NOT NULL |
| mediaStoreUri | String? | nullable (null on iOS) |
| status | String | NOT NULL |
| createdAt | Long | NOT NULL |
| completedAt | Long? | nullable |
| fileSizeBytes | Long? | nullable |
| syncStatus | String | NOT NULL, default "NOT_SYNCED" |

### SyncQueueEntity (table: `sync_queue`)
| Column | Type | Constraints |
|--------|------|-------------|
| id | Long | PK, autoGenerate |
| downloadId | Long | NOT NULL |
| operation | String | NOT NULL ("UPLOAD" or "DELETE") |
| createdAt | Long | NOT NULL |
| retryCount | Int | NOT NULL, default 0 |
| lastError | String? | nullable |

Unique index on (downloadId, operation).

### Migrations (v1→v5)
All existing migrations are raw SQL statements. They move to commonMain with signature change:
- **Before**: `migrate(db: SupportSQLiteDatabase)` → `db.execSQL(...)`
- **After**: `migrate(connection: SQLiteConnection)` → `connection.execSQL(...)`

SQL statements remain identical. No data loss.

## Shared ViewModel State (commonMain — extracted from feature ViewModels)

### DownloadUiState (sealed interface)
```
Idle
  └── initialUrl: String?
Extracting
  └── url: String
FormatSelection
  ├── metadata: VideoMetadata
  ├── selectedFormat: VideoFormatOption?
  └── existingDownload: ExistingDownload?
Downloading
  ├── videoTitle: String
  ├── thumbnailUrl: String?
  └── progress: DownloadProgress?
Done
  ├── videoTitle: String
  ├── thumbnailUrl: String?
  ├── filePath: String
  └── fileUri: String?
Error
  ├── errorType: DownloadErrorType  (enum, replaces @StringRes)
  ├── message: String?
  └── retryAction: RetryAction?
```

### DownloadIntent (sealed interface)
```
PasteUrl(url: String)
Extract
SelectFormat(format: VideoFormatOption)
StartDownload
CancelDownload
Retry
Reset
```

### DownloadEvent (sealed interface — one-shot side effects)
```
ShowError(errorType: DownloadErrorType, message: String?)
NavigateToFile(filePath: String)
RequestNotificationPermission
```

### DownloadErrorType (enum — replaces @StringRes Int)
`NETWORK_ERROR` | `SERVER_UNAVAILABLE` | `EXTRACTION_FAILED` | `UNSUPPORTED_URL` | `STORAGE_FULL` | `DOWNLOAD_FAILED` | `UNKNOWN`

### RetryAction (enum — what to retry after an error)
`RETRY_EXTRACTION` | `RETRY_DOWNLOAD`

### ExistingDownload (data class — for re-download detection)
| Field | Type | Notes |
|-------|------|-------|
| recordId | Long | Existing DownloadRecord.id |
| filePath | String | Previous file path |
| isFileAccessible | Boolean | Whether the old file still exists |

### HistoryItem (data class — UI model for history list)
| Field | Type | Notes |
|-------|------|-------|
| id | Long | DownloadRecord.id |
| title | String | Video title |
| thumbnailUrl | String? | Thumbnail URL |
| platformName | String | Source platform (YouTube, Instagram, etc.) |
| date | String | Formatted date string |
| fileSize | String? | Formatted file size |
| status | DownloadStatus | Current status |
| syncStatus | SyncStatus | Cloud sync state |

### HistoryUiState (sealed interface)
```
Loading
Empty
Content
  ├── items: List<HistoryItem>
  ├── searchQuery: String
  └── selectedItem: HistoryItem?
```

### HistoryEffect (sealed interface — @StringRes removed)
```
ShowMessage(messageType: HistoryMessageType)  // enum instead of @StringRes
NavigateToUpgrade
CopySuccess
```

### HistoryMessageType (enum — replaces @StringRes Int)
`DELETE_SUCCESS` | `DELETE_ALL_SUCCESS` | `COPY_URL_SUCCESS` | `CLOUD_SYNC_ERROR`

### CloudBackupState (data class — embedded in History screen)
| Field | Type | Notes |
|-------|------|-------|
| isSignedIn | Boolean | Whether user is authenticated |
| isEnabled | Boolean | Whether backup is toggled on |
| lastSyncTime | Long? | Epoch millis of last sync |
| recordCount | Int | Number of synced records |
| capacityUsed | Long | Bytes used in cloud storage |
| capacityTotal | Long | Total bytes available |
| currentTier | String | Free / Premium tier name |

### LibraryItem (data class — UI model for library grid)
| Field | Type | Notes |
|-------|------|-------|
| id | Long | DownloadRecord.id |
| title | String | Video title |
| thumbnailUrl | String? | Thumbnail URL |
| filePath | String | Platform-specific file path |
| fileSize | String? | Formatted file size |
| platformName | String | Source platform |
| date | String | Formatted date string |

### LibraryIntent (sealed interface)
```
OpenFile(item: LibraryItem)
ShareFile(item: LibraryItem)
DeleteFile(item: LibraryItem)
Refresh
```

### LibraryEffect (sealed interface — one-shot side effects)
```
OpenPlayer(filePath: String)
ShowShareSheet(filePath: String)
ShowMessage(messageType: LibraryMessageType)
```

### LibraryMessageType (enum)
`DELETE_SUCCESS` | `FILE_NOT_FOUND` | `SHARE_ERROR`

### LibraryUiState (sealed interface)
```
Loading
Empty
Content
  └── items: List<LibraryItem>
```

## Platform-Specific Data

### iOS File Storage
- Download path: `Documents/SocialVideoDownloader/` (visible in Files app)
- Temp download path: System-managed URLSession temp location
- `filePath` in DownloadRecord stores relative path from Documents root
- `mediaStoreUri` is always null on iOS

### Android File Storage (unchanged)
- Download path: `MediaStore.Downloads` collection
- `filePath` stores absolute path
- `mediaStoreUri` stores content:// URI

### Server API DTOs (commonMain — moved from core/data)

### ServerExtractRequest
| Field | Type |
|-------|------|
| url | String |
| api_key | String? |

### ServerExtractResponse
| Field | Type |
|-------|------|
| title | String |
| thumbnail | String? |
| duration | Long? |
| uploader | String? |
| formats | List\<ServerFormatDto\> |
| direct_download_url | String? |

### ServerFormatDto
| Field | Type |
|-------|------|
| format_id | String |
| ext | String |
| resolution | String? |
| filesize | Long? |
| vcodec | String? |
| acodec | String? |
| format_note | String? |
