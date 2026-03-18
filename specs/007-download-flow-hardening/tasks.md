# Tasks: Core Download Flow Hardening

**Input**: Design documents from `/specs/007-download-flow-hardening/`
**Prerequisites**: plan.md (required), spec.md (required), research.md, data-model.md

**Tests**: Included — spec requires new tests for notification permission logic, cancel cleanup, queued state handling, fileSizeBytes population, and notification tap actions.

**Organization**: Tasks are grouped by user story to enable independent implementation and testing of each story.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3)
- Include exact file paths in descriptions

## Path Conventions

- `app/src/main/...` for app entry points, navigation, and manifest changes
- `feature/download/src/main/...` and `feature/download/src/test/...` for feature UI, ViewModels, and feature-specific services
- `core/domain/src/main/...` and `core/domain/src/test/...` for domain models, repository interfaces, and use cases
- `core/data/src/main/...` and `core/data/src/test/...` for Room, MediaStore, repository implementations, and mappers

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Shared types and UI wiring that multiple user stories depend on

- [ ] T001 Add `ShowSnackbar(message: String)` and `RequestNotificationPermission` variants to `DownloadEvent` sealed interface (currently nested inside `DownloadViewModel.kt`) in `feature/download/src/main/kotlin/com/socialvideodownloader/feature/download/ui/DownloadViewModel.kt`
- [ ] T002 Add `SnackbarHostState` to `DownloadScreenContent` and wire `SnackbarHost` to the `Scaffold` in `feature/download/src/main/kotlin/com/socialvideodownloader/feature/download/ui/DownloadScreen.kt` — add event collection for `ShowSnackbar` in the existing `LaunchedEffect` that collects `viewModel.events`
- [ ] T003 [P] Add new string resources to `feature/download/src/main/res/values/strings.xml`: `download_queued` ("Download queued"), `notification_permission_rationale` ("Notifications are disabled — you won't see download progress in the notification shade"), `notification_permission_denied` ("Notification permission denied")

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Data model changes that MUST be complete before story implementation

**⚠️ CRITICAL**: No user story work can begin until this phase is complete

- [ ] T004 Add `totalBytes: Long? = null` field to `DownloadRequest` data class in `core/domain/src/main/kotlin/com/socialvideodownloader/core/domain/model/DownloadRequest.kt`

**Checkpoint**: Foundation ready — user story implementation can now begin

---

## Phase 3: User Story 1 — Notification Permission Prompt (Priority: P1) 🎯 MVP

**Goal**: On API 33+, request POST_NOTIFICATIONS before the first download. If denied, show rationale and proceed.

**Independent Test**: On API 33+ device, first download triggers permission dialog. Deny → snackbar rationale → download proceeds. Grant → notification appears.

### Tests for User Story 1 ⚠️

> **NOTE: Write these tests FIRST, ensure they FAIL before implementation**

- [ ] T005 [P] [US1] Add ViewModel test: `when DownloadClicked on API 33+ without permission, emits RequestNotificationPermission event` in `feature/download/src/test/kotlin/com/socialvideodownloader/feature/download/ui/DownloadViewModelTest.kt`
- [ ] T006 [P] [US1] Add ViewModel test: `when notification permission denied, emits ShowSnackbar with rationale and proceeds with download` in `feature/download/src/test/kotlin/com/socialvideodownloader/feature/download/ui/DownloadViewModelTest.kt`

### Implementation for User Story 1

- [ ] T007 [US1] Add notification permission check to `handleDownload()` in `DownloadViewModel` — on API 33+ if permission not granted, emit `RequestNotificationPermission` event and return early; add `fun onNotificationPermissionResult(granted: Boolean)` method that emits rationale snackbar if denied then calls the actual download start logic in `feature/download/src/main/kotlin/com/socialvideodownloader/feature/download/ui/DownloadViewModel.kt`
- [ ] T008 [US1] Add `rememberLauncherForActivityResult(RequestPermission)` in `DownloadScreen` composable that calls `viewModel.onNotificationPermissionResult(granted)` on result; collect `RequestNotificationPermission` event to trigger the launcher in `feature/download/src/main/kotlin/com/socialvideodownloader/feature/download/ui/DownloadScreen.kt`

