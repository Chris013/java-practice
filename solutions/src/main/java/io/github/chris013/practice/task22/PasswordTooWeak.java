package io.github.chris013.practice.task22;

import java.util.Objects;

/** The new password was refused. The token stays valid, so the user can try again. */
public record PasswordTooWeak(String reason) implements ResetResult {

    public PasswordTooWeak {
        Objects.requireNonNull(reason, "reason");
    }
}
