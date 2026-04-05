# Research: Platform Authentication for Restricted Content

**Feature**: 015-platform-cookie-auth | **Date**: 2026-04-05

## R1: Android Secure Cookie Storage — EncryptedSharedPreferences vs Raw Keystore

**Decision**: Use `EncryptedSharedPreferences` from `androidx.security:security-crypto`.

**Rationale**: Per-platform cookie storage is a classic key-value use case (5 keys, string values). EncryptedSharedPreferences provides AES-256 encryption with Android Keystore-backed master key, handling all crypto complexity. The project's spec 009 rejected it for cloud backup because that needed arbitrary blob encryption — a different use case.

**Alternatives considered**:
- Raw Android Keystore + AES-256-GCM (as used in cloud backup): Works but requires manual IV management, cipher setup, and Base64 encoding. Over-engineered for key-value strings.
- DataStore Preferences with manual encryption: More code, no security advantage over EncryptedSharedPreferences.
- Plain SharedPreferences: No encryption — cookies would be readable on rooted devices.

**Dependency**: `androidx.security:security-crypto:1.1.0-alpha06` in `shared/network/build.gradle.kts` androidMain dependencies.

## R2: iOS Secure Cookie Storage — Keychain API

**Decision**: Use iOS Security framework directly with `kSecClassGenericPassword`.

**Rationale**: Constitution prohibits third-party Keychain wrappers. The Security framework API is straightforward for string storage. Use `kSecAttrService = "com.socialvideodownloader.cookies"` and `kSecAttrAccount = "cookies_{platform}"` as the key.

**Alternatives considered**:
- KeychainAccess (third-party): Simpler API but constitution forbids third-party wrappers.
- UserDefaults: No encryption.
- Multiplatform Settings: Only wraps UserDefaults on iOS — no encryption.

**Dependency**: None (Security framework is part of iOS SDK).

## R3: iOS WebView Approach — CMP UIKitView vs SwiftUI UIViewRepresentable

**Decision**: Use Compose Multiplatform `UIKitView` to wrap WKWebView.

**Rationale**: Constitution Principle III restricts SwiftUI to the Share Extension. CMP's `UIKitView` is the direct equivalent of Android's `AndroidView` — it embeds a UIKit view in a Compose hierarchy. WKWebView is a UIKit class, so `UIKitView` wraps it naturally without needing SwiftUI.

**Alternatives considered**:
- SwiftUI `UIViewRepresentable` in a separate SwiftUI view: Violates constitution Principle III.
- CMP WebView library (e.g., multiplatform-webview): Adds a dependency for something achievable with platform primitives.

## R4: Cookie Injection Strategy — Header Merge vs HttpClient Plugin

**Decision**: Direct header merge in the `http_request` handler loop.

**Rationale**: `WebSocketExtractorApi.extractViaProxy()` already iterates over `reqHeaders` from the server's message and applies them via `headers { reqHeaders.forEach { (k, v) -> append(k, v) } }`. Adding cookie injection is 5-10 lines: parse the request URL host, find the matching platform, parse Netscape cookies, and append/merge into the `Cookie` header. No architectural changes needed.

**Alternatives considered**:
- Install an `HttpSend` interceptor on `rawClient`: Requires changing `rawClient` from a bare `HttpClient {}` to one with a custom plugin. More code, less transparent, harder to debug.
- Create a new `CookieInjectingHttpClient` wrapper: Over-engineered for a single injection point.

## R5: Netscape Cookie Format Parsing

**Decision**: Implement a minimal `NetscapeCookieParser` utility in `:shared:network` commonMain.

**Rationale**: Netscape cookie format is simple (tab-separated, one cookie per line, `#` comments). Parsing to extract `name=value` pairs for HTTP `Cookie` header injection requires ~20 lines. Formatting WebView cookies into Netscape format requires ~15 lines. No library needed.

**Format reference** (per yt-dlp/curl convention):
```
# Netscape HTTP Cookie File
.instagram.com	TRUE	/	TRUE	0	sessionid	abc123def456
```
Fields: domain, include-subdomains, path, secure, expiry, name, value (tab-separated).

## R6: WS Proxy Cookie Pre-seeding

**Decision**: Send cookies in the initial `extract_request` WebSocket message as an optional `cookies` field (base64-encoded Netscape string).

**Rationale**: The server's `proxy_ws.py` reads the initial message to get the URL. Adding an optional `cookies` field lets the server write a temp cookie file and pass it to yt-dlp opts, pre-seeding yt-dlp's internal cookie jar before any HTTP requests are proxied. This handles cases where yt-dlp needs cookies for the initial extraction request (not just subsequent ones).

**Server change**: In `_extract(url, ctx)`, check for `cookies` in the initial message, base64-decode, write to tempfile, pass as `cookiefile` in `get_ydl_opts()`.

## R7: Error Pattern Detection for Auth Errors

**Decision**: Combine URL-based platform detection with error message keyword matching.

**Rationale**: yt-dlp error messages vary by platform but follow patterns:
- Instagram: `[Instagram]` prefix + "login" / "inappropriate" / "private"
- YouTube: `[youtube]` prefix + "Sign in to confirm your age" / "age-restricted"
- Twitter/X: `[twitter]` prefix + "NSFW" / "sensitive" / "login"
- Reddit: `[reddit]` prefix + "NSFW" / "login"  
- Facebook: `[facebook]` prefix + "login" / "private"

Generic auth keywords: "sign in", "log in", "login required", "must be logged in", "inappropriate", "age-restricted", "NSFW"

Strategy: First detect platform from URL (`detectPlatform(url)`). Then check if error contains auth keywords. If both match → `AUTH_REQUIRED`. If only keywords match but URL doesn't map to a supported platform → fall through to existing error types.

## R8: PlatformDelegate Extension for Login Flow

**Decision**: Add `showPlatformLogin(platform: SupportedPlatform)` to the existing `PlatformDelegate` interface.

**Rationale**: `SharedDownloadViewModel` already has a `PlatformDelegate` interface (currently with `checkNotificationPermission()`). Platform-specific login presentation (Android: Compose Navigation to WebView screen; iOS: CMP UIKitView in a dialog/sheet) is inherently a platform concern. The delegate pattern is already established.

**Android implementation**: `DownloadViewModel` implements the delegate, navigates to `PlatformLoginRoute(platformName)` via the NavController callback. The result comes back via `SharedDownloadViewModel.onIntent(PlatformLoginResult(...))`.

**iOS implementation**: The iOS CMP screen shows a dialog/sheet containing the `UIKitView`-wrapped WKWebView. On login success, calls `viewModel.onIntent(PlatformLoginResult(...))`.
