# Task 35 · The scan counter that counted twice: composition over inheritance

| Difficulty | Topics | Time box |
|---|---|---|
| 🟢 Easy | Composition over inheritance · the fragile base class problem · self-use · forwarding (wrapper) classes · the `Set` contract | 1 h |

> **Background reading:** *Effective Java* 3rd ed., Item 18 ("Favor composition over inheritance", the `InstrumentedHashSet` example), Item 19 ("Design and document for inheritance or else prohibit it"), Item 64 ("Refer to objects by their interfaces") · *Design Patterns* (GoF), "Decorator" · Javadoc of `java.util.AbstractCollection#addAll`, the `implSpec` section

---

## The situation

Our warehouse handheld scanners send barcodes to the server. Two numbers go on the operations dashboard: **scans received** and **distinct items**.

A developer built the counter as a subclass of `HashSet<String>`. It overrides `add` to count each call, and `addAll` to count the batch. Single scans look right. Batch uploads do not: a batch of 3 barcodes is counted as **6 scans**. The dashboard has shown inflated scan numbers for months, and nobody noticed, because the numbers looked plausible.

The class is [`InheritedScanCounter`](../src/main/java/io/github/chris013/practice/task35/InheritedScanCounter.java). It is kept in the repo as it was, with a Javadoc that says why it is broken. Your job is to build the replacement and explain why the first attempt went wrong.

The bug is not a typo. It comes from a rule that the author did not know: `HashSet` does not implement `addAll` itself. It inherits `AbstractCollection.addAll`, which calls `add` once for each element. So the batch goes through our `addAll` (+3), and then through our `add` three times (+3 more). The superclass's internal calls are an **implementation detail**, and our class depended on them without knowing it.

## The domain

| Term | Meaning |
|---|---|
| **Scan attempt** | One call to add a barcode to the counter, duplicates included. Dashboard: "scans received". |
| **Distinct item** | A barcode that is in the set at least once. Dashboard: "distinct items". This is the set's `size()`. |
| **Batch upload** | A list of barcodes sent in one request. Each element is one scan attempt. |
| **Delegate** | The set that really stores the barcodes. The wrapper forwards calls to it. |

Two class structures are in play. The legacy one inherits from `HashSet`. The replacement wraps a `Set` and holds it as a field.

```mermaid
classDiagram
    class InheritedScanCounter {
        -int attempts
        +addAttempts() int
    }
    class HashSet
    class ForwardingSet~E~ {
        -Set~E~ delegate
        +add(E) boolean
        +addAll(Collection) boolean
    }
    class CountingSet~E~ {
        -int attempts
        +addAttempts() int
    }
    class Set~E~
    InheritedScanCounter --|> HashSet : extends
    CountingSet --|> ForwardingSet : extends
    ForwardingSet ..|> Set : implements
    ForwardingSet o-- Set : delegate
```

Read the first half as **is-a through the superclass**: `InheritedScanCounter` gets every method of `HashSet`, including the ones we did not think about. Read the second half as **has-a**: `CountingSet` is a `Set`, and it holds another `Set` that does the real work. We can change the delegate without touching the counting code.

### What actually happens on a batch upload

```mermaid
sequenceDiagram
    participant Upload as Batch upload
    participant Counter as InheritedScanCounter
    participant Base as AbstractCollection addAll
    Upload->>Counter: addAll of 3 barcodes
    Counter->>Counter: attempts plus 3
    Counter->>Base: super addAll
    loop each barcode
        Base->>Counter: add barcode
        Counter->>Counter: attempts plus 1
    end
    Note over Counter: attempts ends at 6 for 3 scans
```

The key line is `Base->>Counter: add barcode`. The superclass calls **our** `add`, because `this` is our object, and dynamic dispatch picks our override. Nothing in our code says "count the batch here". The double count happens because two methods of ours are both on the path, and one of them is called by the other.

### Self-use: why the subclass is fragile

A method **uses itself** when it calls another method of the same object, and that call goes through dynamic dispatch. `AbstractCollection.addAll` uses `add`. Its Javadoc has an `implSpec` section that says so: it iterates over the collection and adds each element in turn, and it throws `UnsupportedOperationException` unless `add` is overridden. That last sentence tells you `add` is called. It is a promise to subclasses, and it is easy to miss. Our author did not read it, or did not know that the promise was there.

The result is that overriding `add` changes the behaviour of a method we never mentioned. Every subclass that overrides a method has to know which other methods of the superclass call it. Nothing in the code shows that.

### Why "just do not override addAll" is not a fix

A tempting fix is to delete our `addAll` and keep only `add`. Today a batch of 3 then counts 3, which is right. But the count now depends on `HashSet` never changing `addAll` so that it bypasses `add`. If a future JDK or another superclass does that, batch scans are silently counted as 0. The code works by accident, and a reader cannot tell from it why. A correct fix does not depend on what the superclass does internally.

