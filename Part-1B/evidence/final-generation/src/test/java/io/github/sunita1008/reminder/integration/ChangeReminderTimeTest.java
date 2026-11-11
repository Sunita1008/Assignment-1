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

class ChangeReminderTimeTest {
  private static final String ENVELOPE =
      """
      {"user_message":"Move my dentist reminder to Friday at 9 AM",
       "reference_now":"2026-11-11T10:00:00+05:30","time_zone":"Asia/Kolkata"}
      """;
  private static final String RESCHEDULE =
      """
      {"schema_version":"1.0","intent":"RESCHEDULE","reminder_text":null,
       "scheduled_at":"2026-11-13T03:30:00Z","time_zone":"Asia/Kolkata",
       "target_reference":"dentist reminder","clarification_question":null,
       "reason_code":"NONE"}
      """;
  private static final String SNOOZE =
      """
      {"schema_version":"1.0","intent":"SNOOZE","reminder_text":null,
       "scheduled_at":"2026-11-11T06:30:00Z","time_zone":"Asia/Kolkata",
       "target_reference":"dentist reminder","clarification_question":null,
       "reason_code":"NONE"}
      """;

  @Test
  void validatesRescheduleAndSnoozeProposals() throws Exception {
    FakeModelClient fake = new FakeModelClient(RESCHEDULE, SNOOZE);
    ReminderAgent agent = agent(fake);

    AgentResult reschedule = agent.process(ENVELOPE);
    final AgentResult snooze = agent.process(ENVELOPE);

    assertEquals("RESCHEDULE", reschedule.intent().name());
    assertEquals("2026-11-13T03:30:00Z", reschedule.scheduledAt());
    assertEquals("dentist reminder", reschedule.targetReference());
    assertEquals("SNOOZE", snooze.intent().name());
    assertEquals("2026-11-11T06:30:00Z", snooze.scheduledAt());
    assertEquals(2, fake.callCount());
  }

  @Test
  void clarifiesWhenTheTargetIsNotUniquelyIdentified() throws Exception {
    String clarify =
        """
        {"schema_version":"1.0","intent":"CLARIFY","reminder_text":null,
         "scheduled_at":null,"time_zone":null,"target_reference":null,
         "clarification_question":"Which reminder do you mean?","reason_code":"MISSING_TARGET"}
        """;
    AgentResult result = agent(new FakeModelClient(clarify)).process(ENVELOPE);

    assertEquals("CLARIFY", result.intent().name());
    assertEquals("MISSING_TARGET", result.reasonCode().name());
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
