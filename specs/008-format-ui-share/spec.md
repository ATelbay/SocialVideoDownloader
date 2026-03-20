# Feature Specification: Format Selection UI Overhaul + Share-without-Save

**Feature Branch**: `008-format-ui-share`
**Created**: 2026-03-20
**Status**: Draft
**Input**: User description: "Select Format screen UI overhaul + Share-without-save"

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Consistent Screen Padding (Priority: P1)

A user navigates between the Download, History, and Library screens. All screens display content with correct spacing — no extra blank area at the bottom caused by doubled window inset consumption.

**Why this priority**: This is a bug fix affecting every screen in the app. Incorrect padding degrades the experience everywhere and is the simplest change with the widest impact.

**Independent Test**: Open each of the three screens on any device and verify that content extends to the expected bottom edge without an abnormally large blank gap.

**Acceptance Scenarios**:

1. **Given** the app is open on the Download screen, **When** the user scrolls to the bottom, **Then** content ends with standard spacing (no doubled bottom padding).
2. **Given** the app is open on the History screen, **When** the user views the list, **Then** the bottom of the list aligns correctly with the navigation bar area.
3. **Given** the app is open on the Library screen, **When** the user views the screen, **Then** bottom spacing matches the other two screens.

---

### User Story 2 - Polished Video Info Card (Priority: P1)

After extracting video info, the user sees a visually refined card: the thumbnail spans edge-to-edge within the card with rounded top corners only, and the platform badge (e.g., "YouTube", "TikTok") appears as an overlay on the thumbnail rather than below the title text.

**Why this priority**: The video info card is the centrepiece of the format selection screen. Visual polish here directly impacts perceived app quality.

**Independent Test**: Extract info for any supported URL and visually verify the thumbnail fills the card width, top corners are rounded, bottom corners are square, the platform badge overlays the thumbnail bottom-left, and the old tag row below the title is gone.

**Acceptance Scenarios**:

1. **Given** a video URL has been extracted, **When** the format selection screen displays, **Then** the thumbnail image fills the full width of the card with no inner horizontal or vertical padding.
2. **Given** a video URL has been extracted, **When** the format selection screen displays, **Then** the thumbnail's top corners are rounded and bottom corners are square.
3. **Given** a video URL has been extracted, **When** the format selection screen displays, **Then** the platform badge appears overlaid on the bottom-left of the thumbnail with appropriate padding from the edges.
4. **Given** a video URL has been extracted, **When** the format selection screen displays, **Then** no tag row (FlowRow) or extra spacer appears below the title/uploader text.

---

### User Story 3 - Download and Share Buttons (Priority: P1)

On the format selection screen, the user sees two side-by-side buttons between the video info card and the format chips: "Download" (primary/gradient style) and "Share" (outlined/secondary style). The previous full-width download button at the bottom of the format list is removed.

**Why this priority**: This restructures the primary call-to-action area and introduces the Share entry point needed for User Story 4.

**Independent Test**: Extract info for any URL, verify two half-width buttons appear between the card and format chips, the old bottom download button is gone, and both buttons are tappable.

**Acceptance Scenarios**:

1. **Given** a video URL has been extracted with formats available, **When** the format selection screen displays, **Then** a "Download" button and a "Share" button appear side-by-side in a row between the video info card and the format chips.
2. **Given** the format selection screen is displayed, **When** the user taps "Download", **Then** the app begins downloading the selected format to permanent storage (existing behaviour).
3. **Given** the format selection screen is displayed, **When** the user taps "Share", **Then** the app begins the share flow (see User Story 4).
4. **Given** any other screen that previously used the full-width download button, **When** that screen displays, **Then** the button still renders correctly at full width.

---

### User Story 4 - Share Without Saving (Priority: P2)

The user taps the "Share" button on the format selection screen. The app downloads the video to a temporary location, opens the system share sheet with the video file, and returns the user to the format selection screen (not the "Done" screen). No permanent file is saved, no history record is created.

**Why this priority**: This is the most complex change and introduces new data flow and lifecycle management. It depends on User Story 3 for the Share button entry point.

**Independent Test**: Tap Share, wait for download to complete, verify the system share sheet opens with the video, dismiss the share sheet, and confirm you are back on the format selection screen. Verify no new entry appears in History or Library. Verify temp files are cleaned up after leaving the screen.

