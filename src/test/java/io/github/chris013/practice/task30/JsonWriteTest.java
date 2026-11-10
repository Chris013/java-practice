package io.github.chris013.practice.task30;

import static io.github.chris013.practice.task30.JsonFixtures.array;
import static io.github.chris013.practice.task30.JsonFixtures.num;
import static io.github.chris013.practice.task30.JsonFixtures.object;
import static io.github.chris013.practice.task30.JsonFixtures.str;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.stream.Stream;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;

class JsonWriteTest {

    /**
     * Each row is {@code input, expected JSON text}. The JSON text includes the surrounding quotes. Each
     * Java literal is written out in full, so the table shows exactly what goes in and what comes out.
     */
    static Stream<Arguments> escapes() {
        return Stream.of(
                Arguments.of("a\"b", "\"a\\\"b\""),
                Arguments.of("a\\b", "\"a\\\\b\""),
                Arguments.of("\b", "\"\\b\""),
                Arguments.of("\f", "\"\\f\""),
                Arguments.of("\n", "\"\\n\""),
                Arguments.of("\r", "\"\\r\""),
                Arguments.of("\t", "\"\\t\""),
                Arguments.of("\u0000", "\"\\u0000\""),
                Arguments.of("\u001f", "\"\\u001f\""),
                Arguments.of("\u007f", "\"\u007f\""),
                Arguments.of("a/b", "\"a/b\""),
                Arguments.of("é", "\"é\""),
                Arguments.of("€", "\"€\""));
    }

    @Nested
    class Scalars {

        @Test
        void writes_null() {
            assertThat(Json.write(JsonNull.INSTANCE)).isEqualTo("null");
        }

        @Test
        void writes_true_and_false() {
            assertThat(Json.write(JsonBoolean.TRUE)).isEqualTo("true");
            assertThat(Json.write(JsonBoolean.FALSE)).isEqualTo("false");
        }

        @ParameterizedTest
        @CsvSource({
            "0, 0",
            "1, 1",
            "-2.50, -2.50",
            "0.10, 0.10",
            "1E+3, 1000",
            "1E-7, 0.0000001",
            "12345678901234567890.5, 12345678901234567890.5",
        })
        void writes_numbers_as_plain_strings(String input, String expected) {
            assertThat(Json.write(num(input))).isEqualTo(expected);
        }
    }

    @Nested
    class Composites {

        @Test
        void writes_an_empty_array_and_an_empty_object() {
            assertThat(Json.write(array())).isEqualTo("[]");
            assertThat(Json.write(object())).isEqualTo("{}");
        }

        @Test
        void writes_nested_values_without_any_whitespace() {
            var value = object("a", num("1"), "b", array(JsonBoolean.TRUE, JsonNull.INSTANCE));

            assertThat(Json.write(value)).isEqualTo("{\"a\":1,\"b\":[true,null]}");
        }

        @Test
        void keeps_the_members_in_insertion_order() {
            var value = object("zeta", num("1"), "alpha", num("2"));

            assertThat(Json.write(value)).isEqualTo("{\"zeta\":1,\"alpha\":2}");
        }
    }

    @Nested
    class Strings {

        @ParameterizedTest
        @MethodSource("io.github.chris013.practice.task30.JsonWriteTest#escapes")
        void escapes_a_string_as_rfc_8259_requires(String input, String expected) {
            assertThat(Json.write(str(input))).isEqualTo(expected);
        }

        @Test
        void no_control_character_survives_unescaped() {
            for (char c = 0; c <= 0x1F; c++) {
                String written = Json.write(str(String.valueOf(c)));

                assertThat(written.chars().filter(ch -> ch < 0x20)).as("control char %d", (int) c).isEmpty();
            }
        }

        @Test
        void escapes_keys_as_well_as_values() {
            var value = object("say \"hi\"", str("ok"));

            assertThat(Json.write(value)).isEqualTo("{\"say \\\"hi\\\"\":\"ok\"}");
        }
    }
}
