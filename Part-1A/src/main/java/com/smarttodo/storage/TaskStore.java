package com.smarttodo.storage;

import com.smarttodo.domain.Task;
import java.util.List;
import java.util.Objects;

/** Immutable persistence snapshot, including the next ID that has never been allocated. */
public record TaskStore(long nextId, List<Task> tasks) {

  /** Creates an immutable, ID-ordered snapshot. */
  public TaskStore {
    if (nextId <= 0) {
      throw new IllegalArgumentException("nextId must be positive.");
    }
    tasks = List.copyOf(Objects.requireNonNull(tasks, "tasks"));
    long previousId = 0;
    for (Task task : tasks) {
      Objects.requireNonNull(task, "task");
      if (task.id() <= previousId || task.id() >= nextId) {
        throw new IllegalArgumentException("Tasks must be ID-ordered and below nextId.");
      }
      previousId = task.id();
    }
  }

  public static TaskStore empty() {
    return new TaskStore(1, List.of());
  }
}
