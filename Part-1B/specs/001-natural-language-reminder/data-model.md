# Data Model: Natural-Language Reminder Agent

This feature is stateless: there are no reminder records, user records, or relationships to persist.

## Input

| Field | Type | Validation |
|---|---|---|
| `user_message` | string | Exactly one non-empty message. Treated as untrusted data. |
| `reference_now` | string | RFC 3339 instant with explicit offset or `Z`; parse to `Instant`. |
| `time_zone` | string | Valid IANA `ZoneId`; used for local calendar interpretation. |

The supplied offset determines the instant. The supplied IANA zone determines local-time interpretation; the input's offset is not silently substituted for that zone.

## AgentResult

The only successful agent result is the schema-versioned JSON object defined in `contracts/result.schema.json`. It has eight required fields: `schema_version`, `intent`, `reminder_text`, `scheduled_at`, `time_zone`, `target_reference`, `clarification_question`, and `reason_code`.

| Intent | Required non-null values | Required null values | Reason |
|---|---|---|---|
| `CREATE` | reminder text, future UTC instant, supplied zone | target, clarification question | `NONE` |
| `RESCHEDULE` / `SNOOZE` | normalized user-derived target label, future UTC instant, supplied zone | clarification question | `NONE` |
| `CLARIFY` | focused non-empty question | reminder text, instant, zone, target | specific non-`NONE` reason |
| `REFUSE` | none | all reminder/target fields and clarification question | `UNSAFE_ACTION`, `OUT_OF_SCOPE`, or `INJECTION_ATTEMPT` |

`scheduled_at` must parse as an RFC 3339 UTC instant ending in `Z` and be strictly later than `reference_now`. Local date/time conversion uses the supplied `ZoneId`; invalid, ambiguous, or nonexistent local times are clarified rather than guessed. Calendar dates without a specific time or an unambiguous relative duration also require clarification.

## ModelUnavailable

`ModelUnavailable` is a typed operational outcome, separate from `AgentResult` and the five-intent JSON contract. It carries a stable failure category (`TIMEOUT`, `RATE_LIMITED`, `NETWORK`, `HTTP_ERROR`, or `PROVIDER_PROTOCOL`) and a safe diagnostic message without credentials or raw authorization headers. It cannot be serialized as an agent response.

The public agent outcome is a sealed type with exactly two variants:

- `Success(AgentResult)` — emitted only after parser, schema, and invariant validation.
- `Unavailable(ModelUnavailable)` — emitted for model/service unavailability and never counted as an agent-evaluation failure.

Invalid model-generated JSON or schema/invariant violations are agent/output failures, not service outages. They must not be emitted as an unchecked result.

## EvaluationCase and EvaluationRun

An evaluation case contains a stable ID, the complete `Input`, expected intent, semantic slot constraints or behavior predicates, and one or more exact requirement IDs. The versioned dataset contains exactly 100 cases, 20 per intent, with at least 25 guardrail-tagged cases.

An evaluation run records each case as `PASS`, `FAIL`, or `UNAVAILABLE`. Any `UNAVAILABLE` case makes the run `BLOCKED_INCOMPLETE`; the run has no acceptance score and cannot pass release gates. A complete run records a binary score over all 100 cases, schema/invariant compliance, guardrail pass rate, and threshold result. Results are written to `evidence/eval-results.json` without API keys or request headers.

`ModelUnavailable` is recorded for diagnostics but is not a completed evaluation result. All 100 fixed cases remain in the run manifest; an incomplete run cannot claim the 95.0% acceptance threshold.
