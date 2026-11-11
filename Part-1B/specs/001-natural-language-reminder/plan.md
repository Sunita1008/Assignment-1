# Implementation Plan: Natural-Language Reminder Agent

**Branch**: `001-natural-language-reminder` | **Date**: 2026-11-11 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `specs/001-natural-language-reminder/spec.md`; implementation choices supplied by the user: Java 21, Maven, Jackson, Java JSON Schema validation library, JUnit 5, Checkstyle (`google_checks`), SpotBugs with Find Security Bugs.

## Summary

Build a stateless Java 21 reminder agent that accepts one user message, offset-bearing reference instant, and IANA time zone; asks a language model for one contracted JSON result; validates that result; and returns exactly one result JSON object through a one-shot CLI. Keep model access behind the existing `ModelClient`, with `FakeModelClient` confined to tests and `ModelUnavailable` retained as the typed service-outage outcome. Evaluation is performed only by the independent Python harness in `Part-1B/.harness/` against `Part-1B/evals/dataset.json` (v1.1.0). The agent has no evaluator or scoring component and never reads the dataset. There is no storage, account, notification, GUI, reminder lookup, or external side effect.

## Technical Context

**Language/Version**: Java 21
**Primary Dependencies**: Maven; Jackson; NetworkNT JSON Schema Validator (Draft 2020-12); JUnit 5; Checkstyle with `google_checks`; SpotBugs with Find Security Bugs  
**Storage**: None
**Testing**: JUnit 5 unit, contract, integration-with-fake, and CLI contract tests; evaluation runs independently through the protected Python harness
**Target Platform**: Java 21 runtime on Windows, macOS, and Linux
**Project Type**: Single Maven application with one-shot CLI entry point
**Performance Goals**: No throughput SLA; one bounded model request per agent call, with explicit request timeout  
**Constraints**: No network needed by unit tests; API key only from the environment; no invalid model output may reach the caller; model/service errors must be distinct from invalid agent output; `.harness/` and `evals/` are protected, read-only inputs for implementation
**Scale/Scope**: One input envelope and one result per invocation; English-only v1

## Agent CLI contract

- Build `target/reminder-agent.jar`. The harness invokes `java -jar target/reminder-agent.jar run`.
- The harness writes one JSON envelope to stdin: `{"user_message":"...","reference_now":"...","time_zone":"..."}`.
- On success, the agent prints exactly one result JSON object to stdout and exits with code 0.
- If the model or service is unavailable (timeout, rate limit, network error, or non-2xx response), print `{"unavailable":"<CATEGORY>"}` and exit with code 3. Never turn an outage into `CLARIFY` or `REFUSE`.
- If output fails the agent's own validation, print `{"invalid_output":"<reason>"}` and exit with code 4.
- The result JSON fields are `schema_version` (value `"1.0"`), `intent`, `reason_code`, `scheduled_at` (UTC timestamp ending in `Z`, or `null`), `time_zone`, `reminder_text`, `target_reference`, and `clarification_question`, per `.harness/schema/result.schema.json`.

## Model configuration

Read `MODEL_API_KEY`, `MODEL_PROVIDER`, and `MODEL_NAME` from the environment, plus the optional `MODEL_BASE_URL`. All providers are reached through the OpenAI-compatible `chat/completions` endpoint. Never log or write `MODEL_API_KEY`.

| `MODEL_PROVIDER` | Default base URL |
|---|---|
| `aicredits` | `https://api.aicredits.in/v1/` |
| `gemini` | `https://generativelanguage.googleapis.com/v1beta/openai/` |
| `openai` | `https://api.openai.com/v1/` |

- `MODEL_BASE_URL`, when set, overrides the provider default. It must be an absolute `https` URL with a host; otherwise the agent reports `{"unavailable":"PROVIDER_PROTOCOL"}` (exit 3). A missing trailing slash is added.
- `MODEL_PROVIDER` stays required because the harness records it in the run evidence.
- Reasoning model families (`gpt-5*`, `o1*`, `o3*`, `o4*`, including a `vendor/` prefix such as `openai/gpt-5-mini`) accept only the default sampling temperature, so the request omits `temperature` for them; other models keep `temperature: 0`.
- The request timeout is 60 seconds (connect timeout 5 seconds), because reasoning models take longer to answer.

**Amendment (2026-11-12, before any scored run):** The provider changed from Gemini (`gemini-3.5-flash`) to the AI Credits aggregator (`MODEL_PROVIDER=aicredits`, `MODEL_NAME=openai/gpt-5-mini`). The Gemini free tier allowed only about 5 requests per minute and a small daily cap, so both evaluation attempts ended `BLOCKED_INCOMPLETE` (kept in `evidence/eval-runs/`). This is a configuration and transport change only. The spec, prompts, output contract, dataset, harness, and threshold are unchanged.

## Constitution Check

*GATE: Checked before research and again after design.*

| Principle / requirement | Plan compliance |
|---|---|
| I. Modular Architecture; REQ-ARCH-001 | Separate input, model, prompt, output validation, guardrail, and CLI packages. Agent orchestration cannot bypass result validation. Add module-boundary tests. |
| II. Schema-Guaranteed Output; REQ-OUTPUT-001/002 | Strict Jackson parser, `.harness/schema/result.schema.json`, then invariant validator. Emit only validated results; operational and invalid-output errors use the CLI contract. |
| III. Verifiable Evaluation; REQ-THRESHOLD-001; REQ-TRACE-001/002 | Evaluation belongs exclusively to the independent Python harness in `.harness/`, using the protected `evals/dataset.json` v1.1.0. The Java agent neither reads the dataset nor implements evaluation or scoring. |
| IV. Zero-Tolerance Quality Gates; REQ-QUALITY-001/002/003 | Maven verify binds JUnit tests, zero-warning Google Checkstyle, and SpotBugs + Find Security Bugs. No suppressions. |
| V. Simplicity and Maintainability | One Maven project, no persistence/server/framework, Java's HTTP client, and minimal interfaces with immediate test/configuration use. |
| REQ-PLATFORM-001 / REQ-SEC-001 | Java 21 and Maven; credentials read only from `MODEL_API_KEY`; secrets excluded from fixtures and logs. |
| REQ-FAILURE-001/002; REQ-NET-001 | Preserve `ModelUnavailable` for transport/provider failures; fake-backed deterministic tests; report outages with the unavailable CLI response and exit code 3. |
| REQ-GUARD-001 | Treat `.harness/` and `evals/` as protected, read-only inputs; no guardrail or static-analysis suppressions. |

