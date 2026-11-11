# Tasks: Natural-Language Reminder Agent

**Input**: Design documents from `specs/001-natural-language-reminder/`

**Prerequisites**: `plan.md`, `spec.md`, `research.md`, `data-model.md`, `contracts/result.schema.json`, and `evals/dataset.json`

**Organization**: Tasks are grouped by user story. Shared model, transport, prompt, and validation infrastructure is established first. Each story includes tests that should fail before its implementation is complete.

## Format

- Every task uses `- [ ] Tnnn [P?] [USn?] Description with exact file paths`.
- `[P]` is used only when the task can proceed independently on different files.
- User-story tasks have `[US1]`, `[US2]`, or `[US3]`; setup, foundational, and polish tasks do not.
- Dependencies are called out in task descriptions and summarized below.

## Phase 1: Setup

**Purpose**: Establish the Java 17 Maven project and mandatory quality tooling.

- [ ] T001 Configure `Part-1B/pom.xml` for Java 17, Jackson, NetworkNT JSON Schema Validator (Draft 2020-12), JUnit 5, Maven Surefire, Checkstyle using `google_checks` with zero warnings, SpotBugs with Find Security Bugs, and the `verify` lifecycle quality gates.
- [ ] T002 [P] Add the Google Checkstyle rules configuration at `Part-1B/config/checkstyle-google.xml`; keep the file free of suppressions.

## Phase 2: Foundational

**Purpose**: Build shared types, input/model boundaries, and guaranteed result validation before any user-story work.

- [ ] T003 Define `ReminderInput` in `Part-1B/src/main/java/io/github/sunita1008/reminder/input/ReminderInput.java`; the eight-field `AgentResult` in `Part-1B/src/main/java/io/github/sunita1008/reminder/output/AgentResult.java`, `Intent` in `Part-1B/src/main/java/io/github/sunita1008/reminder/output/Intent.java`, and `ReasonCode` in `Part-1B/src/main/java/io/github/sunita1008/reminder/output/ReasonCode.java`; and sealed `AgentOutcome` in `Part-1B/src/main/java/io/github/sunita1008/reminder/api/AgentOutcome.java` plus `ModelUnavailable` in `Part-1B/src/main/java/io/github/sunita1008/reminder/api/ModelUnavailable.java` with categories `TIMEOUT`, `RATE_LIMITED`, `NETWORK`, `HTTP_ERROR`, and `PROVIDER_PROTOCOL`.
- [ ] T004 Add JUnit tests in `Part-1B/src/test/java/io/github/sunita1008/reminder/input/InputValidatorTest.java` for non-empty message, RFC 3339 reference instant with offset or `Z`, and valid IANA `ZoneId`, including malformed or invalid context.
- [ ] T005 Implement input parsing and validation in `Part-1B/src/main/java/io/github/sunita1008/reminder/input/InputValidator.java`; retain invalid time-context details for the later `CLARIFY` outcome instead of inventing a usable instant or zone.
- [ ] T006 Define `ModelClient` and environment-backed `ModelConfiguration` in `Part-1B/src/main/java/io/github/sunita1008/reminder/model/ModelClient.java` and `Part-1B/src/main/java/io/github/sunita1008/reminder/model/ModelConfiguration.java`; read provider, model, API key, and base URL only from `MODEL_PROVIDER`, `MODEL_NAME`, `MODEL_API_KEY`, and `MODEL_BASE_URL`, and never expose the key in diagnostics.
- [ ] T007 Add local-HTTP-server tests in `Part-1B/src/test/java/io/github/sunita1008/reminder/model/HttpModelClientTest.java` for successful provider responses, timeout, HTTP 429, other non-success HTTP responses, network errors, and malformed provider envelopes; assert each failure maps to the documented `ModelUnavailable` category.
- [ ] T008 Implement `HttpModelClient` in `Part-1B/src/main/java/io/github/sunita1008/reminder/model/HttpModelClient.java` with Java 17 `java.net.http.HttpClient`, bounded request timeout, configured model/provider, and no secret-bearing logs; pass T007 without requiring external network access.
- [ ] T009 Implement test-only `FakeModelClient` in `Part-1B/src/test/java/io/github/sunita1008/reminder/model/FakeModelClient.java` with ordered scripted responses and scripted unavailable outcomes; fail explicitly when the script is exhausted.
- [ ] T010 Copy `Part-1B/specs/001-natural-language-reminder/contracts/result.schema.json` to `Part-1B/src/main/resources/schemas/result.schema.json` and add a contract test at `Part-1B/src/test/java/io/github/sunita1008/reminder/contract/ResultSchemaResourceTest.java` that detects schema drift.
- [ ] T011 Add strict parser tests in `Part-1B/src/test/java/io/github/sunita1008/reminder/output/ResultParserTest.java` for malformed JSON, non-object roots, trailing prose/tokens, duplicate keys, unknown fields, and type coercion.
- [ ] T012 Implement `ResultParser` in `Part-1B/src/main/java/io/github/sunita1008/reminder/output/ResultParser.java` using Jackson configured to reject malformed, duplicate, unknown, coerced, or trailing content.
- [ ] T013 Add schema-validation tests in `Part-1B/src/test/java/io/github/sunita1008/reminder/output/JsonSchemaValidatorTest.java` for all five intent branches and all eight required fields.
- [ ] T014 Implement `JsonSchemaValidator` in `Part-1B/src/main/java/io/github/sunita1008/reminder/output/JsonSchemaValidator.java` using the checked-in Draft 2020-12 schema; reject additional properties and enforce the schema before results can be emitted.
- [ ] T015 Add invariant tests in `Part-1B/src/test/java/io/github/sunita1008/reminder/output/InvariantValidatorTest.java` for intent/null/reason relationships, non-empty required strings, echoed IANA zone, and RFC 3339 UTC `scheduled_at` strictly later than the input instant.
- [ ] T016 Implement `InvariantValidator` in `Part-1B/src/main/java/io/github/sunita1008/reminder/output/InvariantValidator.java`; reject any result violating the data-model and schema contract, including non-future scheduling.
- [ ] T017 Add prompt-boundary tests in `Part-1B/src/test/java/io/github/sunita1008/reminder/prompt/PromptBuilderTest.java` asserting that system instructions and user-supplied message content are sent as separate messages and that raw user text never enters the system instruction.
- [ ] T018 Implement `PromptBuilder` and versioned prompt resources `Part-1B/src/main/java/io/github/sunita1008/reminder/prompt/PromptBuilder.java` and `Part-1B/src/main/resources/prompts/{system.md,task.md,output-format.md}`; require one contracted JSON object, label user content untrusted, and grant no tools.
- [ ] T019 Add the architecture-boundary test in `Part-1B/src/test/java/io/github/sunita1008/reminder/contract/ModuleBoundaryTest.java`; fail if the agent can return model text without parser, schema, and invariant validation, or if model access depends on CLI/input-parsing classes.

