package com.smarttodo;

import com.smarttodo.cli.CommandRunner;
import java.nio.file.Path;
import java.time.Clock;

/** Application entry point for the generated todo launcher. */
public final class TodoMain {

  private TodoMain() {}

  /** Starts the application and exits with the CLI contract's status code. */
  public static void main(String[] args) {
    CommandRunner runner =
        new CommandRunner(Path.of("").toAbsolutePath(), Clock.systemUTC(), System.out, System.err);
    System.exit(runner.run(args));
  }
}
