# Tasks: KMP iOS Migration

**Input**: Design documents from `/specs/011-kmp-ios-migration/`
**Prerequisites**: plan.md (required), spec.md (required), research.md, data-model.md, contracts/

**Tests**: Include unit-test tasks for shared use cases and ViewModels per project test discipline (Principle VI).

**Organization**: Tasks are grouped by user story. US6 (Shared Business Logic) and US2 (Android Preservation) are foundational — they span the migration infrastructure that all iOS stories depend on.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3)
- Include exact file paths in descriptions

## Path Conventions

- `core/domain/src/commonMain/kotlin/...` for shared domain models, interfaces, use cases (KMP)
- `shared/network/src/commonMain/kotlin/...` for shared Ktor-based server API client
- `shared/data/src/commonMain/kotlin/...` for shared Room KMP database, platform abstractions
- `shared/feature-*/src/commonMain/kotlin/...` for shared ViewModel state machines
- `feature/*/src/main/kotlin/...` for Android Compose UI (unchanged path convention)
- `iosApp/iosApp/...` for iOS SwiftUI app
- `build-logic/convention/src/main/kotlin/...` for Gradle convention plugins

---

## Phase 1: Setup (KMP Gradle Infrastructure)

**Purpose**: Establish KMP build system, convention plugins, version catalog updates, and iOS project shell

- [x] T001 Add KMP dependencies to version catalog: Ktor 3.4.1, Koin BOM 4.2.0, SKIE 0.10.10, Multiplatform Settings 1.3.0, kotlin-multiplatform plugin in `gradle/libs.versions.toml`
- [x] T002 Create `KmpLibraryConventionPlugin` (id: `svd.kmp.library`) applying kotlin-multiplatform, configuring Android target + iOS targets (iosArm64, iosSimulatorArm64), setting up source sets in `build-logic/convention/src/main/kotlin/KmpLibraryConventionPlugin.kt`
- [x] T003 Create `KmpFeatureConventionPlugin` (id: `svd.kmp.feature`) extending svd.kmp.library with Koin dependencies and :core:domain dependency in `build-logic/convention/src/main/kotlin/KmpFeatureConventionPlugin.kt`
- [x] T004 Register new convention plugins in `build-logic/convention/build.gradle.kts` gradlePlugin block
- [x] T005 [P] Add `:shared:network`, `:shared:data`, `:shared:feature-download`, `:shared:feature-history`, `:shared:feature-library` module includes in `settings.gradle.kts`
- [x] T006 [P] Create scaffold `shared/network/build.gradle.kts` applying svd.kmp.library with Ktor + kotlinx-serialization dependencies
- [x] T007 [P] Create scaffold `shared/data/build.gradle.kts` applying svd.kmp.library with Room KMP + Koin dependencies
- [x] T008 [P] Create scaffold `shared/feature-download/build.gradle.kts` applying svd.kmp.feature with :shared:data and :shared:network dependencies
- [x] T009 [P] Create scaffold `shared/feature-history/build.gradle.kts` applying svd.kmp.feature with :shared:data dependency
- [x] T010 [P] Create scaffold `shared/feature-library/build.gradle.kts` applying svd.kmp.feature with :shared:data dependency
- [x] T011 Create iOS Xcode project shell at `iosApp/` with SwiftUI App struct linking the shared KMP framework (Hello World verifying framework linkage)
- [x] T081 [US6] Configure SKIE Gradle plugin in the shared framework build — add `id("co.touchlab.skie")` to the appropriate build.gradle.kts, verify StateFlow→AsyncSequence generation *(moved from Phase 6 — SKIE must be configured when the shared framework is first built)*
- [x] T128 Amend constitution to v4.0.0 in `.specify/memory/constitution.md` — update Principles II, III, IV, VIII per plan.md Complexity Tracking justifications *(moved from Phase 11 — must be ratified before implementing against amended architecture)*
- [x] T135 [US2] Record Android build baseline: capture `./gradlew assembleDebug` time and APK size before any migration changes (FR-015 baseline)
- [x] T012 Verify Android app still builds and all tests pass after Gradle restructuring: `./gradlew assembleDebug test`
- [x] T141 Verify iOS project shell builds: `xcodebuild -project iosApp/iosApp.xcodeproj -scheme iosApp -sdk iphonesimulator build`

**Checkpoint**: KMP build infrastructure ready. Constitution amended. Empty shared modules compile. iOS project links shared framework and builds. Android unchanged.

---

## Phase 2: Foundational — Core Domain KMP Conversion (US6 + US2)

**Purpose**: Convert :core:domain from kotlin.jvm to kotlin-multiplatform. This is the BLOCKING prerequisite — all shared modules depend on :core:domain.

**⚠️ CRITICAL**: No shared module work can begin until this phase is complete

