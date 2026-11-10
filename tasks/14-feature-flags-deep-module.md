# Task 14 · A feature flag evaluator as a deep module

| Difficulty | Topics | Time box |
|---|---|---|
| 🔴 Hard | APOSD deep modules and information hiding · Define errors out of existence · Pull complexity downwards · A small line-based parser with good error messages · Deterministic percentage rollouts by hashing · Sealed rule types behind a tiny public API | 3–4 h |

> **Background reading:** John Ousterhout, *A Philosophy of Software Design* (2nd ed.): ch. 4 "Modules Should Be Deep", ch. 5 "Information Hiding (and Leakage)", ch. 8 "Pull Complexity Downwards", ch. 10 "Define Errors Out Of Existence" · Martin Fowler and Pete Hodgson, "Feature Toggles (aka Feature Flags)", martinfowler.com, 2017 · *Effective Java* Item 15 "Minimize mutability" and Item 23 "Prefer class hierarchies to tagged classes" · JEP 409 "Sealed Classes" · JEP 395 "Records"

---

## The situation

The webshop wants to ship a new checkout in four steps: first to staff, then to customers in Germany and Austria, then to 25 percent of all users, and always with a kill switch that turns the feature off for everyone in one edit.

A previous attempt got this working, but it exposed 14 public classes: `RuleFactory`, `RuleVisitor`, `FlagRepositoryManager`, and so on. Every caller of the feature had to understand how rules are built, how they are visited, and how a percentage is computed. Nobody wanted to know any of that. Callers want to ask one question:

> Is this flag on for this user?

That is the design problem. The module must be **deep**: a very small interface in front of a lot of hidden work (parsing, rule evaluation, hashing, error reporting). Ousterhout's measure is simple. The value of a module is the functionality it hides, and the cost is the complexity of its interface. The previous attempt had a lot of interface for little hidden work. We want the opposite.

## The domain

| Term | Meaning |
|---|---|
| **Flag** | A named switch, for example `new-checkout`. Its name is lowercase words joined by hyphens. |
| **Configuration** | Plain text that lists all flags, their rules and their defaults. It is read once and then used for every request. |
| **Rule** | One condition that turns a flag on: a list of users, a list of countries, or a percentage of users. |
| **Default** | What the flag does when no rule matches: `on` or `off`. Absent means `off`. |
| **Kill switch** | The word `off` on its own line in a flag. It turns the flag off for everyone, whatever the other lines say. |
| **Bucket** | A number from 0 to 99 that is fixed for one flag and one user. A user is on at 25 percent when their bucket is below 25. |
| **Evaluation context** | Who is asking: a user id, a country, or both, or an anonymous visitor with a country. |

### How the module answers one question

```mermaid
flowchart TD
    Start[isEnabled called] --> Known{Flag known}
    Known -->|no| Off1[Return false]
    Known -->|yes| Kill{Kill switch present}
    Kill -->|yes| Off2[Return false]
    Kill -->|no| Rule{Next rule matches}
    Rule -->|next rule| Rule
    Rule -->|yes| On[Return true]
    Rule -->|no more rules| Default{Default is on}
    Default -->|yes| On
    Default -->|no| Off3[Return false]
```

The order of the three steps matters. The kill switch is checked before any rule, so a rule can never turn on a flag that was switched off. Rules are checked top to bottom. Only when none of them matches does the default apply.

Because every rule can only turn a flag on, the order of the rules never changes the answer. It only decides which line is responsible (useful for stretch goal 3) and lets evaluation stop early. The order that does matter is kill switch first, default last.

### Shallow versus deep

The previous attempt had a shape like this one. Each box is a public class that a caller could in principle touch:

```mermaid
classDiagram
    class RuleFactory {
        +create(String line) Rule
    }
    class RuleVisitor {
        +visitUser(UserRule rule) boolean
        +visitPercent(PercentRule rule) boolean
    }
    class FlagRepositoryManager {
        +load(String text) void
        +reload() void
    }
    class FlagDefinition
    class FlagParser
    class ConfigLineReader
    class UserRule
    class CountryRule
    class PercentRule
    class PercentageHasher
    class RuleEvaluator
    class FlagRegistry
    class EvaluationContext
    class EvaluationResult
```

The new design has three public types. Everything else is package-private, and a caller cannot see it:

```mermaid
classDiagram
    class FeatureFlags {
        +parse(String configuration) FeatureFlags
        +isEnabled(String flagName, EvaluationContext context) boolean
        +flagNames() Set~String~
    }
    class EvaluationContext {
        +user(String userId, String country) EvaluationContext
        +anonymous(String country) EvaluationContext
        +userId() Optional~String~
        +country() Optional~String~
    }
    class FlagConfigurationException {
        +lineNumber() int
    }
    class FlagConfigParser {
        <<package-private>>
    }
    class Flag {
        <<package-private>>
    }
    class Rule {
        <<sealed package-private>>
    }
    FeatureFlags ..> EvaluationContext : asks
    FeatureFlags ..> FlagConfigurationException : throws
    FeatureFlags ..> FlagConfigParser : uses
    FeatureFlags ..> Flag : holds
    Flag o--> Rule : checks in order
```

Count the public names in each design. Before: 14 classes, every one of them something a caller might need to understand. After: 3 types and 8 public methods in total. The second design does not do less. It does the same work, but the work is hidden.

### What a caller sees

```mermaid
sequenceDiagram
    participant Ops as Config file
    participant Caller as Checkout code
    participant FF as FeatureFlags
    participant P as FlagConfigParser
    participant F as Flag
    Ops->>FF: parse(text)
    FF->>P: parse text
    P-->>FF: flags or FlagConfigurationException
    FF-->>Ops: instance kept for the whole process
    Caller->>FF: isEnabled(new-checkout, context)
    FF->>F: isOnFor(context)
    F-->>FF: true or false
    FF-->>Caller: true or false
```

The caller never sees `FlagConfigParser`, `Flag` or any rule. The parse step happens once at start-up or on reload. The check happens on every request, so it must be cheap and must not throw.

## What I want from you

Implement the module in the package `io.github.chris013.practice.task14`. Only three types are public. Everything else is yours to design.

