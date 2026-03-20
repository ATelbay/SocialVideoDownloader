# Implementation Plan: Warm Editorial UI Redesign

**Branch**: `006-warm-editorial-redesign` | **Date**: 2026-03-17 | **Spec**: [spec.md](spec.md)
**Input**: Feature specification from `/specs/006-warm-editorial-redesign/spec.md`

## Summary

Replace the dark purple UI theme with a warm, light editorial design system. This is a visual-only change touching: color tokens, typography (Space Grotesk replaces PlusJakartaSans for headlines), shapes, spacing, all shared composables in `:core:ui`, all screen composables in `:feature:download` and `:feature:history`, and adding a third Library tab to the bottom navigation bar. No business logic, ViewModel, or domain/data layer changes.

## Technical Context

**Language/Version**: Kotlin 2.2.10
**Primary Dependencies**: Jetpack Compose (BOM 2026.03.00), Material 3, Hilt (KSP), Navigation Compose 2.9.7, Coil
**Storage**: N/A (no storage changes)
**Testing**: JUnit5 + MockK + Turbine (existing — no new tests needed for visual-only changes)
**Target Platform**: Android 8.0+ (API 26)
**Project Type**: Mobile app (Android)
**Performance Goals**: N/A (visual-only — no performance-sensitive changes)
**Constraints**: On-device only, no backend, Compose-only (no XML), KSP-only (no kapt), no ViewModel changes
**Scale/Scope**: Personal utility, 4 modules touched (`:core:ui`, `:feature:download`, `:feature:history`, `:app`), ~31 files modified, 1 font file added (space_grotesk_bold.ttf), 3 font files removed

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principle | Status | Notes |
|-----------|--------|-------|
| I. Privacy & Zero Bloat | PASS | No analytics, tracking, or third-party SDKs added |
| II. On-Device Architecture | PASS | No backend or network changes |
| III. Modern Android Stack | PASS | Compose-only UI, KSP-only, fixed branded palette (allowed per v1.1.0) |
| IV. Modular Separation | PASS | Changes stay within existing module boundaries. Library placeholder screen in `:app` navigation |
| V. Minimal Friction UX | PASS | Same tap-count flows preserved. No UX regression |
| VI. Test Discipline | PASS | Visual-only change — no new use cases or ViewModel state transitions to test |
| VII. Simplicity & Focus | PASS | No new abstractions beyond what's needed. Library tab is minimal placeholder |

All gates pass. No violations to justify.

## Project Structure

### Documentation (this feature)

```text
specs/006-warm-editorial-redesign/
├── plan.md              # This file
├── research.md          # Phase 0 output
├── data-model.md        # Phase 1 output (minimal — visual-only feature)
├── quickstart.md        # Phase 1 output
└── tasks.md             # Phase 2 output (/speckit.tasks)
```

### Source Code (files to modify)

```text
core/ui/src/main/
├── kotlin/com/socialvideodownloader/core/ui/
│   ├── theme/
│   │   ├── Color.kt              — Replace all color tokens (dark → warm light)
│   │   ├── Theme.kt              — Switch to light color scheme, update CompositionLocal
│   │   ├── Type.kt               — Replace PlusJakartaSans with SpaceGrotesk, update scale
│   │   └── Shape.kt              — Update all corner radii to new spec values
│   ├── tokens/
│   │   ├── Spacing.kt            — Update spacing/sizing values
│   │   └── PlatformColors.kt     — Unchanged (platform brand colors stay)
│   └── components/
│       ├── GradientButton.kt     — New gradient colors, new corner radius, updated typography
│       ├── PillNavigationBar.kt  — 3 tabs, new colors, new active/inactive styling
│       ├── FormatChip.kt         — Pill shape, new selected/unselected colors
│       ├── PlatformBadge.kt      — Updated to icon+label chip style (was dot+text)
│       ├── StatusBadge.kt        — Pill shape, new semantic colors, new status variants
│       ├── SvdTopBar.kt          — Card-like row with title + action chip (onActionClick callback)
│       ├── TextActionLink.kt     — NEW (moved from NewDownloadLink in :feature:download)
│       └── VideoInfoCard.kt      — Updated colors, radii, sizes for full + compact variants
└── res/font/
    ├── space_grotesk_bold.ttf    — NEW (replaces plus_jakarta_sans usage for headlines)
    ├── inter_regular.ttf         — KEEP
    ├── inter_medium.ttf          — KEEP
    ├── inter_semibold.ttf        — KEEP
    └── inter_bold.ttf            — KEEP

feature/download/src/main/kotlin/.../ui/
├── DownloadScreen.kt             — Update scaffold bg, top bar per state, section gaps
└── components/
    ├── IdleContent.kt            — Hero section, URL input, platform chips, gradient button
    ├── UrlInputContent.kt        — New styling: surface bg, border, paste chip redesign
    ├── FormatChipsContent.kt     — Section labels, summary bar, chip styling
    ├── DownloadProgressContent.kt — Large percentage (SpaceGrotesk), progress card, stats
    ├── DownloadCompleteContent.kt — New theme colors (inherits from tokens)
    ├── DownloadErrorContent.kt   — New theme colors (inherits from tokens)
    ├── ExtractingContent.kt      — New theme colors (inherits from tokens)
    └── NewDownloadLink.kt        — REMOVED (moved to :core:ui as TextActionLink)

feature/history/src/main/kotlin/.../ui/
├── HistoryScreen.kt              — Top bar → SvdTopBar, search input reuse
├── HistoryContent.kt             — Updated list gap/padding
├── HistoryListItem.kt            — 72x72 thumbnail, new card styling, status chips
└── components/
    ├── HistoryEmptyState.kt      — New theme colors (inherits from tokens)
    ├── HistoryBottomSheet.kt     — New theme colors (inherits from tokens)
    └── HistoryDeleteDialog.kt    — New theme colors (inherits from tokens)

app/src/main/kotlin/.../
├── MainActivity.kt               — Update bg color, handle Library tab index
└── navigation/
    ├── AppNavHost.kt             — Add Library placeholder destination
    └── LibraryRoute.kt           — NEW: placeholder route + empty screen
```

