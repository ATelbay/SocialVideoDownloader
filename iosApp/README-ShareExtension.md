# Share Extension — Xcode Setup Instructions

The source files for the Share Sheet integration have been created. Because `project.pbxproj`
editing is complex and error-prone without running Xcode, the Xcode project wiring must be
completed manually. Follow the steps below once in Xcode.

---

## 1. Add the ShareExtension target

1. Open `iosApp.xcodeproj` in Xcode.
2. File > New > Target…
3. Choose **Share Extension** (under iOS > Application Extensions).
4. Product Name: `ShareExtension`
5. Bundle Identifier: `com.socialvideodownloader.ShareExtension`
6. Language: **Swift**
7. Click **Finish** — Xcode will scaffold a default `ShareViewController.swift`.
8. **Replace** the generated `ShareViewController.swift` with the file already in
   `iosApp/ShareExtension/ShareViewController.swift`.
9. **Replace** the generated `Info.plist` with `iosApp/ShareExtension/Info.plist`.

---

## 2. Assign entitlements

### Main app target (iosApp)
1. Select the **iosApp** target > Signing & Capabilities.
2. Click **+ Capability** > App Groups.
3. Add group: `group.com.socialvideodownloader.shared`
4. Set the entitlements file to `iosApp/iosApp.entitlements`
   (Build Settings > Code Signing Entitlements).

### ShareExtension target
1. Select the **ShareExtension** target > Signing & Capabilities.
2. Click **+ Capability** > App Groups.
3. Add group: `group.com.socialvideodownloader.shared`
4. Set the entitlements file to `iosApp/ShareExtension/ShareExtension.entitlements`
   (Build Settings > Code Signing Entitlements).

---

## 3. Verify URL scheme

Open the **iosApp** target > Info tab and confirm there is a URL Type entry:
- Identifier: `com.socialvideodownloader`
- URL Schemes: `socialvideodownloader`

This is already present in `iosApp/Info.plist`, so it should appear automatically.

---

## 4. Wire `SharedURLState` into `DownloadView`

`App.swift` injects `SharedURLState` as an `@EnvironmentObject`. To make `DownloadView`
auto-populate the URL field when a share arrives, add the following inside `DownloadView`:

```swift
@EnvironmentObject var sharedURLState: SharedURLState

// Inside body or .onAppear / .onChange:
.onChange(of: sharedURLState.pendingURL) { _, newURL in
    guard let newURL else { return }
    // Set the URL in your ViewModel / TextField binding, e.g.:
    viewModel.setUrl(newURL)
    sharedURLState.pendingURL = nil   // consume
}
```

---

## 5. How it works end-to-end

1. User taps **Share** in Safari and selects **Share to SVD**.
2. `ShareViewController` reads the URL from the share sheet, writes it to
   `UserDefaults(suiteName: "group.com.socialvideodownloader.shared")` under key `SharedURL`,
   then calls `extensionContext?.completeRequest(returningItems: nil)`.
3. When the main app becomes active (`scenePhase == .active`), `SharedURLState.consumeSharedURL()`
   reads and clears the stored URL, publishing it via `@Published var pendingURL`.
4. `DownloadView` observes `pendingURL` and populates the URL input field automatically.
