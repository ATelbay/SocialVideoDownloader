# Implementation Plan: Core Video Download Flow

**Branch**: `002-core-download-flow` | **Date**: 2026-03-14 | **Spec**: [spec.md](spec.md)
**Input**: Feature specification from `/specs/002-core-download-flow/spec.md`

## Summary

Implement the core video download flow: URL input → yt-dlp metadata extraction → format selection with chips → foreground service download with progress → completion with Open/Share → error handling with Retry. Uses MVI architecture with a single ViewModel managing six sealed UI states. Downloads run in a foreground service with notification progress and cancellation support. Files saved via MediaStore to Downloads/SocialVideoDownloader/. Download history persisted to Room. Queue supports sequential downloads.

## Technical Context

**Language/Version**: Kotlin 2.2.10
**Primary Dependencies**: Jetpack Compose (BOM 2026.03.00), Hilt 2.59.2, Room 2.8.4, Navigation Compose 2.9.7, youtubedl-android 0.18.0 (+ FFmpeg + Aria2c), Coil 2.7.0, kotlinx-serialization 1.7.3
**Storage**: Room (download history), MediaStore (downloaded files)
**Testing**: JUnit5 + MockK + Turbine
**Target Platform**: Android 8.0+ (API 26, target SDK 36)
**Project Type**: Mobile app (Android)
**Performance Goals**: Progress updates ≥1/sec, extraction start <30s from paste
**Constraints**: All processing on-device, no backend, single active download with queue
**Scale/Scope**: Personal utility, 1 user, ~6 screens/states in single feature

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principle | Status | Notes |
|-----------|--------|-------|
| I. Privacy & Zero Bloat | PASS | No analytics, tracking, auth, or third-party SDKs. Only network calls are yt-dlp extraction and download. |
| II. On-Device Architecture | PASS | All extraction via local yt-dlp. No backend. FFmpeg/Aria2c bundled locally. |
| III. Modern Android Stack | PASS | Compose-only UI, KSP (no kapt), MVI with sealed interfaces, Hilt DI, Kotlin only. |
| IV. Modular Separation | PASS | Domain models + use cases in :core:domain, impls in :core:data, UI in :feature:download. Injected dispatchers. String resources (no hardcoded text). |
| V. Minimal Friction UX | PASS | 2 taps: paste URL → Extract → pre-selected best quality → Download. Clipboard auto-detect further reduces friction. |
| VI. Test Discipline | PASS | Unit tests for all use cases and ViewModel state transitions. Error handling for all four required scenarios. ktlint enforced. |
| VII. Simplicity & Focus | PASS | No over-engineering. In-memory queue (not persisted). Single ViewModel. No abstractions without need. |

**Post-Phase 1 Re-check**: All principles still pass. Repository pattern used per constitution (interface in domain, impl in data). Foreground service is the minimum viable approach for background downloads — WorkManager would be over-engineering.

## Project Structure

### Documentation (this feature)

```text
specs/002-core-download-flow/
├── plan.md              # This file
├── spec.md              # Feature specification
├── research.md          # Phase 0: technology research
├── data-model.md        # Phase 1: entity definitions and state machine
├── quickstart.md        # Phase 1: build and test instructions
├── contracts/
│   └── mvi-contract.md  # Phase 1: MVI, service, use case contracts
└── tasks.md             # Phase 2 output (created by /speckit.tasks)
```

### Source Code (repository root)