1. **[`FeatureFlags`](../src/main/java/io/github/chris013/practice/task14/FeatureFlags.java)**
   * `static FeatureFlags parse(String configuration)` reads the whole text. It throws `FlagConfigurationException` when the text is invalid. `null` throws `NullPointerException`.
   * `boolean isEnabled(String flagName, EvaluationContext context)` answers the question. Both arguments must not be `null`.
   * `Set<String> flagNames()` returns the names of all flags, in the order they are written in the configuration. The set cannot be modified.
   * The instance is immutable.
2. **[`EvaluationContext`](../src/main/java/io/github/chris013/practice/task14/EvaluationContext.java)** is a `final` class, not a record.
   * `static EvaluationContext user(String userId, String country)`: the user id must not be `null` (`NullPointerException`) and must not be blank (`IllegalArgumentException`). The country may be `null` when it is unknown.
   * `static EvaluationContext anonymous(String country)`: the country may be `null`.
   * `Optional<String> userId()` is empty for an anonymous visitor.
   * `Optional<String> country()` is empty when the country is unknown.
3. **[`FlagConfigurationException`](../src/main/java/io/github/chris013/practice/task14/FlagConfigurationException.java)** extends `RuntimeException`.
   * `int lineNumber()` returns the 1-based line where the problem was found.
   * The message starts with `line <n>: ` and then says what is wrong, for example `line 3: percent must be between 0 and 100 but was 120`.
4. **The file format.**
   * `#` starts a comment. A comment is a line whose first non-blank character is `#`. Blank lines are ignored.
   * `flag <name>` starts a flag and is **not** indented. The name matches `[a-z0-9]+(-[a-z0-9]+)*`. A duplicate name is an error.
   * Every other line that belongs to a flag is **indented** by at least one space or tab.
   * A non-indented line that is neither `flag ...`, a comment nor blank is an error.
   * An indented line before the first flag is an error.
5. **The rules, all inside one flag.**
   * `users <id>, <id>, ...` is on when the context has one of these user ids. Matching is exact and case-sensitive. An anonymous visitor never matches.
   * `countries <CC>, ...` takes two uppercase letters per country, otherwise it is an error. It is on when the context's country is in the list. Anonymous visitors can match by country.
   * `percent <n>` takes a whole number from 0 to 100. The user is on when their **bucket** is below `n`. The bucket is `Math.floorMod((flagName + ":" + userId).hashCode(), 100)`. Anonymous visitors never match.
   * `default on` or `default off`: what applies when no rule matched. At most one `default` line per flag. Without it the default is off.
   * `off` alone on a line is the **kill switch**. The flag is off for everyone, wherever the line appears in the block. `off` takes no value.
   * An empty list (`users`, `countries`) and an empty entry (`users alice,,bob`) are errors.
6. **Evaluation order.** A flag is evaluated like this: if the kill switch is present, the answer is `false`. Otherwise the rules are tried top to bottom, and the first one that matches makes the answer `true`. If none matches, the default applies. Because every rule can only turn a flag on, the order of the rules never changes the answer. It only decides which line is responsible (useful for stretch goal 3) and lets evaluation stop early. The order that does matter is kill switch first, default last.
7. **Unknown flags.** `isEnabled` with a flag name that is not in the configuration returns `false`. It never throws.
8. **Errors.** Each of these is a `FlagConfigurationException` with the right line number and a message that names the problem:
   * an unknown keyword, for example `line 7: unknown keyword 'percentage'`;
   * a rule before any flag;
   * an invalid or duplicate flag name, or a `flag` line without a name;
   * a percentage that is not a whole number, or is outside 0 to 100;
   * a country code that is not two uppercase letters;
   * a `default` value other than `on` or `off`, or a second `default` line for the same flag;
   * an empty list or an empty list entry;
   * `off` with a value.

   Parsing stops at the first error. The line number is the line where the error was detected.

## Constraints & non-goals

* **Only three public types.** No public `Rule`, no public parser "for testing", no public factory for rules. Tests use the public API only, and that is a rule, not a suggestion.
* **No regular expression for the whole grammar.** A regular expression for each line type is fine. One regex that matches a whole configuration is not, because nobody can read it or give a good error message from it.
* **No `Random`.** A user who is on at 25 percent must stay on. A random number changes on every call, so users would flip between variants on every page load.
* **No reading files, no reloading, no clocks.** `parse` takes a `String`. Where the text comes from is the caller's problem, and the module should not know.
* **No mutable state after `parse`.** The instance is shared by all request threads. It must never change after it is built.
* **No third-party libraries** in the main code. Plain Java 25.

## How I'd approach it

* **Write the public API first, as a caller would use it.** Write a short example for each use: a parse at start-up, a check in a controller, a test. Only then write the internals. If an internal type is needed by the API, you have a leak.
* **Split each line by its first word.** `line.strip().split("\\s+", 2)` gives you the keyword and the rest. Then a `switch` on the keyword says what the line means, and the error for an unknown keyword is one line. Compare that with one regex that tries to match every line type at once.
* **Keep the line number in one place.** Every error goes through one helper that knows the current line. If you pass the line number as a parameter to twenty methods, you will forget one.
* **Decide what a blank line and a comment are before you write the loop.** Is `  # note` inside a flag a comment or an error? The spec says a comment is a line whose first non-blank character is `#`. Follow it, and test it.
* **Hashing: think about the sign.** `hashCode()` can be negative, and `%` keeps the sign of the left operand. `Math.floorMod` does not. Also think about why we hash the flag name too: with only the user id, every rollout would pick the same users first. Two flags at 25 percent would be the same 25 percent of people.
* **Stability across JVMs.** The Java API documentation of `String.hashCode()` gives the exact formula, so the same string has the same hash on every JVM and every release. That is why the bucket can be part of the contract.
* **Anonymous visitors.** An anonymous visitor has no id, so no stable bucket. Think about what a percentage rollout would do to such a visitor if we gave them a random bucket on every request. Then decide what the rule should do.
* **The unknown-flag decision.** Returning `false` is a design choice (see the key decisions in the spoiler). Write down in your PR description when it would be wrong to make this choice, and what you would do instead for those flags.
* **Sealed rules.** `sealed interface Rule permits ...` with records is a good fit: the set of rule kinds is closed, and a `switch` over it is checked by the compiler. Decide where the behaviour lives: a `matches` method on each record, or one `switch` in the evaluator. Either is fine. Be able to say why you chose the one you chose.
* **Immutability.** `Map.copyOf`, `List.copyOf`, `Set.copyOf` and records make this easy. A class with a `private` constructor and no setters is enough.
* **Tests through the public API only.** If a test needs to reach a package-private class, that is a signal that the test is checking an implementation detail. Test the behaviour the caller sees: the answer, the line number, the message.

