# Data Model: UI Redesign with Custom Dark Theme

**Date**: 2026-03-16 | **Branch**: `005-ui-redesign-dark-theme`

## Overview

This feature is a UI-only redesign. No database schema changes, no new entities, no data migrations. All changes are in the presentation layer (colors, typography, components, layouts).

## Design Token Model

The SVD design token palette defines the visual identity. These are not persisted in the database — they are compile-time constants in the theme layer.

### Color Tokens

| Token | Hex | Usage |
|-------|-----|-------|
| svd-bg | #0F0D15 | Screen background |
| svd-surface | #1A1726 | Cards, inputs, nav pill |
| svd-surface-elevated | #241F33 | Format summary, progress track, elevated surfaces |
| svd-surface-bright | #2E2844 | Borders, dividers, chip outlines |
| svd-primary | #8B5CF6 | Accent color, active states, gradient start |
| svd-primary-end | #7C3AED | Gradient end color for CTA buttons |
| svd-primary-soft | #A78BFA | Progress bar gradient end |
| svd-primary-container | #2D2150 | Hero icon bg, selected chip bg |
| svd-text | #FFFFFF | Primary text |
| svd-text-secondary | #A09BB0 | Secondary text, subtitles |
| svd-text-tertiary | #6B6580 | Hints, placeholders, labels |
| svd-border | #2E2844 | All borders and outlines |
| svd-success | #6ECF83 | Success icon, completed badge text |
| svd-success-container | #1B3D25 | Success circle bg, completed badge bg |
| svd-error | #FF6B6B | Error icon, failed badge text, cancel text |
| svd-error-container | #3D1B1B | Error circle bg, failed badge bg |

### Platform Brand Colors

| Platform | Abbreviation | Color | Text Color |
|----------|-------------|-------|------------|
| YouTube | YT | #FF0000 | White |
| Instagram | IG | #C13584 | White |
| TikTok | TT | #69C9D0 | Black |
| Twitter | X | #1DA1F2 | White |
| Vimeo | VI | #1AB7EA | White |
| Facebook | FB | #1877F2 | White |

### Typography Tokens

| Role | Font Family | Weight | Usage |
|------|------------|--------|-------|
| Display | Plus Jakarta Sans | 800 | Large progress percentage (64sp) |
| Heading Large | Plus Jakarta Sans | 700 | Screen titles (28sp, 24sp, 20sp) |
| Heading Medium | Plus Jakarta Sans | 600 | Top bar titles (18sp), video titles (15sp, 13sp) |
| Heading Small | Plus Jakarta Sans | 700 | Stats values (16sp) |
| Body Large | Inter | 600 | Button text (16sp, 15sp) |
| Body Medium | Inter | 500 | Subtitle text (14sp, 13sp) |
| Body Small | Inter | 500/600 | Labels, badges, metadata (11sp, 10sp, 8sp) |
| Caption | Inter | 400 | Uploader names (13sp, 12sp) |

### Spacing Tokens

| Token | Value | Usage |
|-------|-------|-------|
| content-padding | 24dp | Idle screen horizontal padding |
| content-padding-sm | 20dp | Non-idle screen padding |
| content-padding-history | 16dp | History list padding |
| section-gap-lg | 32dp | Major section gap (download/extracting) |
| section-gap-md | 28dp | Idle screen section gap |
| section-gap | 24dp | Complete/error section gap |
| section-gap-sm | 20dp | Format selection section gap |
| inner-gap | 12dp | Within cards, between badges |
| chip-gap | 8dp | Between chips |
| nav-pill-height | 62dp | Bottom nav height |
| nav-pill-padding | 4dp | Internal padding |
| top-bar-height | 48dp | Custom top bar |
| input-height | 56dp | URL input |
| button-height-lg | 52dp | Primary CTA buttons |
| button-height | 48dp | Secondary buttons (cancel, open) |
| thumbnail-full-height | 180dp | Format selection thumbnail |
| thumbnail-compact | 72x54dp | Compact card thumbnail |
| icon-circle-lg | 88dp | Success/error/empty indicator |
| icon-circle-md | 80dp | Hero icon on idle screen |

### Corner Radius Tokens

| Token | Value | Usage |
|-------|-------|-------|
| radius-pill | 36dp | Nav pill container |
| radius-pill-tab | 26dp | Individual nav tab pill |
| radius-card-lg | 20dp | Full video card |
| radius-card | 16dp | Compact cards, inputs, CTA buttons |
| radius-card-sm | 14dp | Format summary, secondary buttons |
| radius-chip | 12dp | Chips, back button, paste button |
| radius-thumb | 10dp | Compact thumbnails |
| radius-badge-lg | 8dp | Duration/platform badges on full card |
| radius-badge | 6dp | Format/status/platform badges on history items |
| radius-progress | 5dp | Progress bar track and fill |

## Existing Entities (unchanged)

- **DownloadEntity** (Room): No schema change. The `platform` field already stores the platform name which maps to brand colors and abbreviations in the UI layer.
- **VideoInfo** (domain): No change. Used as-is for video card rendering.
- **DownloadFormat** (domain): No change. Used as-is for format chip rendering.
