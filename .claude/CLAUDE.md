# CLAUDE.md — Social Video Downloader

## Project overview
Android video downloader. URL in → video file out. No backend, no ads, no auth.
Built on yt-dlp via youtubedl-android. Personal tool.

## Architecture
- **Pattern:** MVI (Model-View-Intent)
- **UI:** Jetpack Compose on Android + Compose Multiplatform shared UI/iOS shell + Material 3 + Dynamic Color
- **Navigation:** Single Activity on Android, Compose Navigation / JetBrains Navigation Compose in the shared shell (composable destinations, no fragments)
- **DI:** Hilt on Android + Koin in shared/iOS shell (with KSP, NOT kapt)
- **Async:** Coroutines + StateFlow/SharedFlow
- **DB:** Room (KSP) for download history
- **Extraction:** youtubedl-android (yt-dlp wrapper) + FFmpeg + aria2c
- **Images:** Coil for thumbnails
- **Min SDK:** 26 | **Target SDK:** 36 | **Kotlin:** 2.2.10

## Module structure
```
Android-only modules:
:app                       — Activity, navigation, Hilt DI setup, KMP bridge
:feature:download          — Download screen (Compose, delegates to :shared:feature-download VM)
:feature:history           — History screen (Compose, delegates to :shared:feature-history VM)
:feature:library           — Library/file browser screen (Compose)
:core:domain               — KMP: use cases, domain models, repository interfaces (commonMain + androidMain/iosMain)
:core:data                 — Android: Room DB impl, yt-dlp wrapper, MediaStore, server client
:core:ui                   — Android: shared Compose components, theme, design tokens
:core:cloud                — Android: Firebase Auth + Firestore cloud backup
:core:billing              — Android: Play Billing (Pro tier)

KMP Shared modules (Android + iOS):
:shared:ui              — Compose Multiplatform design system, theme, shared UI components
:shared:di              — Compose Multiplatform app shell, Koin init, iOS bridge
:shared:network            — Ktor HTTP client, yt-dlp API models, server communication
:shared:data               — Room KMP DB, DAOs, platform abstractions (FileStorage, DownloadManager, Clipboard)
:shared:feature-download   — SharedDownloadViewModel: state machine, format selection, retry logic
:shared:feature-history    — SharedHistoryViewModel: history list, cloud backup controls
:shared:feature-library    — SharedLibraryViewModel: offline library, file access

iOS-only:
iosApp/                    — SwiftUI shell hosting the Compose Multiplatform root, plus Share Extension screens
```

### KMP Convention Plugins (build-logic)
- `svd.kmp.library` — multiplatform library with Android + iOS targets, Room KMP, Ktor
- `svd.kmp.feature` — KMP feature module with shared ViewModel dependencies
- `svd.kmp.compose` — KMP Compose module with shared UI/runtime/material/resources
- `svd.android.library` — Android-only library (Compose off by default)
- `svd.android.feature` — Android feature module (Compose + Hilt)