**Checkpoint**: US1 is fully functional — permission prompt works on API 33+, denied path shows snackbar, download proceeds either way

---

## Phase 4: User Story 2 — Queued Download Feedback (Priority: P1)

**Goal**: When a second download is queued, show a "Download queued" snackbar.

**Independent Test**: Start download A, start download B → snackbar "Download queued" appears.

### Tests for User Story 2 ⚠️

- [ ] T009 [P] [US2] Add ViewModel test: `when service emits Queued state, emits ShowSnackbar with download_queued message` in `feature/download/src/test/kotlin/com/socialvideodownloader/feature/download/ui/DownloadViewModelTest.kt`

### Implementation for User Story 2

- [ ] T010 [US2] Update `collectServiceState()` in `DownloadViewModel` to handle `DownloadServiceState.Queued` by emitting `DownloadEvent.ShowSnackbar` with the queued message (resolve string via `ErrorMessageMapper` pattern or pass `@StringRes` resource ID) instead of the current no-op in `feature/download/src/main/kotlin/com/socialvideodownloader/feature/download/ui/DownloadViewModel.kt`

**Checkpoint**: US2 is fully functional — queued downloads show snackbar confirmation

---

## Phase 5: User Story 3 — Notification Tap Actions (Priority: P2)

**Goal**: Completion notification opens the file, error notification opens the app, progress notification opens the app.

**Independent Test**: Complete download → tap notification → file opens. Fail download → tap notification → app opens to download screen.

### Tests for User Story 3 ⚠️

> **NOTE: Write these tests FIRST, ensure they FAIL before implementation**

- [ ] T011 [P] [US3] Add unit tests: verify completion notification includes `ACTION_VIEW` contentIntent with correct URI and MIME type, error notification contentIntent targets `MainActivity`, progress notification contentIntent targets `MainActivity` in `feature/download/src/test/kotlin/com/socialvideodownloader/feature/download/service/DownloadNotificationManagerTest.kt`

### Implementation for User Story 3

- [ ] T012 [US3] Add `contentIntent` to `showCompletionNotification()` — build `PendingIntent.getActivity` with `ACTION_VIEW`, MediaStore URI, and MIME type; update method signature to accept `mediaStoreUri: String?` and `mimeType: String` in `feature/download/src/main/kotlin/com/socialvideodownloader/feature/download/service/DownloadNotificationManager.kt`
- [ ] T013 [P] [US3] Add `contentIntent` to `showErrorNotification()` — build `PendingIntent.getActivity` targeting `MainActivity` with `FLAG_ACTIVITY_SINGLE_TOP | FLAG_ACTIVITY_CLEAR_TOP` in `feature/download/src/main/kotlin/com/socialvideodownloader/feature/download/service/DownloadNotificationManager.kt`
- [ ] T014 [P] [US3] Add `contentIntent` to `buildProgressNotification()` — same as error notification (open app to current state) in `feature/download/src/main/kotlin/com/socialvideodownloader/feature/download/service/DownloadNotificationManager.kt`
- [ ] T015 [US3] Update `DownloadService` call sites to pass `mediaStoreUri` and `mimeType` to `showCompletionNotification()` in `feature/download/src/main/kotlin/com/socialvideodownloader/feature/download/service/DownloadService.kt`

**Checkpoint**: US3 is fully functional — all three notification types have working tap actions

---

## Phase 6: User Story 4 — Partial File Cleanup on Cancel (Priority: P2)

**Goal**: When a download is cancelled, immediately delete all partial files from the yt-dlp cache directory.

**Independent Test**: Start download → cancel → verify `cacheDir/ytdl_downloads/` is empty.

### Tests for User Story 4 ⚠️

> **NOTE: Write these tests FIRST, ensure they FAIL before implementation**

- [ ] T016 [P] [US4] Add unit test: verify cache directory contents are deleted after cancel — mock `File.listFiles()` and assert `deleteRecursively()` is called for each file in `feature/download/src/test/kotlin/com/socialvideodownloader/feature/download/service/DownloadServiceTest.kt`

### Implementation for User Story 4

- [ ] T017 [US4] Add cache directory cleanup after `destroyProcessById()` in the `ACTION_CANCEL_DOWNLOAD` handler — `File(cacheDir, "ytdl_downloads").listFiles()?.forEach { it.deleteRecursively() }` in `feature/download/src/main/kotlin/com/socialvideodownloader/feature/download/service/DownloadService.kt`

