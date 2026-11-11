# Tasks: Natural-Language Reminder Agent

**Input**: Design documents in `Part-1B/specs/001-natural-language-reminder/`
**Source of truth**: `plan.md` for the agent CLI, runtime, and model configuration contract; `spec.md` for user-visible behavior and requirements.

**Protected inputs**: `Part-1B/.harness/` and `Part-1B/evals/` are read-only. Tasks may read the schema and run the external harness, but MUST NOT modify either directory. The Java agent contains no evaluator or scoring component and MUST NOT read the dataset.

## Format

- Every task uses `- [ ] Tnnn [P?] [USn?] Description with exact file paths`.
- `[P]` marks work that can proceed independently on different files after its prerequisites.
- User-story tasks use `[US1]`, `[US2]`, or `[US3]`; setup, shared foundation, and release tasks are untagged.
- Tests are written before implementation where practical and MUST be deterministic unless they explicitly run the external harness.

## Phase 1: Setup

**Purpose**: Create the Java 21 Maven application and required quality tooling.

- [x] T001 Configure `Part-1B/pom.xml` for Java 21, Jackson, JSON Schema validation, JUnit 5, Maven Surefire, Checkstyle (`google_checks`), SpotBugs with Find Security Bugs, and `mvn verify` quality gates. Package an executable, dependency-complete `target/reminder-agent.jar` with the CLI entry point in its manifest.
- [x] T002 [P] Configure Maven Checkstyle to use its bundled `google_checks.xml` with zero warnings and no suppressions.

## Phase 2: Foundational

**Purpose**: Establish the input, model, prompt, output-validation, and CLI boundaries shared by all user stories.

