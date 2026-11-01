# Research: Smart Todo CLI

## Scope and Unknowns

The feature specification and repository constitution already select Java 17,
Maven, Jackson, local JSON storage, plain argument parsing, JUnit 5, Checkstyle
with `google_checks`, and SpotBugs with Find Security Bugs. No unresolved
implementation choice blocks the v1 design. The two open product questions in
the specification (alternate data-file paths and table-formatted output) are
explicitly deferred and do not change the required CLI contract.

## Decisions

### Package-level modules in one Maven project

- **Decision**: Use `cli`, `application`, `domain`, and `storage` packages. CLI
  dispatches to application task operations; application depends on the
  repository interface and domain types; the JSON repository depends on domain
  types, never on CLI parsing.
- **Rationale**: Satisfies the constitution's separable CLI, task logic, and
  storage boundaries while keeping this single executable simple to build and
  test.
- **Alternatives considered**: Multiple Maven modules would enforce boundaries
  structurally but add build complexity without a current need. A single
  undivided package would weaken the required boundary and its testability.

### Local versioned JSON document with safe replacement

- **Decision**: Store `version: 1`, the next never-used ID, and ID-ordered task
  records in UTF-8 `tasks.json`. Write a complete sibling temporary file and
  atomically replace the old file only after serialization and writing succeed.
- **Rationale**: This preserves the specified single-file model, ID monotonicity,
  deterministic serialization, and protection from truncated writes. An absent
  file remains an empty store; invalid content is an error, not a reset.
- **Alternatives considered**: A database or one file per task conflicts with
  the specification's local JSON document and would add unnecessary
  dependencies. Directly overwriting the data file could leave partial JSON.

### Direct plain argument parsing and explicit error mapping

- **Decision**: Parse the documented grammar without a CLI framework. Map invalid
  arguments and missing tasks to exit 2; map read and save failures to exit 1;
  keep successful output and errors on their specified streams.
- **Rationale**: The input explicitly requests plain parsing and stable,
  machine-readable behavior; the small command surface does not justify another
  runtime dependency.
- **Alternatives considered**: A third-party CLI parser could reduce boilerplate
  but adds a dependency and behavior not required for this fixed grammar.

### Generated `todo` launcher

- **Decision**: Configure Maven Appassembler to create platform launchers named
  `todo` and `todo.bat` for the application main class.
- **Rationale**: The specification exposes `todo` as the executable command;
  generating launchers makes the command contract concrete while retaining the
  Java/Maven application and plain parser.
- **Alternatives considered**: Requiring users to construct a Java classpath or
  invoke `java -jar` would not provide the specified `todo` command directly.

### Fixed-clock tests and narrow repository seam

- **Decision**: Inject `Clock` into application composition and a repository
  boundary into task logic. Use fixed clocks, in-memory repositories, real-file
  integration tests, and a test-only subprocess launcher.
- **Rationale**: Tests can assert exact timestamps and storage failures
  deterministically without making clock overrides or test controls part of the
  user's CLI.
- **Alternatives considered**: Reading wall-clock time directly makes acceptance
  tests nondeterministic. A broad dependency-injection framework is unnecessary
  for a single application.

## Resolved Questions

- Alternate data-file path: not in v1; use `tasks.json` in the current working
  directory.
- Human table output or description search: not in v1; retain only the specified
  JSON-lines list format and tag filters.

## External Research

No external integrations or unspecified technologies require external research.
All selected technologies and required behavior are stated in the constitution
and feature specification.
