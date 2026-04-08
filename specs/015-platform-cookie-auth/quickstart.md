# Developer Quickstart: Platform Authentication for Restricted Content

**Feature**: 015-platform-cookie-auth | **Date**: 2026-04-05

## What This Feature Does

Adds per-platform cookie authentication so users can download age-restricted/login-gated content from Instagram, YouTube, Twitter/X, Reddit, and Facebook. When extraction fails with an auth error, users log in via an in-app WebView, and the app captures session cookies for future requests.

## Key Files to Understand First

1. **`shared/network/.../WebSocketExtractorApi.kt`** — The WS proxy client. Cookie injection happens in the `http_request` handler loop (~line 72). Read the `extractViaProxy()` method to understand the proxy flow.

2. **`shared/feature-download/.../SharedDownloadViewModel.kt`** — The state machine. `mapErrorToType()` and `friendlyErrorMessage()` are where auth errors are classified. `onIntent()` handles all user actions.

3. **`shared/data/.../PlatformDownloadManager.kt`** — Contains `DownloadErrorType` enum (add `AUTH_REQUIRED` here).

4. **`server/app/routes/proxy_ws.py`** — Server-side WS proxy. The `_extract()` function sets up yt-dlp opts — this is where `cookiefile` gets added.

## Architecture Overview

```
User taps "Connect Instagram"
  → PlatformDelegate.showPlatformLogin(INSTAGRAM)
  → WebView loads instagram.com/accounts/login/
  → User logs in
  → App detects sessionid cookie
  → SecureCookieStore.setCookies(INSTAGRAM, netscapeCookies)
  → SharedDownloadViewModel auto-retries extraction

On next WS proxy request to instagram.com:
  → WebSocketExtractorApi reads SecureCookieStore
  → Merges cookies into proxied request Cookie header
  → Server's yt-dlp gets authenticated responses

On REST fallback:
  → ServerVideoExtractorApi attaches X-Platform-Cookies header
  → Server writes temp cookie file → passes to yt-dlp
```

## New Dependency

**Android only** (in `shared/network/build.gradle.kts` androidMain):
```kotlin
implementation("androidx.security:security-crypto:1.1.0-alpha06")
```

## How to Test Locally

1. **Auth error detection**: Use a known age-restricted Instagram URL. Extraction should fail with `AUTH_REQUIRED` error type and show "Connect Instagram" button.

2. **Cookie injection**: After connecting, retry the same URL. Check logcat/console for the Cookie header being appended to proxied requests.

3. **REST fallback**: Temporarily disable WS proxy (e.g., wrong server URL). Verify `X-Platform-Cookies` header appears in the REST request.

4. **Cookie isolation**: Connect Instagram, then try a YouTube URL. Instagram cookies should NOT appear in YouTube requests.

5. **Stale cookies**: Store invalid cookies manually via debugger, attempt auth-required URL. Should see "Reconnect [Platform]" instead of "Connect".

## Module Boundaries

- **`:shared:network`** owns: `SupportedPlatform`, `SecureCookieStore`, `NetscapeCookieParser`, cookie injection logic
- **`:shared:feature-download`** owns: Auth intents, error UI CTA, connection chips, PlatformDelegate extension
- **`:shared:data`** owns: `AUTH_REQUIRED` enum value
- **`:feature:download`** (Android) owns: WebView login screen, navigation route
- **`server/`** owns: Cookie file handling in yt-dlp opts
