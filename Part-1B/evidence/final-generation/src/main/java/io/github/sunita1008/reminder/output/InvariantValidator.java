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
      fail("INV-1: schema_version must be 1.0");
    }
    if (result.intent() == null || result.reasonCode() == null) {
      fail("INV-1: intent and reason_code are required");
    }
    switch (result.intent()) {
      case CREATE -> validateCreate(result);
      case RESCHEDULE, SNOOZE -> validateChange(result);
      case CLARIFY -> validateClarify(result);
      case REFUSE -> validateRefuse(result);
      default -> fail("INV-1: unknown intent");
    }
    validateScheduledAt(result, input);
  }

  private void validateCreate(AgentResult result) throws OutputValidationException {
    String prefix = "INV-2: CREATE ";
    require(!isBlank(result.reminderText()), prefix + "needs reminder_text");
    require(result.scheduledAt() != null, prefix + "needs scheduled_at");
    require(result.timeZone() != null, prefix + "needs time_zone");
    require(result.targetReference() == null, prefix + "must have null target_reference");
    require(
        result.clarificationQuestion() == null, prefix + "must have null clarification_question");
    require(result.reasonCode() == ReasonCode.NONE, prefix + "must use reason_code NONE");
  }

  private void validateChange(AgentResult result) throws OutputValidationException {
    String prefix = "INV-3: " + result.intent() + " ";
    require(!isBlank(result.targetReference()), prefix + "needs target_reference");
    require(result.scheduledAt() != null, prefix + "needs scheduled_at");
    require(result.timeZone() != null, prefix + "needs time_zone");
    require(
        result.clarificationQuestion() == null, prefix + "must have null clarification_question");
    require(result.reasonCode() == ReasonCode.NONE, prefix + "must use reason_code NONE");
  }

  private void validateClarify(AgentResult result) throws OutputValidationException {
    String prefix = "INV-4: CLARIFY ";
    require(result.reminderText() == null, prefix + "must have null reminder_text");
    require(result.scheduledAt() == null, prefix + "must have null scheduled_at");
    require(result.timeZone() == null, prefix + "must have null time_zone");
    require(result.targetReference() == null, prefix + "must have null target_reference");
    require(!isBlank(result.clarificationQuestion()), prefix + "needs clarification_question");
    require(result.reasonCode() != ReasonCode.NONE, prefix + "must not use reason_code NONE");
  }

  private void validateRefuse(AgentResult result) throws OutputValidationException {
    String prefix = "INV-5: REFUSE ";
    require(result.reminderText() == null, prefix + "must have null reminder_text");
    require(result.scheduledAt() == null, prefix + "must have null scheduled_at");
    require(result.timeZone() == null, prefix + "must have null time_zone");
    require(result.targetReference() == null, prefix + "must have null target_reference");
    require(
        result.clarificationQuestion() == null, prefix + "must have null clarification_question");
    require(
        result.reasonCode() == ReasonCode.UNSAFE_ACTION
            || result.reasonCode() == ReasonCode.OUT_OF_SCOPE
            || result.reasonCode() == ReasonCode.INJECTION_ATTEMPT,
        prefix + "needs a refusal reason_code");
  }

  private void validateScheduledAt(AgentResult result, ReminderInput input)
      throws OutputValidationException {
    if (result.scheduledAt() == null) {
      return;
    }
    require(result.scheduledAt().endsWith("Z"), "INV-6: scheduled_at must end in Z");
    require(
        input.timeZone().equals(result.timeZone()),
        "FR-4: time_zone must echo the supplied zone");
    final Instant scheduled;
    try {
      scheduled = Instant.parse(result.scheduledAt());
    } catch (DateTimeParseException exception) {
      throw new OutputValidationException(
          OutputValidationException.Reason.INVARIANT_VIOLATION,
          "INV-6: scheduled_at is not a valid instant");
    }
    require(
        scheduled.isAfter(input.referenceInstant()),
        "INV-6: scheduled_at must be later than reference_now");
  }

  private boolean isBlank(String value) {
    return value == null || value.isBlank();
  }

  private void require(boolean condition, String detail) throws OutputValidationException {
    if (!condition) {
      fail(detail);
    }
  }

  private void fail(String detail) throws OutputValidationException {
    throw new OutputValidationException(
        OutputValidationException.Reason.INVARIANT_VIOLATION, detail);
  }
}
