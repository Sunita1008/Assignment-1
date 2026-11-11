package io.github.sunita1008.reminder.output;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.sunita1008.reminder.input.InputValidator;
import io.github.sunita1008.reminder.input.ReminderInput;
import org.junit.jupiter.api.Test;

class InvariantValidatorTest {
  private final InvariantValidator validator = new InvariantValidator();
  private final ReminderInput input = validInput();

  @Test
  void acceptsFutureUtcCreateWithZoneEcho() {
    AgentResult result =
        new AgentResult(
            "1.0",
            Intent.CREATE,
            "Call the dentist",
            "2026-11-12T04:00:00Z",
            "Asia/Kolkata",
            null,
            null,
            ReasonCode.NONE);

    assertDoesNotThrow(() -> validator.validate(result, input));
  }

  @Test
  void rejectsPastScheduleAndIncorrectIntentShape() {
    AgentResult past =
        new AgentResult(
            "1.0",
            Intent.CREATE,
            "Call the dentist",
            "2026-11-11T04:00:00Z",
            "Asia/Kolkata",
            null,
            null,
            ReasonCode.NONE);
    AgentResult missingQuestion =
        new AgentResult(
            "1.0", Intent.CLARIFY, null, null, null, null, null, ReasonCode.AMBIGUOUS_TIME);

    assertThrows(OutputValidationException.class, () -> validator.validate(past, input));
    assertThrows(
        OutputValidationException.class, () -> validator.validate(missingQuestion, input));
  }

  private static ReminderInput validInput() {
    try {
      return new InputValidator()
          .parse(
              """
              {"user_message":"Call the dentist","reference_now":"2026-11-11T10:00:00+05:30",
               "time_zone":"Asia/Kolkata"}
              """);
    } catch (Exception exception) {
      throw new IllegalStateException(exception);
    }
  }
}