**Checkpoint**: Shared input, model, prompt, and result-validation boundaries are testable. User-story work can now proceed independently.

## Phase 3: User Story 1 - Parse a reminder request (Priority: P1)

**Goal**: Return a validated `CREATE` proposal for a clear request, with reminder text and future instant.

**Independent Test**: With a fixed reference instant and `Asia/Kolkata`, use scripted model output for “tomorrow at 9:30 AM” and “in 2 hours”; assert schema-valid `CREATE`, semantic reminder text, correct UTC instant, echoed zone, and no side effects.

### Tests

- [ ] T020 [P] [US1] Add deterministic agent tests in `Part-1B/src/test/java/io/github/sunita1008/reminder/integration/CreateReminderTest.java` for the two specification acceptance examples, no prose around JSON, and rejection of a model result that fails schema or time invariants.

### Implementation

- [ ] T021 [US1] Implement `ReminderAgent` orchestration in `Part-1B/src/main/java/io/github/sunita1008/reminder/api/ReminderAgent.java`: validate input, build prompt, call `ModelClient`, parse, schema-validate, invariant-validate, and return `AgentOutcome.Success` only after all checks; surface service failures as `AgentOutcome.Unavailable`.
- [ ] T022 [US1] Implement the one-shot `run` entry point in `Part-1B/src/main/java/io/github/sunita1008/reminder/cli/ReminderCli.java`; accept exactly one JSON input envelope, write only the validated result JSON on success, and report `ModelUnavailable` distinctly to stderr with a non-zero exit status.

**Checkpoint**: The create flow works independently using a fake client and returns only a validated result.

## Phase 4: User Story 2 - Change an existing reminder's time (Priority: P2)

**Goal**: Propose `RESCHEDULE` or `SNOOZE` with a grounded, normalized target label and future instant, without claiming lookup or modification.

**Independent Test**: Use scripted responses for one uniquely identified reschedule and one relative snooze; verify target label, proposed UTC time, and proposal-only semantics. Verify a missing target clarifies rather than inventing one.

### Tests

- [ ] T023 [P] [US2] Add deterministic reschedule and snooze tests in `Part-1B/src/test/java/io/github/sunita1008/reminder/integration/ChangeReminderTimeTest.java` for an explicit replacement time, relative delay, normalized short target labels, missing target, future-instant constraint, and no claimed side effects.

### Implementation

- [ ] T024 [US2] Extend `Part-1B/src/main/resources/prompts/task.md` and `Part-1B/src/main/resources/prompts/output-format.md` with `RESCHEDULE`/`SNOOZE` extraction rules: `target_reference` must be a normalized short label derived from user wording, not an identifier or history lookup; missing or non-unique targets require `CLARIFY`; time changes are proposals only.