- [x] T003 Define `ReminderInput` in `Part-1B/src/main/java/io/github/sunita1008/reminder/input/ReminderInput.java`, the eight-field `AgentResult` in `Part-1B/src/main/java/io/github/sunita1008/reminder/output/AgentResult.java`, `Intent` and `ReasonCode` in the output package, and the typed `ModelUnavailable` outcome in `Part-1B/src/main/java/io/github/sunita1008/reminder/api/ModelUnavailable.java`. Preserve the five JSON intents; outages are not intents.
- [x] T004 [P] Add input validation tests in `Part-1B/src/test/java/io/github/sunita1008/reminder/input/InputValidatorTest.java` for the exact three-field envelope, non-empty message, offset-bearing RFC 3339 reference instant, and valid IANA zone; cover malformed and invalid context.
- [x] T005 Implement strict envelope parsing and input validation in `Part-1B/src/main/java/io/github/sunita1008/reminder/input/InputValidator.java`; retain invalid-context information so the agent can return the specified `CLARIFY` result without inventing time context.
- [x] T006 [P] Add strict result parser tests in `Part-1B/src/test/java/io/github/sunita1008/reminder/output/ResultParserTest.java` for malformed JSON, non-object roots, trailing content, duplicate/unknown fields, and type coercion.
- [x] T007 Implement `ResultParser` in `Part-1B/src/main/java/io/github/sunita1008/reminder/output/ResultParser.java` to accept exactly one JSON object and reject malformed, duplicate, unknown, coerced, or trailing content.
- [x] T008 [P] Add schema validation tests in `Part-1B/src/test/java/io/github/sunita1008/reminder/output/JsonSchemaValidatorTest.java` covering the eight required fields, all five intents, nullable values, and rejection of additional properties.
- [x] T009 Implement `JsonSchemaValidator` in `Part-1B/src/main/java/io/github/sunita1008/reminder/output/JsonSchemaValidator.java` using the authoritative read-only schema at `Part-1B/.harness/schema/result.schema.json`. Configure `Part-1B/pom.xml` to package that schema into the JAR from the protected source without modifying it.
- [x] T010 [P] Add input-relative invariant tests in `Part-1B/src/test/java/io/github/sunita1008/reminder/output/InvariantValidatorTest.java` for intent/null/reason relationships, required non-empty strings, echoed zone, UTC timestamp format, and schedule strictly later than the reference instant.
- [x] T011 Implement `InvariantValidator` in `Part-1B/src/main/java/io/github/sunita1008/reminder/output/InvariantValidator.java`; enforce the intent relationships and time constraints from the schema contract and data model.
- [x] T012 [P] Add model configuration tests in `Part-1B/src/test/java/io/github/sunita1008/reminder/model/ModelConfigurationTest.java` for `MODEL_API_KEY`, `MODEL_PROVIDER`, `MODEL_NAME`, missing configuration, Gemini endpoint selection, and key redaction.
- [x] T013 Implement `ModelConfiguration` in `Part-1B/src/main/java/io/github/sunita1008/reminder/model/ModelConfiguration.java`; read the three documented environment variables, use `https://generativelanguage.googleapis.com/v1beta/openai/` when `MODEL_PROVIDER=gemini`, and never log or write the API key.
- [x] T014 [P] Define the narrow `ModelClient` boundary in `Part-1B/src/main/java/io/github/sunita1008/reminder/model/ModelClient.java`; cover outage propagation through `FakeModelClient` tests only, without network or live-model calls.
- [x] T015 Implement `HttpModelClient` in `Part-1B/src/main/java/io/github/sunita1008/reminder/model/HttpModelClient.java` using Java 21 `HttpClient`, bounded requests, and the configured provider/model/key. Map service failures to stable `ModelUnavailable` categories without exposing credentials.
- [x] T016 Add `FakeModelClient` in `Part-1B/src/test/java/io/github/sunita1008/reminder/model/FakeModelClient.java` for scripted responses and unavailable outcomes; keep it test-only and fail explicitly if its script is exhausted.
- [x] T017 [P] Add prompt-boundary tests in `Part-1B/src/test/java/io/github/sunita1008/reminder/prompt/PromptBuilderTest.java` asserting user content is separate from system instructions, treated as untrusted, and sent without model tools.
- [x] T018 Implement `PromptBuilder` in `Part-1B/src/main/java/io/github/sunita1008/reminder/prompt/PromptBuilder.java` and versioned prompt resources in `Part-1B/src/main/resources/prompts/{system.md,task.md,output-format.md}`. Require one contracted JSON object; do not interpolate raw user content into system instructions.
- [x] T019 [P] Add module-boundary tests in `Part-1B/src/test/java/io/github/sunita1008/reminder/contract/ModuleBoundaryTest.java` to ensure model output cannot bypass parsing, schema validation, or invariant validation.

**Checkpoint**: Shared boundaries are testable without external network access; the JAR can package the authoritative result schema without changing protected inputs.

## Phase 3: User Story 1 - Parse a reminder request (Priority: P1)

**Goal**: Return a validated `CREATE` result with reminder text and a future UTC instant.

**Independent Test**: Script a model response for “tomorrow at 9:30 AM” and “in 2 hours”; verify result fields, supplied zone, schema/invariant compliance, and no extra stdout.

- [x] T020 [P] [US1] Add deterministic creation-flow tests in `Part-1B/src/test/java/io/github/sunita1008/reminder/integration/ReminderAgentTest.java` for both specification examples, valid JSON output, correct UTC instant, echoed zone, and rejection of invalid model output.
- [x] T021 [US1] Implement `ReminderAgent` orchestration in `Part-1B/src/main/java/io/github/sunita1008/reminder/api/ReminderAgent.java`: build the prompt, call `ModelClient`, parse the response, validate schema/invariants/guardrails, and return a result only after all checks pass.
- [x] T022 [P] [US1] Add CLI contract tests in `Part-1B/src/test/java/io/github/sunita1008/reminder/cli/ReminderCliTest.java` using an injected `FakeModelClient`; verify one JSON envelope in, one JSON object out, and success exit code 0.
- [x] T023 [US1] Implement `run` in `Part-1B/src/main/java/io/github/sunita1008/reminder/cli/ReminderCli.java`; read one input envelope from stdin, write only the result JSON to stdout, and keep diagnostics off stdout.