## Tests

Run: `mvn test -Dtest='**/task14/**'`

[`FeatureFlagsTest`](../src/test/java/io/github/chris013/practice/task14/FeatureFlagsTest.java) covers the rules of the public API, grouped by topic: parsing, users, countries, percentages, defaults, the kill switch, rule order, unknown flags and evaluation contexts. The percentage tests use 10,000 simulated users. They check the share, that the same user gets the same answer every time, and that a user who is on at 25 percent is still on at 50 percent.

[`FlagConfigurationErrorsTest`](../src/test/java/io/github/chris013/practice/task14/FlagConfigurationErrorsTest.java) is a table of invalid configurations. Each row gives the line number and a fragment of the expected message.

The starter fails all of these tests, because every method throws `UnsupportedOperationException`. That is expected.

<details>
<summary>Show the tests</summary>

<!-- embed: src/test/java/io/github/chris013/practice/task14/FeatureFlagsTest.java -->
**[`FeatureFlagsTest.java`](../src/test/java/io/github/chris013/practice/task14/FeatureFlagsTest.java)**

```java
package io.github.chris013.practice.task14;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class FeatureFlagsTest {

    private static final EvaluationContext ALICE = EvaluationContext.user("alice", "DE");
    private static final EvaluationContext BOB = EvaluationContext.user("bob", "FR");
    private static final EvaluationContext CAROL = EvaluationContext.user("carol", null);
    private static final EvaluationContext ANONYMOUS = EvaluationContext.anonymous(null);

    @Nested
    class Parsing {

        @Test
        void ignores_comments_and_blank_lines() {
            FeatureFlags flags = FeatureFlags.parse("""
                    # rollout plan for March

                    # the kill switch lives in the ops runbook
                    flag dark-mode
                      # indented comments are comments too
                      default on

                    """);

            assertThat(flags.isEnabled("dark-mode", ANONYMOUS)).isTrue();
        }

        @Test
        void reads_several_flags() {
            FeatureFlags flags = FeatureFlags.parse("""
                    flag new-checkout
                      users alice
                    flag dark-mode
                      default on
                    flag legacy-export
                      off
                    """);

            assertThat(flags.flagNames()).containsExactly("new-checkout", "dark-mode", "legacy-export");
        }

        @Test
        void lists_flag_names_in_the_order_they_are_written() {
            FeatureFlags flags = FeatureFlags.parse("""
                    flag zeta
                      off
                    flag alpha
                      default on
                    flag mid
                      users alice
                    """);

            assertThat(flags.flagNames()).containsExactly("zeta", "alpha", "mid");
        }

        @Test
        void an_empty_configuration_has_no_flags() {
            FeatureFlags flags = FeatureFlags.parse("# nothing yet\n\n");

            assertThat(flags.flagNames()).isEmpty();
        }

        @Test
        void accepts_flag_names_with_digits_and_hyphens() {
            FeatureFlags flags = FeatureFlags.parse("""
                    flag v2-checkout-3
                      default on
                    """);

            assertThat(flags.isEnabled("v2-checkout-3", ANONYMOUS)).isTrue();
        }

        @Test
        void accepts_tab_indented_rules() {
            FeatureFlags flags = FeatureFlags.parse("flag dark-mode\n\tdefault on");

            assertThat(flags.isEnabled("dark-mode", ANONYMOUS)).isTrue();
        }

        @Test
        void a_flag_without_any_rule_is_off() {
            FeatureFlags flags = FeatureFlags.parse("flag empty-flag");

            assertThat(flags.isEnabled("empty-flag", ALICE)).isFalse();
        }

        @Test
        void rejects_a_null_configuration() {
            assertThatThrownBy(() -> FeatureFlags.parse(null)).isInstanceOf(NullPointerException.class);
        }
    }

    @Nested
    class Users {

        private final FeatureFlags flags = FeatureFlags.parse("""
                flag new-checkout
                  users alice, bob
                """);

        @Test
        void is_on_for_each_listed_user() {
            assertThat(flags.isEnabled("new-checkout", ALICE)).isTrue();
            assertThat(flags.isEnabled("new-checkout", BOB)).isTrue();
        }

        @Test
        void is_off_for_other_users() {
            assertThat(flags.isEnabled("new-checkout", CAROL)).isFalse();
        }

        @Test
        void never_matches_anonymous_visitors() {
            assertThat(flags.isEnabled("new-checkout", ANONYMOUS)).isFalse();
        }

        @Test
        void matches_user_ids_exactly() {
            assertThat(flags.isEnabled("new-checkout", EvaluationContext.user("Alice", "DE"))).isFalse();
        }
    }

    @Nested
    class Countries {

        private final FeatureFlags flags = FeatureFlags.parse("""
                flag new-checkout
                  countries DE, AT
                """);

        @Test
        void is_on_for_users_from_a_listed_country() {
            assertThat(flags.isEnabled("new-checkout", ALICE)).isTrue();
            assertThat(flags.isEnabled("new-checkout", EvaluationContext.user("dora", "AT"))).isTrue();
        }

        @Test
        void is_off_for_users_from_other_countries() {
            assertThat(flags.isEnabled("new-checkout", BOB)).isFalse();
        }

        @Test
        void does_not_match_when_the_country_is_unknown() {
            assertThat(flags.isEnabled("new-checkout", CAROL)).isFalse();
        }

        @Test
        void also_matches_anonymous_visitors_by_country() {
            assertThat(flags.isEnabled("new-checkout", EvaluationContext.anonymous("DE"))).isTrue();
            assertThat(flags.isEnabled("new-checkout", EvaluationContext.anonymous("CH"))).isFalse();
        }
    }

    @Nested
    class Percent {

        private static final String FLAG = "new-checkout";

        private static FeatureFlags withPercent(int percent) {
            return FeatureFlags.parse("flag " + FLAG + "\n  percent " + percent);
        }

        private static List<EvaluationContext> users(int count) {
            return IntStream.range(0, count)
                    .mapToObj(i -> EvaluationContext.user("user-" + i, "DE"))
                    .toList();
        }

        @Test
        void zero_percent_matches_nobody() {
            FeatureFlags flags = withPercent(0);

            assertThat(users(1_000)).noneMatch(user -> flags.isEnabled(FLAG, user));
        }

        @Test
        void hundred_percent_matches_every_known_user() {
            FeatureFlags flags = withPercent(100);

            assertThat(users(1_000)).allMatch(user -> flags.isEnabled(FLAG, user));
        }

        @Test
        void anonymous_visitors_never_match_a_percentage() {
            assertThat(withPercent(100).isEnabled(FLAG, ANONYMOUS)).isFalse();
        }

        @Test
        void roughly_the_configured_share_of_users_is_on() {
            FeatureFlags flags = withPercent(50);

            long on = users(10_000).stream().filter(user -> flags.isEnabled(FLAG, user)).count();

            assertThat(on).isBetween(4_700L, 5_300L);
        }

        @Test
        void the_same_user_gets_the_same_answer_every_time() {
            FeatureFlags first = withPercent(50);
            FeatureFlags second = withPercent(50);

            for (EvaluationContext user : users(1_000)) {
                boolean answer = first.isEnabled(FLAG, user);
                assertThat(first.isEnabled(FLAG, user)).isEqualTo(answer);
                assertThat(second.isEnabled(FLAG, user)).isEqualTo(answer);
            }
        }

        @Test
        void a_user_on_at_25_percent_is_still_on_at_50_percent() {
            FeatureFlags at25 = withPercent(25);
            FeatureFlags at50 = withPercent(50);

            for (EvaluationContext user : users(10_000)) {
                if (at25.isEnabled(FLAG, user)) {
                    assertThat(at50.isEnabled(FLAG, user)).as(user.userId().orElseThrow()).isTrue();
                }
            }
        }

        @Test
        void a_user_is_on_exactly_when_their_bucket_is_below_the_percentage() {
            FeatureFlags flags = withPercent(25);

            for (EvaluationContext user : users(1_000)) {
                String userId = user.userId().orElseThrow();
                // The bucket formula is part of the contract: the flag name and the user id are hashed together.
                boolean expected = Math.floorMod((FLAG + ":" + userId).hashCode(), 100) < 25;
                assertThat(flags.isEnabled(FLAG, user)).as(userId).isEqualTo(expected);
            }
        }

        @Test
        void different_flags_split_the_users_differently() {
            FeatureFlags flags = FeatureFlags.parse("""
                    flag new-checkout
                      percent 50
                    flag dark-mode
                      percent 50
                    """);

            long differing = users(1_000).stream()
                    .filter(user -> flags.isEnabled(FLAG, user) != flags.isEnabled("dark-mode", user))
                    .count();

            assertThat(differing).isGreaterThan(0);
        }
    }

    @Nested
    class Defaults {

        @Test
        void the_default_is_off_when_it_is_not_written() {
            FeatureFlags flags = FeatureFlags.parse("""
                    flag new-checkout
                      users alice
                    """);

            assertThat(flags.isEnabled("new-checkout", BOB)).isFalse();
        }

        @Test
        void default_on_applies_when_no_rule_matches() {
            FeatureFlags flags = FeatureFlags.parse("""
                    flag new-checkout
                      users alice
                      default on
                    """);

            assertThat(flags.isEnabled("new-checkout", ALICE)).isTrue();
            assertThat(flags.isEnabled("new-checkout", BOB)).isTrue();
            assertThat(flags.isEnabled("new-checkout", ANONYMOUS)).isTrue();
        }

        @Test
        void default_off_can_be_written_explicitly() {
            FeatureFlags flags = FeatureFlags.parse("""
                    flag new-checkout
                      users alice
                      default off
                    """);

            assertThat(flags.isEnabled("new-checkout", ALICE)).isTrue();
            assertThat(flags.isEnabled("new-checkout", BOB)).isFalse();
        }

        @Test
        void a_flag_with_only_a_default_is_on_for_everyone() {
            FeatureFlags flags = FeatureFlags.parse("""
                    flag dark-mode
                      default on
                    """);

            assertThat(flags.isEnabled("dark-mode", ANONYMOUS)).isTrue();
            assertThat(flags.isEnabled("dark-mode", CAROL)).isTrue();
        }
    }

    @Nested
    class KillSwitch {

        @Test
        void off_turns_the_flag_off_for_everyone_whatever_the_other_lines_say() {
            FeatureFlags flags = FeatureFlags.parse("""
                    flag new-checkout
                      users alice
                      countries DE
                      percent 100
                      default on
                      off
                    """);

            assertThat(flags.isEnabled("new-checkout", ALICE)).isFalse();
            assertThat(flags.isEnabled("new-checkout", BOB)).isFalse();
            assertThat(flags.isEnabled("new-checkout", ANONYMOUS)).isFalse();
        }

        @Test
        void off_works_when_it_is_written_first() {
            FeatureFlags flags = FeatureFlags.parse("""
                    flag new-checkout
                      off
                      default on
                    """);

            assertThat(flags.isEnabled("new-checkout", BOB)).isFalse();
        }

        @Test
        void off_only_affects_its_own_flag() {
            FeatureFlags flags = FeatureFlags.parse("""
                    flag legacy-export
                      off
                    flag dark-mode
                      default on
                    """);

            assertThat(flags.isEnabled("legacy-export", ALICE)).isFalse();
            assertThat(flags.isEnabled("dark-mode", ALICE)).isTrue();
        }
    }

    @Nested
    class RuleOrder {

        @Test
        void a_flag_is_on_when_any_rule_matches_wherever_the_rule_is_written() {
            FeatureFlags flags = FeatureFlags.parse("""
                    flag new-checkout
                      users alice
                      countries FR
                    """);

            assertThat(flags.isEnabled("new-checkout", ALICE)).isTrue();
            assertThat(flags.isEnabled("new-checkout", BOB)).isTrue();
            assertThat(flags.isEnabled("new-checkout", CAROL)).isFalse();
        }

        @Test
        void reversing_the_rules_does_not_change_any_answer() {
            FeatureFlags forward = FeatureFlags.parse("""
                    flag new-checkout
                      users alice
                      countries FR
                      percent 30
                    """);
            FeatureFlags backward = FeatureFlags.parse("""
                    flag new-checkout
                      percent 30
                      countries FR
                      users alice
                    """);

            for (EvaluationContext user : List.of(ALICE, BOB, CAROL, ANONYMOUS)) {
                assertThat(backward.isEnabled("new-checkout", user))
                        .isEqualTo(forward.isEnabled("new-checkout", user));
            }
        }

        @Test
        void the_default_applies_only_when_no_rule_matched() {
            FeatureFlags flags = FeatureFlags.parse("""
                    flag new-checkout
                      users alice
                      default off
                      countries FR
                    """);

            assertThat(flags.isEnabled("new-checkout", ALICE)).isTrue();
            assertThat(flags.isEnabled("new-checkout", BOB)).isTrue();
            assertThat(flags.isEnabled("new-checkout", CAROL)).isFalse();
        }
    }

    @Nested
    class UnknownFlag {

        @Test
        void an_unknown_flag_is_off_for_everyone() {
            FeatureFlags flags = FeatureFlags.parse("""
                    flag new-checkout
                      default on
                    """);

            assertThat(flags.isEnabled("missing-flag", ALICE)).isFalse();
            assertThat(flags.isEnabled("missing-flag", ANONYMOUS)).isFalse();
        }

        @Test
        void every_flag_is_unknown_in_an_empty_configuration() {
            FeatureFlags flags = FeatureFlags.parse("");

            assertThat(flags.isEnabled("new-checkout", ALICE)).isFalse();
        }

        @Test
        void a_null_flag_name_is_a_programming_error_not_an_unknown_flag() {
            FeatureFlags flags = FeatureFlags.parse("");

            assertThatThrownBy(() -> flags.isEnabled(null, ALICE)).isInstanceOf(NullPointerException.class);
        }
    }

    @Nested
    class EvaluationContexts {

        @Test
        void a_user_has_an_id_and_may_have_an_unknown_country() {
            EvaluationContext carol = EvaluationContext.user("carol", null);

            assertThat(carol.userId()).contains("carol");
            assertThat(carol.country()).isEmpty();
        }

        @Test
        void an_anonymous_visitor_has_no_user_id() {
            EvaluationContext visitor = EvaluationContext.anonymous("DE");

            assertThat(visitor.userId()).isEmpty();
            assertThat(visitor.country()).contains("DE");
        }

        @Test
        void an_anonymous_visitor_may_have_no_country() {
            assertThat(EvaluationContext.anonymous(null).country()).isEmpty();
        }

        @Test
        void a_user_needs_an_id_that_is_not_blank() {
            assertThatThrownBy(() -> EvaluationContext.user(null, "DE")).isInstanceOf(NullPointerException.class);
            assertThatThrownBy(() -> EvaluationContext.user("  ", "DE")).isInstanceOf(IllegalArgumentException.class);
        }
    }
}
```
<!-- /embed -->

