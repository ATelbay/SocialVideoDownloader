# Implementation Plan: WebSocket Proxy Extraction

**Branch**: `014-ws-proxy-extraction` | **Date**: 2026-04-02 | **Spec**: [spec.md](spec.md)
**Input**: Feature specification from `/specs/014-ws-proxy-extraction/spec.md`

## Summary

iOS video extraction fails on YouTube, Instagram, and TikTok because the yt-dlp API server's datacenter IP gets blocked by bot detection. Instead of buying residential proxies, this feature makes the iOS device proxy HTTP requests on behalf of yt-dlp via WebSocket. The server runs yt-dlp for parsing; every HTTP request yt-dlp needs is forwarded to the iOS client over WS, executed from the device's residential IP, and the response sent back. A custom yt-dlp `RequestHandler` bridges the sync yt-dlp thread with the async WebSocket connection. The iOS app uses WS-first with REST fallback. Android is unaffected.

## Technical Context

**Language/Version**: Python 3.9+ (server), Kotlin 2.2.10 (KMP/shared), Swift 6.x (iOS shell)
**Primary Dependencies**: FastAPI + uvicorn (server), yt-dlp >= 2025.1.1, Ktor 3.3.0 + ktor-client-websockets (KMP), Koin 4.1.0 (shared DI)
**Storage**: N/A — no persistent storage changes. All data is transient within WebSocket session lifetime.
**Testing**: Manual WebSocket testing via websocat (server), `./gradlew assembleDebug` + `linkDebugFrameworkIosSimulatorArm64` (builds), E2E on iOS device
**Target Platform**: iOS 16.0+ (primary beneficiary), server (FastAPI on AWS EC2). Android unaffected.
**Project Type**: Cross-platform mobile app (KMP) + Python API server
**Performance Goals**: Extraction via WS proxy completes within 30 seconds for standard URLs. Per-request proxy roundtrip <5 seconds typical, 90 second hard timeout.
**Constraints**: yt-dlp is synchronous (runs in thread); WebSocket is async — requires thread↔async bridge. iOS app backgrounding kills WS connection. Server must support concurrent sessions with isolation.
**Scale/Scope**: Personal utility, ~13 files changed/created across server and KMP. Two phases: server (5 files) then client (7 files).

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principle | Status | Notes |
|-----------|--------|-------|
| I. Privacy & Zero Bloat | PASS | No analytics, no tracking. WS proxy is purely functional. No user data transmitted beyond the video extraction requests. |
| II. On-Device (Android) / Server-Mediated (iOS) | PASS | Android untouched — still uses local yt-dlp. iOS continues using server for extraction (Apple §2.5.2); the WS proxy is an enhancement to the existing server-mediated architecture, not a new dependency. |
| III. Modern Stack | PASS | Server: FastAPI (existing). KMP: Ktor WebSocket (same library family as existing HTTP client). Koin DI. No new frameworks introduced. |
| IV. Modular Separation | PASS | Server code in `server/app/`. KMP client code in `:shared:network` (commonMain for WS client, iosMain for engine config) and `:shared:data` (iosMain for repository). No new modules created. |
| V. Minimal Friction UX | PASS | User flow unchanged — same number of taps. WS proxy is transparent to the user. Fallback to REST ensures no degradation. |
| VI. Test Discipline | PASS | Server testable via websocat manually. KMP builds verified. E2E test plan included. |
| VII. Simplicity & Focus | PASS | Solves a concrete problem (IP blocking) with minimal complexity. No over-engineering — the custom RequestHandler is the simplest approach to bridge yt-dlp with WS. |
| VIII. Optional Cloud Features | N/A | This feature does not involve cloud sync, backup, or authentication. |

**Gate result**: PASS — no violations.

## Project Structure

### Documentation (this feature)

```text
specs/014-ws-proxy-extraction/
├── plan.md              # This file
├── research.md          # Phase 0 output — 7 research decisions
├── data-model.md        # Phase 1 output — wire protocol entities, state transitions
├── quickstart.md        # Phase 1 output — dev setup and verification
├── contracts/
│   └── websocket-protocol.md  # Phase 1 output — full WS message format contract
└── checklists/
    └── requirements.md  # Spec quality checklist
```

### Source Code (affected files)

