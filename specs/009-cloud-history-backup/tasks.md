# Tasks: Cloud History Backup

**Input**: Design documents from `/specs/009-cloud-history-backup/`
**Prerequisites**: plan.md (required), spec.md (required for user stories), research.md, data-model.md, contracts/

**Tests**: Include unit-test tasks for use cases and ViewModels (Constitution Principle VI: Test Discipline).

**Organization**: Tasks are grouped by user story to enable independent implementation and testing of each story.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3, US4)
- Include exact file paths in descriptions

## Path Conventions

- `app/src/main/...` for app entry points, navigation, and manifest changes
- `feature/<name>/src/main/...` and `feature/<name>/src/test/...` for feature UI, ViewModels, and feature-specific services
- `core/domain/src/main/...` and `core/domain/src/test/...` for domain models, repository interfaces, and use cases
- `core/data/src/main/...` and `core/data/src/test/...` for Room, MediaStore, repository implementations, and mappers
- `core/cloud/src/main/...` and `core/cloud/src/test/...` for Firebase, encryption, and sync implementations
- `core/billing/src/main/...` and `core/billing/src/test/...` for Google Play Billing
- `core/ui/src/main/...` for shared Compose UI and theme primitives
- `src/main/res/values/strings.xml` in the touched module for user-facing strings

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Create new modules, add dependencies, configure Firebase project

- [ ] T001 Add Firebase BOM, Auth, Firestore, Play Billing, and google-services plugin entries to `gradle/libs.versions.toml`
- [ ] T002 Create `:core:cloud` module with `build.gradle.kts` (android-library, hilt, dependencies on `:core:domain`, `:core:data`, firebase-auth, firebase-firestore) at `core/cloud/build.gradle.kts`
- [ ] T003 [P] Create `:core:billing` module with `build.gradle.kts` (android-library, hilt, dependency on `:core:domain`, play-billing) at `core/billing/build.gradle.kts`
- [ ] T004 Register `:core:cloud` and `:core:billing` in `settings.gradle.kts`
- [ ] T005 Apply google-services plugin in `app/build.gradle.kts`, add dependencies on `:core:cloud` and `:core:billing`
- [ ] T006 [P] Add `:core:cloud` dependency to `feature/history/build.gradle.kts`
- [ ] T007 Place `google-services.json` in `app/` directory (from Firebase Console) and add Firebase project setup with Anonymous Auth enabled and Firestore database created
- [ ] T008 Deploy Firestore security rules from `specs/009-cloud-history-backup/contracts/cloud-sync-interfaces.md` to Firebase project
- [ ] T009 Disable Firebase auto-init via manifest metadata in `app/src/main/AndroidManifest.xml` — add `FirebaseInitProvider` removal with `tools:node="remove"` to prevent cold-start network calls when backup is disabled

**Checkpoint**: Project compiles with new modules. `./gradlew assembleDebug` passes. No runtime behavior changes yet.

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Domain models, interfaces, Room migration, and core service implementations shared across all user stories

**⚠️ CRITICAL**: No user story work can begin until this phase is complete

