package io.github.chris013.practice.task02;

import static io.github.chris013.practice.task02.PasswordViolation.ADMIN_MISSING_SPECIAL_CHARACTER;
import static io.github.chris013.practice.task02.PasswordViolation.ADMIN_TOO_SHORT;
import static io.github.chris013.practice.task02.PasswordViolation.CONTAINS_USERNAME;
import static io.github.chris013.practice.task02.PasswordViolation.MISSING;
import static io.github.chris013.practice.task02.PasswordViolation.MISSING_CHARACTER_CLASSES;
import static io.github.chris013.practice.task02.PasswordViolation.TOO_LONG;
import static io.github.chris013.practice.task02.PasswordViolation.TOO_SHORT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class PasswordPolicyTest {

    private final PasswordPolicy policy = new PasswordPolicy();

    @Test
    void accepts_a_good_password() {
        assertThat(policy.violations("Abcdefgh1", "anna", Role.USER)).isEmpty();
        assertThat(policy.violations("Abcdefghij1!", "root", Role.ADMIN)).isEmpty();
    }

    @Test
    void reports_only_missing_when_there_is_no_password() {
        assertThat(policy.violations(null, "anna", Role.USER)).containsExactly(MISSING);
    }

    @Test
    void reports_every_violation_at_once_in_a_stable_order() {
        assertThat(policy.violations("abc", "abc", Role.USER))
                .containsExactly(TOO_SHORT, MISSING_CHARACTER_CLASSES, CONTAINS_USERNAME);
    }

    @Test
    void applies_the_stricter_rules_to_admins_only() {
        assertThat(policy.violations("abc", "root", Role.ADMIN))
                .containsExactly(TOO_SHORT, ADMIN_TOO_SHORT, MISSING_CHARACTER_CLASSES, ADMIN_MISSING_SPECIAL_CHARACTER);
        assertThat(policy.violations("Abcdefgh1", "anna", Role.USER)).isEmpty();
    }

    @Test
    void reports_too_long_together_with_other_problems() {
        assertThat(policy.violations("x".repeat(70), "anna", Role.USER))
                .containsExactly(MISSING_CHARACTER_CLASSES, TOO_LONG);
    }

    @Test
    void needs_a_role() {
        assertThatThrownBy(() -> policy.violations("Abcdefgh1", "anna", null))
                .isInstanceOf(NullPointerException.class);
    }

    /** After part 2 the old API must still behave like before: it reports the first violation only. */
    @ParameterizedTest
    @CsvSource({
        "abc,          anna, false",
        "abcdefghij,   root, true",
        "Annabelle2024, anna, false",
        "Abcdefgh1,    anna, false",
        "Abcdefghijk1, root, true"
    })
    void agrees_with_the_legacy_checker(String password, String username, boolean isAdmin) {
        String expected = policy.violations(password, username, isAdmin ? Role.ADMIN : Role.USER).stream()
                .findFirst()
                .map(PasswordViolation::message)
                .orElse(null);

        assertThat(new PasswordChecker().check(password, username, isAdmin)).isEqualTo(expected);
    }
}
