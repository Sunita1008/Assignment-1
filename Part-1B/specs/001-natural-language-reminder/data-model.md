# Data Model: Natural-Language Reminder Agent

The Java agent is stateless. It has no reminder records, user records, persistence, or evaluation dataset model.

## Input

| JSON field | Java value | Validation |
|---|---|---|
| `user_message` | `String` | Non-empty; treated as untrusted data. |
| `reference_now` | `String` and parsed `Instant` | RFC 3339 instant with explicit offset or `Z`. |
| `time_zone` | `String` and parsed `ZoneId` | Valid IANA time-zone identifier; used for local interpretation. |

The supplied offset determines the instant. The supplied IANA zone determines local-time interpretation; the input offset is not silently substituted for that zone.

## AgentResult

The only successful result is the eight-field JSON object defined by the protected `Part-1B/.harness/schema/result.schema.json`: `schema_version`, `intent`, `reminder_text`, `scheduled_at`, `time_zone`, `target_reference`, `clarification_question`, and `reason_code`. `schema_version` is exactly `"1.0"`. Every field is present; nullable fields are JSON `null`, and no additional properties are allowed.

| Intent | Required non-null values | Required null values | Reason |
|---|---|---|---|
| `CREATE` | Reminder text, future UTC instant, supplied zone | Target, clarification question | `NONE` |
| `RESCHEDULE` / `SNOOZE` | User-derived target label, future UTC instant, supplied zone | Clarification question | `NONE` |
| `CLARIFY` | Focused non-empty question | Reminder text, instant, zone, target | Specific non-`NONE` reason |
| `REFUSE` | None | Reminder, time, zone, target, clarification question | `UNSAFE_ACTION`, `OUT_OF_SCOPE`, or `INJECTION_ATTEMPT` |

Every non-null `scheduled_at` is an RFC 3339 UTC instant ending in `Z` and strictly later than `reference_now`.

## ModelUnavailable and CLI error envelopes

`ModelUnavailable` is an operational exception, not an intent or `AgentResult`. Its categories are `TIMEOUT`, `RATE_LIMITED`, `NETWORK`, `HTTP_ERROR`, and `PROVIDER_PROTOCOL`. The CLI maps it to `{"unavailable":"<CATEGORY>"}` and exit code 3.

Invalid model-generated JSON or a failed schema/invariant/guardrail check is an output-validation failure. The CLI maps it to `{"invalid_output":"<reason>"}` and exit code 4. Neither CLI error envelope is an `AgentResult`.

## Evaluation ownership

Evaluation cases, run reports, scoring, thresholds, and evidence belong to the independent Python harness in `Part-1B/.harness/`. The Java agent does not load `Part-1B/evals/dataset.json` and has no evaluation-case or evaluation-run classes.