## Build commands
```bash
./gradlew assembleDebug          # Debug build (~138MB APK, ~15s clean build)
./gradlew assembleRelease        # Release build
./gradlew test                   # Unit tests
./gradlew connectedAndroidTest   # Instrumentation tests
# ktlint: exclude iOS native compilation due to Koin 4.1.0 / Kotlin 2.2.x ABI mismatch
./gradlew ktlintCheck -x compileKotlinIosArm64 -x compileKotlinIosSimulatorArm64
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
- Baseline spec artifacts live in: `specs/001-project-foundation/`
- Feature specs generated to: `specs/{NNN-feature-name}/` (spec.md, plan.md, tasks.md)
- Workflow per feature: `/speckit.specify` → `/speckit.clarify` → `/speckit.plan` → `/speckit.tasks` → `/speckit.implement`
- Use Spec Kit for NEW FEATURES only. Bug fixes and small tweaks — direct implementation.
- There is no `docs/PRODUCT_SPEC.md` in this repo right now

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
- Repository history uses `feature/{name}`, `fix/{name}`, `refactor/{name}`; Codex-created branches use `codex/{name}`
- Conventional commits: `feat:`, `fix:`, `refactor:`, `test:`, `docs:`

## What NOT to do
- Do NOT add XML layouts or fragments
- Do NOT use kapt
- Do NOT add network interceptors or analytics
- Do NOT create a backend service
- Do NOT add authentication except Google Sign-In for opt-in cloud features
- Do NOT over-engineer: this is a focused utility, not a framework

## Active Technologies
- Kotlin 2.2.10 + Jetpack Compose (BOM 2026.03.00), Material 3, Hilt (KSP), Navigation Compose 2.9.7, Coil (005-ui-redesign-dark-theme)
- No changes (Room + MediaStore unchanged) (005-ui-redesign-dark-theme)
- N/A (no storage changes) (006-warm-editorial-redesign)
- Room (download history), MediaStore (saved files), cacheDir (yt-dlp temp files) (007-download-flow-hardening)
- Room (download history), MediaStore (saved files), cacheDir (yt-dlp temp files + new share temp) (008-format-ui-share)
- Kotlin 2.2.10 + Jetpack Compose (BOM 2026.03.00), Hilt 2.59.2, Room 2.8.4, Navigation Compose 2.9.7, Coil 2.7.0, Firebase BOM 33.15.0 (Auth + Firestore), Play Billing 7.1.1 (009-cloud-history-backup)
- Room (download history + sync queue), Firestore (encrypted cloud records), DataStore Preferences (backup settings) (009-cloud-history-backup)
- Kotlin 2.2.10 + Jetpack Compose (BOM 2026.03.00), Hilt (KSP), Room (KSP), Navigation Compose 2.9.7, Coil 2.7.0, youtubedl-android 0.18.x (library + ffmpeg + aria2c), Firebase BOM 33.15.0 (Auth + Firestore), Play Billing 7.1.1 (010-apk-size-optimization)
- Kotlin 2.2.10, Swift 6.x (iOS) + Room KMP 2.8.4, Ktor 3.4.1, Koin 4.2.0, SKIE 0.10.10, Multiplatform Settings 1.3.0. Android retains: Jetpack Compose (BOM 2026.03.00), Hilt 2.59.2, Navigation Compose 2.9.7, Coil 2.7.0, Firebase BOM 33.15.0, Play Billing 7.1.1. (011-kmp-ios-migration)
- Room KMP (shared DB schema in commonMain, platform builders), MediaStore (Android), Documents directory (iOS), Multiplatform Settings (preferences) (011-kmp-ios-migration)
- Kotlin 2.2.10 (Android + KMP shared), Swift 6.x (iOS) + GitHub Actions, Gradle KTS, Xcode 16.x, KMP framework binaries, SKIE 0.10.10 (012-cicd-github-actions)
- N/A (CI/CD infrastructure — no data model changes) (012-cicd-github-actions)
- Kotlin 2.2.10 (shared + Android), Swift 6.x (iOS shell + Share Extension) + Compose Multiplatform 1.9.3, JetBrains Navigation Compose 2.9.1, Coil 3.1.0 (KMP), Koin 4.1.0, Ktor 3.3.0 (013-compose-multiplatform-migration)
- No changes — Room KMP (shared DB), MediaStore (Android), Documents (iOS) (013-compose-multiplatform-migration)
- Python 3.9+ (server), Kotlin 2.2.10 (KMP/shared), Swift 6.x (iOS shell) + FastAPI + uvicorn (server), yt-dlp >= 2025.1.1, Ktor 3.3.0 + ktor-client-websockets (KMP), Koin 4.1.0 (shared DI) (014-ws-proxy-extraction)
- N/A — no persistent storage changes. All data is transient within WebSocket session lifetime. (014-ws-proxy-extraction)

## Recent Changes
- 005-ui-redesign-dark-theme: Added Kotlin 2.2.10 + Jetpack Compose (BOM 2026.03.00), Material 3, Hilt (KSP), Navigation Compose 2.9.7, Coil