- [ ] T010 [P] Define `SyncStatus` sealed interface and `CloudTier` enum in `core/domain/src/main/kotlin/com/socialvideodownloader/core/domain/model/SyncStatus.kt` and `core/domain/src/main/kotlin/com/socialvideodownloader/core/domain/model/CloudTier.kt`
- [ ] T011 [P] Define `CloudBackupRepository` interface in `core/domain/src/main/kotlin/com/socialvideodownloader/core/domain/repository/CloudBackupRepository.kt`
- [ ] T012 [P] Define `BillingRepository` interface with `BillingResult` sealed interface in `core/domain/src/main/kotlin/com/socialvideodownloader/core/domain/repository/BillingRepository.kt`
- [ ] T013 [P] Define `CloudAuthService` interface in `core/domain/src/main/kotlin/com/socialvideodownloader/core/domain/sync/CloudAuthService.kt`
- [ ] T014 [P] Define `EncryptionService` interface in `core/domain/src/main/kotlin/com/socialvideodownloader/core/domain/sync/EncryptionService.kt`
- [ ] T015 [P] Define `SyncManager` interface in `core/domain/src/main/kotlin/com/socialvideodownloader/core/domain/sync/SyncManager.kt`
- [ ] T015b [P] Define `BackupPreferences` interface (isBackupEnabled, lastSyncTimestamp, hasEverEnabled flows + setters) in `core/domain/src/main/kotlin/com/socialvideodownloader/core/domain/sync/BackupPreferences.kt` — use cases depend on this abstraction, impl lives in `:core:cloud`
- [ ] T016 Add `syncStatus` column to `DownloadEntity` with default `"NOT_SYNCED"` in `core/data/src/main/kotlin/com/socialvideodownloader/core/data/local/DownloadEntity.kt` and update `DownloadMapper.kt` to map syncStatus
- [ ] T017 Create `SyncQueueEntity` and `SyncQueueDao` in `core/data/src/main/kotlin/com/socialvideodownloader/core/data/local/SyncQueueEntity.kt` and `core/data/src/main/kotlin/com/socialvideodownloader/core/data/local/SyncQueueDao.kt`
- [ ] T018 Write Room migration (add `syncStatus` column to `downloads`, create `sync_queue` table with unique index) and register in `AppDatabase` in `core/data/src/main/kotlin/com/socialvideodownloader/core/data/local/AppDatabase.kt`
- [ ] T019 Implement `KeystoreEncryptionService` (AES-256-GCM encrypt/decrypt, key generation, invalidation detection) in `core/cloud/src/main/kotlin/com/socialvideodownloader/core/cloud/encryption/KeystoreEncryptionService.kt`
- [ ] T020 [P] Implement `FirebaseCloudAuthService` (signInAnonymously, getCurrentUid, isAuthenticated with lazy FirebaseApp init) in `core/cloud/src/main/kotlin/com/socialvideodownloader/core/cloud/auth/FirebaseCloudAuthService.kt`
- [ ] T021 Implement `CloudBackupPreferences` DataStore wrapper (implements `BackupPreferences` from `:core:domain`; isBackupEnabled, lastSyncTimestamp, hasEverEnabled) in `core/cloud/src/main/kotlin/com/socialvideodownloader/core/cloud/preferences/CloudBackupPreferences.kt`
- [ ] T022 Implement `FirestoreCloudBackupRepository` (uploadRecord, deleteRecord, fetchAllRecords, getCloudRecordCount, getTierLimit, updateTierLimit, evictOldestRecords) in `core/cloud/src/main/kotlin/com/socialvideodownloader/core/cloud/repository/FirestoreCloudBackupRepository.kt`
- [ ] T023 Create `CloudModule` Hilt module binding all cloud interfaces to implementations in `core/cloud/src/main/kotlin/com/socialvideodownloader/core/cloud/di/CloudModule.kt`
- [ ] T024 Add cloud backup string resources (toggle label, sync status messages, error messages, capacity banner text) to `feature/history/src/main/res/values/strings.xml`

**Checkpoint**: Foundation ready — all interfaces defined, Room migration works, encryption and auth services implemented. User story implementation can now begin.

---

## Phase 3: User Story 1 — Enable Cloud Backup and Sync History (Priority: P1) 🎯 MVP

**Goal**: User can toggle cloud backup on/off, records auto-sync to cloud, sync status indicator visible

**Independent Test**: Toggle cloud backup on, complete a download, verify encrypted record appears in Firestore. Toggle off, verify no further uploads.

### Tests for User Story 1 ⚠️

> **NOTE: Write these tests FIRST, ensure they FAIL before implementation**

