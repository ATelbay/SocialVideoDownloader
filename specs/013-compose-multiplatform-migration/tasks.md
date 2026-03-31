# Tasks: Compose Multiplatform Migration

**Input**: Design documents from `/specs/013-compose-multiplatform-migration/`
**Prerequisites**: plan.md, spec.md, research.md, data-model.md, quickstart.md

**Tests**: Include unit-test tasks for shared ViewModel consumption patterns and platform action interfaces. Existing ViewModel tests are unchanged — no new test tasks for those.

**Organization**: Tasks are grouped by user story to enable independent implementation and testing of each story. Big-bang release: all stories complete before iOS ships.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3)
- Include exact file paths in descriptions

## Path Conventions

- `build-logic/convention/src/main/kotlin/` for convention plugins
- `shared/ui/src/commonMain/kotlin/com/socialvideodownloader/shared/ui/` for shared theme and components
- `shared/ui/src/commonMain/composeResources/font/` for bundled fonts
- `shared/ui/src/androidMain/kotlin/com/socialvideodownloader/shared/ui/` for Android-specific theme wrappers
- `shared/di/src/commonMain/kotlin/com/socialvideodownloader/shared/di/` for SharedApp composable (navigation entry point)
- `shared/di/src/iosMain/kotlin/com/socialvideodownloader/shared/di/` for iOS Koin aggregation and SharedAppViewController
- `shared/feature-*/src/commonMain/kotlin/com/socialvideodownloader/shared/*/ui/` for shared screen composables
- `shared/feature-*/src/commonMain/kotlin/com/socialvideodownloader/shared/*/platform/` for expect declarations
- `shared/feature-*/src/androidMain/kotlin/com/socialvideodownloader/shared/*/platform/` for Android actual implementations
- `shared/feature-*/src/iosMain/kotlin/com/socialvideodownloader/shared/*/platform/` for iOS actual implementations
- `feature/*/src/main/kotlin/com/socialvideodownloader/feature/*/ui/` for Android thin wrappers
- `iosApp/iosApp/` for iOS app shell

---

## Phase 1: Setup (Build Infrastructure)

**Purpose**: Add CMP Gradle plugins, version catalog entries, and new module scaffolding

- [ ] T001 Amend Constitution v4.0.0 → v5.0.0 (MAJOR — principle redefinition) in `.specify/memory/constitution.md`. Six changes: (1) Principle III line 103 — replace "iOS UI: MUST be built with SwiftUI. UIKit is forbidden for new code." with "iOS UI: MUST be built with Compose Multiplatform (shared with Android via CMP 1.9.x). SwiftUI is permitted only in the Share Extension." (2) Principle III line 115 — replace "iOS SwiftUI ViewModels wrap the shared ViewModel via SKIE-generated async sequences." with "iOS screens consume shared ViewModels directly via Compose Multiplatform. SKIE is retained only for the Share Extension's Swift↔KMP bridge." (3) Tech Stack line 216 — replace "iOS UI: SwiftUI (iOS 16.0+)" with "iOS UI: Compose Multiplatform 1.9.x (iOS 16.0+)" (4) Tech Stack line 222 — replace "iOS↔KMP interop: SKIE (StateFlow → AsyncSequence, sealed class → Swift enum)" with "iOS↔KMP interop: CMP (direct Compose consumption); SKIE retained for Share Extension only" (5) Principle IV lines 125-126 — add `:shared:ui` and `:shared:di` to the Shared KMP modules list (6) Bump version header from v4.0.0 to v5.0.0
- [ ] T002 Add CMP entries to `gradle/libs.versions.toml` — add `compose-multiplatform = "1.9.3"`, `navigation-compose-multiplatform = "2.9.1"`, `coil3 = "3.1.0"` versions; add `coil3-compose`, `coil3-network-ktor3`, `navigation-compose-multiplatform` libraries; add `jetbrainsCompose` plugin
- [ ] T003 Create `svd.kmp.compose` convention plugin in `build-logic/convention/src/main/kotlin/KmpComposeConventionPlugin.kt` — applies `org.jetbrains.compose` and `org.jetbrains.kotlin.plugin.compose`, adds `compose.runtime`, `compose.ui`, `compose.foundation`, `compose.material3`, `compose.components.resources` to commonMain
- [ ] T004 Register `svd.kmp.compose` plugin in `build-logic/convention/build.gradle.kts` gradlePlugin block and add CMP plugin dependency to build-logic classpath
- [ ] T005 Update `KmpFeatureConventionPlugin.kt` in `build-logic/convention/src/main/kotlin/` — stack `svd.kmp.compose` so all shared feature modules get CMP; add `coil3-compose`, `coil3-network-ktor3`, `navigation-compose-multiplatform` to commonMain dependencies
- [ ] T006 [P] Create `:shared:ui` module — add `shared/ui/build.gradle.kts` applying `svd.kmp.compose`, register in `settings.gradle.kts`
- [ ] T007 [P] Create `:shared:di` module — add `shared/di/build.gradle.kts` applying `svd.kmp.library` AND `svd.kmp.compose`; declare commonMain dependencies on `:shared:ui`, `:shared:feature-download`, `:shared:feature-history`, `:shared:feature-library`; declare iosMain dependencies on `:shared:network`, `:shared:data`; register in `settings.gradle.kts`. Note: `svd.kmp.compose` is required because this module hosts `SharedApp` composable (T069) and `SharedAppViewController` (T070)
- [ ] T008 Verify Android Compose BOM coexistence — run `./gradlew assembleDebug` and confirm Android-only modules (`:feature:*`, `:core:ui`) still compile with AndroidX Compose while shared modules use JetBrains CMP artifacts

