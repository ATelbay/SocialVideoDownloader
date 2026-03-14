# Quickstart: Core Video Download Flow

**Branch**: `002-core-download-flow` | **Date**: 2026-03-14

## Prerequisites

- Android Studio (latest stable)
- JDK 17
- Android device or emulator (API 26+)
- Internet connection (for yt-dlp extraction and downloads)

## Build & Run

```bash
# Debug build
./gradlew assembleDebug

# Run unit tests
./gradlew test

# Lint check
./gradlew ktlintCheck
```

## Module Dependency Graph (for this feature)

```
:app
├── :feature:download   (ViewModel, UI, navigation)
├── :feature:history    (unchanged — reads from same Room DB)
├── :core:domain        (use cases, domain models, repository interfaces)
├── :core:data          (repository impls, Room DB, yt-dlp wrapper, MediaStore)
└── :core:ui            (shared composables, theme)
```

## Key Files to Create (by module)

### :core:domain
- `model/VideoMetadata.kt` — extracted video info domain model
- `model/VideoFormatOption.kt` — format option domain model
- `model/DownloadRequest.kt` — download request domain model
- `model/DownloadProgress.kt` — real-time progress domain model
- `repository/VideoExtractorRepository.kt` — interface for yt-dlp operations
- `repository/MediaStoreRepository.kt` — interface for saving to MediaStore
- `repository/ClipboardRepository.kt` — interface for clipboard access
- `usecase/ExtractVideoInfoUseCase.kt`
- `usecase/DownloadVideoUseCase.kt`
- `usecase/CancelDownloadUseCase.kt`
- `usecase/SaveDownloadRecordUseCase.kt`
- `usecase/GetClipboardUrlUseCase.kt`
- `usecase/SaveFileToMediaStoreUseCase.kt`

### :core:data
- `remote/VideoExtractorRepositoryImpl.kt` — wraps youtubedl-android API
- `remote/VideoInfoMapper.kt` — maps yt-dlp VideoInfo/VideoFormat to domain models
- `local/MediaStoreRepositoryImpl.kt` — MediaStore write operations
- `local/ClipboardRepositoryImpl.kt` — Android ClipboardManager wrapper
- `di/ExtractorModule.kt` — Hilt bindings for new repositories

### :feature:download
- `ui/DownloadViewModel.kt` — MVI ViewModel
- `ui/DownloadUiState.kt` — sealed interface for all UI states
- `ui/DownloadIntent.kt` — sealed interface for all user intents
- `ui/DownloadScreen.kt` — **replace existing placeholder** with full Compose UI
- `ui/components/UrlInputContent.kt` — URL text field + Extract button
- `ui/components/VideoInfoContent.kt` — thumbnail, title, duration, author card
- `ui/components/FormatChipsContent.kt` — format selection chips
- `ui/components/DownloadProgressContent.kt` — progress bar, speed, ETA, cancel
- `ui/components/DownloadCompleteContent.kt` — success with Open/Share
- `ui/components/DownloadErrorContent.kt` — error message with Retry
- `service/DownloadService.kt` — foreground service
- `service/DownloadNotificationManager.kt` — notification creation and updates

### :app
- Update `AndroidManifest.xml` — add foreground service declaration, permissions
- Update `SocialVideoDownloaderApp.kt` — create notification channel on startup

### Key Files to Modify
- `DownloadRecord.kt` — add `formatLabel` field
- `DownloadEntity.kt` — add `formatLabel` column
- `DownloadStatus.kt` — add `QUEUED`, `CANCELLED`
- `DownloadMapper.kt` — update mapping for new fields
- `AppDatabase.kt` — bump version to 2, add migration

## Testing Strategy

### Unit Tests (JUnit5 + MockK + Turbine)
- `ExtractVideoInfoUseCaseTest` — success, network error, invalid URL
- `DownloadVideoUseCaseTest` — progress flow, completion, cancellation
- `DownloadViewModelTest` — all state transitions (Idle→Extracting→FormatSelection→Downloading→Done, error paths, cancel, retry)
- `VideoInfoMapperTest` — format filtering, sorting, video-only detection

### Manual Testing Checklist
- [ ] Paste YouTube URL → extract → select format → download → Open/Share
- [ ] Paste TikTok URL → same flow
- [ ] Invalid URL → error → retry
- [ ] Cancel download mid-progress
- [ ] Background app during download → check notification
- [ ] Start second download while first is running → verify queue
- [ ] Clipboard auto-detection on app resume
