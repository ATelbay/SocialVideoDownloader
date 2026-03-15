# Tasks: Download History Screen

**Input**: Design documents from `/specs/003-download-history/`
**Prerequisites**: plan.md (required), spec.md (required), research.md, data-model.md, quickstart.md

**Tests**: Included — spec R6 and constitution require use case and ViewModel unit test coverage.

**Organization**: Tasks grouped by user story for independent implementation and testing.

**Codebase State**: `DownloadRecord` already has `formatLabel`; Room DB is at version 2 (MIGRATION_1_2 added `formatLabel`); `feature/history/` exists as a navigation-wired stub with no ViewModel or real UI; `deleteAll()` and `mediaStoreUri` do not exist yet.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3)
- Include exact file paths in descriptions

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Build logic and test scaffolding needed before feature work

- [ ] T001 Enable JUnit Platform in `build-logic/convention/src/main/kotlin/AndroidLibraryConventionPlugin.kt` and `build-logic/convention/src/main/kotlin/AndroidApplicationConventionPlugin.kt` by adding `useJUnitPlatform()` to the `tasks.withType<Test>` block
- [ ] T002 [P] Update `feature/history/build.gradle.kts` to add `libs.coil.compose`, `libs.androidx.lifecycle.runtime.compose`, and test dependencies (JUnit5, MockK, Turbine, kotlinx-coroutines-test) if not already inherited from convention plugins
- [ ] T003 [P] Create `feature/history/src/test/kotlin/com/socialvideodownloader/feature/history/testutil/MainDispatcherRule.kt` — JUnit5 extension to set `Dispatchers.Main` to `UnconfinedTestDispatcher`
- [ ] T004 [P] Create `feature/history/src/test/kotlin/com/socialvideodownloader/feature/history/testdouble/FakeDownloadRepository.kt` with controllable `Flow<List<DownloadRecord>>` emissions and delete/deleteAll tracking
- [ ] T005 [P] Create `feature/history/src/test/kotlin/com/socialvideodownloader/feature/history/testdouble/FakeHistoryFileManager.kt` with configurable `isFileAccessible`, `resolveContentUri`, and `deleteFile` responses

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Schema migration, repository contract extension, MVI types, and file-management boundary that ALL user stories depend on

**⚠️ CRITICAL**: No user story work can begin until this phase is complete

- [ ] T006 Add `mediaStoreUri: String?` field to domain model in `core/domain/src/main/kotlin/com/socialvideodownloader/core/domain/model/DownloadRecord.kt`
- [ ] T007 Add `mediaStoreUri` column to Room entity in `core/data/src/main/kotlin/com/socialvideodownloader/core/data/local/DownloadEntity.kt` and update mapper in `core/data/src/main/kotlin/com/socialvideodownloader/core/data/local/DownloadMapper.kt` to map `mediaStoreUri`
- [ ] T008 Add `MIGRATION_2_3` for `mediaStoreUri` column and bump database version to 3 in `core/data/src/main/kotlin/com/socialvideodownloader/core/data/local/AppDatabase.kt`; export updated Room schema to `core/data/schemas/com.socialvideodownloader.core.data.local.AppDatabase/3.json`
- [ ] T009 Add `@Query("DELETE FROM downloads") suspend fun deleteAll()` to `core/data/src/main/kotlin/com/socialvideodownloader/core/data/local/DownloadDao.kt`
- [ ] T010 Add `suspend fun deleteAll()` to repository interface in `core/domain/src/main/kotlin/com/socialvideodownloader/core/domain/repository/DownloadRepository.kt` and implement in `core/data/src/main/kotlin/com/socialvideodownloader/core/data/repository/DownloadRepositoryImpl.kt`
- [ ] T011 [P] Create MVI state types: `HistoryUiState` sealed interface in `feature/history/src/main/kotlin/com/socialvideodownloader/feature/history/ui/HistoryUiState.kt`, `HistoryIntent` sealed interface in `feature/history/src/main/kotlin/com/socialvideodownloader/feature/history/ui/HistoryIntent.kt`, and `HistoryEffect` sealed interface in `feature/history/src/main/kotlin/com/socialvideodownloader/feature/history/ui/HistoryEffect.kt` per data-model.md definitions (Loading, Empty, Content variants; all intent types; OpenContent, ShareContent, ShowMessage effects)
- [ ] T012 [P] Create `HistoryListItem` presentation model in `feature/history/src/main/kotlin/com/socialvideodownloader/feature/history/ui/HistoryUiState.kt` with fields: id, title, formatLabel, thumbnailUrl, status, createdAt, fileSizeBytes, contentUri, isFileAccessible
- [ ] T013 [P] Create `HistoryFileManager` interface in `feature/history/src/main/kotlin/com/socialvideodownloader/feature/history/file/HistoryFileManager.kt` with methods: `resolveContentUri(record: DownloadRecord): String?`, `isFileAccessible(contentUri: String): Boolean`, `deleteFile(contentUri: String): Boolean`
- [ ] T014 Implement `AndroidHistoryFileManager` in `feature/history/src/main/kotlin/com/socialvideodownloader/feature/history/file/AndroidHistoryFileManager.kt` — resolve actionable content URI from `mediaStoreUri` (preferred) or legacy `filePath` via FileProvider, check file availability via ContentResolver, delete via MediaStore, all on injected IO dispatcher
- [ ] T015 Create `HistoryModule` Hilt module in `feature/history/src/main/kotlin/com/socialvideodownloader/feature/history/di/HistoryModule.kt` binding `AndroidHistoryFileManager` to `HistoryFileManager`

