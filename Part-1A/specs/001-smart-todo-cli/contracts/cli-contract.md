# CLI Contract

The executable command is `todo`. Parsing is plain argument parsing; options
outside the syntax below are invalid.

## Syntax

```text
todo add --title <title> [--description <text>] [--priority LOW|MEDIUM|HIGH] [--tag <tag> ...]
todo list [--tag <tag> ...] [--sort priority]
todo update <id> [--title <title>] [--description <text>] [--clear-description] [--priority LOW|MEDIUM|HIGH] [--tag <tag> ...] [--clear-tags]
todo status <id> <TODO|IN_PROGRESS|DONE>
todo delete <id>
```

- Add requires a non-empty title. Defaults: priority `MEDIUM`, no tags, no
  description, status `TODO`.
- List may repeat `--tag`; a task matches if it contains any requested tag.
  Tags compare case-insensitively. `--sort priority` orders HIGH, MEDIUM, LOW,
  with ascending ID ties. Default order is ascending ID.
- Update requires at least one field. Repeated `--tag` replaces all tags;
  omitting `--tag` and `--clear-tags` leaves them unchanged. `--clear-tags`
  conflicts with `--tag`; `--clear-description` conflicts with
  `--description`.
- Tags are trimmed, lowercased, and deduplicated while retaining their
  first-supplied order. A tag must contain at least one non-whitespace
  character after trimming; spaces and punctuation are allowed internally.
  IDs are positive integers.

## Success Output

| Operation | stdout |
|---|---|
| Add | `Added task #<id>: <title>\n` |
| Update | `Updated task #<id>.\n` |
| Status | `Task #<id> status set to <STATUS>.\n` |
| Delete | `Deleted task #<id>.\n` |
| Empty list / no filter matches | `No tasks found.\n` |
| Non-empty list | One compact JSON object per line |

Each list object has keys in this order: `id`, `title`, `description`,
`priority`, `tags`, `status`, `createdAt`. An absent description is emitted as
`null`; tags are lowercase JSON strings; creation timestamps use UTC RFC 3339
with `Z`.

## Errors and Exit Status

Every error writes one diagnostic line to stderr, writes nothing to stdout, and
prints no stack trace.

| Category | Diagnostic | Exit |
|---|---|---:|
| Other invalid arguments, title, tag, or ID | `Error: invalid arguments.\n` | 2 |
| Invalid priority | `Error: priority must be LOW, MEDIUM, or HIGH.\n` | 2 |
| Invalid status | `Error: status must be TODO, IN_PROGRESS, or DONE.\n` | 2 |
| Missing task | `Error: task #<id> not found.\n` | 2 |
| Store read, parse, version, or record failure | `Error: unable to read tasks file.\n` | 1 |
| Store save failure | `Error: unable to save tasks file.\n` | 1 |

Successful commands exit 0. The data file is UTF-8 `tasks.json` in the current
working directory, with `version: 1`, `nextId`, and an ID-ordered `tasks` array.
Invalid store data is never overwritten.
