package io.github.sunita1008.reminder.input;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.StreamReadFeature;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.DateTimeException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.Set;

/** Parses the strict three-field CLI envelope and validates its time context. */
public final class InputValidator {
  private static final Set<String> REQUIRED_FIELDS =
      Set.of("user_message", "reference_now", "time_zone");
  private static final ObjectMapper MAPPER =
      new ObjectMapper(
              JsonFactory.builder()
                  .enable(StreamReadFeature.STRICT_DUPLICATE_DETECTION)
                  .build())
          .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS);

  /** Parses one envelope or reports invalid input context. */
  public ReminderInput parse(String envelope) throws InputValidationException {
    final JsonNode root;
    try {
      root = MAPPER.readTree(envelope);
    } catch (JsonProcessingException exception) {
      throw new InputValidationException("Input must be one valid JSON object.");
    }
    if (root == null || !root.isObject() || root.size() != REQUIRED_FIELDS.size()) {
      throw new InputValidationException("Input must contain exactly the three required fields.");
    }
    for (String field : REQUIRED_FIELDS) {
      if (!root.has(field) || !root.get(field).isTextual()) {
        throw new InputValidationException(
            "Input fields must be strings and all fields are required.");
      }
    }

    final String message = root.get("user_message").textValue();
    String referenceNow = root.get("reference_now").textValue();
    String timeZone = root.get("time_zone").textValue();

    final Instant referenceInstant;
    try {
      referenceInstant = OffsetDateTime.parse(referenceNow).toInstant();
    } catch (DateTimeParseException exception) {
      throw new InputValidationException(
          "The reference instant must include an explicit offset.");
    }

    if (!ZoneId.getAvailableZoneIds().contains(timeZone)) {
      throw new InputValidationException(
          "The time zone must be a valid IANA time-zone identifier.");
    }
    final ZoneId zoneId;
    try {
      zoneId = ZoneId.of(timeZone);
    } catch (DateTimeException exception) {
      throw new InputValidationException(
          "The time zone must be a valid IANA time-zone identifier.");
    }
    // Checked after the time context, so invalid context still takes precedence (FR-6).
    if (message.isBlank()) {
      throw new InputValidationException("The user message must not be empty.", true);
    }
    return new ReminderInput(message, referenceNow, timeZone, referenceInstant, zoneId);
  }
}
