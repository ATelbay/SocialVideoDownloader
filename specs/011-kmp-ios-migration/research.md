# Research: KMP iOS Migration

**Feature**: `011-kmp-ios-migration` | **Date**: 2026-03-30

## R-001: Room KMP Readiness

**Decision**: Use Room KMP 2.8.4 (current version, already in the project)

**Rationale**: Room 2.8.4 supports KMP with entities, DAOs, and migrations in `commonMain`. Same version already used — no version bump needed. Room 3.0 is in alpha (March 2026) with breaking API changes; staying on 2.8.x is the conservative choice.

**Key findings**:
- Entities and DAOs go in `commonMain`. Database builder is platform-specific via `@ConstructedBy` + `expect object` implementing `RoomDatabaseConstructor`.
- The Room compiler auto-generates the `actual` implementations — no manual `actual` needed.
- iOS database path uses `NSDocumentDirectory` (not `NSHomeDirectory` as initially assumed).
- **Breaking changes from Android Room**: All DAO methods must be `suspend` (no blocking). No `LiveData` or RxJava return types — `Flow` only. `withTransaction {}` replaced by `useWriterConnection { transactor -> transactor.immediateTransaction {} }`. Migrations use `SQLiteConnection` instead of `SupportSQLiteDatabase`.
- **Not available in KMP**: `createFromAsset()`, `createFromFile()`, `setAutoCloseTimeout`, `enableMultiInstanceInvalidation`.
- Current DAO methods already use `suspend` and `Flow` — migration is straightforward.

**Alternatives considered**:
- SQLDelight: Would require rewriting all entities, DAOs, and queries from scratch. Room KMP keeps existing code largely intact.
- Room 3.0 alpha: Too early for production. Plan upgrade path after 3.0 stabilizes.

## R-002: Ktor 3.x for Multiplatform Networking

**Decision**: Ktor 3.4.1 with OkHttp engine (Android) and Darwin engine (iOS)

**Rationale**: Ktor is the standard KMP HTTP client. OkHttp engine on Android preserves compatibility with existing OkHttp interceptors and configuration patterns. Darwin engine wraps NSURLSession natively.

**Key findings**:
- `ktor-client-core` in `commonMain`, `ktor-client-okhttp` in `androidMain`, `ktor-client-darwin` in `iosMain`.
- `HttpClient` created via `expect fun httpClient(): HttpClient` with platform actuals.
- Supports content negotiation, serialization (kotlinx.serialization), and streaming responses — all needed for the server API.
- CIO engine exists as a pure-Kotlin option but lacks platform-specific features.

**Alternatives considered**:
- Keep OkHttp with `expect/actual` wrapper: Would require duplicating the HTTP client on iOS (no OkHttp there). Ktor provides a clean unified API.
- URLSession directly on iOS + OkHttp on Android: More code duplication, no shared HTTP logic.

## R-003: SKIE for Swift Interop

**Decision**: SKIE 0.10.10 (Touchlab)

**Rationale**: SKIE transforms Kotlin/Native's lossy ObjC interop into idiomatic Swift. Without it, StateFlow becomes an opaque `Kotlinx_coroutines_coreStateFlow` in Swift, and sealed classes lose exhaustive switching.

**Key findings**:
- `StateFlow<T>` → `AsyncSequence` (can use `for await` in Swift)
- `sealed interface` → exhaustive Swift enum (no default case needed)
- `suspend fun` → `async` Swift function
- Gradle plugin only — no code changes in shared module.
- Supports Kotlin 2.2.x (fixed in 0.10.5+).
- **Gotcha**: Custom exceptions inside a `Flow` crash at runtime in Swift. Must catch and convert to sealed result types in Kotlin before exposing to iOS.
- **Gotcha**: Type casting (`as!`, `as?`, `is`) on SKIE-wrapped types is unsafe.

**Alternatives considered**:
- KMP-NativeCoroutines: Older approach, requires more boilerplate. SKIE is the successor and recommended by Touchlab.
- Manual Kotlin→Swift wrappers: Excessive boilerplate for every Flow and sealed class.

## R-004: Koin 4.x for Shared DI

