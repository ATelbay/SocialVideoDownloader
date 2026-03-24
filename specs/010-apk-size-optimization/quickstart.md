# Quickstart: APK Size Optimization

**Feature**: 010-apk-size-optimization | **Date**: 2026-03-25

## What This Feature Does

Reduces APK size by ~50-65% through three build configuration changes:
1. ABI filtering — removes unused native library architectures
2. R8 minification — shrinks and optimizes DEX code in release builds
3. Dependency cleanup — removes duplicate declarations from :app

## Files Modified

| File | Changes |
|------|---------|
| `app/build.gradle.kts` | Add `ndk.abiFilters` in defaultConfig and release buildType; enable `isMinifyEnabled = true` + `isShrinkResources = true` in release; remove 3 duplicate youtubedl-android dependency lines |
| `app/proguard-rules.pro` | Add keep rules for youtubedl-android, Room, Firebase, Play Billing, Kotlin serialization, Hilt |

## How to Verify

```bash
# 1. Build debug APK
./gradlew assembleDebug

# 2. Check APK size (should be ~90-110MB, down from ~230MB)
ls -lh app/build/outputs/apk/debug/app-debug.apk

# 3. Verify only expected ABIs are included
unzip -l app/build/outputs/apk/debug/app-debug.apk | grep "lib/"

# 4. Run unit tests
./gradlew test

# 5. Verify transitive dependency resolution
./gradlew :app:dependencies --configuration debugRuntimeClasspath | grep youtubedl
```

## Important Constraints

- Do NOT modify the `packaging { }` block — `jniLibs.useLegacyPackaging = true` is required by youtubedl-android
- Do NOT run `assembleRelease` without a signing key configured in `keystore.properties`
- The irreducible APK floor is ~55-70MB (FFmpeg + Python/yt-dlp + aria2c for a single ABI)
