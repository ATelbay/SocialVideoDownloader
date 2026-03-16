# Implementation Plan: UI Redesign — Figma Make Design System

**Branch**: `004-figma-ui-redesign` | **Date**: 2026-03-16 | **Spec**: [spec.md](spec.md)
**Input**: Feature specification from `/specs/004-figma-ui-redesign/spec.md`

## Summary

Redesign the app's UI layer to match the Figma Make design system. This involves: (1) defining design tokens (colors, shapes, spacing, typography) in `:core:ui`, (2) rewriting both feature screen composables to match new layouts for all states, (3) extracting shared components (VideoInfoCard, StatusBadge, PlatformBadge, FormatChip) into `:core:ui`, and (4) adding a theme toggle. No changes to ViewModels, use cases, repositories, or data layer.

## Technical Context

**Language/Version**: Kotlin 2.2.10
**Primary Dependencies**: Jetpack Compose BOM 2026.03.00, Material 3, Hilt 2.59.2, Coil 2.7.0, Navigation Compose 2.9.7
**Storage**: Room 2.8.4 (existing, unchanged)
**Testing**: Visual inspection only (no UI tests per constitution MVP scope)
**Target Platform**: Android, Min SDK 26, Target SDK 36
**Project Type**: Mobile app (Android)
**Performance Goals**: 60fps animations, smooth state transitions
**Constraints**: Download/history ViewModels, use cases, and repositories remain unchanged. Theme toggle adds minimal data-layer infrastructure (DataStore preference, SettingsRepository, SettingsViewModel)
**Scale/Scope**: 2 screens, ~15 composable files modified/created, 4 shared components extracted

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principle | Status | Notes |
|-----------|--------|-------|
| I. Privacy & Zero Bloat | PASS | UI-only change, no new SDKs or data collection |
| II. On-Device Architecture | PASS | No backend, no network changes |
| III. Modern Android Stack | PASS | Compose-only, KSP, MVI preserved. No XML or kapt |
| IV. Modular Separation | PASS | Shared components go to `:core:ui`, features stay in their modules |
| V. Minimal Friction UX | PASS | Tap count unchanged; layout improves clarity |
| VI. Test Discipline | PASS | UI-only, no new use cases/VMs to test. ktlint enforced |
| VII. Simplicity & Focus | PASS | Design tokens and shared components reduce duplication, not add abstraction |

All gates pass. No violations to justify.

## Project Structure

### Documentation (this feature)

```text
specs/004-figma-ui-redesign/
├── plan.md              # This file
├── research.md          # Phase 0 output
├── data-model.md        # Phase 1 output (design token catalog)
├── quickstart.md        # Phase 1 output
└── tasks.md             # Phase 2 output (/speckit.tasks)
```

### Source Code (repository root)

```text
app/
└── src/main/kotlin/com/socialvideodownloader/app/
    └── MainActivity.kt                    # Add theme toggle state management

core/ui/
└── src/main/kotlin/com/socialvideodownloader/core/ui/
    ├── theme/
    │   ├── Color.kt                       # REWRITE: new light/dark palette + custom success colors
    │   ├── Theme.kt                       # MODIFY: add success extension properties, theme toggle support
    │   ├── Type.kt                        # REWRITE: new typography scale per Figma tokens
    │   └── Shape.kt                       # NEW: 5-tier shape system (Small→Full)
    ├── tokens/
    │   ├── Spacing.kt                     # NEW: spacing constants
    │   └── PlatformColors.kt             # NEW: platform brand color definitions
    └── components/
        ├── VideoInfoCard.kt               # NEW: shared video card composable
        ├── PlatformBadge.kt               # NEW: colored platform pill badge
        ├── StatusBadge.kt                 # NEW: download status indicator
        └── FormatChip.kt                  # NEW: selectable format chip

feature/download/
└── src/main/kotlin/com/socialvideodownloader/feature/download/
    ├── DownloadScreen.kt                  # MODIFY: add Scaffold + TopAppBar + theme toggle
    ├── components/
    │   ├── UrlInputContent.kt             # REWRITE: new input styling, paste button, platform detection
    │   ├── VideoInfoContent.kt            # REMOVE (replaced by shared VideoInfoCard)
    │   ├── FormatChipsContent.kt          # REWRITE: horizontal scroll, new chip styling
    │   ├── DownloadProgressContent.kt     # REWRITE: progress card with gradient bar
    │   ├── DownloadCompleteContent.kt     # REWRITE: success icon, new button layout
    │   ├── DownloadErrorContent.kt        # REWRITE: error icon, new button layout
    │   ├── IdleContent.kt                 # NEW: hero section + URL input + platforms
    │   └── ExtractingContent.kt           # NEW: spinner card
    └── (ViewModel/Intent/State unchanged)

feature/history/
└── src/main/kotlin/com/socialvideodownloader/feature/history/
    ├── HistoryScreen.kt                   # MODIFY: new TopAppBar styling
    ├── components/
    │   ├── HistoryContent.kt              # MODIFY: card-style list items
    │   ├── HistoryListItem.kt             # REWRITE: new card layout with badges
    │   ├── HistoryMenus.kt               # REMOVE (replaced by bottom sheet)
    │   ├── HistoryBottomSheet.kt          # NEW: replaces dropdown context menu
    │   ├── HistoryDeleteDialog.kt         # REWRITE: new dialog styling
    │   └── HistoryEmptyState.kt           # NEW: styled empty states
    └── (ViewModel/Intent/State unchanged)
```

**Structure Decision**: Follows existing module structure exactly. New files added only where the redesign introduces genuinely new composables (bottom sheet, empty state, hero section). Shared components extracted to `:core:ui:components/` per spec requirement FR-015.

## Complexity Tracking

No constitution violations — this section is not applicable.
