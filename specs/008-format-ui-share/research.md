# Research: Format Selection UI Overhaul + Share-without-Save

**Feature**: 008-format-ui-share | **Date**: 2026-03-20

## R-001: Double Inset Consumption in Nested Scaffolds

**Decision**: Add `contentWindowInsets = WindowInsets(0, 0, 0, 0)` to inner Scaffolds in DownloadScreen, HistoryScreen, and LibraryScreen.

**Rationale**: The outer Scaffold in `MainActivity` already consumes system insets via `innerPadding` and passes the resulting padding as `modifier` to each screen. Inner Scaffolds with default `contentWindowInsets` (which equals `WindowInsets.safeDrawing`) re-consume the same insets, creating doubled bottom padding. Setting `contentWindowInsets` to zero on inner Scaffolds ensures they don't double-apply insets already handled by the outer Scaffold.

**Alternatives considered**:
- Remove inner Scaffolds entirely → rejected because inner Scaffolds provide `topBar` and `snackbarHost` for each screen.
- Set `contentWindowInsets` on the outer Scaffold instead → rejected because the outer Scaffold needs to consume insets for the bottom nav bar.

## R-002: Thumbnail Edge-to-Edge with Asymmetric Rounding

**Decision**: Remove the `Spacing.CardInnerPaddingFull` (14.dp) wrapping padding from the thumbnail area in `FullVideoInfoCard`. Use `RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp, bottomStart = 0.dp, bottomEnd = 0.dp)` for the thumbnail clip, matching the Card's `cardLg` radius. Keep the text content (title, uploader) in a padded Column below.

**Rationale**: The current 14.dp inner padding prevents the thumbnail from reaching the card edges, wasting visual space. Asymmetric rounding (top only) creates a clean transition: rounded top corners match the card shape, square bottom corners meet the text area below without a visual gap.

**Alternatives considered**:
- Keep uniform padding, increase thumbnail height → rejected because the goal is edge-to-edge aesthetics.
- Use full cardLg rounding on all corners → rejected per spec requirement for square bottom corners.

## R-003: PlatformBadge as Thumbnail Overlay

**Decision**: Move `PlatformBadge` into the thumbnail `Box` as a bottom-left aligned child with `Modifier.align(Alignment.BottomStart).padding(10.dp)`. Remove the `FlowRow` and its preceding `Spacer(10.dp)` entirely.

**Rationale**: Overlaying the badge on the thumbnail is more space-efficient and visually cohesive. The FlowRow was only used for the PlatformBadge — removing it simplifies the layout.

**Alternatives considered**:
- Keep FlowRow for future tags → rejected per YAGNI; PlatformBadge is the only tag.

## R-004: GradientButton Width Extraction

**Decision**: Remove `.fillMaxWidth()` from inside `GradientButton`. Add `.fillMaxWidth()` at each existing call site externally via `Modifier.fillMaxWidth()`.

**Rationale**: The button is used in 5 call sites, all currently full-width. The new side-by-side layout needs half-width buttons. Making width caller-controlled is the minimal change that enables both layouts.

**Call sites requiring `.fillMaxWidth()` addition**: `FormatChipsContent.kt` (being removed), `IdleContent.kt`, `DownloadErrorContent.kt`, `HistoryEmptyState.kt`, `LibraryEmptyState.kt`.

**Alternatives considered**:
- Add a `fillWidth: Boolean` parameter → rejected; Modifier is the idiomatic Compose way to control layout.
- Create a separate `HalfWidthGradientButton` → rejected; unnecessary duplication.

## R-005: SecondaryButton Design

**Decision**: Create `SecondaryButton` in `:core:ui` as an outlined variant with identical height (`Spacing.PrimaryButtonHeight = 52.dp`), shape (`AppShapesInstance.control`), and typography as `GradientButton`. Use `Border(1.dp, SvdBorderStrong)` with transparent background. Text and icon use `SvdTextPrimary` colour.

**Rationale**: Matching dimensions ensures visual alignment in the side-by-side row. Outlined style provides clear visual hierarchy: primary action (Download) is gradient-filled, secondary action (Share) is outlined.

**Alternatives considered**:
- Use Material 3 `OutlinedButton` → rejected; the app uses custom button components throughout for consistent design language.

## R-006: Share-Only Download Flow

**Decision**: Add `shareOnly: Boolean = false` to `DownloadRequest` and `isShareMode: Boolean = false` to `DownloadUiState.Downloading`. In `DownloadService`, when `shareOnly = true`: write to `cacheDir/ytdl_share/`, skip `saveFileToMediaStore()` and `saveDownloadRecord()`, generate a `content://` URI via `FileProvider.getUriForFile()`, and emit `Completed` with that URI. In `DownloadViewModel`, when `Completed` arrives in share mode, emit `DownloadEvent.ShareFile` and reset state to `FormatSelection` (not `Done`).

**Rationale**: Reusing the existing download pipeline with a flag is the minimal change. `cacheDir` is app-private and auto-cleaned by the OS on low storage. `FileProvider` is required because `file://` URIs cannot be shared via `ACTION_SEND` on API 24+.

**Alternatives considered**:
- Download to MediaStore then delete after sharing → rejected; creates a race condition and leaves traces in media scanner.
- Stream directly to share intent → rejected; yt-dlp writes to files, streaming isn't supported.

## R-007: FileProvider Cache Path

**Decision**: Add `<cache-path name="share_temp" path="ytdl_share/" />` to `app/src/main/res/xml/history_file_paths.xml`.

**Rationale**: `FileProvider` needs the cache path declared to generate `content://` URIs for files in `cacheDir/ytdl_share/`. The existing XML only has `external-path` for MediaStore downloads.

## R-008: Temp File Cleanup

**Decision**: Clean up `cacheDir/ytdl_share/` in `DownloadViewModel.onCleared()`. Also clean up on share-mode download failure/cancellation.

**Rationale**: `onCleared()` fires when the user leaves the screen (ViewModel scope ends). This covers navigation away, back press, and process death. Belt-and-suspenders: the OS will also reclaim cache space when storage is low.

**Alternatives considered**:
- Clean up immediately after share intent fires → rejected; the receiving app may still be reading the file.
- Use `WorkManager` for deferred cleanup → rejected; over-engineering for a cache directory.