<!-- embed: src/test/java/io/github/chris013/practice/task14/FlagConfigurationErrorsTest.java -->
**[`FlagConfigurationErrorsTest.java`](../src/test/java/io/github/chris013/practice/task14/FlagConfigurationErrorsTest.java)**

```java
package io.github.chris013.practice.task14;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Every invalid input must fail with the right line number and a message that says what is wrong. In the tables
 * below, {@code ~} stands for a line break, and the leading spaces of an indented line are kept as written.
 */
class FlagConfigurationErrorsTest {

    @ParameterizedTest
    @CsvSource(quoteCharacter = '"', delimiter = '|', ignoreLeadingAndTrailingWhitespace = false, value = {
        "  percent 10|1|appears before any flag",
        "# only a comment~  percent 5|2|appears before any flag",
        "flag a~  percentage 10|2|unknown keyword 'percentage'",
        "flag a~  Percent 5|2|unknown keyword 'Percent'",
        "flag a~  percent 5~  users alice~  bogus|4|unknown keyword 'bogus'",
        "flag a~  percent 120|2|percent must be between 0 and 100 but was 120",
        "flag a~  percent -1|2|percent must be between 0 and 100 but was -1",
        "flag one~  default on~~flag two~  percent 101|5|percent must be between 0 and 100 but was 101",
        "flag a~  percent fifty|2|percent must be a whole number but was 'fifty'",
        "flag a~  percent|2|percent needs a number",
        "flag Bad|1|invalid flag name 'Bad'",
        "flag -a|1|invalid flag name '-a'",
        "flag a-|1|invalid flag name 'a-'",
        "flag|1|flag needs a name",
        "flag a~flag a|2|duplicate flag 'a'",
        "flag a~  default on~  default off|3|default is already set for flag 'a'",
        "flag a~  default maybe|2|default must be on or off but was 'maybe'",
        "flag a~  countries de|2|country must be two uppercase letters but was 'de'",
        "flag a~  countries DE,at|2|two uppercase letters but was 'at'",
        "flag a~  countries|2|countries needs at least one value",
        "flag a~  users|2|users needs at least one value",
        "flag a~  users alice,,bob|2|empty entry in users list",
        "flag a~  users alice,|2|empty entry in users list",
        "flag a~  off now|2|'off' takes no value but found 'now'",
        "flag a~  flag b|2|flag lines must not be indented",
        "flag a~percent 5|2|expected 'flag <name>' at the start of a line but found 'percent'"
    })
    void rejects_invalid_configuration_with_its_line_number(String configuration, int line, String messageFragment) {
        String text = configuration.replace('~', '\n');

        assertThatThrownBy(() -> FeatureFlags.parse(text))
                .isInstanceOfSatisfying(FlagConfigurationException.class, e -> {
                    assertThat(e.lineNumber()).isEqualTo(line);
                    assertThat(e.getMessage()).startsWith("line " + line + ": ").contains(messageFragment);
                });
    }
}
```
<!-- /embed -->

