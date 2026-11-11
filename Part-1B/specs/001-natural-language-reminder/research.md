# Research: Natural-Language Reminder Agent

## Decisions

### Java 17 and Maven application

- **Decision**: Implement a small, single-project Java 17 application with Maven and a one-shot command-line entry point. Keep the agent callable as a Java API so tests and future callers do not depend on stdin/stdout.
- **Rationale**: This meets the platform requirements without adding a GUI, server, persistence layer, or deployment framework.
- **Alternatives considered**: A web service would add endpoints and operations not requested by the spec; a CLI-only design would make the orchestration harder to reuse and test.

### Model access through `ModelClient`

- **Decision**: Define a narrow `ModelClient` interface that accepts prompt messages and returns provider text. The production `HttpModelClient` uses Java 17 `java.net.http.HttpClient`; a test-only `FakeModelClient` returns an ordered, scripted response sequence.
- **Rationale**: The seam isolates network behavior from interpretation logic and allows deterministic tests with no network. Production configuration reads `MODEL_PROVIDER`, `MODEL_NAME`, and `MODEL_API_KEY`; `MODEL_BASE_URL` selects the endpoint for the documented OpenAI-compatible chat-completions protocol. Do not log the key or full request headers.
- **Alternatives considered**: Provider SDKs would couple the implementation to a vendor; a second provider-specific client is not needed for the initial compatible protocol.

### Failure taxonomy and evaluation

- **Decision**: The agent returns a typed `AgentOutcome`: either a schema-validated `AgentResult` or a distinct `ModelUnavailable` outcome. Timeout, rate-limit, network, non-success HTTP, and unusable provider-response failures map to `ModelUnavailable`; malformed model-generated result JSON remains an agent/output-validation failure. The evaluation runner records unavailable cases and marks the run `BLOCKED_INCOMPLETE`, with a null acceptance score. It does not count unavailable cases as agent failures or publish a partial acceptance score.
- **Rationale**: This implements the user's clarification and constitution requirements without weakening the spec's fixed corpus or binary scoring for completed cases. An unavailable case is never removed from the corpus. `ModelUnavailable` is an operational Java outcome, not a sixth JSON intent and not a schema-valid agent response.
- **Alternatives considered**: Counting outages as model-quality failures confounds infrastructure with agent behavior; dropping unavailable cases and scoring a smaller denominator could inflate acceptance.

### JSON parsing and schema validation

- **Decision**: Use Jackson for strict JSON parsing/serialization and NetworkNT JSON Schema Validator with a checked-in Draft 2020-12 schema. Reject trailing text, duplicate keys, unknown properties, type coercion, and invalid schema values. Apply Java invariant checks after schema validation and before returning an `AgentResult`.
- **Rationale**: Schema validation handles the published structural contract; invariant validation handles constraints that depend on the input reference instant and intent relationships.
- **Alternatives considered**: Hand-written field validation alone duplicates schema rules and is harder to audit; the schema alone cannot express every time-dependent invariant.

### Prompt construction and guardrails

- **Decision**: Keep versioned prompt resources under `src/main/resources/prompts/`. `PromptBuilder` loads static system/task instructions and serializes the three-field input envelope as a separate user-data message. The model has no tools. The application validates every result; it never relies on prompt wording as the only output safeguard.
- **Rationale**: Separating instructions from user content makes the trust boundary explicit and enables prompt review/versioning. Output validation enforces the schema, time, nullability, and refusal invariants independently of model behavior.
- **Alternatives considered**: Building instructions by concatenating raw message text into a system prompt blurs authority and data; giving the model tools would violate GR-5.

### Evaluation harness and evidence

- **Decision**: Store a versioned, fixed 100-case corpus in `evals/dataset.json` (20 primary cases for each intent). Each case has a complete input envelope, semantic expected outcome, and requirement IDs. The evaluator runs schema, invariant/property, and semantic/guardrail layers in order; it checks full corpus traceability and emits `evidence/eval-results.json`.
- **Rationale**: This directly implements EVAL-1 through EVAL-5 and the constitution's auditability requirement. Semantic comparisons use annotated slots and predicates rather than exact clarification wording.
- **Alternatives considered**: Exact string matching is brittle for natural language; relying only on model self-assessment is not an independent evaluator.

### Clarified product behavior

- **Decision**: V1 is English-only. `target_reference` is a normalized short label grounded in the message, not a quote or identifier. If a calendar date has no specific clock time and no unambiguous relative duration supplies an instant, ask for clarification; never invent a default hour.
- **Rationale**: These are the user's explicit clarifications and are reflected in the feature spec.
- **Alternatives considered**: Multilingual support and a default reminder hour require additional policy and evaluation coverage not in the clarified scope.

## Resolved Technical Context

No unresolved technology choices remain for the planned v1. The provider endpoint must implement the documented OpenAI-compatible chat-completions request/response format; incompatible provider protocols require a separately scoped adapter.
