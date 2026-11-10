package io.github.chris013.practice.task30;

import static io.github.chris013.practice.task30.JsonFixtures.num;
import static io.github.chris013.practice.task30.JsonFixtures.str;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** The value model is given code. These tests pass on the starter as well: they document the guarantees. */
class JsonValueModelTest {

    @Nested
    class Arrays {

        @Test
        void copies_the_elements_so_later_changes_do_not_leak_in() {
            var source = new ArrayList<JsonValue>(List.of(JsonBoolean.TRUE));
            var array = new JsonArray(source);

            source.add(JsonBoolean.FALSE);

            assertThat(array.elements()).containsExactly(JsonBoolean.TRUE);
        }

        @Test
        void rejects_a_null_list() {
            assertThatThrownBy(() -> new JsonArray(null)).isInstanceOf(NullPointerException.class);
        }

        @Test
        void rejects_a_null_element() {
            var withNull = new ArrayList<JsonValue>();
            withNull.add(null);

            assertThatThrownBy(() -> new JsonArray(withNull)).isInstanceOf(NullPointerException.class);
        }
    }

    @Nested
    class Objects {

        @Test
        void keeps_the_insertion_order_and_copies_the_members() {
            var source = new LinkedHashMap<String, JsonValue>();
            source.put("zeta", str("last alphabetically"));
            source.put("alpha", str("first inserted"));

            var object = new JsonObject(source);
            source.put("late", str("not visible"));

            assertThat(object.members()).containsExactly(
                    Map.entry("zeta", str("last alphabetically")),
                    Map.entry("alpha", str("first inserted")));
        }

        @Test
        void the_members_cannot_be_changed() {
            var object = new JsonObject(new LinkedHashMap<>());

            assertThatThrownBy(() -> object.members().put("x", JsonNull.INSTANCE))
                    .isInstanceOf(UnsupportedOperationException.class);
        }

        @Test
        void rejects_a_null_key_or_value() {
            var nullKey = new LinkedHashMap<String, JsonValue>();
            nullKey.put(null, JsonNull.INSTANCE);
            var nullValue = new LinkedHashMap<String, JsonValue>();
            nullValue.put("x", null);

            assertThatThrownBy(() -> new JsonObject(nullKey)).isInstanceOf(NullPointerException.class);
            assertThatThrownBy(() -> new JsonObject(nullValue)).isInstanceOf(NullPointerException.class);
        }
    }

    @Nested
    class Scalars {

        @Test
        void rejects_a_null_number_or_string() {
            assertThatThrownBy(() -> new JsonNumber(null)).isInstanceOf(NullPointerException.class);
            assertThatThrownBy(() -> new JsonString(null)).isInstanceOf(NullPointerException.class);
        }

        @Test
        void values_compare_by_value() {
            assertThat(num("1.5")).isEqualTo(num("1.5")).isNotEqualTo(str("1.5"));
        }
    }
}
