package io.github.sunita1008.reminder.output;

/** A stable, non-sensitive reason that generated output was rejected. */
public final class OutputValidationException extends Exception {
  private static final long serialVersionUID = 1L;

  /** CLI-safe validation failure categories. */
  public enum Reason {
    MALFORMED_JSON,
    SCHEMA_VIOLATION,
    INVARIANT_VIOLATION,
    GUARDRAIL_VIOLATION
  }

  private final Reason reason;

  public OutputValidationException(Reason reason) {
    this(reason, reason.name());
  }

  /** Creates a failure with a fixed, non-sensitive detail naming the broken rule. */
  public OutputValidationException(Reason reason, String detail) {
    super(detail);
    this.reason = reason;
  }

  public Reason reason() {
    return reason;
  }
}