**Checkpoint**: US4 is fully functional — cancel cleans up partial files immediately

---

## Phase 7: User Story 5 — Accurate File Size in History (Priority: P3)

**Goal**: Populate `fileSizeBytes` on `DownloadRecord` with the actual file size after saving to MediaStore.

**Independent Test**: Complete download → check history → file size matches actual.

### Tests for User Story 5 ⚠️

- [ ] T018 [P] [US5] Add ViewModel/service-level test: verify `DownloadRecord` is saved with non-null `fileSizeBytes` after successful download completion in `feature/download/src/test/kotlin/com/socialvideodownloader/feature/download/ui/DownloadViewModelTest.kt`

### Implementation for User Story 5

- [ ] T019 [US5] After `saveFileToMediaStore()` returns in `DownloadService`, query actual file size via `contentResolver.query(uri, arrayOf(OpenableColumns.SIZE), ...)` on API 29+ or `File(filePath).length()` on pre-29; pass result as `fileSizeBytes` to the `DownloadRecord` constructor in `feature/download/src/main/kotlin/com/socialvideodownloader/feature/download/service/DownloadService.kt`

**Checkpoint**: US5 is fully functional — history entries show correct file sizes

---

## Phase 8: User Story 6 — Accurate Downloaded Bytes During Progress (Priority: P3)

**Goal**: Calculate `downloadedBytes` from `progressPercent * totalBytes` in the progress callback instead of hardcoding 0.

**Independent Test**: Start download with known size → downloaded bytes counter increases during progress.

### Implementation for User Story 6

- [ ] T020 [US6] Pass `totalBytes = selectedFormat.fileSizeBytes` when constructing `DownloadRequest` in `handleDownload()` in `feature/download/src/main/kotlin/com/socialvideodownloader/feature/download/ui/DownloadViewModel.kt`
- [ ] T021 [US6] Update the progress callback in `DownloadService` to calculate `downloadedBytes = if (request.totalBytes != null && request.totalBytes > 0) ((progressPercent / 100f) * request.totalBytes).toLong() else 0L` instead of hardcoded `0L` in `feature/download/src/main/kotlin/com/socialvideodownloader/feature/download/service/DownloadService.kt`

**Checkpoint**: US6 is fully functional — progress shows increasing downloaded bytes when total is known

---

## Phase 9: User Story 7 — URL Survives Process Death (Priority: P3)

**Goal**: Persist `currentUrl` to `SavedStateHandle` on every `UrlChanged` intent.

**Independent Test**: Type URL → kill process → reopen → URL is restored.

### Tests for User Story 7 ⚠️

- [ ] T022 [P] [US7] Add ViewModel test: `when UrlChanged intent is handled, currentUrl is written to SavedStateHandle` in `feature/download/src/test/kotlin/com/socialvideodownloader/feature/download/ui/DownloadViewModelTest.kt`

### Implementation for User Story 7

- [ ] T023 [US7] In `handleUrlChanged(url)`, add `savedStateHandle["currentUrl"] = url`; in `init`, update URL fallback to `savedStateHandle["initialUrl"] ?: savedStateHandle["currentUrl"]` in `feature/download/src/main/kotlin/com/socialvideodownloader/feature/download/ui/DownloadViewModel.kt`

**Checkpoint**: US7 is fully functional — typed URLs survive process death

---

## Phase 10: User Story 8 — Safe Retry Handling (Priority: P3)

**Goal**: Replace force-cast `retryAction as RetryAction.RetryExtraction` with exhaustive `when` expression.

**Independent Test**: Unit test verifies retry works without ClassCastException for all RetryAction subtypes.

### Tests for User Story 8 ⚠️

- [ ] T024 [P] [US8] Add ViewModel test: `handleRetry uses exhaustive when and does not throw ClassCastException` in `feature/download/src/test/kotlin/com/socialvideodownloader/feature/download/ui/DownloadViewModelTest.kt`

### Implementation for User Story 8

