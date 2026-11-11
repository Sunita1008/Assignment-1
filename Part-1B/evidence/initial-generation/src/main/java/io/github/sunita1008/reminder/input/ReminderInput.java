package io.github.sunita1008.reminder.input;

import java.time.Instant;
import java.time.ZoneId;

/** Validated input values plus parsed time context. */
public record ReminderInput(
    String userMessage,
    String referenceNow,
    String timeZone,
    Instant referenceInstant,
    ZoneId zoneId) {}
