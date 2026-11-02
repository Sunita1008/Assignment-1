package com.smarttodo.application;

import com.smarttodo.domain.Priority;
import com.smarttodo.domain.Task;
import com.smarttodo.domain.TaskStatus;
import com.smarttodo.storage.TaskRepository;
import com.smarttodo.storage.TaskStore;
import java.time.Clock;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/** Application operations for managing tasks without CLI or filesystem concerns. */
public final class TaskService {

  private final TaskRepository repository;
  private final Clock clock;

  public TaskService(TaskRepository repository, Clock clock) {
    this.repository = repository;
    this.clock = clock;
  }

  /** Creates a task with its allocated ID and current injected time. */
  public Task add(String title, String description, Priority priority, List<String> tags) {
    String normalizedTitle = Objects.requireNonNull(title, "title").strip();
    if (normalizedTitle.isEmpty()) {
      throw new IllegalArgumentException("Task titles must not be empty.");
    }
    List<String> normalizedTags = Task.normalizeTags(tags);
    Objects.requireNonNull(priority, "priority");
    TaskStore store = repository.load();
    Task task =
        new Task(
            store.nextId(),
            normalizedTitle,
            description,
            priority,
            normalizedTags,
            TaskStatus.TODO,
            clock.instant());
    List<Task> tasks = new ArrayList<>(store.tasks());
    tasks.add(task);
    repository.save(new TaskStore(store.nextId() + 1, tasks));
    return task;
  }

  /** Returns tasks in ID order, optionally filtered and sorted by priority. */
  public List<Task> list(List<String> tags, boolean sortByPriority) {
    List<String> normalizedTags = Task.normalizeTags(tags);
    List<Task> tasks = new ArrayList<>(repository.load().tasks());
    if (!normalizedTags.isEmpty()) {
      tasks.removeIf(task -> task.tags().stream().noneMatch(normalizedTags::contains));
    }
    if (sortByPriority) {
      tasks.sort(
          Comparator.comparingInt((Task task) -> priorityRank(task.priority()))
              .thenComparingLong(Task::id));
    }
    return List.copyOf(tasks);
  }

  /** Replaces only the requested editable fields of the identified task. */
  public Task update(
      long id,
      String title,
      String description,
      boolean clearDescription,
      Priority priority,
      List<String> replacementTags,
      boolean clearTags) {
    String normalizedTitle = title == null ? null : title.strip();
    if (normalizedTitle != null && normalizedTitle.isEmpty()) {
      throw new IllegalArgumentException("Task titles must not be empty.");
    }
    List<String> normalizedTags =
        replacementTags == null ? null : Task.normalizeTags(replacementTags);
    TaskStore store = repository.load();
    Task current = find(store, id);
    String newTitle = normalizedTitle == null ? current.title() : normalizedTitle;
    String newDescription =
        clearDescription ? null : description == null ? current.description() : description;
    Priority newPriority = priority == null ? current.priority() : priority;
    List<String> newTags =
        clearTags ? List.of() : normalizedTags == null ? current.tags() : normalizedTags;
    Task updated =
        new Task(
            current.id(),
            newTitle,
            newDescription,
            newPriority,
            newTags,
            current.status(),
            current.createdAt());
    replace(store, updated);
    return updated;
  }

  /** Sets the identified task's progress state. */
  public Task setStatus(long id, TaskStatus status) {
    TaskStore store = repository.load();
    Task current = find(store, id);
    Task updated =
        new Task(
            current.id(),
            current.title(),
            current.description(),
            current.priority(),
            current.tags(),
            status,
            current.createdAt());
    replace(store, updated);
    return updated;
  }

  /** Removes the identified task without reusing its ID. */
  public void delete(long id) {
    TaskStore store = repository.load();
    find(store, id);
    List<Task> tasks = new ArrayList<>(store.tasks());
    tasks.removeIf(task -> task.id() == id);
    repository.save(new TaskStore(store.nextId(), tasks));
  }

  private void replace(TaskStore store, Task updated) {
    List<Task> tasks = new ArrayList<>(store.tasks());
    for (int index = 0; index < tasks.size(); index++) {
      if (tasks.get(index).id() == updated.id()) {
        tasks.set(index, updated);
        repository.save(new TaskStore(store.nextId(), tasks));
        return;
      }
    }
    throw new IllegalStateException("Task disappeared from loaded store.");
  }

  private static Task find(TaskStore store, long id) {
    return store.tasks().stream()
        .filter(task -> task.id() == id)
        .findFirst()
        .orElseThrow(() -> new TaskNotFoundException(id));
  }

  private static int priorityRank(Priority priority) {
    return switch (priority) {
      case HIGH -> 0;
      case MEDIUM -> 1;
      case LOW -> 2;
    };
  }
}
