# Data Model: Compose Multiplatform Migration

**Branch**: `013-compose-multiplatform-migration` | **Date**: 2026-03-31

This migration does not introduce new data entities (Room, Ktor, etc.). The "data model" here is the **shared design token system** — the unified set of colors, typography, shapes, and spacing values that replace the current dual Android/iOS theme implementations.

## Design Tokens

### Color Palette (unified from Android `ExtendedColors` + iOS `Colors.swift`)

| Token Name | Hex | Source | Usage |
|-----------|-----|--------|-------|
| `SvdBg` | `#F6F3EC` | Both | Primary background (warm off-white) |
| `SvdPrimary` | `#F26B3A` | Both | Primary accent (terracotta) |
| `SvdWarning` | `#F2B84B` | Both | Warning/amber accent |
| `SvdAccent` | `#1E8C7A` | Both | Teal accent |
| `SvdSurface` | `#FFFFFF` | Both | Card/surface background |
| `SvdOnSurface` | `#222222` | Both | Primary text |
| `SvdOnSurfaceVariant` | `#777777` | Both | Secondary text |
| `SvdBorder` | `#E0DDD6` | Android only (new to iOS) | Card borders, dividers |
| `SvdSuccess` | `#2E7D32` | Android only (new to iOS) | Success state |
| `SvdError` | `#D32F2F` | Android only (new to iOS) | Error state |
| `SvdPrimarySoft` | `#FFF0EB` | Android only (new to iOS) | Selected chip background |
| `SvdPrimaryGradientStart` | `#F26B3A` | Android | Gradient button start |
| `SvdPrimaryGradientEnd` | `#F2B84B` | Android | Gradient button end |

**Decision**: Use the full Android `ExtendedColors` set as the canonical palette. iOS previously had a 7-token subset; the shared theme provides all 20+ tokens. This is additive — no iOS colors change, only new ones become available.

**Dynamic Color**: Android-only. Provided via `androidMain` `DynamicColorTheme.kt` wrapper that conditionally applies `dynamicLightColorScheme()` when available, falling back to the static SVD palette.

### Typography Scale

| Token | Font Family | Weight | Size (sp) | Line Height | Usage |
|-------|-------------|--------|-----------|-------------|-------|
| `displayLarge` | SpaceGrotesk | Bold | 32 | 40 | Hero headers |
| `headlineLarge` | SpaceGrotesk | SemiBold | 24 | 32 | Section titles |
| `headlineMedium` | SpaceGrotesk | Medium | 20 | 28 | Card titles |
| `titleMedium` | Inter | SemiBold | 16 | 24 | Subtitles |
| `bodyLarge` | Inter | Regular | 16 | 24 | Body text |
| `bodyMedium` | Inter | Regular | 14 | 20 | Secondary body |
| `labelLarge` | Inter | SemiBold | 14 | 20 | Button text, labels |
| `labelMedium` | Inter | Medium | 12 | 16 | Captions, badges |

**Decision**: Unify to the Android scale (which is the superset). iOS had minor size differences in some tokens (iOS `headlineLarge` was 24pt matching Android's `headlineLarge` at 24sp — these are equivalent). All text uses `sp` units so Dynamic Type / font scaling works on both platforms.

**Font bundling**: SpaceGrotesk and Inter `.ttf` files placed in `shared/ui/src/commonMain/composeResources/font/`. Loaded via `compose.components.resources` type-safe `Res.font.*` accessors.

### Shape Tokens

| Token | Radius (dp) | Usage |
|-------|-------------|-------|
| `card` | 22 | Card containers |
| `cardLg` | 24 | Large cards |
| `control` | 18 | Input fields, buttons |
| `summary` | 20 | Summary cards |
| `pill` | 999 | Pill shapes (nav bar, chips) |
| `navTab` | 26 | Navigation tab backgrounds |
| `thumbnail` | 16 | Video thumbnails |
| `small` | 8 | Small elements |

**Decision**: Use Android `AppShapes` values (superset of iOS `SVDRadius`). iOS had `card=22, control=18, pill=999, small=8` — all matching. Android adds `cardLg`, `summary`, `navTab`, `thumbnail`.

### Spacing Tokens

| Token | Value (dp) | Usage |
|-------|-----------|-------|
| `ScreenPadding` | 24 | Screen edge padding |
| `CardPadding` | 20 | Internal card padding |
| `ItemSpacing` | 16 | Between list items |
| `SectionSpacing` | 24 | Between sections |
| `NavBarHeight` | 62 | Bottom navigation bar |
| `TopBarHeight` | 52 | Top app bar |
| `InputHeight` | 56 | Text input fields |
| `PrimaryButtonHeight` | 52 | Primary action buttons |
| `ChipHeight` | 40 | Format selection chips |
| `ThumbnailSize` | 80 | List item thumbnails |

**Decision**: Use Android `Spacing` object values. iOS views used similar values inline; now they reference shared tokens.

## Component Catalog

Components migrating from `:core:ui` (Android) to `:shared:ui` (commonMain):

| Component | Props | Platform-Specific? | Notes |
|-----------|-------|--------------------|-------|
| `GradientButton` | text, onClick, icon?, enabled | No | Vertical gradient SvdPrimary→SvdWarning |
| `SecondaryButton` | text, onClick, enabled | No | Border-only style |
| `FormatChip` | label, selected, onClick | No | Pill shape, selected=SvdPrimarySoft fill |
| `PillNavigationBar` | selectedIndex, onSelect | No | 3-tab bar (Download/Library/History) |
| `SvdTopBar` | title, actionLabel?, onActionClick? | No | 52dp height |
| `VideoInfoCard` | thumbnailUrl, title, uploader?, duration?, platform?, compact | No | Uses Coil 3 `AsyncImage` |
| `PlatformBadge` | platformName | No | Pill badge with platform color |
| `StatusBadge` | status | No | Animated spinner for DOWNLOADING |
| `TextActionLink` | text, onClick | No | Centered text + arrow icon |

**Decision**: All 9 components can be implemented in `commonMain` with no platform-specific code. `VideoInfoCard` uses Coil 3 `AsyncImage` (KMP). `PlatformBadge` uses `PlatformColors` helper (pure color mapping, no platform APIs).

## Platform Action Interfaces

These are the expect/actual boundaries for platform-specific behavior invoked from shared UI:

| Action | commonMain expect | androidMain actual | iosMain actual |
|--------|------------------|-------------------|---------------|
| Open file | `openFile(uri: String)` | `Intent.ACTION_VIEW` | `UIApplication.openURL` |
| Share file | `shareFile(uri: String)` | `Intent.ACTION_SEND` | `UIActivityViewController` |
| Paste from clipboard | Built-in `LocalClipboard` | — | — |
| Request notification permission | `requestNotificationPermission(): Boolean` | `ActivityResultLauncher` | `UNUserNotificationCenter` |
| Google Sign-In | `triggerSignIn(): Result<AuthResult>` | `CredentialManager` | `GIDSignIn` via cinterop |
| Read shared URL (Share Extension) | `getPendingSharedUrl(): String?` | Intent extras | `NSUserDefaults(suiteName:)` |
