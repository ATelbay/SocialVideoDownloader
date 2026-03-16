# Feature Specification: UI Redesign with Custom Dark Theme

**Feature Branch**: `005-ui-redesign-dark-theme`
**Created**: 2026-03-16
**Status**: Draft
**Input**: Implement new custom dark theme with SVD design tokens, pill-shaped bottom navigation, gradient purple CTA buttons, redesigned URL input with inline paste button, platform chips with colored indicator dots, large percentage display with gradient progress bar, compact video card variant, and history items with platform badge overlays on thumbnails.

## User Scenarios & Testing

### User Story 1 - Custom Dark Theme & Design Tokens (Priority: P1)

The entire app adopts a custom dark color scheme using dedicated SVD design tokens instead of Material 3 dynamic colors. Every screen, component, and surface uses the new palette, giving the app a distinctive branded look that is consistent across all Android versions.

**Why this priority**: The color scheme is the foundation — every other visual change builds on top of these tokens. Without it, no other screen redesign can match the designs.

**Independent Test**: Launch the app on any device. Every surface, text color, and border should use SVD tokens. No Material 3 default purple or dynamic color should be visible.

**Acceptance Scenarios**:

1. **Given** the app is launched, **When** any screen is displayed, **Then** the background color is `#0F0D15` (svd-bg), surfaces use `#1A1726` (svd-surface), and elevated surfaces use `#241F33` (svd-surface-elevated).
2. **Given** the app is launched, **When** text is displayed, **Then** primary text is `#FFFFFF`, secondary text is `#A09BB0`, and tertiary/hint text is `#6B6580`.
3. **Given** any interactive element is shown, **When** it uses a primary accent, **Then** the color is `#8B5CF6` (svd-primary), and its soft/container variant is `#2D2150` (svd-primary-container).
4. **Given** any border or divider is shown, **Then** it uses `#2E2844` (svd-border).
5. **Given** a success state is displayed, **Then** success color is `#6ECF83` with container `#1B3D25`. Error states use `#FF6B6B` with container `#3D1B1B`.
6. **Given** heading text is displayed, **Then** it uses the "Plus Jakarta Sans" font family. Body text uses the "Inter" font family.

---

### User Story 2 - Pill-Shaped Bottom Navigation (Priority: P1)

The standard Material 3 bottom navigation bar is replaced with a custom pill-shaped tab bar. It floats at the bottom of every screen with rounded corners and contains two tabs: Download and History. The active tab is highlighted with a filled pill shape.

**Why this priority**: Navigation is visible on every screen and defines the app's visual identity. It is the most prominent structural departure from the current M3 design.

**Independent Test**: Navigate between Download and History tabs. The pill nav should appear on all screens, the active tab pill should be filled purple, and the inactive tab should show muted icon/text.

**Acceptance Scenarios**:

1. **Given** any screen is displayed, **When** looking at the bottom of the screen, **Then** a pill-shaped navigation bar is visible with corner radius 36, background `#1A1726`, 1px `#2E2844` border, 4px internal padding, and height 62.
2. **Given** the Download tab is active, **When** viewing the nav pill, **Then** the Download tab has a filled `#8B5CF6` background with corner radius 26, white icon (18px) and white text "DOWNLOAD" (10sp, weight 600, letter spacing 0.5). The History tab shows muted icon/text in `#6B6580`.
3. **Given** the History tab is active, **When** viewing the nav pill, **Then** the History tab is filled purple and Download tab is muted — the inverse of the above.
4. **Given** the nav pill container, **Then** it has outer padding of 21px left/right/bottom and 12px top.

---

### User Story 3 - Download Idle Screen Redesign (Priority: P1)

The idle/home screen has a centered hero section with a download icon in a circular container, a large title, subtitle, a redesigned URL input field with an inline paste button, a gradient "Extract Video" button, and a supported platforms section with platform chips showing colored indicator dots.

**Why this priority**: This is the entry point and most frequently seen screen. It sets the tone for the entire user experience.

**Independent Test**: Open the app fresh. Verify the hero icon, title, subtitle, URL input with paste button, extract button, divider, and platform chips all render correctly with exact design specs.

**Acceptance Scenarios**:

