# REST API Extension: Cookie Authentication

**Feature**: 015-platform-cookie-auth | **Date**: 2026-04-05

## Overview

Extends the existing `POST /extract` REST endpoint to accept platform cookies for authenticated content extraction.

## New Request Header

| Header | Type | Required | Description |
|--------|------|----------|-------------|
| X-Platform-Cookies | string | no | Base64-encoded Netscape cookie file contents. Platform-agnostic — server feeds directly to yt-dlp. |

### Example Request

```http
POST /extract HTTP/1.1
Content-Type: application/json
X-API-Key: your-api-key
X-Platform-Cookies: IyBOZXRzY2FwZSBIVFRQIENvb2tpZSBGaWxlCi5pbnN0YWdyYW0uY29tCVRSVUUJLwlUUlVFCTAJc2Vzc2lvbmlkCWFiYzEyMw==

{"url": "https://www.instagram.com/reel/xxx"}
```

### Decoded Cookie Content

```
# Netscape HTTP Cookie File
.instagram.com	TRUE	/	TRUE	0	sessionid	abc123
.instagram.com	TRUE	/	TRUE	0	csrftoken	xyz789
```

## Backward Compatibility

- Header is optional. Requests without it work exactly as before.
- No changes to the response format (`ExtractResponse` remains the same).
- No changes to error responses (auth failures still return 422 with yt-dlp error detail).

## Server Implementation Notes

In `extract.py`:

```python
# Read optional cookies
cookies_b64 = request.headers.get("X-Platform-Cookies")
cookie_file_path = None
if cookies_b64:
    import base64, tempfile
    cookie_bytes = base64.b64decode(cookies_b64)
    fd, cookie_file_path = tempfile.mkstemp(suffix=".txt", prefix="ytdlp_cookies_")
    os.write(fd, cookie_bytes)
    os.close(fd)

try:
    opts = get_ydl_opts(cookiefile=cookie_file_path)
    # ... existing extraction logic using opts ...
finally:
    if cookie_file_path and os.path.exists(cookie_file_path):
        os.unlink(cookie_file_path)
```

## Changes to `get_ydl_opts()`

```python
def get_ydl_opts(cookiefile: str | None = None) -> dict:
    opts = {
        # ... existing opts ...
    }
    if cookiefile:
        opts["cookiefile"] = cookiefile
    return opts
```

## Security Considerations

- Cookie file is written to a tempfile with a unique name and deleted immediately after extraction.
- Cookies are transmitted over the same HTTPS/WSS connection already used for extraction — no additional attack surface.
- The server does not log, store, or inspect cookie contents.
- Base64 encoding is for transport safety (cookie strings contain tabs and newlines), not for encryption.
