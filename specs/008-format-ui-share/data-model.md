# Data Model: Format Selection UI Overhaul + Share-without-Save

**Feature**: 008-format-ui-share | **Date**: 2026-03-20

## Modified Entities

### DownloadRequest

**Location**: `:core:domain` → `com.socialvideodownloader.core.domain.model.DownloadRequest`

| Field | Type | Default | Change |
|-------|------|---------|--------|
| id | String | — | existing |
| sourceUrl | String | — | existing |
| videoTitle | String | — | existing |
| thumbnailUrl | String? | null | existing |
| formatId | String | — | existing |
| formatLabel | String | — | existing |
| isVideoOnly | Boolean | — | existing |
| totalBytes | Long? | null | existing |
| **shareOnly** | **Boolean** | **false** | **NEW** |

**Validation**: None — `shareOnly` is a simple flag with a safe default.

### DownloadUiState.Downloading

**Location**: `:feature:download` → `com.socialvideodownloader.feature.download.ui.DownloadUiState`

| Field | Type | Default | Change |
|-------|------|---------|--------|
| metadata | VideoMetadata | — | existing |
| progress | DownloadProgress | — | existing |
| selectedFormatId | String | — | existing |
| **isShareMode** | **Boolean** | **false** | **NEW** |

### DownloadIntent (sealed interface)

**Location**: `:feature:download` → `com.socialvideodownloader.feature.download.ui.DownloadIntent`

| Variant | Change |
|---------|--------|
| **ShareFormatClicked** | **NEW** — data object, triggers share-mode download |
| All existing variants | unchanged |

## State Transitions (Share Flow)

```
FormatSelection
  ├── DownloadClicked → Downloading(isShareMode=false) → Done
  └── ShareFormatClicked → Downloading(isShareMode=true) → [emit ShareFile event] → FormatSelection
```

**Error path**: `Downloading(isShareMode=true)` → `Error` → `RetryClicked` → re-enters extraction (same as existing).

**Cancel path**: `Downloading(isShareMode=true)` → `CancelDownloadClicked` → `FormatSelection` (restored from metadata in Downloading state).

## New One-Shot Events

### DownloadEvent.ShareFile

Already exists in the sealed interface. Currently only used from the `Done` state (sharing a saved file). Will be reused for share-mode flow with a `content://` URI from FileProvider instead of a MediaStore URI.

No new event type needed — the existing `ShareFile(filePath: String)` works for both cases since the `filePath` field carries any URI string.