1. **Given** the idle screen is displayed, **When** looking at the hero section, **Then** an 80px circular container (corner radius 40, background `#2D2150`) contains a 36px download icon in `#8B5CF6`. Below it, the title "Download Videos" is displayed in Plus Jakarta Sans, 28sp, weight 700, white. The subtitle reads "Paste a link from any platform to get started" in Inter, 14sp, `#A09BB0`, centered.
2. **Given** the URL input field, **When** it is empty, **Then** it shows a link icon (20px, `#6B6580`) on the left, placeholder "https://..." in `#6B6580` Inter 15sp, and a "Paste" button on the right. The input has corner radius 16, background `#1A1726`, 1px `#2E2844` border, height 56, and padding 16px left / 6px right.
3. **Given** the Paste button inside the URL input, **Then** it has a `#8B5CF6` fill, corner radius 12, padding 10px vertical / 14px horizontal, contains a clipboard-paste icon (16px, white) and "Paste" text (Inter, 13sp, weight 600, white) with 6px gap.
4. **Given** the Extract Video button, **Then** it spans full width, has height 52, corner radius 16, a linear gradient fill from `#8B5CF6` to `#7C3AED` (top to bottom), contains a sparkles icon (18px, white) and "Extract Video" text (Inter, 16sp, weight 600, white) with 8px gap, centered.
5. **Given** the supported platforms divider, **Then** it shows two horizontal lines (`#2E2844`, 1px) flanking the text "SUPPORTED PLATFORMS" in Inter 11sp, weight 500, `#6B6580`, letter spacing 1px, with 12px gap.
6. **Given** the platform chips grid, **Then** it displays 6 chips in 2 rows of 3 (YouTube, Instagram, TikTok / Twitter, Vimeo, Facebook). Each chip has corner radius 12, background `#1A1726`, 1px `#2E2844` border, padding 10px vertical / 16px horizontal. Each contains an 8px colored dot (ellipse) in the platform's brand color followed by the platform name in Inter 13sp, weight 500, white, with 6px gap.
7. **Given** the content area, **Then** it has padding 28px top, 24px horizontal, 24px bottom, with 28px gap between major sections.

---

### User Story 4 - Extracting State with Large Spinner Display (Priority: P2)

While video info is being fetched, the screen shows a full custom extracting state that mirrors the downloading screen aesthetic: the URL is displayed in a compact input bar (non-editable), a large animated spinner replaces the percentage number, status text says "Extracting video info...", and a cancel button allows aborting.

**Why this priority**: This is a transient but visible state in the core flow. It must match the redesign aesthetic to avoid a jarring visual break between idle and format selection.

**Independent Test**: Paste a URL and tap Extract. Verify the extracting state renders with spinner, status text, and cancel button matching the new design language.

**Acceptance Scenarios**:

1. **Given** the extracting state, **Then** a top bar is shown with back button (36px, corner radius 12, `#1A1726` fill, arrow-left icon 20px white) and "Extracting..." in Plus Jakarta Sans 18sp weight 600 white, with 12px gap. Bar height 48, horizontal padding 16px.
2. **Given** the extracting state, **Then** the URL is shown in a non-editable compact bar (corner radius 16, `#1A1726` fill, 1px `#2E2844` border, height 56, padding 16px horizontal) with a link icon (20px, `#6B6580`) and the URL text in Inter 15sp `#A09BB0`, truncated with ellipsis.
3. **Given** the extracting state, **Then** a large circular spinner (56px diameter) is displayed centered, using `#8B5CF6` color. Below it: "Extracting video info..." in Inter 14sp `#A09BB0`, centered.
4. **Given** the cancel button, **Then** it matches the cancel button spec from the downloading screen (full width, height 48, corner radius 14, no fill, 1.5px `#FF6B6B` border, "Cancel" text Inter 15sp weight 600 `#FF6B6B`, centered).
5. **Given** the content layout, **Then** there is 32px gap between major sections (URL bar, spinner section, cancel button).

---

### User Story 5 - Format Selection Screen Redesign (Priority: P2)


After extracting video info, the format selection screen shows a top bar with back button and title, a full video card with thumbnail (play overlay, duration badge, platform badge), video/audio quality chip selectors, a format summary bar, and a gradient download button.

**Why this priority**: This is the second step in the core download flow. Critical for usability but depends on the theme foundation from P1.

**Independent Test**: Extract a video URL and verify the format selection screen renders with the correct video card, quality chips, summary bar, and download button matching the design.

**Acceptance Scenarios**:

1. **Given** the top bar, **Then** it contains a 36px back button (corner radius 12, `#1A1726` fill, white arrow-left icon 20px) and "Select Format" in Plus Jakarta Sans 18sp, weight 600, white, with 12px gap. Bar height is 48, horizontal padding 16px.
2. **Given** the video card thumbnail area, **Then** it is full-width, 180px height, with the video thumbnail filling the area. A centered play overlay (44px circle, `#00000080`, white play icon 20px) is visible. A duration badge (corner radius 8, `#000000AA` fill, Inter 11sp weight 600 white) sits at bottom-left (offset 8px). A platform badge (corner radius 8, platform brand color fill, Inter 10sp weight 700 white) sits at bottom-right.
3. **Given** the video info section below the thumbnail, **Then** it has 14px top / 16px horizontal padding, showing the title in Plus Jakarta Sans 15sp weight 600 white (full width, multi-line), and uploader name in Inter 13sp `#A09BB0`, with 4px gap between them. The card has corner radius 20, `#1A1726` fill, 1px `#2E2844` border.
4. **Given** the quality section, **Then** "VIDEO QUALITY" and "AUDIO QUALITY" labels are Inter 11sp, weight 600, `#6B6580`, letter spacing 1. Chips have corner radius 12, padding 10px/16px (video) or 10px/14px (audio). Selected chip: `#2D2150` fill, 1.5px `#8B5CF6` border, text `#8B5CF6` Inter 13sp weight 600. Unselected chip: `#1A1726` fill, 1px `#2E2844` border, text white Inter 13sp weight 500. Chips have 8px gap, sections have 12px gap.
5. **Given** the format summary bar, **Then** it has corner radius 14, `#241F33` fill, padding 14px vertical / 16px horizontal. Left side: "Selected Format" (Inter 11sp weight 500 `#6B6580`) over format description (Inter 14sp weight 600 white) with 2px gap. Right side: file size in Plus Jakarta Sans 16sp weight 700 `#8B5CF6`. Content is space-between aligned.
6. **Given** the download button, **Then** it matches the gradient button spec from the idle screen (full width, height 52, corner radius 16, gradient `#8B5CF6` to `#7C3AED`, download icon 18px + "Download" text Inter 16sp weight 600, white, centered, 8px gap).

---

### User Story 6 - Downloading Screen with Large Progress Display (Priority: P2)

During an active download, the screen shows a compact video card (thumbnail + info in a horizontal row), a large percentage number, status text, a gradient progress bar, download stats (speed, ETA, size), and a cancel button.

**Why this priority**: Active download feedback is essential for user confidence, but it builds on the theme and compact card components.

**Independent Test**: Start a download and verify the compact card, large percentage, progress bar, stats row, and cancel button all render per spec.

**Acceptance Scenarios**:

1. **Given** the compact video card during download, **Then** it shows a 72x54px thumbnail (corner radius 10) on the left, then title (Plus Jakarta Sans 13sp weight 600 white, multi-line) and uploader (Inter 12sp `#A09BB0`) with 4px gap, all in a row with 12px gap. Card has corner radius 16, `#1A1726` fill, 12px padding, 1px `#2E2844` border.
2. **Given** the progress percentage, **Then** it is displayed in Plus Jakarta Sans 64sp, weight 800, `#8B5CF6`, centered. Below it: "Downloading video..." in Inter 14sp `#A09BB0`.
3. **Given** the progress bar, **Then** it has a full-width track (10px height, corner radius 5, `#241F33` fill). The fill portion uses a linear gradient from `#8B5CF6` to `#A78BFA` (top to bottom), same height and corner radius, width proportional to progress percentage.
4. **Given** the stats row below the progress bar, **Then** three columns (Speed, ETA, Size) are space-between distributed across full width. Each has a label (Inter 11sp weight 500 `#6B6580`) above a value (Plus Jakarta Sans 16sp weight 700 white) with 2px gap, center-aligned.
5. **Given** the cancel button, **Then** it is full-width, height 48, corner radius 14, no fill, 1.5px `#FF6B6B` border, with "Cancel Download" text in Inter 15sp weight 600 `#FF6B6B`, centered.
6. **Given** the content layout, **Then** there is 32px gap between major sections (compact card, progress section, cancel button) and 16px gap within the progress section.

---

### User Story 7 - Download Complete Screen (Priority: P2)

