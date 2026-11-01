# Tasks: Smart Todo CLI

**Input**: Design documents from `/specs/001-smart-todo-cli/`

**Prerequisites**: plan.md (required), spec.md (required for user stories), research.md, data-model.md, contracts/

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Initialize the Java CLI project and shared build/test tooling.

- [ ] T001 Create the Maven project skeleton and package layout under `src/main/java/com/smarttodo` and `src/test/java/com/smarttodo`
- [ ] T002 Configure Maven with Java 17, JUnit 5, Jackson, Checkstyle, SpotBugs, Find Security Bugs, and Appassembler for the `todo` launcher
- [ ] T003 [P] Add the baseline project configuration and `.gitignore`/source-folder conventions needed for the CLI build

**Checkpoint**: The project compiles, dependency wiring is correct, and the CLI entry point is ready for implementation.

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Establish the domain, storage, and CLI foundation before user-story work begins.

- [ ] T004 Implement the domain model in `src/main/java/com/smarttodo/domain/Task.java`, `Priority.java`, and `TaskStatus.java` with validation, defaults, tag normalization, and immutable creation metadata
- [ ] T005 Implement the persisted JSON store contract in `storage/TaskRepository.java` and `storage/JsonTaskRepository.java` with missing-file handling, validation, atomic safe-save, and versioned JSON support
- [ ] T006 Implement command parsing and command metadata in `src/main/java/com/smarttodo/cli/Command.java` and `ArgumentParser.java` for add/list/update/status/delete syntax
- [ ] T007 Implement the application service in `src/main/java/com/smarttodo/application/TaskService.java` for creation, listing, updates, status changes, filtering, sorting, and deletion rules
- [ ] T008 Implement the CLI entrypoint in `TodoMain.java` and `CommandRunner.java` to map validation/storage errors to the exact stderr/exit-code contract without stack traces
- [ ] T009 [P] Add a module-boundary architecture test asserting `storage` does not depend on `cli` and that the dependency direction matches the plan

**Checkpoint**: All shared abstractions and data rules are in place; user-story tasks can proceed in parallel.

---

## Phase 3: User Story 1 - Capture a task (Priority: P1) 🎯 MVP

**Goal**: Add tasks with defaults and persisted metadata.

**Independent Test**: Add tasks to an empty store, verify defaults, and confirm persistence across process runs.

### Tests for User Story 1

- [ ] T010 [P] [US1] Add unit tests for `FR-1` and `FR-2` in `src/test/java/com/smarttodo/application/TaskServiceTest.java` covering add defaults, title validation, optional fields, and ID allocation
- [ ] T011 [P] [US1] Add storage tests in `src/test/java/com/smarttodo/storage/JsonTaskRepositoryTest.java` covering missing file, versioned JSON round trip, absent description, and monotonic IDs
- [ ] T012 [P] [US1] Add CLI subprocess tests in `src/test/java/com/smarttodo/cli/TodoCliProcessTest.java` for successful add output and persistence across runs

### Implementation for User Story 1

- [ ] T013 [P] [US1] Implement add-command validation and output in `cli/ArgumentParser.java` and `CommandRunner.java`
- [ ] T014 [P] [US1] Implement task creation in `TaskService` and repository save flow for `title`, `description`, `priority`, `tags`, `status`, and `createdAt`
- [ ] T015 [US1] Ensure add operations set the default priority to `MEDIUM`, empty tag list, absent description, status `TODO`, and UTC `createdAt` values
- [ ] T016 [US1] Validate the CLI add contract for empty title, bad tag syntax, invalid priority, and read/save failure behavior

**Checkpoint**: Adding tasks is fully functional and independently testable.

---

## Phase 4: User Story 2 - Review and find tasks (Priority: P1)

**Goal**: List tasks in deterministic order with tag-filtering and priority sorting.

**Independent Test**: Seed multiple tasks with mixed priorities and tags; verify default ordering, filtering, sorting, and empty results.

### Tests for User Story 2

- [ ] T017 [P] [US2] Add list/filter/sort tests in `TaskServiceTest.java` for `FR-3`, `FR-4`, and `FR-5` with case-insensitive tag matching and priority ordering
- [ ] T018 [P] [US2] Add CLI contract tests for JSON-lines output ordering, `No tasks found.` on empty result, and tag-filter success behavior

### Implementation for User Story 2

- [ ] T019 [P] [US2] Implement list handling in `CommandRunner` and `ArgumentParser` with `--tag` and `--sort priority` parsing
- [ ] T020 [US2] Implement `TaskService.listTasks` filtering logic using OR semantics across tags and case-insensitive comparison
- [ ] T021 [US2] Implement stable priority ordering (`HIGH`, `MEDIUM`, `LOW`) with ascending ID as the tie-breaker and default ascending ID ordering
- [ ] T022 [US2] Ensure JSON output serializes in the required key order: `id`, `title`, `description`, `priority`, `tags`, `status`, `createdAt`
- [ ] T023 [US2] Verify empty and no-match list results output `No tasks found.` and exit successfully

