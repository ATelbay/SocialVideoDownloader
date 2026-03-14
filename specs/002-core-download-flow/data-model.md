# Data Model: Core Video Download Flow

**Branch**: `002-core-download-flow` | **Date**: 2026-03-14

## Entities

### VideoMetadata (domain model, in-memory only)

Represents extracted information about a video. Not persisted — lives only during the active session.

| Field | Type | Notes |
|-------|------|-------|
| sourceUrl | String | The original URL entered by the user |
| title | String | Video title |
| thumbnailUrl | String? | URL to the video thumbnail image |
| durationSeconds | Int | Video duration in seconds |
| author | String? | Channel/uploader name |
| formats | List\<VideoFormatOption\> | Available download formats |

### VideoFormatOption (domain model, in-memory only)

A single downloadable format variant.

| Field | Type | Notes |
|-------|------|-------|
| formatId | String | yt-dlp format identifier (e.g., "248", "251") |
| label | String | Human-readable label (e.g., "1080p", "720p", "mp3") |
| resolution | Int? | Height in pixels (null for audio-only) |
| ext | String | File extension (mp4, webm, m4a, mp3) |
| fileSizeBytes | Long? | Estimated file size; null if unknown |
| isAudioOnly | Boolean | True if this is an audio-only format |
| isVideoOnly | Boolean | True if video has no audio track (needs merge) |

**Derived behavior**:
- Formats are grouped into two sections: Video and Audio-Only
- Video formats sorted by resolution descending (best first)
- Audio formats sorted by bitrate descending
- Best video format = first in sorted video list = pre-selected default

### DownloadRequest (domain model, in-memory only)

Represents a pending or active download request. Used by the service queue.

| Field | Type | Notes |
|-------|------|-------|
| id | String | Unique request ID (UUID), also used as yt-dlp processId |
| sourceUrl | String | Video URL |
| videoTitle | String | For display and file naming |
| thumbnailUrl | String? | For notification and completion screen |
| formatId | String | Selected yt-dlp format identifier |
| formatLabel | String | Human-readable format label for display |
| isVideoOnly | Boolean | If true, merge with bestaudio |

### DownloadProgress (domain model, in-memory only)

Real-time progress state emitted by the download service.

| Field | Type | Notes |
|-------|------|-------|
| requestId | String | Matches DownloadRequest.id |
| progressPercent | Float | 0.0 to 100.0 |
| downloadedBytes | Long | Bytes downloaded so far |
| totalBytes | Long? | Total size if known |
| speedBytesPerSec | Long | Current download speed |
| etaSeconds | Long | Estimated time remaining |

### DownloadRecord (Room entity — ALREADY EXISTS)

Persisted record of a completed download. **Already defined** in `:core:domain` and `:core:data`.

| Field | Type | Notes |
|-------|------|-------|
| id | Long | Auto-generated primary key |
| sourceUrl | String | Original video URL |
| videoTitle | String | Video title |
| thumbnailUrl | String? | Thumbnail URL |
| filePath | String? | Local file path or content URI |
| status | DownloadStatus | PENDING, DOWNLOADING, COMPLETED, FAILED |
| createdAt | Long | Epoch millis when download was initiated |
| completedAt | Long? | Epoch millis when download finished |
| fileSizeBytes | Long? | Final file size |

**Changes needed**: Add `formatLabel` (String) field to store what format was selected (e.g., "1080p", "mp3"). Requires Room migration v1→v2.

### DownloadStatus (enum — ALREADY EXISTS)

`PENDING`, `DOWNLOADING`, `COMPLETED`, `FAILED`

**Changes needed**: Add `QUEUED` and `CANCELLED` values.

## State Machine: Download Flow

```
                        ┌─────────────────────────────┐
                        ▼                             │
Idle ──[enter URL]──▶ Extracting ──[success]──▶ FormatSelection
  ▲                     │                         │        │
  │                     │[error]                  │        │
  │                     ▼                         │        │
  │                   Error ◀─────────────────────┘        │
  │                     │                    [error]        │
  │                   [retry]                              │
  │                     │              [select + download]  │
  │                     ▼                                  ▼
  │                 Extracting                        Downloading
  │                                                    │    │
  │                                          [complete] │    │ [cancel]
  │                                                    ▼    ▼
  │                                                  Done  FormatSelection
  │                                                    │
  │                                          [new URL]  │
  └────────────────────────────────────────────────────┘
```

### MVI State Types

**UiState** (`sealed interface DownloadUiState`):
- `Idle` — empty URL field, waiting for input
- `Extracting(url: String)` — loading indicator while yt-dlp runs getInfo
- `FormatSelection(metadata: VideoMetadata, selectedFormatId: String)` — shows video info + format chips
- `Downloading(metadata: VideoMetadata, progress: DownloadProgress)` — shows progress bar, speed, ETA, cancel button
- `Done(metadata: VideoMetadata, filePath: String)` — success screen with Open/Share
- `Error(message: String, retryAction: RetryAction)` — error message + retry button

**RetryAction** (`sealed interface`):
- `RetryExtraction(url: String)`
- `RetryDownload(request: DownloadRequest)`

**Intent** (`sealed interface DownloadIntent`):
- `UrlChanged(url: String)`
- `ExtractClicked`
- `FormatSelected(formatId: String)`
- `DownloadClicked`
- `CancelDownloadClicked`
- `RetryClicked`
- `OpenFileClicked`
- `ShareFileClicked`
- `NewDownloadClicked`
- `ClipboardUrlDetected(url: String)`

## Relationships

```
VideoMetadata 1──*  VideoFormatOption
DownloadRequest  ──▶ VideoFormatOption (by formatId)
DownloadProgress ──▶ DownloadRequest (by requestId)
DownloadRecord   ──  persisted after download completes
```
