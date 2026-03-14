# Tasks: Core Video Download Flow

**Input**: Design documents from `/specs/002-core-download-flow/`
**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/

**Tests**: Included per constitution principle VI (unit tests for use cases and ViewModel state transitions).

**Organization**: Tasks grouped by user story. US1 and US2 are tightly coupled (extraction feeds format selection) so US2 extends US1's foundation.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2)
- Exact file paths included in descriptions

---

## Phase 1: Setup

**Purpose**: Manifest permissions, notification channel, module dependencies

- [ ] T001 Add INTERNET, FOREGROUND_SERVICE, FOREGROUND_SERVICE_DATA_SYNC, POST_NOTIFICATIONS permissions and DownloadService declaration to `app/src/main/AndroidManifest.xml`
- [ ] T002 Create download notification channel in `app/src/main/kotlin/com/socialvideodownloader/SocialVideoDownloaderApp.kt` (onCreate, channel ID: "download_progress", name from string resource)
- [ ] T003 Add `:core:data` dependency on youtubedl-android library in `core/data/build.gradle.kts` (if not already present); add `:feature:download` dependency on `:core:domain` and `:core:data`
- [ ] T004 Add test dependencies (JUnit5, MockK, Turbine, coroutines-test) to `core/domain/build.gradle.kts`, `core/data/build.gradle.kts`, and `feature/download/build.gradle.kts`

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Domain models, repository interfaces, yt-dlp wrapper, Room migration — MUST complete before any user story

**⚠️ CRITICAL**: No user story work can begin until this phase is complete

### Domain Models

- [ ] T005 [P] Create `VideoMetadata` data class in `core/domain/src/main/kotlin/com/socialvideodownloader/core/domain/model/VideoMetadata.kt` (fields: sourceUrl, title, thumbnailUrl, durationSeconds, author, formats)
- [ ] T006 [P] Create `VideoFormatOption` data class in `core/domain/src/main/kotlin/com/socialvideodownloader/core/domain/model/VideoFormatOption.kt` (fields: formatId, label, resolution, ext, fileSizeBytes, isAudioOnly, isVideoOnly)
- [ ] T007 [P] Create `DownloadRequest` data class in `core/domain/src/main/kotlin/com/socialvideodownloader/core/domain/model/DownloadRequest.kt` (fields: id (UUID string), sourceUrl, videoTitle, thumbnailUrl, formatId, formatLabel, isVideoOnly)
- [ ] T008 [P] Create `DownloadProgress` data class in `core/domain/src/main/kotlin/com/socialvideodownloader/core/domain/model/DownloadProgress.kt` (fields: requestId, progressPercent, downloadedBytes, totalBytes, speedBytesPerSec, etaSeconds)
- [ ] T009 Add `QUEUED` and `CANCELLED` values to `DownloadStatus` enum in `core/domain/src/main/kotlin/com/socialvideodownloader/core/domain/model/DownloadStatus.kt`
- [ ] T010 Add `formatLabel: String` field to `DownloadRecord` in `core/domain/src/main/kotlin/com/socialvideodownloader/core/domain/model/DownloadRecord.kt` (default empty string)

### Repository Interfaces

- [ ] T011 [P] Create `VideoExtractorRepository` interface in `core/domain/src/main/kotlin/com/socialvideodownloader/core/domain/repository/VideoExtractorRepository.kt` (methods: extractInfo(url): VideoMetadata, download(request, callback): Unit, cancelDownload(processId): Unit)
- [ ] T012 [P] Create `MediaStoreRepository` interface in `core/domain/src/main/kotlin/com/socialvideodownloader/core/domain/repository/MediaStoreRepository.kt` (method: saveToDownloads(tempFilePath, title, mimeType): Uri)
- [ ] T013 [P] Create `ClipboardRepository` interface in `core/domain/src/main/kotlin/com/socialvideodownloader/core/domain/repository/ClipboardRepository.kt` (method: getVideoUrl(): String?)

### Room Migration

