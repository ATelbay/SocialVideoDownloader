# Tasks: Project Foundation

**Input**: Design documents from `/specs/001-project-foundation/`
**Prerequisites**: plan.md, spec.md, research.md, data-model.md

**Tests**: No test tasks included — spec does not request tests for the foundation feature. Test infrastructure is configured for future features.

**Organization**: Tasks are grouped by user story to enable independent implementation and testing of each story.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3, US4)
- Include exact file paths in descriptions

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Version catalog, settings, root build config, and build-logic convention plugin infrastructure

- [ ] T001 Update version catalog with all dependencies in `gradle/libs.versions.toml`
- [ ] T002 Update root `build.gradle.kts` with all plugin declarations (apply false)
- [ ] T003 Update `settings.gradle.kts` to include all 6 modules and build-logic composite build
- [ ] T004 Create `build-logic/settings.gradle.kts` with version catalog reference
- [ ] T005 Create `build-logic/convention/build.gradle.kts` with plugin registrations and dependencies

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Convention plugins and all module build files — MUST be complete before ANY user story can be implemented

**CRITICAL**: No user story work can begin until this phase is complete

- [ ] T006 [P] Create `AndroidApplicationConventionPlugin.kt` in `build-logic/convention/src/main/kotlin/` (compileSdk 36, minSdk 26, targetSdk 36, Java 17, Kotlin Android plugin)
- [ ] T007 [P] Create `AndroidLibraryConventionPlugin.kt` in `build-logic/convention/src/main/kotlin/` (compileSdk 36, minSdk 26, Java 17, Kotlin Android plugin)
- [ ] T008 [P] Create `AndroidComposeConventionPlugin.kt` in `build-logic/convention/src/main/kotlin/` (compose compiler plugin, buildFeatures.compose = true)
- [ ] T009 [P] Create `AndroidHiltConventionPlugin.kt` in `build-logic/convention/src/main/kotlin/` (hilt plugin, KSP, hilt-android + hilt-compiler dependencies)
- [ ] T010 [P] Create `AndroidFeatureConventionPlugin.kt` in `build-logic/convention/src/main/kotlin/` (composes library + compose + hilt, adds :core:ui and :core:domain dependencies)
- [ ] T011 [P] Create `AndroidRoomConventionPlugin.kt` in `build-logic/convention/src/main/kotlin/` (room plugin, KSP, room-runtime + room-ktx + room-compiler, schema directory)
- [ ] T012 Create `app/build.gradle.kts` using `videograb.android.application` + compose + hilt conventions, add all module dependencies, set namespace `com.videograb` and applicationId
- [ ] T013 [P] Create `feature/download/build.gradle.kts` using `videograb.android.feature` convention, set namespace `com.videograb.feature.download`
- [ ] T014 [P] Create `feature/history/build.gradle.kts` using `videograb.android.feature` convention, set namespace `com.videograb.feature.history`
- [ ] T015 [P] Create `core/domain/build.gradle.kts` as pure Kotlin JVM module (kotlin("jvm") plugin, no Android), set up with kotlin-stdlib only
- [ ] T016 [P] Create `core/data/build.gradle.kts` using `videograb.android.library` + hilt + room conventions, add :core:domain dependency and youtubedl-android dependencies, set namespace `com.videograb.core.data`
- [ ] T017 [P] Create `core/ui/build.gradle.kts` using `videograb.android.library` + compose conventions, add Material 3 and Coil dependencies, set namespace `com.videograb.core.ui`
- [ ] T018 Update `gradle.properties` to enable KSP, Compose, and any needed Gradle flags
- [ ] T019 Create minimal `AndroidManifest.xml` for each Android module that needs one (app, feature/download, feature/history, core/data, core/ui) — app manifest needs INTERNET permission

