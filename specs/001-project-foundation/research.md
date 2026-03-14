# Research: Project Foundation

**Branch**: `001-project-foundation` | **Date**: 2026-03-14

## R1: Kotlin + KSP Version Compatibility

**Decision**: Use Kotlin 2.2.10 (already in project) with matching KSP version `2.2.10-1.0.31`
**Rationale**: The project template already declares Kotlin 2.2.10. KSP versions must match the Kotlin version prefix exactly (`{kotlinVersion}-{kspVersion}`). The KSP 1.0.31 release supports Kotlin 2.2.10.
**Alternatives considered**:
- Downgrade to Kotlin 2.1.10 — unnecessary, 2.2.10 is stable and already configured
- Use kapt instead of KSP — forbidden by constitution (build performance)

## R2: Hilt with KSP (not kapt)

**Decision**: Use Dagger/Hilt 2.56 with KSP processor
**Rationale**: Dagger added KSP support at 2.48, matured by 2.51+. Version 2.56 is the latest stable with full KSP support. The only change from kapt is using `ksp(libs.hilt.compiler)` instead of `kapt(libs.hilt.compiler)` — same artifact, different processor.
**Alternatives considered**:
- Koin — lighter but not Google-recommended for Android; Hilt is constitution-mandated
- Manual DI — too verbose for multi-module project

## R3: Compose BOM and Material 3

**Decision**: Update Compose BOM from 2024.09.00 to 2026.03.00
**Rationale**: Current BOM is 18 months outdated. BOM 2026.03.00 maps to Material3 1.4.0 and Compose UI 1.10.5. Dynamic Color requires `dynamicDarkColorScheme()`/`dynamicLightColorScheme()` from Material3, available since Material3 1.0.0 but improved in later versions.
**Alternatives considered**:
- Keep 2024.09.00 — too old, missing bug fixes and API improvements

## R4: Compose Navigation (Type-Safe, Multi-Module)

**Decision**: Use Navigation Compose 2.9.7 with `@Serializable` route objects and `kotlinx-serialization-json`
**Rationale**: Type-safe navigation (introduced in Navigation 2.8+) uses `@Serializable` objects/data classes as routes instead of string-based routes. Each feature module defines its route objects and exposes `NavGraphBuilder` extension functions. The `:app` module composes them in a single `NavHost`. Requires the `kotlin.plugin.serialization` Gradle plugin.
**Alternatives considered**:
- String-based routes — error-prone, no compile-time safety
- Voyager/Decompose — third-party, unnecessary complexity

## R5: Room with KSP

**Decision**: Use Room 2.8.4 with KSP and the `androidx.room` Gradle plugin
**Rationale**: Room 2.8.4 generates Kotlin code by default when using KSP. The `androidx.room` Gradle plugin manages schema directory configuration. Schema export is configured via `room { schemaDirectory("$projectDir/schemas") }`.
**Alternatives considered**:
- Room with kapt — forbidden by constitution
- SQLDelight — viable but Room is Android-standard and constitution-specified

## R6: Multi-Module Build Strategy

**Decision**: Use convention plugins in `build-logic/` composite build (NowInAndroid pattern)
**Rationale**: Convention plugins eliminate boilerplate across modules. A `build-logic/` included build is superior to `buildSrc` because it doesn't invalidate the entire build cache on changes. Each convention plugin composes others (e.g., `AndroidFeatureConventionPlugin` applies library + compose + hilt). Feature module build files become ~10 lines.
**Alternatives considered**:
- `buildSrc` — invalidates entire build cache on changes, effectively deprecated for this use case
- No convention plugins (duplicate config in each module) — violates DRY, error-prone for 6 modules
- Subprojects block in root build.gradle.kts — less flexible, harder to compose

## R7: youtubedl-android Initialization

**Decision**: Initialize YoutubeDL, FFmpeg, and aria2c asynchronously in Application.onCreate() using Coroutines (Dispatchers.IO + SupervisorJob)
**Rationale**: The `init()` calls extract native binaries on first run (2-5 seconds). Synchronous init on the main thread causes ANR. Each library should be wrapped in its own try/catch so one failure doesn't cancel others. Expose initialization state via a Flow so the download screen can wait until ready.
**Alternatives considered**:
- Synchronous init — causes ANR on first launch
- WorkManager — unnecessary complexity for one-time init
- Lazy init on first download — delays user's first download attempt

## R8: compileSdk Syntax

**Decision**: Use `compileSdk = 36` (integer form) in convention plugins instead of the `compileSdk { version = release(36) { ... } }` block syntax
**Rationale**: The block syntax with `minorApiLevel` is for targeting specific SDK 36 minor API levels. For foundation setup, the integer form is simpler and sufficient. Can be changed later if a specific minor API level is needed.
**Alternatives considered**:
- Keep block syntax — adds complexity without immediate benefit

## R9: Package and Namespace Convention

**Decision**: Use `com.videograb.{module}` as namespace (e.g., `com.videograb.feature.download`, `com.videograb.core.data`). Application ID remains `com.videograb`.
**Rationale**: Matches constitution naming convention `com.videograb.{module}.{layer}`. Each module declares `namespace` in its `build.gradle.kts` instead of using a per-module `AndroidManifest.xml` package attribute (deprecated).
**Alternatives considered**:
- Keep `com.atelbay.socialvideodownloader` — doesn't match constitution's `com.videograb` convention
