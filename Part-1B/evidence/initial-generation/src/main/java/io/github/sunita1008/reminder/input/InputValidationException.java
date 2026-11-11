package io.github.sunita1008.reminder.input;

/** Indicates that the CLI input envelope cannot be used safely. */
public final class InputValidationException extends Exception {
  private static final long serialVersionUID = 1L;

  public InputValidationException(String message) {
    super(message);
  }
}