**Checkpoint**: Build infrastructure ready — both Android and iOS targets compile with CMP plugin applied

---

## Phase 2: Foundational (Shared Design System + DI Restructuring)

**Purpose**: Shared theme, reusable components, and Koin DI module — MUST complete before any screen migration

**⚠️ CRITICAL**: No user story work can begin until this phase is complete

### Theme & Tokens

- [ ] T009 [P] Bundle font files — copy SpaceGrotesk (Bold, SemiBold, Medium) and Inter (Regular, Medium, SemiBold) .ttf files to `shared/ui/src/commonMain/composeResources/font/`
- [ ] T010 [P] Create shared color palette in `shared/ui/src/commonMain/kotlin/com/socialvideodownloader/shared/ui/theme/Color.kt` — define all tokens from data-model.md (SvdBg, SvdPrimary, SvdWarning, SvdAccent, SvdSurface, SvdOnSurface, SvdOnSurfaceVariant, SvdBorder, SvdSuccess, SvdError, SvdPrimarySoft, SvdPrimaryGradientStart, SvdPrimaryGradientEnd); create `SvdColorScheme` with M3 `lightColorScheme` mapping and `ExtendedColors` data class with `LocalExtendedColors` CompositionLocal
- [ ] T011 [P] Create shared typography in `shared/ui/src/commonMain/kotlin/com/socialvideodownloader/shared/ui/theme/Type.kt` — load SpaceGrotesk and Inter via `Res.font.*` accessors; define M3 `Typography` matching data-model.md scale (displayLarge through labelMedium)
- [ ] T012 [P] Create shared shapes in `shared/ui/src/commonMain/kotlin/com/socialvideodownloader/shared/ui/theme/Shape.kt` — define `AppShapes` data class with all tokens (card=22dp, cardLg=24dp, control=18dp, summary=20dp, pill=999dp, navTab=26dp, thumbnail=16dp, small=8dp) and `LocalAppShapes` CompositionLocal
- [ ] T013 [P] Create shared spacing in `shared/ui/src/commonMain/kotlin/com/socialvideodownloader/shared/ui/theme/Spacing.kt` — define `Spacing` object with all tokens from data-model.md (ScreenPadding=24dp through ThumbnailSize=80dp)
- [ ] T014 Create shared theme composable in `shared/ui/src/commonMain/kotlin/com/socialvideodownloader/shared/ui/theme/Theme.kt` — `SvdTheme` wrapping `MaterialTheme` with `CompositionLocalProvider` for `LocalExtendedColors`, `LocalAppShapes`; accept `dynamicColor: Boolean = false` parameter
- [ ] T015 Create Android Dynamic Color wrapper in `shared/ui/src/androidMain/kotlin/com/socialvideodownloader/shared/ui/theme/DynamicColorTheme.kt` — conditionally applies `dynamicLightColorScheme()` on Android 12+ when `dynamicColor = true`, falling back to static SVD palette
- [ ] T016 [P] Create shared `PlatformColors` helper in `shared/ui/src/commonMain/kotlin/com/socialvideodownloader/shared/ui/tokens/PlatformColors.kt` — port brand color constants and `forPlatform()`, `textColor()`, `nameFromUrl()` from `core/ui`

