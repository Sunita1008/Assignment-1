# Implementation Plan: Natural-Language Reminder Agent

**Branch**: `001-natural-language-reminder` | **Date**: 2026-11-11 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `specs/001-natural-language-reminder/spec.md`; implementation choices supplied by the user: Java 17, Maven, Jackson, Java JSON Schema validation library, JUnit 5, Checkstyle (`google_checks`), SpotBugs with Find Security Bugs.

## Summary

Build a stateless Java 17 reminder parser that accepts exactly one user message, offset-bearing reference instant, and IANA zone; asks a language model for one contracted JSON result; and returns only a result validated by Jackson parsing, JSON Schema, and input-relative invariants. Isolate model access behind `ModelClient`, with a `java.net.http` implementation configured by environment and a scripted fake for deterministic tests. Return model/service outages as a separate `ModelUnavailable` outcome. Evaluate the fixed 100-case corpus in schema, invariant/property, and semantic/guardrail layers; write evidence to `evidence/eval-results.json`. There is no storage, account, notification, GUI, reminder lookup, or external side effect.

## Technical Context

**Language/Version**: Java 17  
**Primary Dependencies**: Maven; Jackson; NetworkNT JSON Schema Validator (Draft 2020-12); JUnit 5; Checkstyle with `google_checks`; SpotBugs with Find Security Bugs  
**Storage**: None; evaluation evidence is a local JSON file  
**Testing**: JUnit 5 unit, contract, integration-with-fake, and evaluation tests  
**Target Platform**: Java 17 runtime on Windows, macOS, and Linux  
**Project Type**: Single Maven library/application with one-shot CLI entry point  
**Performance Goals**: No throughput SLA; one bounded model request per agent call, with explicit request timeout  
**Constraints**: No network needed by unit tests; API key only from environment; no invalid model output may reach the caller; model/service errors must be distinct from agent failures  
**Scale/Scope**: One input envelope and one result per invocation; fixed 100-case evaluation; English-only v1

## Constitution Check

*GATE: Checked before research and again after design.*

| Principle / requirement | Plan compliance |
|---|---|
| I. Modular Architecture; REQ-ARCH-001 | Separate input, model, prompt, output validation, guardrail, evaluation, and CLI packages. Agent orchestration cannot bypass result validation. Add module-boundary tests. |
| II. Schema-Guaranteed Output; REQ-OUTPUT-001/002 | Strict Jackson parser, checked-in JSON Schema, then invariant validator. Emit only validated `AgentResult`; operational and invalid-output errors are typed failures, never unchecked JSON. |
| III. Verifiable Evaluation; REQ-THRESHOLD-001; REQ-TRACE-001/002 | Versioned 100-case dataset, stable requirement IDs, frozen threshold, semantic assertions, and evidence report. ModelUnavailable blocks a run without being scored as an agent failure. |
| IV. Zero-Tolerance Quality Gates; REQ-QUALITY-001/002/003 | Maven verify binds all JUnit tests, zero-warning Google Checkstyle, and SpotBugs + Find Security Bugs. No suppressions. |
| V. Simplicity and Maintainability | One Maven project, no persistence/server/framework, Java's HTTP client, and minimal interfaces with immediate test/configuration use. |
| REQ-PLATFORM-001 / REQ-SEC-001 | Java 17 and Maven; credentials read only from `MODEL_API_KEY`; secrets excluded from fixtures, logs, and evidence. |
| REQ-FAILURE-001/002; REQ-NET-001 | Typed `ModelUnavailable` for transport/provider failures; fake-backed deterministic tests; unavailable evaluation run is incomplete, not a failed agent case. |
| REQ-GUARD-001 | Keep the corpus and threshold fixed during a run; do not touch `.harness/`; no guardrail or static-analysis suppressions. |

**Gate result**: PASS. The service-outage policy was reconciled with the user's explicit choice by revising EVAL-2 and EVAL-3 in the spec. The five JSON intents remain unchanged; `ModelUnavailable` is an operational Java outcome, not an additional intent. No other constitution exception is required.

## Architecture

1. `ReminderInput` and `InputValidator` parse one strict input envelope, validate the offset-bearing instant and IANA `ZoneId`, and preserve the original message as untrusted data.
2. `ReminderAgent` orchestrates input validation, `PromptBuilder`, `ModelClient`, parsing, schema validation, and invariant/guardrail checks in that order. There is no call path from a model response directly to output.
3. `PromptBuilder` reads static, versioned prompt resources from `src/main/resources/prompts/`. Static instructions and the output contract form the system instruction; Jackson serializes the input envelope as a separate user-data message. Prompts state that user text cannot modify policy. The model receives no tools.
4. `ModelClient` is transport-only. `HttpModelClient` uses Java 17 `HttpClient`, a finite timeout, configured provider/model/key, and an OpenAI-compatible endpoint. `FakeModelClient` is test-only and returns scripted responses or scripted `ModelUnavailable` cases.
5. `ResultParser` uses strict Jackson settings: one top-level object, duplicate/unknown fields rejected, no coercion, and no prose/trailing tokens. `JsonSchemaValidator` validates against `contracts/result.schema.json`; `InvariantValidator` checks intent/null relationships, reason codes, valid zone echo, future UTC time, and daylight-saving interpretation constraints.
6. `GuardrailValidator` enforces result-side safety invariants and refusal shape. Prompt-injection handling and distinguishing a safe reminder subject (e.g. “remind me to delete the draft”) from an instruction to perform deletion are included in prompt and evaluation cases. The agent has no tool, storage, or delivery capability.
7. `EvaluationRunner` loads the frozen dataset, runs the schema layer, invariant/property layer, and semantic/guardrail scorer, then writes a sanitized JSON report. Any `ModelUnavailable` records `UNAVAILABLE`, sets run status to `BLOCKED_INCOMPLETE`, and leaves the score null. Only a complete 100-case run can be scored or pass acceptance gates.

