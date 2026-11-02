package com.smarttodo.cli;

import com.smarttodo.domain.Priority;
import com.smarttodo.domain.TaskStatus;
import java.util.List;

/** Typed representation of a parsed CLI invocation. */
public sealed interface Command
    permits Command.Add, Command.ListTasks, Command.Update, Command.SetStatus, Command.Delete {

  /** A request to create a task. */
  record Add(String title, String description, Priority priority, List<String> tags)
      implements Command {
    public Add {
      tags = List.copyOf(tags);
    }
  }

  /** A request to list tasks with optional filtering and sorting. */
  record ListTasks(List<String> tags, boolean sortByPriority) implements Command {
    public ListTasks {
      tags = List.copyOf(tags);
    }
  }

  /** A request to replace selected editable fields. */
  record Update(
      long id,
      String title,
      String description,
      boolean clearDescription,
      Priority priority,
      List<String> tags,
      boolean clearTags)
      implements Command {
    public Update {
      tags = tags == null ? null : List.copyOf(tags);
    }
  }

  /** A request to set one task's progress state. */
  record SetStatus(long id, TaskStatus status) implements Command {}

  /** A request to delete one task. */
  record Delete(long id) implements Command {}
}
