# Feature Specification: Download History Screen

**Feature Branch**: `003-download-history`
**Created**: 2026-03-15
**Status**: Draft
**Input**: User description: "Download history screen with searchable list, thumbnails, item actions (open/share/delete), and bulk delete."

## Clarifications

### Session 2026-03-15

- Q: Which timestamp is used for the visible "download date" and default history ordering? → A: Use `createdAt` for both display and newest-first ordering across all records
- Q: How should title search behave? → A: Use case-insensitive substring matching against the stored title; an empty or whitespace-only query shows the full history list
- Q: What exactly happens when the user deletes one item or all items? → A: The confirmation flow offers "delete record only" and, when at least one accessible local file exists, "delete record(s) and file(s)"
- Q: Does `Delete All` apply only to the filtered results when search is active? → A: No. `Delete All` always removes the full history dataset
- Q: How should the screen behave when a completed record's file is no longer available? → A: Keep the record visible, allow Delete, block Open/Share, and show clear file-unavailable feedback if the user tries to act on it
- Q: What persistent file reference should history actions use? → A: Use a persisted MediaStore content URI or equivalent MediaStore reference as the primary open/share/delete handle; treat raw file paths as legacy fallback only

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Browse and Search Past Downloads (Priority: P1)

The user opens the History tab to review everything they have downloaded before. They can scroll through a list of past downloads and quickly identify an item by its thumbnail, title, format label, status, download date, and file size. A search field in the top bar lets them filter the list by video title without leaving the screen.

**Why this priority**: The primary value of a history screen is helping the user find a past download quickly. Without a readable, searchable list, the rest of the actions are much less useful.

**Independent Test**: Can be fully tested by populating the repository with completed and failed download records, opening History, and confirming that the correct items appear and filter correctly by title.

**Acceptance Scenarios**:

1. **Given** the repository contains saved download records, **When** the user opens the History screen, **Then** the app shows a scrollable list ordered from newest to oldest using each record's thumbnail, title, format label, status, download date, and file size
2. **Given** the user enters text in the search field, **When** a record title contains the query, **Then** only matching records remain visible and the filtering updates as the query changes
3. **Given** there are no download records, **When** the History screen is opened, **Then** the existing empty-state placeholder is shown instead of an empty list
4. **Given** there are download records but none match the current search query, **When** filtering finishes, **Then** the screen shows an empty-results state while keeping the search query visible
5. **Given** a record has status `COMPLETED` or `FAILED`, **When** it is rendered in the list, **Then** the status is clearly visible on that item

---

### User Story 2 - Reopen or Share a Completed Download (Priority: P1)

After finding a past download, the user wants to use it immediately. Tapping a completed item should open the saved file with the device's default handler, and a long-press menu should expose sharing for items whose files still exist locally.

**Why this priority**: The history list is only useful if the user can get back to the downloaded media from it. Reopening and sharing a completed download are the main follow-up actions.

**Independent Test**: Can be fully tested by seeding one completed record with an accessible local media reference and one failed record, then verifying open and share behavior directly from the History screen.

**Acceptance Scenarios**:

1. **Given** a history item has status `COMPLETED` and a readable local file, **When** the user taps the item, **Then** the app launches an Android open-file intent for that file
2. **Given** a history item has status `COMPLETED` and a readable local file, **When** the user long-presses the item and chooses Share, **Then** the app launches the system share sheet for that file
3. **Given** a history item has status `FAILED`, **When** the user taps the item, **Then** the app does not attempt to open a file and instead treats the item as non-openable
4. **Given** a history item points to a file that no longer exists, **When** the user tries to open or share it, **Then** the app shows clear feedback that the file is unavailable and keeps the history record visible

---

### User Story 3 - Clean Up History Entries (Priority: P2)

The user wants to remove individual history items or clear the entire history. From a long-press menu they can delete one entry, and from the top bar overflow they can choose Delete All with confirmation. When deleting, the user can remove just the Room record or also remove the underlying file from device storage when one exists.

**Why this priority**: Cleanup is important for long-term usability, but the history screen still delivers value before delete actions are added.

**Independent Test**: Can be fully tested by creating multiple records, deleting one from the context menu, then using Delete All from the overflow menu and confirming the list updates correctly.

**Acceptance Scenarios**:

1. **Given** a history item is visible, **When** the user long-presses it, **Then** a context menu appears with Delete and any other actions allowed for that item's current file state
2. **Given** the user chooses Delete for a history item, **When** they confirm the deletion mode, **Then** the Room record is removed and the app optionally removes the associated device file if the user selected that option
3. **Given** the user opens the top bar overflow menu, **When** they choose Delete All, **Then** the app shows a confirmation dialog before removing anything
4. **Given** the user confirms Delete All, **When** the deletion completes, **Then** all history records are removed and the screen transitions to the empty state
5. **Given** a search filter is active, **When** the user confirms Delete All, **Then** the action removes the full history dataset rather than only the currently filtered subset