### Font Resource Changes

**Remove** (3 files):
- `plus_jakarta_sans_bold.ttf`
- `plus_jakarta_sans_extrabold.ttf`
- `plus_jakarta_sans_semibold.ttf`

**Add** (1 file):
- `space_grotesk_bold.ttf` (only Bold weight needed — spec uses 700 only)

**Keep** (4 files):
- `inter_regular.ttf`, `inter_medium.ttf`, `inter_semibold.ttf`, `inter_bold.ttf`

### String Resource Changes

New/updated strings needed:
- Navigation: `nav_tab_library` ("LIBRARY")
- Top bar titles: per-screen title/action chip text
- Idle: headline, body, placeholder, button text updates
- History: "Search downloads", "Find", "Start new download"

## Implementation Strategy

### Layered Approach (Bottom-Up)

The redesign follows a strict bottom-up dependency order:

1. **Layer 0 — Design Tokens**: Color.kt, Type.kt, Shape.kt, Spacing.kt + font resources
2. **Layer 1 — Shared Components**: All composables in `:core:ui/components/`
3. **Layer 2 — Screen Composables**: `:feature:download` and `:feature:history` screens
4. **Layer 3 — App Shell**: Navigation bar (3 tabs), Library route, MainActivity

Each layer depends only on layers below it. This enables incremental testing: after Layer 0, the app compiles and runs with new colors/fonts on old layouts. After Layer 1, shared components match the spec. After Layer 2, all screens are redesigned. Layer 3 adds the structural change (Library tab).

### Key Design Decisions

1. **Color system**: Replace `SvdColorScheme` (darkColorScheme) with a `lightColorScheme` mapping. Keep `ExtendedColors` data class but add new tokens (surfaceAlt, surfaceStrong, primaryStrong, primarySoft, accentSoft, borderStrong, etc.).

2. **Typography**: Replace `PlusJakartaSans` FontFamily with `SpaceGrotesk` (single Bold weight). Keep `Inter` family unchanged. Update the M3 Typography scale to match spec text styles. Keep `StatsValue` custom style, update to use Inter.

3. **Shapes**: Replace `AppShapes` values to match new spec (card=22dp, cardLg=24dp, control=18dp, summary=20dp, pill=999dp, navTab=26dp, thumbnail=16dp). Remove unused shapes (badge, badgeLg, progress, bottomSheet).

4. **Navigation**: Add `Library` tab between Download and History. Tab indices shift: Download=0, Library=1, History=2. Library navigates to a placeholder composable.

5. **Top bar**: `SvdTopBar` becomes a card-like row (surfaceAlt bg, border, action chip on right) instead of the current minimal row. Accepts `title`, `actionLabel?`, and `onActionClick?` — callers wire the callback to their specific action (popBackStack for "Back", navigate-to-idle for "Hide", null for "Tips" placeholder).

6. **Platform chips**: Change from dot+text to icon+text layout. Platform icons use Lucide equivalents from Material Icons.

7. **Status badges → Status chips**: Rename/restyle from small 6dp-radius badges to pill-shaped 28dp-tall chips with semantic soft backgrounds.

8. **Cancel button**: Changes from red outline (SvdError border) to neutral outline (borderStrong #B6AA97) with foreground text.

9. **System bars**: Light status bar with dark icons to match the light theme. Update `enableEdgeToEdge()` system bar styling.
