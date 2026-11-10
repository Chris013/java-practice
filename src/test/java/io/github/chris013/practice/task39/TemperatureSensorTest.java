package io.github.chris013.practice.task39;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class TemperatureSensorTest {

    @Nested
    class Fahrenheit {

        @ParameterizedTest
        @CsvSource({"-40, -40", "0, 32", "100, 212", "36.6, 97.88"})
        void converts_celsius_to_fahrenheit(double celsius, double expectedFahrenheit) {
            TemperatureSensor sensor = TemperatureSensor.fixed("t-1", celsius);

            assertThat(sensor.readFahrenheit()).isCloseTo(expectedFahrenheit, within(1e-9));
        }
    }

    @Nested
    class Freezing {

        @ParameterizedTest
        @CsvSource({"0.0, true", "0.1, false", "-0.1, true", "-40, true", "25, false"})
        void treats_zero_and_below_as_freezing(double celsius, boolean expected) {
            assertThat(TemperatureSensor.fixed("t-1", celsius).isFreezing()).isEqualTo(expected);
        }
    }

    @Nested
    class Calibration {

        @Test
        void reads_through_to_the_source_on_every_call() {
            AtomicReference<Double> reading = new AtomicReference<>(20.0);
            TemperatureSensor source = new SimulatedSensor("t-1", reading::get);
            TemperatureSensor calibrated = source.calibrated(1.5);

            assertThat(calibrated.readCelsius()).isCloseTo(21.5, within(1e-9));

            reading.set(22.0);

            assertThat(calibrated.readCelsius()).isCloseTo(23.5, within(1e-9));
        }

        @Test
        void keeps_the_id_of_the_source() {
            assertThat(TemperatureSensor.fixed("t-1", 20.0).calibrated(-2.0).id()).isEqualTo("t-1");
        }

        @Test
        void calibrating_a_calibrated_sensor_adds_the_offsets() {
            TemperatureSensor twice = TemperatureSensor.fixed("t-1", 20.0).calibrated(1.0).calibrated(-0.5);

            assertThat(twice.readCelsius()).isCloseTo(20.5, within(1e-9));
        }

        @Test
        void the_default_methods_see_the_calibrated_reading() {
            TemperatureSensor sensor = TemperatureSensor.fixed("t-1", 0.5).calibrated(-1.0);

            assertThat(sensor.isFreezing()).isTrue();
            assertThat(sensor.readFahrenheit()).isCloseTo(31.1, within(1e-9));
        }

        @Test
        void rejects_a_nan_offset() {
            TemperatureSensor sensor = TemperatureSensor.fixed("t-1", 20.0);

            assertThatThrownBy(() -> sensor.calibrated(Double.NaN)).isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    class Fixed {

        @Test
        void reports_the_given_value_and_id() {
            TemperatureSensor sensor = TemperatureSensor.fixed("t-1", 21.25);

            assertThat(sensor.id()).isEqualTo("t-1");
            assertThat(sensor.readCelsius()).isEqualTo(21.25);
        }

        @ParameterizedTest
        @ValueSource(strings = {"", "   "})
        void rejects_a_blank_id(String id) {
            assertThatThrownBy(() -> TemperatureSensor.fixed(id, 20.0)).isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void rejects_a_null_id() {
            assertThatThrownBy(() -> TemperatureSensor.fixed(null, 20.0)).isInstanceOf(NullPointerException.class);
        }
    }
}
