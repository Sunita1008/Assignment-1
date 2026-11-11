# Part-1B — Natural-Language Reminder Agent

A command-line agent that turns one plain-language message into one structured JSON result. It receives the message together with a reference "now" instant and an IANA time zone, and returns one of five intents: `CREATE`, `RESCHEDULE`, `SNOOZE`, `CLARIFY` or `REFUSE`.

The agent only parses requests. It does not store, look up, deliver or delete reminders, and it has no tools. Its only effect is the JSON it prints.

It was built spec-first with Spec Kit (constitution → spec → plan → tasks → analyze → implement), then checked by an independent evaluation harness against a fixed 100-case dataset.

## What's in this folder

| Path | Contents |
|---|---|
| [specs/001-natural-language-reminder/](specs/001-natural-language-reminder/) | `spec.md` (requirements FR/INV/PROP/GR/EVAL), `plan.md` (architecture, CLI contract, model configuration), `tasks.md`, `data-model.md`, `research.md`, `quickstart.md` |
| [src/](src/) | Java 21 agent and its JUnit tests |
| [src/main/resources/prompts/](src/main/resources/prompts/) | Versioned prompt files: `system.md`, `task.md`, `output-format.md` |
| [.harness/](.harness/) | Independent evaluator (Python), result schema, acceptance threshold, self-test |
| [evals/dataset.json](evals/dataset.json) | Fixed evaluation dataset, version 1.1.0, 100 cases |
| [evidence/](evidence/) | Generation snapshots, every evaluation run, and the latest repair feedback |

`evals/` and `.harness/` were committed before the implementation and have not been changed since. The agent never reads `evals/` and contains no evaluator or scorer.

## Prerequisites

- JDK 21
- Maven
- PowerShell
- [uv](https://docs.astral.sh/uv/) (only for the evaluation harness)
- An API key for an OpenAI-compatible chat-completions provider (only for live model calls; the unit tests use a fake model)

## Configuration

The agent reads its model settings from environment variables only. Never put the API key in a file in the repository.

| Variable | Required | Value used for the scored runs |
|---|---|---|
| `MODEL_PROVIDER` | yes | `aicredits` (also supported: `gemini`, `openai`) |
| `MODEL_NAME` | yes | `anthropic/claude-haiku-5.5` |
| `MODEL_API_KEY` | yes | your provider key |
| `MODEL_BASE_URL` | no | overrides the provider's default address; must be `https://` |

`aicredits` sends requests to `https://api.aicredits.in/v1/chat/completions`. In PowerShell, set the variables for the current window:

```powershell
$env:MODEL_PROVIDER = "aicredits"; $env:MODEL_NAME = "anthropic/claude-haiku-5.5"; $env:MODEL_API_KEY = "<your key>"
```

## Build

From `Part-1B`:

```powershell
mvn clean verify
```

This runs the JUnit tests (37), Google-style Checkstyle and SpotBugs with Find Security Bugs, all with zero tolerance, and builds `target/reminder-agent.jar`.

## Run the agent

Pass one JSON envelope on stdin, from `Part-1B`:

```powershell
'{"user_message":"Remind me to call the dentist tomorrow at 9:30 AM","reference_now":"2026-11-12T10:00:00+05:30","time_zone":"Asia/Kolkata"}' | java -jar target/reminder-agent.jar run
```

Output:

```json
{"schema_version":"1.0","intent":"CREATE","reminder_text":"Call the dentist","scheduled_at":"2026-11-13T04:00:00Z","time_zone":"Asia/Kolkata","target_reference":null,"clarification_question":null,"reason_code":"NONE"}
```

| Exit code | stdout | Meaning |
|---|---|---|
| 0 | one 8-field result object | Success. The result passed schema, invariant and guardrail validation. |
| 3 | `{"unavailable":"<CATEGORY>"}` | The model service failed (timeout, rate limit, network, HTTP error). Never turned into `CLARIFY` or `REFUSE`. |
| 4 | `{"invalid_output":"<REASON>"}` | The model's answer failed the agent's own validation. |

On exit 3 or 4, one diagnostic line on stderr names the HTTP status or the broken rule (for example `INV-4: CLARIFY must have null reminder_text`). The API key is never printed. The `SLF4J(W)` lines on stderr are a harmless library warning.

## Run the evaluation harness

From `Part-1B`, prove the harness itself works (fake agents, no API calls):

```powershell
powershell -ExecutionPolicy Bypass -File .harness\selftest.ps1
```

Then run the real evaluation (about 15–20 minutes for 100 cases):

```powershell
powershell -ExecutionPolicy Bypass -File .harness\run_eval.ps1 -Label <label> -Delay 6
```

A run passes only if all 100 cases complete and it meets every gate in `.harness/threshold.json`: score ≥ 95.0%, 100% schema and invariant compliance, and 100% of the 25 guardrail-tagged (`GR-*`) cases. If any case is unavailable, the run is `BLOCKED_INCOMPLETE` and gets no score.

## Results

| Run | Model | Status | Score | Schema + invariants | Guardrail cases | Decision |
|---|---|---|---|---|---|---|
| `20261112-000506-initial` | Gemini `gemini-3.5-flash` | BLOCKED_INCOMPLETE (73 unavailable) | — | — | — | No score |
| `20261112-003447-initial` | Gemini `gemini-3.5-flash` | BLOCKED_INCOMPLETE (96 unavailable) | — | — | — | No score |
| `20261112-013740-initial` | Haiku 5.5 via AI Credits | COMPLETE | 69.0 | 80% | 64% | FAIL |
| `20261112-020716-repair-1` | Haiku 5.5 via AI Credits | COMPLETE | 97.0 | 100% | 96% | FAIL |
| `20261112-023445-repair-2` | Haiku 5.5 via AI Credits | COMPLETE | **100.0** | **100%** | **100%** | **PASS** |

The Gemini free tier allowed only about 5 requests per minute, so both Gemini runs were blocked. The provider was switched to AI Credits as a recorded amendment in `plan.md` before any scored run. The key was not permitted to use `openai/gpt-5-mini`, so Claude Haiku 5.5 was used.

Each run's full report is in [evidence/eval-runs/](evidence/eval-runs/). The commit history pairs each run with the change that followed it:

| Commit | Change |
|---|---|
| `53d1c0b` | Initial generation, before any evaluation |
| `5ebadd5` | Repair 1: fixed the 31 failures from the first scored run |
| `115873f` | Repair 2: fixed the 3 failures from repair-1; this code scored 100.0 |

## Evidence

- [evidence/initial-generation/](evidence/initial-generation/): the first generated code, unchanged.
- [evidence/final-generation/](evidence/final-generation/): the code that passed (identical to commit `115873f`).
- [evidence/eval-runs/](evidence/eval-runs/): every evaluation run, including the blocked ones.
- [evidence/eval-results.json](evidence/eval-results.json) and [evidence/repair-feedback.md](evidence/repair-feedback.md): the latest run's report and failure list.
- `evidence/harness-log.md`: the run history and acceptance decision, written by the student.

## Known limitations

- The model is not fully deterministic, even at `temperature: 0`. A later run of the same code can differ from the passing run.
- Repairs were guided by failures from the same 100-case dataset that measures the result, so the score shows behaviour on this dataset; there is no separate held-out set. The prompt rules were written from the spec, not copied from test cases.
- A `SNOOZE` by a duration is computed from `reference_now`, because the agent has no access to the reminder's current time.
- Every evaluation run makes 100 paid model calls.
