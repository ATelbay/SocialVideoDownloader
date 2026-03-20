# Feature Specification: Warm Editorial UI Redesign

**Feature Branch**: `006-warm-editorial-redesign`
**Created**: 2026-03-17
**Status**: Draft
**Input**: Redesign entire app UI from dark purple theme to warm, light, editorial design system. Visual-only change — no business logic, navigation, or ViewModel changes.

## User Scenarios & Testing

### User Story 1 - Warm Light Color Scheme & Design Tokens (Priority: P1)

The entire app adopts a warm, light color scheme replacing the current dark purple theme. Every screen, component, and surface uses the new editorial palette with warm neutrals, coral/orange accents, and teal status colors, giving the app a fresh editorial look.

**Why this priority**: The color scheme is the foundation for every visual change. Typography, shapes, and component redesigns all depend on these tokens being in place first.

**Independent Test**: Launch the app on any device. Every surface, text, border, and accent should use the new warm palette. No dark purple or Material 3 default colors should be visible.

**Acceptance Scenarios**:

1. **Given** the app is launched, **When** any screen is displayed, **Then** the background is warm off-white (#F6F3EC), surfaces are near-white (#FFFDFC), and elevated surfaces are warm cream (#FAF6EE).
2. **Given** text is displayed, **When** it is primary text, **Then** it is dark charcoal (#1F2328). Secondary text is muted gray (#5E6672). Placeholder/label text is light gray (#7D8794).
3. **Given** an interactive accent element, **Then** primary color is coral-orange (#F26B3A), strong variant is burnt orange (#D95222), and soft container is peach (#FFE0D2).
4. **Given** borders and dividers, **Then** default borders are warm taupe (#D7D0C4) and strong borders are darker taupe (#B6AA97).
5. **Given** status indicators, **Then** success uses green (#2D9D66) on soft green (#DDF4E8), error uses red (#D9534F) on soft red (#FDE5E3), "ready" uses teal (#1E8C7A) on soft teal (#D9F1EC), and "in progress" uses primary soft (#FFE0D2) with strong primary text (#D95222).
6. **Given** display/headline text, **Then** it uses the Space Grotesk font family. Body and label text uses the Inter font family.

---

### User Story 2 - Three-Tab Pill Navigation Bar (Priority: P1)

The bottom navigation bar is a pill-shaped custom component with three tabs: Download, Library, and History. The Library tab is new and sits between the existing two tabs. The active tab is highlighted with a warm accent fill.

**Why this priority**: The navigation bar is visible on every screen and defines the app's structural identity. Adding the Library tab is a structural change that must be in place early.

**Independent Test**: Navigate between all three tabs. The pill nav should appear on all screens, the active tab should show peach fill with burnt-orange icon/text, and inactive tabs should be muted gray.

**Acceptance Scenarios**:

1. **Given** any screen is displayed, **When** looking at the bottom of the screen, **Then** a pill-shaped navigation bar is visible with 999dp corner radius, surface background (#FFFDFC), 1dp border (#D7D0C4), 4dp internal padding, and 62dp height.
2. **Given** the Download tab is active, **When** viewing the nav bar, **Then** the Download tab has a peach fill (#FFE0D2) with 26dp corner radius, burnt-orange icon (18dp) and label "DOWNLOAD" (10sp, weight 700, letter spacing 0.5, burnt orange #D95222). Library and History tabs show muted gray (#7D8794) icon/text at weight 600.
3. **Given** the Library tab is tapped, **When** the screen transitions, **Then** the Library tab shows peach fill and orange active styling, while other tabs become inactive.
4. **Given** the Library tab is active, **Then** it displays a placeholder empty state screen.
5. **Given** the nav bar container, **Then** it has outer padding of 21dp left/right/bottom and 12dp top.

---

### User Story 3 - Download Idle Screen Redesign (Priority: P1)

The idle/home screen has a centered hero section with a large download icon in a circular peach container, a Space Grotesk headline, a body description, a redesigned URL input with an inline "Paste" chip, platform chips, a gradient extract button, and footer text.

**Why this priority**: This is the entry point and most frequently seen screen. It establishes the warm editorial aesthetic for the entire experience.

**Independent Test**: Open the app fresh. Verify the hero icon, headline, URL input with paste chip, platform chips, gradient button, and footer text all render with warm editorial styling.

**Acceptance Scenarios**:

1. **Given** the idle screen, **When** viewing the hero section, **Then** an 88dp circular container with peach fill (#FFE0D2) contains a 34dp download icon in burnt orange (#D95222). Below: a headline in Space Grotesk 28sp weight 700 (#1F2328), center-aligned within 280dp width. Below: body text in Inter 14sp (#5E6672), center-aligned within 300dp width. Elements have 12dp gap.
2. **Given** the top bar, **Then** it is a card-like row: 52dp tall, 18dp corner radius, cream background (#FAF6EE), 1dp border (#D7D0C4), 14dp horizontal padding. Left: "New download" in Inter 16sp weight 600 (#1F2328). Right: "Tips" action chip (pill shape, peach bg #FFE0D2, 30dp tall, 12dp horizontal padding, Inter 12sp weight 600, burnt orange #D95222). Tapping "Tips" is a no-op placeholder in this redesign (future feature).
3. **Given** the URL input, **Then** it is 56dp tall, 18dp corner radius, surface bg (#FFFDFC), 1dp border (#D7D0C4), 16dp horizontal padding. Left: placeholder "Paste a video link" in 15sp (#7D8794). Right: "Paste" chip (pill shape, cream bg #FAF6EE, 32dp tall, 12dp horizontal padding, Inter 12sp weight 600, #1F2328).
4. **Given** the platform chips row, **Then** each chip is pill-shaped with cream bg (#FAF6EE), 1dp border (#D7D0C4), 10dp vertical / 16dp horizontal padding. Content: platform icon (14dp, #D95222) + label (Inter 13sp weight 600, #1F2328) with 8dp gap. Chips have 10dp gap between them.
5. **Given** the extract button, **Then** it is 52dp tall, 18dp corner radius, full width. Background is a vertical gradient from coral (#F26B3A) to warm yellow (#F2B84B). Content: white download icon 18dp + "EXTRACT VIDEO" label (Inter 14sp weight 700, letter spacing 0.6, white) with 8dp gap, centered.
6. **Given** the footer text below the extract button, **Then** it reads "Supports 1700+ sites" in Inter 13sp weight 500, muted gray (#5E6672), center-aligned.
7. **Given** the content area, **Then** it has 16dp top padding, 24dp horizontal padding, with 24dp gap between major sections.

---

### User Story 4 - Format Selection Screen Redesign (Priority: P2)

After extracting video info, the format selection screen shows a top bar, a full video card with hero thumbnail, quality/format chip selectors, a format summary bar, and a gradient download button — all in the warm editorial style.

**Why this priority**: Second step in the core download flow. Depends on the design token foundation from P1 stories.

**Independent Test**: Extract a video URL and verify the format selection screen renders with the correct full video card, chips, summary bar, and download button.

**Acceptance Scenarios**:

1. **Given** the top bar, **Then** it shows "Select format" on the left and "Back" action chip on the right, with the same top bar styling as the idle screen.
2. **Given** the full video card, **Then** it has 24dp corner radius, surface bg (#FFFDFC), 1dp border (#D7D0C4), 14dp padding. Vertical layout with 14dp gap: hero thumbnail (full-width, 184dp tall, 18dp corner radius, peach bg #FFE0D2 with 26dp play icon in #D95222), title (Inter 17sp weight 700, #1F2328), meta text (Inter 13sp, #5E6672, line height 1.45), chip row (platform chip + quality chip, 10dp gap).
3. **Given** the "AVAILABLE FORMATS" section label, **Then** it is Inter 12sp weight 600, letter spacing 1.0, uppercase, in light gray (#7D8794).
4. **Given** the format chips, **Then** selected chips have pill shape, peach bg (#FFE0D2), no stroke, label Inter 13sp weight 700 burnt orange (#D95222). Unselected chips have pill shape, surface bg (#FFFDFC), 1dp border (#D7D0C4), label Inter 13sp weight 600 charcoal (#1F2328). Padding: 10dp vertical / 16dp horizontal.
5. **Given** the format summary bar, **Then** it has 20dp corner radius, cream bg (#FAF6EE), 1dp border (#D7D0C4), 16dp padding. Vertical layout: label (Inter 12sp weight 600, #7D8794, letter spacing 1, uppercase) + description (Inter 14sp, #1F2328, line height 1.45) with 4dp gap.
6. **Given** the download button, **Then** it matches the gradient button spec from the idle screen with "DOWNLOAD" label.
7. **Given** the content area, **Then** 16dp top padding, 24dp horizontal padding, 20dp gap between sections.

---

### User Story 5 - Downloading Screen Redesign (Priority: P2)

During an active download, the screen shows a compact video card, a large percentage display in Space Grotesk, a progress bar with coral fill on warm track, download stats, and a cancel button — all in the warm editorial style.

**Why this priority**: Active download feedback is essential. Depends on token and component foundations from P1.

**Independent Test**: Start a download and verify the compact card, percentage display, progress bar, and cancel button render per the warm editorial spec.

**Acceptance Scenarios**:

1. **Given** the top bar, **Then** it shows "Downloading" on the left and "Hide" action chip on the right. Tapping "Hide" navigates back to the idle screen while the download continues in the background.
2. **Given** the compact video card, **Then** it has 22dp corner radius, surface bg (#FFFDFC), 1dp border (#D7D0C4), 12dp padding. Horizontal layout: thumbnail (96x72dp, 16dp corner radius, accent/teal bg #1E8C7A with white play icon) + info column (fill, 6dp gap): title (Inter 15sp weight 600, #1F2328, 2-line clamp), meta (Inter 13sp, #5E6672), "In progress" status chip (pill, peach bg #FFE0D2, Inter 12sp weight 600, #D95222). 12dp gap between thumbnail and info.
3. **Given** the progress card, **Then** it has 24dp corner radius, cream bg (#FAF6EE), 1dp border (#D7D0C4), 20dp padding. Vertical layout (16dp gap), center-aligned: percentage (Space Grotesk 62sp weight 700, #D95222), time estimate (Inter 14sp weight 500, #5E6672), progress track (12dp tall, pill radius, warm neutral bg #F0EBE0, coral fill #F26B3A), stats row (space-between: size info left + speed right, both Inter 13sp weight 600, #1F2328).
4. **Given** the cancel button, **Then** it is 48dp tall, 18dp corner radius, full width, transparent bg, 1dp darker taupe border (#B6AA97). Label: Inter 14sp weight 600 (#1F2328), centered.
5. **Given** the content area, **Then** 16dp top padding, 24dp horizontal padding, 20dp gap between sections.

---

### User Story 6 - History Screen Redesign (Priority: P2)

The history screen shows a top bar with "History" title and "Filter" action chip, a search input reusing the URL input component, a scrollable list of history items with thumbnail placeholders and status chips, and a "Start new download" text action.

**Why this priority**: History is the secondary feature tab. Visual consistency with the download screens is important but depends on shared component styling from P1.

**Independent Test**: Navigate to History with existing downloads. Verify top bar, search input, list items with status chips, and text action all use warm editorial styling.

**Acceptance Scenarios**:

1. **Given** the history top bar, **Then** it shows "History" on the left and "Filter" action chip on the right, matching the standard top bar spec.
2. **Given** the search input, **Then** it reuses the URL input component with placeholder "Search downloads" and chip text "Find" instead of "Paste". Search behavior is unchanged from the existing implementation (local filter on title).
3. **Given** a history list item, **Then** it has 22dp corner radius, surface bg (#FFFDFC), 1dp border (#D7D0C4), 12dp padding. Horizontal layout: thumbnail placeholder (72x72dp, 16dp corner radius, warm neutral bg #F0EBE0) + info column (fill, 6dp gap): title (Inter 15sp weight 600, #1F2328, 2-line clamp), meta (Inter 13sp, #5E6672), status chip. 12dp gap between thumbnail and info.
4. **Given** a "Downloaded" history item, **Then** the status chip shows soft green bg (#DDF4E8) with green text (#2D9D66).
5. **Given** a "Failed" history item, **Then** the status chip shows soft red bg (#FDE5E3) with red text (#D9534F).
6. **Given** the "Start new download" text action, **Then** it shows label (Inter 14sp weight 600, #D95222) + arrow-right icon (16dp, #D95222) with 6dp gap. No background.
7. **Given** the list layout, **Then** items have 16dp gap between them. Section gap is 16dp. Padding: 16dp top, 24dp horizontal.

---

### User Story 7 - Updated Typography with Space Grotesk (Priority: P1)

All display, headline, and title text uses the Space Grotesk font family. All body, label, chip, and button text uses Inter. This creates the editorial feel that distinguishes the redesign.

**Why this priority**: Typography is a foundational design token alongside color. Every component depends on the correct fonts being loaded and applied.

**Independent Test**: Navigate through all screens. Verify headlines use Space Grotesk (visible through its distinctive geometric letterforms) and body text uses Inter.

**Acceptance Scenarios**:

1. **Given** the idle screen headline, **Then** it uses Space Grotesk 28sp weight 700 with 1.05 line height.
2. **Given** the progress percentage on the downloading screen, **Then** it uses Space Grotesk 62sp weight 700 in burnt orange (#D95222).
3. **Given** button labels, **Then** they use Inter 14sp weight 700 with 0.6 letter spacing, uppercase.
4. **Given** section labels (e.g., "AVAILABLE FORMATS"), **Then** they use Inter 12sp weight 600 with 1.0 letter spacing, uppercase, in light gray (#7D8794).
5. **Given** nav tab labels, **Then** they use Inter 10sp with 0.5 letter spacing, uppercase. Active: weight 700, inactive: weight 600.

---

### User Story 8 - Updated Shapes & Corner Radii (Priority: P1)

All components use a consistent set of corner radii: 22dp for compact cards, 24dp for full cards, 18dp for controls (buttons, inputs, top bar), 20dp for summary bars, 999dp (pill) for chips and nav outer, 26dp for nav tab items, and 16dp for thumbnails.

**Why this priority**: Shape tokens work alongside color and typography to define the visual language. Inconsistent radii break the editorial feel.

**Independent Test**: Inspect cards, buttons, inputs, chips, and nav bar across all screens for correct corner radii.

**Acceptance Scenarios**:

1. **Given** compact cards (downloading compact card, history items), **Then** they have 22dp corner radius.
2. **Given** full cards (format selection video card, progress card), **Then** they have 24dp corner radius.
3. **Given** controls (buttons, inputs, top bar), **Then** they have 18dp corner radius.
4. **Given** chips (platform, format, status, action), **Then** they have pill shape (999dp corner radius).
5. **Given** the nav bar outer shape, **Then** it has 999dp corner radius. Individual nav tab items have 26dp corner radius.

---

### Edge Cases

- What happens when the Library tab is tapped? It should show a centered empty placeholder screen with warm editorial styling, since the Library feature is not yet implemented.
- What happens when the Space Grotesk font fails to load? The system should fall back to the default sans-serif font, maintaining readability.
- What happens on a device with very large font scaling? Components should still be usable, though exact pixel-perfect matching may vary.
- What happens when existing screens (extracting, complete, error) are displayed? They should inherit the new color tokens, typography, and shape values from the shared theme, even though their layout is not explicitly redesigned.

## Requirements

### Functional Requirements

- **FR-001**: App MUST use a warm, light color scheme with the specified editorial tokens — the previous dark purple theme is fully replaced.
- **FR-002**: All typography MUST use two font families: Space Grotesk for display/headline/title text and Inter for body/label/button text, at the sizes, weights, and colors specified per component.
- **FR-003**: Bottom navigation MUST be a pill-shaped component with three tabs (Download, Library, History) using the exact dimensions, colors, corner radii, and active/inactive states specified.
- **FR-004**: The Library tab MUST be functional as a navigation target, displaying a placeholder empty state screen.
- **FR-005**: All primary call-to-action buttons (Extract, Download) MUST use a vertical gradient fill from coral (#F26B3A) to warm yellow (#F2B84B).
- **FR-006**: The URL input field MUST contain an inline "Paste" chip on the right side for clipboard paste functionality.
- **FR-007**: The URL input component MUST be reusable as a search input on the History screen with configurable placeholder and chip text.
- **FR-008**: Platform chips MUST display a platform-specific icon alongside the platform name in the specified editorial styling.
- **FR-009**: The downloading screen MUST display progress as a large centered percentage (Space Grotesk 62sp) with a coral-filled progress bar on a warm neutral track.
- **FR-010**: Compact video cards (downloading screen) MUST use a horizontal layout: 96x72dp thumbnail + info column with title, meta, and status chip.
- **FR-011**: Full video cards (format selection) MUST use a vertical layout: full-width 184dp hero thumbnail + title, meta, and chip row.
- **FR-012**: History items MUST use a horizontal layout: 72x72dp thumbnail placeholder + info column with title, meta, and status chip.
- **FR-013**: Status chips MUST use semantic colors: green for "Downloaded", red for "Failed", peach/orange for "In progress", teal for "Ready to download".
- **FR-014**: The top bar MUST be a card-like row with screen-specific title and action chip, using cream bg with warm border styling.
- **FR-015**: All surface components (cards, inputs, nav bar, chips, top bar) MUST use the specified corner radii, border colors, and padding values.
- **FR-016**: The format summary bar MUST display a label and description in a cream-toned card with 20dp corner radius.
- **FR-017**: The "Start new download" text action on the History screen MUST display as inline text + arrow-right icon in burnt orange.
- **FR-018**: Space Grotesk font files MUST be bundled with the app as font resources.
- **FR-019**: Existing screen states (extracting, complete, error) MUST inherit the new theme tokens without layout changes.
- **FR-020**: No business logic, ViewModel, or domain/data layer changes are permitted in this redesign. Navigation graph changes are limited to adding the Library placeholder route.

### Key Entities

- **Design Token**: A named color, font, spacing, or shape value from the warm editorial palette. Used app-wide for visual consistency.
- **Navigation Tab**: One of three bottom navigation destinations (Download, Library, History) with associated icon, label, and active/inactive visual states.
- **Component Variant**: A shared UI component (card, chip, input, button) with configurable properties (size, color, content) reused across screens.

## Success Criteria

### Measurable Outcomes

- **SC-001**: Every screen uses the warm editorial color palette exclusively — zero instances of the dark purple theme or Material 3 default colors remain visible.
- **SC-002**: All redesigned screens (Idle, Format Selection, Downloading, History) visually match the specified design within 2dp tolerance for spacing and sizing.
- **SC-003**: The three-tab pill navigation bar is visible and functional on every screen, with correct active/inactive tab highlighting for all three tabs.
- **SC-004**: Users can complete the full download flow (paste URL, extract, select format, download) using the redesigned UI without any functional regressions.
- **SC-005**: Typography consistently uses Space Grotesk for headlines/display and Inter for body/labels across all screens.
- **SC-006**: All gradient buttons render with a smooth coral-to-yellow gradient, not a flat color.
- **SC-007**: The Library tab navigates to a placeholder screen without crashes or errors.
- **SC-008**: History items display correct status chip colors for all status variants (downloaded, failed, in progress, ready).
- **SC-009**: All existing screen states (extracting, complete, error) remain functional with the new theme applied.

## Assumptions

- This redesign targets a single light theme. The previous dark theme is fully replaced, not kept as an alternative.
- Dynamic color (Material You) remains disabled in favor of the custom editorial palette.
- Space Grotesk and Inter fonts will be bundled with the app as font resources.
- The Library tab shows a simple placeholder/empty state screen — no Library feature implementation is included.
- Navigation icons use Material Icons: `Icons.Outlined.Download` (Download tab), `Icons.Outlined.FolderOpen` (Library tab), `Icons.Outlined.History` (History tab).
- The top bar replaces any existing M3 TopAppBar. Each screen has specific title/action chip content.
- Existing screens not explicitly detailed (extracting, complete, error) inherit new theme tokens (colors, fonts, shapes) without layout restructuring.
- The cancel button uses an outline style with strong border, not the red-outlined style from the previous dark theme.
- Platform abbreviations and brand colors remain unchanged from the previous implementation.
