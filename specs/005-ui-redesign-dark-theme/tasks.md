# Tasks: UI Redesign with Custom Dark Theme

**Input**: Design documents from `/specs/005-ui-redesign-dark-theme/`
**Prerequisites**: plan.md (required), spec.md (required), research.md, data-model.md, quickstart.md

**Tests**: Include ViewModel test updates since existing tests reference removed theme parameters (isDarkTheme, onToggleTheme).

**Organization**: Tasks are grouped by user story. US1 (Theme) is merged into Foundational since all other stories depend on it.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3)
- Include exact file paths in descriptions

## Path Conventions

- `app/src/main/...` for app entry points, navigation, and manifest changes
- `feature/<name>/src/main/...` and `feature/<name>/src/test/...` for feature UI, ViewModels, and feature-specific services
- `core/ui/src/main/...` for shared Compose UI and theme primitives
- `src/main/res/values/strings.xml` in the touched module for user-facing strings

---

## Phase 1: Setup

**Purpose**: Add font resources and verify build

- [ ] T001 Download Plus Jakarta Sans font files (SemiBold 600, Bold 700, ExtraBold 800) and place in `core/ui/src/main/res/font/` as `plus_jakarta_sans_semibold.ttf`, `plus_jakarta_sans_bold.ttf`, `plus_jakarta_sans_extrabold.ttf`
- [ ] T002 Download Inter font files (Regular 400, Medium 500, SemiBold 600, Bold 700) and place in `core/ui/src/main/res/font/` as `inter_regular.ttf`, `inter_medium.ttf`, `inter_semibold.ttf`, `inter_bold.ttf`
- [ ] T003 Verify clean build with `./gradlew assembleDebug` — fonts should compile as resources without additional dependencies

---

## Phase 2: Foundational — Theme & Shared Components (US1)

**Purpose**: Replace the entire M3 dynamic color theme with the fixed SVD dark palette, add custom fonts, and create reusable composables that ALL other stories depend on.

**⚠️ CRITICAL**: No user story work can begin until this phase is complete.

### Theme Rewrite (US1)

