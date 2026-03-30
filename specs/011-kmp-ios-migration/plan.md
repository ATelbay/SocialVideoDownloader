# Implementation Plan: KMP iOS Migration

**Branch**: `011-kmp-ios-migration` | **Date**: 2026-03-30 | **Spec**: [spec.md](spec.md)
**Input**: Feature specification from `/specs/011-kmp-ios-migration/spec.md`

## Summary

Migrate SocialVideoDownloader from Android-only to Kotlin Multiplatform, sharing domain models, use cases, repository implementations, ViewModel state machines, and the server API client between Android and iOS. Build a native SwiftUI iOS app that uses the existing yt-dlp API server for extraction and download. The migration is incremental — Android must keep working at every step.

## Technical Context

**Language/Version**: Kotlin 2.2.10, Swift 6.x (iOS)
**Primary Dependencies**: Room KMP 2.8.4, Ktor 3.4.1, Koin 4.2.0, SKIE 0.10.10, Multiplatform Settings 1.3.0. Android retains: Jetpack Compose (BOM 2026.03.00), Hilt 2.59.2, Navigation Compose 2.9.7, Coil 2.7.0, Firebase BOM 33.15.0, Play Billing 7.1.1.
**Storage**: Room KMP (shared DB schema in commonMain, platform builders), MediaStore (Android), Documents directory (iOS), Multiplatform Settings (preferences)
**Testing**: kotlin.test + Turbine (shared), JUnit5 + MockK + Turbine (Android), XCTest (iOS)
**Target Platform**: Android 8.0+ (API 26), iOS 16.0+
**Project Type**: Mobile app (Android + iOS via KMP)
**Performance Goals**: Extraction start <30s via server, progress updates ≥1/sec, background downloads complete without app in foreground (iOS)
**Constraints**: iOS cannot run yt-dlp locally (Apple §2.5.2) — server-only extraction. Hilt+Koin coexistence during transition. Android must not regress.
**Scale/Scope**: 3 shared feature modules, 3 shared core modules, 1 new iOS app, 6 platform abstraction interfaces

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principle | Status | Notes |
|-----------|--------|-------|
| I. Privacy & Zero Bloat | PASS | iOS app follows same principles — no analytics, no tracking. Sign in with Apple added alongside Google Sign-In for cloud features only. |
| II. On-Device Architecture | **VIOLATION** | iOS uses server exclusively for extraction/download. Requires amendment: "On Android, extraction happens on-device with server fallback. On iOS, extraction and downloading are performed via the yt-dlp API server." |
| III. Modern Android Stack | **VIOLATION** | iOS uses SwiftUI, not Compose. Shared modules use Koin, not Hilt. Requires broadening to "Modern Stack" allowing KMP + SwiftUI + Koin alongside Compose + Hilt. |
| IV. Modular Separation | **VIOLATION** | New shared modules (:core:domain KMP, :shared:network, :shared:data, :shared:feature-*) expand beyond current module list. Requires acknowledgment of shared module structure. |
| V. Minimal Friction UX | PASS | iOS reproduces same 2-tap flow. Share Sheet integration via iOS share extension. |
| VI. Test Discipline | PASS | Shared code tested with kotlin.test + Turbine on both JVM and Native. Android tests unchanged. |
| VII. Simplicity & Focus | PASS | No over-engineering — KMP is necessary for the stated goal of iOS support. |
| VIII. Optional Cloud Features | **VIOLATION** | Constitution says "Google Sign-In via Credential Manager as the sole permitted mechanism." iOS requires Sign in with Apple per App Store rules. Requires amendment to allow platform-appropriate auth. |

**Constitution amendment required: v4.0.0 MAJOR** — Principles II, III, IV, VIII need changes. See Complexity Tracking below.

**Post-Phase 1 re-check**: All violations are justified and tracked. Design artifacts (data-model, contracts) are consistent with amended principles.

## Project Structure

### Documentation (this feature)

```text
specs/011-kmp-ios-migration/
├── plan.md              # This file
├── research.md          # Phase 0 output — technology decisions
├── data-model.md        # Phase 1 output — shared entities and state
├── quickstart.md        # Phase 1 output — build and run guide
├── contracts/           # Phase 1 output — platform abstraction interfaces
│   ├── platform-download-manager.md
│   ├── platform-file-storage.md
│   ├── platform-clipboard.md
│   ├── platform-string-provider.md
│   ├── server-config.md
│   └── database-factory.md
└── tasks.md             # Phase 2 output (/speckit.tasks)
```

