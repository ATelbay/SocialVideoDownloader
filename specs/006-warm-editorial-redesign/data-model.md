# Data Model: Warm Editorial UI Redesign

**Date**: 2026-03-17
**Feature**: 006-warm-editorial-redesign

## Overview

This is a visual-only redesign. No data model changes — no new entities, no Room schema changes, no domain model changes.

## Design Token Entities (UI Layer Only)

These are not persisted data — they are compile-time constants in the UI theme layer.

### Color Tokens

22 named color values replacing the previous dark purple palette. Defined in `Color.kt`, provided via `MaterialTheme.colorScheme` (M3 mapped) and `LocalExtendedColors` (custom extended).

| Token | Hex | M3 Role or Extended |
|-------|-----|-------------------|
| background | #F6F3EC | M3: `background` |
| surface | #FFFDFC | M3: `surface` |
| surfaceAlt | #FAF6EE | Extended |
| surfaceStrong | #F0EBE0 | Extended |
| card | #FFFFFF | Extended |
| primary | #F26B3A | M3: `primary` |
| primaryStrong | #D95222 | Extended |
| primarySoft | #FFE0D2 | Extended (replaces `primaryContainer` role) |
| warning | #F2B84B | Extended |
| accent | #1E8C7A | Extended |
| accentSoft | #D9F1EC | Extended |
| foreground | #1F2328 | M3: `onBackground`, `onSurface` |
| mutedForeground | #5E6672 | Extended |
| subtleForeground | #7D8794 | Extended |
| border | #D7D0C4 | Extended |
| borderStrong | #B6AA97 | Extended |
| success | #2D9D66 | Extended |
| successSoft | #DDF4E8 | Extended |
| error | #D9534F | M3: `error` |
| errorSoft | #FDE5E3 | M3: `errorContainer` |
| shadow | #8F7C5F24 | Extended |

### Shape Tokens

7 named corner radius values. Defined in `Shape.kt` via `AppShapes` data class.

| Token | Value | Replaces |
|-------|-------|----------|
| card | 22dp | large (16dp) |
| cardLg | 24dp | extraLarge (20dp) |
| control | 18dp | large (16dp) for buttons/inputs |
| summary | 20dp | cardSm (14dp) for summary bar |
| pill | 999dp | pill (36dp) |
| navTab | 26dp | pillTab (26dp) — unchanged |
| thumbnail | 16dp | small (10dp) |

### Spacing Tokens

Updated values in `Spacing` object. Key changes:

| Token | Old Value | New Value |
|-------|-----------|-----------|
| screenPadding | 24dp | 24dp (unchanged) |
| contentTopPadding | 28dp | 16dp |
| sectionGapIdle | 28dp | 24dp |
| sectionGap | 32dp | 20dp |
| navBarHeight | 62dp | 62dp (unchanged) |
| topBarHeight | 48dp | 52dp |
| thumbnailCompactWidth | 72dp | 96dp |
| thumbnailCompactHeight | 54dp | 72dp |
| thumbnailHistorySize | 72x54dp | 72x72dp |

### Navigation Tabs

| Index | Label | Icon | Route |
|-------|-------|------|-------|
| 0 | DOWNLOAD | download | DownloadRoute |
| 1 | LIBRARY | folder-open | LibraryRoute (NEW) |
| 2 | HISTORY | history | HistoryRoute |

## No Database Changes

- Room schema: unchanged
- MediaStore usage: unchanged
- Domain models: unchanged
- Repository interfaces: unchanged
