package com.smarttodo.cli;

import com.smarttodo.domain.Priority;
import com.smarttodo.domain.TaskStatus;
import java.util.ArrayList;
import java.util.List;

/** Plain argument parser for the documented todo command grammar. */
public final class ArgumentParser {

  /** Categories of input errors that have distinct CLI diagnostics. */
  public enum ErrorKind {
    INVALID_ARGUMENTS,
    INVALID_PRIORITY,
    INVALID_STATUS
  }

  /** A rejected command line and its corresponding diagnostic category. */
  public static final class ParseException extends RuntimeException {

    private final ErrorKind kind;

    private ParseException(ErrorKind kind) {
      super(kind.name());
      this.kind = kind;
    }

    public ErrorKind kind() {
      return kind;
    }
  }

  /** Parses one command invocation into its typed representation. */
  public Command parse(String[] args) {
    if (args == null || args.length == 0) {
      throw invalidArguments();
    }
    return switch (args[0]) {
      case "add" -> parseAdd(args);
      case "list" -> parseList(args);
      case "update" -> parseUpdate(args);
      case "status" -> parseStatus(args);
      case "delete" -> parseDelete(args);
      default -> throw invalidArguments();
    };
  }

  private static Command parseAdd(String[] args) {
    String title = null;
    String description = null;
    Priority priority = Priority.MEDIUM;
    List<String> tags = new ArrayList<>();
    boolean hasTitle = false;
    boolean hasDescription = false;
    boolean hasPriority = false;
    for (int index = 1; index < args.length; ) {
      String option = args[index++];
      switch (option) {
        case "--title" -> {
          if (hasTitle) {
            throw invalidArguments();
          }
          title = value(args, index++);
          hasTitle = true;
        }
        case "--description" -> {
          if (hasDescription) {
            throw invalidArguments();
          }
          description = value(args, index++);
          hasDescription = true;
        }
        case "--priority" -> {
          if (hasPriority) {
            throw invalidArguments();
          }
          priority = parsePriority(value(args, index++));
          hasPriority = true;
        }
        case "--tag" -> tags.add(value(args, index++));
        default -> throw invalidArguments();
      }
    }
    if (!hasTitle) {
      throw invalidArguments();
    }
    return new Command.Add(title, description, priority, tags);
  }

  private static Command parseList(String[] args) {
    List<String> tags = new ArrayList<>();
    boolean sortByPriority = false;
    boolean hasSort = false;
    for (int index = 1; index < args.length; ) {
      String option = args[index++];
      switch (option) {
        case "--tag" -> tags.add(value(args, index++));
        case "--sort" -> {
          if (hasSort || !"priority".equals(value(args, index++))) {
            throw invalidArguments();
          }
          hasSort = true;
          sortByPriority = true;
        }
        default -> throw invalidArguments();
      }
    }
    return new Command.ListTasks(tags, sortByPriority);
  }

  private static Command parseUpdate(String[] args) {
    if (args.length < 2) {
      throw invalidArguments();
    }
    long id = parseId(args[1]);
    String title = null;
    String description = null;
    Priority priority = null;
    List<String> tags = null;
    boolean hasTitle = false;
    boolean hasDescription = false;
    boolean clearDescription = false;
    boolean hasPriority = false;
    boolean clearTags = false;
    for (int index = 2; index < args.length; ) {
      String option = args[index++];
      switch (option) {
        case "--title" -> {
          if (hasTitle) {
            throw invalidArguments();
          }
          title = value(args, index++);
          hasTitle = true;
        }
        case "--description" -> {
          if (hasDescription) {
            throw invalidArguments();
          }
          description = value(args, index++);
          hasDescription = true;
        }
        case "--clear-description" -> {
          if (clearDescription) {
            throw invalidArguments();
          }
          clearDescription = true;
        }
        case "--priority" -> {
          if (hasPriority) {
            throw invalidArguments();
          }
          priority = parsePriority(value(args, index++));
          hasPriority = true;
        }
        case "--tag" -> {
          if (tags == null) {
            tags = new ArrayList<>();
          }
          tags.add(value(args, index++));
        }
        case "--clear-tags" -> {
          if (clearTags) {
            throw invalidArguments();
          }
          clearTags = true;
        }
        default -> throw invalidArguments();
      }
    }
    if (hasDescription && clearDescription
        || tags != null && clearTags
        || !hasTitle && !hasDescription && !clearDescription && !hasPriority
            && tags == null && !clearTags) {
      throw invalidArguments();
    }
    return new Command.Update(
        id, title, description, clearDescription, priority, tags, clearTags);
  }

  private static Command parseStatus(String[] args) {
    if (args.length != 3) {
      throw invalidArguments();
    }
    long id = parseId(args[1]);
    try {
      return new Command.SetStatus(id, TaskStatus.valueOf(args[2]));
    } catch (IllegalArgumentException exception) {
      throw new ParseException(ErrorKind.INVALID_STATUS);
    }
  }

  private static Command parseDelete(String[] args) {
    if (args.length != 2) {
      throw invalidArguments();
    }
    return new Command.Delete(parseId(args[1]));
  }

  private static Priority parsePriority(String value) {
    try {
      return Priority.valueOf(value);
    } catch (IllegalArgumentException exception) {
      throw new ParseException(ErrorKind.INVALID_PRIORITY);
    }
  }

  private static long parseId(String value) {
    try {
      long id = Long.parseLong(value);
      if (id <= 0) {
        throw invalidArguments();
      }
      return id;
    } catch (NumberFormatException exception) {
      throw invalidArguments();
    }
  }

  private static String value(String[] args, int index) {
    if (index >= args.length) {
      throw invalidArguments();
    }
    return args[index];
  }

  private static ParseException invalidArguments() {
    return new ParseException(ErrorKind.INVALID_ARGUMENTS);
  }
}
