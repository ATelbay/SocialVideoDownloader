# Tasks: CI/CD GitHub Actions for iOS & Android

**Input**: Design documents from `/specs/012-cicd-github-actions/`
**Prerequisites**: plan.md, spec.md, research.md, quickstart.md

**Tests**: No unit tests — this feature is CI/CD infrastructure (workflow files, Xcode project config, build plugin changes). Verification is via build commands, not unit tests.

**Organization**: Tasks are grouped by user story to enable independent implementation and testing of each story.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3)
- Include exact file paths in descriptions

## Path Conventions

- `build-logic/convention/src/main/kotlin/` for convention plugins
- `iosApp/iosApp.xcodeproj/project.pbxproj` for Xcode project changes
- `iosApp/iosApp.xcodeproj/xcshareddata/xcschemes/` for shared schemes
- `.github/workflows/` for CI/CD workflow files
- `app/build.gradle.kts` for Android app build config
- `.gitignore` for git exclusions

---

## Phase 1: Setup

**Purpose**: No shared setup needed — this feature has no new modules, dependencies, or navigation changes. Proceed directly to user stories.

*(Phase intentionally empty — all work maps directly to user stories)*

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: KMP framework binary configuration is a hard prerequisite for US1 (iOS build), US3 (iOS CI), and US5 (iOS release). Must complete before any iOS-related work.

**CRITICAL**: No iOS user story work can begin until this phase is complete.

- [ ] T001 Add `binaries { framework {} }` to iOS targets in `build-logic/convention/src/main/kotlin/KmpLibraryConventionPlugin.kt` — wrap `iosArm64()` and `iosSimulatorArm64()` calls with `.binaries.framework { baseName = project.name.replace("-", "_"); isStatic = true }`, producing framework names matching existing Swift imports (shared_data, shared_feature_download, etc.)

**Checkpoint**: Run `./gradlew :shared:data:tasks --group=build` and confirm `linkDebugFrameworkIosSimulatorArm64` and `embedAndSignAppleFrameworkForXcode` tasks exist.

---

## Phase 3: User Story 1 — iOS App Builds from Command Line (Priority: P1) MVP

**Goal**: `xcodebuild` succeeds from command line on a clean checkout with zero manual Xcode steps.

**Independent Test**: `xcodebuild -project iosApp/iosApp.xcodeproj -scheme iosApp -sdk iphonesimulator -destination 'platform=iOS Simulator,name=iPhone 16' -configuration Debug CODE_SIGNING_ALLOWED=NO build`

### Implementation for User Story 1

- [ ] T002 [US1] Add 5 missing Swift files to `iosApp/iosApp.xcodeproj/project.pbxproj` — add PBXFileReference, PBXBuildFile, PBXGroup, and PBXSourcesBuildPhase entries for: `History/HistoryItemRow.swift`, `History/HistoryDeleteDialog.swift`, `History/CloudBackupView.swift`, `History/UpgradeView.swift`, `Library/LibraryItemRow.swift`
- [ ] T003 [US1] Add `iosApp.entitlements` to `iosApp/iosApp.xcodeproj/project.pbxproj` — add PBXFileReference entry in root iosApp group (not PBXBuildFile), and set `CODE_SIGN_ENTITLEMENTS = iosApp/iosApp.entitlements` in both Debug and Release build settings
- [ ] T004 [US1] Add PBXShellScriptBuildPhase to `iosApp/iosApp.xcodeproj/project.pbxproj` — add a Run Script phase BEFORE the Sources phase in the iosApp target build phases list, calling `embedAndSignAppleFrameworkForXcode` for all 6 KMP modules (:shared:data, :shared:feature-download, :shared:feature-history, :shared:feature-library, :shared:network, :core:domain)
- [ ] T005 [US1] Create shared Xcode scheme at `iosApp/iosApp.xcodeproj/xcshareddata/xcschemes/iosApp.xcscheme` — standard xcscheme XML with Build and Archive actions targeting iosApp target, buildImplicitDependencies=YES
- [ ] T006 [US1] Attempt iOS build and fix Swift compilation errors in `iosApp/iosApp/*.swift` — iterate on SKIE framework import naming mismatches, missing type references, and syntax errors in the 5 previously-uncompiled Swift files until xcodebuild succeeds
- [ ] T007 [US1] Verify iOS build succeeds: `xcodebuild -project iosApp/iosApp.xcodeproj -scheme iosApp -sdk iphonesimulator -destination 'platform=iOS Simulator,name=iPhone 16' -configuration Debug CODE_SIGNING_ALLOWED=NO build`

