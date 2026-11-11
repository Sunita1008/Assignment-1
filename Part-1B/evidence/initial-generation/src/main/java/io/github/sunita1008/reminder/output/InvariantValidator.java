package io.github.sunita1008.reminder.output;

import io.github.sunita1008.reminder.input.ReminderInput;
import java.time.Instant;
import java.time.format.DateTimeParseException;

/** Checks intent-specific fields and constraints that depend on the input. */
public final class InvariantValidator {
  /** Rejects a result that violates intent or input-relative invariants. */
  public void validate(AgentResult result, ReminderInput input)
      throws OutputValidationException {
    if (result.schemaVersion() == null || !result.schemaVersion().equals("1.0")) {
      fail();
    }
    if (result.intent() == null || result.reasonCode() == null) {
      fail();
    }
    switch (result.intent()) {
      case CREATE -> validateCreate(result);
      case RESCHEDULE, SNOOZE -> validateChange(result);
      case CLARIFY -> validateClarify(result);
      case REFUSE -> validateRefuse(result);
      default -> fail();
    }
    validateScheduledAt(result, input);
  }

  private void validateCreate(AgentResult result) throws OutputValidationException {
    if (isBlank(result.reminderText())
        || result.scheduledAt() == null
        || result.timeZone() == null
        || result.targetReference() != null
        || result.clarificationQuestion() != null
        || result.reasonCode() != ReasonCode.NONE) {
      fail();
    }
  }

  private void validateChange(AgentResult result) throws OutputValidationException {
    if (isBlank(result.targetReference())
        || result.scheduledAt() == null
        || result.timeZone() == null
        || result.clarificationQuestion() != null
        || result.reasonCode() != ReasonCode.NONE) {
      fail();
    }
  }

  private void validateClarify(AgentResult result) throws OutputValidationException {
    if (result.reminderText() != null
        || result.scheduledAt() != null
        || result.timeZone() != null
        || result.targetReference() != null
        || isBlank(result.clarificationQuestion())
        || result.reasonCode() == ReasonCode.NONE) {
      fail();
    }
  }

  private void validateRefuse(AgentResult result) throws OutputValidationException {
    if (result.reminderText() != null
        || result.scheduledAt() != null
        || result.timeZone() != null
        || result.targetReference() != null
        || result.clarificationQuestion() != null
        || (result.reasonCode() != ReasonCode.UNSAFE_ACTION
            && result.reasonCode() != ReasonCode.OUT_OF_SCOPE
            && result.reasonCode() != ReasonCode.INJECTION_ATTEMPT)) {
      fail();
    }
  }

  private void validateScheduledAt(AgentResult result, ReminderInput input)
      throws OutputValidationException {
    if (result.scheduledAt() == null) {
      return;
    }
    if (!result.scheduledAt().endsWith("Z")
        || !input.timeZone().equals(result.timeZone())) {
      fail();
    }
    try {
      if (!Instant.parse(result.scheduledAt()).isAfter(input.referenceInstant())) {
        fail();
      }
    } catch (DateTimeParseException exception) {
      fail();
    }
  }

  private boolean isBlank(String value) {
    return value == null || value.isBlank();
  }

  private void fail() throws OutputValidationException {
    throw new OutputValidationException(OutputValidationException.Reason.INVARIANT_VIOLATION);
  }
}