- [ ] T025 [P] [US1] Add `EnableCloudBackupUseCaseTest` in `core/domain/src/test/kotlin/com/socialvideodownloader/core/domain/sync/EnableCloudBackupUseCaseTest.kt` — test auth trigger, preferences update, initial sync kick-off
- [ ] T026 [P] [US1] Add `DisableCloudBackupUseCaseTest` in `core/domain/src/test/kotlin/com/socialvideodownloader/core/domain/sync/DisableCloudBackupUseCaseTest.kt` — test preferences update, no cloud deletion
- [ ] T027 [P] [US1] Add `FirestoreSyncManagerTest` in `core/cloud/src/test/kotlin/com/socialvideodownloader/core/cloud/sync/FirestoreSyncManagerTest.kt` — test pending operation processing, upload queueing, sync status emission
- [ ] T028 [P] [US1] Add HistoryViewModel cloud backup state tests in `feature/history/src/test/kotlin/com/socialvideodownloader/feature/history/ui/HistoryViewModelCloudTest.kt` — test toggle intent, sync status state, disabled-by-default

### Implementation for User Story 1

- [ ] T029 [P] [US1] Implement `EnableCloudBackupUseCase` (authenticate, set preferences, trigger initial sync) in `core/domain/src/main/kotlin/com/socialvideodownloader/core/domain/sync/EnableCloudBackupUseCase.kt`
- [ ] T030 [P] [US1] Implement `DisableCloudBackupUseCase` (update preferences, stop sync, retain cloud records) in `core/domain/src/main/kotlin/com/socialvideodownloader/core/domain/sync/DisableCloudBackupUseCase.kt`
- [ ] T031 [US1] Implement `FirestoreSyncManager` (processPendingOperations, syncNewRecord, queueDeletion, observeSyncStatus, LRU eviction check) in `core/cloud/src/main/kotlin/com/socialvideodownloader/core/cloud/sync/FirestoreSyncManager.kt`
- [ ] T032 [US1] Hook sync into download completion — after `DownloadRepositoryImpl.insert()` or `update()` with COMPLETED status, queue an UPLOAD operation in sync_queue if backup is enabled. Modify `core/data/src/main/kotlin/com/socialvideodownloader/core/data/repository/DownloadRepositoryImpl.kt`
- [ ] T033 [US1] Hook delete propagation — when `DownloadRepositoryImpl.delete()` is called and record syncStatus is SYNCED, queue a DELETE operation in sync_queue. Modify `core/data/src/main/kotlin/com/socialvideodownloader/core/data/repository/DownloadRepositoryImpl.kt`
- [ ] T034 [US1] Add cloud backup intents (`ToggleCloudBackup`) and sync status state to `HistoryUiState` and `HistoryIntent` in `feature/history/src/main/kotlin/com/socialvideodownloader/feature/history/ui/HistoryViewModel.kt`
- [ ] T035 [US1] Add `CloudBackupToggle` composable (toggle switch + sync status indicator) in `feature/history/src/main/kotlin/com/socialvideodownloader/feature/history/ui/CloudBackupToggle.kt`
- [ ] T036 [US1] Integrate `CloudBackupToggle` into the history screen layout in `feature/history/src/main/kotlin/com/socialvideodownloader/feature/history/ui/HistoryScreen.kt`
- [ ] T037 [US1] Add connectivity observer to trigger sync when network becomes available — use `ConnectivityManager.NetworkCallback` in `core/cloud/src/main/kotlin/com/socialvideodownloader/core/cloud/sync/ConnectivityObserver.kt`

**Checkpoint**: User Story 1 is fully functional. Cloud backup can be toggled on/off, records sync automatically, sync status is visible. Core download flow is unaffected.

---

## Phase 4: User Story 4 — Graceful Degradation Under Failure Conditions (Priority: P2)

**Goal**: All cloud failures handled silently. Core download flow never blocked by cloud issues.

**Independent Test**: Enable backup, simulate airplane mode / Firestore quota exceeded / expired auth token — verify downloads continue normally, sync indicator shows "Backup paused".

### Tests for User Story 4 ⚠️

- [ ] T038 [P] [US4] Add sync retry and error handling tests in `core/cloud/src/test/kotlin/com/socialvideodownloader/core/cloud/sync/FirestoreSyncManagerErrorTest.kt` — test offline deferral, auth re-auth, exponential backoff, max retry cap
- [ ] T039 [P] [US4] Add `KeystoreEncryptionServiceTest` in `core/cloud/src/test/kotlin/com/socialvideodownloader/core/cloud/encryption/KeystoreEncryptionServiceTest.kt` — test key invalidation detection, key regeneration flow

