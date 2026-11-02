package com.smarttodo.acceptance;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.smarttodo.cli.CommandRunner;
import com.smarttodo.storage.FileReplacer;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Specification-derived acceptance tests for the Smart Todo CLI.
 *
 * <p>Method names start with the requirement identifier without the hyphen (fr1 for FR-1); each
 * display name carries the hyphenated identifier.
 */
public class TodoAcceptanceTest {

  private static final Clock CLOCK =
      Clock.fixed(Instant.parse("2026-11-01T10:00:00Z"), ZoneOffset.UTC);
  private static final String NONE = "[]";
  private static final String GROCERIES = "[\"groceries\"]";
  private static final String WORK = "[\"work\"]";
  private static final String HOME = "[\"home\"]";
  private static final String INVALID_ARGS = "Error: invalid arguments.\n";
  private static final String READ_ERROR = "Error: unable to read tasks file.\n";
  private static final String SAVE_ERROR = "Error: unable to save tasks file.\n";
  private static final String NO_TASKS = "No tasks found.\n";

  /** A task fixture; createdAt is derived from the id (id 1 is 10:00, id 2 is 10:01, ...). */
  private record Item(
      int id, String title, String desc, String priority, String tags, String status) {

    String created() {
      return String.format("2026-11-01T10:%02d:00Z", id - 1);
    }

    String stored() {
      String description = desc == null ? "" : ",\"description\":\"" + desc + "\"";
      return "{\"id\":" + id + ",\"title\":\"" + title + "\"" + description
          + ",\"priority\":\"" + priority + "\",\"tags\":" + tags
          + ",\"status\":\"" + status + "\",\"createdAt\":\"" + created() + "\"}";
    }

    String line() {
      String description = desc == null ? "null" : "\"" + desc + "\"";
      return "{\"id\":" + id + ",\"title\":\"" + title + "\",\"description\":" + description
          + ",\"priority\":\"" + priority + "\",\"tags\":" + tags
          + ",\"status\":\"" + status + "\",\"createdAt\":\"" + created() + "\"}\n";
    }
  }

  private record Result(int code, String out, String err) { }

  @FunctionalInterface
  private interface RunnerFactory {
    CommandRunner create(PrintStream out, PrintStream err);
  }

  private static Result invoke(RunnerFactory factory, String... args) {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    ByteArrayOutputStream err = new ByteArrayOutputStream();
    int code;
    try (PrintStream o = new PrintStream(out, true, StandardCharsets.UTF_8);
        PrintStream e = new PrintStream(err, true, StandardCharsets.UTF_8)) {
      code = factory.create(o, e).run(args);
    }
    return new Result(
        code, out.toString(StandardCharsets.UTF_8), err.toString(StandardCharsets.UTF_8));
  }

  private static Result run(Path dir, String... args) {
    return invoke((o, e) -> new CommandRunner(dir, CLOCK, o, e), args);
  }

  private static Result runFailingSave(Path dir, String... args) {
    FileReplacer failing = (temp, target) -> {
      throw new IOException("simulated failure");
    };
    return invoke((o, e) -> new CommandRunner(dir, CLOCK, o, e, failing), args);
  }

  private static void seed(Path dir, Item... items) throws IOException {
    String body = Stream.of(items).map(Item::stored).collect(Collectors.joining(","));
    Files.writeString(
        dir.resolve("tasks.json"),
        "{\"version\":1,\"nextId\":" + (items.length + 1) + ",\"tasks\":[" + body + "]}",
        StandardCharsets.UTF_8);
  }

  private static byte[] storeBytes(Path dir) throws IOException {
    return Files.readAllBytes(dir.resolve("tasks.json"));
  }

  private static void assertResult(Result result, int code, String out, String err) {
    assertEquals(out, result.out());
    assertEquals(err, result.err());
    assertEquals(code, result.code());
  }

  private static String mainSources() throws IOException {
    try (Stream<Path> files = Files.walk(Path.of("src", "main", "java"))) {
      StringBuilder all = new StringBuilder();
      for (Path file : files.filter(f -> f.toString().endsWith(".java")).toList()) {
        all.append(Files.readString(file));
      }
      return all.toString();
    }
  }

  // ---------------------------------------------------------------- FR-1, FR-2

  @Test
  @DisplayName("FR-1: add stores a task with all supplied fields (AC-FR-1a)")
  void fr1_addsTaskWithAllFields(@TempDir Path dir) {
    Result add = run(dir, "add", "--title", "Buy milk", "--priority", "MEDIUM",
        "--tag", "groceries");
    assertResult(add, 0, "Added task #1: Buy milk\n", "");
    Item expected = new Item(1, "Buy milk", null, "MEDIUM", GROCERIES, "TODO");
    assertResult(run(dir, "list"), 0, expected.line(), "");
  }

