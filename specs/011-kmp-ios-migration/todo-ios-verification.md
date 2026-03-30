# TODO: iOS Verification (requires Xcode)

## Must fix before iOS build works

1. **Add KMP framework export** — the convention plugins (`KmpLibraryConventionPlugin`) only configure `compileKotlin*` tasks, not `binaries.framework`. Need to add `binaries { framework { baseName = "shared" } }` (or per-module frameworks) so Xcode can link against them. Without this, `FRAMEWORK_SEARCH_PATHS` in pbxproj points to nothing.

2. **Fix pbxproj missing files** — 6 files added in Phases 7-10 are not in `iosApp/iosApp.xcodeproj/project.pbxproj`:
   - `HistoryItemRow.swift`
   - `HistoryDeleteDialog.swift`
   - `CloudBackupView.swift`
   - `UpgradeView.swift`
   - `LibraryItemRow.swift`
   - `iosApp.entitlements`

## Must verify with Xcode / CI

3. **Swift compilation** — 20 Swift files have never been compiled. Check for syntax errors, wrong imports, SKIE naming mismatches.
4. **iOS app runs on simulator** — end-to-end: paste URL, extract, select format, download, verify file in Documents.
5. **Background downloads** — start download, terminate app, relaunch, verify file saved.
6. **Share extension** — share URL from Safari, verify app opens with URL pre-filled.
7. **Side-by-side design comparison** — iOS vs Android screenshots, verify visual consistency.

## Nice to have

8. **Set up GitHub Actions CI** for iOS build (see prompt in conversation).