- [x] T013 [US6] Change `core/domain/build.gradle.kts` from `kotlin("jvm")` to `svd.kmp.library` plugin, configure commonMain dependencies (kotlinx-coroutines-core, kotlinx-serialization-json) replacing JVM-only deps
- [x] T014 [US6] Move all source files from `core/domain/src/main/kotlin/` to `core/domain/src/commonMain/kotlin/` preserving package structure
- [x] T015 [US6] Remove `javax.inject.Inject` annotations from all use case constructors in `core/domain/src/commonMain/kotlin/.../usecase/` (10 use cases). Use plain constructors — Koin will handle injection.
- [x] T016 [US6] Replace `@IoDispatcher` qualifier annotation with a Koin-compatible approach: define `IoDispatcher` as a named Koin qualifier in `core/domain/src/commonMain/kotlin/.../di/IoDispatcher.kt`
- [x] T017 [US6] Move test sources from `core/domain/src/test/` to `core/domain/src/commonTest/kotlin/` and convert JUnit5 assertions to `kotlin.test` assertions, replace MockK with manual fake implementations (commonMain-compatible — no mocking library supports all KMP targets reliably)
- [x] T018 [US2] Update all Android modules that depend on :core:domain to use the new KMP artifact — verify import paths still resolve in `:core:data`, `:feature:download`, `:feature:history`, `:feature:library`, `:core:cloud`, `:core:billing`
- [x] T019 [US2] Verify Android app builds and all tests pass after domain KMP conversion: `./gradlew assembleDebug test`

**Checkpoint**: :core:domain is KMP. All domain models, interfaces, and use cases compile for both JVM and iOS Native. Android still builds and passes tests.

---

## Phase 3: US6 — Shared Network Module

**Goal**: Port the server API client from OkHttp to Ktor in a shared KMP module, usable by both Android and iOS.

**Independent Test**: Shared unit test calling `ServerVideoExtractorApi.extractInfo()` passes on both JVM and iOS Native targets.

### Tests for Shared Network

- [x] T020 [P] [US6] Create `ServerVideoExtractorApiTest` in `shared/network/src/commonTest/kotlin/.../ServerVideoExtractorApiTest.kt` testing extractInfo mapping logic with mock HTTP responses
- [x] T021 [P] [US6] Create `ServerResponseMapperTest` in `shared/network/src/commonTest/kotlin/.../ServerResponseMapperTest.kt` testing DTO→domain model mapping

### Implementation for Shared Network

- [x] T022 [P] [US6] Move `ServerExtractRequest`, `ServerExtractResponse`, `ServerFormatDto` DTOs to `shared/network/src/commonMain/kotlin/.../dto/ServerExtractResponse.kt`
- [x] T023 [P] [US6] Move `ServerResponseMapper` to `shared/network/src/commonMain/kotlin/.../ServerResponseMapper.kt` (pure Kotlin, no changes needed)
- [x] T024 [P] [US6] Move `VideoInfoMapper` to `shared/network/src/commonMain/kotlin/.../VideoInfoMapper.kt` if no Android dependencies, otherwise leave in core/data
- [x] T025 [US6] Create `expect object ServerConfig` in `shared/network/src/commonMain/kotlin/.../ServerConfig.kt` with baseUrl, extractApiKey, extractPath, timeouts
- [x] T026 [P] [US6] Create `actual object ServerConfig` for Android in `shared/network/src/androidMain/kotlin/.../ServerConfig.android.kt` reading BuildConfig values
- [x] T027 [P] [US6] Create `actual object ServerConfig` for iOS in `shared/network/src/iosMain/kotlin/.../ServerConfig.ios.kt` reading Info.plist or using defaults
- [x] T028 [US6] Create `expect fun createHttpClient(): HttpClient` in `shared/network/src/commonMain/kotlin/.../KtorEngineFactory.kt`
- [x] T029 [P] [US6] Create `actual fun createHttpClient()` with OkHttp engine in `shared/network/src/androidMain/kotlin/.../KtorEngineFactory.android.kt`
- [x] T030 [P] [US6] Create `actual fun createHttpClient()` with Darwin engine in `shared/network/src/iosMain/kotlin/.../KtorEngineFactory.ios.kt`
- [x] T031 [US6] Implement `ServerVideoExtractorApi` using Ktor HttpClient in `shared/network/src/commonMain/kotlin/.../ServerVideoExtractorApi.kt` — port extractInfo() from OkHttp to Ktor, implement downloadFile() with streaming response
- [x] T032 [US6] Create Koin module for shared:network in `shared/network/src/commonMain/kotlin/.../di/NetworkModule.kt` providing HttpClient and ServerVideoExtractorApi
- [x] T033 [US2] Update `FallbackVideoExtractorRepository` in `core/data/src/main/kotlin/.../remote/FallbackVideoExtractorRepository.kt` to use :shared:network's ServerVideoExtractorApi instead of the local OkHttp-based one
- [x] T034 [US2] Remove old `ServerVideoExtractorApi`, `ServerConfig`, `ServerResponseMapper`, DTOs from `core/data/src/main/kotlin/.../remote/` (replaced by shared:network)
- [x] T035 [US2] Verify Android app builds and all tests pass after shared:network integration: `./gradlew assembleDebug test`