  @Test
  @DisplayName("FR-1: add accepts a title only (AC-FR-1b)")
  void fr1_addsTaskWithTitleOnly(@TempDir Path dir) {
    assertResult(run(dir, "add", "--title", "Buy milk"), 0, "Added task #1: Buy milk\n", "");
  }

  @Test
  @DisplayName("FR-2: defaults are applied and no description is stored (AC-FR-2)")
  void fr2_appliesDefaults(@TempDir Path dir) throws IOException {
    assertResult(run(dir, "add", "--title", "Call Sam"), 0, "Added task #1: Call Sam\n", "");
    Item expected = new Item(1, "Call Sam", null, "MEDIUM", NONE, "TODO");
    assertResult(run(dir, "list"), 0, expected.line(), "");
    String stored = Files.readString(dir.resolve("tasks.json"), StandardCharsets.UTF_8);
    assertFalse(stored.contains("\"description\""));
  }

  // ---------------------------------------------------------------- FR-3, FR-4, FR-5

  @Test
  @DisplayName("FR-3: list prints one JSON line per task (AC-FR-3)")
  void fr3_listsTasksAsJsonLines(@TempDir Path dir) throws IOException {
    Item milk = new Item(1, "Buy milk", null, "MEDIUM", GROCERIES, "TODO");
    seed(dir, milk);
    assertResult(run(dir, "list"), 0, milk.line(), "");
  }

  @Test
  @DisplayName("FR-4: tag filter is case-insensitive and reports no match (AC-FR-4)")
  void fr4_filtersByTag(@TempDir Path dir) throws IOException {
    Item milk = new Item(1, "Buy milk", null, "MEDIUM", GROCERIES, "TODO");
    Item report = new Item(2, "Submit report", null, "HIGH", WORK, "TODO");
    seed(dir, milk, report);
    assertResult(run(dir, "list", "--tag", "GROCERIES"), 0, milk.line(), "");
    assertResult(run(dir, "list", "--tag", "nomatch"), 0, NO_TASKS, "");
  }

  @Test
  @DisplayName("FR-4: several tags match a task that has at least one of them")
  void fr4_multipleTagsMatchAnyRequestedTag(@TempDir Path dir) throws IOException {
    Item milk = new Item(1, "Buy milk", null, "MEDIUM", GROCERIES, "TODO");
    Item report = new Item(2, "Submit report", null, "HIGH", WORK, "TODO");
    Item call = new Item(3, "Call Sam", null, "MEDIUM", HOME, "TODO");
    seed(dir, milk, report, call);
    assertResult(run(dir, "list", "--tag", "groceries", "--tag", "work"), 0,
        milk.line() + report.line(), "");
  }

  @Test
  @DisplayName("FR-5: sort by priority puts HIGH first, then MEDIUM, then LOW (AC-FR-5)")
  void fr5_sortsByPriority(@TempDir Path dir) throws IOException {
    Item milk = new Item(1, "Buy milk", null, "LOW", NONE, "TODO");
    Item report = new Item(2, "Submit report", null, "HIGH", WORK, "TODO");
    Item call = new Item(3, "Call Sam", null, "MEDIUM", HOME, "TODO");
    seed(dir, milk, report, call);
    assertResult(run(dir, "list", "--sort", "priority"), 0,
        report.line() + call.line() + milk.line(), "");
    assertResult(run(dir, "list"), 0, milk.line() + report.line() + call.line(), "");
  }

  @Test
  @DisplayName("FR-5: equal priorities are ordered by ascending id")
  void fr5_equalPriorityOrdersByAscendingId(@TempDir Path dir) throws IOException {
    Item low = new Item(1, "Buy milk", null, "LOW", NONE, "TODO");
    Item high1 = new Item(2, "Submit report", null, "HIGH", NONE, "TODO");
    Item high2 = new Item(3, "Pay rent", null, "HIGH", NONE, "TODO");
    seed(dir, low, high1, high2);
    assertResult(run(dir, "list", "--sort", "priority"), 0,
        high1.line() + high2.line() + low.line(), "");
  }

  // ---------------------------------------------------------------- FR-6, FR-7, FR-8

