# Implementation Plan: Cloud History Backup

**Branch**: `009-cloud-history-backup` | **Date**: 2026-03-21 | **Spec**: [spec.md](spec.md)
**Input**: Feature specification from `/specs/009-cloud-history-backup/spec.md`

## Summary

Add optional cloud backup for download history using Firebase Firestore with
on-device encryption (AES-256-GCM via Android Keystore), Firebase Anonymous Auth,
LRU eviction at tier limits (1,000 free / 10,000 paid), and Google Play
Billing for a single one-time upgrade purchase. Room remains the primary
local store; Firestore sync is opt-in and one-directional (local → cloud)
with manual cloud → local restore.

## Technical Context

**Language/Version**: Kotlin 2.2.10
**Primary Dependencies**: Jetpack Compose (BOM 2026.03.00), Hilt 2.59.2, Room 2.8.4, Navigation Compose 2.9.7, Coil 2.7.0, Firebase BOM 33.15.0 (Auth + Firestore), Play Billing 7.1.1
**Storage**: Room (download history + sync queue), Firestore (encrypted cloud records), DataStore Preferences (backup settings)
**Testing**: JUnit5 + MockK + Turbine for Flow
**Target Platform**: Android 8.0+ (API 26)
**Project Type**: Mobile app (Android)
**Performance Goals**: Cloud sync within 30s of local save, restore ≤ 30s for 1,000 records, core download flow unaffected (≤ 5% overhead)
**Constraints**: On-device encryption mandatory, anonymous auth only, no backend for core flow, graceful degradation offline
**Scale/Scope**: Personal utility. 2 new modules (`:core:cloud`, `:core:billing`), 1 modified module (`:core:data` — migration), 1 modified feature (`:feature:history` — UI toggle/status/capacity). ~20 new Kotlin files. Encryption uses raw Android Keystore AES-256-GCM (no Tink dependency).

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principle | Status | Notes |
|-----------|--------|-------|
| I. Privacy & Zero Bloat | PASS | Anonymous auth only (exception clause). On-device encryption before upload (exception clause). No analytics/tracking. |
| II. On-Device Architecture | PASS | Core download flow remains fully on-device. Cloud sync is additive, not a dependency. |
| III. Modern Android Stack | PASS | Compose UI, KSP, Hilt, MVI. No kapt, no fragments, no XML. |
| IV. Modular Separation | PASS | New modules (`:core:cloud`, `:core:billing`) follow existing pattern. Interfaces in `:core:domain`, impls in new modules. |
| V. Minimal Friction UX | PASS | Single-tap backup toggle. No sign-in UI. |
| VI. Test Discipline | PASS | Unit tests planned for all use cases and ViewModel state transitions. |
| VII. Simplicity & Focus | PASS | Cloud sync is conditionally allowed (v2.0.0). Monetization is non-intrusive freemium (no ads, no subscriptions). |
| VIII. Optional Cloud Features | PASS | Opt-in only, zero-knowledge encryption, anonymous auth, graceful degradation, no mandatory backend. |

**Post-design re-check**: All principles still PASS after Phase 1 design. Two new modules (`:core:cloud`, `:core:billing`) are justified by separation of concerns — cloud/billing dependencies should not pollute `:core:data`.

## Project Structure

### Documentation (this feature)

```text
specs/009-cloud-history-backup/
├── plan.md              # This file
├── research.md          # Phase 0: technology decisions
├── data-model.md        # Phase 1: entities, schema, migrations
├── quickstart.md        # Phase 1: setup & validation guide
├── contracts/
│   └── cloud-sync-interfaces.md  # Phase 1: domain interfaces & security rules
├── checklists/
│   └── requirements.md  # Spec quality checklist
└── tasks.md             # Phase 2 output (/speckit.tasks command)
```

### Source Code (repository root)

```text
app/
└── src/main/
    └── kotlin/.../app/       # google-services plugin, Firebase lazy init

feature/
├── history/src/main/
│   └── kotlin/.../history/
│       └── ui/               # HistoryViewModel (add sync toggle, status, capacity)
│                             # CloudBackupToggle composable
│                             # CapacityBanner composable
│                             # RestoreDialog composable
└── history/src/test/
    └── kotlin/.../history/   # HistoryViewModel cloud backup tests

core/
├── domain/src/main/
│   └── kotlin/.../domain/
│       ├── model/            # CloudTier enum, SyncStatus sealed interface
│       ├── repository/       # CloudBackupRepository, BillingRepository interfaces
│       └── sync/             # SyncManager, CloudAuthService, EncryptionService, BackupPreferences interfaces
│                             # EnableCloudBackupUseCase, DisableCloudBackupUseCase
│                             # RestoreFromCloudUseCase, ObserveCloudCapacityUseCase
├── domain/src/test/          # Use case unit tests
├── data/src/main/
│   └── kotlin/.../data/
│       └── local/            # DownloadEntity migration (syncStatus column)
│                             # SyncQueueEntity + SyncQueueDao
├── data/src/test/
├── cloud/src/main/           # NEW MODULE
│   └── kotlin/.../cloud/
│       ├── auth/             # FirebaseCloudAuthService (impl of CloudAuthService)
│       ├── encryption/       # KeystoreEncryptionService (impl of EncryptionService)
│       ├── sync/             # FirestoreSyncManager (impl of SyncManager)
│       ├── repository/       # FirestoreCloudBackupRepository (impl)
│       └── di/               # CloudModule (Hilt bindings)
├── cloud/src/test/           # Cloud module unit tests
├── billing/src/main/         # NEW MODULE
│   └── kotlin/.../billing/
│       ├── PlayBillingRepository (impl of BillingRepository)
│       └── di/BillingModule (Hilt bindings)
├── billing/src/test/         # Billing module unit tests
└── ui/src/main/              # Shared composables (if any new shared components)
```

**Structure Decision**: Add `:core:cloud` and `:core:billing` as new modules.
`:core:data` is modified only for Room migration (syncStatus column + sync_queue table).
Firebase dependencies are isolated in `:core:cloud`. Encryption uses Android framework APIs (no external dependency).
Play Billing is isolated in `:core:billing`.

## Complexity Tracking

| Violation | Why Needed | Simpler Alternative Rejected Because |
|-----------|------------|--------------------------------------|
| Two new modules (`:core:cloud`, `:core:billing`) | Firebase + Play Billing are heavy dependencies that should not pollute `:core:data` or leak into feature modules | Putting everything in `:core:data` would couple Room-only code with Firebase/Billing, making builds slower and testing harder |