### Shared Components

- [ ] T017 [P] Port `GradientButton` composable to `shared/ui/src/commonMain/kotlin/com/socialvideodownloader/shared/ui/components/GradientButton.kt` — vertical gradient SvdPrimary→SvdWarning, same props (text, onClick, icon?, enabled)
- [ ] T018 [P] Port `SecondaryButton` composable to `shared/ui/src/commonMain/kotlin/com/socialvideodownloader/shared/ui/components/SecondaryButton.kt`
- [ ] T019 [P] Port `FormatChip` composable to `shared/ui/src/commonMain/kotlin/com/socialvideodownloader/shared/ui/components/FormatChip.kt`
- [ ] T020 [P] Port `PillNavigationBar` composable to `shared/ui/src/commonMain/kotlin/com/socialvideodownloader/shared/ui/components/PillNavigationBar.kt` — 3-tab bar (Download/Library/History)
- [ ] T021 [P] Port `SvdTopBar` composable to `shared/ui/src/commonMain/kotlin/com/socialvideodownloader/shared/ui/components/SvdTopBar.kt`
- [ ] T022 [P] Port `VideoInfoCard` composable to `shared/ui/src/commonMain/kotlin/com/socialvideodownloader/shared/ui/components/VideoInfoCard.kt` — replace Coil 2 `AsyncImage` with Coil 3 `AsyncImage` (import from `coil3.compose`)
- [ ] T023 [P] Port `PlatformBadge` composable to `shared/ui/src/commonMain/kotlin/com/socialvideodownloader/shared/ui/components/PlatformBadge.kt`
- [ ] T024 [P] Port `StatusBadge` composable to `shared/ui/src/commonMain/kotlin/com/socialvideodownloader/shared/ui/components/StatusBadge.kt`
- [ ] T025 [P] Port `TextActionLink` composable to `shared/ui/src/commonMain/kotlin/com/socialvideodownloader/shared/ui/components/TextActionLink.kt`

### Koin DI Restructuring

- [ ] T026 Move `KoinInitializer.kt` from `shared/feature-library/src/iosMain/kotlin/com/socialvideodownloader/shared/di/KoinInitializer.kt` to `shared/di/src/iosMain/kotlin/com/socialvideodownloader/shared/di/KoinInitializer.kt` — keep same `initKoin()` function aggregating all 6 Koin modules
- [ ] T027 Move `KoinHelper.kt` from `shared/feature-library/src/iosMain/kotlin/com/socialvideodownloader/shared/di/KoinHelper.kt` to `shared/di/src/iosMain/kotlin/com/socialvideodownloader/shared/di/KoinHelper.kt` — keep same `KoinHelper` object with VM factory functions
- [ ] T028 Remove cross-feature dependencies from `shared/feature-library/build.gradle.kts` — remove iosMain dependencies on `:shared:feature-download`, `:shared:feature-history`, `:shared:network` that were only needed for the aggregation files
- [ ] T029 Update `iosApp/iosApp/Helpers/KoinHelper.swift` — change `import shared_feature_library` to `import shared_di`; update typealias references if framework names changed
- [ ] T030 Verify Koin DI restructuring — run iOS build and confirm `initKoin()` resolves all dependencies correctly

### Update Android `:core:ui` References

- [ ] T031 Update Android `:core:ui` components to import from `:shared:ui` where duplicated — add `:shared:ui` as dependency of `:core:ui` in `core/ui/build.gradle.kts`; Android-specific composables (like `DynamicColorTheme` wrapping) stay in `:core:ui`, shared components re-export from `:shared:ui`
- [ ] T031b Remove Coil 2 and migrate Android references to Coil 3 — (1) in `gradle/libs.versions.toml`, remove the `coil = "2.7.0"` version entry and `coil-compose = { group = "io.coil-kt", name = "coil-compose", version.ref = "coil" }` library entry (2) in `core/ui/build.gradle.kts`, replace `implementation(libs.coil.compose)` with `implementation(libs.coil3.compose)` (3) update any Coil 2 imports in `core/ui/` source files: `import coil.compose.*` → `import coil3.compose.*`, `import coil.*` → `import coil3.*`