### The SELF problem

Wrapping has a trap of its own. The delegate knows only itself, not the wrapper around it. If the delegate passes `this` to a callback, the callback receives the **inner** set, and any call it makes skips the counter. For example, a set that calls `listener.added(this)` after each change, and a listener that calls `add` on the object it received, adds scans that are never counted. The fix is to pass the wrapper, or to keep callbacks out of the delegate. Watch for this whenever a wrapped object hands out `this`.

### When inheritance is fine

Inheritance is the right tool when all of these hold:

* The subclass really is a more specific kind of the superclass (is-a), so that code written for the superclass also works for the subclass (the Liskov substitution principle).
* The superclass was designed and documented for extension. Its self-use is written down, the way `AbstractCollection` documents `addAll` with `implSpec`. `java.util.AbstractList` is an example of a class built for subclasses.
* The subclass and the superclass change together. Same package, same team, reviewed together.

Our counter fails all three. A counter is not a more specific set, `HashSet` was not designed for counting subclasses, and the dashboard code lives far from the collection code. Composition works here because it does not need any of the three.

## What I want from you

Build the replacement as a **wrapper**. You will write two classes. The Javadoc describes the contract of both. Their bodies throw `UnsupportedOperationException` in the starter.

1. [`ForwardingSet`](../src/main/java/io/github/chris013/practice/task35/ForwardingSet.java)`<E> implements Set<E>`, constructor `(Set<E> delegate)`.
   * A `null` delegate throws `NullPointerException` with the parameter name.
   * It forwards **every** `Set` method to the delegate. That includes `equals`, `hashCode` and `toString`, so a `ForwardingSet` equals a `HashSet` with the same elements, and the other way round.
   * It is not `final`, because it is designed to be extended. Its Javadoc states the rule that makes extension safe: no method calls another overridable method of the same class (no self-use).
2. [`CountingSet`](../src/main/java/io/github/chris013/practice/task35/CountingSet.java)`<E> extends ForwardingSet<E>`, constructor `(Set<E> delegate)`. It is `final`.
   * `add(e)` counts one attempt, whether or not `e` is new, and returns what the delegate returns.
   * `addAll(c)` counts `c.size()` attempts: one per element, duplicates included. An empty collection counts nothing. `addAll(null)` throws `NullPointerException` and changes nothing. The return value is the delegate's `addAll` result.
   * `int addAttempts()` returns the total number of attempted adds since the set was created.
   * `size()` is inherited and counts **distinct** elements.
   * `static <E> CountingSet<E> overHashSet()` returns a counting set over a new, empty `HashSet`. Each call returns a new set.
3. The counting set is a **view** over the delegate, not a copy. Changes made through the wrapper are visible in the delegate, and changes made to the delegate are visible through the wrapper.
4. `addAttempts()` counts adds only. `remove`, `retainAll`, `removeAll` and `clear` do not change it.

## Constraints & non-goals

* No inheritance from a concrete collection class (`HashSet`, `AbstractSet`, `AbstractCollection`) anywhere in the new code. `ForwardingSet` implements the `Set` interface directly.
* No copying. Do not build a new `HashSet` from the input and then count. The wrapper must hold the delegate and forward to it.
* No libraries (no Guava, no Apache Commons). The point is to see the structure, and a library class would hide it.
* No change to `InheritedScanCounter`. It stays as it is so the tests can document the bug.
* No thread safety. The dashboard counter is not shared between threads in this task.

## How I'd approach it

* **Start with `ForwardingSet`.** Write one method per `Set` method, and make each one a one-line call to the delegate. The class is boring on purpose. If you find yourself writing logic in it, stop and ask whether that logic belongs in the subclass.
* **Check each method for self-use before you write it.** For example, `isEmpty()` could return `size() == 0`. That is shorter, but it calls `size()` on `this`. If a subclass overrides `size()`, then `isEmpty()` now runs the subclass code by accident. Ask the delegate instead.
* **Forward every method, including the default ones.** `Set` gets `spliterator()`, `toArray(IntFunction)` and `removeIf` as default methods from `Collection`. The defaults work through `iterator()` or `size()`, so they would also work if you forgot them. Forward them anyway: the delegate can do the job better, and the rule "every method forwards" is simpler to check when there are no exceptions.
* **`CountingSet` only counts.** It overrides `add` and `addAll`, updates a counter, and calls `super`. Ask yourself: does `super.addAll` call `this.add`? If it does, the count doubles. If it does not, the count is right. You can check this by reading the code, but the tests will check it for you.
* **Take the count before or after the delegate runs?** Think about what happens when the delegate throws halfway through a batch. The dashboard wants every scan that reached the server, so count the attempts the caller made. Write the reason in your PR description.
* **`equals` and `hashCode`.** `Set.equals` is specified in terms of the other set's elements, not its class. Forwarding `equals` to the delegate keeps that promise. Be careful that the forwarded call works in both directions: `wrapper.equals(hashSet)` and `hashSet.equals(wrapper)`.
* **`overHashSet()`** is a factory. Its job is to hide the `new HashSet<>()` call from callers who only want a counter.
* Look at [`CountingSetTest`](../src/test/java/io/github/chris013/practice/task35/CountingSetTest.java) and [`ForwardingSetTest`](../src/test/java/io/github/chris013/practice/task35/ForwardingSetTest.java). The forwarding test records every call the wrapper makes to a delegate, so you can see directly which method calls which.