- [ ] T014 Add `formatLabel` column (String, default "") to `DownloadEntity` in `core/data/src/main/kotlin/com/socialvideodownloader/core/data/local/DownloadEntity.kt`
- [ ] T015 Update `DownloadMapper` in `core/data/src/main/kotlin/com/socialvideodownloader/core/data/local/DownloadMapper.kt` to map `formatLabel` between entity and domain model
- [ ] T016 Add Room migration v1→v2 (ALTER TABLE downloads ADD COLUMN formatLabel TEXT NOT NULL DEFAULT '') and bump database version in `core/data/src/main/kotlin/com/socialvideodownloader/core/data/local/AppDatabase.kt`

### yt-dlp Wrapper (Repository Implementation)

- [ ] T017 Create `VideoInfoMapper` in `core/data/src/main/kotlin/com/socialvideodownloader/core/data/remote/VideoInfoMapper.kt` — map youtubedl-android `VideoInfo`/`VideoFormat` to domain `VideoMetadata`/`VideoFormatOption`. Filter formats: exclude formats with null formatId; detect audio-only (vcodec=="none"), video-only (acodec=="none"); build label from height+"p" or codec; sort video by resolution desc, audio by bitrate desc
- [ ] T018 Create `VideoExtractorRepositoryImpl` in `core/data/src/main/kotlin/com/socialvideodownloader/core/data/remote/VideoExtractorRepositoryImpl.kt` — implement extractInfo() using `YoutubeDL.getInstance().getInfo()`, download() using `execute()` with processId and progress callback, cancelDownload() using `destroyProcessById()`. All operations on injected IO dispatcher
- [ ] T019 Create `MediaStoreRepositoryImpl` in `core/data/src/main/kotlin/com/socialvideodownloader/core/data/local/MediaStoreRepositoryImpl.kt` — implement saveToDownloads() using MediaStore.Downloads (API 29+) with RELATIVE_PATH="Download/SocialVideoDownloader", fallback to direct file access for API 26-28
- [ ] T020 Create `ClipboardRepositoryImpl` in `core/data/src/main/kotlin/com/socialvideodownloader/core/data/local/ClipboardRepositoryImpl.kt` — implement getVideoUrl() using Android ClipboardManager, validate URL with regex pattern
- [ ] T021 Create `ExtractorModule` Hilt module in `core/data/src/main/kotlin/com/socialvideodownloader/core/data/di/ExtractorModule.kt` — bind VideoExtractorRepositoryImpl→VideoExtractorRepository, MediaStoreRepositoryImpl→MediaStoreRepository, ClipboardRepositoryImpl→ClipboardRepository
- [ ] T022 Update `RepositoryModule` in `core/data/src/main/kotlin/com/socialvideodownloader/core/data/di/RepositoryModule.kt` if new bindings conflict; otherwise ExtractorModule handles all new repos

### MVI Skeleton

- [ ] T023 [P] Create `DownloadUiState` sealed interface in `feature/download/src/main/kotlin/com/socialvideodownloader/feature/download/ui/DownloadUiState.kt` — Idle(clipboardUrl: String?), Extracting(url: String), FormatSelection(metadata, selectedFormatId), Downloading(metadata, progress), Done(metadata, filePath), Error(message, retryAction). Include RetryAction sealed interface (RetryExtraction, RetryDownload)
- [ ] T024 [P] Create `DownloadIntent` sealed interface in `feature/download/src/main/kotlin/com/socialvideodownloader/feature/download/ui/DownloadIntent.kt` — UrlChanged, ExtractClicked, FormatSelected, DownloadClicked, CancelDownloadClicked, RetryClicked, OpenFileClicked, ShareFileClicked, NewDownloadClicked, ClipboardUrlDetected

**Checkpoint**: Foundation ready — all domain models, repository interfaces, implementations, and MVI types in place. User story implementation can begin.

---

## Phase 3: User Story 1 — Paste URL and Download Video (Priority: P1) 🎯 MVP

**Goal**: User pastes a URL, app extracts video info, user taps download, video saves to device.

**Independent Test**: Paste a valid YouTube/TikTok URL → extract → select format → download → verify file in Downloads/SocialVideoDownloader/

### Use Cases

