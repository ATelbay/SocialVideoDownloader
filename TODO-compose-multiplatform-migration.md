# Compose Multiplatform Migration Plan

Goal: Replace SwiftUI screens with Compose Multiplatform so there's one UI codebase for both Android and iOS.

## Prerequisites

- [ ] **Move Koin aggregation out of `:shared:feature-library`** — `KoinHelper.kt` and `KoinInitializer.kt` currently live there but aggregate all three features. Create a `:shared:app-entry` or `:shared:di` module for the iOS Koin entry point.

## Phase 1: Build Infrastructure

- [ ] **Add Compose Multiplatform Gradle plugin** — Add `org.jetbrains.compose` plugin to `gradle/libs.versions.toml` (version compatible with Kotlin 2.2.10)
- [ ] **Create `svd.kmp.compose` convention plugin** in `build-logic/` — Applies `org.jetbrains.compose` and `org.jetbrains.kotlin.plugin.compose`, adds Compose Multiplatform dependencies (foundation, material3, runtime) to `commonMain`
- [ ] **Update `svd.kmp.feature` plugin** — Stack `svd.kmp.compose` on top so all shared feature modules get Compose
- [ ] **Verify Android Compose BOM coexists** — Compose Multiplatform replaces `androidx.compose.*` with `org.jetbrains.compose.*` in shared modules. Android-only modules (`:feature:*`, `:core:ui`) can keep using AndroidX Compose. Shared modules must use the JetBrains artifacts.

## Phase 2: Shared Theme & Design System

- [ ] **Create `:shared:ui` module** (or add to existing shared module) with:
  - `Theme.kt` — Material 3 theme wrapping `MaterialTheme` with SVD colors/typography
  - `Color.kt` — SVD color palette (warm off-white, terracotta, teal, etc. from iOS `Colors.swift` and Android `Color.kt`)
  - `Type.kt` — Typography scale (matching current `SVDFont` / Android `Type.kt`)
  - `Shapes.kt` — Corner radius tokens (matching `SVDRadius`)
- [ ] **Move reusable components** from `:core:ui` to `:shared:ui`:
  - `FormatChip`, `GradientButton`, `PillNavigationBar`, `PlatformBadge`, `SecondaryButton`, `StatusBadge`, `SvdTopBar`, `TextActionLink`, `VideoInfoCard`
  - These become `@Composable` in `commonMain` — no Android-specific APIs

## Phase 3: Migrate Screens (one feature at a time)

### 3a. Library (simplest — start here)
- [ ] Move `LibraryScreen`, `LibraryListItemRow`, `LibraryEmptyState` composables into `:shared:feature-library` `commonMain`
- [ ] Wire `SharedLibraryViewModel` directly via `collectAsState()` — remove the SwiftUI `LibraryViewModelWrapper`
- [ ] Platform `expect`/`actual` for file open and share intents (Android: `Intent.ACTION_VIEW`/`ACTION_SEND`, iOS: `UIActivityViewController`)
- [ ] Delete `iosApp/iosApp/Library/LibraryView.swift`, `LibraryItemRow.swift`
- [ ] Update Android `:feature:library` to delegate to the shared composable (thin wrapper with Hilt ViewModel bridge)

### 3b. History (medium complexity — cloud backup UI)
- [ ] Move `HistoryScreen`, `HistoryContent`, `HistoryListItem`, `HistoryEmptyState`, `HistoryDeleteDialog`, `HistoryBottomSheet` to `:shared:feature-history` `commonMain`
- [ ] Move `CloudBackupSection`, `CapacityBanner`, `UpgradeScreen`, `RestoreDialog` to shared
- [ ] Platform `expect`/`actual` for Google Sign-In flow (Android: `CredentialManager`, iOS: `ASAuthorizationController` or web-based flow)
- [ ] Delete all SwiftUI History views
- [ ] Update Android `:feature:history`

### 3c. Download (most complex — platform services)
- [ ] Move `DownloadScreen` and sub-composables (`IdleContent`, `ExtractingContent`, `FormatChipsContent`, `DownloadProgressContent`, `DownloadCompleteContent`, `DownloadErrorContent`, `ExistingDownloadBanner`, `UrlInputContent`) to `:shared:feature-download` `commonMain`
- [ ] Platform `expect`/`actual` for:
  - Clipboard paste button (Android: `ClipboardManager`, iOS: `UIPasteboard`)
  - Notification permission request (Android: `ActivityResultLauncher`, iOS: `UNUserNotificationCenter`)
  - File open/share intents
- [ ] Delete all SwiftUI Download views
- [ ] Update Android `:feature:download`

## Phase 4: iOS App Shell

- [ ] Replace SwiftUI `ContentView.swift` `TabView` with a Compose Multiplatform `UIViewController` hosted via `ComposeUIViewController` in `App.swift`
- [ ] Navigation: Use Compose Navigation in `commonMain` — single `NavHost` with three destinations
- [ ] Keep Share Extension as native Swift (it's minimal and needs App Group access)
- [ ] Remove SKIE dependency if no longer needed for UI bridging (may still be useful for Share Extension)
- [ ] Update Xcode project to embed the new Compose-based framework

## Phase 5: Cleanup

- [ ] Delete all SwiftUI View files (22 files across Download/, History/, Library/, Theme/)
- [ ] Delete `iosApp/iosApp/Helpers/KoinHelper.swift` ViewModel wrapper factories
- [ ] Delete the three `XxxViewModelWrapper` ObservableObject classes
- [ ] Remove `iosApp/iosApp/Theme/` (Colors.swift, Typography.swift, Shapes.swift) — replaced by shared theme
- [ ] Update CI: iOS build step may change (framework linking, test targets)
- [ ] Consider removing SKIE if only used for ViewModel bridging

## Platform Interop Patterns

For platform-specific UI that can't be shared:
- **`expect`/`actual` composables** — e.g., `@Composable expect fun PlatformShareSheet(uri: String)`
- **`UIKitViewController` interop** — for iOS-only flows like Google Sign-In `ASAuthorizationController`
- **`LocalContext.current`** — Android-specific context access stays in `androidMain`

## Risks & Notes

- Compose Multiplatform on iOS uses Skia rendering — animations and scroll physics won't feel 100% native SwiftUI
- Binary size increase on iOS (~15-20MB for Compose runtime + Skia)
- iOS deployment target 16.0 is compatible with Compose Multiplatform
- Dynamic Color (Material You) is Android-only — iOS will use a static Material 3 theme
- Test: Android Compose UI tests (`createComposeRule()`) may need adjustment for multiplatform