After a successful download, the screen shows the compact video card, a success indicator (green check in circle), completion text, Open/Share action buttons, and a "New Download" link.

**Why this priority**: Completion is the payoff moment. It must feel polished and offer clear next actions.

**Independent Test**: Complete a download and verify the success circle, text, action buttons, and new download link match the design.

**Acceptance Scenarios**:

1. **Given** download completion, **Then** the compact video card is identical to the downloading state variant (same specs as User Story 6, scenario 1).
2. **Given** the success indicator, **Then** an 88px circle (corner radius 44, `#1B3D25` fill) contains a 40px check icon in `#6ECF83`. Below: "Download Complete!" in Plus Jakarta Sans 24sp weight 700 white, then "Saved to your Downloads folder" in Inter 14sp `#A09BB0`. Elements are center-aligned with 12px gap.
3. **Given** the Open button, **Then** it is full-width, height 48, corner radius 14, no fill, 1.5px `#8B5CF6` border, with external-link icon 18px `#8B5CF6` + "Open" text Inter 15sp weight 600 `#8B5CF6`, centered, 8px gap.
4. **Given** the Share button, **Then** it is full-width, height 48, corner radius 14, gradient fill `#8B5CF6` to `#7C3AED`, with share-2 icon 18px white + "Share" text Inter 15sp weight 600 white, centered, 8px gap. Open and Share buttons have 12px gap between them.
5. **Given** the New Download link, **Then** it shows a plus icon 16px `#8B5CF6` + "New Download" text Inter 14sp weight 500 `#8B5CF6`, centered, 6px gap, padding 10px vertical / 16px horizontal.
6. **Given** the user taps "New Download", **Then** the download screen resets to Idle state with an empty URL field. Any completed download state is cleared. The user sees the idle hero section.
7. **Given** the content layout, **Then** 24px gap between major sections.

---

### User Story 8 - Error State Screen (Priority: P2)

When a download or extraction fails, the screen mirrors the complete screen structure but with error tokens: a compact video card (if video info was available), an error icon circle, error title and message, a Retry gradient button, and a "New Download" text link.

**Why this priority**: Errors are part of the core flow. A jarring unstyled error screen would break the visual consistency of the redesign.

**Independent Test**: Trigger a download failure (e.g., invalid URL or network error). Verify the error screen renders with the correct icon, text, buttons, and SVD token colors.

**Acceptance Scenarios**:

1. **Given** an error occurs, **Then** a top bar is shown with back button (36px, corner radius 12, `#1A1726` fill, arrow-left icon 20px white) and "Error" in Plus Jakarta Sans 18sp weight 600 white, with 12px gap. Bar height 48, horizontal padding 16px.
2. **Given** video info was available before the error, **Then** the compact video card is shown (same specs as User Story 6, scenario 1). If no video info was available (extraction error), the compact card is omitted.
3. **Given** the error indicator, **Then** an 88px circle (corner radius 44, `#3D1B1B` fill) contains a 40px alert-triangle icon in `#FF6B6B`. Below: the error title in Plus Jakarta Sans 24sp weight 700 white, then the error message in Inter 14sp `#A09BB0`, centered. Elements are center-aligned with 12px gap.
4. **Given** the Retry button, **Then** it is full-width, height 52, corner radius 16, gradient fill `#8B5CF6` to `#7C3AED`, with refresh-cw icon 18px white + "Retry" text Inter 16sp weight 600 white, centered, 8px gap.
5. **Given** the New Download link, **Then** it shows a plus icon 16px `#8B5CF6` + "New Download" text Inter 14sp weight 500 `#8B5CF6`, centered, 6px gap, padding 10px vertical / 16px horizontal.
6. **Given** the user taps "New Download", **Then** the download screen resets to Idle state with an empty URL field. Any error state is cleared. The user sees the idle hero section.
7. **Given** the content layout, **Then** 24dp gap between major sections.

---

### User Story 9 - History Screen with Platform Badge Overlays (Priority: P2)

The history screen shows a title bar with search and more actions, and a scrollable list of history items. Each item is a horizontal card with a thumbnail (platform badge overlay at bottom-left), title, format/status badges, and timestamp/size metadata.

**Why this priority**: History is the secondary feature. It must be visually consistent with the download screens.

**Independent Test**: Navigate to History with existing downloads. Verify the title bar, list items with thumbnail badges, metadata rows, and failed-state styling.