- [ ] T025 [P] [US1] Create `ExtractVideoInfoUseCase` in `core/domain/src/main/kotlin/com/socialvideodownloader/core/domain/usecase/ExtractVideoInfoUseCase.kt` — `operator fun invoke(url: String): Result<VideoMetadata>`, delegates to VideoExtractorRepository.extractInfo(), wraps exceptions in Result.failure
- [ ] T026 [P] [US1] Create `DownloadVideoUseCase` in `core/domain/src/main/kotlin/com/socialvideodownloader/core/domain/usecase/DownloadVideoUseCase.kt` — `operator fun invoke(request: DownloadRequest)`: delegates to VideoExtractorRepository.download(), handles format string construction (append +bestaudio for video-only formats, add --merge-output-format mp4)
- [ ] T027 [P] [US1] Create `CancelDownloadUseCase` in `core/domain/src/main/kotlin/com/socialvideodownloader/core/domain/usecase/CancelDownloadUseCase.kt` — `operator fun invoke(requestId: String)`: delegates to VideoExtractorRepository.cancelDownload()
- [ ] T028 [P] [US1] Create `SaveDownloadRecordUseCase` in `core/domain/src/main/kotlin/com/socialvideodownloader/core/domain/usecase/SaveDownloadRecordUseCase.kt` — `operator fun invoke(record: DownloadRecord): Long`: delegates to DownloadRepository.insert()
- [ ] T029 [P] [US1] Create `SaveFileToMediaStoreUseCase` in `core/domain/src/main/kotlin/com/socialvideodownloader/core/domain/usecase/SaveFileToMediaStoreUseCase.kt` — `operator fun invoke(tempFilePath, title, mimeType): Uri`: delegates to MediaStoreRepository.saveToDownloads()

### ViewModel

- [ ] T030 [US1] Create `DownloadViewModel` in `feature/download/src/main/kotlin/com/socialvideodownloader/feature/download/ui/DownloadViewModel.kt` — @HiltViewModel, inject use cases + @IoDispatcher. Expose `StateFlow<DownloadUiState>` (initial: Idle). Implement `onIntent(intent: DownloadIntent)` handling: UrlChanged→update url in Idle, ExtractClicked→launch extraction coroutine→Extracting→FormatSelection (pre-select best format) or Error, DownloadClicked→start download→Downloading with progress updates, completion→Done. Include viewModelScope coroutine management.

### UI Components

- [ ] T031 [P] [US1] Create `UrlInputContent` composable in `feature/download/src/main/kotlin/com/socialvideodownloader/feature/download/ui/components/UrlInputContent.kt` — OutlinedTextField for URL, "Extract" button (enabled when URL non-empty), loading state disables input
- [ ] T032 [P] [US1] Create `VideoInfoContent` composable in `feature/download/src/main/kotlin/com/socialvideodownloader/feature/download/ui/components/VideoInfoContent.kt` — Card with AsyncImage (Coil) for thumbnail, video title, duration formatted as MM:SS, author name
- [ ] T033 [P] [US1] Create `DownloadProgressContent` composable in `feature/download/src/main/kotlin/com/socialvideodownloader/feature/download/ui/components/DownloadProgressContent.kt` — LinearProgressIndicator with percentage, speed (formatted as KB/s or MB/s), ETA (formatted as MM:SS), Cancel button

### Main Screen

- [ ] T034 [US1] Replace placeholder `DownloadScreen` in `feature/download/src/main/kotlin/com/socialvideodownloader/feature/download/ui/DownloadScreen.kt` — inject DownloadViewModel, collectAsState for UiState, `when` on state to render: Idle→UrlInputContent, Extracting→UrlInputContent(loading) + spinner, FormatSelection→VideoInfoContent + format chips + Download button, Downloading→VideoInfoContent + DownloadProgressContent, Done→completion content, Error→error content. Wire intents from UI events to viewModel.onIntent()
- [ ] T035 [US1] Add string resources for download feature in `feature/download/src/main/res/values/strings.xml` — all user-facing text: "Extract", "Download", "Extracting video info...", "Cancel", "Retry", progress format strings, error messages

### Unit Tests

