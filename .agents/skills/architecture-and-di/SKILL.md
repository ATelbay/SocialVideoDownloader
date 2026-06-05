---
name: architecture-and-di
description: "SocialVideoDownloader architecture guidance: Android/KMP module boundaries, DI with Hilt/Koin, repository contracts, navigation, storage abstractions, and dependency placement. Use for cross-module changes, new modules, DI, or architecture decisions."
user-invocable: true
---

# Architecture and DI

## Module boundaries

- `app/`: Android application entry, Activity, Android navigation host, Hilt setup, platform bridges.
- `feature/*`: Android-only feature UI/adapters. Do not depend on sibling features.
- `core:domain`: domain models, repository interfaces, use cases; keep platform-light/KMP-compatible where possible.
- `core:data`: Android implementations: Room, MediaStore, yt-dlp wrapper, Firebase/Play Billing adapters already present.
- `core:ui`: Android shared Compose components/theme.
- `shared/*`: KMP shared data/UI/features/DI/network for Android+iOS.
- `iosApp/`: SwiftUI shell, iOS-specific bridge/share extension.
- `server/`: optional FastAPI extraction proxy; core app flow must not depend on it unless explicitly required.

## DI rules

- Android uses Hilt with KSP; never kapt.
- Shared/iOS uses Koin or existing KMP DI patterns.
- Bind interfaces at module boundaries; avoid leaking platform implementation types into domain/shared contracts.
- Inject dispatchers, clocks, storage abstractions, and clients for testability.

## Navigation

- Pass navigation callbacks as lambdas to composables; avoid passing `NavController` deep into UI.
- Navigation args should be stable primitive IDs/strings; reload data in the destination layer.
- Cross-feature navigation is coordinated by `app`/navigation host, not feature-to-feature dependencies.

## Dependencies

- Add libraries through version catalogs and convention plugins.
- Prefer `implementation` over `api`; expose only deliberate contracts.
- Before adding a dependency, search for an existing project abstraction.