**Checkpoint**: Shared design system, all 9 components, and Koin DI restructuring complete. Foundation ready — user story implementation can begin.

---

## Phase 3: User Story 1 - Library Screen (Priority: P1) 🎯

**Goal**: Library screen renders from shared composables on both Android and iOS with file open/share functionality

**Independent Test**: Build both apps, navigate to Library, verify grid renders, tap to open a video, long-press to share

### Platform Actions for Library

- [ ] T032 [P] [US1] Define `expect` platform actions in `shared/feature-library/src/commonMain/kotlin/com/socialvideodownloader/shared/feature/library/platform/PlatformActions.kt` — `expect fun openFile(uri: String)`, `expect fun shareFile(uri: String)`
- [ ] T033 [P] [US1] Implement `actual` Android platform actions in `shared/feature-library/src/androidMain/kotlin/com/socialvideodownloader/shared/feature/library/platform/PlatformActions.kt` — `openFile` uses `Intent.ACTION_VIEW`, `shareFile` uses `Intent.ACTION_SEND`
- [ ] T034 [P] [US1] Implement `actual` iOS platform actions in `shared/feature-library/src/iosMain/kotlin/com/socialvideodownloader/shared/feature/library/platform/PlatformActions.kt` — `openFile` uses `UIApplication.sharedApplication.openURL`, `shareFile` presents `UIActivityViewController` via root VC

### Shared Screen Composables

- [ ] T035 [P] [US1] Create `LibraryEmptyState` composable in `shared/feature-library/src/commonMain/kotlin/com/socialvideodownloader/shared/feature/library/ui/LibraryEmptyState.kt` — port from `feature/library/src/main/kotlin/.../ui/components/LibraryEmptyState.kt`, use `SvdTheme` tokens
- [ ] T036 [P] [US1] Create `LibraryListItemRow` composable in `shared/feature-library/src/commonMain/kotlin/com/socialvideodownloader/shared/feature/library/ui/LibraryListItemRow.kt` — port from `feature/library/src/main/kotlin/.../ui/components/LibraryListItemRow.kt`, use shared `VideoInfoCard`, `PlatformBadge`
- [ ] T037 [US1] Create `LibraryScreen` composable in `shared/feature-library/src/commonMain/kotlin/com/socialvideodownloader/shared/feature/library/ui/LibraryScreen.kt` — accepts `SharedLibraryViewModel`, collects `uiState` via `collectAsState()`, renders grid with `LibraryListItemRow`, empty state, and `SvdTopBar`; calls platform actions for open/share via effect collection

### Android Integration

- [ ] T038 [US1] Update Android `LibraryScreen.kt` in `feature/library/src/main/kotlin/com/socialvideodownloader/feature/library/ui/LibraryScreen.kt` — make it a thin wrapper that calls the shared `LibraryScreen` composable, passing `viewModel.shared` and Android platform actions

### Build Verification

- [ ] T039 [US1] Verify Library screen compiles for both targets — run `./gradlew :shared:feature-library:compileKotlinIosSimulatorArm64` and `./gradlew :feature:library:compileDebugKotlin`

**Checkpoint**: Library screen renders from shared code. Android shows no regression. iOS composable compiles.

---

## Phase 4: User Story 2 - History Screen (Priority: P2)

**Goal**: History screen with search, delete, cloud backup section renders from shared composables on both platforms

**Independent Test**: Build both apps, navigate to History, verify list, search, delete flow, and cloud backup section (sign-in, toggle, capacity, restore)

### Platform Actions for History

- [ ] T040 [P] [US2] Define `expect` platform actions in `shared/feature-history/src/commonMain/kotlin/com/socialvideodownloader/shared/feature/history/platform/PlatformActions.kt` — `expect fun shareFile(uri: String)`, `expect fun triggerGoogleSignIn(): Result<AuthResult>`, `expect fun openUpgradeFlow()`
- [ ] T041 [P] [US2] Implement `actual` Android platform actions in `shared/feature-history/src/androidMain/kotlin/com/socialvideodownloader/shared/feature/history/platform/PlatformActions.kt` — `shareFile` via `Intent.ACTION_SEND`, `triggerGoogleSignIn` via `CredentialManager`, `openUpgradeFlow` via Play Billing
- [ ] T042 [P] [US2] Implement `actual` iOS platform actions in `shared/feature-history/src/iosMain/kotlin/com/socialvideodownloader/shared/feature/history/platform/PlatformActions.kt` — `shareFile` via `UIActivityViewController`, `triggerGoogleSignIn` via `GIDSignIn` cinterop, `openUpgradeFlow` as no-op or App Store flow

