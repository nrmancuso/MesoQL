# Issue #4: Integration Tests Plan

## Status

This is a planning document only. No production code changes are proposed in this phase.

## Requirements

The plan below is based on the issue requirements provided by the user:

1. Start the full stack in CI using GitHub Actions.
2. Add a new module named `integration-tests`.
3. Build a framework that enters the MesoQL shell, runs commands, and captures output.
4. First tests should cover:
   - all examples from the syntax documentation
   - quitting the shell
   - failure scenarios such as bad syntax and bad slash commands
5. Focus on the test framework first, not deep correctness assertions.
6. Do not change production code at this time.
7. The stack must be startable via `just` plus a bash script.
8. Tests must be able to run in parallel.

## Scope

This issue is about establishing an integration-test harness around the shipped application, not
about expanding application features or refactoring core runtime code.

In scope:

- CI boot of the full stack
- a dedicated `integration-tests` Gradle module
- shell-driven black-box tests
- syntax-doc example coverage
- shell error-path coverage
- support scripts and `just` commands to run the stack and tests
- parallel-safe test execution

Out of scope for this issue:

- changing parser, planner, shell, ingestion, or query execution behavior
- broad assertions on semantic result quality
- production refactors to improve testability unless later approved separately

## Existing Constraints From The Codebase

Relevant current facts:

- The shell is implemented in [ShellCommand.java](/Users/nick/IdeaProjects/MesoQL/app/src/main/java/com/mesoql/cli/ShellCommand.java).
- The application entry point is [Main.java](/Users/nick/IdeaProjects/MesoQL/app/src/main/java/com/mesoql/Main.java).
- The current local stack is started by
  [docker-compose.yml](/Users/nick/IdeaProjects/MesoQL/docker-compose.yml),
  [Justfile](/Users/nick/IdeaProjects/MesoQL/Justfile), and
  [quickstart.sh](/Users/nick/IdeaProjects/MesoQL/quickstart.sh).
- Current CI only runs unit tests in [test.yml](/Users/nick/IdeaProjects/MesoQL/.github/workflows/test.yml).
- Query examples to cover come from [Query Language.md](/Users/nick/IdeaProjects/MesoQL/obsidian/users-guide/Query%20Language.md).

## Proposed Architecture

### 1. New `integration-tests` Module

Add a new Gradle submodule:

- `integration-tests`

Primary responsibility:

- own the full-stack integration harness
- launch the packaged application
- drive the shell as an external process
- capture and assert on terminal output

Reason for a dedicated module:

- clean separation from unit tests
- explicit ownership of integration-only dependencies
- easier CI and local execution
- no pressure to retrofit source sets into existing modules for this issue

### 2. Black-Box Shell Test Framework

Tests should interact with MesoQL exactly like a user would:

1. start the full stack
2. launch the MesoQL shell process
3. send input lines to stdin
4. capture stdout/stderr
5. assert on observable behavior

This should avoid direct invocation of production classes in-process for the first wave of tests.

Reason:

- matches the issue requirement exactly
- avoids production code changes
- validates packaging and shell behavior together

### 3. Full Stack Startup Model

The test stack should include:

- OpenSearch
- Ollama
- the built MesoQL application

Startup should be available through:

- `just`
- a bash script

Recommended split:

- one script to start dependencies and wait for readiness
- one script to run the integration tests
- one `just` target that composes them

### 4. Parallel Test Execution Model

Parallel execution is required, so the test harness must avoid shared mutable state where possible.

Recommended approach:

1. Start shared service containers once per test run.
2. Load deterministic fixture data into the real `storm_events` and `forecast_discussions` indices
   before any tests start.
3. Keep every shell test read-only.
4. Run tests in parallel against that shared read-only dataset.

This is the preferred model for this issue because it keeps the tests close to real usage and
avoids introducing special index names or test-only index-routing behavior.

## Proposed Module Contents

Suggested layout:

```text
integration-tests/
  build.gradle.kts
  src/test/java/com/mesoql/integration/
    support/
      ShellSession.java
      ShellProcessLauncher.java
      OutputCollector.java
      StackReadiness.java
      FixtureLoader.java
    ShellSmokeTest.java
    ShellSyntaxExamplesTest.java
    ShellErrorScenariosTest.java
  src/test/resources/
    queries/
    fixtures/
  scripts/
    start-stack.sh
    run-integration-tests.sh
```

## Test Framework Design

### Shell Process Handling

The harness should:

1. launch `java -jar app/build/libs/mesoql-0.1.0.jar`
2. wait until the shell prompt appears
3. write one command at a time
4. wait until output settles or the next prompt appears
5. return captured output to the test

