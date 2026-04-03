# Quickstart: WebSocket Proxy Extraction

**Feature**: 014-ws-proxy-extraction

## Prerequisites

- Python 3.9+ with venv for server development
- Android Studio with KMP configured
- Access to the yt-dlp API server (AWS EC2)
- `websocat` CLI tool for manual WS testing (`brew install websocat`)

## Server Development

```bash
cd server
source venv/bin/activate
pip install -r requirements.txt

# Run locally
uvicorn app.main:app --reload --host 0.0.0.0 --port 8000

# Test REST endpoint (existing)
curl -X POST http://localhost:8000/extract \
  -H "Content-Type: application/json" \
  -d '{"url": "https://www.youtube.com/shorts/xxx"}'

# Test WebSocket endpoint (new)
websocat ws://localhost:8000/ws/extract
# Then type: {"type": "extract_request", "url": "https://www.youtube.com/shorts/xxx"}
# Server will send http_request messages; respond manually with http_response
```

## KMP Client Development

```bash
# Build iOS framework
./gradlew linkDebugFrameworkIosSimulatorArm64

# Build Android (verify no regressions)
./gradlew assembleDebug

# Run ktlint
./gradlew ktlintCheck -x compileKotlinIosArm64 -x compileKotlinIosSimulatorArm64
```

## Key Files to Modify

### Server (Phase 1)
| File | Action |
|------|--------|
| `server/app/ytdlp_opts.py` | New — shared ydl_opts + format filter |
| `server/app/ws_request_handler.py` | New — WebSocketProxyRH |
| `server/app/routes/proxy_ws.py` | New — WS endpoint |
| `server/app/main.py` | Edit — register WS router |
| `server/app/routes/extract.py` | Edit — import from ytdlp_opts |
| `server/deploy/nginx.conf` | Edit — add WS location block |

### KMP Client (Phase 2)
| File | Action |
|------|--------|
| `gradle/libs.versions.toml` | Edit — add ktor-client-websockets |
| `shared/network/build.gradle.kts` | Edit — add WS dependency |
| `shared/network/src/iosMain/.../KtorEngineFactory.ios.kt` | Edit — install WebSockets plugin |
| `shared/network/src/commonMain/.../WebSocketExtractorApi.kt` | New — WS client |
| `shared/network/src/commonMain/.../di/NetworkModule.kt` | Edit — add WS DI |
| `shared/data/src/iosMain/.../ServerOnlyVideoExtractorRepository.kt` | Edit — WS-first + fallback |
| `shared/data/src/iosMain/.../di/IosDataModule.kt` | Edit — update DI binding |

## Verification Checklist

1. **Server isolated**: `websocat ws://localhost:8000/ws/extract` → send extract_request → manually respond to http_request → receive extract_result
2. **iOS build**: `./gradlew linkDebugFrameworkIosSimulatorArm64` succeeds
3. **Android build**: `./gradlew assembleDebug` succeeds (no regressions)
4. **E2E**: YouTube Shorts URL on iOS device → extraction via WS → video metadata returned
5. **Fallback**: Disable WS endpoint → iOS falls back to REST `/extract`
