<!--
  SYNC IMPACT REPORT
  ==================
  Version change: (new) → 1.0.0
  Modified principles: N/A (initial creation)
  Added sections:
    - 7 Core Principles (I–VII)
    - Tech Stack & Constraints
    - Quality Gates & Git Workflow
    - Governance
  Removed sections: N/A
  Templates requiring updates:
    ✅ plan-template.md — "Constitution Check" is dynamic, no changes needed
    ✅ spec-template.md — generic, compatible with constitution principles
    ✅ tasks-template.md — generic, compatible with constitution principles
    ✅ No command files exist in .specify/templates/commands/
  Follow-up TODOs: None
-->

# Social Video Downloader Constitution

## Core Principles

### I. Privacy & Zero Bloat (NON-NEGOTIABLE)

- The app MUST NOT include advertising, analytics, tracking, or telemetry of any kind.
- The app MUST NOT require or offer user authentication, accounts, or profiles.
- The app MUST NOT transmit user data off-device beyond the video download itself.
- No network interceptors, crash reporters, or third-party SDKs that phone home.
- Rationale: this is a personal utility built to replace sketchy ad-filled download
  sites. Any data collection or monetization defeats the entire purpose.

### II. On-Device Architecture

- All video extraction, parsing, and downloading MUST happen on the device.
- The app MUST NOT depend on any backend service, API proxy, or cloud function.
- yt-dlp (via youtubedl-android) is the sole extraction engine and runs locally.
- FFmpeg and aria2c run as local binaries bundled with the app.
- Rationale: no backend means zero hosting costs, zero downtime, and full
  user control. yt-dlp's 1700+ site support makes a backend unnecessary.

### III. Modern Android Stack (Compose + KSP + MVI)

- UI MUST be built exclusively with Jetpack Compose + Material 3 (Dynamic Color).
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
  built-in video player, social features, cloud sync/backup,
  monetization, web/desktop versions, user accounts.
- YAGNI applies: build only what is needed now.
- Rationale: scope creep turns utilities into bloatware.
  Every feature added is a feature to maintain.

## Tech Stack & Constraints

- **Language**: Kotlin 2.0+ (Java forbidden for new code)
- **UI**: Jetpack Compose + Material 3 + Dynamic Color
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

**Version**: 1.0.0 | **Ratified**: 2026-03-14 | **Last Amended**: 2026-03-14