Core helper abstraction:

```java
public final class ShellSession implements AutoCloseable {
    public void awaitPrompt();
    public ShellResult sendLine(String line);
    public ShellResult sendLines(List<String> lines);
    public void close();
}
```

What `ShellResult` should expose:

- raw stdout chunk
- raw stderr chunk
- exit code if the shell exits
- whether a prompt was seen again

### Output Assertions

For this issue, assertions should stay intentionally shallow.

Examples:

- shell starts and shows banner/prompt
- quit commands exit cleanly
- a valid query does not produce `ERROR:`
- an invalid query does produce `ERROR:`
- an invalid slash command is rejected visibly

Avoid strong assertions like:

- exact semantic ranking
- exact generated text
- exact formatting of every field

That is later work.

## Test Coverage Plan

### 1. Shell Startup and Quit

Purpose:

- prove the shell can be launched and exited non-interactively

Scenarios:

- start shell and observe `mesoql>` prompt
- send `\\q` and verify clean exit
- send `exit` and verify clean exit
- send `quit` and verify clean exit

### 2. Syntax Documentation Examples

The first query tests should mirror the examples in [Query Language.md](/Users/nick/IdeaProjects/MesoQL/obsidian/users-guide/Query%20Language.md).

Initial example list:

1. basic semantic search
2. filtered storm events query
3. synthesis query
4. forecast discussions query with `EXPLAIN`
5. thematic clustering query

For each example, first-pass assertions should be:

- shell stays alive
- command does not print `ERROR:`
- prompt returns after command execution

If fixture data supports stronger assertions cheaply, we can add a few:

- output contains `Synthesis`
- output contains `Clusters`
- output contains `explanation:`

### 3. Error Scenarios

Required initial failures:

- malformed query syntax
- missing `SEMANTIC(...)`
- unknown slash command
- empty slash command behavior if relevant
- unknown field validation error

Assertions:

- output contains `ERROR:`
- shell remains interactive after the failure
- prompt returns

### 4. Optional Early Shell Robustness Cases

If cheap to add in the same pass:

- blank line is ignored cleanly
- mixed valid then invalid commands in one session
- multiple queries in a single session

## Example Test Cases

These are examples of how the tests should look. They are intentionally black-box and shell-driven.

### Example 1: Quit Via `\\q`

```java
class ShellSmokeTest {

    @Test
    void quitsWithBackslashQ() {
        try (ShellSession shell = shellLauncher.start()) {
            shell.awaitPrompt();

            final ShellResult result = shell.sendLine("\\q");

            assertThat(result.exitCode()).isEqualTo(0);
        }
    }
}
```

### Example 2: Syntax Doc Basic Search

```java
class ShellSyntaxExamplesTest {

    @Test
    void runsBasicSemanticSearchExample() {
        try (ShellSession shell = shellLauncher.start()) {
            shell.awaitPrompt();

            final ShellResult result = shell.sendLine(
                "SEARCH storm_events WHERE SEMANTIC(\"tornado outbreak\") LIMIT 10"
            );

            assertThat(result.stdout()).doesNotContain("ERROR:");
            assertThat(result.promptSeenAgain()).isTrue();
        }
    }
}
```

### Example 3: Explain Example

```java
@Test
void runsExplainExample() {
    try (ShellSession shell = shellLauncher.start()) {
        shell.awaitPrompt();

        final ShellResult result = shell.sendLine(
            "SEARCH forecast_discussions " +
            "WHERE SEMANTIC(\"atmospheric river precipitation\") " +
            "AND region IN (\"Pacific Northwest\") " +
            "EXPLAIN LIMIT 5"
        );

        assertThat(result.stdout()).doesNotContain("ERROR:");
        assertThat(result.promptSeenAgain()).isTrue();
    }
}
```

### Example 4: Bad Syntax

```java
@Test
void showsErrorForBadSyntaxAndKeepsRunning() {
    try (ShellSession shell = shellLauncher.start()) {
        shell.awaitPrompt();

        final ShellResult result = shell.sendLine("SEARCH WHERE");

        assertThat(result.stdout()).contains("ERROR:");
        assertThat(result.promptSeenAgain()).isTrue();
    }
}
```

### Example 5: Bad Slash Command

```java
@Test
void rejectsUnknownSlashCommand() {
    try (ShellSession shell = shellLauncher.start()) {
        shell.awaitPrompt();

        final ShellResult result = shell.sendLine("\\does-not-exist");

        assertThat(result.stdout()).contains("ERROR:");
        assertThat(result.promptSeenAgain()).isTrue();
    }
}
```

