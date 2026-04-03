# Tasks: WebSocket Proxy Extraction

**Input**: Design documents from `/specs/014-ws-proxy-extraction/`
**Prerequisites**: plan.md (required), spec.md (required for user stories), research.md, data-model.md, contracts/

**Tests**: No unit-test tasks generated (no test infrastructure for server Python code or WS integration; verification is manual E2E per quickstart.md).

**Organization**: Tasks are grouped by user story to enable independent implementation and testing of each story.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3)
- Include exact file paths in descriptions

## Path Conventions

- `server/app/` for Python server code (FastAPI routes, handlers, config)
- `server/deploy/` for deployment config (nginx, scripts)
- `shared/network/src/commonMain/kotlin/com/socialvideodownloader/shared/network/` for KMP network client code
- `shared/network/src/iosMain/kotlin/com/socialvideodownloader/shared/network/` for iOS-specific network code
- `shared/data/src/iosMain/kotlin/com/socialvideodownloader/shared/data/` for iOS data layer

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Extract shared yt-dlp configuration and add WebSocket dependency to KMP

- [ ] T001 Extract shared ydl_opts and format filtering into `server/app/ytdlp_opts.py` — move `ydl_opts` dict (lines 48-67) and format filtering logic (lines 85-116) from `server/app/routes/extract.py` into a new shared module. Export `get_ydl_opts()` and `filter_formats(info_dict) -> list[FormatInfo]`. Include the `FormatInfo` Pydantic model.
- [ ] T002 Refactor `server/app/routes/extract.py` to import from `server/app/ytdlp_opts.py` — replace inline `ydl_opts` with `from app.ytdlp_opts import get_ydl_opts, filter_formats`. Keep `ExtractRequest`, `ExtractResponse` models, API key guard, error handling, and the `POST /` route. The endpoint behavior must remain identical.
- [ ] T003 Add `ktor-client-websockets` to `gradle/libs.versions.toml` — add entry `ktor-client-websockets = { group = "io.ktor", name = "ktor-client-websockets", version.ref = "ktor" }` in the `[libraries]` section (Ktor version is `3.3.0`).
- [ ] T004 Add WebSocket dependency to `shared/network/build.gradle.kts` — add `implementation(libs.ktor.client.websockets)` to the `commonMain` dependencies block.

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Server-side WebSocket proxy infrastructure — the custom RequestHandler and WS endpoint that ALL user stories depend on

**CRITICAL**: No user story work can begin until this phase is complete

- [ ] T005 Implement `WSContext` and `WebSocketProxyRH` in `server/app/ws_request_handler.py` — Create new file with: (1) `WSContext` dataclass holding websocket, event loop, pending futures dict, and atomic request counter; (2) `WebSocketProxyRH(RequestHandler)` subclass with `_SUPPORTED_URL_SCHEMES = ('http', 'https')`, implementing `_send(request)` that: generates request_id, serializes request to JSON (url, method, headers via `self._get_headers(request)`, base64-encoded body), sends via `asyncio.run_coroutine_threadsafe(ws.send_json(msg), ctx.loop)`, waits on `concurrent.futures.Future.result(timeout=90)`, builds and returns `Response(fp=BytesIO(body), url=final_url, headers=headers, status=status_code)`. Raise `TransportError` on timeout or `http_error`. (3) `inject_ws_handler(ydl, ctx)` function that builds a `RequestDirector`, adds `WebSocketProxyRH` to it, and assigns to `ydl.__dict__['_request_director']` to shadow the cached_property. Import `RequestHandler`, `Request`, `Response`, `RequestDirector` from `yt_dlp.networking.common` and `TransportError` from `yt_dlp.networking.exceptions`.
- [ ] T006 Implement WebSocket endpoint in `server/app/routes/proxy_ws.py` — Create new file with FastAPI `APIRouter`. Implement `@router.websocket("/extract")` that: (1) accepts WebSocket connection, (2) receives `extract_request` JSON message, validates `type` and `url`, (3) creates `WSContext`, (4) starts two concurrent tasks: `asyncio.to_thread(_extract, url, ctx)` for yt-dlp extraction and `_dispatch_loop(ctx, websocket)` for receiving iOS responses and resolving pending futures, (5) on extract completion sends `extract_result` with filtered format data, (6) on error sends `extract_error` with detail message, (7) closes connection. The `_extract` function creates `YoutubeDL` with `get_ydl_opts()`, calls `inject_ws_handler(ydl, ctx)`, runs `ydl.extract_info(url, download=False)`, applies `filter_formats()`. The `_dispatch_loop` listens for `http_response` and `http_error` messages, resolves matching futures by request_id. Handle `WebSocketDisconnect` gracefully.
- [ ] T007 Register WebSocket router in `server/app/main.py` — add `from app.routes import proxy_ws` and `app.include_router(proxy_ws.router, prefix="/ws")` after the existing extract router registration (line ~33).

