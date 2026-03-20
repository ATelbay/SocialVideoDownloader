# Research: Cloud History Backup

**Feature**: 009-cloud-history-backup
**Date**: 2026-03-21

## 1. Firebase Anonymous Auth

### Decision
Use Firebase Anonymous Auth (`firebase-auth-ktx`) for zero-friction identity.

### Rationale
- Anonymous Auth provides a stable UID per device that persists across app restarts
- UID is lost on: app data clear, uninstall+reinstall, factory reset
- Token refresh is automatic — `FirebaseAuth.AuthStateListener` detects expiry and the SDK re-authenticates silently
- No sign-in UI required: `FirebaseAuth.getInstance().signInAnonymously()` returns a `Task<AuthResult>`

### Key API surface
```kotlin
// Sign in (first time or re-auth after token loss)
FirebaseAuth.getInstance().signInAnonymously()
    .addOnSuccessListener { result ->
        val uid = result.user?.uid  // stable per device
    }

// Check current user (returns null if never signed in or data cleared)
val currentUser = FirebaseAuth.getInstance().currentUser

// Listen for auth state changes
FirebaseAuth.getInstance().addAuthStateListener { auth ->
    val user = auth.currentUser  // null = signed out
}
```

### Lazy initialization strategy
- Firebase auto-initializes via `FirebaseInitProvider` (ContentProvider in manifest)
- To prevent cold-start network calls when cloud backup is disabled:
  - Option A: Disable auto-init with `<meta-data android:name="firebase_data_collection_default_enabled" android:value="false">` and call `Firebase.initialize(context)` manually on first opt-in
  - Option B: Use `FirebaseApp.initializeApp(context)` lazily in the Hilt module, gated by a `CloudBackupPreferences.isEnabled` check
  - **Chosen**: Option B — lazy Hilt provider. The `google-services.json` is still present but Firebase won't make network calls until `signInAnonymously()` is called.
  - Additionally, set `FirebaseFirestore.getInstance().firestoreSettings = firestoreSettings { isPersistenceEnabled = false }` to prevent local disk caching of encrypted blobs.

### Alternatives considered
- Custom device ID (UUID in SharedPreferences): No server-side auth, can't scope Firestore security rules → rejected
- Google Sign-In: Violates Constitution Principle VIII (no user-facing sign-in) → rejected

### Dependencies
```toml
# libs.versions.toml additions
firebaseBom = "33.15.0"

firebase-bom = { group = "com.google.firebase", name = "firebase-bom", version.ref = "firebaseBom" }
firebase-auth = { group = "com.google.firebase", name = "firebase-auth-ktx" }
firebase-firestore = { group = "com.google.firebase", name = "firebase-firestore-ktx" }

# Plugin
google-services = { id = "com.google.gms.google-services", version = "4.5.0" }
```

---

## 2. On-Device Encryption (Raw Android Keystore — AES-256-GCM)

### Decision
Use raw Android Keystore API with AES-256-GCM directly. No Tink dependency.

### Rationale
- Raw Keystore API is straightforward for our single-key, no-rotation use case
- IV management is simple: let `Cipher` generate a random 12-byte IV, prepend it to ciphertext
- GCM auth tag is automatically appended by `Cipher.doFinal()` — no manual handling
- Avoids ~800KB Tink dependency for a single encrypt/decrypt use case
- No key rotation requirement (personal utility, not enterprise)
- All APIs available since API 23 — no compatibility concerns for minSdk 26

### Encryption flow
```kotlin
private const val KEY_ALIAS = "com.socialvideodownloader.cloud_backup_key"

fun encrypt(plaintext: ByteArray, key: SecretKey): ByteArray {
    val cipher = Cipher.getInstance("AES/GCM/NoPadding")
    cipher.init(Cipher.ENCRYPT_MODE, key)  // auto-generates random IV
    val iv = cipher.iv                      // 12 bytes
    val ciphertext = cipher.doFinal(plaintext)  // includes 16-byte auth tag
    return iv + ciphertext  // [12 IV][ciphertext + 16 tag]
}

fun decrypt(ivAndCiphertext: ByteArray, key: SecretKey): ByteArray {
    val iv = ivAndCiphertext.copyOfRange(0, 12)
    val ciphertext = ivAndCiphertext.copyOfRange(12, ivAndCiphertext.size)
    val cipher = Cipher.getInstance("AES/GCM/NoPadding")
    cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(128, iv))
    return cipher.doFinal(ciphertext)  // throws AEADBadTagException if tampered
}
```

