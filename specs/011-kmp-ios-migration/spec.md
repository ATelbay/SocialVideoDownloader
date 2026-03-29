# Feature Specification: KMP iOS Migration

**Feature Branch**: `011-kmp-ios-migration`
**Created**: 2026-03-30
**Status**: Draft
**Input**: Kotlin Multiplatform migration to add iOS support for SocialVideoDownloader. Share maximum business logic via KMP while building native SwiftUI UI for iOS. iOS uses the existing yt-dlp API server exclusively since Apple prohibits embedded interpreters (App Store Review Guidelines §2.5.2).

## User Scenarios & Testing *(mandatory)*

### User Story 1 - iOS User Downloads a Video (Priority: P1)

An iOS user opens the app, pastes a video URL, sees extracted video information with available formats, selects a format, and downloads the video to their device. The entire extraction and download happens via the yt-dlp API server. The downloaded file is saved to the device and accessible through the iOS Files app.

**Why this priority**: This is the core value proposition — downloading videos on iOS. Without this, there is no iOS app.

**Independent Test**: Can be fully tested by pasting a supported URL (e.g., YouTube, Instagram) on an iOS device, selecting a format, completing the download, and verifying the file exists in the Files app.

**Acceptance Scenarios**:

1. **Given** the iOS app is installed and open, **When** the user pastes a valid video URL and taps extract, **Then** the app sends the URL to the yt-dlp API server, displays video title/thumbnail, and shows available format options.
2. **Given** format options are displayed, **When** the user selects a format and taps download, **Then** the app downloads the file via the server's direct download URL, shows progress, and saves the file to the device's Documents directory.
3. **Given** a download is in progress, **When** the user backgrounds the app, **Then** the download continues in the background and completes without user intervention.
4. **Given** the API server is unreachable, **When** the user attempts to extract video info, **Then** the app displays a clear error indicating the server is unavailable and suggests retrying later.

---

### User Story 2 - Android App Continues Working Unchanged (Priority: P1)

Existing Android users experience no regressions. The Android app continues to work exactly as before — on-device yt-dlp extraction with server fallback, foreground service downloads, MediaStore storage, cloud backup, and billing all remain functional throughout and after the migration.

**Why this priority**: Equal to P1 because breaking the existing Android app would lose current users. The migration must be incremental and non-destructive.

**Independent Test**: Run the full existing Android test suite after each migration phase. Manually verify end-to-end download flow on an Android device.

**Acceptance Scenarios**:

1. **Given** the KMP migration is applied, **When** the Android app is built and run, **Then** all existing functionality works identically — URL extraction, format selection, downloading, history, library, cloud backup, and billing.
2. **Given** a new shared KMP module replaces an Android-only module, **When** the Android app uses the shared module, **Then** behavior and performance are equivalent to the previous Android-only implementation.
3. **Given** the existing Android test suite, **When** tests are run after any migration phase, **Then** all tests pass without modification (except import path changes from module restructuring).

---

### User Story 3 - iOS User Views Download History (Priority: P2)

An iOS user can see their past downloads in a history screen, search through them, and delete individual entries or clear all history. History is persisted locally on the device using the same database schema shared with Android.

**Why this priority**: History is a core companion feature to downloading — users need to find and manage their past downloads.

**Independent Test**: Download a video, navigate to the History tab, verify the download appears with correct metadata. Search for it by title. Delete it and verify it's removed.

**Acceptance Scenarios**:

1. **Given** the user has completed one or more downloads, **When** they navigate to the History tab, **Then** they see a list of past downloads with title, thumbnail, platform, date, and file size.
2. **Given** the history list is populated, **When** the user searches by keyword, **Then** the list filters to show only matching entries.
3. **Given** a history entry exists, **When** the user deletes it, **Then** the entry is removed from the list and the database.

---

### User Story 4 - iOS User Browses Downloaded Files in Library (Priority: P2)