**Checkpoint**: Server can accept WebSocket connections at `/ws/extract`, proxy yt-dlp HTTP requests, and return extraction results. Testable with `websocat ws://localhost:8000/ws/extract`.

---

## Phase 3: User Story 1 — iOS Video Extraction via WebSocket Proxy (Priority: P1) MVP

**Goal**: iOS app connects via WebSocket, proxies HTTP requests from device IP, extraction bypasses datacenter IP blocking

**Independent Test**: Paste a YouTube Shorts URL on iOS device → extraction completes via WebSocket proxy → video metadata and formats returned

### Implementation for User Story 1

- [ ] T008 [US1] Install WebSockets plugin in iOS Ktor client in `shared/network/src/iosMain/kotlin/com/socialvideodownloader/shared/network/KtorEngineFactory.ios.kt` — add `install(WebSockets) { pingIntervalMillis = 30_000 }` inside the `HttpClient(Darwin) { ... }` builder block, after the `engine { ... }` block.
- [ ] T009 [US1] Implement `WebSocketExtractorApi` in `shared/network/src/commonMain/kotlin/com/socialvideodownloader/shared/network/WebSocketExtractorApi.kt` — Create new file in package `com.socialvideodownloader.shared.network`. Class takes `HttpClient` and `ServerResponseMapper` as constructor params. Create a separate `rawClient = HttpClient(/* no ContentNegotiation */)` for proxied HTTP requests. Implement `suspend fun extractViaProxy(url: String): VideoMetadata` that: (1) computes WS URL from `ServerConfig.baseUrl` replacing `http` with `ws`, (2) opens WebSocket to `{wsUrl}/ws/extract`, (3) sends `extract_request` JSON frame, (4) loops on incoming frames handling: `http_request` → execute via `rawClient.request { url(msg.url); method(msg.method); headers(msg.headers); body(base64decode(msg.body)) }`, send back `http_response` with status, final URL, headers, base64-encoded body (or `http_error` on exception); `extract_result` → deserialize as `ServerExtractResponse`, delegate to `mapper.mapToMetadata()`, return; `extract_error` → throw `ServerExtractionException(detail, 422)`. Use `kotlinx.serialization.json.Json` for parsing frames. Handle `io.ktor.websocket.Frame.Text` frames only.
- [ ] T010 [US1] Register `WebSocketExtractorApi` in DI in `shared/network/src/commonMain/kotlin/com/socialvideodownloader/shared/network/di/NetworkModule.kt` — add `single { WebSocketExtractorApi(get(), get()) }` to the `networkModule` Koin module.
- [ ] T011 [US1] Update `ServerOnlyVideoExtractorRepository` for WS-first extraction in `shared/data/src/iosMain/kotlin/com/socialvideodownloader/shared/data/repository/ServerOnlyVideoExtractorRepository.kt` — add `wsApi: WebSocketExtractorApi` as a second constructor parameter. Change `extractInfo()` to try `wsApi.extractViaProxy(url)` first, catching any `Exception` and falling back to `serverApi.extractInfo(url)`.
- [ ] T012 [US1] Update iOS DI binding in `shared/data/src/iosMain/kotlin/com/socialvideodownloader/shared/data/di/IosDataModule.kt` — update the `ServerOnlyVideoExtractorRepository` singleton to `ServerOnlyVideoExtractorRepository(serverApi = get(), wsApi = get())`.

