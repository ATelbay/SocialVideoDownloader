# Tasks: UI Redesign — Figma Make Design System

**Input**: Design documents from `/specs/004-figma-ui-redesign/`
**Prerequisites**: plan.md, spec.md, research.md, data-model.md

**Tests**: No test tasks — spec explicitly excludes automated UI testing (visual inspection only).

**Organization**: Tasks are grouped by user story to enable independent implementation and testing of each story.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3)
- Include exact file paths in descriptions

---

## Phase 1: Setup

**Purpose**: Create new directories and string resources needed by later phases

- [ ] T001 Create directory structure for new files: `core/ui/src/main/kotlin/com/socialvideodownloader/core/ui/tokens/` and `core/ui/src/main/kotlin/com/socialvideodownloader/core/ui/components/`
- [ ] T002 Add all new user-facing string resources (screen titles, labels, button text, empty state messages) to `core/ui/src/main/res/values/strings.xml` — includes: "Video Downloader", "Select Format", "Downloading", "Complete", "Error", "Download Videos", "Paste any social media URL to get started", "Supported Platforms", "Extracting video info...", "Fetching available formats", "SELECT QUALITY", "Download complete!", "Failed to extract", "New Download", "Download History", "Delete all", "No downloads yet", "No results found", platform names (YouTube, Instagram, TikTok, Twitter/X, Vimeo, Facebook), status labels, delete dialog text, "Also delete file from storage"

---

## Phase 2: Foundational — US1: Design Tokens (Priority: P1)

**Goal**: Establish the complete design token system (colors, shapes, spacing, typography) that all UI components depend on

**Independent Test**: Launch the app — existing screens should render with the new color palette in both light and dark themes. Dynamic color on API 31+ should override M3 colors but not custom success colors.

**⚠️ CRITICAL**: No shared component or screen redesign work can begin until this phase is complete

