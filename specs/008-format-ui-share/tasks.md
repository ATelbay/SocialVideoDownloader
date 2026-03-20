# Tasks: Format Selection UI Overhaul + Share-without-Save

**Input**: Design documents from `/specs/008-format-ui-share/`
**Prerequisites**: plan.md, spec.md, research.md, data-model.md, quickstart.md

**Tests**: Include unit-test tasks for ViewModel state transitions (share mode) per constitution VI.

**Organization**: Tasks are grouped by user story to enable independent implementation and testing of each story.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3)
- Include exact file paths in descriptions

## Path Conventions

- `app/src/main/...` for app entry points, navigation, and manifest changes
- `feature/<name>/src/main/...` and `feature/<name>/src/test/...` for feature UI, ViewModels, and feature-specific services
- `core/domain/src/main/...` for domain models and repository interfaces
- `core/ui/src/main/...` for shared Compose UI and theme primitives
- `src/main/res/values/strings.xml` in the touched module for user-facing strings

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: No new modules or dependencies. This phase handles string resources needed across multiple stories.

- [ ] T001 Add string resources for "Share" button label and any share-related UI text in `feature/download/src/main/res/values/strings.xml`

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Refactor GradientButton width control, create SecondaryButton, and add `ShareFormatClicked` intent — these block US3 and US4.

**CRITICAL**: No user story 3 or 4 work can begin until this phase is complete.

- [ ] T002 Remove internal `.fillMaxWidth()` from `GradientButton` in `core/ui/src/main/kotlin/com/socialvideodownloader/core/ui/components/GradientButton.kt` — let callers control width
- [ ] T003 [P] Add `Modifier.fillMaxWidth()` to GradientButton call site in `feature/download/src/main/kotlin/com/socialvideodownloader/feature/download/ui/components/IdleContent.kt`
- [ ] T004 [P] Add `Modifier.fillMaxWidth()` to GradientButton call site in `feature/download/src/main/kotlin/com/socialvideodownloader/feature/download/ui/components/DownloadErrorContent.kt`
- [ ] T005 [P] Add `Modifier.fillMaxWidth()` to GradientButton call site in `feature/history/src/main/kotlin/com/socialvideodownloader/feature/history/components/HistoryEmptyState.kt`
- [ ] T006 [P] Add `Modifier.fillMaxWidth()` to GradientButton call site in `feature/library/src/main/kotlin/com/socialvideodownloader/feature/library/ui/LibraryEmptyState.kt`
- [ ] T007 [P] Create `SecondaryButton` composable (outlined variant, same height/shape as GradientButton) in `core/ui/src/main/kotlin/com/socialvideodownloader/core/ui/components/SecondaryButton.kt`
- [ ] T008 [P] Add `ShareFormatClicked` data object to `DownloadIntent` in `feature/download/src/main/kotlin/com/socialvideodownloader/feature/download/ui/DownloadIntent.kt`

**Checkpoint**: `./gradlew assembleDebug` — all existing buttons render at full width, no visual regressions.

---

## Phase 3: User Story 1 — Consistent Screen Padding (Priority: P1) MVP

**Goal**: Fix doubled bottom insets on all three screens by zeroing inner Scaffold `contentWindowInsets`.

**Independent Test**: Open each screen on emulator — bottom padding should look normal, not doubled.

### Implementation for User Story 1

- [ ] T009 [P] [US1] Add `contentWindowInsets = WindowInsets(0, 0, 0, 0)` to inner Scaffold in `feature/download/src/main/kotlin/com/socialvideodownloader/feature/download/ui/DownloadScreen.kt`
- [ ] T010 [P] [US1] Add `contentWindowInsets = WindowInsets(0, 0, 0, 0)` to inner Scaffold in `feature/history/src/main/kotlin/com/socialvideodownloader/feature/history/ui/HistoryScreen.kt`
- [ ] T011 [P] [US1] Add `contentWindowInsets = WindowInsets(0, 0, 0, 0)` to inner Scaffold in `feature/library/src/main/kotlin/com/socialvideodownloader/feature/library/ui/LibraryScreen.kt`

**Checkpoint**: All 3 screens display correct bottom spacing — no doubled padding.

---

## Phase 4: User Story 2 — Polished Video Info Card (Priority: P1)

**Goal**: Restyle VideoInfoCard thumbnail to edge-to-edge with top-only rounding, and move PlatformBadge to thumbnail overlay.

