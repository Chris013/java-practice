<img src="assets/icon.svg" width="64" height="64" alt="">

# Java Practice

Practice tasks for writing clean, professional Java, the way strong teams write it. They build on *Effective Java*, *Refactoring*, *A Philosophy of Software Design*, *Modern Java in Action*, the GoF design patterns and Domain-Driven Design.

Each task is written the way a senior engineer would brief a junior colleague: the business situation, the domain explained with diagrams, the exact contract, hints on how to approach it, unit tests that tell you when you are done, and a reference solution with the key design decisions and the mistakes reviewers flag most often.

Plain Java 25 and Maven. No frameworks.

## How to work on a task

1. Open a task in [`tasks/`](tasks) and read it up to "Reference solution". Leave the spoiler closed.
2. Write your code in `src/main/java/io/github/chris013/practice/taskNN/`. The skeletons throw `UnsupportedOperationException("TODO: Task NN")`.
3. Run the task's tests until they are green:
   ```bash
   mvn test -Dtest='**/task07/**'
   ```
4. Go through the task's "Definition of done", then compare your design with the reference solution and its "What I typically flag" table.

Some tasks have a different shape, and their text says so: refactoring tasks start with legacy code and characterization tests that are already green, and in the testing tasks (09, 19, 22) writing the tests is the exercise.

## Tasks

| # | Task | Difficulty | Topics |
|---|---|---|---|
| 01 | [Money: your first real Value Object](tasks/01-money-value-object.md) | 🟢 Easy | Value objects, immutability, static factories, `BigDecimal`, allocation |
| 02 | [Untangle the password checker](tasks/02-password-policy-refactoring.md) | 🟢 Easy | Refactoring, guard clauses, characterization tests |
| 03 | [The Monday sales report](tasks/03-bookshop-sales-report.md) | 🟡 Medium | Streams, collectors, `teeing`, `Comparator`, `Optional` |
| 04 | [Hotel stays and the half-open interval](tasks/04-date-range.md) | 🟢 Easy | Value objects, `java.time`, `Comparable`, define errors out of existence |
| 05 | [The gym invoice: split phase, then add HTML](tasks/05-gym-invoice-refactoring.md) | 🟡 Medium | Refactoring: Split Phase, Replace Conditional, Replace Primitive with Object |
| 06 | [Shipping rates with sealed types and pattern matching](tasks/06-shipping-sealed-types.md) | 🟡 Medium | Sealed interfaces, records, record patterns, exhaustive `switch` |
| 07 | [Resilient exchange rates with decorators](tasks/07-exchange-rate-decorators.md) | 🟡 Medium | Decorator, composition over inheritance, injected `Clock`, fakes |
| 08 | [The order aggregate](tasks/08-order-aggregate.md) | 🔴 Hard | DDD aggregates, invariants, typed IDs, state machine, domain events |
| 09 | [Test data builders and the bug nobody saw](tasks/09-test-data-builders.md) | 🟡 Medium | Testing craft: builders, boundary values, one behaviour per test |
| 10 | [Library lending with ports and adapters](tasks/10-library-lending-hexagonal.md) | 🟡 Medium | Hexagonal architecture, dependency rule, in-memory adapters |
| 11 | [A thread-safe token bucket rate limiter](tasks/11-token-bucket-rate-limiter.md) | 🔴 Hard | Concurrency: CAS with immutable state, `InstantSource`, stress tests |
| 12 | [Document approval with type-state](tasks/12-document-approval-type-state.md) | 🟡 Medium | Illegal states unrepresentable, type-state with sealed types |
| 13 | [A supermarket receipt with smart enums](tasks/13-receipt-enums.md) | 🟢 Easy | Enums with behaviour, `EnumMap`, `EnumSet`, VAT rounding |
| 14 | [A feature flag evaluator as a deep module](tasks/14-feature-flags-deep-module.md) | 🔴 Hard | Deep modules, information hiding, a small parser, hashing rollouts |
| 15 | [Gift card redemption: results, not exceptions](tasks/15-gift-card-results.md) | 🟡 Medium | Sealed result types vs. exceptions, exhaustive `switch` |
| 16 | [A promotion engine with Strategy and Composite](tasks/16-promotion-engine.md) | 🔴 Hard | Strategy, Composite, open/closed, immutable pipeline, money math |
| 17 | [A type-safe event bus](tasks/17-type-safe-event-bus.md) | 🟡 Medium | Generics, PECS, type tokens, Observer, no unchecked casts |
| 18 | [Fix the race conditions in concert seat reservations](tasks/18-seat-reservation-races.md) | 🔴 Hard | Concurrency bugs, `ConcurrentHashMap.compute`, open calls, virtual threads |
| 19 | [Test-drive a working-day calendar](tasks/19-tdd-working-days.md) | 🟡 Medium | TDD, `java.time`, Easter algorithm, composing rules |
| 20 | [An anti-corruption layer for the legacy CRM](tasks/20-anti-corruption-layer.md) | 🔴 Hard | DDD anti-corruption layer, parsing untrusted data, exception translation |
| 21 | [Simplifying formulas with records and nested patterns](tasks/21-expression-simplifier.md) | 🟡 Medium | Algebraic data types, nested record patterns, recursion, Visitor vs. `switch` |
| 22 | [The right test double for the job](tasks/22-test-doubles-password-reset.md) | 🟡 Medium | Dummies, stubs, spies, mocks, fakes; Mockito where it helps |
| 23 | [A travel quote aggregator with deadlines](tasks/23-quote-aggregator.md) | 🔴 Hard | Virtual threads, `invokeAll` with a budget, cancellation, interruption |
| 24 | [Breaking up a god class](tasks/24-god-class-breakup.md) | 🔴 Hard | Large refactoring, Split Phase, Extract Class, legacy facade |
| 25 | [Capstone: subscription billing](tasks/25-capstone-subscription-billing.md) | ⚫ Expert | DDD + hexagonal, proration, idempotent batch jobs, dunning |

🟢 Easy · 🟡 Medium · 🔴 Hard · ⚫ Expert. The order mixes topics and difficulties on purpose, but every task stands on its own.

## Repository layout

```
tasks/NN-*.md                          the task descriptions
src/main/java/.../practice/taskNN/     your code (starter skeletons and legacy code)
src/test/java/.../practice/taskNN/     the tests you make green
solutions/src/main/java/...            reference solutions
solutions/src/test/java/...            reference tests, for the tasks where writing tests is the exercise
docs/AUTHORING.md                      how to write a new task
tools/EmbedCode.java                   copies the code into the task files
```

## Useful commands

```bash
mvn test -Dtest='**/task07/**'                # one task, against your code
mvn test -Psolutions -Dtest='**/task07/**'    # the same tests against the reference solution
mvn verify -Psolutions                        # every reference solution, as CI runs it
java tools/EmbedCode.java                     # refresh the code shown in the task files
```

Requirements: JDK 25 and Maven 3.9 or newer.

## Adding tasks

New batches of tasks are welcome. [`docs/AUTHORING.md`](docs/AUTHORING.md) describes the layout, the rules for starter code, solutions and tests, and the structure of a task file. After writing a task, run `java tools/EmbedCode.java` and add a row to the table above.
