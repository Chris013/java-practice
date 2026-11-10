package io.github.chris013.practice.task28;

import static io.github.chris013.practice.task28.TestReadings.assertSameStatistics;
import static io.github.chris013.practice.task28.TestReadings.collectInTwoHalves;
import static io.github.chris013.practice.task28.TestReadings.randomReadings;
import static io.github.chris013.practice.task28.TestReadings.reading;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collector;
import java.util.stream.Stream;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class StatisticsCollectorTest {

    @Test
    void a_single_reading_has_zero_standard_deviation() {
        Statistics stats = statisticsOf(List.of(reading("s1", 21.5))).orElseThrow();

        assertThat(stats.count()).isEqualTo(1);
        assertThat(stats.min()).isEqualTo(21.5);
        assertThat(stats.max()).isEqualTo(21.5);
        assertThat(stats.mean()).isEqualTo(21.5);
        assertThat(stats.standardDeviation()).isZero();
    }

    @Test
    void computes_the_population_standard_deviation_of_a_known_data_set() {
        // Classic textbook data: mean 5, sum of squared deviations 32, population variance 32 / 8 = 4.
        List<Reading> readings = Stream.of(2, 4, 4, 4, 5, 5, 7, 9)
                .map(value -> reading("s1", value))
                .toList();

        Statistics stats = statisticsOf(readings).orElseThrow();

        assertThat(stats.count()).isEqualTo(8);
        assertThat(stats.min()).isEqualTo(2.0);
        assertThat(stats.max()).isEqualTo(9.0);
        assertThat(stats.mean()).isEqualTo(5.0);
        assertThat(stats.standardDeviation()).isEqualTo(2.0);
    }

    @Test
    void empty_input_gives_no_statistics() {
        assertThat(statisticsOf(List.of())).isEmpty();
    }

    @Nested
    class NumericalStability {

        // All values are about 1e9. A double has about 16 significant digits, so the squares are about 1e18 and
        // each one is rounded to a multiple of 128. The variance is 22.5, which is a tiny part of that sum.
        private static final List<Double> NEAR_A_BILLION = List.of(1e9 + 4, 1e9 + 7, 1e9 + 13, 1e9 + 16);

        @Test
        void welford_keeps_the_small_variance_when_the_values_are_large() {
            Statistics stats = statisticsOf(NEAR_A_BILLION.stream().map(v -> reading("s1", v)).toList())
                    .orElseThrow();

            assertThat(stats.mean()).isCloseTo(1e9 + 10, within(1e-6));
            assertThat(stats.standardDeviation()).isCloseTo(Math.sqrt(22.5), within(1e-6));
        }

        @Test
        void the_textbook_formula_loses_the_variance_here() {
            // E[x^2] and E[x]^2 are both about 1e18 and are rounded to multiples of 128. Their difference, which should
            // be 22.5, comes out as -128, so sqrt returns NaN.
            double naive = naiveStandardDeviation(NEAR_A_BILLION);

            assertThat(Double.isNaN(naive) || Math.abs(naive - Math.sqrt(22.5)) > 1.0).isTrue();
        }

        /** The formula most people write first. Kept here only to show that it is wrong. */
        private static double naiveStandardDeviation(List<Double> values) {
            double sum = 0;
            double sumOfSquares = 0;
            for (double value : values) {
                sum += value;
                sumOfSquares += value * value;
            }
            double n = values.size();
            double mean = sum / n;
            return Math.sqrt(sumOfSquares / n - mean * mean);
        }
    }

    @Nested
    class Combiner {

        @Test
        void merging_two_partial_results_gives_the_same_answer_as_one_pass() {
            List<Reading> readings = randomReadings(1_000, 1, "s1");

            Optional<Statistics> merged = collectInTwoHalves(SensorCollectors.statistics(), readings.subList(0, 400),
                    readings.subList(400, readings.size()));

            assertSameStatistics(merged.orElseThrow(), statisticsOf(readings).orElseThrow());
        }

        @Test
        void merging_with_an_empty_half_keeps_the_other_half() {
            List<Reading> readings = randomReadings(50, 2, "s1");
            List<Reading> none = List.of();

            assertSameStatistics(collectInTwoHalves(SensorCollectors.statistics(), readings, none).orElseThrow(),
                    statisticsOf(readings).orElseThrow());
            assertSameStatistics(collectInTwoHalves(SensorCollectors.statistics(), none, readings).orElseThrow(),
                    statisticsOf(readings).orElseThrow());
        }

        @Test
        void merging_two_empty_halves_gives_no_statistics() {
            assertThat(collectInTwoHalves(SensorCollectors.statistics(), List.of(), List.of())).isEmpty();
        }    }

    @Nested
    class Parallel {

        @Test
        void parallel_and_sequential_give_the_same_statistics_for_200000_readings() {
            List<Reading> readings = randomReadings(200_000, 42, "s1", "s2", "s3");

            Statistics sequential = readings.stream().collect(SensorCollectors.statistics()).orElseThrow();
            Statistics parallel = readings.parallelStream().collect(SensorCollectors.statistics()).orElseThrow();

            assertSameStatistics(parallel, sequential);
        }
    }

    private static Optional<Statistics> statisticsOf(List<Reading> readings) {
        return readings.stream().collect(SensorCollectors.statistics());
    }
}
