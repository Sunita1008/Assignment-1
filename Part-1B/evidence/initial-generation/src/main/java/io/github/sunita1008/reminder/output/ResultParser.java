package io.github.sunita1008.reminder.output;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.StreamReadFeature;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import java.util.Set;

/** Strictly parses one model-generated result JSON object. */
public final class ResultParser {
  private static final Set<String> REQUIRED_FIELDS =
      Set.of(
          "schema_version",
          "intent",
          "reminder_text",
          "scheduled_at",
          "time_zone",
          "target_reference",
          "clarification_question",
          "reason_code");
  private final ObjectMapper mapper;

  /** Parses one object and rejects malformed, extra, duplicate, or coercible content. */
  public ResultParser() {
    mapper =
        JsonMapper.builder(
                JsonFactory.builder()
                    .enable(StreamReadFeature.STRICT_DUPLICATE_DETECTION)
                    .build())
            .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
            .disable(MapperFeature.ALLOW_COERCION_OF_SCALARS)
            .build();
  }

  /** Converts generated JSON into a typed result while retaining its source tree. */
  public ParsedResult parse(String generatedText) throws OutputValidationException {
    final JsonNode root;
    try {
      root = mapper.readTree(generatedText);
    } catch (JsonProcessingException exception) {
      throw new OutputValidationException(OutputValidationException.Reason.MALFORMED_JSON);
    }
    if (root == null || !root.isObject() || !hasExactFields(root)) {
      throw new OutputValidationException(OutputValidationException.Reason.SCHEMA_VIOLATION);
    }
    try {
      return new ParsedResult(mapper.treeToValue(root, AgentResult.class), root);
    } catch (JsonProcessingException exception) {
      throw new OutputValidationException(OutputValidationException.Reason.SCHEMA_VIOLATION);
    }
  }

  /** Checks the exact required field set before data binding. */
  private boolean hasExactFields(JsonNode root) {
    if (root.size() != REQUIRED_FIELDS.size()) {
      return false;
    }
    for (String field : REQUIRED_FIELDS) {
      if (!root.has(field)) {
        return false;
      }
    }
    return true;
  }
}
