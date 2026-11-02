package com.smarttodo.storage;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.smarttodo.domain.Priority;
import com.smarttodo.domain.Task;
import com.smarttodo.domain.TaskStatus;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class JsonTaskRepositoryTest {

  @TempDir Path tempDir;

  @Test
  void fr1_fr2_fr10_roundTripsUtf8AndOmitsAbsentDescription() throws IOException {
    JsonTaskRepository repository = new JsonTaskRepository(tempDir);
    assertEquals(TaskStore.empty(), repository.load());

    Task task =
        new Task(1, "Café", null, Priority.HIGH, List.of("home"),
            TaskStatus.TODO, Instant.parse("2026-11-01T10:00:00Z"));
    repository.save(new TaskStore(2, List.of(task)));

    String stored = Files.readString(tempDir.resolve("tasks.json"), StandardCharsets.UTF_8);
    assertTrue(stored.contains("Café"));
    assertFalse(stored.contains("\"description\""));
    assertEquals(new TaskStore(2, List.of(task)), repository.load());
  }

  @Test
  void fr10_rejectsMalformedUnsupportedAndInvalidDocuments() throws IOException {
    Path file = tempDir.resolve("tasks.json");
    for (String content : List.of(
        "{not json",
        "{\"version\":2,\"nextId\":1,\"tasks\":[]}",
        "{\"version\":1,\"nextId\":1,\"tasks\":[{\"id\":1,\"title\":\"x\","
            + "\"priority\":\"MEDIUM\",\"tags\":[],\"status\":\"TODO\","
            + "\"createdAt\":\"2026-11-01T10:00:00Z\"}]}")) {
      Files.writeString(file, content, StandardCharsets.UTF_8);
      byte[] before = Files.readAllBytes(file);
      assertThrows(StorageReadException.class, () -> repositoryForTemp().load());
      assertArrayEquals(before, Files.readAllBytes(file));
    }
  }

  @Test
  void fr11_failedReplacementPreservesOriginalAndCleansTemporaryFile() throws IOException {
    JsonTaskRepository repository =
        new JsonTaskRepository(tempDir, (temp, target) -> {
          throw new IOException("injected replacement failure");
        });
    Path file = tempDir.resolve("tasks.json");
    byte[] original = "{\"version\":1,\"nextId\":1,\"tasks\":[]}"
        .getBytes(StandardCharsets.UTF_8);
    Files.write(file, original);

    assertThrows(
        StorageWriteException.class,
        () -> repository.save(new TaskStore(2, List.of(
            new Task(1, "Task", null, Priority.MEDIUM, List.of(),
                TaskStatus.TODO, Instant.parse("2026-11-01T10:00:00Z"))))));

    assertArrayEquals(original, Files.readAllBytes(file));
    try (var files = Files.list(tempDir)) {
      assertEquals(List.of("tasks.json"),
          files.map(path -> path.getFileName().toString()).toList());
    }
  }

  @Test
  void fr1_persistsTasksInAscendingIdOrder() throws IOException {
    JsonTaskRepository repository = new JsonTaskRepository(tempDir);
    Task first =
        new Task(1, "First", null, Priority.LOW, List.of(),
            TaskStatus.TODO, Instant.parse("2026-11-01T10:00:00Z"));
    Task second =
        new Task(2, "Second", null, Priority.LOW, List.of(),
            TaskStatus.TODO, Instant.parse("2026-11-01T10:01:00Z"));
    repository.save(new TaskStore(3, List.of(first, second)));

    assertEquals(List.of(1L, 2L),
        repository.load().tasks().stream().map(Task::id).toList());
  }

  private JsonTaskRepository repositoryForTemp() {
    return new JsonTaskRepository(tempDir);
  }
}
