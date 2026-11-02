package com.smarttodo.cli;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class TodoCliProcessTest {

  private static final Clock CLOCK =
      Clock.fixed(Instant.parse("2026-11-01T10:00:00Z"), ZoneOffset.UTC);

  @TempDir Path tempDir;

  @Test
  void fr1_fr2_addPrintsExactOutputAndPersistsAcrossRuns() {
    assertResult(run(tempDir, "add", "--title", " Café ", "--tag", " Home "),
        0, "Added task #1: Café\n", "");
    assertResult(run(tempDir, "list"), 0,
        "{\"id\":1,\"title\":\"Café\",\"description\":null,\"priority\":\"MEDIUM\","
            + "\"tags\":[\"home\"],\"status\":\"TODO\","
            + "\"createdAt\":\"2026-11-01T10:00:00Z\"}\n",
        "");
  }

  @Test
  void fr10_separateCliProcessesSharePersistedTasks() throws IOException, InterruptedException {
    Result add = runProcess(tempDir, "add", "--title", "Persisted task");
    Result list = runProcess(tempDir, "list");

    assertResult(add, 0, "Added task #1: Persisted task\n", "");
    assertEquals(0, list.code());
    assertEquals("", list.stderr());
    String timestampPrefix =
        "{\"id\":1,\"title\":\"Persisted task\",\"description\":null,\"priority\":\"MEDIUM\","
            + "\"tags\":[],\"status\":\"TODO\",\"createdAt\":\"";
    assertTrue(list.stdout().startsWith(timestampPrefix));
    assertTrue(
        list.stdout()
            .substring(timestampPrefix.length())
            .matches("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}(?:\\.\\d+)?Z\"}\\n"));
  }

  @Test
  void fr3_fr4_fr5_rendersStableJsonAndEmptyResults() throws IOException {
    run(tempDir, "add", "--title", "Low", "--priority", "LOW", "--tag", "home");
    run(tempDir, "add", "--title", "High", "--priority", "HIGH", "--tag", "work");
    assertResult(run(tempDir, "list", "--tag", "HOME", "--tag", "WORK", "--sort", "priority"),
        0,
        "{\"id\":2,\"title\":\"High\",\"description\":null,\"priority\":\"HIGH\","
            + "\"tags\":[\"work\"],\"status\":\"TODO\","
            + "\"createdAt\":\"2026-11-01T10:00:00Z\"}\n"
            + "{\"id\":1,\"title\":\"Low\",\"description\":null,\"priority\":\"LOW\","
            + "\"tags\":[\"home\"],\"status\":\"TODO\","
            + "\"createdAt\":\"2026-11-01T10:00:00Z\"}\n",
        "");
    assertResult(run(tempDir, "list", "--tag", "missing"), 0, "No tasks found.\n", "");
  }

  @Test
  void fr6_fr7_fr8_updateStatusAndDeletePersist() {
    run(tempDir, "add", "--title", "Task", "--description", "details", "--tag", "old");
    assertResult(run(tempDir, "update", "1", "--title", "Updated", "--clear-description",
        "--tag", "new"), 0, "Updated task #1.\n", "");
    assertResult(run(tempDir, "status", "1", "DONE"), 0,
        "Task #1 status set to DONE.\n", "");
    assertResult(run(tempDir, "delete", "1"), 0, "Deleted task #1.\n", "");
    assertResult(run(tempDir, "list"), 0, "No tasks found.\n", "");
  }

  @Test
  void fr9_fr11_reportsInvalidAndMissingIdsOnStderrOnly() throws IOException {
    assertResult(run(tempDir, "add", "--title", " "), 2, "",
        "Error: invalid arguments.\n");
    assertResult(run(tempDir, "add", "--title", "Task", "--priority", "urgent"), 2, "",
        "Error: priority must be LOW, MEDIUM, or HIGH.\n");
    assertResult(run(tempDir, "delete", "99"), 2, "",
        "Error: task #99 not found.\n");
    assertFalse(Files.exists(tempDir.resolve("tasks.json")));
  }

  @Test
  void fr11_saveFailureKeepsOriginalBytesAndWritesNoSuccess() throws IOException {
    Path file = tempDir.resolve("tasks.json");
    byte[] previous = "{\"version\":1,\"nextId\":1,\"tasks\":[]}"
        .getBytes(StandardCharsets.UTF_8);
    Files.write(file, previous);
    FileReplacer failing = (temp, target) -> {
      throw new IOException("injected failure");
    };

    ByteArrayOutputStream stdout = new ByteArrayOutputStream();
    ByteArrayOutputStream stderr = new ByteArrayOutputStream();
    Result result;
    try (PrintStream out = new PrintStream(stdout, true, StandardCharsets.UTF_8);
        PrintStream err = new PrintStream(stderr, true, StandardCharsets.UTF_8)) {
      int code =
          new CommandRunner(tempDir, CLOCK, out, err, failing)
              .run(new String[] {"add", "--title", "Task"});
      result =
          new Result(code, stdout.toString(StandardCharsets.UTF_8),
              stderr.toString(StandardCharsets.UTF_8));
    }

    assertResult(result, 1, "", "Error: unable to save tasks file.\n");
    assertArrayEquals(previous, Files.readAllBytes(file));
  }

  private Result run(Path directory, String... args) {
    ByteArrayOutputStream stdout = new ByteArrayOutputStream();
    ByteArrayOutputStream stderr = new ByteArrayOutputStream();
    try (PrintStream out = new PrintStream(stdout, true, StandardCharsets.UTF_8);
        PrintStream err = new PrintStream(stderr, true, StandardCharsets.UTF_8)) {
      int code = new CommandRunner(directory, CLOCK, out, err).run(args);
      return new Result(code, stdout.toString(StandardCharsets.UTF_8),
          stderr.toString(StandardCharsets.UTF_8));
    }
  }

  private Result runProcess(Path directory, String... args)
      throws IOException, InterruptedException {
    List<String> command = new ArrayList<>();
    String javaExecutable =
        System.getProperty("os.name").startsWith("Windows") ? "java.exe" : "java";
    command.add(Path.of(System.getProperty("java.home"), "bin", javaExecutable).toString());
    command.add("-cp");
    command.add(
        System.getProperty(
            "surefire.test.class.path", System.getProperty("java.class.path")));
    command.add("com.smarttodo.TodoMain");
    command.addAll(List.of(args));

    Process process = new ProcessBuilder(command).directory(directory.toFile()).start();
    String stdout;
    String stderr;
    try (var out = process.getInputStream();
        var err = process.getErrorStream()) {
      stdout = new String(out.readAllBytes(), StandardCharsets.UTF_8);
      stderr = new String(err.readAllBytes(), StandardCharsets.UTF_8);
    }
    return new Result(process.waitFor(), stdout, stderr);
  }

  private static void assertResult(Result result, int code, String stdout, String stderr) {
    assertEquals(code, result.code());
    assertEquals(stdout, result.stdout());
    assertEquals(stderr, result.stderr());
  }

  private record Result(int code, String stdout, String stderr) {}
}