### Shared Screen Composables

- [ ] T043 [P] [US2] Create `HistoryEmptyState` composable in `shared/feature-history/src/commonMain/kotlin/com/socialvideodownloader/shared/feature/history/ui/HistoryEmptyState.kt`
- [ ] T044 [P] [US2] Create `HistoryListItem` composable in `shared/feature-history/src/commonMain/kotlin/com/socialvideodownloader/shared/feature/history/ui/HistoryListItem.kt` — port row layout, use shared `VideoInfoCard`, `StatusBadge`
- [ ] T045 [P] [US2] Create `HistoryDeleteDialog` composable in `shared/feature-history/src/commonMain/kotlin/com/socialvideodownloader/shared/feature/history/ui/HistoryDeleteDialog.kt`
- [ ] T046 [P] [US2] Create `HistoryBottomSheet` composable in `shared/feature-history/src/commonMain/kotlin/com/socialvideodownloader/shared/feature/history/ui/HistoryBottomSheet.kt`
- [ ] T047 [P] [US2] Create `CapacityBanner` composable in `shared/feature-history/src/commonMain/kotlin/com/socialvideodownloader/shared/feature/history/ui/CapacityBanner.kt`
- [ ] T048 [P] [US2] Create `RestoreDialog` composable in `shared/feature-history/src/commonMain/kotlin/com/socialvideodownloader/shared/feature/history/ui/RestoreDialog.kt`
- [ ] T049 [US2] Create `CloudBackupSection` composable in `shared/feature-history/src/commonMain/kotlin/com/socialvideodownloader/shared/feature/history/ui/CloudBackupSection.kt` — sign-in prompt, backup toggle, capacity bar, restore/upgrade buttons; collects `cloudBackupState` from VM
- [ ] T050 [US2] Create `UpgradeScreen` composable in `shared/feature-history/src/commonMain/kotlin/com/socialvideodownloader/shared/feature/history/ui/UpgradeScreen.kt`
- [ ] T051 [US2] Create `HistoryContent` composable in `shared/feature-history/src/commonMain/kotlin/com/socialvideodownloader/shared/feature/history/ui/HistoryContent.kt` — search bar, list, cloud backup section
- [ ] T052 [US2] Create `HistoryScreen` composable in `shared/feature-history/src/commonMain/kotlin/com/socialvideodownloader/shared/feature/history/ui/HistoryScreen.kt` — accepts `SharedHistoryViewModel`, collects `uiState` + `cloudBackupState` + effects, renders `HistoryContent` / empty / loading states with `SvdTopBar`

### Android Integration

- [ ] T053 [US2] Update Android `HistoryScreen.kt` in `feature/history/src/main/kotlin/com/socialvideodownloader/feature/history/ui/HistoryScreen.kt` — make thin wrapper delegating to shared `HistoryScreen`, passing `viewModel.shared` and Android platform actions

### Build Verification

- [ ] T054 [US2] Verify History screen compiles for both targets — run `./gradlew :shared:feature-history:compileKotlinIosSimulatorArm64` and `./gradlew :feature:history:compileDebugKotlin`

**Checkpoint**: History screen with cloud backup renders from shared code. Android shows no regression.

---

## Phase 5: User Story 3 - Download Screen (Priority: P3)

**Goal**: Download screen with all 5+ states renders from shared composables on both platforms, with clipboard, notification permission, and file action interop

**Independent Test**: Paste a URL, extract info, select format, download, open/share the file — on both platforms

### Platform Actions for Download

- [ ] T055 [P] [US3] Define `expect` platform actions in `shared/feature-download/src/commonMain/kotlin/com/socialvideodownloader/shared/feature/download/platform/PlatformActions.kt` — `expect fun openFile(uri: String)`, `expect fun shareFile(uri: String)`, `expect fun requestNotificationPermission(): Boolean`, `expect fun getPendingSharedUrl(): String?`, `expect fun clearPendingSharedUrl()`
- [ ] T056 [P] [US3] Implement `actual` Android platform actions in `shared/feature-download/src/androidMain/kotlin/com/socialvideodownloader/shared/feature/download/platform/PlatformActions.kt` — `openFile`/`shareFile` via Intents, `requestNotificationPermission` via `ActivityResultLauncher`, `getPendingSharedUrl` from Intent extras
- [ ] T057 [P] [US3] Implement `actual` iOS platform actions in `shared/feature-download/src/iosMain/kotlin/com/socialvideodownloader/shared/feature/download/platform/PlatformActions.kt` — `openFile`/`shareFile` via UIKit, `requestNotificationPermission` via `UNUserNotificationCenter`, `getPendingSharedUrl` via `NSUserDefaults(suiteName: "group.com.socialvideodownloader.shared")`

