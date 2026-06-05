# Agent Instructions for SocialVideoDownloader

## Purpose

This file is the compact, always-on contract for coding agents in this repo. Keep it short. Detailed procedures, examples, and long checklists live in `.agents/skills/<name>/SKILL.md`.

## Project overview

- Personal Android + KMP video downloader: URL in → video file out. No ads, no backend dependency for core flow.
- Kotlin 2.2.10, Jetpack Compose / Compose Multiplatform, Material 3, Hilt on Android, Koin in shared/iOS, Room/KMP, Ktor, Coil, Firebase/Play Billing where already present.
- Extraction: `youtubedl-android` on Android plus optional FastAPI/WebSocket extraction proxy under `server/`.
- Main areas: `app`, `feature/*`, `core/*`, `shared/*`, `iosApp/`, `server/`, `specs/`.

## Critical rules

1. Compose-only UI; never add XML layouts or fragments.
2. Use KSP only; never introduce kapt.
3. Inject coroutine dispatchers; do not hardcode `Dispatchers.IO/Main` in production code except at composition roots/platform adapters.
4. Keep domain/repository contracts in `:core:domain`; Android implementations in `:core:data`; KMP implementations in `shared/*` where applicable.
5. Feature modules must not depend on each other directly; share via `core/*` or `shared/*` contracts.
6. New dependencies go through Gradle version catalogs/convention plugins first.
7. User-facing strings must be resources/extractable; no hardcoded strings in composables.
8. Save Android downloads through MediaStore under `Downloads/SocialVideoDownloader/`; keep scoped-storage compatibility.
9. yt-dlp work runs off the main thread; initialize/update `YoutubeDL` from app/platform setup only.
10. Never commit secrets, keystores, `local.properties`, Firebase private config, tokens, or production credentials.
11. Do not suppress lint/build/module-boundary failures; fix the underlying architecture.
12. Keep the app focused: no analytics, auth, or backend requirements beyond explicitly requested opt-in features.

## Architecture defaults

- Pattern: MVI/MVVM with immutable UiState and one-shot events where needed.
- State: `StateFlow`/`SharedFlow`; ViewModels expose state, UI sends intents/actions.
- Navigation: single-activity Compose Navigation on Android; JetBrains Navigation Compose/shared shell where used; pass navigation as lambdas.
- Persistence: Room for download history/library metadata; DataStore/Settings for preferences; platform storage abstractions for files.
- Tests: prioritize use cases and ViewModels with JUnit5, MockK, Turbine; UI tests are optional unless requested.

## Task workflow

- When user requests a task: create todos only; do not execute implementation until explicit `start` / `старт` if the task is not already phrased as an execution request.
- For unclear or large tasks, use `/session-start` to assemble a Task Brief.
- Before non-trivial edits, inspect relevant files and similar implementations.
- Ask before high-risk architecture, security, storage, or cross-platform decisions.
- Run only relevant checks; avoid expensive full builds unless needed or requested.

## Agent artifacts and personal rules

- Agent-generated artifacts go under `ai/`: `ai/specs/` for plans/tasks, `ai/temp/` for scratch, `ai/personal/AGENTS.md` for per-developer overrides. `ai/` is gitignored.
- Per-developer overrides are layered over this file by local agent bridges (for example root `CLAUDE.md` or `.pi/APPEND_SYSTEM.md`). On conflict, personal rules take precedence.
- Shared skills live in `.agents/skills/`. Prefer adding/updating a skill over growing this file.

## Common commands

```bash
./gradlew assembleDebug
./gradlew test
./gradlew ktlintCheck
./gradlew ktlintFormat
./gradlew connectedAndroidTest
```

For KMP/iOS-related checks, exclude unsupported iOS native compile tasks when the current dependency set requires it:

```bash
./gradlew ktlintCheck -x compileKotlinIosArm64 -x compileKotlinIosSimulatorArm64
```

## Skills routing

| Task | Use skill |
|---|---|
| First clone / repair agent wiring | `/initial-project-setup` |
| Agent docs, bridges, local personal rules, shared skills | `/agent-workflow` |
| New or unclear task intake | `/session-start` |
| New feature/screen/ViewModel/module | `/feature-scaffold` |
| Compose UI/design system/strings | `/compose-ui-guidelines` |
| Architecture, DI, module boundaries, navigation | `/architecture-and-di` |
| yt-dlp/video info/download/storage flow | `/download-flow-ytdlp` |
| KMP/shared/iOS migration or bridge work | `/kmp-ios` |
| Spec Kit feature workflow | `/spec-kit` |
| Tests, pre-PR checks, review checklist | `/testing-and-preflight` |
| Gradle/build/ktlint failures | `/gradle-troubleshooting` |
| Merge a branch/PR, branch naming, merge style | `/merge-branch` |

## When in doubt

Search for a similar existing implementation first, load the relevant skill, and choose the smallest safe change. If requirements are ambiguous, ask instead of guessing.
