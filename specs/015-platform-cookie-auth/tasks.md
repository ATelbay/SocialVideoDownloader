# Tasks: Platform Authentication for Restricted Content

**Input**: Design documents from `/specs/015-platform-cookie-auth/`
**Prerequisites**: plan.md (required), spec.md (required), research.md, data-model.md, contracts/

**Tests**: Included per constitution Principle VI — unit tests for use cases, utility functions, and ViewModel state transitions.

**Organization**: Tasks grouped by user story for independent implementation and testing.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3)
- Include exact file paths in descriptions

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Add dependency, create package structure, wire DI

- [ ] T001 Add `androidx.security:security-crypto:1.1.0-alpha06` to androidMain dependencies in `shared/network/build.gradle.kts`
- [ ] T002 [P] Create `auth` subpackage directories under `shared/network/src/commonMain/kotlin/com/socialvideodownloader/shared/network/auth/`, `shared/network/src/androidMain/kotlin/com/socialvideodownloader/shared/network/auth/`, and `shared/network/src/iosMain/kotlin/com/socialvideodownloader/shared/network/auth/`

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Core data types, storage, and utilities that ALL user stories depend on

**CRITICAL**: No user story work can begin until this phase is complete

- [ ] T003 [P] Create `SupportedPlatform` enum with all 5 platforms (displayName, loginUrl, cookieDomains, hostMatches, successCookieName), plus `detectPlatform(url)` and `detectPlatformFromError(errorMessage)` utility functions in `shared/network/src/commonMain/kotlin/com/socialvideodownloader/shared/network/auth/SupportedPlatform.kt`
- [ ] T004 [P] Create `NetscapeCookieParser` utility with `parseToNameValuePairs(netscapeCookieString): List<Pair<String, String>>` for HTTP Cookie header injection, and `formatToNetscape(cookies: List<CookieEntry>): String` for WebView cookie export, in `shared/network/src/commonMain/kotlin/com/socialvideodownloader/shared/network/auth/NetscapeCookieParser.kt`
- [ ] T005 [P] Create `expect class SecureCookieStore` with `getCookies`, `setCookies`, `clearCookies`, `isConnected`, `connectedPlatforms` in `shared/network/src/commonMain/kotlin/com/socialvideodownloader/shared/network/auth/SecureCookieStore.kt`
- [ ] T006 Create `actual class SecureCookieStore` for Android using `EncryptedSharedPreferences` (MasterKey AES256, file "svd_platform_cookies"), keys `cookies_instagram` etc., in `shared/network/src/androidMain/kotlin/com/socialvideodownloader/shared/network/auth/SecureCookieStore.android.kt` — constructor takes `Context` from Koin `androidContext()`
- [ ] T007 Create `actual class SecureCookieStore` for iOS using Security framework Keychain (`kSecClassGenericPassword`, service `com.socialvideodownloader.cookies`, account `cookies_{platform}`), in `shared/network/src/iosMain/kotlin/com/socialvideodownloader/shared/network/auth/SecureCookieStore.ios.kt`
- [ ] T008 Add `AUTH_REQUIRED` to `DownloadErrorType` enum in `shared/data/src/commonMain/kotlin/com/socialvideodownloader/shared/data/platform/PlatformDownloadManager.kt`
- [ ] T009 Register `SecureCookieStore` as a Koin singleton in `shared/network/src/commonMain/kotlin/com/socialvideodownloader/shared/network/di/NetworkModule.kt` — on Android, use `androidContext()` for constructor; on iOS, no-arg constructor
- [ ] T010 Register `SecureCookieStore` as a Hilt `@Provides @Singleton` in `core/data/src/main/kotlin/com/socialvideodownloader/core/data/di/NetworkModule.kt` — bridge from Koin via `KoinPlatform.getKoin().get<SecureCookieStore>()`
- [ ] T011 [P] Define auth-related UI strings as inline constants in a `DownloadAuthStrings` object in `shared/feature-download/src/commonMain/kotlin/com/socialvideodownloader/shared/feature/download/ui/DownloadAuthStrings.kt`. Include: `authRequiredMessage(platformName: String)` = "This content requires authentication. Connect your $platformName account to download.", `connectLabel(platformName: String)` = "Connect $platformName", `reconnectLabel(platformName: String)` = "Reconnect $platformName", `disconnectLabel` = "Disconnect". Follows existing pattern of hardcoded strings in shared CMP code (see `DownloadErrorContent.kt`, `HistoryStrings`). Do NOT create `strings.xml` or `Localizable.strings` — shared Compose code does not use platform string resources.