```text
server/
├── app/
│   ├── ytdlp_opts.py              # NEW — shared ydl_opts + format filtering
│   ├── ws_request_handler.py      # NEW — WebSocketProxyRH + WSContext
│   ├── main.py                    # EDIT — register WS router
│   └── routes/
│       ├── extract.py             # EDIT — import from ytdlp_opts
│       └── proxy_ws.py            # NEW — WS endpoint + dispatch loop
└── deploy/
    └── nginx.conf                 # EDIT — add /ws/ location block

gradle/
└── libs.versions.toml             # EDIT — add ktor-client-websockets

shared/
├── network/
│   ├── build.gradle.kts           # EDIT — add WS dependency
│   └── src/
│       ├── commonMain/kotlin/com/socialvideodownloader/shared/network/
│       │   ├── WebSocketExtractorApi.kt   # NEW — WS extraction client
│       │   └── di/NetworkModule.kt        # EDIT — add WS DI
│       └── iosMain/kotlin/com/socialvideodownloader/shared/network/
│           └── KtorEngineFactory.ios.kt   # EDIT — install WebSockets plugin
└── data/
    └── src/iosMain/kotlin/com/socialvideodownloader/shared/data/
        ├── repository/ServerOnlyVideoExtractorRepository.kt  # EDIT — WS-first + fallback
        └── di/IosDataModule.kt                               # EDIT — update DI binding
```

**Structure Decision**: No new modules. Server gets 3 new files in existing `server/app/`. KMP client gets 1 new file in existing `:shared:network`. All other changes are edits to existing files.

## Design Decisions

### D-001: Custom RequestHandler (not WebSocketRequestHandler)

Subclass `RequestHandler` with `_SUPPORTED_URL_SCHEMES = ('http', 'https')`. This intercepts regular HTTP requests yt-dlp makes during extraction. `WebSocketRequestHandler` is wrong — it's for `ws://`/`wss://` URL schemes (YouTube live chat, etc.).

### D-002: Per-instance _request_director override

Assign custom `RequestDirector` to `ydl.__dict__['_request_director']` to shadow the `functools.cached_property`. Each WS session gets its own handler. REST endpoint is untouched (uses default cached_property).

### D-003: Thread↔Async bridge via run_coroutine_threadsafe + Future

`_send()` in the sync yt-dlp thread uses `asyncio.run_coroutine_threadsafe()` to send WS messages and `concurrent.futures.Future.result(timeout=90)` to wait for responses. The async dispatch loop resolves futures as iOS sends responses.

### D-004: Base64 body encoding

Response bodies encoded in base64 within JSON text frames. ~33% overhead acceptable for typical <1MB HTML/JSON payloads. Avoids binary frame complexity.

### D-005: WS-first with REST fallback (iOS)

`ServerOnlyVideoExtractorRepository` tries `WebSocketExtractorApi.extractViaProxy()` first. On any exception (connection refused, timeout, WS not supported), falls back to `ServerVideoExtractorApi.extractInfo()`. User never blocked.

### D-006: Shared ydl_opts module

Extract `ydl_opts` dict and format filtering logic from `extract.py` into `ytdlp_opts.py`. Both REST and WS endpoints import from the same source. Prevents configuration drift.

### D-007: Raw HTTP client for proxy requests (iOS)

The WebSocket extraction client needs a separate `HttpClient` without `ContentNegotiation` plugin for executing proxied HTTP requests. The main `HttpClient` has JSON serialization that would interfere with raw response proxying.

## Implementation Phases

### Phase 1: Server-side WebSocket Proxy (5 files)

**Goal**: Server can accept WS connections and proxy yt-dlp's HTTP requests to the client.

1. **`server/app/ytdlp_opts.py`** (new)
   - Extract `ydl_opts` dict from `extract.py:48-67`
   - Extract format filtering logic from `extract.py:85-116`
   - Export: `get_ydl_opts()`, `filter_formats(info_dict) -> list[FormatInfo]`

2. **`server/app/routes/extract.py`** (edit)
   - Replace inline `ydl_opts` with `from app.ytdlp_opts import get_ydl_opts, filter_formats`
   - Simplify extraction function to use shared imports
   - Preserve API key guard, error handling, response model

