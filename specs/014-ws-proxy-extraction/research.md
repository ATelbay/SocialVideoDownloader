# Research: WebSocket Proxy Extraction

**Feature**: 014-ws-proxy-extraction  
**Date**: 2026-04-02

## R-001: yt-dlp RequestHandler Subclassing for HTTP Proxy

**Decision**: Subclass `RequestHandler` (not `WebSocketRequestHandler`) with `_SUPPORTED_URL_SCHEMES = ('http', 'https')`.

**Rationale**: The proxy handler intercepts regular HTTP/HTTPS requests that yt-dlp makes during extraction and forwards them to the iOS client. It does NOT handle WebSocket URL schemes (`ws://`, `wss://`). `WebSocketRequestHandler` is for yt-dlp's own WebSocket connections (e.g., YouTube live chat), which is a different use case.

**Alternatives considered**:
- Subclass `WebSocketRequestHandler` — wrong; that's for `ws://`/`wss://` schemes, not for proxying `http://`/`https://` requests
- Monkey-patch `urllib` — fragile, doesn't integrate with yt-dlp's cookie/header management

**Key API surface**:
- `_send(self, request: Request) -> Response` — the one abstract method to implement
- `self._get_headers(request)` — returns merged headers including cookies from CookieJar
- `self._calculate_timeout(request)` — merged timeout value
- `Request`: `.url`, `.method`, `.headers`, `.data` (bytes or None)
- `Response(fp=BytesIO(body), url=str, headers=dict, status=int)`
- Exceptions must be `RequestError` subclasses (use `TransportError` for network errors)

## R-002: Injecting Custom RequestHandler per YoutubeDL Instance

**Decision**: Override `ydl.__dict__['_request_director']` to shadow the `functools.cached_property`, building a custom `RequestDirector` that uses `WebSocketProxyRH` instead of the default HTTP handlers.

**Rationale**: This is per-instance injection — each WebSocket session gets its own handler without touching global state. The REST `/extract` endpoint continues using the default `_request_director` (built from `_REQUEST_HANDLERS`). The `__dict__` trick works because Python checks instance `__dict__` before descriptors.

**Alternatives considered**:
- `@register_rh` global registration — pollutes all YDL instances, breaks REST endpoint
- `ydl._request_director.add_handler()` — adds alongside default handlers; need to remove defaults to force our handler, which is messier
- Subclass `YoutubeDL` — overkill for this

**Implementation detail**: Build a `RequestDirector`, add only `WebSocketProxyRH` to it, assign to `ydl.__dict__['_request_director']`. The handler receives a `WSContext` with the WebSocket reference and event loop.

## R-003: Thread↔Async Bridge Pattern

**Decision**: Use `asyncio.run_coroutine_threadsafe()` + `concurrent.futures.Future` for the bridge between yt-dlp's synchronous thread and the async WebSocket event loop.

**Rationale**: yt-dlp runs synchronously in `asyncio.to_thread()`. The WebSocket is async. The canonical pattern:
1. `_send()` (in sync thread) creates a `concurrent.futures.Future`
2. Uses `asyncio.run_coroutine_threadsafe(ws.send_json(msg), loop)` to send the proxy request
3. Blocks on `future.result(timeout=90)` waiting for the dispatch loop to resolve it
4. The async dispatch loop receives iOS responses and calls `future.set_result()`

**Alternatives considered**:
- `threading.Event` + shared dict — works but `concurrent.futures.Future` is cleaner and handles exceptions
- `asyncio.Queue` — doesn't work well across thread/async boundary for request-response correlation

## R-004: WebSocket Dependency in Ktor KMP

**Decision**: Add `ktor-client-websockets` to `libs.versions.toml` and `shared/network/build.gradle.kts` (commonMain). Install `WebSockets` plugin in iOS `createHttpClient()`.

**Rationale**: Ktor 3.3.0 (current version) includes `ktor-client-websockets` module. It provides `client.webSocket()` DSL for sending/receiving frames. The plugin must be installed per-engine. Only needed on iOS (Android doesn't use WebSocket proxy).

**Key detail**: The WebSocket client for extraction proxy uses the existing `HttpClient` with the `WebSockets` plugin added. A separate "raw" `HttpClient` without `ContentNegotiation` is needed for executing proxied HTTP requests (to avoid JSON serialization interfering with raw responses).

## R-005: Server ydl_opts Extraction

**Decision**: Extract `ydl_opts` from `extract.py` into `server/app/ytdlp_opts.py` shared module.

**Rationale**: Both REST `/extract` and WS `/ws/extract` must use identical yt-dlp settings (User-Agent, extractor_args, timeouts, etc.). Current `ydl_opts` in `extract.py` (lines 48-67) includes Chrome UA spoofing, YouTube PO token config, and Node.js runtime for bgutil.

**Current ydl_opts** (from `extract.py:48-67`):
```python
{
    "extract_flat": False,
    "noplaylist": True,
    "quiet": True,
    "no_warnings": True,
    "socket_timeout": 30,
    "http_headers": {"User-Agent": "Mozilla/5.0 ...Chrome/131.0.0.0..."},
    "extractor_args": {"youtube": ["player_client=web", "po_token=web+bgutil"]},
    "js_runtimes": {"node": {}},
}
```

## R-006: nginx WebSocket Upgrade Configuration

**Decision**: Add a `/ws/` location block before `location /` with `proxy_http_version 1.1`, `Upgrade`, and `Connection "upgrade"` headers. Set `proxy_read_timeout 300s` for long-lived connections.

**Rationale**: nginx defaults to HTTP/1.0 for upstream, which doesn't support WebSocket. The explicit upgrade headers are required. 300s timeout accommodates slow extractions (yt-dlp may make many sequential requests). Current nginx config has HTTPS + rate limiting; WS location should bypass the rate limiter (one WS session = many messages, not many connections).

## R-007: Format Filtering Reuse

**Decision**: Keep format filtering logic in `extract.py` and import it into the WS endpoint, or extract it into `ytdlp_opts.py` alongside the opts.

**Rationale**: The current format filtering (lines 85-116 in extract.py) removes mhtml/storyboard formats and builds the response model. The WS endpoint needs the same filtering before sending `extract_result` back to iOS. Extracting it ensures consistency.