**Checkpoint**: Foundation ready — SupportedPlatform enum, SecureCookieStore, NetscapeCookieParser, AUTH_REQUIRED all available. User story implementation can begin.

---

## Phase 3: User Story 1 — Authenticate to Download Restricted Content (Priority: P1) MVP

**Goal**: When extraction fails with an auth error, show "Connect [Platform]" CTA. User logs in via WebView, cookies are stored, extraction auto-retries with cookies injected into WS proxy requests.

**Independent Test**: Attempt to download age-restricted Instagram content → see auth error CTA → log in → download succeeds on auto-retry.

### Tests for User Story 1

- [ ] T012 [P] [US1] Add unit tests for `detectPlatform()` (all 5 platforms + unknown URLs) and `detectPlatformFromError()` (platform prefixes + auth keywords) in `shared/network/src/commonTest/kotlin/com/socialvideodownloader/shared/network/auth/SupportedPlatformTest.kt`
- [ ] T013 [P] [US1] Add unit tests for `NetscapeCookieParser.parseToNameValuePairs()` (valid cookies, comments, blank lines, malformed lines) and `formatToNetscape()` in `shared/network/src/commonTest/kotlin/com/socialvideodownloader/shared/network/auth/NetscapeCookieParserTest.kt`
- [ ] T014 [P] [US1] Add unit tests for `SharedDownloadViewModel.mapErrorToType()` returning `AUTH_REQUIRED` when error contains auth keywords AND URL matches a supported platform, and falling through to existing types when URL doesn't match. Test `friendlyErrorMessage()` returns platform-specific auth message. In `shared/feature-download/src/commonTest/kotlin/com/socialvideodownloader/shared/feature/download/SharedDownloadViewModelAuthTest.kt`

### Implementation for User Story 1

