# Implementation Plan: CI/CD GitHub Actions for iOS & Android

**Branch**: `012-cicd-github-actions` | **Date**: 2026-03-30 | **Spec**: [spec.md](spec.md)
**Input**: Feature specification from `/specs/012-cicd-github-actions/spec.md`

## Summary

Add CI/CD automation via GitHub Actions: fix iOS command-line buildability (KMP framework binaries, Xcode project wiring, shared scheme), create PR validation workflows for Android and iOS, and add tag-triggered release workflows with signed Android APK/AAB and opt-in iOS IPA.

## Technical Context

**Language/Version**: Kotlin 2.2.10 (Android + KMP shared), Swift 6.x (iOS)
**Primary Dependencies**: GitHub Actions, Gradle KTS, Xcode 16.x, KMP framework binaries, SKIE 0.10.10
**Storage**: N/A (CI/CD infrastructure — no data model changes)
**Testing**: xcodebuild simulator build validation, `./gradlew testDebugUnitTest`, `./gradlew lint`
**Target Platform**: GitHub Actions runners (ubuntu-latest for Android, macos-15 for iOS)
**Project Type**: CI/CD infrastructure + Xcode project fixes
**Performance Goals**: PR feedback within 15 minutes of push
**Constraints**: macOS runners are expensive — path filters avoid triggering them on Android-only changes; no self-hosted runners
**Scale/Scope**: 2 workflow files, 1 convention plugin update, Xcode project fixes (6 files + script phase + scheme), 1 build.gradle.kts signing update

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principle | Status | Notes |
|-----------|--------|-------|
| I. Privacy & Zero Bloat | PASS | CI/CD does not add analytics, tracking, or data transmission to the app |
| II. On-Device Architecture | PASS | No changes to extraction or download architecture |
| III. Modern Stack | PASS | No new app code — only build infrastructure and Xcode project config |
| IV. Modular Separation | PASS | Framework binary config added to existing convention plugin; no new modules |
| V. Minimal Friction UX | N/A | CI/CD does not affect user-facing UX |
| VI. Test Discipline | PASS | CI enforces test discipline (unit tests as quality gate on every PR and release) |
| VII. Simplicity & Focus | PASS | Two focused workflow files, minimal configuration — no over-engineering |
| VIII. Optional Cloud Features | N/A | No cloud features involved |

**Gate result**: PASS — no violations.

## Project Structure

### Documentation (this feature)

```text
specs/012-cicd-github-actions/
├── spec.md              # Feature specification
├── plan.md              # This file
├── research.md          # Phase 0 research findings
├── quickstart.md        # Phase 1 implementation quickstart
└── tasks.md             # Phase 2 output (created by /speckit.tasks)
```

### Source Code (repository root)

```text
build-logic/convention/src/main/kotlin/
└── KmpLibraryConventionPlugin.kt    # Add binaries { framework {} }

iosApp/
├── iosApp.xcodeproj/
│   ├── project.pbxproj              # Add 6 missing files + Run Script phase
│   └── xcshareddata/xcschemes/
│       └── iosApp.xcscheme          # Create shared scheme
└── iosApp/*.swift                   # Fix compilation errors (if any)

.github/workflows/
├── ci.yml                           # PR validation (Android + iOS)
└── release.yml                      # Tag-triggered release (Android + opt-in iOS)

app/build.gradle.kts                 # Add env var fallback for CI signing
.gitignore                           # Add *.jks pattern
```

**Structure Decision**: No new modules. Changes touch the convention plugin (centralized framework config), Xcode project file, and new workflow files at `.github/workflows/`.

## Implementation Phases

### Phase 1: Make iOS CI-Buildable

**Goal**: `xcodebuild` succeeds from command line on a clean checkout.

#### 1a. Add framework binary config to KmpLibraryConventionPlugin

Update `build-logic/convention/src/main/kotlin/KmpLibraryConventionPlugin.kt` to declare framework binaries for each iOS target:

```kotlin
listOf(iosArm64(), iosSimulatorArm64()).forEach { target ->
    target.binaries.framework {
        baseName = project.name.replace("-", "_")
        isStatic = true
    }
}
```

This produces frameworks named `shared_data`, `shared_feature_download`, etc. — matching existing Swift imports. SKIE (already applied in this plugin) will enhance the Swift interface automatically.

**Files**: `build-logic/convention/src/main/kotlin/KmpLibraryConventionPlugin.kt`

#### 1b. Add 6 missing files to Xcode project