### Prompt construction

Keep the prompt text in `src/main/resources/prompts/` (for example `system.md`, `task.md`, and `output-format.md`) and version it with the source. Never interpolate untrusted message text into the system instructions. Send the exact input fields in a user-data message with the reference instant and zone explicit. Require a single JSON object only; parser/schema/invariant validation remain authoritative if the model violates the prompt.

### Output and failure path

Successful path: model text → strict Jackson parse → schema validation → input-relative invariant and guardrail validation → `AgentOutcome.Success(AgentResult)` → JSON serialization. Transport timeout, rate limit, network error, non-2xx HTTP response, or unusable provider envelope → `AgentOutcome.Unavailable(ModelUnavailable)` with a safe category; never convert it to CLARIFY/REFUSE or a fabricated successful result. Malformed generated JSON or failed agent-result validation is an agent/output failure and counts as a failed completed evaluation case.

## Project Structure

### Documentation and evaluation artifacts

```text
specs/001-natural-language-reminder/
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/
│   └── result.schema.json
└── evals/
    └── dataset.json

evidence/
└── eval-results.json       # generated by evaluation; never contains credentials
```

### Source Code (Part-1B project root)

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
│   │   ├── eval/            # dataset loader, scorers, runner, report writer
│   │   └── cli/             # one-shot run/eval entry point
│   └── resources/
│       ├── prompts/
│       │   ├── system.md
│       │   ├── task.md
│       │   └── output-format.md
│       └── schemas/
│           └── result.schema.json
└── test/
    ├── java/io/github/sunita1008/reminder/
    │   ├── unit/
    │   ├── contract/
    │   ├── integration/
    │   └── eval/
    └── resources/
        └── evals/
```

`FakeModelClient` lives in test sources. The published schema resource mirrors the spec contract in `contracts/result.schema.json`; a contract test prevents drift. The repository has no frontend, server, database, or notification module.

**Structure Decision**: One Maven project under `Part-1B`, with a Java API and one-shot CLI. Feature design artifacts are under this feature's `specs/001-natural-language-reminder/`; the fixed dataset is kept beside those artifacts and copied/loaded by the evaluator without being modified at runtime.

## Evaluation Protocol

- **Dataset**: Version 1.1.0 contains exactly 100 cases, 20 primary cases per supported intent; at least 25 cases carry `GR-*` requirement tags. Every case has a defined category (`normal`, `boundary`, `ambiguous`, `missing_information`, `malformed`, or `adversarial`), all three input fields, expected intent, typed semantic slot constraints or named behavior checks, and one or more exact requirement IDs.
- **Layer 1 — Schema**: Parse as one JSON object and validate the eight-field schema. No unknown fields or nonconforming nullable/enum values.
- **Layer 2 — Invariants and properties**: Check intent/null/reason relationships, valid UTC timestamp strictly after the input instant, echoed zone for actionable results, correct DST behavior, no actionable guess for ambiguous/incomplete requests, and no side-effect claims.
- **Layer 3 — Semantic score**: Compare intent and typed semantic constraints. Match target-label `contains_all` anchors after normalization; a clarification must contain at least one annotated `clarification_keywords.any_of` item, without exact-string comparison. Match `reason_code` against the exact expected code or `reason_code_any_of`. Run only the named checks defined by EVAL-2: `no_prompt_leak`, `no_side_effect_claim`, `no_prohibited_data_echo`, `no_guessed_time`, `no_model_call_on_invalid_context`, and `no_invented_id`. Cases do not use free-text predicates.
- **Aggregation**: For a complete run, score `100 × passes / 100`; require at least 95.0%, 100% schema/invariant compliance, and 100% of `GR-*` cases. A single `ModelUnavailable` makes the run `BLOCKED_INCOMPLETE`, with `scorePercent: null`; it is not an agent failure and cannot yield release acceptance.
- **Evidence**: Write dataset version/hash, run status, timestamp, per-case status and requirement IDs, gate summaries, score or null, and threshold to `evidence/eval-results.json`. Do not include API keys, auth headers, or raw network payloads.

## Complexity Tracking

No constitution violations or unnecessary projects are introduced.