- [ ] T015 [US1] Add `ConnectPlatformClicked(platform: SupportedPlatform)` and `PlatformLoginResult(platform: SupportedPlatform, success: Boolean)` to `DownloadIntent` sealed interface in `shared/feature-download/src/commonMain/kotlin/com/socialvideodownloader/shared/feature/download/DownloadIntent.kt`
- [ ] T016 [US1] Extend `PlatformDelegate` interface with `fun showPlatformLogin(platform: SupportedPlatform)` in `shared/feature-download/src/commonMain/kotlin/com/socialvideodownloader/shared/feature/download/SharedDownloadViewModel.kt`
- [ ] T017 [US1] Add `SecureCookieStore` as constructor dependency to `SharedDownloadViewModel`. Update `mapErrorToType()`: when error message contains auth keywords ("sign in", "login required", "must be logged in", "inappropriate", "age-restricted", "NSFW") AND `detectPlatform(url)` returns non-null → return `AUTH_REQUIRED`. Update `friendlyErrorMessage()`: for auth errors, return "This content requires authentication. Connect your [Platform] account to download." Handle `ConnectPlatformClicked` intent: call `platformDelegate?.showPlatformLogin(platform)`. Handle `PlatformLoginResult` intent: if success, auto-retry extraction. In `shared/feature-download/src/commonMain/kotlin/com/socialvideodownloader/shared/feature/download/SharedDownloadViewModel.kt`
- [ ] T018 [US1] Update `DownloadUiState.Error` to include `platformForAuth: SupportedPlatform?` field (null for non-auth errors). Set this when `errorType == AUTH_REQUIRED` using `detectPlatform(url)`. In `shared/feature-download/src/commonMain/kotlin/com/socialvideodownloader/shared/feature/download/DownloadUiState.kt`
- [ ] T019 [US1] Update `DownloadErrorContent` composable: when `errorType == AUTH_REQUIRED` and `platformForAuth != null`, show a "Connect [Platform]" button (using `GradientButton`) that emits `ConnectPlatformClicked(platform)`. Keep existing "Retry" and "New Download" buttons. In `shared/feature-download/src/commonMain/kotlin/com/socialvideodownloader/shared/feature/download/ui/DownloadErrorContent.kt`
- [ ] T020 [US1] Create `PlatformLoginScreen` composable for Android: uses `AndroidView` to embed `WebView`, loads `platform.loginUrl`, enables autofill (`importantForAutofill = IMPORTANT_FOR_AUTOFILL_YES`), monitors `onPageFinished` for `successCookieName` in `CookieManager.getCookie()`, extracts all domain cookies and formats as Netscape string via `NetscapeCookieParser`, stores via `SecureCookieStore.setCookies()`, clears WebView cookies via `CookieManager.removeAllCookies()`, calls `onResult(true)`. Back/cancel calls `onResult(false)`. In `feature/download/src/main/kotlin/com/socialvideodownloader/feature/download/ui/PlatformLoginScreen.kt`
- [ ] T021 [US1] Create iOS `PlatformLoginScreen` composable: uses CMP `UIKitView` to embed `WKWebView`, loads `platform.loginUrl`, monitors `WKNavigationDelegate.didFinishNavigation` for `successCookieName` via `WKWebsiteDataStore.httpCookieStore.getAllCookies()`, extracts domain cookies, formats as Netscape string, stores via `SecureCookieStore.setCookies()`, clears WKWebView cookies via `WKWebsiteDataStore.removeData(ofTypes:)`, calls `onResult(true)`. Cancel calls `onResult(false)`. WKWebView supports system AutoFill and Keychain credential suggestions by default (javaScriptEnabled is true by default), satisfying FR-005 on iOS with no additional configuration. In `shared/feature-download/src/iosMain/kotlin/com/socialvideodownloader/shared/feature/download/platform/PlatformLoginScreen.ios.kt`
- [ ] T022 [US1] Add `PlatformLoginRoute` (Serializable data class with `platformName: String`) to Android navigation. Register `composable<PlatformLoginRoute>` in `NavGraphBuilder.downloadScreen()` that resolves `SupportedPlatform` from name and renders `PlatformLoginScreen`. In `feature/download/src/main/kotlin/com/socialvideodownloader/feature/download/navigation/DownloadNavigation.kt`
- [ ] T023 [US1] Implement `PlatformDelegate.showPlatformLogin()` in Android `DownloadViewModel`: accept a navigation callback (lambda or NavController) and navigate to `PlatformLoginRoute(platform.name)`. Wire the login result back to `shared.onIntent(PlatformLoginResult(...))`. In `feature/download/src/main/kotlin/com/socialvideodownloader/feature/download/ui/DownloadViewModel.kt`
- [ ] T023B [US1] Wire iOS login flow via shared composable fallback: In `SharedDownloadViewModel`, when handling `ConnectPlatformClicked` and `platformDelegate` is null, emit a `DownloadEvent.ShowPlatformLogin(platform: SupportedPlatform)` event. In `DownloadScreen.kt` (shared), collect this event into a local `showLoginForPlatform` state. When non-null, show a fullscreen overlay calling an `expect`/`actual` `PlatformLoginScreen(platform, onResult)` composable — iOS actual is T021's UIKitView+WKWebView, Android actual is `error("Handled by PlatformDelegate")`. On result, clear state and forward `PlatformLoginResult` to the VM. Files: `SharedDownloadViewModel.kt`, `DownloadEvent.kt` (+ShowPlatformLogin), `DownloadScreen.kt` (+overlay), `shared/feature-download/src/commonMain/kotlin/com/socialvideodownloader/shared/feature/download/platform/PlatformLoginScreen.kt` (expect), `shared/feature-download/src/androidMain/kotlin/com/socialvideodownloader/shared/feature/download/platform/PlatformLoginScreen.kt` (actual no-op).
- [ ] T024 [US1] Add `SecureCookieStore` as constructor dependency to `WebSocketExtractorApi`. In `extractViaProxy()`, inside the `http_request` handler, BEFORE the `rawClient.request()` call: parse request URL host, find matching `SupportedPlatform` via `hostMatches`, if `SecureCookieStore.getCookies(platform)` is non-null, parse Netscape cookies to name=value pairs via `NetscapeCookieParser`, merge into existing Cookie header (append, don't overwrite). Update Koin module `single { WebSocketExtractorApi(get(), get(), get()) }` to inject SecureCookieStore. In `shared/network/src/commonMain/kotlin/com/socialvideodownloader/shared/network/WebSocketExtractorApi.kt` and `shared/network/src/commonMain/kotlin/com/socialvideodownloader/shared/network/di/NetworkModule.kt`
- [ ] T025 [US1] Send cookies in WS initial `extract_request` message: in `extractViaProxy()`, detect platform from URL, if cookies exist, base64-encode the Netscape string and add `"cookies"` field to the JSON message. In `shared/network/src/commonMain/kotlin/com/socialvideodownloader/shared/network/WebSocketExtractorApi.kt`
- [ ] T026 [US1] Server: add optional `cookiefile: str | None = None` parameter to `get_ydl_opts()`. If non-None, add `"cookiefile": cookiefile` to the returned opts dict. In `server/app/ytdlp_opts.py`
- [ ] T027 [US1] Server: in `ws_extract()`, read optional `cookies` field from initial `extract_request` message. If present, base64-decode, write to tempfile, pass path as `cookiefile` to `get_ydl_opts()`. Delete tempfile in `finally` block after extraction. In `server/app/routes/proxy_ws.py`

**Checkpoint**: User Story 1 complete on both platforms. Auth error → "Connect [Platform]" → WebView login → cookies stored → auto-retry with cookie injection → download succeeds. On Android, PlatformDelegate navigates via Android navigation (T022/T023). On iOS, the shared DownloadScreen shows a fullscreen overlay via event (T023B). US4 (Cancel) is inherently covered: pressing back/cancel in the WebView calls `onResult(false)`, no cookies are stored.

---

## Phase 4: User Story 2 — Manage Connected Platform Accounts (Priority: P2)

**Goal**: Show connected platform chips on idle download screen. Tap chip → bottom sheet with "Disconnect" button.

**Independent Test**: Connect two platforms, verify chips appear. Tap chip → disconnect → chip disappears, cookies cleared.

### Implementation for User Story 2

- [ ] T028 [US2] Add `connectedPlatforms: List<SupportedPlatform> = emptyList()` to `DownloadUiState.Idle` in `shared/feature-download/src/commonMain/kotlin/com/socialvideodownloader/shared/feature/download/DownloadUiState.kt`
- [ ] T029 [US2] Add `DisconnectPlatformClicked(platform: SupportedPlatform)` intent to `DownloadIntent` sealed interface in `shared/feature-download/src/commonMain/kotlin/com/socialvideodownloader/shared/feature/download/DownloadIntent.kt`
- [ ] T030 [US2] Update `SharedDownloadViewModel`: when emitting `Idle` state, query `SecureCookieStore.connectedPlatforms()` and include in state. Handle `DisconnectPlatformClicked`: call `SecureCookieStore.clearCookies(platform)`, re-emit `Idle` with updated connected platforms. In `shared/feature-download/src/commonMain/kotlin/com/socialvideodownloader/shared/feature/download/SharedDownloadViewModel.kt`
- [ ] T031 [US2] Create `PlatformConnectionChips` composable: takes `connectedPlatforms: List<SupportedPlatform>` and `onDisconnect: (SupportedPlatform) -> Unit`. Renders a `FlowRow` of `AssistChip` for each platform (label = platform.displayName). Tapping a chip opens a `ModalBottomSheet` showing platform name and a "Disconnect" button. If `connectedPlatforms` is empty, renders nothing. In `shared/feature-download/src/commonMain/kotlin/com/socialvideodownloader/shared/feature/download/ui/PlatformConnectionChips.kt`
- [ ] T032 [US2] Wire `PlatformConnectionChips` into the download screen's idle state content (wherever `Idle` state is rendered in the shared download screen composable). Pass `state.connectedPlatforms` and `onDisconnect = { viewModel.onIntent(DisconnectPlatformClicked(it)) }`. In the shared download screen composable file (likely `SharedDownloadScreen.kt` or equivalent in `shared/feature-download/src/commonMain/.../ui/`)

**Checkpoint**: Connected platforms visible as chips, disconnect works. No regression to US1 auth flow.

---

## Phase 5: User Story 3 — Handle Expired or Stale Cookies (Priority: P2)

**Goal**: When auth error occurs but cookies already exist → cookies are stale. Clear them, show "Reconnect [Platform]" instead of "Connect [Platform]".

**Independent Test**: Store invalid cookies for a platform, attempt restricted download, verify "Reconnect" CTA appears after stale cookies cleared.

### Implementation for User Story 3

- [ ] T033 [US3] Add `isReconnect: Boolean = false` to `DownloadUiState.Error` in `shared/feature-download/src/commonMain/kotlin/com/socialvideodownloader/shared/feature/download/DownloadUiState.kt`
- [ ] T034 [US3] Update `SharedDownloadViewModel` auth error handling: when `mapErrorToType` returns `AUTH_REQUIRED` AND `SecureCookieStore.isConnected(platform)` is true → clear stale cookies via `clearCookies(platform)`, set `isReconnect = true` on the `Error` state. In `shared/feature-download/src/commonMain/kotlin/com/socialvideodownloader/shared/feature/download/SharedDownloadViewModel.kt`
- [ ] T035 [US3] Update `DownloadErrorContent`: when `isReconnect == true`, change CTA button text from "Connect [Platform]" to "Reconnect [Platform]". Behavior is identical (emits `ConnectPlatformClicked`). In `shared/feature-download/src/commonMain/kotlin/com/socialvideodownloader/shared/feature/download/ui/DownloadErrorContent.kt`

**Checkpoint**: Stale cookie detection works. "Reconnect" label shown when appropriate. Reconnecting stores fresh cookies and retries.

---

## Phase 6: User Story 5 — REST Fallback with Cookie Forwarding (Priority: P3)

**Goal**: When WS proxy is unavailable and REST fallback is used, forward cookies to the server via `X-Platform-Cookies` header.

**Independent Test**: Disable WS proxy, trigger REST fallback for authenticated content. Server receives cookies and uses them.

### Implementation for User Story 5

- [ ] T036 [US5] Add `SecureCookieStore` as constructor dependency to `ServerVideoExtractorApi`. In `extractInfo()`, detect platform via `detectPlatform(url)`, if cookies exist, base64-encode the Netscape cookie string and add `header("X-Platform-Cookies", encodedCookies)` to the request. Update Koin module `single { ServerVideoExtractorApi(get(), get(), get()) }`. In `shared/network/src/commonMain/kotlin/com/socialvideodownloader/shared/network/ServerVideoExtractorApi.kt` and `shared/network/src/commonMain/kotlin/com/socialvideodownloader/shared/network/di/NetworkModule.kt`
- [ ] T037 [US5] Update Hilt `NetworkModule` to pass `SecureCookieStore` to `ServerVideoExtractorApi` provider. In `core/data/src/main/kotlin/com/socialvideodownloader/core/data/di/NetworkModule.kt`
- [ ] T038 [US5] Server: in `extract_video_info()`, read `X-Platform-Cookies` header. If present, base64-decode, write to tempfile, pass as `cookiefile` to `get_ydl_opts()`. Delete tempfile in `finally` block. In `server/app/routes/extract.py`

**Checkpoint**: REST fallback path correctly forwards cookies. All extraction paths (local → WS proxy → REST) now support authentication.

---

## Phase 7: Polish & Cross-Cutting Concerns

**Purpose**: Tests, cleanup, validation

- [ ] T039 [P] Add unit test for cookie injection in `WebSocketExtractorApi`: mock `SecureCookieStore` returning cookies for Instagram, verify Cookie header is appended to proxied requests matching `instagram.com`, verify cookies NOT appended for `youtube.com` requests. In `shared/network/src/commonTest/kotlin/com/socialvideodownloader/shared/network/WebSocketExtractorApiCookieTest.kt`
- [ ] T040 [P] Add unit test for `ServerVideoExtractorApi` cookie forwarding: mock `SecureCookieStore`, verify `X-Platform-Cookies` header sent for matched platforms, not sent for unmatched/no-cookies. In `shared/network/src/commonTest/kotlin/com/socialvideodownloader/shared/network/ServerVideoExtractorApiCookieTest.kt`
- [ ] T041 [P] Add unit test for stale cookie detection in `SharedDownloadViewModel`: auth error + `isConnected` true → cookies cleared + `isReconnect` true. Auth error + `isConnected` false → normal "Connect" flow. In `shared/feature-download/src/commonTest/kotlin/com/socialvideodownloader/shared/feature/download/SharedDownloadViewModelStaleTest.kt`
- [ ] T041B [P] Add unit tests for `SecureCookieStore` contract in commonTest: create `FakeSecureCookieStore`, verify `setCookies`/`getCookies` round-trip, `clearCookies` removes correct entry only, `isConnected` reflects state, `connectedPlatforms` returns correct list. In `shared/network/src/commonTest/kotlin/com/socialvideodownloader/shared/network/auth/SecureCookieStoreContractTest.kt`
- [ ] T041C [P] Add Android instrumented test (Robolectric) for `SecureCookieStore.android.kt`: verify EncryptedSharedPreferences round-trip for all 5 platforms, verify `clearCookies` removes correct key only. In `shared/network/src/androidUnitTest/kotlin/com/socialvideodownloader/shared/network/auth/SecureCookieStoreAndroidTest.kt`
- [ ] T042 Run `./gradlew ktlintCheck -x compileKotlinIosArm64 -x compileKotlinIosSimulatorArm64` and fix any violations
- [ ] T043 Run `./gradlew test` and verify all existing + new tests pass
- [ ] T044 Validate quickstart.md test scenarios manually: auth error detection, cookie injection, REST fallback, cookie isolation, stale cookies

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies — start immediately
- **Foundational (Phase 2)**: Depends on Phase 1 — BLOCKS all user stories
- **US1 (Phase 3)**: Depends on Phase 2 — MVP, implement first
- **US2 (Phase 4)**: Depends on Phase 2 + US1 T017 (SecureCookieStore in ViewModel)
- **US3 (Phase 5)**: Depends on US1 (builds on auth error handling)
- **US5 (Phase 6)**: Depends on Phase 2 only — can run parallel to US1
- **Polish (Phase 7)**: Depends on all user stories complete

### User Story Dependencies

- **US1 (P1)**: Foundational only — no other story dependencies. **This is the MVP.**
- **US2 (P2)**: Requires SecureCookieStore wired into ViewModel (from US1 T017), but chips UI is independently testable once ViewModel exposes connected platforms
- **US3 (P2)**: Requires auth error handling from US1 — extends it with stale detection
- **US4 (P3)**: **Covered by US1** — cancel behavior is inherent in PlatformLoginScreen (back/cancel = `onResult(false)`, no cookies stored)
- **US5 (P3)**: Foundational only — REST path is independent of WS proxy path

### Within Each User Story

- Tests before or parallel with implementation (marked [P] where possible)
- T023B depends on T016 (PlatformDelegate interface), T021 (iOS screen), T015 (intents); can run parallel with T022/T023
- Data model / utility changes before ViewModel changes
- ViewModel changes before UI changes
- Platform-specific (Android/iOS) screens can be parallel with each other

### Parallel Opportunities

- T003, T004, T005 (enum, parser, expect class) — all different files, no dependencies
- T006, T007 (Android/iOS actuals) — different source sets, parallel
- T012, T013, T014 (US1 tests) — all different test files, parallel
- T020, T021 (Android/iOS login screens) — different modules, parallel
- T023B (iOS wiring) depends on T021 completing; can run parallel with T022/T023
- T039, T040, T041 (Polish tests) — all different test files, parallel
- US5 (Phase 6) can run entirely in parallel with US2/US3

---

## Parallel Example: User Story 1

```text
# Phase 2 parallelism:
Parallel: T003 (SupportedPlatform) | T004 (NetscapeCookieParser) | T005 (SecureCookieStore expect)
Then: T006 (Android actual) | T007 (iOS actual) — parallel
Then: T008 (AUTH_REQUIRED) | T009 (Koin) | T010 (Hilt) | T011 (strings) — parallel

# US1 tests parallelism:
Parallel: T012 (platform detection tests) | T013 (parser tests) | T014 (ViewModel auth tests)

# US1 platform screens parallelism:
Parallel: T020 (Android WebView) | T021 (iOS WKWebView)
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete Phase 1: Setup (T001-T002)
2. Complete Phase 2: Foundational (T003-T011)
3. Complete Phase 3: User Story 1 (T012-T027)
4. **STOP and VALIDATE**: Test auth flow end-to-end on Android and iOS
5. Ship MVP if ready — users can authenticate and download restricted content

### Incremental Delivery

1. Setup + Foundational → Foundation ready
2. **US1** → Auth + Download works → MVP!
3. **US2** → Connection management UI → users see connected status
4. **US3** → Stale cookie handling → graceful expiry recovery
5. **US5** → REST fallback → all paths authenticated
6. Polish → Tests, lint, validation

### Parallel Team Strategy

With multiple developers after Phase 2:
- Developer A: US1 (core auth flow — Android)
- Developer B: US1 (iOS PlatformLoginScreen — T021) + US5 (REST fallback — T036-T038)
- Developer C: US2 (connection chips — T028-T032) once T017 is merged

---

## Notes

- US4 (Cancel Login) has no dedicated tasks — cancel behavior is inherent in PlatformLoginScreen design (T020/T021). Pressing back fires `onResult(false)` with no side effects.
- Server tasks (T026, T027, T038) are Python changes to the existing FastAPI server in `server/app/`.
- iOS PlatformLoginScreen (T021) uses CMP `UIKitView` per plan decision D2 — not SwiftUI.
- All Koin module changes must be mirrored in Hilt `NetworkModule` for Android DI parity.
- Commit after each task per git workflow convention.
