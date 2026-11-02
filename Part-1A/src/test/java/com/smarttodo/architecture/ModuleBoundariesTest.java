package com.smarttodo.architecture;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class ModuleBoundariesTest {

  @Test
  void reqArch001_storageDoesNotDependOnCliParsing() throws IOException {
    Path storage = Path.of("src/main/java/com/smarttodo/storage");
    try (Stream<Path> files = Files.walk(storage)) {
      for (Path file : files.filter(Files::isRegularFile).toList()) {
        assertFalse(Files.readString(file).contains("com.smarttodo.cli"));
      }
    }
    assertTrue(Files.isDirectory(Path.of("src/main/java/com/smarttodo/application")));
    assertTrue(Files.isDirectory(Path.of("src/main/java/com/smarttodo/domain")));
  }
}
