# Feature Specification: Compose Multiplatform Migration

**Feature Branch**: `013-compose-multiplatform-migration`  
**Created**: 2026-03-31  
**Status**: Draft  
**Input**: Replace SwiftUI iOS screens with Compose Multiplatform so Android and iOS share a single UI codebase

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Library Screen Works Identically on Both Platforms (Priority: P1)

A user opens the Library tab on either Android or iOS and sees their downloaded videos in a grid layout with thumbnails, titles, and platform badges. They can tap a video to open it or long-press to share it. The experience is visually consistent across platforms, using the same color palette, typography, and component shapes.

**Why this priority**: Library is the simplest screen (grid list, no cloud features, minimal platform interop) and proves the migration path end-to-end — build tooling, shared theme, shared composables, platform integration for open/share actions, and iOS app shell hosting a Compose screen.

**Independent Test**: Can be fully tested by building both Android and iOS apps, navigating to Library, verifying the grid renders correctly with real downloaded files, opening a file, and sharing a file.

**Acceptance Scenarios**:

1. **Given** a user has downloaded videos, **When** they open the Library tab on iOS, **Then** they see a grid of videos with thumbnails, titles, durations, and platform badges matching the current SwiftUI layout
2. **Given** a user taps a video in Library on iOS, **When** the system file handler opens, **Then** the video plays in the default player
3. **Given** a user long-presses a video in Library on iOS, **When** they select "Share", **Then** the platform share sheet appears with the video file
4. **Given** the Library is empty, **When** a user opens the Library tab, **Then** they see an empty state with guidance text
5. **Given** a user is on Android, **When** they open Library, **Then** the screen renders identically to before the migration (no visual regression)

---

### User Story 2 - History Screen with Cloud Backup Works on Both Platforms (Priority: P2)

A user opens the History tab and sees all past downloads with search, delete, and cloud backup functionality. They can sign in with Google to enable cloud backup, see storage capacity, toggle auto-backup, restore from cloud, and upgrade for more storage. The UI and interaction flow is consistent across Android and iOS.

**Why this priority**: History is the most feature-rich screen (search, delete dialogs, bottom sheets, cloud backup section with sign-in, capacity bar, restore, upgrade). Migrating it validates that complex multi-component screens with modals and sheets work in Compose Multiplatform on iOS.

**Independent Test**: Can be tested by verifying History list rendering, search filtering, item deletion flow, and cloud backup section (sign-in, toggle, capacity display) on both platforms.

**Acceptance Scenarios**:

1. **Given** a user has download history, **When** they open History on iOS, **Then** they see a scrollable list of past downloads with thumbnails, titles, status badges, and dates
2. **Given** a user types in the search bar, **When** they enter a query, **Then** the list filters in real-time to matching items
3. **Given** a user swipes or long-presses a history item, **When** they choose "Delete", **Then** a confirmation dialog appears and the item is removed upon confirmation
4. **Given** a user is not signed in, **When** they view the cloud backup section, **Then** they see a "Sign In" prompt
5. **Given** a user signs in with Google, **When** sign-in completes, **Then** they see backup toggle, capacity bar, and restore/upgrade options
6. **Given** a user is on Android, **When** they open History, **Then** there is no visual regression from the pre-migration experience

---

### User Story 3 - Download Screen Works on Both Platforms (Priority: P3)

A user pastes or enters a video URL, extracts video info, selects a format, and downloads the video — all through a shared UI on both platforms. The download flow progresses through idle, extracting, format selection, downloading, and complete/error states with appropriate feedback at each step. Platform-specific behaviors (clipboard paste, notification permission, file open/share) invoke native capabilities seamlessly.

**Why this priority**: Download is the app's core feature and the most complex screen (5+ distinct states, clipboard integration, notification permission flow, progress tracking, file actions). It is P3 because it requires the most platform interop and benefits from patterns established in P1 and P2.

**Independent Test**: Can be tested by pasting a URL, extracting info, selecting a format, downloading, and verifying the file appears in Library — on both platforms.

**Acceptance Scenarios**:

1. **Given** a user is on the Download tab (idle state), **When** they paste a URL from clipboard, **Then** the URL appears in the input field
2. **Given** a valid URL is entered, **When** the user taps "Extract", **Then** a loading state appears, followed by video info (thumbnail, title, duration) and format chips
3. **Given** format chips are displayed, **When** the user selects a format and taps "Download", **Then** a progress bar tracks the download
4. **Given** a download completes, **When** the user sees the complete state, **Then** they can open or share the downloaded file
5. **Given** a download fails, **When** the error state shows, **Then** the user can retry or reset
6. **Given** a URL was shared from another app (via Share Extension on iOS or share intent on Android), **When** the user opens the app, **Then** the URL is pre-filled in the input field
7. **Given** a user is on Android, **When** they use the Download screen, **Then** there is no visual regression