An iOS user can browse their downloaded video files in a library view, open them for playback, or share them with other apps.

**Why this priority**: Library completes the download-to-consumption flow. Without it, users must leave the app to find their files.

**Independent Test**: Download a video, navigate to Library tab, tap on the file to play it, use share to send it to another app.

**Acceptance Scenarios**:

1. **Given** the user has downloaded files, **When** they navigate to the Library tab, **Then** they see their downloaded videos with thumbnails, titles, and file information.
2. **Given** a file in the library, **When** the user taps it, **Then** the system video player opens the file for playback.
3. **Given** a file in the library, **When** the user shares it, **Then** the iOS share sheet appears allowing the user to send the file to other apps.

---

### User Story 5 - iOS User Receives URL from Another App (Priority: P3)

An iOS user shares a video URL from Safari or another app to SocialVideoDownloader via the iOS Share Sheet. The app opens with the URL pre-filled and ready for extraction.

**Why this priority**: Share sheet integration is a major convenience feature that reduces friction, but the app is fully usable without it.

**Independent Test**: In Safari, tap Share on a video page, select SocialVideoDownloader from the share sheet, verify the app opens with the URL pre-filled.

**Acceptance Scenarios**:

1. **Given** the user is in Safari viewing a supported video page, **When** they share the URL to SocialVideoDownloader via the share sheet, **Then** the app opens with the URL pre-filled in the input field.
2. **Given** the app receives a shared URL, **When** the URL is valid and supported, **Then** extraction begins automatically.

---

### User Story 6 - Shared Business Logic Across Platforms (Priority: P1)

Domain models, use cases, repository interfaces, ViewModel state machines, and the server API client are shared between Android and iOS in common Kotlin code. Changes to business logic are made once and apply to both platforms.

**Why this priority**: This is the architectural goal of KMP — without shared logic, there's no point in using KMP over building a separate native iOS app.

**Independent Test**: Write a unit test for a shared use case or ViewModel in commonMain. Run it on both JVM (Android) and Native (iOS) targets. Both pass with identical behavior.

**Acceptance Scenarios**:

1. **Given** a domain model defined in shared code, **When** it is used on both Android and iOS, **Then** it behaves identically on both platforms.
2. **Given** the shared download ViewModel state machine, **When** a state transition occurs (e.g., Idle to Extracting to FormatSelection), **Then** the same sequence and validation logic applies on both platforms.
3. **Given** a shared use case, **When** a unit test runs on both JVM and iOS Native targets, **Then** both produce identical results.

---

### User Story 7 - iOS Cloud Backup and Billing (Priority: P3)

An iOS user can sign in, enable cloud backup of their download history, and purchase premium tiers via in-app purchase to increase cloud storage capacity.

**Why this priority**: Cloud features and billing are monetization layers that build on top of the core download experience. They can ship after the core iOS app is functional.

**Independent Test**: Sign in on iOS, enable cloud backup, verify history syncs. Purchase a premium tier via sandbox billing, verify increased capacity.

**Acceptance Scenarios**:

1. **Given** the iOS app with authentication configured, **When** the user signs in, **Then** they are authenticated and can access cloud features.
2. **Given** an authenticated user, **When** they enable cloud backup, **Then** their download history begins syncing to the cloud.
3. **Given** the billing screen, **When** the user purchases a premium tier, **Then** their cloud storage capacity increases and the purchase is recorded.

---

### Edge Cases

- What happens when the yt-dlp API server is down or unreachable from iOS? The app must show a clear "server unavailable" message with retry option and not crash or hang.
- What happens when an iOS download is interrupted by network loss mid-transfer? The app should detect the failure, display an error, and allow the user to retry the download.
- What happens when the iOS device runs out of storage during a download? The app must detect the storage error and inform the user, cleaning up any partial file.
- What happens when the server returns an extraction error (e.g., unsupported URL, geo-blocked content)? The app must display the server's error message in a user-friendly format.
- What happens when a background download completes while the app is terminated? The system should save the file and update the database; the user sees the completed download when they next open the app.
- What happens when Room KMP migration encounters a database created by the older Android-only Room version? The migration must be backwards-compatible — existing Android databases must open without data loss.
- What happens when both DI systems try to provide the same dependency in the Android app? The bridge pattern must clearly delineate which system owns which dependency, with no conflicts.