</details>

## Definition of done

- [ ] All tests green, and the whole test class runs in well under a second.
- [ ] `FeatureFlags`, `EvaluationContext` and `FlagConfigurationException` are the only public types in the package.
- [ ] Every error message starts with `line <n>: ` and the line number is the line where the problem is.
- [ ] Percentages use `Math.floorMod` on the hash of the flag name and the user id. No `Random`.
- [ ] The kill switch wins over every rule and over the default, wherever it is written.
- [ ] `FeatureFlags` is immutable after `parse`.
- [ ] Your PR description says when returning `false` for an unknown flag would be a mistake, and what you would do for such a flag.

## Stretch goals

1. **Atomic reload.** Keep the current `FeatureFlags` in an `AtomicReference<FeatureFlags>`. Parse the new text first, and swap only if the parse succeeds. Why is a failed reload better than a half-loaded one?
2. **Variants for A/B tests.** Add `variant <name>` that returns a string such as `control` or `treatment`, with `isEnabled` as a shortcut for "is not control". Does a second kind of answer make the public API deeper or shallower?
3. **An `explain` method for support staff.** `explain(flagName, context)` returns which line decided the answer. It is very useful, and it needs the parser to keep more information. Does it make the module shallower? What would you hide to keep it deep?
4. **Groups.** Add `groups beta-testers` that matches the users in a named group loaded from a second configuration section. Where does the group membership live, and who owns the list?

