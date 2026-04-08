# WebSocket Protocol Extension: Cookie Authentication

**Feature**: 015-platform-cookie-auth | **Date**: 2026-04-05

## Overview

Extends the existing `/ws/extract` WebSocket protocol to support cookie forwarding for authenticated content extraction.

## Changes to `extract_request` Message

### Current format (no change to required fields)

```json
{
  "type": "extract_request",
  "url": "https://www.instagram.com/reel/xxx"
}
```

### Extended format (new optional field)

```json
{
  "type": "extract_request",
  "url": "https://www.instagram.com/reel/xxx",
  "cookies": "IyBOZXRzY2FwZSBIVFRQIENvb2tpZSBGaWxlCi5pbnN0YWdy..."
}
```

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| type | string | yes | Must be `"extract_request"` |
| url | string | yes | Video URL to extract |
| cookies | string | no | Base64-encoded Netscape cookie file contents. If present, server writes to temp file and passes as `cookiefile` in yt-dlp opts to pre-seed the cookie jar. |

### Backward compatibility

- The `cookies` field is optional. Existing clients that don't send it continue to work unchanged.
- The server ignores unknown fields, so older server versions won't break if a new client sends `cookies`.

## No Changes to Other Message Types

The `http_request`, `http_response`, `http_error`, `extract_result`, and `extract_error` message types remain unchanged. Cookie injection into proxied `http_request` responses happens client-side (the client merges stored cookies into the request headers before executing with `rawClient`).

## Server Implementation Notes

In `proxy_ws.py`, after reading the initial message:

```python
# Parse optional cookies from initial message
cookies_b64 = data.get("cookies")
cookie_file_path = None
if cookies_b64:
    import base64, tempfile
    cookie_bytes = base64.b64decode(cookies_b64)
    fd, cookie_file_path = tempfile.mkstemp(suffix=".txt", prefix="ytdlp_cookies_")
    os.write(fd, cookie_bytes)
    os.close(fd)

# Pass to yt-dlp opts
opts = get_ydl_opts(cookiefile=cookie_file_path)

# Cleanup after extraction
if cookie_file_path and os.path.exists(cookie_file_path):
    os.unlink(cookie_file_path)
```
