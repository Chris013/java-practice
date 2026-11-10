package io.github.chris013.practice.task28;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.Collector;

/** Builders and assertion helpers shared by the Task 28 tests. */
final class TestReadings {

    static final Instant AT = Instant.parse("2026-03-01T09:00:00Z");

    private TestReadings() {
    }

    static Reading reading(String sensorId, double celsius) {
        return new Reading(sensorId, AT, celsius);
    }

    /** Deterministic pseudo-random readings: cold-storage temperatures around -18 C, spread over the sensors. */
    static List<Reading> randomReadings(int count, long seed, String... sensorIds) {
        Random random = new Random(seed);
        List<Reading> readings = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            String sensor = sensorIds[random.nextInt(sensorIds.length)];
            readings.add(reading(sensor, -18.0 + 4.0 * random.nextGaussian()));
        }
        return readings;
    }

    /**
     * Parallel reductions merge partial results in another order than a sequential pass, so the floating-point
     * results can differ in the last bits. Count, min and max are exact; mean and deviation get a relative
     * tolerance of 1e-9.
     */
    static void assertSameStatistics(Statistics actual, Statistics expected) {
        assertThat(actual.count()).isEqualTo(expected.count());
        assertThat(actual.min()).isEqualTo(expected.min());
        assertThat(actual.max()).isEqualTo(expected.max());
        assertThat(actual.mean()).isCloseTo(expected.mean(), within(Math.abs(expected.mean()) * 1e-9));
        assertThat(actual.standardDeviation())
                .isCloseTo(expected.standardDeviation(), within(Math.abs(expected.standardDeviation()) * 1e-9));
    }

    /** Splits the work the way a parallel stream does: two accumulators, merged with the combiner. */
    static <T, A, R> R collectInTwoHalves(Collector<T, A, R> collector, List<T> left, List<T> right) {
        A leftAccumulator = collector.supplier().get();
        left.forEach(item -> collector.accumulator().accept(leftAccumulator, item));
        A rightAccumulator = collector.supplier().get();
        right.forEach(item -> collector.accumulator().accept(rightAccumulator, item));
        A merged = collector.combiner().apply(leftAccumulator, rightAccumulator);
        return collector.finisher().apply(merged);
    }
}
