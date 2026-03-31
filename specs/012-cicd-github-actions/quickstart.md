# Quickstart: CI/CD GitHub Actions for iOS & Android

**Branch**: `012-cicd-github-actions` | **Date**: 2026-03-30

## Implementation Order

Work must follow this sequence — each phase has hard dependencies on the previous:

### Phase 1: Make iOS CI-Buildable (P1 — hard blocker)

1. **KmpLibraryConventionPlugin.kt** — Add framework binary config (see baseName convention below)
2. **project.pbxproj** — Add 5 missing Swift files + entitlements to PBXFileReference/PBXBuildFile/PBXGroup/PBXSourcesBuildPhase; set CODE_SIGN_ENTITLEMENTS
3. **project.pbxproj** — Add PBXShellScriptBuildPhase calling `embedAndSignAppleFrameworkForXcode` for all 6 KMP modules; set `ENABLE_USER_SCRIPT_SANDBOXING = NO`
4. **iosApp.xcscheme** — Create shared scheme XML at `xcshareddata/xcschemes/`
5. **Swift fixes** — Build, fix any compilation errors, iterate
6. **Verify** — `xcodebuild ... CODE_SIGNING_ALLOWED=NO build` must pass locally

### Phase 2: CI Workflows (P2)

7. **.github/workflows/ci.yml** — 6 jobs: `changes`, `android-compile`, `android-lint`, `android-ktlint`, `android-test`, `ios-build`

### Phase 3: Release Workflow (P3/P4)

8. **app/build.gradle.kts** — Add `System.getenv()` fallback for signing in CI
9. **.gitignore** — Broaden `svd.jks` to `*.jks`
10. **.github/workflows/release.yml** — 3 jobs: `android-release`, `github-release` (opt-in), `ios-release` (opt-in)
11. **iosApp/ExportOptions.plist** — Create for iOS archive export

## Key Files

| File | Why |
|------|-----|
| `build-logic/convention/src/main/kotlin/KmpLibraryConventionPlugin.kt` | Framework binary config + baseName derivation |
| `iosApp/iosApp.xcodeproj/project.pbxproj` | Xcode project — add files, script phase |
| `iosApp/iosApp/Helpers/KoinHelper.swift` | SKIE typealiases bridging generated names to clean Swift types |
| `iosApp/ExportOptions.plist` | iOS archive export config (uses APPLE_TEAM_ID_PLACEHOLDER) |
| `app/build.gradle.kts` | Signing config with env var fallback |
| `.gitignore` | `*.jks` pattern (broadened from `svd.jks`) |
| `.github/workflows/ci.yml` | CI workflow (6 jobs, path-filtered) |
| `.github/workflows/release.yml` | Release workflow (3 jobs, tag-triggered) |

## Framework baseName Convention

The `baseName` is derived from `project.path` in `KmpLibraryConventionPlugin.kt`:

```kotlin
val frameworkBaseName = project.path.removePrefix(":").replace(":", "_").replace("-", "_")
```

Examples:
| Gradle module | Swift import |
|---|---|
| `:shared:data` | `shared_data` |
| `:shared:feature-download` | `shared_feature_download` |
| `:shared:feature-history` | `shared_feature_history` |
| `:shared:feature-library` | `shared_feature_library` |
| `:core:domain` | `core_domain` |

## SKIE Naming Convention

SKIE flattens sealed interface subtypes and prefixes types by module. Example for `HistoryUiState`:

| Kotlin type | SKIE Swift name |
|---|---|
| `HistoryUiState.Content` | `Feature_historyHistoryUiStateContent` |
| `HistoryUiState.Loading` | `Feature_historyHistoryUiStateLoading` |
| `HistoryUiState.Empty` | `Feature_historyHistoryUiStateEmpty` |
| `DownloadUiState.Idle` | `Feature_downloadDownloadUiStateIdle` |

**KoinHelper.swift** (`iosApp/iosApp/Helpers/KoinHelper.swift`) contains `typealias` declarations that bridge all SKIE-generated names back to clean Swift names (e.g., `typealias HistoryUiStateContent = Feature_historyHistoryUiStateContent`).

## CI Workflow Jobs (ci.yml)

Triggers on: PRs to `main`, `workflow_dispatch`

