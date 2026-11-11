package io.github.sunita1008.reminder.prompt;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.github.sunita1008.reminder.input.ReminderInput;
import io.github.sunita1008.reminder.model.ChatMessage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

/** Builds separate static-system and untrusted-user chat messages. */
public final class PromptBuilder {
  private static final ObjectMapper MAPPER = new ObjectMapper();
  private final String systemInstruction;

  /** Loads the versioned prompt resources from the application classpath. */
  public PromptBuilder() {
    systemInstruction =
        loadResource("prompts/system.md")
            + "\n\n"
            + loadResource("prompts/task.md")
            + "\n\n"
            + loadResource("prompts/output-format.md");
  }

  /** Creates model messages without placing raw user content in system instructions. */
  public List<ChatMessage> build(ReminderInput input) {
    ObjectNode userData = MAPPER.createObjectNode();
    userData.put("user_message", input.userMessage());
    userData.put("reference_now", input.referenceNow());
    userData.put("time_zone", input.timeZone());
    try {
      return List.of(
          new ChatMessage("system", systemInstruction),
          new ChatMessage("user", MAPPER.writeValueAsString(userData)));
    } catch (JsonProcessingException exception) {
      throw new IllegalStateException("Could not encode reminder input.", exception);
    }
  }

  private static String loadResource(String name) {
    try (InputStream stream =
        PromptBuilder.class.getClassLoader().getResourceAsStream(name)) {
      if (stream == null) {
        throw new IllegalStateException("Missing prompt resource: " + name);
      }
      return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
    } catch (IOException exception) {
      throw new IllegalStateException("Could not read prompt resource: " + name, exception);
    }
  }
}