- [ ] T004 [US1] Rewrite `core/ui/src/main/kotlin/com/socialvideodownloader/core/ui/theme/Color.kt` — Remove all M3 light/dark palettes. Define SVD color constants: SvdBg (#0F0D15), SvdSurface (#1A1726), SvdSurfaceElevated (#241F33), SvdSurfaceBright (#2E2844), SvdPrimary (#8B5CF6), SvdPrimaryEnd (#7C3AED), SvdPrimarySoft (#A78BFA), SvdPrimaryContainer (#2D2150), SvdText (#FFFFFF), SvdTextSecondary (#A09BB0), SvdTextTertiary (#6B6580), SvdBorder (#2E2844), SvdSuccess (#6ECF83), SvdSuccessContainer (#1B3D25), SvdError (#FF6B6B), SvdErrorContainer (#3D1B1B). Create a single `SvdColorScheme` mapping to M3 ColorScheme roles per research.md R2. Keep ExtendedColors with SVD success/error values.
- [ ] T005 [US1] Rewrite `core/ui/src/main/kotlin/com/socialvideodownloader/core/ui/theme/Type.kt` — Define `PlusJakartaSans` FontFamily (from res/font/ TTFs: SemiBold, Bold, ExtraBold) and `Inter` FontFamily (Regular, Medium, SemiBold, Bold). Create Typography mapping: displayLarge → PlusJakartaSans ExtraBold 64sp (progress %), titleLarge → PlusJakartaSans Bold 28sp, titleMedium → PlusJakartaSans SemiBold 18sp, titleSmall → PlusJakartaSans SemiBold 15sp, headlineMedium → PlusJakartaSans Bold 24sp, headlineSmall → PlusJakartaSans Bold 20sp, bodyLarge → Inter SemiBold 16sp, bodyMedium → Inter Medium 14sp, bodySmall → Inter Regular 12sp, labelLarge → Inter SemiBold 15sp, labelMedium → Inter SemiBold 13sp, labelSmall → Inter SemiBold 11sp. Exact weights, sizes, and letter spacing per data-model.md typography tokens.
- [ ] T006 [US1] Rewrite `core/ui/src/main/kotlin/com/socialvideodownloader/core/ui/theme/Theme.kt` — Remove `dynamicColor` parameter, remove light theme branch, remove `isSystemInDarkTheme()` call. Always apply `SvdColorScheme` to MaterialTheme. Keep ExtendedColors with SVD values. Remove `LocalExtendedColors` light variant.
- [ ] T007 [US1] Update `core/ui/src/main/kotlin/com/socialvideodownloader/core/ui/theme/Shape.kt` — Update `AppShapes` to match design token radii: small → 10dp (thumbnails), medium → 12dp (chips, back button), large → 16dp (cards, inputs, CTA buttons), extraLarge → 20dp (full video card), full → 24dp (bottom sheets). Add `pill` → 36dp (nav pill), `pillTab` → 26dp (nav tab), `cardSm` → 14dp (format summary, secondary buttons), `badge` → 6dp (status/format badges), `badgeLg` → 8dp (duration/platform on full card), `progress` → 5dp (progress bar).
- [ ] T008 [US1] Update `core/ui/src/main/kotlin/com/socialvideodownloader/core/ui/tokens/Spacing.kt` — Update spacing constants to match design tokens: ContentPadding → 24dp (idle), ContentPaddingSm → 20dp (non-idle), ContentPaddingHistory → 16dp, SectionGapLg → 32dp, SectionGapMd → 28dp, SectionGap → 24dp, SectionGapSm → 20dp, InnerGap → 12dp, ChipGap → 8dp, NavPillHeight → 62dp, TopBarHeight → 48dp, InputHeight → 56dp, ButtonHeightLg → 52dp, ButtonHeight → 48dp, ThumbnailFullHeight → 180dp, ThumbnailCompactWidth → 72dp, ThumbnailCompactHeight → 54dp, IconCircleLg → 88dp, IconCircleMd → 80dp.

### Shared Components (US1)

- [ ] T009 [P] [US1] Update `core/ui/src/main/kotlin/com/socialvideodownloader/core/ui/tokens/PlatformColors.kt` — Add platform abbreviation mapping (YouTube→"YT", Instagram→"IG", TikTok→"TT", Twitter→"X", Vimeo→"VI", Facebook→"FB"). Add text color mapping (TikTok→Black, all others→White). Add Vimeo color (#1AB7EA) if missing.
- [ ] T010 [P] [US1] Create `core/ui/src/main/kotlin/com/socialvideodownloader/core/ui/components/GradientButton.kt` — Composable accepting onClick, text, icon (optional), enabled. Box with Modifier.fillMaxWidth().height(52dp).clip(RoundedCornerShape(16dp)).background(Brush.verticalGradient(SvdPrimary, SvdPrimaryEnd)). Centered Row with icon (18dp, white) + text (Inter 16sp SemiBold white), 8dp gap. Disabled state: 0.5f alpha.
- [ ] T011 [P] [US1] Create `core/ui/src/main/kotlin/com/socialvideodownloader/core/ui/components/SvdTopBar.kt` — Composable accepting title string, onBack callback (nullable — if null, no back button). Row, height 48dp, horizontal padding 16dp, verticalAlignment center. Back button: 36dp Box, cornerRadius 12dp, SvdSurface fill, arrow-left icon 20dp white. Title: Plus Jakarta Sans 18sp SemiBold white, 12dp start padding from back button.
- [ ] T012 [P] [US1] Update `core/ui/src/main/kotlin/com/socialvideodownloader/core/ui/components/FormatChip.kt` — Restyle: selected state = SvdPrimaryContainer fill, 1.5dp SvdPrimary border, SvdPrimary text Inter 13sp SemiBold. Unselected = SvdSurface fill, 1dp SvdBorder border, white text Inter 13sp Medium. Corner radius 12dp, padding 10dp/16dp (video) or 10dp/14dp (audio). Accept `isAudio` param for padding variant.
- [ ] T013 [P] [US1] Update `core/ui/src/main/kotlin/com/socialvideodownloader/core/ui/components/VideoInfoCard.kt` — Full variant: cornerRadius 20dp, SvdSurface fill, 1dp SvdBorder border. Thumbnail 180dp height, play overlay 44dp circle #00000080, duration badge (cornerRadius 8dp, #000000AA, Inter 11sp SemiBold white) at bottom-left offset 8dp, platform badge (cornerRadius 8dp, brand color, Inter 10sp Bold white) at bottom-right. Info: 14dp top / 16dp horizontal padding, title Plus Jakarta Sans 15sp SemiBold white, uploader Inter 13sp SvdTextSecondary, 4dp gap. Compact variant: Row with 72x54 thumbnail (cornerRadius 10dp) + info column, 12dp gap, card cornerRadius 16dp, SvdSurface fill, 12dp padding, 1dp SvdBorder border. Title: Plus Jakarta Sans 13sp SemiBold white (maxLines 2). Uploader: Inter 12sp SvdTextSecondary.
- [ ] T014 [P] [US1] Update `core/ui/src/main/kotlin/com/socialvideodownloader/core/ui/components/PlatformBadge.kt` — Add an abbreviation variant for history items: cornerRadius 6dp, platform brand color fill, 2-letter text (Inter 8sp Bold, white or black per platform), padding 2dp/6dp. Keep existing full-name variant but restyle with SVD tokens.
- [ ] T015 [P] [US1] Update `core/ui/src/main/kotlin/com/socialvideodownloader/core/ui/components/StatusBadge.kt` — Restyle: cornerRadius 6dp. Completed: SvdSuccessContainer fill, SvdSuccess text. Failed: SvdErrorContainer fill, SvdError text. Downloading: keep animated dot but use SvdPrimary color. Text: Inter 10sp SemiBold. Padding 3dp/8dp. Remove M3 color references.

### String Resources (US1)

- [ ] T016 [P] [US1] Add new string resources to `core/ui/src/main/res/values/strings.xml` — Platform abbreviations: `platform_abbr_youtube`="YT", `platform_abbr_instagram`="IG", `platform_abbr_tiktok`="TT", `platform_abbr_twitter`="X", `platform_abbr_vimeo`="VI", `platform_abbr_facebook`="FB".
- [ ] T017 [P] [US1] Add new string resources to `feature/download/src/main/res/values/strings.xml` — "Extract Video", "SUPPORTED PLATFORMS", "VIDEO QUALITY", "AUDIO QUALITY", "Selected Format", "Downloading video...", "Extracting video info...", "Cancel Download", "Download Complete!", "Saved to your Downloads folder", "New Download", "Cancel", stats labels ("Speed", "ETA", "Size").
- [ ] T018 [P] [US1] Add new string resources to `feature/history/src/main/res/values/strings.xml` — "Start Downloading", "No downloads yet", "Downloaded videos will appear here. Start by pasting a video link!".

**Checkpoint**: Theme, fonts, shared components, and string resources are ready. All subsequent user stories can now begin.

---

## Phase 3: User Story 2 — Pill-Shaped Bottom Navigation (Priority: P1) 🎯 MVP

**Goal**: Replace M3 NavigationBar with custom pill nav, remove SettingsViewModel and theme toggle.

**Independent Test**: Launch app, verify pill nav renders at bottom of both Download and History screens. Tap between tabs — active pill fills purple, inactive is muted. No theme toggle visible.

- [ ] T019 [US2] Create `core/ui/src/main/kotlin/com/socialvideodownloader/core/ui/components/PillNavigationBar.kt` — Composable accepting selectedIndex (0=Download, 1=History) and onSelect callback. Outer container: padding 12dp top, 21dp left/right/bottom. Inner pill: Row, height 62dp, fillMaxWidth, cornerRadius 36dp, SvdSurface fill, 1dp SvdBorder border, 4dp padding. Two tab items each fillMaxWidth weight(1f): Column centered, cornerRadius 26dp. Active: SvdPrimary fill, icon 18dp white, text "DOWNLOAD"/"HISTORY" Inter 10sp SemiBold white, letterSpacing 0.5sp, 4dp gap. Inactive: transparent fill, icon 18dp SvdTextTertiary, text SvdTextTertiary Inter 10sp Medium.
- [ ] T020 [US2] Update `app/src/main/kotlin/com/socialvideodownloader/MainActivity.kt` — Remove `SettingsViewModel` injection, remove `themeMode`/`isDarkTheme`/`onToggleTheme`. Replace M3 `NavigationBar` in Scaffold bottomBar with `PillNavigationBar(selectedIndex, onSelect)`. Remove `SocialVideoDownloaderTheme(darkTheme = darkTheme)` params — just call `SocialVideoDownloaderTheme { ... }`. Remove import of SettingsViewModel.
- [ ] T021 [US2] Delete `app/src/main/kotlin/com/socialvideodownloader/SettingsViewModel.kt`
- [ ] T022 [US2] Update `app/src/main/kotlin/com/socialvideodownloader/navigation/AppNavHost.kt` — Remove `isDarkTheme` and `onToggleTheme` parameters. Update `downloadScreen()` call to not pass theme params. Update `historyScreen()` call accordingly.
- [ ] T023 [US2] Update `feature/download/src/main/kotlin/com/socialvideodownloader/feature/download/navigation/DownloadNavigation.kt` — Remove `isDarkTheme` and `onToggleTheme` parameters from `downloadScreen()` function and its composable destination. Update `DownloadScreen` call.
- [ ] T024 [US2] Update `feature/download/src/main/kotlin/com/socialvideodownloader/feature/download/ui/DownloadScreen.kt` — Remove `isDarkTheme` and `onToggleTheme` parameters. Remove theme toggle IconButton from TopAppBar actions. Replace M3 TopAppBar with SvdTopBar (pass title from state, onBack if not Idle). Update Scaffold background to SvdBg color.
- [ ] T025 [US2] Update ViewModel tests: `feature/download/src/test/kotlin/com/socialvideodownloader/feature/download/ui/DownloadViewModelTest.kt` — Remove any references to theme-related parameters or SettingsViewModel that may exist in test setup.

**Checkpoint**: App launches with pill nav, both tabs work, no theme toggle. All screens use SVD dark background.

---

## Phase 4: User Story 3 — Download Idle Screen Redesign (Priority: P1)

**Goal**: Redesign the idle/home screen with hero section, inline paste URL input, gradient extract button, and platform chips with colored dots.

**Independent Test**: Open app fresh. See hero icon, title, subtitle, URL input with Paste button, Extract Video gradient button, "SUPPORTED PLATFORMS" divider, and 6 platform chips in 2x3 grid.

- [ ] T026 [US3] Rewrite `feature/download/src/main/kotlin/com/socialvideodownloader/feature/download/ui/components/UrlInputContent.kt` — Replace OutlinedTextField with custom Row composable: fillMaxWidth, height 56dp, cornerRadius 16dp, SvdSurface fill, 1dp SvdBorder border, padding 16dp left / 6dp right. Link icon (20dp, SvdTextTertiary) on left. BasicTextField in center (Inter 15sp, white text, SvdTextTertiary placeholder "https://..."). Paste button on right: Row(cornerRadius 12dp, SvdPrimary fill, padding 10dp/14dp) with clipboard-paste icon (16dp, white) + "Paste" text (Inter 13sp SemiBold white), 6dp gap. Paste button reads clipboard on click and calls onUrlChange.
- [ ] T027 [US3] Rewrite `feature/download/src/main/kotlin/com/socialvideodownloader/feature/download/ui/components/IdleContent.kt` — Column, fillMaxWidth, padding 28dp top / 24dp horizontal / 24dp bottom, 28dp gap. Hero: 80dp circle (cornerRadius 40dp, SvdPrimaryContainer) with download icon 36dp SvdPrimary. Title group: "Download Videos" Plus Jakarta Sans 28sp Bold white (centered), subtitle Inter 14sp SvdTextSecondary (centered, fillMaxWidth). UrlInputContent. GradientButton("Extract Video", sparkles icon). Divider: Row with line(SvdBorder 1dp, weight 1f) + "SUPPORTED PLATFORMS" (Inter 11sp Medium SvdTextTertiary, letterSpacing 1sp) + line, 12dp gap. Platform grid: 2 rows of 3 chips. Each chip: Row(cornerRadius 12dp, SvdSurface fill, 1dp SvdBorder border, padding 10dp/16dp) with 8dp circle (platform brand color) + name (Inter 13sp Medium white), 6dp gap. Rows have 10dp gap, centered with justifyContent center. Use FlowRow for the platform chip grid to ensure wrapping on screens narrower than 360dp.

**Checkpoint**: Idle screen renders exactly as designed. URL paste works. Extract button triggers extraction.

---

## Phase 5: User Story 4 — Extracting State (Priority: P2)

**Goal**: Full custom extracting screen with URL bar, large spinner, and cancel button.

**Independent Test**: Paste a URL and tap Extract. See top bar with "Extracting...", read-only URL bar, large purple spinner, status text, and cancel button.

- [ ] T028 [US4] Rewrite `feature/download/src/main/kotlin/com/socialvideodownloader/feature/download/ui/components/ExtractingContent.kt` — Column, fillMaxWidth, padding 8dp top / 20dp horizontal / 20dp bottom, 32dp gap. URL bar: Row(fillMaxWidth, height 56dp, cornerRadius 16dp, SvdSurface fill, 1dp SvdBorder, padding 16dp horizontal) with link icon (20dp SvdTextTertiary) + URL text (Inter 15sp SvdTextSecondary, singleLine, ellipsis). Spinner section: Column(centered, 16dp gap) with CircularProgressIndicator(56dp, SvdPrimary, strokeWidth 4dp) + "Extracting video info..." (Inter 14sp SvdTextSecondary). Cancel button: Box(fillMaxWidth, height 48dp, cornerRadius 14dp, 1.5dp SvdError border) with "Cancel" (Inter 15sp SemiBold SvdError, centered).

---

## Phase 6: User Story 5 — Format Selection Screen (Priority: P2)

**Goal**: Redesigned format selection with full video card, quality chips split by video/audio, format summary bar, and gradient download button.

**Independent Test**: Extract a video URL. See full video card with play overlay and badges, video/audio quality chip groups, format summary bar with size, and gradient Download button.

- [ ] T029 [US5] Rewrite `feature/download/src/main/kotlin/com/socialvideodownloader/feature/download/ui/components/FormatChipsContent.kt` — Column, fillMaxWidth, 20dp gap. VideoInfoCard (full variant). Quality section (12dp gap): "VIDEO QUALITY" label (Inter 11sp SemiBold SvdTextTertiary, letterSpacing 1sp) + horizontal LazyRow of FormatChip(isAudio=false) with 8dp gap. "AUDIO QUALITY" label + LazyRow of FormatChip(isAudio=true) with 8dp gap. Format summary: Row(fillMaxWidth, cornerRadius 14dp, SvdSurfaceElevated fill, padding 14dp/16dp, spaceBetween) — left Column: "Selected Format" (Inter 11sp Medium SvdTextTertiary) + format string (Inter 14sp SemiBold white), 2dp gap — right: size text (Plus Jakarta Sans 16sp Bold SvdPrimary). GradientButton("Download", download icon).

---

## Phase 7: User Story 6 — Downloading Screen (Priority: P2)

**Goal**: Large percentage progress display with gradient progress bar and download stats.

**Independent Test**: Start a download. See compact video card, large "67%" in purple, "Downloading video..." text, gradient progress bar, Speed/ETA/Size stats, and cancel button.

- [ ] T030 [US6] Rewrite `feature/download/src/main/kotlin/com/socialvideodownloader/feature/download/ui/components/DownloadProgressContent.kt` — Column, fillMaxWidth, padding 8dp top / 20dp horizontal / 20dp bottom, 32dp gap between sections. Compact VideoInfoCard. Progress section (Column, centered, 16dp gap): percentage text (Plus Jakarta Sans 64sp ExtraBold SvdPrimary), "Downloading video..." (Inter 14sp SvdTextSecondary), progress bar (Box fillMaxWidth height 10dp cornerRadius 5dp SvdSurfaceElevated) with fill (Box cornerRadius 5dp, Brush.verticalGradient(SvdPrimary, SvdPrimarySoft), width = fillFraction * maxWidth), stats Row(fillMaxWidth, spaceBetween) with 3 columns each Column(centered, 2dp gap): label (Inter 11sp Medium SvdTextTertiary) + value (Plus Jakarta Sans 16sp Bold white). Cancel button: Box(fillMaxWidth, height 48dp, cornerRadius 14dp, 1.5dp SvdError border) + "Cancel Download" (Inter 15sp SemiBold SvdError, centered).

---

## Phase 8: User Story 7 — Download Complete Screen (Priority: P2)

**Goal**: Success screen with compact card, green check circle, Open/Share buttons, and New Download link.

**Independent Test**: Complete a download. See compact card, 88dp green circle with check, "Download Complete!" text, Open (outlined) and Share (gradient) buttons, "New Download" link.

- [ ] T031 [US7] Rewrite `feature/download/src/main/kotlin/com/socialvideodownloader/feature/download/ui/components/DownloadCompleteContent.kt` — Column, fillMaxWidth, padding 8dp top / 20dp horizontal / 20dp bottom, 24dp gap. Compact VideoInfoCard. Success section (Column, centered, 12dp gap): 88dp circle (cornerRadius 44dp, SvdSuccessContainer) with check icon 40dp SvdSuccess, "Download Complete!" (Plus Jakarta Sans 24sp Bold white), "Saved to your Downloads folder" (Inter 14sp SvdTextSecondary). Action buttons (Column, 12dp gap): Open button (Box fillMaxWidth height 48dp cornerRadius 14dp, 1.5dp SvdPrimary border) Row centered: external-link icon 18dp SvdPrimary + "Open" Inter 15sp SemiBold SvdPrimary, 8dp gap. Share button: same dimensions but gradient fill (SvdPrimary→SvdPrimaryEnd), share-2 icon 18dp white + "Share" Inter 15sp SemiBold white. New Download: Row(centered, padding 10dp/16dp) plus icon 16dp SvdPrimary + "New Download" Inter 14sp Medium SvdPrimary, 6dp gap. New Download onClick calls onNewDownload callback which sends ResetToIdle intent to ViewModel, clearing URL and returning to idle hero section.

---

## Phase 9: User Story 8 — Error State Screen (Priority: P2)

**Goal**: Error screen mirroring complete screen structure with error tokens.

**Independent Test**: Trigger a failure. See error circle (red), error title/message, Retry gradient button, and "New Download" link.

- [ ] T032 [US8] Rewrite `feature/download/src/main/kotlin/com/socialvideodownloader/feature/download/ui/components/DownloadErrorContent.kt` — Column, fillMaxWidth, padding 8dp top / 20dp horizontal / 20dp bottom, 24dp gap. Conditional compact VideoInfoCard (only if videoInfo is available). Error section (Column, centered, 12dp gap): 88dp circle (cornerRadius 44dp, SvdErrorContainer) with alert-triangle icon 40dp SvdError, error title (Plus Jakarta Sans 24sp Bold white), error message (Inter 14sp SvdTextSecondary, centered). GradientButton("Retry", refresh-cw icon, height 52dp). New Download: Row(centered, padding 10dp/16dp) plus icon 16dp SvdPrimary + "New Download" Inter 14sp Medium SvdPrimary, 6dp gap. New Download onClick calls onNewDownload callback which sends ResetToIdle intent to ViewModel, clearing URL and returning to idle hero section.

---

## Phase 10: User Story 9 — History Screen with Platform Badge Overlays (Priority: P2)

**Goal**: Redesigned history list with custom top bar, platform badge overlays on thumbnails, format/status badges, and failed-state opacity.

**Independent Test**: Navigate to History with existing downloads. See custom top bar with search/more buttons, list items with platform badge on thumbnail, format + status badges, timestamp/size metadata. Failed items at 60% opacity.

- [ ] T033 [US9] Update `feature/history/src/main/kotlin/com/socialvideodownloader/feature/history/ui/HistoryScreen.kt` — Replace M3 TopAppBar with custom Row: "Download History" (Plus Jakarta Sans 20sp Bold white) on left, search button (36dp, cornerRadius 12dp, SvdSurface, search icon 18dp white) and more button (same, ellipsis-vertical icon) on right with 8dp gap. Bar height 48dp, horizontal padding 16dp. Update Scaffold background to SvdBg. Keep existing search TextField logic but restyle: SvdSurface background, SvdBorder border, white text, SvdTextTertiary placeholder.
- [ ] T034 [US9] Rewrite `feature/history/src/main/kotlin/com/socialvideodownloader/feature/history/ui/HistoryListItem.kt` — Row card: cornerRadius 16dp, SvdSurface fill, 12dp padding, 1dp SvdBorder border. Alpha 0.6f for failed items. Thumbnail: 72x54dp Box, cornerRadius 10dp, Coil image. Platform badge overlay at bottom-start (offset 2dp left, 38dp top): PlatformBadge abbreviation variant. Info column (12dp gap from thumb, 6dp vertical gap): title (Plus Jakarta Sans 13sp SemiBold white, maxLines 2, ellipsis), meta row (6dp gap): format badge (cornerRadius 6dp, SvdSurfaceElevated fill, padding 3dp/8dp, Inter 10sp SemiBold SvdTextSecondary) + StatusBadge. Timestamp row (4dp gap): time (Inter 11sp SvdTextTertiary) + "·" + size (Inter 11sp SvdTextTertiary, or "—" if failed/unavailable).
- [ ] T035 [US9] Update `feature/history/src/main/kotlin/com/socialvideodownloader/feature/history/ui/HistoryContent.kt` — Update LazyColumn: padding 8dp top / 16dp horizontal / 16dp bottom, spacedBy 10dp. Update loading indicator to SvdPrimary. Keep existing search filtering logic.
- [ ] T036 [P] [US9] Update `feature/history/src/main/kotlin/com/socialvideodownloader/feature/history/components/HistoryBottomSheet.kt` — Restyle: SvdSurface containerColor, SvdBorder on dividers, white text, SvdError for delete. Drag handle: SvdTextTertiary alpha 0.5f.
- [ ] T037 [P] [US9] Update `feature/history/src/main/kotlin/com/socialvideodownloader/feature/history/components/HistoryDeleteDialog.kt` — Restyle: SvdSurfaceElevated surface, SvdErrorContainer icon circle, SvdError text for delete action, SvdBorder dividers, white title/body text, SvdPrimary for cancel button.

---

## Phase 11: User Story 10 — History Empty State (Priority: P3)

**Goal**: Centered empty state with icon, title, description, and gradient "Start Downloading" CTA.

**Independent Test**: Clear history or fresh install. See centered layout with calendar-clock icon in circle, "No downloads yet" title, description text, and gradient "Start Downloading" button that navigates to Download tab.

- [ ] T038 [US10] Rewrite `feature/history/src/main/kotlin/com/socialvideodownloader/feature/history/components/HistoryEmptyState.kt` — Column, centered (verticalArrangement Center, horizontalAlignment Center), fillMaxSize, padding 40dp horizontal, 16dp gap. 88dp circle (cornerRadius 44dp, SvdSurfaceElevated) with calendar-clock icon 36dp SvdTextTertiary. "No downloads yet" (Plus Jakarta Sans 20sp Bold white, centered). Description (Inter 14sp SvdTextSecondary, centered, fillMaxWidth). GradientButton variant: cornerRadius 14dp, padding 12dp/24dp (not full-width, wrap content), download icon 16dp white + "Start Downloading" Inter 14sp SemiBold white, 8dp gap. Button onClick navigates to Download tab.

---

## Phase 12: Polish & Cross-Cutting Concerns

**Purpose**: Final consistency pass, test updates, and cleanup.

- [ ] T039 Verify all screens: Run app end-to-end through all 8 states (Idle, Extracting, FormatSelection, Downloading, Complete, Error, History, HistoryEmpty). Screenshot each and compare against Pencil designs for spacing/color accuracy.
- [ ] T040 Run `./gradlew test` — Fix any compilation errors in existing ViewModel tests caused by removed theme parameters or changed composable signatures.
- [ ] T041 Run `./gradlew ktlintCheck` — Fix any lint violations in modified files.
- [ ] T042 [P] Remove unused imports and dead code: Scan all modified files for leftover M3 color references (DarkColorScheme, LightColorScheme, dynamicDarkColorScheme, etc.), unused ExtendedColors variants, and SettingsViewModel imports.
- [ ] T043 [P] Verify edge cases: Long URL clipping in input, long title truncation (2 lines + ellipsis) in compact cards, progress at 0% and 100%, empty stats display ("—" for unavailable values).

---

## Dependencies & Execution Order

### Phase Dependencies

- **Phase 1 (Setup)**: No dependencies — start immediately
- **Phase 2 (Foundational/US1)**: Depends on Phase 1 — **BLOCKS all user stories**
- **Phases 3-11 (User Stories)**: All depend on Phase 2 completion
  - US2 (Pill Nav) and US3 (Idle) can proceed in parallel
  - US4-US8 (Download screens) can all proceed in parallel after Phase 2
  - US9 (History) and US10 (Empty) can proceed in parallel after Phase 2
  - All user stories are independent of each other
- **Phase 12 (Polish)**: Depends on all stories being complete

### User Story Dependencies

- **US1 (Theme)**: Merged into Foundational — no external dependency
- **US2 (Pill Nav)**: Depends on Foundational only — independent of all other stories
- **US3 (Idle Screen)**: Depends on Foundational only — independent
- **US4 (Extracting)**: Depends on Foundational only — independent
- **US5 (Format Selection)**: Depends on Foundational only — independent
- **US6 (Downloading)**: Depends on Foundational only — independent
- **US7 (Complete)**: Depends on Foundational only — independent
- **US8 (Error)**: Depends on Foundational only — independent
- **US9 (History)**: Depends on Foundational only — independent
- **US10 (Empty)**: Depends on Foundational only — independent

### Parallel Opportunities

Once Phase 2 (Foundational) completes, ALL user stories can run in parallel:

**Parallel Group A** (core app shell):
- T019-T025: US2 Pill Nav
- T026-T027: US3 Idle Screen

**Parallel Group B** (download flow screens — each is a separate .kt file):
- T028: US4 Extracting
- T029: US5 Format Selection
- T030: US6 Downloading
- T031: US7 Complete
- T032: US8 Error

**Parallel Group C** (history screens):
- T033-T037: US9 History
- T038: US10 Empty

---

## Parallel Example: Phase 2 (Foundational)

```bash
# These touch different files and can run in parallel:
T009: PlatformColors.kt
T010: GradientButton.kt (NEW)
T011: SvdTopBar.kt (NEW)
T012: FormatChip.kt
T013: VideoInfoCard.kt
T014: PlatformBadge.kt
T015: StatusBadge.kt
T016: core/ui strings.xml
T017: feature/download strings.xml
T018: feature/history strings.xml
```

---

## Implementation Strategy

### MVP First (US1 + US2 + US3)

1. Complete Phase 1: Setup (font files)
2. Complete Phase 2: Foundational (theme + shared components)
3. Complete Phase 3: US2 (Pill Nav) — app navigates with new look
4. Complete Phase 4: US3 (Idle Screen) — primary screen redesigned
5. **STOP and VALIDATE**: App launches with new theme, pill nav, redesigned idle screen
6. Deploy if ready — remaining screens still work, just not yet restyled

### Incremental Delivery

1. Setup + Foundational → Theme foundation ready
2. US2 + US3 → Core experience redesigned (MVP!)
3. US4-US8 → All download flow screens restyled
4. US9-US10 → History screens restyled
5. Polish → Final consistency check

---

## Notes

- [P] tasks = different files, no dependencies
- [Story] label maps task to specific user story for traceability
- Each user story is independently completable and testable
- Commit after each task or logical group
- Stop at any checkpoint to validate story independently
- All 43 tasks touch ONLY presentation layer — no data model, business logic, or use case changes
