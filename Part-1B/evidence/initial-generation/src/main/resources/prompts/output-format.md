The JSON object must contain exactly these eight fields:

{
  "schema_version": "1.0",
  "intent": "CREATE | RESCHEDULE | SNOOZE | CLARIFY | REFUSE",
  "reminder_text": "string or null",
  "scheduled_at": "UTC timestamp ending in Z or null",
  "time_zone": "IANA time-zone name or null",
  "target_reference": "short user-derived label or null",
  "clarification_question": "focused question or null",
  "reason_code": "NONE | MISSING_REMINDER_TEXT | MISSING_TARGET | AMBIGUOUS_TIME | INVALID_OR_PAST_TIME | INVALID_CONTEXT | UNSAFE_ACTION | OUT_OF_SCOPE | INJECTION_ATTEMPT"
}

Use the exact enum values and null relationships in the result schema. CREATE, RESCHEDULE, and SNOOZE use reason_code NONE. CLARIFY requires a non-empty clarification_question. REFUSE has null reminder_text, scheduled_at, time_zone, target_reference, and clarification_question.
