# Quickstart: KMP iOS Migration

**Feature**: `011-kmp-ios-migration` | **Date**: 2026-03-30

## Prerequisites

### Android Development
- Android Studio (current stable)
- JDK 17+
- Android SDK 36

### iOS Development
- macOS with Xcode 16+
- CocoaPods or SPM (for iOS dependency management)
- Apple Developer account (for device testing)

### KMP Tooling
- Kotlin Multiplatform plugin for Android Studio (bundled in recent versions)
- Xcode command-line tools (`xcode-select --install`)

## Module Structure

```
:app                       — Activity, navigation, DI (Hilt), KMP bridge
:feature:download          — Android Download screen (delegates to shared VM)
:feature:history           — Android History screen (delegates to shared VM)
:feature:library           — Android Library screen
:core:domain               — KMP: use cases, domain models, repository interfaces
:core:data                 — Android: Room DB impl, yt-dlp wrapper, MediaStore
:core:ui                   — Android: shared Compose components, theme
:core:cloud                — Android: Firebase Auth + Firestore sync
:core:billing              — Android: Play Billing
:shared:network            — KMP: Ktor HTTP client for yt-dlp API server
:shared:data               — KMP: Room KMP DB, platform abstractions (file, clipboard, download)
:shared:feature-download   — KMP: SharedDownloadViewModel (state machine, retry logic)
:shared:feature-history    — KMP: SharedHistoryViewModel (history + cloud backup)
:shared:feature-library    — KMP: SharedLibraryViewModel (file browser)
iosApp/                    — SwiftUI iOS app (Download, History, Library, Cloud, Share Extension)
```

## Building

### Android (unchanged from pre-KMP)
```bash
./gradlew assembleDebug          # Debug APK (~138MB)
./gradlew assembleRelease        # Release APK
./gradlew test                   # Unit tests (JVM)
./gradlew connectedAndroidTest   # Instrumentation tests (requires device/emulator)
./gradlew ktlintCheck            # Lint (excludes iOS native compilation)
```

### Shared Modules
```bash
# Build individual shared modules
./gradlew :shared:network:assembleDebug
./gradlew :shared:data:assembleDebug
./gradlew :shared:feature-download:assembleDebug

# Build core domain (KMP)
./gradlew :core:domain:build

# Run shared tests (JVM/Android targets)
./gradlew :core:domain:test
./gradlew :shared:feature-download:test
./gradlew :shared:feature-history:test
./gradlew :shared:feature-library:test
```

### iOS Framework Generation
```bash
# Generate KMP framework for iOS Simulator (Apple Silicon)
./gradlew :shared:data:linkDebugFrameworkIosSimulatorArm64

# Generate KMP framework for iOS Device
./gradlew :shared:data:linkDebugFrameworkIosArm64

# Open Xcode project (requires Xcode installed)
open iosApp/iosApp.xcodeproj
```

**Note**: iOS native compilation (`compileKotlinIosArm64`) requires Koin 4.2.0 which
needs Kotlin 2.3.x. Until Koin is downgraded or Kotlin is upgraded, iOS native
compilation is blocked. Use `-x compileKotlinIosArm64 -x compileKotlinIosSimulatorArm64`
to run ktlintCheck without triggering this failure:
```bash
./gradlew ktlintCheck -x compileKotlinIosArm64 -x compileKotlinIosSimulatorArm64
```

## Running

### Android
1. Open project in Android Studio
2. Select `app` run configuration
3. Run on device/emulator (same as before)

### iOS
1. Generate the KMP framework first (see above)
2. Open `iosApp/iosApp.xcodeproj` in Xcode
3. Select iOS Simulator target
4. Build and Run (Cmd+R)
5. Note: First build takes longer (compiles Kotlin/Native framework)

## Testing

### Shared Unit Tests (JVM/Android targets — fast, no device needed)
```bash
./gradlew :core:domain:testDebugUnitTest
./gradlew :shared:feature-download:test
./gradlew :shared:feature-history:test
./gradlew :shared:feature-library:test
```

### iOS Tests
Run from Xcode: Product → Test (Cmd+U)

### Android Tests (unchanged)
```bash
./gradlew test                     # All JVM tests
./gradlew :feature:download:test   # Download feature tests
```

## Key Configuration

### Server URL
- **Android**: Set in `core/data/build.gradle.kts` via `buildConfigField("YTDLP_SERVER_URL", ...)`
  or in `gradle.properties` as `ytdlp.server.url=http://13.50.106.77:8000`
- **iOS**: Set in `iosApp/iosApp/Info.plist` key `YTDLP_SERVER_URL` (defaults to `http://13.50.106.77:8000`)

### Koin Initialization
- **Android**: `SocialVideoDownloaderApp.onCreate()` calls `initKoin(this)` before Hilt starts
- **iOS**: `App.swift` calls `KoinKt.doInitKoin()` in `init()`

### DI Approach
- **Android**: Hilt for Android-specific components; Koin for KMP shared modules
- **iOS**: Koin only

## Error Handling & Retry

The `SharedDownloadViewModel` includes automatic retry with exponential backoff:
- **Transient errors** (NETWORK_ERROR, SERVER_UNAVAILABLE): up to 3 retries with 1s/2s/4s delays
- **Permanent errors** (UNSUPPORTED_URL, EXTRACTION_FAILED): fail immediately, no retry
- Users can always manually retry via the Retry button in the error state

## Troubleshooting

### "Framework not found" in Xcode
Run `./gradlew :shared:data:linkDebugFrameworkIosSimulatorArm64` first.

### Room KMP compilation errors
Ensure all DAO methods are `suspend` or return `Flow`. Blocking DAO methods are not supported in KMP.

### SKIE build failures with Kotlin 2.2.x
Ensure SKIE version is 0.10.5+ (0.10.10 recommended). Earlier versions have a macOS framework path bug.

### Koin 4.2.0 / Kotlin 2.2.x incompatibility (iOS native targets)
Koin 4.2.0 klibs were compiled with Kotlin 2.3.x (ABI 2.3.0) but the project uses 2.2.10 (max ABI 2.2.0).
**Workaround**: Downgrade Koin to 4.1.x OR upgrade Kotlin to 2.3.x when stable.
Android targets are unaffected — only `compileKotlinIosArm64` / `compileKotlinIosSimulatorArm64` fail.

### ktlint violations in KSP-generated files
KSP-generated files under `build/generated/` are excluded via `.editorconfig`. If you see
violations in generated files, check that `.editorconfig` is present at the project root.

### Build time / APK size baseline (2026-03-30)
- Full clean build (--rerun-tasks): ~15s
- APK size (debug): ~138MB (baseline was ~135MB before KMP, +2.2%)