**Acceptance Scenarios**:

1. **Given** the history top bar, **Then** it shows "Download History" in Plus Jakarta Sans 20sp weight 700 white on the left. On the right: search button (36px, corner radius 12, `#1A1726` fill, search icon 18px white) and more button (same styling, ellipsis-vertical icon), with 8px gap between them. Bar height 48, horizontal padding 16px.
2. **Given** a history list item, **Then** it is a horizontal card with corner radius 16, `#1A1726` fill, 12px padding, 1px `#2E2844` border, 12px gap between thumbnail and info. Thumbnail is 72x54px, corner radius 10. A platform badge is overlaid at bottom-left (offset 2px from left, 38px from top): corner radius 6, platform brand color fill, 2-letter abbreviation (YT/IG/TT/X) in Inter 8sp weight 700 (white for most, black for TikTok), padding 2px vertical / 6px horizontal.
3. **Given** the info column in a history item, **Then** title is Plus Jakarta Sans 13sp weight 600 white (multi-line, full width). Below: a row with format badge (corner radius 6, `#241F33` fill, padding 3px/8px, text Inter 10sp weight 600 `#A09BB0`) and status badge (corner radius 6, status-colored fill, text Inter 10sp weight 600 in status color). Completed: `#1B3D25` fill / `#6ECF83` text. Failed: `#3D1B1B` fill / `#FF6B6B` text. Badges have 6px gap. Below: timestamp and file size in Inter 11sp `#6B6580` separated by a dot with 4px gap. Info sections have 6px vertical gap.
4. **Given** a failed history item, **Then** the entire card has opacity 0.6. The file size shows a dash instead of a value.
5. **Given** the history list, **Then** items have 10px gap between them, with padding 8px top / 16px horizontal / 16px bottom.

---

### User Story 10 - History Empty State (Priority: P3)

When there are no downloads in history, the screen shows a centered empty state with an icon, title, description, and a "Start Downloading" button that navigates to the download screen.

**Why this priority**: Edge case screen, but important for first-time users. Lower priority since it is a simple state.

**Independent Test**: Clear all history or use a fresh install. Verify the empty state renders with correct icon, text, and CTA button.

**Acceptance Scenarios**:

1. **Given** no history items exist, **When** the History screen is shown, **Then** the empty state is vertically centered with horizontal padding 40px.
2. **Given** the empty state icon, **Then** an 88px circle (corner radius 44, `#241F33` fill) contains a calendar-clock icon 36px in `#6B6580`.
3. **Given** the empty state text, **Then** "No downloads yet" in Plus Jakarta Sans 20sp weight 700 white, centered. Below: "Downloaded videos will appear here. Start by pasting a video link!" in Inter 14sp `#A09BB0`, centered, full width. Elements have 16px gap.
4. **Given** the Start Downloading button, **Then** it has gradient fill `#8B5CF6` to `#7C3AED`, corner radius 14, padding 12px vertical / 24px horizontal, download icon 16px white + "Start Downloading" text Inter 14sp weight 600 white, 8px gap. Tapping it navigates to the Download tab.

---

### Edge Cases

- What happens when the URL input text is very long? It should be single-line with horizontal scrolling, clipped by the input bounds.
- What happens when a video title is very long in the compact card? It should be multi-line but limited to 2 lines with ellipsis truncation.
- What happens on a very small screen (below 360dp width)? Platform chips in the idle screen should wrap to fit, and the nav pill should scale proportionally.
- What happens when the progress percentage reaches 100% before transitioning to complete state? The progress bar fill should reach full width and the percentage should show "100%".
- What happens when download speed is zero or ETA is unknown? Stats should show a dash for unavailable values.

## Clarifications

### Session 2026-03-16

- Q: How should the Extracting state (loading while fetching video info) be styled? → A: Full custom redesign with large spinner matching the downloading screen aesthetic.
- Q: How should the Error state (download/extraction failure) be styled? → A: Mirror the complete screen structure with 88px error circle, error title/message, Retry gradient button + "New Download" text link.

## Requirements

### Functional Requirements

