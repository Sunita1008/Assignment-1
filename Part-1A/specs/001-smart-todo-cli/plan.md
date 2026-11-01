# Implementation Plan: Smart Todo CLI

**Branch**: `001-smart-todo-cli` | **Date**: 2026-11-01 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `specs/001-smart-todo-cli/spec.md`

## Summary

Build a single-user, offline Java CLI for adding, listing, filtering, sorting,
updating, changing status, and deleting tasks. Plain argument parsing dispatches
validated commands to task logic; a versioned Jackson JSON document in the
current directory provides persistence. The implementation preserves the
specification's exact stdout/stderr, exit-code, JSON-lines, and storage-failure
contracts.

## Technical Context

**Language/Version**: Java 17

**Primary Dependencies**: Maven; Jackson for JSON; JUnit 5 for tests; Checkstyle
with `google_checks`; SpotBugs with Find Security Bugs; Maven Appassembler
plugin to generate the `todo` launcher

**Storage**: UTF-8 `tasks.json` in the current working directory; versioned JSON

**Testing**: JUnit 5 unit, integration, and subprocess CLI tests

**Target Platform**: Java 17-compatible desktop operating systems; local filesystem

**Project Type**: Single-project command-line application

**Performance Goals**: No latency target specified; deterministic behavior for a
local task collection

**Constraints**: No network access, user accounts, GUI, or recurring tasks.
Plain argument parsing only. Writes must not expose a partially written store.
Failures must not be reported as successes or print stack traces.

**Scale/Scope**: One local user's tasks; no multi-user or concurrent-writer
guarantee is required by v1.

## Constitution Check

| Principle / requirement | Plan compliance |
|---|---|
| I. Modular Boundaries / REQ-ARCH-001 | Separate `cli`, `application`, `domain`, and `storage` packages; dependency direction is CLI → application → storage, with domain types shared inward. Storage has no CLI dependency. Verify boundaries with a test. |
| II. Predictable CLI Behavior / REQ-CLI-001 | Central command dispatch maps validation errors to stderr/exit 2, storage errors to stderr/exit 1, and valid results to stdout/exit 0; no stack traces. Include a process-level assertion of stdout, stderr, and exit status. |
| III. Verifiable Requirements / REQ-TRACE-001 | Name each acceptance test with its stable `FR-n` or `NFR-n` identifier and assert observable behavior. Keep the spec criteria unchanged. |
| IV. Zero-Tolerance Quality Gates / REQ-QUALITY-001–003 | Maven `verify` runs the full JUnit 5 suite, Checkstyle (`google_checks`, zero warnings), and SpotBugs with Find Security Bugs (no Medium-or-higher findings). |
| V. Simplicity and Maintainability | Use a single Maven application, direct argument parsing, one JSON store, and no speculative features. |
| REQ-PLATFORM-001 / NFR-2 | Compile and test for Java 17 using Maven. |
| REQ-SEC-001 | Do not add secrets; secret detection must report no findings in changed files. |
| REQ-NET-001 / NFR-1 | Production and test code perform no network operations; run verification offline after dependencies have been provisioned. |

**Pre-design gate**: Pass. The selected stack and local-only scope satisfy the
constitution without exceptions. The data file is created only on a successful
write, and parse/version errors do not modify it.

## Architecture and Key Decisions

1. **Keep the CLI thin.** A plain parser recognizes only the documented
   subcommands and options. It converts arguments into typed command inputs and
   renders the stable output contract; it contains no persistence or task rules.
2. **Centralize task rules in the application/domain layer.** The application
   service coordinates repository operations. Domain types own task fields,
   defaults, validation, tag normalization, and status/priority values. Inject a
   `Clock` at composition boundaries so creation-time tests are deterministic;
   production uses UTC system time.
3. **Use a single versioned JSON store.** Jackson reads/writes `version`,
   `nextId`, and tasks. A missing file means an empty store with next ID 1.
   Malformed JSON, invalid stored records, and unsupported versions become
   storage errors; they are never treated as an empty store.
4. **Replace the store safely.** Serialize the complete new document as UTF-8 to
   a temporary sibling file, then replace the original using an atomic move.
   Clean up the temporary file on failure. If a safe replacement cannot be
   completed, preserve the old file and return the specified save error rather
   than falling back to a potentially partial write.