**Checkpoint**: Server API client is shared KMP code. Android uses it via FallbackVideoExtractorRepository. iOS can use it directly.

---

## Phase 4: US6 — Shared Data Module (Database + Platform Abstractions)

**Goal**: Move Room entities/DAOs to shared KMP code. Define platform abstraction interfaces. Set up Koin DI for shared modules.

**Independent Test**: Room KMP database compiles and tests pass on both JVM and iOS Native. Platform interfaces are defined and Android implementations work.

### Tests for Shared Data

- [x] T036 [P] [US6] Create `DownloadRepositoryImplTest` in `shared/data/src/commonTest/kotlin/.../DownloadRepositoryImplTest.kt` testing CRUD operations with in-memory Room database
- [x] T037 [P] [US6] Create `DownloadMapperTest` in `shared/data/src/commonTest/kotlin/.../DownloadMapperTest.kt` testing entity↔domain model mapping

### Implementation for Shared Data — Room KMP

- [x] T038 [P] [US6] Move `DownloadEntity` to `shared/data/src/commonMain/kotlin/.../local/DownloadEntity.kt` (unchanged)
- [x] T039 [P] [US6] Move `SyncQueueEntity` to `shared/data/src/commonMain/kotlin/.../local/SyncQueueEntity.kt` (unchanged)
- [x] T040 [P] [US6] Move `DownloadDao` to `shared/data/src/commonMain/kotlin/.../local/DownloadDao.kt` — verify all methods are `suspend` or return `Flow` (Room KMP requirement)
- [x] T041 [P] [US6] Move `SyncQueueDao` to `shared/data/src/commonMain/kotlin/.../local/SyncQueueDao.kt` — verify all methods are `suspend` or return `Flow`
- [x] T042 [US6] Move `DownloadMapper` to `shared/data/src/commonMain/kotlin/.../local/DownloadMapper.kt` (pure Kotlin, no changes)
- [x] T043 [US6] Create `AppDatabase` in `shared/data/src/commonMain/kotlin/.../local/AppDatabase.kt` with `@ConstructedBy(AppDatabaseConstructor::class)` and `expect object AppDatabaseConstructor`
- [x] T044 [US6] Rewrite migrations 1→5 in `shared/data/src/commonMain/kotlin/.../local/Migrations.kt` using `SQLiteConnection.execSQL()` instead of `SupportSQLiteDatabase.execSQL()` — SQL statements remain identical
- [x] T045 [US6] Create `expect fun createDatabaseBuilder(): RoomDatabase.Builder<AppDatabase>` in `shared/data/src/commonMain/kotlin/.../platform/DatabaseFactory.kt`
- [x] T046 [P] [US6] Create Android `actual fun createDatabaseBuilder()` in `shared/data/src/androidMain/kotlin/.../platform/DatabaseFactory.android.kt` using `Room.databaseBuilder(context, ...)`
- [x] T047 [P] [US6] Create iOS `actual fun createDatabaseBuilder()` in `shared/data/src/iosMain/kotlin/.../platform/DatabaseFactory.ios.kt` using NSDocumentDirectory path

### Implementation for Shared Data — Platform Abstractions

- [x] T048 [P] [US6] Define `PlatformDownloadManager` interface in `shared/data/src/commonMain/kotlin/.../platform/PlatformDownloadManager.kt` per contract: startDownload, cancelDownload, downloadState StateFlow
- [x] T049 [P] [US6] Define `PlatformFileStorage` interface in `shared/data/src/commonMain/kotlin/.../platform/PlatformFileStorage.kt` per contract: saveToDownloads, isFileAccessible, deleteFile, getShareableUri
- [x] T050 [P] [US6] Define `PlatformClipboard` interface in `shared/data/src/commonMain/kotlin/.../platform/PlatformClipboard.kt` per contract: copyToClipboard
- [x] T051 [P] [US6] Define `PlatformStringProvider` interface and `StringKey` enum in `shared/data/src/commonMain/kotlin/.../platform/PlatformStringProvider.kt` per contract
- [x] T052 [P] [US6] Implement `AndroidDownloadManager` in `shared/data/src/androidMain/kotlin/.../platform/AndroidDownloadManager.kt` wrapping DownloadServiceStateHolder + startForegroundService
- [x] T053 [P] [US6] Implement `AndroidFileStorage` in `shared/data/src/androidMain/kotlin/.../platform/AndroidFileStorage.kt` wrapping MediaStoreRepositoryImpl + AndroidFileAccessManager
- [x] T054 [P] [US6] Implement `AndroidClipboard` in `shared/data/src/androidMain/kotlin/.../platform/AndroidClipboard.kt` using ClipboardManager
- [x] T055 [P] [US6] Implement `AndroidStringProvider` in `shared/data/src/androidMain/kotlin/.../platform/AndroidStringProvider.kt` mapping StringKey→R.string.*
- [x] T056 [US6] Move `DownloadRepositoryImpl` to `shared/data/src/commonMain/kotlin/.../repository/DownloadRepositoryImpl.kt` — adapt to use shared DAO (may need minor changes for platform-specific file paths)
- [x] T057 [US6] Create `ServerOnlyVideoExtractorRepository` in `shared/data/src/iosMain/kotlin/.../repository/ServerOnlyVideoExtractorRepository.kt` — always uses server API, no local yt-dlp fallback

