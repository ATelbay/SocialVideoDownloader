# Data Model: Download History Screen

**Branch**: `003-download-history` | **Date**: 2026-03-15

## Core Persistence Updates

### DownloadRecord (updated)

Represents one stored download attempt as exposed from `:core:domain`.

| Attribute      | Type            | Constraints                                  | Notes |
| -------------- | --------------- | -------------------------------------------- | ----- |
| id             | Long            | Primary key, existing                        | Stable record identifier |
| sourceUrl      | String          | Not null                                     | Original download source |
| videoTitle     | String          | Not null                                     | Search source for history |
| formatLabel    | String?         | New, nullable                                | Required for history UI; nullable to preserve pre-migration rows |
| mediaStoreUri  | String?         | New, nullable                                | Primary persisted content URI for open/share/delete flows |
| thumbnailUrl   | String?         | Nullable                                     | Coil thumbnail source |
| filePath       | String?         | Nullable                                     | Legacy fallback path for rows created before MediaStore URI persistence |
| status         | DownloadStatus  | Not null                                     | `COMPLETED` and `FAILED` are the main user-facing states |
| createdAt      | Long            | Not null, epoch millis                       | Visible download date and newest-first sort key |
| completedAt    | Long?           | Nullable                                     | Retained for future download flow work |
| fileSizeBytes  | Long?           | Nullable                                     | Rendered with local file-size formatting |

### DownloadEntity (updated)

Room storage mirrors the domain record and adds nullable `formatLabel` and `mediaStoreUri` columns. Because both new columns are nullable, the database can migrate from version 1 to version 2 without backfilling historic rows.

### DownloadRepository (updated)

| Operation | Signature | Purpose |
| --------- | --------- | ------- |
| Observe all records | `fun getAll(): Flow<List<DownloadRecord>>` | Existing newest-first Room-backed stream |
| Delete one record | `suspend fun delete(record: DownloadRecord)` | Existing single-item delete path |
| Delete all records | `suspend fun deleteAll()` | New bulk delete path for overflow action |

## Feature Presentation Models

### HistoryListItem

Presentation model derived from a `DownloadRecord` plus file availability.

| Attribute        | Type           | Notes |
| ---------------- | -------------- | ----- |
| id               | Long           | Stable lazy-list key |
| title            | String         | Displayed title and search target |
| formatLabel      | String?        | Nullable; UI renders fallback text when absent |
| thumbnailUrl     | String?        | Passed to Coil |
| status           | DownloadStatus | Drives status chip/label |
| createdAt        | Long           | Formatted in UI for local date/time text |
| fileSizeBytes    | Long?          | Formatted in UI when available |
| contentUri       | String?        | Actionable content URI resolved from `mediaStoreUri` or legacy `filePath` fallback |
| isFileAccessible | Boolean        | Derived by `HistoryFileManager`; enables open/share affordances |

### DeleteConfirmationState

Dialog model reused for single-item delete and Delete All.

| Attribute            | Type | Notes |
| -------------------- | ---- | ----- |
| target               | DeleteTarget | `Single(itemId)` or `All(totalCount)` |
| hasAnyAccessibleFile | Boolean | Controls whether the "also delete file(s)" option is shown |
| deleteFilesSelected  | Boolean | Current checkbox/toggle choice |
| affectedCount        | Int | Count of records to remove for confirmation copy |

### HistoryUiState

The feature follows the constitution-mandated sealed-interface pattern.

| Variant | Fields | Purpose |
| ------- | ------ | ------- |
| `Loading` | none | Initial state before the first repository emission is processed |
| `Empty` | `query: String`, `isFiltering: Boolean` | No records exist or no records match the active query |
| `Content` | `query: String`, `items: List<HistoryListItem>`, `openMenuItemId: Long?`, `deleteConfirmation: DeleteConfirmationState?`, `isDeleting: Boolean` | Main browsing state with menu/dialog state embedded |

### HistoryIntent

User actions entering the ViewModel.

| Intent | Payload | Purpose |
| ------ | ------- | ------- |
| `SearchQueryChanged` | `query: String` | Update local filter state |
| `HistoryItemClicked` | `itemId: Long` | Request open for a completed accessible item |
| `HistoryItemLongPressed` | `itemId: Long` | Show context actions |
| `DismissItemMenu` | none | Close the active long-press menu |
| `ShareClicked` | `itemId: Long` | Trigger share flow |
| `DeleteItemClicked` | `itemId: Long` | Open confirmation for one record |
| `DeleteAllClicked` | none | Open confirmation for bulk delete |
| `DeleteFilesSelectionChanged` | `selected: Boolean` | Toggle record-only vs record-plus-files mode |
| `ConfirmDeletion` | none | Execute pending delete request |
| `DismissDeletionDialog` | none | Cancel the pending delete request |

### HistoryEffect

One-off events consumed by the route/composable layer.

| Effect | Payload | Purpose |
| ------ | ------- | ------- |
| `OpenContent` | `contentUri: String` | Launch external open intent |
| `ShareContent` | `contentUri: String` | Launch system share sheet |
| `ShowMessage` | `message: String` or string resource mapping | Surface missing-file and delete-failure feedback |

## State Transitions

```text
Loading
  ├── repository emits 0 items ───────────────> Empty(query="", isFiltering=false)
  └── repository emits 1+ items ──────────────> Content(query="", items=[...])

Content
  ├── SearchQueryChanged + matches remain ────> Content(query, filteredItems)
  ├── SearchQueryChanged + no matches ────────> Empty(query, isFiltering=true)
  ├── HistoryItemLongPressed ─────────────────> Content(openMenuItemId=itemId)
  ├── DeleteItemClicked/DeleteAllClicked ─────> Content(deleteConfirmation=...)
  ├── ConfirmDeletion ────────────────────────> Content(isDeleting=true)
  ├── delete completes + items remain ────────> Content(updated items)
  └── delete completes + no items remain ─────> Empty(query=currentQuery, isFiltering=currentQuery.isNotBlank())

Empty
  ├── SearchQueryChanged + matches found ─────> Content(query, filteredItems)
  └── repository emits 1+ items ──────────────> Content(query, items)
```
