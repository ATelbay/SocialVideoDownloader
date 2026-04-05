# Feature Specification: Platform Authentication for Restricted Content

**Feature Branch**: `015-platform-cookie-auth`  
**Created**: 2026-04-05  
**Status**: Draft  
**Input**: User description: "Platform Authentication for Restricted Content (Instagram, YouTube, Twitter/X, Reddit, Facebook)"

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Authenticate to Download Restricted Content (Priority: P1)

A user pastes a URL to age-restricted or login-gated content (e.g., an Instagram reel marked "This content may be inappropriate"). The download attempt fails with a clear message explaining that authentication is required. The error screen shows a "Connect [Platform]" button. The user taps it, a WebView opens the platform's login page, the user logs in (with autofill assistance for saved credentials), and the app automatically extracts session cookies, stores them securely, clears the WebView, and retries the download. The content downloads successfully.

**Why this priority**: This is the core value proposition. Without this flow, users cannot download any restricted content at all.

**Independent Test**: Can be fully tested by attempting to download a known age-restricted Instagram reel without cookies, verifying the auth error CTA appears, completing login, and confirming the download succeeds on auto-retry.

**Acceptance Scenarios**:

1. **Given** a user pastes a URL to age-restricted Instagram content, **When** extraction fails due to authentication requirements, **Then** the error screen displays a friendly message ("This content requires authentication. Connect your Instagram account to download.") and a "Connect Instagram" button.
2. **Given** the user taps "Connect Instagram", **When** the WebView opens the Instagram login page, **Then** the login page loads correctly and autofill is available for saved credentials.
3. **Given** the user successfully logs into Instagram via the WebView, **When** the app detects the session cookie, **Then** the app extracts all domain cookies, stores them in encrypted local storage, clears the WebView cookies, and automatically retries the extraction with cookies injected.
4. **Given** cookies are stored for Instagram, **When** extraction is retried via the WS proxy path, **Then** cookies are injected into proxied HTTP requests matching `instagram.com` hosts, and the download completes.

---

### User Story 2 - Manage Connected Platform Accounts (Priority: P2)

A user who has previously connected one or more platform accounts sees small status chips on the download screen (e.g., "Instagram", "YouTube") indicating which platforms are authenticated. The user can tap a chip to see details and disconnect the account (clearing stored cookies) if desired.

**Why this priority**: Users need visibility into which platforms are connected and the ability to revoke access. This is essential for trust and privacy.

**Independent Test**: Can be tested by connecting two platforms, verifying chips appear, tapping a chip to open the disconnect sheet, disconnecting one, and confirming the chip disappears and cookies are cleared.

**Acceptance Scenarios**:

1. **Given** a user has stored cookies for Instagram and YouTube, **When** the download screen is in its idle state, **Then** small chips labeled "Instagram" and "YouTube" are visible.
2. **Given** no platforms are connected, **When** the download screen loads, **Then** no connection chips are shown.
3. **Given** the user taps a connected platform chip, **When** the bottom sheet appears, **Then** it shows the platform name and a "Disconnect" button.
4. **Given** the user taps "Disconnect" on the bottom sheet, **When** the action completes, **Then** the platform's cookies are cleared from encrypted storage, the chip disappears, and future extractions for that platform proceed without cookies.

---

### User Story 3 - Handle Expired or Stale Cookies (Priority: P2)

A user who previously connected a platform attempts to download restricted content, but the session cookies have expired. The extraction fails with an auth error despite cookies being stored. The app detects the stale cookies, clears them, and shows a "Reconnect [Platform]" button instead of "Connect [Platform]".

**Why this priority**: Cookie expiry is inevitable. Without graceful handling, users would see confusing errors despite having previously authenticated.

**Independent Test**: Can be tested by storing intentionally expired/invalid cookies for a platform, attempting a restricted download, and verifying the app shows "Reconnect [Platform]" after clearing stale cookies.

**Acceptance Scenarios**:

1. **Given** a user has stored cookies for YouTube that have expired, **When** extraction fails with an auth error, **Then** the app detects that cookies exist but are stale, clears them, and shows "Reconnect YouTube" instead of "Connect YouTube".
2. **Given** the user taps "Reconnect YouTube", **When** they complete the login flow, **Then** new cookies are stored and the extraction auto-retries successfully.