**Checkpoint**: iOS app builds from command line on a clean checkout. US1 is fully functional.

---

## Phase 4: User Story 2 — Signing Credentials Secured (Priority: P1)

**Goal**: Signing config supports both local file (dev) and environment variables (CI). Git tracking is clean.

**Independent Test**: Verify `.gitignore` has `*.jks`, `keystore.properties`, `app/google-services.json`. Verify `./gradlew assembleRelease` still works locally with `keystore.properties` on disk.

### Implementation for User Story 2

- [ ] T008 [P] [US2] Update signing config in `app/build.gradle.kts` — add `System.getenv()` fallback branch when `keystore.properties` doesn't exist, reading `ANDROID_KEYSTORE_PATH`, `ANDROID_STORE_PASSWORD`, `ANDROID_KEY_ALIAS`, `ANDROID_KEY_PASSWORD` from environment
- [ ] T009 [P] [US2] Broaden `.gitignore` keystore pattern — replace the specific `svd.jks` entry with `*.jks` and verify that `keystore.properties` and `app/google-services.json` entries are already present (research confirmed they are)

**Checkpoint**: Local dev builds work unchanged. CI can pass signing via env vars. No credentials in git tracking.

---

## Phase 5: User Story 3 — PR Validation Catches Build Failures (Priority: P2)

**Goal**: Every PR to main gets automated Android compile/lint/test + iOS build validation.

**Independent Test**: Open a PR with a deliberate compilation error, verify CI job fails. Open a PR with only iOS changes, verify Android jobs are skipped.

**Depends on**: US1 (iOS must build from CLI before iOS CI job works)

### Implementation for User Story 3

- [ ] T010 [US3] Create `.github/workflows/ci.yml` with 5 jobs — `android-compile` (JDK 17, decode google-services.json, `compileDebugKotlin`), `android-lint` (`lint`, upload reports on failure), `android-ktlint` (`ktlintCheck -x compileKotlinIosArm64 -x compileKotlinIosSimulatorArm64`, upload reports on failure), `android-test` (`testDebugUnitTest`, upload test results on failure), `ios-build` (macos-15, JDK 17, Gradle framework build, xcodebuild simulator build CODE_SIGNING_ALLOWED=NO, upload logs on failure). Triggers: `pull_request` to main + `workflow_dispatch`. Concurrency: `ci-${{ github.ref }}` with `cancel-in-progress: true`. Path filters: Android jobs on `app/**, core/**, feature/**, shared/**, build-logic/**, *.gradle.kts, gradle/**`; iOS job on `iosApp/**, shared/**, core/domain/**, build-logic/**, *.gradle.kts, gradle/**`

**Checkpoint**: PR validation runs on every PR. Path filters correctly skip irrelevant jobs.

---

## Phase 6: User Story 4 — Android Release Build on Tag Push (Priority: P3)

**Goal**: Pushing a `v*` tag produces signed APK + AAB artifacts. Optional GitHub Release creation.

**Independent Test**: Push a `v0.1.0-test` tag, verify APK + AAB artifacts appear in the Actions run.

**Depends on**: US2 (signing config must support env vars)

### Implementation for User Story 4

- [ ] T011 [US4] Create `.github/workflows/release.yml` with `android-release` job — triggers on `push tags: v*` + `workflow_dispatch`, concurrency `release-${{ github.ref }}` with `cancel-in-progress: false`. Steps: JDK 17, decode google-services.json, decode keystore from `ANDROID_KEYSTORE_BASE64` to `signing/release.jks`, set `ANDROID_KEYSTORE_PATH` + password env vars, `testDebugUnitTest` (quality gate), `assembleRelease bundleRelease`, upload APK + AAB as artifacts, upload test reports on failure
- [ ] T012 [US4] Add `github-release` job to `.github/workflows/release.yml` — gated by `if: vars.ENABLE_GITHUB_RELEASE == 'true'`, needs: `android-release`. Steps: download Android artifacts, create GitHub Release with tag, attach APK + AAB

**Checkpoint**: Tag push produces signed Android artifacts. GitHub Release is opt-in.

---

## Phase 7: User Story 5 — iOS Signed IPA Release (Priority: P4)

**Goal**: Opt-in iOS release produces a signed IPA as a CI artifact.

**Independent Test**: Set `ENABLE_IOS_RELEASE=true`, configure Apple signing secrets, push a version tag, verify IPA artifact is produced.

