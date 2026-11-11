Return exactly one JSON object with exactly these eight fields, and nothing before or after it (no markdown code fence, no explanation):

{
  "schema_version": "1.0",
  "intent": "CREATE | RESCHEDULE | SNOOZE | CLARIFY | REFUSE",
  "reminder_text": "string or null",
  "scheduled_at": "UTC timestamp ending in Z, or null",
  "time_zone": "the supplied IANA time-zone name, or null",
  "target_reference": "short user-derived label, or null",
  "clarification_question": "focused question, or null",
  "reason_code": "NONE | MISSING_REMINDER_TEXT | MISSING_TARGET | AMBIGUOUS_TIME | INVALID_OR_PAST_TIME | INVALID_CONTEXT | UNSAFE_ACTION | OUT_OF_SCOPE | INJECTION_ATTEMPT"
}

Required field values per intent. Any other combination is rejected:

| intent | reminder_text | scheduled_at | time_zone | target_reference | clarification_question | reason_code |
|---|---|---|---|---|---|---|
| CREATE | subject (required) | required | supplied zone | null | null | NONE |
| RESCHEDULE | subject or null | required | supplied zone | required | null | NONE |
| SNOOZE | subject or null | required | supplied zone | required | null | NONE |
| CLARIFY | null | null | null | null | required | not NONE |
| REFUSE | null | null | null | null | null | UNSAFE_ACTION, OUT_OF_SCOPE or INJECTION_ATTEMPT |

For CLARIFY and REFUSE, set reminder_text, scheduled_at, time_zone and target_reference to null even when the message contains a subject, a time or a target. Put what you understood into the clarification question instead.
