package io.github.chris013.practice.task28;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.PriorityQueue;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.stream.Collector;
import java.util.stream.Collectors;

/**
 * Collectors for sensor readings: statistics, top-k, per-sensor summaries and threshold counts.
 *
 * <p>Every collector here keeps its state in its own accumulation container. None of them shares a list or a
 * map between threads, so the same collector gives the same answer sequentially and in parallel.
 */
public final class SensorCollectors {

    private SensorCollectors() {
        throw new AssertionError("No instances");
    }

    /**
     * Count, min, max, mean and population standard deviation in one pass, numerically stable.
     *
     * <p>The accumulator is a {@link RunningStatistics} that uses Welford's online update, not the sums of
     * {@code x} and {@code x*x}. The combiner uses Chan et al.'s merge, so two halves computed in parallel give the
     * same result as one pass over everything. The finisher turns the accumulator into {@code Optional<Statistics>},
     * so the characteristics are {@link Collector.Characteristics#UNORDERED} only:
     * <ul>
     *   <li>{@code IDENTITY_FINISH} is not set because the finisher really does change the type, from
     *       {@code RunningStatistics} to {@code Optional<Statistics>}.</li>
     *   <li>{@code UNORDERED} is fine: the order of the readings does not change the mathematical result. The
     *       floating-point result can differ in the last few bits, which is why the tests compare with a tolerance.</li>
     *   <li>{@code CONCURRENT} is not set: the accumulator is not thread-safe, and each thread gets its own.</li>
     * </ul>
     */
    public static Collector<Reading, ?, Optional<Statistics>> statistics() {
        return Collector.of(
                RunningStatistics::new,
                (running, reading) -> running.add(reading.celsius()),
                RunningStatistics::merge,
                RunningStatistics::toStatistics,
                Collector.Characteristics.UNORDERED);
    }

    /**
     * The {@code k} greatest elements according to {@code order}, greatest first, as an unmodifiable list.
     *
     * <p>A min-heap of at most {@code k} elements: its head is the smallest element we still keep. A new element
     * replaces the head only when it is greater, so memory is O(k) and the time is O(n log k). Sorting all
     * elements would cost O(n log n) time and O(n) memory for the same answer.
     *
     * <p>The parameter is {@code Comparator<? super T>} (PECS: the comparator consumes the elements), so you can pass
     * a {@code Comparator<Object>} to rank Strings.
     */
    public static <T> Collector<T, ?, List<T>> topK(int k, Comparator<? super T> order) {
        if (k < 1) {
            throw new IllegalArgumentException("k must be at least 1, was " + k);
        }
        Objects.requireNonNull(order, "order");
        return Collector.of(
                () -> new BoundedHeap<T>(k, order),
                BoundedHeap::offer,
                BoundedHeap::merge,
                BoundedHeap::greatestFirst,
                Collector.Characteristics.UNORDERED);
    }

    /**
     * Statistics per sensor. The per-sensor collector is an ordinary composition: {@code groupingBy} with a
     * {@code TreeMap} so keys are sorted, {@link #statistics()} as the downstream collector, and
     * {@code collectingAndThen} to take the value out of the {@code Optional} and make the map unmodifiable.
     * A group always has at least one reading, so {@code orElseThrow} cannot fail.
     */
    public static Collector<Reading, ?, SortedMap<String, Statistics>> statisticsPerSensor() {
        Collector<Reading, ?, Statistics> oneSensor =
                Collectors.collectingAndThen(statistics(), Optional::orElseThrow);
        return Collectors.collectingAndThen(
                Collectors.groupingBy(Reading::sensorId, TreeMap::new, oneSensor),
                Collections::unmodifiableSortedMap);
    }

    /**
     * Counts with {@link Collectors#partitioningBy partitioningBy}, which always creates both keys.
     * A reading equal to the threshold is not above it. A NaN threshold would make every comparison false
     * without any error, so it is rejected up front.
     */
    public static Collector<Reading, ?, Map<Boolean, Long>> countAbove(double thresholdCelsius) {
        if (!Double.isFinite(thresholdCelsius)) {
            throw new IllegalArgumentException("threshold must be finite, was " + thresholdCelsius);
        }
        return Collectors.partitioningBy(
                reading -> reading.celsius() > thresholdCelsius,
                Collectors.counting());
    }

    /**
     * Mutable accumulation state for {@link #statistics()}.
     *
     * <p>Welford's update for one new value {@code x}:
     * {@code delta = x - mean; mean += delta / n; m2 += delta * (x - mean)}.
     * Chan's merge for two parts A and B with {@code n = nA + nB}:
     * {@code delta = meanB - meanA; mean = meanA + delta * nB / n; m2 = m2A + m2B + delta^2 * nA * nB / n}.
     */
    private static final class RunningStatistics {

        private long count;
        private double mean;
        /** Sum of squared distances from the mean. Variance is {@code m2 / count}. */
        private double m2;
        private double min = Double.POSITIVE_INFINITY;
        private double max = Double.NEGATIVE_INFINITY;

        void add(double x) {
            count++;
            double delta = x - mean;
            mean += delta / count;
            m2 += delta * (x - mean);
            min = Math.min(min, x);
            max = Math.max(max, x);
        }

        RunningStatistics merge(RunningStatistics other) {
            if (other.count == 0) {
                return this;
            }
            if (count == 0) {
                count = other.count;
                mean = other.mean;
                m2 = other.m2;
                min = other.min;
                max = other.max;
                return this;
            }
            double nA = count;
            double nB = other.count;
            double n = nA + nB;
            double delta = other.mean - mean;
            mean += delta * (nB / n);
            m2 += other.m2 + delta * delta * (nA * nB / n);
            count += other.count;
            min = Math.min(min, other.min);
            max = Math.max(max, other.max);
            return this;
        }

        Optional<Statistics> toStatistics() {
            if (count == 0) {
                return Optional.empty();
            }
            // m2 cannot be negative in exact arithmetic. The clamp only guards against rounding, so sqrt never sees a
            // negative number.
            double variance = Math.max(0.0, m2) / count;
            return Optional.of(new Statistics(count, min, max, mean, Math.sqrt(variance)));
        }
    }

    /**
     * Keeps the {@code capacity} greatest elements seen so far. The heap's head is the least of them, so it is the
     * element a new candidate has to beat.
     */
    private static final class BoundedHeap<T> {

        private final int capacity;
        private final Comparator<? super T> order;
        private final PriorityQueue<T> smallestFirst;

        BoundedHeap(int capacity, Comparator<? super T> order) {
            this.capacity = capacity;
            this.order = order;
            this.smallestFirst = new PriorityQueue<>(order);
        }

        void offer(T element) {
            if (smallestFirst.size() < capacity) {
                smallestFirst.add(element);
            } else if (order.compare(element, smallestFirst.peek()) > 0) {
                smallestFirst.poll();
                smallestFirst.add(element);
            }
        }

        BoundedHeap<T> merge(BoundedHeap<T> other) {
            other.smallestFirst.forEach(this::offer);
            return this;
        }

        List<T> greatestFirst() {
            List<T> sorted = new ArrayList<>(smallestFirst);
            sorted.sort(order.reversed());
            return List.copyOf(sorted);
        }
    }
}
