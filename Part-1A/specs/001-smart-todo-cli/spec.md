# Feature Specification: Smart Todo CLI

**Feature Branch**: `001-smart-todo-cli`

**Created**: 2026-11-01

**Status**: Draft

**Input**: User description: "Build a Smart Todo CLI for one user that saves tasks in a local JSON file. Users can add, list, update and delete tasks. Each task has an id, title, optional description, priority (LOW, MEDIUM, HIGH), tags, status and created time. Users can filter the list by tag, sort the list by priority, and move a task between statuses TODO, IN_PROGRESS and DONE. Exclude login, multiple users, networking, a GUI and recurring tasks."

## User Scenarios & Testing

### User Story 1 - Capture a task (Priority: P1)

As the sole user, I can add a task with a title and optional details so I can record work I need to do.

**Why this priority**: Adding tasks is the entry point for all task-management journeys.

**Independent Test**: Add a task to an empty data file, then list it and inspect its persisted fields.

**Acceptance Scenarios**:

1. **Given** no tasks exist, **When** I add a task with a title, priority, and tag, **Then** the CLI prints the assigned ID and stores the task with status TODO and a UTC creation time.
2. **Given** no priority, description, or tags were supplied, **When** I add a task, **Then** the task uses the documented defaults and the description is absent.

### User Story 2 - Review and find tasks (Priority: P1)

As the sole user, I can list my tasks, optionally filter by tag, and optionally sort by priority so I can focus on relevant work.

**Why this priority**: Persisted tasks are useful only if I can reliably retrieve and inspect them.

**Independent Test**: Seed tasks with different priorities and tags, then check unfiltered, filtered, and sorted results.

**Acceptance Scenarios**:

1. **Given** tasks exist, **When** I list them, **Then** each task is printed with all its fields in a stable format.
2. **Given** tasks have tags, **When** I filter by one or more tags, **Then** only tasks matching at least one requested tag are printed.
3. **Given** tasks have different priorities, **When** I sort by priority, **Then** HIGH precedes MEDIUM and LOW, with IDs ascending within a priority.
4. **Given** no tasks match a filter, **When** I list them, **Then** the CLI reports that no tasks were found and exits successfully.

### User Story 3 - Maintain task details and progress (Priority: P1)

As the sole user, I can update a task's details and move it among TODO, IN_PROGRESS, and DONE so the list reflects its current state.

**Why this priority**: Task details and progress change as work proceeds.

**Independent Test**: Update each supported field and move a task through all three statuses; verify the persisted task after each command.

**Acceptance Scenarios**:

1. **Given** a task exists, **When** I update one or more editable fields, **Then** only the requested fields change and its ID and creation time remain unchanged.
2. **Given** a task exists, **When** I set its status to any supported status, **Then** the new status is persisted.
3. **Given** an optional description exists, **When** I explicitly clear it, **Then** the description is removed.

### User Story 4 - Remove a task (Priority: P2)

As the sole user, I can delete a task I no longer need so it no longer appears in my list.

**Why this priority**: Deletion keeps the active task list relevant, after the core add, review, and update journeys.

**Independent Test**: Add a task, delete it by ID, and verify that listing returns no tasks.

**Acceptance Scenarios**:

1. **Given** a task exists, **When** I delete it by ID, **Then** the CLI confirms deletion and the task is absent from subsequent lists.
2. **Given** the supplied ID does not exist, **When** I attempt to delete or update it, **Then** the CLI reports a not-found error without changing stored data.

### Edge Cases

- An empty task file or a filter with no matching tasks produces `No tasks found.` and exit code 0.
- A missing task ID, malformed command, invalid priority, invalid status, or invalid tag produces a concise error on stderr, no stack trace, and a non-zero exit code.
- A missing data file is treated as an empty list; it is created when the first successful add is saved.
- Invalid JSON or an unsupported data-file version is reported as a storage error; the file is not silently replaced or overwritten.
- A write failure reports a storage error and does not print a success message.
- Repeated tags on one task are stored once. Tags are case-insensitive and stored in lowercase.
- IDs are positive integers, allocated monotonically, and are not reused after deletion.
- Repeating a status transition to the task's current status is allowed and leaves the task unchanged.

