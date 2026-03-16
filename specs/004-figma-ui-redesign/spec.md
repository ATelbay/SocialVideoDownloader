# Feature Specification: UI Redesign — Figma Make Design System

**Feature Branch**: `004-figma-ui-redesign`
**Created**: 2026-03-15
**Status**: Draft
**Input**: Apply new Figma Make design system across :core:ui tokens, download screen, and history screen

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Consistent Visual Identity Across All Screens (Priority: P1)

A user opens the app and sees a polished, cohesive design language on every screen — consistent colors, spacing, typography, and shapes — whether using light mode, dark mode, or dynamic color (Material You).

**Why this priority**: The design system tokens (colors, shapes, spacing, typography) are the foundation that every other UI component depends on. Without these, individual screen redesigns have no consistent baseline.

**Independent Test**: Can be verified by launching the app and visually inspecting both light and dark themes, toggling Material You on/off, and confirming all screens use the new palette, radius values, spacing, and type scale.

**Acceptance Scenarios**:

1. **Given** the app is launched on a device with API 31+ and dynamic color enabled, **When** the user views any screen, **Then** all standard M3 colors are overridden by the device wallpaper palette while custom colors (success, platform badges) remain fixed
2. **Given** the app is launched on a device below API 31, **When** the user views any screen, **Then** all colors match the Figma Make light/dark palette exactly
3. **Given** the user toggles between light and dark theme, **When** switching themes via the in-app toggle, **Then** all surfaces, text, and accents transition to the correct counterpart colors
4. **Given** any card, chip, input, or dialog is rendered, **When** the user inspects the element, **Then** corner radii match the design system (Small=10dp, Medium=12dp, Large=16dp, ExtraLarge=20dp, Full=24dp)

---

### User Story 2 - Redesigned Download Experience (Priority: P1)

A user navigates through the full download flow — from pasting a URL to completing a download — and each state (idle, extracting, format selection, downloading, complete, error) presents a distinct, purposeful layout with clear visual hierarchy and obvious next actions.

**Why this priority**: The download screen is the primary screen users interact with. A confusing or ugly download flow directly impacts core usability and perceived app quality.

**Independent Test**: Can be tested by walking through each download state end-to-end, verifying layout matches the Figma designs at each stage, and confirming all interactive elements (buttons, chips, cancel) function correctly.

**Acceptance Scenarios**:

1. **Given** the app is opened with no active download, **When** the idle state is displayed, **Then** the user sees a hero icon, title, subtitle, URL input with paste button, extract button, and supported platforms section
2. **Given** a valid URL is pasted and extraction starts, **When** the extracting state is shown, **Then** the URL field is disabled at 0.7 opacity, a spinner card with cancel button is displayed, and the top bar title reads "Video Downloader"
3. **Given** extraction completes with available formats, **When** the format selection state is shown, **Then** a video info card displays the thumbnail with duration badge and platform badge, format chips scroll horizontally, and the selected format info bar shows quality and file size
4. **Given** the user selects a format and starts downloading, **When** the downloading state is shown, **Then** a progress card displays the percentage (large text), an animated gradient progress bar, speed, ETA, and a cancel button in error color
5. **Given** the download completes, **When** the complete state is shown, **Then** a success icon (88dp, green), "Download complete!" heading, and Open/Share buttons are displayed
6. **Given** the download or extraction fails, **When** the error state is shown, **Then** an error icon (96dp, red), error message, Retry button, and "New Download" button are displayed
7. **Given** the user is in any non-idle state, **When** they tap the back button in the top bar, **Then** the screen resets to idle state

---

### User Story 3 - Redesigned Download History (Priority: P2)

A user navigates to the history screen and sees all past downloads in a card-based list with thumbnails, platform badges, status indicators, and can search, share, or delete items via a bottom sheet.

**Why this priority**: History is the secondary screen. It enhances the app but is not the core workflow. Users can use the app fully without history.

**Independent Test**: Can be tested by navigating to history, verifying card layout and badges, searching, long-pressing an item to open the bottom sheet, sharing, and deleting (with the delete confirmation dialog).