- [ ] T036 [P] [US1] Create `ExtractVideoInfoUseCaseTest` in `core/domain/src/test/kotlin/com/socialvideodownloader/core/domain/usecase/ExtractVideoInfoUseCaseTest.kt` — test success returns VideoMetadata, test network error returns failure, test invalid URL returns failure
- [ ] T037 [P] [US1] Create `VideoInfoMapperTest` in `core/data/src/test/kotlin/com/socialvideodownloader/core/data/remote/VideoInfoMapperTest.kt` — test format filtering (null formatId excluded), audio-only detection (vcodec=="none"), video-only detection (acodec=="none"), label generation, sorting (video by resolution desc, audio by bitrate desc)
- [ ] T038 [US1] Create `DownloadViewModelTest` in `feature/download/src/test/kotlin/com/socialvideodownloader/feature/download/ui/DownloadViewModelTest.kt` — test Idle→Extracting→FormatSelection flow, test Extracting→Error flow, test FormatSelection→Downloading→Done flow, test UrlChanged updates state, test default format pre-selection. Use Turbine for StateFlow assertions, MockK for use cases.

**Checkpoint**: Core download flow works end-to-end. User can paste URL, extract info, see formats, download, and file is saved. This is the MVP.

---

## Phase 4: User Story 2 — Format Selection with Pre-Selected Best Quality (Priority: P1)

**Goal**: Formats displayed as chips grouped by type (video/audio), best quality pre-selected, single selection.

**Independent Test**: Extract a YouTube URL → verify format chips show resolution + file size, best is pre-selected, tapping another chip changes selection.

- [ ] T039 [P] [US2] Create `FormatChipsContent` composable in `feature/download/src/main/kotlin/com/socialvideodownloader/feature/download/ui/components/FormatChipsContent.kt` — two FlowRow sections ("Video" and "Audio-Only" headers), FilterChip per format showing label + file size (formatted as MB/GB), selected state matches selectedFormatId, onFormatSelected callback
- [ ] T040 [US2] Update `DownloadViewModel` to handle `FormatSelected` intent — update selectedFormatId in FormatSelection state; ensure pre-selection logic picks first video format (highest resolution) in `feature/download/src/main/kotlin/com/socialvideodownloader/feature/download/ui/DownloadViewModel.kt`
- [ ] T041 [US2] Integrate `FormatChipsContent` into `DownloadScreen` FormatSelection state rendering in `feature/download/src/main/kotlin/com/socialvideodownloader/feature/download/ui/DownloadScreen.kt`
- [ ] T042 [US2] Add format-related string resources to `feature/download/src/main/res/values/strings.xml` — "Video", "Audio Only", file size format strings

**Checkpoint**: Format selection fully functional with chips, grouping, and pre-selection.

---

## Phase 5: User Story 3 — Background Download with Foreground Service (Priority: P1)

**Goal**: Downloads run in a foreground service with notification progress, cancel action, and queue support.

**Independent Test**: Start download → minimize app → verify notification shows progress → return to app → verify in-app progress matches. Cancel from notification → verify download stops.

### Service Infrastructure

- [ ] T043 [US3] Create `DownloadNotificationManager` in `feature/download/src/main/kotlin/com/socialvideodownloader/feature/download/service/DownloadNotificationManager.kt` — build progress notification (title: video title, content: "Downloading... XX%", progress bar, speed + ETA in subtext, cancel PendingIntent action), completion notification, error notification. Use channel ID "download_progress".
- [ ] T044 [US3] Create `DownloadService` foreground service in `feature/download/src/main/kotlin/com/socialvideodownloader/feature/download/service/DownloadService.kt` — @AndroidEntryPoint, inject DownloadVideoUseCase + CancelDownloadUseCase + SaveDownloadRecordUseCase + SaveFileToMediaStoreUseCase + DownloadNotificationManager + @IoDispatcher. Handle start commands: ACTION_START_DOWNLOAD (DownloadRequest as extras), ACTION_CANCEL_DOWNLOAD (requestId). Manage in-memory ConcurrentLinkedQueue<DownloadRequest>. Run yt-dlp execute() on IO dispatcher with progress callback → update notification + emit state via shared StateFlow. On completion: save to MediaStore, save DownloadRecord to Room, update notification. On cancel: destroyProcessById, delete partial file. On error: emit error state. Auto-stop service when queue empty and no active download.
- [ ] T045 [US3] Create shared `DownloadServiceStateHolder` singleton in `feature/download/src/main/kotlin/com/socialvideodownloader/feature/download/service/DownloadServiceStateHolder.kt` — @Singleton Hilt-provided, holds `StateFlow<DownloadServiceState>` for communication between service and ViewModel. States: Idle, Downloading(requestId, progress), Queued(pendingIds), Completed(requestId, filePath), Failed(requestId, error), Cancelled(requestId).