**Checkpoint**: Reschedule and snooze work without a reminder store and remain independently testable through `FakeModelClient`.

## Phase 5: User Story 3 - Resolve ambiguity and refuse unsafe actions (Priority: P3)

**Goal**: Clarify missing, invalid, conflicting, or ambiguous information without guessing; refuse unsafe or unsupported actions without reminder fields.

**Independent Test**: Script missing-time, missing-target, invalid/past/DST-time, injection, data-transfer, and out-of-scope examples; assert focused clarification or the annotated refusal reason and exact null-field invariants.

### Tests

- [ ] T025 [P] [US3] Add deterministic clarification and refusal tests in `Part-1B/src/test/java/io/github/sunita1008/reminder/integration/ClarifyAndRefuseTest.java` for missing reminder text/time/target, ambiguous and conflicting time, invalid or past time, daylight-saving gaps/overlaps, direct injection, deletion/transmission, and safe reminder subjects that merely mention deletion/transmission; assert invalid `reference_now` and invalid IANA `time_zone` each produce `CLARIFY` with `INVALID_CONTEXT`, null reminder fields, and a focused question without invoking `ModelClient`.

### Implementation

- [ ] T026 [US3] Implement deterministic invalid-context handling in `Part-1B/src/main/java/io/github/sunita1008/reminder/api/ReminderAgent.java` so malformed reference instants or invalid IANA zones return schema-valid `CLARIFY` with `INVALID_CONTEXT`, no reminder fields, and a focused question without asking the model to invent context.
- [ ] T027 [US3] Refine `Part-1B/src/main/resources/prompts/system.md` and `Part-1B/src/main/resources/prompts/task.md` to clarify instead of guessing for missing clock times, past/invalid dates, conflicting times, daylight-saving gaps/overlaps, and to ignore embedded instruction overrides while refusing direct protected-data requests.
- [ ] T028 [US3] Add `GuardrailValidator` in `Part-1B/src/main/java/io/github/sunita1008/reminder/guardrail/GuardrailValidator.java` and integrate it into `Part-1B/src/main/java/io/github/sunita1008/reminder/api/ReminderAgent.java`; enforce input-to-result safety checks for direct unsafe requests, protected-instruction attempts, and safe reminder subjects, without duplicating the schema and intent/null/reason checks owned by `JsonSchemaValidator` and `InvariantValidator`.

**Checkpoint**: Ambiguity and unsafe/out-of-scope requests never become guessed actionable results; safe reminder subjects remain parseable content.

## Phase 6: Evaluation Harness

**Purpose**: Verify the fixed corpus, score complete runs, and keep service unavailability distinct from agent quality.

- [ ] T029 [P] Add dataset contract tests in `Part-1B/src/test/java/io/github/sunita1008/reminder/eval/EvaluationDatasetTest.java` for `Part-1B/specs/001-natural-language-reminder/evals/dataset.json`: exactly 100 unique cases, 20 per intent, all six allowed categories represented, complete three-part inputs, at least 25 `GR-*` cases, non-empty requirement IDs, no free-text predicates, valid named checks and keyword-constraint shapes, and coverage of every `FR-*`, `INV-*`, `PROP-*`, and `GR-*` ID.
- [ ] T030 Implement evaluation case and dataset loading/validation in `Part-1B/src/main/java/io/github/sunita1008/reminder/eval/EvaluationDataset.java` and `Part-1B/src/main/java/io/github/sunita1008/reminder/eval/EvaluationDatasetLoader.java`; reject any corpus that violates EVAL-1 or traceability requirements.
- [ ] T031 [P] Add semantic-scoring tests in `Part-1B/src/test/java/io/github/sunita1008/reminder/eval/SemanticScorerTest.java` for `contains_all` target anchors, `clarification_keywords.any_of`, `reason_code_any_of`, each EVAL-2 named check, and schema-derived checks without exact natural-language matching.
- [ ] T032 Implement semantic and guardrail scoring in `Part-1B/src/main/java/io/github/sunita1008/reminder/eval/SemanticScorer.java`; normalize target labels and match all listed anchors, match at least one clarification keyword/phrase, accept only the annotated reason code or listed alternatives, and implement exactly the named checks from EVAL-2 with no free-text predicate evaluator.
- [ ] T033 Add evaluation-runner tests in `Part-1B/src/test/java/io/github/sunita1008/reminder/eval/EvaluationRunnerTest.java` for schema and invariant hard gates, 95.0% threshold, 100% guardrail gate, model/output failure classification, and `ModelUnavailable` making the run `BLOCKED_INCOMPLETE` with `scorePercent: null` and no agent-failure count.
- [ ] T034 Implement `EvaluationRunner` and sanitized JSON report writer in `Part-1B/src/main/java/io/github/sunita1008/reminder/eval/EvaluationRunner.java` and `Part-1B/src/main/java/io/github/sunita1008/reminder/eval/EvaluationReportWriter.java`; score only complete 100-case runs as `100 × passes / 100`, apply all EVAL-4 gates, and write `Part-1B/evidence/eval-results.json` without credentials, headers, or raw network payloads.
- [ ] T035 Add the `eval` command to `Part-1B/src/main/java/io/github/sunita1008/reminder/cli/ReminderCli.java`; load the fixed corpus, run the evaluator, and clearly distinguish complete scored results from blocked/incomplete service runs.

