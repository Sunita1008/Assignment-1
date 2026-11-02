package com.smarttodo.storage;

/** Indicates that the stored task document could not be read or validated. */
public final class StorageReadException extends RuntimeException {

  public StorageReadException(Throwable cause) {
    super(cause);
  }
}