### ViewModel Integration

- [ ] T046 [US3] Update `DownloadViewModel` to use DownloadService instead of direct use case calls in `feature/download/src/main/kotlin/com/socialvideodownloader/feature/download/ui/DownloadViewModel.kt` — DownloadClicked sends start intent to DownloadService, CancelDownloadClicked sends cancel intent. Collect DownloadServiceStateHolder.state to update UiState (Downloading progress, Done on completion, Error on failure). Handle queued state display.
- [ ] T047 [US3] Add `DownloadService` declaration to `feature/download/src/main/AndroidManifest.xml` — `<service android:name=".service.DownloadService" android:foregroundServiceType="dataSync" android:exported="false" />`

### Unit Tests

- [ ] T048 [US3] Add cancel and service state tests to `DownloadViewModelTest` in `feature/download/src/test/kotlin/com/socialvideodownloader/feature/download/ui/DownloadViewModelTest.kt` — test CancelDownloadClicked returns to FormatSelection, test service Completed state transitions to Done, test service Failed state transitions to Error

**Checkpoint**: Downloads survive app backgrounding. Notification shows progress and cancel. Queue works for sequential downloads.

---

## Phase 6: User Story 4 — Clipboard Auto-Detection (Priority: P2)

**Goal**: App auto-detects video URLs from clipboard when opened/resumed.

**Independent Test**: Copy a YouTube URL → open app → URL field is pre-filled. Copy non-URL text → open app → field stays empty.

- [ ] T049 [P] [US4] Create `GetClipboardUrlUseCase` in `core/domain/src/main/kotlin/com/socialvideodownloader/core/domain/usecase/GetClipboardUrlUseCase.kt` — `operator fun invoke(): String?`: delegates to ClipboardRepository.getVideoUrl()
- [ ] T050 [US4] Update `DownloadViewModel` to handle clipboard detection in `feature/download/src/main/kotlin/com/socialvideodownloader/feature/download/ui/DownloadViewModel.kt` — add `checkClipboard()` method called on init and when ClipboardUrlDetected intent received. Track `lastClipboardUrl` to avoid re-populating. Only auto-fill when in Idle state.
- [ ] T051 [US4] Update `DownloadScreen` to trigger clipboard check on `Lifecycle.Event.ON_RESUME` in `feature/download/src/main/kotlin/com/socialvideodownloader/feature/download/ui/DownloadScreen.kt` — use `LifecycleEventEffect(Lifecycle.Event.ON_RESUME)` to call viewModel.onIntent(ClipboardUrlDetected)
- [ ] T052 [US4] Add clipboard-related string resources to `feature/download/src/main/res/values/strings.xml` — optional toast or hint: "URL detected from clipboard"

**Checkpoint**: Clipboard auto-detection works. User opens app with a video URL in clipboard and it auto-populates.

---

## Phase 7: User Story 5 — Error Handling with Retry (Priority: P2)

**Goal**: All errors show human-readable messages with actionable retry.

**Independent Test**: Enter invalid URL → see friendly error → tap Retry → extraction re-attempted.