### Implementation for User Story 4

- [ ] T040 [US4] Add exponential backoff retry logic with max 5 retries to `FirestoreSyncManager.processPendingOperations()` in `core/cloud/src/main/kotlin/com/socialvideodownloader/core/cloud/sync/FirestoreSyncManager.kt`
- [ ] T041 [US4] Add silent re-authentication on `FirebaseAuthException` in `FirestoreSyncManager` — catch auth errors, call `authService.signInAnonymously()`, retry the operation
- [ ] T042 [US4] Add `KeyPermanentlyInvalidatedException` handling in `KeystoreEncryptionService` — detect invalidation, regenerate key, emit `SyncStatus.Error` with user-facing message about old records being unrecoverable
- [ ] T043 [US4] Add "Backup paused" indicator state to `HistoryViewModel` — map `SyncStatus.Paused` and `SyncStatus.Error` to a subtle UI indicator (not an error dialog) in `feature/history/src/main/kotlin/com/socialvideodownloader/feature/history/ui/HistoryViewModel.kt`
- [ ] T044 [US4] Ensure all cloud operations in `FirestoreSyncManager` are wrapped in try-catch that never propagates exceptions to callers — cloud failures MUST NOT crash the app or block the download flow

**Checkpoint**: All failure scenarios from US4 acceptance criteria handled. Downloads work identically with or without cloud backup, regardless of network state.

---

## Phase 5: User Story 2 — Restore History from Cloud (Priority: P2)

**Goal**: User can manually restore encrypted cloud records into local database with dedup

**Independent Test**: Back up history, clear app data, re-enable backup, tap "Restore from Cloud", verify records appear locally.

### Tests for User Story 2 ⚠️

- [ ] T045 [P] [US2] Add `RestoreFromCloudUseCaseTest` in `core/domain/src/test/kotlin/com/socialvideodownloader/core/domain/sync/RestoreFromCloudUseCaseTest.kt` — test merge logic, dedup (same sourceUrl + createdAt skipped), key-lost error handling
- [ ] T046 [P] [US2] Add HistoryViewModel restore state tests in `feature/history/src/test/kotlin/com/socialvideodownloader/feature/history/ui/HistoryViewModelRestoreTest.kt` — test restore intent, progress state, completion, error states

### Implementation for User Story 2

- [ ] T047 [US2] Implement `RestoreFromCloudUseCase` (fetch all cloud records via CloudBackupRepository, decrypt, dedup against local DB by sourceUrl+createdAt, insert non-duplicates) in `core/domain/src/main/kotlin/com/socialvideodownloader/core/domain/sync/RestoreFromCloudUseCase.kt`
- [ ] T048 [US2] Add `RestoreFromCloud` intent, restore progress state, and restore error state to `HistoryViewModel` in `feature/history/src/main/kotlin/com/socialvideodownloader/feature/history/ui/HistoryViewModel.kt`
- [ ] T049 [US2] Create `RestoreDialog` composable (progress indicator with "X of Y records restored", completion state, key-lost error message) in `feature/history/src/main/kotlin/com/socialvideodownloader/feature/history/ui/RestoreDialog.kt`
- [ ] T050 [US2] Add "Restore from Cloud" action button (visible only when backup is enabled) to history screen in `feature/history/src/main/kotlin/com/socialvideodownloader/feature/history/ui/HistoryScreen.kt`
- [ ] T051 [US2] Add restore-specific string resources (progress text, completion message, key-lost error) to `feature/history/src/main/res/values/strings.xml`

**Checkpoint**: Restore flow works end-to-end. Duplicates skipped. Key-loss error handled gracefully.

---

## Phase 6: User Story 3 — Purchase Additional Cloud Capacity (Priority: P3)

**Goal**: Capacity banner at 90%+ usage, one-time purchase unlocks 10,000 records via Google Play Billing