**Depends on**: US1 (iOS must build from CLI), US4 (release.yml must exist to add ios-release job)

### Implementation for User Story 5

- [ ] T013 [P] [US5] Create `iosApp/ExportOptions.plist` — method: development, team ID placeholder for `APPLE_TEAM_ID`, bundle ID `com.socialvideodownloader.ios`, provisioning profile mapping
- [ ] T014 [US5] Add `ios-release` job to `.github/workflows/release.yml` — gated by `if: vars.ENABLE_IOS_RELEASE == 'true'`, runs on macos-15. Steps: JDK 17, install Apple signing certificate + provisioning profile from secrets (`APPLE_CERTIFICATE_BASE64`, `APPLE_CERTIFICATE_PASSWORD`, `APPLE_PROVISION_PROFILE_BASE64`, `APPLE_TEAM_ID`, `APPLE_KEYCHAIN_PASSWORD`), Gradle KMP framework build, `sed` substitute `APPLE_TEAM_ID` into `iosApp/ExportOptions.plist` from secret, `xcodebuild archive` + `xcodebuild -exportArchive` with the substituted ExportOptions.plist, upload .ipa as Actions artifact, upload archive/export logs on failure

**Checkpoint**: iOS release produces signed IPA when enabled. Skipped when disabled.

---

## Phase 8: Polish & Cross-Cutting Concerns

**Purpose**: Documentation and validation across all stories

- [ ] T015 [P] Update `specs/012-cicd-github-actions/quickstart.md` with final verified commands and any gotchas discovered during implementation
- [ ] T016 Validate all workflows end-to-end: push a test PR to verify ci.yml, push a test tag to verify release.yml (android-release job only, iOS release requires manual secret setup)

---

## Dependencies & Execution Order

### Phase Dependencies

- **Foundational (Phase 2)**: No dependencies — start immediately. BLOCKS US1, US3, US5
- **US1 (Phase 3)**: Depends on Foundational — BLOCKS US3, US5
- **US2 (Phase 4)**: No dependencies on other stories — can run in parallel with US1. BLOCKS US4
- **US3 (Phase 5)**: Depends on US1 (iOS build must work for iOS CI job)
- **US4 (Phase 6)**: Depends on US2 (signing env vars for CI)
- **US5 (Phase 7)**: Depends on US1 (iOS build) + US4 (release.yml exists)
- **Polish (Phase 8)**: Depends on all stories complete

### User Story Dependencies

```
Foundational (T001)
├── US1: iOS Build (T002-T007) ──┬── US3: PR CI (T010)
│                                 └── US5: iOS Release (T013-T014)
└── US2: Signing (T008-T009) ──── US4: Android Release (T011-T012) ──── US5
```

### Parallel Opportunities

- **T008 + T009** (US2) can run in parallel with each other AND with US1 (T002-T007) — different files entirely
- **T013** (ExportOptions.plist) can run in parallel with T014 setup — different files
- **T015** can run in parallel with T016

### Recommended Execution (Single Developer)

1. T001 (Foundational — framework binaries)
2. T002, T003, T004, T005 (US1 — Xcode project fixes, can batch)
3. T006, T007 (US1 — build + verify)
4. T008, T009 (US2 — signing + gitignore, parallel)
5. T010 (US3 — CI workflow)
6. T011, T012 (US4 — release workflow)
7. T013, T014 (US5 — iOS release)
8. T015, T016 (Polish)

---

## Implementation Strategy

### MVP First (US1 + US2)

1. Complete Phase 2: Foundational (T001)
2. Complete Phase 3: US1 — iOS builds from CLI
3. Complete Phase 4: US2 — Signing secured
4. **STOP and VALIDATE**: iOS builds locally, signing works for both local and CI
5. This alone unblocks all further CI/CD work

### Incremental Delivery

1. US1 + US2 → iOS buildable + signing ready (MVP)
2. US3 → PR validation live on every PR
3. US4 → Android release automated on tag push
4. US5 → iOS release opt-in (requires Apple secrets)
5. Each story adds automation value without breaking previous stories

---

## Notes

- No unit tests in this feature — it's infrastructure (workflow YAML, Xcode project config, Gradle plugin changes)
- Verification is via build commands (`xcodebuild`, `./gradlew assembleRelease`) and CI run observation
- The `project.pbxproj` edits (T002-T004) are the most delicate — pbxproj has a specific format with UUID references
- ShareExtension on disk is out of scope — ignore it
- Developer must manually configure GitHub Secrets before release workflows will pass
