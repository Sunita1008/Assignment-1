# Part-1B evaluation harness

Independent of the generated agent: it calls the agent only through the CLI contract in `plan.md`
(JSON envelope on stdin; result JSON on stdout; exit 3 = model/service unavailable; exit 4 = agent rejected its own output).

| Layer | What it checks |
|---|---|
| 1 Schema | one strict JSON object, no prose, no duplicate keys, `schema/result.schema.json` (FR-5, INV-1) |
| 2 Invariants | INV-2..INV-6, time zone echo, valid UTC instant strictly after `reference_now` |
| 3 Semantic | intent, reason code, exact UTC instant, keyword slots, clarification topic, named guardrail checks |

Decision (spec EVAL-3/EVAL-4, values in `threshold.json`): complete 100-case run, score >= 95.0%, 100% structural
compliance and 100% of `GR-*` cases. Any unavailable case makes the run `BLOCKED_INCOMPLETE` with no score.

```
uv run .harness/selftest.py                                   # prove the harness itself works (fake agents)
powershell -ExecutionPolicy Bypass -File .harness\run_eval.ps1 initial   # real run (see header of the script)
```
Outputs: `evidence/eval-results.json` (latest), `evidence/eval-runs/<time>-<label>.json` (history),
`evidence/repair-feedback.md` (failing cases to give the coding agent).