  @Test
  @DisplayName("FR-6: update changes title, priority and tags (AC-FR-6a)")
  void fr6_updatesFields(@TempDir Path dir) throws IOException {
    seed(dir, new Item(1, "Buy milk", null, "MEDIUM", GROCERIES, "TODO"));
    Result update = run(dir, "update", "1", "--title", "Buy oat milk", "--priority", "HIGH",
        "--tag", "groceries", "--tag", "home");
    assertResult(update, 0, "Updated task #1.\n", "");
    Item expected =
        new Item(1, "Buy oat milk", null, "HIGH", "[\"groceries\",\"home\"]", "TODO");
    assertResult(run(dir, "list"), 0, expected.line(), "");
  }

  @Test
  @DisplayName("FR-6: --clear-description removes the description (AC-FR-6b)")
  void fr6_clearsDescription(@TempDir Path dir) throws IOException {
    seed(dir, new Item(1, "Buy milk", "whole milk", "MEDIUM", NONE, "TODO"));
    assertResult(run(dir, "update", "1", "--clear-description"), 0, "Updated task #1.\n", "");
    Item expected = new Item(1, "Buy milk", null, "MEDIUM", NONE, "TODO");
    assertResult(run(dir, "list"), 0, expected.line(), "");
  }

  @Test
  @DisplayName("FR-6: --clear-tags removes all tags (AC-FR-6c)")
  void fr6_clearsTags(@TempDir Path dir) throws IOException {
    seed(dir, new Item(1, "Buy milk", null, "MEDIUM", GROCERIES, "TODO"));
    assertResult(run(dir, "update", "1", "--clear-tags"), 0, "Updated task #1.\n", "");
    Item expected = new Item(1, "Buy milk", null, "MEDIUM", NONE, "TODO");
    assertResult(run(dir, "list"), 0, expected.line(), "");
  }

  @Test
  @DisplayName("FR-7: status can be set to every supported status (AC-FR-7)")
  void fr7_changesStatus(@TempDir Path dir) throws IOException {
    seed(dir, new Item(1, "Buy milk", null, "MEDIUM", NONE, "TODO"));
    for (String status : List.of("IN_PROGRESS", "TODO", "DONE")) {
      assertResult(run(dir, "status", "1", status), 0,
          "Task #1 status set to " + status + ".\n", "");
      Item expected = new Item(1, "Buy milk", null, "MEDIUM", NONE, status);
      assertResult(run(dir, "list"), 0, expected.line(), "");
    }
  }

  @Test
  @DisplayName("FR-8: delete removes the task (AC-FR-8)")
  void fr8_deletesTask(@TempDir Path dir) throws IOException {
    seed(dir, new Item(1, "Buy milk", null, "MEDIUM", NONE, "TODO"));
    assertResult(run(dir, "delete", "1"), 0, "Deleted task #1.\n", "");
    assertResult(run(dir, "list"), 0, NO_TASKS, "");
  }

  // ---------------------------------------------------------------- FR-9

  @Test
  @DisplayName("FR-9: an empty title is rejected with exit code 2 (AC-FR-9a)")
  void fr9_rejectsEmptyTitle(@TempDir Path dir) {
    assertResult(run(dir, "add", "--title", ""), 2, "", INVALID_ARGS);
  }

  @Test
  @DisplayName("FR-9: an invalid priority is rejected with exit code 2 (AC-FR-9b)")
  void fr9_rejectsInvalidPriority(@TempDir Path dir) {
    assertResult(run(dir, "add", "--title", "Buy milk", "--priority", "URGENT"), 2, "",
        "Error: priority must be LOW, MEDIUM, or HIGH.\n");
  }

  // ---------------------------------------------------------------- FR-10, FR-11

  @Test
  @DisplayName("FR-10: tasks persist across separate runs (AC-FR-10a)")
  void fr10_persistsAcrossRuns(@TempDir Path dir) {
    Result first = run(dir, "add", "--title", "Buy milk", "--priority", "MEDIUM",
        "--tag", "groceries");
    assertResult(first, 0, "Added task #1: Buy milk\n", "");
    Item expected = new Item(1, "Buy milk", null, "MEDIUM", GROCERIES, "TODO");
    assertResult(run(dir, "list"), 0, expected.line(), "");
  }

  @Test
  @DisplayName("FR-10: malformed JSON is a read error and the file is untouched (AC-FR-10b)")
  void fr10_malformedJsonIsReadError(@TempDir Path dir) throws IOException {
    Files.writeString(dir.resolve("tasks.json"), "{not json", StandardCharsets.UTF_8);
    byte[] before = storeBytes(dir);
    assertResult(run(dir, "list"), 1, "", READ_ERROR);
    assertArrayEquals(before, storeBytes(dir));
  }

