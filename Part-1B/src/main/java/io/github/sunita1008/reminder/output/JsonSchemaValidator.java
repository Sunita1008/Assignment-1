package io.github.sunita1008.reminder.output;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.ValidationMessage;
import java.io.IOException;
import java.io.InputStream;
import java.util.Set;

/** Validates model results against the packaged authoritative Draft 2020-12 schema. */
public final class JsonSchemaValidator {
  private final JsonSchema schema;

  /** Loads the schema bundled from the protected harness schema at build time. */
  public JsonSchemaValidator() {
    try (InputStream stream =
        JsonSchemaValidator.class.getResourceAsStream("/schemas/result.schema.json")) {
      if (stream == null) {
        throw new IllegalStateException("The packaged result schema is missing.");
      }
      ObjectMapper mapper = new ObjectMapper();
      JsonSchemaFactory factory =
          JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012);
      ObjectNode schemaDocument = (ObjectNode) mapper.readTree(stream);
      // NetworkNT requires an absolute $id; the protected schema uses an opaque identifier.
      schemaDocument.remove("$id");
      schema = factory.getSchema(schemaDocument);
    } catch (IOException exception) {
      throw new IllegalStateException("The packaged result schema could not be loaded.", exception);
    }
  }

  /** Throws when the JSON object violates the result schema. */
  public void validate(JsonNode result) throws OutputValidationException {
    Set<ValidationMessage> errors = schema.validate(result);
    if (!errors.isEmpty()) {
      throw new OutputValidationException(OutputValidationException.Reason.SCHEMA_VIOLATION);
    }
  }
}
