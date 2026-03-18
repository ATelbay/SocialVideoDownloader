# Research: Core Download Flow Hardening

**Feature**: 007-download-flow-hardening | **Date**: 2026-03-18

## R1: POST_NOTIFICATIONS runtime permission on API 33+

**Decision**: Use `rememberLauncherForActivityResult(RequestPermission)` in DownloadScreen composable, triggered by ViewModel event before first download.

**Rationale**: Co-locates permission logic with the download trigger. The Compose `ActivityResultLauncher` API is the modern standard for permission requests. Handling in the composable avoids lifecycle issues with Activity-level launchers and keeps MainActivity clean. The ViewModel orchestrates via events (not direct permission calls) to maintain testability.

**Alternatives considered**:
- **Activity-level `registerForActivityResult`**: Works but pollutes MainActivity with download-specific logic. Rejected for separation of concerns.
- **Request on cold launch**: Rejected — spec explicitly requires prompt at first download, not app start. Premature permission requests reduce grant rates.
- **Accompanist Permissions library**: Unnecessary dependency for a single permission. The raw `ActivityResultContracts.RequestPermission` is sufficient.

**Key implementation details**:
- Check with `ContextCompat.checkSelfPermission(context, POST_NOTIFICATIONS)` — only on `Build.VERSION.SDK_INT >= 33`.
- If `shouldShowRequestPermissionRationale` returns true, show rationale first, then request.
- If permanently denied (rationale returns false after first denial), show snackbar with "Open Settings" action.
- ViewModel exposes a `needsNotificationPermission: Boolean` computed from the permission state, checked in `handleDownload()`.

## R2: Partial-file cleanup on cancel

**Decision**: Delete contents of `cacheDir/ytdl_downloads/` synchronously in the cancel handler, after `destroyProcessById`.

**Rationale**: The yt-dlp library writes partial downloads to a single temp directory. `destroyProcessById` sends SIGTERM to the yt-dlp process, which terminates within milliseconds. Deleting the directory contents immediately after is safe — the process is dead. Synchronous deletion is fine because the files are small (partial downloads in cache, not final output).

**Alternatives considered**:
- **Background coroutine for cleanup**: Overengineered — the files are small and `deleteRecursively()` completes in <100ms. Synchronous is simpler.
- **Delete only the specific request's files**: yt-dlp doesn't name temp files predictably. Safer to clear the entire temp directory since only one download is active at a time.
- **Schedule cleanup via WorkManager**: Way too heavy for a simple file delete.

**Key implementation details**:
- `File(cacheDir, "ytdl_downloads").listFiles()?.forEach { it.deleteRecursively() }` — idempotent, no-crash on missing dir.
- Directory itself is preserved (yt-dlp recreates it on next download).

## R3: Queued download UI feedback

**Decision**: Emit `DownloadEvent.ShowSnackbar(message)` from ViewModel when Queued state is received. No new UiState variant.

**Rationale**: The queued state is transient — it only matters for the moment the user triggers the second download. A snackbar is the standard Material Design pattern for transient confirmations ("Action completed" feedback). Adding a persistent `Queued` UI state would require a new rendering branch in AnimatedContent for a state that lasts <1 second before transitioning to the next active download.

**Alternatives considered**:
- **New `DownloadUiState.Queued`**: Rejected — adds a visible UI state that would flash briefly before reverting. Worse UX than a snackbar.
- **Toast**: Rejected — not Material 3 idiomatic, not testable, overlaps with notification shade.
- **Inline banner**: Rejected — too heavy for a transient confirmation. Snackbar is correct.

**Key implementation details**:
- ViewModel `collectServiceState()` Queued branch: `_events.send(DownloadEvent.ShowSnackbar(context.getString(R.string.download_queued)))`.
- DownloadScreenContent gets `SnackbarHostState` + `SnackbarHost` wired to Scaffold.
- Same snackbar mechanism reused for permission denial rationale.

## R4: Notification tap actions (contentIntent)

**Decision**: Build `PendingIntent.getActivity` for each notification type with appropriate intent configuration.

**Rationale**: Standard Android notification pattern. Every notification should have a contentIntent — notifications without one are considered broken UX.

**Alternatives considered**:
- **Deep link via Navigation**: Possible but overkill — the app has a single download screen and tapping any notification should just bring the app to the foreground. Deep links would be needed if multiple screens could be the target.
- **PendingIntent.getService**: Not appropriate for opening UI.