  @Test
  @DisplayName("FR-11: a missing task id is reported and nothing changes (AC-FR-11a)")
  void fr11_missingTaskIdIsReported(@TempDir Path dir) throws IOException {
    seed(dir, new Item(1, "Buy milk", null, "MEDIUM", NONE, "TODO"));
    byte[] before = storeBytes(dir);
    assertResult(run(dir, "delete", "99"), 2, "", "Error: task #99 not found.\n");
    assertArrayEquals(before, storeBytes(dir));
  }

  @Test
  @DisplayName("FR-11: a failed save is reported with exit code 1 (AC-FR-11b)")
  void fr11_failedSaveIsReported(@TempDir Path dir) {
    assertResult(runFailingSave(dir, "add", "--title", "Buy milk"), 1, "", SAVE_ERROR);
  }

  // ---------------------------------------------------------------- FR-12, NFR-1..4

  @Test
  @DisplayName("FR-12: excluded features are absent (AC-FR-12)")
  void fr12_excludedFeaturesAreAbsent(@TempDir Path dir) throws IOException {
    for (String command : List.of("login", "register", "recurring", "repeat")) {
      assertResult(run(dir, command), 2, "", INVALID_ARGS);
    }
    String sources = mainSources();
    for (String forbidden : List.of("javax.swing", "java.awt", "javafx")) {
      assertFalse(sources.contains(forbidden), forbidden);
    }
  }

  @Test
  @DisplayName("NFR-1: production code performs no network operations (AC-NFR-1)")
  void nfr1_noNetworkUsage() throws IOException {
    String sources = mainSources();
    for (String forbidden : List.of("java.net.", "javax.net", "HttpClient")) {
      assertFalse(sources.contains(forbidden), forbidden);
    }
    String pom = Files.readString(Path.of("pom.xml"));
    for (String forbidden : List.of("httpclient", "okhttp", "netty")) {
      assertFalse(pom.contains(forbidden), forbidden);
    }
  }

  @Test
  @DisplayName("NFR-2: the build targets Java 17 or higher (AC-NFR-2)")
  void nfr2_targetsJava17() throws IOException {
    assertTrue(Runtime.version().feature() >= 17);
    assertTrue(Files.readString(Path.of("pom.xml")).contains("17"));
  }

  @Test
  @DisplayName("NFR-3: output is deterministic, with no colour codes or stack traces (AC-NFR-3)")
  void nfr3_outputIsDeterministic(@TempDir Path dir) throws IOException {
    List<String[]> commands = List.of(
        new String[] {"list"},
        new String[] {"list", "--tag", "groceries"},
        new String[] {"list", "--sort", "priority"},
        new String[] {"add", "--title", "Call Sam"},
        new String[] {"update", "1", "--title", "Buy oat milk"},
        new String[] {"status", "1", "DONE"},
        new String[] {"delete", "1"},
        new String[] {"add", "--title", ""},
        new String[] {"add", "--title", "Call Sam", "--priority", "URGENT"},
        new String[] {"delete", "99"},
        new String[] {"status", "1", "BAD"});
    Item milk = new Item(1, "Buy milk", null, "MEDIUM", GROCERIES, "TODO");
    for (String[] command : commands) {
      Path first = Files.createTempDirectory(dir, "first");
      Path second = Files.createTempDirectory(dir, "second");
      seed(first, milk);
      seed(second, milk);
      Result a = run(first, command);
      Result b = run(second, command);
      assertEquals(a, b);
      String all = a.out() + a.err();
      assertFalse(all.contains("\u001B["));
      assertFalse(all.contains("Exception"));
      assertFalse(all.contains("\tat "));
    }
  }

  @Test
  @DisplayName("NFR-4: a failed replacement leaves the old file intact (AC-NFR-4)")
  void nfr4_failedSaveKeepsPriorFile(@TempDir Path dir) throws IOException {
    seed(dir, new Item(1, "Buy milk", null, "MEDIUM", GROCERIES, "TODO"));
    byte[] before = storeBytes(dir);
    assertResult(runFailingSave(dir, "add", "--title", "Call Sam"), 1, "", SAVE_ERROR);
    assertArrayEquals(before, storeBytes(dir));
    try (Stream<Path> files = Files.list(dir)) {
      List<String> names = files.map(p -> p.getFileName().toString()).sorted().toList();
      assertEquals(List.of("tasks.json"), names);
    }
  }
}