- [ ] T053 [US5] Create error message mapping utility in `feature/download/src/main/kotlin/com/socialvideodownloader/feature/download/ui/ErrorMessageMapper.kt` — map YoutubeDLException messages to user-friendly strings: "Unsupported URL"→"This URL is not supported. Try a link from a supported platform.", "Video unavailable"→"This video is unavailable. It may be private or removed.", network errors→"No internet connection. Check your network and try again.", storage errors→"Not enough storage space. Free up space and try again.", generic→"Something went wrong. Please try again." Use string resources.
- [ ] T054 [P] [US5] Create `DownloadErrorContent` composable in `feature/download/src/main/kotlin/com/socialvideodownloader/feature/download/ui/components/DownloadErrorContent.kt` — error icon (Material Icons), error message text, "Retry" button, "New Download" button
- [ ] T055 [US5] Update `DownloadViewModel` to handle RetryClicked intent in `feature/download/src/main/kotlin/com/socialvideodownloader/feature/download/ui/DownloadViewModel.kt` — inspect RetryAction: RetryExtraction→re-run extraction with stored URL, RetryDownload→re-submit download request to service. Update error state creation to use ErrorMessageMapper.
- [ ] T056 [US5] Integrate `DownloadErrorContent` into `DownloadScreen` Error state rendering in `feature/download/src/main/kotlin/com/socialvideodownloader/feature/download/ui/DownloadScreen.kt`
- [ ] T057 [US5] Add error string resources to `feature/download/src/main/res/values/strings.xml` — all error messages from ErrorMessageMapper

**Checkpoint**: All error scenarios show user-friendly messages with working Retry.

---

## Phase 8: User Story 6 — Download Completion with Open and Share (Priority: P2)

**Goal**: Success screen with Open (default media player) and Share (system share sheet) buttons.

**Independent Test**: Complete a download → see success screen → tap Open → video plays → go back → tap Share → share sheet appears.

