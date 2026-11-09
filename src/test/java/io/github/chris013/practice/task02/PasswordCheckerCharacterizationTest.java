package io.github.chris013.practice.task02;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Pins down what {@link PasswordChecker#check} does <em>today</em>, quirks included. These tests are green before you
 * start and must stay green after every single refactoring step.
 */
class PasswordCheckerCharacterizationTest {

    private static final String OK = null;
    private static final boolean USER = false;
    private static final boolean ADMIN = true;

    static Stream<Arguments> currentBehaviour() {
        return Stream.of(
                Arguments.of(null, "anna", USER, "Password missing"),
                Arguments.of("Ab1", "anna", USER, "Password too short"),
                Arguments.of("Abcdef1", "anna", USER, "Password too short"),
                Arguments.of("Abcdefg1", "anna", USER, OK),
                Arguments.of("abcdefgh1", "anna", USER, "Password needs digit, upper and lower case letter"),
                Arguments.of("ABCDEFGH1", "anna", USER, "Password needs digit, upper and lower case letter"),
                Arguments.of("Abcdefghi", "anna", USER, "Password needs digit, upper and lower case letter"),
                Arguments.of("Annabelle2024", "anna", USER, "Password must not contain the username"),
                Arguments.of("xANNAx2024x", "anna", USER, "Password must not contain the username"),
                Arguments.of("Abcdefgh1", null, USER, OK),
                Arguments.of("Abcdefg1" + "x".repeat(56), "anna", USER, OK),
                Arguments.of("Abcdefg1" + "x".repeat(57), "anna", USER, "Password too long"),
                Arguments.of("x".repeat(70), "anna", USER, "Password needs digit, upper and lower case letter"),

                Arguments.of("abc", "root", ADMIN, "Password too short"),
                Arguments.of("Abcdefg1!", "root", ADMIN, "Admin passwords need at least 12 characters"),
                Arguments.of("abcdefghij", "root", ADMIN, "Admin passwords need at least 12 characters"),
                Arguments.of("Abcdefghijk1", "root", ADMIN, "Admin passwords need a special character"),
                Arguments.of("Abcdefghij1!", "root", ADMIN, OK),
                Arguments.of("Abcdefghij1 ", "root", ADMIN, OK),
                Arguments.of("Rootabcdefg1!", "root", ADMIN, "Password must not contain the username"),
                Arguments.of("Abcdefghij1" + "x".repeat(60), "root", ADMIN, "Admin passwords need a special character"),

                // Known quirk: an empty username is contained in every string, so every password is rejected.
                // Keep this behaviour while refactoring. Fixing it is a separate, deliberate change (see stretch goals).
                Arguments.of("Abcdefgh1", "", USER, "Password must not contain the username"));
    }

    @ParameterizedTest(name = "[{index}] check(\"{0}\", \"{1}\", admin={2}) -> {3}")
    @MethodSource("currentBehaviour")
    void behaves_exactly_as_before(String password, String username, boolean isAdmin, String expected) {
        assertThat(new PasswordChecker().check(password, username, isAdmin)).isEqualTo(expected);
    }
}
