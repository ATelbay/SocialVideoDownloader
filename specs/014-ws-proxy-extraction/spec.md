# Feature Specification: WebSocket Proxy Extraction

**Feature Branch**: `014-ws-proxy-extraction`  
**Created**: 2026-04-02  
**Status**: Draft  
**Input**: User description: "WebSocket proxy request handler — iOS makes HTTP requests on behalf of yt-dlp server to bypass datacenter IP detection"

## User Scenarios & Testing *(mandatory)*

### User Story 1 - iOS Video Extraction via WebSocket Proxy (Priority: P1)

An iOS user pastes a video URL (YouTube, Instagram, TikTok, etc.) into the app. The app connects to the server via WebSocket, the server runs yt-dlp for parsing, but all HTTP requests that yt-dlp needs to make are proxied back to the iOS device. The device executes these HTTP requests from its residential IP, sends the responses back, and the server completes the extraction. The user sees video metadata and available formats as usual.

**Why this priority**: This is the core feature. Without it, extraction on iOS fails for sites that block datacenter IPs (YouTube, Instagram, TikTok). This directly solves the bot detection problem.

**Independent Test**: Paste a YouTube Shorts URL on iOS device, extraction completes via WebSocket proxy, video metadata and formats are returned successfully.

**Acceptance Scenarios**:

1. **Given** an iOS user with the app open, **When** they paste a YouTube URL and trigger extraction, **Then** the app connects via WebSocket, proxies all HTTP requests from the device's IP, and returns video metadata with available formats.
2. **Given** the server sends an HTTP request to the iOS client via WebSocket, **When** the iOS device executes the request, **Then** the response (status, headers, body, final URL after redirects) is sent back to the server within 90 seconds.
3. **Given** the server sends an HTTP request that includes cookies managed by yt-dlp, **When** the iOS device executes it, **Then** all headers including cookies are faithfully forwarded.

---

### User Story 2 - Fallback to Direct REST Extraction (Priority: P2)

