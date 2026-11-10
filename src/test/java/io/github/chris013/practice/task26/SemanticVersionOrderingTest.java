package io.github.chris013.practice.task26;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class SemanticVersionOrderingTest {

    /** The example chain from SemVer section 11.4, lowest first. */
    private static final List<String> SEMVER_CHAIN = List.of(
            "1.0.0-alpha",
            "1.0.0-alpha.1",
            "1.0.0-alpha.beta",
            "1.0.0-beta",
            "1.0.0-beta.2",
            "1.0.0-beta.11",
            "1.0.0-rc.1",
            "1.0.0");

    private static SemanticVersion v(String text) {
        return SemanticVersion.parse(text);
    }

    private static List<SemanticVersion> chain() {
        return SEMVER_CHAIN.stream().map(SemanticVersion::parse).toList();
    }

    @Nested
    class Precedence {

        @Test
        void follows_the_semver_example_chain() {
            List<SemanticVersion> versions = chain();

            assertThat(versions).isSorted();
            assertThat(versions).doesNotHaveDuplicates();

            List<SemanticVersion> shuffled = new ArrayList<>(versions.reversed());
            Collections.sort(shuffled);
            assertThat(shuffled).containsExactlyElementsOf(versions);
        }

        @Test
        void sorts_numbers_by_value_not_by_text() {
            assertThat(v("1.9.0")).isLessThan(v("1.10.0"));
            assertThat(v("1.9.9")).isLessThan(v("2.0.0"));
        }

        @ParameterizedTest
        @CsvSource({
            "1.2.3, 1.2.4",
            "1.2.9, 1.3.0",
            "1.9.9, 2.0.0",
            "0.9.9, 1.0.0-alpha",
            "1.0.0-rc.1, 1.0.0",
            "1.0.0-alpha, 1.0.0-alpha.0",
            "1.0.0-1, 1.0.0-alpha",
            "1.0.0-2, 1.0.0-10",
            "1.0.0-9, 1.0.0-99999999999999999999",
            "1.0.0-A, 1.0.0-a",
            "2.0.0-rc.1, 2.0.0-rc.1.1",
        })
        void orders_the_lower_version_first(String lower, String higher) {
            assertThat(v(lower)).isLessThan(v(higher));
            assertThat(v(higher)).isGreaterThan(v(lower));
        }

        @Test
        void precedence_comparator_is_one_shared_constant() {
            assertThat(SemanticVersion.precedence()).isSameAs(SemanticVersion.precedence());
        }

        @Test
        void precedence_sorts_the_same_way_when_there_is_no_build_metadata() {
            List<SemanticVersion> versions = chain();
            List<SemanticVersion> shuffled = new ArrayList<>(versions.reversed());

            shuffled.sort(SemanticVersion.precedence());

            assertThat(shuffled).containsExactlyElementsOf(versions);
        }
    }

    @Nested
    class BuildMetadata {

        @Test
        void precedence_ignores_build_metadata() {
            assertThat(SemanticVersion.precedence().compare(v("1.0.0+a"), v("1.0.0+b"))).isZero();
        }

        @Test
        void compare_to_breaks_the_tie_with_build_metadata() {
            assertThat(v("1.0.0+a")).isLessThan(v("1.0.0+b"));
        }

        @ParameterizedTest
        @CsvSource({
            "1.0.0, 1.0.0+build.1",
            "1.0.0+a, 1.0.0+a.1",
            "1.0.0+a.10, 1.0.0+a.9",
            "1.0.0-rc.1+zzz, 1.0.0+aaa",
        })
        void orders_builds_after_precedence_as_plain_ascii(String lower, String higher) {
            assertThat(v(lower)).isLessThan(v(higher));
        }
    }

    @Nested
    class ConsistentWithEquals {

        @ParameterizedTest
        @CsvSource({
            "1.0.0, 1.0.0, true",
            "1.0.0+a, 1.0.0+a, true",
            "1.0.0+a, 1.0.0+b, false",
            "1.0.0+a, 1.0.0, false",
            "1.0.0-rc.1, 1.0.0-rc.1, true",
            "1.0.0-rc.1, 1.0.0-rc.2, false",
        })
        void compare_to_is_zero_exactly_when_equals_is_true(String left, String right, boolean same) {
            assertThat(v(left).compareTo(v(right)) == 0).isEqualTo(same);
            assertThat(v(left).equals(v(right))).isEqualTo(same);
        }

        @Test
        void a_natural_order_tree_set_keeps_both_builds() {
            TreeSet<SemanticVersion> versions = new TreeSet<>();

            versions.add(v("1.0.0+a"));
            versions.add(v("1.0.0+b"));

            assertThat(versions).hasSize(2);
        }

        @Test
        void a_hash_set_keeps_both_builds() {
            Set<SemanticVersion> versions = new HashSet<>(List.of(v("1.0.0+a"), v("1.0.0+b")));

            assertThat(versions).hasSize(2);
        }

        @Test
        void a_precedence_tree_set_treats_the_two_builds_as_one() {
            TreeSet<SemanticVersion> versions = new TreeSet<>(SemanticVersion.precedence());

            assertThat(versions.add(v("1.0.0+a"))).isTrue();
            assertThat(versions.add(v("1.0.0+b"))).isFalse();

            assertThat(versions).containsExactly(v("1.0.0+a"));
        }
    }
}