## Tests

Run: `mvn test -Dtest='**/task35/**'`

[`InheritedScanCounterTest`](../src/test/java/io/github/chris013/practice/task35/InheritedScanCounterTest.java) documents the bug and passes on the starter already. [`CountingSetTest`](../src/test/java/io/github/chris013/practice/task35/CountingSetTest.java) and [`ForwardingSetTest`](../src/test/java/io/github/chris013/practice/task35/ForwardingSetTest.java) describe the new behaviour and fail until you implement it.

<details>
<summary>Show the tests</summary>

<!-- embed: src/test/java/io/github/chris013/practice/task35/CountingSetTest.java -->
**[`CountingSetTest.java`](../src/test/java/io/github/chris013/practice/task35/CountingSetTest.java)**

```java
package io.github.chris013.practice.task35;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Supplier;
import java.util.stream.Stream;
import org.junit.jupiter.api.Named;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;

class CountingSetTest {

    @Nested
    class Counting {

        @Test
        void add_all_of_null_throws_and_counts_nothing() {
            CountingSet<String> set = CountingSet.overHashSet();

            assertThatThrownBy(() -> set.addAll(null)).isInstanceOf(NullPointerException.class);

            assertThat(set.addAttempts()).isZero();
        }

        @Test
        void add_counts_one_attempt_and_one_item() {
            CountingSet<String> set = CountingSet.overHashSet();

            boolean added = set.add("4006381333931");

            assertThat(added).isTrue();
            assertThat(set.addAttempts()).isEqualTo(1);
            assertThat(set.size()).isEqualTo(1);
        }

        @Test
        void a_new_hash_set_starts_at_zero() {
            assertThat(CountingSet.overHashSet().addAttempts()).isZero();
            assertThat(CountingSet.overHashSet().size()).isZero();
        }

        @Test
        void a_duplicate_add_counts_as_an_attempt_but_not_as_an_item() {
            CountingSet<String> set = CountingSet.overHashSet();
            set.add("A");

            boolean added = set.add("A");

            assertThat(added).isFalse();
            assertThat(set.addAttempts()).isEqualTo(2);
            assertThat(set.size()).isEqualTo(1);
        }

        @ParameterizedTest(name = "add of [{0}] gives {1} attempts and {2} items")
        @CsvSource({
            "'A',           1, 1",
            "'A A',         2, 1",
            "'A B C',       3, 3",
            "'A A B',       3, 2",
            "'A B A C B',   5, 3"
        })
        void add_counts_every_attempt_but_only_distinct_items(String barcodes, int attempts, int items) {
            CountingSet<String> set = CountingSet.overHashSet();

            for (String barcode : barcodes.split(" ")) {
                set.add(barcode);
            }

            assertThat(set.addAttempts()).isEqualTo(attempts);
            assertThat(set.size()).isEqualTo(items);
        }

        @Test
        void add_all_counts_each_element_once() {
            CountingSet<String> set = CountingSet.overHashSet();

            set.addAll(List.of("A", "B", "C"));

            assertThat(set.addAttempts()).isEqualTo(3);
            assertThat(set.size()).isEqualTo(3);
        }

        @ParameterizedTest(name = "addAll of [{0}] gives {1} attempts and {2} items")
        @CsvSource({
            "'A',           1, 1",
            "'A A',         2, 1",
            "'A B C',       3, 3",
            "'A A B',       3, 2",
            "'A B A C B',   5, 3"
        })
        void add_all_counts_every_element_including_duplicates_in_the_batch(
                String barcodes, int attempts, int items) {
            CountingSet<String> set = CountingSet.overHashSet();

            set.addAll(List.of(barcodes.split(" ")));

            assertThat(set.addAttempts()).isEqualTo(attempts);
            assertThat(set.size()).isEqualTo(items);
        }

        @Test
        void an_empty_add_all_counts_nothing_and_changes_nothing() {
            CountingSet<String> set = CountingSet.overHashSet();

            boolean changed = set.addAll(List.of());

            assertThat(changed).isFalse();
            assertThat(set.addAttempts()).isZero();
            assertThat(set).isEmpty();
        }

        @Test
        void add_all_reports_whether_anything_new_was_added() {
            CountingSet<String> set = CountingSet.overHashSet();
            set.add("A");

            assertThat(set.addAll(List.of("A"))).isFalse();
            assertThat(set.addAll(List.of("A", "B"))).isTrue();
            assertThat(set.addAttempts()).isEqualTo(4);
        }

        @Test
        void single_adds_and_batches_add_up() {
            CountingSet<String> set = CountingSet.overHashSet();

            set.add("A");
            set.addAll(List.of("B", "C"));
            set.add("A");

            assertThat(set.addAttempts()).isEqualTo(4);
            assertThat(set.size()).isEqualTo(3);
        }

        @Test
        void retain_all_and_remove_do_not_count_as_attempts() {
            CountingSet<String> set = CountingSet.overHashSet();
            set.addAll(List.of("A", "B", "C"));

            set.retainAll(List.of("B", "C", "D"));
            set.remove("B");

            assertThat(set.addAttempts()).isEqualTo(3);
            assertThat(set).containsExactly("C");
        }

        @Test
        void remove_all_and_clear_do_not_count_as_attempts() {
            CountingSet<String> set = CountingSet.overHashSet();
            set.addAll(List.of("A", "B", "C", "D"));

            set.removeAll(List.of("A"));
            set.clear();

            assertThat(set.addAttempts()).isEqualTo(4);
            assertThat(set).isEmpty();
        }
    }

    @Nested
    class WorksOverAnySet {

        static Stream<Named<Supplier<Set<String>>>> backingSets() {
            return Stream.of(
                    Named.<Supplier<Set<String>>>of("HashSet", HashSet::new),
                    Named.<Supplier<Set<String>>>of("TreeSet", TreeSet::new),
                    Named.<Supplier<Set<String>>>of("LinkedHashSet", LinkedHashSet::new));
        }

        @ParameterizedTest(name = "over {0}")
        @MethodSource("backingSets")
        void counts_attempts_and_items_over_any_set(Supplier<Set<String>> backing) {
            CountingSet<String> set = new CountingSet<>(backing.get());

            set.add("A");
            set.addAll(List.of("B", "C", "A"));

            assertThat(set.addAttempts()).isEqualTo(4);
            assertThat(set.size()).isEqualTo(3);
        }

        @Test
        void tree_set_iteration_stays_sorted() {
            CountingSet<String> set = new CountingSet<>(new TreeSet<>());

            set.addAll(List.of("C", "A", "B"));

            assertThat(set).containsExactly("A", "B", "C");
        }

        @Test
        void linked_hash_set_keeps_insertion_order() {
            CountingSet<String> set = new CountingSet<>(new LinkedHashSet<>());

            set.addAll(List.of("C", "A", "B"));

            assertThat(set).containsExactly("C", "A", "B");
        }
    }

    @Nested
    class SetContract {

        @Test
        void equals_a_hash_set_with_the_same_elements_in_both_directions() {
            CountingSet<String> set = CountingSet.overHashSet();
            set.addAll(List.of("A", "B"));
            Set<String> plain = new HashSet<>(List.of("B", "A"));

            assertThat(set).isEqualTo(plain);
            assertThat(plain).isEqualTo(set);
            assertThat(set.hashCode()).isEqualTo(plain.hashCode());
        }

        @Test
        void a_set_with_different_elements_is_not_equal() {
            CountingSet<String> set = CountingSet.overHashSet();
            set.add("A");

            assertThat(set).isNotEqualTo(new HashSet<>(List.of("B")));
        }

        @Test
        void to_string_is_the_same_as_the_delegates() {
            Set<String> delegate = new LinkedHashSet<>(List.of("A", "B"));
            CountingSet<String> set = new CountingSet<>(delegate);

            assertThat(set.toString()).isEqualTo("[A, B]").isEqualTo(delegate.toString());
        }

        @Test
        void remove_and_contains_work() {
            CountingSet<String> set = CountingSet.overHashSet();
            set.add("A");

            assertThat(set.contains("A")).isTrue();
            assertThat(set.remove("A")).isTrue();
            assertThat(set.contains("A")).isFalse();
            assertThat(set.remove("A")).isFalse();
        }

        @Test
        void retain_all_keeps_only_the_common_elements() {
            CountingSet<String> set = CountingSet.overHashSet();
            set.addAll(List.of("A", "B", "C"));

            boolean changed = set.retainAll(List.of("B", "C", "D"));

            assertThat(changed).isTrue();
            assertThat(set).containsExactlyInAnyOrder("B", "C");
        }

        @Test
        void iterator_remove_works() {
            CountingSet<String> set = CountingSet.overHashSet();
            set.addAll(List.of("A", "B", "C"));

            var iterator = set.iterator();
            while (iterator.hasNext()) {
                if (iterator.next().equals("B")) {
                    iterator.remove();
                }
            }

            assertThat(set).containsExactlyInAnyOrder("A", "C");
            assertThat(set.size()).isEqualTo(2);
        }
    }

    @Nested
    class Delegation {

        @Test
        void changes_through_the_wrapper_show_up_in_the_delegate() {
            Set<String> delegate = new HashSet<>();
            CountingSet<String> set = new CountingSet<>(delegate);

            set.add("A");

            assertThat(delegate).containsExactly("A");
        }

        @Test
        void changes_in_the_delegate_show_up_through_the_wrapper() {
            Set<String> delegate = new HashSet<>();
            CountingSet<String> set = new CountingSet<>(delegate);

            delegate.add("B");

            assertThat(set).containsExactly("B");
            assertThat(set.size()).isEqualTo(1);
        }

        @Test
        void the_wrapper_is_a_view_not_a_copy() {
            Set<String> delegate = new HashSet<>(List.of("A", "B"));
            CountingSet<String> set = new CountingSet<>(delegate);

            delegate.clear();

            assertThat(set).isEmpty();
        }

        @Test
        void rejects_a_null_delegate() {
            assertThatThrownBy(() -> new CountingSet<String>(null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("delegate");
        }
    }
}
```
<!-- /embed -->