## Requirements

### Functional Requirements

- **FR-1**: The CLI MUST add a task with a required non-empty title and optional description, priority, and repeated tags. It MUST assign an ID, set status to TODO, and record the creation time.
- **FR-2**: The CLI MUST default an omitted priority to MEDIUM, tags to an empty list, and description to absent.
- **FR-3**: The CLI MUST list all tasks in a stable, machine-readable, one-task-per-line format containing ID, title, description, priority, tags, status, and creation time.
- **FR-4**: The CLI MUST filter tasks by one or more tags. A task matches when it contains at least one requested tag; tag comparison MUST be case-insensitive.
- **FR-5**: The CLI MUST sort tasks by priority when requested, in HIGH, MEDIUM, LOW order, and sort equal-priority tasks by ascending ID. Without a sort option, tasks MUST be listed in ascending ID order.
- **FR-6**: The CLI MUST update a task's title, description, priority, or complete tag set by ID. Updating a task MUST preserve its ID and creation time. The description MUST be clearable explicitly.
- **FR-7**: The CLI MUST set a task's status by ID to TODO, IN_PROGRESS, or DONE.
- **FR-8**: The CLI MUST delete a task by ID.
- **FR-9**: The CLI MUST validate required values, priorities, statuses, tags, and task IDs. Invalid user input MUST be reported on stderr with exit code 2 and no stack trace; valid-command output MUST go to stdout.
- **FR-10**: The CLI MUST persist tasks across process runs in a local JSON file. A missing file MUST behave as an empty store. Invalid or unsupported JSON data MUST produce a storage error and MUST NOT be overwritten.
- **FR-11**: The CLI MUST report missing task IDs and storage failures on stderr, exit non-zero, and MUST NOT report success for a failed operation.
- **FR-12**: The product MUST be for one local user and MUST NOT provide login, multi-user support, networking, a GUI, or recurring tasks.

### Non-Functional Requirements

- **NFR-1**: The application and its tests MUST make no network requests or open network connections; after dependencies are provisioned, the Maven build and tests MUST be runnable offline.
- **NFR-2**: Production and test code MUST target Java 17 or higher and use Maven.
- **NFR-3**: CLI output MUST be deterministic: no color or progress output; valid command results go to stdout, errors to stderr, and errors MUST NOT include stack traces.
- **NFR-4**: JSON data MUST be UTF-8, versioned, and saved so a failed write does not leave a partially written task file.
- **NFR-5**: Every acceptance criterion MUST have a test whose name includes its stable requirement ID. The complete JUnit 5 test suite, Checkstyle with `google_checks` and zero warnings, and SpotBugs with Find Security Bugs with no findings of Medium severity or higher MUST pass.

### CLI Contract

The executable command is `todo`. Commands use this syntax:

```text
todo add --title <title> [--description <text>] [--priority LOW|MEDIUM|HIGH] [--tag <tag> ...]
todo list [--tag <tag> ...] [--sort priority]
todo update <id> [--title <title>] [--description <text>] [--clear-description] [--priority LOW|MEDIUM|HIGH] [--tag <tag> ...] [--clear-tags]
todo status <id> <TODO|IN_PROGRESS|DONE>
todo delete <id>
```

For `update`, repeated `--tag` values replace the complete tag set; omitting both `--tag` and `--clear-tags` leaves tags unchanged. `--clear-tags` removes all tags and cannot be combined with `--tag`. `--clear-description` removes the description and cannot be combined with `--description`. An update MUST specify at least one field to change. Conflicting options and updates with no fields to change are invalid arguments. Titles MUST be trimmed of leading and trailing whitespace before validation and storage; internal whitespace is preserved. Empty or whitespace-only titles after trimming are invalid. Tags MUST contain at least one non-whitespace character after trimming; spaces and punctuation are allowed internally. Tags are lowercased and trimmed before storage. The data file is `tasks.json` in the current working directory. It MUST be a UTF-8 JSON object with `version` set to `1`, `nextId` set to the next never-used positive integer, and `tasks` set to an array of task records. Each record contains `id`, `title`, `priority`, `tags`, `status`, and `createdAt`; `description` is present only when set. The stored task array MUST be ordered by ascending ID. The list command emits `description:null` when the stored description is absent.

