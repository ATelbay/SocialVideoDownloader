# Quickstart: Download History Screen

**Branch**: `003-download-history` | **Date**: 2026-03-15

## Prerequisites

- Android Studio latest stable
- Android SDK 36 and a device/emulator running API 26+
- JDK 17+
- Existing history records in Room, or a local way to seed them for manual verification

## Build and Test

```bash
# Work on the feature branch used in this repo
git checkout codex/003-download-history

# Run focused unit tests for the feature and supporting data module
./gradlew :core:data:testDebugUnitTest :feature:history:testDebugUnitTest

# Run the full unit test suite
./gradlew test

# Check formatting/lint gates
./gradlew ktlintCheck

# Build a debug APK
./gradlew assembleDebug
```

## Manual Verification Flow

1. Launch the app and open the History tab from the bottom navigation.
2. Verify that history rows render newest-first with thumbnail, title, format label, status, date, and file size.
3. Type a partial title in the top-bar search field and confirm filtering updates immediately; clear the query and confirm the full list returns.
4. Tap a completed record with a valid MediaStore-backed item and verify the system opens the file.
5. Long-press a completed record and verify Share and Delete are available; long-press a failed or missing-file record and verify Delete remains available while invalid file actions are blocked with feedback.
6. Delete one record with record-only mode, then repeat with record-plus-file mode.
7. Activate a search query, use the overflow `Delete All` action, and confirm the dialog still describes deleting the full history dataset.
8. After deletion, verify the list updates immediately and falls back to the empty state when no items remain.

## Key Files to Inspect

- `feature/history/src/main/kotlin/com/socialvideodownloader/feature/history/ui/HistoryViewModel.kt`
- `feature/history/src/main/kotlin/com/socialvideodownloader/feature/history/ui/HistoryScreen.kt`
- `feature/history/src/main/kotlin/com/socialvideodownloader/feature/history/domain/ObserveHistoryItemsUseCase.kt`
- `feature/history/src/main/kotlin/com/socialvideodownloader/feature/history/di/HistoryModule.kt`
- `feature/history/src/main/kotlin/com/socialvideodownloader/feature/history/file/AndroidHistoryFileManager.kt`
- `core/domain/src/main/kotlin/com/socialvideodownloader/core/domain/model/DownloadRecord.kt`
- `core/domain/src/main/kotlin/com/socialvideodownloader/core/domain/repository/DownloadRepository.kt`
- `core/data/src/main/kotlin/com/socialvideodownloader/core/data/local/DownloadEntity.kt`
- `core/data/src/main/kotlin/com/socialvideodownloader/core/data/local/DownloadDao.kt`
- `core/data/src/main/kotlin/com/socialvideodownloader/core/data/local/AppDatabase.kt`
- `core/data/schemas/com.socialvideodownloader.core.data.local.AppDatabase/2.json`
- `build-logic/convention/src/main/kotlin/AndroidLibraryConventionPlugin.kt`
