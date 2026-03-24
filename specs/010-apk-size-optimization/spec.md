# Feature Specification: APK Size Optimization

**Feature Branch**: `010-apk-size-optimization`
**Created**: 2026-03-25
**Status**: Draft
**Input**: User description: "Reduce Debug APK from ~230MB to ~100MB via ABI filtering, dependency cleanup, and R8 minification"

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Faster Debug Build Installs (Priority: P1)

As a developer, I want the debug APK to only include native libraries for architectures I actually use (arm64-v8a for physical devices, x86_64 for emulators), so that build-deploy-test cycles are faster and the APK doesn't waste disk space with unused ABIs.

**Why this priority**: ABI filtering addresses the single largest contributor to APK bloat (~160MB of native libraries shipped for 4 ABIs when only 2 are needed). This delivers the most size reduction with the least risk.

**Independent Test**: Build a debug APK and verify it contains only arm64-v8a and x86_64 native libraries, reducing APK size by approximately 50%.

**Acceptance Scenarios**:

1. **Given** the project builds a debug APK, **When** the APK is assembled, **Then** it contains native libraries only for arm64-v8a and x86_64 architectures.
2. **Given** the project builds a debug APK, **When** the APK is inspected, **Then** no armeabi-v7a or x86 native libraries are present.
3. **Given** the debug APK is installed on an arm64-v8a device, **When** the app launches, **Then** all features (video download, FFmpeg processing, aria2c downloads) work correctly.
4. **Given** the debug APK is installed on an x86_64 emulator, **When** the app launches, **Then** all features work correctly.

---

### User Story 2 - Minimal Release APK (Priority: P1)

As a developer distributing the app, I want the release APK to include only arm64-v8a native libraries and have R8 code shrinking enabled, so that the distributed APK is as small as possible for end users.

**Why this priority**: Release builds are what users install. Combining single-ABI filtering with R8 minification and resource shrinking targets both native library bloat and DEX file bloat simultaneously, achieving the target ~60-80MB release size.

**Independent Test**: Build a release APK and verify it contains only arm64-v8a native libraries, has minified DEX files, and all app functionality remains intact.

**Acceptance Scenarios**:

1. **Given** the project builds a release APK, **When** the APK is assembled, **Then** it contains native libraries only for arm64-v8a.
2. **Given** R8 minification is enabled for release builds, **When** the APK is assembled, **Then** the total DEX size is reduced by at least 30% compared to the unminified build.
3. **Given** R8 minification is enabled, **When** the release APK is installed on a device, **Then** all app features work correctly (video extraction, downloading with progress, history, sharing).
4. **Given** keep rules are configured, **When** the release APK runs, **Then** no runtime crashes occur from stripped classes (youtubedl-android, Room, Firebase, Hilt, Kotlin serialization).

---

### User Story 3 - Clean Dependency Graph (Priority: P2)

As a developer maintaining the project, I want each module to declare only its own direct dependencies, so that the dependency graph accurately reflects which module is responsible for which library.

**Why this priority**: Removing duplicate dependency declarations from :app improves codebase clarity and reduces confusion about architectural boundaries. This has no impact on APK size but improves maintainability.

**Independent Test**: Remove duplicate youtubedl-android declarations from :app and verify the libraries still resolve transitively through :core:data.

**Acceptance Scenarios**:

1. **Given** youtubedl-android dependencies are declared only in :core:data, **When** the :app module's runtime classpath is inspected, **Then** all three youtubedl-android artifacts (library, ffmpeg, aria2c) are still resolved transitively.
2. **Given** the duplicate declarations are removed from :app, **When** the project builds, **Then** no compilation or runtime errors occur.

---

### Edge Cases

- What happens when a user attempts to run the debug APK on an armeabi-v7a device? The APK will fail to install or crash — this is acceptable since armeabi-v7a devices represent <1% of the market and the app targets SDK 26+.
- What happens if R8 strips a class needed at runtime by a dynamically-loaded library (e.g., yt-dlp's Python runtime)? Keep rules must cover all reflection-accessed classes. The rules for youtubedl-android, Room, Firebase, Play Billing, Hilt, and Kotlin serialization prevent this.
- What happens if the existing packaging configuration (legacy packaging, keepDebugSymbols) is accidentally removed? The yt-dlp native libraries will fail to load at runtime. This configuration must be preserved.
- What happens if a release build is attempted without a signing key? The build will fail — this is expected and acceptable. Release build verification is optional.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: Debug builds MUST include native libraries for exactly two ABIs: arm64-v8a and x86_64.
- **FR-002**: Release builds MUST include native libraries for exactly one ABI: arm64-v8a.
- **FR-003**: Release builds MUST have code shrinking (minification) enabled.
- **FR-004**: Release builds MUST have resource shrinking enabled.
- **FR-005**: Keep rules MUST preserve all classes accessed via reflection by youtubedl-android, Room, Firebase, Play Billing, Hilt, and Kotlin serialization.
- **FR-006**: The existing packaging configuration (legacy JNI packaging, debug symbol retention for *.zip.so) MUST be preserved unchanged.
- **FR-007**: Duplicate youtubedl-android dependency declarations MUST be removed from :app, relying on transitive resolution through :core:data.
- **FR-008**: All existing app functionality (video URL extraction, format selection, downloading with progress, download history, sharing) MUST continue to work after optimization changes.
- **FR-009**: All existing unit tests MUST continue to pass after optimization changes.
- **FR-010**: Build configuration changes MUST be limited to the :app module only — no changes to convention plugins or other modules.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Debug APK size is reduced from ~230MB to ~90-110MB (50%+ reduction).
- **SC-002**: Release APK size is reduced from ~230MB to ~60-80MB (65%+ reduction).
- **SC-003**: Debug build completes successfully with no errors.
- **SC-004**: All existing unit tests pass without modification.
- **SC-005**: The app launches and completes a video download successfully on an arm64-v8a device after optimization.
- **SC-006**: The app launches and runs correctly on an x86_64 emulator using the debug APK.

## Assumptions

- armeabi-v7a and x86 ABIs can be safely excluded — the app targets SDK 26+ and 99%+ of devices in the target market use arm64-v8a.
- x86_64 is included in debug builds solely for emulator testing convenience.
- The existing keep rules file is nearly empty (template comments only) and needs to be populated.
- A release signing key may not be available in all environments, so release build verification is optional.
- youtubedl-android libraries are accessed via reflection/JNI and require explicit keep rules to survive minification.

## Scope Boundaries

**In scope**:
- ABI filtering configuration in the app build script
- Code shrinking enablement and keep rules for release builds
- Removing duplicate dependency declarations from :app
- Verifying debug build success and test pass

**Out of scope**:
- App Bundle (AAB) conversion
- Product flavors or ABI splits
- Resource optimization (fonts, assets — <2MB total)
- Changes to build-logic convention plugins
- Changes to any module other than :app