Add to `project.pbxproj`:
- **PBXFileReference**: `HistoryItemRow.swift`, `HistoryDeleteDialog.swift`, `CloudBackupView.swift`, `UpgradeView.swift`, `LibraryItemRow.swift`, `iosApp.entitlements`
- **PBXBuildFile**: All 5 Swift files (entitlements is not a build file)
- **PBXGroup**: Add to appropriate groups (History, Library, root)
- **PBXSourcesBuildPhase**: Add 5 Swift files
- **Build Settings**: Set `CODE_SIGN_ENTITLEMENTS = iosApp/iosApp.entitlements`

**Files**: `iosApp/iosApp.xcodeproj/project.pbxproj`

#### 1c. Add Gradle Run Script build phase

Add a `PBXShellScriptBuildPhase` to the iosApp target, positioned **before** the Sources phase:

```bash
cd "$SRCROOT/.."
./gradlew \
  :shared:data:embedAndSignAppleFrameworkForXcode \
  :shared:feature-download:embedAndSignAppleFrameworkForXcode \
  :shared:feature-history:embedAndSignAppleFrameworkForXcode \
  :shared:feature-library:embedAndSignAppleFrameworkForXcode \
  :shared:network:embedAndSignAppleFrameworkForXcode \
  :core:domain:embedAndSignAppleFrameworkForXcode
```

Note: Xcode passes `CONFIGURATION`, `SDK_NAME`, and `ARCHS` environment variables automatically. The `embedAndSignAppleFrameworkForXcode` task reads these from the environment.

**Files**: `iosApp/iosApp.xcodeproj/project.pbxproj`

#### 1d. Create shared Xcode scheme

Create `iosApp/iosApp.xcodeproj/xcshareddata/xcschemes/iosApp.xcscheme` with Build and Archive actions targeting the iosApp target. Standard xcscheme XML template.

**Files**: `iosApp/iosApp.xcodeproj/xcshareddata/xcschemes/iosApp.xcscheme`

#### 1e. Fix Swift compilation errors

After 1a-1d, attempt build. Fix any:
- SKIE framework naming mismatches (import names may differ from `baseName`)
- Missing type references from KMP modules
- Syntax errors in the 5 previously-uncompiled files

**Files**: Various `iosApp/iosApp/*.swift`

#### 1f. Verify local iOS build

```bash
xcodebuild -project iosApp/iosApp.xcodeproj \
  -scheme iosApp \
  -sdk iphonesimulator \
  -destination 'platform=iOS Simulator,name=iPhone 16' \
  -configuration Debug \
  CODE_SIGNING_ALLOWED=NO \
  build
```

Must succeed before proceeding to Phase 2.

### Phase 2: CI Workflows (PR Validation)

**Goal**: Every PR gets automated Android + iOS build validation.

Create `.github/workflows/ci.yml`:

**Triggers**: `pull_request` to `main`, `workflow_dispatch`
**Concurrency**: `group: ci-${{ github.ref }}`, `cancel-in-progress: true`

**Jobs**:

| Job | Runner | Path Filter | Steps |
|-----|--------|-------------|-------|
| `android-compile` | ubuntu-latest | app/**, core/**, feature/**, shared/**, build-logic/**, *.gradle.kts, gradle/** | JDK 17, decode google-services.json, `compileDebugKotlin` |
| `android-lint` | ubuntu-latest | (same) | JDK 17, decode google-services.json, `lint`, upload reports on failure |
| `android-test` | ubuntu-latest | (same) | JDK 17, decode google-services.json, `testDebugUnitTest`, upload test results on failure |
| `android-ktlint` | ubuntu-latest | (same) | JDK 17, `ktlintCheck -x compileKotlinIosArm64 -x compileKotlinIosSimulatorArm64`, upload reports on failure |
| `ios-build` | macos-15 | iosApp/**, shared/**, core/domain/**, build-logic/**, *.gradle.kts, gradle/** | JDK 17, Gradle framework build, xcodebuild simulator build (CODE_SIGNING_ALLOWED=NO), upload logs on failure |

**Files**: `.github/workflows/ci.yml`

### Phase 3: Release Workflow

**Goal**: Tag push produces signed artifacts.

#### 3a. Update app/build.gradle.kts signing

Add `System.getenv()` fallback when `keystore.properties` doesn't exist:

```kotlin
val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties()
if (keystorePropertiesFile.exists()) {
    keystorePropertiesFile.reader().use { keystoreProperties.load(it) }
}

signingConfigs {
    create("release") {
        if (keystorePropertiesFile.exists()) {
            storeFile = file(keystoreProperties.getProperty("storeFile", "../svd.jks"))
            storePassword = keystoreProperties.getProperty("storePassword", "")
            keyAlias = keystoreProperties.getProperty("keyAlias", "")
            keyPassword = keystoreProperties.getProperty("keyPassword", "")
        } else {
            storeFile = file(System.getenv("ANDROID_KEYSTORE_PATH") ?: "../svd.jks")
            storePassword = System.getenv("ANDROID_STORE_PASSWORD") ?: ""
            keyAlias = System.getenv("ANDROID_KEY_ALIAS") ?: ""
            keyPassword = System.getenv("ANDROID_KEY_PASSWORD") ?: ""
        }
    }
}
```

**Files**: `app/build.gradle.kts`

#### 3b. Update .gitignore

Add `*.jks` pattern (currently only `svd.jks` is listed — broader pattern prevents any keystore from being tracked).

**Files**: `.gitignore`

#### 3c. Create release workflow

Create `.github/workflows/release.yml`:

**Triggers**: `push tags: v*`, `workflow_dispatch`
**Concurrency**: `group: release-${{ github.ref }}`, `cancel-in-progress: false`

**Jobs**:

| Job | Runner | Gate | Steps |
|-----|--------|------|-------|
| `android-release` | ubuntu-latest | always | JDK 17, decode google-services.json, decode keystore from ANDROID_KEYSTORE_BASE64, set env vars, `testDebugUnitTest`, `assembleRelease bundleRelease`, upload APK + AAB |
| `github-release` | ubuntu-latest | `if: vars.ENABLE_GITHUB_RELEASE == 'true'`, needs: android-release | Download artifacts, create GitHub Release with tag, attach APK + AAB |
| `ios-release` | macos-15 | `if: vars.ENABLE_IOS_RELEASE == 'true'` | JDK 17, install Apple certificate + provisioning profile, Gradle framework build, xcodebuild archive + exportArchive, upload IPA |

#### 3d. Create ExportOptions.plist

Create `iosApp/ExportOptions.plist` for iOS archive export:
- Method: development
- Team ID: referenced from `APPLE_TEAM_ID` (substituted at CI runtime)
- Bundle ID: `com.socialvideodownloader.ios`

**Files**: `iosApp/ExportOptions.plist`

## Required GitHub Secrets & Variables

### Secrets (sensitive values)

| Secret | Purpose |
|--------|---------|
| `GOOGLE_SERVICES_JSON` | Firebase config file contents |
| `ANDROID_KEYSTORE_BASE64` | Base64-encoded .jks keystore |
| `ANDROID_STORE_PASSWORD` | Keystore store password |
| `ANDROID_KEY_ALIAS` | Signing key alias |
| `ANDROID_KEY_PASSWORD` | Signing key password |
| `APPLE_CERTIFICATE_BASE64` | Base64-encoded .p12 signing certificate (iOS release only) |
| `APPLE_CERTIFICATE_PASSWORD` | Certificate password (iOS release only) |
| `APPLE_PROVISION_PROFILE_BASE64` | Base64-encoded .mobileprovision (iOS release only) |
| `APPLE_TEAM_ID` | Apple Developer Team ID (iOS release only) |
| `APPLE_KEYCHAIN_PASSWORD` | Temporary keychain password for CI (iOS release only) |

### Repository Variables (non-sensitive toggles)

| Variable | Purpose | Default |
|----------|---------|---------|
| `ENABLE_GITHUB_RELEASE` | Gate GitHub Release creation | unset (disabled) |
| `ENABLE_IOS_RELEASE` | Gate iOS IPA release build | unset (disabled) |

## Complexity Tracking

No constitution violations — no complexity justification needed.

## Post-Phase 1 Constitution Re-Check

| Principle | Status | Notes |
|-----------|--------|-------|
| I. Privacy & Zero Bloat | PASS | No analytics or tracking added |
| II. On-Device Architecture | PASS | No architecture changes |
| III. Modern Stack | PASS | Convention plugin change is build infra only |
| IV. Modular Separation | PASS | Framework config centralised in convention plugin |
| VI. Test Discipline | PASS | CI enforces unit tests on every PR and release |
| VII. Simplicity & Focus | PASS | Two workflow files, minimal config |

**Gate result**: PASS — design aligned with constitution.
