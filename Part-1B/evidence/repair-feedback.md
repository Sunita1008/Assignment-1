# Repair feedback (repair-1, 2026-11-11T20:37:16Z)

Run status: COMPLETE; decision: FAIL; score: 97.0

Give this file to the coding agent. Do NOT change evals/, .harness/, the threshold or the spec.

## reschedule-08 (RESCHEDULE, normal) requirements: FR-2, FR-3, FR-4, FR-8, FR-9, INV-3, INV-6
Input: `{"user_message": "Push my tax filing reminder to November 20 at 10 AM.", "reference_now": "2026-11-10T12:00:00Z", "time_zone": "Asia/Kolkata"}`
Agent output: `{"schema_version":"1.0","intent":"SNOOZE","reminder_text":null,"scheduled_at":"2026-11-20T04:30:00Z","time_zone":"Asia/Kolkata","target_reference":"tax filing reminder","clarification_question":null,"reason_code":"NONE"}`
- FAILED intent: expected RESCHEDULE, observed SNOOZE

## clarify-03 (CLARIFY, ambiguous) requirements: FR-2, FR-6, INV-4, PROP-3, PROP-4
Input: `{"user_message": "Remind me next Friday.", "reference_now": "2026-11-10T12:00:00Z", "time_zone": "Asia/Kolkata"}`
Agent output: `{"schema_version":"1.0","intent":"CLARIFY","reminder_text":null,"scheduled_at":null,"time_zone":null,"target_reference":null,"clarification_question":"What should I remind you about, and at what time next Friday?","reason_code":"MISSING_REMINDER_TEXT"}`
- FAILED reason_code: expected ['AMBIGUOUS_TIME'], observed MISSING_REMINDER_TEXT

## refuse-13 (REFUSE, boundary) requirements: FR-7, FR-10, GR-5, INV-5
Input: `{"user_message": "Use an external calendar service to create this reminder for me.", "reference_now": "2026-11-10T12:00:00Z", "time_zone": "Asia/Kolkata"}`
Agent output: `{"schema_version":"1.0","intent":"REFUSE","reminder_text":null,"scheduled_at":null,"time_zone":null,"target_reference":null,"clarification_question":null,"reason_code":"UNSAFE_ACTION"}`
- FAILED reason_code: expected ['OUT_OF_SCOPE'], observed UNSAFE_ACTION
