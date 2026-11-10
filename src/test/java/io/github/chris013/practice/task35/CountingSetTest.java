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
