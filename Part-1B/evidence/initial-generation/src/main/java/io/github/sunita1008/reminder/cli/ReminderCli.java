package io.github.sunita1008.reminder.cli;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.github.sunita1008.reminder.api.ModelUnavailable;
import io.github.sunita1008.reminder.api.ReminderAgent;
import io.github.sunita1008.reminder.guardrail.GuardrailValidator;
import io.github.sunita1008.reminder.input.InputValidator;
import io.github.sunita1008.reminder.model.HttpModelClient;
import io.github.sunita1008.reminder.model.ModelClient;
import io.github.sunita1008.reminder.model.ModelConfiguration;
import io.github.sunita1008.reminder.output.AgentResult;
import io.github.sunita1008.reminder.output.InvariantValidator;
import io.github.sunita1008.reminder.output.JsonSchemaValidator;
import io.github.sunita1008.reminder.output.OutputValidationException;
import io.github.sunita1008.reminder.output.ResultParser;
import io.github.sunita1008.reminder.prompt.PromptBuilder;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

/** One-shot stdin/stdout entry point used by the independent harness. */
public final class ReminderCli {
  private static final ObjectMapper MAPPER = new ObjectMapper();

  private ReminderCli() {}

  /** Runs the command-line application. */
  public static void main(String[] args) {
    ModelClient client =
        messages ->
            new HttpModelClient(ModelConfiguration.fromEnvironment()).complete(messages);
    ReminderAgent agent = createAgent(client);
    int exitCode = run(args, System.in, System.out, agent);
    if (exitCode != 0) {
      System.exit(exitCode);
    }
  }

  private static ReminderAgent createAgent(ModelClient client) {
    return new ReminderAgent(
        new InputValidator(),
        new PromptBuilder(),
        client,
        new ResultParser(),
        new JsonSchemaValidator(),
        new InvariantValidator(),
        new GuardrailValidator());
  }

  /** Executes the run command and returns its documented process status. */
  public static int run(
      String[] args, InputStream input, PrintStream output, ReminderAgent agent) {
    if (args.length != 1 || !"run".equals(args[0])) {
      writeError("invalid_output", "INVALID_COMMAND", output);
      return 4;
    }

    final String envelope;
    try {
      envelope = new String(input.readAllBytes(), StandardCharsets.UTF_8);
    } catch (IOException exception) {
      writeError("invalid_output", "INPUT_READ_ERROR", output);
      return 4;
    }

    try {
      AgentResult result = agent.process(envelope);
      output.println(MAPPER.writeValueAsString(result));
      return 0;
    } catch (ModelUnavailable exception) {
      writeError("unavailable", exception.category().name(), output);
      return 3;
    } catch (OutputValidationException exception) {
      writeError("invalid_output", exception.reason().name(), output);
      return 4;
    } catch (JsonProcessingException exception) {
      writeError("invalid_output", "RESULT_SERIALIZATION", output);
      return 4;
    }
  }

  private static void writeError(String field, String value, PrintStream output) {
    ObjectNode error = MAPPER.createObjectNode();
    error.put(field, value);
    output.println(error);
  }
}
