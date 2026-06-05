---
name: testing-and-preflight
description: "Testing and pre-PR checklist for SocialVideoDownloader: unit tests, ViewModel Flow tests, ktlint, targeted Gradle checks, Android/KMP/iOS command selection, and review reminders. Use before finalizing changes."
user-invocable: true
---

# Testing and Preflight

## Test priorities

- Use cases and pure domain logic: fast unit tests.
- ViewModels/state machines: JUnit5 + MockK + Turbine where existing modules use them.
- Repository/platform adapters: test with fakes; avoid real network/storage where possible.
- UI tests are optional unless requested.

## Targeted checks

Choose the smallest meaningful set:

```bash
./gradlew :feature:download:testDebugUnitTest
./gradlew :feature:history:testDebugUnitTest
./gradlew :core:domain:test
./gradlew ktlintCheck
./gradlew assembleDebug
```

For broad final validation:

```bash
./gradlew assembleDebug test ktlintCheck
```

Use KMP/iOS exclusions only when needed by current dependency/tooling limitations.

## Review checklist

- No secrets or local machine paths.
- No kapt/XML/fragments.
- Dispatchers/platform services injected.
- Strings/resources extracted.
- Module boundaries respected.
- User-visible download/storage behavior preserved.