**Independent Test**: Fill cloud store near 1,000 records, verify banner appears, complete test purchase, verify new limit applied.

### Tests for User Story 3 ⚠️

- [ ] T052 [P] [US3] Add `ObserveCloudCapacityUseCaseTest` in `core/domain/src/test/kotlin/com/socialvideodownloader/core/domain/sync/ObserveCloudCapacityUseCaseTest.kt` — test capacity calculation, 90% threshold, tier changes
- [ ] T053 [P] [US3] Add `PlayBillingRepositoryTest` in `core/billing/src/test/kotlin/com/socialvideodownloader/core/billing/PlayBillingRepositoryTest.kt` — test purchase flow, restoration, refund detection

### Implementation for User Story 3

- [ ] T054 [US3] Implement `PlayBillingRepository` (BillingClient setup, queryProductDetails, launchBillingFlow, acknowledgePurchase, queryPurchasesAsync for restore, observeTier) in `core/billing/src/main/kotlin/com/socialvideodownloader/core/billing/PlayBillingRepository.kt`
- [ ] T055 [US3] Create `BillingModule` Hilt module binding `BillingRepository` in `core/billing/src/main/kotlin/com/socialvideodownloader/core/billing/di/BillingModule.kt`
- [ ] T056 [US3] Implement `ObserveCloudCapacityUseCase` (combine cloud record count + tier limit, emit capacity info) in `core/domain/src/main/kotlin/com/socialvideodownloader/core/domain/sync/ObserveCloudCapacityUseCase.kt`
- [ ] T057 [US3] Add capacity and upgrade intents/state to `HistoryViewModel` — `CloudCapacity(used: Int, limit: Int)` state, `TapUpgrade` intent in `feature/history/src/main/kotlin/com/socialvideodownloader/feature/history/ui/HistoryViewModel.kt`
- [ ] T058 [US3] Create `CapacityBanner` composable (non-intrusive banner showing "X of Y cloud records used" with "Upgrade" action) in `feature/history/src/main/kotlin/com/socialvideodownloader/feature/history/ui/CapacityBanner.kt`
- [ ] T059 [US3] Create `UpgradeScreen` composable (price display, 10,000-record limit, "Buy" button launching billing flow) in `feature/history/src/main/kotlin/com/socialvideodownloader/feature/history/ui/UpgradeScreen.kt`
- [ ] T060 [US3] Integrate `CapacityBanner` into history screen (show when cloud record count ≥ 90% of tier limit) in `feature/history/src/main/kotlin/com/socialvideodownloader/feature/history/ui/HistoryScreen.kt`
- [ ] T061 [US3] Update `FirestoreCloudBackupRepository.updateTierLimit()` to sync tier limit to Firestore counters document after purchase
- [ ] T062 [US3] Add billing and capacity string resources (banner text, upgrade screen copy, purchase success/error messages) to `feature/history/src/main/res/values/strings.xml`
- [ ] T063 [US3] Configure in-app product `cloud_history_10k` in Google Play Console (BILLING permission is auto-added by Play Billing library — no manifest change needed)

**Checkpoint**: Freemium model works end-to-end. Capacity banner shows at 90%+, purchase unlocks 10,000 records, purchase restores on reinstall.

---

## Phase 7: Polish & Cross-Cutting Concerns

**Purpose**: Improvements that affect multiple user stories

