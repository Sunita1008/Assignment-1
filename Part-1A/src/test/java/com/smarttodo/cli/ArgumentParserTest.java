package com.smarttodo.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.smarttodo.domain.Priority;
import com.smarttodo.domain.TaskStatus;
import java.util.List;
import org.junit.jupiter.api.Test;

class ArgumentParserTest {

  private final ArgumentParser parser = new ArgumentParser();

  @Test
  void fr1_fr2_parsesAddFieldsAndRepeatedTags() {
    assertEquals(
        new Command.Add("Title", "description", Priority.HIGH, List.of("work", "home")),
        parser.parse(new String[] {
          "add", "--title", "Title", "--description", "description",
          "--priority", "HIGH", "--tag", "work", "--tag", "home"
        }));
  }

  @Test
  void fr3_fr4_fr5_parsesListFilterAndPrioritySort() {
    assertEquals(
        new Command.ListTasks(List.of("work", "home"), true),
        parser.parse(new String[] {
          "list", "--tag", "work", "--tag", "home", "--sort", "priority"
        }));
  }

  @Test
  void fr6_parsesUpdateAndRejectsNoOpOrConflictingClears() {
    assertEquals(
        new Command.Update(5, null, null, false, null, List.of("home"), false),
        parser.parse(new String[] {"update", "5", "--tag", "home"}));

    assertInvalid("update", "5");
    assertInvalid("update", "5", "--description", "details", "--clear-description");
    assertInvalid("update", "5", "--tag", "home", "--clear-tags");
  }

  @Test
  void fr7_fr9_rejectsInvalidStatusPriorityAndIdsWithSpecificErrorKinds() {
    ArgumentParser.ParseException priority =
        assertThrows(ArgumentParser.ParseException.class,
            () -> parser.parse(new String[] {"add", "--title", "Task", "--priority", "URGENT"}));
    assertEquals(ArgumentParser.ErrorKind.INVALID_PRIORITY, priority.kind());

    ArgumentParser.ParseException status =
        assertThrows(ArgumentParser.ParseException.class,
            () -> parser.parse(new String[] {"status", "1", "BAD"}));
    assertEquals(ArgumentParser.ErrorKind.INVALID_STATUS, status.kind());

    assertInvalid("delete", "0");
    assertEquals(new Command.SetStatus(1, TaskStatus.DONE),
        parser.parse(new String[] {"status", "1", "DONE"}));
  }

  private void assertInvalid(String... args) {
    ArgumentParser.ParseException exception =
        assertThrows(ArgumentParser.ParseException.class, () -> parser.parse(args));
    assertEquals(ArgumentParser.ErrorKind.INVALID_ARGUMENTS, exception.kind());
  }
}
