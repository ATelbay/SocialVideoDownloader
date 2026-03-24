# Tasks: APK Size Optimization

**Input**: Design documents from `/specs/010-apk-size-optimization/`
**Prerequisites**: plan.md (required), spec.md (required for user stories), research.md

**Tests**: No new unit tests required — this feature modifies only build configuration files. Existing tests must pass (FR-009).

**Organization**: Tasks are grouped by user story to enable independent implementation and testing of each story.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3)
- Include exact file paths in descriptions

## Path Conventions

- `app/build.gradle.kts` — ABI filters, R8 enablement, dependency cleanup
- `app/proguard-rules.pro` — Keep rules for R8 minification

---

## Phase 1: User Story 1 - Faster Debug Build Installs (Priority: P1) 🎯 MVP

**Goal**: Reduce debug APK from ~230MB to ~90-110MB by filtering native libraries to arm64-v8a + x86_64 only

**Independent Test**: Run `./gradlew assembleDebug` and verify APK size dropped ~50%. Inspect APK to confirm only arm64-v8a and x86_64 native libraries are present.

### Implementation for User Story 1

- [ ] T001 [US1] Add `ndk { abiFilters += setOf("arm64-v8a", "x86_64") }` inside `defaultConfig { }` in `app/build.gradle.kts`

**Checkpoint**: Debug APK should now be ~90-110MB. Run `./gradlew assembleDebug` and check size with `ls -lh app/build/outputs/apk/debug/app-debug.apk`. Verify packaging { } block is untouched.

---

## Phase 2: User Story 2 - Minimal Release APK (Priority: P1)

**Goal**: Enable R8 minification and single-ABI filtering for release builds, reducing release APK to ~60-80MB

**Independent Test**: Verify release buildType has `isMinifyEnabled = true`, `isShrinkResources = true`, and `ndk.abiFilters` set to arm64-v8a only. (Full release build verification is optional — requires signing key.)

### Implementation for User Story 2

- [ ] T002 [US2] Enable R8 minification: change `isMinifyEnabled = false` to `isMinifyEnabled = true` and add `isShrinkResources = true` in the `release` buildType in `app/build.gradle.kts`
- [ ] T003 [US2] Add release-only ABI filter: add `ndk { abiFilters.clear(); abiFilters += setOf("arm64-v8a") }` inside the `release` buildType in `app/build.gradle.kts`
- [ ] T004 [US2] Write ProGuard/R8 keep rules in `app/proguard-rules.pro` for youtubedl-android, Room, Firebase, Play Billing, Kotlin serialization, Hilt, and source file/line number attributes

**Checkpoint**: `app/build.gradle.kts` release block should have minification + resource shrinking + arm64-v8a-only ABI filter. `app/proguard-rules.pro` should have all required keep rules.

---

## Phase 3: User Story 3 - Clean Dependency Graph (Priority: P2)

**Goal**: Remove duplicate youtubedl-android dependency declarations from :app to clarify module boundaries

**Independent Test**: Run `./gradlew :app:dependencies --configuration debugRuntimeClasspath | grep youtubedl` and verify all three artifacts still resolve transitively through :core:data.

### Implementation for User Story 3

- [ ] T005 [US3] Remove the three duplicate youtubedl-android dependency lines (`libs.youtubedl.android.library`, `libs.youtubedl.android.ffmpeg`, `libs.youtubedl.android.aria2c`) from the `dependencies { }` block in `app/build.gradle.kts`

**Checkpoint**: Build should still succeed. Run `./gradlew :app:dependencies --configuration debugRuntimeClasspath | grep youtubedl` to confirm transitive resolution.

---

## Phase 4: Verification & Polish

**Purpose**: Validate all changes together and confirm success criteria

- [ ] T006 Run `./gradlew assembleDebug` and verify build succeeds
- [ ] T007 Run `./gradlew test` and verify all existing unit tests pass
- [ ] T008 Verify debug APK size is ~90-110MB (check `app/build/outputs/apk/debug/app-debug.apk`)
- [ ] T009 Verify the `packaging { }` block in `app/build.gradle.kts` is unchanged: `jniLibs.useLegacyPackaging = true` and `keepDebugSymbols` for `*.zip.so` are still present (FR-006)
- [ ] T010 Install debug APK on an arm64-v8a device or emulator, launch the app, and confirm video extraction + download completes successfully (SC-005)
- [ ] T011 Install debug APK on an x86_64 emulator, launch the app, and confirm all features work (SC-006)

**Checkpoint**: All success criteria (SC-001 through SC-006) validated. Feature complete.

---

## Dependencies & Execution Order

### Phase Dependencies

- **Phase 1 (US1)**: No dependencies — can start immediately
- **Phase 2 (US2)**: No dependency on Phase 1 (different sections of build.gradle.kts), but logically follows
- **Phase 3 (US3)**: No dependency on Phases 1-2 (removes lines, doesn't conflict)
- **Phase 4 (Verification)**: Depends on ALL previous phases being complete

### User Story Dependencies

- **User Story 1 (P1)**: Independent — ABI filter in defaultConfig
- **User Story 2 (P1)**: Independent — R8 config + keep rules in release buildType + proguard-rules.pro
- **User Story 3 (P2)**: Independent — removes dependency lines

### Parallel Opportunities

- T001, T004, and T005 touch different sections of `app/build.gradle.kts` and could theoretically be parallelized, but since they modify the same file, sequential execution is safer
- T002 and T003 modify the same `release` buildType block — must be sequential or combined
- T004 modifies `app/proguard-rules.pro` (different file) — could run in parallel with T001-T003

### Parallel Example

```bash
# These can run in parallel (different files):
Task T001: "Add ABI filters in app/build.gradle.kts defaultConfig"
Task T004: "Write keep rules in app/proguard-rules.pro"
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete T001 (ABI filtering)
2. **STOP and VALIDATE**: `./gradlew assembleDebug` + check APK size
3. This alone delivers ~50% size reduction — the biggest win

### Full Delivery

1. T001 → ABI filtering (debug ~110MB)
2. T002-T004 → R8 minification + keep rules (release ~70MB)
3. T005 → Dependency cleanup (maintainability)
4. T006-T008 → Final verification

---

## Notes

- All tasks modify only 2 files in the :app module: `build.gradle.kts` and `proguard-rules.pro`
- Do NOT touch the existing `packaging { }` block (FR-006)
- Do NOT run `assembleRelease` without a signing key
- Commit after each phase for clean git history
