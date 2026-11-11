package io.github.sunita1008.reminder.api;

import io.github.sunita1008.reminder.guardrail.GuardrailValidator;
import io.github.sunita1008.reminder.input.InputValidationException;
import io.github.sunita1008.reminder.input.InputValidator;
import io.github.sunita1008.reminder.input.ReminderInput;
import io.github.sunita1008.reminder.model.ModelClient;
import io.github.sunita1008.reminder.output.AgentResult;
import io.github.sunita1008.reminder.output.InvariantValidator;
import io.github.sunita1008.reminder.output.JsonSchemaValidator;
import io.github.sunita1008.reminder.output.OutputValidationException;
import io.github.sunita1008.reminder.output.ParsedResult;
import io.github.sunita1008.reminder.output.ReasonCode;
import io.github.sunita1008.reminder.output.ResultParser;
import io.github.sunita1008.reminder.prompt.PromptBuilder;

/** Coordinates input validation, model access, and result validation. */
public final class ReminderAgent {
  private static final String INVALID_CONTEXT_QUESTION =
      "Please provide a valid reference time and IANA time zone.";

  private final InputValidator inputValidator;
  private final PromptBuilder promptBuilder;
  private final ModelClient modelClient;
  private final ResultParser resultParser;
  private final JsonSchemaValidator schemaValidator;
  private final InvariantValidator invariantValidator;
  private final GuardrailValidator guardrailValidator;

  /** Creates an agent with its transport and validation boundaries. */
  public ReminderAgent(
      InputValidator inputValidator,
      PromptBuilder promptBuilder,
      ModelClient modelClient,
      ResultParser resultParser,
      JsonSchemaValidator schemaValidator,
      InvariantValidator invariantValidator,
      GuardrailValidator guardrailValidator) {
    this.inputValidator = inputValidator;
    this.promptBuilder = promptBuilder;
    this.modelClient = modelClient;
    this.resultParser = resultParser;
    this.schemaValidator = schemaValidator;
    this.invariantValidator = invariantValidator;
    this.guardrailValidator = guardrailValidator;
  }

  /** Returns a validated result, or throws a typed service/output failure. */
  public AgentResult process(String inputEnvelope)
      throws ModelUnavailable, OutputValidationException {
    final ReminderInput input;
    try {
      input = inputValidator.parse(inputEnvelope);
    } catch (InputValidationException exception) {
      return AgentResult.clarify(INVALID_CONTEXT_QUESTION, ReasonCode.INVALID_CONTEXT);
    }
    String generatedText = modelClient.complete(promptBuilder.build(input));
    ParsedResult parsed = resultParser.parse(generatedText);
    schemaValidator.validate(parsed.json());
    invariantValidator.validate(parsed.result(), input);
    guardrailValidator.validate(parsed.result());
    return parsed.result();
  }

}
