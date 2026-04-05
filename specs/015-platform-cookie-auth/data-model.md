# Data Model: Platform Authentication for Restricted Content

**Feature**: 015-platform-cookie-auth | **Date**: 2026-04-05

## Entities

### SupportedPlatform (Enum)

Represents a social media platform that may require authentication for restricted content.

| Attribute | Type | Description |
|-----------|------|-------------|
| displayName | String | Human-readable name (e.g., "Instagram") |
| loginUrl | String | Platform's official login page URL |
| cookieDomains | List\<String\> | Domains to extract cookies from after WebView login (e.g., ".instagram.com") |
| hostMatches | List\<String\> | URL host patterns for cookie injection matching (e.g., "instagram.com") |
| successCookieName | String | Cookie name that indicates successful login (e.g., "sessionid") |

**Values**: INSTAGRAM, YOUTUBE, TWITTER, REDDIT, FACEBOOK

**Utility functions**:
- `detectPlatform(url: String): SupportedPlatform?` — matches video URL host to platform
- `detectPlatformFromError(errorMessage: String): SupportedPlatform?` — matches yt-dlp error prefix to platform

### SecureCookieStore (expect/actual)

Per-platform encrypted key-value store for Netscape-format cookie strings.

| Operation | Signature | Description |
|-----------|-----------|-------------|
| getCookies | `(platform: SupportedPlatform): String?` | Returns stored Netscape cookie string, or null if not connected |
| setCookies | `(platform: SupportedPlatform, cookies: String)` | Stores cookie string for platform |
| clearCookies | `(platform: SupportedPlatform)` | Removes stored cookies for platform |
| isConnected | `(platform: SupportedPlatform): Boolean` | Returns true if cookies exist for platform |
| connectedPlatforms | `(): List<SupportedPlatform>` | Returns all platforms with stored cookies |

**Storage keys**: `cookies_instagram`, `cookies_youtube`, `cookies_twitter`, `cookies_reddit`, `cookies_facebook`

**Platform implementations**:
- Android: EncryptedSharedPreferences (AES-256, Keystore-backed master key)
- iOS: Keychain (kSecClassGenericPassword, service: "com.socialvideodownloader.cookies")

### DownloadErrorType (Enum Extension)

Existing enum in `:shared:data`. New value added:

| Value | Description |
|-------|-------------|
| AUTH_REQUIRED | Extraction failed due to authentication requirement on a supported platform |

### Cookie Data (Transient)

Not persisted in a database. Cookie strings are stored as opaque blobs in SecureCookieStore.

**Netscape cookie format** (per line):
```
<domain>\t<include_subdomains>\t<path>\t<secure>\t<expiry>\t<name>\t<value>
```

Example:
```
.instagram.com	TRUE	/	TRUE	0	sessionid	abc123def456
.instagram.com	TRUE	/	TRUE	0	csrftoken	xyz789
```

## State Transitions

### Platform Connection Lifecycle

```
DISCONNECTED ──[user taps "Connect [Platform]"]──> LOGIN_IN_PROGRESS
LOGIN_IN_PROGRESS ──[login succeeds, cookies stored]──> CONNECTED
LOGIN_IN_PROGRESS ──[user cancels]──> DISCONNECTED
CONNECTED ──[user taps "Disconnect"]──> DISCONNECTED
CONNECTED ──[auth error with stored cookies (stale)]──> STALE
STALE ──[cookies cleared, user taps "Reconnect"]──> LOGIN_IN_PROGRESS
```

Note: These states are derived, not stored. `CONNECTED` = `SecureCookieStore.isConnected(platform)`. `STALE` is a transient detection during error handling (auth error + cookies present → clear cookies → show "Reconnect").

### Download Flow with Auth (Extension of Existing)

```
IDLE ──[paste URL, extract]──> EXTRACTING
EXTRACTING ──[auth error detected]──> ERROR(AUTH_REQUIRED)
ERROR(AUTH_REQUIRED) ──[user taps "Connect [Platform]"]──> LOGIN_IN_PROGRESS (WebView)
LOGIN_IN_PROGRESS ──[cookies stored]──> EXTRACTING (auto-retry)
EXTRACTING ──[success with cookies]──> FORMAT_SELECTION
```

## Relationships

```
SharedDownloadViewModel
  ├── uses SupportedPlatform.detectPlatform(url) for error classification
  ├── uses SupportedPlatform.detectPlatformFromError(error) for error classification
  ├── reads SecureCookieStore.connectedPlatforms() for UI state
  └── delegates to PlatformDelegate.showPlatformLogin(platform) for WebView

WebSocketExtractorApi
  ├── reads SecureCookieStore.getCookies(platform) per proxied request
  └── uses SupportedPlatform.hostMatches for injection targeting

ServerVideoExtractorApi
  ├── reads SecureCookieStore.getCookies(platform) for REST header
  └── uses SupportedPlatform.detectPlatform(url) to find matching cookies

PlatformLoginScreen (Android) / PlatformLoginScreen (iOS)
  ├── loads SupportedPlatform.loginUrl in WebView
  ├── monitors SupportedPlatform.successCookieName for login detection
  ├── extracts cookies from SupportedPlatform.cookieDomains
  └── writes to SecureCookieStore.setCookies(platform, cookies)
```
