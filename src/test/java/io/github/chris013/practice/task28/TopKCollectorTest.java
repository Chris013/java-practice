package io.github.chris013.practice.task28;

import static io.github.chris013.practice.task28.TestReadings.collectInTwoHalves;
import static io.github.chris013.practice.task28.TestReadings.reading;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import java.util.stream.Collector;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class TopKCollectorTest {

    private static final Comparator<String> BY_LENGTH = Comparator.comparingInt(String::length);

    @Test
    void returns_the_k_greatest_elements_greatest_first() {
        List<String> fruits = List.of("pear", "fig", "banana", "kiwi", "apple");

        List<String> result = fruits.stream().collect(SensorCollectors.topK(2, BY_LENGTH));

        assertThat(result).containsExactly("banana", "apple");
    }

    @Test
    void returns_everything_when_k_is_larger_than_the_input() {
        List<String> result = Stream.of("fig", "banana", "pear")
                .collect(SensorCollectors.topK(10, BY_LENGTH));

        assertThat(result).containsExactly("banana", "pear", "fig");
    }

    @Test
    void k_of_one_is_the_maximum() {
        List<String> result = Stream.of("fig", "banana", "pear")
                .collect(SensorCollectors.topK(1, BY_LENGTH));

        assertThat(result).containsExactly("banana");
    }

    @Test
    void empty_input_gives_an_empty_list() {
        List<String> result = Stream.<String>empty().collect(SensorCollectors.topK(3, BY_LENGTH));

        assertThat(result).isEmpty();
    }

    @Test
    void works_for_readings_ordered_by_temperature() {
        List<Reading> readings = List.of(
                reading("a", -1.0), reading("b", 5.0), reading("c", 2.5), reading("d", 9.0), reading("e", 0.0));

        List<Reading> warmest = readings.stream()
                .collect(SensorCollectors.topK(3, Comparator.comparingDouble(Reading::celsius)));

        assertThat(warmest).extracting(Reading::sensorId).containsExactly("d", "b", "c");
    }

    @Test
    void the_result_cannot_be_modified() {
        List<String> result = Stream.of("fig", "banana").collect(SensorCollectors.topK(2, BY_LENGTH));

        assertThatThrownBy(() -> result.add("kiwi")).isInstanceOf(UnsupportedOperationException.class);
    }

    @ParameterizedTest
    @CsvSource({"0", "-1", "-100"})
    void needs_k_of_at_least_one(int k) {
        assertThatThrownBy(() -> SensorCollectors.topK(k, BY_LENGTH))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void the_comparator_must_not_be_null() {
        assertThatThrownBy(() -> SensorCollectors.topK(2, null)).isInstanceOf(NullPointerException.class);
    }

    @Test
    void accepts_a_comparator_for_a_supertype_so_it_can_rank_strings() {
        // PECS: the comparator only consumes elements, so a Comparator<Object> ranks a stream of Strings.
        Comparator<Object> byText = Comparator.comparing(Object::toString);

        List<String> result = Stream.of("pear", "apple", "zucchini").collect(SensorCollectors.topK(2, byText));

        assertThat(result).containsExactly("zucchini", "pear");
    }

    @Nested
    class Parallel {

        @Test
        void parallel_equals_sequential_when_there_are_no_ties() {
            List<Integer> numbers = new ArrayList<>(IntStream.range(0, 100_000).boxed().toList());
            Collections.shuffle(numbers, new Random(42));

            List<Integer> sequential = numbers.stream().collect(SensorCollectors.topK(10, Comparator.naturalOrder()));
            List<Integer> parallel = numbers.parallelStream()
                    .collect(SensorCollectors.topK(10, Comparator.naturalOrder()));

            assertThat(sequential).containsExactly(99_999, 99_998, 99_997, 99_996, 99_995,
                    99_994, 99_993, 99_992, 99_991, 99_990);
            assertThat(parallel).isEqualTo(sequential);
        }

        @Test
        void merging_two_partial_heaps_keeps_only_k_elements() {
            Collector<String, ?, List<String>> collector = SensorCollectors.topK(2, BY_LENGTH);

            List<String> result = collectInTwoHalves(collector, List.of("fig", "banana"),
                    List.of("strawberry", "kiwi"));

            assertThat(result).containsExactly("strawberry", "banana");
        }
    }
}
