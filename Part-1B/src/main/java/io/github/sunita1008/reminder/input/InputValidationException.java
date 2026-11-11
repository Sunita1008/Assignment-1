package io.github.sunita1008.reminder.input;

/** Indicates that the CLI input envelope cannot be used safely. */
public final class InputValidationException extends Exception {
  private static final long serialVersionUID = 1L;

  private final boolean emptyMessage;

  public InputValidationException(String message) {
    this(message, false);
  }

  InputValidationException(String message, boolean emptyMessage) {
    super(message);
    this.emptyMessage = emptyMessage;
  }

  /**
   * Returns true when the time context is valid but the user message is blank, so the missing
   * information is the reminder request itself rather than the input time context.
   */
  public boolean emptyMessage() {
    return emptyMessage;
  }
}
