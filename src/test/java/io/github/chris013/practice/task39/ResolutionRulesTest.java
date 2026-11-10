package io.github.chris013.practice.task39;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class ResolutionRulesTest {

    @Nested
    class LegacyImplementers {

        @Test
        void legacy_thermometer_gets_the_new_defaults_without_any_change() {
            LegacyThermometer thermometer = new LegacyThermometer("t-2", -3.0);

            assertThat(thermometer.readFahrenheit()).isCloseTo(26.6, within(1e-9));
            assertThat(thermometer.isFreezing()).isTrue();
        }

        @Test
        void legacy_thermometer_calibration_follows_its_updates() {
            LegacyThermometer thermometer = new LegacyThermometer("t-2", 10.0);
            TemperatureSensor calibrated = thermometer.calibrated(2.0);

            thermometer.update(-5.0);

            assertThat(calibrated.readCelsius()).isCloseTo(-3.0, within(1e-9));
            assertThat(calibrated.isFreezing()).isTrue();
        }
    }

    @Nested
    class ClassWinsOverDefault {

        @Test
        void the_class_method_replaces_the_interface_default() {
            // 1.1 degrees Fahrenheit round-trips to 1.1000000000000014 through the default formula,
            // so an exact comparison proves that the class method is the one being called.
            ImperialSensor sensor = new ImperialSensor("t-3", 1.1);

            assertThat(sensor.readFahrenheit()).isEqualTo(1.1);
        }

        @Test
        void the_class_still_converts_its_celsius_reading() {
            ImperialSensor sensor = new ImperialSensor("t-3", 212.0);

            assertThat(sensor.readCelsius()).isCloseTo(100.0, within(1e-9));
            assertThat(sensor.isFreezing()).isFalse();
        }
    }

    @Nested
    class ConflictingLabels {

        @Test
        void a_room_sensor_resolves_the_label_conflict_itself() {
            RoomSensor sensor = new RoomSensor("t-7", "Kitchen", TemperatureSensor.fixed("t-7", 19.0));

            assertThat(sensor.label()).isEqualTo("Kitchen (Sensor t-7)");
        }

        @Test
        void a_room_sensor_reads_from_its_source() {
            RoomSensor sensor = new RoomSensor("t-7", "Kitchen", TemperatureSensor.fixed("t-7", -1.0));

            assertThat(sensor.id()).isEqualTo("t-7");
            assertThat(sensor.room()).isEqualTo("Kitchen");
            assertThat(sensor.readCelsius()).isCloseTo(-1.0, within(1e-9));
            assertThat(sensor.isFreezing()).isTrue();
        }
    }
}
