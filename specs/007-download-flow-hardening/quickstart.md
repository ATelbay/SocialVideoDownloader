# Quickstart: Core Download Flow Hardening

**Feature**: 007-download-flow-hardening | **Date**: 2026-03-18

## Prerequisites

- Android Studio with Kotlin 2.2.10+
- Android device/emulator running API 33+ (for notification permission testing)
- Second device/emulator running API 26-32 (for pre-33 path verification)

## Build & Run

```bash
git checkout 007-download-flow-hardening
./gradlew assembleDebug
# Install on device and test
```

## Files to Modify (in implementation order)

### 1. DownloadRequest — add totalBytes field
`feature/download/src/main/kotlin/com/socialvideodownloader/feature/download/service/DownloadRequest.kt`

### 2. DownloadEvent — add new event variants
`feature/download/src/main/kotlin/com/socialvideodownloader/feature/download/ui/DownloadEvent.kt`

### 3. DownloadService — cancel cleanup, fileSizeBytes, downloadedBytes
`feature/download/src/main/kotlin/com/socialvideodownloader/feature/download/service/DownloadService.kt`

### 4. DownloadNotificationManager — add contentIntent to all notifications
`feature/download/src/main/kotlin/com/socialvideodownloader/feature/download/service/DownloadNotificationManager.kt`

### 5. DownloadViewModel — queued state, SavedStateHandle, safe retry, permission check
`feature/download/src/main/kotlin/com/socialvideodownloader/feature/download/ui/DownloadViewModel.kt`

### 6. DownloadScreen — SnackbarHost, permission launcher, event handling
`feature/download/src/main/kotlin/com/socialvideodownloader/feature/download/ui/DownloadScreen.kt`

### 7. strings.xml — new string resources
`feature/download/src/main/res/values/strings.xml`

### 8. Tests
`feature/download/src/test/kotlin/com/socialvideodownloader/feature/download/ui/DownloadViewModelTest.kt`

## Testing Checklist

1. **Notification permission (API 33+)**: First download → dialog appears → grant → notification shows
2. **Permission denial**: Deny → snackbar rationale → download proceeds → no notification
3. **Cancel cleanup**: Start download → cancel → check `cacheDir/ytdl_downloads/` is empty
4. **Queued feedback**: Start download A → start download B → "Download queued" snackbar appears
5. **Completion notification tap**: Complete download → tap notification → file opens in viewer
6. **Error notification tap**: Trigger failure → tap notification → app opens to download screen
7. **File size in history**: Complete download → check history → correct size shown
8. **Downloaded bytes**: During download → bytes counter increases (not stuck at 0)
9. **URL process death**: Type URL → background → kill process → reopen → URL restored
10. **Retry safety**: Trigger extraction error → tap Retry → works correctly
11. **Existing tests**: `./gradlew :feature:download:test` — all pass
