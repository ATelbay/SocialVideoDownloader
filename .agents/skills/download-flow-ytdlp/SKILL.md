---
name: download-flow-ytdlp
description: "Project-specific yt-dlp/youtubedl-android download flow guidance: initialization, video info extraction, format selection, progress, MediaStore saving, cache temp files, proxy fallback, and error handling. Use for video download/extraction changes."
user-invocable: true
---

# Download Flow / yt-dlp

## Android yt-dlp rules

- Initialize `YoutubeDL.getInstance().init(context)` from application/platform setup.
- Run `getInfo`, downloads, FFmpeg/aria2c work off the main thread with injected dispatchers.
- Keep format selection deterministic and testable; use domain/shared models at UI boundaries.
- Update yt-dlp through the existing update path; do not update binaries from arbitrary UI code.

## Storage

- Save user-visible Android downloads via MediaStore under `Downloads/SocialVideoDownloader/`.
- Use `cacheDir` only for temporary yt-dlp/share/proxy artifacts; clean up temp files.
- Keep legacy path/content URI compatibility where history/library code already supports it.
- iOS storage goes through the existing shared/platform file abstractions.

## Progress and errors

- Progress state should include idle/loading/downloading/success/error/cancelled as appropriate.
- Map library/proxy errors into user-facing messages without leaking stack traces or tokens.
- Cancellation must cancel child jobs and leave partial files in a known state.

## Proxy/server

- The FastAPI/WebSocket server is an optional extraction path. Do not make the local Android flow depend on it unless the task explicitly says so.
- Network clients live in shared/network or existing server-client abstractions.
