# Contract: PlatformFileStorage

**Location**: `shared/data/src/commonMain/kotlin/.../platform/PlatformFileStorage.kt`

## Interface

```
PlatformFileStorage
  ├── saveToDownloads(tempFilePath: String, fileName: String, mimeType: String): SaveResult
  │   Move a downloaded file from temp location to the platform's downloads directory.
  │   Returns the permanent file path and optional platform URI.
  │
  ├── isFileAccessible(filePath: String): Boolean
  │   Check if a previously saved file still exists and is accessible.
  │
  ├── deleteFile(filePath: String): Boolean
  │   Delete a downloaded file. Returns true if deleted or already absent.
  │
  └── getShareableUri(filePath: String): String?
      Get a URI suitable for sharing the file with other apps.
```

## SaveResult
```
SaveResult
  ├── filePath: String       # Permanent file path
  ├── platformUri: String?   # Android: content:// URI. iOS: null.
  └── fileSizeBytes: Long    # Actual file size after save
```

## Platform Implementations

### Android: `AndroidFileStorage`
- Wraps existing `MediaStoreRepositoryImpl` + `AndroidFileAccessManager`
- `saveToDownloads()` uses `MediaStore.Downloads` with `ContentResolver`
- `isFileAccessible()` checks if content:// URI is resolvable
- `getShareableUri()` uses `FileProvider.getUriForFile()`
- Requires `Context` and `ContentResolver`

### iOS: `IosFileStorage`
- Base directory: `Documents/SocialVideoDownloader/`
- `saveToDownloads()` moves file via `FileManager.default.moveItem()`
- `isFileAccessible()` uses `FileManager.default.fileExists(atPath:)`
- `getShareableUri()` returns file:// URL (iOS handles sharing via file URLs)
- Sets `UIFileSharingEnabled` in Info.plist so files appear in Files app