Successful add prints `Added task #<id>: <title>` followed by one newline. Successful update, status, and delete print `Updated task #<id>.`, `Task #<id> status set to <STATUS>.`, and `Deleted task #<id>.`, respectively, each followed by one newline. These commands exit 0.

List prints one compact JSON object per task followed by one newline, using these keys in this order: `id`, `title`, `description`, `priority`, `tags`, `status`, `createdAt`. An absent description is JSON `null`; tags are a JSON array of lowercase strings; `createdAt` is UTC RFC 3339 with `Z`. An empty result prints `No tasks found.` and one newline. Listing exits 0.

Invalid user input prints one concise diagnostic to stderr followed by one newline, prints nothing to stdout, and exits 2. A missing task prints `Error: task #<id> not found.`. Invalid priority prints `Error: priority must be LOW, MEDIUM, or HIGH.`. Invalid status prints `Error: status must be TODO, IN_PROGRESS, or DONE.`. Other invalid arguments print `Error: invalid arguments.`. Data-file read, parse, version, or write failures print `Error: unable to read tasks file.` for read/parse/version failures or `Error: unable to save tasks file.` for write failures, print nothing to stdout, and exit 1. No error path prints a stack trace.

### Acceptance Criteria

Each numbered criterion is a binary pass/fail check. Unless a criterion says otherwise, a quoted command output below means exactly the shown stdout bytes, including one trailing newline; stderr is empty; and the process exit code is exactly the stated code. Fixed timestamps in examples are provided by a test clock.

#### FR-1: Add a task

- **AC-FR-1a**: With an empty store and the test clock set to `2026-11-01T10:00:00Z`, run `todo add --title "Buy milk" --priority MEDIUM --tag groceries`. Pass only if stdout is exactly `Added task #1: Buy milk\n`, stderr is empty, exit code is 0, and the persisted task has ID 1, title `Buy milk`, priority MEDIUM, tags `["groceries"]`, status TODO, and createdAt `2026-11-01T10:00:00Z`. Test name includes `FR-1`.
- **AC-FR-1b**: Run `todo add --title "Buy milk"` with an empty store. Pass only if stdout is exactly `Added task #1: Buy milk\n`, stderr is empty, and exit code is 0. Test name includes `FR-1`.
- **AC-FR-1c**: With an empty store, run `todo add --title "  Buy  milk  "`. Pass only if stdout is exactly `Added task #1: Buy  milk\n`, stderr is empty, exit code is 0, and the persisted title is exactly `Buy  milk`. Test name includes `FR-1`.

#### FR-2: Defaults and optional description

- **AC-FR-2**: After `todo add --title "Call Sam"` with a fixed test clock, pass only if the stored task has priority MEDIUM, tags `[]`, status TODO, no description value, and the exact fixed UTC creation time; stdout is exactly `Added task #1: Call Sam\n`, stderr is empty, and exit code is 0. Test name includes `FR-2`.

#### FR-3: List tasks

- **AC-FR-3**: Given a stored task `{id:1,title:"Buy milk",description:null,priority:"MEDIUM",tags:["groceries"],status:"TODO",createdAt:"2026-11-01T10:00:00Z"}`, run `todo list`. Pass only if stdout is exactly `{"id":1,"title":"Buy milk","description":null,"priority":"MEDIUM","tags":["groceries"],"status":"TODO","createdAt":"2026-11-01T10:00:00Z"}\n`, stderr is empty, and exit code is 0. Test name includes `FR-3`.

#### FR-4: Filter by tag

