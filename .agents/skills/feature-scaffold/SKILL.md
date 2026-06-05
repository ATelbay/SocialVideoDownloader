---
name: feature-scaffold
description: "Scaffold or extend SocialVideoDownloader features: module layout, screen/route/ViewModel/state/intent/use-case patterns, tests, and navigation registration. Use for new screens/features or substantial feature changes."
user-invocable: true
---

# Feature Scaffold

## Before editing

1. Read `AGENTS.md` and relevant specs under `specs/` if this is a planned feature.
2. Find a similar feature (`feature/download`, `feature/history`, `feature/library`, or `shared:feature-*`).
3. Decide Android-only vs shared/KMP scope.

## Typical Android feature shape

- `ui/` — route/screen, composables, UiState, intents/actions.
- `domain/` only when feature-local logic is truly not shared; otherwise use `core:domain`.
- `navigation/` — public navigation entry points needed by `app`.
- Tests next to module conventions (`src/test` or variant-specific tests).

## Typical shared feature shape

- `shared:feature-*` contains shared ViewModel/state/use-case adapters.
- Platform APIs are behind interfaces/expect-actual or injected abstractions.
- Android `feature/*` can delegate to shared ViewModels/UI where current architecture does.

## Checklist

- No feature-to-feature dependency.
- Strings/resources extracted.
- Dispatchers and platform services injected.
- Navigation args minimal and stable.
- Unit tests for ViewModel/use-case logic when behavior changes.
