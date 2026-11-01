<!--
Sync Impact Report
Version change: unestablished -> 1.0.0
Modified principles: all five template placeholders replaced with project principles
Added sections: Technology and Security Constraints; Quality Gates and Workflow
Removed sections: none
Follow-up TODO: confirm the original ratification date
-->
# Smart Todo CLI Constitution

## Core Principles

### I. Modular Boundaries
The application MUST separate CLI parsing, task logic, and JSON storage into distinct
modules with explicit responsibilities. Each module MUST be testable independently at its
public boundary. Dependencies MUST flow from the CLI to task logic and from task logic to
storage; storage MUST NOT depend on CLI parsing. This keeps command handling, domain rules,
and persistence changeable without entangling one another.

### II. Predictable CLI Behavior
Valid commands MUST communicate results on stdout. Invalid arguments and invalid user input
MUST print a concise error message to stderr and exit with a non-zero status; they MUST NOT
print a stack trace. The same invalid input MUST produce the same error category and exit
behavior regardless of invocation context, so callers can reliably diagnose failures.

### III. Verifiable Requirements
Every acceptance criterion MUST be a binary pass/fail check and MUST have at least one test
whose test name includes that criterion's stable requirement ID. Tests MUST assert the
observable behavior, and MUST NOT be weakened, deleted, or reduced in assertion strength to
make a change pass. This makes requirement coverage auditable and prevents ambiguous
completion claims.

### IV. Zero-Tolerance Quality Gates
Every change MUST pass all JUnit 5 tests, Checkstyle using `google_checks` with zero
warnings, and SpotBugs with Find Security Bugs reporting no findings of Medium severity or
higher. No lint or security suppressions may be added to bypass these gates. A change that
fails any gate is not complete.

### V. Simplicity and Maintainability
Implementations MUST prefer the simplest design that satisfies the acceptance criteria.
Abstractions, dependencies, and indirection MUST have a concrete present need; speculative
features and unnecessary complexity MUST NOT be added. Names and module boundaries MUST make
responsibilities apparent, keeping the codebase understandable and maintainable.

## Technology and Security Constraints

- **REQ-PLATFORM-001:** Production and test code MUST target Java 17 or higher and use Maven
	for build and dependency management. A build using the declared Java release level MUST
	pass.
- **REQ-ARCH-001:** CLI parsing, task logic, and JSON storage MUST be separate modules with
	the dependency direction defined in Principle I. Module-boundary tests MUST fail if the
	storage module depends on CLI parsing.
- **REQ-CLI-001:** Invalid input MUST write an error to stderr, return a non-zero exit code,
	and emit no stack trace. A CLI process test MUST assert all three outcomes.
- **REQ-SEC-001:** Secrets MUST NOT be committed in source code, tests, fixtures, or
	configuration. Secret detection MUST report no findings in changed files.
- **REQ-NET-001:** The application and its tests MUST NOT make network requests or open
	network connections. Tests MUST verify network-dependent behavior is absent; the Maven
	build and test suite MUST be runnable offline after dependencies are provisioned.

## Quality Gates and Workflow

- **REQ-QUALITY-001:** Checkstyle MUST use `google_checks` and report exactly zero warnings.
- **REQ-QUALITY-002:** SpotBugs with Find Security Bugs MUST report zero findings of Medium
	severity or higher.
- **REQ-QUALITY-003:** The complete JUnit 5 test suite MUST pass.
- **REQ-TRACE-001:** Every acceptance criterion MUST have an unambiguous pass/fail result and
	at least one test whose name contains the criterion's requirement ID. CI MUST fail when a
	criterion lacks a matching test or when its check fails.
- **REQ-AGENT-001:** Coding agents MUST NOT weaken or delete acceptance criteria or tests,
	lower assertions, add lint or security suppressions, or modify any file under `.harness/`.
	Change review MUST verify the protected criteria, assertions, suppressions, and directory
	remain unchanged.

Each test that verifies a requirement MUST include its exact ID in the test name, for
example, `REQ-CLI-001_invalidInputReportsErrorWithoutStackTrace`. The build MUST run the
complete test and static-analysis gates before a change is accepted. No gate may be skipped
or made advisory to accommodate a failing change.

## Governance

This constitution governs project requirements, implementation, and review. Amendments
MUST be documented in the constitution, reviewed for compatibility with existing
requirements and tests, and reflected in the version and amendment date. Version changes
MUST follow semantic versioning: MAJOR for incompatible governance changes, MINOR for new
or materially expanded principles, and PATCH for clarifications that do not change
requirements.

Reviews MUST verify requirement-to-test traceability, module boundaries, CLI error behavior,
secret and network constraints, and all quality gates. Any exception requires an explicit
constitution amendment; individual changes MUST NOT bypass these rules.

**Version**: 1.0.0 | **Ratified**: TODO(RATIFICATION_DATE): original adoption date not provided | **Last Amended**: 2026-11-01