3. **`server/app/ws_request_handler.py`** (new)
   - `WSContext` dataclass: websocket, loop, pending dict, request counter
   - `WebSocketProxyRH(RequestHandler)`:
     - `_SUPPORTED_URL_SCHEMES = ('http', 'https')`
     - `_send(request)`: generate request_id, serialize request (url, method, headers via `self._get_headers(request)`, base64 body), send via `run_coroutine_threadsafe`, wait on Future with 90s timeout, build Response
     - Raise `TransportError` on timeout or error
   - `inject_ws_handler(ydl, ctx)`: build RequestDirector with WebSocketProxyRH, assign to `ydl.__dict__['_request_director']`

4. **`server/app/routes/proxy_ws.py`** (new)
   - `@router.websocket("/extract")` endpoint
   - Accept connection, receive `extract_request` message
   - Create `WSContext`, start two concurrent tasks:
     - `asyncio.to_thread(_extract, url, ctx)` — runs yt-dlp with injected handler
     - `_dispatch_loop(ctx, websocket)` — receives iOS messages, resolves pending futures
   - On completion: send `extract_result` or `extract_error`, close connection
   - `_extract(url, ctx)`: create YoutubeDL with shared opts, inject WS handler, run `extract_info`, filter formats

5. **`server/deploy/nginx.conf`** (edit)
   - Add `/ws/` location block before `location /`
   - WebSocket upgrade headers, 300s read timeout
   - No rate limiting on WS (one connection = one session)

6. **`server/app/main.py`** (edit)
   - Import `proxy_ws` router
   - `app.include_router(proxy_ws.router, prefix="/ws")`

### Phase 2: KMP/iOS WebSocket Client (7 files)

**Goal**: iOS app uses WebSocket proxy for extraction with REST fallback.

1. **`gradle/libs.versions.toml`** (edit)
   - Add: `ktor-client-websockets = { group = "io.ktor", name = "ktor-client-websockets", version.ref = "ktor" }`

2. **`shared/network/build.gradle.kts`** (edit)
   - Add to commonMain dependencies: `implementation(libs.ktor.client.websockets)`

3. **`shared/network/src/iosMain/.../KtorEngineFactory.ios.kt`** (edit)
   - Add `install(WebSockets) { pingIntervalMillis = 30_000 }` to the HttpClient config

4. **`shared/network/src/commonMain/.../WebSocketExtractorApi.kt`** (new)
   - Class with `HttpClient` (for WS) and `ServerResponseMapper` dependencies
   - Separate raw `HttpClient` (no ContentNegotiation) for proxied HTTP requests
   - `extractViaProxy(url: String): VideoMetadata`:
     - Connect to `{wsUrl}/ws/extract`
     - Send `extract_request`
     - Loop on incoming frames:
       - `http_request` → execute via rawClient, send `http_response` or `http_error`
       - `extract_result` → map to VideoMetadata, return
       - `extract_error` → throw ServerExtractionException

5. **`shared/network/src/commonMain/.../di/NetworkModule.kt`** (edit)
   - Add: `single { WebSocketExtractorApi(get(), get()) }`

6. **`shared/data/src/iosMain/.../ServerOnlyVideoExtractorRepository.kt`** (edit)
   - Add `wsApi: WebSocketExtractorApi` constructor parameter
   - `extractInfo()`: try `wsApi.extractViaProxy()`, catch any exception → fallback to `serverApi.extractInfo()`

7. **`shared/data/src/iosMain/.../di/IosDataModule.kt`** (edit)
   - Update `ServerOnlyVideoExtractorRepository` binding to include `wsApi = get()`

## Complexity Tracking

No constitution violations to justify — all gates passed.

## Post-Phase 1 Constitution Re-check

| Principle | Status | Notes |
|-----------|--------|-------|
| I. Privacy & Zero Bloat | PASS | No new tracking or analytics. WS proxy is transparent infrastructure. |
| II. Server-Mediated (iOS) | PASS | Enhancement to existing server architecture. Android path untouched. |
| III. Modern Stack | PASS | Ktor WebSockets (same library family). FastAPI WebSocket (same framework). |
| IV. Modular Separation | PASS | No new modules. Code placed in correct existing modules. |
| VII. Simplicity & Focus | PASS | Minimal new abstractions: 1 handler class, 1 context object, 1 API client. |