## Full Stack Startup Plan

### Local Execution

Add `just` targets and bash scripts so developers can run the same flow locally that CI uses.

Recommended commands:

- `just integration-stack`
- `just integration-test`

Recommended scripts:

- `integration-tests/scripts/start-stack.sh`
- `integration-tests/scripts/run-integration-tests.sh`

`start-stack.sh` responsibilities:

1. start `docker compose`
2. wait for OpenSearch health
3. wait for Ollama health
4. ensure required Ollama models are present
5. build the application jar
6. load deterministic fixture data if needed

`run-integration-tests.sh` responsibilities:

1. call `start-stack.sh`
2. run the integration test Gradle task

### CI Execution

Update GitHub Actions to start the full stack and run the new module tests.

Recommended shape:

1. build application jar
2. start stack via `just` or the bash script
3. run `./gradlew :integration-tests:test`
4. upload logs and test reports on failure

Important detail:

The issue specifically calls for full-stack startup in CI, so the workflow should use the same
entrypoints we support locally rather than duplicating startup logic inline in YAML.

## Data and Fixture Strategy

Because the first pass is about framework, fixture strategy should be minimal and deterministic.

Recommended approach:

1. Use the existing sample storm-events data where practical.
2. Preload a known dataset into the real application indices before the test suite starts.
3. Ensure forecast discussion queries have fixture data available before any shell tests run.
4. Keep fixture loading outside production code.

Because we are avoiding production changes in this issue, fixture loading should happen through:

- existing CLI commands
- shell commands
- standalone scripts

not through new test-only hooks in production modules.

Recommended suite bootstrap order:

1. start OpenSearch and Ollama
2. build the MesoQL jar
3. ingest fixture data into `storm_events`
4. ingest fixture data into `forecast_discussions`
5. run the shell integration tests in parallel

## Parallel Execution Plan

Tests must be runnable in parallel, so the framework should be designed around session-level
isolation.

### Safe Parallelism Rules

1. Each test gets its own shell process.
2. Tests do not write to shared files in the repo root.
3. Test data is loaded once before the suite and is treated as read-only.
4. Tests do not mutate or delete indexed fixture data.
5. Shared stack startup happens once per test run.
6. Read-only shell scenarios are allowed to run fully in parallel.

### Likely Implementation Tactics

1. Use JUnit 5 parallel execution.
2. Mark stack bootstrap as a one-time suite fixture.
3. Use temporary directories for per-test working files.
4. Keep the shell suite read-only after fixture load completes.

## Risks

### 1. Shell I/O timing and prompt detection

Interactive tests can become flaky if prompt detection is naive.

Mitigation:

- wait for explicit prompt text
- collect output incrementally
- use bounded timeouts with diagnostic dumps

### 2. Ollama model availability in CI

Full-stack CI will be slow or flaky if model pulls are unmanaged.

Mitigation:

- use an explicit startup script
- check whether models already exist before pulling
- persist with predictable readiness checks

### 3. Parallel tests against a shared stack

Shared state can create nondeterminism.

Mitigation:

- preload fixtures into the real indices once before the suite
- keep all shell tests read-only
- use independent shell processes per test

### 4. Requirement mismatch on bad slash commands

Current shell code only has explicit handling for `\\q`, `exit`, and `quit`. Unknown slash commands
may currently surface as parser errors rather than dedicated shell-command errors.

Implication:

- the framework can still test this now
- assertions should target visible failure behavior, not a specific error phrasing

## Acceptance Criteria

Issue `#4` should be considered complete when:

1. A new `integration-tests` module exists.
2. CI starts the full stack before running integration tests.
3. There is a reusable shell test framework that launches MesoQL, sends commands, and captures output.
4. The first test suite covers every example in [Query Language.md](/Users/nick/IdeaProjects/MesoQL/obsidian/users-guide/Query%20Language.md).
5. The suite covers quitting the shell.
6. The suite covers failure scenarios including bad syntax and bad slash commands.
7. Stack startup is available through `just` and a bash script.
8. Tests are configured to run in parallel safely.
9. No production code changes are required to deliver the framework.

## Implementation Order

Recommended sequence:

1. Add `integration-tests` module.
2. Add shell-process harness and prompt/output capture utilities.
3. Add stack startup script and `just` commands.
4. Wire CI to start the stack and run the new module.
5. Add quit tests.
6. Add syntax-doc example tests.
7. Add failure-scenario tests.
8. Enable and verify parallel execution.

## Recommendation

The most important design choice is to keep the first iteration black-box and shell-driven. That
aligns with the issue, avoids production-code churn, and gives a durable base for richer
integration coverage later.