**Checkpoint**: Listing, filtering, and sorting behavior match the output contract and requirements.

---

## Phase 5: User Story 3 - Maintain task details and progress (Priority: P1)

**Goal**: Update task fields and move tasks between statuses with strict validation.

**Independent Test**: Update fields, clear description/tags, and set status through `TODO`, `IN_PROGRESS`, and `DONE` on an existing task.

### Tests for User Story 3

- [ ] T024 [P] [US3] Add `TaskServiceTest` coverage for `FR-6` and `FR-7`, including field replacement semantics, status transitions, immutable IDs/timestamps, and explicit clear behavior
- [ ] T025 [P] [US3] Add CLI tests for invalid status, invalid ID, updated results, and same-state no-op status transitions

### Implementation for User Story 3

- [ ] T026 [P] [US3] Implement update-command parsing in `ArgumentParser` for `--title`, `--description`, `--clear-description`, `--priority`, `--tag`, and `--clear-tags`
- [ ] T027 [US3] Implement `TaskService.updateTask` to preserve ID and `createdAt`, replace only requested fields, and honor explicit clear operations
- [ ] T028 [US3] Implement status-setting logic in `TaskService` and `CommandRunner` for `TODO`, `IN_PROGRESS`, and `DONE`, allowing idempotent same-state updates
- [ ] T029 [US3] Validate conflict handling for invalid combinations such as `--description` with `--clear-description` and `--tag` with `--clear-tags`

**Checkpoint**: Task details and status can be maintained without corrupting persisted records.

---

## Phase 6: User Story 4 - Remove a task (Priority: P2)

**Goal**: Delete tasks and enforce missing-ID behavior.

**Independent Test**: Delete an existing task and confirm missing-ID errors fail without mutating the file.

### Tests for User Story 4

- [ ] T030 [P] [US4] Add tests for successful deletion, missing task lookup, and ID monotonicity after deletion in `TaskServiceTest` and `JsonTaskRepositoryTest`
- [ ] T031 [P] [US4] Add CLI process tests for `todo delete` success and `Error: task #<id> not found.` failures

### Implementation for User Story 4

- [ ] T032 [P] [US4] Implement delete command parsing and output in `ArgumentParser` and `CommandRunner`
- [ ] T033 [US4] Implement `TaskService.deleteTask` and repository removal while retaining `nextId` and never reusing IDs
- [ ] T034 [US4] Ensure failed delete operations report `Error: task #<id> not found.` on stderr with exit code 2 and no success output

**Checkpoint**: The task lifecycle supports creation, review, updates, status changes, and deletion end-to-end.

---

## Phase 7: Quality Gates and Final Validation

**Purpose**: Verify the feature against the full offline build, style, and security gate requirements.

- [ ] T035 [P] Run the JUnit 5 suite for the full CLI feature and confirm acceptance tests include the stable requirement IDs (`FR-n` / `NFR-n`)
- [ ] T036 [P] Run `mvn -o verify` and confirm the full project passes Checkstyle (`google_checks`, zero warnings), SpotBugs/Find Security Bugs (no Medium-or-higher findings), and the test suite
- [ ] T037 [P] Validate the subprocess-level CLI contract for exact stdout/stderr separation, exit codes, stable messages, and absence of stack traces
- [ ] T038 [P] Update the feature documentation and quickstart examples to match the final behavior and confirm all docs remain consistent with the implementation

**Checkpoint**: The CLI is production-ready for the v1-local scope, with quality gates passing offline.

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies; can start immediately.
- **Foundational (Phase 2)**: Depends on Setup completion; blocks all user-story work.
- **User Stories (Phase 3+)**: All depend on Foundational completion and can proceed in parallel if staffed.
- **Quality Gates (Phase 7)**: Depends on all stories being complete.

### User Story Dependencies

- **User Story 1 (P1)**: Add-task flow is the foundation for all persistence and CLI semantics.
- **User Story 2 (P1)**: Depends on storage and add semantics; validates listing and ordering contracts.
- **User Story 3 (P1)**: Depends on list and add flows; validates updates and status transitions.
- **User Story 4 (P2)**: Depends on repository and task lifecycle semantics; validates delete and error handling.

### Parallel Opportunities

- All `[P]` tasks can be run concurrently within each phase.
- Once foundational work is complete, all user stories can be implemented in parallel by multiple engineers or sequentially if the team is small.
- Quality-gate tasks are best executed only after the full task lifecycle is implemented and validated.

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete Phase 1: Setup.
2. Complete Phase 2: Foundational.
3. Complete Phase 3: User Story 1.
4. Stop and validate the add flow before moving to list, update, and delete.

### Incremental Delivery Sequence

1. Implement domain and repository invariants.
2. Implement add-lifecycle validation and persistence.
3. Implement list/filter/sort and CLI output formatting.
4. Implement update/status semantics and explicit clear operations.
5. Implement delete and error-path handling.
6. Validate the full offline build and quality gates.