### Shared Screen Composables

- [ ] T058 [P] [US3] Create `UrlInputContent` composable in `shared/feature-download/src/commonMain/kotlin/com/socialvideodownloader/shared/feature/download/ui/UrlInputContent.kt` — URL text field, paste button (using `LocalClipboard`), extract button
- [ ] T059 [P] [US3] Create `ExistingDownloadBanner` composable in `shared/feature-download/src/commonMain/kotlin/com/socialvideodownloader/shared/feature/download/ui/ExistingDownloadBanner.kt`
- [ ] T060 [P] [US3] Create `ExtractingContent` composable in `shared/feature-download/src/commonMain/kotlin/com/socialvideodownloader/shared/feature/download/ui/ExtractingContent.kt`
- [ ] T061 [P] [US3] Create `FormatChipsContent` composable in `shared/feature-download/src/commonMain/kotlin/com/socialvideodownloader/shared/feature/download/ui/FormatChipsContent.kt` — format chip grid using shared `FormatChip`, video info using shared `VideoInfoCard`
- [ ] T062 [P] [US3] Create `DownloadProgressContent` composable in `shared/feature-download/src/commonMain/kotlin/com/socialvideodownloader/shared/feature/download/ui/DownloadProgressContent.kt`
- [ ] T063 [P] [US3] Create `DownloadCompleteContent` composable in `shared/feature-download/src/commonMain/kotlin/com/socialvideodownloader/shared/feature/download/ui/DownloadCompleteContent.kt`
- [ ] T064 [P] [US3] Create `DownloadErrorContent` composable in `shared/feature-download/src/commonMain/kotlin/com/socialvideodownloader/shared/feature/download/ui/DownloadErrorContent.kt`
- [ ] T065 [US3] Create `IdleContent` composable in `shared/feature-download/src/commonMain/kotlin/com/socialvideodownloader/shared/feature/download/ui/IdleContent.kt` — composes `UrlInputContent` + `ExistingDownloadBanner`
- [ ] T066 [US3] Create `DownloadScreen` composable in `shared/feature-download/src/commonMain/kotlin/com/socialvideodownloader/shared/feature/download/ui/DownloadScreen.kt` — accepts `SharedDownloadViewModel`, collects `uiState` + events, switches on sealed state to render Idle/Extracting/FormatChips/Progress/Complete/Error content

### Android Integration

- [ ] T067 [US3] Update Android `DownloadScreen.kt` in `feature/download/src/main/kotlin/com/socialvideodownloader/feature/download/ui/DownloadScreen.kt` — make thin wrapper delegating to shared `DownloadScreen`, passing `viewModel.shared` and Android platform actions

### Build Verification

- [ ] T068 [US3] Verify Download screen compiles for both targets — run `./gradlew :shared:feature-download:compileKotlinIosSimulatorArm64` and `./gradlew :feature:download:compileDebugKotlin`

**Checkpoint**: All three screens render from shared code. Android shows no regression across all screens.

---

## Phase 6: User Story 4 - Navigation Shell (Priority: P4)

**Goal**: Shared navigation host with PillNavigationBar, tab state preservation; iOS app shell hosts ComposeUIViewController

**Independent Test**: Launch iOS app, switch between all three tabs, verify state preservation and smooth transitions

### Shared Navigation

- [ ] T069 [US4] Create shared `SharedApp` composable entry point in `shared/di/src/commonMain/kotlin/com/socialvideodownloader/shared/di/SharedApp.kt` — `NavHost` with three destinations (download, library, history), `PillNavigationBar` from `:shared:ui`, `SvdTheme` wrapping; resolve ViewModels from Koin for iOS or accept them as parameters
- [ ] T070 [US4] Add `SharedAppViewController` factory function in `shared/di/src/iosMain/kotlin/com/socialvideodownloader/shared/di/SharedAppViewController.kt` — returns `ComposeUIViewController { SharedApp() }` for Swift consumption

