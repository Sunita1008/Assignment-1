# Quickstart: Natural-Language Reminder Agent

## Prerequisites

- JDK 21
- Maven
- For a model request: `MODEL_API_KEY`, `MODEL_PROVIDER`, and `MODEL_NAME` in the process environment (optional `MODEL_BASE_URL`).

The current setup uses the AI Credits aggregator:

```powershell
$env:MODEL_PROVIDER = "aicredits"
$env:MODEL_NAME     = "anthropic/claude-haiku-5.5"
$env:MODEL_API_KEY  = "<your AI Credits key>"   # never commit this
```

`aicredits` uses `https://api.aicredits.in/v1/`; `gemini` and `openai` remain supported. See "Model configuration" in `plan.md`. Tests use `FakeModelClient` and do not call a live model.

## Build and quality gates

From `Part-1B`:

```powershell
mvn verify
```

This runs JUnit 5, Google Checkstyle, and SpotBugs with Find Security Bugs. The executable fat JAR is `target/reminder-agent.jar`.

## Run the CLI

From `Part-1B`, pass one JSON envelope to stdin:

```powershell
@'
{"user_message":"Remind me to call the dentist tomorrow at 9:30 AM","reference_now":"2026-11-11T10:00:00+05:30","time_zone":"Asia/Kolkata"}
'@ | java -jar target/reminder-agent.jar run
```

On success, stdout contains exactly one eight-field result object conforming to `.harness/schema/result.schema.json`, and the process exits 0. A model/service outage produces `{"unavailable":"<CATEGORY>"}` and exit 3. Rejected generated output produces `{"invalid_output":"<reason>"}` and exit 4. Credentials are never included in output or logs.

The Python evaluation harness is independent of the Java agent. It owns dataset loading, scoring, and evidence; the Java agent never reads `evals/`.