- [ ] T064 Add purchase restoration check on app launch in `app/src/main/kotlin/com/socialvideodownloader/app/` — call `billingRepository.restorePurchases()` and update tier limit on startup
- [ ] T065 [P] Add connectivity-triggered sync on app resume — when app returns to foreground with backup enabled, process pending operations
- [ ] T066 [P] Reconcile Firestore counter document on app launch if backup is enabled — verify `recordCount` matches actual document count
- [ ] T067 Run `./gradlew ktlintCheck` and fix any violations across all new and modified files
- [ ] T068 Run `./gradlew test` and verify all unit tests pass
- [ ] T069 Run quickstart.md validation — follow all validation steps in `specs/009-cloud-history-backup/quickstart.md`

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies — can start immediately
- **Foundational (Phase 2)**: Depends on Setup completion — BLOCKS all user stories
- **User Story 1 (Phase 3)**: Depends on Foundational (Phase 2) — No dependencies on other stories
- **User Story 4 (Phase 4)**: Depends on User Story 1 (Phase 3) — builds on sync infrastructure
- **User Story 2 (Phase 5)**: Depends on Foundational (Phase 2) + US1 toggle infrastructure (T034–T036) — restore button requires backup-enabled state
- **User Story 3 (Phase 6)**: Depends on Foundational (Phase 2) — can run in parallel with US1/US2
- **Polish (Phase 7)**: Depends on all user stories being complete

### User Story Dependencies

- **User Story 1 (P1)**: Can start after Foundational (Phase 2) — No dependencies on other stories. **This is the MVP.**
- **User Story 4 (P2)**: Depends on User Story 1 — adds error handling to sync infrastructure built in US1
- **User Story 2 (P2)**: Depends on Foundational (Phase 2) + US1 toggle UI (T034–T036) — restore button is only visible when backup is enabled. Independent of US1 sync flow otherwise.
- **User Story 3 (P3)**: Can start after Foundational (Phase 2) — uses BillingRepository and CloudBackupRepository. Independent of US1/US2.

### Within Each User Story

- Tests MUST be written and FAIL before implementation
- Domain contracts before repository implementations
- Repository and use case work before UI wiring
- Core implementation before integration
- Story complete before moving to next priority

### Parallel Opportunities

- All Setup tasks marked [P] can run in parallel
- All Foundational tasks marked [P] can run in parallel (T010–T015 domain interfaces)
- Once Foundational phase completes, US2 and US3 can start in parallel with US1
- Within each story, all test tasks marked [P] can run in parallel
- Within each story, all implementation tasks marked [P] can run in parallel

---

## Parallel Example: Foundational Phase

```bash
# Launch all domain interface definitions together:
Task: T010 "Define SyncStatus and CloudTier models"
Task: T011 "Define CloudBackupRepository interface"
Task: T012 "Define BillingRepository interface"
Task: T013 "Define CloudAuthService interface"
Task: T014 "Define EncryptionService interface"
Task: T015 "Define SyncManager interface"
```

## Parallel Example: User Story 1

```bash
# Launch all tests together:
Task: T025 "EnableCloudBackupUseCaseTest"
Task: T026 "DisableCloudBackupUseCaseTest"
Task: T027 "FirestoreSyncManagerTest"
Task: T028 "HistoryViewModel cloud backup tests"

# Then launch parallel implementation:
Task: T029 "EnableCloudBackupUseCase"
Task: T030 "DisableCloudBackupUseCase"
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete Phase 1: Setup
2. Complete Phase 2: Foundational (CRITICAL — blocks all stories)
3. Complete Phase 3: User Story 1
4. **STOP and VALIDATE**: Toggle backup on, download a video, verify encrypted record in Firestore
5. Deploy/demo if ready

### Incremental Delivery

1. Complete Setup + Foundational → Foundation ready
2. Add User Story 1 → Test independently → Deploy/Demo (MVP!)
3. Add User Story 4 → Harden error handling → Deploy/Demo
4. Add User Story 2 → Restore flow → Deploy/Demo
5. Add User Story 3 → Freemium billing → Deploy/Demo
6. Polish → Final validation → Deploy/Demo

### Recommended Execution Order (single developer)

Setup → Foundational → US1 → US4 → US2 → US3 → Polish

---

## Notes

- [P] tasks = different files, no dependencies
- [Story] label maps task to specific user story for traceability
- Each user story should be independently completable and testable
- Verify tests fail before implementing
- Commit after each task or logical group
- Stop at any checkpoint to validate story independently
- US4 (Graceful Degradation) depends on US1 because it adds error handling to the sync manager built in US1
- Avoid: vague tasks, same file conflicts, cross-story dependencies that break independence
