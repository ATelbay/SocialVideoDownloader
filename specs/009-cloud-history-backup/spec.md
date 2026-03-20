# Feature Specification: Cloud History Backup

**Feature Branch**: `009-cloud-history-backup`
**Created**: 2026-03-21
**Status**: Draft
**Input**: User description: "Optional cloud backup for download history using Firebase Firestore. Room remains primary local store. Firestore sync is opt-in. Firebase Anonymous Auth, on-device encryption via Android Keystore, LRU eviction at 1,000 records, one-directional local→cloud backup with manual cloud→local restore. Freemium: free tier 1,000 records, paid tier unlocks additional capacity via one-time Google Play Billing purchase."

## Clarifications

### Session 2026-03-21

- Q: Paid tier structure — single or multiple tiers, and what capacity? → A: Single paid tier: 10,000 records for 500 tenge (~$1). One SKU, no further tiers.
- Q: When a user deletes a local history record, should the cloud copy also be deleted? → A: Yes, propagate deletes to cloud. Cloud mirrors local intent.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Enable Cloud Backup and Sync History (Priority: P1)

A user has been downloading videos and has a growing local history. They want to back up their history to the cloud in case they lose their device or reset the app. They open the history screen, see a "Cloud Backup" toggle, and turn it on. Behind the scenes, the app silently creates an anonymous identity (no sign-in screen, no email, no password), encrypts all existing history records on-device, and uploads them to the cloud. From this point forward, every new completed download is automatically backed up. The user sees a subtle sync status indicator confirming their history is backed up.

**Why this priority**: This is the core value proposition of the feature. Without the ability to enable backup and have history sync automatically, no other cloud functionality matters.

**Independent Test**: Can be fully tested by toggling cloud backup on, completing a download, and verifying that the record appears in the cloud store (encrypted). Then toggling off and verifying no further uploads occur.

**Acceptance Scenarios**:

1. **Given** the user is on the history screen with cloud backup disabled, **When** they tap the "Cloud Backup" toggle, **Then** the app silently authenticates anonymously (no sign-in UI) and begins uploading encrypted history records
2. **Given** cloud backup is enabled and a download completes, **When** the download record is saved locally, **Then** the record is encrypted on-device and uploaded to the cloud within 30 seconds
3. **Given** cloud backup is enabled, **When** the user views the history screen, **Then** a sync status indicator shows the last successful backup time or "Syncing..." if an upload is in progress
4. **Given** cloud backup is enabled, **When** the user toggles it off, **Then** no further records are uploaded, no background network activity occurs, and existing cloud records are retained (not deleted)
5. **Given** cloud backup has never been enabled, **Then** no anonymous auth token is created, no network requests are made, and the app behaves identically to the pre-feature baseline

---

### User Story 2 - Restore History from Cloud (Priority: P2)

A user has reinstalled the app or cleared app data and wants to recover their download history from a previous backup. They enable cloud backup, and then tap a "Restore from Cloud" action. The app downloads all cloud records, decrypts them on-device, and merges them into the local database. Duplicates (same source URL + creation timestamp) are skipped. The user sees the restored records appear in their history list.

**Why this priority**: Restore completes the backup story — backup without restore is useless. However, it is a less frequent action than ongoing sync, so it is secondary.

**Independent Test**: Can be tested by backing up history on one app instance, clearing app data, enabling cloud backup with the same anonymous identity, triggering restore, and verifying records appear locally.

**Acceptance Scenarios**:

1. **Given** cloud backup is enabled and cloud records exist, **When** the user taps "Restore from Cloud", **Then** cloud records are downloaded, decrypted on-device, and inserted into the local database
2. **Given** a restore is in progress, **When** records are being processed, **Then** the user sees a progress indicator showing how many records have been restored
3. **Given** the local database already contains a record with the same source URL and creation timestamp as a cloud record, **When** restore processes that record, **Then** the duplicate is skipped (not inserted twice)
4. **Given** the encryption key used for backup is no longer available (device change without key migration), **When** restore is attempted, **Then** the user sees a clear message explaining that records cannot be decrypted and are unrecoverable

---

### User Story 3 - Purchase Additional Cloud Capacity (Priority: P3)

A user approaching the 1,000-record free tier limit sees a notification in the history screen indicating they are near capacity. They can tap to upgrade and make a one-time purchase (500 tenge / ~$1) to unlock 10,000 cloud records. The purchase is handled through Google Play Billing. After purchase, the new limit takes effect immediately and the LRU eviction threshold is raised.

**Why this priority**: Monetization is a secondary concern — the free tier covers the vast majority of personal use. This story enables sustainability but is not required for the feature to deliver value.

