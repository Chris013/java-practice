package io.github.chris013.practice.task02;

/**
 * Legacy entry point, kept so existing callers don't break. New code should use {@link PasswordPolicy}, which
 * reports every violation instead of only the first one.
 */
public class PasswordChecker {

    private final PasswordPolicy policy = new PasswordPolicy();

    /** @return the message of the first violated rule, or {@code null} if the password is acceptable */
    public String check(String password, String username, boolean isAdmin) {
        Role role = isAdmin ? Role.ADMIN : Role.USER;
        return policy.violations(password, username, role).stream()
                .findFirst()
                .map(PasswordViolation::message)
                .orElse(null);
    }
}
