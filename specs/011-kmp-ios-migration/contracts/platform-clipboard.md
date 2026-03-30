# Contract: PlatformClipboard

**Location**: `shared/data/src/commonMain/kotlin/.../platform/PlatformClipboard.kt`

## Interface

```
PlatformClipboard
  └── copyToClipboard(text: String): Unit
      Copy a text string to the system clipboard.
```

## Platform Implementations

### Android: `AndroidClipboard`
- Uses `Context.getSystemService(Context.CLIPBOARD_SERVICE)` to get `ClipboardManager`
- Creates `ClipData.newPlainText("url", text)` and sets as primary clip
- Requires `Context` injection

### iOS: `IosClipboard`
- Uses `platform.UIKit.UIPasteboard.generalPasteboard.string = text`
- No permissions required