**Checkpoint**: Foundation ready — schema updated to v3, repository extended with `deleteAll()`, MVI types defined, file manager injectable

---

## Phase 3: User Story 1 — Browse and Search Past Downloads (Priority: P1) 🎯 MVP

**Goal**: User opens History tab, sees a searchable newest-first list with thumbnail, title, format label, status, date, and file size

**Independent Test**: Populate repository with completed and failed records, open History, confirm items render newest-first and filter correctly by title

### Tests for User Story 1

> **NOTE: Write these tests FIRST, ensure they FAIL before implementation**

- [ ] T016 [P] [US1] Create `feature/history/src/test/kotlin/com/socialvideodownloader/feature/history/domain/ObserveHistoryItemsUseCaseTest.kt` — verify DownloadRecord-to-HistoryListItem mapping with correct `isFileAccessible` and `contentUri` via FakeHistoryFileManager
- [ ] T017 [P] [US1] Create `feature/history/src/test/kotlin/com/socialvideodownloader/feature/history/ui/HistoryViewModelTest.kt` — verify Loading → Content transition, empty state when no records, search filtering (case-insensitive substring), and empty-results state when query matches nothing

### Implementation for User Story 1

- [ ] T018 [US1] Create `ObserveHistoryItemsUseCase` in `feature/history/src/main/kotlin/com/socialvideodownloader/feature/history/domain/ObserveHistoryItemsUseCase.kt` — combine `DownloadRepository.getAll()` with `HistoryFileManager` to emit `List<HistoryListItem>` with resolved contentUri and isFileAccessible
- [ ] T019 [US1] Implement `HistoryViewModel` in `feature/history/src/main/kotlin/com/socialvideodownloader/feature/history/ui/HistoryViewModel.kt` — observe `ObserveHistoryItemsUseCase`, combine with `MutableStateFlow<String>` search query, emit `HistoryUiState` (Loading/Empty/Content), handle `SearchQueryChanged` intent with case-insensitive substring filtering
- [ ] T020 [US1] Add history string resources to `feature/history/src/main/res/values/strings.xml` — search hint, empty state messages (no history / no results), status labels, format fallback text
- [ ] T021 [US1] Create `HistoryListItem` composable in `feature/history/src/main/kotlin/com/socialvideodownloader/feature/history/ui/HistoryListItem.kt` — thumbnail via Coil, title, format label, status chip/label with visual distinction for COMPLETED/FAILED, formatted date (device-local), formatted file size
- [ ] T022 [US1] Create `HistoryContent` composable in `feature/history/src/main/kotlin/com/socialvideodownloader/feature/history/ui/HistoryContent.kt` — LazyColumn rendering HistoryListItem rows keyed by id, empty-state placeholder reuse when no records, empty-results state when filtering produces no matches
- [ ] T023 [US1] Replace stub `HistoryScreen` in `feature/history/src/main/kotlin/com/socialvideodownloader/feature/history/ui/HistoryScreen.kt` — top app bar with search field, collect ViewModel state, render HistoryContent, collect effects for snackbar/feedback

**Checkpoint**: History tab shows searchable, newest-first list — MVP functional

---

## Phase 4: User Story 2 — Reopen or Share a Completed Download (Priority: P1)

**Goal**: User taps a completed item to open it, or long-presses to share — with clear feedback when files are missing

**Independent Test**: Seed one completed record with accessible file and one failed record, verify open/share behavior from History screen

### Tests for User Story 2

- [ ] T024 [US2] Extend `feature/history/src/test/kotlin/com/socialvideodownloader/feature/history/ui/HistoryViewModelTest.kt` — verify `HistoryItemClicked` emits `OpenContent` effect for accessible completed items, `ShowMessage` for inaccessible/failed; verify `ShareClicked` emits `ShareContent` effect; verify `HistoryItemLongPressed`/`DismissItemMenu` toggles `openMenuItemId`

### Implementation for User Story 2