### Implementation for Shared Data — Koin DI

- [x] T058 [US6] Create shared Koin module in `shared/data/src/commonMain/kotlin/.../di/SharedDataModule.kt` providing AppDatabase, DAOs, DownloadRepositoryImpl, shared use cases
- [x] T059 [P] [US6] Create Android Koin module in `shared/data/src/androidMain/kotlin/.../di/AndroidDataModule.kt` providing AndroidDownloadManager, AndroidFileStorage, AndroidClipboard, AndroidStringProvider, database builder
- [x] T060 [P] [US6] Create iOS Koin module in `shared/data/src/iosMain/kotlin/.../di/IosDataModule.kt` providing iOS platform implementations and ServerOnlyVideoExtractorRepository

### Android Integration

- [x] T061 [US2] Create Koin initialization in `app/src/main/kotlin/.../di/KoinInitializer.kt` — call `startKoin {}` with androidContext and shared + android Koin modules
- [x] T062 [US2] Create Hilt bridge module `app/src/main/kotlin/.../di/KoinBridgeModule.kt` — `@Provides` methods calling `KoinPlatform.getKoin().get<T>()` for each shared dependency consumed by Hilt-managed Android components
- [x] T063 [US2] Update `SocialVideoDownloaderApp.onCreate()` in `app/src/main/kotlin/.../SocialVideoDownloaderApp.kt` to init Koin before Hilt
- [x] T064 [US2] Update `core/data/build.gradle.kts` to depend on `:shared:data` and `:shared:network`, remove migrated Room entities/DAOs/mapper source files
- [x] T065 [US2] Verify Android app builds and all tests pass after shared:data integration: `./gradlew assembleDebug test`
- [x] T136 [US2] Room backwards-compatibility test: copy an existing Android Room v5 database file, open it with the new KMP Room builder, verify all records load without data loss or migration errors
- [x] T137 [US2] Hilt+Koin coexistence smoke test: verify `./gradlew assembleDebug` succeeds with both DI systems active, run app on emulator, confirm no runtime DI conflicts (duplicate bindings, missing providers)

**Checkpoint**: Room KMP database shared. Platform abstractions defined. Android uses shared DB via Koin bridge. iOS has ServerOnlyVideoExtractorRepository. Backwards-compat and DI coexistence verified.

---

## Phase 5: US6 — Shared Feature ViewModels

**Goal**: Extract ViewModel state machines to shared KMP code. Android ViewModels become thin delegates.

**Independent Test**: SharedDownloadViewModel state transitions pass on both JVM and iOS Native.

### Tests for Shared Feature ViewModels

- [x] T066 [P] [US6] Create `SharedDownloadViewModelTest` in `shared/feature-download/src/commonTest/kotlin/.../SharedDownloadViewModelTest.kt` testing state transitions: Idle→Extracting→FormatSelection→Downloading→Done→Error
- [x] T067 [P] [US6] Create `SharedHistoryViewModelTest` in `shared/feature-history/src/commonTest/kotlin/.../SharedHistoryViewModelTest.kt` testing search, delete, cloud backup state
- [x] T068 [P] [US6] Create `SharedLibraryViewModelTest` in `shared/feature-library/src/commonTest/kotlin/.../SharedLibraryViewModelTest.kt` testing observe+map flow

### Implementation for Shared Feature ViewModels

