# Implementation Plan: Core Download Flow Hardening

**Branch**: `007-download-flow-hardening` | **Date**: 2026-03-18 | **Spec**: [spec.md](spec.md)
**Input**: Feature specification from `/specs/007-download-flow-hardening/spec.md`

## Summary

Close 8 gaps found during the spec 002 audit: POST_NOTIFICATIONS runtime permission, partial-file cleanup on cancel, queued download UI feedback, notification tap actions, fileSizeBytes population, downloadedBytes calculation, URL persistence via SavedStateHandle, and safe retry handling. All changes are correctness/polish — no new screens or modules.

## Technical Context

**Language/Version**: Kotlin 2.2.10
**Primary Dependencies**: Jetpack Compose (BOM 2026.03.00), Material 3, Hilt (KSP), Navigation Compose 2.9.7, Coil
**Storage**: Room (download history), MediaStore (saved files), cacheDir (yt-dlp temp files)
**Testing**: JUnit5 + MockK + Turbine for Flow
**Target Platform**: Android 8.0+ (API 26), Target SDK 36
**Project Type**: Mobile app (Android)
**Performance Goals**: Progress updates ≥1/sec, cache cleanup <1 sec after cancel
**Constraints**: On-device only, no backend, MVI architecture, all strings in resources
**Scale/Scope**: Personal utility, 2 feature modules, 6 existing UI states (no new states added)

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principle | Status | Notes |
|-----------|--------|-------|
| I. Privacy & Zero Bloat | PASS | No analytics, tracking, or off-device data. Permission request is OS-required. |
| II. On-Device Architecture | PASS | All changes are on-device. No backend or cloud dependency. |
| III. Modern Android Stack | PASS | Compose-only UI changes, KSP, MVI pattern, Hilt, Kotlin. |
| IV. Modular Separation | PASS | Changes in :feature:download, :core:data, :app only. Repository interfaces in :core:domain, impls in :core:data. Injected dispatchers. Strings in resources. |
| V. Minimal Friction UX | PASS | Permission prompt at first download (not cold launch) preserves 2-tap flow. Queued feedback adds clarity without extra taps. |
| VI. Test Discipline | PASS | New unit tests for permission logic, cancel cleanup, queued state, fileSizeBytes. Existing tests must stay green. |
| VII. Simplicity & Focus | PASS | No new abstractions, no new modules, no over-engineering. 8 targeted fixes. |

**Gate result: PASS** — no violations.

## Project Structure

### Documentation (this feature)

```text
specs/007-download-flow-hardening/
├── plan.md              # This file
├── research.md          # Phase 0 output
├── data-model.md        # Phase 1 output
├── quickstart.md        # Phase 1 output
└── tasks.md             # Phase 2 output (/speckit.tasks)
```

### Source Code Changes

```text
app/src/main/kotlin/com/socialvideodownloader/
└── MainActivity.kt                              # (no changes needed — permission handled in Compose)

feature/download/src/main/kotlin/com/socialvideodownloader/feature/download/
├── service/
│   ├── DownloadService.kt                       # Cancel cleanup, fileSizeBytes query, downloadedBytes calc
│   └── DownloadNotificationManager.kt           # contentIntent for completion/error/progress notifications
├── ui/
│   ├── DownloadViewModel.kt                     # Queued state, SavedStateHandle write, safe retry, permission flow
│   ├── DownloadUiState.kt                       # (no new states — queued is a side-effect event)
│   ├── DownloadIntent.kt                        # (no changes needed)
│   ├── (DownloadEvent nested in DownloadViewModel.kt) # Add NotificationPermissionRequired, DownloadQueued events
│   └── DownloadScreen.kt                        # SnackbarHost, permission launcher, queued snackbar
└── src/main/res/values/strings.xml              # New string resources

feature/download/src/test/kotlin/com/socialvideodownloader/feature/download/
├── ui/DownloadViewModelTest.kt                  # New tests for queued, SavedStateHandle, safe retry
└── service/
    ├── DownloadNotificationManagerTest.kt       # New tests for notification contentIntent
    └── DownloadServiceTest.kt                   # New test for cancel cleanup
```