- [ ] T025 [US8] Replace `state.retryAction as RetryAction.RetryExtraction` with `when (val action = state.retryAction) { is RetryAction.RetryExtraction -> { currentUrl = action.url; handleExtract() } }` in `handleRetry()` in `feature/download/src/main/kotlin/com/socialvideodownloader/feature/download/ui/DownloadViewModel.kt`

**Checkpoint**: US8 is fully functional — retry is compile-time safe for future RetryAction variants

---

## Phase 11: Polish & Cross-Cutting Concerns

**Purpose**: Validation and cleanup across all stories

- [ ] T026 Run all existing unit tests via `./gradlew :feature:download:test` and verify zero regressions
- [ ] T027 [P] Run `./gradlew ktlintCheck` and fix any violations
- [ ] T028 [P] Manual validation against quickstart.md testing checklist (11 items) — include verification that no hardcoded user-facing strings were introduced (FR-012)

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies — can start immediately
- **Foundational (Phase 2)**: Depends on Setup completion — BLOCKS US6 (downloadedBytes needs totalBytes on DownloadRequest)
- **User Stories (Phase 3–10)**: All depend on Phase 1 completion (DownloadEvent variants, SnackbarHost)
  - US1 and US2 depend on Phase 1 (SnackbarHost, ShowSnackbar event)
  - US6 depends on Phase 2 (totalBytes field on DownloadRequest)
  - US3, US4, US5, US7, US8 can start after Phase 1
- **Polish (Phase 11)**: Depends on all stories being complete

### User Story Dependencies

- **US1 (P1)**: Depends on Phase 1 only — no cross-story dependencies
- **US2 (P1)**: Depends on Phase 1 only — no cross-story dependencies
- **US3 (P2)**: Depends on Phase 1 only — no cross-story dependencies
- **US4 (P2)**: No dependencies beyond Phase 1 — standalone service change
- **US5 (P3)**: No dependencies beyond Phase 1 — standalone service change
- **US6 (P3)**: Depends on Phase 2 (T004 adds totalBytes to DownloadRequest)
- **US7 (P3)**: No dependencies beyond Phase 1 — standalone ViewModel change
- **US8 (P3)**: No dependencies beyond Phase 1 — standalone ViewModel change

### Within Each User Story

- Tests MUST be written and FAIL before implementation
- Implementation tasks within a story execute sequentially unless marked [P]
- Story complete before moving to next priority

### Parallel Opportunities

- T001, T003 can run in parallel (different files)
- T005, T006 can run in parallel (same file, different test methods)
- After Phase 1 completes: US1, US2, US3, US4, US5, US7, US8 can all start in parallel
- After Phase 2 completes: US6 can start
- T012, T013, T014 within US3 — T013 and T014 are parallel (same file, different methods)
- US4 (T017), US5 (T019), US7 (T023), US8 (T025) are single-task implementation stories that can all run in parallel

---

## Parallel Example: After Phase 1

```
# These can all execute simultaneously:
Agent A: US1 (T005 → T006 → T007 → T008) — Permission flow
Agent B: US2 (T009 → T010) — Queued feedback
Agent C: US3 (T011 → T012, T013, T014 → T015) — Notification taps
Agent D: US4 (T016 → T017) + US5 (T018 → T019) + US7 (T022 → T023) + US8 (T024 → T025) — Small fixes
```

---

## Implementation Strategy

### MVP First (User Story 1 + 2)

1. Complete Phase 1: Setup (T001–T003)
2. Complete Phase 2: Foundational (T004)
3. Complete Phase 3: US1 — Notification Permission (T005–T008)
4. Complete Phase 4: US2 — Queued Feedback (T009–T010)
5. **STOP and VALIDATE**: Test both P1 stories independently
6. Deploy/demo if ready

### Incremental Delivery

1. Complete Setup + Foundational → Foundation ready
2. Add US1 + US2 → Test independently → Demo (MVP!)
3. Add US3 + US4 → Test independently → Demo (P2 complete)
4. Add US5–US8 → Test independently → Demo (all P3 complete)
5. Polish phase → Final validation

---

## Notes

- [P] tasks = different files, no dependencies
- [Story] label maps task to specific user story for traceability
- Each user story is independently completable and testable
- US4, US5, US7, US8 are single-task implementation stories (very small scope)
- Commit after each task or logical group
- Stop at any checkpoint to validate story independently
