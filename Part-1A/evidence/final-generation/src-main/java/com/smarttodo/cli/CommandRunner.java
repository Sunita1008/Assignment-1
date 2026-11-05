package com.smarttodo.cli;

import com.smarttodo.application.TaskNotFoundException;
import com.smarttodo.application.TaskService;
import com.smarttodo.domain.Task;
import com.smarttodo.storage.FileReplacer;
import com.smarttodo.storage.JsonTaskRepository;
import com.smarttodo.storage.StorageReadException;
import com.smarttodo.storage.StorageWriteException;
import java.io.PrintStream;
import java.nio.file.Path;
import java.time.Clock;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

/** Composes CLI parsing, application operations, persistence, and output streams. */
public final class CommandRunner {

  private final Path workingDirectory;
  private final Clock clock;
  private final Consumer<String> out;
  private final Consumer<String> err;
  private final FileReplacer replacer;

  /** Creates a runner that uses atomic filesystem replacement. */
  public CommandRunner(Path workingDirectory, Clock clock, PrintStream out, PrintStream err) {
    this(workingDirectory, clock, out, err, FileReplacer.atomicMove());
  }

  /** Creates a runner with an injectable persistence replacement operation. */
  public CommandRunner(
      Path workingDirectory,
      Clock clock,
      PrintStream out,
      PrintStream err,
      FileReplacer replacer) {
    this.workingDirectory = workingDirectory;
    this.clock = clock;
    PrintStream checkedOut = Objects.requireNonNull(out, "out");
    PrintStream checkedErr = Objects.requireNonNull(err, "err");
    FileReplacer checkedReplacer = Objects.requireNonNull(replacer, "replacer");
    this.out = checkedOut::print;
    this.err = checkedErr::print;
    this.replacer = (temp, target) -> checkedReplacer.replace(temp, target);
  }

  /** Runs a command and returns its documented process exit status. */
  public int run(String[] args) {
    Command command;
    try {
      command = new ArgumentParser().parse(args);
    } catch (ArgumentParser.ParseException exception) {
      printParseError(exception.kind());
      return 2;
    }

    TaskService service =
        new TaskService(new JsonTaskRepository(workingDirectory, replacer), clock);
    try {
      execute(command, service);
      return 0;
    } catch (TaskNotFoundException exception) {
      err.accept("Error: task #" + exception.taskId() + " not found.\n");
      return 2;
    } catch (StorageReadException exception) {
      err.accept("Error: unable to read tasks file.\n");
      return 1;
    } catch (StorageWriteException exception) {
      err.accept("Error: unable to save tasks file.\n");
      return 1;
    } catch (IllegalArgumentException exception) {
      err.accept("Error: invalid arguments.\n");
      return 2;
    }
  }

  private void execute(Command command, TaskService service) {
    if (command instanceof Command.Add add) {
      Task task = service.add(add.title(), add.description(), add.priority(), add.tags());
      out.accept("Added task #" + task.id() + ": " + task.title() + "\n");
    } else if (command instanceof Command.ListTasks list) {
      List<Task> tasks = service.list(list.tags(), list.sortByPriority());
      if (tasks.isEmpty()) {
        out.accept("No tasks found.\n");
      } else {
        for (Task task : tasks) {
          out.accept(renderTask(task));
          out.accept("\n");
        }
      }
    } else if (command instanceof Command.Update update) {
      Task task =
          service.update(
              update.id(),
              update.title(),
              update.description(),
              update.clearDescription(),
              update.priority(),
              update.tags(),
              update.clearTags());
      out.accept("Updated task #" + task.id() + ".\n");
    } else if (command instanceof Command.SetStatus status) {
      Task task = service.setStatus(status.id(), status.status());
      out.accept("Task #" + task.id() + " status set to " + task.status().name() + ".\n");
    } else if (command instanceof Command.Delete delete) {
      service.delete(delete.id());
      out.accept("Deleted task #" + delete.id() + ".\n");
    } else {
      throw new IllegalStateException("Unsupported parsed command.");
    }
  }

  private static String renderTask(Task task) {
    return "{\"id\":" + task.id()
        + ",\"title\":" + quote(task.title())
        + ",\"description\":"
        + (task.description() == null ? "null" : quote(task.description()))
        + ",\"priority\":" + quote(task.priority().name())
        + ",\"tags\":" + renderTags(task.tags())
        + ",\"status\":" + quote(task.status().name())
        + ",\"createdAt\":" + quote(task.createdAt().toString())
        + "}";
  }

  private static String renderTags(List<String> tags) {
    StringBuilder rendered = new StringBuilder("[");
    for (int index = 0; index < tags.size(); index++) {
      if (index > 0) {
        rendered.append(',');
      }
      rendered.append(quote(tags.get(index)));
    }
    return rendered.append(']').toString();
  }

  private static String quote(String value) {
    StringBuilder rendered = new StringBuilder("\"");
    for (int index = 0; index < value.length(); index++) {
      char character = value.charAt(index);
      switch (character) {
        case '"' -> rendered.append("\\\"");
        case '\\' -> rendered.append("\\\\");
        case '\b' -> rendered.append("\\b");
        case '\f' -> rendered.append("\\f");
        case '\n' -> rendered.append("\\n");
        case '\r' -> rendered.append("\\r");
        case '\t' -> rendered.append("\\t");
        default -> {
          if (character < 0x20 || Character.isSurrogate(character)) {
            rendered.append(String.format("\\u%04x", (int) character));
          } else {
            rendered.append(character);
          }
        }
      }
    }
    return rendered.append('"').toString();
  }

  private void printParseError(ArgumentParser.ErrorKind kind) {
    switch (kind) {
      case INVALID_ARGUMENTS -> err.accept("Error: invalid arguments.\n");
      case INVALID_PRIORITY -> err.accept("Error: priority must be LOW, MEDIUM, or HIGH.\n");
      case INVALID_STATUS -> err.accept("Error: status must be TODO, IN_PROGRESS, or DONE.\n");
      default -> throw new IllegalStateException("Unsupported parser error kind.");
    }
  }
}