**Decision**: Koin 4.2.0 (BOM) for shared KMP modules. Hilt stays for Android-only modules.

**Rationale**: Koin uses no code generation, making it KMP-compatible in `commonMain`. Hilt is Android-only (uses KSP/kapt). The bridge pattern lets Hilt consume Koin-managed objects.

**Key findings**:
- Shared modules define Koin `module {}` blocks in `commonMain`.
- Android: `startKoin {}` in `Application.onCreate()`, before Hilt initialization.
- Hilt `@Provides` methods bridge to Koin via `KoinPlatform.getKoin().get<T>()`.
- iOS: `KoinKt.doInitKoin()` from Swift `AppDelegate`.
- Koin Annotations 4.x (optional) provide `@Single`, `@Factory` decorators — closer to Hilt ergonomics.

**Alternatives considered**:
- Full Koin migration (drop Hilt entirely): Larger blast radius, not needed for MVP. Can be done later.
- kotlin-inject: KMP-compatible but requires code generation per platform. Less community adoption than Koin.

## R-005: Multiplatform Settings

**Decision**: multiplatform-settings 1.3.0 (russhwolf)

**Rationale**: Lightweight key-value abstraction over SharedPreferences (Android) and NSUserDefaults (iOS). Simpler than multiplatform DataStore for the limited preferences needed (cloud backup toggle, server URL).

**Key findings**:
- `Settings` interface in `commonMain` with `putString`/`getString`/`remove`/`clear`.
- `FlowSettings` extension for `Flow`-based observation (requires `multiplatform-settings-coroutines`).
- `multiplatform-settings-datastore` can wrap existing DataStore as a `Settings` — migration path for current `CloudBackupPreferences`.
- Android backing: `SharedPreferences`. iOS backing: `NSUserDefaults`.

**Alternatives considered**:
- DataStore Multiplatform 1.1.0: Heavier, more API surface than needed for simple preferences.
- Manual `expect/actual` wrapper: Unnecessary when a well-maintained library exists.

## R-006: Room KMP Migration Compatibility

**Decision**: Existing Room migrations (v1→v5) will need refactoring from `SupportSQLiteDatabase` to `SQLiteConnection` API.

**Rationale**: Room KMP uses `androidx.sqlite` driver API, not the legacy `SupportSQLite*` classes. All 4 existing migrations must be rewritten to use `SQLiteConnection.execSQL()` instead of `SupportSQLiteDatabase.execSQL()`. The SQL statements themselves remain identical — only the wrapper class changes.

**Key findings**:
- Current migrations are raw SQL (`ALTER TABLE`, `CREATE TABLE`, `CREATE INDEX`). The SQL is portable.
- Only the migration class signature changes: `Migration.migrate(db: SupportSQLiteDatabase)` → `Migration.migrate(connection: SQLiteConnection)`.
- Existing Android databases will be opened by Room KMP with the same schema — no data loss.

## R-007: iOS Authentication

**Decision**: Support both Google Sign-In for iOS SDK and Sign in with Apple.

**Rationale**: App Store Review Guidelines require Sign in with Apple when third-party sign-in (Google) is offered. Current constitution allows Google Sign-In only — requires amendment.

**Key findings**:
- Google Sign-In iOS SDK provides the same identity as Android (same Google account → same Firestore user).
- Sign in with Apple provides an Apple-specific identity — would need Firestore account linking if the same user signs in with Google on Android and Apple on iOS.
- Firebase Auth supports both providers and handles account linking.

## R-008: URLSession Background Downloads

**Decision**: Use `URLSessionDownloadTask` with background configuration for iOS downloads.

**Rationale**: Only way to continue downloads when the iOS app is suspended or terminated. URLSession background tasks are managed by the OS and resumed automatically.

**Key findings**:
- `URLSessionConfiguration.background(withIdentifier:)` creates a background session.
- Downloads continue even after app termination. The system re-launches the app when complete.
- Progress callbacks only fire when the app is in the foreground.
- Only supports HTTP/HTTPS — fits the server's direct download URLs.
- Completed downloads land in a temporary location; must be moved to Documents directory before the delegate callback returns.
- Download progress and completion state need to be persisted (Room) so the app can reconcile state on relaunch.