**Checkpoint**: A clear create request completes through the packaged one-shot CLI using a deterministic fake model.

## Phase 4: User Story 2 - Change an existing reminder's time (Priority: P2)

**Goal**: Return `RESCHEDULE` or `SNOOZE` proposals grounded in a user-derived target label, without implying lookup or modification.

**Independent Test**: Script one reschedule and one relative snooze; verify target, proposed UTC instant, proposal-only semantics, and clarification when the target is missing or non-unique.

- [x] T024 [P] [US2] Add reschedule and snooze tests in `Part-1B/src/test/java/io/github/sunita1008/reminder/integration/ChangeReminderTimeTest.java` for explicit and relative times, normalized short target labels, missing targets, and future-time validation.
- [x] T025 [US2] Update `Part-1B/src/main/resources/prompts/task.md` and `Part-1B/src/main/resources/prompts/output-format.md` with `RESCHEDULE`/`SNOOZE` rules: derive a short target label from user wording, never invent an ID or look up history, clarify a missing or non-unique target, and describe a proposal only.

**Checkpoint**: Both change intents remain stateless proposals and are independently testable with `FakeModelClient`.

## Phase 5: User Story 3 - Resolve ambiguity and refuse unsafe actions (Priority: P3)

**Goal**: Clarify missing, invalid, conflicting, or ambiguous information without guessing; refuse unsafe or unsupported actions without reminder fields.

**Independent Test**: Exercise missing/past/ambiguous time, invalid context, instruction injection, direct unsafe actions, and safe reminder subjects that merely mention those actions.

- [x] T026 [P] [US3] Add clarification/refusal tests in `Part-1B/src/test/java/io/github/sunita1008/reminder/integration/ClarifyAndRefuseTest.java` for invalid context, missing target, ambiguous time, refusal shape, and safe reminder subjects.
- [x] T027 [US3] Handle invalid input context in `Part-1B/src/main/java/io/github/sunita1008/reminder/api/ReminderAgent.java`: return schema-valid `CLARIFY` with `INVALID_CONTEXT`, null reminder fields, and a focused question without invoking `ModelClient`.
- [x] T028 [P] [US3] Add guardrail tests in `Part-1B/src/test/java/io/github/sunita1008/reminder/guardrail/GuardrailValidatorTest.java` for refusal-reason consistency and safe reminder-subject acceptance.
- [x] T029 [US3] Implement `GuardrailValidator` in `Part-1B/src/main/java/io/github/sunita1008/reminder/guardrail/GuardrailValidator.java` and integrate it with `ReminderAgent`; enforce result-side safety invariants without replacing schema or intent/null validation.
- [x] T030 [US3] Refine `Part-1B/src/main/resources/prompts/system.md` and `Part-1B/src/main/resources/prompts/task.md` to clarify instead of guessing for incomplete or ambiguous requests, ignore embedded instruction overrides, refuse direct unsafe/out-of-scope actions, and preserve safe reminder-subject content.

**Checkpoint**: Ambiguity and unsafe requests produce the specified clarification/refusal shapes; service outages remain distinct and never become `CLARIFY` or `REFUSE`.

## Phase 6: CLI Failure Contract and Documentation

**Purpose**: Verify the exact stdout/exit-code contract and keep implementation documentation aligned with the current plan.

- [x] T031 Add CLI failure tests in `Part-1B/src/test/java/io/github/sunita1008/reminder/cli/ReminderCliTest.java` asserting `{"unavailable":"<CATEGORY>"}` on stdout with exit code 3 for model/service outages, and `{"invalid_output":"<reason>"}` with exit code 4 for invalid generated output. Assert outages are not converted to intents and secrets never appear in output.
- [x] T032 Implement the unavailable and invalid-output CLI mappings in `Part-1B/src/main/java/io/github/sunita1008/reminder/cli/ReminderCli.java`; preserve the existing `ModelClient`, `FakeModelClient` (test-only), and `ModelUnavailable` boundaries.
- [x] T033 [P] Update `Part-1B/specs/001-natural-language-reminder/research.md`, `Part-1B/specs/001-natural-language-reminder/data-model.md`, and `Part-1B/specs/001-natural-language-reminder/quickstart.md` to reflect Java 21, the current CLI/model contract, and independent harness ownership. Do not modify `spec.md`, `.harness/`, or `evals/`.

