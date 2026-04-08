# Implementation Plan: Platform Authentication for Restricted Content

**Branch**: `015-platform-cookie-auth` | **Date**: 2026-04-05 | **Spec**: [spec.md](spec.md)
**Input**: Feature specification from `/specs/015-platform-cookie-auth/spec.md`

## Summary

Add per-platform cookie authentication so users can download age-restricted and login-gated content from Instagram, YouTube, Twitter/X, Reddit, and Facebook. When extraction fails with an auth error, the user taps "Connect [Platform]", logs in via an embedded browser, and the app captures session cookies into encrypted local storage. Cookies are then injected into WS proxy HTTP requests (client-side) and forwarded to the REST fallback server (via header). The feature is fully opt-in — zero friction for users who never encounter restricted content.

## Technical Context

**Language/Version**: Kotlin 2.2.10 (Android + KMP shared), Swift 6.x (iOS)
**Primary Dependencies**: Jetpack Compose (BOM 2026.03.00), Hilt 2.59.2 (Android DI), Koin 4.x (KMP DI), Ktor 3.x (networking), Compose Multiplatform 1.9.x (shared UI)
**Storage**: EncryptedSharedPreferences (Android), iOS Keychain (iOS) — new per-platform cookie storage
**Testing**: JUnit5 + MockK + Turbine (existing), plus new unit tests for platform detection and cookie injection
**Target Platform**: Android 8.0+ (API 26), iOS 16.0+
**Project Type**: KMP mobile app (Android + iOS)
**Performance Goals**: Connect-and-retry flow < 60s excluding login time; cookie injection adds < 50ms per proxied request
**Constraints**: Cookies on-device only; no third-party transmission; local yt-dlp path unchanged; SwiftUI limited to Share Extension per constitution (see violation below)
**Scale/Scope**: 5 platforms, ~10 new files, ~14 modified client files, 3 server files modified

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principle | Status | Notes |
|-----------|--------|-------|
| I. Privacy & Zero Bloat | PASS | Cookies stay on-device. Transmitted only in HTTP requests the device already makes (WS proxy) or to user's own server (REST). No third parties, no analytics. |
| II. On-Device / Server-Mediated | PASS | Android on-device yt-dlp path unchanged (FR-019). Cookies only enhance WS proxy and REST fallback paths. iOS server-mediated path gains cookie forwarding. |
| III. Modern Stack | VIOLATION | PlatformLoginView on iOS requires WKWebView. Constitution restricts SwiftUI to Share Extension only. Resolution: use CMP `UIKitView` to wrap WKWebView in Compose Multiplatform instead of SwiftUI `UIViewRepresentable`. See Complexity Tracking. |
| IV. Modular Separation | PASS | `SupportedPlatform` + `SecureCookieStore` in `:shared:network` (commonMain + platform actuals). Auth intents in `:shared:feature-download`. `DownloadErrorType.AUTH_REQUIRED` in `:shared:data`. Android WebView screen in `:feature:download`. |
| V. Minimal Friction | PASS | Fully opt-in. Zero UI added to default flow. Auth CTA only appears on auth-specific errors. |
| VI. Test Discipline | PASS | Unit tests required for: `detectPlatform()`, `detectPlatformFromError()`, `mapErrorToType()` AUTH_REQUIRED branch, cookie injection logic, Netscape cookie parsing. |
| VII. Simplicity & Focus | PASS | Simple enum registry, expect/actual store, header-based forwarding. No abstractions beyond immediate need. |
| VIII. Optional Cloud | N/A | Feature does not involve cloud sync. |

## Complexity Tracking

| Violation | Why Needed | Simpler Alternative Rejected Because |
|-----------|------------|-------------------------------------|
| Principle III: iOS PlatformLoginView uses platform WebView | WKWebView is a UIKit component that must be wrapped for use in Compose screens. Originally spec'd as SwiftUI, but resolved by using CMP `UIKitView` wrapper instead. | SwiftUI UIViewRepresentable would violate constitution. CMP `UIKitView` achieves the same result within Compose Multiplatform, keeping SwiftUI confined to Share Extension. |

## Project Structure

### Documentation (this feature)

```text
specs/015-platform-cookie-auth/
├── plan.md              # This file
├── spec.md              # Feature specification
├── research.md          # Phase 0: research decisions
├── data-model.md        # Phase 1: entity model
├── quickstart.md        # Phase 1: developer quickstart
├── contracts/           # Phase 1: interface contracts
│   ├── ws-protocol.md   # WebSocket protocol extension
│   └── rest-api.md      # REST API extension
└── checklists/
    └── requirements.md  # Spec quality checklist
```

### Source Code (new and modified files)