---

## Reference solution

<details>
<summary>Spoiler: open only after you have made a serious attempt</summary>

### Key decisions

**1. Three public types, everything else package-private.** `FeatureFlags`, `EvaluationContext` and `FlagConfigurationException` are the whole public surface. The parser, the flags, the rules and the bucketing are package-private. The rejected alternative was to make `Rule` public "so that tests can construct rules". That would have made every rule a public promise, and it would have forced tests to test internals. The public API is what the webshop needs, and the tests only use that.

**2. `EvaluationContext` is a final class with two named factories.** A record `EvaluationContext(String userId, String country)` would have a canonical constructor that accepts `null` and blank strings, and it would tempt callers to write `new EvaluationContext(null, null)`. Two factories say exactly what the situation is: a known user, or an anonymous visitor. Nulls are stored in one private constructor and never reach a caller. The accessors return `Optional`, so the caller must decide what "unknown" means at the point of use.

**3. Unknown flags return `false`.** This is the define-errors-out-of-existence move from APOSD ch. 10. A caller who asks about a flag that is not in the configuration does not have to handle an exception: it gets the answer "off", which is the safe answer for a feature. It is safe here because `false` always selects the old, proven code path: whether the flag was removed, not yet added, or misspelled, the worst case is that a new feature stays hidden.
It would not be safe when the default answer must not be silent. For a flag that guards a payment path or a data migration, a typo in the name would quietly send everybody down the old path with no alarm. For such flags, the caller should check `flagNames()` at start-up, or the team should test that the flag names exist. The alternative, throwing `FlagNotFoundException`, pushes a try/catch into every caller, which is exactly the complexity this module is meant to hide.

**4. The parser keeps the line number, and only one helper creates errors.** Every error is built by `error(detail)`, which reads the current line number from the parser state. That makes it impossible to report an error with the wrong number, and it means the parser does not need to pass a line number around. We stop at the first error, because a person fixing a file usually fixes one problem at a time, and a clear first error is easier to act on than a list that might cascade. Collecting all errors is a fine stretch goal, but it costs more code.

**5. Split by keyword, not one big regex.** Each line is split into a keyword and an argument. A `switch` on the keyword handles each line type. This gives one readable branch per keyword, and an unknown keyword is a single `default` branch. A single regex would be shorter and would give terrible messages: "no match at line 7" tells the reader nothing.

**6. Bucket by flag name and user id together, with `floorMod`.** Hashing only the user id would make every rollout pick the same users first, so a feature at 25 percent would always be the same quarter of the customer base as every other feature. Hashing the flag name decorrelates them. `Math.floorMod` avoids the negative bucket that `%` would produce for a negative hash. The rejected alternative was `Random`: users would flip between variants on every page load. Another rejected alternative was `Objects.hash(flagName, userId)`. It works, but it hides the formula behind a helper whose details the reader must look up, and the explicit string makes the contract visible.

**7. Anonymous visitors never match a percentage.** An anonymous visitor has no id, so there is nothing stable to hash. Giving them a random bucket on each request would flip them in and out of the feature between two page loads, which is worse than leaving them out. The right fix is to give anonymous visitors a stable id (for example a cookie) and pass it as a user. That is a decision for the caller, and the module keeps its promise of stable answers.

**8. Sealed `Rule` with a method on each record, and only the rule that needs the flag name gets it.** Each rule record implements `matches(context)`. The behaviour is next to the data it uses. Only `PercentRule` needs the flag name, because its bucket depends on it, so only `PercentRule` carries the name. The rejected alternative was to pass the flag name to every rule. `UserRule` and `CountryRule` would then receive a value they never use, and every future rule would inherit that parameter. That is information leakage in the APOSD sense (ch. 5): a part of the module knows a detail it does not need. `sealed` does not add much at runtime today: the methods do the work. It does mean that a future `switch` over rule kinds, for example an `explain` method, is checked by the compiler when a new kind is added. The alternative, one large `switch` in the evaluator, would have worked too. It would have kept the records as plain data, which is a fair choice, and we would move to it if the rules started to need different context.

**9. `FeatureFlags` is immutable and shared.** `parse` builds the whole map and never changes it. `flagNames` returns an unmodifiable view of the names, in the order they are written in the configuration. Reads need no locks and every request thread can share one instance. This is *Effective Java* Item 15 in practice. Reloads create a new instance (see stretch goal 1) instead of mutating the one in use.

### Code

<!-- embed: solutions/src/main/java/io/github/chris013/practice/task14/FeatureFlags.java -->
**[`FeatureFlags.java`](../solutions/src/main/java/io/github/chris013/practice/task14/FeatureFlags.java)**

```java
package io.github.chris013.practice.task14;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Answers one question for the whole shop: is this flag on for this user?
 *
 * <p>Callers never see rules, parsers or hashing. They parse a configuration once, keep the instance, and ask
 * {@link #isEnabled}. The instance is immutable, so it can be shared between request threads.
 *
 * <p>Task 14: read {@code tasks/14-feature-flags-deep-module.md} first.
 */
public final class FeatureFlags {

    private final Map<String, Flag> flags;

    private FeatureFlags(Map<String, Flag> flags) {
        this.flags = flags;
    }

    /**
     * Parses a configuration text in the line-based format described in the task.
     *
     * @throws FlagConfigurationException if the text is invalid; the message starts with {@code line <n>: }
     */
    public static FeatureFlags parse(String configuration) {
        Objects.requireNonNull(configuration, "configuration");
        return new FeatureFlags(FlagConfigParser.parse(configuration));
    }

    /**
     * Returns whether the flag is on for this context. An unknown flag name returns {@code false}.
     *
     * <p>That is a deliberate choice: a flag that nobody has written down is, for every practical purpose, a flag
     * that is off. Callers never need a try/catch around a flag check. The trade-off is that a typo in a flag name
     * fails silently, so tests should cover the flag names they depend on.
     */
    public boolean isEnabled(String flagName, EvaluationContext context) {
        Objects.requireNonNull(flagName, "flagName");
        Objects.requireNonNull(context, "context");
        Flag flag = flags.get(flagName);
        return flag != null && flag.isOnFor(context);
    }

    /**
     * The names of all flags in the configuration, in the order they are written. The set cannot be modified.
     */
    public Set<String> flagNames() {
        return Collections.unmodifiableSet(flags.keySet());
    }
}
```
<!-- /embed -->