- **FR-001**: App MUST use a fixed custom dark color scheme with SVD design tokens — dynamic color and light mode are removed for this redesign.
- **FR-002**: All typography MUST use two font families: "Plus Jakarta Sans" for headings/display text and "Inter" for body/label text, at the exact sizes, weights, and colors specified per screen.
- **FR-003**: Bottom navigation MUST be a custom pill-shaped component (not M3 NavigationBar) with the exact dimensions, colors, corner radii, and active/inactive states specified.
- **FR-004**: All primary call-to-action buttons (Extract Video, Download, Share, Start Downloading) MUST use a linear gradient fill from `#8B5CF6` to `#7C3AED`.
- **FR-005**: The URL input field MUST contain an inline Paste button on the right side that reads the clipboard and populates the input.
- **FR-006**: Platform chips on the idle screen MUST show an 8px colored dot (ellipse) before the platform name, using each platform's brand color.
- **FR-007**: The downloading screen MUST display progress as a large centered percentage (64sp) with a gradient-filled progress bar below it.
- **FR-008**: Download and complete screens MUST use a compact video card variant (horizontal layout: 72x54 thumbnail + info column) instead of the full vertical card.
- **FR-009**: History list items MUST display a 2-letter platform abbreviation badge overlaid on the thumbnail at the bottom-left corner, colored with the platform's brand color.
- **FR-010**: History list items MUST show format badge (MP4/WEBM) and status badge (Completed/Failed) as inline pill-shaped badges with status-appropriate colors.
- **FR-011**: Failed history items MUST render at 60% opacity with a dash for unavailable file size.
- **FR-012**: The format selection screen MUST show separate Video Quality and Audio Quality chip groups with selected/unselected visual states, plus a format summary bar.
- **FR-013**: The download complete screen MUST show Open and Share action buttons, plus a "New Download" text link.
- **FR-014**: The history screen top bar MUST include search and more-options icon buttons.
- **FR-015**: All surface components (cards, inputs, nav pill, chips) MUST use the specified corner radii, border colors, and padding values from the design.

### Key Entities

- **Design Token**: A named color, font, spacing, or radius value from the SVD palette (e.g., svd-bg, svd-primary, svd-surface). Used app-wide to ensure visual consistency.
- **Platform**: A supported video source (YouTube, Instagram, TikTok, Twitter, Vimeo, Facebook) with associated brand color and 2-letter abbreviation.
- **Format Chip**: A selectable option representing video resolution (1080p, 720p, etc.) or audio bitrate (320kbps, 256kbps, etc.) with selected/unselected visual states.
- **Compact Video Card**: A horizontal card layout showing thumbnail + info used in downloading and complete states, as opposed to the full vertical card used in format selection.

## Success Criteria

### Measurable Outcomes

- **SC-001**: Every screen in the app uses SVD design tokens exclusively — zero instances of Material 3 default colors or dynamic theming remain visible.
- **SC-002**: All 6 redesigned screens (Idle, Format Selection, Downloading, Complete, History With Items, History Empty) visually match the Pencil designs within 2dp tolerance for spacing and sizing.
- **SC-003**: The pill-shaped bottom navigation is visible and functional on every screen, with correct active/inactive tab highlighting.
- **SC-004**: Users can complete the full download flow (paste URL, extract, select format, download, open/share) using the redesigned UI without any usability regressions.
- **SC-005**: Typography consistently uses Plus Jakarta Sans for headings and Inter for body text across all screens.
- **SC-006**: All gradient buttons render with a smooth purple gradient, not a flat color.
- **SC-007**: History items display platform badge overlays on thumbnails for all supported platforms.
- **SC-008**: Failed history items are visually distinct from completed ones (reduced opacity, status badge color).

## Assumptions

- This redesign targets dark mode only. Light mode support is intentionally dropped in favor of a single branded dark theme.
- Dynamic color (Material You) is disabled in favor of the custom SVD palette.
- The "Plus Jakarta Sans" and "Inter" fonts will be bundled with the app as custom font resources.
- The theme toggle button currently in the top bar is removed since there is only one theme.
- The existing M3 TopAppBar on the download screen is replaced with a custom top bar (back button + title) for non-idle states, and no top bar for the idle state.
- The history screen top bar search and more-options buttons reuse existing functionality — no new search or menu features are added, only visual restyling.
- Platform abbreviations are: YT (YouTube), IG (Instagram), TT (TikTok), X (Twitter), VI (Vimeo), FB (Facebook).
- The TikTok platform badge uses black text on the `#69C9D0` background; all other platform badges use white text.