## Requirements *(mandatory)*

### Functional Requirements

**Core Download (iOS)**

- **FR-001**: The iOS app MUST extract video information by sending the URL to the yt-dlp API server and displaying the returned metadata (title, thumbnail, duration, available formats).
- **FR-002**: The iOS app MUST download video files using direct download URLs provided by the server, with visible progress indication.
- **FR-003**: The iOS app MUST continue downloads in the background when the user leaves the app, completing them without user intervention.
- **FR-004**: The iOS app MUST save downloaded files to a location accessible via the iOS Files app.
- **FR-005**: The iOS app MUST allow users to cancel an in-progress download.
- **FR-006**: The iOS app MUST display clear, user-friendly error messages when extraction or download fails, including server unavailability, unsupported URLs, and network errors.
- **FR-007**: The iOS app MUST notify the user when a background download completes.

**Shared Architecture**

- **FR-008**: Domain models, repository interfaces, and use cases MUST be defined in shared Kotlin code usable by both Android and iOS.
- **FR-009**: ViewModel state machines (download, history, library) MUST be defined in shared Kotlin code, with platform-specific actions delegated through defined interfaces.
- **FR-010**: The server API client MUST be implemented in shared Kotlin code using a multiplatform HTTP client, usable on both Android and iOS.
- **FR-011**: The local database schema (entities, DAOs, migrations) MUST be defined in shared Kotlin code with platform-specific database builders.
- **FR-012**: Platform-specific behaviors (file storage, download management, clipboard, notifications) MUST be abstracted behind interfaces defined in shared code, with implementations provided per platform.

**Android Preservation**

- **FR-013**: The Android app MUST retain all existing functionality — on-device yt-dlp extraction, server fallback, foreground service downloads, MediaStore storage, cloud backup, and billing — throughout and after the migration.
- **FR-014**: The Android app MUST continue to pass all existing unit tests after each migration phase, with only import-path changes permitted.
- **FR-015**: The Android app's build and runtime performance MUST NOT degrade as a result of the migration.

**iOS UI**

- **FR-016**: The iOS app MUST have three main sections: Download, Library, and History, accessible via tab navigation.
- **FR-017**: The iOS app MUST reproduce the existing warm editorial design aesthetic — same color palette, typography choices, and corner radius conventions — adapted for iOS design patterns.
- **FR-018**: The iOS app MUST accept shared URLs from other apps via the iOS Share Sheet.

**iOS Cloud and Billing**

- **FR-019**: The iOS app MUST support user authentication for cloud features.
- **FR-020**: The iOS app MUST sync download history to the cloud when the user enables backup.
- **FR-021**: The iOS app MUST support in-app purchases for premium cloud storage tiers using the platform's native billing system.

**Dependency Injection**

- **FR-022**: Shared modules MUST use a multiplatform-compatible DI framework. Android-only modules MAY continue using their existing DI framework during a transitional period.
- **FR-023**: The Android app MUST bridge both DI systems without conflicts, with clear ownership boundaries for each dependency.

### Key Entities