<!-- embed: solutions/src/main/java/io/github/chris013/practice/task14/EvaluationContext.java -->
**[`EvaluationContext.java`](../solutions/src/main/java/io/github/chris013/practice/task14/EvaluationContext.java)**

```java
package io.github.chris013.practice.task14;

import java.util.Objects;
import java.util.Optional;

/**
 * Who is asking: a known user, possibly with a country, or an anonymous visitor with a country.
 *
 * <p>This is a final class and not a record on purpose: a record would expose a canonical constructor that takes
 * nullable values or {@code Optional}s, and callers would have two ways to say the same thing. Two named factories
 * say exactly what the situation is. The private constructor is the only place where nulls are stored.
 */
public final class EvaluationContext {

    private final String userId;
    private final String country;

    private EvaluationContext(String userId, String country) {
        this.userId = userId;
        this.country = country;
    }

    /** A known user. The user id must not be blank; the country may be {@code null} when it is unknown. */
    public static EvaluationContext user(String userId, String country) {
        Objects.requireNonNull(userId, "userId");
        if (userId.isBlank()) {
            throw new IllegalArgumentException("userId must not be blank");
        }
        return new EvaluationContext(userId, country);
    }

    /** An anonymous visitor. The country may be {@code null} when it is unknown. */
    public static EvaluationContext anonymous(String country) {
        return new EvaluationContext(null, country);
    }

    /** The user id, or empty for an anonymous visitor. */
    public Optional<String> userId() {
        return Optional.ofNullable(userId);
    }

    /** The ISO 3166-1 alpha-2 country code, or empty when unknown. */
    public Optional<String> country() {
        return Optional.ofNullable(country);
    }
}
```
<!-- /embed -->

<!-- embed: solutions/src/main/java/io/github/chris013/practice/task14/FlagConfigParser.java -->
**[`FlagConfigParser.java`](../solutions/src/main/java/io/github/chris013/practice/task14/FlagConfigParser.java)**

```java
package io.github.chris013.practice.task14;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Turns configuration text into {@link Flag}s. This is the only code that knows the file format, and the only code
 * that knows line numbers, so every error is reported here, next to the line that caused it.
 *
 * <p>The parser works line by line. A non-indented line starts a flag; an indented line is a rule of the current
 * flag. Each line is split into a keyword and the rest, and the keyword decides what the line means. That is easier
 * to read and to get right than one regular expression for the whole grammar.
 */
final class FlagConfigParser {

    private static final Pattern FLAG_NAME = Pattern.compile("[a-z0-9]+(-[a-z0-9]+)*");
    private static final Pattern COUNTRY_CODE = Pattern.compile("[A-Z]{2}");

    private final Map<String, FlagDraft> drafts = new LinkedHashMap<>();
    private FlagDraft current;
    private int lineNumber;

    private FlagConfigParser() {
    }

    static Map<String, Flag> parse(String configuration) {
        FlagConfigParser parser = new FlagConfigParser();
        // \R matches \n, \r\n and the other line terminators; -1 keeps trailing empty lines.
        String[] lines = configuration.split("\\R", -1);
        for (int i = 0; i < lines.length; i++) {
            parser.lineNumber = i + 1;
            parser.parseLine(lines[i]);
        }
        Map<String, Flag> flags = new LinkedHashMap<>();
        for (FlagDraft draft : parser.drafts.values()) {
            flags.put(draft.name, draft.build());
        }
        return Collections.unmodifiableMap(flags);
    }

    private void parseLine(String line) {
        String text = line.strip();
        if (text.isEmpty() || text.startsWith("#")) {
            return;
        }
        if (line.startsWith(" ") || line.startsWith("\t")) {
            parseRule(text);
        } else {
            parseFlagHeader(text);
        }
    }

    private void parseFlagHeader(String text) {
        Words words = Words.of(text);
        if (!words.keyword().equals("flag")) {
            throw error("expected 'flag <name>' at the start of a line but found '" + words.keyword() + "'");
        }
        String name = words.argument();
        if (name.isEmpty()) {
            throw error("flag needs a name");
        }
        if (!FLAG_NAME.matcher(name).matches()) {
            throw error("invalid flag name '" + name + "'");
        }
        if (drafts.containsKey(name)) {
            throw error("duplicate flag '" + name + "'");
        }
        current = new FlagDraft(name);
        drafts.put(name, current);
    }

    private void parseRule(String text) {
        Words words = Words.of(text);
        if (current == null) {
            throw error("rule '" + words.keyword() + "' appears before any flag");
        }
        String argument = words.argument();
        switch (words.keyword()) {
            case "users" -> current.rules.add(new UserRule(listOf("users", argument)));
            case "countries" -> current.rules.add(new CountryRule(countryCodes(argument)));
            case "percent" -> current.rules.add(new PercentRule(current.name, percent(argument)));
            case "default" -> setDefault(argument);
            case "off" -> killSwitch(argument);
            case "flag" -> throw error("flag lines must not be indented");
            default -> throw error("unknown keyword '" + words.keyword() + "'");
        }
    }

    private void setDefault(String argument) {
        if (current.defaultSet) {
            throw error("default is already set for flag '" + current.name + "'");
        }
        current.defaultSet = true;
        current.defaultOn = switch (argument) {
            case "on" -> true;
            case "off" -> false;
            default -> throw error("default must be on or off but was '" + argument + "'");
        };
    }

    private void killSwitch(String argument) {
        if (!argument.isEmpty()) {
            throw error("'off' takes no value but found '" + argument + "'");
        }
        current.killSwitch = true;
    }

    private List<String> countryCodes(String argument) {
        List<String> codes = listOf("countries", argument);
        for (String code : codes) {
            if (!COUNTRY_CODE.matcher(code).matches()) {
                throw error("country must be two uppercase letters but was '" + code + "'");
            }
        }
        return codes;
    }

    private int percent(String argument) {
        if (argument.isEmpty()) {
            throw error("percent needs a number");
        }
        int value;
        try {
            value = Integer.parseInt(argument);
        } catch (NumberFormatException e) {
            throw error("percent must be a whole number but was '" + argument + "'");
        }
        if (value < 0 || value > 100) {
            throw error("percent must be between 0 and 100 but was " + value);
        }
        return value;
    }

    /** Splits a comma-separated argument. Every entry must be present; "a,,b" is an error, not a silent skip. */
    private List<String> listOf(String keyword, String argument) {
        if (argument.isEmpty()) {
            throw error(keyword + " needs at least one value");
        }
        List<String> values = new ArrayList<>();
        for (String raw : argument.split(",", -1)) {
            String value = raw.strip();
            if (value.isEmpty()) {
                throw error("empty entry in " + keyword + " list");
            }
            values.add(value);
        }
        return values;
    }

    private FlagConfigurationException error(String detail) {
        return new FlagConfigurationException(lineNumber, detail);
    }

    /** The first word of a line and the rest of it, trimmed. */
    private record Words(String keyword, String argument) {

        static Words of(String text) {
            String[] parts = text.split("\\s+", 2);
            return new Words(parts[0], parts.length == 2 ? parts[1].strip() : "");
        }
    }

    /** A flag while its lines are still being read. Turned into an immutable {@link Flag} at the end. */
    private static final class FlagDraft {

        private final String name;
        private final List<Rule> rules = new ArrayList<>();
        private boolean killSwitch;
        private boolean defaultSet;
        private boolean defaultOn;

        FlagDraft(String name) {
            this.name = name;
        }

        Flag build() {
            return new Flag(killSwitch, defaultOn, rules);
        }
    }
}
```
<!-- /embed -->

