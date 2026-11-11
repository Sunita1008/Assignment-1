package io.github.sunita1008.reminder.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.sunita1008.reminder.api.ModelUnavailable;
import io.github.sunita1008.reminder.api.ModelUnavailable.Category;
import io.github.sunita1008.reminder.api.ReminderAgent;
import io.github.sunita1008.reminder.guardrail.GuardrailValidator;
import io.github.sunita1008.reminder.input.InputValidator;
import io.github.sunita1008.reminder.model.FakeModelClient;
import io.github.sunita1008.reminder.output.InvariantValidator;
import io.github.sunita1008.reminder.output.JsonSchemaValidator;
import io.github.sunita1008.reminder.output.ResultParser;
import io.github.sunita1008.reminder.prompt.PromptBuilder;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class ReminderCliTest {
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
  private final ObjectMapper mapper = new ObjectMapper();

  @Test
  void emitsOneSuccessObjectAndExitsZero() throws Exception {
    RunResult run = execute(new FakeModelClient(CREATE_RESULT), ENVELOPE);
    JsonNode result = mapper.readTree(run.stdout());

    assertEquals(0, run.exitCode());
    assertEquals("CREATE", result.path("intent").asText());
    assertEquals(8, result.size());
    assertEquals(1, run.stdout().lines().count());
  }

  @Test
  void mapsUnavailableAndInvalidOutputToContractedExitCodes() throws Exception {
    FakeModelClient unavailable = new FakeModelClient();
    unavailable.enqueueUnavailable(new ModelUnavailable(Category.NETWORK, "network failure"));
    RunResult unavailableRun = execute(unavailable, ENVELOPE);
    JsonNode unavailableJson = mapper.readTree(unavailableRun.stdout());
    final RunResult invalidRun = execute(new FakeModelClient("not json"), ENVELOPE);
    final JsonNode invalidJson = mapper.readTree(invalidRun.stdout());

    assertEquals(3, unavailableRun.exitCode());
    assertEquals("NETWORK", unavailableJson.path("unavailable").asText());
    assertEquals(1, unavailableJson.size());
    assertEquals(4, invalidRun.exitCode());
    assertEquals("MALFORMED_JSON", invalidJson.path("invalid_output").asText());
    assertEquals(1, invalidJson.size());
    assertFalse(unavailableRun.stdout().contains("network failure"));
  }

  @Test
  void invalidInputContextDoesNotRequireModelConfigurationOrCall() throws Exception {
    FakeModelClient fake = new FakeModelClient();
    String invalidInput =
        """
        {"user_message":"Call the dentist","reference_now":"bad","time_zone":"Asia/Kolkata"}
        """;
    RunResult run = execute(fake, invalidInput);
    JsonNode result = mapper.readTree(run.stdout());

    assertEquals(0, run.exitCode());
    assertEquals("CLARIFY", result.path("intent").asText());
    assertEquals("INVALID_CONTEXT", result.path("reason_code").asText());
    assertEquals(0, fake.callCount());
    assertTrue(result.path("clarification_question").isTextual());
  }

  private RunResult execute(FakeModelClient fake, String envelope) throws Exception {
    ReminderAgent agent =
        new ReminderAgent(
            new InputValidator(),
            new PromptBuilder(),
            fake,
            new ResultParser(),
            new JsonSchemaValidator(),
            new InvariantValidator(),
            new GuardrailValidator());
    ByteArrayOutputStream bytes = new ByteArrayOutputStream();
    PrintStream output = new PrintStream(bytes, true, StandardCharsets.UTF_8);
    int exitCode =
        ReminderCli.run(
            new String[] {"run"},
            new ByteArrayInputStream(envelope.getBytes(StandardCharsets.UTF_8)),
            output,
            agent);
    output.flush();
    return new RunResult(exitCode, bytes.toString(StandardCharsets.UTF_8));
  }

  private record RunResult(int exitCode, String stdout) {}
}