```text
app/
├── src/main/
│   ├── AndroidManifest.xml          # + foreground service, permissions
│   └── kotlin/com/socialvideodownloader/
│       ├── SocialVideoDownloaderApp.kt  # + notification channel
│       ├── MainActivity.kt
│       ├── di/DispatcherModule.kt
│       └── navigation/AppNavHost.kt

core/domain/
├── src/main/kotlin/com/socialvideodownloader/core/domain/
│   ├── di/Qualifiers.kt
│   ├── model/
│   │   ├── DownloadRecord.kt       # + formatLabel field
│   │   ├── DownloadStatus.kt       # + QUEUED, CANCELLED
│   │   ├── VideoMetadata.kt        # NEW
│   │   ├── VideoFormatOption.kt    # NEW
│   │   ├── DownloadRequest.kt      # NEW
│   │   └── DownloadProgress.kt     # NEW
│   ├── repository/
│   │   ├── DownloadRepository.kt   # existing
│   │   ├── VideoExtractorRepository.kt  # NEW
│   │   ├── MediaStoreRepository.kt      # NEW
│   │   └── ClipboardRepository.kt       # NEW
│   └── usecase/
│       ├── ExtractVideoInfoUseCase.kt       # NEW
│       ├── DownloadVideoUseCase.kt          # NEW
│       ├── CancelDownloadUseCase.kt         # NEW
│       ├── SaveDownloadRecordUseCase.kt     # NEW
│       ├── GetClipboardUrlUseCase.kt        # NEW
│       └── SaveFileToMediaStoreUseCase.kt   # NEW

core/data/
├── src/main/kotlin/com/socialvideodownloader/core/data/
│   ├── di/
│   │   ├── DatabaseModule.kt       # existing
│   │   ├── RepositoryModule.kt     # + new repository bindings
│   │   └── ExtractorModule.kt      # NEW
│   ├── local/
│   │   ├── AppDatabase.kt          # version 1→2 migration
│   │   ├── DownloadDao.kt          # existing
│   │   ├── DownloadEntity.kt       # + formatLabel column
│   │   ├── DownloadMapper.kt       # + formatLabel mapping
│   │   ├── MediaStoreRepositoryImpl.kt  # NEW
│   │   └── ClipboardRepositoryImpl.kt   # NEW
│   └── remote/
│       ├── VideoExtractorRepositoryImpl.kt  # NEW (yt-dlp wrapper)
│       └── VideoInfoMapper.kt              # NEW

feature/download/
├── src/main/
│   ├── AndroidManifest.xml
│   └── kotlin/com/socialvideodownloader/feature/download/
│       ├── navigation/DownloadNavigation.kt  # existing
│       ├── ui/
│       │   ├── DownloadViewModel.kt          # NEW
│       │   ├── DownloadUiState.kt            # NEW
│       │   ├── DownloadIntent.kt             # NEW
│       │   ├── DownloadScreen.kt             # REPLACE placeholder
│       │   └── components/
│       │       ├── UrlInputContent.kt        # NEW
│       │       ├── VideoInfoContent.kt       # NEW
│       │       ├── FormatChipsContent.kt     # NEW
│       │       ├── DownloadProgressContent.kt # NEW
│       │       ├── DownloadCompleteContent.kt # NEW
│       │       └── DownloadErrorContent.kt   # NEW
│       └── service/
│           ├── DownloadService.kt            # NEW
│           └── DownloadNotificationManager.kt # NEW
├── src/test/kotlin/com/socialvideodownloader/feature/download/
│   ├── ui/DownloadViewModelTest.kt           # NEW
│   └── ...

core/domain/
├── src/test/kotlin/com/socialvideodownloader/core/domain/usecase/
│   ├── ExtractVideoInfoUseCaseTest.kt        # NEW
│   ├── DownloadVideoUseCaseTest.kt           # NEW
│   └── ...

core/data/
├── src/test/kotlin/com/socialvideodownloader/core/data/remote/
│   └── VideoInfoMapperTest.kt                # NEW
```

**Structure Decision**: Follows the existing multi-module Android structure established in 001-project-foundation. No new modules needed — all new code fits within `:core:domain`, `:core:data`, and `:feature:download`. The foreground service lives in `:feature:download` since it's specific to the download feature, not shared infrastructure.

## Complexity Tracking

No constitution violations. No complexity justifications needed.
