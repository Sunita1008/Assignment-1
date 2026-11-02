package com.smarttodo.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.smarttodo.domain.Priority;
import com.smarttodo.domain.Task;
import com.smarttodo.domain.TaskStatus;
import com.smarttodo.storage.StorageReadException;
import com.smarttodo.storage.StorageWriteException;
import com.smarttodo.storage.TaskRepository;
import com.smarttodo.storage.TaskStore;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class TaskServiceTest {

  private static final Instant CREATED_AT = Instant.parse("2026-11-01T10:00:00Z");
  private static final Clock CLOCK = Clock.fixed(CREATED_AT, ZoneOffset.UTC);

  @Test
  void fr1_fr2_createsTrimmedTasksWithDefaultsAndNormalizedTags() {
    InMemoryRepository repository = new InMemoryRepository();
    TaskService service = new TaskService(repository, CLOCK);

    Task first = service.add("  Buy  oat milk  ", null, Priority.MEDIUM,
        List.of(" Groceries ", "HOME", "groceries", " Work project, urgent! "));
    final Task second = service.add("Call Sam", null, Priority.MEDIUM, List.of());

    assertEquals(1, first.id());
    assertEquals("Buy  oat milk", first.title());
    assertEquals(List.of("groceries", "home", "work project, urgent!"), first.tags());
    assertEquals(TaskStatus.TODO, first.status());
    assertEquals(CREATED_AT, first.createdAt());
    assertEquals(2, second.id());
    assertEquals(CREATED_AT, second.createdAt());
  }

  @Test
  void fr9_rejectsEmptyTitlesAndTags() {
    TaskService service = new TaskService(new InMemoryRepository(), CLOCK);

    assertThrows(IllegalArgumentException.class,
        () -> service.add(" \t ", null, Priority.MEDIUM, List.of()));
    assertThrows(IllegalArgumentException.class,
        () -> service.add("Valid", null, Priority.MEDIUM, List.of("  ")));
  }

  @Test
  void fr3_fr4_fr5_listsWithOrTagFilteringAndDeterministicPriorityOrder() {
    InMemoryRepository repository = new InMemoryRepository();
    TaskService service = new TaskService(repository, CLOCK);
    service.add("Low", null, Priority.LOW, List.of("other"));
    service.add("High one", null, Priority.HIGH, List.of("work"));
    service.add("High two", null, Priority.HIGH, List.of("home"));

    assertEquals(List.of(1L, 2L, 3L),
        service.list(List.of(), false).stream().map(Task::id).toList());
    assertEquals(List.of(2L, 3L),
        service.list(List.of(" WORK ", "HOME"), false).stream().map(Task::id).toList());
    assertEquals(List.of(2L, 3L, 1L),
        service.list(List.of(), true).stream().map(Task::id).toList());
  }

  @Test
  void fr6_fr7_updatesFieldsAndStatusWithoutChangingIdentity() {
    InMemoryRepository repository = new InMemoryRepository();
    TaskService service = new TaskService(repository, CLOCK);
    Task created = service.add("Task", "details", Priority.LOW, List.of("old"));

    Task updated =
        service.update(created.id(), " Renamed ", null, false, Priority.HIGH,
            List.of("New", "new"), false);
    assertEquals("Renamed", updated.title());
    assertEquals("details", updated.description());
    assertEquals(List.of("new"), updated.tags());
    assertEquals(created.id(), updated.id());
    assertEquals(created.createdAt(), updated.createdAt());
    assertEquals(TaskStatus.TODO, updated.status());

    Task cleared =
        service.update(created.id(), null, null, true, null, null, true);
    assertNull(cleared.description());
    assertEquals(List.of(), cleared.tags());
    assertEquals(TaskStatus.DONE, service.setStatus(created.id(), TaskStatus.DONE).status());
    assertEquals(TaskStatus.DONE, service.setStatus(created.id(), TaskStatus.DONE).status());
  }

  @Test
  void fr8_fr11_deletionDoesNotReuseIdsAndMissingIdsDoNotSave() {
    InMemoryRepository repository = new InMemoryRepository();
    TaskService service = new TaskService(repository, CLOCK);
    service.add("First", null, Priority.MEDIUM, List.of());
    service.add("Second", null, Priority.MEDIUM, List.of());
    service.delete(1);
    int savesBeforeMissingDelete = repository.saveCount;

    assertThrows(TaskNotFoundException.class, () -> service.delete(99));
    assertEquals(savesBeforeMissingDelete, repository.saveCount);
    assertEquals(3L, service.add("Third", null, Priority.MEDIUM, List.of()).id());
  }

  private static final class InMemoryRepository implements TaskRepository {

    private TaskStore store = TaskStore.empty();
    private int saveCount;

    @Override
    public TaskStore load() throws StorageReadException {
      return store;
    }

    @Override
    public void save(TaskStore newStore) throws StorageWriteException {
      store = newStore;
      saveCount++;
    }
  }
}
