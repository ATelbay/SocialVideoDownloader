<!--
  SYNC IMPACT REPORT
  ==================
  Version change: 2.0.0 → 3.0.0
  Modified principles:
    - I. Privacy & Zero Bloat: Added exception clause for opt-in cloud
      features governed by Principle VIII. Core prohibitions unchanged.
    - VII. Simplicity & Focus: "cloud sync" and "monetization" moved from
      permanently out of scope to conditionally allowed. Cloud sync is
      allowed as opt-in without affecting offline-first behavior.
      Monetization is allowed only as non-intrusive freemium (no ads,
      no subscriptions, no paywalls on core functionality).
  Added sections:
    - VIII. Optional Cloud Features (new principle)
  Removed sections: N/A
  Templates requiring updates:
    ✅ plan-template.md — "Constitution Check" is dynamic, no changes needed
    ✅ spec-template.md — generic, compatible with new principles
    ✅ tasks-template.md — generic, compatible with new principles
    ✅ No command files exist in .specify/templates/commands/
  Follow-up TODOs:
    - Future specs involving cloud features MUST include a Principle VIII
      compliance section in their Constitution Check.
    - CLAUDE.md "What NOT to do" list should be reviewed when cloud sync
      work begins (currently says "Do NOT add authentication of any kind").
-->

<!--
  SYNC IMPACT REPORT
  ==================
  Version change: 3.0.0 → 4.0.0
  Modified principles:
    - II. On-Device Architecture: Renamed to "On-Device Architecture (Android) /
      Server-Mediated Architecture (iOS)". Added iOS-specific clause: iOS uses
      the yt-dlp API server exclusively for extraction and download because
      Apple §2.5.2 prohibits embedded interpreters. Android behaviour unchanged.
    - III. Modern Android Stack: Broadened to "Modern Stack". iOS uses SwiftUI
      (not Compose) and shared KMP modules use Koin (not Hilt). Both are
      explicitly permitted alongside the existing Android-only stack.
    - IV. Modular Separation: Expanded module list to include the new
      :shared:network, :shared:data, :shared:feature-download,
      :shared:feature-history, :shared:feature-library KMP modules with
      commonMain/androidMain/iosMain source sets.
    - VIII. Optional Cloud Features: Added Sign in with Apple as a permitted
      authentication mechanism on iOS. App Store rules require it when any
      third-party sign-in is offered. Google Sign-In remains the Android
      mechanism; both are needed for cross-platform cloud identity.
  Added sections: N/A
  Removed sections: N/A
  Templates requiring updates:
    ✅ plan-template.md — "Constitution Check" is dynamic, no changes needed
    ✅ spec-template.md — generic, compatible with new principles
    ✅ tasks-template.md — generic, compatible with new principles
  Follow-up TODOs:
    - All specs touching shared KMP modules MUST verify Principle IV compliance
      (new shared module structure).
    - iOS feature specs MUST cite Principle II iOS clause justifying server use.
    - Cloud features on iOS MUST use Sign in with Apple per Principle VIII.
-->

# Social Video Downloader Constitution

## Core Principles

### I. Privacy & Zero Bloat (NON-NEGOTIABLE)

- The app MUST NOT include advertising, analytics, tracking, or telemetry of any kind.
- The app MUST NOT require or offer user-facing authentication, accounts, or profiles
  for its core functionality.
  Exception: Google Sign-In via Credential Manager is permitted solely for opt-in
  cloud features governed by Principle VIII.
- The app MUST NOT transmit user data off-device beyond the video download itself.
  Exception: opt-in cloud features governed by Principle VIII MAY transmit
  on-device-encrypted data. Unencrypted user data MUST NOT leave the device.
- No network interceptors, crash reporters, or third-party SDKs that phone home.
- Rationale: this is a personal utility built to replace sketchy ad-filled download
  sites. Any data collection or monetization defeats the entire purpose. The
  Principle VIII exceptions are narrowly scoped to preserve this guarantee.

### II. On-Device Architecture (Android) / Server-Mediated Architecture (iOS)

- **Android**: All video extraction, parsing, and downloading MUST happen on the
  device. yt-dlp (via youtubedl-android) is the sole extraction engine and runs
  locally. FFmpeg and aria2c run as local binaries bundled with the app.
  The Android app MUST NOT depend on any backend service for its core download
  functionality (server fallback is permitted but the on-device path is primary).
- **iOS**: Video extraction and downloading are performed via the yt-dlp API server.
  Apple §2.5.2 prohibits embedding interpreters or executing downloaded code, making
  on-device yt-dlp execution impossible. The server is a thin, self-hosted wrapper
  running the same open-source tool — there is no viable alternative.
  The iOS app MUST NOT use any third-party hosted extraction service; only the
  user's own server instance (or a trusted self-hosted instance) is permitted.