**Independent Test**: Can be tested by filling the cloud store to near capacity, verifying the upgrade prompt appears, completing a test purchase via Google Play test environment, and confirming the new limit is applied.

**Acceptance Scenarios**:

1. **Given** the user has 900+ records in the cloud (90%+ of free tier), **When** they view the history screen, **Then** a non-intrusive banner shows "X of 1,000 cloud records used" with an "Upgrade" action
2. **Given** the user taps "Upgrade", **When** the upgrade options are displayed, **Then** the upgrade screen shows the price (500 tenge / ~$1), the 10,000-record limit, and a "Buy" button
3. **Given** the user taps "Buy", **When** the purchase flow completes successfully via Google Play, **Then** the new record limit takes effect immediately and the capacity banner updates
4. **Given** the user has purchased an upgrade, **When** they reinstall the app, **Then** the purchase is restored via Google Play Billing and the upgraded limit is applied without re-purchasing
5. **Given** the user has not purchased an upgrade and reaches 1,000 records, **When** a new download completes, **Then** the oldest cloud record is evicted (deleted from cloud) to make room for the new one

---

### User Story 4 - Graceful Degradation Under Failure Conditions (Priority: P2)

The cloud backup feature operates in an environment where network connectivity, cloud service availability, and device cryptographic state can all change unexpectedly. The user MUST never be blocked from using the app's core download functionality by any cloud-related failure. All failures are handled silently or with non-blocking informational messages.

**Why this priority**: Principle VIII of the constitution mandates graceful degradation. Any failure in cloud features that disrupts the core download flow is a constitutional violation.

**Independent Test**: Can be tested by enabling cloud backup, then simulating each failure condition (airplane mode, Firestore quota exceeded, expired auth token) and verifying the app continues to function normally for downloads.

**Acceptance Scenarios**:

1. **Given** cloud backup is enabled and the device is offline, **When** a download completes, **Then** the record is saved locally, the cloud upload is silently deferred, and the upload retries automatically when connectivity returns
2. **Given** the anonymous auth token has expired, **When** a sync attempt is made, **Then** the system silently re-authenticates and retries the upload without user intervention
3. **Given** Firestore quota is exceeded or the service is unavailable, **When** a sync attempt fails, **Then** the failure is logged locally, the user sees a subtle "Backup paused" indicator (not an error dialog), and sync retries with exponential backoff
4. **Given** the encryption key in the device secure store has been invalidated (e.g., user changed device lock), **When** a new backup attempt is made, **Then** the system generates a new encryption key, re-encrypts and re-uploads all local records, and notifies the user that previous cloud records are no longer decryptable
5. **Given** any cloud feature failure, **Then** the core download flow (URL → extract → format select → download → save) MUST continue to function identically to the offline-only baseline

---

### Edge Cases

