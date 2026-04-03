# Implementation Plan: Compose Multiplatform Migration

**Branch**: `013-compose-multiplatform-migration` | **Date**: 2026-03-31 | **Spec**: [spec.md](spec.md)
**Input**: Feature specification from `/specs/013-compose-multiplatform-migration/spec.md`

## Summary

Replace all SwiftUI iOS screens with Compose Multiplatform (CMP) so Android and iOS share a single UI codebase. The shared ViewModel layer already exists in `:shared:feature-*` modules — this migration moves the UI layer from platform-specific (Jetpack Compose on Android, SwiftUI on iOS) to shared Compose Multiplatform in `commonMain`. Big-bang release: all three screens migrate together before the iOS app ships.

## Technical Context

**Language/Version**: Kotlin 2.2.10 (shared + Android), Swift 6.x (iOS app shell + Share Extension)
**Primary Dependencies**: Compose Multiplatform 1.9.3, JetBrains Navigation Compose 2.9.1, Coil 3.x (KMP, replaces Coil 2.7.0 on Android), Koin 4.x, SKIE 0.10.10 (retained for Share Extension)
**Storage**: No changes — Room KMP (shared DB), MediaStore (Android), Documents (iOS)
**Testing**: JUnit5 + MockK + Turbine for shared ViewModel tests (unchanged). Manual smoke testing for UI migration. VoiceOver + Dynamic Type manual testing on iOS.
**Target Platform**: Android 8.0+ (API 26), iOS 16.0+
**Project Type**: Mobile app (Android + iOS, KMP)
**Performance Goals**: iOS launch < 3s, tab switch < 300ms, no frame drops during screen transitions
**Constraints**: Big-bang migration (no hybrid SwiftUI+Compose shipping). Share Extension stays native Swift. Android must have zero visual regression.
**Scale/Scope**: 3 feature screens, 1 navigation shell, 1 shared design system, ~22 SwiftUI files deleted, ~9 new shared UI components, 2 new Gradle modules (`:shared:ui`, `:shared:di`)

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principle | Status | Notes |
|-----------|--------|-------|
| I. Privacy & Zero Bloat | PASS | No new data collection, analytics, or SDKs. Coil 3 upgrade replaces Coil 2 — same functionality. |
| II. On-Device Architecture | PASS | No changes to extraction or download architecture on either platform. |
| **III. Modern Stack** | **VIOLATION — JUSTIFIED** | Constitution says "iOS UI: MUST be built with SwiftUI. UIKit is forbidden for new code." This migration replaces SwiftUI with Compose Multiplatform on iOS. See Complexity Tracking below for justification. **Constitution amendment required**: Principle III iOS UI clause must be updated from "SwiftUI" to "Compose Multiplatform (shared with Android)" before merge. |
| IV. Modular Separation | PASS | Adds `:shared:ui` and `:shared:di` modules. Follows established module structure pattern. |
| V. Minimal Friction UX | PASS | No UX flow changes. Same tap counts, same share sheet flow. |
| VI. Test Discipline | PASS | ViewModel tests unchanged. Manual iOS accessibility testing added. |
| VII. Simplicity & Focus | PASS | Reduces total codebase size (deletes ~22 SwiftUI files + 3 ViewModel wrappers + iOS theme files). One UI codebase is simpler than two. |
| VIII. Optional Cloud Features | PASS | Cloud backup UI migrates to shared Compose — no behavioral changes. |

**Post-Phase-1 re-check**: Same results. No new violations introduced by design artifacts.

## Project Structure

### Documentation (this feature)

```text
specs/013-compose-multiplatform-migration/
├── plan.md              # This file
├── research.md          # Phase 0 output — 12 research decisions
├── data-model.md        # Phase 1 output — design token and component model
├── quickstart.md        # Phase 1 output — developer onboarding
└── tasks.md             # Phase 2 output (created by /speckit.tasks)
```

### Source Code (new and modified paths)

