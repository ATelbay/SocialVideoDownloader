---

description: "Task list template for feature implementation"
---

# Tasks: [FEATURE NAME]

**Input**: Design documents from `/specs/[###-feature-name]/`
**Prerequisites**: plan.md (required), spec.md (required for user stories), research.md, data-model.md, contracts/

**Tests**: Include unit-test tasks for use cases and ViewModels unless the feature is documentation-only.

**Organization**: Tasks are grouped by user story to enable independent implementation and testing of each story.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3)
- Include exact file paths in descriptions

## Path Conventions

- `app/src/main/...` for app entry points, navigation, and manifest changes
- `feature/<name>/src/main/...` and `feature/<name>/src/test/...` for feature UI, ViewModels, and feature-specific services
- `core/domain/src/main/...` and `core/domain/src/test/...` for domain models, repository interfaces, and use cases
- `core/data/src/main/...` and `core/data/src/test/...` for Room, MediaStore, repository implementations, and mappers
- `core/ui/src/main/...` for shared Compose UI and theme primitives
- `src/main/res/values/strings.xml` in the touched module for user-facing strings

<!-- 
  ============================================================================
  IMPORTANT: The tasks below are SAMPLE TASKS for illustration purposes only.
  
  The /speckit.tasks command MUST replace these with actual tasks based on:
  - User stories from spec.md (with their priorities P1, P2, P3...)
  - Feature requirements from plan.md
  - Entities from data-model.md
  - Endpoints from contracts/
  
  Tasks MUST be organized by user story so each story can be:
  - Implemented independently
  - Tested independently
  - Delivered as an MVP increment
  
  DO NOT keep these sample tasks in the generated tasks.md file.
  ============================================================================
-->

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Feature scaffolding and shared wiring

- [ ] T001 Create project structure per implementation plan
- [ ] T002 Add or update module dependencies required for the feature
- [ ] T003 [P] Wire navigation, DI entry points, or manifest declarations needed by the feature

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Core infrastructure that MUST be complete before ANY user story can be implemented

**⚠️ CRITICAL**: No user story work can begin until this phase is complete

Examples of foundational tasks (adjust based on your project):

- [ ] T004 Define or update shared domain models in `core/domain/src/main/...`
- [ ] T005 [P] Add repository interfaces and use cases in `core/domain/src/main/...`
- [ ] T006 [P] Add repository bindings, data sources, or Room migrations in `core/data/src/main/...`
- [ ] T007 Configure injected dispatchers, service hooks, or notification channels used across stories
- [ ] T008 Add shared error mapping and UI state contracts
- [ ] T009 Add required string resources and shared UI components

**Checkpoint**: Foundation ready - user story implementation can now begin in parallel

---

## Phase 3: User Story 1 - [Title] (Priority: P1) 🎯 MVP

**Goal**: [Brief description of what this story delivers]

**Independent Test**: [How to verify this story works on its own]

### Tests for User Story 1 ⚠️

> **NOTE: Write these tests FIRST, ensure they FAIL before implementation**

- [ ] T010 [P] [US1] Add use case test in `core/domain/src/test/kotlin/.../[UseCase]Test.kt`
- [ ] T011 [P] [US1] Add ViewModel test in `feature/[feature]/src/test/kotlin/.../[Feature]ViewModelTest.kt`

### Implementation for User Story 1

- [ ] T012 [P] [US1] Add or update domain model in `core/domain/src/main/kotlin/.../[Entity].kt`
- [ ] T013 [P] [US1] Implement use case or repository interface in `core/domain/src/main/kotlin/.../[UseCase].kt`
- [ ] T014 [US1] Implement repository/data source in `core/data/src/main/kotlin/.../[RepositoryImpl].kt`
- [ ] T015 [US1] Implement screen, state, or intent handling in `feature/[feature]/src/main/kotlin/.../[ScreenOrViewModel].kt`
- [ ] T016 [US1] Add validation, error mapping, and string resources
- [ ] T017 [US1] Wire navigation, service integration, or persistence needed for User Story 1

**Checkpoint**: At this point, User Story 1 should be fully functional and testable independently

---

## Phase 4: User Story 2 - [Title] (Priority: P2)

**Goal**: [Brief description of what this story delivers]

**Independent Test**: [How to verify this story works on its own]

### Tests for User Story 2 ⚠️

- [ ] T018 [P] [US2] Add use case or mapper test in `core/domain/src/test/kotlin/...` or `core/data/src/test/kotlin/...`
- [ ] T019 [P] [US2] Add ViewModel or UI-state test in `feature/[feature]/src/test/kotlin/...`

### Implementation for User Story 2

