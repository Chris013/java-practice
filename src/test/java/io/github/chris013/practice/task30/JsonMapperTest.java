package io.github.chris013.practice.task30;

import static io.github.chris013.practice.task30.JsonFixtures.array;
import static io.github.chris013.practice.task30.JsonFixtures.num;
import static io.github.chris013.practice.task30.JsonFixtures.object;
import static io.github.chris013.practice.task30.JsonFixtures.str;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class JsonMapperTest {

    record Customer(String name, int age) {}

    record Renamed(@JsonName("customer_id") String id, @JsonName("is_active") boolean active) {}

    record Line(String sku, int quantity) {}

    record Order(String id, List<Line> lines, Map<String, Integer> counts) {}

    record Maybe(String name, Optional<String> nickname) {}

    record Pair(List<String> first, List<String> second) {}

    record Nullable(String value) {}

    record Duplicate(@JsonName("same") String a, @JsonName("same") String b) {}

    record Fragile(String name) {
        @Override
        public String name() {
            throw new IllegalStateException("boom");
        }
    }

    record Node(List<Object> children) {}

    enum Colour { RED, GREEN }

    @Nested
    class Scalars {

        @Test
        void maps_null_to_json_null() {
            assertThat(JsonMapper.toJson(null)).isEqualTo(JsonNull.INSTANCE);
        }

        @Test
        void returns_a_json_value_unchanged() {
            var value = num("7");

            assertThat(JsonMapper.toJson(value)).isSameAs(value);
        }

        @Test
        void maps_booleans() {
            assertThat(JsonMapper.toJson(true)).isEqualTo(JsonBoolean.TRUE);
            assertThat(JsonMapper.toJson(false)).isEqualTo(JsonBoolean.FALSE);
        }

        @Test
        void maps_integral_numbers_exactly() {
            assertThat(JsonMapper.toJson(42)).isEqualTo(num("42"));
            assertThat(JsonMapper.toJson(Long.MIN_VALUE)).isEqualTo(num("-9223372036854775808"));
            assertThat(JsonMapper.toJson((short) -3)).isEqualTo(num("-3"));
            assertThat(JsonMapper.toJson((byte) 7)).isEqualTo(num("7"));
            assertThat(JsonMapper.toJson(new BigInteger("12345678901234567890"))).isEqualTo(num("12345678901234567890"));
        }

        @Test
        void maps_big_decimal_as_it_is() {
            assertThat(Json.write(JsonMapper.toJson(new BigDecimal("1.50")))).isEqualTo("1.50");
        }

        @ParameterizedTest
        @CsvSource({"0.1, 0.1", "2.5, 2.5", "1.0E10, 10000000000"})
        void maps_doubles_through_their_decimal_text(double input, String expected) {
            // BigDecimal.valueOf(double) goes through Double.toString, so 0.1 stays 0.1.
            assertThat(Json.write(JsonMapper.toJson(input))).isEqualTo(expected);
        }

        @Test
        void maps_a_float_through_its_shortest_text() {
            assertThat(Json.write(JsonMapper.toJson(0.1f))).isEqualTo("0.1");
        }

        @Test
        void rejects_nan_and_infinity_because_json_has_no_such_numbers() {
            assertThatThrownBy(() -> JsonMapper.toJson(Double.NaN)).isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> JsonMapper.toJson(Double.POSITIVE_INFINITY))
                    .isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> JsonMapper.toJson(Float.NaN)).isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void maps_strings_characters_and_char_sequences() {
            assertThat(JsonMapper.toJson("ada")).isEqualTo(str("ada"));
            assertThat(JsonMapper.toJson('x')).isEqualTo(str("x"));
            assertThat(JsonMapper.toJson(new StringBuilder("sb"))).isEqualTo(str("sb"));
        }

        @Test
        void maps_an_enum_to_its_name() {
            assertThat(JsonMapper.toJson(Colour.GREEN)).isEqualTo(str("GREEN"));
        }

        @Test
        void maps_java_time_values_to_their_to_string_text() {
            assertThat(JsonMapper.toJson(LocalDate.of(2024, 3, 1))).isEqualTo(str("2024-03-01"));
            assertThat(JsonMapper.toJson(Instant.parse("2024-03-01T10:15:30Z")))
                    .isEqualTo(str("2024-03-01T10:15:30Z"));
        }

        @Test
        void maps_an_optional_to_its_value_and_an_empty_one_to_null() {
            assertThat(JsonMapper.toJson(Optional.of("x"))).isEqualTo(str("x"));
            assertThat(JsonMapper.toJson(Optional.empty())).isEqualTo(JsonNull.INSTANCE);
        }

        @Test
        void rejects_a_type_it_does_not_know_and_names_it() {
            assertThatThrownBy(() -> JsonMapper.toJson(new Object()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("unsupported type: java.lang.Object");
            assertThatThrownBy(() -> JsonMapper.toJson(Thread.currentThread()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("unsupported type: java.lang.Thread");
        }
    }

    @Nested
    class Containers {

        @Test
        void maps_a_list_to_an_array_in_order() {
            assertThat(JsonMapper.toJson(List.of("a", "b"))).isEqualTo(array(str("a"), str("b")));
        }

        @Test
        void maps_a_list_element_that_is_an_empty_optional_to_null() {
            var list = new ArrayList<Object>();
            list.add(Optional.empty());

            assertThat(JsonMapper.toJson(list)).isEqualTo(array(JsonNull.INSTANCE));
        }

        @Test
        void maps_a_map_keeping_its_iteration_order() {
            var map = new LinkedHashMap<String, Integer>();
            map.put("zeta", 1);
            map.put("alpha", 2);

            assertThat(Json.write(JsonMapper.toJson(map))).isEqualTo("{\"zeta\":1,\"alpha\":2}");
        }

        @Test
        void rejects_a_map_with_a_non_string_key_and_names_the_key_type() {
            assertThatThrownBy(() -> JsonMapper.toJson(Map.of(1, "one")))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("map keys must be String, not java.lang.Integer");
        }

        @Test
        void maps_primitive_int_arrays() {
            assertThat(JsonMapper.toJson(new int[] {1, 2})).isEqualTo(array(num("1"), num("2")));
        }

        @Test
        void maps_string_arrays_and_empty_arrays() {
            assertThat(JsonMapper.toJson(new String[] {"a"})).isEqualTo(array(str("a")));
            assertThat(JsonMapper.toJson(new int[0])).isEqualTo(array());
        }
    }

    @Nested
    class Records {

        @Test
        void maps_components_in_declaration_order_with_their_names() {
            var value = JsonMapper.toJson(new Customer("Ada", 36));

            assertThat(value).isEqualTo(object("name", str("Ada"), "age", num("36")));
            assertThat(Json.write(value)).isEqualTo("{\"name\":\"Ada\",\"age\":36}");
        }

        @Test
        void uses_json_name_when_it_is_present() {
            var value = JsonMapper.toJson(new Renamed("c-1", true));

            assertThat(Json.write(value)).isEqualTo("{\"customer_id\":\"c-1\",\"is_active\":true}");
        }

        @Test
        void maps_nested_records_with_lists_and_maps() {
            var order = new Order("o-1", List.of(new Line("tea", 2)), Map.of("tea", 2));

            assertThat(Json.write(JsonMapper.toJson(order)))
                    .isEqualTo("{\"id\":\"o-1\",\"lines\":[{\"sku\":\"tea\",\"quantity\":2}],\"counts\":{\"tea\":2}}");
        }

        @Test
        void leaves_out_an_empty_optional_component() {
            var value = JsonMapper.toJson(new Maybe("Ada", Optional.empty()));

            assertThat(value).isEqualTo(object("name", str("Ada")));
        }

        @Test
        void includes_a_present_optional_component_as_its_value() {
            var value = JsonMapper.toJson(new Maybe("Ada", Optional.of("Countess")));

            assertThat(value).isEqualTo(object("name", str("Ada"), "nickname", str("Countess")));
        }

        @Test
        void maps_a_null_component_to_json_null_instead_of_omitting_it() {
            assertThat(JsonMapper.toJson(new Nullable(null))).isEqualTo(object("value", JsonNull.INSTANCE));
        }

        @Test
        void rejects_two_components_with_the_same_json_name() {
            assertThatThrownBy(() -> JsonMapper.toJson(new Duplicate("x", "y")))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("duplicate JSON name: same");
        }

        @Test
        void surfaces_an_accessor_failure_with_the_original_exception_as_cause() {
            assertThatThrownBy(() -> JsonMapper.toJson(new Fragile("x")))
                    .isInstanceOf(IllegalStateException.class)
                    .hasCauseInstanceOf(IllegalStateException.class)
                    .cause()
                    .hasMessage("boom");
        }

        @Test
        void the_same_list_in_two_components_is_not_a_cycle() {
            var shared = List.of("a");

            assertThat(JsonMapper.toJson(new Pair(shared, shared)))
                    .isEqualTo(object("first", array(str("a")), "second", array(str("a"))));
        }
    }

    @Nested
    class Cycles {

        @Test
        void rejects_a_list_that_contains_itself() {
            var list = new ArrayList<Object>();
            list.add(list);

            assertThatThrownBy(() -> JsonMapper.toJson(list))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("cycle detected");
        }

        @Test
        void rejects_a_cycle_that_goes_through_a_record() {
            var children = new ArrayList<Object>();
            var node = new Node(children);
            children.add(node);

            assertThatThrownBy(() -> JsonMapper.toJson(node))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("cycle detected");
        }

        @Test
        void rejects_a_map_that_contains_itself() {
            var map = new LinkedHashMap<String, Object>();
            map.put("self", map);

            assertThatThrownBy(() -> JsonMapper.toJson(map))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("cycle detected");
        }

        @Test
        void two_equal_lists_that_are_different_objects_are_not_a_cycle() {
            var outer = List.of(List.of("x"), List.of("x"));

            assertThat(JsonMapper.toJson(outer)).isEqualTo(array(array(str("x")), array(str("x"))));
        }
    }
}
