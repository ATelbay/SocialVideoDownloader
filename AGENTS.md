# SocialVideoDownloader Development Guidelines

Auto-generated from all feature plans. Last updated: 2026-03-15

## Active Technologies

- Kotlin 2.2.10 + Jetpack Compose (BOM 2026.03.00), Hilt 2.59.2, Room 2.8.4, Navigation Compose 2.9.7, youtubedl-android 0.18.0 (+ FFmpeg + Aria2c), Coil 2.7.0, kotlinx-serialization 1.7.3 (002-core-download-flow)
- Room (download history), MediaStore (downloaded files) (002-core-download-flow)
- Kotlin 2.2.10 + Jetpack Compose (BOM 2026.03.00), Dagger/Hilt 2.56, Room 2.8.4, Navigation Compose 2.9.7, youtubedl-android 0.18.x, Coil 2.7.0, kotlinx-serialization 1.7.3 (001-project-foundation)

## Project Structure

```text
:app
:feature:download
:feature:history
:core:domain
:core:data
:core:ui
specs/
```

## Commands

- `./gradlew assembleDebug` - build debug APK
- `./gradlew assembleRelease` - build release APK
- `./gradlew test` - run unit tests
- `./gradlew connectedAndroidTest` - run instrumentation tests
- `./gradlew ktlintCheck` - run lint/style checks

## Code Style

Kotlin 2.2.10: Follow standard conventions, Compose-only UI, KSP instead of kapt, repository interfaces in `:core:domain` with implementations in `:core:data`

## Recent Changes

- 002-core-download-flow: Added Kotlin 2.2.10 + Jetpack Compose (BOM 2026.03.00), Hilt 2.59.2, Room 2.8.4, Navigation Compose 2.9.7, youtubedl-android 0.18.0 (+ FFmpeg + Aria2c), Coil 2.7.0, kotlinx-serialization 1.7.3
- 001-project-foundation: Added Kotlin 2.2.10 + Jetpack Compose (BOM 2026.03.00), Dagger/Hilt 2.56, Room 2.8.4, Navigation Compose 2.9.7, youtubedl-android 0.18.x, Coil 2.7.0, kotlinx-serialization 1.7.3

<!-- MANUAL ADDITIONS START -->
## Project Overview

Android video downloader. URL in, video file out. No backend, no ads, no auth. Personal utility built on yt-dlp through `youtubedl-android`.

## Architecture Notes

- Pattern: MVI
- UI: Jetpack Compose + Material 3 + Dynamic Color
- Navigation: single-activity Compose Navigation, no fragments
- DI: Hilt with KSP, never kapt
- Async: Coroutines + `StateFlow`/`SharedFlow`
- Storage: Room for download history
- Extraction: `youtubedl-android` with FFmpeg and aria2c
- Images: Coil for thumbnails
- SDKs: min 26, target 36

## Naming Conventions

- Packages: `com.socialvideodownloader.{module}.{layer}`
- Composables: PascalCase with `Screen`, `Content`, or `Item` suffixes
- ViewModels: `{Feature}ViewModel`
- Use cases: verb phrase with `UseCase` suffix
- Room types: `{Name}Entity`, `{Name}Dao`, `AppDatabase`
- State containers: `{Feature}UiState`, `{Feature}Intent`

## Spec Kit For Codex

- Use Spec Kit only for new features. Bug fixes and small tweaks should be implemented directly.
- Codex runtime guidance lives in `AGENTS.md`; Claude-specific guidance remains in `.claude/CLAUDE.md`.
- The current baseline spec artifacts are under `specs/001-project-foundation/`. There is no `docs/PRODUCT_SPEC.md` in this repo right now.
- `/speckit.specify` equivalent: run `.specify/scripts/bash/create-new-feature.sh --json --short-name "<short-name>" "<feature description>"`, then fill the generated `spec.md`.
- `/speckit.plan` equivalent: run `.specify/scripts/bash/setup-plan.sh --json`, complete `plan.md`, generate design artifacts, then run `.specify/scripts/bash/update-agent-context.sh codex`.
- If a plan step says to update the agent context, always target `codex` from this environment so `AGENTS.md` stays in sync.

## yt-dlp Notes

- Initialize `YoutubeDL.getInstance().init(context)` in `Application.onCreate()`
- Run yt-dlp work on `Dispatchers.IO`
- Use `getInfo()` for format selection before download
- Update the yt-dlp binary periodically with `updateYoutubeDL()`
- Save downloads through MediaStore under `Downloads/SocialVideoDownloader/`

## Testing And Git

- Prioritize unit tests for use cases and ViewModels with JUnit5, MockK, and Turbine
- UI tests are deferred for MVP
- Never push directly to `main`
- Repository history uses `feature/`, `fix/`, and `refactor/` naming, but Codex-created branches must still use the required `codex/*` prefix

## Avoid

- No XML layouts or fragments
- No kapt
- No analytics, auth, or backend services
- No hardcoded user-facing strings in composables
- No hardcoded coroutine dispatchers
<!-- MANUAL ADDITIONS END -->