**Gate result**: PASS. The five JSON intents remain unchanged; `ModelUnavailable` is an operational Java outcome, not an additional intent. The Python harness owns all evaluation and scoring. No other constitution exception is required.

## Architecture

1. `ReminderInput` and `InputValidator` parse one strict input envelope, validate the offset-bearing instant and IANA `ZoneId`, and preserve the original message as untrusted data.
2. `ReminderAgent` orchestrates input validation, `PromptBuilder`, `ModelClient`, parsing, schema validation, and invariant/guardrail checks in that order. There is no call path from a model response directly to output.
3. `PromptBuilder` reads static, versioned prompt resources from `src/main/resources/prompts/`. Static instructions and the output contract form the system instruction; Jackson serializes the input envelope as a separate user-data message. Prompts state that user text cannot modify policy. The model receives no tools.
4. Preserve the existing `ModelClient` and `ModelUnavailable` behavior. `HttpModelClient` uses Java 21 `HttpClient`, a finite timeout, configured provider/model/key, and an OpenAI-compatible endpoint. `FakeModelClient` remains test-only and returns scripted responses or scripted `ModelUnavailable` cases.
5. `ResultParser` uses strict Jackson settings: one top-level object, duplicate/unknown fields rejected, no coercion, and no prose/trailing tokens. Validate against `.harness/schema/result.schema.json`; `InvariantValidator` checks intent/null relationships, reason codes, valid zone echo, future UTC time, and daylight-saving interpretation constraints.
6. `GuardrailValidator` enforces result-side safety invariants and refusal shape. Prompt-injection handling and distinguishing a safe reminder subject (e.g. “remind me to delete the draft”) from an instruction to perform deletion are included in prompt and agent tests. The agent has no tool, storage, or delivery capability.
7. The CLI maps successful validation to one result JSON object and exit code 0, service/model outages to the unavailable object and exit code 3, and failed output validation to the invalid-output object and exit code 4. It does not load or access the evaluation dataset.

### Prompt construction

Keep the prompt text in `src/main/resources/prompts/` (for example `system.md`, `task.md`, and `output-format.md`) and version it with the source. Never interpolate untrusted message text into the system instructions. Send the exact input fields in a user-data message with the reference instant and zone explicit. Require a single JSON object only; parser/schema/invariant validation remain authoritative if the model violates the prompt.

### Output and failure path

Successful path: model text → strict Jackson parse → schema validation → input-relative invariant and guardrail validation → JSON serialization. Transport timeout, rate limit, network error, non-2xx HTTP response, or unusable provider envelope produces the unavailable JSON object and exit code 3; never convert it to `CLARIFY`/`REFUSE` or a fabricated successful result. Malformed generated JSON or failed agent-result validation produces the invalid-output JSON object and exit code 4.

## Project Structure

### Protected inputs (read-only for implementation)

```text
.harness/
├── run_eval.py
└── schema/
    └── result.schema.json
evals/
└── dataset.json             # v1.1.0
```

The independent Python harness runs against the protected dataset. Do not modify these paths, copy the dataset into Java resources, or have the agent read it.

### Agent source code (Part-1B project root)

```text
pom.xml
config/
└── checkstyle-google.xml
src/
├── main/
│   ├── java/io/github/sunita1008/reminder/
│   │   ├── api/             # ReminderAgent, AgentOutcome, ModelUnavailable
│   │   ├── input/           # ReminderInput and input validation
│   │   ├── model/           # ModelClient, HTTP implementation and configuration
│   │   ├── prompt/          # PromptBuilder and resource loading
│   │   ├── output/          # AgentResult, parser, schema and invariant validation
│   │   ├── guardrail/       # Result-side safety checks
│   │   └── cli/             # one-shot run entry point
│   └── resources/
│       └── prompts/
│           ├── system.md
│           ├── task.md
│           └── output-format.md
└── test/
    ├── java/io/github/sunita1008/reminder/
    │   ├── unit/
    │   ├── contract/
    │   └── integration/
    └── resources/
```

`FakeModelClient` lives in test sources. The agent validates output against the protected `.harness/schema/result.schema.json`; `.harness/` and `evals/` are not implementation-owned or writable by the Java agent. The repository has no frontend, server, database, evaluator, scoring component, or notification module.

**Structure Decision**: One Maven project under `Part-1B`, with a Java 21 API and one-shot CLI. Feature design artifacts are under `specs/001-natural-language-reminder/`. The independent Python harness and its dataset remain protected at the project root and are never loaded by the agent.

## Evaluation Ownership

The independent Python harness at `Part-1B/.harness/run_eval.py` evaluates the agent against `Part-1B/evals/dataset.json` (v1.1.0). The Java project contains no `EvaluationRunner`, dataset loader, evaluator, scorer, evaluation report writer, or evaluation test suite. Evaluation thresholds and evidence are owned by the harness, not by the agent.

## Complexity Tracking

No constitution violations or unnecessary projects are introduced.