If the WebSocket connection fails (server doesn't support WS, network issue, timeout), the iOS app automatically falls back to the existing REST API extraction endpoint. The user may see reduced success rates (datacenter IP blocks) but the app remains functional.

**Why this priority**: Ensures backward compatibility and resilience. Users should never be completely blocked from extraction even if the WebSocket proxy is unavailable.

**Independent Test**: Disable WebSocket endpoint on the server, attempt extraction on iOS — app falls back to REST `/extract` and either succeeds or shows an appropriate error from the server.

**Acceptance Scenarios**:

1. **Given** the WebSocket endpoint is unavailable, **When** an iOS user attempts extraction, **Then** the app automatically retries via the REST API without user intervention.
2. **Given** a WebSocket connection drops mid-extraction (e.g., app backgrounded), **When** the pending requests time out, **Then** the extraction fails cleanly with a user-friendly error and the user can retry.

---

### User Story 3 - Android Unaffected (Priority: P2)

Android users continue using the local yt-dlp binary for extraction. The WebSocket proxy feature is iOS-only. No changes to the Android extraction flow.

**Why this priority**: Android already works via local yt-dlp. This story ensures no regressions are introduced.

**Independent Test**: Build and run Android debug APK, perform extraction — behavior is identical to before this feature.

**Acceptance Scenarios**:

1. **Given** an Android user, **When** they extract a video, **Then** the local yt-dlp binary is used (no WebSocket proxy involved).
2. **Given** the WebSocket proxy code is deployed, **When** the Android app is built, **Then** the build succeeds and no WebSocket-related code is active on Android.

---

### User Story 4 - Server Handles Multiple Concurrent WebSocket Sessions (Priority: P3)

Multiple iOS users can connect simultaneously. Each WebSocket session has its own isolated yt-dlp instance and request handler. One user's extraction does not interfere with another's.

**Why this priority**: Important for multi-user support but not critical for initial single-user personal tool use.

**Independent Test**: Open two WebSocket connections simultaneously with different URLs — both complete independently.

**Acceptance Scenarios**:

1. **Given** two iOS users connected via WebSocket simultaneously, **When** both trigger extraction, **Then** each session processes independently without cross-contamination of requests or responses.

---

### Edge Cases

- What happens when the iOS app goes to background during extraction? The WebSocket connection breaks, pending server-side request futures time out (90s), and extraction fails. User sees an error and can retry.
- What happens if a proxied HTTP request times out on the iOS side? The client sends an error message, and the server propagates the error to yt-dlp which may retry or fail the extraction.
- What happens with very large response bodies (>1MB)? Bodies are base64-encoded. For typical HTML/JSON responses (<1MB), the ~33% overhead is acceptable. Unusually large responses may cause increased latency but should still work within WebSocket frame limits.
- What happens if the server sends a request for a non-HTTP scheme? The proxy handler only supports HTTP and HTTPS schemes. Other schemes fall through to yt-dlp's default handling or error.
- What happens if yt-dlp needs to follow redirects? The iOS client follows redirects automatically and includes the final URL in the response for yt-dlp to track correctly.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: System MUST establish a WebSocket connection between the iOS client and the server for video extraction sessions.
- **FR-002**: System MUST proxy all HTTP/HTTPS requests that yt-dlp needs to make through the iOS client's network connection, using the device's residential IP.
- **FR-003**: System MUST use a JSON-based message protocol over WebSocket text frames with message types: `extract_request`, `http_request`, `http_response`, `http_error`, `extract_result`, `extract_error`.
- **FR-004**: System MUST correlate proxied requests and responses using unique request IDs.
- **FR-005**: System MUST encode HTTP response bodies in base64 within the WebSocket protocol.
- **FR-006**: System MUST forward all HTTP headers from yt-dlp (including cookies) faithfully in proxied requests.
- **FR-007**: System MUST include the final URL after redirects in proxied HTTP responses.
- **FR-008**: System MUST time out pending proxy requests after 90 seconds if no response is received from the iOS client.
- **FR-009**: System MUST fall back to direct REST API extraction on iOS when the WebSocket connection fails or is unavailable.
- **FR-010**: System MUST NOT affect the Android extraction flow (local yt-dlp remains the primary method).
- **FR-011**: System MUST isolate each WebSocket session so concurrent users do not interfere with each other.
- **FR-012**: The existing REST extraction endpoint MUST continue to function unchanged for both direct API use and as a fallback.
- **FR-013**: The server's WebSocket endpoint and REST endpoint MUST share the same yt-dlp configuration/options.

### Key Entities

- **WebSocket Session**: A single extraction session connecting one iOS client to one yt-dlp instance on the server. Contains: connection state, pending request futures, event loop reference.
- **Proxy Request**: An HTTP request that yt-dlp needs to make, serialized and forwarded to the iOS client. Contains: request ID, URL, method, headers, optional body.
- **Proxy Response**: The iOS client's HTTP response sent back to the server. Contains: request ID, status code, final URL, headers, base64-encoded body.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: iOS users can successfully extract video metadata from YouTube, Instagram, and TikTok URLs that previously failed due to datacenter IP blocking.
- **SC-002**: Extraction via WebSocket proxy completes within 30 seconds for standard video URLs (comparable to direct extraction when not IP-blocked).
- **SC-003**: When the WebSocket proxy is unavailable, the iOS app falls back to REST extraction within 5 seconds without user intervention.
- **SC-004**: Android build and extraction flow remain unaffected — no regressions in existing functionality.
- **SC-005**: The server supports at least 5 concurrent WebSocket extraction sessions without degradation.

## Scope

### In Scope

- Server-side WebSocket endpoint with custom yt-dlp request handler
- iOS/KMP WebSocket client that proxies HTTP requests
- JSON-over-WebSocket message protocol
- Shared yt-dlp configuration between REST and WebSocket endpoints
- WS-first with REST fallback on iOS
- Reverse proxy WebSocket upgrade support

### Out of Scope

- Cookie persistence across extraction sessions (yt-dlp manages cookies per-session)
- Background execution / keep-alive when iOS app is backgrounded
- Android WebSocket proxy support (Android uses local yt-dlp)
- Video download proxying (only metadata extraction is proxied)
- Authentication or rate limiting on the WebSocket endpoint

## Assumptions

- iOS devices have reliable residential IP addresses that are not flagged by video platforms.
- The 90-second timeout per proxied request is sufficient for all normal HTTP interactions during extraction.
- Base64 encoding overhead (~33%) for response bodies is acceptable given typical payload sizes (<1MB).
- The server's yt-dlp request handler can be overridden per-instance without affecting global state.
- The server runs behind a reverse proxy that can be configured for WebSocket upgrade.
- Single extraction sessions are sequential in their HTTP requests (yt-dlp typically makes one request at a time), but the protocol supports concurrent requests via request ID correlation.

## Dependencies

- Existing server infrastructure (FastAPI on AWS EC2)
- Existing REST extraction endpoint
- yt-dlp library with request handler extensibility
- WebSocket client support in the KMP/iOS networking layer
- Reverse proxy with WebSocket support
