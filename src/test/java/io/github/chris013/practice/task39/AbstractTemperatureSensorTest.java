package io.github.chris013.practice.task39;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class AbstractTemperatureSensorTest {

    /** A second subclass with the same id as a {@link SimulatedSensor}: equal ids are not enough to be equal. */
    private static final class OtherSensor extends AbstractTemperatureSensor {

        OtherSensor(String id) {
            super(id);
        }

        @Override
        public double readCelsius() {
            return 0.0;
        }
    }

    @Nested
    class Identity {

        @Test
        void two_sensors_of_the_same_class_and_id_are_equal() {
            SimulatedSensor first = new SimulatedSensor("t-1", () -> 20.0);
            SimulatedSensor second = new SimulatedSensor("t-1", () -> 35.0);

            assertThat(first).isEqualTo(second);
            assertThat(first.hashCode()).isEqualTo(second.hashCode());
        }

        @Test
        void sensors_with_different_ids_are_not_equal() {
            assertThat(new SimulatedSensor("t-1", () -> 20.0)).isNotEqualTo(new SimulatedSensor("t-2", () -> 20.0));
        }

        @Test
        void sensors_of_different_classes_are_not_equal_even_with_the_same_id() {
            assertThat(new SimulatedSensor("t-1", () -> 20.0)).isNotEqualTo(new OtherSensor("t-1"));
        }

        @Test
        void a_sensor_is_not_equal_to_null_or_to_a_plain_object() {
            SimulatedSensor sensor = new SimulatedSensor("t-1", () -> 20.0);

            assertThat(sensor).isNotEqualTo(null);
            assertThat(sensor).isNotEqualTo("t-1");
        }
    }

    @Nested
    class Text {

        @Test
        void to_string_shows_the_class_and_the_id() {
            assertThat(new SimulatedSensor("t-1", () -> 20.0)).hasToString("SimulatedSensor[t-1]");
        }
    }

    @Nested
    class Construction {

        @Test
        void rejects_a_blank_id() {
            assertThatThrownBy(() -> new OtherSensor(" ")).isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void rejects_a_null_id() {
            assertThatThrownBy(() -> new OtherSensor(null)).isInstanceOf(NullPointerException.class);
        }

        @Test
        void keeps_the_id_it_was_given() {
            assertThat(new OtherSensor("t-9").id()).isEqualTo("t-9");
        }
    }
}