**Acceptance Scenarios**:

1. **Given** the user is on the format selection screen with a format selected, **When** the user taps "Share", **Then** a download begins in share mode (temporary storage).
2. **Given** a share-mode download is in progress, **When** the download completes, **Then** the system share sheet opens with the downloaded video file.
3. **Given** the share sheet has been presented, **When** the user dismisses or completes the share, **Then** the app returns to the format selection screen (not the "Done" state).
4. **Given** a share-mode download completed, **When** the user checks History or Library, **Then** no record of the shared video appears.
5. **Given** the user leaves the format selection screen after sharing, **When** the screen is destroyed, **Then** temporary share files are deleted from the device.

---

### Edge Cases

- What happens when the share-mode download fails mid-way? The app shows the standard error state and returns to format selection, with any partial temp files cleaned up.
- What happens when the user cancels a share-mode download? The download stops, partial temp files are cleaned up, and the user returns to format selection.
- What happens when the user's device has very low storage? The share download fails gracefully with a clear error message, same as a regular download.
- What happens when the user navigates away during a share-mode download? The download continues in the background service; if the user returns, they see progress. Temp files are cleaned up when the ViewModel is cleared.
- What happens when no app on the device can handle the shared video? The OS share sheet indicates no compatible apps — this is handled by the OS, not the app.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: All screens (Download, History, Library) MUST display content with correct bottom spacing, avoiding double inset consumption.
- **FR-002**: The video info card thumbnail MUST span the full width of the card with no inner padding.
- **FR-003**: The video info card thumbnail MUST have rounded top corners and square bottom corners.
- **FR-004**: The platform badge MUST appear as an overlay on the bottom-left of the thumbnail image.
- **FR-005**: The tag row (FlowRow) and its preceding spacer below the title/uploader text MUST be removed.
- **FR-006**: The format selection screen MUST display two side-by-side buttons (Download and Share) between the video info card and the format chips.
- **FR-007**: The Download button MUST use the existing primary/gradient style.
- **FR-008**: The Share button MUST use an outlined/secondary style with the same height and shape as the Download button.
- **FR-009**: The previous full-width download button at the bottom of the format chip list MUST be removed.
- **FR-010**: The Download button component MUST NOT enforce full-width internally; callers MUST control its width.
- **FR-011**: All existing uses of the Download button MUST continue to render at full width.
- **FR-012**: Tapping "Share" MUST trigger a download to a temporary directory, bypassing permanent storage and history recording.
- **FR-013**: Upon share download completion, the system MUST present the OS share sheet with the downloaded video file.
- **FR-014**: After sharing, the app MUST return the user to the format selection screen, not the completed/done state.
- **FR-015**: Temporary share files MUST be cleaned up when the user leaves the format selection screen.

### Key Entities

- **DownloadRequest**: Represents a user's intent to download a video. Extended with a flag indicating whether the download is for sharing only (temporary) or permanent saving.
- **ShareFile event**: A one-shot event emitted when a share-mode download completes, carrying the shareable file reference for the system share sheet.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: All three screens (Download, History, Library) display correct bottom spacing on devices of varying screen sizes — no visible doubled padding.
- **SC-002**: The video info card thumbnail visually fills the card width edge-to-edge and displays rounded top corners with square bottom corners.
- **SC-003**: The platform badge is visible overlaid on the thumbnail in the bottom-left corner on all tested videos.
- **SC-004**: Two side-by-side buttons (Download, Share) are visible between the video card and format chips, and both respond to taps.
- **SC-005**: Tapping Share downloads the video, opens the share sheet, and returns to format selection — with zero entries added to History or Library.
- **SC-006**: After leaving the format selection screen following a share, temporary files in the share cache directory are deleted within the same app session.
- **SC-007**: The app builds successfully and all existing functionality (URL input, format selection, regular download, history, library) continues to work without regression.

## Assumptions

- The app already has a FileProvider configured (for history file sharing) that can be extended with an additional cache path for share temp files.
- The existing download service can be parameterized to write to a different output directory without major restructuring.
- The OS share sheet is invoked via a standard ACTION_SEND intent; no custom share UI is needed.
- The outlined/secondary button follows the same design language (shape, height, typography) as the existing gradient button, differing only in fill vs. outline treatment.