- [x] T069 [P] [US6] Extract `DownloadUiState`, `DownloadIntent`, `DownloadEvent` sealed interfaces to `shared/feature-download/src/commonMain/kotlin/.../DownloadUiState.kt` — replace `@StringRes` with `DownloadErrorType` enum and `StringKey`
- [x] T070 [US6] Extract `SharedDownloadViewModel` state machine to `shared/feature-download/src/commonMain/kotlin/.../SharedDownloadViewModel.kt` — move all state transition logic from DownloadViewModel, use PlatformDownloadManager/PlatformStringProvider interfaces for platform actions
- [x] T071 [P] [US6] Extract `HistoryUiState`, `HistoryEffect`, `CloudBackupState` to `shared/feature-history/src/commonMain/kotlin/.../HistoryUiState.kt` — replace `@StringRes` with `HistoryMessageType` enum
- [x] T072 [US6] Extract `SharedHistoryViewModel` to `shared/feature-history/src/commonMain/kotlin/.../SharedHistoryViewModel.kt` — move combine logic, use PlatformClipboard interface
- [x] T073 [P] [US6] Extract `LibraryUiState`, `LibraryIntent`, `LibraryEffect` to `shared/feature-library/src/commonMain/kotlin/.../LibraryUiState.kt` — replace `@StringRes` with `StringKey`
- [x] T074 [US6] Extract `SharedLibraryViewModel` to `shared/feature-library/src/commonMain/kotlin/.../SharedLibraryViewModel.kt`
- [x] T075 [US6] Create Koin modules for shared feature modules in each `shared/feature-*/src/commonMain/kotlin/.../di/` directory

### Android ViewModel Delegation

- [x] T076 [US2] Refactor `DownloadViewModel` in `feature/download/src/main/kotlin/.../ui/DownloadViewModel.kt` to delegate to SharedDownloadViewModel — keep @HiltViewModel, inject shared VM via Koin bridge, handle Android-specific concerns (SavedStateHandle, notification permissions)
- [x] T077 [US2] Refactor `HistoryViewModel` in `feature/history/src/main/kotlin/.../ui/HistoryViewModel.kt` to delegate to SharedHistoryViewModel
- [x] T078 [US2] Refactor `LibraryViewModel` in `feature/library/src/main/kotlin/.../ui/LibraryViewModel.kt` to delegate to SharedLibraryViewModel
- [x] T079 [US2] Update Koin bridge module in `app/src/main/kotlin/.../di/KoinBridgeModule.kt` to provide shared feature ViewModels
- [x] T080 [US2] Verify Android app builds and all tests pass after shared ViewModel extraction: `./gradlew assembleDebug test`

**Checkpoint**: All ViewModel state machines are shared KMP code. Android VMs are thin Hilt delegates. US6 (Shared Business Logic) is complete. US2 (Android Preservation) verified.

---

## Phase 6: US1 — iOS User Downloads a Video (Priority: P1) 🎯 MVP

**Goal**: iOS user can paste a URL, extract video info via server, select a format, download the file, and find it in Files app. Background downloads work.

**Independent Test**: On iOS Simulator, paste a YouTube URL → see formats → tap download → file appears in Documents directory.

### Implementation for iOS Download