- [ ] T025 [US2] Extend `HistoryViewModel` in `feature/history/src/main/kotlin/com/socialvideodownloader/feature/history/ui/HistoryViewModel.kt` to handle `HistoryItemClicked` (check status + isFileAccessible → emit `OpenContent` or `ShowMessage`), `ShareClicked` (emit `ShareContent` or `ShowMessage`), `HistoryItemLongPressed`, and `DismissItemMenu` intents
- [ ] T026 [US2] Create long-press context menu composable in `feature/history/src/main/kotlin/com/socialvideodownloader/feature/history/ui/HistoryMenus.kt` — DropdownMenu showing Share (when file accessible) and Delete options
- [ ] T027 [US2] Wire open/share effect handlers in `feature/history/src/main/kotlin/com/socialvideodownloader/feature/history/ui/HistoryScreen.kt` — launch `ACTION_VIEW` intent for `OpenContent`, launch share sheet via `ACTION_SEND` for `ShareContent`, show Snackbar for `ShowMessage`
- [ ] T028 [P] [US2] Create FileProvider paths XML at `app/src/main/res/xml/history_file_paths.xml` and register provider in `app/src/main/AndroidManifest.xml` for legacy `filePath`-based sharing fallback
- [ ] T029 [US2] Update `HistoryContent` and `HistoryListItem` in `feature/history/src/main/kotlin/com/socialvideodownloader/feature/history/ui/` to support tap (onClick), long-press with context menu, and visual distinction for non-openable items
- [ ] T030 [US2] Add share/open string resources to `feature/history/src/main/res/values/strings.xml` — file unavailable message, open error, share action label, context menu labels

**Checkpoint**: Completed items can be opened and shared; missing files show clear feedback

---

## Phase 5: User Story 3 — Clean Up History Entries (Priority: P2)

**Goal**: User can delete individual items or clear all history, with choice to also remove local files

**Independent Test**: Create multiple records, delete one via context menu, then Delete All via overflow menu, confirm list updates correctly

### Tests for User Story 3

> **NOTE: Write these tests FIRST, ensure they FAIL before implementation**

- [ ] T031 [P] [US3] Create `feature/history/src/test/kotlin/com/socialvideodownloader/feature/history/domain/DeleteHistoryItemUseCaseTest.kt` — verify single record deletion via repository, optional file deletion via HistoryFileManager, and partial-failure handling
- [ ] T032 [P] [US3] Create `feature/history/src/test/kotlin/com/socialvideodownloader/feature/history/domain/DeleteAllHistoryUseCaseTest.kt` — verify `deleteAll()` on repository, optional bulk file cleanup, file-cleanup failure reporting
- [ ] T033 [US3] Extend `feature/history/src/test/kotlin/com/socialvideodownloader/feature/history/ui/HistoryViewModelTest.kt` — verify delete confirmation state transitions, single delete, Delete All (ignores search filter), file-delete toggle, post-delete empty state, and file-cleanup failure feedback

### Implementation for User Story 3

- [ ] T034 [P] [US3] Create `DeleteHistoryItemUseCase` in `feature/history/src/main/kotlin/com/socialvideodownloader/feature/history/domain/DeleteHistoryItemUseCase.kt` — delete Room record via repository, optionally delete file via HistoryFileManager, return success/partial-failure result
- [ ] T035 [P] [US3] Create `DeleteAllHistoryUseCase` in `feature/history/src/main/kotlin/com/socialvideodownloader/feature/history/domain/DeleteAllHistoryUseCase.kt` — call `deleteAll()` on repository, optionally iterate accessible files for deletion, report file-cleanup failures
- [ ] T036 [US3] Add `DeleteConfirmationState` data class to `feature/history/src/main/kotlin/com/socialvideodownloader/feature/history/ui/HistoryUiState.kt` with target (Single/All), hasAnyAccessibleFile, deleteFilesSelected, affectedCount; extend HistoryIntent with delete-related intents
- [ ] T037 [US3] Extend `HistoryViewModel` in `feature/history/src/main/kotlin/com/socialvideodownloader/feature/history/ui/HistoryViewModel.kt` to handle `DeleteItemClicked`, `DeleteAllClicked`, `DeleteFilesSelectionChanged`, `ConfirmDeletion`, `DismissDeletionDialog` — manage DeleteConfirmationState, execute use cases on confirmation, emit ShowMessage on file-cleanup failure
- [ ] T038 [US3] Create `HistoryDeleteDialog` composable in `feature/history/src/main/kotlin/com/socialvideodownloader/feature/history/ui/HistoryDeleteDialog.kt` — confirmation dialog with record count, "also delete files" checkbox (visible only when accessible files exist), confirm/cancel buttons
- [ ] T039 [US3] Add Delete All overflow menu item to top app bar in `feature/history/src/main/kotlin/com/socialvideodownloader/feature/history/ui/HistoryScreen.kt` — visible only when history is non-empty; wire HistoryDeleteDialog into Screen composable
- [ ] T040 [US3] Add delete-related string resources to `feature/history/src/main/res/values/strings.xml` — delete confirmation titles (single/all), checkbox label, file cleanup failure message, Delete All menu label

