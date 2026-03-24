# Research: APK Size Optimization

**Feature**: 010-apk-size-optimization | **Date**: 2026-03-25

## No NEEDS CLARIFICATION Items

The feature spec had zero unknowns — all technical decisions were specified by the user with explicit file paths, line numbers, and code snippets. This research document records the validation of those decisions.

## Decision 1: ABI Filtering Strategy

**Decision**: Use `ndk.abiFilters` in `defaultConfig` + `release` buildType override. No splits, no product flavors.

**Rationale**: `ndk.abiFilters` is the simplest mechanism — a single line in defaultConfig. ABI splits (`splits { abi { } }`) generate multiple APKs, adding distribution complexity for no benefit in a sideloaded app. Product flavors add a dimension to the build variant matrix, also unnecessary.

**Alternatives considered**:
- `splits { abi { } }` — generates per-ABI APKs, useful for Play Store distribution but overkill for sideloading
- Product flavors — adds build variant complexity, not needed when build-type-specific config suffices
- AAB format — delegates ABI splitting to Play Store, but this app is sideloaded

## Decision 2: ABI Selection

**Decision**: Debug = arm64-v8a + x86_64. Release = arm64-v8a only.

**Rationale**:
- arm64-v8a: 99%+ of Android devices since 2017, mandatory for SDK 26+ targets
- x86_64: emulator-only, needed for debug testing but not distribution
- armeabi-v7a: <1% market share for SDK 26+ devices, safe to drop
- x86: 32-bit emulator, unnecessary with 64-bit x86_64 support

**Alternatives considered**:
- Including armeabi-v7a in release — rejected, negligible market share for SDK 26+ apps
- Excluding x86_64 from debug — rejected, breaks emulator testing

## Decision 3: R8 Keep Rules Scope

**Decision**: Broad keep rules for youtubedl-android, Room, Firebase, Play Billing, Hilt, and Kotlin serialization.

**Rationale**: These libraries use reflection, JNI, or code generation that R8 cannot trace statically. Keeping entire packages is safer than surgical class-level keeps for an initial R8 rollout. Can be tightened later once the release build is validated.

**Alternatives considered**:
- Surgical per-class keep rules — higher risk of missing a reflection target, premature optimization
- Consumer ProGuard rules from libraries — many libraries ship their own rules via `META-INF/proguard`, but youtubedl-android and some Firebase components may not. Explicit rules are safer as a starting point.

## Decision 4: Duplicate Dependency Removal

**Decision**: Remove youtubedl-android declarations from :app, keep them in :core:data only.

**Rationale**: :app depends on :core:data (line 59 of app/build.gradle.kts). Gradle resolves transitive dependencies. The duplicate declarations in :app (lines 77-79) are harmless but misleading — they suggest :app directly uses yt-dlp APIs.

**Validation**: After removal, `./gradlew :app:dependencies --configuration debugRuntimeClasspath | grep youtubedl` must show the artifacts resolved via `:core:data`.
