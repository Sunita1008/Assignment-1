# Quickstart: Smart Todo CLI

## Prerequisites

- JDK 17
- Maven
- Dependencies already available in the local Maven cache for offline
  verification

## Verify the Build and Quality Gates

From the project root:

```text
mvn -o clean verify
```

The command must pass the complete JUnit 5 suite, Checkstyle using
`google_checks` with zero warnings, and SpotBugs with Find Security Bugs and no
Medium-or-higher findings.

## Exercise the CLI

Run commands from a temporary working directory so the test does not touch a
user's existing `tasks.json`. Invoke the generated `todo` (or `todo.bat` on
Windows) launcher by its full path while keeping that temporary directory as
the current working directory:

```text
todo add --title "Buy milk" --priority HIGH --tag groceries
todo add --title "Call Sam"
todo list
todo list --tag GROCERIES
todo list --sort priority
todo update 1 --title "Buy oat milk" --tag groceries --tag home
todo status 1 IN_PROGRESS
todo delete 2
```

Expected behavior: adds report their assigned IDs; list emits compact JSON lines
with stable fields; the tag filter is case-insensitive; priority sort puts HIGH
first; updates preserve creation time and ID; status changes are persisted; and
deletion removes the task. Re-running `todo list` in a new process from the same
working directory reads the persisted store.

## Validate Failure Cases

- Run `todo add --title ""`: stdout is empty, stderr is
  `Error: invalid arguments.`, and exit status is 2.
- Run `todo add --title "Buy milk" --priority URGENT`: stdout is empty, stderr
  is `Error: priority must be LOW, MEDIUM, or HIGH.`, and exit status is 2.
- Run `todo delete 99` when ID 99 is absent: stdout is empty, stderr is
  `Error: task #99 not found.`, and exit status is 2.
- Put malformed JSON in `tasks.json`, then run `todo list`: stdout is empty,
  stderr is `Error: unable to read tasks file.`, exit status is 1, and the file
  bytes remain unchanged.
- Simulate a repository save failure in the storage/CLI tests: stdout is empty,
  stderr is `Error: unable to save tasks file.`, exit status is 1, and no
  success message or stack trace is emitted.

See [cli-contract.md](./contracts/cli-contract.md) for exact syntax and stream
contracts, and [data-model.md](./data-model.md) for persisted fields and
invariants.
