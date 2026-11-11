package io.github.sunita1008.reminder.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.sunita1008.reminder.api.ModelUnavailable;
import io.github.sunita1008.reminder.api.ModelUnavailable.Category;
import java.io.IOException;
import java.net.ConnectException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.time.Duration;
import java.util.List;

/** Production HTTP transport for OpenAI-compatible chat completions. */
public final class HttpModelClient implements ModelClient {
  private static final ObjectMapper MAPPER = new ObjectMapper();
  private final ModelConfiguration configuration;
  private final HttpClient httpClient;
  private final Duration requestTimeout;

  /** Creates an HTTP client with bounded connection and request timeouts. */
  public HttpModelClient(ModelConfiguration configuration) {
    this(
        configuration,
        HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build(),
        Duration.ofSeconds(60));
  }

  HttpModelClient(
      ModelConfiguration configuration, HttpClient httpClient, Duration requestTimeout) {
    this.configuration = configuration;
    this.httpClient = httpClient;
    this.requestTimeout = requestTimeout;
  }

  @Override
  public String complete(List<ChatMessage> messages) throws ModelUnavailable {
    final String requestBody;
    try {
      requestBody =
          MAPPER.writeValueAsString(
              new CompletionRequest(
                  configuration.modelName(),
                  messages,
                  configuration.supportsTemperature() ? Integer.valueOf(0) : null));
    } catch (JsonProcessingException exception) {
      throw new ModelUnavailable(Category.PROVIDER_PROTOCOL, "Could not encode model request.");
    }

    HttpRequest request =
        HttpRequest.newBuilder(configuration.endpoint().resolve("chat/completions"))
            .timeout(requestTimeout)
            .header("Authorization", "Bearer " + configuration.apiKey())
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(requestBody))
            .build();

    final HttpResponse<String> response;
    try {
      response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    } catch (HttpTimeoutException exception) {
      throw new ModelUnavailable(Category.TIMEOUT, "Model request timed out.");
    } catch (ConnectException exception) {
      throw new ModelUnavailable(Category.NETWORK, "Could not connect to model service.");
    } catch (IOException exception) {
      throw new ModelUnavailable(Category.NETWORK, "Model network request failed.");
    } catch (InterruptedException exception) {
      Thread.currentThread().interrupt();
      throw new ModelUnavailable(Category.NETWORK, "Model request was interrupted.");
    }

    if (response.statusCode() == 429) {
      throw new ModelUnavailable(Category.RATE_LIMITED, "Model service rate limit reached.");
    }
    if (response.statusCode() < 200 || response.statusCode() >= 300) {
      throw new ModelUnavailable(
          Category.HTTP_ERROR,
          "Model service returned HTTP "
              + response.statusCode()
              + providerError(response.body()));
    }
    return extractContent(response.body());
  }

  /**
   * Returns the provider's error message for diagnostics: one line, at most 200 characters, with
   * the API key removed. Returns an empty string when the body has no readable error message.
   */
  String providerError(String body) {
    String message;
    try {
      JsonNode root = MAPPER.readTree(body);
      JsonNode error = root == null ? null : root.path("error");
      if (error == null || error.isMissingNode()) {
        return "";
      }
      message = error.isTextual() ? error.textValue() : error.path("message").asText("");
    } catch (JsonProcessingException exception) {
      return "";
    }
    String apiKey = configuration.apiKey();
    if (!apiKey.isEmpty()) {
      message = message.replace(apiKey, "[REDACTED]");
    }
    message = message.replaceAll("\\s+", " ").strip();
    if (message.length() > 200) {
      message = message.substring(0, 200);
    }
    return message.isEmpty() ? "" : ": " + message;
  }

  private String extractContent(String body) throws ModelUnavailable {
    final JsonNode root;
    try {
      root = MAPPER.readTree(body);
    } catch (JsonProcessingException exception) {
      throw new ModelUnavailable(
          Category.PROVIDER_PROTOCOL, "Model service returned invalid JSON.");
    }
    if (root == null || !root.isObject()) {
      throw new ModelUnavailable(
          Category.PROVIDER_PROTOCOL, "Model service returned an invalid envelope.");
    }
    JsonNode content = root.path("choices").path(0).path("message").path("content");
    if (!content.isTextual() || content.textValue().isBlank()) {
      throw new ModelUnavailable(
          Category.PROVIDER_PROTOCOL, "Model service returned no response text.");
    }
    return content.textValue();
  }

  /** Request body; {@code temperature} is omitted when null (reasoning models). */
  @JsonInclude(JsonInclude.Include.NON_NULL)
  record CompletionRequest(String model, List<ChatMessage> messages, Integer temperature) {}
}
