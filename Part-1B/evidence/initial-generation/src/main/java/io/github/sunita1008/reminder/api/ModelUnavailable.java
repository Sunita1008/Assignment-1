package io.github.sunita1008.reminder.api;

/** A model-provider failure that is distinct from a reminder intent. */
public final class ModelUnavailable extends Exception {
  private static final long serialVersionUID = 1L;

  /** Stable operational failure categories emitted by the CLI. */
  public enum Category {
    TIMEOUT,
    RATE_LIMITED,
    NETWORK,
    HTTP_ERROR,
    PROVIDER_PROTOCOL
  }

  private final Category category;

  public ModelUnavailable(Category category, String message) {
    super(message);
    this.category = category;
  }

  public Category category() {
    return category;
  }
}
