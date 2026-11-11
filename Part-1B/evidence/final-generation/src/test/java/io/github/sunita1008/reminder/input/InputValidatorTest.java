package io.github.sunita1008.reminder.input;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class InputValidatorTest {
  private final InputValidator validator = new InputValidator();

  @Test
  void parsesOffsetAndIanaZone() throws Exception {
    ReminderInput input =
        validator.parse(
            """
            {"user_message":"Call the dentist","reference_now":"2026-11-11T10:00:00+05:30",
             "time_zone":"Asia/Kolkata"}
            """);

    assertEquals("Call the dentist", input.userMessage());
    assertEquals("2026-11-11T04:30:00Z", input.referenceInstant().toString());
    assertEquals("Asia/Kolkata", input.zoneId().getId());
  }

  @Test
  void rejectsMalformedOrIncompleteEnvelope() {
    assertThrows(InputValidationException.class, () -> validator.parse("{"));
    assertThrows(
        InputValidationException.class,
        () ->
            validator.parse(
                """
                {"user_message":"x","reference_now":"2026-11-11T10:00:00Z"}
                """));
    assertThrows(
        InputValidationException.class,
        () ->
            validator.parse(
                """
                {"user_message":"x","reference_now":"2026-11-11T10:00:00Z",
                 "time_zone":"UTC","extra":true}
                """));
  }

  @Test
  void rejectsInvalidReferenceAndTimeZone() {
    assertThrows(
        InputValidationException.class,
        () ->
            validator.parse(
                """
                {"user_message":"x","reference_now":"2026-11-11T10:00:00",
                 "time_zone":"Asia/Kolkata"}
                """));
    assertThrows(
        InputValidationException.class,
        () ->
            validator.parse(
                """
                {"user_message":"x","reference_now":"2026-11-11T10:00:00Z",
                 "time_zone":"Not/AZone"}
                """));
  }
}