- [ ] T003 [US1] Rewrite light and dark color palettes with all Figma Make hex values including surfaceContainer variants, and define ExtendedColors data class (success/onSuccess/successContainer/onSuccessContainer) with light and dark instances and LocalExtendedColors CompositionLocal in `core/ui/src/main/kotlin/com/socialvideodownloader/core/ui/theme/Color.kt`
- [ ] T004 [P] [US1] Create 5-tier shape system (Small=10dp, Medium=12dp, Large=16dp, ExtraLarge=20dp, Full=24dp) as a custom Shapes object in `core/ui/src/main/kotlin/com/socialvideodownloader/core/ui/theme/Shape.kt`
- [ ] T005 [P] [US1] Rewrite typography scale with Figma tokens (ScreenTitle 20sp/700/0.2sp, SectionHeader 13sp/600/0.8sp uppercase, Body 14sp/500, Caption 12sp/500, BadgeText 11sp/700, PlatformBadge 10sp/700) using FontFamily.Default in `core/ui/src/main/kotlin/com/socialvideodownloader/core/ui/theme/Type.kt`
- [ ] T006 [P] [US1] Create spacing constants object (ContentPadding=16dp, CardPaddingHorizontal=14dp, CardPaddingVertical=16dp, ListItemGap=8dp, ListItemInternalGap=12dp, SectionGap=16dp, HeroTopPadding=28dp) in `core/ui/src/main/kotlin/com/socialvideodownloader/core/ui/tokens/Spacing.kt`
- [ ] T007 [P] [US1] Create PlatformColors object with brand colors (YouTube=#FF0000, Instagram=#C13584, TikTok=#010101, Twitter/X=#1DA1F2, Vimeo=#1AB7EA, Facebook=#1877F2) and a helper function to resolve platform name to color in `core/ui/src/main/kotlin/com/socialvideodownloader/core/ui/tokens/PlatformColors.kt`
- [ ] T008 [US1] Update Theme.kt to provide ExtendedColors via CompositionLocalProvider (light/dark aware), add MaterialTheme.extendedColors extension accessor, apply new Shapes, and preserve dynamic color support on API 31+ in `core/ui/src/main/kotlin/com/socialvideodownloader/core/ui/theme/Theme.kt`

**Checkpoint**: App compiles and renders with new design tokens. All existing screens use new colors/shapes/typography.

---

## Phase 3: US5 — Shared UI Components (Priority: P1)

**Goal**: Extract reusable composables (VideoInfoCard, PlatformBadge, StatusBadge, FormatChip) into :core:ui so both screens use identical components

**Independent Test**: Components can be verified in Compose Preview with sample data. Both download and history screens reference the same composables.

- [ ] T009 [P] [US5] Create PlatformBadge composable — colored pill (ExtraLarge radius=20dp) with platform name text (PlatformBadge typography 10sp/700), background from PlatformColors, white text, in `core/ui/src/main/kotlin/com/socialvideodownloader/core/ui/components/PlatformBadge.kt`
- [ ] T010 [P] [US5] Create StatusBadge composable — pill badge mapping DownloadStatus to colors: COMPLETED→successContainer/success, FAILED→errorContainer/error, DOWNLOADING→blue tint with animated spinner dot, other→onSurfaceVariant, using BadgeText typography (11sp/700), in `core/ui/src/main/kotlin/com/socialvideodownloader/core/ui/components/StatusBadge.kt`
- [ ] T011 [P] [US5] Create FormatChip composable — selectable chip with ExtraLarge radius (20dp), 2px border, selected state (primary border + primaryContainer bg + weight 700), unselected state (outlineVariant border + surfaceContainerLow bg), in `core/ui/src/main/kotlin/com/socialvideodownloader/core/ui/components/FormatChip.kt`
- [ ] T012 [US5] Create VideoInfoCard composable with two variants controlled by a `compact: Boolean` parameter — full variant: 16dp radius card, surfaceContainerHigh bg, outlineVariant border, full-width thumbnail (180dp height, object-fit cover), duration badge (bottom-right, rgba(0,0,0,0.8), 6dp radius), play button overlay (48dp circle), PlatformBadge, title (14sp/500, 2-line clamp), uploader (12sp, onSurfaceVariant); compact variant: 72×54dp thumbnail (10dp radius), platform badge overlay (bottom-right, 4dp radius), no play button overlay — in `core/ui/src/main/kotlin/com/socialvideodownloader/core/ui/components/VideoInfoCard.kt`

**Checkpoint**: All 4 shared components compile, render in Preview, and are importable from feature modules.

---

## Phase 4: US2 — Redesigned Download Experience (Priority: P1) 🎯 MVP

**Goal**: Rewrite all download screen composables to match Figma designs for all 6 states, add Scaffold with TopAppBar, and animate state transitions

**Independent Test**: Walk through complete download flow (idle → extract → format select → download → complete). Also trigger error state. Verify layout, spacing, colors, and animations match Figma at each stage. Back button resets to idle.

- [ ] T013 [US2] Rewrite DownloadScreen.kt — wrap in Scaffold with custom TopAppBar (surfaceContainer bg, outlineVariant bottom border, state-dependent title, back button visible in non-idle states that dispatches NewDownloadClicked, theme toggle placeholder), replace Column body with AnimatedContent keyed on uiState::class with fadeIn+scaleIn/fadeOut transitionSpec in `feature/download/src/main/kotlin/com/socialvideodownloader/feature/download/DownloadScreen.kt`
- [ ] T014 [US2] Create IdleContent composable — hero section (80dp circular primaryContainer icon container with Download icon 36dp), title "Download Videos" + subtitle, UrlInputContent below, platform auto-detection text, full-width Extract button (primary bg, 14dp radius, disabled=surfaceVariant), "SUPPORTED PLATFORMS" section header (SectionHeader typography, uppercase) + horizontal row of platform chips (ExtraLarge radius, surfaceContainerHigh bg, outlineVariant border) in `feature/download/src/main/kotlin/com/socialvideodownloader/feature/download/components/IdleContent.kt`
- [ ] T015 [US2] Rewrite UrlInputContent.kt — rounded 16dp border, 2px border (primary when focused, outline when empty), surfaceContainerLow bg, floating label, paste button (36×36, primaryContainer bg, Small radius=10dp) inside the trailing icon slot in `feature/download/src/main/kotlin/com/socialvideodownloader/feature/download/components/UrlInputContent.kt`
- [ ] T016 [US2] Create ExtractingContent composable — disabled URL field at 0.7 opacity + spinner card (ExtraLarge radius=20dp, surfaceContainerHigh bg) containing 56dp CircularProgressIndicator (primaryContainer track, primary indicator), "Extracting video info..." title, "Fetching available formats" subtitle, Cancel TextButton in `feature/download/src/main/kotlin/com/socialvideodownloader/feature/download/components/ExtractingContent.kt`
- [ ] T017 [US2] Rewrite FormatChipsContent.kt — replace FlowRow with LazyRow of FormatChip components (from :core:ui), add selected format info bar (secondaryContainer bg, Medium radius=12dp) showing format label + quality + file size, add full-width Download button (same style as Extract) in `feature/download/src/main/kotlin/com/socialvideodownloader/feature/download/components/FormatChipsContent.kt`
- [ ] T018 [US2] Rewrite DownloadProgressContent.kt — progress card (ExtraLarge radius=20dp, surfaceContainerHigh bg) with "Downloading..." label + large percentage (20sp/700, primary color), gradient progress bar (8dp height, 4dp radius, surfaceVariant track, primary→primaryContainer gradient fill using Canvas), speed + ETA labels below (SectionHeader typography for labels, Body typography weight 600 for values), Cancel OutlinedButton (Small radius=10dp, error color text) in `feature/download/src/main/kotlin/com/socialvideodownloader/feature/download/components/DownloadProgressContent.kt`
- [ ] T019 [US2] Rewrite DownloadCompleteContent.kt — VideoInfoCard (full variant), success icon (88dp circle, successContainer bg, CheckCircle 52dp, success color from extendedColors), "Download complete!" (ScreenTitle typography), subtitle, side-by-side buttons: Open (OutlinedButton, Medium radius=12dp) + Share (filled Button, primary), "New Download" TextButton below in `feature/download/src/main/kotlin/com/socialvideodownloader/feature/download/components/DownloadCompleteContent.kt`
- [ ] T020 [US2] Rewrite DownloadErrorContent.kt — error icon (96dp circle, errorContainer bg, AlertCircle/ErrorOutline 56dp, error color), "Failed to extract" (ScreenTitle typography), descriptive error message (Body typography), Retry Button (filled primary, Medium radius=12dp) + "New Download" OutlinedButton in `feature/download/src/main/kotlin/com/socialvideodownloader/feature/download/components/DownloadErrorContent.kt`
- [ ] T021 [US2] Delete VideoInfoContent.kt — all usages now reference the shared VideoInfoCard from :core:ui. Update any remaining imports in DownloadScreen.kt to use the shared component in `feature/download/src/main/kotlin/com/socialvideodownloader/feature/download/components/VideoInfoContent.kt`

**Checkpoint**: Full download flow works end-to-end with new design. All 6 states render correctly with animations. Back button resets to idle.

---

## Phase 5: US3 — Redesigned Download History (Priority: P2)

**Goal**: Rewrite history screen with card-based list items, bottom sheet (replacing dropdown menu), styled empty states, and redesigned delete dialog

**Independent Test**: Navigate to history, verify card layout with badges. Long-press opens bottom sheet (not dropdown). Search works. Delete dialog shows checkbox. Empty states display correctly.

- [ ] T022 [US3] Update HistoryScreen.kt — restyle TopAppBar (surfaceContainer bg, outlineVariant bottom border), restyle search mode input (surfaceContainerHigh bg, Medium radius=12dp), restyle overflow menu (surfaceContainerHighest bg, Medium radius=12dp, "Delete all" in error color), replace HistoryItemMenu dropdown with HistoryBottomSheet triggered by long-press in `feature/history/src/main/kotlin/com/socialvideodownloader/feature/history/HistoryScreen.kt`
- [ ] T023 [US3] Rewrite HistoryListItem.kt — card-style layout (surfaceContainerLow bg, Large radius=16dp, outlineVariant border, 12dp×16dp padding), compact VideoInfoCard thumbnail (72×54dp, Small radius=10dp) with PlatformBadge overlay, title (13sp/600, 2-line clamp), format tag pill (BadgeText typography, surfaceContainerHigh bg, 6dp radius), StatusBadge component, timestamp + file size row (Caption typography) in `feature/history/src/main/kotlin/com/socialvideodownloader/feature/history/components/HistoryListItem.kt`
- [ ] T024 [P] [US3] Update HistoryContent.kt — set LazyColumn item spacing to ListItemGap (8dp), apply ContentPadding (16dp), use updated HistoryListItem composable in `feature/history/src/main/kotlin/com/socialvideodownloader/feature/history/components/HistoryContent.kt`
- [ ] T025 [P] [US3] Create HistoryEmptyState composable — 88dp circle icon container (surfaceContainerHigh bg), Clock icon for no downloads / Search icon for no search results, title (18sp/700), description (Body typography 14sp) in `feature/history/src/main/kotlin/com/socialvideodownloader/feature/history/components/HistoryEmptyState.kt`
- [ ] T026 [US3] Create HistoryBottomSheet composable — ModalBottomSheet (Full radius=24dp top corners, surfaceContainerHigh bg, skipPartiallyExpanded=true), custom drag handle (40×4dp, onSurfaceVariant 50% opacity), item title preview + HorizontalDivider, Share action row (onSurface color), Delete action row (error color) in `feature/history/src/main/kotlin/com/socialvideodownloader/feature/history/components/HistoryBottomSheet.kt`
- [ ] T027 [US3] Rewrite HistoryDeleteDialog.kt — custom Dialog (not AlertDialog): surfaceContainerHigh bg, Full radius=24dp, drop shadow, trash icon in 52dp errorContainer circle, title + description text, "Also delete file from storage" checkbox row (surfaceContainerHighest bg, Medium radius=12dp), Cancel OutlinedButton + Delete Button (error bg) in `feature/history/src/main/kotlin/com/socialvideodownloader/feature/history/components/HistoryDeleteDialog.kt`
- [ ] T028 [US3] Delete HistoryMenus.kt (dropdown menu) — all usages replaced by HistoryBottomSheet. Update HistoryScreen.kt to remove HistoryItemMenu references in `feature/history/src/main/kotlin/com/socialvideodownloader/feature/history/components/HistoryMenus.kt`

**Checkpoint**: History screen fully redesigned. Cards, bottom sheet, empty states, and delete dialog all match Figma. No regressions in search, share, or delete functionality.

---

## Phase 6: US4 — Theme Toggle (Priority: P2)

**Goal**: Add a theme toggle button (Moon/Sun icon) to the download screen TopAppBar that persists the user's light/dark preference via DataStore

**Independent Test**: Tap the toggle — entire app switches themes immediately. Kill and relaunch the app — theme preference is preserved.

- [ ] T029 [US4] Create ThemeMode enum (SYSTEM, LIGHT, DARK) in `core/domain/src/main/kotlin/com/socialvideodownloader/core/domain/model/ThemeMode.kt`
- [ ] T030 [US4] Create SettingsRepository interface with `themeMode: Flow<ThemeMode>` and `suspend fun setThemeMode(mode: ThemeMode)` in `core/domain/src/main/kotlin/com/socialvideodownloader/core/domain/repository/SettingsRepository.kt`
- [ ] T031 [US4] Add DataStore<Preferences> dependency to version catalog and :core:data build.gradle.kts if not already present, create DataStore singleton provider in Hilt module
- [ ] T032 [US4] Create SettingsRepositoryImpl implementing SettingsRepository interface — reads/writes ThemeMode to DataStore stringPreferencesKey("theme_mode"), exposes Flow<ThemeMode> with SYSTEM default, catch block for parse errors, @Inject constructor, in `core/data/src/main/kotlin/com/socialvideodownloader/core/data/repository/SettingsRepositoryImpl.kt`
- [ ] T033 [US4] Create SettingsViewModel — @HiltViewModel, injects SettingsRepository, exposes themeMode as StateFlow<ThemeMode> via stateIn(WhileSubscribed(5000), SYSTEM), fun toggleTheme() that cycles LIGHT↔DARK, in `app/src/main/kotlin/com/socialvideodownloader/app/SettingsViewModel.kt`
- [ ] T034 [US4] Update MainActivity.kt — inject SettingsViewModel via hiltViewModel(), collect themeMode with collectAsStateWithLifecycle(), derive darkTheme boolean from ThemeMode enum, pass darkTheme and onToggleTheme callback (calls viewModel.toggleTheme()) down through NavHost/Scaffold in `app/src/main/kotlin/com/socialvideodownloader/app/MainActivity.kt`
- [ ] T035 [US4] Update Theme.kt — add darkTheme parameter to SocialVideoDownloaderTheme (or rename to AppTheme), wire CompositionLocalProvider for ExtendedColors based on darkTheme flag in `core/ui/src/main/kotlin/com/socialvideodownloader/core/ui/theme/Theme.kt`
- [ ] T036 [US4] Add theme toggle button to download screen TopAppBar — 40×40dp rounded container, Moon icon (dark mode available) / Sun icon (light mode available), animateColorAsState for smooth transition, calls onToggleTheme callback in `feature/download/src/main/kotlin/com/socialvideodownloader/feature/download/DownloadScreen.kt`

**Checkpoint**: Theme toggle works end-to-end. Preference survives app restart. Dynamic color still works independently on API 31+.

---

## Phase 7: Polish & Cross-Cutting Concerns

**Purpose**: Final cleanup, consistency pass, and ktlint compliance

- [ ] T037 Run ktlintCheck and fix any violations across all modified/new files
- [ ] T038 Visual regression check — walk through every download state (idle, extracting, format selection, downloading, complete, error) in both light and dark themes, verify spacing/colors/radius match data-model.md token definitions
- [ ] T039 Visual regression check — walk through history screen (populated list, search, empty states, bottom sheet, delete dialog) in both light and dark themes
- [ ] T040 Test dynamic color on API 31+ emulator — verify M3 palette overridden by wallpaper, success colors and platform badge colors remain fixed

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies — can start immediately
- **Foundational/US1 (Phase 2)**: Depends on Setup — BLOCKS all UI work
- **US5 Shared Components (Phase 3)**: Depends on US1 tokens — BLOCKS screen redesigns
- **US2 Download Screen (Phase 4)**: Depends on US5 shared components
- **US3 History Screen (Phase 5)**: Depends on US5 shared components
- **US4 Theme Toggle (Phase 6)**: Depends on US1 tokens (Phase 2) — can run in parallel with US2/US3
- **Polish (Phase 7)**: Depends on all prior phases

### User Story Dependencies

- **US1 (Design Tokens)**: Foundation — no dependencies on other stories
- **US5 (Shared Components)**: Depends on US1 only
- **US2 (Download Screen)**: Depends on US1 + US5
- **US3 (History Screen)**: Depends on US1 + US5 — **independent of US2, can run in parallel**
- **US4 (Theme Toggle)**: Depends on US1 — **independent of US2/US3/US5, can run in parallel after Phase 2**

### Within Each User Story

- Token files (Color, Shape, Type, Spacing) before Theme.kt integration
- Shared components before screen composables that use them
- Screen scaffold/structure before individual state composables
- Individual state composables can be parallelized (different files)

### Parallel Opportunities

- T004, T005, T006, T007 can all run in parallel (independent token files)
- T009, T010, T011 can run in parallel (independent shared components)
- T024, T025 can run in parallel (independent history composables)
- US2 (Phase 4) and US3 (Phase 5) can run in parallel after Phase 3 completes
- US4 (Phase 6) can run in parallel with US2/US3 after Phase 2 completes

---

## Parallel Example: Phase 2 (Design Tokens)

```bash
# After T003 (Color.kt), launch these 4 in parallel:
Task: "T004 — Create Shape.kt"
Task: "T005 — Rewrite Type.kt"
Task: "T006 — Create Spacing.kt"
Task: "T007 — Create PlatformColors.kt"
# Then T008 (Theme.kt) depends on all above
```

## Parallel Example: Phase 3 (Shared Components)

```bash
# Launch first 3 in parallel:
Task: "T009 — PlatformBadge.kt"
Task: "T010 — StatusBadge.kt"
Task: "T011 — FormatChip.kt"
# Then T012 (VideoInfoCard) which uses PlatformBadge
```

---

## Implementation Strategy

### MVP First (US1 + US5 + US2)

1. Complete Phase 1: Setup
2. Complete Phase 2: US1 Design Tokens (CRITICAL — blocks everything)
3. Complete Phase 3: US5 Shared Components
4. Complete Phase 4: US2 Download Screen Redesign
5. **STOP and VALIDATE**: Full download flow works with new design
6. This is the MVP — the primary screen is fully redesigned

### Incremental Delivery

1. Setup + US1 Tokens + US5 Components → Design system ready
2. Add US2 Download Screen → Test independently → MVP complete
3. Add US3 History Screen → Test independently → Both screens redesigned
4. Add US4 Theme Toggle → Test independently → User preference support
5. Polish → Final consistency and ktlint pass

### Parallel Opportunity (After Phase 3)

Once shared components are done:
- Stream A: US2 (Download Screen) — T013 through T021
- Stream B: US3 (History Screen) — T022 through T028
- Stream C: US4 (Theme Toggle) — T029 through T034 (can start after Phase 2)

---

## Notes

- [P] tasks = different files, no dependencies
- [Story] label maps task to specific user story for traceability
- Each user story should be independently completable and testable
- Commit after each task or logical group
- Stop at any checkpoint to validate story independently
- ViewModels, Intents, UiStates, use cases, and repositories remain UNCHANGED
- All user-facing strings must use string resources (constitution IV)
