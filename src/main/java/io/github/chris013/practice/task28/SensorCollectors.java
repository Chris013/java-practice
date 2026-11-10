package io.github.chris013.practice.task28;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.SortedMap;
import java.util.stream.Collector;

/**
 * Collectors for sensor readings: statistics, top-k, per-sensor summaries and threshold counts.
 *
 * <p>Task 28: read {@code tasks/28-custom-collectors.md} first.
 */
public final class SensorCollectors {

    private SensorCollectors() {
        throw new UnsupportedOperationException("TODO: Task 28");
    }

    /**
     * Count, min, max, mean and population standard deviation in one pass, numerically stable.
     * Empty input gives {@link Optional#empty()}. Must give the same result sequentially and in parallel,
     * within floating-point tolerance.
     */
    public static Collector<Reading, ?, Optional<Statistics>> statistics() {
        throw new UnsupportedOperationException("TODO: Task 28");
    }

    /**
     * The {@code k} greatest elements according to {@code order}, greatest first, as an unmodifiable list.
     * Keeps at most {@code k} elements in memory at any time. If there are fewer than {@code k} elements,
     * all of them are returned. Which of several equal elements is kept at the cut-off is unspecified.
     *
     * @throws IllegalArgumentException if {@code k} is less than 1
     */
    public static <T> Collector<T, ?, List<T>> topK(int k, Comparator<? super T> order) {
        throw new UnsupportedOperationException("TODO: Task 28");
    }

    /**
     * Statistics per sensor, keyed by sensor id and sorted by that id. The map is unmodifiable.
     * Empty input gives an empty map. Built by composing {@link #statistics()} with {@code groupingBy}.
     */
    public static Collector<Reading, ?, SortedMap<String, Statistics>> statisticsPerSensor() {
        throw new UnsupportedOperationException("TODO: Task 28");
    }

    /**
     * Counts the readings strictly above {@code thresholdCelsius}. Both keys {@code true} and {@code false}
     * are always present, even when a partition is empty.
     *
     * @throws IllegalArgumentException if the threshold is NaN or infinite
     */
    public static Collector<Reading, ?, Map<Boolean, Long>> countAbove(double thresholdCelsius) {
        throw new UnsupportedOperationException("TODO: Task 28");
    }
}
