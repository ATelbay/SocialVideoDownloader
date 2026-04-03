# WebSocket Proxy Protocol Contract

**Version**: 1.0  
**Transport**: JSON over WebSocket text frames  
**Endpoint**: `wss://{server}/ws/extract`

## Message Types

### 1. `extract_request` (iOS → Server)

Initiates extraction. Sent once per WebSocket session immediately after connection.

```json
{
  "type": "extract_request",
  "url": "https://instagram.com/reel/xxx"
}
```

| Field | Type | Required | Constraints |
|-------|------|----------|-------------|
| type | string | yes | Literal `"extract_request"` |
| url | string | yes | Valid HTTP/HTTPS URL |

### 2. `http_request` (Server → iOS)

Server asks iOS to execute an HTTP request on its behalf.

```json
{
  "type": "http_request",
  "id": "req-001",
  "url": "https://www.youtube.com/watch?v=xxx",
  "method": "GET",
  "headers": {
    "User-Agent": "Mozilla/5.0 ...",
    "Cookie": "VISITOR_INFO1_LIVE=...",
    "Accept": "text/html,application/xhtml+xml"
  },
  "body": null
}
```

| Field | Type | Required | Constraints |
|-------|------|----------|-------------|
| type | string | yes | Literal `"http_request"` |
| id | string | yes | Unique within session, format `"req-NNN"` |
| url | string | yes | Full URL including query params |
| method | string | yes | HTTP method (GET, POST, HEAD, etc.) |
| headers | object | yes | String→String map of all headers including cookies |
| body | string/null | no | Base64-encoded request body, or null |

### 3. `http_response` (iOS → Server)

iOS returns the result of an HTTP request.

```json
{
  "type": "http_response",
  "id": "req-001",
  "status": 200,
  "url": "https://final-url-after-redirects.com/...",
  "headers": {
    "Content-Type": "text/html; charset=utf-8",
    "Set-Cookie": "..."
  },
  "body": "PGh0bWw+Li4uPC9odG1sPg=="
}
```

| Field | Type | Required | Constraints |
|-------|------|----------|-------------|
| type | string | yes | Literal `"http_response"` |
| id | string | yes | Must match the `http_request.id` |
| status | int | yes | HTTP status code (200, 301, 404, etc.) |
| url | string | yes | Final URL after all redirects |
| headers | object | yes | String→String map of response headers |
| body | string | yes | Base64-encoded response body |

### 4. `http_error` (iOS → Server)

iOS reports that an HTTP request failed (network error, timeout, etc.).

```json
{
  "type": "http_error",
  "id": "req-001",
  "error": "Connection timed out"
}
```

| Field | Type | Required | Constraints |
|-------|------|----------|-------------|
| type | string | yes | Literal `"http_error"` |
| id | string | yes | Must match the `http_request.id` |
| error | string | yes | Human-readable error description |

### 5. `extract_result` (Server → iOS)

Extraction completed successfully. Payload matches the REST `/extract` response format.

```json
{
  "type": "extract_result",
  "data": {
    "title": "Video Title",
    "thumbnail": "https://...",
    "duration": 120,
    "formats": [
      {
        "format_id": "18",
        "ext": "mp4",
        "resolution": "640x360",
        "filesize": 12345678,
        "url": "https://...",
        "vcodec": "avc1.42001E",
        "acodec": "mp4a.40.2"
      }
    ]
  }
}
```

| Field | Type | Required | Constraints |
|-------|------|----------|-------------|
| type | string | yes | Literal `"extract_result"` |
| data | object | yes | Same schema as REST `POST /extract` response body |

### 6. `extract_error` (Server → iOS)

Extraction failed.

```json
{
  "type": "extract_error",
  "detail": "Unsupported URL"
}
```

| Field | Type | Required | Constraints |
|-------|------|----------|-------------|
| type | string | yes | Literal `"extract_error"` |
| detail | string | yes | Human-readable error message |

## Session Lifecycle

```
1. iOS opens WebSocket to wss://{server}/ws/extract
2. Server accepts connection
3. iOS sends one extract_request message
4. Server starts yt-dlp extraction in background thread
5. Loop:
   a. Server sends http_request (yt-dlp needs to fetch a URL)
   b. iOS executes the HTTP request from its own IP
   c. iOS sends http_response or http_error
   d. Server feeds response back to yt-dlp
6. Server sends extract_result or extract_error
7. WebSocket closes
```

## Timeouts

| Timeout | Value | Description |
|---------|-------|-------------|
| Per-request response | 90 seconds | Server waits this long for iOS to respond to an `http_request` |
| WebSocket idle | 300 seconds | Reverse proxy read timeout for the WS connection |
| iOS socket timeout | 60 seconds | iOS URLSession timeout for individual proxied HTTP requests |

## Error Handling

- If iOS sends `http_error`, server wraps it as a `TransportError` for yt-dlp, which may retry or fail
- If iOS disconnects mid-session, all pending futures timeout → extraction fails → server logs error
- If yt-dlp throws any exception, server sends `extract_error` and closes the WebSocket
- If iOS sends an unknown message type, server ignores it
