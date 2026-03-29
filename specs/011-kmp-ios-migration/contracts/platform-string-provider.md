# Contract: PlatformStringProvider

**Location**: `shared/data/src/commonMain/kotlin/.../platform/PlatformStringProvider.kt`

## Interface

```
PlatformStringProvider
  └── getString(key: StringKey): String
      Resolve a platform-localized string by typed key.
```

## StringKey (enum in commonMain)
```
StringKey
  ├── ERROR_NETWORK
  ├── ERROR_SERVER_UNAVAILABLE
  ├── ERROR_EXTRACTION_FAILED
  ├── ERROR_UNSUPPORTED_URL
  ├── ERROR_STORAGE_FULL
  ├── ERROR_DOWNLOAD_FAILED
  ├── ERROR_UNKNOWN
  ├── HISTORY_DELETED
  ├── HISTORY_ALL_DELETED
  ├── HISTORY_RESTORED
  ├── LIBRARY_OPEN_ERROR
  └── COPY_SUCCESS
```

## Platform Implementations

### Android: `AndroidStringProvider`
- Maps `StringKey` → `R.string.*` resource ID
- Uses `Context.getString(resId)` for localized strings
- Requires `Context` injection

### iOS: `IosStringProvider`
- Maps `StringKey` → `NSLocalizedString` key
- Uses `Bundle.main` for string lookup
- Strings defined in `Localizable.strings`

## Design Decision
Replaces `@StringRes Int` pattern used in current Android ViewModels. Shared ViewModels emit `StringKey` enum values in effects; platform UI layers resolve them to localized strings. This keeps shared code platform-independent while preserving localization support.
