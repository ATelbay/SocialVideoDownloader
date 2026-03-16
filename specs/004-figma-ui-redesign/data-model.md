# Data Model: UI Redesign — Figma Make Design System

**Date**: 2026-03-16 | **Spec**: [spec.md](spec.md)

This feature does not introduce new database entities or domain models. The "data model" here is the **design token catalog** — the structured set of values that define the visual language.

## Design Token Entities

### ColorTokens

Light and dark palette values. Standard M3 colors are overridden by dynamic color on API 31+; custom colors are not.

| Token | Light | Dark | Dynamic Color Override? |
|-------|-------|------|------------------------|
| primary | #6750A4 | #D0BCFF | Yes |
| onPrimary | #FFFFFF | #381E72 | Yes |
| primaryContainer | #EADDFF | #4F378B | Yes |
| onPrimaryContainer | #21005D | #EADDFF | Yes |
| secondary | #625B71 | #CCC2DC | Yes |
| onSecondary | #FFFFFF | #332D41 | Yes |
| secondaryContainer | #E8DEF8 | #4A4458 | Yes |
| onSecondaryContainer | #1D192B | #E8DEF8 | Yes |
| surface | #FFFBFE | #141218 | Yes |
| onSurface | #1C1B1F | #E6E1E5 | Yes |
| surfaceVariant | #E7E0EC | #49454F | Yes |
| onSurfaceVariant | #49454F | #CAC4D0 | Yes |
| surfaceContainerLowest | #FFFFFF | #0F0D13 | Yes |
| surfaceContainerLow | #F7F2FA | #1D1B20 | Yes |
| surfaceContainer | #F3EDF7 | #211F26 | Yes |
| surfaceContainerHigh | #ECE6F0 | #2B2930 | Yes |
| surfaceContainerHighest | #E6E0E9 | #36343B | Yes |
| outline | #79747E | #938F99 | Yes |
| outlineVariant | #CAC4D0 | #49454F | Yes |
| background | #FFFBFE | #141218 | Yes |
| onBackground | #1C1B1F | #E6E1E5 | Yes |
| error | #B3261E | #F2B8B5 | Yes |
| onError | #FFFFFF | #601410 | Yes |
| errorContainer | #F9DEDC | #8C1D18 | Yes |
| onErrorContainer | #410E0B | #F9DEDC | Yes |
| inverseSurface | #313033 | #E6E1E5 | Yes |
| inverseOnSurface | #F4EFF4 | #313033 | Yes |
| scrim | #000000 | #000000 | No |
| **success** | **#1B873D** | **#6ECF83** | **No** |
| **onSuccess** | **#FFFFFF** | **#003919** | **No** |
| **successContainer** | **#C8F5D5** | **#1B5E20** | **No** |
| **onSuccessContainer** | **#002112** | **#A7F3B7** | **No** |

### PlatformColors

Fixed brand colors, never overridden by dynamic color.

| Platform | Color | Text Color |
|----------|-------|------------|
| YouTube | #FF0000 | #FFFFFF |
| Instagram | #C13584 | #FFFFFF |
| TikTok | #010101 | #FFFFFF |
| Twitter/X | #1DA1F2 | #FFFFFF |
| Vimeo | #1AB7EA | #FFFFFF |
| Facebook | #1877F2 | #FFFFFF |

### ShapeTokens

| Name | Radius | Usage |
|------|--------|-------|
| Small | 10dp | Thumbnails, small badges |
| Medium | 12dp | Buttons, dialogs, search fields |
| Large | 16dp | Cards, inputs, list items |
| ExtraLarge | 20dp | Chips, progress cards, format selectors |
| Full | 24dp | Bottom sheets, delete dialogs |

### SpacingTokens

| Name | Value | Usage |
|------|-------|-------|
| ContentPadding | 16dp | Screen-level horizontal/vertical padding |
| CardPaddingHorizontal | 12-14dp | Inside cards |
| CardPaddingVertical | 12-20dp | Inside cards |
| ListItemGap | 8dp | Between list items |
| ListItemInternalGap | 12dp | Between elements inside a list item |
| SectionGap | 14-16dp | Between sections |
| HeroTopPadding | 28dp | Top padding for hero/idle section |

### TypographyTokens

| Name | Size | Weight | LetterSpacing | Notes |
|------|------|--------|---------------|-------|
| ScreenTitle | 20sp | 700 | 0.2sp | — |
| SectionHeader | 13sp | 600 | 0.8sp | Uppercase |
| Body | 14-15sp | 500 | default | — |
| Caption | 11-12sp | 500 | default | Meta/timestamps |
| BadgeText | 11sp | 700 | default | Status badges |
| PlatformBadge | 9-10sp | 700 | default | Platform pills |

## Shared Component Interfaces

### VideoInfoCard

Displays video metadata in a card layout. Used in download screen (format selection, downloading, complete) and history screen.

**Inputs**:
- `thumbnailUrl: String?` — video thumbnail URL
- `title: String` — video title (2-line clamp)
- `uploaderName: String?` — channel/uploader name
- `duration: Long?` — video duration in seconds (null = hide badge)
- `platformName: String?` — detected platform name
- `platformColor: Color?` — platform brand color

**Variants**:
- Download screen: full-width thumbnail (180dp height), play button overlay
- History screen: compact thumbnail (72×54dp), no overlay

### PlatformBadge

Colored pill showing the detected platform name.

**Inputs**:
- `platformName: String` — e.g. "YouTube", "TikTok"
- `platformColor: Color` — brand color for background

### StatusBadge

Download status indicator with appropriate coloring.

**Inputs**:
- `status: DownloadStatus` — COMPLETED, FAILED, DOWNLOADING, PENDING, QUEUED, CANCELLED

**Visual mapping**:
- COMPLETED → successContainer bg, success text
- FAILED → errorContainer bg, error text
- DOWNLOADING → blue tint bg, blue text, animated spinner dot
- Other → onSurfaceVariant text

### FormatChip

Selectable chip for video/audio format selection.

**Inputs**:
- `label: String` — format description (e.g. "1080p", "720p")
- `selected: Boolean` — selection state
- `onClick: () → Unit` — selection callback

## Theme Mode Persistence

| Field | Type | Storage | Default |
|-------|------|---------|---------|
| themeMode | enum (SYSTEM, LIGHT, DARK) | DataStore Preferences | SYSTEM |

No database migration needed — this uses DataStore, not Room.
