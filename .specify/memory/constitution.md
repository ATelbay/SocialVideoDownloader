<!--
  SYNC IMPACT REPORT
  ==================
  Version change: 1.1.0 → 2.0.0
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

# Social Video Downloader Constitution

## Core Principles

### I. Privacy & Zero Bloat (NON-NEGOTIABLE)

- The app MUST NOT include advertising, analytics, tracking, or telemetry of any kind.
- The app MUST NOT require or offer user-facing authentication, accounts, or profiles.
  Exception: anonymous authentication (no sign-in UI, no user identity) is permitted
  solely for opt-in cloud features governed by Principle VIII.
- The app MUST NOT transmit user data off-device beyond the video download itself.
  Exception: opt-in cloud features governed by Principle VIII MAY transmit
  on-device-encrypted data. Unencrypted user data MUST NOT leave the device.
- No network interceptors, crash reporters, or third-party SDKs that phone home.
- Rationale: this is a personal utility built to replace sketchy ad-filled download
  sites. Any data collection or monetization defeats the entire purpose. The
  Principle VIII exceptions are narrowly scoped to preserve this guarantee.

### II. On-Device Architecture

- All video extraction, parsing, and downloading MUST happen on the device.
- The app MUST NOT depend on any backend service, API proxy, or cloud function
  for its core download functionality.
- yt-dlp (via youtubedl-android) is the sole extraction engine and runs locally.
- FFmpeg and aria2c run as local binaries bundled with the app.
- Rationale: no backend means zero hosting costs, zero downtime, and full
  user control. yt-dlp's 1700+ site support makes a backend unnecessary.

### III. Modern Android Stack (Compose + KSP + MVI)

- UI MUST be built exclusively with Jetpack Compose + Material 3.
  Dynamic Color is optional; a fixed branded palette is acceptable when
  design consistency across devices is required.
  XML layouts and Android View system are forbidden for new code.
- Code generation MUST use KSP only. kapt is forbidden (build performance).
- Fragments are forbidden. Navigation uses Compose Navigation with composable
  destinations in a Single Activity architecture.
- Architecture MUST follow MVI: ViewModel exposes a single `StateFlow<UiState>`,
  receives a sealed `Intent`. UiState and Intent are always `sealed interface`.
- DI MUST use Hilt with KSP annotation processing.
- Language: Kotlin 2.0+ only. Java is forbidden for new code.

### IV. Modular Separation

- The codebase MUST follow the defined module structure:
  `:app`, `:feature:download`, `:feature:history`,
  `:core:domain`, `:core:data`, `:core:ui`.
- Repository pattern: interfaces in `:core:domain`, implementations in `:core:data`.
- Use cases MUST be single-purpose classes with `operator fun invoke()`.
- Coroutine dispatchers MUST be injected, never hardcoded (`Dispatchers.IO`).
- Feature modules MUST NOT define their own `NavHost`.
- All user-facing strings MUST use string resources (no hardcoded text in composables).

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
- Authentication MUST be anonymous: no user-facing sign-in flow, no email,
  no password, no OAuth consent screen. Firebase Anonymous Auth or
  equivalent device-bound tokens are the only permitted mechanism.
- Cloud features MUST degrade gracefully:
  - When the device is offline: silent no-op, no error UI.
  - When the user has disabled cloud features: no background network
    activity, no prompts to re-enable.
  - When the cloud service is unavailable: local-first behavior continues
    uninterrupted.
- Cloud features MUST NOT introduce mandatory backend dependencies.
  The app's core download flow (Principle II) MUST remain fully on-device.
- Rationale: cloud features add value (cross-device history, backup) but
  MUST NOT compromise the app's privacy-first, offline-first identity.
  Zero-knowledge encryption and anonymous auth ensure that even if a cloud
  provider is compromised, user data remains protected.

## Tech Stack & Constraints

- **Language**: Kotlin 2.0+ (Java forbidden for new code)
- **UI**: Jetpack Compose + Material 3
- **Architecture**: MVI (`sealed interface` for State + Intent)
- **DI**: Hilt with KSP
- **Database**: Room with KSP (download history)
- **Extraction**: youtubedl-android (yt-dlp) + FFmpeg + aria2c
- **Images**: Coil (video thumbnails)
- **Async**: Coroutines + StateFlow / SharedFlow
- **Build**: Gradle KTS, version catalogs
- **Min SDK**: 26 (Android 8.0) | **Target SDK**: 36 (Android 16)
- **Naming**: `com.socialvideodownloader.{module}.{layer}` package convention
- **Composables**: PascalCase with Screen/Content/Item suffix
- **ViewModels**: `{Feature}ViewModel`
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

**Version**: 2.0.0 | **Ratified**: 2026-03-14 | **Last Amended**: 2026-03-21
