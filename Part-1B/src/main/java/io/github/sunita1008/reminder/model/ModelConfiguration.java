package io.github.sunita1008.reminder.model;

import io.github.sunita1008.reminder.api.ModelUnavailable;
import io.github.sunita1008.reminder.api.ModelUnavailable.Category;
import java.net.URI;
import java.util.Locale;
import java.util.Map;

/** Environment-derived provider settings with an explicitly redacted string form. */
public final class ModelConfiguration {
  private static final URI GEMINI_ENDPOINT =
      URI.create("https://generativelanguage.googleapis.com/v1beta/openai/");
  private static final URI OPENAI_ENDPOINT = URI.create("https://api.openai.com/v1/");

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
    String modelName = required(environment, "MODEL_NAME");
    String apiKey = required(environment, "MODEL_API_KEY");
    URI endpoint = switch (provider.toLowerCase(Locale.ROOT)) {
      case "gemini" -> GEMINI_ENDPOINT;
      case "openai" -> OPENAI_ENDPOINT;
      default ->
          throw new ModelUnavailable(
              Category.PROVIDER_PROTOCOL, "Unsupported model provider.");
    };
    return new ModelConfiguration(provider, modelName, apiKey, endpoint);
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

  @Override
  public String toString() {
    return "ModelConfiguration{apiKey='[REDACTED]'}";
  }
}
