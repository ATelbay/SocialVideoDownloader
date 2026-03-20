# Quickstart: Cloud History Backup

**Feature**: 009-cloud-history-backup
**Date**: 2026-03-21

## Prerequisites

1. Android Studio with Kotlin 2.2.10+
2. Firebase project created at console.firebase.google.com
3. `google-services.json` downloaded and placed in `app/` directory
4. Google Play Console access for billing setup (US3 only)

## Setup Steps

### 1. Firebase Project Setup

```bash
# Option A: Use Firebase MCP tool
# firebase_create_project → firebase_create_app (Android) → firebase_get_sdk_config

# Option B: Manual
# 1. Go to console.firebase.google.com
# 2. Create project "SocialVideoDownloader"
# 3. Add Android app with package "com.socialvideodownloader"
# 4. Download google-services.json → app/
# 5. Enable Anonymous Auth in Authentication → Sign-in method
# 6. Create Firestore database in production mode
# 7. Deploy security rules from contracts/cloud-sync-interfaces.md
```

### 2. Add Dependencies

In `gradle/libs.versions.toml`, add:
```toml
[versions]
firebaseBom = "33.15.0"
playBilling = "7.1.1"
googleServices = "4.5.0"

[libraries]
firebase-bom = { group = "com.google.firebase", name = "firebase-bom", version.ref = "firebaseBom" }
firebase-auth = { group = "com.google.firebase", name = "firebase-auth-ktx" }
firebase-firestore = { group = "com.google.firebase", name = "firebase-firestore-ktx" }
play-billing = { group = "com.android.billingclient", name = "billing-ktx", version.ref = "playBilling" }

[plugins]
google-services = { id = "com.google.gms.google-services", version.ref = "googleServices" }
```

### 3. Module Setup

Create new modules:
```bash
# :core:cloud module
mkdir -p core/cloud/src/main/kotlin/com/socialvideodownloader/core/cloud
mkdir -p core/cloud/src/test/kotlin/com/socialvideodownloader/core/cloud

# :core:billing module
mkdir -p core/billing/src/main/kotlin/com/socialvideodownloader/core/billing
mkdir -p core/billing/src/test/kotlin/com/socialvideodownloader/core/billing
```

Add to `settings.gradle.kts`:
```kotlin
include(":core:cloud")
include(":core:billing")
```

### 4. Build & Verify

```bash
# Verify project compiles with new dependencies
./gradlew assembleDebug

# Run existing tests to ensure no regressions
./gradlew test

# Lint check
./gradlew ktlintCheck
```

### 5. Validate Cloud Backup Flow

1. Install debug build on device/emulator
2. Download a video to populate history
3. Toggle "Cloud Backup" on in history screen
4. Verify in Firebase Console → Firestore that encrypted document appeared under `users/{uid}/history/`
5. Verify the `encryptedPayload` field is not human-readable (encrypted blob)
6. Toggle backup off, download another video → verify no new cloud document
7. Toggle backup on → verify the new record syncs

### 6. Validate Restore Flow

1. With backup enabled and records synced
2. Clear app data (Settings → Apps → SocialVideoDownloader → Clear Data)
3. Reopen app → history should be empty
4. Toggle Cloud Backup on → "Restore from Cloud"
5. **Expected**: Error message about encryption key loss (key was in Keystore, cleared with app data)

### 7. Validate Graceful Degradation

1. Enable backup, sync some records
2. Enable Airplane Mode
3. Download a new video → should complete normally
4. Check history → sync indicator shows "Backup paused" or equivalent
5. Disable Airplane Mode → record should sync within 30 seconds

### 8. Validate Purchase Flow (requires Play Console setup)

1. Add test account as license tester in Play Console
2. Create in-app product `cloud_history_10k` (500 KZT)
3. Build signed APK, upload to internal testing track
4. Install from Play Store on test device
5. Fill cloud history near 1,000 records
6. Verify capacity banner appears
7. Tap "Upgrade" → complete test purchase
8. Verify tier limit updates to 10,000
