package io.github.chris013.practice.task02;

import java.util.Set;

/**
 * The new API: reports <em>all</em> violations at once, so the sign-up form can show every problem in one go.
 *
 * <p>Task 02, part 2. Implement this only after you have refactored {@link PasswordChecker}.
 */
public final class PasswordPolicy {

    /**
     * @return every rule the password violates, iterated in the order of {@link PasswordViolation}'s constants;
     *     empty if the password is acceptable
     */
    public Set<PasswordViolation> violations(String password, String username, Role role) {
        throw new UnsupportedOperationException("TODO: Task 02, part 2");
    }
}
