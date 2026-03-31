# Research: CI/CD GitHub Actions for iOS & Android

**Branch**: `012-cicd-github-actions` | **Date**: 2026-03-30

## R1: KMP Framework Binary Configuration

**Decision**: Add `binaries { framework {} }` to `KmpLibraryConventionPlugin` centrally, rather than per-module.

**Rationale**: All 6 KMP modules (shared/data, shared/feature-download, shared/feature-history, shared/feature-library, shared/network, core/domain) use either `svd.kmp.library` or `svd.kmp.feature` (which delegates to `svd.kmp.library`). Adding the framework declaration once in the convention plugin covers all modules. SKIE is already applied in the same plugin (line 26) — it transforms the Swift interface but does not produce the `.framework` bundle itself.

**Alternatives considered**:
- Per-module `binaries { framework {} }` in each build.gradle.kts — rejected because it's repetitive and the convention plugin already centralizes iOS target setup
- Umbrella framework (single module re-exporting all others) — rejected because it requires changing all Swift imports and adding a new module

**Key details discovered**:
- `KmpLibraryConventionPlugin.kt` already declares `iosArm64()` and `iosSimulatorArm64()` targets but never declares `binaries { framework {} }`
- SKIE 0.10.10 is applied in the convention plugin — framework names will be SKIE-wrapped
- `FRAMEWORK_SEARCH_PATHS` in Xcode is already set to `$(SRCROOT)/../shared/build/xcode-frameworks/$(CONFIGURATION)/$(SDK_NAME)` — correct path, just nothing produces output there
- Framework `baseName` must match what Swift files import (e.g., `import shared_data`)

## R2: Xcode Project State

**Decision**: Add 5 missing Swift files + entitlements to project.pbxproj, add a Run Script build phase, and create a shared scheme.

**Rationale**: Research confirmed exactly which files are missing and that no build script phase exists at all.

**Current state**:
- 15 Swift files registered in project.pbxproj out of 20 on disk
- Missing from pbxproj: `HistoryItemRow.swift`, `HistoryDeleteDialog.swift`, `CloudBackupView.swift`, `UpgradeView.swift`, `LibraryItemRow.swift`
- Missing from pbxproj: `iosApp.entitlements` (exists on disk, `CODE_SIGN_ENTITLEMENTS` not set)
- No `PBXShellScriptBuildPhase` exists — framework embedding script completely absent
- No shared scheme at `xcshareddata/xcschemes/` — directory doesn't exist
- Build phases: Sources, Frameworks (empty), Resources (Localizable.strings only)
- `ShareExtension/` exists on disk but has no target in pbxproj — out of scope for this feature

**Alternatives considered**:
- Using `xcodegen` to regenerate project from YAML — rejected as overkill for adding 6 files and a script phase
- SPM-based framework linking — rejected, not compatible with KMP framework output

## R3: Signing Credentials Current State

**Decision**: Credentials are already secured in .gitignore. Only need to add env var fallback to build.gradle.kts for CI.

**Rationale**: Research revealed that `.gitignore` already contains `svd.jks`, `keystore.properties`, and `app/google-services.json`. None are git-tracked. The spec's concern about "committed credentials" is outdated — they were removed from tracking previously.

**Current signing config in app/build.gradle.kts**:
```kotlin
val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties()
if (keystorePropertiesFile.exists()) {
    keystorePropertiesFile.reader().use { keystoreProperties.load(it) }
}
signingConfigs {
    create("release") {
        storeFile = file(keystoreProperties.getProperty("storeFile", "../svd.jks"))
        storePassword = keystoreProperties.getProperty("storePassword", "")
        keyAlias = keystoreProperties.getProperty("keyAlias", "")
        keyPassword = keystoreProperties.getProperty("keyPassword", "")
    }
}
```

**What needs to change**: Add `System.getenv()` fallback so CI can pass signing values via environment variables when `keystore.properties` doesn't exist on disk.

**Current .gitignore already contains**:
- `svd.jks`
- `keystore.properties`
- `app/google-services.json`

**Remaining .gitignore addition**: `*.jks` (broader pattern to prevent any keystore from being tracked).

## R4: GitHub Actions Workflow Design

**Decision**: Two workflow files — `ci.yml` (PR validation) and `release.yml` (tag-triggered release).

**Rationale**: Separating CI and release keeps concerns clean. CI runs on every PR; release only on tags/manual dispatch.

**Key design decisions**:
- Path filters using GitHub's `paths` trigger to avoid expensive macOS runners on Android-only changes
- Gradle caching via `actions/setup-java` with `cache: 'gradle'`
- `google-services.json` decoded from `GOOGLE_SERVICES_JSON` secret in both workflows
- iOS CI job needs JDK 17 for Gradle/KMP framework build before xcodebuild
- Concurrency: cancel-in-progress for CI, never cancel for release

**Alternatives considered**:
- Single workflow file with conditional jobs — rejected for readability
- Self-hosted runners — rejected, not needed for initial setup
- Fastlane — rejected as unnecessary complexity for this project's needs

## R5: KMP Framework Task Names with SKIE

**Decision**: Use `embedAndSignAppleFrameworkForXcode` task (standard KMP task, SKIE wraps it transparently).

**Rationale**: SKIE applies a Gradle plugin that intercepts the standard KMP framework tasks. The task name remains `embedAndSignAppleFrameworkForXcode` — SKIE hooks into the framework compilation, not the task lifecycle. The task is generated per-module when `binaries { framework {} }` is declared.

**Task naming pattern**: `:shared:data:embedAndSignAppleFrameworkForXcode` (requires `-PXCODE_CONFIGURATION`, `-PXCODE_SDK_NAME`, `-PXCODE_ARCHS` properties from Xcode).

**For CI (non-Xcode context)**: Use `linkDebugFrameworkIosSimulatorArm64` (or `linkReleaseFrameworkIosArm64` for release) to build frameworks directly without Xcode environment variables.
