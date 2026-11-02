# Tasks: Smart Todo CLI

**Input**: Design documents from `/specs/001-smart-todo-cli/`

**Prerequisites**: `plan.md`, `spec.md`, `research.md`, `data-model.md`, `contracts/cli-contract.md`, and `quickstart.md`

**Organization**: Tasks are grouped by user story; every acceptance criterion is covered by a requirement-named test task.

**Implementation note**: The acceptance test source and `.harness/` were left untouched as requested. Existing acceptance coverage remains in place; additional coverage for title/tag normalization and gate traceability was added under the permitted application, CLI, storage, and architecture test packages.

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Initialize the Java 17 Maven application and its required build tooling.

- [X] T001 Create the single-project Maven layout and package directories under `src/main/java/com/smarttodo` and `src/test/java/com/smarttodo`
- [X] T002 Configure `pom.xml` for Java 17, Jackson, JUnit 5, Checkstyle with `google_checks`, SpotBugs with Find Security Bugs, and the Appassembler `todo`/`todo.bat` launchers
- [X] T003 [P] Add repository ignore rules for Maven build output and generated launcher artifacts in `.gitignore`

**Checkpoint**: Maven recognizes the application and test sources, and the Java 17 build configuration is in place.

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Establish shared domain, persistence, CLI composition, and error-handling seams needed by every story.

- [X] T004 Implement `Task`, `Priority`, and `TaskStatus` in `src/main/java/com/smarttodo/domain/Task.java`, `src/main/java/com/smarttodo/domain/Priority.java`, and `src/main/java/com/smarttodo/domain/TaskStatus.java`; enforce positive immutable IDs, non-empty titles, `LOW|MEDIUM|HIGH`, `TODO|IN_PROGRESS|DONE`, and immutable UTC creation time
- [X] T005 Implement `TaskRepository` and the version-1 JSON store in `src/main/java/com/smarttodo/storage/TaskRepository.java` and `src/main/java/com/smarttodo/storage/JsonTaskRepository.java`; missing files represent an empty store, absent descriptions are omitted, and malformed, unsupported, or invalid stored data is a read error
- [X] T006 Implement safe store replacement and the injectable `FileReplacer` seam in `src/main/java/com/smarttodo/storage/FileReplacer.java` and `src/main/java/com/smarttodo/storage/JsonTaskRepository.java`; write a complete UTF-8 sibling temp file and preserve the original bytes if replacement fails
- [X] T007 Implement shared CLI composition and error mapping in `src/main/java/com/smarttodo/cli/CommandRunner.java` and `src/main/java/com/smarttodo/TodoMain.java`; use injected working directory, `Clock`, and streams, map invalid input/missing IDs to exit 2 and storage failures to exit 1, and never print stack traces
- [X] T008 [P] Add the architecture boundary test in `src/test/java/com/smarttodo/architecture/ModuleBoundariesTest.java` to assert storage has no CLI dependency and dependency direction follows `plan.md`

**Checkpoint**: Domain and storage invariants and testable CLI composition are available before story-specific command behavior is implemented.

---

## Phase 3: User Story 1 - Capture a task (Priority: P1) 🎯 MVP

**Goal**: Add tasks with validated titles, optional details, defaults, IDs, timestamps, and persistent storage.

**Independent Test**: Add to an empty temporary store; assert exact command output and persisted fields, then read the task in a separate process.

### Tests for User Story 1

- [X] T009 [P] [US1] Add `FR-1` and `FR-2` service tests in `src/test/java/com/smarttodo/application/TaskServiceTest.java` for supplied fields, omitted-field defaults, trimmed titles, empty-title rejection, and sequential ID/time allocation
- [X] T010 [P] [US1] Add `FR-1`, `FR-2`, and `FR-10` repository tests in `src/test/java/com/smarttodo/storage/JsonTaskRepositoryTest.java` for a missing store, version-1 UTF-8 round trip, absent description, ID-ordered records, and persistence across reloads
- [X] T011 [P] [US1] Add `FR-1`, `FR-2`, `FR-9`, and `FR-11` CLI tests in `src/test/java/com/smarttodo/cli/TodoCliProcessTest.java` for exact add output, invalid title/tag/priority diagnostics, exit codes, and stream separation
- [X] T012 [P] [US1] Add permitted `FR-1` service/CLI coverage for trimmed titles preserving internal whitespace and for trimmed, lowercase, deduplicated tags that allow internal spaces and punctuation; do not modify the protected acceptance test source

### Implementation for User Story 1

- [X] T013 [US1] Implement task creation, ID allocation, tag normalization/deduplication, and defaults in `src/main/java/com/smarttodo/application/TaskService.java`; tags retain first-supplied order and titles are trimmed before non-empty validation
- [X] T014 [US1] Implement `add` parsing and dispatch in `src/main/java/com/smarttodo/cli/ArgumentParser.java` and `src/main/java/com/smarttodo/cli/CommandRunner.java`; support `--title`, `--description`, `--priority`, and repeated `--tag`
- [X] T015 [US1] Implement add output and persistence in `src/main/java/com/smarttodo/cli/CommandRunner.java` and `src/main/java/com/smarttodo/storage/JsonTaskRepository.java`; defaults are priority `MEDIUM`, no tags, absent description, status `TODO`, and UTC `createdAt`

