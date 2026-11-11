package io.github.sunita1008.reminder.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.Map;
import org.junit.jupiter.api.Test;

class HttpModelClientTest {
  private static final String KEY = "test-only-secret";

  @Test
  void providerErrorIsOneRedactedLine() throws Exception {
    HttpModelClient client = new HttpModelClient(configuration(" " + KEY + "\n"));

    String detail =
        client.providerError(
            "{\"error\":{\"code\":401,\"message\":\"Invalid key\\n " + KEY + "\"}}");

    assertEquals(": Invalid key [REDACTED]", detail);
    assertFalse(detail.contains(KEY));
    assertEquals("", client.providerError("not json"));
    assertEquals("", client.providerError("{\"ok\":true}"));
    assertEquals(
        ": Insufficient credits", client.providerError("{\"error\":\"Insufficient credits\"}"));
  }

  private static ModelConfiguration configuration(String key) throws Exception {
    return ModelConfiguration.from(
        Map.of(
            "MODEL_PROVIDER", "aicredits",
            "MODEL_NAME", "openai/gpt-5-mini",
            "MODEL_API_KEY", key));
  }
}
