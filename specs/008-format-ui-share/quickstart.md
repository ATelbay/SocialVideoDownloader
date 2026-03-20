# Quickstart: Format Selection UI Overhaul + Share-without-Save

**Feature**: 008-format-ui-share | **Date**: 2026-03-20

## Implementation Order

The spec prescribes this order: **1 → 2 → 3 → 4 → 5**. Each step is independently verifiable.

### Step 1: Fix Bottom Padding (all screens)

**Files**: `DownloadScreen.kt`, `HistoryScreen.kt`, `LibraryScreen.kt`

Add `contentWindowInsets = WindowInsets(0, 0, 0, 0)` to each inner `Scaffold`. This is a one-line addition per file.

**Verify**: Build and check all 3 screens — bottom padding should look normal, not doubled.

### Step 2: Thumbnail Styling in VideoInfoCard

**File**: `VideoInfoCard.kt` (FullVideoInfoCard)

- Remove the outer `Modifier.padding(Spacing.CardInnerPaddingFull)` from the Card's content Column (or restructure so it only applies to the text area, not the thumbnail).
- Change thumbnail clip shape to `RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)` with square bottom corners.
- Keep title/uploader text in a padded Column below the thumbnail.

**Verify**: Extract a video URL and check the thumbnail fills the card edge-to-edge with rounded top corners only.

### Step 3: Platform Badge Overlay

**File**: `VideoInfoCard.kt` (FullVideoInfoCard)

- Move `PlatformBadge` into the thumbnail `Box` with `Modifier.align(Alignment.BottomStart).padding(10.dp)`.
- Delete the `FlowRow` and its preceding `Spacer(10.dp)`.

**Verify**: Badge overlays the thumbnail bottom-left. No tag row below title.

### Step 4: Download + Share Buttons

**Files**: `GradientButton.kt`, `SecondaryButton.kt` (new), `FormatChipsContent.kt`, `DownloadScreen.kt`, `IdleContent.kt`, `DownloadErrorContent.kt`, `HistoryEmptyState.kt`, `LibraryEmptyState.kt`

1. Remove `.fillMaxWidth()` from inside `GradientButton`.
2. Add `Modifier.fillMaxWidth()` at all 4 remaining call sites (IdleContent, DownloadErrorContent, HistoryEmptyState, LibraryEmptyState).
3. Create `SecondaryButton` in `:core:ui` — same height/shape as GradientButton, outlined style.
4. Remove `GradientButton` from `FormatChipsContent` and its `onDownloadClicked` callback.
5. In `DownloadScreen.kt` `FormatSelection` branch, add a `Row` between `VideoInfoCard` and `FormatChipsContent` with two `Modifier.weight(1f)` buttons: Download (GradientButton) and Share (SecondaryButton).

**Verify**: Two half-width buttons visible. All other screens still show full-width buttons.

### Step 5: Share Flow

**Files**: `DownloadRequest.kt`, `DownloadIntent.kt`, `DownloadUiState.kt`, `DownloadViewModel.kt`, `DownloadService.kt`, `DownloadScreen.kt`, `history_file_paths.xml`

1. Add `shareOnly: Boolean = false` to `DownloadRequest`.
2. Add `ShareFormatClicked` to `DownloadIntent`.
3. Add `isShareMode: Boolean = false` to `DownloadUiState.Downloading`.
4. In ViewModel: `ShareFormatClicked` builds request with `shareOnly = true`, sets `Downloading(isShareMode = true)`.
5. In DownloadService: pass `shareOnly` via intent extra. When true: use `cacheDir/ytdl_share/`, skip MediaStore save and Room record, generate `content://` URI via `FileProvider.getUriForFile()`, emit `Completed` with that URI.
6. In ViewModel: when `Completed` arrives and current state is `Downloading(isShareMode = true)`, emit `ShareFile` event and restore `FormatSelection` state.
7. Add `<cache-path name="share_temp" path="ytdl_share/" />` to `history_file_paths.xml`.
8. In ViewModel `onCleared()`: delete `cacheDir/ytdl_share/` recursively.

**Verify**: `./gradlew assembleDebug`. Run on emulator. Test Share button end-to-end. Verify no History/Library entry. Verify temp cleanup.

## Build & Test

```bash
./gradlew assembleDebug          # Must pass after each step
./gradlew test                   # Unit tests
./gradlew ktlintCheck            # Lint
```
