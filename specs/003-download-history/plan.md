# Implementation Plan: Download History Screen

**Branch**: `003-download-history` | **Date**: 2026-03-15 | **Spec**: [spec.md](./spec.md)
**Input**: Feature specification from `/specs/003-download-history/spec.md`

## Summary

Implement the `:feature:history` screen as a Compose-first MVI flow that observes Room-backed download history, enriches records with actionable local content references, filters them locally by title, and renders a searchable lazy list with thumbnail, status, and action affordances. The work also includes focused `:core:domain` and `:core:data` updates because the checked-in history model does not yet expose the spec-required `formatLabel`, a persisted MediaStore URI, or a bulk-delete repository operation, plus a shared build-logic update so the planned JUnit5 tests actually run.

## Technical Context

**Language/Version**: Kotlin 2.2.10
**Primary Dependencies**: Jetpack Compose Material 3, Navigation Compose 2.9.7, Hilt 2.56, Room 2.8.4, Coil 2.7.0, Kotlin Coroutines/Flow
**Storage**: Room for download history metadata; MediaStore-backed content URIs for file actions, with legacy `filePath` fallback where needed
**Testing**: JUnit5, MockK, Turbine, kotlinx-coroutines-test
**Target Platform**: Android 8.0+ (minSdk 26), targetSdk 36
**Project Type**: Android multi-module mobile application
**Performance Goals**: Newest-first local history should appear immediately from Room; title filtering across 100+ records should respond within 2 seconds; list scrolling should stay smooth while thumbnails load
**Constraints**: Compose-only UI, sealed `HistoryUiState` and `HistoryIntent`, injected dispatchers for file I/O, no hardcoded user-facing strings, no backend/network dependency, graceful handling for missing local files, shared JUnit Platform enablement in build logic
**Scale/Scope**: One feature module (`:feature:history`) plus targeted contract/schema changes in `:core:domain`, `:core:data`, a small build-logic update for Android unit test execution, and minimal app-level fallback sharing configuration for legacy path-based rows

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

- **I. Privacy & Zero Bloat**: PASS. The feature reads local Room data and launches system file intents only; it adds no analytics, backend calls, or third-party tracking.
- **II. On-Device Architecture**: PASS. All browsing, filtering, and file actions remain on-device.
- **III. Modern Android Stack (Compose + KSP + MVI)**: PASS. UI remains Compose-only, DI stays on Hilt/KSP, and the feature design uses sealed `HistoryUiState` and `HistoryIntent`.
- **IV. Modular Separation**: PASS with targeted core updates. Repository interfaces stay in `:core:domain`, Room implementation stays in `:core:data`, Android file operations are kept in an injectable feature-local helper, and Hilt binding for that helper stays within `:feature:history`.
- **V. Minimal Friction UX**: PASS. Search, open/share, and cleanup all happen from a single screen with human-readable failure feedback.
- **VI. Test Discipline**: PASS if the plan's use case and ViewModel unit tests are implemented.
- **VII. Simplicity & Focus**: PASS. The design adds only history browsing and cleanup behavior; no player, sync, or settings creep.

**Phase 1 Re-check**: PASS. The design artifacts introduce only minimal schema changes (`formatLabel`, `mediaStoreUri`, `deleteAll`), feature-local file handling, and shared JUnit5 enablement needed to satisfy the approved spec.

## Project Structure

### Documentation (this feature)

```text
specs/003-download-history/
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
└── tasks.md
```

### Source Code (repository root)

```text
build-logic/convention/src/main/kotlin/
├── AndroidApplicationConventionPlugin.kt
└── AndroidLibraryConventionPlugin.kt

app/
└── src/main/
    ├── AndroidManifest.xml
    ├── kotlin/com/socialvideodownloader/navigation/AppNavHost.kt
    └── res/xml/history_file_paths.xml

core/domain/src/main/kotlin/com/socialvideodownloader/core/domain/
├── model/DownloadRecord.kt
└── repository/DownloadRepository.kt

core/data/src/main/kotlin/com/socialvideodownloader/core/data/
├── local/AppDatabase.kt
├── local/DownloadDao.kt
├── local/DownloadEntity.kt
├── local/DownloadMapper.kt
└── repository/DownloadRepositoryImpl.kt

core/data/schemas/com.socialvideodownloader.core.data.local.AppDatabase/
└── 2.json

feature/history/src/main/kotlin/com/socialvideodownloader/feature/history/
├── navigation/HistoryNavigation.kt
├── di/HistoryModule.kt
├── domain/ObserveHistoryItemsUseCase.kt
├── domain/DeleteHistoryItemUseCase.kt
├── domain/DeleteAllHistoryUseCase.kt
├── file/HistoryFileManager.kt
├── file/AndroidHistoryFileManager.kt
└── ui/
    ├── HistoryScreen.kt
    ├── HistoryViewModel.kt
    ├── HistoryUiState.kt
    ├── HistoryIntent.kt
    ├── HistoryEffect.kt
    ├── HistoryContent.kt
    ├── HistoryListItem.kt
    ├── HistoryDeleteDialog.kt
    └── HistoryMenus.kt

feature/history/src/main/res/values/strings.xml

feature/history/src/test/kotlin/com/socialvideodownloader/feature/history/
├── domain/ObserveHistoryItemsUseCaseTest.kt
├── domain/DeleteHistoryItemUseCaseTest.kt
├── domain/DeleteAllHistoryUseCaseTest.kt
└── ui/HistoryViewModelTest.kt
```

**Structure Decision**: Keep persistence contract and schema changes in `:core:domain` and `:core:data`, keep screen-specific MVI types, Hilt wiring, file helpers, and tests in `:feature:history`, and make the JUnit Platform change once in build logic so every Android module inherits the same unit-test behavior.

## Complexity Tracking

No constitution violations or extra complexity exemptions are required for this feature.
