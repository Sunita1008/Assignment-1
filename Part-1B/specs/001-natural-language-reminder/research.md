# Research: Natural-Language Reminder Agent

## Decisions

### Java 21 Maven application

- **Decision**: Implement a Java 21 Maven application with an API and one-shot executable CLI packaged as `target/reminder-agent.jar`.
- **Rationale**: The CLI is the independent harness boundary; an API keeps core orchestration testable without subprocesses.
- **Alternatives considered**: A server, persistence layer, or provider SDK would add scope and coupling not required by the feature.

### Model access and configuration

- **Decision**: Keep model access behind `ModelClient`. Read `MODEL_API_KEY`, `MODEL_PROVIDER`, and `MODEL_NAME` from the process environment only. Use the OpenAI-compatible Gemini endpoint from `plan.md` for `MODEL_PROVIDER=gemini`; use the OpenAI chat-completions endpoint for `MODEL_PROVIDER=openai`. Keep `FakeModelClient` in test sources.
- **Rationale**: The seam isolates network behavior and enables deterministic tests. The API key is used only in the authorization header and is never logged or written.
- **Alternatives considered**: Provider SDKs would couple the implementation to vendors; reading credentials from files or command-line arguments would expand the secret-handling surface.

### Failure and CLI contract

- **Decision**: A model/service failure is a typed `ModelUnavailable` with a stable category. The CLI emits `{"unavailable":"<CATEGORY>"}` and exits 3. Invalid generated output emits `{"invalid_output":"<reason>"}` and exits 4. A validated `AgentResult` is emitted as the eight-field schema object and exits 0.
- **Rationale**: Operational failures remain distinct from model-produced intents and rejected output. Failure envelopes are CLI protocol objects, not `AgentResult`s.
- **Alternatives considered**: Converting outages into `CLARIFY` or `REFUSE` misrepresents the failure; emitting malformed model text would bypass validation.

### Parsing and schema validation

- **Decision**: Use Jackson for strict input/result parsing and serialization, NetworkNT for Draft 2020-12 validation, and Java invariants for intent-dependent nullability and input-relative scheduling. Package the protected `.harness/schema/result.schema.json` into the runnable JAR during the build without modifying the source schema.
- **Rationale**: The schema enforces the structural contract; Java checks enforce relationships that depend on the input instant and time zone.
- **Alternatives considered**: A second independently maintained schema could drift; hand-written checks alone would duplicate structural rules.

### Prompt construction and guardrails

- **Decision**: Keep versioned prompt resources in `src/main/resources/prompts/`. Send static system instructions separately from a JSON-serialized user-data message. Give the model no tools, and validate every generated result before output.
- **Rationale**: The trust boundary remains explicit and prompt instructions are not the only enforcement mechanism.
- **Alternatives considered**: Interpolating raw user text into system instructions or granting model tools would violate the feature's trust and side-effect boundaries.

### Independent evaluation

- **Decision**: Evaluation belongs exclusively to the Python harness under `Part-1B/.harness/`, which uses the protected `Part-1B/evals/dataset.json` v1.1.0. The Java agent contains no evaluator, scorer, dataset loader, or evaluation report writer.
- **Rationale**: Evaluation remains independent of the agent implementation; the fixed corpus and harness are read-only inputs.
- **Alternatives considered**: Embedding evaluation in the agent would violate the CLI separation and could let agent changes affect scoring.

### Product behavior

- **Decision**: V1 is English-only. `target_reference` is a short label grounded in the message, not an identifier. A date without a specific time or unambiguous relative duration requires clarification. Rescheduling and snoozing produce proposals only.
- **Rationale**: No reminder inventory, identity, authorization context, or prior conversation is available.
- **Alternatives considered**: Default clock times, record lookup, and side-effectful operations require separate policy and scope.