- **AC-FR-4**: Given task 1 `{id:1,title:"Buy milk",description:null,priority:"MEDIUM",tags:["groceries"],status:"TODO",createdAt:"2026-11-01T10:00:00Z"}` and task 2 tagged `work`, run `todo list --tag GROCERIES`. Pass only if stdout is exactly `{"id":1,"title":"Buy milk","description":null,"priority":"MEDIUM","tags":["groceries"],"status":"TODO","createdAt":"2026-11-01T10:00:00Z"}\n`, stderr is empty, and exit code is 0. With no match, stdout is exactly `No tasks found.\n`, stderr is empty, and exit code is 0. Test name includes `FR-4`.

#### FR-5: Sort by priority

- **AC-FR-5**: Given task 1 `{id:1,title:"Buy milk",description:null,priority:"LOW",tags:[],status:"TODO",createdAt:"2026-11-01T10:00:00Z"}`, task 2 `{id:2,title:"Submit report",description:null,priority:"HIGH",tags:["work"],status:"TODO",createdAt:"2026-11-01T10:01:00Z"}`, and task 3 `{id:3,title:"Call Sam",description:null,priority:"MEDIUM",tags:["home"],status:"TODO",createdAt:"2026-11-01T10:02:00Z"}`, run `todo list --sort priority`. Pass only if stdout is exactly the following three JSON lines, in order, followed by a newline: `{"id":2,"title":"Submit report","description":null,"priority":"HIGH","tags":["work"],"status":"TODO","createdAt":"2026-11-01T10:01:00Z"}\n{"id":3,"title":"Call Sam","description":null,"priority":"MEDIUM","tags":["home"],"status":"TODO","createdAt":"2026-11-01T10:02:00Z"}\n{"id":1,"title":"Buy milk","description":null,"priority":"LOW","tags":[],"status":"TODO","createdAt":"2026-11-01T10:00:00Z"}\n`; stderr is empty and exit code is 0. For equal priorities, lower IDs MUST appear first. Test name includes `FR-5`.

#### FR-6: Update task details

- **AC-FR-6a**: Given task 1 `{id:1,title:"Buy milk",description:null,priority:"MEDIUM",tags:["groceries"],status:"TODO",createdAt:"2026-11-01T10:00:00Z"}`, run `todo update 1 --title "Buy oat milk" --priority HIGH --tag groceries --tag home`. Pass only if command stdout is exactly `Updated task #1.\n`, stderr is empty, exit code is 0, and subsequent `todo list` stdout is exactly `{"id":1,"title":"Buy oat milk","description":null,"priority":"HIGH","tags":["groceries","home"],"status":"TODO","createdAt":"2026-11-01T10:00:00Z"}\n` with exit code 0. Test name includes `FR-6`.
- **AC-FR-6b**: Given task 1 `{id:1,title:"Buy milk",description:"whole milk",priority:"MEDIUM",tags:[],status:"TODO",createdAt:"2026-11-01T10:00:00Z"}`, run `todo update 1 --clear-description`. Pass only if command stdout is exactly `Updated task #1.\n`, stderr is empty, exit code is 0, and subsequent `todo list` stdout is exactly `{"id":1,"title":"Buy milk","description":null,"priority":"MEDIUM","tags":[],"status":"TODO","createdAt":"2026-11-01T10:00:00Z"}\n` with exit code 0. Test name includes `FR-6`.
- **AC-FR-6c**: Given task 1 `{id:1,title:"Buy milk",description:null,priority:"MEDIUM",tags:["groceries"],status:"TODO",createdAt:"2026-11-01T10:00:00Z"}`, run `todo update 1 --clear-tags`. Pass only if command stdout is exactly `Updated task #1.\n`, stderr is empty, exit code is 0, and subsequent `todo list` stdout is exactly `{"id":1,"title":"Buy milk","description":null,"priority":"MEDIUM","tags":[],"status":"TODO","createdAt":"2026-11-01T10:00:00Z"}\n` with exit code 0. Test name includes `FR-6`.
- **AC-FR-6d**: Given task 1 `{id:1,title:"Buy milk",description:null,priority:"MEDIUM",tags:[],status:"TODO",createdAt:"2026-11-01T10:00:00Z"}`, run `todo update 1 --title "  Call  Sam  "`. Pass only if command stdout is exactly `Updated task #1.\n`, stderr is empty, exit code is 0, and subsequent `todo list` stdout is exactly `{"id":1,"title":"Call  Sam","description":null,"priority":"MEDIUM","tags":[],"status":"TODO","createdAt":"2026-11-01T10:00:00Z"}\n`. Test name includes `FR-6`.

