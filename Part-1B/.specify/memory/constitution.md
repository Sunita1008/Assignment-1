# Natural-Language Reminder Agent Constitution

## Core Principles

### I. Modular Architecture
The reminder agent MUST separate input handling, model-client integration, output validation,
and guardrail enforcement into distinct modules with explicit responsibilities. Dependencies
MUST flow from input handling to model access and then to validation; no module may bypass the
validation layer. This keeps request parsing, model interaction, and schema enforcement
independently testable and changeable without entangling one another.

### II. Schema-Guaranteed Output
The agent's output MUST always conform to the declared JSON schema, regardless of the model's
wording, refusal style, or partial failure. Validation MUST occur before the final response is
accepted, and invalid or partial output MUST be retried, rejected, or surfaced as a structured
error rather than emitted as unvalidated text. The application MUST preserve schema fidelity at
all times.

### III. Verifiable Evaluation
Every evaluation case MUST trace to a stable requirement, invariant, or acceptance criterion ID.
The acceptance threshold MUST be declared in the specification before the final evaluation and
MUST NOT be lowered afterward without a spec revision. Tests MUST assert observables and MUST
NOT be weakened, deleted, or reduced in strictness to make a change pass.

### IV. Zero-Tolerance Quality Gates
Every change MUST pass all JUnit 5 tests, Checkstyle using `google_checks` and reporting zero
warnings, and SpotBugs with Find Security Bugs reporting no findings of Medium severity or
higher. No lint or security suppressions may be added to bypass these gates. A change that
fails any gate is not complete.

### V. Simplicity and Maintainability
Implementations MUST prefer the simplest design that satisfies the acceptance criteria.
Abstractions, dependencies, and indirection MUST have a concrete present need; speculative
features, overengineering, and unnecessary complexity MUST NOT be added. Names and module
boundaries MUST make responsibilities apparent, keeping the system understandable and
maintainable.

## Technology and Security Constraints

- **REQ-PLATFORM-001:** Production and test code MUST target Java 17 or higher and use Maven
  for build and dependency management. All builds MUST compile and test under the declared Java
  release level.
- **REQ-ARCH-001:** Input handling, model client logic, output validation, and guardrails MUST
  be separate modules with unambiguous responsibilities. A module-boundary test MUST fail if
  validation is bypassed or if model access depends directly on UI/input parsing.
- **REQ-OUTPUT-001:** Every final agent response MUST validate successfully against the declared
  JSON schema. A response failing schema validation MUST not be returned to the caller.
- **REQ-THRESHOLD-001:** The evaluation threshold MUST be explicitly declared in the specification
  before final evaluation and MUST remain immutable for that evaluation run unless the
  specification is revised.
- **REQ-SEC-001:** Secrets MUST NOT be committed in source code, tests, fixtures, or
  configuration. The API key MUST come only from an environment variable.
- **REQ-FAILURE-001:** Model or service failures such as timeouts, rate limits, and network
  errors MUST be reported distinctly from application failures and evaluation failures.
- **REQ-TRACE-001:** Every evaluation case MUST include an exact requirement, invariant, or
  acceptance criterion ID in its name or metadata so that traceability to the specification is
  auditable.
- **REQ-NET-001:** The application and its tests MUST NOT require network access for correctness
  beyond the declared external model endpoint. Network-dependent failures must be isolated and
  classified separately from logic failures.

## Measurable Quality Standards

- **Quality Standard 1: Zero-warning static analysis**
  Checkstyle using `google_checks` MUST report exactly zero warnings in the main and test code.
- **Quality Standard 2: Security severity threshold**
  SpotBugs with Find Security Bugs MUST report zero findings of Medium severity or higher.
- **Quality Standard 3: Full regression pass**
  The complete JUnit 5 suite MUST pass on every change before acceptance.
- **Quality Standard 4: Schema correctness**
  100% of agent responses MUST pass JSON schema validation before dispatch to the caller.
- **Quality Standard 5: Requirement traceability**
  100% of evaluation cases MUST map to a requirement, invariant, or acceptance criterion ID.
- **Quality Standard 6: Threshold stability**
  The acceptance threshold MUST remain unchanged from the pre-evaluation declaration for that
  run unless a formal specification revision is recorded.
- **Quality Standard 7: Guardrail integrity**
  The agent MUST not disable or weaken guardrails, remove evaluation cases, lower assertions, add
  lint/security suppressions, or edit files under `.harness/`.

## Quality Gates and Workflow

- **REQ-QUALITY-001:** Checkstyle MUST use `google_checks` and report exactly zero warnings.
- **REQ-QUALITY-002:** SpotBugs with Find Security Bugs MUST report zero findings of Medium
  severity or higher.
- **REQ-QUALITY-003:** The complete JUnit 5 test suite MUST pass.
- **REQ-GUARD-001:** Coding agents MUST NOT remove or weaken evaluation cases, change expected
  results without a spec revision, lower the threshold, disable guardrails, add lint or security
  suppressions, or edit any file under `.harness/`.
- **REQ-OUTPUT-002:** Every response MUST be validated against the declared JSON schema before
  being returned.
- **REQ-FAILURE-002:** Model or service failures MUST be reported separately from application or
  evaluation failures in logs, tests, and result classification.
- **REQ-TRACE-002:** Every evaluation artifact MUST clearly identify the associated requirement,
  invariant, or acceptance criterion ID.

The build MUST run the complete validation pipeline before a change is accepted. No gate may be
made advisory or skipped to accommodate a failing change.

## Governance

This constitution governs project requirements, implementation, and review. Amendments MUST be
recorded in the constitution, reviewed for compatibility with existing requirements and tests,
and reflected in the version and amendment date. Version changes MUST follow semantic versioning:
MAJOR for incompatible governance changes, MINOR for new or materially expanded principles, and
PATCH for clarifications that do not change requirements.

Reviews MUST verify requirement-to-test traceability, modular boundaries, schema compliance,
service-failure reporting, secret handling, threshold stability, and all quality gates. Any
exception requires an explicit constitution amendment; individual changes MUST NOT bypass these
rules.

**Version**: 1.0.0 | **Ratified**: 2026-11-10 | **Last Amended**: 2026-11-10
