package io.github.chris013.practice.task02;

import static io.github.chris013.practice.task02.PasswordViolation.ADMIN_MISSING_SPECIAL_CHARACTER;
import static io.github.chris013.practice.task02.PasswordViolation.ADMIN_TOO_SHORT;
import static io.github.chris013.practice.task02.PasswordViolation.CONTAINS_USERNAME;
import static io.github.chris013.practice.task02.PasswordViolation.MISSING;
import static io.github.chris013.practice.task02.PasswordViolation.MISSING_CHARACTER_CLASSES;
import static io.github.chris013.practice.task02.PasswordViolation.TOO_LONG;
import static io.github.chris013.practice.task02.PasswordViolation.TOO_SHORT;
import static java.util.stream.Collectors.toCollection;

import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;

/** Our password rules, as a list you can read top to bottom. */
public final class PasswordPolicy {

    private static final int MIN_LENGTH = 8;
    private static final int MIN_ADMIN_LENGTH = 12;
    private static final int MAX_LENGTH = 64;

    private static final List<Rule> RULES = List.of(
            new Rule(TOO_SHORT, candidate -> candidate.length() < MIN_LENGTH),
            new Rule(ADMIN_TOO_SHORT, candidate -> candidate.isAdmin() && candidate.length() < MIN_ADMIN_LENGTH),
            new Rule(MISSING_CHARACTER_CLASSES, candidate -> !candidate.hasDigitUpperAndLowerCase()),
            new Rule(ADMIN_MISSING_SPECIAL_CHARACTER, candidate -> candidate.isAdmin() && !candidate.hasSpecialCharacter()),
            new Rule(CONTAINS_USERNAME, Candidate::containsUsername),
            new Rule(TOO_LONG, candidate -> candidate.length() > MAX_LENGTH));

    /**
     * @return every rule the password violates, iterated in the order of {@link PasswordViolation}'s constants;
     *     empty if the password is acceptable
     */
    public Set<PasswordViolation> violations(String password, String username, Role role) {
        Objects.requireNonNull(role, "role");
        if (password == null) {
            return EnumSet.of(MISSING);
        }
        Candidate candidate = new Candidate(password, username, role);
        return RULES.stream()
                .filter(rule -> rule.isViolatedBy(candidate))
                .map(Rule::violation)
                .collect(toCollection(() -> EnumSet.noneOf(PasswordViolation.class)));
    }

    private record Rule(PasswordViolation violation, Predicate<Candidate> violatedBy) {

        boolean isViolatedBy(Candidate candidate) {
            return violatedBy.test(candidate);
        }
    }

    private record Candidate(String password, String username, Role role) {

        int length() {
            return password.length();
        }

        boolean isAdmin() {
            return role == Role.ADMIN;
        }

        boolean hasDigitUpperAndLowerCase() {
            return contains(CharacterClass.DIGIT) && contains(CharacterClass.UPPER_CASE) && contains(CharacterClass.LOWER_CASE);
        }

        boolean hasSpecialCharacter() {
            return contains(CharacterClass.SPECIAL);
        }

        boolean containsUsername() {
            // Known quirk kept from the legacy checker: an empty username matches every password.
            return username != null && password.toLowerCase().contains(username.toLowerCase());
        }

        private boolean contains(CharacterClass characterClass) {
            return password.chars().anyMatch(character -> CharacterClass.of(character) == characterClass);
        }
    }

    private enum CharacterClass {
        DIGIT,
        UPPER_CASE,
        LOWER_CASE,
        /** Anything else, including whitespace. */
        SPECIAL;

        static CharacterClass of(int character) {
            if (Character.isDigit(character)) {
                return DIGIT;
            }
            if (Character.isUpperCase(character)) {
                return UPPER_CASE;
            }
            if (Character.isLowerCase(character)) {
                return LOWER_CASE;
            }
            return SPECIAL;
        }
    }
}