**Independent Test**: Extract any video URL — thumbnail fills card edge-to-edge with rounded top corners only, badge overlays bottom-left of thumbnail, no FlowRow below title.

### Implementation for User Story 2

- [ ] T012 [US2] Restructure `FullVideoInfoCard` layout in `core/ui/src/main/kotlin/com/socialvideodownloader/core/ui/components/VideoInfoCard.kt`: remove outer 14dp padding from thumbnail area, keep padded Column for text content below thumbnail
- [ ] T013 [US2] Change thumbnail clip shape to `RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp, bottomStart = 0.dp, bottomEnd = 0.dp)` in `core/ui/src/main/kotlin/com/socialvideodownloader/core/ui/components/VideoInfoCard.kt`
- [ ] T014 [US2] Move `PlatformBadge` into the thumbnail `Box` as a bottom-left overlay with `Modifier.align(Alignment.BottomStart).padding(10.dp)` and remove the `FlowRow` and its preceding `Spacer` in `core/ui/src/main/kotlin/com/socialvideodownloader/core/ui/components/VideoInfoCard.kt`

**Checkpoint**: VideoInfoCard displays edge-to-edge thumbnail, rounded top corners, square bottom corners, badge overlaid on thumbnail.

---

## Phase 5: User Story 3 — Download and Share Buttons (Priority: P1)

**Goal**: Replace the bottom download button in format chips with a side-by-side Download + Share button row between VideoInfoCard and FormatChipsContent.

**Independent Test**: Extract any URL — two half-width buttons visible between card and format chips, old bottom button gone, all other screens show full-width buttons.

### Implementation for User Story 3

- [ ] T015 [US3] Remove `GradientButton` and `onDownloadClicked` callback from `FormatChipsContent` in `feature/download/src/main/kotlin/com/socialvideodownloader/feature/download/ui/components/FormatChipsContent.kt`, and in the same commit update `DownloadScreen.kt`: remove the `onDownloadClicked` arg from the `FormatChipsContent` call, add a `Row` with Download (`GradientButton`, `Modifier.weight(1f)`, onClick → `DownloadIntent.DownloadClicked`) and Share (`SecondaryButton`, `Modifier.weight(1f)`, onClick → `DownloadIntent.ShareFormatClicked`) buttons between `VideoInfoCard` and `FormatChipsContent` in the `FormatSelection` branch

**Checkpoint**: Two half-width buttons visible. Download triggers existing flow. Share emits `ShareFormatClicked` (handler implemented in US4).

---

## Phase 6: User Story 4 — Share Without Saving (Priority: P2)

**Goal**: Implement share-only download flow: temp dir, skip MediaStore/Room, FileProvider URI, share sheet, return to FormatSelection.

**Independent Test**: Tap Share, download completes, share sheet opens, dismiss → back to FormatSelection. No History/Library entry. Temp files cleaned up on screen exit.

### Tests for User Story 4

- [ ] T016 [P] [US4] Add ViewModel test for share-mode state transitions in `feature/download/src/test/kotlin/com/socialvideodownloader/feature/download/ui/DownloadViewModelTest.kt`: (a) ShareFormatClicked → Downloading(isShareMode=true) → Completed → ShareFile event emitted + state restored to FormatSelection, (b) share-mode download failure → state returns to FormatSelection (not Error→Done)

### Implementation for User Story 4

- [ ] T017 [P] [US4] Add `shareOnly: Boolean = false` to `DownloadRequest` in `core/domain/src/main/kotlin/com/socialvideodownloader/core/domain/model/DownloadRequest.kt`
- [ ] T018 [P] [US4] Add `isShareMode: Boolean = false` to `DownloadUiState.Downloading` in `feature/download/src/main/kotlin/com/socialvideodownloader/feature/download/ui/DownloadUiState.kt`
- [ ] T019 [US4] Add `<cache-path name="share_temp" path="ytdl_share/" />` to `app/src/main/res/xml/history_file_paths.xml`
- [ ] T020 [US4] Handle `shareOnly` in `DownloadService`: pass via intent extra, when true use `cacheDir/ytdl_share/` output dir, skip `saveFileToMediaStore()` and `saveDownloadRecord()`, generate `content://` URI via `FileProvider.getUriForFile()`, emit `Completed` with that URI in `feature/download/src/main/kotlin/com/socialvideodownloader/feature/download/service/DownloadService.kt`
- [ ] T021 [US4] Handle `ShareFormatClicked` in ViewModel: build `DownloadRequest(shareOnly=true)`, set `Downloading(isShareMode=true)`. On `Completed` in share mode: emit `ShareFile` event and reconstruct `FormatSelection(metadata, selectedFormatId)` from the `Downloading` state fields. On failure/cancellation in share mode: return to `FormatSelection` (not `Error`), clean up partial temp files. File: `feature/download/src/main/kotlin/com/socialvideodownloader/feature/download/ui/DownloadViewModel.kt`
- [ ] T022 [US4] Add `cacheDir/ytdl_share/` cleanup in `DownloadViewModel.onCleared()` in `feature/download/src/main/kotlin/com/socialvideodownloader/feature/download/ui/DownloadViewModel.kt`

