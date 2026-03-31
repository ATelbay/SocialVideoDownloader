# Feature Specification: CI/CD GitHub Actions for iOS & Android

**Feature Branch**: `012-cicd-github-actions`
**Created**: 2026-03-30
**Status**: Draft
**Input**: User description: "CI/CD Plan: GitHub Actions for iOS & Android — fix iOS build wiring, add PR validation workflows, Android release automation, and opt-in iOS signed IPA release"

## User Scenarios & Testing *(mandatory)*

### User Story 1 - iOS App Builds from Command Line (Priority: P1)

As a developer, I want the iOS app to build successfully from the command line so that automated CI is possible and I can verify builds without opening Xcode.

**Why this priority**: The iOS app currently cannot build outside Xcode — no shared schemes, no Gradle-to-Xcode framework build phase, and framework search paths point to non-existent directories. This is a hard blocker for all iOS CI automation.

**Independent Test**: Run `xcodebuild` with the shared scheme targeting iOS Simulator and confirm the build succeeds on a clean checkout with no manual Xcode steps.

**Acceptance Scenarios**:

1. **Given** the repo is freshly cloned with no prior build artifacts, **When** I run the xcodebuild command with the shared scheme, **Then** Gradle builds KMP frameworks automatically and xcodebuild compiles the iOS app without errors
2. **Given** the iOS app has Swift files that have never been compiled, **When** I build from the command line, **Then** all Swift files compile without syntax errors, import mismatches, or missing type references
3. **Given** the Xcode project, **When** I list available schemes, **Then** a shared scheme for the iOS app target is listed (not just user-local schemes)
4. **Given** Swift files were added to disk during KMP migration but not to the Xcode project, **When** I build, **Then** all files are included in compilation (HistoryItemRow, HistoryDeleteDialog, CloudBackupView, UpgradeView, LibraryItemRow, iosApp.entitlements)

---

### User Story 2 - Signing Credentials Secured (Priority: P1)

As a developer, I want signing credentials removed from git tracking and stored securely so that credentials are not exposed in the repository.

**Why this priority**: Signing credentials were previously removed from git tracking and `.gitignore` already covers `svd.jks`, `keystore.properties`, and `google-services.json`. The remaining work is: (1) broaden `.gitignore` from the specific `svd.jks` to `*.jks` to prevent any future keystore from being tracked, and (2) add `System.getenv()` fallback in `build.gradle.kts` so CI can sign without a local properties file. Co-equal P1 with iOS buildability since it's a hard prerequisite for release workflows.

**Independent Test**: Verify `.gitignore` has `*.jks`, `keystore.properties`, `app/google-services.json`. Verify `./gradlew assembleRelease` works locally with `keystore.properties` on disk, and that the signing config code path for env vars compiles correctly.

**Acceptance Scenarios**:

1. **Given** the cleanup is complete, **When** I check git status, **Then** keystore properties and keystore files are untracked and listed in `.gitignore`
2. **Given** a local development environment with a keystore properties file on disk, **When** I run a release build, **Then** the build reads signing config from the local file and succeeds
3. **Given** a CI environment without a keystore properties file on disk, **When** the release workflow runs, **Then** it reads signing config from environment variables (decoded from secrets) and succeeds
4. **Given** `google-services.json` contains project-specific config, **When** I check `.gitignore`, **Then** it is listed so CI decodes it from a secret instead

---

### User Story 3 - PR Validation Catches Build Failures (Priority: P2)

As a developer, I want every pull request to be automatically validated for compilation, lint, and test failures on both Android and iOS so that broken code never merges to main.

**Why this priority**: Once iOS builds work (P1) and credentials are secured (P1), the highest-value automation is catching regressions on every PR. This prevents "it works on my machine" problems and establishes a quality gate.

**Independent Test**: Open a PR with a deliberate compilation error and verify the CI job fails and reports the issue.

**Acceptance Scenarios**:

1. **Given** a PR that modifies Android code, **When** the PR is opened, **Then** Android compile, lint, and unit test jobs run automatically
2. **Given** a PR that modifies only iOS-related files, **When** the PR is opened, **Then** the iOS build job runs but Android jobs are skipped (path filtering)
3. **Given** a PR that modifies only Android-specific files, **When** the PR is opened, **Then** Android jobs run but the iOS build job is skipped
4. **Given** a PR with a compilation error, **When** CI runs, **Then** the relevant compile job fails and the PR is blocked from merging
5. **Given** a PR with failing unit tests, **When** CI runs, **Then** test reports are uploaded as artifacts for debugging
6. **Given** two pushes to the same PR in quick succession, **When** CI triggers, **Then** the first run is cancelled and only the latest commit is validated

---

### User Story 4 - Android Release Build on Tag Push (Priority: P3)

As a developer, I want to push a version tag (e.g., `v0.1.0`) and have CI automatically produce a signed release APK and AAB so that I don't need to build releases manually.

**Why this priority**: Automated release builds eliminate manual signing steps and ensure reproducible builds. Depends on credential rotation (P1) being resolved first.

**Independent Test**: Push a version tag and verify APK + AAB artifacts appear in the CI run.

**Acceptance Scenarios**:

1. **Given** signing credentials are stored as secrets (not committed to repo), **When** the release workflow runs, **Then** it decodes the keystore and signs the APK/AAB correctly
2. **Given** a version tag is pushed, **When** CI runs, **Then** unit tests execute first as a quality gate before building release artifacts
3. **Given** the release build succeeds, **When** artifacts are uploaded, **Then** both APK and AAB files are available as downloadable CI artifacts
4. **Given** GitHub Release creation is enabled via a repository variable, **When** the release build succeeds, **Then** a GitHub Release is created with the tag and the APK + AAB attached
5. **Given** GitHub Release creation is not enabled, **When** the release build succeeds, **Then** no GitHub Release is created (artifacts are still available in the CI run)

---

### User Story 5 - iOS Signed IPA Release (Priority: P4)

As a developer, I want an opt-in workflow that produces a signed IPA for iOS so that I can distribute development builds to registered test devices.

**Why this priority**: iOS release is lower priority than Android since the iOS app is newer and the signing setup requires Apple Developer Program enrollment. This is opt-in via a repository variable.

**Independent Test**: Enable the iOS release flag, configure Apple signing secrets, push a version tag, and verify an IPA artifact is produced.

**Acceptance Scenarios**:

1. **Given** iOS release is enabled and Apple signing secrets are configured, **When** a version tag is pushed, **Then** the iOS release job builds KMP frameworks, archives the app, and exports a signed IPA
2. **Given** iOS release is disabled or unset, **When** a version tag is pushed, **Then** the iOS release job is skipped entirely
3. **Given** the IPA is built successfully, **When** the job completes, **Then** the IPA is uploaded as a CI artifact (not attached to the GitHub Release)
4. **Given** the archive or export fails, **When** the job completes, **Then** build logs are uploaded as artifacts for debugging

---

### Edge Cases

- What happens when the Gradle framework build fails during Xcode's build phase? The xcodebuild should fail with a clear Gradle error, not a cryptic "framework not found" message.
- What happens when a CI runner doesn't have the required Xcode version? The iOS job should fail fast with a clear error about Xcode version mismatch.
- What happens when the Firebase config secret is not configured? The Android jobs should fail with a clear message about the missing secret, not a cryptic build error.
- What happens when Apple signing secrets are partially configured (e.g., certificate but no provisioning profile)? The iOS release job should fail with a descriptive error identifying the missing secret.
- What happens when multiple version tags are pushed simultaneously? Each tag should trigger its own independent release workflow run (no concurrency cancellation for releases).
- What happens when the build cache is corrupted on a CI runner? The workflow should still succeed (slower) since caching is an optimization, not a requirement.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: Each KMP module imported by Swift MUST produce a framework binary that the Xcode build can link against
- **FR-002**: The Xcode project MUST include a build phase that invokes Gradle to produce KMP frameworks before Swift compilation
- **FR-003**: The Xcode project MUST contain a shared scheme (not user-local) that enables command-line builds
- **FR-004**: All Swift source files and entitlements created during KMP migration MUST be registered in the Xcode project file
- **FR-005**: PR validation MUST run Android compile, lint, and unit test jobs on every pull request to main
- **FR-006**: PR validation MUST run an iOS simulator build job on every pull request to main
- **FR-007**: CI jobs MUST use path filters so iOS-only changes skip Android jobs and vice versa
- **FR-008**: PR CI runs MUST cancel previous in-progress runs for the same PR when new commits are pushed
- **FR-009**: The release workflow MUST trigger on version tag pushes (pattern: `v*`) and manual dispatch
- **FR-010**: The Android release job MUST run unit tests as a quality gate before building release artifacts
- **FR-011**: The Android release job MUST produce both a signed APK and a signed AAB as downloadable artifacts
- **FR-012**: GitHub Release creation MUST be gated behind a repository variable
- **FR-013**: iOS release MUST be gated behind a repository variable
- **FR-014**: Signing credentials (keystore, passwords) MUST NOT be committed to the repository
- **FR-015**: The build system MUST support dual signing sources: local file for development, environment variables for CI
- **FR-016**: Firebase config MUST be decoded from a secret in CI, not committed to the repository
- **FR-017**: Failed CI jobs MUST upload relevant reports/logs as artifacts (test results, lint reports, build logs)
- **FR-018**: Release workflow concurrency MUST NOT cancel in-progress runs (unlike PR CI which cancels stale runs)
- **FR-019**: PR validation MUST run ktlintCheck as a quality gate on every pull request to main

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: iOS app builds successfully from the command line on a clean checkout with zero manual configuration steps
- **SC-002**: Every pull request to main receives automated build validation feedback within 15 minutes of opening
- **SC-003**: Android-only PRs do not trigger macOS runner usage (path filtering works correctly)
- **SC-004**: Pushing a version tag produces downloadable signed APK and AAB artifacts without any manual build steps
- **SC-005**: Zero signing credentials or service config files exist in git tracking after cleanup
- **SC-006**: Local development workflow is unaffected — developers can still build debug and release variants using local files without any CI-specific setup
- **SC-007**: Failed CI runs provide actionable diagnostic artifacts (test reports, lint results, build logs) that allow debugging without reproducing locally
- **SC-008**: iOS release (when enabled) produces an installable IPA that launches on a registered test device

### Assumptions

- GitHub Actions is the CI/CD platform (no self-hosted runners needed for initial setup)
- macOS runners with Xcode 16.x are available in the GitHub Actions runner pool
- The developer will manually configure secrets and repository variables before enabling release workflows
- The developer will generate a new Android keystore and rotate credentials as a manual step before release automation
- Apple Developer Program enrollment is a prerequisite for iOS release signing (handled outside this feature)
- JDK 17 is sufficient for all Gradle/KMP build tasks
- The existing keystore files will remain on disk locally (just removed from git tracking)
