package io.github.chris013.practice.task28;

import static io.github.chris013.practice.task28.TestReadings.assertSameStatistics;
import static io.github.chris013.practice.task28.TestReadings.randomReadings;
import static io.github.chris013.practice.task28.TestReadings.reading;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class ComposedCollectorsTest {

    @Nested
    class StatisticsPerSensor {

        @Test
        void gives_one_entry_per_sensor_sorted_by_sensor_id() {
            List<Reading> readings = List.of(
                    reading("kitchen", 4.0), reading("dock", -18.0), reading("kitchen", 6.0), reading("dock", -16.0));

            SortedMap<String, Statistics> perSensor = readings.stream()
                    .collect(SensorCollectors.statisticsPerSensor());

            assertThat(perSensor.keySet()).containsExactly("dock", "kitchen");
            assertThat(perSensor.get("dock").count()).isEqualTo(2);
            assertThat(perSensor.get("dock").mean()).isEqualTo(-17.0);
            assertThat(perSensor.get("kitchen").mean()).isEqualTo(5.0);
            assertThat(perSensor.get("kitchen").standardDeviation()).isEqualTo(1.0);
        }

        @Test
        void the_map_cannot_be_modified() {
            SortedMap<String, Statistics> perSensor = List.of(reading("dock", -18.0))
                    .stream()
                    .collect(SensorCollectors.statisticsPerSensor());

            assertThatThrownBy(() -> perSensor.put("new", perSensor.get("dock")))
                    .isInstanceOf(UnsupportedOperationException.class);
        }

        @Test
        void empty_input_gives_an_empty_map() {
            assertThat(List.<Reading>of().stream().collect(SensorCollectors.statisticsPerSensor())).isEmpty();
        }

        @Test
        void parallel_equals_sequential_for_200000_readings() {
            List<Reading> readings = randomReadings(200_000, 7, "dock", "freezer", "kitchen", "office");

            SortedMap<String, Statistics> sequential = readings.stream()
                    .collect(SensorCollectors.statisticsPerSensor());
            SortedMap<String, Statistics> parallel = readings.parallelStream()
                    .collect(SensorCollectors.statisticsPerSensor());

            assertThat(parallel.keySet()).containsExactlyElementsOf(sequential.keySet());
            sequential.forEach((sensor, stats) -> assertSameStatistics(parallel.get(sensor), stats));
        }
    }

    @Nested
    class CountAbove {

        private static final List<Reading> READINGS = List.of(
                reading("dock", -18.0), reading("dock", -2.0), reading("dock", -2.0), reading("dock", 3.0));

        @Test
        void counts_readings_strictly_above_the_threshold() {
            Map<Boolean, Long> counts = READINGS.stream().collect(SensorCollectors.countAbove(-2.0));

            // The two readings at exactly -2.0 are not above -2.0.
            assertThat(counts).containsEntry(true, 1L).containsEntry(false, 3L);
        }

        @Test
        void has_both_keys_when_no_reading_is_above() {
            Map<Boolean, Long> counts = READINGS.stream().collect(SensorCollectors.countAbove(10.0));

            assertThat(counts).containsEntry(true, 0L).containsEntry(false, 4L);
        }

        @Test
        void has_both_keys_when_every_reading_is_above() {
            Map<Boolean, Long> counts = READINGS.stream().collect(SensorCollectors.countAbove(-100.0));

            assertThat(counts).containsEntry(true, 4L).containsEntry(false, 0L);
        }

        @Test
        void has_both_keys_for_empty_input() {
            Map<Boolean, Long> counts = List.<Reading>of().stream().collect(SensorCollectors.countAbove(0.0));

            assertThat(counts).containsEntry(true, 0L).containsEntry(false, 0L);
        }

        @ParameterizedTest
        @CsvSource({"NaN", "Infinity", "-Infinity"})
        void rejects_a_threshold_that_is_not_a_finite_number(double threshold) {
            assertThatThrownBy(() -> SensorCollectors.countAbove(threshold))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void parallel_equals_sequential() {
            List<Reading> readings = randomReadings(200_000, 11, "dock", "kitchen");

            Map<Boolean, Long> sequential = readings.stream().collect(SensorCollectors.countAbove(-18.0));
            Map<Boolean, Long> parallel = readings.parallelStream().collect(SensorCollectors.countAbove(-18.0));

            assertThat(parallel).isEqualTo(sequential);
        }
    }
}