- **VideoMetadata**: Represents extracted video information — title, thumbnail URL, duration, source platform, available formats. Shared across platforms.
- **DownloadRequest**: Represents a user's intent to download — URL, selected format, target quality. Shared across platforms.
- **DownloadRecord**: Represents a completed download — file path, title, platform, date, size, thumbnail. Persisted in shared database.
- **VideoFormatOption**: Represents an available download format — resolution, file size estimate, codec info, extension. Shared across platforms.
- **DownloadProgress**: Represents ongoing download state — percentage, bytes downloaded, total bytes, estimated time remaining. Shared across platforms.
- **PlatformDownloadManager**: Abstraction for platform-specific download execution — start, cancel, observe progress. Android: foreground service. iOS: background URL session.
- **PlatformFileStorage**: Abstraction for platform-specific file operations — save, check accessibility, delete. Android: MediaStore. iOS: Documents directory.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: iOS users can extract video information and complete a download for any URL supported by the existing Android app, with the full flow (paste URL, select format, download complete) achievable in under 3 minutes on a standard connection.
- **SC-002**: iOS downloads continue to completion when the app is backgrounded or terminated, with the file available upon next app launch.
- **SC-003**: The Android app passes 100% of its existing test suite after every migration phase, with zero functional regressions.
- **SC-004**: At least 80% of non-UI business logic (domain models, use cases, repository implementations, ViewModel state machines, server API client) resides in shared Kotlin code, measured by line count in shared vs. platform-specific modules.
- **SC-005**: The iOS app's visual design is recognizably consistent with the Android app — same color palette, font choices, spacing conventions, and interaction patterns — as validated by side-by-side comparison.
- **SC-006**: iOS download history persists across app launches, with all records queryable and deletable.
- **SC-007**: iOS users can authenticate and enable cloud backup, with history records appearing in the cloud within 30 seconds of sync initiation.
- **SC-008**: iOS in-app purchases complete successfully via the platform billing system, with tier upgrades reflected immediately in the app.

## Assumptions

- The existing yt-dlp API server is reliable enough to serve as the sole extraction/download backend for iOS. Server scaling and redundancy are out of scope but acknowledged as a future concern.
- Apple will not reject the app for downloading video content, provided it complies with App Store Review Guidelines (no piracy features, proper content descriptions).
- Room KMP 2.8.x is stable enough for production use with both Android and iOS targets. If stability issues arise, SQLDelight is the fallback (requiring entity/DAO rewrites).
- The Hilt-Koin bridge pattern will not introduce DI conflicts in the Android app during the transitional period.
- SKIE provides sufficient Swift interop quality for StateFlow, sealed classes, and suspend functions to build idiomatic SwiftUI code.
- The existing Android convention plugins can coexist with new KMP convention plugins in the same build-logic module.
- iOS users will accept that downloads require server connectivity (no offline extraction), as this is a platform limitation.
- Sign in with Apple will be offered alongside Google Sign-In as an authentication option on iOS, per App Store requirements for apps offering third-party sign-in.

## Scope Boundaries

**In Scope**:
- KMP Gradle infrastructure and convention plugins
- Converting existing pure-Kotlin modules to KMP
- New shared network module (multiplatform HTTP client)
- New shared data module (shared database, platform file storage)
- New shared feature modules (ViewModel state machines)
- iOS SwiftUI app with all three tabs (Download, Library, History)
- iOS background downloads
- iOS share sheet integration
- iOS cloud backup and billing
- iOS design system matching Android aesthetic
- Constitution amendment for multi-platform architecture

**Out of Scope**:
- Server scaling, auto-scaling, or redundancy for the yt-dlp API server
- Local yt-dlp execution on iOS (prohibited by Apple)
- Full migration from Hilt to Koin on Android (optional future work)
- iPad-specific layouts or macOS Catalyst support
- Widgets, App Clips, or other iOS extensions beyond share sheet
- Android UI changes or redesign
- Automated UI testing (Compose testing or XCUITest)
- App Store submission and review process
- Localization beyond English

## Dependencies

- The yt-dlp API server must remain operational throughout development and after launch.
- Apple Developer Program membership required for iOS development, testing, and distribution.
- Xcode and macOS required for iOS builds — CI/CD must support macOS runners.
- SKIE library must support the Kotlin version used and produce stable Swift interop.
