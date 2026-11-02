package com.smarttodo.application;

/** Indicates that an operation referenced an ID not present in the store. */
public final class TaskNotFoundException extends RuntimeException {

  private final long taskId;

  public TaskNotFoundException(long taskId) {
    super("Task not found.");
    this.taskId = taskId;
  }

  public long taskId() {
    return taskId;
  }
}