```text
# New files
shared/network/src/commonMain/kotlin/com/socialvideodownloader/shared/network/
├── auth/SupportedPlatform.kt          # Platform enum + detection utilities
├── auth/SecureCookieStore.kt          # expect class
└── auth/NetscapeCookieParser.kt       # Netscape cookie string parsing utilities

shared/network/src/androidMain/kotlin/com/socialvideodownloader/shared/network/
└── auth/SecureCookieStore.android.kt  # actual: EncryptedSharedPreferences

shared/network/src/iosMain/kotlin/com/socialvideodownloader/shared/network/
└── auth/SecureCookieStore.ios.kt      # actual: iOS Keychain

shared/feature-download/src/commonMain/kotlin/com/socialvideodownloader/shared/feature/download/
└── ui/PlatformConnectionChips.kt      # Connected platform chips + disconnect bottom sheet

shared/feature-download/src/commonMain/kotlin/com/socialvideodownloader/shared/feature/download/
└── platform/PlatformLoginScreen.kt      # expect composable for login screen

shared/feature-download/src/androidMain/kotlin/com/socialvideodownloader/shared/feature/download/
└── platform/PlatformLoginScreen.kt      # actual no-op (Android uses navigation)

shared/feature-download/src/iosMain/kotlin/com/socialvideodownloader/shared/feature/download/
└── platform/PlatformLoginScreen.ios.kt  # CMP UIKitView wrapping WKWebView

feature/download/src/main/kotlin/com/socialvideodownloader/feature/download/
└── ui/PlatformLoginScreen.kt          # Android WebView login (Compose + AndroidView)

# Modified files
shared/network/src/commonMain/.../WebSocketExtractorApi.kt        # +SecureCookieStore dep, cookie injection
shared/network/src/commonMain/.../ServerVideoExtractorApi.kt      # +SecureCookieStore dep, X-Platform-Cookies header
shared/network/src/commonMain/.../di/NetworkModule.kt             # +SecureCookieStore to Koin graph
shared/feature-download/src/commonMain/.../DownloadIntent.kt      # +ConnectPlatformClicked, PlatformLoginResult
shared/feature-download/src/commonMain/.../DownloadUiState.kt     # +connectedPlatforms to Idle state
shared/feature-download/src/commonMain/.../SharedDownloadViewModel.kt  # AUTH_REQUIRED handling, auto-retry
shared/feature-download/src/commonMain/.../ui/DownloadErrorContent.kt  # "Connect/Reconnect [Platform]" CTA
shared/data/src/commonMain/.../platform/PlatformDownloadManager.kt     # +AUTH_REQUIRED enum value
shared/feature-download/src/commonMain/.../DownloadEvent.kt                   # +ShowPlatformLogin event
shared/feature-download/src/commonMain/.../ui/DownloadScreen.kt               # +fullscreen login overlay for iOS
core/data/src/main/.../di/NetworkModule.kt                        # +SecureCookieStore to Hilt graph
feature/download/src/main/.../ui/DownloadViewModel.kt             # PlatformDelegate.showPlatformLogin()
feature/download/src/main/.../navigation/DownloadNavigation.kt    # +PlatformLoginRoute
shared/network/build.gradle.kts                                   # +security-crypto (androidMain)
server/app/ytdlp_opts.py                                          # +cookiefile param
server/app/routes/extract.py                                      # +X-Platform-Cookies handling
server/app/routes/proxy_ws.py                                     # +cookies in extract_request message
```

## Design Decisions

### D1: EncryptedSharedPreferences for Android cookie storage

The project previously rejected EncryptedSharedPreferences for cloud backup encryption (spec 009) because it only handles key-value pairs, not arbitrary blobs. However, per-platform cookie storage IS a key-value use case (5 keys, each holding a cookie string). EncryptedSharedPreferences is the right tool here — simpler than raw Keystore API, and the rejection rationale from spec 009 does not apply.

### D2: CMP UIKitView instead of SwiftUI for iOS WebView

The user's original spec called for a SwiftUI `PlatformLoginView` with `UIViewRepresentable`. However, the constitution restricts SwiftUI to the Share Extension. CMP provides `UIKitView` (analogous to Android's `AndroidView`) which can wrap WKWebView directly in a `@Composable` function. This avoids the constitution violation while delivering identical functionality.

### D3: Cookie injection at request-header level, not HttpClient plugin

Injecting cookies by modifying `reqHeaders` in the existing `http_request` handler loop is simpler than installing an `HttpSend` interceptor on `rawClient`. The injection point is ~5 lines of code in one location. No need for a plugin architecture.

### D4: Netscape cookie format for storage and transport

yt-dlp natively uses Netscape cookie format for `--cookies`. Storing cookies in this format avoids any conversion step. The same string goes from WebView extraction → storage → WS proxy injection (parsed to key=value pairs) or REST header (base64-encoded).

### D5: expect/actual class in :shared:network auth subpackage

Placing `SecureCookieStore` in `:shared:network` (not `:shared:data`) because its primary consumers are `WebSocketExtractorApi` and `ServerVideoExtractorApi`, both in `:shared:network`. This avoids a circular dependency and keeps auth-related code co-located.
