package io.github.chris013.practice.task26;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class SemanticVersionParsingTest {

    @Nested
    class Valid {

        @ParameterizedTest
        @CsvSource({
            "0.0.0",
            "1.2.3",
            "10.20.30",
            "2147483647.2147483647.2147483647",
            "1.0.0-alpha",
            "1.0.0-alpha.1",
            "1.0.0-0.3.7",
            "1.0.0-x.7.z.92",
            "1.0.0-0A.is.legal",
            "1.0.0-x-y-z.--",
            "1.0.0-alpha.123456789012345678901234567890.beta-gamma.1",
            "1.0.0+20130313144700",
            "1.0.0-beta+exp.sha.5114f85",
            "1.0.0+21AF26D3----117B344092BD",
            "1.0.0+001",
        })
        void parses_and_prints_the_same_text(String text) {
            assertThat(SemanticVersion.parse(text)).hasToString(text);
        }

        @ParameterizedTest
        @CsvSource({
            "1.2.3",
            "1.0.0-rc.1",
            "1.0.0+build.7",
            "1.0.0-beta+exp.sha.5114f85",
            "1.0.0+001",
        })
        void round_trip_gives_an_equal_version(String text) {
            SemanticVersion version = SemanticVersion.parse(text);

            assertThat(SemanticVersion.parse(version.toString())).isEqualTo(version);
        }

        @Test
        void exposes_each_part() {
            SemanticVersion version = SemanticVersion.parse("1.2.3-rc.1+build.7");

            assertThat(version.major()).isEqualTo(1);
            assertThat(version.minor()).isEqualTo(2);
            assertThat(version.patch()).isEqualTo(3);
            assertThat(version.preRelease()).containsExactly("rc", "1");
            assertThat(version.build()).containsExactly("build", "7");
        }

        @Test
        void keeps_leading_zeros_in_build_metadata() {
            assertThat(SemanticVersion.parse("1.0.0+001").build()).containsExactly("001");
        }

        @Test
        void of_builds_a_stable_version_without_metadata() {
            SemanticVersion version = SemanticVersion.of(1, 2, 3);

            assertThat(version).isEqualTo(SemanticVersion.parse("1.2.3"));
            assertThat(version.preRelease()).isEmpty();
            assertThat(version.build()).isEmpty();
        }

        @ParameterizedTest
        @CsvSource({
            "1.2.3, false, true",
            "0.9.0, false, false",
            "1.0.0+build, false, true",
            "1.0.0-rc.1, true, false",
            "2.0.0-rc.1, true, false",
        })
        void tells_pre_releases_and_stable_versions_apart(String text, boolean preRelease, boolean stable) {
            SemanticVersion version = SemanticVersion.parse(text);

            assertThat(version.isPreRelease()).isEqualTo(preRelease);
            assertThat(version.isStable()).isEqualTo(stable);
        }
    }

    @Nested
    class Invalid {

        // Column 1 is the input, column 2 a fragment of the message. The input is quoted so that spaces survive.
        @ParameterizedTest
        @CsvSource(delimiter = '|', quoteCharacter = '"', value = {
            "\"\"|version is empty",
            "1.2|expected three numbers",
            "1.2.3.4|expected three numbers",
            "v1.2.3|invalid character 'v' in major",
            "01.2.3|leading zero in major",
            "1.2.3-|empty pre-release identifier",
            "1.2.3-01|leading zero in pre-release identifier '01'",
            "1.2.3-a..b|empty pre-release identifier",
            "1.2.3+|empty build identifier",
            "1.2.3-a$|invalid character '$' in pre-release identifier",
            "99999999999.0.0|major is too large",
            "\" 1.2.3\"|invalid character ' ' in major",
        })
        void rejects_the_text_and_says_what_is_wrong(String text, String reason) {
            assertThatThrownBy(() -> SemanticVersion.parse(text))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("'" + text + "'")
                    .hasMessageContaining(reason);
        }

        @Test
        void rejects_null() {
            assertThatThrownBy(() -> SemanticVersion.parse(null)).isInstanceOf(NullPointerException.class);
        }
    }

    @Nested
    class Construction {

        @Test
        void rejects_a_negative_number() {
            assertThatThrownBy(() -> new SemanticVersion(-1, 0, 0, List.of(), List.of()))
                    .isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> SemanticVersion.of(0, 0, -1)).isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void rejects_a_leading_zero_in_a_numeric_pre_release_identifier() {
            assertThatThrownBy(() -> new SemanticVersion(1, 0, 0, List.of("01"), List.of()))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void allows_a_leading_zero_in_build_metadata() {
            assertThat(new SemanticVersion(1, 0, 0, List.of(), List.of("007")).build()).containsExactly("007");
        }

        @Test
        void rejects_null_components() {
            assertThatThrownBy(() -> new SemanticVersion(1, 0, 0, null, List.of()))
                    .isInstanceOf(NullPointerException.class);
            assertThatThrownBy(() -> new SemanticVersion(1, 0, 0, List.of(), null))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        void copies_the_lists_so_later_changes_do_not_leak_in() {
            List<String> preRelease = new ArrayList<>(List.of("rc"));
            SemanticVersion version = new SemanticVersion(1, 0, 0, preRelease, List.of());

            preRelease.add("2");

            assertThat(version.preRelease()).containsExactly("rc");
        }
    }
}
