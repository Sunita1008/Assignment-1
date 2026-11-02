package com.smarttodo.acceptance;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smarttodo.cli.CommandRunner;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Acceptance tests for the rules added in the 2026-11-03 clarification session.
 *
 * <p>Each display name carries the requirement identifier and the acceptance criterion.
 */
public class ClarifiedRulesAcceptanceTest {

  private static final Clock CLOCK =
      Clock.fixed(Instant.parse("2026-11-01T10:00:00Z"), ZoneOffset.UTC);
  private static final String INVALID_ARGS = "Error: invalid arguments.\n";
  private static final String UPDATED = "Updated task #1.\n";

  private record Result(int code, String out, String err) { }

  private static Result run(Path dir, String... args) {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    ByteArrayOutputStream err = new ByteArrayOutputStream();
    int code;
    try (PrintStream o = new PrintStream(out, true, StandardCharsets.UTF_8);
        PrintStream e = new PrintStream(err, true, StandardCharsets.UTF_8)) {
      code = new CommandRunner(dir, CLOCK, o, e).run(args);
    }
    return new Result(
        code, out.toString(StandardCharsets.UTF_8), err.toString(StandardCharsets.UTF_8));
  }

  private static String created(int id) {
    return String.format("2026-11-01T10:%02d:00Z", id - 1);
  }

  private static String stored(int id, String title, String priority, String tags) {
    return "{\"id\":" + id + ",\"title\":\"" + title + "\",\"priority\":\"" + priority
        + "\",\"tags\":" + tags + ",\"status\":\"TODO\",\"createdAt\":\"" + created(id)
        + "\"}";
  }

  private static String line(int id, String title, String priority, String tags) {
    return "{\"id\":" + id + ",\"title\":\"" + title + "\",\"description\":null,\"priority\":\""
        + priority + "\",\"tags\":" + tags + ",\"status\":\"TODO\",\"createdAt\":\""
        + created(id) + "\"}\n";
  }

  private static void seed(Path dir, String... storedTasks) throws IOException {
    Files.writeString(
        dir.resolve("tasks.json"),
        "{\"version\":1,\"nextId\":" + (storedTasks.length + 1) + ",\"tasks\":["
            + String.join(",", storedTasks) + "]}",
        StandardCharsets.UTF_8);
  }

  @Test
  @DisplayName("FR-1: add trims the title and keeps inner spaces (AC-FR-1c)")
  void fr1_trimsTitleOnAdd(@TempDir Path dir) throws IOException {
    assertEquals(
        new Result(0, "Added task #1: Buy  milk\n", ""),
        run(dir, "add", "--title", "  Buy  milk  "));
    String persisted =
        new ObjectMapper()
            .readTree(Files.readString(dir.resolve("tasks.json")))
            .get("tasks")
            .get(0)
            .get("title")
            .asText();
    assertEquals("Buy  milk", persisted);
  }

  @Test
  @DisplayName("FR-4: OR matching is case-insensitive and excludes other tasks (AC-FR-4)")
  void fr4_orMatchingExcludesTasksWithoutRequestedTags(@TempDir Path dir)
      throws IOException {
    seed(dir,
        stored(1, "Buy milk", "MEDIUM", "[\"groceries\"]"),
        stored(2, "Submit report", "HIGH", "[\"work\"]"),
        stored(3, "Call Sam", "MEDIUM", "[\"home\"]"));
    String expected =
        line(1, "Buy milk", "MEDIUM", "[\"groceries\"]")
            + line(2, "Submit report", "HIGH", "[\"work\"]");
    assertEquals(
        new Result(0, expected, ""), run(dir, "list", "--tag", "GROCERIES", "--tag", "work"));
  }

  @Test
  @DisplayName("FR-6: update trims the title and keeps inner spaces (AC-FR-6d)")
  void fr6_trimsTitleOnUpdate(@TempDir Path dir) throws IOException {
    seed(dir, stored(1, "Buy milk", "MEDIUM", "[]"));
    assertEquals(
        new Result(0, UPDATED, ""), run(dir, "update", "1", "--title", "  Call  Sam  "));
    assertEquals(
        new Result(0, line(1, "Call  Sam", "MEDIUM", "[]"), ""), run(dir, "list"));
  }

  @Test
  @DisplayName("FR-9: a whitespace-only tag is rejected and nothing is saved (AC-FR-9c)")
  void fr9_rejectsWhitespaceOnlyTag(@TempDir Path dir) {
    assertEquals(
        new Result(2, "", INVALID_ARGS),
        run(dir, "add", "--title", "Buy milk", "--tag", "   "));
    assertFalse(Files.exists(dir.resolve("tasks.json")));
  }

  @Test
  @DisplayName("FR-9: a tag with inner spaces and punctuation is valid (AC-FR-9c)")
  void fr9_acceptsTagWithInnerSpacesAndPunctuation(@TempDir Path dir) {
    assertEquals(
        new Result(0, "Added task #1: Buy milk\n", ""),
        run(dir, "add", "--title", "Buy milk", "--tag", " Needs Review! "));
    assertEquals(
        new Result(0, line(1, "Buy milk", "MEDIUM", "[\"needs review!\"]"), ""),
        run(dir, "list"));
  }

  @Test
  @DisplayName("FR-9: conflicting or empty updates are rejected and change nothing (AC-FR-9d)")
  void fr9_rejectsConflictingAndEmptyUpdates(@TempDir Path dir) throws IOException {
    seed(dir, stored(1, "Buy milk", "MEDIUM", "[\"groceries\"]"));
    byte[] before = Files.readAllBytes(dir.resolve("tasks.json"));
    String[][] invalidUpdates = {
      {"update", "1", "--description", "new text", "--clear-description"},
      {"update", "1", "--clear-tags", "--tag", "home"},
      {"update", "1"}
    };
    for (String[] args : invalidUpdates) {
      assertEquals(new Result(2, "", INVALID_ARGS), run(dir, args));
      assertArrayEquals(before, Files.readAllBytes(dir.resolve("tasks.json")));
    }
  }

  @Test
  @DisplayName("FR-9: a whitespace-only title is rejected (AC-FR-9e)")
  void fr9_rejectsWhitespaceOnlyTitle(@TempDir Path dir) {
    assertEquals(new Result(2, "", INVALID_ARGS), run(dir, "add", "--title", "   "));
    assertFalse(Files.exists(dir.resolve("tasks.json")));
  }
}
