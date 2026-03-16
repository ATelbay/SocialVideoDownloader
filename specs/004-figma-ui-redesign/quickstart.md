# Quickstart: UI Redesign — Figma Make Design System

**Branch**: `004-figma-ui-redesign` | **Date**: 2026-03-16

## What This Feature Does

Redesigns the app's visual layer to match the Figma Make design system. All existing functionality (downloading, history, navigation) is preserved — only the composables change.

## Key Files to Understand

| File | Purpose |
|------|---------|
| `core/ui/.../theme/Color.kt` | Light/dark color palettes |
| `core/ui/.../theme/Theme.kt` | Theme composable + dynamic color + custom color extensions |
| `core/ui/.../theme/Type.kt` | Typography scale |
| `core/ui/.../theme/Shape.kt` | Shape radius tiers (NEW) |
| `core/ui/.../tokens/Spacing.kt` | Spacing constants (NEW) |
| `core/ui/.../tokens/PlatformColors.kt` | Platform brand colors (NEW) |
| `core/ui/.../components/` | Shared composables: VideoInfoCard, PlatformBadge, StatusBadge, FormatChip (NEW) |
| `feature/download/.../DownloadScreen.kt` | Download screen entry point — gets Scaffold + TopAppBar |
| `feature/history/.../HistoryScreen.kt` | History screen — dropdown menu → bottom sheet |

## Implementation Order

1. **Design tokens first** — Color.kt, Shape.kt, Type.kt, Spacing.kt, PlatformColors.kt, Theme.kt
2. **Shared components** — VideoInfoCard, PlatformBadge, StatusBadge, FormatChip
3. **Download screen** — Scaffold/TopAppBar, then each state composable
4. **History screen** — card redesign, bottom sheet, empty states, delete dialog
5. **Theme toggle** — DataStore persistence + toggle button in download TopAppBar

## Build & Verify

```bash
./gradlew assembleDebug     # Build after changes
./gradlew ktlintCheck       # Must pass before commit
```

Visual verification: run on emulator, walk through all download states and history interactions in both light and dark themes.

## Constraints

- **DO NOT** modify ViewModels, Intents, UiStates, use cases, or repositories
- **DO NOT** add new dependencies (all required libraries already in the project)
- **DO NOT** change navigation routes or module dependencies
- Custom colors (success, platform) must NOT be overridden by dynamic color
