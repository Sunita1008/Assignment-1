# Data Model: Smart Todo CLI

## Task

One work item belonging to the sole local user.

| Field | Type | Required | Rules |
|---|---|---:|---|
| `id` | positive integer | yes | Assigned from `nextId`; immutable; never reused after deletion. |
| `title` | string | yes | Must not be empty or whitespace-only. |
| `description` | string | no | Absent in persisted records when unset; list output represents it as JSON `null`. |
| `priority` | enum | yes | `LOW`, `MEDIUM`, or `HIGH`; defaults to `MEDIUM`. |
| `tags` | ordered string list | yes | Defaults to `[]`; trimmed, lowercased, deduplicated; each tag must contain at least one non-whitespace character after trimming. Internal spaces and punctuation are allowed. |
| `status` | enum | yes | `TODO`, `IN_PROGRESS`, or `DONE`; defaults to `TODO`. |
| `createdAt` | UTC RFC 3339 timestamp | yes | Assigned on creation; immutable; serialized with `Z`. |

Tags retain their first-supplied order. A task matches multiple requested tags
when at least one normalized requested tag is present (OR semantics). Repeating
the current status is allowed and leaves the task unchanged.

## Task Store

The default path is `tasks.json` in the current working directory. It is a
UTF-8 JSON object:

```json
{
  "version": 1,
  "nextId": 2,
  "tasks": [
    {
      "id": 1,
      "title": "Buy milk",
      "priority": "MEDIUM",
      "tags": ["groceries"],
      "status": "TODO",
      "createdAt": "2026-11-01T10:00:00Z"
    }
  ]
}
```

`description` is omitted from a stored record when absent. In list output it is
always present, with `null` for an absent value. `tasks` are stored in ascending
ID order. `nextId` is the next never-used positive integer, including after
deletions; an absent file behaves as an empty collection with `nextId` 1.

The only supported store version is `1`. Malformed JSON, unsupported versions,
and invalid stored records are read failures. They must not be silently reset or
overwritten.

## Operations and Invariants

- **Create**: validate fields, allocate `id = nextId`, set defaults and UTC
  creation time, persist task and increment `nextId`.
- **Update**: require at least one requested change; replace only specified
  fields; `--tag` replaces the complete tag set; explicit clear options remove
  tags or description; preserve ID, creation time, and status unless the
  separate status command is used.
- **Set status**: accept only the three status enum values; same-state updates
  are valid.
- **Delete**: remove the task but do not decrement or reuse `nextId`.
- **List**: without priority sorting, return ascending ID; with priority sort,
  order `HIGH`, `MEDIUM`, `LOW`, then ascending ID.
- **Filter**: normalize requested tags as task tags are normalized and retain
  tasks matching any requested tag.

Only a completely written new document is eligible to replace the previous
store. Read, validation, or write failures do not produce success-shaped output.
