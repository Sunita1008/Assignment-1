package io.github.sunita1008.reminder.output;

import com.fasterxml.jackson.databind.JsonNode;

/** Holds the typed result together with its original JSON tree for schema validation. */
public record ParsedResult(AgentResult result, JsonNode json) {}
