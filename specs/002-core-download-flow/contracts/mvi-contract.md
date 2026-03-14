# MVI Contract: Download Feature

**Module**: `:feature:download`

## ViewModel Contract

```
DownloadViewModel
├── Input:  DownloadIntent (sealed interface)
├── Output: StateFlow<DownloadUiState> (sealed interface)
└── Side effects: None (all actions expressed as state transitions)
```

### UiState

| State | Data | UI Rendering |
|-------|------|-------------|
| Idle | clipboardUrl: String? | URL text field (empty or pre-filled), Extract button (disabled until URL non-empty) |
| Extracting | url: String | URL field (read-only), loading spinner, "Extracting video info..." text |
| FormatSelection | metadata: VideoMetadata, selectedFormatId: String | Video card (thumbnail, title, duration, author), format chips, Download button |
| Downloading | metadata: VideoMetadata, progress: DownloadProgress | Video card, progress bar, speed/ETA text, Cancel button |
| Done | metadata: VideoMetadata, filePath: String | Video card, checkmark, "Download complete", Open button, Share button, New Download button |
| Error | message: String, retryAction: RetryAction | Error icon, message text, Retry button, New Download button |

### Intents

| Intent | From State(s) | Triggers |
|--------|--------------|----------|
| UrlChanged(url) | Idle | User types or pastes in URL field |
| ExtractClicked | Idle | User taps Extract button |
| FormatSelected(formatId) | FormatSelection | User taps a format chip |
| DownloadClicked | FormatSelection | User taps Download button |
| CancelDownloadClicked | Downloading | User taps Cancel (in-app or notification) |
| RetryClicked | Error | User taps Retry button |
| OpenFileClicked | Done | User taps Open button |
| ShareFileClicked | Done | User taps Share button |
| NewDownloadClicked | Done, Error | User taps New Download / starts fresh |
| ClipboardUrlDetected(url) | Idle | App resumed with video URL in clipboard |

## Service Contract

```
DownloadService (Foreground Service)
├── Input:  start/cancel download commands via Intent extras
├── Output: StateFlow<DownloadServiceState> (shared via Hilt singleton)
└── Notification: progress, speed, ETA, cancel action
```

### DownloadServiceState

| State | Data |
|-------|------|
| Idle | — |
| Downloading | requestId, progress, speed, eta |
| Queued | list of pending requestIds |
| Completed | requestId, filePath |
| Failed | requestId, errorMessage |
| Cancelled | requestId |

## Use Case Contracts

| Use Case | Module | Input | Output |
|----------|--------|-------|--------|
| ExtractVideoInfoUseCase | :core:domain | url: String | Result\<VideoMetadata\> |
| DownloadVideoUseCase | :core:domain | DownloadRequest | Flow\<DownloadProgress\> |
| CancelDownloadUseCase | :core:domain | requestId: String | Unit |
| SaveDownloadRecordUseCase | :core:domain | DownloadRecord | Long (inserted ID) |
| GetClipboardUrlUseCase | :core:domain | — | String? |
| SaveFileToMediaStoreUseCase | :core:domain | tempFilePath, title, mimeType | Uri |

## Repository Contracts

| Repository | Module (interface) | Methods |
|------------|-------------------|---------|
| DownloadRepository | :core:domain | getAll(), getById(), insert(), update(), delete() — **already exists** |
| VideoExtractorRepository | :core:domain | extractInfo(url): VideoMetadata, download(request, callback): Unit, cancelDownload(processId): Unit |
| MediaStoreRepository | :core:domain | saveToDownloads(tempFile, title, mimeType): Uri |
| ClipboardRepository | :core:domain | getVideoUrl(): String? |
