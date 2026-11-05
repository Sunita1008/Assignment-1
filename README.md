# AI-Augmented SDLC — Assignment 1

## What's in this repository

- **Part-1A** contains the Smart Todo CLI, its specifications, implementation, and harness.
- **Part-1B** will be added later.

## Prerequisites

- Java 17 or higher
- Maven
- PowerShell

## Run the Part-1A harness

From the repository root, run:

```powershell
cd Part-1A
powershell -ExecutionPolicy Bypass -File .harness\run_harness.ps1 <label>
```

Replace `<label>` with a short name for the run, such as `initial` or `clarified`.

## Build and run the app

From `Part-1A`, build the app:

```powershell
mvn -o package
```

The Windows launcher is generated at `Part-1A\target\appassembler\bin\todo.bat`. For example, from `Part-1A`:

```powershell
.\target\appassembler\bin\todo.bat add --title "Buy milk"
```

## Evidence

Part-1A run results and generation snapshots are in [Part-1A/evidence/](Part-1A/evidence/). The run history and acceptance decision are recorded in [harness-log.md](Part-1A/evidence/harness-log.md); the `initial-generation/` and `final-generation/` folders contain the respective snapshots.

## Known limitations

See the known verification limitations and acceptance decision in [Part-1A/evidence/harness-log.md](Part-1A/evidence/harness-log.md), Section 6.