**Checkpoint**: iOS extraction uses WebSocket proxy with device's residential IP. YouTube/Instagram/TikTok URLs that previously failed due to datacenter IP blocking now succeed.

---

## Phase 4: User Story 2 — Fallback to Direct REST Extraction (Priority: P2)

**Goal**: If WebSocket fails, iOS automatically falls back to REST `/extract` without user intervention

**Independent Test**: Disable WS endpoint on server → iOS extraction falls back to REST `/extract`

### Implementation for User Story 2

- [ ] T013 [US2] Verify and refine fallback behavior in `shared/data/src/iosMain/kotlin/com/socialvideodownloader/shared/data/repository/ServerOnlyVideoExtractorRepository.kt` — ensure the catch block in `extractInfo()` logs the WS failure reason (use `println` or Koin logger) before falling back. Verify that `WebSocketDisconnect`, connection refused, and timeout exceptions all trigger fallback correctly. Ensure `download()` method is unaffected (still uses `serverApi.downloadFile()` directly — downloads don't go through WS proxy).

**Checkpoint**: When WS endpoint is unavailable or connection drops, iOS silently falls back to REST extraction.

---

## Phase 5: User Story 3 — Android Unaffected (Priority: P2)

**Goal**: Android build succeeds with no regressions, no WebSocket code active on Android

**Independent Test**: `./gradlew assembleDebug` succeeds, Android extraction uses local yt-dlp as before

### Implementation for User Story 3

- [ ] T014 [US3] Verify Android build and no WS code activation — run `./gradlew assembleDebug` to confirm the ktor-client-websockets dependency in commonMain doesn't break Android. Verify that `WebSocketExtractorApi` is registered in commonMain DI but only used by `ServerOnlyVideoExtractorRepository` in `iosMain` — Android's `VideoExtractorRepository` binding (in `:core:data` androidMain) is untouched. No code changes expected; this is a verification task. If the Android build fails due to the new dependency, add a platform-specific guard or move the WS dependency to iosMain only.

**Checkpoint**: Android APK builds and extraction works identically to before this feature.

---

## Phase 6: User Story 4 — Concurrent WebSocket Sessions (Priority: P3)

**Goal**: Multiple iOS clients can extract simultaneously without interference

**Independent Test**: Two concurrent WebSocket sessions with different URLs both complete independently

### Implementation for User Story 4

- [ ] T015 [US4] Verify session isolation in `server/app/ws_request_handler.py` and `server/app/routes/proxy_ws.py` — ensure each `ws_extract` call creates a fresh `WSContext` with its own `pending` dict and request counter. Verify that `WebSocketProxyRH` is instantiated per-session (not shared). Verify `inject_ws_handler` creates a new `RequestDirector` per YDL instance. Test with 5 concurrent WebSocket sessions (matching SC-005) using different URLs — all must complete independently without cross-contamination. No code changes expected if Phase 2 was implemented correctly; this is a verification/audit task.

**Checkpoint**: Five concurrent WebSocket extractions complete independently without degradation.

---

## Phase 7: Polish & Cross-Cutting Concerns

**Purpose**: Deployment config and final verification

- [ ] T016 [P] Add WebSocket support to nginx in `server/deploy/nginx.conf` — add a `location /ws/` block before the existing `location /` block with: `proxy_pass http://127.0.0.1:8000;`, `proxy_http_version 1.1;`, `proxy_set_header Upgrade $http_upgrade;`, `proxy_set_header Connection "upgrade";`, `proxy_set_header Host $host;`, `proxy_set_header X-Real-IP $remote_addr;`, `proxy_read_timeout 300s;`. Do NOT apply rate limiting to this location (WS sessions are long-lived).
- [ ] T017 [P] Add `websockets` to `server/requirements.txt` if needed — check if FastAPI WebSocket support requires any additional dependencies beyond `uvicorn[standard]` (which includes `websockets`). If `uvicorn[standard]` already covers it, no change needed.
- [ ] T018 Run full verification per `specs/014-ws-proxy-extraction/quickstart.md` — (1) server isolated test with websocat, (2) iOS build with `./gradlew linkDebugFrameworkIosSimulatorArm64`, (3) Android build with `./gradlew assembleDebug`, (4) E2E on iOS device with YouTube Shorts URL — measure extraction time, must complete within 30 seconds (SC-002), (5) fallback test with WS endpoint disabled — measure time to fall back to REST, must switch within 5 seconds (SC-003), (6) no-network test (disconnect device) — app must show user-friendly error, (7) invalid URL test via WS — server returns `extract_error`, app shows actionable error message.

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies — can start immediately
- **Foundational (Phase 2)**: Depends on T001 (shared ydl_opts) and T002 (extract.py refactor). T003-T004 (Gradle deps) are independent.
- **User Story 1 (Phase 3)**: Depends on Phase 2 (server WS endpoint) AND T003-T004 (Gradle WS deps)
- **User Story 2 (Phase 4)**: Depends on User Story 1 (T011 adds fallback logic)
- **User Story 3 (Phase 5)**: Depends on T003-T004 (Gradle deps). Can run in parallel with US1.
- **User Story 4 (Phase 6)**: Depends on Phase 2. Can run in parallel with US1.
- **Polish (Phase 7)**: T016-T017 can start after Phase 2. T018 depends on all stories.

### User Story Dependencies

- **User Story 1 (P1)**: Depends on Foundational (Phase 2) + Setup (Phase 1)
- **User Story 2 (P2)**: Depends on User Story 1 (fallback is built into US1 implementation, US2 verifies/refines it)
- **User Story 3 (P2)**: Independent — only depends on Phase 1 Gradle changes
- **User Story 4 (P3)**: Independent — only depends on Phase 2 server implementation

### Parallel Opportunities

- T001 and T003+T004 can run in parallel (server vs Gradle changes)
- T005 and T006 should be sequential (T006 depends on T005's WSContext/handler)
- T008, T009, T010 can start as soon as Phase 2 + T003-T004 complete
- T014 (Android verification) can run in parallel with T008-T012 (iOS WS client)
- T015 (concurrent sessions) can run in parallel with Phase 3
- T016 and T017 can run in parallel with any phase after Phase 2

---

## Parallel Example: Phase 1 Setup

```
# These can run in parallel (different codebases/files):
T001: Extract shared ydl_opts into server/app/ytdlp_opts.py
T003: Add ktor-client-websockets to gradle/libs.versions.toml
T004: Add WS dependency to shared/network/build.gradle.kts
```

## Parallel Example: User Story 1

```
# After Phase 2 completes, these iOS tasks are sequential:
T008: Install WebSockets plugin in KtorEngineFactory.ios.kt
T009: Implement WebSocketExtractorApi.kt (depends on T008 for WS plugin)
T010: Register in NetworkModule.kt (depends on T009)
T011: Update ServerOnlyVideoExtractorRepository.kt (depends on T009, T010)
T012: Update IosDataModule.kt (depends on T011)
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete Phase 1: Setup (T001-T004)
2. Complete Phase 2: Foundational (T005-T007) — server WS endpoint ready
3. Complete Phase 3: User Story 1 (T008-T012) — iOS WS proxy extraction working
4. **STOP and VALIDATE**: Test with YouTube Shorts URL on iOS device
5. Deploy server changes, update iOS build

### Incremental Delivery

1. Setup + Foundational → Server accepts WS connections
2. User Story 1 → iOS extracts via WS proxy (MVP!)
3. User Story 2 → Verify fallback to REST (resilience)
4. User Story 3 → Verify Android unaffected (no regressions)
5. User Story 4 → Verify concurrent session isolation
6. Polish → nginx config, final E2E verification

---

## Notes

- [P] tasks = different files, no dependencies
- [Story] label maps task to specific user story for traceability
- Server tasks (T001-T007, T016-T017) are Python; client tasks (T008-T012) are Kotlin KMP
- US2, US3, US4 are primarily verification/refinement — core implementation is in Setup + Foundational + US1
- Commit after each task or logical group
- The `rawClient` in WebSocketExtractorApi (T009) must NOT have ContentNegotiation plugin installed — it proxies raw HTTP requests