### Key generation
```kotlin
fun getOrCreateKey(): SecretKey {
    val ks = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
    ks.getKey(KEY_ALIAS, null)?.let { return it as SecretKey }

    val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
    generator.init(
        KeyGenParameterSpec.Builder(KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .setUserAuthenticationRequired(false)  // background sync needs this
            .build()
    )
    return generator.generateKey()
}
```

### Key invalidation handling
- With `setUserAuthenticationRequired(false)`, keys are NOT invalidated by lock screen or biometric changes
- Keys ARE destroyed on: factory reset, app uninstall, app data clear
- If key is lost: `KeyPermanentlyInvalidatedException` caught → delete old alias, generate new key, re-encrypt local records, upload as new cloud records
- Old cloud records become undecryptable (documented in spec, accepted tradeoff)

### API 26+ compatibility
- Hardware-backed Keystore available on most API 26+ devices (StrongBox on API 28+)
- Tink works with software-backed Keystore as fallback — no issues on API 26

### Alternatives considered
- Google Tink (`tink-android`): Higher-level AEAD abstraction with key rotation support. ~800KB dependency — overkill for single-key personal utility. Would be the right choice for an enterprise app needing key versioning → rejected for now
- EncryptedSharedPreferences: Only for key-value prefs, not arbitrary blobs → rejected
- Jetpack Security Crypto (AndroidX): Wraps Tink internally, adds `EncryptedFile`/`EncryptedSharedPreferences` — overkill for our blob-encryption use case → rejected

### Dependencies
No additional encryption dependencies needed — `javax.crypto.Cipher`, `java.security.KeyStore`, and `android.security.keystore.KeyGenParameterSpec` are all Android framework APIs.

---

## 3. Firestore Data Model & LRU Eviction

### Decision
Store encrypted records in per-user subcollections with a counter document for quota management.

### Data model
```
Firestore structure:
  users/
    {uid}/
      meta/
        counters    → { recordCount: 42, tierLimit: 1000 }
      history/
        {recordId}  → { encryptedPayload: Blob, createdAt: Timestamp, sourceUrlHash: String }
```

- `encryptedPayload`: Firestore `Blob` field containing the Tink-encrypted JSON
- `createdAt`: Plaintext `Timestamp` for ordering/eviction queries
- `sourceUrlHash`: SHA-256 hash of `sourceUrl + createdAt` — used for dedup during restore without exposing plaintext URL

### LRU eviction strategy
```kotlin
// Query oldest records beyond limit
val excess = firestore.collection("users/$uid/history")
    .orderBy("createdAt", Query.Direction.ASCENDING)
    .limit(evictionCount.toLong())
    .get()
    .await()

// Batch delete
val batch = firestore.batch()
excess.documents.forEach { batch.delete(it.reference) }
batch.commit().await()

// Update counter
firestore.document("users/$uid/meta/counters")
    .update("recordCount", FieldValue.increment(-evictionCount.toLong()))
    .await()
```

### Offline handling
- **Disable Firestore persistence** (`isPersistenceEnabled = false`): We don't want encrypted blobs cached locally in Firestore's SQLite cache — Room is the source of truth. Firestore's offline queue still works for pending writes.
- Deferred writes: When offline, writes queue in memory. On connectivity restore, Firestore flushes automatically.
- Connectivity detection: Use Android `ConnectivityManager.NetworkCallback` to trigger sync of pending records.

### Quota management
- Maintain a `counters` document with `recordCount` field
- Increment on upload, decrement on delete/eviction
- On app launch, reconcile by querying actual count if counter seems stale (rare — defensive check)

### Security rules
```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    match /users/{userId}/{document=**} {
      allow read, write: if request.auth != null && request.auth.uid == userId;
    }
  }
}
```

### Alternatives considered
- Flat collection with UID field: No natural security boundary per user, complex rules → rejected
- Cloud Functions for eviction: Adds backend dependency (violates Constitution Principle II for core flow, and Principle VIII says no mandatory backend) → rejected
- Client-side count via `getAll()`: Expensive reads at scale → rejected in favor of counter document

---

## 4. Google Play Billing (One-Time Purchase)

### Decision
Use Google Play Billing Library 7.x for a single non-consumable in-app product.

### Rationale
- One-time purchase (non-consumable) is the simplest billing model
- Single SKU: `cloud_history_10k` — unlocks 10,000 cloud record capacity
- No subscription management, no server-side receipt validation needed for personal utility
- Purchase restoration via `queryPurchasesAsync()` handles reinstalls