```text
# NEW modules
shared/di/                              # App entry point + Koin aggregation
├── build.gradle.kts                    # applies svd.kmp.library + svd.kmp.compose
└── src/
    ├── commonMain/kotlin/.../di/
    │   └── SharedApp.kt               # NavHost + PillNavigationBar + SvdTheme
    └── iosMain/kotlin/.../di/
        ├── KoinInitializer.kt          # initKoin() — moved here
        ├── KoinHelper.kt              # VM factory — moved here
        └── SharedAppViewController.kt  # ComposeUIViewController factory

shared/ui/                              # Shared design system + components
├── build.gradle.kts
└── src/
    ├── commonMain/
    │   ├── composeResources/
    │   │   └── font/                   # SpaceGrotesk + Inter .ttf files
    │   └── kotlin/.../shared/ui/
    │       ├── theme/
    │       │   ├── Color.kt            # Unified color palette
    │       │   ├── Type.kt             # Typography scale
    │       │   ├── Shape.kt            # Corner radius tokens
    │       │   ├── Spacing.kt          # Dimension tokens
    │       │   └── Theme.kt            # SvdTheme composable
    │       └── components/
    │           ├── GradientButton.kt
    │           ├── SecondaryButton.kt
    │           ├── FormatChip.kt
    │           ├── PillNavigationBar.kt
    │           ├── SvdTopBar.kt
    │           ├── VideoInfoCard.kt
    │           ├── PlatformBadge.kt
    │           ├── StatusBadge.kt
    │           └── TextActionLink.kt
    └── androidMain/kotlin/.../shared/ui/theme/
        └── DynamicColorTheme.kt        # Android Dynamic Color wrapper

# MODIFIED shared feature modules (add Compose screens to commonMain)
shared/feature-library/src/commonMain/kotlin/.../
    ├── ui/
    │   ├── LibraryScreen.kt
    │   ├── LibraryListItemRow.kt
    │   └── LibraryEmptyState.kt
    └── platform/
        └── PlatformActions.kt          # expect fun openFile(), shareFile()

shared/feature-history/src/commonMain/kotlin/.../
    ├── ui/
    │   ├── HistoryScreen.kt
    │   ├── HistoryContent.kt
    │   ├── HistoryListItem.kt
    │   ├── HistoryEmptyState.kt
    │   ├── HistoryDeleteDialog.kt
    │   ├── HistoryBottomSheet.kt
    │   ├── CloudBackupSection.kt
    │   ├── CapacityBanner.kt
    │   ├── UpgradeScreen.kt
    │   └── RestoreDialog.kt
    └── platform/
        └── PlatformActions.kt          # expect fun signIn(), shareFile()

shared/feature-download/src/commonMain/kotlin/.../
    ├── ui/
    │   ├── DownloadScreen.kt
    │   ├── IdleContent.kt
    │   ├── UrlInputContent.kt
    │   ├── ExtractingContent.kt
    │   ├── FormatChipsContent.kt
    │   ├── DownloadProgressContent.kt
    │   ├── DownloadCompleteContent.kt
    │   ├── DownloadErrorContent.kt
    │   └── ExistingDownloadBanner.kt
    └── platform/
        └── PlatformActions.kt          # expect fun pasteFromClipboard(), requestNotificationPermission()

# MODIFIED Android feature modules (thin wrappers)
feature/library/src/main/kotlin/.../ui/
    └── LibraryScreen.kt                # Delegates to shared composable
feature/history/src/main/kotlin/.../ui/
    └── HistoryScreen.kt                # Delegates to shared composable
feature/download/src/main/kotlin/.../ui/
    └── DownloadScreen.kt               # Delegates to shared composable

# MODIFIED build-logic
build-logic/convention/src/main/kotlin/
    ├── KmpComposeConventionPlugin.kt   # NEW: svd.kmp.compose
    └── KmpFeatureConventionPlugin.kt   # MODIFIED: stacks svd.kmp.compose

# MODIFIED iOS app shell
iosApp/iosApp/
    ├── App.swift                       # ComposeUIViewController instead of SwiftUI
    └── Helpers/KoinHelper.swift        # Updated imports (shared_di framework)

# DELETED (after migration complete)
iosApp/iosApp/Download/                 # 6 SwiftUI files
iosApp/iosApp/History/                  # 5 SwiftUI files
iosApp/iosApp/Library/                  # 2 SwiftUI files
iosApp/iosApp/ContentView.swift         # SwiftUI TabView
iosApp/iosApp/Theme/                    # Colors.swift, Typography.swift, Shapes.swift
```

**Structure Decision**: Add two new shared KMP modules (`:shared:ui`, `:shared:di`). `:shared:di` hosts both Koin aggregation (iosMain) and the `SharedApp` navigation entry point composable (commonMain). Move screen composables into existing `:shared:feature-*` modules' `commonMain` source sets. Android feature modules become thin wrappers. iOS app shell hosts `ComposeUIViewController`.

## Complexity Tracking

| Violation | Why Needed | Simpler Alternative Rejected Because |
|-----------|------------|-------------------------------------|
| Principle III: SwiftUI → CMP | Maintaining two UI codebases (Compose + SwiftUI) doubles UI development cost. CMP enables a single UI definition consumed by both platforms. | Keeping SwiftUI means every UI change requires parallel implementation in two frameworks with two design token systems. The shared ViewModel layer is already in place — the UI layer is the last duplication point. |

