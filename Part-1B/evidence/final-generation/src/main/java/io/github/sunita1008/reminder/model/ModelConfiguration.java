package io.github.sunita1008.reminder.model;

import io.github.sunita1008.reminder.api.ModelUnavailable;
import io.github.sunita1008.reminder.api.ModelUnavailable.Category;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

/** Environment-derived provider settings with an explicitly redacted string form. */
public final class ModelConfiguration {
  private static final URI AICREDITS_ENDPOINT = URI.create("https://api.aicredits.in/v1/");
  private static final URI GEMINI_ENDPOINT =
      URI.create("https://generativelanguage.googleapis.com/v1beta/openai/");
  private static final URI OPENAI_ENDPOINT = URI.create("https://api.openai.com/v1/");

  /** Reasoning model families that only accept the default sampling temperature. */
  private static final Pattern REASONING_MODEL = Pattern.compile("^(gpt-5|o[134]).*");

  private final String provider;
  private final String modelName;
  private final String apiKey;
  private final URI endpoint;

  ModelConfiguration(String provider, String modelName, String apiKey, URI endpoint) {
    this.provider = provider;
    this.modelName = modelName;
    this.apiKey = apiKey;
    this.endpoint = endpoint;
  }

  /** Reads the supported provider settings from the process environment. */
  public static ModelConfiguration fromEnvironment() throws ModelUnavailable {
    return from(System.getenv());
  }

  static ModelConfiguration from(Map<String, String> environment) throws ModelUnavailable {
    String provider = required(environment, "MODEL_PROVIDER");
    String modelName = required(environment, "MODEL_NAME").strip();
    // Trim stray spaces or a pasted newline, which would otherwise break the auth header.
    String apiKey = required(environment, "MODEL_API_KEY").strip();
    String baseUrl = environment.get("MODEL_BASE_URL");
    URI endpoint =
        baseUrl == null || baseUrl.isBlank() ? providerEndpoint(provider) : parseBaseUrl(baseUrl);
    return new ModelConfiguration(provider.strip(), modelName, apiKey, endpoint);
  }

  private static URI providerEndpoint(String provider) throws ModelUnavailable {
    return switch (provider.strip().toLowerCase(Locale.ROOT)) {
      case "aicredits" -> AICREDITS_ENDPOINT;
      case "gemini" -> GEMINI_ENDPOINT;
      case "openai" -> OPENAI_ENDPOINT;
      default ->
          throw new ModelUnavailable(
              Category.PROVIDER_PROTOCOL, "Unsupported model provider.");
    };
  }

  private static URI parseBaseUrl(String baseUrl) throws ModelUnavailable {
    String trimmed = baseUrl.trim();
    String normalized = trimmed.endsWith("/") ? trimmed : trimmed + "/";
    final URI uri;
    try {
      uri = new URI(normalized);
    } catch (URISyntaxException exception) {
      throw new ModelUnavailable(Category.PROVIDER_PROTOCOL, "MODEL_BASE_URL is not a valid URL.");
    }
    if (!"https".equalsIgnoreCase(uri.getScheme()) || uri.getHost() == null) {
      throw new ModelUnavailable(
          Category.PROVIDER_PROTOCOL, "MODEL_BASE_URL must be an absolute https URL.");
    }
    return uri;
  }

  private static String required(Map<String, String> environment, String name)
      throws ModelUnavailable {
    String value = environment.get(name);
    if (value == null || value.isBlank()) {
      throw new ModelUnavailable(
          Category.PROVIDER_PROTOCOL, "Required model configuration is missing: " + name);
    }
    return value;
  }

  String provider() {
    return provider;
  }

  String modelName() {
    return modelName;
  }

  String apiKey() {
    return apiKey;
  }

  URI endpoint() {
    return endpoint;
  }

  /**
   * Returns whether the model accepts an explicit temperature. Reasoning models such as
   * {@code openai/gpt-5-mini} only accept the provider default, so it must be omitted.
   */
  boolean supportsTemperature() {
    String name = modelName.toLowerCase(Locale.ROOT);
    String family = name.substring(name.lastIndexOf('/') + 1);
    return !REASONING_MODEL.matcher(family).matches();
  }

  @Override
  public String toString() {
    return "ModelConfiguration{apiKey='[REDACTED]'}";
  }
}
