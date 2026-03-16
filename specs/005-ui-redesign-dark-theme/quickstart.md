# Quickstart: UI Redesign with Custom Dark Theme

**Branch**: `005-ui-redesign-dark-theme`

## Setup

```bash
git checkout 005-ui-redesign-dark-theme
./gradlew assembleDebug   # Verify clean build
./gradlew test            # Run existing tests
```

## Font Files

Download and place font files before building:

```
core/ui/src/main/res/font/
├── plus_jakarta_sans_semibold.ttf    (weight 600)
├── plus_jakarta_sans_bold.ttf        (weight 700)
├── plus_jakarta_sans_extrabold.ttf   (weight 800)
├── inter_regular.ttf                 (weight 400)
├── inter_medium.ttf                  (weight 500)
├── inter_semibold.ttf                (weight 600)
└── inter_bold.ttf                    (weight 700)
```

Source: Google Fonts (OFL license) — https://fonts.google.com/specimen/Plus+Jakarta+Sans and https://fonts.google.com/specimen/Inter

## Files to Modify (by module)

### core/ui (theme + shared components)
- `theme/Color.kt` — Replace M3 palette with SVD tokens, remove light colors
- `theme/Theme.kt` — Remove dynamic color, remove light/dark branching, single SVD theme
- `theme/Type.kt` — Add FontFamily declarations, map to type slots
- `theme/Shape.kt` — Update radii to match design tokens
- `tokens/Spacing.kt` — Update spacing values to match designs
- `tokens/PlatformColors.kt` — Add abbreviation and text color mapping
- `components/FormatChip.kt` — Restyle with SVD tokens
- `components/PlatformBadge.kt` — Restyle with SVD tokens, add abbreviation variant
- `components/StatusBadge.kt` — Restyle with SVD tokens
- `components/VideoInfoCard.kt` — Restyle full and compact variants
- **NEW** `components/PillNavigationBar.kt` — Custom pill nav
- **NEW** `components/GradientButton.kt` — Gradient CTA button
- **NEW** `components/SvdTopBar.kt` — Custom top bar (back + title)

### app (navigation shell)
- `MainActivity.kt` — Replace NavigationBar with PillNavigationBar, remove theme toggle, remove SettingsViewModel
- `SettingsViewModel.kt` — Delete (no longer needed)
- `navigation/AppNavHost.kt` — Remove isDarkTheme/onToggleTheme params

### feature/download (all screen states)
- `ui/DownloadScreen.kt` — Use SvdTopBar, update AnimatedContent styling
- `ui/components/IdleContent.kt` — Full redesign (hero, input, platforms)
- `ui/components/UrlInputContent.kt` — Redesign with inline paste button
- `ui/components/ExtractingContent.kt` — Full redesign (large spinner)
- `ui/components/FormatChipsContent.kt` — Restyle chips, add summary bar
- `ui/components/DownloadProgressContent.kt` — Large percentage, gradient bar, stats
- `ui/components/DownloadCompleteContent.kt` — Redesign with Open/Share/New
- `ui/components/DownloadErrorContent.kt` — Mirror complete screen with error tokens

### feature/history (list + empty state)
- `ui/HistoryScreen.kt` — Custom top bar with search/more buttons
- `ui/HistoryContent.kt` — Update list styling
- `ui/HistoryListItem.kt` — Platform badge overlay, format/status badges
- `components/HistoryEmptyState.kt` — Redesign with SVD tokens
- `components/HistoryBottomSheet.kt` — Restyle with SVD tokens
- `components/HistoryDeleteDialog.kt` — Restyle with SVD tokens

### String resources (new entries)
- `app/src/main/res/values/strings.xml` — Nav labels
- `feature/download/src/main/res/values/strings.xml` — New UI text
- `core/ui/src/main/res/values/strings.xml` — Platform abbreviations

## Build Verification

After each task:
```bash
./gradlew assembleDebug   # Must compile
./gradlew test            # Existing tests must pass (update as needed)
./gradlew ktlintCheck     # Must have zero violations
```

## Key Design References

- Pencil file: `pencil-welcome-desktop.pen`
- Screenshots: `design/screens/*.png`
- Design tokens: See `data-model.md` for complete token tables
