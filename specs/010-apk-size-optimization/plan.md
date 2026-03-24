# Implementation Plan: APK Size Optimization

**Branch**: `010-apk-size-optimization` | **Date**: 2026-03-25 | **Spec**: [spec.md](spec.md)
**Input**: Feature specification from `/specs/010-apk-size-optimization/spec.md`

## Summary

Reduce APK size from ~230MB to ~100MB (debug) / ~70MB (release) by adding ABI filters to exclude unused native library architectures, enabling R8 minification with proper keep rules for release builds, and removing duplicate youtubedl-android dependency declarations from :app. All changes are confined to two files in the :app module: `build.gradle.kts` and `proguard-rules.pro`.

## Technical Context

**Language/Version**: Kotlin 2.2.10
**Primary Dependencies**: Jetpack Compose (BOM 2026.03.00), Hilt (KSP), Room (KSP), Navigation Compose 2.9.7, Coil 2.7.0, youtubedl-android 0.18.x (library + ffmpeg + aria2c), Firebase BOM 33.15.0 (Auth + Firestore), Play Billing 7.1.1
**Storage**: Room (download history), MediaStore (saved files), cacheDir (yt-dlp temp files)
**Testing**: JUnit5 + MockK + Turbine
**Target Platform**: Android 8.0+ (API 26)
**Project Type**: Mobile app (Android)
**Performance Goals**: Debug APK ≤ 110MB, Release APK ≤ 80MB
**Constraints**: On-device only, no backend. Existing `packaging { }` block with `jniLibs.useLegacyPackaging = true` and `keepDebugSymbols` MUST be preserved.
**Scale/Scope**: Build config changes only — 2 files modified in :app module

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principle | Status | Notes |
|-----------|--------|-------|
| I. Privacy & Zero Bloat | PASS | No new SDKs, no analytics, no data collection. This feature *reduces* bloat. |
| II. On-Device Architecture | PASS | No backend changes. All extraction remains on-device. |
| III. Modern Android Stack | PASS | No UI changes. KSP-only constraint unaffected. |
| IV. Modular Separation | PASS | Changes limited to :app module. Removing duplicate deps from :app actually improves module boundary clarity. |
| V. Minimal Friction UX | PASS | No UX changes. |
| VI. Test Discipline | PASS | All existing tests must pass (FR-009). No new tests needed for build config. |
| VII. Simplicity & Focus | PASS | Minimal changes, no over-engineering. ndk.abiFilters is the simplest approach (no splits, no flavors). |
| VIII. Optional Cloud Features | N/A | No cloud feature changes. |

**Gate result**: ALL PASS. No violations to justify.

## Project Structure

### Documentation (this feature)

```text
specs/010-apk-size-optimization/
├── plan.md              # This file
├── research.md          # Phase 0 output
├── spec.md              # Feature specification
├── data-model.md        # Phase 1 output (minimal — no data model changes)
├── quickstart.md        # Phase 1 output
└── checklists/
    └── requirements.md  # Spec quality checklist
```

### Source Code (files modified)

```text
app/
├── build.gradle.kts     # ABI filters, R8 enablement, dependency cleanup
└── proguard-rules.pro   # Keep rules for R8 minification
```

**Structure Decision**: No new modules, no new source files. This feature modifies only build configuration in the existing :app module.

## Implementation Approach

### Phase 1: ABI Filtering (FR-001, FR-002)

**File**: `app/build.gradle.kts`

Add `ndk { abiFilters }` in `defaultConfig` for debug (arm64-v8a + x86_64), override in `release` buildType to arm64-v8a only.

**Key constraint**: Do NOT touch the existing `packaging { }` block (lines 37-40).

### Phase 2: Remove Duplicate Dependencies (FR-007)

**File**: `app/build.gradle.kts`

Remove lines 77-79 (youtubedl-android library/ffmpeg/aria2c). These are already in `core/data/build.gradle.kts` (lines 22-24) and resolve transitively via `:core:data` dependency (line 59).

**Verification**: `./gradlew :app:dependencies --configuration debugRuntimeClasspath | grep youtubedl` must still show all three artifacts.

### Phase 3: R8 Minification (FR-003, FR-004, FR-005)

**File**: `app/build.gradle.kts`

In the `release` buildType:
- Change `isMinifyEnabled = false` to `isMinifyEnabled = true`
- Add `isShrinkResources = true`

**File**: `app/proguard-rules.pro`

Replace template comments with keep rules for:
- youtubedl-android (JNI/reflection-loaded)
- Room (generated code)
- Firebase Auth + Firestore
- Play Billing
- Kotlin serialization
- Hilt DI
- Source file/line number attributes for crash reports

### Phase 4: Verification (FR-008, FR-009)

1. `./gradlew assembleDebug` — must succeed
2. `./gradlew test` — all unit tests must pass
3. Inspect debug APK size — should be ~90-110MB
4. Do NOT run `assembleRelease` (signing key may not be available)

## Complexity Tracking

No constitution violations — this section is intentionally empty.