**Checkpoint**: All 6 modules compile with `./gradlew assembleDebug` (may have empty source sets, that's OK). Convention plugins resolve correctly.

---

## Phase 3: User Story 1 - Build and Launch the App (Priority: P1) MVP

**Goal**: The app builds across all 6 modules, launches on a device, displays a themed screen, and initializes yt-dlp/FFmpeg/aria2c without crashing.

**Independent Test**: Run `./gradlew assembleDebug`, install on API 26+ device/emulator, verify app opens to themed main screen and logcat shows successful library initialization.

### Implementation for User Story 1

- [ ] T020 [P] [US1] Create `Color.kt` with static fallback color scheme (light + dark) in `core/ui/src/main/kotlin/com/videograb/core/ui/theme/Color.kt`
- [ ] T021 [P] [US1] Create `Type.kt` with Material 3 typography definition in `core/ui/src/main/kotlin/com/videograb/core/ui/theme/Type.kt`
- [ ] T022 [US1] Create `Theme.kt` with `VideoGrabTheme` composable (Dynamic Color on API 31+, static fallback otherwise) in `core/ui/src/main/kotlin/com/videograb/core/ui/theme/Theme.kt`
- [ ] T023 [US1] Create `VideoGrabApp.kt` with `@HiltAndroidApp` annotation and async yt-dlp/FFmpeg/aria2c initialization (injected @IoDispatcher + SupervisorJob, individual try/catch per library — do NOT hardcode Dispatchers.IO per constitution IV) in `app/src/main/kotlin/com/videograb/VideoGrabApp.kt`
- [ ] T024 [US1] Create `MainActivity.kt` as `@AndroidEntryPoint` single Activity with `VideoGrabTheme` wrapping a placeholder `Scaffold` in `app/src/main/kotlin/com/videograb/MainActivity.kt`
- [ ] T025 [P] [US1] Create placeholder `DownloadScreen.kt` composable in `feature/download/src/main/kotlin/com/videograb/feature/download/ui/DownloadScreen.kt`
- [ ] T026 [P] [US1] Create placeholder `HistoryScreen.kt` composable in `feature/history/src/main/kotlin/com/videograb/feature/history/ui/HistoryScreen.kt`
- [ ] T027 [US1] Update `app/src/main/AndroidManifest.xml` to register `VideoGrabApp` as application class and `MainActivity` as launcher activity (no XML layouts — use Compose `setContent`)

**Checkpoint**: App builds and launches. Main screen shows themed Scaffold. Logcat confirms yt-dlp/FFmpeg/aria2c initialization. Dynamic Color works on API 31+ devices.

---

## Phase 4: User Story 2 - Navigate Between Screens (Priority: P2)

**Goal**: Compose Navigation is set up with type-safe routes. User can move between Download and History screens via bottom navigation.

**Independent Test**: Launch app, tap between Download and History in bottom navigation bar. Each screen displays its placeholder content.

### Implementation for User Story 2

- [ ] T028 [P] [US2] Create `DownloadNavigation.kt` with `@Serializable object DownloadRoute` and `NavGraphBuilder.downloadScreen()` extension in `feature/download/src/main/kotlin/com/videograb/feature/download/navigation/DownloadNavigation.kt`
- [ ] T029 [P] [US2] Create `HistoryNavigation.kt` with `@Serializable object HistoryRoute` and `NavGraphBuilder.historyScreen()` extension in `feature/history/src/main/kotlin/com/videograb/feature/history/navigation/HistoryNavigation.kt`
- [ ] T030 [US2] Create `AppNavHost.kt` composable that composes `downloadScreen()` and `historyScreen()` into a single NavHost with `DownloadRoute` as start destination in `app/src/main/kotlin/com/videograb/navigation/AppNavHost.kt`
- [ ] T031 [US2] Update `MainActivity.kt` to replace placeholder Scaffold with bottom navigation bar (Download + History items) and `AppNavHost` content area in `app/src/main/kotlin/com/videograb/MainActivity.kt`

**Checkpoint**: App has bottom navigation. Tapping Download/History switches between screens. Back navigation works correctly.

---

## Phase 5: User Story 3 - Verify Dependency Injection Works (Priority: P2)

**Goal**: Hilt DI graph is fully wired — repository interface in :core:domain, implementation in :core:data, Hilt module binds them, and the graph compiles and resolves at runtime.

**Independent Test**: Build project with no Hilt errors. App launches and the DI-provided repository is resolvable. Adding a new `@Inject` constructor in a feature module compiles without modifying :app.

### Implementation for User Story 3

- [ ] T032 [P] [US3] Create `DownloadRecord.kt` domain model (data class with id, sourceUrl, videoTitle, thumbnailUrl, filePath, status, createdAt, completedAt, fileSizeBytes) in `core/domain/src/main/kotlin/com/videograb/core/domain/model/DownloadRecord.kt`
- [ ] T033 [P] [US3] Create `DownloadStatus.kt` enum (PENDING, DOWNLOADING, COMPLETED, FAILED) in `core/domain/src/main/kotlin/com/videograb/core/domain/model/DownloadStatus.kt`
- [ ] T034 [US3] Create `DownloadRepository.kt` interface with suspend functions (insert, getAll, getById, updateStatus, delete) in `core/domain/src/main/kotlin/com/videograb/core/domain/repository/DownloadRepository.kt`
- [ ] T035 [US3] Create `DownloadRepositoryImpl.kt` implementing `DownloadRepository` with `@Inject constructor` (stub implementation returning empty/no-op for now) in `core/data/src/main/kotlin/com/videograb/core/data/repository/DownloadRepositoryImpl.kt`
- [ ] T036 [US3] Create `RepositoryModule.kt` Hilt module that `@Binds` `DownloadRepository` to `DownloadRepositoryImpl` in `core/data/src/main/kotlin/com/videograb/core/data/di/RepositoryModule.kt`

**Checkpoint**: Project compiles with full Hilt graph. No runtime DI errors on app launch.

---

## Phase 6: User Story 4 - Room Database Schema Present (Priority: P3)

**Goal**: Room database skeleton is in place with DownloadEntity, DownloadDao, and AppDatabase compiling via KSP. Database can be instantiated at runtime.

**Independent Test**: Build project — Room KSP generates database implementation. Launch app and verify database creation in device file explorer or logcat.

### Implementation for User Story 4

- [ ] T037 [P] [US4] Create `DownloadEntity.kt` Room entity with fields matching data-model.md (id as autoGenerate PK, sourceUrl, videoTitle, thumbnailUrl, filePath, status, createdAt, completedAt, fileSizeBytes) in `core/data/src/main/kotlin/com/videograb/core/data/local/DownloadEntity.kt`
- [ ] T038 [US4] Create `DownloadDao.kt` interface with @Insert, @Query (getAll, getById), @Update, @Delete operations in `core/data/src/main/kotlin/com/videograb/core/data/local/DownloadDao.kt`
- [ ] T039 [US4] Create `AppDatabase.kt` Room database class (version 1, entities = [DownloadEntity], exportSchema = true) in `core/data/src/main/kotlin/com/videograb/core/data/local/AppDatabase.kt`
- [ ] T040 [US4] Create `DatabaseModule.kt` Hilt module that @Provides AppDatabase (singleton) and @Provides DownloadDao in `core/data/src/main/kotlin/com/videograb/core/data/di/DatabaseModule.kt`
- [ ] T041 [US4] Update `DownloadRepositoryImpl.kt` to inject `DownloadDao` and delegate calls to it (replacing stubs from T035) in `core/data/src/main/kotlin/com/videograb/core/data/repository/DownloadRepositoryImpl.kt`
- [ ] T042 [US4] Create entity-to-domain mapper functions (DownloadEntity <-> DownloadRecord) in `core/data/src/main/kotlin/com/videograb/core/data/local/DownloadMapper.kt`

**Checkpoint**: Room generates database impl via KSP. App launches and database is created. DownloadRepository delegates to real DAO.

---

## Phase 7: Polish & Cross-Cutting Concerns

**Purpose**: Final validation and cleanup

- [ ] T043 Verify full build from clean state: `./gradlew clean assembleDebug` passes with zero errors
- [ ] T044 Verify module isolation: each feature module builds independently, :core:domain has no Android dependencies
- [ ] T045 Add `.gitignore` entries for Room schema exports (`core/data/schemas/`) and any new build artifacts
- [ ] T046 Run quickstart.md validation — follow all steps and confirm they work as documented
- [ ] T047 Configure ktlint plugin in `build-logic/convention/` and version catalog (`gradle/libs.versions.toml`), verify `./gradlew ktlintCheck` passes with zero violations

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies — can start immediately
- **Foundational (Phase 2)**: Depends on Setup completion — BLOCKS all user stories
- **US1 - Build and Launch (Phase 3)**: Depends on Foundational phase completion
- **US2 - Navigation (Phase 4)**: Depends on US1 (needs MainActivity and placeholder screens)
- **US3 - DI Verification (Phase 5)**: Depends on Foundational phase (Hilt convention plugin). Can run in parallel with US1/US2 at the model/interface level.
- **US4 - Room Database (Phase 6)**: Depends on US3 (needs repository interface and impl to wire into)
- **Polish (Phase 7)**: Depends on all user stories being complete

### User Story Dependencies

- **User Story 1 (P1)**: Can start after Foundational (Phase 2) — No dependencies on other stories
- **User Story 2 (P2)**: Depends on US1 (needs MainActivity and placeholder screens to add navigation to)
- **User Story 3 (P2)**: Can start after Foundational (Phase 2) — Domain models and DI are independent of UI
- **User Story 4 (P3)**: Depends on US3 (needs repository interface and impl to connect Room to)

### Within Each User Story

- Parallel tasks ([P]) within same story can run simultaneously
- Models/entities before services/repositories
- Infrastructure before integration
- Story complete before moving to next priority

### Parallel Opportunities

- T006–T011 (all convention plugins) can run in parallel
- T013–T017 (feature + core module build files) can run in parallel
- T020–T021 (Color.kt + Type.kt) can run in parallel
- T025–T026 (placeholder screens) can run in parallel
- T028–T029 (navigation files) can run in parallel
- T032–T033 (domain models) can run in parallel

---

## Parallel Example: Phase 2 (Foundational)

```
# Launch all convention plugins together:
Task: T006 "AndroidApplicationConventionPlugin.kt"
Task: T007 "AndroidLibraryConventionPlugin.kt"
Task: T008 "AndroidComposeConventionPlugin.kt"
Task: T009 "AndroidHiltConventionPlugin.kt"
Task: T010 "AndroidFeatureConventionPlugin.kt"
Task: T011 "AndroidRoomConventionPlugin.kt"

# Then launch all module build files together:
Task: T013 "feature/download/build.gradle.kts"
Task: T014 "feature/history/build.gradle.kts"
Task: T015 "core/domain/build.gradle.kts"
Task: T016 "core/data/build.gradle.kts"
Task: T017 "core/ui/build.gradle.kts"
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete Phase 1: Setup (T001–T005)
2. Complete Phase 2: Foundational (T006–T019) — CRITICAL
3. Complete Phase 3: User Story 1 (T020–T027)
4. **STOP and VALIDATE**: `./gradlew assembleDebug` + launch on device
5. App launches with themed screen and initialized libraries

### Incremental Delivery

1. Setup + Foundational → All modules compile
2. Add US1 → App builds and launches (MVP!)
3. Add US2 → Navigation between screens works
4. Add US3 → DI graph fully wired
5. Add US4 → Room database ready for feature development
6. Polish → Clean, validated foundation

---

## Notes

- [P] tasks = different files, no dependencies
- [Story] label maps task to specific user story for traceability
- Each user story should be independently completable and testable
- Commit after each task: `feat: T0XX description`
- Stop at any checkpoint to validate story independently
- `:core:domain` is a pure Kotlin/JVM module — no Android dependencies allowed
- All annotation processing uses KSP — never kapt
