# Feature Specification: Core Download Flow Hardening

**Feature Branch**: `007-download-flow-hardening`
**Created**: 2026-03-18
**Status**: Draft
**Input**: User description: "Harden the existing download flow by closing gaps found during the spec 002 audit. No new user-facing features — this is about correctness, permission handling, and user feedback for flows that already exist."

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Notification Permission Prompt (Priority: P1)

A user on Android 13+ opens the app and triggers their first download. Before the download starts, the app asks for notification permission so the user can see download progress in the notification shade. If the user declines, the download still proceeds but the user sees an explanation that progress notifications are unavailable.

**Why this priority**: Without notification permission on API 33+, the foreground service notification is silently suppressed, leaving the user with no visibility into download progress outside the app. This is the most impactful gap for new installs on modern Android.

**Independent Test**: Install the app on an API 33+ device, trigger a download, verify the permission dialog appears. Deny it and confirm the download still works with a rationale message shown.

**Acceptance Scenarios**:

1. **Given** the app is running on API 33+ and notification permission has not been granted, **When** the user taps the Download button for the first time, **Then** the system shows a runtime permission dialog for POST_NOTIFICATIONS before starting the download.
2. **Given** the user denies the notification permission, **When** the download starts, **Then** the app displays an inline message explaining that progress notifications will not appear, and the download proceeds normally.
3. **Given** the user grants the notification permission, **When** the download starts, **Then** the progress notification appears in the notification shade as expected.
4. **Given** the app is running on API < 33, **When** the user taps Download, **Then** no permission dialog is shown and the download starts immediately.

---

### User Story 2 - Queued Download Feedback (Priority: P1)

A user starts a download and then pastes a second URL and taps Download again. The app confirms the second download is queued and shows that a queue exists while the first download is in progress.

**Why this priority**: Currently the app silently accepts queued downloads with no feedback, leaving the user unsure whether their action succeeded.

**Independent Test**: Start two downloads in quick succession, verify the second shows a "Download queued" confirmation and the download screen indicates a queue is active.

**Acceptance Scenarios**:

1. **Given** a download is in progress, **When** the user starts a second download, **Then** the app shows a confirmation message (snackbar or inline) that the download has been queued.
2. **Given** multiple downloads are queued, **When** the user views the download screen, **Then** the UI indicates a queue exists (e.g., queue count or "queued" label).

---

### User Story 3 - Notification Tap Actions (Priority: P2)

A user sees a "Download complete" notification and taps it. The downloaded file opens in the appropriate viewer. If a download fails, tapping the error notification opens the app's download screen so the user can retry.

**Why this priority**: Notifications without tap actions are a dead end. Users expect tapping a notification to navigate somewhere meaningful.

**Independent Test**: Complete a download, tap the notification, verify the file opens. Trigger a failure, tap the error notification, verify the app opens to the download screen.

**Acceptance Scenarios**:

1. **Given** a download has completed, **When** the user taps the completion notification, **Then** the downloaded file opens via the system file viewer using the correct MIME type.
2. **Given** a download has failed, **When** the user taps the error notification, **Then** the app opens and navigates to the download screen.
3. **Given** a download is in progress, **When** the user taps the progress notification, **Then** the app opens and shows the current download state.

---

### User Story 4 - Partial File Cleanup on Cancel (Priority: P2)

A user cancels an in-progress download. The app immediately cleans up any partially downloaded files in the cache directory rather than leaving orphaned data until the next download.

**Why this priority**: Partial files waste device storage and can confuse subsequent downloads if not cleaned up promptly.

**Independent Test**: Start a download, cancel it mid-progress, verify that the yt-dlp download cache directory is empty afterward.

**Acceptance Scenarios**:

1. **Given** a download is in progress, **When** the user cancels the download, **Then** all partial files in the download cache directory are deleted immediately after the yt-dlp process is destroyed.
2. **Given** the cache directory contains files from the cancelled download, **When** cleanup runs, **Then** only the contents of the download cache directory are removed (the directory itself is preserved).

---

### User Story 5 - Accurate File Size in History (Priority: P3)

After a download completes, the history screen shows the actual file size rather than "0 bytes" or an unknown indicator.

**Why this priority**: Displaying incorrect file size undermines user trust in the download history but does not block core functionality.

**Independent Test**: Complete a download, navigate to history, verify the displayed file size matches the actual file on disk.

**Acceptance Scenarios**:

1. **Given** a download has completed successfully, **When** the download record is saved, **Then** the fileSizeBytes field contains the actual size of the saved file (queried from the device's storage).
2. **Given** the file size query fails for any reason, **When** the record is saved, **Then** fileSizeBytes is set to null rather than 0, and the history gracefully handles the missing value.

---

### User Story 6 - Accurate Downloaded Bytes During Progress (Priority: P3)

While a download is in progress, the UI shows a meaningful downloaded-bytes value rather than always displaying 0.

**Why this priority**: The downloaded bytes indicator exists in the UI but is non-functional, providing misleading information.

**Independent Test**: Start a download where the total size is known, verify the downloaded bytes value increases as progress advances.

**Acceptance Scenarios**:

1. **Given** a download is in progress and the total file size is known, **When** progress updates arrive, **Then** the downloaded bytes are calculated from the progress percentage and total size and displayed.
2. **Given** the total file size is unknown, **When** progress updates arrive, **Then** downloaded bytes remain unset or are derived from available progress data if possible.

---

### User Story 7 - URL Survives Process Death (Priority: P3)

A user types a URL into the input field but has not yet tapped Extract. The system kills the app's process in the background. When the user returns, the typed URL is still present in the input field.

**Why this priority**: Losing a typed URL is a minor annoyance but easy to prevent since the state persistence mechanism is already available.

**Independent Test**: Type a URL, background the app, simulate process death, return to the app, verify the URL is restored.

**Acceptance Scenarios**:

1. **Given** the user has typed a URL in the input field, **When** the process is killed and the activity is recreated, **Then** the previously typed URL is restored and visible in the input field.

---

### User Story 8 - Safe Retry Handling (Priority: P3)

The retry mechanism handles all possible retry action types without crashing, even as new retry variants are added in the future.

**Why this priority**: A force-cast in the retry handler is a latent crash waiting to happen when the code evolves. This is a defensive correctness fix.

**Independent Test**: Verify in a unit test that calling handleRetry() with any RetryAction subtype does not throw a ClassCastException.

**Acceptance Scenarios**:

1. **Given** a download extraction has failed, **When** the user taps Retry, **Then** the retry handler dispatches based on the RetryAction type using exhaustive pattern matching (not a cast).
2. **Given** a new RetryAction variant is added in the future, **When** the retry handler encounters it, **Then** the code does not compile unless the new variant is handled (enforced by exhaustive matching).

---

### Edge Cases

- What happens when the user revokes notification permission after granting it? Downloads continue; notifications silently stop appearing. No re-prompt until the next app session.
- What happens if the cache directory does not exist when cancel cleanup runs? The cleanup is a no-op — no crash.
- What happens if two cancels arrive in rapid succession? Cleanup is idempotent; deleting already-deleted files causes no errors.
- What happens if the file-size query returns 0 or fails? The record stores null for fileSizeBytes rather than a misleading 0.
- What happens if the download completes before the user taps the progress notification? The app opens and shows the current (completed) state.
- What happens if no app on the device can handle the downloaded file's MIME type? The completion notification tap shows a system "no app to handle" dialog or toast rather than crashing.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: On API 33+, the app MUST request POST_NOTIFICATIONS runtime permission before starting the first foreground download service. The request MUST occur at first download trigger, not on cold launch.
- **FR-002**: If the user denies notification permission, the app MUST show a rationale message explaining that progress notifications will not appear, and MUST proceed with the download normally.
- **FR-003**: When a download is cancelled, the service MUST delete all contents of the yt-dlp download cache directory immediately after destroying the yt-dlp process.
- **FR-004**: When a download is queued, the app MUST update the UI to reflect the queued status and display a confirmation message to the user.
- **FR-005**: The completion notification MUST include a tap action that opens the downloaded file via the system file viewer with the correct MIME type.
- **FR-006**: The error notification MUST include a tap action that launches the app and navigates to the download screen.
- **FR-007**: The progress notification MUST include a tap action that opens the app to the current download state.
- **FR-008**: After a file is saved to device storage, the app MUST query the actual file size and populate fileSizeBytes on the download history record before saving it.
- **FR-009**: The download progress indicator MUST calculate downloaded bytes from the progress percentage and total size when the total size is known, rather than always reporting 0.
- **FR-010**: The app MUST persist the current URL input to survive process death, so typed-but-not-extracted URLs are restored when the user returns.
- **FR-011**: The retry handler MUST use exhaustive pattern matching over the retry action types instead of a force-cast, ensuring compile-time safety when new variants are added.
- **FR-012**: All new user-facing strings MUST be defined in string resources, not hardcoded in code.

### Key Entities

- **DownloadProgress**: In-flight download state including progress percentage, downloaded bytes, total bytes, speed, and ETA. The downloadedBytes field transitions from always-zero to a calculated value.
- **DownloadRecord**: Persisted history entry for a completed or failed download. The fileSizeBytes field transitions from always-zero to the actual file size.
- **DownloadServiceState.Queued**: Service state variant representing queued downloads. Transitions from ignored by the UI to surfaced with user feedback.
- **RetryAction**: Sealed type for retry dispatch. Transitions from a single-variant force-cast to exhaustive pattern matching.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: On API 33+ devices, 100% of first-download attempts prompt for notification permission before the download starts.
- **SC-002**: After cancelling a download, the download cache directory contains zero partial files within 1 second.
- **SC-003**: When a second download is queued, the user receives visual confirmation within the same screen (no navigation required).
- **SC-004**: Tapping a completion notification opens the downloaded file in a viewer 100% of the time when a compatible app is installed.
- **SC-005**: Tapping an error notification returns the user to the download screen 100% of the time.
- **SC-006**: Download history entries show the correct file size (within 1% of actual) for all completed downloads.
- **SC-007**: Downloaded bytes progress shows a non-zero, increasing value during downloads where total size is known.
- **SC-008**: A URL typed in the input field survives process death and is restored when the user returns to the app.
- **SC-009**: The retry handler functions correctly for all current and future retry action variants without runtime exceptions.
- **SC-010**: All existing unit tests continue to pass after the changes.

## Assumptions

- The POST_NOTIFICATIONS permission is already declared in the AndroidManifest.
- The yt-dlp download cache directory is located at `cacheDir/ytdl_downloads/`.
- The app uses a single notification channel for all download-related notifications; this spec does not require splitting into multiple channels.
- The state persistence mechanism (SavedStateHandle) is already injected into the ViewModel.
- The service already emits the Queued state; only the ViewModel and UI need updating.

## Constraints

- All changes stay within :feature:download, :core:data, :core:ui, and :app modules. No new modules.
- No new UI screens — modifications to existing composables and service only.
- Existing unit tests must stay green. New tests required for: notification permission logic, cancel cleanup, queued state handling, fileSizeBytes population.
- Follow existing MVI pattern, Hilt injection, injected dispatchers.
- All new user-facing strings must go in strings.xml, not hardcoded.
