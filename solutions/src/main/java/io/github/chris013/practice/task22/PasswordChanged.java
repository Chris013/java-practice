package io.github.chris013.practice.task22;

/** The password was changed and every reset token of the user was invalidated. */
public record PasswordChanged() implements ResetResult {
}
