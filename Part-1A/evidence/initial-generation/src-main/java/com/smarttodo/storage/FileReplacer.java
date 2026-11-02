package com.smarttodo.storage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/** Replaces a target file with a fully written sibling temporary file. */
@FunctionalInterface
public interface FileReplacer {

  void replace(Path temp, Path target) throws IOException;

  static FileReplacer atomicMove() {
    return (temp, target) -> Files.move(
        temp, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
  }
}