#### FR-7: Change status

- **AC-FR-7**: Given task 1 `{id:1,title:"Buy milk",description:null,priority:"MEDIUM",tags:[],status:"TODO",createdAt:"2026-11-01T10:00:00Z"}`, run `todo status 1 IN_PROGRESS`. Pass only if command stdout is exactly `Task #1 status set to IN_PROGRESS.\n`, stderr is empty, exit code is 0, and subsequent `todo list` stdout is exactly `{"id":1,"title":"Buy milk","description":null,"priority":"MEDIUM","tags":[],"status":"IN_PROGRESS","createdAt":"2026-11-01T10:00:00Z"}\n` with exit code 0. Running `todo status 1 TODO` or `todo status 1 DONE` on the same fixture MUST print the corresponding `Task #1 status set to <STATUS>.\n`, exit 0, and list the exact corresponding status. Test name includes `FR-7`.

#### FR-8: Delete task

- **AC-FR-8**: Given task 1, run `todo delete 1`. Pass only if stdout is exactly `Deleted task #1.\n`, stderr is empty, exit code is 0, and a subsequent `todo list` prints exactly `No tasks found.\n` and exits 0. Test name includes `FR-8`.

#### FR-9: Invalid user input

- **AC-FR-9a**: Run `todo add --title ""`. Pass only if stdout is empty, stderr is exactly `Error: invalid arguments.\n`, exit code is 2, and no stack trace is present. Test name includes `FR-9`.
- **AC-FR-9b**: Run `todo add --title "Buy milk" --priority URGENT`. Pass only if stdout is empty, stderr is exactly `Error: priority must be LOW, MEDIUM, or HIGH.\n`, exit code is 2, and no stack trace is present. Test name includes `FR-9`.
- **AC-FR-9c**: Run `todo add --title "Buy milk" --tag "   "`. Pass only if stdout is empty, stderr is exactly `Error: invalid arguments.\n`, exit code is 2, and no stack trace is present. A tag containing any non-whitespace content, including spaces or punctuation internally, is valid. Test name includes `FR-9`.
- **AC-FR-9d**: Given task 1 exists, run `todo update 1 --description "new text" --clear-description`, then run `todo update 1` with no fields. Pass only if each command prints nothing to stdout, prints exactly `Error: invalid arguments.\n` to stderr, exits 2, and leaves stored tasks unchanged. Test name includes `FR-9`.
- **AC-FR-9e**: Run `todo add --title "   "`. Pass only if stdout is empty, stderr is exactly `Error: invalid arguments.\n`, exit code is 2, and no stack trace is present. Test name includes `FR-9`.

#### FR-10: Local JSON persistence

- **AC-FR-10a**: With the test clock set to `2026-11-01T10:00:00Z`, run `todo add --title "Buy milk" --priority MEDIUM --tag groceries`, terminate the process, and run `todo list` in a new process using the same working directory. Pass only if the first stdout is exactly `Added task #1: Buy milk\n` and the second stdout is exactly `{"id":1,"title":"Buy milk","description":null,"priority":"MEDIUM","tags":["groceries"],"status":"TODO","createdAt":"2026-11-01T10:00:00Z"}\n`; both stderr streams are empty and both exit codes are 0. Test name includes `FR-10`.
- **AC-FR-10b**: With malformed JSON in `tasks.json`, run `todo list`. Pass only if stdout is empty, stderr is exactly `Error: unable to read tasks file.\n`, exit code is 1, and the file bytes remain unchanged. Test name includes `FR-10`.

#### FR-11: Missing task and storage failures

