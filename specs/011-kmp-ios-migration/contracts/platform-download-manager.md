# Contract: PlatformDownloadManager

**Location**: `shared/data/src/commonMain/kotlin/.../platform/PlatformDownloadManager.kt`

## Interface

```
PlatformDownloadManager
  ├── startDownload(request: DownloadRequest): Unit
  │   Start a download for the given request. On Android, starts the foreground service.
  │   On iOS, creates a URLSessionDownloadTask with background configuration.
  │
  ├── cancelDownload(requestId: String): Unit
  │   Cancel an in-progress download by request ID.
  │
  ├── downloadState: StateFlow<DownloadServiceState>
  │   Observable stream of download state changes (progress, completion, error).
  │
  └── activeRequestId: String?
      The currently active download request ID, or null if idle.
```

## State Machine

`DownloadServiceState` (sealed interface, already exists in feature/download — moves to commonMain):
- `Idle` — no active download
- `Queued(requestId, videoTitle)` — waiting to start
- `Downloading(requestId, progress: DownloadProgress)` — in progress
- `Completed(requestId, filePath, fileUri)` — download finished
- `Failed(requestId, error: DownloadErrorType)` — download failed
- `Cancelled(requestId)` — user cancelled

## Platform Implementations

### Android: `AndroidDownloadManager`
- Wraps existing `DownloadServiceStateHolder` (already a StateFlow bridge)
- `startDownload()` builds an `Intent` for `DownloadService` and calls `startForegroundService()`
- `cancelDownload()` sends cancel `Intent` to `DownloadService`
- Requires `Context` injection

### iOS: `IosDownloadManager`
- Creates `URLSessionDownloadTask` with `URLSessionConfiguration.background(withIdentifier:)`
- Progress tracked via `URLSessionDownloadDelegate` callbacks (foreground only)
- Completed file moved from temp location to Documents directory
- State persisted to allow reconciliation on app relaunch
- Written in Kotlin/Native with platform.Foundation APIs

## Error Handling
- Network errors → `DownloadErrorType.NETWORK_ERROR`
- Server 4xx/5xx → `DownloadErrorType.DOWNLOAD_FAILED`
- Storage full → `DownloadErrorType.STORAGE_FULL`
- Timeout → `DownloadErrorType.NETWORK_ERROR`