---

### User Story 4 - Unified Navigation and App Shell (Priority: P4)

A user launches the app on iOS and sees a bottom navigation bar with three tabs (Download, Library, History) that matches the Android navigation experience. Tab switching is instant and preserves each tab's scroll position and state.

**Why this priority**: The navigation shell ties all three migrated screens together into a complete app experience. It replaces the SwiftUI TabView with a Compose Multiplatform navigation host.

**Independent Test**: Can be tested by launching the iOS app, switching between all three tabs, verifying state preservation, and confirming the navigation bar appearance matches the Android version.

**Acceptance Scenarios**:

1. **Given** a user launches the iOS app, **When** the app loads, **Then** they see a bottom navigation bar with Download, Library, and History tabs
2. **Given** a user is on the Library tab with a scroll position, **When** they switch to History and back, **Then** the Library scroll position is preserved
3. **Given** a user taps a tab, **When** the screen transitions, **Then** the transition is smooth with no visible lag or flicker

---

### User Story 5 - Consistent Visual Design Across Platforms (Priority: P5)

Both Android and iOS render the same design system: warm off-white backgrounds, terracotta/teal accent colors, SpaceGrotesk display font, Inter body font, consistent corner radii, and identical component styling (gradient buttons, format chips, pill navigation bar, video info cards). Dynamic Color (Material You) remains an Android-only enhancement; iOS uses the static SVD palette.

**Why this priority**: Visual consistency is the primary motivation for this migration. A unified design system ensures brand coherence without maintaining two separate theme implementations.

**Independent Test**: Can be tested by placing Android and iOS screenshots side-by-side for each screen and verifying visual parity (accounting for platform-standard differences like status bar and system font rendering).

**Acceptance Scenarios**:

1. **Given** both platforms are running, **When** screenshots of each screen are compared, **Then** colors, typography, spacing, and component shapes match within platform-standard tolerances
2. **Given** a user is on Android with Dynamic Color enabled, **When** they view the app, **Then** Material You theming applies as before (no regression)
3. **Given** a user is on iOS, **When** they view the app, **Then** the static SVD color palette is applied consistently

---

### Edge Cases

- What happens when Compose Multiplatform renders custom fonts (SpaceGrotesk, Inter) on iOS? Font file bundling and fallback behavior must be verified.
- How does the iOS keyboard interact with Compose text fields (URL input, search bar)? Inset handling and focus behavior may differ from SwiftUI.
- iOS accessibility (VoiceOver, Dynamic Type) must achieve full parity with the current SwiftUI implementation. All interactive elements must be navigable via VoiceOver, and text must respect Dynamic Type size settings.
- How does the app behave if the Share Extension writes a URL while the Compose-based iOS app is in the foreground? The App Group UserDefaults bridge must still work.
- What happens with iOS-specific gestures (swipe-back navigation, pull-to-refresh) in Compose Multiplatform?

## Clarifications

### Session 2026-03-31

- Q: What level of iOS accessibility support (VoiceOver, Dynamic Type) is required? → A: Full parity — VoiceOver and Dynamic Type must work equivalently to the current SwiftUI implementation.
- Q: Should iOS migration be incremental (hybrid SwiftUI+Compose during transition) or big-bang (all screens at once)? → A: Big-bang — all three screens migrated together; iOS app switches to full Compose in a single release.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: The app MUST render the Library screen from a single shared codebase on both Android and iOS with equivalent functionality (grid display, file open, file share)
- **FR-002**: The app MUST render the History screen from a single shared codebase with equivalent functionality (list display, search, delete, cloud backup controls)
- **FR-003**: The app MUST render the Download screen from a single shared codebase with equivalent functionality (URL input, extraction, format selection, progress, completion, error states)
- **FR-004**: The app MUST provide a bottom navigation bar on both platforms with three tabs (Download, Library, History) and state preservation across tab switches
- **FR-005**: The app MUST apply a consistent design system (colors, typography, shapes, spacing) from a single shared theme definition
- **FR-006**: Platform-specific capabilities (clipboard paste, file open, file share, notification permission, Google Sign-In) MUST invoke native platform APIs seamlessly from within shared UI screens
- **FR-007**: The iOS Share Extension MUST continue to function, passing shared URLs to the main app as before
- **FR-008**: Dynamic Color (Material You) MUST remain functional on Android devices that support it
- **FR-009**: The app MUST NOT introduce visual regressions on Android — existing users see no change in appearance or behavior
- **FR-010**: The iOS app MUST maintain its current minimum deployment target compatibility (iOS 16.0)
- **FR-011**: All shared screens MUST support VoiceOver navigation and Dynamic Type text scaling on iOS at parity with the current SwiftUI implementation

