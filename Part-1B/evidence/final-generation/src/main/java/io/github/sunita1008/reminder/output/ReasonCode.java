package io.github.sunita1008.reminder.output;

/** Stable result reason codes from the output contract. */
public enum ReasonCode {
  NONE,
  MISSING_REMINDER_TEXT,
  MISSING_TARGET,
  AMBIGUOUS_TIME,
  INVALID_OR_PAST_TIME,
  INVALID_CONTEXT,
  UNSAFE_ACTION,
  OUT_OF_SCOPE,
  INJECTION_ATTEMPT
}