---

### User Story 4 - Cancel Login Without Side Effects (Priority: P3)

A user sees the "Connect [Platform]" button, taps it, but decides not to log in and dismisses the WebView. No cookies are stored or cleared, and the app returns to the error state unchanged.

**Why this priority**: Users must be able to back out of the login flow without unintended consequences.

**Independent Test**: Can be tested by opening the login WebView, pressing back/cancel without logging in, and verifying no cookies were stored.

**Acceptance Scenarios**:

1. **Given** the user taps "Connect Instagram" and the WebView opens, **When** the user presses back or cancels without logging in, **Then** no cookies are stored, no existing cookies are modified, and the user returns to the previous error screen.

---

### User Story 5 - REST Fallback with Cookie Forwarding (Priority: P3)

When the WS proxy extraction path is unavailable and the app falls back to the REST extraction endpoint, stored platform cookies are forwarded to the server so that server-side yt-dlp can use them for extraction.

**Why this priority**: Ensures authentication works across all extraction paths, not just the WS proxy.

**Independent Test**: Can be tested by disabling the WS proxy, triggering a REST fallback extraction for authenticated content, and verifying the server receives and uses the forwarded cookies.

**Acceptance Scenarios**:

1. **Given** a user has stored cookies for a platform and the WS proxy is unavailable, **When** extraction falls back to the REST endpoint, **Then** the app sends cookies to the server and the server uses them for yt-dlp extraction.
2. **Given** the WS proxy initial extraction request includes cookies, **When** yt-dlp starts on the server, **Then** the cookies pre-seed yt-dlp's internal cookie jar before proxying begins.

---

### Edge Cases

- What happens when the user's device has no internet during the WebView login flow? The WebView shows its standard offline error; the user can retry or cancel.
- What happens if a platform changes its login URL or flow? Login may fail; the user cancels and the app remains in its previous state. Platform registry updates would be delivered via app updates.
- What happens if the WebView login succeeds but the success cookie is not detected (e.g., platform changed cookie names)? The login times out or the user cancels; no cookies are stored. The user sees the original error.
- What happens if a user is logged into multiple platforms and one expires? Only the expired platform's cookies are cleared; other platforms remain connected and functional.
- What happens when yt-dlp errors don't contain recognizable platform prefixes? The error is treated as a generic extraction error (not auth-related), and no "Connect" button is shown.
- What happens if the same platform URL works without auth for some content but requires auth for other content? The auth flow only triggers when extraction fails with auth-specific errors. Non-restricted content downloads normally without any login prompt.
- What happens if the platform requires two-factor authentication (2FA) during login? The WebView renders the 2FA page normally. The user completes 2FA within the WebView. Cookie detection proceeds after the final redirect — no special handling needed because the success cookie only appears after full authentication including 2FA.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: System MUST detect when an extraction failure is caused by authentication requirements based on platform-specific error patterns (e.g., "sign in to confirm", "login required", "must be logged in", "NSFW", "inappropriate").
- **FR-002**: System MUST identify which platform an authentication error corresponds to, using both the video URL and error message prefixes (e.g., "[Instagram]", "[youtube]").
- **FR-003**: System MUST display a contextual "Connect [Platform]" call-to-action button on the error screen when an authentication error is detected for a supported platform.
- **FR-004**: System MUST provide an in-app login flow via embedded browser that loads the platform's official login page.
- **FR-005**: System MUST enable autofill/credential manager assistance in the embedded browser to minimize login friction.
- **FR-006**: System MUST detect successful login by monitoring for the presence of platform-specific session cookies after each page load.
- **FR-007**: System MUST extract all cookies for the platform's domain(s) after successful login detection, formatted as Netscape cookie strings (compatible with yt-dlp's `--cookies` format).
- **FR-008**: System MUST store extracted cookies in platform-encrypted local storage (one entry per platform), never transmitting them to third parties.
- **FR-009**: System MUST clear all cookies from the embedded browser after extraction to prevent cross-platform cookie leakage.
- **FR-010**: System MUST automatically retry the failed extraction after successful login and cookie storage.
- **FR-011**: System MUST inject stored cookies into WS proxy HTTP requests by matching the request URL host against the platform's host patterns, merging with (not overwriting) any existing cookies.
- **FR-012**: System MUST forward stored cookies to the REST extraction endpoint via the `X-Platform-Cookies` header (base64-encoded Netscape cookie string) when the WS proxy path is unavailable.
- **FR-013**: The server MUST accept forwarded cookies, use them for yt-dlp extraction via a temporary cookie file, and delete the temporary file after extraction.
- **FR-014**: System MUST never inject one platform's cookies into requests for a different platform.
- **FR-015**: System MUST show connected platform status as chips/badges on the download screen's idle state, only for platforms with stored cookies.
- **FR-016**: System MUST allow users to disconnect a platform (clearing its stored cookies) via a bottom sheet triggered by tapping the platform chip.
- **FR-017**: System MUST reactively detect expired cookies (auth error + cookies already stored) by clearing stale cookies and showing "Reconnect [Platform]" instead of "Connect [Platform]".
- **FR-018**: System MUST allow users to cancel the login flow at any time without storing or modifying any cookies.
- **FR-019**: On Android, the system MUST NOT modify the local on-device yt-dlp extraction path; cookies are only used in WS proxy and REST fallback paths.
- **FR-020**: System MUST support five platforms: Instagram, YouTube, Twitter/X, Reddit, and Facebook.
- **FR-021**: System MUST NOT store usernames, passwords, or any credentials beyond session cookies.

