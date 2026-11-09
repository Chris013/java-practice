package io.github.chris013.practice.task14;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Every invalid input must fail with the right line number and a message that says what is wrong. In the tables
 * below, {@code ~} stands for a line break, and the leading spaces of an indented line are kept as written.
 */
class FlagConfigurationErrorsTest {

    @ParameterizedTest
    @CsvSource(quoteCharacter = '"', delimiter = '|', ignoreLeadingAndTrailingWhitespace = false, value = {
        "  percent 10|1|appears before any flag",
        "# only a comment~  percent 5|2|appears before any flag",
        "flag a~  percentage 10|2|unknown keyword 'percentage'",
        "flag a~  Percent 5|2|unknown keyword 'Percent'",
        "flag a~  percent 5~  users alice~  bogus|4|unknown keyword 'bogus'",
        "flag a~  percent 120|2|percent must be between 0 and 100 but was 120",
        "flag a~  percent -1|2|percent must be between 0 and 100 but was -1",
        "flag one~  default on~~flag two~  percent 101|5|percent must be between 0 and 100 but was 101",
        "flag a~  percent fifty|2|percent must be a whole number but was 'fifty'",
        "flag a~  percent|2|percent needs a number",
        "flag Bad|1|invalid flag name 'Bad'",
        "flag -a|1|invalid flag name '-a'",
        "flag a-|1|invalid flag name 'a-'",
        "flag|1|flag needs a name",
        "flag a~flag a|2|duplicate flag 'a'",
        "flag a~  default on~  default off|3|default is already set for flag 'a'",
        "flag a~  default maybe|2|default must be on or off but was 'maybe'",
        "flag a~  countries de|2|country must be two uppercase letters but was 'de'",
        "flag a~  countries DE,at|2|two uppercase letters but was 'at'",
        "flag a~  countries|2|countries needs at least one value",
        "flag a~  users|2|users needs at least one value",
        "flag a~  users alice,,bob|2|empty entry in users list",
        "flag a~  users alice,|2|empty entry in users list",
        "flag a~  off now|2|'off' takes no value but found 'now'",
        "flag a~  flag b|2|flag lines must not be indented",
        "flag a~percent 5|2|expected 'flag <name>' at the start of a line but found 'percent'"
    })
    void rejects_invalid_configuration_with_its_line_number(String configuration, int line, String messageFragment) {
        String text = configuration.replace('~', '\n');

        assertThatThrownBy(() -> FeatureFlags.parse(text))
                .isInstanceOfSatisfying(FlagConfigurationException.class, e -> {
                    assertThat(e.lineNumber()).isEqualTo(line);
                    assertThat(e.getMessage()).startsWith("line " + line + ": ").contains(messageFragment);
                });
    }
}