- Rationale: On Android, no backend means zero hosting costs, zero downtime, and
  full user control. On iOS, the server constraint is unavoidable due to platform
  policy; the self-hosted requirement preserves privacy and user control.

### III. Modern Stack (Android: Compose + KSP + MVI + Hilt | iOS: SwiftUI + KMP + Koin)

- **Android UI**: MUST be built exclusively with Jetpack Compose + Material 3.
  Dynamic Color is optional; a fixed branded palette is acceptable when
  design consistency across devices is required.
  XML layouts and Android View system are forbidden for new code.
- **iOS UI**: MUST be built with SwiftUI. UIKit is forbidden for new code.
  iOS follows the same MVI-equivalent pattern: ObservableObject/StateFlow-backed
  ViewModels expose state; views send intents/actions only.
- **Android DI**: MUST use Hilt with KSP annotation processing.
- **Shared KMP modules DI**: MUST use Koin (Hilt cannot run in commonMain).
  Hilt and Koin coexist in the Android app via a bridge module.
- **Android code generation**: MUST use KSP only. kapt is forbidden (build performance).
- **Android navigation**: Fragments are forbidden. Uses Compose Navigation with
  composable destinations in a Single Activity architecture.
- **Architecture**: Shared KMP ViewModels (in :shared:feature-*) are the canonical
  state machines. They expose `StateFlow<UiState>` and receive a sealed `Intent`.
  Android Compose ViewModels delegate to the shared ViewModel.
  iOS SwiftUI ViewModels wrap the shared ViewModel via SKIE-generated async sequences.
  UiState and Intent are always `sealed interface` (Kotlin) / equivalent Swift enums.
- **Language**: Kotlin 2.0+ for all Kotlin code (Android + shared). Swift 6.x for iOS.
  Java is forbidden for new code.

### IV. Modular Separation

- The codebase MUST follow the defined module structure:
  - **Android app modules**: `:app`, `:feature:download`, `:feature:history`,
    `:core:domain`, `:core:data`, `:core:ui`, `:core:cloud`, `:core:billing`
  - **Shared KMP modules**: `:shared:network`, `:shared:data`,
    `:shared:feature-download`, `:shared:feature-history`, `:shared:feature-library`
  - **iOS app**: `iosApp/` (Swift, outside Gradle)
- Shared KMP modules use `commonMain`/`androidMain`/`iosMain` source sets.
  Android-specific code (Hilt, Compose, MediaStore) stays in androidMain or
  existing Android modules. iOS-specific code goes in iosMain.
- Repository pattern: interfaces in `:core:domain` (KMP), implementations split
  between `:shared:data` (cross-platform logic) and `:core:data` (Android-specific).
- Use cases MUST be single-purpose classes with `operator fun invoke()`.
- Coroutine dispatchers MUST be injected, never hardcoded (`Dispatchers.IO`).
- Android feature modules MUST NOT define their own `NavHost`.
- All user-facing strings MUST use platform string resources:
  string resources on Android, Localizable.strings on iOS.
  No hardcoded user-facing text in composables or SwiftUI views.

### V. Minimal Friction UX

- Maximum 2 taps from URL to download start (direct entry flow).
- Share Sheet flow MUST complete in 3 taps: share → app opens → tap format → downloading.
- Share Sheet integration (`ACTION_SEND`) is the primary UX entry point.
- "Best quality" MUST be pre-selected as the default format.
- No settings screen in MVP — sane defaults only.
- Error messages MUST be human-readable with actionable suggestions
  (not raw exception text).

### VI. Test Discipline

- All use cases MUST have unit tests (JUnit5 + MockK + Turbine for Flow).
- ViewModels MUST have tests covering state transitions.
- No UI/instrumentation tests required in MVP.
- ktlint MUST pass before merge.
- Every user-facing feature MUST handle these error scenarios:
  no network, invalid URL, extraction failure, storage full.

### VII. Simplicity & Focus

- This is a focused utility, not a framework or platform.
- Do NOT over-engineer: no abstractions without immediate concrete need.
- Features that are permanently out of scope:
  built-in video player, social features, web/desktop versions, user accounts.
- Features that are conditionally allowed:
  - **Cloud sync/backup**: Allowed only as an opt-in feature that does not
    affect offline-first behavior. MUST comply with Principle VIII.
  - **Monetization**: Allowed only as non-intrusive freemium. No ads, no
    subscriptions, no paywalls on core functionality (URL → video download).
    Core features MUST remain fully functional without payment.
- YAGNI applies: build only what is needed now.
- Rationale: scope creep turns utilities into bloatware.
  Every feature added is a feature to maintain. Conditional allowances
  are narrowly scoped to prevent erosion of the app's core identity.

### VIII. Optional Cloud Features

- Cloud features (sync, backup, cross-device history) are opt-in only.
  The app MUST function fully without them. Disabling or never enabling
  cloud features MUST NOT degrade any local functionality.
- All user data MUST be encrypted on-device before upload to any cloud
  service. The cloud provider MUST NOT have access to plaintext user data
  (zero-knowledge principle).
