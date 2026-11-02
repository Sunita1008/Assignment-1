package com.smarttodo.storage;

/** Persistence boundary for complete task-store snapshots. */
public interface TaskRepository {

  TaskStore load() throws StorageReadException;

  void save(TaskStore store) throws StorageWriteException;
}