- [ ] T020 [P] [US2] Extend shared model, mapper, or UI contract in the owning module
- [ ] T021 [US2] Implement the feature logic in the relevant domain or data layer file
- [ ] T022 [US2] Implement the Compose UI or service behavior in `feature/[feature]/src/main/kotlin/...`
- [ ] T023 [US2] Integrate with User Story 1 flows without regressing independent behavior

**Checkpoint**: At this point, User Stories 1 AND 2 should both work independently

---

## Phase 5: User Story 3 - [Title] (Priority: P3)

**Goal**: [Brief description of what this story delivers]

**Independent Test**: [How to verify this story works on its own]

### Tests for User Story 3 ⚠️

- [ ] T024 [P] [US3] Add domain/data-layer test in the touched module test source set
- [ ] T025 [P] [US3] Add feature ViewModel or state transition test in `feature/[feature]/src/test/kotlin/...`

### Implementation for User Story 3

- [ ] T026 [P] [US3] Add or update supporting model, contract, or shared UI component
- [ ] T027 [US3] Implement the required domain/data behavior in the owning module
- [ ] T028 [US3] Implement the corresponding Compose UI, service, or navigation behavior

**Checkpoint**: All user stories should now be independently functional

---

[Add more user story phases as needed, following the same pattern]

---

## Phase N: Polish & Cross-Cutting Concerns

**Purpose**: Improvements that affect multiple user stories

- [ ] TXXX [P] Documentation updates in `specs/[###-feature-name]/quickstart.md` or module README content
- [ ] TXXX Code cleanup and refactoring
- [ ] TXXX Performance optimization across all stories
- [ ] TXXX [P] Additional unit tests in the touched module `src/test/kotlin/...`
- [ ] TXXX Validate manifest entries, permissions, and notification behavior
- [ ] TXXX Run quickstart.md validation

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies - can start immediately
- **Foundational (Phase 2)**: Depends on Setup completion - BLOCKS all user stories
- **User Stories (Phase 3+)**: All depend on Foundational phase completion
  - User stories can then proceed in parallel (if staffed)
  - Or sequentially in priority order (P1 → P2 → P3)
- **Polish (Final Phase)**: Depends on all desired user stories being complete

### User Story Dependencies

- **User Story 1 (P1)**: Can start after Foundational (Phase 2) - No dependencies on other stories
- **User Story 2 (P2)**: Can start after Foundational (Phase 2) - May integrate with US1 but should be independently testable
- **User Story 3 (P3)**: Can start after Foundational (Phase 2) - May integrate with US1/US2 but should be independently testable

### Within Each User Story

- Tests MUST be written and FAIL before implementation
- Domain contracts before repository implementations
- Repository and use case work before UI wiring
- Core implementation before integration
- Story complete before moving to next priority

### Parallel Opportunities

- All Setup tasks marked [P] can run in parallel
- All Foundational tasks marked [P] can run in parallel (within Phase 2)
- Once Foundational phase completes, all user stories can start in parallel (if team capacity allows)
- All tests for a user story marked [P] can run in parallel
- Models within a story marked [P] can run in parallel
- Different user stories can be worked on in parallel by different team members

---

## Parallel Example: User Story 1

```bash
# Launch all tests for User Story 1 together:
Task: "Add use case test in core/domain/src/test/kotlin/.../[UseCase]Test.kt"
Task: "Add ViewModel test in feature/[feature]/src/test/kotlin/.../[Feature]ViewModelTest.kt"

# Launch all models for User Story 1 together:
Task: "Add or update domain model in core/domain/src/main/kotlin/.../[Entity1].kt"
Task: "Add or update domain model in core/domain/src/main/kotlin/.../[Entity2].kt"
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete Phase 1: Setup
2. Complete Phase 2: Foundational (CRITICAL - blocks all stories)
3. Complete Phase 3: User Story 1
4. **STOP and VALIDATE**: Test User Story 1 independently
5. Deploy/demo if ready

### Incremental Delivery

1. Complete Setup + Foundational → Foundation ready
2. Add User Story 1 → Test independently → Deploy/Demo (MVP!)
3. Add User Story 2 → Test independently → Deploy/Demo
4. Add User Story 3 → Test independently → Deploy/Demo
5. Each story adds value without breaking previous stories

### Parallel Team Strategy

With multiple developers:

1. Team completes Setup + Foundational together
2. Once Foundational is done:
   - Developer A: User Story 1
   - Developer B: User Story 2
   - Developer C: User Story 3
3. Stories complete and integrate independently

---

## Notes

- [P] tasks = different files, no dependencies
- [Story] label maps task to specific user story for traceability
- Each user story should be independently completable and testable
- Verify tests fail before implementing
- Commit after each task or logical group
- Stop at any checkpoint to validate story independently
- Avoid: vague tasks, same file conflicts, cross-story dependencies that break independence
