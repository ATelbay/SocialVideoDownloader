# Quickstart: Warm Editorial UI Redesign

**Date**: 2026-03-17
**Feature**: 006-warm-editorial-redesign

## Prerequisites

- Android Studio with Kotlin 2.2.10+
- Space Grotesk Bold font file (TTF, from Google Fonts, SIL OFL license)
- Branch `006-warm-editorial-redesign` checked out

## Build & Run

```bash
# Build debug APK
./gradlew assembleDebug

# Run existing tests (should still pass — no logic changes)
./gradlew test

# Lint check
./gradlew ktlintCheck
```

## Implementation Order

Follow this strict layer order to keep the app compilable at each step:

### Step 1: Font Resources
1. Download `space_grotesk_bold.ttf` from Google Fonts
2. Add to `core/ui/src/main/res/font/`
3. Remove `plus_jakarta_sans_*.ttf` files (do this together with Type.kt update)

### Step 2: Design Tokens (Layer 0)
Update in this order (each file compiles independently):
1. `Color.kt` — Replace all color values, update `SvdColorScheme` to `lightColorScheme`, expand `ExtendedColors`
2. `Type.kt` — Replace `PlusJakartaSans` with `SpaceGrotesk`, update typography scale
3. `Shape.kt` — Replace all `AppShapes` values
4. `Spacing.kt` — Update spacing constants
5. `Theme.kt` — Ensure `SocialVideoDownloaderTheme` uses updated scheme + provides new extended colors

### Step 3: Shared Components (Layer 1)
Update each independently (they depend on tokens from Step 2):
- `GradientButton.kt` — New gradient, corner radius, text style
- `SvdTopBar.kt` — Full redesign to card-like row with action chip
- `PillNavigationBar.kt` — 3 tabs, new colors
- `FormatChip.kt` — Pill shape, new color states
- `StatusBadge.kt` — Pill shape, new semantic colors, rename if desired
- `PlatformBadge.kt` — Icon+label layout
- `VideoInfoCard.kt` — New sizes, radii, colors for both variants

### Step 4: Screen Composables (Layer 2)
- `IdleContent.kt` + `UrlInputContent.kt` — Hero section, input redesign
- `FormatChipsContent.kt` — Section labels, summary bar
- `DownloadProgressContent.kt` — Large percentage, progress card
- `DownloadCompleteContent.kt` — Token-level changes
- `DownloadErrorContent.kt` — Token-level changes
- `ExtractingContent.kt` — Token-level changes
- `DownloadScreen.kt` — Scaffold bg, top bar per state
- `HistoryScreen.kt` + `HistoryListItem.kt` + `HistoryContent.kt` — Full history redesign
- `HistoryEmptyState.kt` — Token-level changes

### Step 5: App Shell (Layer 3)
- `LibraryRoute.kt` — New placeholder composable
- `AppNavHost.kt` — Add Library destination
- `PillNavigationBar.kt` — Already updated in Step 3
- `MainActivity.kt` — Update tab index mapping, system bar styling

## Verification Checklist

After each layer, the app should:
- [ ] Compile without errors
- [ ] Launch without crashes
- [ ] Show the warm light theme (after Layer 0)
- [ ] Display correct component styling (after Layer 1)
- [ ] Match screen layouts per spec (after Layer 2)
- [ ] Navigate between all 3 tabs (after Layer 3)
- [ ] Complete full download flow without regressions