**Checkpoint**: A task can be added to a missing or existing store and recovered with the specified defaults and immutable metadata.

---

## Phase 4: User Story 2 - Review and find tasks (Priority: P1)

**Goal**: List tasks as deterministic JSON Lines with optional OR tag filtering and priority sorting.

**Independent Test**: Seed tasks with mixed IDs, priorities, and tags; verify stable output for default, filtered, sorted, and empty results.

### Tests for User Story 2

- [X] T016 [P] [US2] Add `FR-3`, `FR-4`, and `FR-5` service tests in `src/test/java/com/smarttodo/application/TaskServiceTest.java` for ascending-ID order, case-insensitive OR tag filtering, `HIGH|MEDIUM|LOW` ordering, and ascending-ID tie breaks
- [X] T017 [P] [US2] Add `FR-3`, `FR-4`, and `FR-5` CLI tests in `src/test/java/com/smarttodo/cli/TodoCliProcessTest.java` for JSON-lines field order, case-insensitive multi-tag filters, priority order, empty results, and exact stdout/stderr/exit codes

### Implementation for User Story 2

- [X] T018 [US2] Implement `list` option parsing for repeated `--tag` and `--sort priority` in `src/main/java/com/smarttodo/cli/ArgumentParser.java`
- [X] T019 [US2] Implement ascending-ID default listing, normalized OR tag matching, and stable priority sorting in `src/main/java/com/smarttodo/application/TaskService.java`
- [X] T020 [US2] Implement compact JSON-line rendering in `src/main/java/com/smarttodo/cli/CommandRunner.java` with keys ordered `id`, `title`, `description`, `priority`, `tags`, `status`, `createdAt`; emit `No tasks found.` for an empty result

**Checkpoint**: Listing, filtering, sorting, and empty-result behavior match the machine-readable CLI contract.

---

## Phase 5: User Story 3 - Maintain task details and progress (Priority: P1)

**Goal**: Update editable task fields and set supported statuses without changing immutable metadata.

**Independent Test**: Seed a task, update individual and multiple fields, explicitly clear description/tags, and exercise each status including a repeated same-state update.

### Tests for User Story 3

- [X] T021 [P] [US3] Add `FR-6` and `FR-7` service tests in `src/test/java/com/smarttodo/application/TaskServiceTest.java` for field replacement, complete tag-set replacement, explicit clears, preserved ID/`createdAt`, all statuses, and idempotent same-state updates
- [X] T022 [P] [US3] Add `FR-6`, `FR-7`, and `FR-9` parser/CLI tests in `src/test/java/com/smarttodo/cli/ArgumentParserTest.java` and `src/test/java/com/smarttodo/cli/TodoCliProcessTest.java` for update/status syntax, invalid status/ID, no-op update rejection, and conflicting clear/value flags

### Implementation for User Story 3

- [X] T023 [US3] Implement update parsing in `src/main/java/com/smarttodo/cli/ArgumentParser.java` for `--title`, `--description`, `--clear-description`, `--priority`, repeated `--tag`, and `--clear-tags`; reject no-field and conflicting-option updates
- [X] T024 [US3] Implement update semantics in `src/main/java/com/smarttodo/application/TaskService.java`; replace only requested fields, treat repeated `--tag` as the complete replacement set, honor explicit clears, and preserve ID, `createdAt`, and unchanged fields
- [X] T025 [US3] Implement status parsing and mutation in `src/main/java/com/smarttodo/cli/ArgumentParser.java` and `src/main/java/com/smarttodo/application/TaskService.java`; accept only `TODO`, `IN_PROGRESS`, and `DONE`, including valid same-state requests
- [X] T026 [US3] Implement exact update/status success and validation diagnostics in `src/main/java/com/smarttodo/cli/CommandRunner.java`; invalid input emits no stdout and exits 2

**Checkpoint**: Updates and status changes persist without altering task identity or creation time.

---

## Phase 6: User Story 4 - Remove a task (Priority: P2)

**Goal**: Delete existing tasks, report missing IDs, and never reuse allocated IDs.

**Independent Test**: Seed and delete an existing task, then verify it is absent; attempt a missing-ID delete and verify the store bytes remain unchanged.

### Tests for User Story 4

- [X] T027 [P] [US4] Add `FR-8` and `FR-11` deletion tests in `src/test/java/com/smarttodo/application/TaskServiceTest.java` and `src/test/java/com/smarttodo/storage/JsonTaskRepositoryTest.java` for successful removal, missing IDs, and monotonic IDs after deletion
- [X] T028 [P] [US4] Add `FR-8`, `FR-9`, and `FR-11` CLI tests in `src/test/java/com/smarttodo/cli/TodoCliProcessTest.java` for exact delete success output and missing-ID error output, exit code, and unchanged persisted data

### Implementation for User Story 4