<!-- embed: src/test/java/io/github/chris013/practice/task35/ForwardingSetTest.java -->
**[`ForwardingSetTest.java`](../src/test/java/io/github/chris013/practice/task35/ForwardingSetTest.java)**

```java
package io.github.chris013.practice.task35;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class ForwardingSetTest {

    @Test
    void rejects_a_null_delegate() {
        assertThatThrownBy(() -> new ForwardingSet<String>(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("delegate");
    }

    @Nested
    class Forwarding {

        static Stream<Arguments> set_methods() {
            return Stream.of(
                    Arguments.of("size()", (Consumer<Set<String>>) s -> s.size()),
                    Arguments.of("isEmpty()", (Consumer<Set<String>>) s -> s.isEmpty()),
                    Arguments.of("contains(Object)", (Consumer<Set<String>>) s -> s.contains("A")),
                    Arguments.of("iterator()", (Consumer<Set<String>>) s -> s.iterator()),
                    Arguments.of("toArray()", (Consumer<Set<String>>) s -> s.toArray()),
                    Arguments.of("toArray(Object[])", (Consumer<Set<String>>) s -> s.toArray(new String[0])),
                    Arguments.of("toArray(IntFunction)", (Consumer<Set<String>>) s -> s.toArray(String[]::new)),
                    Arguments.of("add(Object)", (Consumer<Set<String>>) s -> s.add("C")),
                    Arguments.of("remove(Object)", (Consumer<Set<String>>) s -> s.remove("A")),
                    Arguments.of("containsAll(Collection)", (Consumer<Set<String>>) s -> s.containsAll(List.of("A"))),
                    Arguments.of("addAll(Collection)", (Consumer<Set<String>>) s -> s.addAll(List.of("C"))),
                    Arguments.of("retainAll(Collection)", (Consumer<Set<String>>) s -> s.retainAll(List.of("A"))),
                    Arguments.of("removeAll(Collection)", (Consumer<Set<String>>) s -> s.removeAll(List.of("A"))),
                    Arguments.of("removeIf(Predicate)", (Consumer<Set<String>>) s -> s.removeIf("A"::equals)),
                    Arguments.of("clear()", (Consumer<Set<String>>) s -> s.clear()),
                    Arguments.of("forEach(Consumer)", (Consumer<Set<String>>) s -> s.forEach(x -> { })),
                    Arguments.of("spliterator()", (Consumer<Set<String>>) s -> s.spliterator()),
                    Arguments.of("equals(Object)", (Consumer<Set<String>>) s -> s.equals(Set.of())),
                    Arguments.of("hashCode()", (Consumer<Set<String>>) s -> s.hashCode()),
                    Arguments.of("toString()", (Consumer<Set<String>>) s -> s.toString()));
        }

        @ParameterizedTest(name = "forwards {0}")
        @MethodSource("set_methods")
        void forwards_every_set_method_to_the_delegate(String method, Consumer<Set<String>> call) {
            List<String> delegateCalls = new ArrayList<>();
            Set<String> wrapper = new ForwardingSet<>(recording(new LinkedHashSet<>(List.of("A", "B")), delegateCalls));

            call.accept(wrapper);

            assertThat(delegateCalls).contains(method);
        }

        @Test
        void returns_what_the_delegate_returns() {
            Set<String> wrapper = new ForwardingSet<>(new HashSet<>(List.of("A", "B")));

            assertThat(wrapper.size()).isEqualTo(2);
            assertThat(wrapper.contains("A")).isTrue();
            assertThat(wrapper.add("A")).isFalse();
            assertThat(wrapper.add("C")).isTrue();
            assertThat(wrapper.removeAll(List.of("C", "Z"))).isTrue();
            assertThat(wrapper).containsExactlyInAnyOrder("A", "B");
        }

        @Test
        void a_wrapper_equals_and_hashes_like_the_delegate() {
            Set<String> delegate = new HashSet<>(List.of("A", "B"));
            Set<String> wrapper = new ForwardingSet<>(delegate);

            assertThat(wrapper).isEqualTo(delegate);
            assertThat(delegate).isEqualTo(wrapper);
            assertThat(wrapper.hashCode()).isEqualTo(delegate.hashCode());
            assertThat(wrapper.toString()).isEqualTo(delegate.toString());
        }
    }

    @Nested
    class SelfUse {

        /** Counts calls to {@code add} made on the wrapper itself. */
        private static final class AddCountingSet extends ForwardingSet<String> {

            int adds;

            AddCountingSet(Set<String> delegate) {
                super(delegate);
            }

            @Override
            public boolean add(String element) {
                adds++;
                return super.add(element);
            }
        }

        @Test
        void add_all_does_not_call_the_overridden_add_of_the_wrapper() {
            AddCountingSet wrapper = new AddCountingSet(new HashSet<>());

            wrapper.addAll(List.of("A", "B"));

            assertThat(wrapper.adds).isZero();
            assertThat(wrapper).containsExactlyInAnyOrder("A", "B");
        }

        @Test
        void is_empty_does_not_call_the_overridden_size_of_the_wrapper() {
            Set<String> wrapper = new ForwardingSet<>(new HashSet<>()) {
                @Override
                public int size() {
                    throw new AssertionError("self-use: isEmpty must not call size");
                }
            };

            assertThat(wrapper.isEmpty()).isTrue();
        }
    }

    /**
     * A Set that behaves like {@code real} and records the signature of every method called on it, such as
     * {@code toArray(IntFunction)}. The proxy exists so the test can see which delegate method gets called,
     * without a hand-written Set of twenty methods.
     */
    @SuppressWarnings("unchecked") // the proxy implements Set, so the cast cannot fail
    private static Set<String> recording(Set<String> real, Collection<String> calls) {
        return (Set<String>) Proxy.newProxyInstance(
                Set.class.getClassLoader(),
                new Class<?>[] {Set.class},
                (proxy, method, args) -> {
                    calls.add(method.getName() + Arrays.stream(method.getParameterTypes())
                            .map(Class::getSimpleName)
                            .collect(Collectors.joining(",", "(", ")")));
                    try {
                        return method.invoke(real, args);
                    } catch (InvocationTargetException e) {
                        throw e.getCause();
                    }
                });
    }
}
```
<!-- /embed -->

