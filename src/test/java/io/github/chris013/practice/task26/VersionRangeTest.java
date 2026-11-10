package io.github.chris013.practice.task26;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class VersionRangeTest {

    private static boolean includes(String range, String version) {
        return VersionRange.parse(range).includes(SemanticVersion.parse(version));
    }

    @Nested
    class Caret {

        @ParameterizedTest
        @CsvSource({
            "^1.2.3, 1.2.3, true",
            "^1.2.3, 1.9.9, true",
            "^1.2.3, 1.2.2, false",
            "^1.2.3, 2.0.0, false",
            "^0.2.3, 0.2.3, true",
            "^0.2.3, 0.2.9, true",
            "^0.2.3, 0.3.0, false",
            "^0.2.3, 0.2.2, false",
            "^0.0.3, 0.0.3, true",
            "^0.0.3, 0.0.4, false",
            "^0.0.3, 0.1.0, false",
        })
        void follows_the_npm_rules_for_major_minor_and_patch_zero(String range, String version, boolean expected) {
            assertThat(includes(range, version)).isEqualTo(expected);
        }
    }

    @Nested
    class Tilde {

        @ParameterizedTest
        @CsvSource({
            "~1.2.3, 1.2.3, true",
            "~1.2.3, 1.2.9, true",
            "~1.2.3, 1.3.0, false",
            "~1.2.3, 1.2.2, false",
            "~0.2.3, 0.2.9, true",
            "~0.2.3, 0.3.0, false",
        })
        void allows_only_patch_changes(String range, String version, boolean expected) {
            assertThat(includes(range, version)).isEqualTo(expected);
        }
    }

    @Nested
    class Exact {

        @ParameterizedTest
        @CsvSource({
            "1.2.3, 1.2.3, true",
            "1.2.3, 1.2.4, false",
            "=1.2.3, 1.2.3, true",
            "1.2.3, 1.2.3+build.5, true",
            "1.2.3-rc.1, 1.2.3-rc.1, true",
            "1.2.3-rc.1, 1.2.3-rc.2, false",
        })
        void matches_by_precedence_only(String range, String version, boolean expected) {
            assertThat(includes(range, version)).isEqualTo(expected);
        }
    }

    @Nested
    class Star {

        @ParameterizedTest
        @CsvSource({
            "*, 0.0.0, true",
            "*, 0.1.0, true",
            "*, 1.0.0, true",
            "*, 1.0.0+build.9, true",
            "*, 1.0.0-rc.1, false",
        })
        void includes_every_version_without_a_pre_release(String range, String version, boolean expected) {
            assertThat(includes(range, version)).isEqualTo(expected);
        }
    }

    @Nested
    class ComparatorSets {

        @ParameterizedTest
        @CsvSource({
            ">=1.2.0 <1.5.0, 1.2.0, true",
            ">=1.2.0 <1.5.0, 1.4.9, true",
            ">=1.2.0 <1.5.0, 1.5.0, false",
            ">=1.2.0 <1.5.0, 1.1.9, false",
            ">1.2.0 <=1.5.0, 1.2.0, false",
            ">1.2.0 <=1.5.0, 1.5.0, true",
            "<2.0.0, 1.9.9, true",
            "<2.0.0, 2.0.0, false",
        })
        void every_comparator_must_hold(String range, String version, boolean expected) {
            assertThat(includes(range, version)).isEqualTo(expected);
        }
    }

    @Nested
    class PreReleases {

        @ParameterizedTest
        @CsvSource({
            "^1.2.3-beta.2, 1.2.3-beta.4, true",
            "^1.2.3-beta.2, 1.2.3-beta.1, false",
            "^1.2.3-beta.2, 1.2.4-beta.1, false",
            "^1.2.3-beta.2, 1.2.3, true",
            "^1.2.3-beta.2, 2.0.0-rc.1, false",
            "^1.2.3, 1.3.0-rc.1, false",
            "~1.2.3-beta.2, 1.2.3-beta.3, true",
            "~1.2.3-beta.2, 1.2.4-beta.1, false",
            ">=1.2.0-rc.1 <1.2.0, 1.2.0-rc.2, true",
            ">=1.2.0 <1.5.0, 1.4.0-rc.1, false",
        })
        void a_pre_release_needs_a_comparator_with_the_same_major_minor_patch(
                String range, String version, boolean expected) {
            assertThat(includes(range, version)).isEqualTo(expected);
        }
    }

    @Nested
    class Parsing {

        @Test
        void to_string_returns_the_trimmed_original_text() {
            assertThat(VersionRange.parse("  ^1.2.3 ")).hasToString("^1.2.3");
            assertThat(VersionRange.parse(">=1.2.0 <1.5.0")).hasToString(">=1.2.0 <1.5.0");
        }

        @ParameterizedTest
        @ValueSource(strings = {
            "",
            "   ",
            "^",
            "~",
            "^1.2",
            "^1.2.3 <2.0.0",
            "~ 1.2.3",
            ">=",
            ">=1.2.0 1.5.0",
            "1.2.3 <2.0.0",
            ">=1.2.3 foo",
            "x",
            "*1.2.3",
            "^2147483647.0.0",
        })
        void rejects_malformed_ranges_and_names_the_input(String text) {
            assertThatThrownBy(() -> VersionRange.parse(text))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("invalid range '" + text.strip() + "'");
        }

        @Test
        void rejects_null() {
            assertThatThrownBy(() -> VersionRange.parse(null)).isInstanceOf(NullPointerException.class);
        }
    }
}