**Acceptance Scenarios**:

1. **Given** the user has past downloads, **When** they open the history screen, **Then** items appear as cards with thumbnail (72×54dp), platform badge, title, format tag, status badge, timestamp, and file size
2. **Given** the user taps the search icon, **When** they type a query, **Then** the top bar transitions to search mode with a text input and results filter in real-time
3. **Given** a search returns no results, **When** the empty state is shown, **Then** a Search icon in an 88dp circle, title, and description are displayed
4. **Given** the user has no downloads at all, **When** the history screen loads, **Then** a Clock icon in an 88dp circle, title, and description are displayed
5. **Given** the user long-presses a history item, **When** the bottom sheet appears, **Then** it shows the item title, a Share action, and a Delete action in error color, with spring animation
6. **Given** the user taps Delete in the bottom sheet, **When** the delete dialog appears, **Then** it shows a trash icon, title, description, "Also delete file from storage" checkbox, and Cancel/Delete buttons
7. **Given** the user taps the overflow menu and selects "Delete all", **When** the confirmation dialog appears, **Then** all items can be deleted with the same dialog pattern

---

### User Story 4 - Theme Toggle (Priority: P2)

A user taps the theme toggle button (Moon/Sun icon) in the download screen's top bar to switch between light and dark modes without leaving the screen.

**Why this priority**: Provides quick access to theme switching, enhancing usability in different lighting conditions, but is not critical to core functionality.

**Independent Test**: Can be tested by tapping the toggle and confirming the entire UI switches themes immediately.

**Acceptance Scenarios**:

1. **Given** the app is in light mode, **When** the user taps the Moon icon, **Then** the app switches to dark mode and the icon changes to a Sun
2. **Given** the app is in dark mode, **When** the user taps the Sun icon, **Then** the app switches to light mode and the icon changes to a Moon
3. **Given** the user switches theme, **When** the transition occurs, **Then** color changes animate smoothly

---

### User Story 5 - Shared UI Components (Priority: P1)

Shared components (VideoInfoCard, StatusBadge, PlatformBadge, FormatChip) are extracted into the :core:ui module so they render consistently wherever used — the download screen and the history screen.

**Why this priority**: Component consistency is foundational. Without shared components, the same visual element would look different across screens.

**Independent Test**: Can be verified by inspecting that both screens use the same composable for video cards, badges, and chips, and that they render identically.

**Acceptance Scenarios**:

1. **Given** a VideoInfoCard is displayed on the download screen (format selection, downloading, complete states), **When** the same video data is shown in history, **Then** the card layout, spacing, and styling are identical
2. **Given** a platform badge is shown for a YouTube URL, **When** it appears on both download and history screens, **Then** it uses the same red (#FF0000) pill component with consistent styling
3. **Given** a status badge shows "Completed", **When** it renders, **Then** it uses successContainer background and success text color consistently

---

### Edge Cases

- What happens when the thumbnail URL is unavailable or fails to load? → Show a placeholder with the platform color as background
- What happens when the video title is extremely long? → Clamp to 2 lines with ellipsis
- What happens when the file size is unknown? → Show "Unknown size" text in the format info bar
- What happens when the duration is missing from video info? → Hide the duration badge entirely
- What happens when the platform cannot be detected from the URL? → Show a generic "Video" badge with onSurfaceVariant color
- What happens on very small screens (< 360dp width)? → Format chips scroll horizontally; layout remains usable

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: App MUST provide a design token system defining colors (light + dark), shapes (5 radius tiers), spacing constants, and typography scale as specified in the Figma Make export
- **FR-002**: App MUST support Material You dynamic color on API 31+, overriding standard M3 palette colors while preserving custom colors (success, platform badge colors)
- **FR-003**: Download screen MUST display a top app bar with state-dependent title, back button (non-idle states), and theme toggle button
- **FR-004**: Download screen idle state MUST show a hero section, URL input with inline paste button, platform auto-detection, extract button, and supported platforms chips
- **FR-005**: Download screen extracting state MUST show a disabled URL field (0.7 opacity) and a spinner card with cancel option
- **FR-006**: Download screen format selection state MUST show a VideoInfoCard, horizontally scrollable format chips with selected/unselected visual states, format info bar, and download button
- **FR-007**: Download screen downloading state MUST show a VideoInfoCard, progress card with percentage, animated gradient progress bar, speed/ETA labels, and cancel button
- **FR-008**: Download screen complete state MUST show a VideoInfoCard, success icon, success message, Open and Share buttons, and a "New Download" text button
- **FR-009**: Download screen error state MUST show an error icon, error message, Retry button, and "New Download" button
- **FR-010**: History screen MUST display items as cards with thumbnail, platform badge, title, format tag, status badge, timestamp, and file size
- **FR-011**: History screen MUST support search mode in the top app bar with real-time filtering
- **FR-012**: History screen MUST use a bottom sheet (not a dropdown menu) for item context actions (Share, Delete), triggered by long-press
- **FR-013**: History screen delete dialog MUST include an "Also delete file from storage" checkbox
- **FR-014**: History screen MUST support a "Delete all" action via the overflow menu
- **FR-015**: App MUST provide shared composable components: VideoInfoCard, StatusBadge, PlatformBadge, FormatChip in the :core:ui module
- **FR-016**: Platform badges MUST use the specified brand colors: YouTube (#FF0000), Instagram (#C13584), TikTok (#010101), Twitter/X (#1DA1F2), Vimeo (#1AB7EA), Facebook (#1877F2)
- **FR-017**: All state transitions on the download screen MUST use animated transitions (not hard cuts)
- **FR-018**: Existing download and history ViewModel logic MUST remain unchanged. Theme toggle adds new infrastructure (SettingsRepository, SettingsViewModel, DataStore) scoped to :core:domain, :core:data, and :app modules

### Key Entities

- **DesignTokens**: Color palette (light/dark), shape radii (5 tiers), spacing constants, typography scale — defines the visual language for the entire app
- **PlatformIdentity**: Platform name and brand color — used to render platform-specific badges from URL detection
- **VideoInfoCard**: Composite display of video thumbnail, duration, platform, title, uploader — reused across download and history screens

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: All screens visually match the Figma Make design — colors, spacing, radius, and typography match token definitions
- **SC-002**: Both light and dark themes render correctly with no mismatched colors, illegible text, or invisible elements
- **SC-003**: Dynamic color (Material You) on API 31+ overrides standard palette without affecting custom colors (success, platform badges)
- **SC-004**: Users can navigate through all download states (idle → extracting → format selection → downloading → complete, or error) with smooth animated transitions between states
- **SC-005**: History screen items display all required metadata (thumbnail, badge, title, format, status, timestamp, size) and long-press opens a bottom sheet
- **SC-006**: Theme toggle switches the entire app between light and dark mode with smooth animation
- **SC-007**: Shared components (VideoInfoCard, PlatformBadge, StatusBadge, FormatChip) render identically on both download and history screens
- **SC-008**: No regressions in existing functionality — all download, extraction, and history features work exactly as before the redesign

## Assumptions

- The app already has a working download flow with state management (idle, extracting, format selection, downloading, complete, error) — this redesign only changes the visual presentation
- The theme toggle persists the user's preference across app restarts
- Platform detection from URL is already implemented in the domain layer
- The "Also delete file from storage" checkbox defaults to unchecked
- The gradient on the progress bar goes from primary to a lighter tint (primaryContainer or similar)

## Scope Boundaries

### In Scope
- Design token definitions (colors, shapes, spacing, typography)
- Download screen composable redesign (all 7 states)
- History screen composable redesign (list, search, empty states, bottom sheet, delete dialog)
- Shared component extraction to :core:ui
- Theme toggle functionality
- Animated state transitions

### Out of Scope
- Changes to ViewModel, use case, or repository logic
- New data models or database migrations
- Backend or network changes
- Custom fonts (uses system font)
- Accessibility audit (can be a follow-up)
- Landscape/tablet layout optimization
- Automated UI testing (can be a follow-up)