</details>

## Definition of done

- [ ] `ForwardingSet` forwards every `Set` method, including `equals`, `hashCode` and `toString`
- [ ] `ForwardingSet` rejects a `null` delegate with a `NullPointerException` that names `delegate`
- [ ] `ForwardingSet`'s Javadoc states the no-self-use rule
- [ ] `CountingSet` is `final` and extends `ForwardingSet`, not a concrete collection
- [ ] `add` counts every attempt, duplicates included
- [ ] `addAll` counts one attempt per element, and a batch of 3 barcodes gives `addAttempts()` of 3
- [ ] `size()` counts distinct items, and `retainAll`, `removeAll`, `remove` and `clear` do not change `addAttempts()`
- [ ] `overHashSet()` returns a new counting set on each call
- [ ] `mvn test -Dtest='**/task35/**'` passes all tests in `CountingSetTest`, `ForwardingSetTest` and `InheritedScanCounterTest`
- [ ] No compiler warnings with `-Xlint:all`

## Stretch goals

1. Write a `ForwardingMap<K, V>` with the same rule: no self-use, every method forwarded. Note which `Map` default methods (`getOrDefault`, `merge`, `compute`) would call back into your own methods if you did not override them.
2. Read the source of Guava's `com.google.common.collect.ForwardingSet`. It is abstract, and the subclass implements a `delegate()` method instead of holding a field. Write down one practical difference from our class, and one reason Guava chose it.
3. Extend `CountingSet` so that it also counts removals: `removeAttempts()`. Decide whether a `remove` of an item that is not in the set should count, and write a test that pins your decision.
4. Write an unmodifiable view with `Collections.unmodifiableSet` and a `ForwardingSet` that throws on every mutator. Explain why both are the same pattern, and where they differ.