### Source Code (repository root)

```text
# Existing Android modules (modified)
app/src/main/kotlin/com/socialvideodownloader/app/
  ├── SocialVideoDownloaderApp.kt    # Add Koin init alongside Hilt
  ├── di/KoinBridgeModule.kt         # NEW: Hilt @Provides bridging to Koin
  └── di/DispatcherModule.kt         # Existing

core/domain/
  ├── src/commonMain/kotlin/com/socialvideodownloader/core/domain/  # MOVED from src/main
  │   ├── model/          # All domain models (unchanged)
  │   ├── repository/     # All repository interfaces (unchanged)
  │   ├── usecase/        # All use cases (javax.inject removed)
  │   ├── service/        # Service interfaces (unchanged)
  │   └── util/           # UrlNormalizer, PlatformNameResolver
  └── build.gradle.kts    # kotlin.jvm → kotlin-multiplatform

core/data/src/main/kotlin/com/socialvideodownloader/core/data/
  ├── local/              # Room DB stays Android-only (wrapper over shared:data)
  ├── remote/             # VideoExtractorRepositoryImpl (yt-dlp JNI) stays
  ├── remote/FallbackVideoExtractorRepository.kt  # Updated to use shared:network
  └── repository/         # Android-specific repo impls that wrap shared repos

core/ui/                  # Unchanged (Android Compose theme/components)
core/cloud/               # Unchanged (Firebase Android)
core/billing/             # Unchanged (Play Billing)

feature/download/
  ├── src/main/kotlin/.../ui/DownloadViewModel.kt  # Delegates to SharedDownloadViewModel
  ├── src/main/kotlin/.../service/                  # DownloadService stays Android-only
  └── src/main/kotlin/.../ui/screens/               # Compose UI stays

feature/history/          # ViewModel delegates to SharedHistoryViewModel
feature/library/          # ViewModel delegates to SharedLibraryViewModel

# New shared KMP modules
shared/network/
  ├── src/commonMain/kotlin/com/socialvideodownloader/shared/network/
  │   ├── ServerVideoExtractorApi.kt    # Ktor-based (ported from OkHttp)
  │   ├── ServerResponseMapper.kt       # Moved from core/data
  │   ├── dto/ServerExtractResponse.kt  # Moved from core/data
  │   └── ServerConfig.kt               # expect/actual for server URL
  ├── src/androidMain/kotlin/.../
  │   ├── KtorEngineFactory.android.kt  # OkHttp engine
  │   └── ServerConfig.android.kt       # Reads BuildConfig
  ├── src/iosMain/kotlin/.../
  │   ├── KtorEngineFactory.ios.kt      # Darwin engine
  │   └── ServerConfig.ios.kt           # Bundle config or hardcoded
  └── build.gradle.kts                  # svd.kmp.library

shared/data/
  ├── src/commonMain/kotlin/com/socialvideodownloader/shared/data/
  │   ├── local/AppDatabase.kt          # Room KMP database definition
  │   ├── local/DownloadEntity.kt       # Moved from core/data
  │   ├── local/DownloadDao.kt          # Moved from core/data
  │   ├── local/SyncQueueEntity.kt      # Moved from core/data
  │   ├── local/SyncQueueDao.kt         # Moved from core/data
  │   ├── local/DownloadMapper.kt       # Moved from core/data
  │   ├── local/Migrations.kt           # Rewritten for SQLiteConnection API
  │   ├── repository/DownloadRepositoryImpl.kt  # Shared impl
  │   ├── platform/PlatformDownloadManager.kt   # Interface
  │   ├── platform/PlatformFileStorage.kt       # Interface
  │   ├── platform/PlatformClipboard.kt         # Interface
  │   ├── platform/PlatformStringProvider.kt    # Interface
  │   ├── platform/DatabaseFactory.kt           # expect/actual
  │   └── di/SharedDataModule.kt                # Koin module
  ├── src/androidMain/kotlin/.../
  │   ├── platform/AndroidDownloadManager.kt
  │   ├── platform/AndroidFileStorage.kt
  │   ├── platform/AndroidClipboard.kt
  │   ├── platform/AndroidStringProvider.kt
  │   ├── platform/DatabaseFactory.android.kt
  │   └── di/AndroidDataModule.kt              # Koin android module
  ├── src/iosMain/kotlin/.../
  │   ├── platform/IosDownloadManager.kt
  │   ├── platform/IosFileStorage.kt
  │   ├── platform/IosClipboard.kt
  │   ├── platform/IosStringProvider.kt
  │   ├── platform/DatabaseFactory.ios.kt
  │   ├── repository/ServerOnlyVideoExtractorRepository.kt
  │   └── di/IosDataModule.kt                 # Koin ios module
  └── build.gradle.kts                        # svd.kmp.library + room

shared/feature-download/
  ├── src/commonMain/kotlin/.../
  │   ├── SharedDownloadViewModel.kt    # State machine extracted from DownloadViewModel
  │   ├── DownloadUiState.kt           # Moved from feature/download
  │   ├── DownloadIntent.kt            # Moved from feature/download
  │   └── DownloadEvent.kt             # Moved from feature/download (StringRes removed)
  └── build.gradle.kts                 # svd.kmp.feature

shared/feature-history/
  ├── src/commonMain/kotlin/.../
  │   ├── SharedHistoryViewModel.kt
  │   ├── HistoryUiState.kt
  │   └── HistoryEffect.kt            # StringRes replaced with typed message enum
  └── build.gradle.kts

shared/feature-library/
  ├── src/commonMain/kotlin/.../
  │   ├── SharedLibraryViewModel.kt
  │   ├── LibraryUiState.kt
  │   └── LibraryEffect.kt
  └── build.gradle.kts

# New iOS app
iosApp/
  ├── iosApp.xcodeproj/
  ├── iosApp/
  │   ├── App.swift                    # @main, Koin init
  │   ├── ContentView.swift            # TabView with 3 tabs
  │   ├── Theme/
  │   │   ├── Colors.swift             # Warm editorial palette
  │   │   ├── Typography.swift         # SpaceGrotesk + Inter
  │   │   └── Shapes.swift             # Corner radii
  │   ├── Download/
  │   │   ├── DownloadView.swift
  │   │   ├── UrlInputView.swift
  │   │   ├── FormatSelectionView.swift
  │   │   ├── DownloadProgressView.swift
  │   │   └── DownloadCompleteView.swift
  │   ├── Library/
  │   │   ├── LibraryView.swift
  │   │   └── LibraryItemRow.swift
  │   ├── History/
  │   │   ├── HistoryView.swift
  │   │   ├── HistoryItemRow.swift
  │   │   └── CloudBackupView.swift
  │   ├── Services/
  │   │   ├── BackgroundDownloadManager.swift  # URLSession background
  │   │   └── NotificationService.swift
  │   └── ShareExtension/
  │       └── ShareViewController.swift
  └── iosAppTests/

# Build system additions
build-logic/convention/src/main/kotlin/
  ├── KmpLibraryConventionPlugin.kt    # NEW: svd.kmp.library
  └── KmpFeatureConventionPlugin.kt    # NEW: svd.kmp.feature

gradle/libs.versions.toml             # Add: ktor, koin, skie, multiplatform-settings
settings.gradle.kts                   # Add: :shared:network, :shared:data, :shared:feature-*
```