- [ ] T058 [P] [US6] Create `DownloadCompleteContent` composable in `feature/download/src/main/kotlin/com/socialvideodownloader/feature/download/ui/components/DownloadCompleteContent.kt` — video thumbnail + title, checkmark icon, "Download complete" text, "Open" button (filled), "Share" button (outlined), "New Download" button (text)
- [ ] T059 [US6] Update `DownloadViewModel` to handle OpenFileClicked and ShareFileClicked intents in `feature/download/src/main/kotlin/com/socialvideodownloader/feature/download/ui/DownloadViewModel.kt` — OpenFileClicked: create ACTION_VIEW intent with file URI + video MIME type, emit as one-shot event (SharedFlow or Channel). ShareFileClicked: create ACTION_SEND intent with file URI, emit as one-shot event. NewDownloadClicked: reset to Idle state.
- [ ] T060 [US6] Integrate `DownloadCompleteContent` into `DownloadScreen` Done state rendering and handle one-shot intent events (LaunchedEffect collecting from ViewModel's event channel, launching intents via LocalContext) in `feature/download/src/main/kotlin/com/socialvideodownloader/feature/download/ui/DownloadScreen.kt`
- [ ] T061 [US6] Add completion string resources to `feature/download/src/main/res/values/strings.xml` — "Download complete", "Open", "Share", "New Download"

**Checkpoint**: Full post-download experience works — Open plays video, Share opens share sheet.

---

## Phase 9: Polish & Cross-Cutting Concerns

**Purpose**: Additional tests, ktlint, validation

- [ ] T062 [P] Create `DownloadVideoUseCaseTest` in `core/domain/src/test/kotlin/com/socialvideodownloader/core/domain/usecase/DownloadVideoUseCaseTest.kt` — test format string construction (video-only appends +bestaudio), test cancellation handling (CanceledException not treated as error)
- [ ] T063 [P] Add ViewModel error + retry tests to `DownloadViewModelTest` in `feature/download/src/test/kotlin/com/socialvideodownloader/feature/download/ui/DownloadViewModelTest.kt` — test Error→RetryClicked→Extracting, test Error→NewDownloadClicked→Idle, test clipboard detection (Idle with clipboardUrl)
- [ ] T064 Run `./gradlew ktlintCheck` and fix any violations across all modified files
- [ ] T065 Run `./gradlew assembleDebug` to verify clean build with no compilation errors
- [ ] T066 Run `./gradlew test` to verify all unit tests pass

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies — start immediately
- **Foundational (Phase 2)**: Depends on Phase 1 — BLOCKS all user stories
- **US1 (Phase 3)**: Depends on Phase 2 — core MVP
- **US2 (Phase 4)**: Depends on Phase 3 (needs DownloadScreen + ViewModel)
- **US3 (Phase 5)**: Depends on Phase 3 (wraps existing download logic in service)
- **US4 (Phase 6)**: Depends on Phase 2 only (can parallel with US1 but needs ViewModel from Phase 3 for integration)
- **US5 (Phase 7)**: Depends on Phase 3 (needs ViewModel + error states)
- **US6 (Phase 8)**: Depends on Phase 3 (needs Done state + ViewModel)
- **Polish (Phase 9)**: Depends on all user story phases

### User Story Dependencies

- **US1 (P1)**: Foundation only — no other story dependencies. **This is the MVP.**
- **US2 (P1)**: Depends on US1 (needs FormatSelection state and DownloadScreen)
- **US3 (P1)**: Depends on US1 (moves download execution from ViewModel to Service)
- **US4 (P2)**: Depends on US1 (needs Idle state and ViewModel)
- **US5 (P2)**: Depends on US1 (needs Error state and ViewModel)
- **US6 (P2)**: Depends on US1 (needs Done state and ViewModel)
- **US4, US5, US6** can run in parallel after US1

### Within Each User Story

- Use cases before ViewModel integration
- ViewModel before UI components
- UI components before screen integration
- Tests can parallel with implementation (not TDD for this project)

### Parallel Opportunities

**Phase 2 (Foundation)**: T005–T008 (models), T011–T013 (repo interfaces), T023–T024 (MVI types) — all [P]
**Phase 3 (US1)**: T025–T029 (use cases), T031–T033 (UI components), T036–T037 (tests) — all [P]
**After US1**: US4, US5, US6 can run in parallel (different files/concerns)

---

## Parallel Example: Phase 2 Foundation

```bash
# Launch all domain models in parallel:
Task: "T005 Create VideoMetadata in core/domain/.../model/VideoMetadata.kt"
Task: "T006 Create VideoFormatOption in core/domain/.../model/VideoFormatOption.kt"
Task: "T007 Create DownloadRequest in core/domain/.../model/DownloadRequest.kt"
Task: "T008 Create DownloadProgress in core/domain/.../model/DownloadProgress.kt"

# Launch all repository interfaces in parallel:
Task: "T011 Create VideoExtractorRepository interface"
Task: "T012 Create MediaStoreRepository interface"
Task: "T013 Create ClipboardRepository interface"
```

## Parallel Example: US1 Use Cases

```bash
# Launch all use cases in parallel:
Task: "T025 Create ExtractVideoInfoUseCase"
Task: "T026 Create DownloadVideoUseCase"
Task: "T027 Create CancelDownloadUseCase"
Task: "T028 Create SaveDownloadRecordUseCase"
Task: "T029 Create SaveFileToMediaStoreUseCase"
```

---

## Implementation Strategy

### MVP First (US1 Only)

1. Complete Phase 1: Setup
2. Complete Phase 2: Foundational
3. Complete Phase 3: User Story 1 (core download flow)
4. **STOP and VALIDATE**: Test end-to-end by downloading a video
5. This alone delivers a usable app

### Incremental Delivery

1. Setup + Foundation → infrastructure ready
2. Add US1 → core flow works → **MVP!**
3. Add US2 → format chips polished → better UX
4. Add US3 → service + notification → production-ready downloads
5. Add US5 → error handling → robust experience
6. Add US6 → Open/Share → complete post-download UX
7. Add US4 → clipboard detection → convenience feature
8. Polish → tests pass, ktlint clean

---

## Notes

- [P] tasks = different files, no dependencies on incomplete tasks
- [Story] label maps task to specific user story for traceability
- Commit after each task or logical group
- Constitution requires unit tests for all use cases and ViewModel state transitions (Principle VI)
- All user-facing strings via resources (Principle IV)
- No hardcoded dispatchers — inject via @IoDispatcher (Principle IV)
