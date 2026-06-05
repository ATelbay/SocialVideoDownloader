---
name: kmp-ios
description: "Kotlin Multiplatform/iOS guidance for SocialVideoDownloader: shared modules, expect/actual/platform abstractions, Compose Multiplatform, SwiftUI shell, SKIE/Koin, and iOS build caveats. Use for shared/iOS work."
user-invocable: true
---

# KMP / iOS

## Scope decision

- Put behavior shared by Android and iOS in `shared/*` or `core:domain` when compatible.
- Keep Android-only APIs (MediaStore, Hilt, youtubedl-android, Play Billing) behind platform adapters.
- Keep iOS shell specifics in `iosApp/`.

## Shared code rules

- Prefer common models/use cases with injected platform abstractions.
- Use expect/actual only when an interface injection is not enough.
- Avoid leaking Android classes into commonMain.
- Compose Multiplatform UI belongs in `shared:ui`/shared feature modules when reused.

## Build caveats

- Some checks may require excluding iOS native compile tasks depending on current Koin/Kotlin ABI state:

```bash
./gradlew ktlintCheck -x compileKotlinIosArm64 -x compileKotlinIosSimulatorArm64
```

- For iOS builds, use the existing Xcode project/scheme and simulator destination from project docs or current CI.
