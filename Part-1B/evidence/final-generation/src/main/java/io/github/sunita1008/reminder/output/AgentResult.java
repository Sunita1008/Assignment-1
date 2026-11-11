package io.github.sunita1008.reminder.output;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

/** The schema-versioned result emitted after all validation succeeds. */
@JsonPropertyOrder({
  "schema_version",
  "intent",
  "reminder_text",
  "scheduled_at",
  "time_zone",
  "target_reference",
  "clarification_question",
  "reason_code"
})
public record AgentResult(
    @JsonProperty("schema_version") String schemaVersion,
    Intent intent,
    @JsonProperty("reminder_text") String reminderText,
    @JsonProperty("scheduled_at") String scheduledAt,
    @JsonProperty("time_zone") String timeZone,
    @JsonProperty("target_reference") String targetReference,
    @JsonProperty("clarification_question") String clarificationQuestion,
    @JsonProperty("reason_code") ReasonCode reasonCode) {
  /** Creates the only valid result shape for invalid input time context. */
  public static AgentResult clarify(String question, ReasonCode reasonCode) {
    return new AgentResult(
        "1.0", Intent.CLARIFY, null, null, null, null, question, reasonCode);
  }
}