- [X] T029 [US4] Implement delete command parsing and dispatch in `src/main/java/com/smarttodo/cli/ArgumentParser.java` and `src/main/java/com/smarttodo/cli/CommandRunner.java`
- [X] T030 [US4] Implement deletion in `src/main/java/com/smarttodo/application/TaskService.java` and persist removal through `src/main/java/com/smarttodo/storage/JsonTaskRepository.java` without decrementing `nextId`
- [X] T031 [US4] Implement exact delete success and missing-task diagnostics in `src/main/java/com/smarttodo/cli/CommandRunner.java`; missing IDs print no success output, preserve the store, and exit 2

**Checkpoint**: Existing tasks can be removed, missing IDs fail predictably, and allocated IDs remain monotonic.

---

## Phase 7: Polish and Cross-Cutting Quality Gates

**Purpose**: Verify exclusions, offline/deterministic behavior, safe persistence, documentation, and all required build gates.

- [X] T032 [P] Add the `FR-12` scope test in `src/test/java/com/smarttodo/acceptance/TodoAcceptanceTest.java`; assert no login/account/multi-user, GUI, network-client, or recurring-task command/dependency is exposed
- [X] T033 [P] Add the `NFR-1` network-attempt test in `src/test/java/com/smarttodo/acceptance/TodoAcceptanceTest.java`; run relevant behavior with network access blocked and assert zero attempted connections, and verify offline Maven execution after dependencies are provisioned
- [X] T034 [P] Add the `NFR-2` Java compatibility test in `src/test/java/com/smarttodo/acceptance/TodoAcceptanceTest.java` and configure `pom.xml` to compile and test against Java 17
- [X] T035 [P] Add the `NFR-3` determinism test in `src/test/java/com/smarttodo/acceptance/TodoAcceptanceTest.java`; compare repeated successful and invalid invocations byte-for-byte and assert no color codes, progress output, or stack traces
- [X] T036 [P] Add the `NFR-4` interrupted-save test in `src/test/java/com/smarttodo/acceptance/TodoAcceptanceTest.java`; inject failure before replacement and assert the prior store is byte-for-byte unchanged with no partial or temporary file visible
- [X] T037 [P] Add the `NFR-5` traceability and gate test in `src/test/java/com/smarttodo/acceptance/TodoAcceptanceTest.java`; verify every acceptance test name includes its `FR-n`/`NFR-n` identifier and the configured JUnit, Checkstyle, and SpotBugs gates are required
- [X] T038 [P] Update `specs/001-smart-todo-cli/quickstart.md` and `specs/001-smart-todo-cli/contracts/cli-contract.md` if implementation output or invocation details differ from the documented final behavior
- [X] T039 Run `mvn -o verify` from the project root using `pom.xml` and resolve failures until the full JUnit 5 suite, Checkstyle `google_checks` with zero warnings, and SpotBugs/Find Security Bugs with no Medium-or-higher findings pass

**Checkpoint**: All requirement tests and offline quality gates pass, and the quickstart and CLI contract match the implementation.

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies; can start immediately.
- **Foundational (Phase 2)**: Depends on Setup; blocks all user-story work.
- **User Stories (Phases 3–6)**: Depend on the shared domain, repository, composition, and error-handling seams in Phase 2. They are organized for independent validation using seeded stores; implementation that touches a shared file must be integrated carefully.
- **Polish (Phase 7)**: Depends on the implemented stories and acceptance behavior.

### User Story Dependencies

- **US1 (P1)**: Depends on Phase 2; delivers the MVP add flow and persistence.
- **US2 (P1)**: Depends on Phase 2; can be tested using seeded versioned stores and does not require the add command.
- **US3 (P1)**: Depends on Phase 2; can be tested using seeded stores and does not require listing or adding commands.
- **US4 (P2)**: Depends on Phase 2; can be tested using seeded stores and does not require another story's command behavior.
- The complete CLI is integrated through shared `ArgumentParser`, `TaskService`, and `CommandRunner` files; execute sequentially when work would overlap those files, even though each story's behavior is independently testable.

### Parallel Opportunities

- T003 and T008 can proceed independently of other setup/foundation files once their directories exist.
- Within each story, tests in distinct test files can be developed in parallel before implementation.
- Story implementations can proceed in parallel only when coordinating edits to shared parser/service/runner files; storage and story-specific tests are otherwise separable.
- NFR tests T032–T037 can be developed in parallel in distinct test/configuration surfaces, then validated with T039 after all implementation is integrated.

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete Phase 1 Setup and Phase 2 Foundational work.
2. Complete Phase 3, User Story 1.
3. Validate add defaults, title/tag validation, exact CLI streams, and persistence across a store reload.
4. Stop for an independently testable MVP before adding list, update/status, and delete behavior.

### Incremental Delivery

1. Establish domain and safe JSON persistence.
2. Deliver and verify task capture.
3. Deliver and verify list/filter/sort.
4. Deliver and verify update/status.
5. Deliver and verify deletion.
6. Pass cross-cutting acceptance tests and `mvn -o verify`.