### iOS App Shell

- [ ] T071 [US4] Rewrite `iosApp/iosApp/App.swift` — replace SwiftUI `ContentView` with `ComposeView: UIViewControllerRepresentable` hosting `SharedAppViewController`; keep `KoinInitializerKt.doInitKoin()` in `init()`; keep Share Extension URL handling via `onOpenURL` and `scenePhase` observation, push URL into shared ViewModel via `KoinHelper`
- [ ] T072 [US4] Delete `iosApp/iosApp/ContentView.swift` (SwiftUI TabView — replaced by shared NavHost)

### iOS Build Verification

- [ ] T073 [US4] Build and run iOS app on simulator — verify app launches with Compose-rendered navigation, all three tabs render, tab switching preserves state, Share Extension URL still passes through

**Checkpoint**: iOS app runs entirely on Compose Multiplatform with shared navigation.

---

## Phase 7: User Story 5 - Visual Design Parity (Priority: P5)

**Goal**: Side-by-side visual parity between Android and iOS — same colors, fonts, spacing, components

**Independent Test**: Take screenshots of all screens on both platforms and compare visual elements

### Accessibility

- [ ] T074 [P] [US5] Add `Modifier.semantics` / `contentDescription` annotations to all shared composables for VoiceOver — ensure all interactive elements in Library, History, and Download screens are navigable and labeled
- [ ] T075 [P] [US5] Verify Dynamic Type scaling — confirm all text in shared composables uses `sp` units, test with iOS Dynamic Type set to largest size and verify text scales correctly

### Visual Polish

- [ ] T076 [P] [US5] Android Dynamic Color integration — update Android `:app` theme wiring to pass `dynamicColor = true` to `SvdTheme` when `Build.VERSION.SDK_INT >= 31`, preserving Material You behavior on supported devices
- [ ] T077 [US5] Visual regression check — run both Android and iOS apps side-by-side on each screen (Download idle, extracting, format selection, progress, complete, error; Library grid, empty; History list, empty, cloud backup section) and verify color/font/spacing/shape parity

**Checkpoint**: Visual parity confirmed across platforms. VoiceOver and Dynamic Type functional on iOS.

---

## Phase 8: Polish & Cross-Cutting Concerns

**Purpose**: Cleanup deleted files, update CI, final verification

### SwiftUI Cleanup

- [ ] T078 [P] Delete `iosApp/iosApp/Download/` directory (6 SwiftUI files: DownloadView.swift, UrlInputView.swift, FormatSelectionView.swift, DownloadProgressView.swift, DownloadCompleteView.swift, DownloadErrorView.swift)
- [ ] T079 [P] Delete `iosApp/iosApp/History/` directory (5 SwiftUI files: HistoryView.swift, HistoryItemRow.swift, CloudBackupView.swift, HistoryDeleteDialog.swift, UpgradeView.swift)
- [ ] T080 [P] Delete `iosApp/iosApp/Library/` directory (2 SwiftUI files: LibraryView.swift, LibraryItemRow.swift)
- [ ] T081 [P] Delete `iosApp/iosApp/Theme/` directory (3 files: Colors.swift, Typography.swift, Shapes.swift)
- [ ] T082 [P] Delete `iosApp/iosApp/Services/NotificationService.swift` — notification permission now handled from iosMain Kotlin

### SKIE & Framework Cleanup

- [ ] T083 Evaluate SKIE removal — check if SKIE is still needed for Share Extension or other non-UI bridging; if only used for ViewModel wrappers (now deleted), remove `co.touchlab.skie` plugin from `svd.kmp.library` convention plugin and `gradle/libs.versions.toml`
- [ ] T084 Update Xcode project — remove deleted Swift file references from `iosApp/iosApp.xcodeproj/project.pbxproj`; update framework linking if `:shared:di` replaced `:shared:feature-library` as the primary imported framework

### CI/CD Updates

- [ ] T085 Update CI workflow — update `.github/workflows/` iOS build step for the new CMP framework structure; ensure `compileKotlinIosArm64` and `compileKotlinIosSimulatorArm64` tasks include `:shared:ui` and `:shared:di` modules; update any Xcode build commands

### Final Verification