**Checkpoint**: Share flow works end-to-end. No History/Library entries for shared videos. Temp files cleaned up.

---

## Phase 7: Polish & Cross-Cutting Concerns

**Purpose**: Final validation and cleanup.

- [ ] T023 Run `./gradlew assembleDebug` and verify clean build
- [ ] T024 Run `./gradlew test` and verify all unit tests pass
- [ ] T025 Run `./gradlew ktlintCheck` and fix any violations
- [ ] T026 Manual emulator test: verify all 3 screens padding, format selection visuals, Download and Share buttons end-to-end

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies — can start immediately
- **Foundational (Phase 2)**: T002 must complete before T003–T006 (they depend on the refactored GradientButton). T007, T008 are independent of T002–T006 and of each other.
- **US1 (Phase 3)**: Can start after Phase 1 — independent of Foundational phase
- **US2 (Phase 4)**: Can start after Phase 1 — independent of Foundational phase
- **US3 (Phase 5)**: Depends on Phase 2 (needs SecondaryButton, refactored GradientButton, and ShareFormatClicked intent)
- **US4 (Phase 6)**: Depends on US3 (Share button wired in DownloadScreen) and Phase 2 (ShareFormatClicked intent exists)
- **Polish (Phase 7)**: Depends on all phases complete

### User Story Dependencies

- **US1 (P1)**: Independent — can start immediately after Phase 1
- **US2 (P1)**: Independent — can start immediately after Phase 1
- **US3 (P1)**: Depends on Phase 2 (SecondaryButton, GradientButton refactor, ShareFormatClicked intent)
- **US4 (P2)**: Depends on US3 (Share button must exist in UI)

### Parallel Opportunities

- T001 (setup) can run first, then T002–T008 foundational tasks
- T003, T004, T005, T006 can run in parallel (different files, all add `.fillMaxWidth()`)
- T007, T008 can run in parallel with T003–T006 (different files)
- T009, T010, T011 can run in parallel (different screen files, same 1-line fix)
- T016, T017, T018 can run in parallel (different files, no dependencies)
- US1 and US2 can run in parallel with Phase 2 (no dependencies between them)

---

## Parallel Example: Foundational Phase

```
# These 4 tasks touch different files and can run simultaneously:
T003: Add .fillMaxWidth() to IdleContent.kt
T004: Add .fillMaxWidth() to DownloadErrorContent.kt
T005: Add .fillMaxWidth() to HistoryEmptyState.kt
T006: Add .fillMaxWidth() to LibraryEmptyState.kt
```

## Parallel Example: User Story 1

```
# All 3 tasks touch different screen files:
T009: Fix insets in DownloadScreen.kt
T010: Fix insets in HistoryScreen.kt
T011: Fix insets in LibraryScreen.kt
```

## Parallel Example: User Story 4 Setup

```
# These 3 tasks touch different model files:
T016: Add ViewModel tests for share mode
T017: Add shareOnly to DownloadRequest.kt
T018: Add isShareMode to DownloadUiState.kt
```

---

## Implementation Strategy

### MVP First (US1 + US2)

1. Complete Phase 1: Setup (string resources)
2. Complete Phase 3: US1 (padding fix) — all screens immediately improved
3. Complete Phase 4: US2 (card restyle) — format selection screen polished
4. **STOP and VALIDATE**: Test padding on all screens, test card visuals
5. Deploy/demo if ready

### Full Delivery

1. Setup → Foundational → US1 + US2 (parallel) → US3 → US4 → Polish
2. Each phase adds visible value without breaking previous work
3. US4 is the only phase with new backend logic (service changes)

---

## Notes

- [P] tasks = different files, no dependencies
- [Story] label maps task to specific user story for traceability
- Each user story is independently completable and testable
- Commit after each task or logical group
- Stop at any checkpoint to validate story independently
