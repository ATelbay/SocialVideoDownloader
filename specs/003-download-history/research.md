# Research: Download History Screen

**Branch**: `003-download-history` | **Date**: 2026-03-15

## R1: Local Search and State Derivation

**Decision**: Keep title search local to `HistoryViewModel` by combining the repository flow with a `MutableStateFlow<String>` search query, and filter after records have been mapped into presentation items.

**Rationale**: `DownloadRepository.getAll()` already emits newest-first history from Room, and the feature scope is local-only. Filtering in the ViewModel avoids adding search-only DAO APIs, keeps the repository contract small, and lets query updates react immediately without round-tripping through the database on every keystroke.

**Alternatives considered**:
- Add a Room search query API for every keystroke — more surface area than the feature needs
- Filter inside the composable — harder to test and mixes presentation state with UI rendering

## R2: One-Off File and Feedback Actions

**Decision**: Use durable `HistoryUiState` for screen state and a `SharedFlow<HistoryEffect>` for one-off actions such as open-file, share-file, and snackbar/toast feedback.

**Rationale**: Open/share actions should not replay after configuration changes, and user feedback about missing files or delete failures should not live forever in persistent screen state. This keeps the MVI state machine predictable while still allowing the composable route to bridge into Android intents.

**Alternatives considered**:
- Encode open/share requests as booleans in UI state — easy to replay accidentally after recomposition or rotation
- Launch intents directly from the ViewModel with `Context` — breaks separation and complicates testing

## R3: Android File Handling Boundary

**Decision**: Add a feature-local `HistoryFileManager` abstraction, provided through a feature-local Hilt module, to resolve an actionable content URI from persisted history data, check file availability, and perform optional delete-file work on an injected I/O dispatcher. The Android implementation prefers a persisted MediaStore URI and falls back to a FileProvider URI derived from `filePath` only for legacy records.

**Rationale**: The repository should stay responsible for Room data, not filesystem concerns. A small file-manager boundary keeps Android-specific I/O injectable and testable, and it allows the history feature to work with the project's MediaStore direction while still tolerating older path-based rows during migration.

**Alternatives considered**:
- Extend `DownloadRepository` to manage filesystem state — mixes data persistence with file platform concerns
- Put file existence and delete logic directly in the ViewModel — harder to unit test and violates separation

## R4: Schema and Repository Contract Alignment

**Decision**: Extend `DownloadRecord` and `DownloadEntity` with nullable `formatLabel` and `mediaStoreUri` fields, bump the Room database from version 1 to version 2 with an additive migration, and add `deleteAll()` to the DAO and repository.

**Rationale**: The approved spec requires every row to display a format label and the clarified file-action design requires a persisted MediaStore-backed handle for open/share/delete. Making both new columns nullable preserves existing rows and allows a low-risk additive migration. A dedicated `deleteAll()` operation avoids repeatedly emitting partial list states while deleting an entire history dataset one row at a time.

**Alternatives considered**:
- Synthesize format text from file names — unreliable and not part of the data contract
- Loop `delete(record)` from the ViewModel for Delete All — more churn, more failure points, and poorer UX
- Destructive migration — loses user history for a non-breaking additive change

## R5: Delete Confirmation UX

**Decision**: Model single-item and bulk deletion with the same confirmation dialog state, using a `deleteFilesSelected` toggle that is shown only when at least one selected record has an accessible local file.

**Rationale**: The clarification requires a user choice between deleting records only or deleting records and files. Reusing one dialog model keeps the state machine small and makes the UX consistent between item delete and Delete All.

**Alternatives considered**:
- Separate dialogs per delete action — duplicates state and strings
- Hardcode file deletion behavior — conflicts with clarified spec

## R6: Test Scope

**Decision**: Add unit tests for `ObserveHistoryItemsUseCase`, `DeleteHistoryItemUseCase`, `DeleteAllHistoryUseCase`, and `HistoryViewModel`; skip instrumentation tests for this feature phase.

**Rationale**: The constitution requires use case coverage and ViewModel state-transition coverage, while MVP explicitly defers UI tests. The feature logic is mostly pure state transformation around a repository flow plus file-manager doubles, which is well-suited to JUnit5, MockK, Turbine, and coroutine test dispatchers.

**Alternatives considered**:
- Add Compose UI tests now — extra setup cost for low-value early coverage
- Test only the ViewModel — would leave delete/file behaviors under-specified

## R7: Shared JUnit5 Enablement

**Decision**: Enable JUnit Platform once in the shared Android convention plugins so Android unit tests in `:feature:history` and `:core:data` run JUnit5 consistently.

**Rationale**: The repo guidance and task plan both rely on JUnit5, but the current Android build conventions only add dependencies. Centralizing `useJUnitPlatform()` in build logic avoids per-module drift and ensures the planned unit tests are actually executable.

**Alternatives considered**:
- Configure JUnit Platform separately in each module — repetitive and easy to miss
- Fall back to JUnit4 — conflicts with the repo's stated testing stack