- [ ] T086 Run full Android build and test suite — `./gradlew assembleDebug test` — confirm zero regressions
- [ ] T087 Run full iOS build on simulator — confirm app launches, all screens render, navigation works, Share Extension passes URLs, VoiceOver navigable, Dynamic Type scales text
- [ ] T088 Run ktlint — `./gradlew ktlintCheck -x compileKotlinIosArm64 -x compileKotlinIosSimulatorArm64` — confirm zero violations
- [ ] T089 Update CLAUDE.md — add `:shared:ui` and `:shared:di` to module structure section; update iOS UI reference from SwiftUI to Compose Multiplatform; update Active Technologies section

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies — start immediately
- **Foundational (Phase 2)**: Depends on Setup completion — BLOCKS all user stories
- **User Stories (Phase 3-7)**: All depend on Foundational phase completion
  - US1 (Library), US2 (History), US3 (Download) screens can proceed in parallel
  - US4 (Navigation Shell) depends on at least one screen being complete
  - US5 (Visual Parity) depends on all screens and navigation being complete
- **Polish (Phase 8)**: Depends on all user stories being complete

### User Story Dependencies

- **US1 (Library)**: Can start after Phase 2 — no dependencies on other stories
- **US2 (History)**: Can start after Phase 2 — no dependencies on other stories
- **US3 (Download)**: Can start after Phase 2 — no dependencies on other stories
- **US4 (Navigation)**: Depends on US1 + US2 + US3 (needs all three screens for NavHost)
- **US5 (Visual Parity)**: Depends on US4 (needs full app running to verify parity)

### Within Each User Story

- Platform expect declarations before actual implementations
- Sub-composables before parent screen composable
- Shared screen before Android thin wrapper
- Build verification as final step

### Parallel Opportunities

- All Setup tasks T006-T007 are [P] (different modules)
- All theme token tasks T009-T013 are [P] (different files)
- All 9 component ports T017-T025 are [P] (different files)
- Koin DI tasks T026-T028 are sequential (move → move → cleanup)
- US1/US2/US3 screen migrations can proceed in parallel after Phase 2
- Within each story: expect declarations [P], sub-composables [P]

---

## Parallel Example: Phase 2 Components

```text
# Launch all 9 component ports in parallel:
T017: Port GradientButton to shared/ui/.../components/GradientButton.kt
T018: Port SecondaryButton to shared/ui/.../components/SecondaryButton.kt
T019: Port FormatChip to shared/ui/.../components/FormatChip.kt
T020: Port PillNavigationBar to shared/ui/.../components/PillNavigationBar.kt
T021: Port SvdTopBar to shared/ui/.../components/SvdTopBar.kt
T022: Port VideoInfoCard to shared/ui/.../components/VideoInfoCard.kt
T023: Port PlatformBadge to shared/ui/.../components/PlatformBadge.kt
T024: Port StatusBadge to shared/ui/.../components/StatusBadge.kt
T025: Port TextActionLink to shared/ui/.../components/TextActionLink.kt
```

## Parallel Example: Screen Migrations (US1 + US2 + US3)

```text
# After Phase 2 completes, launch all three screen migrations in parallel:
Agent A: US1 Library (T032-T039)
Agent B: US2 History (T040-T054)
Agent C: US3 Download (T055-T068)
```

---

## Implementation Strategy

### Big-Bang Release (per spec clarification)

1. Complete Phase 1: Setup
2. Complete Phase 2: Foundational (CRITICAL — blocks all stories)
3. Complete Phase 3-5: All three screen migrations (Library, History, Download) — can be parallel
4. Complete Phase 6: Navigation Shell (ties screens together)
5. Complete Phase 7: Visual Parity verification
6. Complete Phase 8: Cleanup (delete SwiftUI, update CI)
7. **SHIP**: iOS app fully on Compose Multiplatform in a single release

### Incremental Development (within big-bang)

Even though the release is big-bang, development is incremental:
1. Each screen is developed and tested independently
2. Android regressions caught at each checkpoint
3. iOS compilation verified at each checkpoint
4. Navigation shell integrates all screens
5. Visual parity verified last

---

## Notes

- [P] tasks = different files, no dependencies on incomplete tasks
- [Story] label maps task to specific user story for traceability
- All screens developed independently but released together (big-bang)
- Android must show zero visual regression at every checkpoint
- Commit after each task or logical group
- Stop at any checkpoint to validate progress