---

## Reference solution

<details>
<summary>Spoiler: open only after you have made a serious attempt</summary>

### Key decisions

1. **`ForwardingSet` forwards every method and calls no other method of its own.** `isEmpty` asks the delegate, not `size()`, and `addAll` gives the batch to the delegate's `addAll`, not to a loop over `add`. The rejected alternative was to write defaults in terms of other methods, as `AbstractSet` does. That is less code, but a subclass that overrides one method changes the behaviour of others it never mentioned. The Javadoc states the rule, because a class that is open for extension must say which of its methods are safe to override. That is why `ForwardingSet` is not `final`: a subclass is allowed, and the rule is what makes it safe.

2. **`CountingSet` is `final`.** Counting is the whole contract. A subclass could change what an attempt means and make the dashboard lie again. The rejected alternative was to make it open for "flexibility". Nothing in this task needs that flexibility, and an open class would need the same self-use documentation that `ForwardingSet` already has.

3. **The count is taken from the argument's size, before the delegate runs.** A batch that stops half way (an exception in the delegate, a full store) still counted every scan that reached the server. The rejected alternative was to count the elements the delegate actually added, which is the return value of `addAll`. That gives a different number, and it is not what the dashboard wants: a duplicate scan is still a scan.

4. **The wrapper holds the delegate and forwards to it. It does not copy the input.** A copy would drift from the delegate as soon as someone else changed it, and it would cost memory. The view semantics are tested in `Delegation`.

