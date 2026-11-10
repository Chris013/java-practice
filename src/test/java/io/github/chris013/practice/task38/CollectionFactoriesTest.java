package io.github.chris013.practice.task38;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Learning tests: they document what the JDK collection factories and wrappers really do.
 * They pass on the starter and on the solution.
 */
class CollectionFactoriesTest {

    @Nested
    class ImmutableFactories {

        @Test
        void list_of_rejects_null_elements() {
            assertThatThrownBy(() -> List.of("anna", null)).isInstanceOf(NullPointerException.class);
        }

        @Test
        void list_of_throws_when_asked_whether_it_contains_null() {
            List<String> names = List.of("anna");

            assertThatThrownBy(() -> names.contains(null)).isInstanceOf(NullPointerException.class);
        }

        @Test
        void map_of_rejects_duplicate_keys() {
            assertThatThrownBy(() -> Map.of("anna", 1, "anna", 2)).isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void map_copy_of_is_unmodifiable() {
            Map<String, Integer> copy = Map.copyOf(new HashMap<>(Map.of("anna", 1)));

            assertThatThrownBy(() -> copy.put("ben", 2)).isInstanceOf(UnsupportedOperationException.class);
        }
    }

    @Nested
    class Wrappers {

        @Test
        void arrays_as_list_allows_set_but_not_add() {
            List<String> names = Arrays.asList("anna", "ben");

            names.set(0, "cy");

            assertThat(names).containsExactly("cy", "ben");
            assertThatThrownBy(() -> names.add("dora")).isInstanceOf(UnsupportedOperationException.class);
        }

        @Test
        void unmodifiable_list_is_a_view_that_follows_its_backing_list() {
            List<String> backing = new ArrayList<>(List.of("anna"));
            List<String> view = Collections.unmodifiableList(backing);

            backing.add("ben");

            assertThat(view).containsExactly("anna", "ben");
            assertThatThrownBy(() -> view.add("cy")).isInstanceOf(UnsupportedOperationException.class);
        }
    }
}
