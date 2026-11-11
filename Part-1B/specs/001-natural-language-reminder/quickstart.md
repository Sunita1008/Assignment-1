# Quickstart: Natural-Language Reminder Agent

## Prerequisites

- JDK 17
- Maven
- For a live model call only: `MODEL_PROVIDER`, `MODEL_NAME`, `MODEL_API_KEY`, and `MODEL_BASE_URL` for an OpenAI-compatible chat-completions endpoint.

The unit and evaluation tests use `FakeModelClient` and do not require network access or real credentials.

## Build and quality gates

From `Part-1B`:

```powershell
mvn verify
```

The verify lifecycle runs JUnit 5, Checkstyle with `google_checks` and zero warnings, and SpotBugs with Find Security Bugs. No gate is advisory.

## Exercise a deterministic agent request

Use the public agent API in a test with a `FakeModelClient` scripted with one response from `evals/dataset.json`. The test must assert the returned `AgentResult`, schema validation, and invariant validation. The fake must consume exactly the scripted response expected by the test and fail explicitly if no response is available.

## Run the evaluation

With valid model environment variables:

```powershell
mvn exec:java -Dexec.args="eval evals/dataset.json"
```

The runner validates the fixed dataset, executes the three scoring layers, and writes `evidence/eval-results.json`. A run is complete only when all 100 cases return agent results. If a case returns `ModelUnavailable`, the report records the category, marks the run `BLOCKED_INCOMPLETE`, leaves the acceptance score null, and does not count that case as an agent failure. No acceptance claim is made for an incomplete run.

## Manual request

Invoke the one-shot `run` command with one JSON input envelope containing exactly `user_message`, `reference_now`, and `time_zone`. A successful call writes only the validated eight-field result JSON. A model outage is surfaced as the distinct `ModelUnavailable` operational outcome and must not be serialized as one of the five intents.

See [the result contract](contracts/result.schema.json), [data model](data-model.md), and [fixed evaluation dataset](evals/dataset.json).
