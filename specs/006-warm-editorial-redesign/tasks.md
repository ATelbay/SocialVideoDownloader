# Tasks: Warm Editorial UI Redesign

**Input**: Design documents from `/specs/006-warm-editorial-redesign/`
**Prerequisites**: plan.md (required), spec.md (required), research.md, data-model.md, quickstart.md

**Tests**: No new test tasks — this is a visual-only redesign with no business logic, ViewModel, or use case changes. Existing tests must continue to pass.

**Organization**: Tasks grouped by user story. US1/US7/US8 (design tokens) are combined into Foundational phase since they block all other stories.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US2, US3)
- Include exact file paths in descriptions

## Path Conventions

- `core/ui/src/main/kotlin/com/socialvideodownloader/core/ui/` for theme and shared components
- `core/ui/src/main/res/font/` for font resources
- `core/ui/src/main/res/values/strings.xml` for shared UI strings
- `feature/download/src/main/kotlin/com/socialvideodownloader/feature/download/` for download feature
- `feature/history/src/main/kotlin/com/socialvideodownloader/feature/history/` for history feature
- `app/src/main/kotlin/com/socialvideodownloader/` for navigation and app shell

---

## Phase 1: Setup (Font Resources & Strings)

**Purpose**: Add new font files and string resources needed by the redesign

- [ ] T001 Add `space_grotesk_bold.ttf` to `core/ui/src/main/res/font/` (download from Google Fonts, SIL OFL license)
- [ ] T002 Remove `plus_jakarta_sans_bold.ttf`, `plus_jakarta_sans_extrabold.ttf`, `plus_jakarta_sans_semibold.ttf` from `core/ui/src/main/res/font/` — ⚠️ MUST be done atomically with T005 (Type.kt update) to avoid dangling R.font references
- [ ] T003 [P] Add new string resources for navigation tab label ("LIBRARY"), top bar titles/actions per screen, idle screen text, and history screen text in `core/ui/src/main/res/values/strings.xml`, `feature/download/src/main/res/values/strings.xml`, `feature/history/src/main/res/values/strings.xml`, and `app/src/main/res/values/strings.xml`

---

## Phase 2: Foundational — Design Tokens (US1 + US7 + US8)

**Purpose**: Replace all design tokens (color, typography, shapes, spacing). BLOCKS all user story work.

**Goal**: The app compiles and runs with the new warm light palette, Space Grotesk headlines, updated corner radii, and revised spacing — even though component layouts still use old structure.

**⚠️ CRITICAL**: No user story work can begin until this phase is complete

