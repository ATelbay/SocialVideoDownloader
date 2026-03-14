# CLAUDE.md — Social Video Downloader

## Project overview
Android video downloader. URL in → video file out. No backend, no ads, no auth.
Built on yt-dlp via youtubedl-android. Personal tool.

## Architecture
- **Pattern:** MVI (Model-View-Intent)
- **UI:** Jetpack Compose + Material 3 + Dynamic Color
- **Navigation:** Single Activity, Compose Navigation (composable destinations, no fragments)
- **DI:** Hilt (with KSP, NOT kapt)
- **Async:** Coroutines + StateFlow/SharedFlow
- **DB:** Room (KSP) for download history
- **Extraction:** youtubedl-android (yt-dlp wrapper) + FFmpeg + aria2c
- **Images:** Coil for thumbnails
- **Min SDK:** 26 | **Target SDK:** 36 | **Kotlin:** 2.0+

## Module structure
```
:app                    — Activity, navigation, DI setup
:feature:download       — Main screen (URL input, format selection, progress)
:feature:history        — Download history screen
:core:domain            — Use cases, domain models, repository interfaces
:core:data              — Repository implementations, Room DB, yt-dlp wrapper
:core:ui                — Shared composables, theme, design tokens
```

## Build commands
```bash
./gradlew assembleDebug          # Debug build
./gradlew assembleRelease        # Release build
./gradlew test                   # Unit tests
./gradlew connectedAndroidTest   # Instrumentation tests
./gradlew ktlintCheck            # Lint
```

## Coding standards
- Kotlin, never Java for new code
- KSP only, never kapt (build performance)
- Compose-only UI, never XML layouts
- `sealed interface` for state and intent types
- Repository pattern: interface in :core:domain, impl in :core:data
- Use cases are single-purpose classes with `operator fun invoke()`
- Coroutine dispatchers injected, never hardcoded (testability)
- All strings extractable (no hardcoded user-facing text in composables)

## Naming conventions
- Packages: `com.socialvideodownloader.{module}.{layer}`
- Composables: PascalCase, suffix with `Screen`, `Content`, `Item`
- ViewModels: `{Feature}ViewModel`
- Use cases: verb phrase — `ExtractVideoInfoUseCase`, `DownloadVideoUseCase`
- Room: `{Name}Entity`, `{Name}Dao`, `AppDatabase`
- State: `{Feature}UiState`, `{Feature}Intent`

## SDD Tooling: GitHub Spec Kit
- Spec Kit manages feature specs via `/speckit.*` slash commands
- Constitution: `.specify/memory/constitution.md` (high-level principles)
- Feature specs generated to: `specs/{NNN-feature-name}/` (spec.md, plan.md, tasks.md)
- Product spec (reference): `docs/PRODUCT_SPEC.md`
- Workflow per feature: `/speckit.specify` → `/speckit.clarify` → `/speckit.plan` → `/speckit.tasks` → `/speckit.implement`
- Use Spec Kit for NEW FEATURES only. Bug fixes and small tweaks — direct implementation.
- Always reference `docs/PRODUCT_SPEC.md` when running `/speckit.specify`

## Key dependencies
```kotlin
// youtubedl-android (yt-dlp + ffmpeg + aria2c)
io.github.junkfood02.youtubedl-android:library:0.18.+
io.github.junkfood02.youtubedl-android:ffmpeg:0.18.+
io.github.junkfood02.youtubedl-android:aria2c:0.18.+
```

## Important: yt-dlp specifics
- Initialize YoutubeDL.getInstance().init(context) in Application.onCreate()
- yt-dlp runs in a separate process — heavy operations on Dispatchers.IO
- getInfo() returns VideoInfo with formats list — use for format selection
- Update yt-dlp binary periodically: YoutubeDL.getInstance().updateYoutubeDL()
- Downloads go to MediaStore (Downloads/SocialVideoDownloader/) via Scoped Storage

## Testing approach
- Unit tests for use cases and ViewModels (JUnit5 + Mockk + Turbine for Flow)
- No UI tests in MVP (Compose testing later)

## Git workflow
- IMPORTANT: Always push to a branch and create a PR. Never push directly to main.
- Branch naming: `feature/{name}`, `fix/{name}`, `refactor/{name}`
- Conventional commits: `feat:`, `fix:`, `refactor:`, `test:`, `docs:`

## What NOT to do
- Do NOT add XML layouts or fragments
- Do NOT use kapt
- Do NOT add network interceptors or analytics
- Do NOT create a backend service
- Do NOT add authentication of any kind
- Do NOT over-engineer: this is a focused utility, not a framework