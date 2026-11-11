package io.github.sunita1008.reminder.guardrail;

import io.github.sunita1008.reminder.output.AgentResult;
import io.github.sunita1008.reminder.output.Intent;
import io.github.sunita1008.reminder.output.OutputValidationException;
import io.github.sunita1008.reminder.output.ReasonCode;

/** Validates result-side safety constraints not represented by the schema. */
public final class GuardrailValidator {
  /** Rejects refusal results that do not carry a refusal reason. */
  public void validate(AgentResult result) throws OutputValidationException {
    if (result.intent() == Intent.REFUSE
        && (result.reasonCode() != ReasonCode.UNSAFE_ACTION
            && result.reasonCode() != ReasonCode.OUT_OF_SCOPE
            && result.reasonCode() != ReasonCode.INJECTION_ATTEMPT)) {
      throw new OutputValidationException(
          OutputValidationException.Reason.GUARDRAIL_VIOLATION);
    }
  }
}