### Key Entities

- **Supported Platform**: A social media platform that may require authentication for restricted content. Attributes: display name, login URL, cookie domains to extract from, host patterns for cookie injection, success indicator cookie name.
- **Secure Cookie Store**: Per-platform encrypted storage holding Netscape-format cookie strings. Operations: get, set, clear, check connected status, list connected platforms.
- **Platform Connection Status**: Whether a user has active stored cookies for a given platform (connected/disconnected).

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Users can download age-restricted or login-gated content from all five supported platforms after a single login per platform, with the entire connect-and-retry flow completing in under 60 seconds (excluding login time on the platform's page).
- **SC-002**: 100% of authentication-related extraction errors for supported platforms are correctly identified and show the "Connect [Platform]" CTA rather than a generic error message.
- **SC-003**: Users who never encounter restricted content experience zero additional UI elements, prompts, or friction in their normal download flow.
- **SC-004**: Stored cookies persist across app restarts and remain functional until the platform expires them, with no proactive expiry checks adding overhead.
- **SC-005**: Cookie isolation is maintained: authenticating with one platform never leaks cookies to requests for a different platform.
- **SC-006**: Users can connect and disconnect platforms independently, with the connection status UI accurately reflecting the current state at all times.
- **SC-007**: The login flow works on both Android and iOS, with platform-native embedded browsers and credential manager/autofill support on each platform.

## Assumptions

- Platform login pages render correctly in embedded WebViews/WKWebViews and do not block in-app browsers.
- Platform session cookies have sufficient longevity (weeks to months) that re-authentication is infrequent.
- yt-dlp error messages consistently contain platform-identifiable prefixes (e.g., "[Instagram]", "[youtube]") or recognizable auth-related keywords.
- The five listed platforms cover the majority of auth-gated content users encounter. Additional platforms can be added later by extending the platform registry.
- Users have existing accounts on the platforms they wish to authenticate with; the app does not facilitate account creation.
- Per-platform cookie storage is bounded by practical limits (typically < 50 KB per domain). No explicit size cap is enforced; EncryptedSharedPreferences and Keychain handle these volumes without issue.

## Scope Boundaries

**In scope**:
- WebView-based login for five platforms (Instagram, YouTube, Twitter/X, Reddit, Facebook)
- Encrypted local cookie storage per platform
- Cookie injection into WS proxy and REST fallback extraction paths
- Auth error detection and contextual CTA on the error screen
- Connection status UI with disconnect capability
- Reactive cookie expiry detection and re-authentication flow

**Out of scope**:
- OAuth flows or token-based authentication
- Reading cookies from other installed apps (browser, platform apps)
- Settings screen for connection management (inline on download screen only)
- Automatic cookie refresh or rotation
- Analytics or tracking of login events
- Platforms beyond the five specified
- Modification of the local on-device yt-dlp extraction path
- Username/password storage
