package io.github.sunita1008.reminder.output;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class ResultParserTest {
  private static final String VALID_RESULT =
      """
      {"schema_version":"1.0","intent":"CLARIFY","reminder_text":null,
       "scheduled_at":null,"time_zone":null,"target_reference":null,
       "clarification_question":"What time?","reason_code":"AMBIGUOUS_TIME"}
      """;
  private final ResultParser parser = new ResultParser();

  @Test
  void parsesOneContractedObject() throws Exception {
    ParsedResult parsed = parser.parse(VALID_RESULT);

    assertEquals(Intent.CLARIFY, parsed.result().intent());
    assertEquals(ReasonCode.AMBIGUOUS_TIME, parsed.result().reasonCode());
  }

  @Test
  void rejectsInvalidJsonAndTrailingContent() {
    assertThrows(
        OutputValidationException.class,
        () -> parser.parse("not json"));
    assertThrows(
        OutputValidationException.class,
        () -> parser.parse(VALID_RESULT + "\nextra"));
  }

  @Test
  void rejectsDuplicateUnknownAndMissingFields() {
    String duplicateIntent =
        VALID_RESULT.replace(
            "\"intent\":\"CLARIFY\"", "\"intent\":\"CLARIFY\",\"intent\":\"REFUSE\"");
    String unknownField =
        VALID_RESULT.replace(
            "\"reason_code\":\"AMBIGUOUS_TIME\"",
            "\"reason_code\":\"AMBIGUOUS_TIME\",\"extra\":1");
    String missingField = VALID_RESULT.replace(",\"time_zone\":null", "");

    assertThrows(
        OutputValidationException.class,
        () -> parser.parse(duplicateIntent));
    assertThrows(
        OutputValidationException.class,
        () -> parser.parse(unknownField));
    assertThrows(
        OutputValidationException.class,
        () -> parser.parse(missingField));
  }

  @Test
  void classifiesInvalidEnumAsSchemaViolation() {
    OutputValidationException exception =
        assertThrows(
            OutputValidationException.class,
            () -> parser.parse(VALID_RESULT.replace("CLARIFY", "UNKNOWN")));

    assertEquals(OutputValidationException.Reason.SCHEMA_VIOLATION, exception.reason());
  }
}
