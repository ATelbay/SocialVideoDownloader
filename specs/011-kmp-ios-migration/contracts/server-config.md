# Contract: ServerConfig

**Location**: `shared/network/src/commonMain/kotlin/.../ServerConfig.kt` (expect/actual)

## Interface

```
expect object ServerConfig
  ├── val baseUrl: String          # Server base URL
  ├── val extractApiKey: String?   # Optional API key
  ├── val extractPath: String      # "/extract" (constant)
  ├── val connectTimeoutSeconds: Long   # 10L (constant)
  └── val readTimeoutSeconds: Long      # 60L (constant)
```

## Platform Implementations

### Android: `actual object ServerConfig`
- `baseUrl` reads from `BuildConfig.YTDLP_SERVER_URL` (injected at build time)
- `extractApiKey` reads from `BuildConfig.YTDLP_API_KEY`
- Same behavior as current `ServerConfig` in core/data

### iOS: `actual object ServerConfig`
- `baseUrl` reads from `Info.plist` key `YTDLP_SERVER_URL` or defaults to `http://13.50.106.77:8000`
- `extractApiKey` reads from `Info.plist` key `YTDLP_API_KEY` or defaults to null
- Configured via Xcode build settings → Info.plist preprocessing
