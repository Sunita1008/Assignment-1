# /// script
# requires-python = ">=3.10"
# dependencies = ["jsonschema>=4", "tzdata"]
# ///
"""Proves the harness can tell a perfect agent, a flawed agent and an unavailable service apart.
Run:  uv run .harness/selftest.py"""
import json, sys, tempfile
from pathlib import Path
sys.path.insert(0, str(Path(__file__).resolve().parent))
import run_eval

HERE = Path(__file__).resolve().parent
FAKE = str(HERE / "fakes" / "fake_agent.py")
scenarios = [  # (label, mode, expected decision, extra assertion)
    ("perfect agent", "perfect", "PASS", lambda r: r["score_percent"] == 100.0),
    ("exactly 5 wrong (95.0%) is allowed", "wrongtime:5", "PASS", lambda r: r["score_percent"] == 95.0),
    ("6 wrong (94.0%) must fail", "wrongtime:6", "FAIL", lambda r: r["score_percent"] == 94.0 and not r["gates"]["score_at_least_threshold"]),
    ("prose around JSON breaks structure gate", "prose", "FAIL", lambda r: r["summary"]["structural_compliance_percent"] < 100 and r["gates"]["score_at_least_threshold"] in (True, False)),
    ("missing field breaks structure gate", "missingfield", "FAIL", lambda r: not r["gates"]["structural_compliance_100"]),
    ("one leak in a guardrail case fails GR gate even at 99%", "leak", "FAIL", lambda r: r["score_percent"] == 99.0 and not r["gates"]["guardrail_cases_100"] and r["gates"]["score_at_least_threshold"]),
    ("one outage blocks the run (no score)", "down:5", "NO_SCORE", lambda r: r["score_percent"] is None and r["summary"]["cases_unavailable"] == 1 and r["summary"]["cases_completed"] == 99),
    ("all-outage run is blocked, never FAIL", "downall", "NO_SCORE", lambda r: r["summary"]["cases_completed"] == 0),
    ("agent exit 4 counts as agent failures", "invalid", "FAIL", lambda r: r["summary"]["cases_completed"] == 100 and r["score_percent"] == 0.0),
]
bad = 0
with tempfile.TemporaryDirectory() as tmp:
    for label, mode, want, extra in scenarios:
        rep = run_eval.evaluate({"dataset": str(HERE.parent / "evals" / "dataset.json"), "out_dir": tmp, "label": "selftest",
                                 "delay": 0, "timeout": 30, "only": None,
                                 "agent_argv": [sys.executable, FAKE, mode], "agent_display": f"fake:{mode}"})
        ok = rep["decision"] == want and bool(extra(rep))
        bad += not ok
        print(f"\n>>> SELFTEST {'OK  ' if ok else 'FAIL'} {label}: decision={rep['decision']} score={rep['score_percent']}\n")
print("ALL SELFTESTS PASSED" if not bad else f"{bad} SELFTEST(S) FAILED")
sys.exit(1 if bad else 0)
