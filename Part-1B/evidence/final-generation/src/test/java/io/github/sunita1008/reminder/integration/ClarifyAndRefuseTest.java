package io.github.sunita1008.reminder.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import io.github.sunita1008.reminder.api.ReminderAgent;
import io.github.sunita1008.reminder.guardrail.GuardrailValidator;
import io.github.sunita1008.reminder.input.InputValidator;
import io.github.sunita1008.reminder.model.FakeModelClient;
import io.github.sunita1008.reminder.output.AgentResult;
import io.github.sunita1008.reminder.output.InvariantValidator;
import io.github.sunita1008.reminder.output.JsonSchemaValidator;
import io.github.sunita1008.reminder.output.ResultParser;
import io.github.sunita1008.reminder.prompt.PromptBuilder;
import org.junit.jupiter.api.Test;

class ClarifyAndRefuseTest {
  private static final String INPUT =
      """
      {"user_message":"Reschedule it to Friday at 9 AM",
       "reference_now":"2026-11-11T10:00:00+05:30",
       "time_zone":"Asia/Kolkata"}
      """;

  @Test
  void validatesClarificationAndRefusalShapes() throws Exception {
    String clarify =
        """
        {"schema_version":"1.0","intent":"CLARIFY","reminder_text":null,
         "scheduled_at":null,"time_zone":null,"target_reference":null,
         "clarification_question":"Which reminder do you mean?","reason_code":"MISSING_TARGET"}
        """;
    String refuse =
        """
        {"schema_version":"1.0","intent":"REFUSE","reminder_text":null,
         "scheduled_at":null,"time_zone":null,"target_reference":null,
         "clarification_question":null,"reason_code":"UNSAFE_ACTION"}
        """;
    String unsafeInput =
          """
          {"user_message":"Delete my documents",
           "reference_now":"2026-11-11T10:00:00+05:30","time_zone":"Asia/Kolkata"}
          """;
    ReminderAgent agent = agent(new FakeModelClient(clarify, refuse));

    AgentResult clarification = agent.process(INPUT);
    AgentResult refusal = agent.process(unsafeInput);

    assertEquals("CLARIFY", clarification.intent().name());
    assertEquals("MISSING_TARGET", clarification.reasonCode().name());
    assertEquals("REFUSE", refusal.intent().name());
    assertEquals("UNSAFE_ACTION", refusal.reasonCode().name());
  }

  @Test
  void safeReminderSubjectIsNotRejectedByResultGuardrail() throws Exception {
    String create =
        """
        {"schema_version":"1.0","intent":"CREATE","reminder_text":"Delete the draft",
         "scheduled_at":"2026-11-12T04:00:00Z","time_zone":"Asia/Kolkata",
         "target_reference":null,"clarification_question":null,"reason_code":"NONE"}
        """;

    String safeInput =
        """
        {"user_message":"Remind me to delete the draft tomorrow at 9:30 AM",
         "reference_now":"2026-11-11T10:00:00+05:30","time_zone":"Asia/Kolkata"}
        """;
    AgentResult result = agent(new FakeModelClient(create)).process(safeInput);

    assertEquals("CREATE", result.intent().name());
    assertEquals("Delete the draft", result.reminderText());
  }

  @Test
  void ambiguousTimeUsesClarificationInsteadOfSchedule() throws Exception {
    String ambiguousInput =
        """
        {"user_message":"Remind me next Friday","reference_now":"2026-11-11T10:00:00+05:30",
         "time_zone":"Asia/Kolkata"}
        """;
    String clarify =
        """
        {"schema_version":"1.0","intent":"CLARIFY","reminder_text":null,
         "scheduled_at":null,"time_zone":null,"target_reference":null,
         "clarification_question":"What time on Friday?","reason_code":"AMBIGUOUS_TIME"}
        """;

    AgentResult result = agent(new FakeModelClient(clarify)).process(ambiguousInput);

    assertEquals("CLARIFY", result.intent().name());
    assertEquals("AMBIGUOUS_TIME", result.reasonCode().name());
    assertNull(result.scheduledAt());
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
