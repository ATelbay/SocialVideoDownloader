# Feature Specification: Core Video Download Flow

**Feature Branch**: `002-core-download-flow`
**Created**: 2026-03-14
**Status**: Draft
**Input**: User description: "Core video download flow — URL input, metadata extraction, format selection, download with progress, foreground service, completion/error handling, MediaStore storage, Room history"

## Clarifications

### Session 2026-03-14

- Q: Can the user cancel an in-progress download, and from where? → A: User can cancel from both the in-app progress UI and the download notification.
- Q: What happens if the user starts a second download while one is active? → A: Queue it — the second download starts automatically after the first one finishes.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Paste URL and Download Video (Priority: P1)

A user has found a video on a social media platform (TikTok, Instagram, Twitter/X, YouTube, etc.) and wants to save it locally. They copy the video URL, open the app, paste or type the URL into the input field, wait for the app to extract video information, see the video thumbnail/title/duration/author, pick a quality format, and tap download. The video downloads with visible progress and is saved to their device.

**Why this priority**: This is the core value proposition of the entire app. Without this flow, the app has no purpose.

**Independent Test**: Can be fully tested by pasting a valid video URL, selecting a format, completing a download, and verifying the file exists in Downloads.

**Acceptance Scenarios**:

1. **Given** the app is open on the main screen, **When** the user pastes a valid video URL and taps "Extract", **Then** the app shows a loading indicator and extracts video metadata (thumbnail, title, duration, author)
2. **Given** video metadata is displayed with available formats, **When** the user selects a format chip and taps "Download", **Then** the download begins with a real-time progress bar showing percentage, speed, and ETA
3. **Given** a download is in progress, **When** the download completes successfully, **Then** the app shows a success screen with "Open" and "Share" buttons, and the file is saved to Downloads/SocialVideoDownloader/
4. **Given** a download completes, **Then** the download metadata (URL, title, format, file path, timestamp) is saved to the local database for history

---

### User Story 2 - Format Selection with Pre-Selected Best Quality (Priority: P1)

After metadata extraction, the user sees available formats displayed as selectable chips. Video formats show resolution labels (2160p, 1080p, 720p, 480p) with estimated file sizes. Audio-only formats (mp3, m4a) are also available. The "best quality" video option is pre-selected by default so the user can download immediately without manually choosing.

**Why this priority**: Format selection is integral to the download flow — users need control over quality and file size, especially on mobile data.

**Independent Test**: Can be tested by extracting info from a URL and verifying that formats are displayed correctly, best quality is pre-selected, and tapping a different chip changes the selection.

**Acceptance Scenarios**:

1. **Given** video metadata has been extracted, **When** formats are displayed, **Then** video formats show resolution (2160p/1080p/720p/480p) and estimated file size, and audio-only formats show codec (mp3/m4a) and file size
2. **Given** formats are displayed, **When** the user has not manually selected a format, **Then** the highest quality video format is pre-selected
3. **Given** a format is pre-selected, **When** the user taps a different format chip, **Then** that format becomes selected and the previous selection is deselected

---

### User Story 3 - Background Download with Foreground Service Notification (Priority: P1)

Once a download starts, it runs in a foreground service so it continues even if the user leaves the app or the screen turns off. A persistent notification shows download progress (percentage, speed, ETA). The user can return to the app at any time to see the in-app progress UI.

**Why this priority**: Downloads can take minutes — users will leave the app. Without a foreground service, Android will kill the download process.

**Independent Test**: Can be tested by starting a download, minimizing the app, verifying the notification updates, and returning to the app to confirm progress is still displayed.

**Acceptance Scenarios**:

1. **Given** a download is started, **When** the user navigates away from the app, **Then** the download continues in the background via a foreground service
2. **Given** a download is running in the background, **Then** a persistent notification shows download progress, speed, and ETA, along with a "Cancel" action
3. **Given** the app was backgrounded during a download, **When** the user returns to the app, **Then** the in-app UI reflects the current download progress
4. **Given** a download is in progress, **When** the user taps "Cancel" in the in-app UI or the notification, **Then** the download is stopped, the partial file is deleted, and the UI returns to the format selection state

---

### User Story 4 - Clipboard Auto-Detection (Priority: P2)

When the user opens the app or returns to it, if the clipboard contains a URL that looks like a supported video link, the app auto-populates the URL input field. This saves the user from manually pasting.

**Why this priority**: Convenience feature that reduces friction but is not essential — users can always paste manually.

**Independent Test**: Can be tested by copying a video URL to the clipboard, opening the app, and verifying the URL field is pre-filled.

**Acceptance Scenarios**:

1. **Given** the clipboard contains a valid video URL, **When** the user opens the app, **Then** the URL input field is pre-filled with the clipboard URL
2. **Given** the clipboard contains non-URL text or is empty, **When** the user opens the app, **Then** the URL input field remains empty
3. **Given** the clipboard URL was already used for a previous extraction, **When** the user returns to the app, **Then** the same URL is not auto-populated again

---

### User Story 5 - Error Handling with Retry (Priority: P2)

When something goes wrong — unsupported URL, network error, extraction failure, download failure — the user sees a clear, human-readable error message explaining what happened, along with a "Retry" button to attempt the operation again.

**Why this priority**: Errors are inevitable with yt-dlp and network operations. Good error UX prevents user frustration.

**Independent Test**: Can be tested by providing an invalid URL or simulating a network failure and verifying the error message and retry button appear.

**Acceptance Scenarios**:

1. **Given** the user enters an unsupported or invalid URL, **When** extraction is attempted, **Then** a human-readable error message is shown (not a raw stack trace) with a "Retry" button
2. **Given** a network error occurs during download, **When** the error is detected, **Then** the app shows an error message indicating a connectivity issue with a "Retry" button
3. **Given** an error screen is displayed, **When** the user taps "Retry", **Then** the failed operation is re-attempted from the beginning

---

### User Story 6 - Download Completion with Open and Share (Priority: P2)

When a download finishes successfully, the user sees a completion screen showing the video thumbnail and title, with "Open" (launches the video in the default media player) and "Share" (opens the system share sheet) buttons.

**Why this priority**: Post-download actions are expected UX for a downloader. Without them, users must navigate to the file manager.

**Independent Test**: Can be tested by completing a download and verifying both "Open" and "Share" buttons work correctly.

**Acceptance Scenarios**:

1. **Given** a download completes successfully, **Then** a success screen is shown with the video thumbnail, title, and "Open" and "Share" buttons
2. **Given** the success screen is displayed, **When** the user taps "Open", **Then** the video opens in the device's default media player
3. **Given** the success screen is displayed, **When** the user taps "Share", **Then** the system share sheet opens with the downloaded video file

---

### Edge Cases

- What happens when the user pastes a URL for a platform that yt-dlp does not support? → Show a clear error: "This URL is not supported. Try a link from a supported platform."
- What happens when a video has been taken down or is private? → Show error: "This video is unavailable. It may be private or removed."
- What happens when the device runs out of storage mid-download? → Show error: "Not enough storage space. Free up space and try again."
- What happens when the user starts a new extraction while a download is in progress? → Allow it — the current download continues in the foreground service, and the user can start a new extraction flow.
- What happens when the user starts a second download while the first is still active? → The second download is queued and starts automatically after the first finishes. The user sees a confirmation that their download has been queued.
- What happens when the network switches from Wi-Fi to mobile data during a download? → The download continues if possible; if it fails, show a network error with retry.
- What happens when the user cancels a download? → The download is stopped immediately, the partial file is deleted, and the UI returns to the format selection state so the user can re-select or change formats.
- What happens when the app is force-killed during a download? → The foreground service and download are terminated. On next app launch, no automatic resume (MVP scope).
- What happens when yt-dlp returns no formats for a valid URL? → Show error: "Could not find downloadable formats for this video."
- What happens when the same video is downloaded twice? → Allow duplicate downloads. Each download creates a separate file and history entry.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: System MUST accept a URL via text input field or clipboard auto-detection
- **FR-002**: System MUST extract video metadata (title, thumbnail URL, duration, author/channel name) from a given URL
- **FR-003**: System MUST display extracted metadata: video thumbnail image, title, duration, and author
- **FR-004**: System MUST display available download formats as selectable chips, showing resolution and estimated file size for video formats, and codec and file size for audio-only formats
- **FR-005**: System MUST pre-select the highest quality video format by default
- **FR-006**: System MUST allow the user to select exactly one format at a time
- **FR-007**: System MUST download the selected format and save the file to the device's Downloads directory under a "SocialVideoDownloader" subfolder
- **FR-008**: System MUST show real-time download progress including percentage complete, download speed, and estimated time remaining
- **FR-009**: System MUST run downloads in a foreground service with a notification showing progress and a "Cancel" action
- **FR-015**: System MUST allow the user to cancel an in-progress download from both the in-app progress UI and the notification; cancellation stops the download, deletes the partial file, and returns the UI to the format selection state
- **FR-010**: System MUST display a success screen with "Open" and "Share" actions upon download completion
- **FR-011**: System MUST display human-readable error messages with a "Retry" button when extraction or download fails
- **FR-012**: System MUST save download metadata (source URL, video title, selected format, local file path, timestamp, file size) to a local database after successful download
- **FR-013**: System MUST allow the user to start a new URL extraction while a download is in progress
- **FR-016**: System MUST queue a new download request if a download is already active, and start the queued download automatically when the current one finishes
- **FR-014**: System MUST auto-detect video URLs from the clipboard when the app is opened or resumed

### Key Entities

- **Video Metadata**: Represents extracted information about a video — title, thumbnail URL, duration (seconds), author/channel name, source URL, list of available formats
- **Download Format**: A specific downloadable variant of a video — resolution or codec label, estimated file size, format identifier, whether video or audio-only
- **Download Record**: A completed download entry for history — source URL, video title, format selected, local file path, file size, download timestamp, thumbnail URL

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Users can go from pasting a URL to starting a download in under 30 seconds (excluding extraction wait time)
- **SC-002**: Download progress updates are visible to the user at least once per second during active downloads
- **SC-003**: 95% of supported-platform URLs successfully extract metadata on first attempt (given stable network)
- **SC-004**: Downloaded files are immediately accessible via the device's file manager and media gallery
- **SC-005**: Users can identify and recover from errors within 10 seconds using the displayed error message and retry button
- **SC-006**: Downloads continue uninterrupted when the user leaves the app, with notification progress visible at all times

## Assumptions

- The app targets personal use — one active download at a time in MVP, with queuing for additional requests
- yt-dlp supports the major social platforms the user cares about (TikTok, Instagram, YouTube, Twitter/X) — no custom extractors needed
- File naming uses the video title sanitized for filesystem compatibility, with a timestamp suffix to avoid collisions
- The downloads folder is "Downloads/SocialVideoDownloader/" to match the app name
- No download resume/pause capability in MVP — failed downloads restart from the beginning
- Clipboard auto-detection requires standard Android clipboard access; no special permissions beyond what the app normally requests
- Audio-only formats (mp3/m4a) require FFmpeg for conversion, which is bundled via the yt-dlp wrapper library
