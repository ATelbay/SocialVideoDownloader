# Implementation Plan: UI Redesign with Custom Dark Theme

**Branch**: `005-ui-redesign-dark-theme` | **Date**: 2026-03-16 | **Spec**: [spec.md](spec.md)
**Input**: Feature specification from `/specs/005-ui-redesign-dark-theme/spec.md`

## Summary

Replace the current Material 3 dynamic-color theme with a fixed custom dark palette using SVD design tokens. Redesign all 8 app states (Idle, Extracting, Format Selection, Downloading, Complete, Error, History, History Empty) to match Pencil designs. Replace M3 NavigationBar with a custom pill-shaped nav. Add custom fonts (Plus Jakarta Sans + Inter), gradient CTA buttons, redesigned URL input, platform chips with dots, large progress display, compact video cards, and history items with platform badge overlays.

**Technical approach**: Keep MaterialTheme as the composition framework but inject a custom SVD-based ColorScheme. Bundle fonts as TTF resources. Build 3 new shared composables (PillNavigationBar, GradientButton, SvdTopBar). Restyle all existing screen composables in-place. Remove SettingsViewModel and theme toggle. No data model or business logic changes.

## Technical Context

**Language/Version**: Kotlin 2.2.10
**Primary Dependencies**: Jetpack Compose (BOM 2026.03.00), Material 3, Hilt (KSP), Navigation Compose 2.9.7, Coil
**Storage**: No changes (Room + MediaStore unchanged)
**Testing**: JUnit5 + MockK + Turbine (existing ViewModel tests need theme-related param updates)
**Target Platform**: Android 8.0+ (API 26), compileSdk/targetSdk 36
**Project Type**: Mobile app (Android)
**Performance Goals**: No performance-impacting changes. Font loading should not add perceptible startup latency.
**Constraints**: On-device only, no backend, no analytics. Fonts bundled as APK resources (no network fetch).
**Scale/Scope**: Personal utility, 2 feature modules + core/ui, 8 UI states, ~30 files modified, 3 new composables, 7 font files added.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principle | Status | Notes |
|-----------|--------|-------|
| I. Privacy & Zero Bloat | PASS | No new dependencies that phone home. Fonts bundled locally. |
| II. On-Device Architecture | PASS | No backend, no network for fonts. |
| III. Modern Android Stack | PASS | Constitution v1.1.0 makes Dynamic Color optional. Fixed SVD palette is compliant. |
| IV. Modular Separation | PASS | Same module structure. New composables in core/ui. No cross-module violations. |
| V. Minimal Friction UX | PASS | No flow changes. Same tap count. |
| VI. Test Discipline | PASS | Existing ViewModel tests updated. No new test requirements (UI reskin). |
| VII. Simplicity & Focus | PASS | UI reskin, no feature additions. SettingsViewModel removal reduces complexity. |

**Post-Phase 1 re-check**: All decisions in research.md and data-model.md maintain compliance. The Dynamic Color violation is justified below.

## Project Structure

### Documentation (this feature)

```text
specs/005-ui-redesign-dark-theme/
├── plan.md              # This file
├── spec.md              # Feature specification
├── research.md          # Phase 0: technical decisions
├── data-model.md        # Phase 1: design token model
├── quickstart.md        # Phase 1: developer setup guide
└── tasks.md             # Phase 2 output (/speckit.tasks)
```

### Source Code (repository root)

```text
core/ui/src/main/
├── kotlin/com/socialvideodownloader/core/ui/
│   ├── theme/
│   │   ├── Color.kt              # SVD token palette (rewrite)
│   │   ├── Theme.kt              # Single dark theme (rewrite)
│   │   ├── Type.kt               # Custom fonts + type scale (rewrite)
│   │   └── Shape.kt              # Updated radii (update)
│   ├── tokens/
│   │   ├── Spacing.kt            # Updated spacing values (update)
│   │   └── PlatformColors.kt     # Add abbreviations + text colors (update)
│   └── components/
│       ├── FormatChip.kt         # Restyle (update)
│       ├── PlatformBadge.kt      # Restyle + abbreviation variant (update)
│       ├── StatusBadge.kt        # Restyle (update)
│       ├── VideoInfoCard.kt      # Restyle full + compact (update)
│       ├── PillNavigationBar.kt  # NEW — custom pill nav
│       ├── GradientButton.kt     # NEW — gradient CTA
│       └── SvdTopBar.kt          # NEW — back + title bar
└── res/
    ├── font/                     # NEW — 7 TTF files
    └── values/strings.xml        # Add platform abbreviation strings

app/src/main/kotlin/com/socialvideodownloader/
├── MainActivity.kt               # Replace NavigationBar, remove theme toggle (update)
├── SettingsViewModel.kt           # DELETE
└── navigation/AppNavHost.kt       # Remove theme params (update)

feature/download/src/main/kotlin/.../ui/
├── DownloadScreen.kt              # Use SvdTopBar (update)
├── components/
│   ├── IdleContent.kt             # Full redesign (rewrite)
│   ├── UrlInputContent.kt         # Inline paste button (rewrite)
│   ├── ExtractingContent.kt       # Large spinner (rewrite)
│   ├── FormatChipsContent.kt      # Quality chips + summary bar (rewrite)
│   ├── DownloadProgressContent.kt # Large %, gradient bar, stats (rewrite)
│   ├── DownloadCompleteContent.kt # Success + Open/Share/New (rewrite)
│   └── DownloadErrorContent.kt    # Error mirror of complete (rewrite)
└── res/values/strings.xml         # New UI text strings

feature/history/src/main/kotlin/.../
├── ui/
│   ├── HistoryScreen.kt           # Custom top bar (update)
│   ├── HistoryContent.kt          # List styling (update)
│   └── HistoryListItem.kt         # Platform badge overlay (rewrite)
├── components/
│   ├── HistoryEmptyState.kt       # Redesign (rewrite)
│   ├── HistoryBottomSheet.kt      # Restyle (update)
│   └── HistoryDeleteDialog.kt     # Restyle (update)
└── res/values/strings.xml         # New UI text strings
```

**Structure Decision**: Use the existing multi-module layout. All new composables land in `core/ui/components/`. No new modules. Font resources in `core/ui/res/font/`. SettingsViewModel deleted from `:app`.

## Complexity Tracking

| Violation | Why Needed | Simpler Alternative Rejected Because |
|-----------|------------|-------------------------------------|
| Principle III: Dropping Dynamic Color | The Pencil designs mandate a single branded dark palette (#0F0D15 bg, #8B5CF6 primary, etc.) for visual consistency across all devices. Dynamic Color produces different palettes per device wallpaper, making it impossible to match the designs. | Keeping Dynamic Color with SVD overrides creates a confusing hybrid where some surfaces use wallpaper-derived colors and others use fixed tokens. The designs are exclusively dark — light mode adds zero value for a personal tool. MaterialTheme is still used as the composition framework, so M3 component compatibility is preserved. |