5. **`equals`, `hashCode` and `toString` are forwarded.** `Set` defines equality by elements, not by class. A wrapper that inherited `Object.equals` would be unequal to an equal `HashSet`, and the `SetContract` tests would catch that. The rejected alternative was to rely on `AbstractSet`'s implementation, which is only correct if the wrapper implements every method it uses.

### Code

<!-- embed: solutions/src/main/java/io/github/chris013/practice/task35/InheritedScanCounter.java -->
**[`InheritedScanCounter.java`](../solutions/src/main/java/io/github/chris013/practice/task35/InheritedScanCounter.java)**

```java
package io.github.chris013.practice.task35;

import java.io.Serial;
import java.util.Collection;
import java.util.HashSet;

/**
 * <b>Broken. Kept for comparison only, do not use.</b>
 *
 * <p>This class counts scans by overriding {@code add} and {@code addAll} of {@link HashSet}. It
 * double-counts every batch upload. {@link HashSet} does not override {@code addAll}; it inherits
 * {@link java.util.AbstractCollection#addAll}, which calls {@code add} once per element. So
 * {@code addAll(of 3 barcodes)} runs our {@code addAll}, which adds 3, and then 3 more calls to our
 * {@code add}. The result is 6 attempts for 3 scans. The superclass's internal calls are an
 * implementation detail, and we depend on them without knowing it.
 */
public class InheritedScanCounter extends HashSet<String> {

    @Serial
    private static final long serialVersionUID = 1L;

    private int attempts;

    @Override
    public boolean add(String barcode) {
        attempts++;
        return super.add(barcode);
    }

    @Override
    public boolean addAll(Collection<? extends String> barcodes) {
        attempts += barcodes.size();
        return super.addAll(barcodes);
    }

    public int addAttempts() {
        return attempts;
    }
}
```
<!-- /embed -->

<!-- embed: solutions/src/main/java/io/github/chris013/practice/task35/ForwardingSet.java -->
**[`ForwardingSet.java`](../solutions/src/main/java/io/github/chris013/practice/task35/ForwardingSet.java)**

```java
package io.github.chris013.practice.task35;

import java.util.Collection;
import java.util.Iterator;
import java.util.Objects;
import java.util.Set;
import java.util.Spliterator;
import java.util.function.Consumer;
import java.util.function.IntFunction;
import java.util.function.Predicate;

/**
 * A {@link Set} that forwards every call to another set, its delegate.
 *
 * <p>This is the base for wrappers such as {@link CountingSet}. A subclass overrides only the methods it
 * wants to change, and all other calls pass through to the delegate.
 *
 * <p>Task 35: read {@code tasks/35-counting-set-composition.md} first.
 *
 * <p>This class is not final, so it is designed for extension. It documents one rule: <b>no self-use</b>.
 * No method of this class calls another overridable method of this class. For example,
 * {@link #isEmpty()} asks the delegate, not {@code size()}, and {@link #addAll} hands the batch to the
 * delegate's {@code addAll} instead of looping over {@code add}. So an override in a subclass runs exactly
 * when the caller invokes that method, and never as a side effect of another method. Contrast
 * {@link java.util.AbstractCollection#addAll}, which calls {@code add} for each element. That is the bug
 * in {@link InheritedScanCounter}.
 *
 * <p>The wrapper is a view, not a copy. Changes through it show up in the delegate and the other way
 * round. {@code equals}, {@code hashCode} and {@code toString} are forwarded too, so a wrapper equals any
 * set with the same elements, in both directions.
 */
public class ForwardingSet<E> implements Set<E> {

    private final Set<E> delegate;

    /**
     * @param delegate the set that holds the elements; must not be {@code null}
     * @throws NullPointerException if {@code delegate} is {@code null}
     */
    public ForwardingSet(Set<E> delegate) {
        this.delegate = Objects.requireNonNull(delegate, "delegate");
    }

    @Override
    public int size() {
        return delegate.size();
    }

    @Override
    public boolean isEmpty() {
        return delegate.isEmpty();
    }

    @Override
    public boolean contains(Object o) {
        return delegate.contains(o);
    }

    @Override
    public Iterator<E> iterator() {
        return delegate.iterator();
    }

    @Override
    public Object[] toArray() {
        return delegate.toArray();
    }

    @Override
    public <T> T[] toArray(T[] a) {
        return delegate.toArray(a);
    }

    @Override
    public <T> T[] toArray(IntFunction<T[]> generator) {
        return delegate.toArray(generator);
    }

    @Override
    public boolean add(E e) {
        return delegate.add(e);
    }

    @Override
    public boolean remove(Object o) {
        return delegate.remove(o);
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        return delegate.containsAll(c);
    }

    @Override
    public boolean addAll(Collection<? extends E> c) {
        return delegate.addAll(c);
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        return delegate.retainAll(c);
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        return delegate.removeAll(c);
    }

    @Override
    public boolean removeIf(Predicate<? super E> filter) {
        return delegate.removeIf(filter);
    }

    @Override
    public void clear() {
        delegate.clear();
    }

    @Override
    public void forEach(Consumer<? super E> action) {
        delegate.forEach(action);
    }

    @Override
    public Spliterator<E> spliterator() {
        return delegate.spliterator();
    }

    @Override
    public boolean equals(Object o) {
        return delegate.equals(o);
    }

    @Override
    public int hashCode() {
        return delegate.hashCode();
    }

    @Override
    public String toString() {
        return delegate.toString();
    }
}
```
<!-- /embed -->

