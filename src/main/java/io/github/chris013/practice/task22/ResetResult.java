package io.github.chris013.practice.task22;

/** The closed set of outcomes of {@link PasswordResetService#resetPassword}. */
public sealed interface ResetResult permits PasswordChanged, InvalidToken, ExpiredToken, PasswordTooWeak {
}
