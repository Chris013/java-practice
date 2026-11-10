package io.github.chris013.practice.task30;

import static io.github.chris013.practice.task30.JsonFixtures.array;
import static io.github.chris013.practice.task30.JsonFixtures.num;
import static io.github.chris013.practice.task30.JsonFixtures.object;
import static io.github.chris013.practice.task30.JsonFixtures.str;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class JsonPrettyTest {

    @Nested
    class Layout {

        @Test
        void indents_by_two_spaces_and_puts_each_member_on_its_own_line() {
            var value = object("name", str("Ada"), "tags", array(str("x"), str("y")), "age", num("36"));

            assertThat(Json.writePretty(value)).isEqualTo("""
                    {
                      "name": "Ada",
                      "tags": [
                        "x",
                        "y"
                      ],
                      "age": 36
                    }""");
        }

        @Test
        void writes_empty_containers_on_one_line() {
            var value = object("empty", array(), "nothing", object());

            assertThat(Json.writePretty(value)).isEqualTo("""
                    {
                      "empty": [],
                      "nothing": {}
                    }""");
        }

        @Test
        void writes_a_scalar_as_is_and_has_no_trailing_newline() {
            assertThat(Json.writePretty(str("x"))).isEqualTo("\"x\"");
            assertThat(Json.writePretty(JsonNull.INSTANCE)).isEqualTo("null");
            assertThat(Json.writePretty(array())).isEqualTo("[]");
        }
    }

    @Nested
    class NestedContainers {

        @Test
        void writes_an_array_of_objects_with_each_object_indented_one_level() {
            var value = array(object("id", num("1")), object("id", num("2")));

            assertThat(Json.writePretty(value)).isEqualTo("""
                    [
                      {
                        "id": 1
                      },
                      {
                        "id": 2
                      }
                    ]""");
        }

        @Test
        void writes_an_empty_array_inside_an_array() {
            assertThat(Json.writePretty(array(array()))).isEqualTo("""
                    [
                      []
                    ]""");
        }

        @Test
        void pretty_and_compact_output_contain_the_same_tokens() {
            var value = object("a", array(JsonBoolean.TRUE, JsonNull.INSTANCE), "b", object("c", str("d")));

            String compactWithoutSpace = Json.write(value);
            String prettyWithoutSpace = Json.writePretty(value).replaceAll("\\s+", "");

            assertThat(prettyWithoutSpace).isEqualTo(compactWithoutSpace);
        }
    }
}