<!-- embed: solutions/src/main/java/io/github/chris013/practice/task35/CountingSet.java -->
**[`CountingSet.java`](../solutions/src/main/java/io/github/chris013/practice/task35/CountingSet.java)**

```java
package io.github.chris013.practice.task35;

import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * A set of distinct items that also counts every attempted add, duplicates included.
 *
 * <p>{@link #size()} is the number of distinct items, inherited from {@link ForwardingSet}. Use
 * {@link #addAttempts()} for the number of scans received. The counting happens here, in the wrapper, and
 * the delegate is only asked to store the items. Because {@link ForwardingSet#addAll} hands the whole batch
 * to the delegate and never calls {@link #add}, each element of a batch is counted exactly once.
 *
 * <p>Task 35: read {@code tasks/35-counting-set-composition.md} first.
 *
 * <p>The class is final. Counting is the whole contract, and a subclass could change what an "attempt"
 * means and break the dashboard numbers without anyone noticing.
 */
public final class CountingSet<E> extends ForwardingSet<E> {

    private int attempts;

    /**
     * @param delegate the set that stores the items; must not be {@code null}
     * @throws NullPointerException if {@code delegate} is {@code null}
     */
    public CountingSet(Set<E> delegate) {
        super(delegate);
    }

    /** A counting set over a new, empty {@link HashSet}. */
    public static <E> CountingSet<E> overHashSet() {
        return new CountingSet<>(new HashSet<>());
    }

    @Override
    public boolean add(E element) {
        attempts++;
        return super.add(element);
    }

    /**
     * Counts one attempt per element of {@code elements}, duplicates included. The count is taken from
     * the argument's size before the delegate runs, so a batch that fails half way is still counted in full.
     *
     * @throws NullPointerException if {@code elements} is null; nothing is counted then
     */
    @Override
    public boolean addAll(Collection<? extends E> elements) {
        Objects.requireNonNull(elements, "elements");
        attempts += elements.size();
        return super.addAll(elements);
    }

    /** Every attempted add so far, including adds of items that were already in the set. */
    public int addAttempts() {
        return attempts;
    }
}
```
<!-- /embed -->

### What I typically flag in first attempts

| Finding | Why it matters |
|---|---|
| `class CountingSet extends HashSet` (or `AbstractSet`) | You inherit `addAll` from the superclass, and that calls `add`. The batch double-counts again, and the next superclass upgrade can change the count. |
| Extending a concrete collection, even `AbstractSet` | Every method of the base class is now part of your class, and you must know its self-use to be safe. The wrapper needs only the `Set` interface. |
| Overriding only `add` and leaving `addAll` alone | The count is right today by accident. If the superclass ever stops calling `add` from `addAll`, batches are silently counted as 0. |
| `addAll` counted by the return value of `super.addAll` | `addAll` returns a boolean, not a count. Counting by the return value counts only new items and drops every duplicate scan. |
| `isEmpty` as `size() == 0` in the wrapper | It calls `this.size()`. A subclass that overrides `size` now changes `isEmpty` too. Forward to the delegate. |
| `equals` or `hashCode` not forwarded | A wrapper equal to a `HashSet` on one side and unequal on the other breaks the `Set` contract, and it breaks `HashMap` keys. |
| A non-final wrapper with no documented self-use | The next developer overrides one method and gets double counts, and nothing in the code says which calls are safe. |
| Copying the input into a new `HashSet` | Changes through the wrapper do not reach the caller's set, and changes to the caller's set do not reach the counter. The "view" contract breaks silently. |

</details>