## Design Decisions

### D1: Permission request location — Compose `rememberLauncherForActivityResult` in DownloadScreen

The POST_NOTIFICATIONS permission request will be handled in the DownloadScreen composable using `rememberLauncherForActivityResult(RequestPermission)`, not in MainActivity. This keeps the permission logic co-located with the download trigger and avoids polluting the Activity. The ViewModel will check permission status before starting a download and emit a `DownloadEvent` to trigger the launcher if needed.

**Flow**: DownloadClicked → ViewModel checks `ContextCompat.checkSelfPermission` → if not granted on API 33+, emit `DownloadEvent.RequestNotificationPermission` → Composable launches permission request → on result (grant or deny), ViewModel proceeds with download. If denied, emit a `DownloadEvent.ShowSnackbar` with rationale text.

### D2: Queued feedback — Snackbar event, not a new UiState

Queued downloads will be surfaced via a `DownloadEvent.ShowSnackbar("Download queued")` side-effect, not a new `DownloadUiState.Queued` variant. The current download stays in `Downloading` state. Rationale: the queued state is transient and the primary screen shows the active download's progress. A snackbar is the standard Android pattern for transient confirmations.

The ViewModel's `collectServiceState()` Queued branch will emit the snackbar event. The DownloadScreenContent composable needs a `SnackbarHostState` wired to the Scaffold.

### D3: Notification contentIntent — PendingIntent to MainActivity with FLAG_ACTIVITY_SINGLE_TOP

- **Completion notification**: `PendingIntent.getActivity` with `ACTION_VIEW` + MediaStore URI + MIME type. Falls back to app launch if no handler.
- **Error notification**: `PendingIntent.getActivity` targeting `MainActivity` with `FLAG_ACTIVITY_SINGLE_TOP`.
- **Progress notification**: Same as error — opens app to current state.

All PendingIntents use `FLAG_IMMUTABLE | FLAG_UPDATE_CURRENT`.

### D4: Partial-file cleanup — Delete cache dir contents inline in cancel handler

In `DownloadService.cancelDownload()`, after `YoutubeDL.getInstance().destroyProcessById(requestId)`, delete all files in `cacheDir/ytdl_downloads/` using `File.listFiles()?.forEach { it.deleteRecursively() }`. This is synchronous and fast (yt-dlp writes to a single temp directory). The directory itself is preserved.

### D5: fileSizeBytes — Query ContentResolver after MediaStore save

After `saveFileToMediaStore()` returns the URI string, query `ContentResolver.query(uri, [SIZE], null, null, null)` to get the actual byte count. On pre-API 29, use `File(filePath).length()`. Pass this value to the `DownloadRecord` constructor. If the query fails or returns 0, store `null`.

This logic lives in `DownloadService` at the call site, not in a new use case — it's a single ContentResolver query, not worth a new abstraction.

### D6: downloadedBytes — Calculate from percent × total in progress callback

In the `DownloadService` progress callback, calculate `downloadedBytes = ((progressPercent / 100f) * (totalBytes ?: 0L)).toLong()`. The `totalBytes` must be passed into the service via `DownloadRequest` (it's already available from `selectedFormat.fileSizeBytes` in the ViewModel). If `totalBytes` is null/0, `downloadedBytes` stays 0.

### D7: SavedStateHandle persistence — Write on every UrlChanged

In `handleUrlChanged(url)`, add `savedStateHandle["currentUrl"] = url`. In `init`, read `savedStateHandle["currentUrl"]` as fallback after `savedStateHandle["initialUrl"]`. This ensures both share-intent URLs and manually typed URLs survive process death.

### D8: Safe retry — Exhaustive `when` over RetryAction sealed interface

Replace `state.retryAction as RetryAction.RetryExtraction` with:
```kotlin
when (val action = state.retryAction) {
    is RetryAction.RetryExtraction -> {
        currentUrl = action.url
        handleExtract()
    }
}
```
The `when` is exhaustive over the sealed interface, so adding a new variant forces a compile error.

## Complexity Tracking

> No violations — table not needed.