- What happens when the user disables cloud backup after syncing 500 records? → Cloud records are retained but no new uploads occur. Re-enabling backup resumes uploads from where it left off.
- What happens when two different devices use the same anonymous UID? → Out of scope for this feature — cross-device merge is a future feature. Each device operates independently.
- What happens when the cloud security rules reject a write? → The write is retried once. If it fails again, the error is logged and sync is paused with a subtle indicator. Core app functionality is unaffected.
- What happens when the user's Google Play purchase is refunded? → The app reverts to the free tier limit on the next purchase status check. Records above the free tier limit are not immediately evicted but no new records are backed up until the count drops below the limit.
- What happens when the user has exactly 1,000 records and downloads a new video? → The oldest cloud record (by creation timestamp) is evicted, and the new record is uploaded. The user is not prompted — eviction is automatic and silent.
- What happens when the app is force-killed during a cloud upload? → The upload is lost. On next app launch, the sync layer detects unsynced local records and re-uploads them.
- What happens when the user clears app data? → The local encryption key is destroyed, the anonymous auth token is lost, and cloud records become undecryptable. The user effectively starts fresh. This is documented in onboarding/help text.
- What happens when network switches from Wi-Fi to mobile data during a bulk sync? → Sync continues on mobile data. No special handling — individual record uploads are small (< 1 KB encrypted payload each).
- What happens when the user deletes a local history record while cloud backup is enabled? → The corresponding cloud record is also deleted. If offline, the cloud deletion is queued and executed when connectivity returns.
- What happens when the user deletes a local record while offline, then restores from cloud before going online? → The restore re-inserts the record locally. When connectivity returns, the queued cloud deletion executes, removing the cloud copy. The local copy persists (restore is a local insert, not affected by cloud deletion).

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: System MUST provide an opt-in toggle for cloud backup, accessible from the history screen
- **FR-002**: System MUST authenticate anonymously (no sign-in UI, no email, no password, no OAuth consent) when cloud backup is first enabled
- **FR-003**: System MUST encrypt all download history record data on-device before uploading to the cloud, using device-bound key management
- **FR-004**: System MUST upload each new completed download record to the cloud within 30 seconds of local persistence (target), measured as p99 ≤ 60 seconds per SC-002, when cloud backup is enabled and the device is online
- **FR-005**: System MUST enforce a maximum of 1,000 cloud records per user on the free tier, evicting the oldest record (by creation timestamp) when the limit is reached
- **FR-006**: System MUST support one-directional sync by default: local → cloud (backup only)
- **FR-007**: System MUST provide a manual "Restore from Cloud" action that downloads, decrypts, and merges cloud records into the local database
- **FR-008**: System MUST skip duplicate records during restore (matching on source URL + creation timestamp)
- **FR-009**: System MUST display a sync status indicator on the history screen showing last backup time or current sync state
- **FR-010**: System MUST NOT make any network requests, create auth tokens, or perform background work when cloud backup has never been enabled
- **FR-011**: System MUST handle offline conditions by deferring uploads and retrying automatically when connectivity returns
- **FR-012**: System MUST silently re-authenticate when the anonymous auth token expires, without user intervention
- **FR-013**: System MUST display a non-intrusive capacity banner when cloud record count reaches 90% of the user's tier limit
- **FR-014**: System MUST support one-time purchase via Google Play Billing to unlock additional cloud record capacity
- **FR-015**: System MUST restore prior purchases on app reinstall without requiring re-purchase
- **FR-016**: System MUST scope all cloud data access to the authenticated anonymous UID — no user can read or write another user's records
- **FR-017**: System MUST NOT block, delay, or degrade the core download flow (URL input → extraction → format selection → download → save) under any cloud feature failure condition
- **FR-018**: System MUST retain cloud records when the user disables cloud backup (no deletion on toggle-off)
- **FR-019**: System MUST generate a new encryption key and re-encrypt records if the existing key is invalidated
- **FR-020**: System MUST delete the corresponding cloud record when a user deletes a local history record, when cloud backup is enabled and the device is online. If offline, the cloud deletion MUST be deferred and executed when connectivity returns

### Key Entities

- **Cloud History Record**: An encrypted representation of a download history entry stored in the cloud — contains the encrypted payload (derived from the local download record), creation timestamp, and owner identifier. The plaintext is never stored or transmitted to the cloud.
- **Sync State**: Tracks the backup status of each local record — whether it has been uploaded, is pending upload, or failed. Used to determine which records need syncing on app launch or connectivity change.
- **Cloud Tier**: Represents the user's cloud capacity — tier level (free or paid), maximum record count, and purchase status. Determines the LRU eviction threshold.
- **Encryption Key Metadata**: Device-bound cryptographic key reference used for encrypting and decrypting history records — key alias, creation date, and validity status. The key material never leaves the device's secure hardware.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Users can enable cloud backup in a single tap with zero sign-in friction (no credentials, no consent screens)
- **SC-002**: 99% of completed download records are backed up to the cloud within 60 seconds of local save, given stable network
- **SC-003**: Restore from cloud completes within 30 seconds for up to 1,000 records
- **SC-004**: Core download flow performance (URL to download start) is unaffected (within 5% of baseline) when cloud backup is enabled
- **SC-005**: All cloud feature failures are handled without showing error dialogs or blocking user interaction — informational indicators only
- **SC-006**: The free tier (1,000 records) covers 90%+ of personal use scenarios without requiring purchase
- **SC-007**: Purchase flow completes in under 60 seconds, including Google Play confirmation
- **SC-008**: No plaintext user data is transmitted to or stored in the cloud — verifiable by inspecting cloud-stored records

## Assumptions

- Anonymous authentication provides a stable UID that persists across app sessions on the same device, but is lost on app data clear or device change
- Android Keystore is available on all devices running API 26+ and provides hardware-backed key storage where supported
- The encrypted payload for a single history record is small (< 1 KB), making individual uploads fast and mobile-data-friendly
- Google Play Billing handles purchase verification, restoration, and refund detection — the app does not need its own receipt validation server
- Cloud service free tier (1 GiB storage, 50K reads/day, 20K writes/day) is sufficient for personal use at the 1,000-record scale
- The "oldest record" for LRU eviction is determined by the creation timestamp of the original download, not the upload timestamp
- Cross-device sync, real-time sync, and conflict resolution are explicitly out of scope — each device operates independently against the same cloud store
- Video files are NOT synced — only download metadata/history records
- The freemium price point (500 tenge / ~$1) is fixed for initial launch; pricing adjustments are a future concern