## Phase 7: Verification

**Purpose**: Run agent quality gates and the independent harness without adding evaluation code to the agent.

- [x] T034 Run `mvn verify` from `Part-1B/`; resolve JUnit, Checkstyle, and SpotBugs failures without suppressions.
- [ ] T035 Run the protected harness self-test with `uv run .harness/selftest.py` from `Part-1B/`; do not edit `.harness/` or `evals/`.
- [ ] T036 After building the JAR and configuring model credentials, run the independent acceptance harness with `powershell -ExecutionPolicy Bypass -File .harness\run_eval.ps1 initial` from `Part-1B/`. Confirm it calls the CLI contract, and do not implement dataset loading, scoring, or report writing in Java.

## Dependencies and Execution Order

### Phase dependencies

- Setup (Phase 1) precedes the shared foundation (Phase 2).
- The shared foundation blocks all user stories.
- User Stories 1, 2, and 3 depend on the shared foundation; story 2 and story 3 do not require story 1's implementation.
- CLI failure mapping and documentation depend on the CLI and model boundaries; verification follows implementation.

### Task dependencies

- T002 is independent of T001; T003 follows T001.
- T004 → T005; T006 → T007; T008 → T009; T010 → T011; T012 → T013; T014 → T015.
- T017 → T018. T019 requires the parser/schema/invariant boundaries from T007, T009, and T011.
- T020 → T021; T022 → T023 for the successful CLI contract.
- T024 → T025.
- T026 → T027; T028 → T029; T030 follows the prompt foundation in T018.
- T031 → T032; T033 may proceed independently after the CLI contract stabilizes.
- T034 follows implementation and tests. T035 is independent; T036 requires a packaged JAR and configured model credentials.

### Parallel opportunities

- T001 and T002 touch separate files.
- Input, parser/schema, invariant, model configuration/client, and prompt test/implementation pairs can proceed in parallel when their listed prerequisites are met.
- After Phase 2, the three user-story phases can proceed independently on separate files.
- The protected harness self-test (T035) can run independently from Maven verification.

## Independent Test Criteria

- **US1**: Clear creation requests yield one schema-valid result object with correct reminder text, future UTC time, and supplied zone; success exits 0.
- **US2**: A grounded target plus replacement/delay yields a proposal; a missing/non-unique target clarifies; no ID, lookup, or applied change is claimed.
- **US3**: Missing, invalid, conflicting, past, or ambiguous information clarifies without a guessed schedule; direct unsafe/out-of-scope requests refuse with the required null fields; safe reminder subjects remain parseable content.
- **CLI**: Unavailable model/service produces one unavailable object and exit 3; invalid generated output produces one invalid-output object and exit 4; neither failure is converted to a successful intent.
- **Harness boundary**: Java never loads `Part-1B/evals/dataset.json` and contains no evaluator, scoring component, evaluation command, or report writer. `.harness/` and `evals/` remain read-only.

## Amendment 2026-11-12: provider switch to AI Credits

Configuration-only change made before any scored run (see "Model configuration" in `plan.md`).

- [x] T037 Extend `ModelConfigurationTest` for the `aicredits` provider, the `MODEL_BASE_URL` override (trailing-slash normalization, rejection of non-https or relative URLs), and the reasoning-model temperature rule.
- [x] T038 Implement the `aicredits` provider (`https://api.aicredits.in/v1/`), the optional `MODEL_BASE_URL` override, and the reasoning-model temperature rule in `ModelConfiguration`. `HttpModelClient` omits `temperature` for reasoning models and uses a 60-second request timeout.
- [ ] T039 Rebuild with `mvn clean verify`, then run the protected harness with `MODEL_PROVIDER=aicredits` and `MODEL_NAME=openai/gpt-5-mini` (requires the student's API key).