### Key flow
```kotlin
// 1. Initialize
val billingClient = BillingClient.newBuilder(context)
    .setListener(purchasesUpdatedListener)
    .enablePendingPurchases()
    .build()

// 2. Connect
billingClient.startConnection(object : BillingClientStateListener { ... })

// 3. Query product
val params = QueryProductDetailsParams.newBuilder()
    .setProductList(listOf(
        QueryProductDetailsParams.Product.newBuilder()
            .setProductId("cloud_history_10k")
            .setProductType(BillingClient.ProductType.INAPP)
            .build()
    )).build()
val (result, productDetailsList) = billingClient.queryProductDetails(params)

// 4. Launch purchase
val flowParams = BillingFlowParams.newBuilder()
    .setProductDetailsParamsList(listOf(
        BillingFlowParams.ProductDetailsParams.newBuilder()
            .setProductDetails(productDetails)
            .build()
    )).build()
billingClient.launchBillingFlow(activity, flowParams)

// 5. Handle purchase in listener
// 6. Acknowledge purchase (required within 3 days or Google refunds)
billingClient.acknowledgePurchase(
    AcknowledgePurchaseParams.newBuilder()
        .setPurchaseToken(purchase.purchaseToken)
        .build()
)
```

### Purchase restoration
```kotlin
// On app launch, check for existing purchases
val params = QueryPurchasesParams.newBuilder()
    .setProductType(BillingClient.ProductType.INAPP)
    .build()
val result = billingClient.queryPurchasesAsync(params)
// If cloud_history_10k found in result → set tier to PAID
```

### Refund detection
- Google Play voids the purchase server-side
- On next `queryPurchasesAsync()`, the purchase won't be present → app reverts to free tier
- No Voided Purchases API needed for a personal utility — periodic purchase check is sufficient

### Dependencies
```toml
# libs.versions.toml additions
playBilling = "7.1.1"

play-billing = { group = "com.android.billingclient", name = "billing-ktx", version.ref = "playBilling" }
```

---

## 5. Module Architecture Decision

### Decision
Add a new `:core:cloud` module for Firebase + encryption + sync logic. Billing lives in `:core:billing`.

### Rationale
- `:core:data` should not depend on Firebase — it's currently a clean Room + MediaStore module
- Cloud sync is a cross-cutting concern that wraps the existing download repository
- Encryption is tightly coupled with cloud sync (no other use case) — keep together
- Billing is a separate concern with its own lifecycle — dedicated module

### Module dependency graph (new modules in bold)
```
:app → :feature:download, :feature:history, :core:data, :core:cloud, :core:billing
:feature:history → :core:domain, :core:ui, :core:cloud (for sync toggle/status)
:core:cloud → :core:domain (for DownloadRecord), :core:data (for DownloadDao read)
:core:billing → :core:domain (for CloudTier model)
:core:data → :core:domain
```

### Alternatives considered
- Everything in `:core:data`: Pollutes the module with Firebase dependencies, harder to test → rejected
- Single `:core:cloud-billing` module: Couples billing to cloud sync unnecessarily → rejected
- Cloud logic in `:feature:history`: Violates modular separation (feature modules shouldn't own data logic) → rejected

---

## 6. Sync State Tracking

### Decision
Add a `syncStatus` column to the existing `downloads` Room table and a new `SyncQueueEntity` table for pending operations.

### Rationale
- Adding `syncStatus` to `DownloadEntity` tracks whether each record has been backed up
- A separate `SyncQueueEntity` table queues pending operations (UPLOAD, DELETE) with retry metadata
- This avoids querying Firestore to determine what needs syncing — Room is the source of truth

### Schema additions
```kotlin
// Add to existing DownloadEntity
val syncStatus: String = "NOT_SYNCED"  // NOT_SYNCED, SYNCED, PENDING_DELETE

// New table
@Entity(tableName = "sync_queue")
data class SyncQueueEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val downloadId: Long,           // FK to downloads.id
    val operation: String,          // UPLOAD, DELETE
    val createdAt: Long,
    val retryCount: Int = 0,
    val lastError: String? = null,
)
```

### Alternatives considered
- Separate `cloud_records` table mirroring downloads: Duplication, harder to keep in sync → rejected
- Firestore-only tracking (check what's uploaded): Requires reading Firestore on every sync check, expensive → rejected