- Authentication for cloud features MUST use platform-appropriate sign-in:
  - **Android**: Google Sign-In via Credential Manager. This provides a stable
    identity that persists across reinstalls and devices, making cloud backup
    actually recoverable.
  - **iOS**: Sign in with Apple is REQUIRED alongside Google Sign-In. Apple App
    Store Review requires Sign in with Apple when any third-party sign-in is
    offered (App Store Review Guideline 4.8). Both mechanisms MUST be supported
    on iOS so that users can choose, and so Android and iOS users can share cloud
    history (Google identity bridges both platforms; Apple identity is iOS-only).
  - No email/password auth, no custom OAuth flows on either platform.
- Cloud features MUST degrade gracefully:
  - When the device is offline: silent no-op, no error UI.
  - When the user has disabled cloud features: no background network
    activity, no prompts to re-enable.
  - When the cloud service is unavailable: local-first behavior continues
    uninterrupted.
- Cloud features MUST NOT introduce mandatory backend dependencies.
  The Android app's core download flow (Principle II) MUST remain fully on-device.
  The iOS server dependency (Principle II iOS clause) is pre-existing and separate
  from cloud feature backend dependencies.
- Rationale: cloud features add value (cross-device history, backup) but
  MUST NOT compromise the app's privacy-first, offline-first identity.
  Zero-knowledge encryption ensures that even if a cloud provider is
  compromised, user data remains protected. Google Sign-In provides a
  stable identity that survives app reinstalls and device changes, making
  cloud backup genuinely recoverable.

## Tech Stack & Constraints

- **Language (Android + shared)**: Kotlin 2.0+ (Java forbidden for new code)
- **Language (iOS)**: Swift 6.x
- **Android UI**: Jetpack Compose + Material 3
- **iOS UI**: SwiftUI (iOS 16.0+)
- **Architecture**: MVI (`sealed interface` for State + Intent in Kotlin; equivalent Swift enums on iOS)
- **Android DI**: Hilt with KSP
- **Shared KMP DI**: Koin 4.x
- **Database**: Room KMP (shared schema in commonMain, platform builders in androidMain/iosMain)
- **Networking (shared)**: Ktor 3.x (OkHttp engine on Android, Darwin engine on iOS)
- **iOS↔KMP interop**: SKIE (StateFlow → AsyncSequence, sealed class → Swift enum)
- **Extraction (Android)**: youtubedl-android (yt-dlp) + FFmpeg + aria2c (on-device)
- **Extraction (iOS)**: yt-dlp API server (self-hosted, server-mediated)
- **Images**: Coil (Android video thumbnails)
- **Async**: Coroutines + StateFlow / SharedFlow (Kotlin); async/await + AsyncStream (Swift)
- **Build**: Gradle KTS, version catalogs
- **Min SDK**: 26 (Android 8.0) | **Target SDK**: 36 (Android 16) | **iOS**: 16.0+
- **Naming (Kotlin)**: `com.socialvideodownloader.{module}.{layer}` package convention
- **Composables**: PascalCase with Screen/Content/Item suffix
- **ViewModels**: `{Feature}ViewModel` (Android wrapper), `Shared{Feature}ViewModel` (KMP)
- **Use cases**: verb phrase (`ExtractVideoInfoUseCase`)
- **State types**: `{Feature}UiState`, `{Feature}Intent`
- **Room**: `{Name}Entity`, `{Name}Dao`, `AppDatabase`

## Quality Gates & Git Workflow

### Quality gates

- Unit tests for all use cases and ViewModel state transitions MUST pass.
- ktlint MUST report zero violations.
- Error handling for no-network, invalid-URL, extraction-failure,
  and storage-full MUST be present before a feature is considered complete.

### Git workflow

- Never push directly to `main` — always branch + PR.
- Repository history uses `feature/{name}`, `fix/{name}`, `refactor/{name}`.
- Codex-created branches MUST use `codex/{name}` to satisfy local agent workflow requirements.
- Commit messages follow conventional commits:
  `feat:`, `fix:`, `refactor:`, `test:`, `docs:`.
- Each Spec Kit task = one commit.

## Governance

- This constitution is the authoritative source of architectural and process
  decisions for Social Video Downloader. It supersedes ad-hoc conventions.
- Amendments require: (1) documented rationale, (2) version bump per semver,
  (3) sync impact report verifying template compatibility.
- Version policy: MAJOR for principle removals/redefinitions, MINOR for new
  principles or materially expanded guidance, PATCH for clarifications.
- Compliance review: every PR and spec review MUST verify alignment with
  these principles. Violations MUST be flagged before merge.
- Runtime development guidance lives in `AGENTS.md` for Codex and `.claude/CLAUDE.md` for Claude Code.

**Version**: 4.0.0 | **Ratified**: 2026-03-14 | **Last Amended**: 2026-03-30
