# Quickstart: Project Foundation

**Branch**: `001-project-foundation` | **Date**: 2026-03-14

## Prerequisites

- Android Studio (latest stable)
- Android SDK 36 installed
- JDK 17+
- Device or emulator running API 26+

## Build & Run

```bash
# Clone and checkout branch
git checkout 001-project-foundation

# Build debug APK
./gradlew assembleDebug

# Run unit tests
./gradlew test

# Lint check
./gradlew ktlintCheck
```

## Project Structure After Implementation

```
VideoGrab/
├── app/                          # Application module (composition root)
├── feature/
│   ├── download/                 # Download screen module
│   └── history/                  # History screen module
├── core/
│   ├── domain/                   # Use cases, interfaces, domain models
│   ├── data/                     # Repository impls, Room DB, yt-dlp wrapper
│   └── ui/                       # Shared theme, composables
├── build-logic/
│   └── convention/               # Convention plugins
├── gradle/
│   └── libs.versions.toml        # Version catalog
├── settings.gradle.kts
└── build.gradle.kts
```

## Module Dependency Graph

```
:app → :feature:download → :core:domain
    → :feature:history   → :core:ui
    → :core:data
    → :core:ui

:core:data → :core:domain
```

## Key Files to Verify

- `gradle/libs.versions.toml` — all dependency versions centralized
- `build-logic/convention/` — convention plugins for shared build config
- `app/src/main/.../VideoGrabApp.kt` — Hilt application, yt-dlp init
- `app/src/main/.../navigation/AppNavHost.kt` — navigation graph
- `core/ui/src/main/.../theme/Theme.kt` — Material 3 + Dynamic Color
- `core/data/src/main/.../local/AppDatabase.kt` — Room database skeleton
- `core/data/src/main/.../local/DownloadEntity.kt` — download history entity
- `core/data/src/main/.../local/DownloadDao.kt` — DAO interface
