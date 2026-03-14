# Data Model: Project Foundation

**Branch**: `001-project-foundation` | **Date**: 2026-03-14

## Entities

### DownloadRecord

Represents a single video download attempt (completed, in-progress, or failed).

| Attribute      | Type     | Constraints                          | Notes                                    |
| -------------- | -------- | ------------------------------------ | ---------------------------------------- |
| id             | Long     | Primary key, auto-generated          | Unique identifier                        |
| sourceUrl      | String   | Not null                             | Original URL the user shared/entered     |
| videoTitle     | String   | Not null                             | Title extracted from video metadata      |
| thumbnailUrl   | String   | Nullable                             | URL for video thumbnail (loaded by Coil) |
| filePath       | String   | Nullable                             | Local file path after download completes |
| status         | String   | Not null, one of enum values         | See DownloadStatus below                 |
| createdAt      | Long     | Not null, epoch millis               | When the download was initiated          |
| completedAt    | Long     | Nullable, epoch millis               | When the download finished               |
| fileSizeBytes  | Long     | Nullable                             | Final file size, populated on completion |

### DownloadStatus (enum values stored as String)

| Value        | Description                                      |
| ------------ | ------------------------------------------------ |
| PENDING      | Download queued but not yet started               |
| DOWNLOADING  | Download actively in progress                    |
| COMPLETED    | Download finished successfully, file available    |
| FAILED       | Download failed (extraction error, network, etc.) |

## Relationships

- **DownloadRecord** is a standalone entity with no foreign keys in the MVP.
- Future consideration: a `VideoFormat` entity could link to `DownloadRecord` for format selection history, but this is out of scope for the foundation skeleton.

## DAO Operations (Skeleton)

| Operation         | Description                                | Query Pattern               |
| ----------------- | ------------------------------------------ | --------------------------- |
| insert            | Insert a new download record               | @Insert                     |
| getAll            | Get all records ordered by createdAt desc   | @Query, ORDER BY createdAt  |
| getById           | Get a single record by ID                  | @Query, WHERE id = :id      |
| updateStatus      | Update status and optionally completedAt   | @Update                     |
| delete            | Delete a record by ID                      | @Delete                     |

## State Transitions

```
PENDING → DOWNLOADING → COMPLETED
                      → FAILED
```

- No reverse transitions in MVP (no retry mechanism in foundation).
- Retry will be added in the download feature spec.