### Key Entities

- **Shared Screen**: A UI screen defined once and rendered on both platforms, consuming a shared ViewModel's state
- **Platform Action**: A native platform capability (file open, share, clipboard, notifications, sign-in) invoked from shared UI through a platform abstraction layer
- **Design Token**: A named value (color, font size, corner radius, spacing) defined in the shared theme and applied consistently across platforms

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: All three screens (Download, Library, History) render and function correctly on both Android and iOS from a single shared UI codebase
- **SC-002**: The number of UI source files maintained for iOS is reduced by at least 80% (from ~22 SwiftUI view files to at most 4-5 platform-specific stubs)
- **SC-003**: Side-by-side screenshots of each screen on Android and iOS show visual parity (same colors, fonts, spacing, component shapes) within platform-standard tolerances
- **SC-004**: All existing Android functionality passes manual smoke testing with zero regressions after migration
- **SC-005**: iOS app launch-to-interactive time remains under 3 seconds on a reference device (iPhone 14 or equivalent)
- **SC-006**: Tab switching between the three screens completes in under 300ms with no visible frame drops on both platforms
- **SC-007**: Platform-specific actions (clipboard paste, file open, share, notifications, Google Sign-In) work correctly on both platforms with no user-visible bridging artifacts
- **SC-008**: The iOS Share Extension continues to pass URLs to the main app correctly after the migration

## Assumptions

- Compose Multiplatform for iOS is stable enough for production use at the current version compatible with Kotlin 2.2.10
- SpaceGrotesk and Inter font files can be bundled as shared resources in a KMP module and loaded by Compose Multiplatform on both platforms
- The existing shared ViewModel layer (SharedDownloadViewModel, SharedHistoryViewModel, SharedLibraryViewModel) requires no changes — only the UI consumption layer changes from SwiftUI to Compose
- iOS binary size increase of ~15-20MB from Compose runtime and Skia renderer is acceptable for this personal-use app
- Scroll physics and animation feel on iOS via Compose Multiplatform (Skia-rendered) are acceptable, even if not identical to native SwiftUI
- The iOS Share Extension remains a native Swift/UIKit target and does not need to use Compose Multiplatform
- SKIE may still be needed for the Share Extension's KMP interop or other non-UI bridging; removal is evaluated during cleanup

## Scope Boundaries

### In Scope
- Migrating all three feature screens (Library, History, Download) from SwiftUI to Compose Multiplatform
- Creating a shared design system (theme, colors, typography, shapes, spacing, reusable components)
- Replacing the iOS SwiftUI TabView with a Compose Multiplatform navigation shell
- Build infrastructure (Gradle plugins, dependency management) for Compose Multiplatform in the project
- Removing SwiftUI view files, ViewModel wrappers, and iOS theme files after migration
- Updating CI/CD for the new build configuration

### Migration Strategy
- **Big-bang release**: All three screens are migrated together. The iOS app switches from SwiftUI to full Compose Multiplatform in a single release — no hybrid SwiftUI+Compose intermediate state ships to users.
- Screens are developed and tested incrementally (Library first, then History, then Download), but the iOS app is only released after all screens and the navigation shell are complete.

### Out of Scope
- Changes to the shared ViewModel layer or business logic
- Changes to the data layer (Room, Ktor, repositories)
- Changes to the Android-only modules beyond thin wrappers delegating to shared composables
- Rewriting the iOS Share Extension in Compose
- Adding new features or screens during migration
- Performance optimization of Compose Multiplatform rendering on iOS (addressed separately if needed)

## Dependencies

- Compose Multiplatform release compatible with Kotlin 2.2.10
- JetBrains Compose Multiplatform Navigation library compatible with the chosen Compose version
- Existing shared ViewModel layer stability (no concurrent refactors during migration)

## Risks

- **iOS scroll/animation feel**: Compose Multiplatform uses Skia rendering on iOS, which may not perfectly match native SwiftUI scroll physics and gesture feel. Mitigation: Accept minor differences for this personal-use app; evaluate user feel during testing.
- **Keyboard interaction on iOS**: Compose text fields may handle iOS keyboard appearance, insets, and focus differently than SwiftUI. Mitigation: Test URL input and search fields early in the migration.
- **Binary size increase**: iOS app binary grows ~15-20MB. Mitigation: Acceptable for personal use; documented as a known trade-off.
- **Compose Multiplatform maturity**: Some iOS-specific behaviors (accessibility, Dynamic Type, gesture interop) may have rough edges. Mitigation: Test accessibility features; document known limitations.
- **Build complexity**: Adding Compose Multiplatform alongside existing Android Compose introduces dependency management complexity (JetBrains artifacts in shared modules vs AndroidX in Android-only modules). Mitigation: Convention plugins isolate this complexity.