**Key implementation details**:
- **Completion**: `Intent(ACTION_VIEW).setDataAndType(Uri.parse(mediaStoreUri), mimeType)` wrapped in `PendingIntent.getActivity`. Use `Intent.FLAG_GRANT_READ_URI_PERMISSION` for MediaStore URIs.
- **Error**: `Intent(context, MainActivity::class.java)` with `FLAG_ACTIVITY_SINGLE_TOP` and `FLAG_ACTIVITY_CLEAR_TOP`.
- **Progress**: Same as error — just bring the app to foreground.
- All PendingIntents: `FLAG_IMMUTABLE or FLAG_UPDATE_CURRENT`. Request code = notificationId for uniqueness.
- `DownloadNotificationManager` method signatures gain `mediaStoreUri: String?` and `mimeType: String` for completion, no new params for error/progress.

## R5: fileSizeBytes population

**Decision**: Query `ContentResolver` for file size after `saveFileToMediaStore()` returns, in `DownloadService`.

**Rationale**: The MediaStore URI is available immediately after save. Querying `OpenableColumns.SIZE` is a single cursor query — fast and reliable. This avoids creating a new use case for a one-liner.

**Alternatives considered**:
- **New `GetFileSizeUseCase`**: Rejected — YAGNI. A single ContentResolver query doesn't warrant a use case class. Inline in the service.
- **Estimate from yt-dlp format info**: Rejected — yt-dlp's reported size is an estimate. The actual saved file size (post-muxing) can differ. Query the real size.
- **Read file size from temp file before MediaStore save**: Rejected — the temp file may differ from the final output (ffmpeg muxing can change size).

**Key implementation details**:
- API 29+: `contentResolver.query(Uri.parse(mediaStoreUri), arrayOf(OpenableColumns.SIZE), null, null, null)?.use { cursor -> if (cursor.moveToFirst()) cursor.getLong(0) else null }`.
- Pre-API 29: `File(filePath).length().takeIf { it > 0 }`.
- Pass result to `DownloadRecord(fileSizeBytes = queriedSize)`.

## R6: downloadedBytes calculation

**Decision**: Calculate `downloadedBytes = (progressPercent / 100f * totalBytes).toLong()` in the progress callback. Pass `totalBytes` via `DownloadRequest`.

**Rationale**: yt-dlp's progress callback provides percent but not absolute bytes downloaded. Since we know the total from format selection, simple multiplication gives an accurate enough estimate. The ViewModel already has `selectedFormat.fileSizeBytes`.

**Alternatives considered**:
- **Parse yt-dlp progress line for absolute bytes**: yt-dlp sometimes includes `downloaded_bytes` in the progress output, but the youtubedl-android wrapper doesn't expose it. Parsing raw output is fragile.
- **Accumulate from speed × time**: Inaccurate due to variable speed and timing gaps.

**Key implementation details**:
- Add `totalBytes: Long?` field to `DownloadRequest` data class.
- In ViewModel's `handleDownload()`, set `totalBytes = selectedFormat.fileSizeBytes`.
- In service progress callback: `downloadedBytes = if (totalBytes != null && totalBytes > 0) ((progressPercent / 100f) * totalBytes).toLong() else 0L`.

## R7: SavedStateHandle URL persistence

**Decision**: Write `currentUrl` to SavedStateHandle on every `UrlChanged` intent. Read with fallback chain in `init`.

**Rationale**: SavedStateHandle survives process death. Writing on every keystroke is lightweight (it's just a Bundle put). The existing `initialUrl` key (from share intent) takes priority; `currentUrl` is the fallback for manually typed URLs.

**Alternatives considered**:
- **Debounce writes**: Unnecessary — SavedStateHandle writes to a Bundle, which is in-memory. No I/O cost.
- **Save entire UiState**: Overengineered for this case. Only the URL needs to survive; the rest of the state (extraction results, formats) is re-derivable.

**Key implementation details**:
- `handleUrlChanged(url)`: add `savedStateHandle["currentUrl"] = url`.
- `init`: `val initialUrl = savedStateHandle["initialUrl"] ?: savedStateHandle["currentUrl"]`.

## R8: Safe retry handling

**Decision**: Replace force-cast with exhaustive `when` expression over `RetryAction` sealed interface.

**Rationale**: Kotlin's `when` on a sealed interface is exhaustive — the compiler enforces handling all variants. This is strictly safer than an `as` cast and costs zero runtime overhead.

**Alternatives considered**:
- **Safe cast `as?` with null check**: Less safe than `when` — doesn't force handling new variants at compile time.
- **Visitor pattern**: Overengineered for 1-2 variants.

**Key implementation details**:
- Replace lines 209-211 in DownloadViewModel with `when (val action = state.retryAction) { is RetryAction.RetryExtraction -> { currentUrl = action.url; handleExtract() } }`.