**Structure Decision**: Extend the existing multi-module layout with new `:shared:*` KMP modules alongside existing `:core:*` and `:feature:*` Android modules. Existing Android modules are modified to delegate to shared code but retain their Android-specific responsibilities.

## Complexity Tracking

> Constitution violations justified below. Amendment to v4.0.0 required.

| Violation | Why Needed | Simpler Alternative Rejected Because |
|-----------|------------|-------------------------------------|
| Principle II: iOS uses server for extraction | Apple prohibits embedded interpreters (§2.5.2). yt-dlp cannot run on iOS. Server is a thin wrapper running the same open-source tool. | No alternative exists — iOS physically cannot run yt-dlp locally. |
| Principle III: SwiftUI + Koin alongside Compose + Hilt | iOS requires native UI framework (SwiftUI). KMP shared modules need KMP-compatible DI (Koin). | Building iOS UI in Compose Multiplatform would sacrifice native iOS UX and miss platform conventions. Hilt cannot run in commonMain. |
| Principle IV: New shared module structure | KMP requires separate modules with commonMain/androidMain/iosMain source sets. Cannot add iOS targets to existing Android library modules. | Putting everything in one module would create a monolith. The shared modules follow the same separation pattern as existing modules. |
| Principle VIII: Sign in with Apple alongside Google | App Store requires Sign in with Apple when third-party sign-in is offered. Rejection guaranteed without it. | Dropping Google Sign-In on iOS breaks cross-device identity (Android user can't access cloud backup on iOS). Both are needed. |
