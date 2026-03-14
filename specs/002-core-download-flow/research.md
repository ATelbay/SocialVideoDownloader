# Research: Core Video Download Flow

**Branch**: `002-core-download-flow` | **Date**: 2026-03-14

## R1: youtubedl-android API for Metadata Extraction

**Decision**: Use `YoutubeDL.getInstance().getInfo(YoutubeDLRequest(url))` on `Dispatchers.IO`

**Rationale**: `getInfo()` adds `--dump-json` to a yt-dlp invocation and returns a `VideoInfo` object containing all fields needed for the spec: `title`, `thumbnail`, `duration`, `uploader`, and `formats` (list of `VideoFormat` with `formatId`, `height`, `ext`, `fileSize`/`fileSizeApproximate`, `vcodec`, `acodec`).

**Alternatives considered**:
- Raw yt-dlp process invocation with JSON parsing — unnecessary, library already handles it
- OkHttp + platform-specific scraping — fragile, loses yt-dlp's 1700+ site support

## R2: Format Selection Strategy

**Decision**: Use `-f <formatId>` with explicit format IDs from `VideoInfo.formats`

**Rationale**: The format picker UI shows specific formats from `getInfo()`. When the user selects one, we pass its `formatId` directly via `request.addOption("-f", formatId)`. For video formats that are video-only (no audio track), combine with best audio: `"-f", "$videoFormatId+bestaudio"`. Audio-only formats use their `formatId` directly.

**Alternatives considered**:
- `-S` sort-based selection — better for preset quality preferences, but our UI lets users pick a specific format from the list
- `"best"` default — doesn't give user control over quality/size tradeoff

## R3: Download Execution with Progress

**Decision**: Use `YoutubeDL.getInstance().execute(request, processId, callback)` with a unique `processId` per download

**Rationale**: The `execute()` method is blocking and runs yt-dlp as a subprocess. The callback receives `(progress: Float, eta: Long, line: String)` — progress is 0.0–100.0 percentage, eta is seconds remaining. The `processId` is required for cancellation support.

**Alternatives considered**:
- Direct process execution — loses progress parsing, cancellation, and error handling the library provides

## R4: Download Cancellation

**Decision**: Use `YoutubeDL.getInstance().destroyProcessById(processId)` — callable from any thread

**Rationale**: The library maintains an internal `Map<String, Process>`. Calling `destroyProcessById()` terminates the OS process; the blocked `execute()` call then throws `YoutubeDL.CanceledException`. This is safe to call from UI thread (notification action or in-app button).

**Alternatives considered**:
- Thread interruption — less reliable, may leave zombie processes
- Coroutine cancellation alone — the native process won't respond to coroutine cancellation without `destroyProcessById()`

## R5: Error Handling Mapping

**Decision**: Catch three exception types and map to user-friendly messages

**Rationale**:
- `YoutubeDLException` → parse `message` for yt-dlp error strings (e.g., "ERROR: Unsupported URL", "ERROR: Video unavailable") and map to human-readable text
- `YoutubeDL.CanceledException` → user-initiated, not an error — return to format selection state
- `InterruptedException` → treat as cancellation/system shutdown

**Alternatives considered**:
- Exposing raw yt-dlp stderr to user — violates FR-011 (human-readable errors)

## R6: Foreground Service for Downloads

**Decision**: Use a bound+started foreground service with `FOREGROUND_SERVICE_DATA_SYNC` type

**Rationale**: Android 14+ requires foreground service type declarations. `DATA_SYNC` is the appropriate type for downloading content. The service will be both started (to survive activity destruction) and bound (to communicate progress back to the UI via a `StateFlow`). Notification shows progress, speed, ETA, and a cancel action via `PendingIntent`.

**Alternatives considered**:
- WorkManager — doesn't support real-time progress UI well, adds unnecessary complexity for a user-initiated foreground operation
- Background service — will be killed by Android on API 26+

## R7: MediaStore Storage

**Decision**: Use `MediaStore.Downloads` collection to save files to `Downloads/SocialVideoDownloader/`

**Rationale**: Scoped Storage (API 29+) requires MediaStore for shared storage. Use `ContentResolver.insert()` with `MediaStore.Downloads.EXTERNAL_CONTENT_URI`, setting `DISPLAY_NAME` and `RELATIVE_PATH = "Download/SocialVideoDownloader"`. For API 26-28, fall back to direct file access with `WRITE_EXTERNAL_STORAGE` permission.

**Alternatives considered**:
- App-internal storage — files wouldn't be visible in file manager or gallery (violates SC-004)
- SAF (Storage Access Framework) — requires user to pick a folder each time, too much friction

## R8: Download Queue Implementation

**Decision**: In-memory FIFO queue managed by the foreground service, max 1 concurrent download

**Rationale**: The service maintains a `ConcurrentLinkedQueue<DownloadRequest>`. When a new download is requested while one is active, it's added to the queue. On completion/cancellation/error of the active download, the service dequeues and starts the next one. Queue state is transient — lost on force-kill (acceptable per MVP assumptions).

**Alternatives considered**:
- Room-persisted queue — over-engineering for MVP; adds DB migrations and complexity
- WorkManager chaining — poor progress reporting, not designed for user-visible foreground work

## R9: Clipboard Auto-Detection

**Decision**: Read clipboard in `onResume` lifecycle callback, validate URL pattern, track last-used URL in ViewModel state

**Rationale**: Android 10+ shows a toast when apps read the clipboard, so users are aware. Read once on resume, check against a URL regex pattern, and compare with last-extracted URL to avoid re-populating. Store the last-used clipboard URL in ViewModel to prevent duplicate auto-population.

**Alternatives considered**:
- `ClipboardManager.addPrimaryClipChangedListener()` — fires while app is in background, unnecessary and potentially invasive
- Always auto-populate without dedup — annoying if user already dismissed/used the URL

## R10: Video-Only Format Merging

**Decision**: For video-only formats (where `acodec == "none"`), automatically append `+bestaudio` to the format string

**Rationale**: Many platforms (YouTube especially) serve high-quality video and audio as separate streams. When a user selects a video format like "1080p", they expect it to have audio. The app should transparently merge video+audio using FFmpeg (already bundled). The format string becomes `"-f", "$videoFormatId+bestaudio"` and yt-dlp handles the merge via `--merge-output-format mp4`.

**Alternatives considered**:
- Only showing pre-merged formats — would exclude most high-quality options
- Showing video-only explicitly — confusing for non-technical users
