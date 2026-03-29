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

## Building

### Android (unchanged)
```bash
./gradlew assembleDebug          # Debug APK
./gradlew test                   # Unit tests (JVM + shared commonTest)
./gradlew connectedAndroidTest   # Instrumentation tests
```

### Shared Modules (new)
```bash
./gradlew :shared:network:build          # Build shared network module
./gradlew :shared:data:build             # Build shared data module
./gradlew :shared:feature-download:build # Build shared download feature
./gradlew :core:domain:build             # Build KMP domain module

# Run shared tests on all targets
./gradlew :core:domain:allTests
./gradlew :shared:network:allTests
./gradlew :shared:data:allTests
```

### iOS
```bash
# Generate iOS framework from shared modules
./gradlew :shared:data:linkDebugFrameworkIosArm64      # Device
./gradlew :shared:data:linkDebugFrameworkIosSimulatorArm64  # Simulator

# Open Xcode project
open iosApp/iosApp.xcodeproj

# Build and run from Xcode (Cmd+R)
# Or from command line:
xcodebuild -project iosApp/iosApp.xcodeproj \
  -scheme iosApp \
  -destination 'platform=iOS Simulator,name=iPhone 16' \
  build
```

## Running

### Android
1. Open project in Android Studio
2. Select `app` run configuration
3. Run on device/emulator (same as before)

### iOS
1. Open `iosApp/iosApp.xcodeproj` in Xcode
2. Select iOS Simulator target
3. Build and Run (Cmd+R)
4. Note: First build takes longer (compiles Kotlin/Native framework)

## Testing

### Shared Unit Tests
```bash
# Run all shared tests on JVM (fast)
./gradlew :core:domain:jvmTest
./gradlew :shared:network:jvmTest

# Run all shared tests on iOS Native
./gradlew :core:domain:iosSimulatorArm64Test
./gradlew :shared:data:iosSimulatorArm64Test
```

### Android Tests (unchanged)
```bash
./gradlew test                   # All JVM tests
./gradlew :feature:download:test # Download feature tests
```

### iOS Tests
Run from Xcode: Product → Test (Cmd+U)

## Key Configuration

### Server URL
- **Android**: Set in `core/data/build.gradle.kts` via `buildConfigField("YTDLP_SERVER_URL", ...)`
- **iOS**: Set in `iosApp/iosApp/Info.plist` key `YTDLP_SERVER_URL` (defaults to `http://13.50.106.77:8000`)

### Koin Initialization
- **Android**: `SocialVideoDownloaderApp.onCreate()` calls `initKoin(this)` before Hilt starts
- **iOS**: `App.swift` calls `KoinKt.doInitKoin()` in `init()`

## Troubleshooting

### "Framework not found" in Xcode
Run `./gradlew :shared:data:linkDebugFrameworkIosSimulatorArm64` first.

### Room KMP compilation errors
Ensure all DAO methods are `suspend` or return `Flow`. Blocking DAO methods are not supported in KMP.

### SKIE build failures with Kotlin 2.2.x
Ensure SKIE version is 0.10.5+ (0.10.10 recommended). Earlier versions have a macOS framework path bug.
