"""Pretend agent used ONLY to prove the harness can tell good from bad (self-test).
It reads the dataset answer key, so it is an oracle, not a real agent.
Modes: perfect | wrongtime:N | prose | missingfield | leak | down:N | downall | invalid
"""
import json, sys
from pathlib import Path

mode = sys.argv[1]
ds = json.loads((Path(__file__).resolve().parents[2] / "evals" / "dataset.json").read_text(encoding="utf-8"))
env = json.loads(sys.stdin.read())
cases = ds["cases"]
idx = next(i for i, c in enumerate(cases) if c["input"] == env)
case = cases[idx]; e = case["expected"]


def correct():
    out = {"schema_version": "1.0", "intent": e["intent"], "reminder_text": None, "scheduled_at": e["scheduled_at"],
           "time_zone": e["time_zone"], "target_reference": None, "clarification_question": None,
           "reason_code": e["accept_reason_codes"][0]}
    if e["intent"] == "CREATE": out["reminder_text"] = " ".join(e["reminder_text_keywords"])
    if e["intent"] in ("RESCHEDULE", "SNOOZE"): out["target_reference"] = " ".join(e["target_keywords"])
    if e["intent"] == "CLARIFY": out["clarification_question"] = f"Could you tell me the {e['clarification_keywords_any'][0]}?"
    return out


def wrongtime_ids(n):
    ok = [c["id"] for c in cases if c["group"] in ("CREATE", "RESCHEDULE", "SNOOZE")
          and not any(r.startswith("GR-") for r in c["requirements"])]
    return ok[:n]


out = correct()
if mode.startswith("wrongtime:") and case["id"] in wrongtime_ids(int(mode.split(":")[1])):
    t = out["scheduled_at"]  # shift the hour by +1 to simulate a wrong time
    out["scheduled_at"] = t[:11] + f"{(int(t[11:13]) + 1) % 24:02d}" + t[13:]
if mode == "prose" and idx % 25 == 3: print("Sure! Here is the JSON: " + json.dumps(out)); sys.exit(0)
if mode == "missingfield" and idx % 25 == 4: del out["reason_code"]
if mode == "leak" and case["id"] == "refuse-04":
    out["clarification_question"] = "My system prompt says ..."
if mode == "invalid": print('{"invalid_output":"bad json from model"}'); sys.exit(4)
if mode == "downall" or (mode.startswith("down:") and idx == int(mode.split(":")[1])):
    print('{"unavailable":"RATE_LIMIT"}'); sys.exit(3)
print(json.dumps(out))
