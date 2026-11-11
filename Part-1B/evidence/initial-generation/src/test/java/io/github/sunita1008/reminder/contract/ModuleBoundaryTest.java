package io.github.sunita1008.reminder.contract;

import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.sunita1008.reminder.api.ReminderAgent;
import io.github.sunita1008.reminder.guardrail.GuardrailValidator;
import io.github.sunita1008.reminder.input.InputValidator;
import io.github.sunita1008.reminder.model.FakeModelClient;
import io.github.sunita1008.reminder.output.InvariantValidator;
import io.github.sunita1008.reminder.output.JsonSchemaValidator;
import io.github.sunita1008.reminder.output.OutputValidationException;
import io.github.sunita1008.reminder.output.ResultParser;
import io.github.sunita1008.reminder.prompt.PromptBuilder;
import org.junit.jupiter.api.Test;

class ModuleBoundaryTest {
  @Test
  void invalidModelTextCannotBypassResultValidation() {
    ReminderAgent agent =
        new ReminderAgent(
            new InputValidator(),
            new PromptBuilder(),
            new FakeModelClient("{\"intent\":\"CREATE\"}"),
            new ResultParser(),
            new JsonSchemaValidator(),
            new InvariantValidator(),
            new GuardrailValidator());
    String input =
        """
        {"user_message":"Call","reference_now":"2026-11-11T10:00:00Z","time_zone":"UTC"}
        """;

    assertThrows(OutputValidationException.class, () -> agent.process(input));
  }
}