- [ ] T004 [P] Replace all color tokens in `core/ui/src/main/kotlin/com/socialvideodownloader/core/ui/theme/Color.kt` — switch `SvdColorScheme` from `darkColorScheme` to `lightColorScheme`, replace all raw palette colors (SvdBg→background #F6F3EC, SvdSurface→surface #FFFDFC, SvdPrimary→primary #F26B3A, etc.), expand `ExtendedColors` data class with new tokens (surfaceAlt #FAF6EE, surfaceStrong #F0EBE0, card #FFFDFC, primaryStrong #D95222, primarySoft #FFE0D2, warning #F2B84B, accent #1E8C7A (teal), accentSoft #D9F1EC, mutedForeground #5E6672, subtleForeground #7D8794, border #D7D0C4, borderStrong #B6AA97, success #2D9D66, successSoft #DDF4E8, errorSoft #FDE5E3, shadow), update `SvdExtendedColors` instance and `LocalExtendedColors`
- [ ] T005 [P] Replace PlusJakartaSans with SpaceGrotesk in `core/ui/src/main/kotlin/com/socialvideodownloader/core/ui/theme/Type.kt` — define `SpaceGrotesk` FontFamily (Bold from R.font.space_grotesk_bold), keep `Inter` unchanged, update M3 Typography scale: displayLarge→SpaceGrotesk 62sp 700 (progress %), titleLarge→SpaceGrotesk 28sp 700 (headline), remove headlineMedium/Small from SpaceGrotesk if unused, update bodyLarge/Medium/Small and labelLarge/Medium/Small to Inter per spec text styles, update `StatsValue` to Inter 13sp 600
- [ ] T006 [P] Update all shape values in `core/ui/src/main/kotlin/com/socialvideodownloader/core/ui/theme/Shape.kt` — rename and update `AppShapes` fields: card=22dp, cardLg=24dp, control=18dp, summary=20dp, pill=999dp, navTab=26dp, thumbnail=16dp; remove unused shapes (badge, badgeLg, progress, bottomSheet) or rename them to match new spec tokens
- [ ] T007 [P] Update spacing constants in `core/ui/src/main/kotlin/com/socialvideodownloader/core/ui/tokens/Spacing.kt` — screenPadding=24dp, contentTopPadding=16dp, sectionGapIdle=24dp, sectionGap=20dp, cardInnerPaddingCompact=12dp, cardInnerPaddingFull=14dp, progressCardPadding=20dp, summaryBarPadding=16dp, chipRowGap=10dp, chipPaddingV=10dp, chipPaddingH=16dp, navBarHeight=62dp, navBarPadding values, topBarHeight=52dp, inputHeight=56dp, primaryButtonHeight=52dp, secondaryButtonHeight=48dp, heroIconSize=88dp, thumbnailFullHeight=184dp, thumbnailCompactWidth=96dp, thumbnailCompactHeight=72dp, thumbnailHistorySize=72dp, statusChipHeight=28dp, actionChipHeight=30dp, progressTrackHeight=12dp
- [ ] T008 Update `SocialVideoDownloaderTheme` in `core/ui/src/main/kotlin/com/socialvideodownloader/core/ui/theme/Theme.kt` — ensure it uses the updated `SvdColorScheme` (now lightColorScheme), provides expanded `ExtendedColors` via `LocalExtendedColors`, and applies updated `Typography`

**Checkpoint**: App compiles and launches with warm light colors, Space Grotesk headlines, and new shapes. Components may look partially styled (old layout + new tokens).

---

## Phase 3: Shared Components (Cross-Cutting)

**Purpose**: Update all shared composables in `:core:ui` that are used by multiple user stories. Must complete before screen-level work.

- [ ] T009 [P] Redesign `SvdTopBar` in `core/ui/src/main/kotlin/com/socialvideodownloader/core/ui/components/SvdTopBar.kt` — change from back-button+title row to card-like row: 52dp tall, control shape (18dp), surfaceAlt bg, 1dp border, 14dp horizontal padding; left: title (Inter 16sp 600 foreground); right: action chip (pill, primarySoft bg, 30dp tall, 12dp horizontal padding, Inter 12sp 600 primaryStrong); update parameters to accept title: String, actionLabel: String?, onActionClick: (() -> Unit)? = null; remove onBack parameter — callers wire onActionClick to their specific action (e.g., navController.popBackStack() for "Back", dismiss for "Hide", no-op/null for "Tips")
- [ ] T010 [P] Redesign `GradientButton` in `core/ui/src/main/kotlin/com/socialvideodownloader/core/ui/components/GradientButton.kt` — change gradient from purple (SvdPrimary→SvdPrimaryEnd) to coral-to-yellow (primary #F26B3A → warning #F2B84B), update shape to control (18dp), height to 52dp, text style to Inter 14sp 700 letterSpacing 0.6 UPPERCASE white, icon 18dp white, gap 8dp
- [ ] T011 [P] Redesign `FormatChip` in `core/ui/src/main/kotlin/com/socialvideodownloader/core/ui/components/FormatChip.kt` — change to pill shape (999dp), selected: primarySoft bg, no stroke, Inter 13sp 700 primaryStrong; unselected: surface bg, 1dp border, Inter 13sp 600 foreground; padding 10dp vertical / 16dp horizontal
- [ ] T012 [P] Redesign `StatusBadge` in `core/ui/src/main/kotlin/com/socialvideodownloader/core/ui/components/StatusBadge.kt` — change to pill shape (999dp), 28dp tall, 10dp horizontal padding; update color mapping: COMPLETED→successSoft bg/success text, FAILED→errorSoft bg/error text, DOWNLOADING→primarySoft bg/primaryStrong text, PENDING/QUEUED→accentSoft bg/accent text; update text to Inter 12sp 600; remove AnimatedSpinnerDot or restyle if kept
- [ ] T013 [P] Redesign `PlatformBadge` in `core/ui/src/main/kotlin/com/socialvideodownloader/core/ui/components/PlatformBadge.kt` — change from abbreviation badge to platform chip: pill shape, surfaceAlt bg, 1dp border, 10dp vertical / 16dp horizontal padding; content: platform icon 14dp primaryStrong + label Inter 13sp 600 foreground, 8dp gap; remove abbreviation mode
- [ ] T014 [P] Redesign `VideoInfoCard` in `core/ui/src/main/kotlin/com/socialvideodownloader/core/ui/components/VideoInfoCard.kt` — Full card: 24dp radius (cardLg), surface bg, 1dp border, 14dp padding, 14dp gap; hero thumbnail full-width 184dp tall, 18dp radius, primarySoft bg with primaryStrong play icon 26dp; title Inter 17sp 700 foreground; meta Inter 13sp mutedForeground lineHeight 1.45; chip row with platform chip + quality chip 10dp gap. Compact card: 22dp radius (card), surface bg, 1dp border, 12dp padding; thumbnail 96x72dp 16dp radius, accent/teal bg (#1E8C7A) with white play icon; info column (6dp gap): title Inter 15sp 600 foreground 2-line clamp, meta Inter 13sp mutedForeground, status chip; 12dp gap between thumb and info

**Checkpoint**: All shared components match the warm editorial design spec. Screens still need per-screen layout updates.

---

## Phase 4: User Story 2 — Three-Tab Pill Navigation Bar (Priority: P1)

**Goal**: Bottom navigation has 3 tabs (Download, Library, History) with warm editorial styling and the Library tab navigates to a placeholder screen.

**Independent Test**: Tap each of the 3 tabs. Active tab shows peach fill (#FFE0D2) with burnt-orange icon/text. Library tab shows a placeholder screen.

### Implementation for User Story 2

- [ ] T015 [P] [US2] Redesign `PillNavigationBar` in `core/ui/src/main/kotlin/com/socialvideodownloader/core/ui/components/PillNavigationBar.kt` — add third tab (Library) between Download and History; update icons to download/folder-open/history; outer container: pill shape (999dp), surface bg (#FFFDFC), 1dp border (#D7D0C4), 62dp tall, 4dp internal padding; each tab: 26dp radius navTab shape, icon 18dp + 4dp gap + label Inter 10sp UPPERCASE 0.5 letterSpacing; active: primarySoft bg (#FFE0D2), primaryStrong icon/text (#D95222), weight 700; inactive: transparent, subtleForeground (#7D8794), weight 600; outer padding 21dp sides/bottom + 12dp top; update selectedIndex to handle 3 tabs (0=Download, 1=Library, 2=History)
- [ ] T016 [P] [US2] Create `LibraryRoute` data object and placeholder `LibraryScreen` composable in `app/src/main/kotlin/com/socialvideodownloader/navigation/LibraryRoute.kt` — simple centered text "Library coming soon" with warm editorial styling (background #F6F3EC, text subtleForeground #7D8794)
- [ ] T017 [US2] Add Library destination to `AppNavHost` in `app/src/main/kotlin/com/socialvideodownloader/navigation/AppNavHost.kt` — add `composable<LibraryRoute>` destination between download and history
- [ ] T018 [US2] Update `MainActivity` in `app/src/main/kotlin/com/socialvideodownloader/MainActivity.kt` — update tab index mapping for 3 tabs (Download=0, Library=1, History=2), update selectedIndex derivation from NavDestination, update system bar styling for light theme (dark icons on light background)

**Checkpoint**: Three-tab navigation works. Library tab shows placeholder. Active/inactive styling matches spec.

---

## Phase 5: User Story 3 — Download Idle Screen Redesign (Priority: P1) 🎯 MVP

**Goal**: The idle/home screen shows the warm editorial hero section with peach icon circle, Space Grotesk headline, redesigned URL input with "Paste" chip, platform chips with icons, and coral-to-yellow gradient extract button.

**Independent Test**: Open the app. Verify hero icon (88dp peach circle, burnt-orange download icon), headline (Space Grotesk 28sp), URL input with "Paste" chip, platform chips, gradient button, and footer text all match the warm editorial spec.

### Implementation for User Story 3

- [ ] T019 [P] [US3] Redesign `UrlInputContent` in `feature/download/src/main/kotlin/com/socialvideodownloader/feature/download/ui/components/UrlInputContent.kt` — 56dp tall, control shape (18dp), surface bg (#FFFDFC), 1dp border (#D7D0C4), 16dp horizontal padding; left: placeholder "Paste a video link" Inter 15sp subtleForeground; right: "Paste" chip (pill, surfaceAlt bg #FAF6EE, 32dp tall, 12dp horizontal padding, Inter 12sp 600 foreground #1F2328); remove left link icon; update paste button from primary fill to surfaceAlt chip style
- [ ] T020 [US3] Redesign `IdleContent` in `feature/download/src/main/kotlin/com/socialvideodownloader/feature/download/ui/components/IdleContent.kt` — hero section: 88dp circle primarySoft bg (#FFE0D2) with 34dp download icon primaryStrong (#D95222); headline Space Grotesk 28sp 700 foreground, center-aligned width 280dp; body Inter 14sp mutedForeground, center-aligned width 300dp; 12dp gap between elements; platform chips row with icon+label chips (10dp gap), remove colored dot layout; gradient button with "EXTRACT VIDEO"; footer text Inter 13sp mutedForeground center; 24dp section gaps; 16dp top / 24dp sides padding
- [ ] T021 [US3] Update `DownloadScreen` scaffold and top bar routing in `feature/download/src/main/kotlin/com/socialvideodownloader/feature/download/ui/DownloadScreen.kt` — change scaffold containerColor from SvdBg to background (#F6F3EC); add SvdTopBar per state: Idle → title "New download", actionLabel "Tips", onActionClick = null (no-op placeholder); FormatSelection → title "Select format", actionLabel "Back", onActionClick = navController.popBackStack(); Downloading → title "Downloading", actionLabel "Hide", onActionClick = navigate back to idle (download continues in background); update non-idle padding from 20dp to 24dp horizontal

**Checkpoint**: Idle screen fully matches warm editorial design. Extract flow still functional.

---

## Phase 6: User Story 4 — Format Selection Screen Redesign (Priority: P2)

**Goal**: Format selection screen shows the full video card, section label, format chips, summary bar, and download button in warm editorial style.

**Independent Test**: Extract a video and verify the format selection screen renders with correct full video card, "AVAILABLE FORMATS" label, pill-shaped format chips, cream summary bar, and gradient download button.

### Implementation for User Story 4

- [ ] T022 [US4] Redesign `FormatChipsContent` in `feature/download/src/main/kotlin/com/socialvideodownloader/feature/download/ui/components/FormatChipsContent.kt` — section label: Inter 12sp 600 subtleForeground (#7D8794), letterSpacing 1.0, UPPERCASE; format chips use updated FormatChip (pill shape, new colors from T011); summary bar: summary shape (20dp), surfaceAlt bg (#FAF6EE), 1dp border, 16dp padding, vertical layout (4dp gap): label Inter 12sp 600 subtleForeground letterSpacing 1 UPPERCASE + description Inter 14sp foreground lineHeight 1.45; gradient download button; 20dp section gaps

**Checkpoint**: Format selection screen matches warm editorial design.

---

## Phase 7: User Story 5 — Downloading Screen Redesign (Priority: P2)

**Goal**: Active download screen shows compact video card with "In progress" chip, large Space Grotesk percentage in progress card, coral progress bar, and neutral cancel button.

**Independent Test**: Start a download and verify compact card, progress card (62sp percentage, progress bar, stats row), and cancel button all match warm editorial spec.

### Implementation for User Story 5

- [ ] T023 [US5] Redesign `DownloadProgressContent` in `feature/download/src/main/kotlin/com/socialvideodownloader/feature/download/ui/components/DownloadProgressContent.kt` — wrap in progress card: cardLg shape (24dp), surfaceAlt bg (#FAF6EE), 1dp border, 20dp padding, 16dp gap, center-aligned; percentage: Space Grotesk 62sp 700 primaryStrong (#D95222); time estimate: Inter 14sp 500 mutedForeground; progress track: 12dp tall, pill radius, surfaceStrong bg (#F0EBE0), primary fill (#F26B3A); stats row: space-between, Inter 13sp 600 foreground; cancel button: 48dp, control shape (18dp), transparent bg, 1dp borderStrong (#B6AA97), Inter 14sp 600 foreground (not red)

**Checkpoint**: Downloading screen matches warm editorial design. Cancel button is neutral-toned.

---

## Phase 8: User Story 6 — History Screen Redesign (Priority: P2)

**Goal**: History screen shows card-like top bar with "Filter" action, search input reusing URL input component, history items with 72x72dp thumbnail placeholders and status chips, and "Start new download" text action.

**Independent Test**: Navigate to History with existing downloads. Verify top bar, search input, list items with correct status chips, and text action link all use warm editorial styling.

### Implementation for User Story 6

- [ ] T024 [P] [US6] Redesign `HistoryListItem` in `feature/history/src/main/kotlin/com/socialvideodownloader/feature/history/ui/HistoryListItem.kt` — card shape (22dp), surface bg (#FFFDFC), 1dp border (#D7D0C4), 12dp padding; thumbnail placeholder 72x72dp (not 72x54), thumbnail shape (16dp), surfaceStrong bg (#F0EBE0); info column (6dp gap): title Inter 15sp 600 foreground 2-line clamp, meta Inter 13sp mutedForeground, status chip (pill shape from T012); 12dp gap between thumbnail and info; remove platform badge abbreviation from thumbnail; remove opacity reduction for failed items
- [ ] T025 [P] [US6] Move `NewDownloadLink` from `feature/download/.../components/` to `core/ui/src/main/kotlin/com/socialvideodownloader/core/ui/components/TextActionLink.kt` — rename to `TextActionLink` (generic shared component); change to text action style: label Inter 14sp 600 primaryStrong (#D95222) + arrow-right icon 16dp primaryStrong, 6dp gap, no background fill; accept `text: String` and `onClick` params; update usages in both `:feature:download` and `:feature:history` to import from `:core:ui`
- [ ] T026 [US6] Redesign `HistoryScreen` top bar and search in `feature/history/src/main/kotlin/com/socialvideodownloader/feature/history/ui/HistoryScreen.kt` — replace inline custom top bar with `SvdTopBar(title="History", actionLabel="Filter", onActionClick=...)` for default mode; replace search TextField with reusable URL input component variant (placeholder "Search downloads", chip text "Find"); remove overflow menu icon from top bar; update scaffold containerColor to background (#F6F3EC)
- [ ] T027 [US6] Update `HistoryContent` list layout in `feature/history/src/main/kotlin/com/socialvideodownloader/feature/history/ui/HistoryContent.kt` — list item gap from 10dp to 16dp; section gap 16dp; padding 16dp top, 24dp horizontal; add "Start new download →" text action below list using updated NewDownloadLink
- [ ] T028 [US6] Update `HistoryEmptyState` in `feature/history/src/main/kotlin/com/socialvideodownloader/feature/history/components/HistoryEmptyState.kt` — update to warm editorial token colors: surfaceStrong bg for icon circle, subtleForeground icon tint, foreground title, mutedForeground subtitle, gradient button with new coral-to-yellow gradient

**Checkpoint**: History screen fully matches warm editorial design. Status chips show correct semantic colors.

---

## Phase 9: Polish & Cross-Cutting Concerns

**Purpose**: Theme inheritance for non-redesigned screens, cleanup, validation

- [ ] T029 [P] Update `DownloadCompleteContent` in `feature/download/src/main/kotlin/com/socialvideodownloader/feature/download/ui/components/DownloadCompleteContent.kt` — replace dark purple token references with warm editorial equivalents: success circle uses successSoft/success colors, open button uses primaryStrong outline, share button uses new gradient, text uses foreground/mutedForeground
- [ ] T030 [P] Update `DownloadErrorContent` in `feature/download/src/main/kotlin/com/socialvideodownloader/feature/download/ui/components/DownloadErrorContent.kt` — replace dark purple token references with warm editorial equivalents: error circle uses errorSoft/error colors, retry button uses new gradient, text uses foreground/mutedForeground
- [ ] T031 [P] Update `ExtractingContent` in `feature/download/src/main/kotlin/com/socialvideodownloader/feature/download/ui/components/ExtractingContent.kt` — replace dark purple token references: URL bar uses surface bg/border, spinner uses primary color, cancel button uses neutral borderStrong style (not red), text uses mutedForeground
- [ ] T032 [P] Update `HistoryBottomSheet` in `feature/history/src/main/kotlin/com/socialvideodownloader/feature/history/components/HistoryBottomSheet.kt` — replace dark purple token references with warm editorial equivalents: surface bg, border, foreground text, error color for delete
- [ ] T033 [P] Update `HistoryDeleteDialog` in `feature/history/src/main/kotlin/com/socialvideodownloader/feature/history/components/HistoryDeleteDialog.kt` — replace dark purple token references with warm editorial equivalents: surfaceAlt bg, error colors, border, foreground text
- [ ] T034 Run `./gradlew test` to verify all existing unit tests pass with no regressions
- [ ] T035 Run `./gradlew ktlintCheck` and fix any lint violations
- [ ] T036 Run `./gradlew assembleDebug` and verify full app flow: idle → extract → format select → download → complete; navigate all 3 tabs; check history screen with items

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies — can start immediately
- **Foundational (Phase 2)**: Depends on Phase 1 (font files must exist for Type.kt) — BLOCKS all user stories
- **Shared Components (Phase 3)**: Depends on Phase 2 (tokens must be defined) — BLOCKS screen-level work
- **User Stories (Phases 4-8)**: All depend on Phase 3 completion
  - US2 (Nav bar) and US3 (Idle) can run in parallel
  - US4, US5, US6 can run in parallel after Phase 3
  - US4 depends on FormatChip (T011) and VideoInfoCard (T014) from Phase 3
  - US5 depends on VideoInfoCard (T014) and StatusBadge (T012) from Phase 3
  - US6 depends on StatusBadge (T012) and SvdTopBar (T009) from Phase 3
- **Polish (Phase 9)**: Depends on Phase 2 (token references only, no layout dependencies on other phases)

### User Story Dependencies

- **US2 (Nav Bar, P1)**: After Phase 3 — independent of other stories
- **US3 (Idle, P1)**: After Phase 3 — independent of other stories
- **US4 (Format Selection, P2)**: After Phase 3 — independent of other stories
- **US5 (Downloading, P2)**: After Phase 3 — independent of other stories
- **US6 (History, P2)**: After Phase 3 — independent of other stories (uses TextActionLink from :core:ui)

### Parallel Opportunities

- T004, T005, T006, T007 (all token files) can run in parallel
- T009–T014 (all shared components) can run in parallel
- T015, T016 (nav bar + library route) can run in parallel
- T019, T020 can run in parallel (different files in idle screen)
- T024, T025 can run in parallel (different files in history)
- T029–T033 (all polish tasks) can run in parallel
- After Phase 3, all user stories (Phases 4–8) can proceed in parallel

---

## Parallel Example: Phase 2 (Foundational)

```
# All token files are independent — launch in parallel:
Task T004: "Replace color tokens in Color.kt"
Task T005: "Replace typography in Type.kt"
Task T006: "Update shapes in Shape.kt"
Task T007: "Update spacing in Spacing.kt"

# Then T008 (Theme.kt) depends on all above
```

## Parallel Example: Phase 3 (Shared Components)

```
# All components depend only on Phase 2 tokens — launch in parallel:
Task T009: "Redesign SvdTopBar"
Task T010: "Redesign GradientButton"
Task T011: "Redesign FormatChip"
Task T012: "Redesign StatusBadge"
Task T013: "Redesign PlatformBadge"
Task T014: "Redesign VideoInfoCard"
```

---

## Implementation Strategy

### MVP First (Phases 1-5: Setup + Tokens + Components + Nav + Idle)

1. Complete Phase 1: Setup (font files + strings)
2. Complete Phase 2: Foundational (all design tokens)
3. Complete Phase 3: Shared Components
4. Complete Phase 4: Navigation Bar (3 tabs)
5. Complete Phase 5: Idle Screen
6. **STOP and VALIDATE**: The most visible screen (idle) is fully redesigned, nav bar has 3 tabs, entire app uses warm light theme
7. Deploy/demo if ready

### Incremental Delivery

1. Setup + Foundational + Components → Warm light theme everywhere, components styled
2. Add Nav Bar (US2) + Idle Screen (US3) → MVP! Primary experience redesigned
3. Add Format Selection (US4) → Core download flow fully styled
4. Add Downloading (US5) → Active download experience styled
5. Add History (US6) → Secondary tab fully styled
6. Polish → Complete/Error/Extracting states + cleanup

### Parallel Execution (Worktree Agents)

After Phase 3 completes:
- Agent A: Phase 4 (Nav Bar) + Phase 5 (Idle Screen)
- Agent B: Phase 6 (Format Selection) + Phase 7 (Downloading)
- Agent C: Phase 8 (History) + Phase 9 (Polish)

---

## Notes

- [P] tasks = different files, no dependencies
- [Story] label maps task to specific user story for traceability
- No test tasks generated — visual-only redesign, existing tests must still pass (T034)
- Cancel button changes from red outline to neutral borderStrong in both downloading and extracting states
- Platform chips change from dot+text to icon+text layout
- History items change from 72x54dp thumbnails to 72x72dp square thumbnails
- PlusJakartaSans font files MUST be removed (T002) atomically with Type.kt update (T005) to avoid dangling R.font references — execute as a single commit
- Commit after each task or logical group per convention