## Phase 7: Polish and Quality Gates

**Purpose**: Finish documentation and verify the entire implementation against the constitution.

- [ ] T036 Update `Part-1B/specs/001-natural-language-reminder/quickstart.md` and `Part-1B/README.md` with Maven commands, environment-variable names, fake-client testing, `run`/`eval` invocation, and `ModelUnavailable` behavior.
- [ ] T037 Run `mvn verify` from `Part-1B/pom.xml` and resolve all failures until JUnit 5 passes, Checkstyle reports zero warnings under `google_checks`, and SpotBugs with Find Security Bugs reports no Medium-or-higher findings; do not add suppressions or change the dataset/threshold to bypass a gate.

## Dependencies and Execution Order

### Phase dependencies

- Setup (Phase 1) precedes Foundational (Phase 2).
- Foundational (Phase 2) blocks all user stories.
- User Stories 1, 2, and 3 depend on the shared foundation and can be implemented in parallel after it; story 2 and 3 do not require story 1's implementation.
- Evaluation harness work requires the result contract and completed story behaviors. Polish depends on all desired stories and evaluation tasks.

### Task dependencies

- T002 can run alongside T001 because it touches only Checkstyle configuration; T003 follows project setup.
- T004 → T005; T006 → T007 → T008; T009 uses the shared `ModelClient` from T006.
- T010, T011, and T013 precede their associated schema/parser/invariant implementations (T012, T014, T016). T015 precedes T016.
- T017 precedes T018; T019 requires the agent/validation boundaries established in Phase 2.
- T020 → T021 → T022 for the end-to-end create path.
- T023 → T024 for reschedule/snooze behavior.
- T025 → T026/T027/T028 for clarification and guardrail behavior.
- T029 → T030; T031 → T032; T030 and T032 → T033 → T034 → T035.
- T036 can run independently during the final phase; T037 follows all code, tests, and docs.

### Parallel opportunities

- T001 and T002 can run concurrently because T002 only creates the standalone Checkstyle rules file.
- Within the foundation, input validation, model configuration/client, schema/parser, and prompt tasks can be assigned in parallel where their stated prerequisites are complete and edits do not overlap.
- After Phase 2, the three user-story phases can be implemented concurrently by separate contributors.
- Evaluation dataset validation tests and semantic scorer tests (T029 and T031) can run concurrently.
- Final documentation (T036) can run alongside evaluation implementation once paths and command names are stable.

## Independent Test Criteria

- **US1**: Fixed-time and relative-time creation requests produce the annotated `CREATE` fields, schema-valid UTC timestamps later than the reference instant, and no non-JSON output or side-effect claim.
- **US2**: A uniquely named target plus explicit replacement/delay produces the correct proposal; a missing/non-unique target produces `CLARIFY`; no ID, reminder lookup, or applied change is claimed.
- **US3**: Missing, invalid, conflicting, past, ambiguous, or DST-problem time context yields focused `CLARIFY`; direct unsafe/out-of-scope requests yield `REFUSE` with the correct reason and null reminder fields; injection text cannot override rules; safe reminder-subject text remains content.

## Implementation Strategy

1. Complete setup and foundational validation before any user-story feature code.
2. Deliver US1 as the MVP: create a reminder with a scripted fake response and validate it end to end.
3. Add US2 and US3 as independently testable behavior increments.
4. Implement the fixed-corpus evaluator; use fake clients in automated tests and live provider configuration only for live model evaluation.
5. Run the full `mvn verify` quality pipeline. An evaluation containing any `ModelUnavailable` is blocked/incomplete and cannot produce an acceptance score.

## Notes

- `[P]` tasks touch distinct files and have no unmet prerequisite.
- The production `HttpModelClient` must not be used by deterministic JUnit tests.
- `ModelUnavailable` is an operational outcome, not a sixth JSON intent. Do not serialize it as `AgentResult`, count it as an agent failure, or publish a partial score.
- Keep the evaluation dataset fixed at exactly 100 cases with the 95.0% threshold and 100% schema/invariant and guardrail gates unchanged.