| Job | Runner | Condition | Key command |
|---|---|---|---|
| `changes` | ubuntu-latest | always | `dorny/paths-filter@v3` |
| `android-compile` | ubuntu-latest | android paths changed | `./gradlew compileDebugKotlin` |
| `android-lint` | ubuntu-latest | android paths changed | `./gradlew lint` |
| `android-ktlint` | ubuntu-latest | android paths changed | `./gradlew ktlintCheck -x compileKotlinIosArm64 -x compileKotlinIosSimulatorArm64` |
| `android-test` | ubuntu-latest | android paths changed | `./gradlew testDebugUnitTest` |
| `ios-build` | macos-15 | ios paths changed | `xcodebuild ... -destination 'platform=iOS Simulator,OS=latest,name=iPhone 16 Pro'` |

Path filter groups:
- **android**: `app/**`, `core/**`, `feature/**`, `shared/**`, `build-logic/**`, `*.gradle.kts`, `gradle/**`
- **ios**: `iosApp/**`, `shared/**`, `core/domain/**`, `build-logic/**`, `*.gradle.kts`, `gradle/**`

iOS build command (CI):
```bash
xcodebuild -project iosApp/iosApp.xcodeproj \
  -scheme iosApp \
  -sdk iphonesimulator \
  -destination 'platform=iOS Simulator,OS=latest,name=iPhone 16 Pro' \
  -configuration Debug \
  CODE_SIGNING_ALLOWED=NO \
  build
```

Local dev build (uses `iPhone 17 Pro` — whatever simulator is available):
```bash
xcodebuild -project iosApp/iosApp.xcodeproj \
  -scheme iosApp \
  -sdk iphonesimulator \
  -destination 'platform=iOS Simulator,name=iPhone 17 Pro' \
  -configuration Debug \
  CODE_SIGNING_ALLOWED=NO \
  build
```

## Release Workflow Jobs (release.yml)

Triggers on: version tags (`v*`), `workflow_dispatch`

| Job | Runner | Gate | Description |
|---|---|---|---|
| `android-release` | ubuntu-latest | always | Build + sign APK and AAB |
| `github-release` | ubuntu-latest | `ENABLE_GITHUB_RELEASE == 'true'` (repo var) | Create GitHub Release with artifacts |
| `ios-release` | macos-15 | `ENABLE_IOS_RELEASE == 'true'` (repo var) | Build KMP frameworks, archive, export IPA |

iOS release KMP step: `./gradlew linkReleaseFrameworkIosArm64`

## Secrets Setup (manual, before release works)

Configure in GitHub repo settings before release workflows succeed:

**Required for all CI**: `GOOGLE_SERVICES_JSON`

**Required for Android release**: `ANDROID_KEYSTORE_BASE64`, `ANDROID_STORE_PASSWORD`, `ANDROID_KEY_ALIAS`, `ANDROID_KEY_PASSWORD`

**Required for iOS release (opt-in)**: `APPLE_CERTIFICATE_BASE64`, `APPLE_CERTIFICATE_PASSWORD`, `APPLE_PROVISION_PROFILE_BASE64`, `APPLE_TEAM_ID`, `APPLE_KEYCHAIN_PASSWORD`

**Opt-in repo variables**: `ENABLE_GITHUB_RELEASE`, `ENABLE_IOS_RELEASE` (set to `'true'` to activate)

## Gotchas

- SKIE wraps framework compilation transparently — task names stay standard (`embedAndSignAppleFrameworkForXcode`)
- The `baseName` in `binaries { framework {} }` must match what Swift files use in `import` statements
- `FRAMEWORK_SEARCH_PATHS` in Xcode is already correct — don't change it
- The Run Script phase must be **before** the Sources phase in build order
- `ENABLE_USER_SCRIPT_SANDBOXING = NO` must be set on the Gradle run script phase or it will fail in Xcode's sandbox
- `ShareExtension/` exists on disk but has no pbxproj target — out of scope, ignore it
- Signing files (`*.jks`, `keystore.properties`, `google-services.json`) are already gitignored and untracked
- The `storeFile` path in keystore.properties points to `../svd.jks` (one level above project root)
- CI uses `iPhone 16 Pro` with `OS=latest`; local dev typically uses `iPhone 17 Pro` — both work with `CODE_SIGNING_ALLOWED=NO`
- `ExportOptions.plist` uses literal `APPLE_TEAM_ID_PLACEHOLDER` — substituted via `sed` in CI before archive export
