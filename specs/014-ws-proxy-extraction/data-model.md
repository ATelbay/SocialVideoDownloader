# Data Model: WebSocket Proxy Extraction

**Feature**: 014-ws-proxy-extraction  
**Date**: 2026-04-02

## Entities

### WSContext (Server-side, per-session)

Holds the state for one WebSocket extraction session.

| Field | Type | Description |
|-------|------|-------------|
| websocket | WebSocket | FastAPI WebSocket connection |
| loop | asyncio.AbstractEventLoop | The async event loop for thread↔async bridge |
| pending | dict[str, Future] | Map of request_id → concurrent.futures.Future awaiting iOS response |
| request_counter | int | Atomic counter for generating unique request IDs (req-001, req-002, ...) |

**Lifecycle**: Created when WebSocket connects, destroyed when extraction completes or connection closes.

### ProxyRequest (Wire format: server → iOS)

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| type | string | yes | Always `"http_request"` |
| id | string | yes | Unique request ID (e.g., `"req-001"`) |
| url | string | yes | Full URL to request |
| method | string | yes | HTTP method (`GET`, `POST`, `HEAD`, etc.) |
| headers | dict[string, string] | yes | Full HTTP headers including cookies |
| body | string or null | no | Base64-encoded request body, null if none |

### ProxyResponse (Wire format: iOS → server)

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| type | string | yes | `"http_response"` or `"http_error"` |
| id | string | yes | Matching request ID |
| status | int | yes* | HTTP status code (*only for http_response) |
| url | string | yes* | Final URL after redirects (*only for http_response) |
| headers | dict[string, string] | yes* | Response headers (*only for http_response) |
| body | string | yes* | Base64-encoded response body (*only for http_response) |
| error | string | yes** | Error description (**only for http_error) |

### ExtractRequest (Wire format: iOS → server)

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| type | string | yes | Always `"extract_request"` |
| url | string | yes | Video URL to extract |

### ExtractResult (Wire format: server → iOS)

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| type | string | yes | `"extract_result"` or `"extract_error"` |
| data | object | yes* | Same structure as REST `/extract` response (*only for extract_result) |
| detail | string | yes** | Error message (**only for extract_error) |

## State Transitions

```
iOS connects → WebSocket OPEN
  → iOS sends extract_request
    → Server starts yt-dlp in thread + dispatch loop
      → yt-dlp calls _send() → server sends http_request to iOS
        → iOS sends http_response OR http_error
          → server resolves Future, yt-dlp continues
      → (repeat for each HTTP request yt-dlp needs)
    → yt-dlp completes → server sends extract_result OR extract_error
  → WebSocket CLOSED
```

## No Persistent Storage Changes

This feature does not modify any database schemas, Room entities, or persistent storage. All data is transient within the WebSocket session lifetime.
