package io.github.sunita1008.reminder.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.sunita1008.reminder.api.ModelUnavailable;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ModelConfigurationTest {
  @Test
  void readsGeminiConfigurationAndRedactsKey() throws Exception {
    String secret = "test-only-secret";
    ModelConfiguration configuration =
        ModelConfiguration.from(
            Map.of(
                "MODEL_PROVIDER", "gemini",
                "MODEL_NAME", "test-model",
                "MODEL_API_KEY", secret));

    assertEquals(
        "https://generativelanguage.googleapis.com/v1beta/openai/",
        configuration.endpoint().toString());
    assertEquals("test-model", configuration.modelName());
    assertFalse(configuration.toString().contains(secret));
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
}