- **AC-FR-11a**: Run `todo delete 99` when ID 99 does not exist. Pass only if stdout is empty, stderr is exactly `Error: task #99 not found.\n`, exit code is 2, and stored tasks are unchanged. Test name includes `FR-11`.
- **AC-FR-11b**: Make the data file unwritable and run a command that changes data. Pass only if stdout is empty, stderr is exactly `Error: unable to save tasks file.\n`, exit code is 1, and no success-shaped output or stack trace is emitted. Test name includes `FR-11`.

#### FR-12: Explicit scope exclusions

- **AC-FR-12**: Inspect the command surface and application dependencies. Pass only if no login, user-account or multi-user command, network client, GUI, or recurring-task command/dependency is present. Test name includes `FR-12`.

#### NFR-1: Offline and network-free operation

- **AC-NFR-1**: With dependencies provisioned, run the full build and test suite with network access disabled. Pass only if the suite completes successfully and a network-connection test reports zero attempted connections. Test name includes `NFR-1`.

#### NFR-2: Runtime and build compatibility

- **AC-NFR-2**: Build and test using Java 17. Pass only if Maven exits 0 and reports the full JUnit 5 suite successful. Test name includes `NFR-2`.

#### NFR-3: Deterministic CLI behavior

- **AC-NFR-3**: Run each documented successful command and invalid-input example twice against the same controlled fixture. Pass only if stdout, stderr, and exit code match byte-for-byte across runs, and no ANSI color codes, progress messages, or stack traces appear. Test name includes `NFR-3`.

#### NFR-4: Safe JSON persistence

- **AC-NFR-4**: Interrupt a simulated save before replacement of the prior data file. Pass only if the prior file remains valid and unchanged, and no partial task file is visible. Test name includes `NFR-4`.

#### NFR-5: Quality gates and traceability

- **AC-NFR-5**: Run JUnit 5, Checkstyle with `google_checks`, and SpotBugs with Find Security Bugs. Pass only if all JUnit tests pass, Checkstyle reports zero warnings, SpotBugs reports no findings of Medium severity or higher, and every acceptance test name contains its FR/NFR identifier. Test name includes `NFR-5`.

## Key Entities

- **Task**: A single user's item of work, identified by a positive integer ID. It has a required title; optional description; priority LOW, MEDIUM, or HIGH; zero or more tags; status TODO, IN_PROGRESS, or DONE; and immutable UTC creation time.
- **Task Store**: The local, versioned JSON document holding the task collection and next ID. The default file is `tasks.json` in the current working directory.

## Assumptions

- The application has one local user; no identity or account data is needed.
- The default and only v1 data location is `tasks.json` in the current working directory.
- New tasks start in TODO with MEDIUM priority, no tags, and no description unless supplied.
- IDs start at 1, increase monotonically, and are never reused; the creation time is assigned at successful creation and stored in UTC.
- Tags are trimmed, lowercased, and deduplicated. A tag is valid when it contains any non-whitespace content after trimming; spaces and punctuation are allowed internally. A task matches a multi-tag filter if it has any requested tag.
- Tags retain the order in which they were first supplied; updating tags replaces that ordered set.
- Listing without a sort option orders tasks by ascending ID; priority sort orders HIGH, MEDIUM, LOW and breaks ties by ascending ID.
- The compact JSON line format and diagnostics in the CLI Contract are the externally observable output contract.
- Java 17+, Maven, local-only operation, and the project quality gates are required by the existing project constitution.

## Deferred Considerations

The following are outside the v1 scope and do not change its CLI contract:

- Configurable data-file paths beyond `tasks.json` in the current working directory.
- Description search or a human-oriented table output mode in addition to JSON Lines.

## Clarifications

### Session 2026-11-03

- Q: Where is the data file stored? → A: `tasks.json` in the current working directory.
- Q: How is title whitespace handled? → A: Titles are trimmed; empty or whitespace-only titles are invalid (exit code 2).
- Q: How are tags validated? → A: A tag must contain at least one non-whitespace character after trimming. Spaces and punctuation are allowed internally; tags are lowercased and trimmed before storage.
- Q: What happens on an update with no fields? → A: It is rejected as invalid arguments (exit code 2).
