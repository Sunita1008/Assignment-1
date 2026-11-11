package io.github.sunita1008.reminder.guardrail;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.sunita1008.reminder.output.AgentResult;
import io.github.sunita1008.reminder.output.Intent;
import io.github.sunita1008.reminder.output.OutputValidationException;
import io.github.sunita1008.reminder.output.ReasonCode;
import org.junit.jupiter.api.Test;

class GuardrailValidatorTest {
  private final GuardrailValidator validator = new GuardrailValidator();

  @Test
  void acceptsRefusalWithStableUnsafeReason() {
    AgentResult result =
        new AgentResult(
            "1.0", Intent.REFUSE, null, null, null, null, null, ReasonCode.UNSAFE_ACTION);

    assertDoesNotThrow(() -> validator.validate(result));
  }

  @Test
  void rejectsRefusalWithoutRefusalReason() {
    AgentResult result =
        new AgentResult("1.0", Intent.REFUSE, null, null, null, null, null, ReasonCode.NONE);

    assertThrows(OutputValidationException.class, () -> validator.validate(result));
  }
}
