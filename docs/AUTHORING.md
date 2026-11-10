# Authoring a task

This guide is for whoever writes the next batch of tasks (a person or an AI agent). Tasks 01–07 are the reference. When in doubt, copy what they do.

## Repository layout

```
tasks/NN-short-name.md                                   the task text (one file per task)
src/main/java/io/github/chris013/practice/taskNN/        starter code: what the learner edits
src/test/java/io/github/chris013/practice/taskNN/        the tests the learner must make green
solutions/src/main/java/io/github/chris013/practice/taskNN/   reference solution: a COMPLETE copy of every main class of the task
solutions/src/test/java/io/github/chris013/practice/taskNN/   only for tasks where writing tests IS the exercise
tools/EmbedCode.java                                     copies code into the markdown files
```

* Package: `io.github.chris013.practice.taskNN` (two digits). Each task is self-contained: never import from another task's package.
* `mvn -Psolutions` compiles `solutions/src/main/java` **instead of** `src/main/java`. Therefore the solution folder must contain **every** main class of the task, including the unchanged given ones (records, enums, exceptions, interfaces).
* Test classes in `solutions/src/test/java` must have **different class names** from those in `src/test/java` (both are on the test path under `-Psolutions`).

## Starter code rules

* Given types (records, enums, exceptions, ports, legacy code to refactor) are complete and identical in starter and solution, unless the task is to change them.
* Classes the learner implements are skeletons. Every method body and constructor is `throw new UnsupportedOperationException("TODO: Task NN");`. Keep the Javadoc that describes the contract and add `<p>Task NN: read {@code tasks/NN-....md} first.` on the main class.
* The starter must **compile** (`mvn -B test-compile`), with no warnings under `-Xlint:all`. Unused constructor parameters in skeletons are fine.
* Tests that describe new behaviour fail on the starter. Characterization tests (refactoring tasks) pass on both the starter and the solution.

## Solution rules

* Plain Java 25. No libraries in main code. No Spring, no Lombok.
* This is the "what a strong senior would merge" version: small, final classes, records for values, `private` by default, package-private helpers, sealed types where the set is closed, `Objects.requireNonNull(x, "x")` with the parameter name, `IllegalArgumentException` for bad values and `IllegalStateException` for wrong state, `BigDecimal` for money, `java.time` with an injected `Clock` for time.
* No clever code for its own sake. Javadoc on public types and on the non-obvious public methods, explaining *why*, not *what*.
* No compiler warnings with `-Xlint:all`.

## Test rules

* JUnit 6 Jupiter + AssertJ. Method names in `snake_case` that read as sentences (`rejects_a_wrong_amount_without_changing_anything`). Group with `@Nested` classes. Use `@ParameterizedTest` + `@CsvSource` for tables.
* Hand-written fakes over mocks, unless the task is about mocks (task 22).
* Tests must be deterministic and fast: no `Thread.sleep` for timing, inject `Clock`. Concurrency tests may use threads/latches but must finish in well under 5 s.
* Comments only where a number in an assertion needs explaining (`// 0.95 x 1.5 = 1.425`).

## The markdown file

Write in the voice of a senior engineer guiding a junior they respect: direct, concrete, explains *why*, no fluff, no jokes, no emojis besides the difficulty marker. Plain, clear English (many readers are non-native speakers). Use `**bold**` sparingly for the key idea of a paragraph.

Sections, in this order (see `tasks/07-exchange-rate-decorators.md`):

1. `# Task NN · Title`
2. A table: `| Difficulty | Topics | Time box |` with one of 🟢 Easy, 🟡 Medium, 🔴 Hard, ⚫ Expert.
3. `> **Background reading:**` with concrete chapters/items of *Effective Java*, *Refactoring* (2nd ed.), *A Philosophy of Software Design*, *Modern Java in Action*, DDD books, GoF, JEPs, well-known articles. Only cite things that exist.
4. `---`
5. `## The situation`: the business story and why the problem matters.
6. `## The domain`: glossary table and **at least one Mermaid diagram** (classDiagram, stateDiagram-v2, sequenceDiagram, flowchart, gantt). Keep Mermaid labels simple: avoid brackets, quotes, `<`/`>` and semicolons inside labels.
7. `## What I want from you`: the exact contract, numbered, linking the files (`[`Order`](../src/main/java/io/github/chris013/practice/task08/Order.java)`). Every rule the tests check must be stated here.
8. `## Constraints & non-goals`
9. `## How I'd approach it`: hints, ordering, design trade-offs, pitfalls, without giving the solution away.
10. `## Tests`: the run command `mvn test -Dtest='**/taskNN/**'`, links to the test files, then
    ```
    <details>
    <summary>Show the tests</summary>

    <!-- embed: src/test/java/io/github/chris013/practice/taskNN/SomeTest.java -->
    <!-- /embed -->

    </details>
    ```
11. `## Definition of done`: a `- [ ]` checklist.
12. `## Stretch goals`: numbered, 3–5 items.
13. `---`, then `## Reference solution` wrapped in `<details><summary>Spoiler: open only after you have made a serious attempt</summary>` containing:
    * `### Key decisions`: numbered bold headings, each explaining one design decision and the alternative that was rejected.
    * `### Code`: one `<!-- embed: solutions/src/main/java/... -->` + `<!-- /embed -->` pair per interesting solution file (skip trivial given files).
    * `### What I typically flag in first attempts`: a `| Finding | Why it matters |` table with 5–8 rows.

Leave the embed markers **empty**. `java tools/EmbedCode.java` fills them (it writes a link plus a fenced code block between the markers); `java tools/EmbedCode.java --check` fails if any embed is out of date.

## Checklist before you hand in a task

```bash
mvn -B clean test-compile 2>&1 | grep -E '^\[WARNING\] /|ERROR'               # starter: no errors, no warnings
mvn -B clean test-compile -Psolutions 2>&1 | grep -E '^\[WARNING\] /|ERROR'   # solution: the same
mvn -B test -Dtest='**/taskNN/**'                        # new-behaviour tests FAIL on the starter (characterization tests pass)
mvn -B test -Psolutions -Dtest='**/taskNN/**'            # everything passes on the solution
java tools/EmbedCode.java && java tools/EmbedCode.java --check
```

Always build with `clean` and without `-q`: an incremental build skips unchanged files, and `-q` hides compiler warnings.

Then add the task to the index table in `README.md`.
