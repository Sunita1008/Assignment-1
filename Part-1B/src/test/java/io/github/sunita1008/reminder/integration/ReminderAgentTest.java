package io.github.sunita1008.reminder.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.sunita1008.reminder.api.ModelUnavailable;
import io.github.sunita1008.reminder.api.ModelUnavailable.Category;
import io.github.sunita1008.reminder.api.ReminderAgent;
import io.github.sunita1008.reminder.guardrail.GuardrailValidator;
import io.github.sunita1008.reminder.input.InputValidator;
import io.github.sunita1008.reminder.model.FakeModelClient;
import io.github.sunita1008.reminder.output.AgentResult;
import io.github.sunita1008.reminder.output.Intent;
import io.github.sunita1008.reminder.output.InvariantValidator;
import io.github.sunita1008.reminder.output.JsonSchemaValidator;
import io.github.sunita1008.reminder.output.OutputValidationException;
import io.github.sunita1008.reminder.output.ReasonCode;
import io.github.sunita1008.reminder.output.ResultParser;
import io.github.sunita1008.reminder.prompt.PromptBuilder;
import java.util.Locale;
import org.junit.jupiter.api.Test;

class ReminderAgentTest {
  private static final String ENVELOPE =
      """
      {"user_message":"Remind me to call the dentist tomorrow at 9:30 AM",
       "reference_now":"2026-11-11T10:00:00+05:30","time_zone":"Asia/Kolkata"}
      """;
  private static final String CREATE_RESULT =
      """
      {"schema_version":"1.0","intent":"CREATE","reminder_text":"Call the dentist",
       "scheduled_at":"2026-11-12T04:00:00Z","time_zone":"Asia/Kolkata",
       "target_reference":null,"clarification_question":null,"reason_code":"NONE"}
      """;

  @Test
  void acceptsValidatedCreateUsingFakeModelClient() throws Exception {
    AgentResult result = agent(new FakeModelClient(CREATE_RESULT)).process(ENVELOPE);

    assertEquals(Intent.CREATE, result.intent());
    assertEquals("2026-11-12T04:00:00Z", result.scheduledAt());
  }

  @Test
  void acceptsRelativeCreateUsingFakeModelClient() throws Exception {
    String relativeInput =
        """
        {"user_message":"Remind me to stretch in 2 hours",
         "reference_now":"2026-11-11T10:00:00+05:30","time_zone":"Asia/Kolkata"}
        """;
    String relativeResult =
        """
        {"schema_version":"1.0","intent":"CREATE","reminder_text":"Stretch",
         "scheduled_at":"2026-11-11T06:30:00Z","time_zone":"Asia/Kolkata",
         "target_reference":null,"clarification_question":null,"reason_code":"NONE"}
        """;

    AgentResult result = agent(new FakeModelClient(relativeResult)).process(relativeInput);

    assertEquals("2026-11-11T06:30:00Z", result.scheduledAt());
  }

  @Test
  void invalidContextClarifiesWithoutCallingModel() throws Exception {
    FakeModelClient fake = new FakeModelClient();
    String invalidContext =
        """
        {"user_message":"Call the dentist","reference_now":"bad","time_zone":"Asia/Kolkata"}
        """;

    AgentResult result = agent(fake).process(invalidContext);

    assertEquals(Intent.CLARIFY, result.intent());
    assertEquals(ReasonCode.INVALID_CONTEXT, result.reasonCode());
    assertEquals(0, fake.callCount());
  }

  @Test
  void blankMessageAsksWhatToRemindWithoutCallingModel() throws Exception {
    FakeModelClient fake = new FakeModelClient();
    String blank =
        """
        {"user_message":"    ","reference_now":"2026-11-10T12:00:00Z","time_zone":"Asia/Kolkata"}
        """;

    AgentResult result = agent(fake).process(blank);

    assertEquals(Intent.CLARIFY, result.intent());
    assertEquals(ReasonCode.MISSING_REMINDER_TEXT, result.reasonCode());
    assertTrue(result.clarificationQuestion().toLowerCase(Locale.ROOT).contains("remind"));
    assertEquals(0, fake.callCount());
  }

  @Test
  void invalidContextWinsOverBlankMessage() throws Exception {
    String blankWithBadZone =
        """
        {"user_message":"  ","reference_now":"2026-11-10T12:00:00Z","time_zone":"Not/AZone"}
        """;

    AgentResult result = agent(new FakeModelClient()).process(blankWithBadZone);

    assertEquals(ReasonCode.INVALID_CONTEXT, result.reasonCode());
  }

  @Test
  void clarifyWithFilledSlotsIsRejectedWithRuleDetail() {
    String clarifyWithText =
        """
        {"schema_version":"1.0","intent":"CLARIFY","reminder_text":"Call the dentist",
         "scheduled_at":null,"time_zone":"Asia/Kolkata","target_reference":null,
         "clarification_question":"What time?","reason_code":"AMBIGUOUS_TIME"}
        """;

    OutputValidationException exception =
        assertThrows(
            OutputValidationException.class,
            () -> agent(new FakeModelClient(clarifyWithText)).process(ENVELOPE));

    assertEquals(OutputValidationException.Reason.INVARIANT_VIOLATION, exception.reason());
    assertEquals("INV-4: CLARIFY must have null reminder_text", exception.getMessage());
  }

  @Test
  void rejectsInvalidGeneratedOutputAndPreservesModelOutage() throws Exception {
    assertThrows(
        OutputValidationException.class,
        () -> agent(new FakeModelClient("not json")).process(ENVELOPE));

    FakeModelClient fake = new FakeModelClient();
    fake.enqueueUnavailable(new ModelUnavailable(Category.RATE_LIMITED, "rate limited"));
    assertThrows(ModelUnavailable.class, () -> agent(fake).process(ENVELOPE));
  }

  private static ReminderAgent agent(FakeModelClient fake) {
    return new ReminderAgent(
        new InputValidator(),
        new PromptBuilder(),
        fake,
        new ResultParser(),
        new JsonSchemaValidator(),
        new InvariantValidator(),
        new GuardrailValidator());
  }
}