- [x] T082 [US1] Implement iOS Koin initialization in `iosApp/iosApp/App.swift` — call `KoinKt.doInitKoin()` with iOS platform modules
- [x] T083 [US1] Create SwiftUI design system: `iosApp/iosApp/Theme/Colors.swift` (SvdBg #F6F3EC, SvdPrimary #F26B3A, SvdWarning #F2B84B, SvdAccent #1E8C7A), `Typography.swift` (SpaceGrotesk + Inter), `Shapes.swift` (card 22, control 18, pill 999)
- [x] T084 [US1] Create `ContentView.swift` in `iosApp/iosApp/ContentView.swift` with TabView (3 tabs: Download, Library, History) using warm editorial tab bar styling
- [x] T085 [US1] Implement `IosDownloadManager` in `shared/data/src/iosMain/kotlin/.../platform/IosDownloadManager.kt` — URLSessionDownloadTask with background configuration, progress tracking, file move on completion
- [x] T086 [US1] Implement `IosFileStorage` in `shared/data/src/iosMain/kotlin/.../platform/IosFileStorage.kt` — Documents/SocialVideoDownloader/ directory, FileManager operations
- [x] T087 [P] [US1] Implement `IosClipboard` in `shared/data/src/iosMain/kotlin/.../platform/IosClipboard.kt` — UIPasteboard.generalPasteboard
- [x] T088 [P] [US1] Implement `IosStringProvider` in `shared/data/src/iosMain/kotlin/.../platform/IosStringProvider.kt` — NSLocalizedString mapping from StringKey
- [x] T089 [US1] Create `DownloadView.swift` in `iosApp/iosApp/Download/DownloadView.swift` — main download screen observing SharedDownloadViewModel.uiState via SKIE AsyncSequence, renders state-appropriate subviews
- [x] T090 [P] [US1] Create `UrlInputView.swift` in `iosApp/iosApp/Download/UrlInputView.swift` — URL text field with paste button, extract action
- [x] T091 [P] [US1] Create `FormatSelectionView.swift` in `iosApp/iosApp/Download/FormatSelectionView.swift` — video info card + format chips grid + download button
- [x] T092 [P] [US1] Create `DownloadProgressView.swift` in `iosApp/iosApp/Download/DownloadProgressView.swift` — progress bar, speed/ETA text, cancel button
- [x] T093 [P] [US1] Create `DownloadCompleteView.swift` in `iosApp/iosApp/Download/DownloadCompleteView.swift` — success state with open/share actions
- [x] T094 [P] [US1] Create `DownloadErrorView.swift` in `iosApp/iosApp/Download/DownloadErrorView.swift` — error message with retry action
- [x] T095 [US1] Add download completion notification via UNUserNotificationCenter in `iosApp/iosApp/Services/NotificationService.swift`
- [x] T096 [US1] Add `Localizable.strings` in `iosApp/iosApp/` with all StringKey string values for error messages and UI text
- [x] T097 [US1] Configure `Info.plist` — set `UIFileSharingEnabled`, `LSSupportsOpeningDocumentsInPlace`, `YTDLP_SERVER_URL`, and `NSUserNotificationUsageDescription`
- [x] T098 [US1] End-to-end test: build iOS app on simulator, paste URL, extract, select format, download, verify file in Documents directory
- [x] T138 [US1] Terminated-state background download test: start a download, terminate the app (not just background), relaunch — verify the file was saved and the download record appears in history

**Checkpoint**: iOS download flow works end-to-end including background/terminated scenarios. US1 (iOS Download) is the MVP — app is functional for its core purpose.

---

## Phase 7: US3 — iOS User Views Download History (Priority: P2)

**Goal**: iOS user sees past downloads in History tab, can search and delete entries.

**Independent Test**: After downloading a video, navigate to History tab → see the download → search by title → delete it.

- [x] T099 [US3] Create `HistoryView.swift` in `iosApp/iosApp/History/HistoryView.swift` — list of history items observing SharedHistoryViewModel, search bar, empty state
- [x] T100 [P] [US3] Create `HistoryItemRow.swift` in `iosApp/iosApp/History/HistoryItemRow.swift` — thumbnail, title, platform badge, date, swipe-to-delete
- [x] T101 [P] [US3] Create `HistoryDeleteDialog.swift` in `iosApp/iosApp/History/HistoryDeleteDialog.swift` — confirmation alert for single/all delete
- [x] T102 [US3] Wire History tab in `ContentView.swift` to navigate to HistoryView with SharedHistoryViewModel
- [x] T103 [US3] Verify history persistence: download a video, force-quit app, reopen → history entry persists

**Checkpoint**: History screen complete. Past downloads visible, searchable, deletable.

---

## Phase 8: US4 — iOS User Browses Downloaded Files in Library (Priority: P2)

**Goal**: iOS user can browse downloaded files, open for playback, and share with other apps.

**Independent Test**: Download a video → Library tab shows it → tap to play → share sheet works.

- [x] T104 [US4] Create `LibraryView.swift` in `iosApp/iosApp/Library/LibraryView.swift` — grid/list of downloaded files observing SharedLibraryViewModel, empty state
- [x] T105 [P] [US4] Create `LibraryItemRow.swift` in `iosApp/iosApp/Library/LibraryItemRow.swift` — thumbnail, title, file size, platform name, date
- [x] T106 [US4] Implement file open action in LibraryView — use system video player via `UIApplication.shared.open()` or QuickLook
- [x] T107 [US4] Implement share action in LibraryView — present `UIActivityViewController` (share sheet) with file URL
- [x] T108 [US4] Wire Library tab in `ContentView.swift` to navigate to LibraryView with SharedLibraryViewModel

**Checkpoint**: Library screen complete. Files browsable, playable, shareable.

---

## Phase 9: US5 — iOS User Receives URL from Another App (Priority: P3)

**Goal**: User can share a URL from Safari to the app via iOS Share Sheet. App opens with URL pre-filled.

**Independent Test**: In Safari, share a video URL → select SocialVideoDownloader → app opens with URL in input field.

- [x] T109 [US5] Create Share Extension target in Xcode project at `iosApp/ShareExtension/` with `ShareViewController.swift` that extracts URL from `NSExtensionItem`
- [x] T110 [US5] Configure App Group shared container between main app and Share Extension for URL passing
- [x] T111 [US5] Update `App.swift` to check for shared URL on launch/resume and pass to SharedDownloadViewModel
- [x] T112 [US5] Add Share Extension entitlements and Info.plist configuration for URL content types

**Checkpoint**: Share sheet integration complete. URLs from other apps flow into the download screen.

---

## Phase 10: US7 — iOS Cloud Backup and Billing (Priority: P3)

**Goal**: iOS user can sign in, enable cloud backup, and purchase premium tiers.

**Independent Test**: Sign in on iOS → enable backup → history syncs → purchase premium tier in sandbox → capacity increases.

### iOS Cloud Infrastructure

- [x] T113 [US7] Add Firebase iOS SDK (Auth + Firestore) via Swift Package Manager to the Xcode project
- [x] T114 [US7] Create `IosCloudAuthService` in `shared/data/src/iosMain/kotlin/.../cloud/IosCloudAuthService.kt` implementing CloudAuthService — Firebase Auth with Google Sign-In + Sign in with Apple
- [x] T115 [US7] Create `IosCloudBackupRepository` in `shared/data/src/iosMain/kotlin/.../cloud/IosCloudBackupRepository.kt` implementing CloudBackupRepository — Firestore document CRUD
- [x] T116 [US7] Create `IosEncryptionService` in `shared/data/src/iosMain/kotlin/.../cloud/IosEncryptionService.kt` implementing EncryptionService — iOS Keychain + CryptoKit for zero-knowledge encryption
- [x] T117 [US7] Create `IosConnectivityObserver` in `shared/data/src/iosMain/kotlin/.../cloud/IosConnectivityObserver.kt` — NWPathMonitor for network status
- [x] T118 [US7] Create `IosSyncManager` in `shared/data/src/iosMain/kotlin/.../cloud/IosSyncManager.kt` implementing SyncManager — background sync queue processing

### iOS Billing

- [x] T119 [US7] Create `StoreKitBillingRepository` in `shared/data/src/iosMain/kotlin/.../billing/StoreKitBillingRepository.kt` implementing BillingRepository — StoreKit 2 for product listing, purchase, and entitlement verification
- [x] T120 [US7] Configure StoreKit products in App Store Connect (or StoreKit Configuration file for testing)

### iOS Cloud UI

- [x] T121 [US7] Create `CloudBackupView.swift` in `iosApp/iosApp/History/CloudBackupView.swift` — sign-in button, backup toggle, capacity indicator, restore button
- [x] T122 [US7] Create `UpgradeView.swift` in `iosApp/iosApp/History/UpgradeView.swift` — tier comparison, purchase buttons using StoreKit 2 API
- [x] T123 [US7] Integrate cloud backup UI into HistoryView — add CloudBackupSection at top of history list
- [x] T124 [US7] Update iOS Koin module in `shared/data/src/iosMain/kotlin/.../di/IosDataModule.kt` to provide cloud and billing implementations
- [x] T125 [US7] Test sign-in flow: Google Sign-In + Sign in with Apple → verify Firebase Auth creates user
- [x] T126 [US7] Test billing flow: sandbox purchase → verify entitlement → verify capacity increase

**Checkpoint**: Cloud backup and billing work on iOS. Full feature parity with Android cloud features.

---

## Phase 11: Polish & Cross-Cutting Concerns

**Purpose**: Improvements that affect multiple user stories

- [x] T127 [P] Update `specs/011-kmp-ios-migration/quickstart.md` with final build commands, actual framework paths, and verified troubleshooting steps
- [x] T129 [P] Update `CLAUDE.md` module structure section to reflect new `:shared:*` modules and KMP convention plugins
- [x] T130 [P] Add iOS-specific error handling: server unavailable indicator, retry with exponential backoff in SharedDownloadViewModel
- [x] T131 Run full Android test suite one final time: `./gradlew assembleDebug test ktlintCheck` — verify zero regressions
- [x] T132 Run shared module tests on all targets: `./gradlew allTests` — verify commonTest passes on JVM and iOS Native
- [x] T133 Build and run iOS app on physical device — verify downloads, background downloads, file access, notifications
- [x] T134 Side-by-side design comparison: iOS app screenshots vs Android app screenshots — verify visual consistency (matching hex colors, font names SpaceGrotesk/Inter, corner radii 22/18/999, spacing conventions)
- [x] T139 Measure shared code percentage: run `cloc` on `shared/` and `core/domain/src/commonMain/` vs platform-specific directories, verify ≥80% of non-UI business logic is shared (SC-004)
- [x] T140 [US2] Compare Android build time and APK size against T135 baseline — verify no significant degradation (FR-015). Flag if build time increased >20% or APK size increased >10%.

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies — can start immediately. Includes constitution amendment (T128), SKIE config (T081), build baseline (T135), and iOS build verification (T141)
- **Foundational (Phase 2)**: Depends on Setup — BLOCKS all shared module work
- **Shared Network (Phase 3)**: Depends on Phase 2 (needs KMP :core:domain)
- **Shared Data (Phase 4)**: Depends on Phase 2 (needs KMP :core:domain). Can run in parallel with Phase 3.
- **Shared Features (Phase 5)**: Depends on Phases 3 + 4 (needs shared:network and shared:data)
- **iOS Download (Phase 6)**: Depends on Phase 5 (needs shared feature ViewModels)
- **iOS History (Phase 7)**: Depends on Phase 6 (needs working iOS app shell)
- **iOS Library (Phase 8)**: Depends on Phase 6 (needs working iOS app shell). Can run in parallel with Phase 7.
- **iOS Share Sheet (Phase 9)**: Depends on Phase 6 (needs working iOS download flow)
- **iOS Cloud & Billing (Phase 10)**: Depends on Phases 7 + 8 (needs history UI for cloud section)
- **Polish (Phase 11)**: Depends on all desired phases being complete. Includes performance comparison (T140), shared code measurement (T139)
- **Polish (Phase 11)**: Depends on all desired phases being complete

### User Story Dependencies

- **US6 (Shared Business Logic, P1)**: Phases 2-5. Foundational — all other stories depend on this.
- **US2 (Android Preservation, P1)**: Verified at each phase checkpoint. No implementation of its own — it's a constraint.
- **US1 (iOS Download, P1)**: Phase 6. Depends on US6 completion. MVP milestone.
- **US3 (iOS History, P2)**: Phase 7. Depends on US1 (needs iOS app shell). Independent of US4.
- **US4 (iOS Library, P2)**: Phase 8. Depends on US1 (needs iOS app shell). Can parallelize with US3.
- **US5 (iOS Share Sheet, P3)**: Phase 9. Depends on US1. Independent of US3/US4.
- **US7 (iOS Cloud & Billing, P3)**: Phase 10. Depends on US3 (needs history UI for cloud section).

### Within Each Phase

- Tests written before implementation (tests should fail first)
- Platform abstractions defined before platform implementations
- Shared code before platform-specific code
- Core implementation before UI wiring
- Android verification at each phase boundary

### Parallel Opportunities

- **Phase 1**: T005-T010 (all module scaffolds) can run in parallel
- **Phase 3 + Phase 4**: Can overlap — shared:network and shared:data have no dependency on each other
- **Phase 4**: T038-T041 (entity/DAO moves), T048-T051 (interface definitions), T052-T055 (Android implementations) each have internal parallelism
- **Phase 5**: T066-T068 (shared VM tests) in parallel, T069+T071+T073 (state extraction) in parallel
- **Phase 6**: T087-T088 (clipboard/string), T090-T094 (SwiftUI subviews) in parallel
- **Phase 7 + Phase 8**: iOS History and Library can run in parallel
- **Phase 9**: Independent of Phases 7+8, can run after Phase 6

---

## Parallel Example: Phase 4 (Shared Data)

```
# Launch all entity moves in parallel:
T038: Move DownloadEntity to shared/data/src/commonMain/
T039: Move SyncQueueEntity to shared/data/src/commonMain/
T040: Move DownloadDao to shared/data/src/commonMain/
T041: Move SyncQueueDao to shared/data/src/commonMain/

# After entities moved, launch platform abstractions in parallel:
T048: Define PlatformDownloadManager interface
T049: Define PlatformFileStorage interface
T050: Define PlatformClipboard interface
T051: Define PlatformStringProvider interface

# After interfaces defined, launch Android implementations in parallel:
T052: AndroidDownloadManager
T053: AndroidFileStorage
T054: AndroidClipboard
T055: AndroidStringProvider
```

## Parallel Example: Phase 6 (iOS Download)

```
# After design system + shell (T083-T084), launch all SwiftUI subviews in parallel:
T090: UrlInputView.swift
T091: FormatSelectionView.swift
T092: DownloadProgressView.swift
T093: DownloadCompleteView.swift
T094: DownloadErrorView.swift
```

---

## Implementation Strategy

### MVP First (US1 Only)

1. Complete Phases 1-5: Setup + Shared Architecture (US6 + US2)
2. Complete Phase 6: iOS Download (US1)
3. **STOP and VALIDATE**: iOS app downloads videos end-to-end
4. Ship TestFlight build for personal use

### Incremental Delivery

1. Phases 1-5 → Shared architecture complete, Android verified
2. Phase 6 → iOS MVP (download only) → TestFlight
3. Phases 7+8 → History + Library → TestFlight
4. Phase 9 → Share Sheet → TestFlight
5. Phase 10 → Cloud + Billing → App Store submission
6. Phase 11 → Polish → Final release

### Single Developer Strategy (recommended for this project)

1. Sequential phases 1→5 (shared architecture — tightest dependency chain)
2. Phase 6 (iOS MVP — immediate value)
3. Phases 7+8 interleaved (History/Library screens are small, similar work)
4. Phase 9 (Share Sheet — small scope)
5. Phase 10 (Cloud + Billing — can defer to post-launch)
6. Phase 11 (Polish — ongoing)

---

## Notes

- [P] tasks = different files, no dependencies
- [Story] label maps task to specific user story for traceability
- US6 and US2 are foundational — they don't have standalone UI but underpin everything
- Android verification (US2) happens at every phase checkpoint, not as a separate phase
- Constitution amendment (T128) moved to Phase 1 — must be ratified before implementing against amended architecture
- SKIE (T081) moved to Phase 1 — must be configured when the shared framework is first built
- Commit after each task or logical group
- Stop at any checkpoint to validate story independently
