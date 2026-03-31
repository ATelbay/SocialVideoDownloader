# Research: Compose Multiplatform Migration

**Date**: 2026-03-31 | **Branch**: `013-compose-multiplatform-migration`

## R-001: CMP Version Compatible with Kotlin 2.2.10

**Decision**: Use Compose Multiplatform **1.9.3** with Kotlin 2.2.10.

**Rationale**: CMP 1.9.x requires Kotlin 2.2 for native/web targets. CMP 1.9.3 is the latest in this line and confirmed compatible with Kotlin 2.2.10. CMP 1.10.x documents Kotlin 2.2.20 as its baseline for native — a minor risk with 2.2.10.

**Alternatives considered**:
- CMP 1.10.3 (latest): Likely works with 2.2.10 but 2.2.20 is the documented minimum. Risk of subtle native compilation issues.
- CMP 1.8.x: Compatible but older; misses navigation improvements in 1.9.x.

## R-002: CMP Plugin and Compiler Setup

**Decision**: Apply two separate plugins per module:
1. `org.jetbrains.compose` (version 1.9.3) — CMP runtime/framework
2. `org.jetbrains.kotlin.plugin.compose` (version 2.2.10 = Kotlin version) — Compose compiler

**Rationale**: Since Kotlin 2.0, the Compose compiler ships with Kotlin itself. CMP does NOT bundle its own compiler. Both plugins are required in every module that uses Compose.

**Dependencies via type-safe accessors**:
- `compose.runtime`, `compose.ui`, `compose.foundation`, `compose.material3`
- `compose.components.resources` (for fonts, images, strings in commonMain)
- Maven group: `org.jetbrains.compose.*` (resolved automatically by the plugin)

## R-003: Navigation Library

**Decision**: Use `org.jetbrains.androidx.navigation:navigation-compose:2.9.1` (ships with CMP 1.9.3).

**Rationale**: This is the multiplatform fork of AndroidX Navigation maintained by JetBrains. Works in commonMain, covers Android + iOS. Based on Jetpack Navigation 2.9.x — API-compatible with the existing Android navigation setup.

**Alternatives considered**:
- Navigation 3 (`navigation3-ui:1.0.0-alpha06`): Available in CMP 1.10.0+, still alpha. Not production-ready.
- Android-only `androidx.navigation:navigation-compose:2.9.7`: Does not work in commonMain.

## R-004: Font and Resource Bundling

**Decision**: Use `compose.components.resources` to bundle SpaceGrotesk and Inter font files in `commonMain/composeResources/font/`.

**Rationale**: Fully supported since CMP 1.6.0. Type-safe `Res` accessors generated at compile time. Font files (.ttf/.otf) placed in `src/commonMain/composeResources/font/` are automatically available on both platforms.

**Alternatives considered**:
- Platform-specific font loading (Android resources + iOS asset catalog): Works but defeats the purpose of shared resources.

## R-005: iOS Accessibility (VoiceOver + Dynamic Type)

**Decision**: CMP 1.9.3 supports both VoiceOver and Dynamic Type with no special configuration.

**Rationale**:
- **VoiceOver**: Stable since CMP 1.8.0. CMP's semantic tree maps to native `UIAccessibility` objects automatically. Material widgets produce accessibility semantics with no extra code. `testTag` maps to `accessibilityIdentifier`.
- **Dynamic Type**: Supported since CMP 1.5.0. `LocalDensity.current.fontScale` reads iOS `UIContentSizeCategory` automatically. Text in `sp` scales proportionally. Runtime changes are reflected immediately.

**Known gap**: High-contrast mode is not automatic — requires detecting `UIAccessibilityDarkerSystemColorsEnabled` from Swift and passing a flag to swap `ColorScheme`. This is the only accessibility gap requiring manual implementation.

## R-006: Clipboard Access

**Decision**: Use CMP built-in `LocalClipboard.current` in commonMain.

**Rationale**: CMP 1.8.x+ provides `LocalClipboard` compositionLocal. `getClipEntry()?.getText()` reads clipboard, `setClipEntry(ClipEntry.withPlainText(...))` writes. On iOS it delegates to `UIPasteboard.general` under the hood. No expect/actual needed for text.

**Alternatives considered**:
- Direct `UIPasteboard` via expect/actual: Only needed if reading non-text content (URLs, images).

## R-007: Share Sheet (UIActivityViewController)

**Decision**: Implement as expect/actual. iOS actual presents `UIActivityViewController` via root view controller from Kotlin/Native.

**Rationale**: There is no built-in CMP composable for presenting modal view controllers. The pattern is: get root VC via `UIApplication.sharedApplication.keyWindow?.rootViewController`, call `presentViewController` with `UIActivityViewController`. This works reliably on iOS 16+.

## R-008: Share Extension → Compose Bridge

**Decision**: Read App Group `NSUserDefaults` from Kotlin/Native iosMain using `NSUserDefaults(suiteName:)`. Scene activation hook remains in Swift parent ViewController, pushing URL into a shared ViewModel StateFlow.

**Rationale**: Kotlin/Native has full Obj-C interop with `platform.Foundation.NSUserDefaults`. The Share Extension writes to `UserDefaults(suiteName: "group.com.socialvideodownloader.shared")` — Kotlin reads the same. `ComposeUIViewControllerDelegate` was deprecated in CMP 1.8.2; lifecycle hooks go through the Swift parent VC.

## R-009: Google Sign-In on iOS

**Decision**: Use CocoaPods `GoogleSignIn` + cinterop from iosMain. Call `GIDSignIn.sharedInstance.signInWithPresentingViewController` wrapped in `suspendCoroutine`.

**Rationale**: The project already uses Firebase Auth with Google Sign-In. CocoaPods cinterop is the standard approach for accessing iOS SDKs from Kotlin/Native. The existing Google Sign-In flow moves from SwiftUI presentation to Kotlin-triggered presentation via root VC.

## R-010: Notification Permission

**Decision**: Call `UNUserNotificationCenter.requestAuthorizationWithOptions` from iosMain Kotlin wrapped in `suspendCancellableCoroutine`.

**Rationale**: Full Obj-C interop available via `platform.UserNotifications.*`. Permission requesting works from Kotlin; notification delegate registration still needs Swift AppDelegate (already exists in the project).

## R-011: Image Loading (Coil)

**Decision**: Upgrade from Coil 2.7.0 (Android-only) to Coil 3.x (KMP). Use `coil-compose` + `coil-network-ktor3` in commonMain.

**Rationale**: Coil 3 has first-class KMP support (Android, iOS, Desktop, Wasm). Reuses the existing Ktor engine configuration from `:shared:network`. `AsyncImage` composable works in commonMain identically to Android.

**Alternatives considered**:
- Kamel: KMP image loader, simpler but less mature than Coil 3.
- Landscapist-Coil3: More loading state control, but unnecessary complexity.

## R-012: Koin DI Restructuring

**Decision**: Create a new `:shared:di` module to host `initKoin()` and `KoinHelper` (currently misplaced in `:shared:feature-library`).

**Rationale**: Both files aggregate all three feature modules + data + network. They live in `:shared:feature-library` as acknowledged tech debt (TODO comment in source). The new module declares iosMain dependencies on all five leaf modules and exports the aggregation functions.

**Impact**:
- Move `KoinInitializer.kt` and `KoinHelper.kt` from `:shared:feature-library/iosMain` to `:shared:di/iosMain`
- Remove cross-feature dependencies from `:shared:feature-library/build.gradle.kts`
- Update Xcode project to import `shared_di` framework instead of `shared_feature_library`
- Update Swift `App.swift` and `KoinHelper.swift` import statements
