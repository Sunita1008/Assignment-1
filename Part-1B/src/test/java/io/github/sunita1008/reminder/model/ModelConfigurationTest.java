package io.github.sunita1008.reminder.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.sunita1008.reminder.api.ModelUnavailable;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ModelConfigurationTest {
  private static final String KEY = "test-only-secret";

  @Test
  void readsGeminiConfigurationAndRedactsKey() throws Exception {
    ModelConfiguration configuration =
        ModelConfiguration.from(
            Map.of(
                "MODEL_PROVIDER", "gemini",
                "MODEL_NAME", "test-model",
                "MODEL_API_KEY", KEY));

    assertEquals(
        "https://generativelanguage.googleapis.com/v1beta/openai/",
        configuration.endpoint().toString());
    assertEquals("test-model", configuration.modelName());
    assertFalse(configuration.toString().contains(KEY));
  }

  @Test
  void readsAiCreditsConfiguration() throws Exception {
    ModelConfiguration configuration =
        ModelConfiguration.from(
            Map.of(
                "MODEL_PROVIDER", "aicredits",
                "MODEL_NAME", "openai/gpt-5-mini",
                "MODEL_API_KEY", KEY));

    assertEquals("https://api.aicredits.in/v1/", configuration.endpoint().toString());
    assertEquals(
        "https://api.aicredits.in/v1/chat/completions",
        configuration.endpoint().resolve("chat/completions").toString());
    assertFalse(configuration.toString().contains(KEY));
  }

  @Test
  void baseUrlOverridesProviderDefaultAndGetsTrailingSlash() throws Exception {
    ModelConfiguration configuration =
        ModelConfiguration.from(
            Map.of(
                "MODEL_PROVIDER", "aicredits",
                "MODEL_NAME", "openai/gpt-5-mini",
                "MODEL_API_KEY", KEY,
                "MODEL_BASE_URL", "https://example.test/v2"));

    assertEquals(
        "https://example.test/v2/chat/completions",
        configuration.endpoint().resolve("chat/completions").toString());
  }

  @Test
  void rejectsInsecureOrRelativeBaseUrl() {
    for (String url : List.of("http://api.aicredits.in/v1", "api.aicredits.in/v1", "https://")) {
      assertThrows(
          ModelUnavailable.class,
          () ->
              ModelConfiguration.from(
                  Map.of(
                      "MODEL_PROVIDER", "aicredits",
                      "MODEL_NAME", "openai/gpt-5-mini",
                      "MODEL_API_KEY", KEY,
                      "MODEL_BASE_URL", url)),
          url);
    }
  }

  @Test
  void omitsTemperatureOnlyForReasoningModels() throws Exception {
    assertFalse(withModel("openai/gpt-5-mini").supportsTemperature());
    assertFalse(withModel("gpt-5").supportsTemperature());
    assertFalse(withModel("openai/o4-mini").supportsTemperature());
    assertTrue(withModel("openai/gpt-4o-mini").supportsTemperature());
    assertTrue(withModel("gemini-3.5-flash").supportsTemperature());

    ObjectMapper mapper = new ObjectMapper();
    List<ChatMessage> messages = List.of(new ChatMessage("user", "hi"));
    String reasoning =
        mapper.writeValueAsString(
            new HttpModelClient.CompletionRequest("openai/gpt-5-mini", messages, null));
    String standard =
        mapper.writeValueAsString(
            new HttpModelClient.CompletionRequest("openai/gpt-4o-mini", messages, 0));
    assertFalse(reasoning.contains("temperature"));
    assertTrue(standard.contains("\"temperature\":0"));
  }

  @Test
  void rejectsMissingOrUnsupportedConfiguration() {
    assertThrows(
        ModelUnavailable.class,
        () ->
            ModelConfiguration.from(
                Map.of("MODEL_PROVIDER", "gemini", "MODEL_NAME", "test-model")));
    assertThrows(
        ModelUnavailable.class,
        () ->
            ModelConfiguration.from(
                Map.of(
                    "MODEL_PROVIDER", "unknown",
                    "MODEL_NAME", "test-model",
                    "MODEL_API_KEY", "not-a-real-key")));
  }

  private static ModelConfiguration withModel(String model) throws ModelUnavailable {
    return ModelConfiguration.from(
        Map.of("MODEL_PROVIDER", "aicredits", "MODEL_NAME", model, "MODEL_API_KEY", KEY));
  }
}
