package io.github.sunita1008.reminder.output;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

class JsonSchemaValidatorTest {
  private final JsonSchemaValidator validator = new JsonSchemaValidator();
  private final ObjectMapper mapper = new ObjectMapper();

  @Test
  void acceptsContractedResult() throws Exception {
    JsonNode result =
        mapper.readTree(
            """
            {"schema_version":"1.0","intent":"CLARIFY","reminder_text":null,
             "scheduled_at":null,"time_zone":null,"target_reference":null,
             "clarification_question":"What time?","reason_code":"AMBIGUOUS_TIME"}
            """);

    assertDoesNotThrow(() -> validator.validate(result));
  }

  @Test
  void rejectsUnknownFieldsAndInvalidEnums() throws Exception {
    JsonNode extra =
        mapper.readTree(
            """
            {"schema_version":"1.0","intent":"CLARIFY","reminder_text":null,
             "scheduled_at":null,"time_zone":null,"target_reference":null,
             "clarification_question":"What time?","reason_code":"AMBIGUOUS_TIME",
             "extra":true}
            """);
    JsonNode invalidIntent =
        mapper.readTree(
            """
            {"schema_version":"1.0","intent":"UNKNOWN","reminder_text":null,
             "scheduled_at":null,"time_zone":null,"target_reference":null,
             "clarification_question":"What time?","reason_code":"AMBIGUOUS_TIME"}
            """);

    assertThrows(OutputValidationException.class, () -> validator.validate(extra));
    assertThrows(OutputValidationException.class, () -> validator.validate(invalidIntent));
  }
}