---

### Edge Cases

- What happens when a completed record exists but its file has been moved or deleted outside the app? → Keep the history record visible, disable successful open/share behavior, and show a clear "file unavailable" message when the user tries to act on it
- What happens when the search query returns no matches? → Show an empty-results state while preserving the user's query so they can adjust it
- What happens when a failed record is long-pressed? → Delete remains available, but Share is hidden or disabled because there is no usable file to share
- What happens when Delete All is triggered while a search filter is active? → Delete All still applies to every stored history record, not only the filtered list
- What happens when the user chooses to delete both the record and file, but file removal fails? → The Room record is still removed, and the app informs the user that file cleanup could not be completed for one or more items
- What happens if the repository emits a non-terminal status such as `PENDING` or `DOWNLOADING`? → The item remains visible with its status label, but open/share actions are not offered until a readable completed file exists

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: System MUST observe the full download history from `DownloadRepository.getAll()` and render the screen reactively as repository data changes
- **FR-002**: System MUST display history records in a vertically scrollable list ordered from newest to oldest by `createdAt`
- **FR-003**: Each visible history item MUST show the video's thumbnail, title, format label, status, download date, and file size when those values are available
- **FR-004**: System MUST provide a search field in the top app bar that filters the visible list by video title without leaving the screen
- **FR-005**: Title filtering MUST be case-insensitive substring matching and update as the query changes; an empty or whitespace-only query MUST show the full list
- **FR-006**: System MUST reuse the existing empty-state placeholder when there are no history records
- **FR-007**: System MUST show an empty-results state when history exists but no records match the current search query
- **FR-008**: System MUST visually distinguish item status values, with `COMPLETED` and `FAILED` clearly readable in the list
- **FR-009**: System MUST open a completed item through an Android file-view intent when the user taps an item whose file is still accessible
- **FR-010**: System MUST prevent open-file actions for failed items or inaccessible files and provide clear user feedback instead of failing silently
- **FR-011**: System MUST show a long-press context menu for each history item
- **FR-012**: The long-press menu MUST always include Delete and MUST include Share only when the item has a shareable local file
- **FR-013**: System MUST launch the Android share sheet when the user selects Share for a shareable item
- **FR-014**: System MUST allow deletion of a single history item from the long-press menu
- **FR-015**: Single-item deletion MUST remove the Room record and MUST offer the user a choice between deleting the record only or deleting the record plus the associated local file when that file exists and is accessible
- **FR-016**: System MUST provide a Delete All action in the top app bar overflow menu
- **FR-017**: Delete All MUST require explicit user confirmation before any records are removed
- **FR-018**: Delete All MUST remove every history record, regardless of any active search filter, and MUST offer the user a choice between deleting records only or deleting records plus any associated local files that still exist and are accessible
- **FR-019**: After any delete action, the visible list and empty state MUST update immediately to reflect the new repository contents
- **FR-020**: Human-readable `createdAt` date values and file-size values MUST be shown using device-local formatting

### Key Entities

- **History Item**: A stored download record presented in the history list, including title, thumbnail URL, format label, local file reference, status, created-at timestamp, and file size
- **Search Query**: The text the user enters in the top bar to filter history items by title
- **Deletion Mode**: The user's chosen deletion scope for an item or bulk action: remove history records only, or remove records plus underlying local files when available

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: A user can open the History screen and visually scan their 20 most recent downloads without navigating away from the tab
- **SC-002**: A user can narrow a history list of at least 100 records to matching titles within 2 seconds of typing a query
- **SC-003**: 100% of visible history rows show title, status, and download date, and show thumbnail, format label, and file size whenever the corresponding data exists
- **SC-004**: Tapping a completed item with an accessible local media reference opens the file successfully on the first attempt in at least 95% of cases
- **SC-005**: Users can remove a single history item or clear the full history without leaving the screen, and every confirmed delete action is reflected in the list immediately

## Assumptions

- The existing placeholder empty screen can be reused for both "no history yet" and "no search results" states, with copy adjusted if needed
- The history screen is scoped to local data only; search does not call a backend or re-query a remote source
- `Delete All` applies to the full stored history, not the currently filtered subset
- The requested `formatLabel` is expected to be available from the history data source before implementation, even though the checked-in `DownloadRecord` model does not currently expose that field
- The history data contract will be extended to persist a MediaStore-backed content URI or equivalent identifier for open/share/delete flows; `filePath` remains only as a backward-compatible fallback
- `COMPLETED` and `FAILED` are the primary user-facing history statuses, but if non-terminal statuses are emitted they are shown as non-openable, non-shareable items