5. **Keep test seams narrow.** Use a repository boundary for isolated task tests
   and deterministic storage-failure injection; exercise the real JSON adapter
   and CLI composition in integration tests. A test-only launcher supplies a
   fixed clock for subprocess tests without adding a user-facing clock option.
6. **Honor the explicit v1 contract.** Retain JSON-lines output and the one
   current-directory data file. Alternate paths and human-oriented table output
   are deferred; no command or dependency is added for excluded scope.

## Error Handling

- Invalid syntax, values, IDs, empty titles, tags, or incompatible update flags:
  print the specified concise diagnostic to stderr, nothing to stdout, exit 2.
- Unknown task IDs: print `Error: task #<id> not found.`, exit 2, and do not save.
- File read, JSON parse, data-version, or stored-data errors: print
  `Error: unable to read tasks file.`, exit 1, and leave the source bytes alone.
- Failed persistence: print `Error: unable to save tasks file.`, exit 1; do not
  emit a success message. Preserve the last valid file.
- Empty results are successful: print `No tasks found.` to stdout and exit 0.
- Catch expected failures at the CLI boundary and map them to these diagnostics;
  do not catch broadly or expose stack traces.

## Testing Strategy

- **Unit**: parser and validation cases; task defaults and immutable ID/time;
  tag normalization/deduplication and OR filtering; priority ordering and ID
  tie-break; updates, explicit clears, status idempotence, and monotonic IDs
  after deletion. Use fixed clocks and in-memory repositories.
- **Storage integration**: missing file, UTF-8/version-1 round trip, absent
  description representation, ascending stored IDs, malformed JSON, unsupported
  version, invalid records, and injected write/move failure. Assert source bytes
  remain unchanged for rejected reads and failed writes.
- **CLI integration/subprocess**: assert exact output bytes, stream separation,
  exit codes, JSON field order, cross-process persistence, and absence of stack
  traces. Cover successful commands and the specified invalid-input, missing-ID,
  read-failure, and write-failure scenarios.
- **Architecture and scope**: assert the storage package has no dependency on
  CLI parsing; inspect command/dependency surface for prohibited network, GUI,
  multi-user, and recurring-task features.
- **Quality gates**: `mvn -o verify` after dependencies are provisioned; require
  all JUnit tests, zero Checkstyle warnings under `google_checks`, and no
  Medium-or-higher SpotBugs/Find Security Bugs findings.
- **Traceability**: acceptance test names contain their applicable `FR-n` or
  `NFR-n` identifier, including failure-path and quality-gate tests.

## Project Structure

### Documentation (this feature)

```text
specs/001-smart-todo-cli/
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
└── contracts/
    └── cli-contract.md
```

### Source Code

```text
pom.xml
src/
├── main/
│   └── java/
│       └── com/
│           └── smarttodo/
│               ├── TodoMain.java
│               ├── cli/
│               │   ├── ArgumentParser.java
│               │   ├── Command.java
│               │   └── CommandRunner.java
│               ├── application/
│               │   └── TaskService.java
│               ├── domain/
│               │   ├── Task.java
│               │   ├── Priority.java
│               │   └── TaskStatus.java
│               └── storage/
│                   ├── TaskRepository.java
│                   └── JsonTaskRepository.java
└── test/
    └── java/
        └── com/
            └── smarttodo/
                ├── cli/
                │   ├── ArgumentParserTest.java
                │   └── TodoCliProcessTest.java
                ├── application/
                │   └── TaskServiceTest.java
                ├── storage/
                │   └── JsonTaskRepositoryTest.java
                └── architecture/
                    └── ModuleBoundariesTest.java
```

`com.smarttodo` is the package root. Maven Appassembler generates the executable
`todo` launcher (including its Windows launcher) for `TodoMain`. Production
dependencies flow from CLI to application and from application to the storage
repository interface; domain types are independent. In particular, `storage`
MUST NOT import `cli`.

**Structure Decision**: One Maven project with package-level boundaries, matching
the single CLI application and constitution without introducing multiple Maven
modules.

## Post-design Constitution Check

Pass. The design retains the required layer direction, deterministic CLI
diagnostics, stable requirement-named tests, offline build gates, and minimal
local scope. The JSON adapter owns safe persistence; CLI parsing is isolated
from it. No exceptions or complexity justifications are required.
