# Implementation Plan: Format Selection UI Overhaul + Share-without-Save

**Branch**: `008-format-ui-share` | **Date**: 2026-03-20 | **Spec**: [spec.md](spec.md)
**Input**: Feature specification from `/specs/008-format-ui-share/spec.md`

## Summary

Five incremental changes: (1) fix doubled bottom insets on all three screens by zeroing inner Scaffold `contentWindowInsets`, (2) restyle `VideoInfoCard` thumbnail to edge-to-edge with top-only rounding, (3) move `PlatformBadge` into a thumbnail overlay and remove the FlowRow, (4) replace the bottom download button with a side-by-side Download + Share button row between the card and format chips (requires extracting `.fillMaxWidth()` from `GradientButton` and adding a new `SecondaryButton`), (5) implement a share-only download flow via `cacheDir/ytdl_share/`, `FileProvider`, and a one-shot `ShareFile` event that returns to `FormatSelection` state instead of `Done`.

## Technical Context

**Language/Version**: Kotlin 2.2.10
**Primary Dependencies**: Jetpack Compose (BOM 2026.03.00), Material 3, Hilt (KSP), Navigation Compose 2.9.7, Coil
**Storage**: Room (download history), MediaStore (saved files), cacheDir (yt-dlp temp files + new share temp)
**Testing**: JUnit5 + MockK + Turbine for Flow
**Target Platform**: Android 8.0+ (API 26)
**Project Type**: Mobile app (Android), personal utility
**Performance Goals**: No new performance targets — share download uses same yt-dlp pipeline
**Constraints**: On-device only, no backend, no analytics, no auth
**Scale/Scope**: 3 feature modules touched (download, history, library), 1 core module touched (ui), 1 app module touched (FileProvider XML). ~6 existing UI states, 1 new intent added, 2 model fields added.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principle | Status | Notes |
|-----------|--------|-------|
| I. Privacy & Zero Bloat | PASS | No analytics, tracking, or off-device data. Share uses system ACTION_SEND. |
| II. On-Device Architecture | PASS | All downloads remain on-device via yt-dlp. No backend involved. |
| III. Modern Android Stack | PASS | Compose-only UI, KSP, MVI with sealed interfaces, Hilt DI. |
| IV. Modular Separation | PASS | Changes stay within existing module boundaries. New `SecondaryButton` goes to `:core:ui`. Use case layer unchanged. |
| V. Minimal Friction UX | PASS | Share is 1 extra tap from format selection. Download flow unchanged. |
| VI. Test Discipline | PASS | ViewModel state transitions for share mode need unit tests. |
| VII. Simplicity & Focus | PASS | No new abstractions beyond `SecondaryButton` composable. `shareOnly` flag is the minimal extension. |

All gates pass. No violations to justify.

## Project Structure

### Documentation (this feature)

```text
specs/008-format-ui-share/
├── plan.md              # This file
├── research.md          # Phase 0 output
├── data-model.md        # Phase 1 output
├── quickstart.md        # Phase 1 output
└── tasks.md             # Phase 2 output (/speckit.tasks)
```

### Source Code (files touched)

```text
app/
└── src/main/
    └── res/xml/history_file_paths.xml          # Add cache-path for share temp

feature/
├── download/src/main/kotlin/.../ui/
│   ├── DownloadScreen.kt                       # Add Share button row in FormatSelection
│   ├── DownloadIntent.kt                       # Add ShareFormatClicked intent
│   ├── DownloadUiState.kt                      # Add isShareMode to Downloading
│   ├── DownloadViewModel.kt                    # Handle share flow + cleanup
│   └── components/
│       └── FormatChipsContent.kt               # Remove bottom GradientButton
├── download/src/main/kotlin/.../service/
│   └── DownloadService.kt                      # Share-mode: cacheDir, skip MediaStore/Room
├── history/src/main/kotlin/.../ui/
│   └── HistoryScreen.kt                        # Fix contentWindowInsets
└── library/src/main/kotlin/.../ui/
    └── LibraryScreen.kt                        # Fix contentWindowInsets

core/
├── domain/src/main/kotlin/.../model/
│   └── DownloadRequest.kt                      # Add shareOnly: Boolean = false
└── ui/src/main/kotlin/.../components/
    ├── GradientButton.kt                       # Remove internal fillMaxWidth()
    ├── SecondaryButton.kt                      # NEW: outlined button component
    └── VideoInfoCard.kt                        # Thumbnail restyle + badge overlay
```

## Complexity Tracking

No violations — table not needed.