**Required action**: Amend Constitution v4.0.0 → v5.0.0 (MAJOR — principle redefinition). Update Principle III in four locations: (1) iOS UI clause (line 103), (2) iOS ViewModel consumption pattern (line 115), (3) Tech Stack iOS UI entry (line 216), (4) Tech Stack SKIE interop entry (line 222). Also update Principle IV module list to include `:shared:ui` and `:shared:di`. This amendment should be included in the first PR of this feature branch.

## Phase 0: Research Summary

All 12 research items resolved. See [research.md](research.md) for full details.

| ID | Topic | Decision |
|----|-------|----------|
| R-001 | CMP version | 1.9.3 (Kotlin 2.2.10 compatible) |
| R-002 | Plugin setup | `org.jetbrains.compose` 1.9.3 + `kotlin.plugin.compose` 2.2.10 |
| R-003 | Navigation | `org.jetbrains.androidx.navigation:navigation-compose:2.9.1` |
| R-004 | Font/resource bundling | `compose.components.resources` in commonMain |
| R-005 | iOS accessibility | VoiceOver + Dynamic Type supported natively since CMP 1.8.0 |
| R-006 | Clipboard | `LocalClipboard.current` in commonMain (built-in) |
| R-007 | Share sheet | expect/actual → `UIActivityViewController` via root VC |
| R-008 | Share Extension bridge | `NSUserDefaults(suiteName:)` from iosMain + Swift lifecycle hook |
| R-009 | Google Sign-In (iOS) | CocoaPods `GoogleSignIn` + cinterop |
| R-010 | Notification permission | `UNUserNotificationCenter` from iosMain |
| R-011 | Image loading | Coil 3.x (KMP) replaces Coil 2.7.0 |
| R-012 | Koin DI restructuring | New `:shared:di` module for iOS Koin aggregation |

## Phase 1: Design

### Architecture Overview

```
┌─────────────────────────────────────────────────────┐
│                   iOS App Shell                      │
│  App.swift → ComposeUIViewController → SharedApp()   │
│  ShareExtension/ (native Swift, unchanged)           │
└──────────────────────┬──────────────────────────────┘
                       │
┌──────────────────────▼──────────────────────────────┐
│              :shared:di (iosMain)                     │
│  initKoin() aggregates all modules                   │
│  KoinHelper provides VM factories                    │
└──────────────────────┬──────────────────────────────┘
                       │
┌──────────────────────▼──────────────────────────────┐
│          Shared Navigation (commonMain)               │
│  NavHost { downloadScreen, libraryScreen, history }   │
│  PillNavigationBar (from :shared:ui)                  │
└───┬──────────────────┬──────────────────┬───────────┘
    │                  │                  │
┌───▼───┐        ┌────▼────┐       ┌────▼────┐
│Library │        │ History │       │Download │
│Screen  │        │ Screen  │       │ Screen  │
│(shared)│        │(shared) │       │(shared) │
└───┬───┘        └────┬────┘       └────┬────┘
    │                  │                  │
┌───▼───┐        ┌────▼────┐       ┌────▼────┐
│Shared │        │Shared   │       │Shared   │
│Library│        │History  │       │Download │
│  VM   │        │  VM     │       │  VM     │
└───────┘        └─────────┘       └─────────┘
(existing)       (existing)        (existing)
```

**Android path**: `:app` → Android NavHost → `:feature:*` (thin wrappers with Hilt bridge) → shared composables from `:shared:feature-*`

**iOS path**: `App.swift` → `ComposeUIViewController` → `SharedApp()` composable (in `:shared:di` or a new entry point module) → shared NavHost → shared composables

### Platform Abstraction Pattern

Each shared feature module defines `expect` declarations for platform-specific actions:

```kotlin
// commonMain
expect class PlatformActions {
    fun openFile(uri: String)
    fun shareFile(uri: String)
}

// androidMain — delegates to Intent
actual class PlatformActions(private val context: Context) { ... }

// iosMain — delegates to UIKit
actual class PlatformActions { ... }
```

Platform actions are provided via Koin (iOS) or Hilt (Android) and injected into screens as parameters or CompositionLocals.

### Design artifacts

- **[data-model.md](data-model.md)**: Design token model (colors, typography, shapes, spacing) and component catalog
- **[quickstart.md](quickstart.md)**: Developer setup and build instructions for CMP
- No `/contracts/` directory needed — this is an internal UI migration with no external interfaces
