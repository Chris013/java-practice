package io.github.chris013.practice.task02;

/**
 * Everything that can be wrong with a password, in the order the legacy {@link PasswordChecker} reports them.
 */
public enum PasswordViolation {
    MISSING("Password missing"),
    TOO_SHORT("Password too short"),
    ADMIN_TOO_SHORT("Admin passwords need at least 12 characters"),
    MISSING_CHARACTER_CLASSES("Password needs digit, upper and lower case letter"),
    ADMIN_MISSING_SPECIAL_CHARACTER("Admin passwords need a special character"),
    CONTAINS_USERNAME("Password must not contain the username"),
    TOO_LONG("Password too long");

    private final String message;

    PasswordViolation(String message) {
        this.message = message;
    }

    public String message() {
        return message;
    }
}
