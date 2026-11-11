package io.github.sunita1008.reminder.model;

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
        Duration.ofSeconds(30));
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
              new CompletionRequest(configuration.modelName(), messages, 0));
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
          Category.HTTP_ERROR, "Model service returned a non-success status.");
    }
    return extractContent(response.body());
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

  private record CompletionRequest(String model, List<ChatMessage> messages, int temperature) {}
}
