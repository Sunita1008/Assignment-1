package com.smarttodo.storage;

/** Indicates that a complete task-store snapshot could not safely be persisted. */
public final class StorageWriteException extends RuntimeException {

  public StorageWriteException(Throwable cause) {
    super(cause);
  }
}
