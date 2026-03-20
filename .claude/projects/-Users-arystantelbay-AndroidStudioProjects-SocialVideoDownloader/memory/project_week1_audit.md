---
name: Week 1-4 Audit Gap List
description: Concrete gaps found by auditing the download flow against spec 002, history feature, and Library tab — organized by roadmap week
type: project
---

Full audit of codebase against spec 002 and roadmap, completed 2026-03-18.

## Week 1: Core Flow Hardening

| # | Gap | Severity | Details |
|---|---|---|---|
| 1 | **No `POST_NOTIFICATIONS` runtime permission request** | High | Declared in manifest but never requested at runtime. On API 33+ notifications silently fail. |
| 2 | **Partial file not deleted on cancel** | Medium | `cacheDir/ytdl_downloads/` only wiped at start of next download, not on cancel. Should delete in `cancelDownload()`. |
| 3 | **Queued state has no UI feedback** | Medium | `DownloadServiceState.Queued` emitted by service but ViewModel `collectServiceState()` treats as no-op. |
| 4 | **Completion/error notifications have no tap action** | Low | Tapping dismisses only. Completion should open file; error should navigate to app. |
| 5 | **`fileSizeBytes` never populated in `DownloadRecord`** | Low | Service builds record without setting file size. History records carry 0. |
| 6 | **`downloadedBytes` always 0 in `DownloadProgress`** | Low | Only percent/speed/ETA are live values. |
| 7 | **`currentUrl` not persisted to `SavedStateHandle`** | Low | Typed URL lost on process death. `SavedStateHandle` injected but only read, never written. |
| 8 | **`handleRetry()` force-casts `RetryAction`** | Low | `retryAction as RetryAction.RetryExtraction` — safe today but fragile. Use `when` instead. |

**Recommended priority order:** 1 → 2 → 3 → 4 → 5 → 6 → 7 → 8

## Week 2: Clipboard Auto-Detection (US4)

| # | Gap | Details |
|---|---|---|
| 1 | No automatic clipboard detection — only manual paste via `LocalClipboardManager` |
| 2 | No `ClipboardRepository` / `GetClipboardUrlUseCase` in domain layer |
| 3 | No "already used" URL tracking to avoid re-populating |
| 4 | No lifecycle-triggered ViewModel hook (ON_RESUME) |

## Week 3: History Completion

| # | Gap | Details |
|---|---|---|
| 1 | `DeleteAllHistoryUseCase` exists and is unit-tested but not injected into `HistoryViewModel` |
| 2 | `DeleteTarget` sealed interface only has `Single` — needs `All` variant |
| 3 | No "Delete All" button in History UI |
| 4 | No `HistoryIntent` for delete-all flow (`DeleteAllClicked`, `ConfirmDeleteAll`) |
| 5 | `handleConfirmDeletion` only handles `Single` — needs `All` branch |
| 6 | Confirmation dialog needs bulk variant text ("Delete all downloads?") |
| 7 | `history_delete_file_cleanup_failed` string exists but has no call site — wire it |

## Week 4: Library MVP

| # | Gap | Details |
|---|---|---|
| 1 | Library is a single `Text("coming soon")` placeholder — no ViewModel, no feature module |
| 2 | No product decision on Library vs History distinction. Recommendation: Library = file browser (MediaStore), History = activity log (Room) |
| 3 | Route and navigation already exist — just needs real content |

## Weeks 5-6: QA & Release

| # | Gap | Details |
|---|---|---|
| 1 | Zero `connectedAndroidTest` coverage |
| 2 | No runtime permission test coverage |
| 3 | No device-matrix validation (API 26-28 vs 29+) |
| 4 | Migration 3→4 data migration needs device validation |

**Why:** This is the actionable output of the Week 1 roadmap audit. Each item maps to a spec requirement or a code-level defect found during review.
**How to apply:** Use this as the task backlog for weeks 1-4. Check off items as they're implemented.