<!-- embed: solutions/src/main/java/io/github/chris013/practice/task14/Flag.java -->
**[`Flag.java`](../solutions/src/main/java/io/github/chris013/practice/task14/Flag.java)**

```java
package io.github.chris013.practice.task14;

import java.util.List;

/**
 * One parsed flag: a kill switch, a default, and rules in the order they were written.
 *
 * <p>The kill switch is checked first, so it wins over everything else, wherever it appears in the block. Then the
 * rules are tried top to bottom; the first one that matches turns the flag on. Only if none matches does the
 * default apply.
 */
record Flag(boolean killSwitch, boolean defaultOn, List<Rule> rules) {

    Flag {
        rules = List.copyOf(rules);
    }

    boolean isOnFor(EvaluationContext context) {
        if (killSwitch) {
            return false;
        }
        for (Rule rule : rules) {
            if (rule.matches(context)) {
                return true;
            }
        }
        return defaultOn;
    }
}
```
<!-- /embed -->

<!-- embed: solutions/src/main/java/io/github/chris013/practice/task14/Rule.java -->
**[`Rule.java`](../solutions/src/main/java/io/github/chris013/practice/task14/Rule.java)**

```java
package io.github.chris013.practice.task14;

/**
 * One condition that turns a flag on. The set of rule kinds is closed and known at compile time, so this is a sealed
 * interface: a {@code switch} over it can be checked by the compiler, and nobody outside the package can add a kind.
 */
sealed interface Rule permits UserRule, CountryRule, PercentRule {

    /** Whether this rule turns the flag on for the context. */
    boolean matches(EvaluationContext context);
}
```
<!-- /embed -->

<!-- embed: solutions/src/main/java/io/github/chris013/practice/task14/PercentRule.java -->
**[`PercentRule.java`](../solutions/src/main/java/io/github/chris013/practice/task14/PercentRule.java)**

```java
package io.github.chris013.practice.task14;

import java.util.Objects;

/**
 * {@code percent 25}: on for the users whose bucket is below 25.
 *
 * <p>A bucket is a number from 0 to 99 that is fixed for one flag and one user. We hash the flag name together with
 * the user id. Without the flag name, the same users would be first in line for every rollout, and a feature at 10
 * percent would always be the same 10 percent of users as every other one. {@link String#hashCode()} is specified by
 * the JLS, so the bucket is the same on every JVM. A random number generator is not an option: users would flip
 * between variants on every page load.
 *
 * <p>Anonymous visitors have no stable identity, so they never match: a rollout by percentage must not flip a visitor
 * in and out of the feature between page loads.
 */
record PercentRule(String flagName, int percent) implements Rule {

    private static final int BUCKETS = 100;

    PercentRule {
        Objects.requireNonNull(flagName, "flagName");
    }

    @Override
    public boolean matches(EvaluationContext context) {
        return context.userId()
                .map(userId -> bucket(flagName, userId) < percent)
                .orElse(false);
    }

    private static int bucket(String flagName, String userId) {
        // floorMod, not %: hashCode() is often negative, and % would then return a negative bucket.
        return Math.floorMod((flagName + ":" + userId).hashCode(), BUCKETS);
    }
}
```
<!-- /embed -->

### What I typically flag in first attempts

| Finding | Why it matters |
|---|---|
| `public` rule classes "for testing" | Every public class is a promise. The tests should go through `FeatureFlags`, and the rules can then change freely. |
| `Random` for percentages | Users flip between variants on every request. The user sees a different checkout after a reload. |
| `hashCode() % 100` | A negative hash gives a negative bucket, so the users with negative hashes are never on. Use `Math.floorMod`. |
| Hashing only the user id | Every rollout selects the same users first. Two features at 25 percent are the same 25 percent of customers. |
| Error messages without a line number, or a line number from the wrong line | The person fixing the file has to search the whole text. An error at the wrong line sends them to the wrong place. |
| One regex for the whole grammar | Unreadable, hard to extend, and it cannot say what is wrong. Split lines by keyword. |
| `isEnabled` throwing for an unknown flag | Every caller now needs a try/catch, and a request that checks a flag that was just removed fails with a 500. |
| A `FeatureFlags` with setters or a `reload` that mutates it | Shared across threads, a half-updated instance can answer a request. Build a new instance and swap it. |
| Tests that reach into `FlagConfigParser` or `Flag` | The tests now break on every refactoring of the internals, which is the cost the deep module was supposed to remove. |

</details>