**Checkpoint**: Single and bulk delete work with record-only or record+file modes; list updates immediately

---

## Phase 6: Polish & Cross-Cutting Concerns

**Purpose**: Final validation and quality gates

- [ ] T041 Run focused unit tests with `./gradlew :core:data:testDebugUnitTest :feature:history:testDebugUnitTest` and fix any failures
- [ ] T042 Run full quality gates with `./gradlew test ktlintCheck assembleDebug` and fix any failures
- [ ] T043 Verify Room schema export at `core/data/schemas/com.socialvideodownloader.core.data.local.AppDatabase/3.json` is correct and committed
- [ ] T044 Run quickstart.md manual verification flow on device/emulator

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies — can start immediately
- **Foundational (Phase 2)**: Depends on Phase 1 (JUnit Platform) — BLOCKS all user stories
- **User Story 1 (Phase 3)**: Depends on Phase 2 completion
- **User Story 2 (Phase 4)**: Depends on Phase 3 (needs HistoryScreen, ViewModel, list items)
- **User Story 3 (Phase 5)**: Depends on Phase 3 (needs HistoryScreen, ViewModel); can run in parallel with Phase 4
- **Polish (Phase 6)**: Depends on all user stories being complete

### User Story Dependencies

- **US1 (P1)**: Can start after Foundational — no dependencies on other stories
- **US2 (P1)**: Requires US1 HistoryScreen and ViewModel as base to extend with tap/long-press handlers
- **US3 (P2)**: Requires US1 HistoryScreen and ViewModel as base; independent of US2 open/share logic

### Within Each User Story

- Tests written FIRST, verified to FAIL before implementation
- Use cases / domain logic before ViewModel
- ViewModel before UI composables
- String resources before composables that reference them
- Screen composable integrates everything last

### Parallel Opportunities

**Phase 1**: T002, T003, T004, T005 can all run in parallel (after T001 if test deps need it)
**Phase 2**: T006→T007→T008 sequential (schema chain); T009→T010 sequential (DAO→repo); T011, T012, T013 in parallel with each other and with schema chain; T014 depends on T013; T015 depends on T14
**Phase 3**: T016 and T017 in parallel (tests); T018 after tests; T021 and T022 in parallel (composables, after T020 strings)
**Phase 4**: T028 in parallel with T025–T027
**Phase 5**: T031 and T032 in parallel (tests); T034 and T035 in parallel (use cases); T038 after T036

---

## Parallel Example: Phase 2 (Foundational)

```text
# Sequential chain (schema changes):
T006 → T007 → T008 → T009 → T010

# In parallel with the above:
T011 (MVI state types)
T012 (HistoryListItem model)
T013 (HistoryFileManager interface) → T014 (Android impl) → T015 (Hilt module)
```

## Parallel Example: User Story 1

```text
# Tests first (parallel):
T016 (ObserveHistoryItemsUseCase test)
T017 (ViewModel browse/search test)

# Then implementation:
T018 (ObserveHistoryItemsUseCase)
T019 (HistoryViewModel) — depends on T018
T020 (strings) — parallel with T019
T021 (HistoryListItem composable) + T022 (HistoryContent) — after T020
T023 (HistoryScreen) — depends on all above
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete Phase 1: Setup (JUnit5 + test scaffolding)
2. Complete Phase 2: Foundational (schema v3, deleteAll, MVI types, file manager)
3. Complete Phase 3: User Story 1 (browse + search)
4. **STOP and VALIDATE**: Build and test — History tab shows searchable list
5. Demo/iterate if ready

### Incremental Delivery

1. Setup + Foundational → Foundation ready
2. Add US1 (browse/search) → Test independently → MVP!
3. Add US2 (open/share) → Test independently → Usable history
4. Add US3 (delete) → Test independently → Feature complete
5. Polish → Validated and clean

---

## Notes

- Total tasks: 44
- Setup: 5 (T001–T005)
- Foundational: 10 (T006–T015)
- US1: 8 (T016–T023)
- US2: 7 (T024–T030)
- US3: 10 (T031–T040)
- Polish: 4 (T041–T044)
- `DownloadRecord.formatLabel` already exists — no migration needed for it
- Room DB already at version 2 — only `mediaStoreUri` needs MIGRATION_2_3 to version 3
- `HistoryViewModelTest.kt` grows across stories (US1 → US2 → US3) as it verifies the same ViewModel
- Use injected dispatchers and `HistoryFileManager` boundary for file I/O — never hardcode `Dispatchers.IO`
- All user-facing text must use string resources — no hardcoded strings in composables
